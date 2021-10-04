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

/**
 * Factory for creating the decimal logical type.
 */
public class DecimalFactory implements LogicalTypeFactory {

  @Override
  public String getTypeName() {
    return "decimal";
  }

  @Override
  public LogicalType fromSchema(Schema schema) {
    Number scale = (Number) schema.getObjectProp("scale");
    Number precision = (Number) schema.getObjectProp("precision");
    if (schema.getType() == Schema.Type.FIXED
            ||  "official".equals(schema.getObjectProp("format"))
            || Boolean.getBoolean("avro.defaultToStandardDecimalFormat")) {
      if (precision == null) { // we deviate here from avro and default to 36 instead of erroring out
        if (scale == null) {
          return LogicalTypes.decimal(36, 0);
        } else {
          return LogicalTypes.decimal(36, scale.intValue());
        }
      } else {
        if (scale == null) {
          return LogicalTypes.decimal(precision.intValue(), 0);
        } else {
          return LogicalTypes.decimal(precision.intValue(), scale.intValue());
        }
      }
    }
    return Decimal2Factory.newDecimal2LogicalType(getTypeName(), schema, precision, scale);
  }

}
