
package org.spf4j.avro.logical_types.converters;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.RawValue;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.spf4j.avro.Conversion;
import org.spf4j.avro.DatumContext;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaResolver;
import org.spf4j.avro.io.Decoder;
import org.spf4j.avro.io.Encoder;
import org.spf4j.avro.io.JsonExtensionDecoder;
import org.spf4j.avro.io.JsonExtensionEncoder;
import org.spf4j.avro.logical_types.SchemaLogicalType;

/**
 *
 * @author Zoltan Farkas
 */
public class SchemaConverter extends Conversion<Schema> {

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  @Override
  public Class<Schema> getConvertedType() {
    return Schema.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "avsc";
  }

  @Override
  public CharSequence toCharSequence(Schema value, Schema schema, LogicalType type) {
    return value.toString();
  }

  @Override
  public Schema fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
     return new Schema.Parser()
             .setSchemaResolver(DatumContext.resolveEffectiveSchemaResolver(null, schema))
             .parse(value.toString());
  }


  @Override
  public Optional<Schema> tryDirectDecode(Decoder dec, final Schema schema) throws IOException {
    return tryDirectDecode(dec, schema, null);
  }

  @Override
  public Optional<Schema> tryDirectDecode(Decoder dec, final Schema schema, DatumContext context) throws IOException {
    if (dec instanceof JsonExtensionDecoder) {
      JsonExtensionDecoder pd = (JsonExtensionDecoder) dec;
      // The library decoder's readValueAsTree reads through the decoder's
      // own mapper (its parsers carry no ObjectCodec, so a local
      // bufferValue + own-mapper route-around was needed only until that
      // library fix landed). A ResolvingDecoder whose underlying decoder
      // is not an extension decoder throws from the extension check
      // before anything is consumed, so that case declines safely to the
      // classic conversion path.
      JsonNode nodes;
      try {
        nodes = pd.readValueAsTree(schema);
      } catch (UnsupportedOperationException ex) {
        return Optional.empty();
      }
      SchemaResolver resolver = context != null
              ? context.getEffectiveSchemaResolver(schema)
              : DatumContext.resolveEffectiveSchemaResolver(null, schema);
      return Optional.of(new Schema.Parser().setSchemaResolver(resolver).parse(nodes));
    } else {
      return Optional.empty();
    }
  }

  public static RawValue toString(Schema schema, SchemaResolver res) {
    StringWriter sw = new StringWriter(64);
    try {
      JsonGenerator jgen = JSON_FACTORY.createGenerator(sw);
      // See AnyAvroConverter.toString: schema props are written through
      // JsonGenerator.writeObject, which needs an ObjectCodec on the
      // generator; this private factory has none, so set one explicitly.
      jgen.setCodec(JsonConversions.MAPPER);
      schema.toJson(res, jgen);
      jgen.flush();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return new RawValue(sw.toString());
  }

  @Override
  public boolean tryDirectEncode(Schema object, Encoder enc, final Schema schema) throws IOException {
    return tryDirectEncode(object, enc, schema, null);
  }

  @Override
  public boolean tryDirectEncode(Schema object, Encoder enc, final Schema schema, DatumContext context) throws IOException {
    if (enc instanceof JsonExtensionEncoder) {
      SchemaResolver resolver = context != null
              ? context.getEffectiveSchemaResolver(schema)
              : DatumContext.resolveEffectiveSchemaResolver(null, schema);
      ((JsonExtensionEncoder) enc).writeValue(toString(object, resolver), schema);
      return true;
    } else {
      return false;
    }
  }


}
