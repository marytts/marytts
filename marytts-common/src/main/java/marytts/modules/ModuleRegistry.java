/**
 * Copyright 2008 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import marytts.modules.MaryModule;

import marytts.MaryException;

import marytts.config.MaryConfigurationFactory;
import marytts.config.MaryConfiguration;
import marytts.exceptions.MaryConfigurationException;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;

import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.collections.keyvalue.MultiKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Reflections
import java.lang.Class;
import java.lang.reflect.Constructor;

/**
 *  The entry point containing the modules.
 *
 *  This class should be seen as a factory of modules which are classiied in 2 two ways
 *  (simultaneously): by configuration, by category and configuration. The category is here to help
 *  the user to restric the listing of modules when the registry is queried for this information
 *
 */
public class ModuleRegistry {

    /** The map which associates a list of modules to a configuration **/
    private static Map<String, List<MaryModule>> modules_by_conf;

    /** The map which associates the category to a configuration map listing the modules */
    private static Map<String, Map<String, List<MaryModule>>> modules_by_cat_and_conf;

    /** The logger of the registry */
    private static Logger logger;

    /**
     *  Default constructor which is private to approximate a singleton
     *
     */
    private ModuleRegistry() {
    }

    /**
     * Create a new, empty module repository.
     */
    static {
        modules_by_conf = new HashMap<String, List<MaryModule>>();
	modules_by_cat_and_conf =  new HashMap<String, Map<String, List<MaryModule>>>();
        logger = LogManager.getLogger(ModuleRegistry.class);
    }

    // ////////////////////////////////////////////////////////////////
    // /////////////////////// instantiation //////////////////////////
    // ////////////////////////////////////////////////////////////////

    /**
     *  Method which instanciate a given module based on a given configuration
     *
     *  @param conf the given configuration
     *  @param module_class_name the class name given to find the module
     *  @return the instanciated and initialised module
     *  @throws MaryException if anything is going wrong
     */
    protected static MaryModule instantiateModule(String conf, String module_class_name) throws
        MaryException {
        logger.info("Now initiating mary module '" + module_class_name + "' associated with configuration '" + conf + "'");

	try {
	    // Instantiate class
	    Class<?> clazz = Class.forName(module_class_name);
	    Constructor<?> ctor = clazz.getConstructor();
	    MaryModule m = (MaryModule) ctor.newInstance(new Object[] {});

	    // Apply the configuration
	    MaryConfiguration oconf = MaryConfigurationFactory.getConfiguration(conf);
	    m.setDefaultConfiguration(oconf);

	    return m;
	} catch (Exception ex) {
	    throw new MaryException("cannot instantiate module \"" + module_class_name + "\"", ex);
	}
    }

    // ////////////////////////////////////////////////////////////////
    // /////////////////////// registration ///////////////////////////
    // ////////////////////////////////////////////////////////////////

    /**
     *  Method to register a module to the registry given a specific configuration and the module class name
     *
     *  @param configuration the given configuration
     *  @param module_class_name the class name of the module to (create and) register
     *  @throws IllegalStateException
     *  @throws MaryException if a dedicated problem to mary happens
     */
    @SuppressWarnings("unchecked")
    public static synchronized void registerModule(String configuration, String module_class_name) throws IllegalStateException, MaryException {
        // 1. instantiate the module
        MaryModule module = instantiateModule(configuration, module_class_name);

        // 2. startup the module
        long before = System.currentTimeMillis();
        try {
            // Only start the modules here if in server mode:
            if (module.getState() != MaryModule.MODULE_OFFLINE) {
                throw new MaryException("The module is already started !");
            }

            module.startup();
            module.checkStartup();
        } catch (Throwable t) {
            throw new MaryException("Problem starting module " + module.getClass().getName(), t);
        }
        long after = System.currentTimeMillis();
        logger.debug("Starting module \"" + module.getClass().getName() + "\": " + (after - before) + " ms");

        // 3. Add it the modules_by_conf map
	if (!modules_by_conf.containsKey(configuration)) {
	    modules_by_conf.put(configuration, new ArrayList<MaryModule>());
	}
        modules_by_conf.get(configuration).add(module);

        // 4. Add it the modules_by_cat_and_conf map
	String cat = module.getCategory();
	if (!modules_by_cat_and_conf.containsKey(cat)) {
	    modules_by_cat_and_conf.put(cat, new HashMap<String, List<MaryModule>>());
	}

	Map<String, List<MaryModule>> cat_submap = modules_by_cat_and_conf.get(cat);
	if (! cat_submap.containsKey(configuration)) {
	    cat_submap.put(configuration, new ArrayList<MaryModule>());
	}

	modules_by_cat_and_conf.get(cat).get(configuration).add(module);
    }

