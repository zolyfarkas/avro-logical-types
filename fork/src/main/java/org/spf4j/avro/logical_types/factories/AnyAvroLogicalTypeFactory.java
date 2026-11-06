package org.spf4j.avro.logical_types.factories;

import org.spf4j.avro.AvroRuntimeException;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.LogicalTypes;
import org.spf4j.avro.Schema;
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
    String resolverName = schema.getProp("resolver");
    if (resolverName != null) {
      return new AnyAvro(resolverName, schema.getSchemaResolver());
    } else if (schema.getSchemaResolver() != null) {
      return new AnyAvro(null, schema.getSchemaResolver());
    } else {
      return AnyAvro.DEFAULT_INSTANCE;
    }
  }

  @Override
  public String getTypeName() {
    return "any";
  }

}
