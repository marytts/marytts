package marytts.language.ru;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import com.ibm.icu.util.ULocale;
import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;



public class PreprocessTest {
	private static Preprocess module;
	
	@BeforeSuite
	public static void setUpBeforeClass() throws MaryConfigurationException {
		module = new Preprocess("Preprocess", MaryDataType.TEXT, MaryDataType.WORDS, new ULocale("ru_RU").toLocale());
	}
	
	@DataProvider(name = "CardinalNumberExpansion")
	private Object[][] expandingCardinalNumbers() {

		return new Object[][] {
				{ "1", "оди́н" },
				{ "10", "де́сять" },
				{ "15", "пятна́дцать" },
				{ "32", "три́дцать два" },
				{ "101", "сто оди́н" },
				{ "1032", "оди́н ты́сяча три́дцать два" },
				{ "10004", "де́сять ты́сяча четы́ре" },
				{ "100500", "сто ты́сяча пятьсо́т" },
				{ "1200000", "оди́н миллио́н две́сти ты́сяча" },
				{ "1003000005", "оди́н миллиард три миллио́н пять" }

		};

	}
	
	@Test(dataProvider = "CardinalNumberExpansion")
	public void testCardinal(String tokenised, String expected) throws Exception, NumberFormatException{
		String result = "No available expansion";
		result = module.expandCardinal(Double.parseDouble(tokenised));
		Assert.assertEquals(result, expected);
	}

}
