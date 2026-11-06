
package org.spf4j.avro.logical_types.converters;

import java.net.URI;
import java.net.URISyntaxException;
import org.spf4j.avro.AvroRuntimeException;
import org.spf4j.avro.Conversion;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.Schema;

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
