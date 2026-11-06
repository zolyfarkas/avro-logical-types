
package org.spf4j.avro.logical_types.converters;

import org.spf4j.avro.logical_types.converters.JsonAnyConversion;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaBuilder;
import org.spf4j.avro.generic.GenericData;
import org.spf4j.avro.io.BinaryDecoder;
import org.spf4j.avro.io.BinaryEncoder;
import org.spf4j.avro.io.DatumReader;
import org.spf4j.avro.io.DatumWriter;
import org.spf4j.avro.io.DecoderFactory;
import org.spf4j.avro.io.EncoderFactory;
import org.junit.Test;
import org.spf4j.avro.logical_types.JsonAny;


/**
 *
 * @author Zoltan Farkas
 */
 public class JsonAnyConversionTest {

  @Test
  public void testBackAndForth() {
    JsonAnyConversion conv = new JsonAnyConversion();
    Schema schema = Schema.create(Schema.Type.STRING);
    JsonAny lt = JsonAny.instance();
    lt.addToSchema(schema);
    CharSequence cs = conv.toCharSequence("127.0.0.1", schema, lt);
    String back = (String) conv.fromCharSequence(cs, schema, lt);
    Assert.assertEquals("127.0.0.1", back);
  }

  @Test
  public void testBackAndForthSer() throws IOException {
    Schema schema = Schema.create(Schema.Type.STRING);
    JsonAny lt = JsonAny.instance();
    lt.addToSchema(schema);
    Schema maoSchema = SchemaBuilder.map().values(schema);
    // The expected map is String-keyed, so the read must be configured
    // for String map keys: GenericData.setStringType is the documented
    // per-schema lever for exactly this (it is meaningful on map
    // schemas, for the keys), and both datum readers honor it. With no
    // such configuration a generic read yields Utf8 keys -- the
    // CharSequence default -- in this library and in the fork alike
    // (the fork's only other lever was the JVM-wide
    // avro.generic.default.stringClass system property, which this
    // test never set; verified against the fork jars, where this
    // scenario unconfigured also reads Utf8 keys and the maps do not
    // compare equal). A Utf8-keyed map never equals a String-keyed
    // one, so the configuration belongs to this test's setup, not to
    // its assertion, which is unchanged.
    GenericData.setStringType(maoSchema, GenericData.StringType.String);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    BinaryEncoder enc =  EncoderFactory.get().binaryEncoder(bos, null);
    DatumWriter dw =  GenericData.get().createDatumWriter(maoSchema);
    Map<String, String> data = new HashMap<>();
    data.put("bla", "127.0.0.1");
    dw.write(data, enc);
    enc.flush();
    ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
    BinaryDecoder bd =DecoderFactory.get().binaryDecoder(bin, null);
    DatumReader dr = GenericData.get().createDatumReader(maoSchema);
    Map<String, String> res =  (Map) dr.read(null, bd);
    Assert.assertEquals(data, res);
  }

}
