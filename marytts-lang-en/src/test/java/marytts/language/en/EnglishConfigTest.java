/**
 * 
 */
package marytts.language.en;

import java.util.Locale;

import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author marc
 *
 */
public class EnglishConfigTest {

	@Test
	public void isNotMainConfig() {
		MaryConfig m = new EnglishConfig();
		assertFalse(m.isMainConfig());
	}
	
	@Test
	public void haveLanguageConfig() {
		assertTrue(MaryConfig.countLanguageConfigs() > 0);
	}
	
	@Test
	public void canGet() {
		MaryConfig m = MaryConfig.getLanguageConfig(Locale.ENGLISH);
		assertNotNull(m);
		assertEquals(Locale.ENGLISH, ((LanguageConfig)m).getLocale());
	}
	
	
	@Test
	public void hasEnglishLocale() {
		LanguageConfig e = new EnglishConfig();
		assertEquals(Locale.ENGLISH, e.getLocale());
	}
}
