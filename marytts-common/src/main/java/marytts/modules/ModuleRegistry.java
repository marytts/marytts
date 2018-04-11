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
 * A hierarchical repository for Mary modules, allowing the flexible indexing by
 * an ordered hierarchy of datatype, locale and voice. A given lookup will
 * search for a combination of datatype, locale and voice first; if it does not
 * find a value, it will look for datatype, locale, and null; if it does notfind
 * that, it will look for datatype, null, and null.
 *
 * @author marc
 *
 */
public class ModuleRegistry {
    private static Map<String, List<MaryModule>> modules_by_conf;
    private static Map<String, Map<String, List<MaryModule>>> modules_by_cat_and_conf;
    private static Logger logger;

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

    public static MaryModule instantiateModule(String conf, String module_class_name) throws
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


    public static MaryModule instantiateModule(String module_class_name) throws
        MaryException {
	return instantiateModule(MaryConfigurationFactory.DEFAULT_KEY, module_class_name);
    }

    // ////////////////////////////////////////////////////////////////
    // /////////////////////// registration ///////////////////////////
    // ////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    public static void registerModule(String configuration, MaryModule module) throws IllegalStateException {
	if (!modules_by_conf.containsKey(configuration)) {
	    modules_by_conf.put(configuration, new ArrayList<MaryModule>());
	}
        modules_by_conf.get(configuration).add(module);

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


    @SuppressWarnings("unchecked")
    public static void registerModule(MaryModule module) throws IllegalStateException {
	registerModule(MaryConfigurationFactory.DEFAULT_KEY, module);
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
