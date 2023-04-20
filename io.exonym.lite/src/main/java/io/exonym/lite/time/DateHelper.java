package io.exonym.lite.time;

import java.util.GregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;


public class DateHelper {
	
	public static DateTimeFormatter dhms = ISODateTimeFormat.dateHourMinuteSecond();
	public static DateTimeFormatter d = ISODateTimeFormat.date();
	
	public static GregorianCalendar getCurrentUtcTime(){
		DateTime dt = new DateTime(DateTimeZone.UTC);
		return dt.toGregorianCalendar();
		
	}

	public static long getCurrentUtcMillis() {
		return new DateTime(DateTimeZone.UTC).getMillis();

	}

	public static String currentIsoUtcDateTime(){
		return dhms.print(new DateTime(DateTimeZone.UTC)) + "Z";
		
	}
	
	public static String currentIsoUtcDate(){
		return d.print(new DateTime(DateTimeZone.UTC));
		
	}	

	public static String currentBareIsoUtcDate(){
		return d.print(new DateTime(DateTimeZone.UTC)).replaceAll("-", "");
		
	}

	public static String bareIsoUtcDate(DateTime target){
		return d.print(target).replaceAll("-", "");

	}

	public static String yesterdayBareIsoUtcDate(){
		DateTime yesterday = new DateTime(DateTimeZone.UTC);
		yesterday = yesterday.minusDays(1);
		return d.print(yesterday).replaceAll("-", "");

	}

	public static String isoUtcDateTime(DateTime dt){
		DateTime d0 = dt.withZone(DateTimeZone.UTC);
		return dhms.print(d0) + "Z";

	}

	public static String isoUtcDateTime(long msSince){
		DateTime d0 = new DateTime(msSince, DateTimeZone.UTC);
		return dhms.print(d0) + "Z";

	}

	public static String isoUtcDate(DateTime dt){
		DateTime d0 = dt.withZone(DateTimeZone.UTC);
		return d.print(d0);
		
	}


}
