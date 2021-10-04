
package org.spf4j.avro.logical_types.converters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.spf4j.avro.logical_types.Date;

/**
 * @author Zoltan Farkas
 */
public class DateConverter extends Conversion<LocalDate> {

  @Override
  public Class<LocalDate> getConvertedType() {
    return LocalDate.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "date";
  }

  @Override
  public IndexedRecord toRecord(LocalDate value, Schema schema, LogicalType type) {
    Date dlt = (Date) type;
    GenericRecord rec = new GenericData.Record(schema);
    rec.put(dlt.getYearIdx(), value.getYear());
    rec.put(dlt.getMonthIdx(), value.getMonthValue());
    rec.put(dlt.getDayIdx(), value.getDayOfMonth());
    return rec;
  }

  @Override
  public CharSequence toCharSequence(LocalDate value, Schema schema, LogicalType type) {
    DateTimeFormatter fmt = ((Date) type).getFormatter();
    StringBuilder sb = new StringBuilder();
    if (fmt != null) {
      fmt.formatTo(value, sb);
    } else {
      DateTimeFormatter.ISO_LOCAL_DATE.formatTo(value, sb);
    }
    return sb;
  }

  @Override
  public Long toLong(LocalDate value, Schema schema, LogicalType type) {
    return value.toEpochDay();
  }

  @Override
  public Integer toInt(LocalDate value, Schema schema, LogicalType type) {
    Boolean isYmd = ((Date) type).getIsYmdInt();
    if (isYmd == null || !isYmd) {
      return (int) value.toEpochDay();
    } else {
      return value.getYear() * 10000 + value.getMonthValue() * 100 + value.getDayOfMonth();
    }
  }

  @Override
  public LocalDate fromRecord(IndexedRecord value, Schema schema, LogicalType type) {
    Date dlt = (Date) type;
    return LocalDate.of(((Number) value.get(dlt.getYearIdx())).intValue(),
            ((Number) value.get(dlt.getMonthIdx())).intValue(),
            ((Number) value.get(dlt.getDayIdx())).intValue());
  }

  @Override
  public LocalDate fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    DateTimeFormatter fmt = ((Date) type).getFormatter();
    if (fmt != null) {
      return fmt.parse(value, LocalDate::from);
    } else {
      return DateTimeFormatter.ISO_LOCAL_DATE.parse(value, LocalDate::from);
    }
  }

  @Override
  public LocalDate fromLong(Long value, Schema schema, LogicalType type) {
    return LocalDate.ofEpochDay(value);
  }

  @Override
  public LocalDate fromInt(Integer value, Schema schema, LogicalType type) {
    Boolean isYmd = ((Date) type).getIsYmdInt();
    if (isYmd == null || !isYmd) {
      return LocalDate.ofEpochDay(value);
    } else {
      int day = value % 100;
      int month = value % 10000 / 100;
      int year = value / 10000;
      return LocalDate.of(year, month, day);
    }
  }

}
