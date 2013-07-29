/*******************************************************************************
 * Mission Control Technologies, Copyright (c) 2009-2012, United States Government
 * as represented by the Administrator of the National Aeronautics and Space 
 * Administration. All rights reserved.
 *
 * The MCT platform is licensed under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License.
 *
 * MCT includes source code licensed under additional open source licenses. See 
 * the MCT Open Source Licenses file included with this distribution or the About 
 * MCT Licenses dialog available at runtime from the MCT Help menu for additional 
 * information. 
 *******************************************************************************/
package gov.nasa.arc.mct.scenario.util;

import java.text.ParseException;

// converts back and forth between strings and longs (i.e. 01:01:01 to time in ms)

/**
 * Utility class to support conversion of Durations from Strings to longs (where 
 * longs are time in ms.)
 * 
 * The JRE includes classes/interfaces for formatting Dates, but a date is 
 * somewhat different than a duration. For instance, "10 second past the epoch" 
 * is 001 00:00:10 as a date, but 000 00:00:10 as a duration (that is, 
 * people expect to see dates using 1-based indexing, whereas durations 
 * are 0-based quantities.)
 * 
 * This class expects and produces durations in the form:
 * 
 * "DAYS HOURS:MINUTES:SECONDS"
 * 
 * Where days or seconds may be omitted (and will be omitted during String 
 * conversion if they are 0.)
 * 
 * @author vwoeltje
 *
 */
public class DurationFormatter {
	private static final long MS_IN_SEC = 1000L;
	private static final long MS_IN_MIN = 	60L * MS_IN_SEC;
	private static final long MS_IN_HOUR = 	60L * MS_IN_MIN;
	private static final long MS_IN_DAY = 24L * MS_IN_HOUR;
	
	/**
	 * Convert a String representation of a duration to time in milliseconds.
	 * String should be in one of the following formats:
	 * 
	 * "DAYS HOURS:MINUTES:SECONDS"
	 * "HOURS:MINUTES:SECONDS"
	 * "HOURS:MINUTES"
	 * "DAYS HOURS:MINUTES"
	 * 
	 * @param duration the String representation of the duration
	 * @return time in milliseconds
	 * @throws ParseException thrown when String format is not recognized
	 */
	public static long parse(String duration) throws ParseException {
		long ms = 0;
		try {
			String[] dayTime = duration.split(" ");
			if (dayTime.length > 1) {
				// Parse days first, if they're there
				return Long.parseLong(dayTime[0]) * MS_IN_DAY + parse(dayTime[1]);
			} else {
				// Parse hours/minutes/seconds
				String[] hms = dayTime[0].split(":");
				for (int i = 0; i < 3; i++) { // Hours, Minutes, Seconds
					ms *= 60L; // Multiply running total by 60 (convert sec->min, min->hrs)
					if (hms.length > i) { // Ignore minutes/seconds if unspecified (treat as 0)
						ms += Long.parseLong(hms[i]) * 1000L; // Treat as seconds; subsequent passes will scale to min/hrs
					}
				}
			}
		} catch (Exception e) {
			throw new ParseException("Could not parse " + duration, 0);
		}
		return ms;		
	}
	
	/**
	 * Convert a duration to a String representation. Format is 
	 * "DAYS HOURS:MINUTES:SECONDS"
	 * 
	 * Note that either DAYS and/or SECONDS will be omitted if 
	 * they are 0. 
	 * 
	 * @param duration
	 * @return
	 */
	public static String formatDuration(long duration) {
		StringBuilder builder = new StringBuilder();
		if (duration > MS_IN_DAY) { // Don't show days if duration is less than a day
			builder.append(duration / MS_IN_DAY); // Integer divide to get days
			builder.append(' ');
		}
		
		duration %= MS_IN_DAY; // Modulo out days, leaving H:M:S
		appendTwoDigits(builder, duration / MS_IN_HOUR); // Integer divide to get hours
		builder.append(':');
		
		duration %= MS_IN_HOUR; // Modulo out hours, leaving M:S
		appendTwoDigits(builder, duration / MS_IN_MIN); // Integer divide to get minutes
		
		
		duration %= MS_IN_MIN; // Modulo out minutes, leaving seconds
		if (duration > 0) { // Don't show seconds if there are none
			builder.append(':');
			appendTwoDigits(builder, duration / MS_IN_SEC); // Integer divide to get seconds
		}
		
		return builder.toString();
	}
	
	/**
	 * Utility method to ensure two digits are always appended 
	 * (for hours/mins/secs formatting)
	 * @param b
	 * @param d
	 */
	private static void appendTwoDigits(StringBuilder b, long d) {
		if (d < 10) {
			b.append('0');
		}
		b.append(d);
	}
	
	public static void main(String[] arghs) { 
		System.out.println(formatDuration(310 * MS_IN_DAY + 30 * 60 * 60 * 1000L));
	}
}
