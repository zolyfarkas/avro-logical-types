
package org.spf4j.avro.logical_types.converters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.RawValue;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.spf4j.avro.AvroRuntimeException;
import org.spf4j.avro.Conversion;
import org.spf4j.avro.DatumContext;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaResolver;
import org.spf4j.avro.generic.GenericContainer;
import org.spf4j.avro.generic.GenericData;
import org.spf4j.avro.generic.GenericDatumReader;
import org.spf4j.avro.generic.GenericDatumWriter;
import org.spf4j.avro.generic.GenericRecord;
import org.spf4j.avro.generic.IndexedRecord;
import org.spf4j.avro.io.BinaryDecoder;
import org.spf4j.avro.io.DatumReader;
import org.spf4j.avro.io.DatumWriter;
import org.spf4j.avro.io.Decoder;
import org.spf4j.avro.io.DecoderFactory;
import org.spf4j.avro.io.Encoder;
import org.spf4j.avro.io.EncoderFactory;
import org.spf4j.avro.io.ExtendedJsonDecoder;
import org.spf4j.avro.io.ExtendedJsonEncoder;
import org.spf4j.avro.io.JsonExtensionDecoder;
import org.spf4j.avro.io.JsonExtensionEncoder;
import org.spf4j.avro.io.ResolvingDecoder;
import org.spf4j.avro.reflect.ReflectData;
import org.spf4j.avro.logical_types.AnyAvro;

/**
 * Conversion for the {@code "any"} logical type.
 *
 * <p>
 * Resolver sourcing follows the standalone library's resolver design (no
 * global registry): every entry point resolves its effective schema resolver
 * per call. The direct hooks receive a {@link DatumContext} from the datum
 * reader/writer and use
 * {@link DatumContext#getEffectiveSchemaResolver(Schema)} (GenericData
 * context &gt; schema stamp &gt; embedded-names-only). The plain
 * {@code toRecord}/{@code fromRecord} conversion methods have no IO context,
 * so they start the same precedence at the schema stamp via
 * {@link DatumContext#resolveEffectiveSchemaResolver(GenericData, Schema)}
 * with a {@code null} datum model.
 * </p>
 */
public class AnyAvroConverter extends Conversion<Object> {

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  @Override
  public Class<Object> getConvertedType() {
    return Object.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "any";
  }

  private static SchemaResolver effectiveResolver(final DatumContext context, final Schema schema) {
    return context != null
            ? context.getEffectiveSchemaResolver(schema)
            : DatumContext.resolveEffectiveSchemaResolver(null, schema);
  }

  @Override
  public Optional<Object> tryDirectDecode(Decoder dec, Schema schema) throws IOException {
    return tryDirectDecode(dec, schema, null);
  }

