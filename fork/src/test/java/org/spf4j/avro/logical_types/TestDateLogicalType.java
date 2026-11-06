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

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.Month;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.LogicalTypes;
import org.spf4j.avro.Schema;
import org.spf4j.avro.SchemaBuilder;
import org.spf4j.avro.generic.GenericRecord;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Zoltan Farkas
 */
public class TestDateLogicalType {



  //"2017-06-20"
  @Test
  public void testDateStr() throws IOException {
    Schema strType = Schema.create(Schema.Type.STRING);
    strType.addProp(LogicalType.LOGICAL_TYPE_PROP, "date");
    strType.addProp("format", "yyyy-MM-dd");
    LogicalType lt = LogicalTypes.fromSchema(strType);
    lt.addToSchema(strType);
    Schema testSchema = SchemaBuilder.builder().record("test_record").fields()
            .name("date_fld").type(strType)
            .noDefault()
            .endRecord();
   GenericRecord rec = AvroTestIO.readAvroExtendedJson(
           new StringReader("{ \"date_fld\":\"2017-06-20\"}"), testSchema);
   System.out.println(rec);
   Assert.assertTrue(rec.get("date_fld") instanceof LocalDate);
  }


  //"2017-06-20"
  @Test
  public void testDateInt() throws IOException {
    Schema strType = Schema.create(Schema.Type.INT);
    strType.addProp(LogicalType.LOGICAL_TYPE_PROP, "date");
    LogicalType lt = LogicalTypes.fromSchema(strType);
    lt.addToSchema(strType);
    Schema testSchema = SchemaBuilder.builder().record("test_record").fields()
            .name("date_fld").type(strType)
            .noDefault()
            .endRecord();
   GenericRecord rec = AvroTestIO.readAvroExtendedJson(
           new StringReader("{ \"date_fld\":0}"), testSchema);
   System.out.println(rec);
   Object theDate = rec.get("date_fld");
   Assert.assertTrue(theDate instanceof LocalDate);
   Assert.assertEquals(LocalDate.of(1970, 1, 1), (LocalDate) theDate);
  }

//"2017-06-20"
  @Test
  public void testDateIntYmd() throws IOException {
    Schema strType = Schema.create(Schema.Type.INT);
    strType.addProp(LogicalType.LOGICAL_TYPE_PROP, "date");
    strType.addProp("ymd", Boolean.TRUE);
    LogicalType lt = LogicalTypes.fromSchema(strType);
    lt.addToSchema(strType);
    Schema testSchema = SchemaBuilder.builder().record("test_record").fields()
            .name("date_fld").type(strType)
            .noDefault()
            .endRecord();
   GenericRecord rec = AvroTestIO.readAvroExtendedJson(
           new StringReader("{ \"date_fld\":20170620}"), testSchema);
   System.out.println(rec);
   Object theDate = rec.get("date_fld");
   Assert.assertTrue(theDate instanceof LocalDate);
   Assert.assertEquals(LocalDate.of(2017, 06, 20), (LocalDate) theDate);
  }




}
