/**
 * 
 */
package marytts.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * @author marc
 *
 */
public class MaryConfigTest {

	@Test
	public void canCountConfigs() {
		// exercise
		int num = MaryConfig.countConfigs();
		// verify
		assertTrue(num >= 0);
	}
	
	@Test
	public void haveMainConfig() {
		MaryConfig m = MaryConfig.getMainConfig();
		assertNotNull(m);
	}
}
