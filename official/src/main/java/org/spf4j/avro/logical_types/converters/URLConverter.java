
package org.spf4j.avro.logical_types.converters;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

/**
 *
 * @author Zoltan Farkas
 */
public class URLConverter extends Conversion<URL> {

  @Override
  public CharSequence toCharSequence(URL value, Schema schema, LogicalType type) {
    return value.toString();
  }

  @Override
  public URL fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    try {
    return new URL(value.toString());
    } catch (MalformedURLException ex) {
      throw new AvroRuntimeException("Invalid URL " + value, ex);
    }
  }

  @Override
  public Class<URL> getConvertedType() {
    return URL.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "url";
  }


}
