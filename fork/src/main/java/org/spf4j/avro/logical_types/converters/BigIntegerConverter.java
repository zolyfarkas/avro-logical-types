
package org.spf4j.avro.logical_types.converters;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.spf4j.avro.Conversion;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.Schema;
import org.spf4j.avro.io.Decoder;
import org.spf4j.avro.io.Encoder;
import org.spf4j.avro.io.JsonExtensionDecoder;
import org.spf4j.avro.io.JsonExtensionEncoder;
import java.util.Optional;

/**
 *
 * @author Zoltan Farkas
 */
public class BigIntegerConverter extends Conversion<BigInteger> {

  @Override
  public Class<BigInteger> getConvertedType() {
    return BigInteger.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "bigint";
  }

  public Optional<BigInteger> tryDirectDecode(Decoder dec, Schema schema) throws IOException {
    if (dec instanceof JsonExtensionDecoder) {
      return Optional.ofNullable(((JsonExtensionDecoder) dec).readBigInteger(schema));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public boolean tryDirectEncode(BigInteger object, Encoder enc, Schema schema) throws IOException {
    if (enc instanceof JsonExtensionEncoder) {
      ((JsonExtensionEncoder) enc).writeBigInteger(object, schema);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public ByteBuffer toBytes(BigInteger value, Schema schema, LogicalType type) {
    return ByteBuffer.wrap(value.toByteArray());
  }

  @Override
  public CharSequence toCharSequence(BigInteger value, Schema schema, LogicalType type) {
    return value.toString();
  }

  @Override
  public BigInteger fromBytes(ByteBuffer value, Schema schema, LogicalType type) {
    value.rewind();
    byte[] unscaled = new byte[value.remaining()];
    value.get(unscaled);
    return new java.math.BigInteger(unscaled);
  }

  @Override
  public BigInteger fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    return new BigInteger(value.toString());
  }

}
