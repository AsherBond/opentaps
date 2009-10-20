/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.common.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javolution.util.FastMap;
import org.apache.commons.validator.routines.CalendarValidator;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;


/**
 * UtilDate - A place for date helper methods.
 */
public abstract class UtilDate {

    private static final String MODULE = UtilDate.class.getName();

    // Utility class should not be instantiated.
    private UtilDate() { }

    /** Number of milliseconds in a day. */
    private static final long MS_IN_A_DAY = 24 * 60 * 60 * 1000;

    /**
     * JDBC escape format for java.sql.Date conversions.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * JDBC escape format for java.sql.Time conversions.
     */
    public static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * Default pattern that <code>getJsDateTimeFormat</code> can return in case of error or
     * if given pattern element isn't supported by jscalendar .
     */
    private final static String fallBackJSPattern = "%Y-%m-%d %H:%M:%S.0";

    /**
     * Parses a timestamp into fields suitable for selection of default values for AM/PM based form widgets.
     *
     *  Please note that this routine can also take a time-only or date-only string (in the localized format),
     *  under which case the returned date field will be set to null; this can be useful
     *  for setting a default time, without having to specify a default date.
     *
     * @param timestamp a <code>String</code> representing a timestamp
     * @param timeZone the <code>TimeZone</code> to use for conversion
     * @param locale the <code>Locale</code> to use for conversion
     * @return a <code>Map</code> containing the parsed values for "date", "hour", "minute" and "ampm", empty <code>Map</code> if there was a problem
     */
    public static Map<String, Object> timestampToAmPm(String timestamp, TimeZone timeZone, Locale locale) {

        Map<String, Object> map = FastMap.newInstance();

        int hour        = 12;
        int minute      = 0;
        String ampm     = "AM";
        String date     = null;
        boolean hasDate = false;
        boolean hasTime = false;

        if (UtilValidate.isEmpty(timestamp)) {
            return map;
        }

        try {
            // There are no robust algorithm to split date/time in unconditioned format.
            // Let's try convert timestamp using different patterns and see at result.
            ParsePosition pos = new ParsePosition(0);
            SimpleDateFormat df = new SimpleDateFormat(getTimeFormat(locale), locale);
            //df.setTimeZone(timeZone);
            Date dateObj = df.parse(timestamp, pos);
            if (dateObj != null) {
                hasTime = true;
            }
            if (dateObj == null) {
                pos.setIndex(0);
                String dateTimeFormat = getDateTimeFormat(locale);
                df = new SimpleDateFormat(dateTimeFormat, locale);
                //df.setTimeZone(timeZone);
                dateObj = df.parse(timestamp, pos);
                if (
                        dateObj != null &&
                        (
                                // validation method distinguish patterns with or without milliseconds placeholder
                                UtilDate.isDateTime(timestamp, dateTimeFormat, locale, timeZone) ||
                                UtilDate.isDateTime(timestamp, dateTimeFormat + ".S", locale, timeZone)
                        )
                ) {
                    hasTime = true;
                    hasDate = true;
                }
            }
            if (dateObj == null) {
                pos.setIndex(0);
                String dateFormat = getDateFormat(locale);
                df = new SimpleDateFormat(dateFormat, locale);
                //df.setTimeZone(timeZone);
                dateObj = df.parse(timestamp, pos);
                if (dateObj != null && UtilDate.isDateTime(timestamp, dateFormat, locale, timeZone)) {
                    hasDate = true;
                }
            }
            if (dateObj == null) {
                return map;
            }

            Calendar calendar = Calendar.getInstance(/*timeZone,*/ locale);
            calendar.setTime(dateObj);

            if (hasDate) {
                df = new SimpleDateFormat(getDateFormat(locale), locale);
                //df.setTimeZone(timeZone);
                date = df.format(calendar.getTime());
            }

            if (hasTime) {
                hour = calendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                minute = calendar.get(Calendar.MINUTE);
                if (calendar.get(Calendar.AM_PM) == Calendar.PM) {
                    ampm = "PM";
                }
            }

        } catch (IllegalArgumentException iae) {
            Debug.logWarning(iae.getLocalizedMessage(), MODULE);
            return map;
        }

        if (UtilValidate.isNotEmpty(date)) {
            map.put("date", date);
        }
        map.put("hour", new Integer(hour));
        map.put("ampm", ampm);
        map.put("minute", new Integer(minute));

        return map;
    }

