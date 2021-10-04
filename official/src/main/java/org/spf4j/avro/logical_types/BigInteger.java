
package org.spf4j.avro.logical_types;

import org.apache.avro.LogicalType;

public class BigInteger extends LogicalType {

  public static final BigInteger INSTANCE = new BigInteger();

  public static BigInteger instance() {
    return INSTANCE;
  }


  public BigInteger() {
    super("bigint");
  }

}
