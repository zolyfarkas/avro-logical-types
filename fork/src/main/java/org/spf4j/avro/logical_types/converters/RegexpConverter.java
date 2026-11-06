
package org.spf4j.avro.logical_types.converters;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.spf4j.avro.AvroRuntimeException;
import org.spf4j.avro.Conversion;
import org.spf4j.avro.LogicalType;
import org.spf4j.avro.Schema;

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
