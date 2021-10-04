package org.spf4j.avro.logical_types.converters;

import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
/**
 *
 * @author Zoltan Farkas
 */
public class SchemaConverter extends Conversion<Schema> {

  @Override
  public Class<Schema> getConvertedType() {
    return Schema.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "avsc";
  }

  @Override
  public CharSequence toCharSequence(Schema value, Schema schema, LogicalType type) {
    return value.toString();
  }

  @Override
  public Schema fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
     return new Schema.Parser()
             .parse(value.toString());
  }

}
