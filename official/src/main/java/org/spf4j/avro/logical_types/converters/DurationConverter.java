
package org.spf4j.avro.logical_types.converters;

import java.time.Duration;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.spf4j.avro.logical_types.DurationLogicalType;

/**
 * @author Zoltan Farkas
 */
public class DurationConverter extends Conversion<Duration> {

  @Override
  public CharSequence toCharSequence(Duration value, Schema schema, LogicalType type) {
    return value.toString();
  }

  @Override
  public Duration fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
      return Duration.parse(value);
  }

  @Override
  public Long toLong(Duration value, Schema schema, LogicalType type) {
    return value.toNanos();
  }

  @Override
  public Duration fromLong(Long value, Schema schema, LogicalType type) {
    return Duration.ofNanos(value);
  }


  @Override
  public Class<Duration> getConvertedType() {
    return Duration.class;
  }

  @Override
  public String getLogicalTypeName() {
    return DurationLogicalType.instance().getName();
  }


}
