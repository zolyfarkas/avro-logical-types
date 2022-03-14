
package org.spf4j.avro.logical_types.converters;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import org.apache.avro.Conversion;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.JsonExtensionDecoder;
import org.apache.avro.io.JsonExtensionEncoder;
import org.apache.avro.util.AppendableWriter;
import org.apache.avro.util.ByteArrayBuilder;
import org.apache.avro.util.CharSequenceReader;
import org.apache.avro.util.Optional;

/**
 *
 * @author Zoltan Farkas
 */
class JsonConversions<T> extends Conversion<T> {


  static {
    SimpleModule module = newJavaTimeModule();
    Schema.MAPPER.registerModule(module);
  }

  public static SimpleModule newJavaTimeModule() {
    SimpleModule module = new SimpleModule("extra");
    module.addSerializer(Instant.class, new JsonSerializer<Instant>() {
      @Override
      public void serialize(final Instant instant, final JsonGenerator gen, final SerializerProvider sp)
              throws IOException {
        gen.writeString(instant.toString());
      }
    });
    module.addDeserializer(Instant.class, new JsonDeserializer<Instant>() {
      @Override
      public Instant deserialize(final JsonParser parser, final DeserializationContext ctx)
              throws IOException, JsonProcessingException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
          return Instant.parse(parser.getText());
        } else {
          throw new JsonParseException(parser, "Expected instant as string, found " + parser.currentToken());
        }
      }
    });
    module.addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
      @Override
      public void serialize(final LocalDate date, final JsonGenerator gen, final SerializerProvider sp)
              throws IOException {
        gen.writeString(date.toString());
      }
    });
    module.addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
      @Override
      public LocalDate deserialize(JsonParser parser, DeserializationContext ctx)
              throws IOException, JsonProcessingException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
          return LocalDate.parse(parser.getText());
        } else {
          throw new JsonParseException(parser, "Expected local date as string, found " + parser.currentToken());
        }
      }
    });
    module.addSerializer(Duration.class, new JsonSerializer<Duration>() {
      @Override
      public void serialize(final Duration date, final JsonGenerator gen, final SerializerProvider sp)
              throws IOException {
        gen.writeString(date.toString());
      }
    });
    module.addDeserializer(Duration.class, new JsonDeserializer<Duration>() {
      @Override
      public Duration deserialize(JsonParser parser, DeserializationContext ctx)
              throws IOException, JsonProcessingException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
          return Duration.parse(parser.getText());
        } else {
          throw new JsonParseException(parser, "Expected duration as string, found " + parser.currentToken());
        }
      }
    });
    module.addSerializer(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
      @Override
      public void serialize(final ZonedDateTime date, final JsonGenerator gen, final SerializerProvider sp)
              throws IOException {
        gen.writeString(date.toString());
      }
    });
    module.addDeserializer(ZonedDateTime.class, new JsonDeserializer<ZonedDateTime>() {
      @Override
      public ZonedDateTime deserialize(JsonParser parser, DeserializationContext ctx)
              throws IOException, JsonProcessingException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
          return ZonedDateTime.parse(parser.getText());
        } else {
          throw new JsonParseException(parser, "Expected zoned date time as string, found " + parser.currentToken());
        }
      }
    });
    module.addSerializer(YearMonth.class, new JsonSerializer<YearMonth>() {
      @Override
      public void serialize(final YearMonth date, final JsonGenerator gen, final SerializerProvider sp)
              throws IOException {
        gen.writeString(date.toString());
      }
    });
    module.addDeserializer(YearMonth.class, new JsonDeserializer<YearMonth>() {
      @Override
      public YearMonth deserialize(JsonParser parser, DeserializationContext ctx)
              throws IOException, JsonProcessingException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
          return YearMonth.parse(parser.getText());
        } else {
          throw new JsonParseException(parser, "Expected zoned date time as string, found " + parser.currentToken());
        }
      }
    });
    module.addSerializer(Year.class, new JsonSerializer<Year>() {
      @Override
      public void serialize(final Year date, final JsonGenerator gen, final SerializerProvider sp)
              throws IOException {
        gen.writeNumber(date.getValue());
      }
    });
    module.addDeserializer(Year.class, new JsonDeserializer<Year>() {
      @Override
      public Year deserialize(JsonParser parser, DeserializationContext ctx)
              throws IOException, JsonProcessingException {
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
          return Year.of(parser.getIntValue());
        } else {
          throw new JsonParseException(parser, "Expected zoned date time as string, found " + parser.currentToken());
        }
      }
    });
    return module;
  }

  private final Class<T> clasz;

  private final String typeName;

  JsonConversions(String typeName, Class<T> clasz) {
    this.clasz = clasz;
    this.typeName = typeName;
  }

  @Override
  public Class<T> getConvertedType() {
    return this.clasz;
  }

  @Override
  public String getLogicalTypeName() {
    return typeName;
  }

  public Optional<T> tryDirectDecode(Decoder dec, Schema schema) throws IOException {
    if (dec instanceof JsonExtensionDecoder) {
      JsonExtensionDecoder pd = (JsonExtensionDecoder) dec;
      return Optional.of(pd.readValue(schema, clasz));
    } else {
      return Optional.empty();
    }
  }

  public boolean tryDirectEncode(T object, Encoder enc, Schema schema) throws IOException {
    if (enc instanceof JsonExtensionEncoder) {
      ((JsonExtensionEncoder) enc).writeValue(object, schema);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public ByteBuffer toBytes(T value, Schema schema, LogicalType type) {
    ByteArrayBuilder bab = new ByteArrayBuilder(16);
    try {
      Schema.MAPPER.writeValue(bab, value);
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot serialize " + value, ex);
    }
    return ByteBuffer.wrap(bab.getBuffer(), 0, bab.size());
  }

  @Override
  public CharSequence toCharSequence(T value, Schema schema, LogicalType type) {
    StringBuilder sb = new StringBuilder();
    try {
      Schema.MAPPER.writeValue(new AppendableWriter(sb), value);
      return sb;
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot serialize " + value, ex);
    }
  }

  @Override
  public T fromBytes(ByteBuffer value, Schema schema, LogicalType type) {
    try {
      if (value.hasArray()) {
        return Schema.MAPPER.readValue(Schema.FACTORY.createParser(value.array(), value.arrayOffset(),
                value.limit() - value.position()), clasz);
      } else {
        byte[] bytes = new byte[value.limit() - value.position()];
        ByteBuffer bb = value.duplicate();
        bb.get(bytes);
        return Schema.MAPPER.readValue(Schema.FACTORY.createParser(bytes), clasz);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot deserialize " + value, ex);
    }
  }

  @Override
  public T fromCharSequence(CharSequence value, Schema schema, LogicalType type) {
    try {
      return Schema.MAPPER.readValue(new CharSequenceReader(value), clasz);
    } catch (IOException ex) {
      throw new UncheckedIOException("Cannot deserialize: " + value + ", as " + clasz, ex);
    }
  }



}
