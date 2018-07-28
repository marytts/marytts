package marytts.language.en;

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
		JPhonemiser phonemiser = (JPhonemiser) module;
		Assert.assertTrue(phonemiser.isPosPunctuation("."));
		Assert.assertTrue(phonemiser.isPosPunctuation(","));
		Assert.assertTrue(phonemiser.isPosPunctuation(":"));
		Assert.assertFalse(phonemiser.isPosPunctuation("NN"));
	}

	@Test
	public void testMaybePronounceable() {
		JPhonemiser phonemiser = (JPhonemiser) module;
		Assert.assertFalse(phonemiser.maybePronounceable(null, "NN"));
		Assert.assertFalse(phonemiser.maybePronounceable(null, "."));
		Assert.assertFalse(phonemiser.maybePronounceable("", "NN"));
		Assert.assertFalse(phonemiser.maybePronounceable("", "."));
		Assert.assertTrue(phonemiser.maybePronounceable("foo", "NN"));
		Assert.assertTrue(phonemiser.maybePronounceable("foo", "."));
		Assert.assertTrue(phonemiser.maybePronounceable("@", "NN"));
		Assert.assertFalse(phonemiser.maybePronounceable("@", "."));
	}
}
