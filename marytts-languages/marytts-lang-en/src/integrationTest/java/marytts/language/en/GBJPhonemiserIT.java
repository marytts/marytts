package marytts.language.en;

import java.util.ArrayList;

import marytts.modules.nlp.JPhonemiser;
import marytts.language.en.GBJPhonemiser;
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
public class GBJPhonemiserIT extends MaryModuleTestCase {

    public GBJPhonemiserIT() throws Exception {
    }

    @BeforeSuite(alwaysRun = true)
    public void setup() throws Exception {
        super.setup(true); // need mary startup
	for (MaryModule mod :ModuleRegistry.listRegisteredModules())
	    System.out.println(mod.getClass().toString());
        module = ModuleRegistry.getModule(GBJPhonemiser.class);
	Assert.assertNotNull(module);
	MaryConfigurationFactory.getConfiguration("en_GB").applyConfiguration(module);
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

    @Test
    public void testLTS() {
        JPhonemiser phonemiser = (JPhonemiser) module;
	Assert.assertNotNull(phonemiser);
	StringBuilder sb = new StringBuilder();
	Assert.assertNotNull(phonemiser.phonemise("webmail", "NN", sb));
    }
}
