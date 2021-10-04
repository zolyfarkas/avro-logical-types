
package org.spf4j.avro.logical_types.converters;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.avro.logical_types.InstantLogicalType;

/**
 * @author Zoltan Farkas
 */
public class InstantConverter extends Conversion<Instant> {

  @Override
  public Class<Instant> getConvertedType() {
    return Instant.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "instant";
  }

  @Override
  public IndexedRecord toRecord(Instant value, Schema schema, LogicalType type) {
    InstantLogicalType dlt = (InstantLogicalType) type;
    GenericRecord rec = new GenericData.Record(schema);
    if (dlt.getMillisIdx() != null) {
      rec.put(dlt.getMillisIdx(), value.toEpochMilli());
    } else {
      rec.put(dlt.getEpochSecondIdx(), value.getEpochSecond());
      rec.put(dlt.getNanoIdx(), value.getNano());
    }
    return rec;
  }

  @Override
  public CharSequence toCharSequence(Instant value, Schema schema, LogicalType type) {
    DateTimeFormatter fmt = ((InstantLogicalType) type).getFormatter();
    StringBuilder sb = new StringBuilder(30);
    if (fmt != null) {
      fmt.formatTo(value, sb);
    } else {
      DateTimeFormatter.ISO_INSTANT.formatTo(value, sb);
    }
    return sb;
  }

  @Override
  public Long toLong(Instant value, Schema schema, LogicalType type) {
    return value.toEpochMilli();
  }

  @Override
  public Instant fromRecord(IndexedRecord value, Schema schema, LogicalType type) {
    InstantLogicalType dlt = (InstantLogicalType) type;
    Integer millisIdx = dlt.getMillisIdx();
    if (millisIdx != null) {
      return Instant.ofEpochMilli(((Number) value.get(millisIdx)).longValue());
    } else {
      return Instant.ofEpochSecond(((Number) value.get(dlt.getEpochSecondIdx())).longValue(),
            ((Number) value.get(dlt.getNanoIdx())).longValue());
    }
  }

  @Override
  public Instant fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    DateTimeFormatter fmt = ((InstantLogicalType) type).getFormatter();
    if (fmt != null) {
      return fmt.parse(value, Instant::from);
    } else {
      return DateTimeFormatter.ISO_INSTANT.parse(value, Instant::from);
    }
  }

  @Override
  public Instant fromLong(Long value, Schema schema, LogicalType type) {
    return Instant.ofEpochMilli(value);
  }


}
