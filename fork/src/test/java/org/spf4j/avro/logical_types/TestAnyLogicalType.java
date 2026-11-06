
/*
 * Copyright 2019 The Apache Software Foundation.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaBuilder;
import org.spf4j.avro.SchemaResolver;
import org.spf4j.avro.generic.GenericData;
import org.spf4j.avro.generic.GenericDatumReader;
import org.spf4j.avro.generic.GenericRecord;
import org.spf4j.avro.io.DatumReader;
import org.spf4j.avro.io.Decoder;
import org.spf4j.avro.io.ExtendedJsonDecoder;

/**
 * Port of the fork core's {@code TestAnyLogicalType} into the logical-types
 * library's own test tree, adapted to the standalone library per the
 * resolver design record:
 *
 * <ul>
 * <li>The donor {@code @BeforeClass} registration of a default resolver in
 * the fork's global {@code SchemaResolvers} registry is replaced by the
 * design's two mechanisms: the resolver is registered on the
 * {@link GenericData} used for IO (context), and/or stamped at parse time
 * via {@code Parser.setSchemaResolver}. The resolver itself is the donor's:
 * ids matching {@code org.spf4j.avro:service-schema:.*:6} resolve to the
 * service-error schema, and a schema's id is its {@code mvnId} prop.</li>
 * <li>{@code AvroUtils} calls are replaced by {@link AvroTestIO} (test-tree
 * datum-API helper).</li>
 * <li>The spf4j-generated classes ({@code org.spf4j.base.avro.*},
 * {@code org.spf4j.service.avro.*}) are re-expressed as locally defined
 * schemas + {@link GenericRecord}s (see {@link #serviceErrorSchema()} and
 * friends) preserving what each scenario pins.</li>
 * <li>{@code testServiceErrorParsing2}'s donor path
 * ({@code ExtendedReflectData} + {@code ReflectDatumReader} over the
 * generated {@code ServiceError} class) cannot be preserved: the standalone
 * library has no {@code ExtendedReflectData} (spf4j's own lives in spf4j,
 * not in the library) and there is no generated class to reflect over. The
 * scenario — reading the real {@code bugRepro2.json} payload, whose nested
 * any content references the service-error schema by {@code $ref} id — is
 * re-expressed with a generic datum reader over the locally defined
 * schema, parsed with the resolver so the stamp carries it.</li>
 * </ul>
 *
 * @author Zoltan Farkas
 */
public class TestAnyLogicalType {

  private static Schema serviceErrorSchema;

  /**
   * The donor test's registry resolver, verbatim in behavior: resolves
   * service-schema ids to the service-error schema, derives ids from the
   * {@code mvnId} prop.
   */
  static final SchemaResolver SERVICE_RESOLVER = new SchemaResolver() {
    @Override
    public Schema resolveSchema(String id) {
      if (id.matches("org.spf4j.avro:service-schema:.*:6")) {
        return serviceErrorSchema();
      } else {
        throw new UnsupportedOperationException("Unknown schema: " + id);
      }
    }

    @Override
    public String getId(Schema schema) {
      return schema.getProp("mvnId");
    }
  };

  /**
   * Local re-expression of the generated {@code org.spf4j.service.avro.ServiceError}
   * and the {@code org.spf4j.base.avro} types it references (DebugDetail,
   * Throwable, StackSampleElement, Method, LogRecord), with the same field
   * names and the same payload shape as the real payloads carry. The
   * {@code payload} field is the any-typed field.
   */
  static synchronized Schema serviceErrorSchema() {
    if (serviceErrorSchema == null) {
      Schema schema = new Schema.Parser().parse(SERVICE_ERROR_AVSC);
      schema.addProp("mvnId", "org.spf4j.avro:service-schema:local:6");
      serviceErrorSchema = schema;
    }
    return serviceErrorSchema;
  }

  private static final String ANY_RECORD =
      "{ \"type\": \"record\", \"name\": \"AnyPayload\", \"logicalType\": \"any\", \"fields\": ["
      + " { \"name\": \"avsc\", \"type\": \"string\" },"
      + " { \"name\": \"content\", \"type\": \"bytes\" } ] }";

