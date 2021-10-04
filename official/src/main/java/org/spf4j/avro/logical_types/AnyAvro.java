
package org.spf4j.avro.logical_types;

import org.apache.avro.LogicalType;
import org.apache.avro.Schema;


public class AnyAvro extends LogicalType {

  public static final AnyAvro DEFAULT_INSTANCE = new AnyAvro();

  private final int avscIdx;

  private final int contentIdx;

  public AnyAvro() {
    super("any");
    this.avscIdx = 0;
    this.contentIdx = 1;
  }

  public int getAvscIdx() {
    return avscIdx;
  }

  public int getContentIdx() {
    return contentIdx;
  }


}
