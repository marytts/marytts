package marytts.language.it;

import marytts.language.it.JPhonemiser;
import marytts.modules.ModuleRegistry;
import marytts.modules.MaryModule;
import marytts.config.MaryConfiguration;
import marytts.config.MaryConfigurationFactory;
import marytts.tests.modules.MaryModuleTestCase;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * @author ingmar
 */
public class JPhonemiserIT extends MaryModuleTestCase {


    // public JPhonemiserIT() throws Exception {
    //     super(true); // need mary startup
    //     module = ModuleRegistry.getModule(JPhonemiser.class);
    // }

    @BeforeSuite(alwaysRun = true)
    public void setup() throws Exception {
        super.setup(true); // need mary startup
	for (MaryModule mod :ModuleRegistry.listRegisteredModules())
	    System.out.println(mod.getClass().toString());
        module = ModuleRegistry.getModule(JPhonemiser.class);
	Assert.assertNotNull(module);
	MaryConfigurationFactory.getConfiguration("default").applyConfiguration(module);
    }

    // @Test
    // public void testIsPosPunctuation() {
    // 	Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("FB"));
    // 	Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("FC"));
    // 	Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("FF"));
    // 	Assert.assertTrue(((JPhonemiser) module).isPosPunctuation("FS"));
    // 	Assert.assertFalse(((JPhonemiser) module).isPosPunctuation("NN"));
    // }
}