  private static final String SERVICE_ERROR_AVSC =
      "{ \"type\": \"record\", \"name\": \"ServiceError\", \"namespace\": \"org.spf4j.avro.logical_types.test\","
      + " \"fields\": ["
      + "  { \"name\": \"code\", \"type\": \"int\" },"
      + "  { \"name\": \"type\", \"type\": \"string\" },"
      + "  { \"name\": \"message\", \"type\": \"string\" },"
      + "  { \"name\": \"payload\", \"type\": [ \"null\", " + ANY_RECORD + " ], \"default\": null },"
      + "  { \"name\": \"detail\", \"type\": [ \"null\", "
      + "    { \"type\": \"record\", \"name\": \"DebugDetail\", \"fields\": ["
      + "      { \"name\": \"origin\", \"type\": \"string\" },"
      + "      { \"name\": \"logs\", \"type\": { \"type\": \"array\", \"items\": "
      + "        { \"type\": \"record\", \"name\": \"LogRecord\", \"fields\": ["
      + "          { \"name\": \"trId\", \"type\": \"string\" },"
      + "          { \"name\": \"level\", \"type\": \"string\" },"
      + "          { \"name\": \"ts\", \"type\": \"string\" },"
      + "          { \"name\": \"logger\", \"type\": \"string\" },"
      + "          { \"name\": \"thr\", \"type\": \"string\" },"
      + "          { \"name\": \"msg\", \"type\": \"string\" },"
      + "          { \"name\": \"xtra\", \"type\": [ \"null\", { \"type\": \"string\", \"logicalType\": \"json_any\" } ],"
      + "            \"default\": null } ] } }, \"default\": [] },"
      + "      { \"name\": \"throwable\", \"type\": [ \"null\", "
      + "        { \"type\": \"record\", \"name\": \"Throwable\", \"fields\": ["
      + "          { \"name\": \"className\", \"type\": \"string\" },"
      + "          { \"name\": \"message\", \"type\": [ \"null\", \"string\" ], \"default\": null },"
      + "          { \"name\": \"stackTrace\", \"type\": { \"type\": \"array\", \"items\": "
      + "            { \"type\": \"record\", \"name\": \"StackTraceElement\", \"fields\": ["
      + "              { \"name\": \"method\", \"type\": "
      + "                { \"type\": \"record\", \"name\": \"Method\", \"fields\": ["
      + "                  { \"name\": \"declaringClass\", \"type\": \"string\" },"
      + "                  { \"name\": \"name\", \"type\": \"string\" } ] } },"
      + "              { \"name\": \"location\", \"type\": [ \"null\", "
      + "                { \"type\": \"record\", \"name\": \"Location\", \"fields\": ["
      + "                  { \"name\": \"fileName\", \"type\": [ \"null\", \"string\" ], \"default\": null },"
      + "                  { \"name\": \"lineNumber\", \"type\": \"int\", \"default\": -1 } ] } ],"
      + "                \"default\": null },"
      + "              { \"name\": \"packageInfo\", \"type\": [ \"null\", "
      + "                { \"type\": \"record\", \"name\": \"PackageInfo\", \"fields\": ["
      + "                  { \"name\": \"url\", \"type\": [ \"null\", \"string\" ], \"default\": null } ] } ],"
      + "                \"default\": null } ] } }, \"default\": [] },"
      + "          { \"name\": \"cause\", \"type\": [ \"null\", \"Throwable\" ], \"default\": null } ] } ],"
      + "        \"default\": null },"
      + "      { \"name\": \"stackSamples\", \"type\": { \"type\": \"array\", \"items\": "
      + "        { \"type\": \"record\", \"name\": \"StackSampleElement\", \"fields\": ["
      + "          { \"name\": \"id\", \"type\": \"long\" },"
      + "          { \"name\": \"parentId\", \"type\": \"long\" },"
      + "          { \"name\": \"count\", \"type\": \"int\" },"
      + "          { \"name\": \"method\", \"type\": \"Method\" } ] } }, \"default\": [] } ] } ],"
      + "    \"default\": null } ] }";

