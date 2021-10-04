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
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

/**
 * @author Zoltan Farkas
 */
public class InstantLogicalType extends LogicalType {

  private static final InstantLogicalType DEFAULT_INSTANCE = new InstantLogicalType(null, null, null, null);

  public static InstantLogicalType instance() {
    return DEFAULT_INSTANCE;
  }

  private final String format;

  private final DateTimeFormatter formatter;

  private final Integer epochSecondIdx;

  private final Integer nanoIdx;

  private final Integer millisIdx;


  public InstantLogicalType(String format, Integer epochSecondIdx, Integer nanoIdx, Integer millisIdx) {
    super("instant");
    this.format = format;
    ZoneId zulu = ZoneId.of("Z");
    if (format != null) {
      formatter = DateTimeFormatter.ofPattern(format).withZone(zulu);
    } else {
      formatter = null;
    }
    this.epochSecondIdx = epochSecondIdx;
    this.nanoIdx = nanoIdx;
    this.millisIdx = millisIdx;
  }

  public DateTimeFormatter getFormatter() {
    return formatter;
  }

  public Integer getEpochSecondIdx() {
    return epochSecondIdx;
  }

  public Integer getNanoIdx() {
    return nanoIdx;
  }

  public Integer getMillisIdx() {
    return millisIdx;
  }

  @Override
  public Schema addToSchema(Schema schema) {
      super.addToSchema(schema);
      if (format != null) {
        schema.addProp("format", format);
      }
      return schema;
  }

}
