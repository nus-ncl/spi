package net.deterlab.testbed.api;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;

/**
 * Base class for any object that is visible in the API.  This is mainly a
 * container for static utility functions.
 * @author DETER team
 * @version 1.0
 */
public class ApiObject {
    /** The DateFormat for converting from/to java.util.Date objects */
    static private SimpleDateFormat format=
	new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    /** TimeZone to export dates in GMT */
    static TimeZone gmt = TimeZone.getTimeZone("GMT+0000");

    /* Initialize the formatter to use GMT */
    static {
	format.setTimeZone(gmt);
    }

    /**
     * Convert a Date to the string encoding used by ApiObjects.
     * Specifically yyyyMMddTHHmmssZ where the T and Z are constants.  The
     * times are in GMT.
     * @param v the Date to convert
     * @return the string encoding
     */
    public static String dateToString(Date v) {
	if ( v == null ) return null;

	StringBuffer vb = new StringBuffer();

	format.format(v, vb, new FieldPosition(0));
	return vb.toString();
    }

    /**
     * Convert a string encoded date to a Date.  Specifically yyyyMMddTHHmmssZ
     * where the T and Z are constants.  The
     * times are in GMT.
     * @param v the date string to convert
     * @return Date encoded by v
     */
    public static Date stringToDate(String v) {
	if (v == null) return null;
	else return format.parse(v, new ParsePosition(0));
    }

    /**
     * placeholder constructor
     */
    public ApiObject() { }

}