  @Override
  public Optional<Object> tryDirectDecode(Decoder dec, Schema schema, DatumContext context) throws IOException {
  if (dec instanceof JsonExtensionDecoder) {
      JsonExtensionDecoder pd = (JsonExtensionDecoder) dec;
      if (dec instanceof ResolvingDecoder && hasUnionContent(schema)) {
        // ResolvingDecoder.bufferValue advances its resolving grammar by
        // the value schema after the underlying decoder consumed the
        // value, and that walk does not support a writer-selected union
        // branch (the any-record's nullable content field): it would
        // throw after consumption, when declining is no longer possible.
        // Decline up front instead; the classic path reads the record
        // form and converts it through fromRecord.
        return Optional.empty();
      }
      JsonParser parser;
      try {
        parser = pd.bufferValue(schema);
      } catch (UnsupportedOperationException ex) {
        // A ResolvingDecoder whose underlying decoder is not an extension
        // decoder (binary, stock JSON) throws from its extension check
        // before anything is consumed; declining is safe.
        return Optional.empty();
      }
      JsonToken token = parser.currentToken();
      if (token != JsonToken.START_OBJECT) {
        throw new AvroRuntimeException("Unexpected token: " + token);
      }
      token = parser.nextToken();
      if (token != JsonToken.FIELD_NAME) {
         throw new AvroRuntimeException("Unexpected token: " + token);
      }
      if (!"avsc".equals(parser.currentName())) {
        throw new AvroRuntimeException("Unexpected field: " + parser.currentName());
      }
      SchemaResolver resolver = effectiveResolver(context, schema);
      token = parser.nextToken();
      if (token == JsonToken.VALUE_STRING) {
        String schemaText = parser.getText();
        char fc = schemaText.isEmpty() ? '\0' : schemaText.charAt(0);
        if (fc == '{' || fc == '"') {
          // This avro record is not envoded "direct", will attempt classic decoding.
          Schema anySchema = new Schema.Parser().setSchemaResolver(resolver).parse(schemaText);
          token = parser.nextToken();
          if (token != JsonToken.FIELD_NAME) {
             throw new AvroRuntimeException("Unexpected token: " + token);
          }
          if (!"content".equals(parser.currentName())) {
            throw new AvroRuntimeException("Unexpected field: " + parser.currentName());
          }
          token = parser.nextToken();
          if (token != JsonToken.VALUE_STRING) {
            throw new AvroRuntimeException("Unexpected token: " + token);
          }
          String asText = parser.getText();
          byte[] bytes = asText.getBytes(StandardCharsets.ISO_8859_1);
          BinaryDecoder jdec = DecoderFactory.get().directBinaryDecoder(new ByteArrayInputStream(bytes), null);
          DatumReader reader = new GenericDatumReader(anySchema, anySchema);
          return decoded(reader.read(null, jdec), schema, anySchema, resolver);
        }
        // Not an inline schema text: a schema id to resolve through the
        // effective resolver, or a bare type name (e.g. "null") that parses
        // as a schema directly (as a JSON string node, the form the fork's
        // Schema.parse(JsonNode, ...) accepted).
        Schema anySchema = resolveById(resolver, schemaText);
        if (anySchema == null) {
          anySchema = new Schema.Parser().setSchemaResolver(resolver).parse('"' + schemaText + '"');
        }
        return decoded(readContent(parser, anySchema), schema, anySchema, resolver);
      }
      JsonNode avscNode = JsonConversions.MAPPER.readTree(
              TokenBuffer.asCopyOfValue(parser).asParserOnFirstToken());
      Schema anySchema = new Schema.Parser().setSchemaResolver(resolver).parse(avscNode);
      return decoded(readContent(parser, anySchema), schema, anySchema, resolver);
    } else {
      return Optional.empty();
    }
  }

  /**
   * The result of a direct decode that has already consumed the value from
   * the decoder (via {@code bufferValue}). The direct-decode contract does
   * not allow declining at this point: {@code Optional.empty()} makes the
   * datum reader re-read the value from a stream that has already advanced
   * past it, corrupting the read. A non-null decoded value is returned
   * as-is. A decoded {@code null} (a null any: avsc is the null schema, or a
   * nullable content schema with a null value) cannot be carried by an
   * {@code Optional}; for that case the classic record representation of
   * the null any is delivered instead &mdash; the same {@code {avsc,
   * content}} record a reader without the direct hook produces, whose
   * null-content case {@link #fromRecord} converts back to {@code null}.
   */
  private Optional<Object> decoded(final Object value, final Schema schema, final Schema anySchema,
          final SchemaResolver resolver) {
    if (value != null) {
      return Optional.of(value);
    }
    AnyAvro lt = (AnyAvro) schema.getLogicalType();
    GenericRecord record = new GenericData.Record(schema);
    record.put(lt.getAvscIdx(), (String) toString(anySchema, resolver).rawValue());
    record.put(lt.getContentIdx(), null);
    return Optional.of(record);
  }

  private static boolean hasUnionContent(final Schema anyRecordSchema) {
    AnyAvro lt = (AnyAvro) anyRecordSchema.getLogicalType();
    Schema content = anyRecordSchema.getFields().get(lt.getContentIdx()).schema();
    return content.getType() == Schema.Type.UNION;
  }

