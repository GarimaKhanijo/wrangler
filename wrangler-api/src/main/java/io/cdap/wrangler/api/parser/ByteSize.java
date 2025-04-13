package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * Represents a ByteSize token, e.g., "10KB", "1MB", "2GB"
 * Parses the token string and converts it into byte representation.
 */
public class ByteSize implements Token {
  private final long bytes;
  private final String original;

  public ByteSize(String value) {
    this.original = value;
    String input = value.trim().toUpperCase(Locale.ENGLISH);
    long multiplier = 1;

    // Determine multiplier based on suffix
    if (input.endsWith("KB")) {
      multiplier = 1024L;
      input = input.substring(0, input.length() - 2);
    } else if (input.endsWith("MB")) {
      multiplier = 1024L * 1024L;
      input = input.substring(0, input.length() - 2);
    } else if (input.endsWith("GB")) {
      multiplier = 1024L * 1024L * 1024L;
      input = input.substring(0, input.length() - 2);
    } else if (input.endsWith("TB")) {
      multiplier = 1024L * 1024L * 1024L * 1024L;
      input = input.substring(0, input.length() - 2);
    } else if (input.endsWith("B")) {
      input = input.substring(0, input.length() - 1);
    }

    // Convert numeric part to bytes
    this.bytes = (long) (Double.parseDouble(input) * multiplier);
  }

  // Method to retrieve the byte value
  public long getBytes() {
    return this.bytes;
  }

  // Token interface: return value in bytes
  @Override
  public Object value() {
    return this.bytes;
  }

  // Token interface: return token type
  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  // Token interface: return JSON representation
  @Override
  public JsonElement toJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("type", "BYTE_SIZE");
    obj.addProperty("original", this.original);
    obj.addProperty("bytes", this.bytes);
    return obj;
  }
}