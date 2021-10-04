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

import org.apache.avro.LogicalType;

/**
 * Schema logicalType
 */
public final class SchemaLogicalType extends LogicalType {
  public static final SchemaLogicalType INSTANCE = new SchemaLogicalType();

  public SchemaLogicalType() {
    super("avsc");
  }

}
