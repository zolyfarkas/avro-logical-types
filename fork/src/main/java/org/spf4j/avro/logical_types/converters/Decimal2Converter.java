
package org.spf4j.avro.logical_types.converters;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.BinaryData;
import org.apache.avro.io.DecimalEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.JsonExtensionDecoder;
import org.apache.avro.io.JsonExtensionEncoder;
import org.apache.avro.util.Optional;
import org.spf4j.avro.logical_types.Decimal2;

/**
 * @author Zoltan Farkas
 */
public final class Decimal2Converter extends Conversion<BigDecimal> {

  private static final boolean SET_SCALE_WHEN_SERIALIZING =
          Boolean.parseBoolean(System.getProperty("avro.decimal.setScaleWhenSerializing", "true"));

  @Override
  public Class<BigDecimal> getConvertedType() {
    return BigDecimal.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "decimal2";
  }

  public static void writeInt(final int n, final ByteBuffer buf) {
    int val = (n << 1) ^ (n >> 31);
    if ((val & ~0x7F) == 0) {
      buf.put((byte) val);
      return;
    } else if ((val & ~0x3FFF) == 0) {
      buf.put((byte) (0x80 | val));
      buf.put((byte) (val >>> 7));
      return;
    }
    byte[] tmp = new byte[5];
    int len = BinaryData.encodeInt(n, tmp, 0);
    buf.put(tmp, 0, len);
  }

  @Override
  public ByteBuffer toBytes(BigDecimal pdecimal, Schema schema, LogicalType type) {
    BigDecimal decimal = writeScale(pdecimal, (Decimal2) type);
    return toBytes(decimal);
  }

  private static ByteBuffer toBytes(BigDecimal decimal) {
    byte[] unscaledValue = decimal.unscaledValue().toByteArray();
    ByteBuffer buf = ByteBuffer.allocate(5 + unscaledValue.length);
    writeInt(decimal.scale(), buf);
    buf.put(unscaledValue);
    buf.flip();
    return buf;
  }

  public static int readInt(final ByteBuffer buf) {
    int n = 0;
    int b;
    int shift = 0;
    do {
      b = buf.get() & 0xff;
      n |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return (n >>> 1) ^ -(n & 1); // back to two's-complement
      }
      shift += 7;
    } while (shift < 32);
    throw new AvroRuntimeException("Invalid int encoding: " + buf);
  }

  @Override
  public BigDecimal fromBytes(ByteBuffer pbuf, Schema schema, LogicalType type) {
    ByteBuffer buf = pbuf.duplicate();
    int lscale = readInt(buf);
    byte[] unscaled = new byte[buf.remaining()];
    buf.get(unscaled);
    BigInteger unscaledBi = new BigInteger(unscaled);
    return readScale(new BigDecimal(unscaledBi, lscale), (Decimal2) type);
  }

  @Override
  public IndexedRecord toRecord(BigDecimal pdecimal, Schema schema, LogicalType type) {
    BigDecimal decimal = writeScale(pdecimal, (Decimal2) type);
    Decimal2 lt = (Decimal2) type;
    GenericRecord rec = new GenericData.Record(schema);
    rec.put(lt.getScaleIdx(), decimal.scale());
    rec.put(lt.getUnscaledIdx(), ByteBuffer.wrap(decimal.unscaledValue().toByteArray()));
    return rec;
  }

  @Override
  public BigDecimal fromRecord(IndexedRecord rec, Schema schema, LogicalType type) {
    Decimal2 lt = (Decimal2) type;
    ByteBuffer unscaled =  (ByteBuffer) rec.get(lt.getUnscaledIdx());
    byte[] usa = new byte[unscaled.remaining()];
    unscaled.get(usa);
    BigInteger unscaledBi = new BigInteger(usa);
    return readScale(new BigDecimal(unscaledBi, (int) rec.get(lt.getScaleIdx())), lt);
  }

  @Override
  public CharSequence toCharSequence(BigDecimal value, Schema schema, LogicalType type) {
        Decimal2 lt = (Decimal2) type;
        Boolean usePlainString = lt.getUsePlainString();
        if (usePlainString != null && usePlainString) {
          return writeScale(value, lt).toPlainString();
        } else {
          return writeScale(value, lt).toString();
        }
  }

  @Override
  public BigDecimal fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    return readScale(new BigDecimal(value.toString()), (Decimal2) type);
  }

  private BigDecimal readScale(BigDecimal result, Decimal2 logicalType) throws AvroRuntimeException {
    result = result.stripTrailingZeros(); // reduce precission if possible.
    int precision = result.precision();
    if (precision > logicalType.getPrecision()) {
      MathContext deserRm = logicalType.getDeserMc();
      if (deserRm != null) {
        result = result.round(deserRm);
      } else {
        throw new AvroRuntimeException("Received Decimal " + result + " is not compatible with precision " + precision
                + " if you desire rounding, you can annotate type with @deserRounding(\"HALF_UP\") or "
                        + "set the system property avro.decimal.defaultDeserRounding=HALF_UP ");
      }
    }
    return result;
  }

  private BigDecimal writeScale(BigDecimal decimal, Decimal2 logicalType) throws UnsupportedOperationException {
    Integer scale = logicalType.getScale();
    if (SET_SCALE_WHEN_SERIALIZING && scale != null) {
      MathContext serRm = logicalType.getSerMc();
      if (serRm != null) {
        decimal = decimal.setScale(scale, serRm.getRoundingMode());
      } else {
        decimal = decimal.setScale(scale);
      }
    }
    decimal = decimal.stripTrailingZeros();  // reduce precission if possible. (and the payload size)
    int precision = logicalType.getPrecision();
    if (decimal.precision() > precision) {
      throw new UnsupportedOperationException("Decimal " + decimal + " exceeds precision " + precision);
    }
    return decimal;
  }


  @Override
  public Optional<BigDecimal> tryDirectDecode(Decoder dec, final Schema schema) throws IOException {
    if (dec instanceof JsonExtensionDecoder) {
      BigDecimal bigD = ((JsonExtensionDecoder) dec).readBigDecimal(schema);
      if (bigD != null) {
        return Optional.of(readScale(bigD, (Decimal2) schema.getLogicalType()));
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean tryDirectEncode(BigDecimal object, Encoder enc, final Schema schema) throws IOException {
    if (DecimalEncoder.OPTIMIZED_JSON_DECIMAL_WRITE && enc instanceof JsonExtensionEncoder) {
      ((JsonExtensionEncoder) enc).writeDecimal(writeScale(object, (Decimal2) schema.getLogicalType()), schema);
      return true;
    } else {
      return false;
    }
  }

}
