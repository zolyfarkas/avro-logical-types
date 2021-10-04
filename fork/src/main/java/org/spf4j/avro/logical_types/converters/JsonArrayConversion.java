package org.spf4j.avro.logical_types.converters;

import java.util.List;


/**
 * @author Zoltan Farkas
 */
public class JsonArrayConversion extends JsonConversions<List<Object>> {

  @SuppressWarnings("unchecked")
  public JsonArrayConversion() {
    super("json_array", (Class) List.class);
  }

}