  @Test
  public void testJsonRecord() throws IOException {
    Schema anyRecord = SchemaBuilder.record("test")
            .fields()
            .requiredString("avsc")
            .requiredBytes("content")
            .endRecord();
    AnyAvro.DEFAULT_INSTANCE.addToSchema(anyRecord);

    Schema testSchema = SchemaBuilder.builder().record("test_record").fields()
            .name("anyField").type(anyRecord)
            .noDefault()
            .endRecord();
    GenericData.Record record = new GenericData.Record(testSchema);
    record.put("anyField", "someString");
    String writeAvroExtendedJson = AvroTestIO.writeAvroExtendedJson(record);
    System.out.println(writeAvroExtendedJson);
    GenericRecord back = AvroTestIO.readAvroExtendedJson(new StringReader(writeAvroExtendedJson), testSchema);
    Assert.assertEquals(record.toString(), back.toString());

  }

  @Test
  public void testJsonRecord2() throws IOException {
    GenericData.Record record = createTestRecord();
    String writeAvroExtendedJson = AvroTestIO.writeAvroExtendedJson(record);
    System.out.println(writeAvroExtendedJson);
    GenericRecord back = AvroTestIO.readAvroExtendedJson(new StringReader(writeAvroExtendedJson), record.getSchema());
    Assert.assertEquals(record.toString(), back.toString());
  }

  @Test
  public void testJsonRecord3() throws IOException {
    GenericData.Record record = createTestRecord();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    AvroTestIO.writeAvroBin(bos, record);
    System.out.println(new String(bos.toByteArray(), StandardCharsets.UTF_8));
    GenericRecord back = (GenericRecord) AvroTestIO.readAvroBin(new ByteArrayInputStream(bos.toByteArray()),
            record.getSchema());
    Assert.assertEquals(record.toString(), back.toString());
  }

  public static GenericData.Record createTestRecord() {
    Schema anyRecord = SchemaBuilder.record("test")
            .fields()
            .requiredString("avsc")
            .requiredBytes("content")
            .endRecord();
    AnyAvro.DEFAULT_INSTANCE.addToSchema(anyRecord);
    Schema testSchema = SchemaBuilder.builder().record("test_record").fields()
            .name("someCrap").type(Schema.create(Schema.Type.BOOLEAN)).withDefault(true)
            .name("anyField").type(anyRecord).noDefault()
            .name("otherCrap").type(Schema.create(Schema.Type.STRING)).withDefault("bubu")
            .endRecord();
    GenericData.Record record = new GenericData.Record(testSchema);
    record.put("someCrap", true);
    record.put("anyField", Arrays.asList("someString"));
    record.put("otherCrap", "false");
    return record;
  }

