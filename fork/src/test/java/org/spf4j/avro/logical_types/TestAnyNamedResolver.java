
package org.spf4j.avro.logical_types;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaResolver;
import org.spf4j.avro.generic.GenericData;
import org.spf4j.avro.generic.GenericDatumReader;
import org.spf4j.avro.generic.GenericRecord;
import org.spf4j.avro.io.DatumReader;
import org.spf4j.avro.io.ExtendedJsonDecoder;

/**
 * The named-resolver scenario from the resolver design record, which had
 * no donor coverage: two named resolvers registered on one GenericData
 * context; an {@code any} schema carrying {@code resolver=B} resolves B's
 * schemas (and not A's); an any payload with no context and no stamp
 * resolves embedded-definitions-only.
 *
 * <p>
 * Resolvers A and B both resolve the same schema id
 * ({@code test:shared:1}) to <em>different</em> schemas, so the decoded
 * value's shape proves which resolver was selected.
 * </p>
 */
public class TestAnyNamedResolver {

  private static final String SHARED_ID = "test:shared:1";

  private static final Schema SCHEMA_A = new Schema.Parser().parse(
          "{ \"type\": \"record\", \"name\": \"SharedA\", \"fields\": ["
          + " { \"name\": \"a\", \"type\": \"string\" } ] }");

  private static final Schema SCHEMA_B = new Schema.Parser().parse(
          "{ \"type\": \"record\", \"name\": \"SharedB\", \"fields\": ["
          + " { \"name\": \"b\", \"type\": \"int\" } ] }");

  private static SchemaResolver resolverReturning(final Schema schema) {
    return new SchemaResolver() {
      @Override
      public Schema resolveSchema(String id) {
        if (SHARED_ID.equals(id)) {
          return schema;
        }
        throw new UnsupportedOperationException("Unknown schema: " + id);
      }

      @Override
      public String getId(Schema schema) {
        return null;
      }
    };
  }

  private static Schema anyWrapperSchema(final String resolverProp) {
    String prop = resolverProp == null ? "" : ", \"resolver\": \"" + resolverProp + "\"";
    return new Schema.Parser().parse(
            "{ \"type\": \"record\", \"name\": \"Wrapper\", \"fields\": ["
            + " { \"name\": \"anyField\", \"type\": "
            + "   { \"type\": \"record\", \"name\": \"AnyPayload\", \"logicalType\": \"any\"" + prop + ", \"fields\": ["
            + "     { \"name\": \"avsc\", \"type\": \"string\" },"
            + "     { \"name\": \"content\", \"type\": \"bytes\" } ] } } ] }");
  }

  private static GenericRecord readExtendedJson(final String json, final Schema schema, final GenericData data)
          throws IOException {
    DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema, schema, data);
    ExtendedJsonDecoder decoder = new ExtendedJsonDecoder(schema,
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    return reader.read(null, decoder);
  }

  @Test
  public void namedResolverSelectsBNotA() throws IOException {
    GenericData data = new GenericData();
    data.registerSchemaResolver("A", resolverReturning(SCHEMA_A));
    data.registerSchemaResolver("B", resolverReturning(SCHEMA_B));
    Schema wrapper = anyWrapperSchema("B");
    // The any schema carries the resolver=B prop (written by
    // AnyAvro.addToSchema from the parsed "resolver" attribute).
    Assert.assertEquals("B", wrapper.getField("anyField").schema().getProp("resolver"));
    String json = "{ \"anyField\": { \"avsc\": \"" + SHARED_ID + "\", \"content\": { \"b\": 7 } } }";
    GenericRecord back = readExtendedJson(json, wrapper, data);
    Object value = back.get("anyField");
    Assert.assertTrue("expected a GenericRecord decoded with B's schema, got: " + value,
            value instanceof GenericRecord);
    GenericRecord content = (GenericRecord) value;
    Assert.assertEquals(SCHEMA_B, content.getSchema());
    Assert.assertEquals(7, content.get("b"));
  }

  @Test
  public void namedResolverSelectsANotB() throws IOException {
    GenericData data = new GenericData();
    data.registerSchemaResolver("A", resolverReturning(SCHEMA_A));
    data.registerSchemaResolver("B", resolverReturning(SCHEMA_B));
    Schema wrapper = anyWrapperSchema("A");
    String json = "{ \"anyField\": { \"avsc\": \"" + SHARED_ID + "\", \"content\": { \"a\": \"fromA\" } } }";
    GenericRecord back = readExtendedJson(json, wrapper, data);
    GenericRecord content = (GenericRecord) back.get("anyField");
    Assert.assertEquals(SCHEMA_A, content.getSchema());
    Assert.assertEquals("fromA", content.get("a").toString());
  }

  @Test
  public void noContextNoStampResolvesEmbeddedOnly() throws IOException {
    // Parsed without a resolver (no stamp) and read with a plain
    // GenericData (no context resolvers): only what the payload itself
    // carries can resolve. A bare type name needs no external definition.
    Schema wrapper = anyWrapperSchema(null);
    String json = "{ \"anyField\": { \"avsc\": \"string\", \"content\": \"hi\" } }";
    GenericRecord back = readExtendedJson(json, wrapper, GenericData.get());
    Assert.assertEquals("hi", back.get("anyField").toString());
  }

  @Test
  public void noContextNoStampInlineObjectSchema() throws IOException {
    // Same as above, but the embedded schema travels as an inline JSON
    // object rather than as schema text.
    Schema wrapper = anyWrapperSchema(null);
    String json = "{ \"anyField\": { \"avsc\": { \"type\": \"record\", \"name\": \"EmbeddedObj\", \"fields\": "
            + "[ { \"name\": \"x\", \"type\": \"string\" } ] }, \"content\": { \"x\": \"ho\" } } }";
    GenericRecord back = readExtendedJson(json, wrapper, GenericData.get());
    GenericRecord content = (GenericRecord) back.get("anyField");
    Assert.assertEquals("EmbeddedObj", content.getSchema().getName());
    Assert.assertEquals("ho", content.get("x").toString());
  }
}
