/**
 * 
 */
package marytts.language.en;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;
import marytts.exceptions.MaryConfigurationException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

/**
 * @author marc
 *
 */
public class EnglishConfigTest {

	@Test
	public void isNotMainConfig() throws MaryConfigurationException {
		MaryConfig m = new EnglishConfig();
		assertFalse(m.isMainConfig());
	}
	
	@Test
	public void haveLanguageConfig() {
		assertTrue(MaryConfig.countLanguageConfigs() > 0);
	}
	
	@Test
	public void canGet() {
		MaryConfig m = MaryConfig.getLanguageConfig(Locale.US);
		assertNotNull(m);
		assertTrue(((LanguageConfig)m).getLocales().contains(Locale.US));
	}
	
	
	@Test
	public void hasEnglishLocale() throws MaryConfigurationException {
		LanguageConfig e = new EnglishConfig();
		assertTrue(e.getLocales().contains(Locale.US));
	}
	
	@Test(expected=MaryConfigurationException.class)
	public void requireLocale1() throws MaryConfigurationException {
		new LanguageConfig(new ByteArrayInputStream(new byte[0]));
	}
	
	@Test(expected=MaryConfigurationException.class)
	public void requireLocale2() throws MaryConfigurationException, IOException {
		Properties p = new Properties();
		p.setProperty("a", "b");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		p.store(baos, "");
		// exercise:
		new LanguageConfig(new ByteArrayInputStream(baos.toByteArray()));
	}
	
}
