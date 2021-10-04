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

import java.math.RoundingMode;
import javax.annotation.Nullable;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes.LogicalTypeFactory;
import org.apache.avro.Schema;
import org.spf4j.avro.logical_types.Decimal2;

/**
 * factory for creating the decimal logical type.
 */
public class Decimal2Factory implements LogicalTypeFactory {

  private static final boolean USE_PLAIN_STRING
          = Boolean.parseBoolean(System.getProperty("avro.decimal.usePlainString", "false"));

  @Override
  public String getTypeName() {
    return "decimal2";
  }

  @Nullable
  private static RoundingMode getRoundingMode(Schema schema, String fieldName) {
    Object n = schema.getObjectProp(fieldName);
    if (n == null) {
      return null;
    } else {
      if (n instanceof RoundingMode) {
        return (RoundingMode) n;
      } else {
        String str = n.toString();
        return RoundingMode.valueOf(str);
      }
    }
  }

  @Override
  public LogicalType fromSchema(Schema schema) {
    Number scale = (Number) schema.getObjectProp("scale");
    Number precision = (Number) schema.getObjectProp("precision");
    return newDecimal2LogicalType(getTypeName(), schema, precision, scale);
  }

  public static LogicalType newDecimal2LogicalType(String typeName, Schema schema,
          Number precision, Number scale) {
    switch (schema.getType()) {
      case BYTES:
      case STRING:
      case RECORD:
        break;
      default:
        throw new AvroRuntimeException("Unsupported avro type for decimal: " + schema);
    }
    boolean usePlainString = USE_PLAIN_STRING;
    Object ups = schema.getObjectProp("usePlainString");
    if (ups != null) {
      usePlainString = (Boolean) ups;
    }
    Integer scaleIdx;
    Integer unscaledIdx;
    if (schema.getType() == Schema.Type.RECORD) {
      scaleIdx = schema.getField("scale").pos();
      unscaledIdx = schema.getField("unscaled").pos();
    } else {
      scaleIdx = null;
      unscaledIdx = null;
    }
    return new Decimal2(typeName, precision, scale,
            getRoundingMode(schema, "serRounding"),
            getRoundingMode(schema, "deserRounding"),
            usePlainString, scaleIdx, unscaledIdx);
  }

}
