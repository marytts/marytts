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
import marytts.modules.MaryModule;

import marytts.MaryException;
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
    private static List<MaryModule> allModules;
    private static Map<String, List<MaryModule>> module_by_categories;
    private static Logger logger;

    private ModuleRegistry() {
    }

    /**
     * Create a new, empty module repository.
     */
    static {
        allModules = new ArrayList<MaryModule>();
        logger = LogManager.getLogger(ModuleRegistry.class);
    }

    // ////////////////////////////////////////////////////////////////
    // /////////////////////// instantiation //////////////////////////
    // ////////////////////////////////////////////////////////////////

    public static MaryModule instantiateModule(String moduleInitInfo) throws
        MaryException {
        logger.info("Now initiating mary module '" + moduleInitInfo + "'");

	try {
	    Class<?> clazz = Class.forName(moduleInitInfo);
	    Constructor<?> ctor = clazz.getConstructor();
	    MaryModule m = (MaryModule) ctor.newInstance(new Object[] {});
	    return m;
	} catch (Exception ex) {
	    throw new MaryException("cannot instantiate module \"" + moduleInitInfo + "\"", ex);
	}
    }

    // ////////////////////////////////////////////////////////////////
    // /////////////////////// registration ///////////////////////////
    // ////////////////////////////////////////////////////////////////

    /**
     * Register a MaryModule as an appropriate module to process the given
     * combination of UtteranceType for the input data, locale of the input data,
     * and voice requested for processing. Note that it is possible to register
     * more than one module for a given combination of input type, locale and
     * voice; in that case, all of them will be remembered, and will be returned
     * as a List by get().
     *
     * @param module
     *            the module to add to the registry, under its input type and
     *            the given locale and voice.
     * @param locale
     *            the locale (language or language-COUNTRY) of the input data;
     *            can be null to signal that the module is locale-independent.
     * @throws IllegalStateException
     *             if called after registration is complete.
     */
    @SuppressWarnings("unchecked")
    public static void registerModule(MaryModule module) throws IllegalStateException {
        allModules.add(module);
    }

    // ////////////////////////////////////////////////////////////////
    // /////////////////////// modules /////////////////////////////
    // ////////////////////////////////////////////////////////////////

    /**
     * Provide a list containing all MaryModules instances. The order is not
     * important.
     *
     * @throws IllegalStateException
     *             if called while registration is not yet complete.
     * @return Collections.unmodifiableList(allModules)
     */
    public static List<MaryModule> listRegisteredModules() {
        return Collections.unmodifiableList(allModules);
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
    public static MaryModule getModule(Class<?> moduleClass) {
        for (Iterator<MaryModule> it = allModules.iterator(); it.hasNext();) {
            MaryModule m = it.next();
            if (moduleClass == m.getClass()) {
                return m;
            }
        }
        // Not found:
        return null;
    }
}
