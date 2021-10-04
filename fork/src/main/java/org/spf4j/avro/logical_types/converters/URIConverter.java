
package org.spf4j.avro.logical_types.converters;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

/**
 * @author Zoltan Farkas
 */
public class URIConverter extends Conversion<URI> {

  @Override
  public CharSequence toCharSequence(URI value, Schema schema, LogicalType type) {
    return value.toString();
  }

  @Override
  public URI fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    try {
      return new URI(value.toString());
    } catch (URISyntaxException ex) {
      throw new AvroRuntimeException("Invalid URI " + value, ex);
    }
  }

  @Override
  public Class<URI> getConvertedType() {
    return URI.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "uri";
  }


}
