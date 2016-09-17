/**
 * 
 */
package marytts.server;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The tests in this class rely on a special section for tests being present in marybase.config
 * 
 * @author marc
 *
 */
public class MaryPropertiesIT {

	@Test(expected = NoSuchPropertyException.class)
	public void failOnNonexistingProperty() throws Exception {
		MaryProperties.needProperty("test.property.nonexisting");
	}

	@Test(expected = MaryConfigurationException.class)
	public void failOnNonexistingStream() throws Exception {
		MaryProperties.getStream("test.stream.nonexistant");
	}

	@Test
	public void succeedOnExistingStream() throws Exception {
		InputStream is = MaryProperties.getStream("test.stream.existant");
		assertNotNull(is);
	}

}
