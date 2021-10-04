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
package org.spf4j.avro.logical_types.factories;

import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.LogicalTypes.LogicalTypeFactory;
import org.apache.avro.Schema;
import org.spf4j.avro.logical_types.Date;

/**
 * @author zfarkas
 */
public class DateLogicalTypeFactory implements LogicalTypeFactory {

  @Override
  public String getTypeName() {
    return "date";
  }

  @Override
  public LogicalType fromSchema(final Schema schema) {
    Boolean isYmd = (Boolean) schema.getObjectProp("ymd");
    Schema.Type type = schema.getType();
    if (type == Schema.Type.INT && isYmd == null) {
      return LogicalTypes.date();
    } else {
      String format = schema.getProp("format");
      Integer yearIdx = null;
      Integer monthIdx = null;
      Integer dayIdx = null;
      if (type == Schema.Type.RECORD) {
        yearIdx = schema.getField("year").pos();
        monthIdx = schema.getField("month").pos();
        dayIdx = schema.getField("day").pos();
      }
      return new Date(isYmd, format, yearIdx, monthIdx, dayIdx);
    }
  }

}
