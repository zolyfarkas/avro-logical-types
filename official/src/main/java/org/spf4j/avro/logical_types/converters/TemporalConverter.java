/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.avro.logical_types.converters;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.threeten.extra.YearQuarter;

public class TemporalConverter extends Conversion<Temporal> {

  @Override
  public Class<Temporal> getConvertedType() {
    return Temporal.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "temporal";
  }

  private static int indexOf(final CharSequence cs, final int from, final int to, final char c) {
    for (int i = from; i < to; i++) {
      if (c == cs.charAt(i)) {
        return i;
      }
    }
    return -1;
  }

  private static int indexOfZone(final CharSequence cs, final int from, final int to) {
    for (int i = from; i < to; i++) {
      char c = cs.charAt(i);
      if (c == ':' || c == '.' || Character.isDigit(c)) {
        continue;
      } else {
        return i;
      }
    }
    return -1;
  }

  @Override
  public CharSequence toCharSequence(Temporal value, Schema schema, LogicalType type) {
    return value.toString();
  }

  @Override
  public Temporal fromCharSequence(CharSequence strVal, Schema schema, LogicalType type) {
    int l = strVal.length();
    int idx = indexOf(strVal, 0, l, '-');
    if (idx == 0) { // BC Year
      idx = indexOf(strVal, 1, l, '-');
    }
    if (idx < 0) {
      return Year.parse(strVal);
    }
    int mIdx = idx + 1;
    idx = indexOf(strVal, mIdx, l, '-');
    if (idx < 0) {
      if (strVal.charAt(mIdx) == 'Q') {
        return YearQuarter.parse(strVal);
      } else {
        return YearMonth.parse(strVal);
      }
    }
    idx = indexOf(strVal, idx + 1, l, 'T');
    if (idx < 0) {
      return LocalDate.parse(strVal);
    }
    idx = indexOfZone(strVal, idx + 1, l);
    if (idx < 0) {
      return LocalDateTime.parse(strVal);
    } else {
      return ZonedDateTime.parse(strVal);
    }
  }

}