    /**
     * Converts a <code>String</code> into a <code>Timestamp</code> value.
     * @param timestampString a timestamp <code>String</code> in JDBC timestamp escape (yyyy-MM-dd hh:mm:ss.fff) or ISO standard (yyyy-MM-dd) format
     * @param timeZone the <code>TimeZone</code> to use for conversion
     * @param locale the <code>Locale</code> to use for conversion
     * @return the <code>Timestamp</code> corresponding to the given <code>String</code>
     */
    public static Timestamp toTimestamp(String timestampString, TimeZone timeZone, Locale locale) {
        String dateFormat = getDateFormat(timestampString, locale);
        if (!UtilDate.isDateTime(timestampString, dateFormat, locale)) {
            // timestampString doesn't match pattern
            return null;
        }

        Date parsedDate = null;
        try {
            DateFormat df = UtilDateTime.toDateTimeFormat(dateFormat, timeZone, locale);
            parsedDate = df.parse(timestampString);
        } catch (ParseException e) {
            return null;
        }

        return new Timestamp(parsedDate.getTime());
    }

    /**
     * Returns the number of days between the beginning of two days.
     * This value is always positive.
     * @param one first <code>Timestamp</code> value
     * @param two second <code>Timestamp</code> value
     * @return the absolute value of the number of days between the two given <code>Timestamp</code>
     */
    public static Integer dateDifference(Timestamp one, Timestamp two) {
        Calendar first = Calendar.getInstance();
        Calendar second = Calendar.getInstance();
        first.setTime(one);
        second.setTime(two);

        // set to the beginning of the day
        first.set(Calendar.HOUR_OF_DAY, 0);
        first.set(Calendar.MINUTE, 0);
        first.set(Calendar.SECOND, 0);
        second.set(Calendar.HOUR_OF_DAY, 0);
        second.set(Calendar.MINUTE, 0);
        second.set(Calendar.SECOND, 0);

        double msdiff = first.getTimeInMillis() - second.getTimeInMillis();
        long days = Math.round(msdiff / MS_IN_A_DAY);
        return new Integer((int) Math.abs(days));
    }

    /**
     * Returns appropriate time format string.
     * @param locale User's locale, may be <code>null</code>
     * @return Time format string
     */
    public static String getTimeFormat(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        int timeStyle = -1;

        if (TIME_FORMAT == null || "DEFAULT".equals(TIME_FORMAT) || "SHORT".equals(TIME_FORMAT)) {
            timeStyle = DateFormat.SHORT;
        } else if ("MEDIUM".equals(TIME_FORMAT)) {
            timeStyle = DateFormat.MEDIUM;
        } else if ("LONG".equals(TIME_FORMAT)) {
            timeStyle = DateFormat.LONG;
        } else {
            return TIME_FORMAT;
        }

        SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(timeStyle, locale);
        return df.toPattern();
    }

    /**
     * Returns appropriate date + time format string.
     * @param locale User's locale, may be <code>null</code>.
     * @return Date/time format string
     */
    public static String getDateTimeFormat(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        int dateStyle = -1;
        if (DATE_FORMAT == null || "DEFAULT".equals(DATE_FORMAT) || "SHORT".equals(DATE_FORMAT)) {
            dateStyle = DateFormat.SHORT;
        } else if ("MEDIUM".equals(DATE_FORMAT)) {
            dateStyle = DateFormat.MEDIUM;
        } else if ("LONG".equals(DATE_FORMAT)) {
            dateStyle = DateFormat.LONG;
        }

        int timeStyle = -1;
        if (TIME_FORMAT == null || "DEFAULT".equals(TIME_FORMAT) || "SHORT".equals(TIME_FORMAT)) {
            timeStyle = DateFormat.SHORT;
        } else if ("MEDIUM".equals(TIME_FORMAT)) {
            timeStyle = DateFormat.MEDIUM;
        } else if ("LONG".equals(TIME_FORMAT)) {
            timeStyle = DateFormat.LONG;
        }

        if (dateStyle >= 0 && timeStyle >= 0) {
            SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
            return df.toPattern();
        }

        if (dateStyle >= 0 && timeStyle == -1) {
            SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat.getDateInstance(dateStyle, locale);
            return (df.toPattern() + " " + TIME_FORMAT);
        }

        if (dateStyle == -1 && timeStyle == -1) {
            return DATE_FORMAT + " " + TIME_FORMAT;
        }

        SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(timeStyle, locale);
        return DATE_FORMAT + " " + df.toPattern();
    }

    /**
     * Returns appropriate date format string.
     * @param locale User's locale, may be <code>null</code>
     * @return Date format string
     */
    public static String getDateFormat(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        int dateStyle = -1;

        if (DATE_FORMAT == null || "DEFAULT".equals(DATE_FORMAT) || "SHORT".equals(DATE_FORMAT)) {
            dateStyle = DateFormat.SHORT;
        } else if ("MEDIUM".equals(DATE_FORMAT)) {
            dateStyle = DateFormat.MEDIUM;
        } else if ("LONG".equals(DATE_FORMAT)) {
            dateStyle = DateFormat.LONG;
        } else {
            return DATE_FORMAT;
        }

        SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat.getDateInstance(dateStyle, locale);
        return df.toPattern();
    }

