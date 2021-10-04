package org.spf4j.avro.logical_types.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.avro.AvroNamesRefResolver;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.data.RawJsonString;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.JsonExtensionDecoder;
import org.apache.avro.io.JsonExtensionEncoder;
import org.apache.avro.util.CharArrayBuilder;
import org.apache.avro.util.Optional;
import org.spf4j.avro.logical_types.SchemaLogicalType;

/**
 *
 * @author Zoltan Farkas
 */
public class SchemaConverter extends Conversion<Schema> {

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
     return new Schema.Parser(new AvroNamesRefResolver(((SchemaLogicalType) type).getResolver()))
             .parse(value.toString());
  }


  @Override
  public Optional<Schema> tryDirectDecode(Decoder dec, final Schema schema) throws IOException {
    if (dec instanceof JsonExtensionDecoder) {
      JsonExtensionDecoder pd = (JsonExtensionDecoder) dec;
      JsonNode nodes = pd.readValueAsTree(schema);
      return Optional.of(Schema.parse(nodes, new AvroNamesRefResolver(((SchemaLogicalType) schema.getLogicalType())
              .getResolver()),
              true, false, true));
    } else {
      return Optional.empty();
    }
  }

  public static RawJsonString toString(Schema schema, SchemaResolver res) {
    CharArrayBuilder sw = new CharArrayBuilder(32);
    try {
      JsonGenerator jgen = Schema.FACTORY.createGenerator(sw);
      schema.toJson(new AvroNamesRefResolver(res), jgen);
      jgen.flush();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return new RawJsonString(sw.getBuf(), sw.size());
  }

  @Override
  public boolean tryDirectEncode(Schema object, Encoder enc, final Schema schema) throws IOException {
    if (enc instanceof JsonExtensionEncoder) {
      ((JsonExtensionEncoder) enc).writeValue(toString(object,
              ((SchemaLogicalType) schema.getLogicalType()).getResolver()), schema);
      return true;
    } else {
      return false;
    }
  }


}
