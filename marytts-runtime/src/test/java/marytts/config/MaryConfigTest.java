/**
 * 
 */
package marytts.config;

import org.junit.Test;
import static org.junit.Assert.*;


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