    /**
     *  Method to register a module to the registry given the module class name on the default configuration
     *
     *  @param module_class_name the class name of the module to (create and) register
     *  @throws IllegalStateException
     *  @throws MaryException if a dedicated problem to mary happens
     */
    @SuppressWarnings("unchecked")
    public static synchronized void registerModule(String module_class_name) throws IllegalStateException, MaryException {
	registerModule(MaryConfigurationFactory.DEFAULT_KEY, module_class_name);
    }

    /**
     *  Method to remove a given module from the registry after shutting it down
     *
     *  @param configuration the given configuration
     *  @param module_class_name the class name of the module to (create and) register
     *  @throws IllegalStateException
     */
    public static synchronized void deregisterModule(MaryModule module) throws IllegalStateException {

        // Shutting down the module
        long before = System.currentTimeMillis();
        module.shutdown();
        long after = System.currentTimeMillis();
        logger.debug("Shutting down module \"" + module.getClass().getName() + "\": " + (after - before) + " ms");

        // Remove from the hash by configuration
	for (String conf: modules_by_conf.keySet()) {
            modules_by_conf.get(conf).remove(module);
        }

        // Remove from the hash by category and configuration
        Map<String, List<MaryModule>> local_modules_by_conf = modules_by_cat_and_conf.get(module.getCategory());
        for (String conf: local_modules_by_conf.keySet()) {
            List<MaryModule> list_modules = local_modules_by_conf.get(conf);
            local_modules_by_conf.get(conf).remove(module);
        }
    }

    /**
     *  Method to clear the module registry from any kind of entries !
     *
     *  @throws IllegalStateException
     */
    public static synchronized void clear() throws IllegalStateException {
	for (String conf: modules_by_conf.keySet()) {
	    for (MaryModule m: modules_by_conf.get(conf)) {
		if (m.getState() == MaryModule.MODULE_RUNNING) {
                    long before = System.currentTimeMillis();
		    m.shutdown();
                    long after = System.currentTimeMillis();
                    logger.debug("Shutting down module \"" + m.getClass().getName() + "\": " + (after - before) + " ms");
		}
	    }
        }

        // Clear maps
        modules_by_conf.clear();
        modules_by_cat_and_conf.clear();
    }

    // ////////////////////////////////////////////////////////////////
    // /////////////////////// modules /////////////////////////////
    // ////////////////////////////////////////////////////////////////

    /**
     * Provide a list containing all MaryModules instances. The order is not
     * important.
     *
     * @return Collections.unmodifiableList(all_modules)
     */
    public static Map<String, List<MaryModule>> listRegisteredModules() {
        return Collections.unmodifiableMap(modules_by_conf);
    }

    /**
     * Provide a map list modules by categories containing all MaryModules instances. The order is not
     * important.
     *
     * @throws IllegalStateException
     *             if called while registration is not yet complete.
     * @return Collections.unmodifiableList(all_modules)
     */
    public static Map<String, Map<String, List<MaryModule>>> listModulesByCategories() {
        return Collections.unmodifiableMap(modules_by_cat_and_conf);
    }

    /**
     * Find an active module by its class.
     *
     * @param moduleClass
     *            moduleClass
     * @return the module instance if found, or null if not found.
     * @throws IllegalStateException
     *             if called while registration is not yet complete.
     */
    public static MaryModule getDefaultModule(String class_name) throws ClassNotFoundException {
	Class<?> cls = Class.forName(class_name);

	return getModule(MaryConfigurationFactory.DEFAULT_KEY, cls);
    }

    /**
     * Find an active module by its class.
     *
     * @param moduleClass
     *            moduleClass
     * @return the module instance if found, or null if not found.
     * @throws IllegalStateException
     *             if called while registration is not yet complete.
     */
    public static MaryModule getModule(String configuration, Class<?> moduleClass) {
        for (Iterator<MaryModule> it = modules_by_conf.get(configuration).iterator(); it.hasNext();) {
            MaryModule m = it.next();
            if (moduleClass == m.getClass()) {
                return m;
            }
        }
        // Not found:
        return null;
    }
}
