package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * Represents a TimeDuration token like "150ms", "2s", "1h", etc.
 * Converts to milliseconds internally.
 */
public class TimeDuration implements Token {
  private final long millis;
  private final String original;

  public TimeDuration(String value) {
    this.original = value;
    String input = value.trim().toLowerCase(Locale.ENGLISH);
    long multiplier = 1;

    if (input.endsWith("ms")) {
      multiplier = 1L;
      input = input.substring(0, input.length() - 2);
    } else if (input.endsWith("s")) {
      multiplier = 1000L;
      input = input.substring(0, input.length() - 1);
    } else if (input.endsWith("m")) {
      multiplier = 60L * 1000L;
      input = input.substring(0, input.length() - 1);
    } else if (input.endsWith("h")) {
      multiplier = 60L * 60L * 1000L;
      input = input.substring(0, input.length() - 1);
    } else if (input.endsWith("d")) {
      multiplier = 24L * 60L * 60L * 1000L;
      input = input.substring(0, input.length() - 1);
    } else if (input.endsWith("w")) {
      multiplier = 7L * 24L * 60L * 60L * 1000L;
      input = input.substring(0, input.length() - 1);
    } else if (input.endsWith("mo")) {
      multiplier = 30L * 24L * 60L * 60L * 1000L;
      input = input.substring(0, input.length() - 2);
    } else if (input.endsWith("y")) {
      multiplier = 365L * 24L * 60L * 60L * 1000L;
      input = input.substring(0, input.length() - 1);
    }

    // Convert numeric part to milliseconds
    this.millis = (long) (Double.parseDouble(input) * multiplier);
  }

  // Method to retrieve milliseconds
  public long getMillis() {
    return this.millis;
  }

  // Token interface: return value in ms
  @Override
  public Object value() {
    return this.millis;
  }

  // Token interface: return token type
  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  // Token interface: return JSON representation
  @Override
  public JsonElement toJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("type", "TIME_DURATION");
    obj.addProperty("original", this.original);
    obj.addProperty("millis", this.millis);
    return obj;
  }
}
