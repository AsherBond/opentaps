/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opentaps.common.util;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javolution.util.FastMap;
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
    public static final long MS_IN_A_DAY = 24 * 60 * 60 * 1000;
    
    private static final String TIME_FORMAT = "HH:mm:ss";

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
            SimpleDateFormat df = new SimpleDateFormat(UtilDateTime.getTimeFormat(locale), locale);
            //df.setTimeZone(timeZone);
            Date dateObj = df.parse(timestamp, pos);
            if (dateObj != null) {
                hasTime = true;
            }
            if (dateObj == null) {
                pos.setIndex(0);
                String dateTimeFormat = UtilDateTime.getDateTimeFormat(locale);
                df = new SimpleDateFormat(dateTimeFormat, locale);
                //df.setTimeZone(timeZone);
                dateObj = df.parse(timestamp, pos);
                if (
                        dateObj != null &&
                        (
                                // validation method distinguish patterns with or without milliseconds placeholder
                                UtilValidate.isDateTime(timestamp, dateTimeFormat, locale, timeZone) ||
                                UtilValidate.isDateTime(timestamp, dateTimeFormat + ".S", locale, timeZone)
                        )
                ) {
                    hasTime = true;
                    hasDate = true;
                }
            }
            if (dateObj == null) {
                pos.setIndex(0);
                String dateFormat = UtilDateTime.getDateFormat(locale);
                df = new SimpleDateFormat(dateFormat, locale);
                //df.setTimeZone(timeZone);
                dateObj = df.parse(timestamp, pos);
                if (dateObj != null && UtilValidate.isDateTime(timestamp, dateFormat, locale, timeZone)) {
                    hasDate = true;
                }
            }
            if (dateObj == null) {
                return map;
            }

            Calendar calendar = Calendar.getInstance(/*timeZone,*/ locale);
            calendar.setTime(dateObj);

            if (hasDate) {
                df = new SimpleDateFormat(UtilDateTime.getDateFormat(locale), locale);
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
        if (!UtilValidate.isDateTime(timestampString, dateFormat, locale)) {
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
        String dateFormat = UtilDateTime.getDateFormat(locale);
        if (UtilValidate.isEmpty(dateString))
            return dateFormat;
        if (dateString.indexOf(" ") != -1) {
            // Date and time in localized format
            dateFormat = UtilDateTime.getDateTimeFormat(locale);
        }
        if(!UtilValidate.isDateTime(dateString, dateFormat, locale)) {
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

    /**
     * Returns the time string of time value.
     * @param time a <code>Time</code> value
     * @return a <code>String</code> value
     */
    public static String timeToString(Time time) {
    	DateFormat df = new SimpleDateFormat(TIME_FORMAT); 
    	return df.format(time);
    }
    
    
    /**
     * Returns the Time value of a time string.
     * @param timeString a <code>String</code> value
     * @return a <code>Time</code> value
     * @throws ParseException 
     */
    public static Time stringToTime(String timeString) throws ParseException {
    	DateFormat df = new SimpleDateFormat(TIME_FORMAT); 
    	return new Time(df.parse(timeString).getTime());
    }
}
