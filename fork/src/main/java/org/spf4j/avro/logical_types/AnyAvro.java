
package org.spf4j.avro.logical_types;

import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.SchemaResolver;
import org.apache.avro.SchemaResolvers;


public class AnyAvro extends LogicalType {

  public static final AnyAvro DEFAULT_INSTANCE = new AnyAvro(null);

  private final int avscIdx;

  private final int contentIdx;

  private final SchemaResolver resolver;

  private final String resolverName;

  public AnyAvro(String resolverName) {
    super("any");
    this.avscIdx = 0;
    this.contentIdx = 1;
    this.resolverName = resolverName;
    this.resolver = SchemaResolvers.get(this.resolverName);
  }

  public int getAvscIdx() {
    return avscIdx;
  }

  public int getContentIdx() {
    return contentIdx;
  }

  public SchemaResolver getResolver() {
    return resolver;
  }

  @Override
  public Schema addToSchema(Schema schema) {
      super.addToSchema(schema);
      if (resolverName != null) {
        schema.addProp("resolver", resolverName);
      }
      return schema;
  }

}