  public static GenericData.Record createTestRecord2() {
    Schema anyRecord = SchemaBuilder.record("test")
            .fields()
            .requiredString("avsc")
            .requiredBytes("content")
            .endRecord();
    AnyAvro.DEFAULT_INSTANCE.addToSchema(anyRecord);
    Schema testSchema = SchemaBuilder.builder().record("test_record").fields()
            .name("someCrap").type(Schema.create(Schema.Type.BOOLEAN)).withDefault(true)
            .name("anyField").type(Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), anyRecord)))
            .noDefault()
            .name("otherCrap").type(Schema.create(Schema.Type.STRING)).withDefault("bubu")
            .endRecord();
    GenericData.Record record = new GenericData.Record(testSchema);
    record.put("someCrap", true);
    record.put("anyField", Arrays.asList("someString"));
    record.put("otherCrap", "false");
    return record;
  }

  @Test
  public void testJsonRecord2Json() throws IOException {
    GenericData.Record record = createTestRecord2();
    String writeAvroExtendedJson = AvroTestIO.writeAvroExtendedJson(record);
    System.out.println(writeAvroExtendedJson);
    GenericRecord back = AvroTestIO.readAvroExtendedJson(new StringReader(writeAvroExtendedJson), record.getSchema());
    Assert.assertEquals(record.toString(), back.toString());
  }

  @Test
  public void testJsonRecord2OJson() throws IOException {
    GenericData.Record record = createTestRecord2();
    String writeAvroJson = AvroTestIO.writeAvroJson(record);
    System.out.println(writeAvroJson);
    GenericRecord back = AvroTestIO.readAvroJson(new StringReader(writeAvroJson), record.getSchema());
    Assert.assertEquals(record.toString(), back.toString());
  }

  @Test
  public void testJsonRecord2Bin() throws IOException {
    GenericData.Record record = createTestRecord2();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    AvroTestIO.writeAvroBin(bos, record);
    System.out.println(new String(bos.toByteArray(), StandardCharsets.UTF_8));
    GenericRecord back = (GenericRecord) AvroTestIO.readAvroBin(new ByteArrayInputStream(bos.toByteArray()),
            record.getSchema());
    Assert.assertEquals(record.toString(), back.toString());
  }

  /**
   * Donor: read {@code testAnyJson.json} as the generated {@code HealthRecord}.
   * Re-expressed with a locally defined recursive schema covering the real
   * payload's shape; its log {@code attrs} maps carry raw-JSON values, read
   * through the {@code json_any} logical type (the payload contains no
   * {avsc, content} any-records). The pin is the same: the real payload
   * reads end-to-end through the extended JSON decoder.
   */
  @Test
  public void testHealthCheckCluster2Repro() throws IOException {
    Schema schema = new Schema.Parser().parse(HEALTH_RECORD_AVSC);
    Object back = AvroTestIO.readAvroExtendedJson(
            TestAnyLogicalType.class.getResourceAsStream("/testAnyJson.json"), schema);
    Assert.assertNotNull(back);
    GenericRecord health = (GenericRecord) back;
    Assert.assertEquals("spf4j.jaxrs.serverHost", health.get("origin").toString());
    List<?> components = (List<?>) health.get("componentsHealth");
    Assert.assertFalse(components.isEmpty());
    GenericRecord detail = (GenericRecord) ((GenericRecord) components.get(0)).get("detail");
    Assert.assertNotNull(detail);
    List<?> logs = (List<?>) detail.get("logs");
    Assert.assertFalse(logs.isEmpty());
    Map<?, ?> attrs = (Map<?, ?>) ((GenericRecord) logs.get(2)).get("attrs");
    Assert.assertTrue(attrs.keySet().stream().anyMatch(k -> "headers".equals(String.valueOf(k))));
  }

  private static final String HEALTH_RECORD_AVSC =
      "{ \"type\": \"record\", \"name\": \"HealthRecord\", \"namespace\": \"org.spf4j.avro.logical_types.test\","
      + " \"fields\": ["
      + "  { \"name\": \"origin\", \"type\": \"string\" },"
      + "  { \"name\": \"status\", \"type\": \"string\" },"
      + "  { \"name\": \"name\", \"type\": [ \"null\", \"string\" ], \"default\": null },"
      + "  { \"name\": \"componentsHealth\", \"type\": { \"type\": \"array\", \"items\": \"HealthRecord\" },"
      + "    \"default\": [] },"
      + "  { \"name\": \"detail\", \"type\": [ \"null\", "
      + "    { \"type\": \"record\", \"name\": \"HealthDetail\", \"fields\": ["
      + "      { \"name\": \"origin\", \"type\": \"string\" },"
      + "      { \"name\": \"logs\", \"type\": { \"type\": \"array\", \"items\": "
      + "        { \"type\": \"record\", \"name\": \"HealthLogRecord\", \"fields\": ["
      + "          { \"name\": \"trId\", \"type\": \"string\" },"
      + "          { \"name\": \"level\", \"type\": \"string\" },"
      + "          { \"name\": \"ts\", \"type\": \"string\" },"
      + "          { \"name\": \"logger\", \"type\": \"string\" },"
      + "          { \"name\": \"thr\", \"type\": \"string\" },"
      + "          { \"name\": \"msg\", \"type\": \"string\" },"
      + "          { \"name\": \"attrs\", \"type\": { \"type\": \"map\", \"values\": "
      + "            { \"type\": \"string\", \"logicalType\": \"json_any\" } }, \"default\": {} } ] } },"
      + "        \"default\": [] },"
      + "      { \"name\": \"stackSamples\", \"type\": { \"type\": \"array\", \"items\": "
      + "        { \"type\": \"record\", \"name\": \"HealthStackSample\", \"fields\": ["
      + "          { \"name\": \"id\", \"type\": \"long\" },"
      + "          { \"name\": \"parentId\", \"type\": \"long\" },"
      + "          { \"name\": \"count\", \"type\": \"int\" },"
      + "          { \"name\": \"method\", \"type\": "
      + "            { \"type\": \"record\", \"name\": \"HealthMethod\", \"fields\": ["
      + "              { \"name\": \"declaringClass\", \"type\": \"string\" },"
      + "              { \"name\": \"name\", \"type\": \"string\" } ] } } ] } }, \"default\": [] } ] } ],"
      + "    \"default\": null } ] }";

  /**
   * Donor: read {@code testAnyJson2.json} as an array of the generated
   * {@code MBeanAttributeInfo}. Re-expressed with a locally defined schema of
   * the same shape; the {@code descriptor} map values are any-records whose
   * content is null (the any-record here carries a nullable content field,
   * as the payload requires).
   */
  @Test
  public void testJMXAttrsRepro() throws IOException {
    Schema schema = Schema.createArray(new Schema.Parser().parse(MBEAN_ATTRIBUTE_INFO_AVSC));
    Object back = AvroTestIO.readAvroExtendedJson(
            TestAnyLogicalType.class.getResourceAsStream("/testAnyJson2.json"), schema);
    Assert.assertNotNull(back);
    List<?> attrs = (List<?>) back;
    Assert.assertEquals(5, attrs.size());
    GenericRecord first = (GenericRecord) attrs.get(0);
    Assert.assertEquals("ObjectPendingFinalizationCount", first.get("name").toString());
    Map<?, ?> descriptor = (Map<?, ?>) first.get("descriptor");
    Assert.assertFalse(descriptor.isEmpty());
  }

  private static final String MBEAN_ATTRIBUTE_INFO_AVSC =
      "{ \"type\": \"record\", \"name\": \"MBeanAttributeInfo\", \"namespace\": \"org.spf4j.avro.logical_types.test\","
      + " \"fields\": ["
      + "  { \"name\": \"name\", \"type\": \"string\" },"
      + "  { \"name\": \"type\", \"type\": \"string\" },"
      + "  { \"name\": \"avroSchema\", \"type\": \"string\" },"
      + "  { \"name\": \"description\", \"type\": \"string\" },"
      + "  { \"name\": \"isIs\", \"type\": [ \"null\", \"boolean\" ], \"default\": null },"
      + "  { \"name\": \"isWriteable\", \"type\": [ \"null\", \"boolean\" ], \"default\": null },"
      + "  { \"name\": \"descriptor\", \"type\": { \"type\": \"map\", \"values\": "
      + "    { \"type\": \"record\", \"name\": \"NullableAnyPayload\", \"logicalType\": \"any\", \"fields\": ["
      + "      { \"name\": \"avsc\", \"type\": \"string\" },"
      + "      { \"name\": \"content\", \"type\": [ \"null\", \"bytes\" ], \"default\": null } ] } },"
      + "    \"default\": {} } ] }";

  private static GenericRecord serviceError(final int code, final String type, final String message,
          final GenericRecord payload, final GenericRecord detail) {
    GenericRecord rec = new GenericData.Record(serviceErrorSchema());
    rec.put("code", code);
    rec.put("type", type);
    rec.put("message", message);
    rec.put("payload", payload);
    rec.put("detail", detail);
    return rec;
  }

  private static GenericRecord debugDetail(final String origin, final GenericRecord throwable,
          final List<GenericRecord> stackSamples) {
    Schema detailSchema = serviceErrorSchema().getField("detail").schema().getTypes().get(1);
    GenericRecord detail = new GenericData.Record(detailSchema);
    detail.put("origin", origin);
    detail.put("logs", new ArrayList<>());
    detail.put("throwable", throwable);
    detail.put("stackSamples", stackSamples);
    return detail;
  }

  private static GenericRecord throwable(final String className, final String message) {
    Schema detailSchema = serviceErrorSchema().getField("detail").schema().getTypes().get(1);
    Schema throwableSchema = detailSchema.getField("throwable").schema().getTypes().get(1);
    GenericRecord thr = new GenericData.Record(throwableSchema);
    thr.put("className", className);
    thr.put("message", message);
    thr.put("stackTrace", new ArrayList<>());
    thr.put("cause", null);
    return thr;
  }

  private static GenericRecord stackSample(final long id, final long parentId, final int count,
          final String declaringClass, final String methodName) {
    Schema detailSchema = serviceErrorSchema().getField("detail").schema().getTypes().get(1);
    Schema sampleSchema = detailSchema.getField("stackSamples").schema().getElementType();
    GenericRecord sample = new GenericData.Record(sampleSchema);
    sample.put("id", id);
    sample.put("parentId", parentId);
    sample.put("count", count);
    GenericRecord method = new GenericData.Record(sampleSchema.getField("method").schema());
    method.put("declaringClass", declaringClass);
    method.put("name", methodName);
    sample.put("method", method);
    return sample;
  }

  /**
   * Donor: specific-record round-trip of a nested generated
   * {@code ServiceError} chain via extended JSON, with the registry
   * resolver resolving the nested any content's schema by id. Re-expressed
   * with generic records over the local schema, and the resolver carried
   * by the {@link GenericData} IO context (design mechanism 2) instead of
   * the global registry.
   */
  @Test
  public void testServiceErrorParsing() throws IOException {
    GenericRecord err0 = serviceError(404, "404", "bla", null, null);
    GenericRecord err1 = serviceError(404, "404", "bla", err0,
            debugDetail("origin", throwable("aclass", "exception0"),
                    Arrays.asList(stackSample(0, 0, 1, "a", "b"))));
    GenericRecord err2 = serviceError(400, "400", "bla2", err1,
            debugDetail("origin", throwable("aclass", "exception"), Collections.emptyList()));
    GenericData data = new GenericData();
    data.setSchemaResolver(SERVICE_RESOLVER);
    String writeAvroExtendedJson = AvroTestIO.writeAvroExtendedJson(err2, data);
    System.out.println(writeAvroExtendedJson);
    GenericRecord back = AvroTestIO.readAvroExtendedJson(new StringReader(writeAvroExtendedJson),
            serviceErrorSchema(), data);
    Assert.assertEquals(err2.toString(), back.toString());
  }

  /**
   * Donor: reflect-path repro over {@code bugRepro2.json}
   * ({@code ExtendedReflectData.get().getSchema(ServiceError.class)} +
   * {@code ReflectDatumReader}). That path cannot be preserved — see the
   * class javadoc. Re-expressed: the schema is parsed with the resolver
   * (design mechanism 1, the parse-time stamp), read with a plain generic
   * datum reader, and the nested any content — referenced in the payload
   * by {@code {"$ref": "org.spf4j.avro:service-schema:1.0.0:6"}} — must
   * resolve through the stamp to the service-error schema.
   */
  @Test
  public void testServiceErrorParsing2() throws IOException {
    Schema schema = new Schema.Parser().setSchemaResolver(SERVICE_RESOLVER).parse(SERVICE_ERROR_AVSC);
    try (InputStream openStream = TestAnyLogicalType.class.getResourceAsStream("/bugRepro2.json")) {
      DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema, schema);
      Decoder decoder = new ExtendedJsonDecoder(schema, openStream);
      try {
        GenericRecord back = reader.read(null, decoder);
        Assert.assertNotNull(back);
        Assert.assertEquals(404, back.get("code"));
        GenericRecord payload = (GenericRecord) back.get("payload");
        Assert.assertNotNull(payload);
        Assert.assertEquals(404, payload.get("code"));
        Assert.assertEquals("javax.ws.rs.NotFoundException", payload.get("type").toString());
        System.out.println(back);
      } catch (RuntimeException ex) {
        System.out.println("Exception processing at " + decoder);
        throw ex;
      }
    }
  }
}
