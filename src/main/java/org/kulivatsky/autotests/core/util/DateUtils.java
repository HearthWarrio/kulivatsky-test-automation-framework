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

public final class DateUtils {
  private DateUtils() {
  }

  public static Instant utcInstant() {
    return Clock.systemUTC().instant();
  }

  public static Date utcDate() {
    return dateOf(utcInstant(), ZoneOffset.UTC);
  }

  public static Date utcDatePlus(final Duration duration) {
    return dateOf(utcInstant().plus(duration), ZoneOffset.UTC);
  }

  public static Date utcDatePlus(final Period period) {
    return Timestamp.valueOf(LocalDateTime.ofInstant(utcInstant(), ZoneOffset.UTC).plus(period));
  }

  public static Date utcDateMinus(final Duration duration) {
    return dateOf(utcInstant().minus(duration), ZoneOffset.UTC);
  }

  public static Date utcDateMinus(final Period period) {
    return Timestamp.valueOf(LocalDateTime.ofInstant(utcInstant(), ZoneOffset.UTC).minus(period));
  }

  public static Date dateInZone(final ZoneId zoneId) {
    return dateOf(utcInstant(), zoneId);
  }

  public static Date dateInZonePlus(final ZoneId zoneId, final Duration duration) {
    return dateOf(utcInstant().plus(duration), zoneId);
  }

  public static Date dateInZoneMinus(final ZoneId zoneId, final Duration duration) {
    return dateOf(utcInstant().minus(duration), zoneId);
  }

  public static Date dateOf(final LocalDateTime localDateTime) {
    return Timestamp.valueOf(localDateTime);
  }

  public static Date dateOf(final Instant instant) {
    return dateOf(instant, ZoneOffset.UTC);
  }

  public static Date dateOf(final Instant instant, final ZoneId zoneId) {
    return Timestamp.valueOf(LocalDateTime.ofInstant(instant, zoneId));
  }

  public static Date dateOf(final String format, final String value) {
    try {
      return new SimpleDateFormat(format).parse(value);
    } catch (final ParseException ex) {
      throw new IllegalArgumentException("Unable to parse date '" + value + "' with format '" + format + "'", ex);
    }
  }

  public static Date dateOf(final String value) {
    return dateOf("yyyy-MM-dd", value);
  }

  public static String strDateOf(final Date date, final String format) {
    return new SimpleDateFormat(format).format(date);
  }

  public static String strDateOf(final Date date) {
    return strDateOf(date, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  }
}
