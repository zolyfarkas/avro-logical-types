
package org.spf4j.avro.logical_types;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.spf4j.avro.Schema;
import org.spf4j.avro.generic.ExtendedGenericDatumWriter;
import org.spf4j.avro.generic.GenericData;
import org.spf4j.avro.generic.GenericDatumReader;
import org.spf4j.avro.generic.GenericDatumWriter;
import org.spf4j.avro.generic.GenericRecord;
import org.spf4j.avro.io.DatumReader;
import org.spf4j.avro.io.DatumWriter;
import org.spf4j.avro.io.Decoder;
import org.spf4j.avro.io.DecoderFactory;
import org.spf4j.avro.io.Encoder;
import org.spf4j.avro.io.EncoderFactory;
import org.spf4j.avro.io.ExtendedJsonDecoder;

/**
 * Test-tree replacement for the fork core's {@code AvroUtils} entry points
 * (not ported as production API of the standalone library): the same
 * extended-JSON / stock-JSON / binary datum IO, expressed directly on the
 * library's datum APIs. Extended-JSON writes go through
 * {@link ExtendedGenericDatumWriter} exactly like the donor helper did.
 */
public final class AvroTestIO {

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  static {
    // The extended-JSON write path hands whole values (maps, RawValues,
    // JsonNodes) to JsonGenerator.writeObject through
    // JsonExtensionEncoder.writeValue, which requires an ObjectCodec on
    // the generator; a bare JsonFactory provides none.
    JSON_FACTORY.setCodec(new ObjectMapper());
  }

  /**
   * The datum model the donor {@code AvroUtils} effectively used: the
   * fork's {@code GenericData} singleton carried the default conversions
   * (it even had clearConversions()/defaultConversions() methods to reset
   * them). The standalone library's {@code GenericData} carries only
   * ServiceLoader conversions, so the defaults are added explicitly here
   * through the library's own opt-in, {@code Conversions.defaultConversions()}.
   */
  static GenericData defaultData() {
    GenericData data = new GenericData();
    for (org.spf4j.avro.Conversion<?> conversion : org.spf4j.avro.Conversions.defaultConversions().values()) {
      data.addLogicalTypeConversion(conversion);
    }
    return data;
  }

  private AvroTestIO() {
  }

  public static String writeAvroExtendedJson(final GenericRecord req) throws IOException {
    return writeAvroExtendedJson(req, defaultData());
  }

  public static String writeAvroExtendedJson(final GenericRecord req, final GenericData data) throws IOException {
    StringWriter writer = new StringWriter();
    writeAvroExtendedJson(writer, req, data);
    return writer.toString();
  }

  public static void writeAvroExtendedJson(final Writer out, final GenericRecord req) throws IOException {
    writeAvroExtendedJson(out, req, defaultData());
  }

  public static void writeAvroExtendedJson(final Writer out, final GenericRecord req, final GenericData data)
          throws IOException {
    Schema schema = req.getSchema();
    DatumWriter<GenericRecord> writer = new ExtendedGenericDatumWriter<>(schema, data);
    Encoder encoder = new org.spf4j.avro.io.ExtendedJsonEncoder(schema, JSON_FACTORY.createGenerator(out));
    writer.write(req, encoder);
    encoder.flush();
  }

  public static void writeAvroExtendedJson(final OutputStream out, final GenericRecord req) throws IOException {
    Schema schema = req.getSchema();
    DatumWriter<GenericRecord> writer = new ExtendedGenericDatumWriter<>(schema, defaultData());
    Encoder encoder = new org.spf4j.avro.io.ExtendedJsonEncoder(schema, JSON_FACTORY.createGenerator(out));
    writer.write(req, encoder);
    encoder.flush();
  }

  public static GenericRecord readAvroExtendedJson(final Reader input, final Schema schema) throws IOException {
    return readAvroExtendedJson(input, schema, defaultData());
  }

  public static GenericRecord readAvroExtendedJson(final Reader input, final Schema schema, final GenericData data)
          throws IOException {
    DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema, schema, data);
    Decoder decoder = new ExtendedJsonDecoder(schema, JSON_FACTORY.createParser(input), true);
    return reader.read(null, decoder);
  }

  public static Object readAvroExtendedJson(final InputStream input, final Schema schema) throws IOException {
    return readAvroExtendedJson(input, schema, defaultData());
  }

  public static Object readAvroExtendedJson(final InputStream input, final Schema schema, final GenericData data)
          throws IOException {
    DatumReader<Object> reader = new GenericDatumReader<>(schema, schema, data);
    Decoder decoder = new ExtendedJsonDecoder(schema, input);
    return reader.read(null, decoder);
  }

  public static String writeAvroJson(final GenericRecord req) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    Schema schema = req.getSchema();
    DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
    Encoder encoder = EncoderFactory.get().jsonEncoder(schema, bos);
    writer.write(req, encoder);
    encoder.flush();
    return bos.toString(StandardCharsets.UTF_8);
  }

  public static GenericRecord readAvroJson(final Reader input, final Schema schema) throws IOException {
    DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
    String text = readAll(input);
    Decoder decoder = DecoderFactory.get().jsonDecoder(schema, text);
    return reader.read(null, decoder);
  }

  public static void writeAvroBin(final OutputStream out, final GenericRecord req) throws IOException {
    DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(req.getSchema());
    Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
    writer.write(req, encoder);
    encoder.flush();
  }

  public static Object readAvroBin(final InputStream input, final Schema schema) throws IOException {
    DatumReader<Object> reader = new GenericDatumReader<>(schema);
    Decoder decoder = DecoderFactory.get().binaryDecoder(input, null);
    return reader.read(null, decoder);
  }

  private static String readAll(final Reader input) throws IOException {
    StringWriter sw = new StringWriter();
    char[] buf = new char[4096];
    int read;
    while ((read = input.read(buf)) >= 0) {
      sw.write(buf, 0, read);
    }
    return sw.toString();
  }
}