  private static Schema resolveById(final SchemaResolver resolver, final String id) {
    try {
      return resolver.resolveSchema(id);
    } catch (UnsupportedOperationException ex) {
      // A resolver that resolves nothing (SchemaResolver.NONE and
      // decorators over it) signals that by throwing; treat as unresolved.
      return null;
    }
  }

  private static Object readContent(final JsonParser parser, final Schema anySchema) throws IOException {
    JsonToken token = parser.nextToken();
    if (token != JsonToken.FIELD_NAME) {
       throw new AvroRuntimeException("Unexpected token: " + token);
    }
    if (!"content".equals(parser.currentName())) {
      throw new AvroRuntimeException("Unexpected field: " + parser.currentName());
    }
    ExtendedJsonDecoder jdec = new ExtendedJsonDecoder(anySchema,
            parser, true);
    DatumReader reader = new GenericDatumReader(anySchema, anySchema);
    return reader.read(null, jdec);
  }

  /**
   * The schema JSON of {@code schema} as raw JSON, with named schemas
   * written as references through {@code ares} (the fork wrote this through
   * its {@code RawJsonString} type, which the standalone library does not
   * carry; Jackson's {@link RawValue} is the equivalent raw-JSON holder for
   * {@code writeValue}/{@code writeObject}).
   */
  public static RawValue toString(Schema schema, SchemaResolver ares)  {
    StringWriter sw = new StringWriter(64);
    try {
      JsonGenerator jgen = JSON_FACTORY.createGenerator(sw);
      // Schema.toJson writes schema props through JsonGenerator.writeObject,
      // which requires an ObjectCodec on the generator; the library's own
      // schema factory carries one (Schema.FACTORY.setCodec(Schema.MAPPER)),
      // this private factory does not, so set the codec explicitly.
      jgen.setCodec(JsonConversions.MAPPER);
      schema.toJson(ares, jgen);
      jgen.flush();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return new RawValue(sw.toString());
  }

  /**
   * Infer the schema of an arbitrary value. Replaces the fork's
   * {@code ExtendedReflectData.createSchema(Class, Object, ...)} (not part
   * of the standalone library): containers and scalars are inferred from
   * the value itself (like the fork's object-aware variant), records carry
   * their own schema, and anything else falls back to the library's
   * {@link ReflectData} class-based inference.
   */
  static Schema schemaOf(final Object object) {
    if (object == null) {
      return Schema.create(Schema.Type.NULL);
    }
    if (object instanceof GenericContainer) {
      return ((GenericContainer) object).getSchema();
    }
    if (object instanceof CharSequence) {
      return Schema.create(Schema.Type.STRING);
    }
    if (object instanceof Boolean) {
      return Schema.create(Schema.Type.BOOLEAN);
    }
    if (object instanceof Integer) {
      return Schema.create(Schema.Type.INT);
    }
    if (object instanceof Long) {
      return Schema.create(Schema.Type.LONG);
    }
    if (object instanceof Float) {
      return Schema.create(Schema.Type.FLOAT);
    }
    if (object instanceof Double) {
      return Schema.create(Schema.Type.DOUBLE);
    }
    if (object instanceof byte[] || object instanceof ByteBuffer) {
      return Schema.create(Schema.Type.BYTES);
    }
    if (object instanceof List) {
      List<?> list = (List<?>) object;
      Schema element = list.isEmpty() ? Schema.create(Schema.Type.NULL) : schemaOf(list.get(0));
      return Schema.createArray(element);
    }
    if (object instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) object;
      Schema value = map.isEmpty() ? Schema.create(Schema.Type.NULL)
              : schemaOf(map.values().iterator().next());
      return Schema.createMap(value);
    }
    return ReflectData.get().getSchema(object.getClass());
  }

  @Override
  public boolean tryDirectEncode(Object object, Encoder enc, Schema schema) throws IOException {
    return tryDirectEncode(object, enc, schema, null);
  }

