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
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;

/**
 * Creates, shifts, parses, and formats date/time values without changing an instant accidentally.
 *
 * <p>{@link Instant} is used for absolute points in time, {@link LocalDateTime} for timezone-free
 * calendar fields, and {@link ZonedDateTime} when a timezone is part of the value. Legacy
 * {@link Date} values are created from an {@code Instant}; converting an instant through local
 * fields is deliberately avoided because that silently shifts the represented moment.</p>
 */
public final class DateUtils {
  /** No instances. */
  private DateUtils() {
  }

  /** @return current instant from the system UTC clock */
  public static Instant utcInstant() {
    return Clock.systemUTC().instant();
  }

  /** @return current instant represented as legacy {@link Date} */
  public static Date utcDate() {
    return dateOf(utcInstant());
  }

  /**
   * Adds an exact duration to the current instant.
   *
   * @param duration amount to add
   * @return shifted legacy date
   */
  public static Date utcDatePlus(final Duration duration) {
    return dateOf(utcInstant().plus(Objects.requireNonNull(duration, "duration")));
  }

  /**
   * Adds a calendar period in UTC, then converts the resulting instant to {@link Date}.
   *
   * @param period calendar period to add
   * @return shifted legacy date
   */
  public static Date utcDatePlus(final Period period) {
    return dateOf(ZonedDateTime.ofInstant(utcInstant(), ZoneOffset.UTC)
      .plus(Objects.requireNonNull(period, "period"))
      .toInstant());
  }

  /**
   * Subtracts an exact duration from the current instant.
   *
   * @param duration amount to subtract
   * @return shifted legacy date
   */
  public static Date utcDateMinus(final Duration duration) {
    return dateOf(utcInstant().minus(Objects.requireNonNull(duration, "duration")));
  }

  /**
   * Subtracts a calendar period in UTC, then converts the resulting instant to {@link Date}.
   *
   * @param period calendar period to subtract
   * @return shifted legacy date
   */
  public static Date utcDateMinus(final Period period) {
    return dateOf(ZonedDateTime.ofInstant(utcInstant(), ZoneOffset.UTC)
      .minus(Objects.requireNonNull(period, "period"))
      .toInstant());
  }

  /**
   * Represents the current instant together with the requested timezone.
   *
   * @param zoneId timezone used for calendar fields and offset rules
   * @return current zoned date/time
   */
  public static ZonedDateTime zonedNow(final ZoneId zoneId) {
    return ZonedDateTime.ofInstant(utcInstant(), Objects.requireNonNull(zoneId, "zoneId"));
  }

  /**
   * Adds an exact duration to the current instant and represents it in the requested timezone.
   *
   * @param zoneId target timezone
   * @param duration amount to add
   * @return shifted zoned date/time
   */
  public static ZonedDateTime zonedNowPlus(final ZoneId zoneId, final Duration duration) {
    return ZonedDateTime.ofInstant(
      utcInstant().plus(Objects.requireNonNull(duration, "duration")),
      Objects.requireNonNull(zoneId, "zoneId")
    );
  }

  /**
   * Subtracts an exact duration from the current instant and represents it in the requested timezone.
   *
   * @param zoneId target timezone
   * @param duration amount to subtract
   * @return shifted zoned date/time
   */
  public static ZonedDateTime zonedNowMinus(final ZoneId zoneId, final Duration duration) {
    return ZonedDateTime.ofInstant(
      utcInstant().minus(Objects.requireNonNull(duration, "duration")),
      Objects.requireNonNull(zoneId, "zoneId")
    );
  }

  /**
   * Converts timezone-free local fields to a JDBC timestamp without applying any timezone.
   *
   * <p>Use {@link #dateOf(LocalDateTime, ZoneId)} when those fields represent local time in a
   * specific timezone.</p>
   *
   * @param localDateTime local calendar fields
   * @return timestamp with the same local fields
   */
  public static Date dateOf(final LocalDateTime localDateTime) {
    return Timestamp.valueOf(Objects.requireNonNull(localDateTime, "localDateTime"));
  }

  /**
   * Converts local calendar fields in a known timezone to an absolute legacy date.
   *
   * @param localDateTime local calendar fields
   * @param zoneId timezone that gives those fields an offset
   * @return date representing the resulting instant
   */
  public static Date dateOf(final LocalDateTime localDateTime, final ZoneId zoneId) {
    return dateOf(Objects.requireNonNull(localDateTime, "localDateTime")
      .atZone(Objects.requireNonNull(zoneId, "zoneId"))
      .toInstant());
  }

  /**
   * Converts an absolute instant to a legacy date without changing the represented moment.
   *
   * @param instant instant to convert
   * @return date representing exactly the same instant
   */
  public static Date dateOf(final Instant instant) {
    return Date.from(Objects.requireNonNull(instant, "instant"));
  }

  /**
   * Parses a legacy date with {@link SimpleDateFormat}.
   *
   * <p>The formatter uses the JVM default locale and timezone because neither is supplied by this
   * method. Lenient parsing is disabled, so invalid calendar dates are rejected rather than rolled
   * into a different month. Use the java.time API directly when parsing requires explicit timezone
   * semantics.</p>
   *
   * @param format {@link SimpleDateFormat} pattern
   * @param value text to parse
   * @return parsed date
   */
  public static Date dateOf(final String format, final String value) {
    final SimpleDateFormat formatter = new SimpleDateFormat(format);
    formatter.setLenient(false);
    try {
      return formatter.parse(value);
    } catch (final ParseException ex) {
      throw new IllegalArgumentException("Unable to parse date '" + value + "' with format '" + format + "'", ex);
    }
  }

  /** @return parsed date using {@code yyyy-MM-dd} */
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

  /** @return text using {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX} in the JVM default timezone */
  public static String strDateOf(final Date date) {
    return strDateOf(date, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  }
}
