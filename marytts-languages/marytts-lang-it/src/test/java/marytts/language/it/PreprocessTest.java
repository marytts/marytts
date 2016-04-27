package marytts.language.it;

import marytts.exceptions.MaryConfigurationException;
import marytts.language.it.preprocess.NumberEP;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.w3c.dom.Document;

class NumberEPT extends NumberEP {
	int matchT(String s, int t) {
		int type;
		type = match(s, t);
		return type;
	}

	int canDealWithT(String s, int t) {
		int type;
		type = canDealWith(s, t);
		return type;
	}

	String expandFloatT(String s) {
		String res = expandFloat(s);
		return res;
	}

	String expandIntegerT(long n) {
		String res = expandInteger(n);
		return res;
	}

	String expandDigitsT(String s) {
		String res = expandDigits(s);
		return res;
	}
}

public class PreprocessTest extends NumberEP {

	private static NumberEP module;

	@BeforeSuite
	public static void setUpBeforeClass() throws MaryConfigurationException {
		module = new NumberEP();
	}

	@DataProvider(name = "RealNumExpansion")
	private Object[][] expandingRealNumbers() {

		return new Object[][] {
				{ "1", "uno" },
				{ "10", "dieci" },
				{ "15", "quindici" },
				{ "32", "trenta due" },
				{ "101", "cento uno" },
				{ "1032", "mille trenta due" },
				{ "10004", "dieci mila quattro" },
				{ "100500", "cento mila cinque cento" },
				{ "1200000", "un milione due cento mila" },
				{ "1003000005", "un miliardo tre milioni cinque" },
				{ "120000000022", "cento venti miliardi venti due" },
				{ "1234567890123", "uno due tre quattro cinque sei sette otto nove zero uno due tre" },
				{ "12345678901234567", "uno due tre quattro cinque sei sette otto nove zero uno due tre quattro cinque sei sette" }

		};

	}

	@Test(dataProvider = "RealNumExpansion")
	public void testNumbers(String tokenised, String exWord) throws Exception, NumberFormatException {
		// TODO: How does mary converts input strings to mary data types.
		int type = match(tokenised, 0);
		int typeDeal = canDealWith(tokenised, type);
		String result = "No expansion";

		if (typeDeal == 1) {
			result = expandFloat(tokenised);
		} else if (typeDeal == 2) {
			result = expandInteger(tokenised);
		} else if (typeDeal == 5) {
			result = expandDigits(tokenised);
		} else {
			new NumberFormatException("Test For the number type not implemented");
		}

		Assert.assertEquals(result, exWord);

	}
}
