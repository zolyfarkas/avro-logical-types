
package org.spf4j.avro.logical_types.converters;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

/**
 *
 * @author Zoltan Farkas
 */
public class RegexpConverter extends Conversion<Pattern> {

  @Override
  public CharSequence toCharSequence(Pattern value, Schema schema, LogicalType type) {
    return value.toString();
  }

  @Override
  public Pattern fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    try {
      return Pattern.compile(value.toString());
    } catch (PatternSyntaxException ex) {
      throw new AvroRuntimeException("Invalid URL " + value, ex);
    }
  }

  @Override
  public Class<Pattern> getConvertedType() {
    return Pattern.class;
  }

  @Override
  public String getLogicalTypeName() {
    return "regexp";
  }


}