    /**
     * Returns a Calendar object initialized to the specified date/time, time zone,
     * and locale.
     *
     * @param date date/time to use
     * @param timeZone the timezone, optional, will use the default timezone if <code>null</code>
     * @param locale the locale, optional, will use the default locale if <code>null</code>
     * @return Calendar object
     * @see java.util.Calendar
     */
    public static Calendar toCalendar(Date date, TimeZone timeZone, Locale locale) {
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
            Debug.logWarning("Null timeZone, using default: " + timeZone, MODULE);
        }
        if (locale == null) {
            locale = Locale.getDefault();
            Debug.logWarning("Null locale, using default: " + locale, MODULE);
        }
        Calendar cal = Calendar.getInstance(timeZone, locale);

        if (date != null) {
            cal.setTime(date);
        }
        return cal;
    }

    /**
     * Method converts given date/time pattern in SimpleDateFormat style to form that can be used by
     * jscalendar.<br>Called from FTL and form widget rendering code for setup calendar.
     *
     * @param pattern Pattern to convert. Results of <code>getDate[Time]Format(locale)</code> as a rule.
     * @return Date/time format pattern that conforms to <b>jscalendar</b> requirements.
     */
    public static String getJsDateTimeFormat(String pattern) {
        if (UtilValidate.isEmpty(pattern)) {
            throw new IllegalArgumentException("UtilDate.getJsDateTimeFormat: Pattern string can't be empty.");
        }

        /*
         * The table contains translation rules.
         * Column number equals to placeholder length.
         * For example:
         *   Row  {"%m", "%m", "%b", "%B"},   // M (Month)
         * represents how we should translate following patterns
         *   "M" -> "%m", "MM" -> "%m", "MMM" -> "%b", "MMMM" -> "%B"
         *
         * Translation inpissible if array element equals to null.
         * This means usualy that jscalendar has no equivalent for some Java
         * pattern symbol and method returns fallBackJSPattern constant.
         */
        final String[][] translationTable = {
                {null, null, null, null},   // G (Era designator)
                {null, "%y", "%Y", "%Y"},   // y (Year)
                {"%m", "%m", "%b", "%B"},   // M (Month)
                {"%e", "%d", "%d", "%d"},   // d (Day in month)
                {null, null, null, null},   // k (Hour in day 1-24)
                {"%k", "%H", "%H", "%H"},   // H (Hour in day 0-23)
                {"%M", "%M", "%M", "%M"},   // m (Minute in hour)
                {"%S", "%S", "%S", "%S"},   // s (Second in minute)
                {null, null, null, null},   // S (Millisecond)
                {"%a", "%a", "%a", "%A"},   // E (Day in week)
                {"%j", "%j", "%j", "%j"},   // D (Day in year)
                {"%w", "%w", "%w", "%w"},   // F (Day of week in month)
                {"%W", "%W", "%W", "%W"},   // w (Week in year)
                {null, null, null, null},   // W (Week in month)
                {"%p", "%p", "%p", "%p"},   // a (Am/pm marker)
                {"%l", "%I", null, null},   // h (Hour in am/pm 1-12)
                {null, null, null, null},   // K (Hour in am/pm 0-11)
                {null, null, null, null},   // z (Time zone)
                {null, null, null, null}    // Z (Time zone/RFC-822)
        };

        String javaDateFormat = pattern;

        /* Unlocalized date/time pattern characters. */
        final String patternChars = "GyMdkHmsSEDFwWahKzZ";

        // all others chars in source string are separators between fields.
        List<String> tokens = Arrays.asList(javaDateFormat.split("[" + patternChars + "]"));
        String separators = "";
        Iterator<String> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            String token = iterator.next();
            if (UtilValidate.isNotEmpty(token) && separators.indexOf(token) == -1) {
                separators += token;
            }
        }

        // Going over pattern elements and replace it by those in translation table
        StringBuffer jsDateFormat = new StringBuffer();
        StringTokenizer tokenizer = new StringTokenizer(javaDateFormat, separators, true);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (UtilValidate.isEmpty(token)) {
                continue;
            }

            int index = patternChars.indexOf(token.charAt(0));
            if (index == -1) {
                // token is fixed part of pattern
                jsDateFormat.append(token);
                continue;
            }

            String jsPlaceholder = null;
            try {
                // token is placeholder that we should replce by equivalent from table
                jsPlaceholder = translationTable[index][token.length() - 1];
            } catch (IndexOutOfBoundsException e) {
                // specified Java pattern have some placeholder with length grater than supported
                Debug.logError(e, "Wrong placeholder [" + token + "] in date/time pattern. Probably too long, maximum 4 chars allowed.", MODULE);
                return fallBackJSPattern;
            }

            if (UtilValidate.isEmpty(jsPlaceholder)) {
                //Ouch! jscalendar doesn't support milliseconds but some parts of framework
                // require it. Just replace miiseconds with zero symbol.
                if (token.startsWith("S")) {
                    jsDateFormat.append("0");
                    continue;
                }
                // Source pattern contains something that we can't translate. Return fallback pattern.
                Debug.logError("Translation of date/time pattern [" + javaDateFormat + "] to jscalendar format is failed as jscalendar doesn't support placeholder [" + token + "]. Returns fallback pattern " + fallBackJSPattern, MODULE);
                return fallBackJSPattern;
            }

            // add new element to target pattern
            jsDateFormat.append(jsPlaceholder);
        }

        return jsDateFormat.toString();

    }

    /**
     * Verify if date/time string match pattern and is valid.
     * @param value a <code>String</code> value
     * @param pattern a <code>String</code> value
     * @param locale a <code>Locale</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isDateTime(String value, String pattern, Locale locale, TimeZone timeZone) {
        CalendarValidator validator = new CalendarValidator();
        if (timeZone == null) {
            return (validator.validate(value, pattern, locale) != null);
        }
        return (validator.validate(value, pattern, locale, timeZone) != null);
    }

    /**
     * Verify if date/time string match pattern and is valid.
     * @param value a <code>String</code> value
     * @param pattern a <code>String</code> value
     * @param locale a <code>Locale</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isDateTime(String value, String pattern, Locale locale) {
        return isDateTime(value, pattern, locale, null);
    }

    /**
     * Verify either date/time string conforms timestamp pattern.
     * @param value a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isTimestamp(String value) {
        if (value.length() == 10) {
            return isDateTime(value, "yyyy-MM-dd", Locale.getDefault());
        } else {
            return isDateTime(value, "yyyy-MM-dd HH:mm:ss.S", Locale.getDefault());
        }
    }

    /**
     * Converts an SQL <code>Timestamp</code> to an SQL <code>Date</code>.
     *
     * @param ts a <code>Timestamp</code> value
     * @return a <code>java.sql.Date</code> value
     */
    public static java.sql.Date timestampToSqlDate(Timestamp ts) {
        return new java.sql.Date(UtilDateTime.getDayStart(ts).getTime());
    }


    /**
     * Returns appropriate date format string, default using locale format, if it isn't match the date string, then try to get date format from date string.
     * @param dateString a <code>String</code> value
     * @param locale User's locale, may be <code>null</code>
     * @return Date format string
     */
    public static String getDateFormat(String dateString, Locale locale) {
        String dateFormat = getDateFormat(locale);
        if (UtilValidate.isEmpty(dateString))
            return dateFormat;
        if (dateString.indexOf(" ") != -1) {
            // Date and time in localized format
            dateFormat = getDateTimeFormat(locale);
        }
        if(!UtilDate.isDateTime(dateString, dateFormat, locale)) {
            if (UtilValidate.isEmpty(dateString)) {
                return null;
            }
            if (dateString.matches("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+$")) {
                // JDBC format
                dateFormat = "yyyy-MM-dd HH:mm:ss.S";
            } else if (dateString.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                // ISO format
                dateFormat = "yyyy-MM-dd";
            } else if (dateString.matches("^\\d{2}/\\d{2}/\\d{2}$")) {
                // MM/dd/yy
                dateFormat = "MM/dd/yy";
            } else if (dateString.matches("^\\d{2}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+$")) {
                // MM/dd/yy HH:mm:ss.S
                dateFormat = "MM/dd/yy HH:mm:ss.S";
            } else if (dateString.matches("\\w{3} \\w{3} \\d{2} \\d{4} \\d{2}:\\d{2}:\\d{2} \\S{8}$")) {
                // GWT Date input format1, example : Sat Oct 10 2009 00:00:00 GMT+0800
                dateFormat = "EEE MMM dd yyyy hh:mm:ss";
            } else if (dateString.matches("\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\S{8} \\d{4}$")) {
                // GWT Date input format2, example : Thu Oct 15 00:00:00 UTC+0800 2009
                dateFormat = "EEE MMM dd hh:mm:ss zZ yyyy";
            }
        }
        return dateFormat;
    }
}
