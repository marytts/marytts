package marytts.language.it;

import marytts.modules.JPhonemiser;
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
		Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("FB"));
		Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("FC"));
		Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("FF"));
		Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("FS"));
		Assert.assertFalse(((JPhonemiser) module).isPosPunctuation("NN"));
	}
}
