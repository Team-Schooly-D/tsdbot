package org.tsd.tsdbot.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Stolen from http://kurtischiappone.com/blog/relative-date
 */
public class RelativeDate {

//    private static SimpleDateFormat sdf = new SimpleDateFormat( "h:mm a MMM dd, yyyy" );
    private static SimpleDateFormat sdf = new SimpleDateFormat( "EEE, MMM dd h:mm a z" );

    /**
     * This method computes the relative date according to
     * the Calendar being passed in and the number of years,
     * months, days, etc. that differ. This will compute both
     * past and future relative dates. E.g., "one day ago" and
     * "one day ago".
     * <p>
     * <strong>NOTE:</strong> If the calendar date relative
     * to "now" is older than one day, we display the actual date in
     * its default format as specified by this class. The date format
     * may be changed by calling {@link RelativeDate#setDateFormat(SimpleDateFormat)}
     * If you don't want to show the actual date, but you want to show
     * the relative date for days, months, and years, you can add the
     * other cases in by copying the logic for hours, minutes, seconds.
     *
     * @param Calendar calendar
     * @param int years
     * @param int months
     * @param int days
     * @param int hours
     * @param int minutes
     * @param int seconds
     * @return String representing the relative date
     */

    private static String computeRelativeDate( Calendar calendar, int years, int months, int days, int hours, int minutes, int seconds ) {

        String date = sdf.format( calendar.getTime() );

        // Year

        if ( years != 0 ) return date;

            // Month

        else if ( months != 0 ) return date;

            // Day

        else if ( days != 0 ) return date;

            // Hour

        else if ( hours == 1 ) return 1 + " hour ago";
        else if ( hours == -1 ) return 1 + " hour ago";
        else if ( hours > 0 ) return hours + " hours ago";
        else if ( hours < 0 ) return Math.abs( hours ) + " hours ago";

            // Minute

        else if ( minutes == 1 ) return 1 + " minute ago";
        else if ( minutes == -1 ) return 1 + " minute ago";
        else if ( minutes > 0 ) return minutes + " minutes ago";
        else if ( minutes < 0 ) return Math.abs( minutes ) + " minutes ago";

            // Second

        else if ( seconds == 1 ) return 1 + " second ago";
        else if ( seconds == -1 ) return 1 + " second ago";
        else if ( seconds > 0 ) return seconds + " seconds ago";
        else if ( seconds < 0 ) return Math.abs( seconds ) + " seconds ago";

            // Must be now (date and times are identical)

        else return "now";

    }

    /**
     * This method returns a String representing the relative
     * date by comparing the Calendar being passed in to the
     * date / time that it is right now.
     *
     * @param Calendar calendar
     * @return String representing the relative date
     */

    public static String getRelativeDate( Calendar calendar ) {

        Calendar now = GregorianCalendar.getInstance();

        int years = calendar.get( Calendar.YEAR ) - now.get( Calendar.YEAR );
        int months = calendar.get( Calendar.MONTH ) - now.get( Calendar.MONTH );
        int days = calendar.get( Calendar.DAY_OF_MONTH ) - now.get( Calendar.DAY_OF_MONTH );
        int hours = calendar.get( Calendar.HOUR_OF_DAY ) - now.get( Calendar.HOUR_OF_DAY );
        int minutes = calendar.get( Calendar.MINUTE ) - now.get( Calendar.MINUTE );
        int seconds = calendar.get( Calendar.SECOND ) - now.get( Calendar.SECOND );

        return computeRelativeDate( calendar, years, months, days, hours, minutes, seconds );

    }

    /**
     * This method returns a String representing the relative
     * date by comparing the Date being passed in to the date /
     * time that it is right now.
     *
     * @param Date date
     * @return String representing the relative date
     */

    public static String getRelativeDate( Date date ) {

        Calendar converted = GregorianCalendar.getInstance();
        converted.setTime( date );
        return getRelativeDate( converted );

    }

    /**
     * This method sets the date format. This is used when
     * the relative date is beyond one day. E.g., if the
     * relative date is > 1 day, we will display the date
     * in the format: h:mm a MMM dd, yyyy
     * <p>
     * This can be changed by passing in a new simple date
     * format and then calling {@link RelativeDate#getRelativeDate(Calendar)}
     * or {@link RelativeDate#getRelativeDate(Date)}.
     *
     * @param SimpleDateFormat dateFormat
     */

    public static void setDateFormat( SimpleDateFormat dateFormat ) {

        sdf = dateFormat;

    }

}
