/*
 * Copyright 2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spf4j.avro.logical_types;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.Schema;

/**
 * The extended {@code date} logical type: in addition to the standard
 * int-days-since-epoch form it supports a string form with a configurable
 * {@code format} and an int form carrying a {@code ymd} (yyyyMMdd) value.
 *
 * <p>Ported from this repository's official module (which targets the fork
 * avro) into the fork module against the standalone library; the package
 * migration {@code org.apache.avro} &rarr; {@code org.spf4j.avro} is the
 * only change. Registered through {@link
 * org.spf4j.avro.logical_types.factories.DateLogicalTypeFactory}; a
 * registered factory wins over the library's built-in {@code date} factory,
 * which is what makes the extended semantics live.
 *
 * @author Zoltan Farkas
 */
public class Date extends LogicalType {

  private final Boolean isYmdInt;

  private final String format;

  private final DateTimeFormatter formatter;

  private final Integer yearIdx;

  private final Integer monthIdx;

  private final Integer dayIdx;

  public Date(Boolean isYmdInt, String format, Integer yearIdx, Integer monthIdx, Integer dayIdx) {
    super("date");
    this.isYmdInt = isYmdInt;
    this.format = format;
    if (format != null) {
      ZoneId zulu = ZoneId.of("Z");
      formatter = DateTimeFormatter.ofPattern(format).withZone(zulu);
    } else {
      formatter = null;
    }
    this.yearIdx = yearIdx;
    this.monthIdx = monthIdx;
    this.dayIdx = dayIdx;
  }

  public Boolean getIsYmdInt() {
    return isYmdInt;
  }

  public DateTimeFormatter getFormatter() {
    return formatter;
  }

  public Integer getYearIdx() {
    return yearIdx;
  }

  public Integer getMonthIdx() {
    return monthIdx;
  }

  public Integer getDayIdx() {
    return dayIdx;
  }

  @Override
  public Schema addToSchema(Schema schema) {
      super.addToSchema(schema);
      if (isYmdInt != null) {
        schema.addProp("ymd", isYmdInt);
      }
      if (format != null) {
        schema.addProp("format", format);
      }
      return schema;
  }

}
