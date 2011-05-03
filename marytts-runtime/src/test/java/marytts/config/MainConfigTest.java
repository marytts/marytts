/**
 * 
 */
package marytts.config;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 * @author marc
 *
 */
public class MainConfigTest {

	private MaryConfig mc;
	
	@Before
	public void setUp() {
		mc = new MainConfig();
	}
	
	@Test
	public void isMainConfig() {
		assertTrue(mc.isMainConfig());
	}
}
