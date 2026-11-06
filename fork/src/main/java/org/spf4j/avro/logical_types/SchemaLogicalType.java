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

import org.spf4j.avro.DatumContext;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaResolver;

/**
 * Decimal represents arbitrary-precision fixed-scale decimal numbers
 */
public final class SchemaLogicalType extends LogicalType {

  private final SchemaResolver resolver;

  private final String resolvername;

  public SchemaLogicalType(final String resolverName) {
    this(resolverName, null);
  }

  public SchemaLogicalType(final String resolverName, final SchemaResolver stampedResolver) {
    super("avsc");
    this.resolver = stampedResolver;
    this.resolvername = resolverName;
  }

  public SchemaResolver getResolver() {
    return resolver;
  }

  @Override
  public Schema addToSchema(Schema schema) {
      super.addToSchema(schema);
      if (resolvername != null) {
        schema.addProp(DatumContext.RESOLVER_PROP, resolvername);
      }
      return schema;
  }

}
