
package org.spf4j.avro.logical_types;

import org.spf4j.avro.DatumContext;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaResolver;

/**
 * The {@code "any"} logical type, applied to a record schema of exactly two
 * fields: {@code avsc} (index 0) and {@code content} (index 1).
 *
 * <p>
 * Resolver sourcing follows the standalone library's resolver design (no
 * global registry): this type carries only the resolver its schema graph was
 * stamped with at parse time ({@link Schema#getSchemaResolver()}), captured
 * here by the factory at logical-type construction. At IO time the converter
 * resolves per call through the datum context, which applies the precedence
 * GenericData context &gt; schema stamp &gt; embedded-names-only
 * (see {@link DatumContext#getEffectiveSchemaResolver(Schema)}). The
 * {@code "resolver"} name prop is still written by {@link #addToSchema} when
 * a name is set; the name is selected against the resolvers registered on the
 * caller's datum context, never against JVM-global state.
 * </p>
 */
public class AnyAvro extends LogicalType {

  public static final AnyAvro DEFAULT_INSTANCE = new AnyAvro(null, null);

  private final int avscIdx;

  private final int contentIdx;

  private final SchemaResolver resolver;

  private final String resolverName;

  public AnyAvro(String resolverName) {
    this(resolverName, null);
  }

  public AnyAvro(String resolverName, SchemaResolver stampedResolver) {
    super("any");
    this.avscIdx = 0;
    this.contentIdx = 1;
    this.resolverName = resolverName;
    this.resolver = stampedResolver;
  }

  public int getAvscIdx() {
    return avscIdx;
  }

  public int getContentIdx() {
    return contentIdx;
  }

  /**
   * The resolver stamped on the schema this logical type was constructed
   * from, or {@code null} when the schema was parsed without a resolver (or
   * built programmatically). This is a fallback source only: at IO time the
   * datum context takes precedence.
   */
  public SchemaResolver getResolver() {
    return resolver;
  }

  public String getResolverName() {
    return resolverName;
  }

  @Override
  public Schema addToSchema(Schema schema) {
      super.addToSchema(schema);
      if (resolverName != null) {
        schema.addProp(DatumContext.RESOLVER_PROP, resolverName);
      }
      return schema;
  }

}
