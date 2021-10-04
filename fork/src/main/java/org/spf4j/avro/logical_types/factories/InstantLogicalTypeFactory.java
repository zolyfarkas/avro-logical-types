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

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes.LogicalTypeFactory;
import org.apache.avro.Schema;
import org.spf4j.avro.logical_types.InstantLogicalType;

/**
 * @author zfarkas
 */
public class InstantLogicalTypeFactory implements LogicalTypeFactory {

  @Override
  public String getTypeName() {
    return "instant";
  }

  @Override
  public LogicalType fromSchema(final Schema schema) {
    Schema.Type type = schema.getType();
    switch (type) {
      case STRING:
        String format = schema.getProp("format");
        if (format == null) {
          return InstantLogicalType.instance();
        } else {
          return new InstantLogicalType(format, null, null, null);
        }
      case LONG:
         return InstantLogicalType.instance();
      case RECORD:
        Schema.Field millisField = schema.getField("millis");
        if (millisField != null) {
          return new InstantLogicalType(null, null, null, millisField.pos());
        } else {
          return new InstantLogicalType(null, schema.getField("epochSecond").pos(),
                  schema.getField("nano").pos(), null);
        }
      default:
        throw new AvroRuntimeException("Unsupported schema for instant " + schema);
    }
  }

}
