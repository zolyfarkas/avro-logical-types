package org.spf4j.avro.logical_types.factories;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.spf4j.avro.logical_types.AnyAvro;

/**
 * @author Zoltan Farkas
 */
public class AnyAvroLogicalTypeFactory implements LogicalTypes.LogicalTypeFactory {

  @Override
  public LogicalType fromSchema(Schema schema) {
    if (schema.getType() != Schema.Type.RECORD) {
      throw new AvroRuntimeException("Invalid schema type for Any: " + schema);
    }
    return AnyAvro.DEFAULT_INSTANCE;
  }

  @Override
  public String getTypeName() {
    return "any";
  }

}
