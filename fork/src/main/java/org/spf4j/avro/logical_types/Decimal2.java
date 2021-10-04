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

import java.math.MathContext;
import java.math.RoundingMode;
import javax.annotation.Nullable;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

/**
 * Decimal represents precision and scale limited decimal numbers.
 */
public class Decimal2 extends LogicalType {

  private static final RoundingMode DEFAULT_DESER_ROUNDING = getRoundingMode("avro.decimal.defaultDeserRounding");
  private static final RoundingMode DEFAULT_SER_ROUNDING = getRoundingMode("avro.decimal.defaultSerRounding");

  private static RoundingMode getRoundingMode(final String property) {
    String sdr = System.getProperty(property, "none");
    if (sdr == null || sdr.isEmpty() || "none".equalsIgnoreCase(sdr)) {
      return null;
    } else {
      return RoundingMode.valueOf(sdr);
    }
  }

  final MathContext mc;
  private final Number declaredPrecision;
  private final int precision;
  @Nullable
  private final Integer scale;
  private final MathContext serMc;
  private final MathContext deserMc;
  private final RoundingMode serRm;
  private final RoundingMode deserRm;
  private final Boolean usePlainString;
  private final Integer scaleIdx;
  private final Integer unscaledIdx;


  public Decimal2(String typeName, @Nullable Number precision, @Nullable Number scale,
          @Nullable RoundingMode serRm, @Nullable RoundingMode deserRm,
          final Boolean usePlainString, final Integer scaleIdx, final Integer unscaledIdx) {
    super(typeName);
    int pInt;
    if (precision == null) {
      pInt = 36;
      this.declaredPrecision = null;
    } else {
      pInt = precision.intValue();
      this.declaredPrecision = precision;
    }
    if (pInt <= 0) {
      throw new IllegalArgumentException("Invalid decimal precision: "
              + precision + " (must be positive)");
    }
    this.serMc = serRm == null
            ? (DEFAULT_SER_ROUNDING == null ? null : new MathContext(pInt, DEFAULT_SER_ROUNDING))
            : new MathContext(pInt, serRm);

    this.deserMc = deserRm == null
            ? (DEFAULT_DESER_ROUNDING == null ? null : new MathContext(pInt, DEFAULT_DESER_ROUNDING))
            : new MathContext(pInt, deserRm);
    mc = new MathContext(pInt, RoundingMode.HALF_EVEN);
    this.precision = pInt;
    this.scale = scale == null ? null : scale.intValue();
    this.usePlainString = usePlainString;
    this.scaleIdx = scaleIdx;
    this.unscaledIdx = unscaledIdx;
    this.serRm = serRm;
    this.deserRm = deserRm;
  }

  public Boolean getUsePlainString() {
    return usePlainString;
  }



  public int getPrecision() {
    return precision;
  }

  @Nullable
  public Integer getScale() {
    return scale;
  }

  public MathContext getSerMc() {
    return serMc;
  }

  public MathContext getDeserMc() {
    return deserMc;
  }

  public Integer getScaleIdx() {
    return scaleIdx;
  }

  public Integer getUnscaledIdx() {
    return unscaledIdx;
  }

  @Override
  public Schema addToSchema(Schema schema) {
      super.addToSchema(schema);
      if (declaredPrecision != null) {
        schema.addProp("precision", declaredPrecision);
      }
      if (scale != null) {
        schema.addProp("scale", scale);
      }
      if (serRm != null) {
        schema.addProp("serRounding", serRm.toString());
      }
      if (deserRm != null) {
         schema.addProp("deserRounding", deserRm.toString());
      }
      return schema;
  }





}
