package marytts.language.en;

import marytts.modules.nlp.JPhonemiser;
import marytts.modules.ModuleRegistry;
import marytts.modules.MaryModule;
import marytts.config.MaryConfigurationFactory;
import marytts.tests.modules.MaryModuleTestCase;

// Reflection
import org.reflections.Reflections;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import org.testng.Assert;
import org.testng.annotations.*;


/**
 * @author ingmar
 */
public class JPhonemiserIT extends MaryModuleTestCase {

    public JPhonemiserIT() throws Exception {
        setup(true); // need mary startup
        module = ModuleRegistry.getModule(JPhonemiser.class);
	Assert.assertNotNull(module);
	MaryConfigurationFactory.getConfiguration("en_US").applyConfiguration(module);
    }

    @Test
    public void testIsPosPunctuation() {
        JPhonemiser phonemiser = (JPhonemiser) module;
	Assert.assertNotNull(phonemiser);
        Assert.assertTrue(phonemiser.isPosPunctuation("."));
        Assert.assertTrue(phonemiser.isPosPunctuation(","));
        Assert.assertTrue(phonemiser.isPosPunctuation(":"));
        Assert.assertFalse(phonemiser.isPosPunctuation("NN"));
    }

    @Test
    public void testMaybePronounceable() {
        JPhonemiser phonemiser = (JPhonemiser) module;
	Assert.assertNotNull(phonemiser);
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
