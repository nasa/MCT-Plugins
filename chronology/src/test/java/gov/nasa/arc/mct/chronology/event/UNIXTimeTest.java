package gov.nasa.arc.mct.chronology.event;

import gov.nasa.arc.mct.chronology.ChronologyDomain;

import java.text.ParseException;
import java.util.Date;

import org.testng.Assert;
import org.testng.annotations.Test;

public class UNIXTimeTest {
	private static final long TEST_DATES[] = {  384584400000l,
		     -14182940000l ,
		      17537915655000l };
	
	
	private ChronologyDomain<UNIXTimeInstant> domain = UNIXTimeInstant.DOMAIN;
	
	@Test
	public void testInstant() {
		UNIXTimeInstant instant = new UNIXTimeInstant(1000);
		Assert.assertEquals(instant.getDomain(), domain);
		Assert.assertEquals(instant.getTimeMillis(), 1000);
	}
	
	@Test
	public void testDomainContains() {
		UNIXTimeInstant a = new UNIXTimeInstant(1000);
		UNIXTimeInstant b = new UNIXTimeInstant(10000);
		UNIXTimeInstant c = new UNIXTimeInstant(100000);
		UNIXTimeInstant d = new UNIXTimeInstant(1000000);
		ChronologicalInterval<UNIXTimeInstant> ab = new ChronologicalInterval<UNIXTimeInstant>(a,b);
		ChronologicalInterval<UNIXTimeInstant> bc = new ChronologicalInterval<UNIXTimeInstant>(b,c);
		ChronologicalInterval<UNIXTimeInstant> ac = new ChronologicalInterval<UNIXTimeInstant>(a,c);
		ChronologicalInterval<UNIXTimeInstant> cd = new ChronologicalInterval<UNIXTimeInstant>(c,d);
		
		Assert.assertFalse(domain.contains(bc, ac));
		Assert.assertTrue (domain.contains(ac, bc));
		Assert.assertFalse(domain.contains(ab, ac));
		Assert.assertTrue (domain.contains(ac, ab));
		
		Assert.assertFalse(domain.contains(ab, cd));
		Assert.assertFalse(domain.contains(cd, ab));
	}
	
	@Test
	public void testConvertToInstant() throws ParseException {
		
		for (long time : TEST_DATES) {
			Date d = new Date(time);
			String formatted = UNIXTimeInstant.DATE_FORMAT.format(d);
			
			UNIXTimeInstant i = domain.convertToInstant(formatted);
			Assert.assertEquals(i.getTimeMillis(), time);
		}
	}
	
	@Test (expectedExceptions = {ParseException.class})
	public void testConvertToInstantException() throws ParseException {
		domain.convertToInstant("Not a date value.");
	}
	
	@Test
	public void testComparator() {
		UNIXTimeInstant instants[] = new UNIXTimeInstant[TEST_DATES.length];
		
		for (int i = 0; i < TEST_DATES.length; i++) {
			instants[i] = new UNIXTimeInstant(TEST_DATES[i]);
		}
		
		for (int i = 0; i < TEST_DATES.length; i++) {
			for (int j = 0; j < TEST_DATES.length; j++) {
				long diff = TEST_DATES[i] - TEST_DATES[j];
				Assert.assertEquals(
						Long.signum(diff), 
						Integer.signum(domain.getComparator().compare(instants[i], instants[j])));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testSubInterval() {
		int steps = 10;
		int step  = 10000;
		
		UNIXTimeInstant[] instants = new UNIXTimeInstant[steps];
		for (int i = 0; i < steps; i++) {
			instants[i] = new UNIXTimeInstant(i * step);
		}
		
		ChronologicalInterval<UNIXTimeInstant>[][] intervals;
		intervals = new ChronologicalInterval[steps][];
		for (int i = 0; i < steps; i++) {
			intervals[i] = new ChronologicalInterval[steps];
			for (int j = 0; j < steps; j++) {
				intervals[i][j] = new ChronologicalInterval<UNIXTimeInstant>(instants[i], instants[j]);
			}
		}
		
		for (int i = 0; i < steps; i++) {
			for (int j = i + 3; j < steps; j++) {
				int diff = j - i;
				for (int low = 1; low < diff - 1; low++) {
					for (int high = low + 1; high < diff; high++) {
						double lowProportion  = (double) low  / (double) diff;
						double highProportion = (double) high / (double) diff;
						int    lowIndex       = i + low;
						int    highIndex      = i + high;
						
						ChronologicalInterval<UNIXTimeInstant> subInterval =
							domain.getSubInterval(intervals[i][j], lowProportion, highProportion);
						Assert.assertEquals(instants[lowIndex].getTimeMillis(), 
								subInterval.getStart().getTimeMillis());
						Assert.assertEquals(instants[highIndex].getTimeMillis(), 
								subInterval.getEnd().getTimeMillis());
					}
				}
			}
		}
		
		for (int i = 0; i < steps; i++) {
			for (int j = i + 1; j < steps; j++) {
				int diff = j - i;
				for (int k = 0; k <= diff; k++) {
					double p = (double) k / (double) diff;
					Assert.assertEquals(p,
							domain.locateBetween(instants[i+k], instants[i], instants[j]));
				}
			}
		}
		
	}
		
	
}
