package org.kulivatsky.autotests.core.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

/** Creates, shifts, parses, and formats date/time values used by test data and assertions. */
public final class DateUtils {
  /** No instances. */
  private DateUtils() {
  }

  /**
   * Reads the current instant from the system UTC clock.
   *
   * @return current UTC instant
   */
  public static Instant utcInstant() {
    return Clock.systemUTC().instant();
  }

  /**
   * Converts the current UTC instant to a legacy {@link Date} through a UTC local date-time.
   *
   * @return current date/time represented as {@link Timestamp}
   */
  public static Date utcDate() {
    return dateOf(utcInstant(), ZoneOffset.UTC);
  }

  /**
   * Adds a time-based duration to the current UTC instant.
   *
   * @param duration amount of seconds/nanos to add
   * @return resulting date/time
   */
  public static Date utcDatePlus(final Duration duration) {
    return dateOf(utcInstant().plus(duration), ZoneOffset.UTC);
  }

  /**
   * Adds a calendar-based period to the current UTC local date-time.
   *
   * <p>{@link Period} operates on calendar units such as days, months, and years; it is therefore
   * applied after converting the instant to UTC {@link LocalDateTime}.</p>
   *
   * @param period calendar period to add
   * @return resulting date/time
   */
  public static Date utcDatePlus(final Period period) {
    return Timestamp.valueOf(LocalDateTime.ofInstant(utcInstant(), ZoneOffset.UTC).plus(period));
  }

  /**
   * Subtracts a time-based duration from the current UTC instant.
   *
   * @param duration amount to subtract
   * @return resulting date/time
   */
  public static Date utcDateMinus(final Duration duration) {
    return dateOf(utcInstant().minus(duration), ZoneOffset.UTC);
  }

  /**
   * Subtracts a calendar-based period from the current UTC local date-time.
   *
   * @param period calendar period to subtract
   * @return resulting date/time
   */
  public static Date utcDateMinus(final Period period) {
    return Timestamp.valueOf(LocalDateTime.ofInstant(utcInstant(), ZoneOffset.UTC).minus(period));
  }

  /**
   * Represents the current instant as local date/time in a requested zone and returns it as a
   * legacy {@link Date}/{@link Timestamp} value.
   *
   * <p>Be aware that {@link Timestamp#valueOf(LocalDateTime)} stores the resulting local fields; use
   * {@link Instant} directly when absolute-instant semantics are required.</p>
   *
   * @param zoneId zone used for local conversion
   * @return converted date/time
   */
  public static Date dateInZone(final ZoneId zoneId) {
    return dateOf(utcInstant(), zoneId);
  }

  /**
   * Adds a duration to the current instant before converting it to a requested zone.
   *
   * @param zoneId zone used for local conversion
   * @param duration amount to add
   * @return converted date/time
   */
  public static Date dateInZonePlus(final ZoneId zoneId, final Duration duration) {
    return dateOf(utcInstant().plus(duration), zoneId);
  }

  /**
   * Subtracts a duration from the current instant before converting it to a requested zone.
   *
   * @param zoneId zone used for local conversion
   * @param duration amount to subtract
   * @return converted date/time
   */
  public static Date dateInZoneMinus(final ZoneId zoneId, final Duration duration) {
    return dateOf(utcInstant().minus(duration), zoneId);
  }

  /**
   * Converts a {@link LocalDateTime} to a JDBC {@link Timestamp} without applying a time zone.
   *
   * @param localDateTime local date/time fields
   * @return timestamp with the same local fields
   */
  public static Date dateOf(final LocalDateTime localDateTime) {
    return Timestamp.valueOf(localDateTime);
  }

  /**
   * Converts an instant through UTC local time.
   *
   * @param instant instant to convert
   * @return UTC-based timestamp representation
   */
  public static Date dateOf(final Instant instant) {
    return dateOf(instant, ZoneOffset.UTC);
  }

  /**
   * Converts an instant to local fields in a zone, then stores those fields in a
   * {@link Timestamp}.
   *
   * @param instant instant to convert
   * @param zoneId zone used to derive local fields
   * @return timestamp created from the zoned local date/time
   */
  public static Date dateOf(final Instant instant, final ZoneId zoneId) {
    return Timestamp.valueOf(LocalDateTime.ofInstant(instant, zoneId));
  }

  /**
   * Parses a legacy {@link Date} with {@link SimpleDateFormat}.
   *
   * <p>The formatter uses the JVM default locale/time zone because neither is overridden here.</p>
   *
   * @param format {@link SimpleDateFormat} pattern
   * @param value text to parse
   * @return parsed date
   * @throws IllegalArgumentException when parsing fails
   */
  public static Date dateOf(final String format, final String value) {
    try {
      return new SimpleDateFormat(format).parse(value);
    } catch (final ParseException ex) {
      throw new IllegalArgumentException("Unable to parse date '" + value + "' with format '" + format + "'", ex);
    }
  }

  /**
   * Parses an ISO-like calendar date using {@code yyyy-MM-dd}.
   *
   * @param value date text
   * @return parsed legacy date
   */
  public static Date dateOf(final String value) {
    return dateOf("yyyy-MM-dd", value);
  }

  /**
   * Formats a legacy date with {@link SimpleDateFormat}.
   *
   * @param date date to format
   * @param format formatter pattern
   * @return formatted text
   */
  public static String strDateOf(final Date date, final String format) {
    return new SimpleDateFormat(format).format(date);
  }

  /**
   * Formats a legacy date with millisecond precision and numeric zone offset.
   *
   * <p>The formatter uses the JVM default time zone.</p>
   *
   * @param date date to format
   * @return text using {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX}
   */
  public static String strDateOf(final Date date) {
    return strDateOf(date, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  }
}
