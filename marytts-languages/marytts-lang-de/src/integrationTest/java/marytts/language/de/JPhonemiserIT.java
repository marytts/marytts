package marytts.language.de;

import marytts.language.de.JPhonemiser;
import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author ingmar
 */
public class JPhonemiserIT extends MaryModuleTestCase {

	public JPhonemiserIT() throws Exception {
		super(true); // need mary startup
		module = ModuleRegistry.getModule(JPhonemiser.class);
	}

	@Test
	public void testIsPosPunctuation() {
		Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("$,"));
		Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("$."));
		Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("$("));
		Assert.assertFalse(((JPhonemiser) module).isPosPunctuation("NN"));
	}
}
