
package org.spf4j.avro.logical_types.converters;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.JsonExtensionDecoder;
import org.apache.avro.io.JsonExtensionEncoder;
import org.apache.avro.util.AppendableWriter;
import org.apache.avro.util.ByteArrayBuilder;
import org.apache.avro.util.CharSequenceReader;
import org.apache.avro.util.Optional;

/**
 *
 * @author Zoltan Farkas
 */
class JsonConversions<T> extends Conversion<T> {

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

  public Optional<T> tryDirectDecode(Decoder dec, Schema schema) throws IOException {
    if (dec instanceof JsonExtensionDecoder) {
      JsonExtensionDecoder pd = (JsonExtensionDecoder) dec;
      return Optional.of(pd.readValue(schema, clasz));
    } else {
      return Optional.empty();
    }
  }

  public boolean tryDirectEncode(T object, Encoder enc, Schema schema) throws IOException {
    if (enc instanceof JsonExtensionEncoder) {
      ((JsonExtensionEncoder) enc).writeValue(object, schema);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public ByteBuffer toBytes(T value, Schema schema, LogicalType type) {
    ByteArrayBuilder bab = new ByteArrayBuilder(16);
    try {
      Schema.MAPPER.writeValue(bab, value);
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot serialize " + value, ex);
    }
    return ByteBuffer.wrap(bab.getBuffer(), 0, bab.size());
  }

  @Override
  public CharSequence toCharSequence(T value, Schema schema, LogicalType type) {
    StringBuilder sb = new StringBuilder();
    try {
      Schema.MAPPER.writeValue(new AppendableWriter(sb), value);
      return sb;
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot serialize " + value, ex);
    }
  }

  @Override
  public T fromBytes(ByteBuffer value, Schema schema, LogicalType type) {
    try {
      if (value.hasArray()) {
        return Schema.MAPPER.readValue(Schema.FACTORY.createParser(value.array(), value.arrayOffset(),
                value.limit() - value.position()), clasz);
      } else {
        byte[] bytes = new byte[value.limit() - value.position()];
        ByteBuffer bb = value.duplicate();
        bb.get(bytes);
        return Schema.MAPPER.readValue(Schema.FACTORY.createParser(bytes), clasz);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot deserialize " + value, ex);
    }
  }

  @Override
  public T fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    try {
      return Schema.MAPPER.readValue(new CharSequenceReader(value), clasz);
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot deserialize: " + value + ", as " + clasz, ex);
    }
  }



}
