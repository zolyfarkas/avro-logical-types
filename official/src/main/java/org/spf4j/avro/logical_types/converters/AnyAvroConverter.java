
package org.spf4j.avro.logical_types.converters;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.util.ByteArrayBuilder;
import org.spf4j.avro.logical_types.AnyAvro;


public class AnyAvroConverter extends Conversion<Object> {

  @Override
  public Class<Object> getConvertedType() {
    return Object.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "any";
  }



  @Override
  public IndexedRecord toRecord(Object value, Schema rschema, LogicalType type) {
      Schema schema;
      if (value == null) {
        schema = Schema.create(Schema.Type.NULL);
      } else {
        schema = ReflectData.get().getSchema(value.getClass());
        if (schema == null) {
          schema = ReflectData.get().induce(value);
        }
        if (rschema.getLogicalType().equals(schema.getLogicalType())) {
          return (IndexedRecord) value;
        }
      }
      AnyAvro lt = (AnyAvro) rschema.getLogicalType();
      String strSchema = schema.toString();
      GenericRecord result = new GenericData.Record(rschema);
      result.put(lt.getAvscIdx(), strSchema);
      ByteArrayBuilder bos = new ByteArrayBuilder(32);
      DatumWriter writer = new ReflectDatumWriter(schema);
      Encoder encoder = EncoderFactory.get().binaryEncoder(bos, null);
      try {
        writer.write(value, encoder);
        encoder.flush();
      } catch (IOException | RuntimeException ex) {
        throw new AvroRuntimeException("Cannot serialize " + value, ex);
      }
      result.put(lt.getContentIdx(), ByteBuffer.wrap(bos.getBuffer(), 0, bos.size()));
      return result;
  }

  @Override
  public Object fromRecord(IndexedRecord rec, Schema rschema, LogicalType type) {
    AnyAvro lt = (AnyAvro) rschema.getLogicalType();
    CharSequence schema = (CharSequence) rec.get(lt.getAvscIdx());
    Schema sch = new Schema.Parser().parse(schema.toString());

    ByteBuffer bb = (ByteBuffer) rec.get(lt.getContentIdx());
    DatumReader reader = new GenericDatumReader(sch, sch);
    InputStream is = new ByteArrayInputStream(bb.array(),  bb.arrayOffset(), bb.limit() - bb.position());
    try {
      Decoder decoder = DecoderFactory.get().binaryDecoder(is, null);
      return reader.read(null, decoder);
    } catch (IOException | RuntimeException ex) {
      throw new AvroRuntimeException(this + " parsing failed for " + sch + ", from " + is, ex);
    }
  }




}
