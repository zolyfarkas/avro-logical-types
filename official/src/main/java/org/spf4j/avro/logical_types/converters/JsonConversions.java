
package org.spf4j.avro.logical_types.converters;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.util.AppendableWriter;
import org.apache.avro.util.ByteArrayBuilder;
import org.apache.avro.util.CharSequenceReader;


/**
 *
 * @author Zoltan Farkas
 */
class JsonConversions<T> extends Conversion<T> {

  static final JsonFactory FACTORY = new JsonFactory();
  static final ObjectMapper MAPPER = new ObjectMapper(FACTORY);

  static {
    FACTORY.enable(JsonParser.Feature.ALLOW_COMMENTS);
    FACTORY.setCodec(MAPPER);
    FACTORY.enable(JsonParser.Feature.ALLOW_COMMENTS);
    FACTORY.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    FACTORY.setCodec(MAPPER);
  }

  private final Class<T> clasz;

  private final String typeName;

  JsonConversions(String typeName, Class<T> clasz) {
    this.clasz = clasz;
    this.typeName = typeName;
  }

  @Override
  public Class<T> getConvertedType() {
    return this.clasz;
  }

  @Override
  public String getLogicalTypeName() {
    return typeName;
  }

  @Override
  public ByteBuffer toBytes(T value, Schema schema, LogicalType type) {
    ByteArrayBuilder bab = new ByteArrayBuilder(16);
    try {
      MAPPER.writeValue(bab, value);
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot serialize " + value, ex);
    }
    return ByteBuffer.wrap(bab.getBuffer(), 0, bab.size());
  }

  @Override
  public CharSequence toCharSequence(T value, Schema schema, LogicalType type) {
    StringBuilder sb = new StringBuilder();
    try {
      MAPPER.writeValue(new AppendableWriter(sb), value);
      return sb;
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot serialize " + value, ex);
    }
  }

  @Override
  public T fromBytes(ByteBuffer value, Schema schema, LogicalType type) {
    try {
      if (value.hasArray()) {
        return MAPPER.readValue(FACTORY.createParser(value.array(), value.arrayOffset(),
                value.limit() - value.position()), clasz);
      } else {
        byte[] bytes = new byte[value.limit() - value.position()];
        ByteBuffer bb = value.duplicate();
        bb.get(bytes);
        return MAPPER.readValue(FACTORY.createParser(bytes), clasz);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot deserialize " + value, ex);
    }
  }

  @Override
  public T fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    try {
      return MAPPER.readValue(new CharSequenceReader(value), clasz);
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot deserialize: " + value + ", as " + clasz, ex);
    }
  }



}
