
package org.spf4j.avro.logical_types.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.data.RawJsonString;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.ExtendedJsonDecoder;
import org.apache.avro.io.ExtendedJsonEncoder;
import org.apache.avro.io.JsonExtensionDecoder;
import org.apache.avro.io.JsonExtensionEncoder;
import org.apache.avro.reflect.ExtendedReflectData;
import org.apache.avro.reflect.ExtendedReflectDatumWriter;
import org.apache.avro.util.ByteArrayBuilder;
import org.apache.avro.util.CharArrayBuilder;
import org.apache.avro.util.CharSequenceReader;
import org.apache.avro.util.Optional;
import org.spf4j.avro.logical_types.AnyAvro;


public class AnyAvroConverter extends Conversion<Object> {

  @Override
  public Class<Object> getConvertedType() {
    return Object.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "any";
  }

  public Optional<Object> tryDirectDecode(Decoder dec, Schema schema) throws IOException {
  if (dec instanceof JsonExtensionDecoder) {
      JsonExtensionDecoder pd = (JsonExtensionDecoder) dec;
      JsonParser parser = pd.bufferValue(schema);
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
      AnyAvro lt = (AnyAvro) schema.getLogicalType();
      AvroNamesRefResolver ares = new AvroNamesRefResolver(lt.getResolver());
      token = parser.nextToken();
      if (token == JsonToken.VALUE_STRING) {
        String schemaText = parser.getText();
        char fc = schemaText.charAt(0);
        if (fc == '{' || fc == '"') {
          // This avro record is not envoded "direct", will attempt classic decoding.
          Schema anySchema = new Schema.Parser(ares).setValidate(false).parse(schemaText);
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
          return Optional.of(reader.read(null, jdec));
        }
      }
      Schema anySchema = Schema.parse(Schema.MAPPER.readTree(TokenBuffer.asCopyOfValue(parser).asParserOnFirstToken()),
              ares, true, false, true);
      token = parser.nextToken();
      if (token != JsonToken.FIELD_NAME) {
        throw new AvroRuntimeException("Unexpected token: " + token);
      }
      if (!"content".equals(parser.currentName())) {
        throw new AvroRuntimeException("Unexpected field: " + parser.currentName());
      }
      ExtendedJsonDecoder jdec = new ExtendedJsonDecoder(anySchema,
              parser, true);
      DatumReader reader = new GenericDatumReader(anySchema, anySchema);
      Object read = reader.read(null, jdec);
      return Optional.of(read);
    } else {
      return Optional.empty();
    }
  }

  public static RawJsonString toString(Schema schema, AvroNamesRefResolver ares)  {
    CharArrayBuilder sw = new CharArrayBuilder(32);
    try {
      JsonGenerator jgen = Schema.FACTORY.createGenerator(sw);
      schema.toJson(ares, jgen);
      jgen.flush();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return new RawJsonString(sw.getBuf(), sw.size());
  }

  @Override
  public boolean tryDirectEncode(Object object, Encoder enc, Schema schema) throws IOException {
   if (enc instanceof JsonExtensionEncoder) {
      Schema avsc;
      AnyAvro lt = (AnyAvro) schema.getLogicalType();
      AvroNamesRefResolver ares = new AvroNamesRefResolver(lt.getResolver());
      if (object == null) {
        avsc = Schema.create(Schema.Type.NULL);
      } else {
        avsc = ExtendedReflectData.get().getSchema(object.getClass());
        if (avsc == null) {
          avsc = ExtendedReflectData.get().createSchema(object.getClass(), object, new HashMap<>());
        }
        if (schema.getLogicalType().equals(avsc.getLogicalType())) {
          return false;
        }
      }
      Map record = new HashMap(4);
      record.put("avsc", toString(avsc, ares));
      CharArrayBuilder bab = new CharArrayBuilder(32);
      ExtendedJsonEncoder penc = new ExtendedJsonEncoder(avsc, Schema.FACTORY.createGenerator(bab));
      ExtendedReflectDatumWriter wr = new ExtendedReflectDatumWriter(avsc);
      wr.write(object, penc);
      penc.flush();
      record.put("content", new RawJsonString(bab.getBuf(), bab.size()));
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
        schema = ExtendedReflectData.get().getSchema(value.getClass());
        if (schema == null) {
          schema = ExtendedReflectData.get().createSchema(value.getClass(), value, new HashMap<>());
        }
        if (rschema.getLogicalType().equals(schema.getLogicalType())) {
          return (IndexedRecord) value;
        }
      }
      AnyAvro lt = (AnyAvro) rschema.getLogicalType();
      String strSchema = toString(schema, new AvroNamesRefResolver(lt.getResolver())).toString();
      GenericRecord result = new GenericData.Record(rschema);
      result.put(lt.getAvscIdx(), strSchema);
      ByteArrayBuilder bos = new ByteArrayBuilder(32);
      DatumWriter writer = new ExtendedReflectDatumWriter(schema);
      Encoder encoder = EncoderFactory.get().binaryEncoder(bos, null);
      try {
        writer.write(value, encoder);
        encoder.flush();
      } catch (IOException | RuntimeException ex) {
        throw new AvroRuntimeException("Cannot serialize " + value, ex);
      }
      result.put(lt.getContentIdx(), ByteBuffer.wrap(bos.getBuffer(), 0, bos.size()));
      return result;
  }

  @Override
  public Object fromRecord(IndexedRecord rec, Schema rschema, LogicalType type) {
    AnyAvro lt = (AnyAvro) rschema.getLogicalType();
    CharSequence schema = (CharSequence) rec.get(lt.getAvscIdx());
    Schema sch;
    try {
      sch = new Schema.Parser(new AvroNamesRefResolver(lt.getResolver())).parse(
              Schema.FACTORY.createParser(new CharSequenceReader(schema)));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    ByteBuffer bb = (ByteBuffer) rec.get(lt.getContentIdx());
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
