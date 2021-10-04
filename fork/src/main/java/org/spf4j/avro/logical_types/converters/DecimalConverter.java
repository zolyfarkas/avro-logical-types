package org.spf4j.avro.logical_types.converters;

import java.math.BigDecimal;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalTypes;

/**
 *
 * @author Zoltan Farkas
 */
public class DecimalConverter extends MultiConverter<BigDecimal> {

  public DecimalConverter() {
    super(type -> type.getClass() == LogicalTypes.Decimal.class ? 0 : 1,
             new Conversions.DecimalConversion(), new Decimal2Converter());
  }

}
