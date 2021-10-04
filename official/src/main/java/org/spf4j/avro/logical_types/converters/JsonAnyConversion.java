/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.avro.logical_types.converters;

/**
 *
 * @author Zoltan Farkas
 */
public class JsonAnyConversion extends JsonConversions<Object> {

  public JsonAnyConversion() {
    super("json_any", Object.class);
  }

}