  @Override
  public boolean tryDirectEncode(Object object, Encoder enc, Schema schema, DatumContext context) throws IOException {
   if (enc instanceof JsonExtensionEncoder) {
      Schema avsc;
      SchemaResolver resolver = effectiveResolver(context, schema);
      if (object == null) {
        avsc = Schema.create(Schema.Type.NULL);
      } else {
        avsc = schemaOf(object);
        if (schema.getLogicalType().equals(avsc.getLogicalType())) {
          return false;
        }
      }
      Map record = new HashMap(4);
      record.put("avsc", toString(avsc, resolver));
      ByteArrayOutputStream bab = new ByteArrayOutputStream(64);
      // The nested content write can itself contain extension values
      // (a nested any, json logical types), which reach
      // JsonExtensionEncoder.writeValue and its codec-requiring
      // JsonGenerator.writeObject; the (Schema, OutputStream) encoder
      // constructor creates a codec-less generator, so supply a generator
      // carrying this module's mapper as its codec instead.
      JsonGenerator nestedGen = JSON_FACTORY.createGenerator(bab);
      nestedGen.setCodec(JsonConversions.MAPPER);
      ExtendedJsonEncoder penc = new ExtendedJsonEncoder(avsc, nestedGen);
      DatumWriter wr = new GenericDatumWriter(avsc);
      wr.write(object, penc);
      penc.flush();
      record.put("content", new RawValue(new String(bab.toByteArray(), StandardCharsets.UTF_8)));
      ((JsonExtensionEncoder) enc).writeValue(record, schema);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public IndexedRecord toRecord(Object value, Schema rschema, LogicalType type) {
      Schema schema;
      if (value == null) {
        schema = Schema.create(Schema.Type.NULL);
      } else {
        schema = schemaOf(value);
        if (rschema.getLogicalType().equals(schema.getLogicalType())) {
          return (IndexedRecord) value;
        }
      }
      AnyAvro lt = (AnyAvro) rschema.getLogicalType();
      // The raw schema JSON text, not RawValue.toString(): databind's
      // RawValue renders itself as a diagnostic string ("[RawValue of type
      // ...]"), unlike the fork's RawJsonString, whose toString was the raw
      // text the donor stored here.
      String strSchema = (String) toString(schema,
              DatumContext.resolveEffectiveSchemaResolver(null, rschema)).rawValue();
      GenericRecord result = new GenericData.Record(rschema);
      result.put(lt.getAvscIdx(), strSchema);
      ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
      DatumWriter writer = new GenericDatumWriter(schema);
      Encoder encoder = EncoderFactory.get().binaryEncoder(bos, null);
      try {
        writer.write(value, encoder);
        encoder.flush();
      } catch (IOException | RuntimeException ex) {
        throw new AvroRuntimeException("Cannot serialize " + value, ex);
      }
      result.put(lt.getContentIdx(), ByteBuffer.wrap(bos.toByteArray()));
      return result;
  }

  @Override
  public Object fromRecord(IndexedRecord rec, Schema rschema, LogicalType type) {
    AnyAvro lt = (AnyAvro) rschema.getLogicalType();
    ByteBuffer bb = (ByteBuffer) rec.get(lt.getContentIdx());
    if (bb == null) {
      // A null content is a null any value (the any-record variants with a
      // nullable content field carry it directly; see the JMX payloads).
      return null;
    }
    CharSequence schema = (CharSequence) rec.get(lt.getAvscIdx());
    Schema sch = new Schema.Parser()
            .setSchemaResolver(DatumContext.resolveEffectiveSchemaResolver(null, rschema))
            .parse(schema.toString());
    DatumReader reader = new GenericDatumReader(sch, sch);
    InputStream is = new ByteArrayInputStream(bb.array(),  bb.arrayOffset(), bb.limit() - bb.position());
    try {
      Decoder decoder = DecoderFactory.get().binaryDecoder(is, null);
      return reader.read(null, decoder);
    } catch (IOException | RuntimeException ex) {
      throw new AvroRuntimeException(this + " parsing failed for " + sch + ", from " + is, ex);
    }
  }




}
