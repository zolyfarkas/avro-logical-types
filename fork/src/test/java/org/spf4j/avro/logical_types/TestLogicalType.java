package org.spf4j.avro.logical_types;

import java.util.LinkedHashMap;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.LogicalTypes;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaBuilder;
import org.spf4j.avro.generic.GenericData;
import org.spf4j.avro.generic.GenericRecord;
import org.junit.Assert;
import org.junit.Test;
import org.spf4j.avro.logical_types.JsonArray;
import org.spf4j.avro.logical_types.JsonRecord;

public class TestLogicalType {

  @Test
  public void testJsonRecord() throws IOException {
    Schema bytes = Schema.create(Schema.Type.BYTES);
    bytes.addProp(LogicalType.LOGICAL_TYPE_PROP, "json_record");
    LogicalType lt = LogicalTypes.fromSchema(bytes);
    lt.addToSchema(bytes);
    Schema bytes2 = Schema.create(Schema.Type.BYTES);
    bytes2.addProp(LogicalType.LOGICAL_TYPE_PROP, "json_array");
    LogicalTypes.fromSchema(bytes2).addToSchema(bytes2);

    Schema testSchema = SchemaBuilder.builder().record("test_record").fields()
            .name("jsonField").type(bytes)
            .withDefault("{}".getBytes(StandardCharsets.UTF_8))
            .name("jsonField2").type(bytes)
            .withDefault("{}".getBytes(StandardCharsets.UTF_8))
            .name("jsonField5").type(bytes2)
            .withDefault("[]".getBytes(StandardCharsets.UTF_8))
            .name("jsonField3").type(bytes)
            .withDefault("{}".getBytes(StandardCharsets.UTF_8)).endRecord();
    Map<String, Object> json = new LinkedHashMap<>();
    json.put("a", 3);
    json.put("b", "ty");
    GenericData.Record record = new GenericData.Record(testSchema);
    record.put("jsonField", json);
    record.put("jsonField2", Collections.EMPTY_MAP);
    record.put("jsonField3", json);
    record.put("jsonField5", Arrays.asList(1, "b"));
    String writeAvroExtendedJson = AvroTestIO.writeAvroExtendedJson(record);
    System.out.println(writeAvroExtendedJson);
    GenericRecord back = AvroTestIO.readAvroExtendedJson(new StringReader(writeAvroExtendedJson), testSchema);
    Assert.assertEquals(record, back);

  }

  @Test
  public void testJsonRecordLenientParse() throws IOException {
    Schema bytes = Schema.create(Schema.Type.BYTES);
    JsonRecord.instance().addToSchema(bytes);

    Schema bytes2 = Schema.create(Schema.Type.BYTES);
    JsonArray.instance().addToSchema(bytes2);

    Schema testSchema = SchemaBuilder.builder().record("test_record").fields()
            .name("jsonField").type(bytes)
            .withDefault("{}".getBytes(StandardCharsets.UTF_8))
            .name("jsonField2").type(bytes)
            .withDefault("{}".getBytes(StandardCharsets.UTF_8))
            .name("jsonField5").type(bytes2)
            .withDefault("[]".getBytes(StandardCharsets.UTF_8))
            .name("jsonField3").type(bytes)
            .withDefault("{}".getBytes(StandardCharsets.UTF_8)).endRecord();
    Map<String, Object> json = new LinkedHashMap<>();
    json.put("a", 3);
    json.put("b", "ty");
    GenericData.Record record = new GenericData.Record(testSchema);
    record.put("jsonField", json);
    record.put("jsonField2", Collections.EMPTY_MAP);
    record.put("jsonField3", json);
    record.put("jsonField5", Arrays.asList(1, "b"));
    String writeAvroJson = AvroTestIO.writeAvroJson(record);
    System.out.println(writeAvroJson);
    GenericRecord back = AvroTestIO.readAvroExtendedJson(new StringReader(writeAvroJson), testSchema);
    Assert.assertEquals(record, back);

  }

  @Test
  public void testDecimalWithNonByteArrayOrStringTypes() {
    // test simple types
    Schema[] nonBytes = new Schema[]{
      Schema.createRecord("Record", null, null, false),
      Schema.createArray(Schema.create(Schema.Type.BYTES)),
      Schema.createMap(Schema.create(Schema.Type.BYTES)),
      Schema.createEnum("Enum", null, null, Arrays.asList("a", "b")),
      Schema.create(Schema.Type.BOOLEAN), Schema.create(Schema.Type.INT),
      Schema.create(Schema.Type.LONG), Schema.create(Schema.Type.FLOAT),
      Schema.create(Schema.Type.DOUBLE), Schema.create(Schema.Type.NULL)};
    for (final Schema schema : nonBytes) {
      schema.addProp(LogicalType.LOGICAL_TYPE_PROP, "decimal");
      try {
        LogicalTypes.fromSchema(schema);
        Assert.fail("should not be able to create " + schema);
      } catch (RuntimeException ex) {
        // expected
      }

    }
  }

  public static void assertEqualsTrue(String message, Object o1, Object o2) {
    Assert.assertTrue("Should be equal (forward): " + message, o1.equals(o2));
    Assert.assertTrue("Should be equal (reverse): " + message, o2.equals(o1));
  }

  public static void assertEqualsFalse(String message, Object o1, Object o2) {
    Assert.assertFalse("Should be equal (forward): " + message, o1.equals(o2));
    Assert.assertFalse("Should be equal (reverse): " + message, o2.equals(o1));
  }

  /**
   * A convenience method to avoid a large number of @Test(expected=...) tests
   *
   * @param message A String message to describe this assertion
   * @param expected An Exception class that the Runnable should throw
   * @param containedInMessage A String that should be contained by the thrown exception's message
   * @param callable A Callable that is expected to throw the exception
   */
  public static void assertThrows(String message,
          Class<? extends Exception> expected,
          String containedInMessage,
          Callable callable) {
    try {
      callable.call();
      Assert.fail("No exception was thrown (" + message + "), expected: "
              + expected.getName());
    } catch (Exception actual) {
      Assert.assertEquals(message, expected, actual.getClass());
      Assert.assertTrue(
              "Expected exception message (" + containedInMessage + ") missing: "
              + actual.getMessage(),
              actual.getMessage().contains(containedInMessage)
      );
    }
  }
}
