package org.spf4j.avro.logical_types.converters;

import java.util.Map;

/**
 * @author Zoltan Farkas
 */
public class JsonRecordConversion extends JsonConversions<Map<String, Object>> {

  @SuppressWarnings("unchecked")
  public JsonRecordConversion() {
    super("json_record", (Class) Map.class);
  }

}
