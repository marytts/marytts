/**
 *
 */
package marytts.server;

import java.io.InputStream;

import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;


import org.testng.Assert;
import org.testng.annotations.*;


/**
 * The tests in this class rely on a special section for tests being present in marybase.config
 *
 * @author marc
 *
 */
public class MaryPropertiesIT {

	@Test(expectedExceptions = NoSuchPropertyException.class)
	public void failOnNonexistingProperty() throws Exception {
		MaryProperties.needProperty("test.property.nonexisting");
	}

	@Test(expectedExceptions = MaryConfigurationException.class)
	public void failOnNonexistingStream() throws Exception {
		MaryProperties.getStream("test.stream.nonexistant");
	}

	@Test
	public void succeedOnExistingStream() throws Exception {
		InputStream is = MaryProperties.getStream("test.stream.existant");
		Assert.assertNotNull(is);
	}

}
