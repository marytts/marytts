package marytts.language.de;


// Reflection
import org.reflections.Reflections;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;

import marytts.config.MaryConfigLoader;
import marytts.config.MaryConfigurationFactory;
import marytts.language.de.JPhonemiser;
import marytts.modules.ModuleRegistry;
import marytts.tests.modules.MaryModuleTestCase;

import marytts.runutils.Mary;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * @author ingmar
 */
public class JPhonemiserIT extends MaryModuleTestCase {

    public JPhonemiserIT() throws Exception {
        super(true); // need mary startup
        module = ModuleRegistry.getModule(JPhonemiser.class);
	// Assert.assertNotNull(MaryConfigurationFactory.getConfiguration("de_DE"));
    }

    @Test
    public void testIsPosPunctuation() throws Exception {
        Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("$,"));
        Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("$."));
        Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("$("));
        Assert.assertFalse(((JPhonemiser) module).isPosPunctuation("NN"));
    }
}
