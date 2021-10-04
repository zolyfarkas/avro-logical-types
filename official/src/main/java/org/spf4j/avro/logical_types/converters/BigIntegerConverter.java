
package org.spf4j.avro.logical_types.converters;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
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
