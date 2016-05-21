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

import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.server.MaryProperties;

import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.log4j.Logger;

/**
 * A hierarchical repository for Mary modules, allowing the flexible indexing by an ordered hierarchy of datatype, locale and
 * voice. A given lookup will search for a combination of datatype, locale and voice first; if it does not find a value, it will
 * look for datatype, locale, and null; if it does notfind that, it will look for datatype, null, and null.
 *
 * @author marc
 *
 */
public class ModuleRegistry {
	private static MultiKeyMap mkm;
	private static List<MaryModule> allModules;
	private static boolean registrationComplete;
	private static Logger logger;

	private static List<MaryModule> preferredModules;

	private ModuleRegistry() {
	}

	/**
	 * Create a new, empty module repository.
	 */
	static {
		mkm = new MultiKeyMap();
		allModules = new ArrayList<MaryModule>();
		registrationComplete = false;
		logger = MaryUtils.getLogger("ModuleRegistry");
	}

	// ////////////////////////////////////////////////////////////////
	// /////////////////////// instantiation //////////////////////////
	// ////////////////////////////////////////////////////////////////

	/**
	 * From the given module init info, instantiate a new mary module.
	 *
	 * @param moduleInitInfo
	 *            a string description of the module to instantiate. The moduleInitInfo is expected to have one of the following
	 *            forms:
	 *            <ol>
	 *            <li>my.class.which.extends.MaryModule</li>
	 *            <li>my.class.which.extends.MaryModule(any,string,args,without,spaces)</li>
	 *            <li>my.class.which.extends.MaryModule(arguments,$my.special.property,other,args)</li>
	 *            </ol>
	 *            where 'my.special.property' is a property in the property file.
	 * @throws MaryConfigurationException
	 *             if the module cannot be instantiated
	 * @return m
	 */
	public static MaryModule instantiateModule(String moduleInitInfo) throws MaryConfigurationException {
		logger.info("Now initiating mary module '" + moduleInitInfo + "'");
		MaryModule m = (MaryModule) MaryRuntimeUtils.instantiateObject(moduleInitInfo);
		return m;
	}

	// ////////////////////////////////////////////////////////////////
	// /////////////////////// registration ///////////////////////////
	// ////////////////////////////////////////////////////////////////

	/**
	 * Register a MaryModule as an appropriate module to process the given combination of MaryDataType for the input data, locale
	 * of the input data, and voice requested for processing. Note that it is possible to register more than one module for a
	 * given combination of input type, locale and voice; in that case, all of them will be remembered, and will be returned as a
	 * List by get().
	 *
	 * @param module
	 *            the module to add to the registry, under its input type and the given locale and voice.
	 * @param locale
	 *            the locale (language or language-COUNTRY) of the input data; can be null to signal that the module is
	 *            locale-independent.
	 * @param voice
	 *            a voice for which this module is suited. Can be null to indicate that the module is not specific to any voice.
	 * @throws IllegalStateException
	 *             if called after registration is complete.
	 */
	@SuppressWarnings("unchecked")
	public static void registerModule(MaryModule module, Locale locale, Voice voice) throws IllegalStateException {
		if (registrationComplete)
			throw new IllegalStateException("cannot register modules after registration is complete");
		MaryDataType type = module.inputType();
		Object o = mkm.get(type, locale, voice);
		List<MaryModule> l;
		if (o != null) {
			assert o instanceof List : "Expected List of MaryModules, got " + o.getClass();
			l = (List<MaryModule>) o;
		} else {
			l = new ArrayList<MaryModule>(1);
			mkm.put(type, locale, voice, l);
		}
		assert l != null;
		l.add(module);

		allModules.add(module);

		MaryDataType.registerDataType(type);
		MaryDataType.registerDataType(module.outputType());
	}

	/**
	 * Determine whether or not the registration is complete. When the registration is not (yet) complete, calls to
	 *
	 * @see #registerModule(MaryModule, Locale, Voice) are possible; when the registration is complete, calls to the other methods
	 *      are possible.
	 *
	 * @return false when the registration is still open, true when it is complete.
	 */
	public static boolean getRegistrationComplete() {
		return registrationComplete;
	}

	/**
	 * Indicate that the registration is now complete. No further calls to registerModules() will be possible.
	 *
	 * @throws IllegalStateException
	 *             if called when registration was already completed before.
	 */
	public static void setRegistrationComplete() throws IllegalStateException {
		if (registrationComplete)
			throw new IllegalStateException("Registration has already completed, cannot do that a second time");

		// Set registration complete lockup
		registrationComplete = true;
		MaryDataType.setRegistrationComplete();

		// Define system preferred modules
		List<String> preferredModulesClasses = MaryProperties.getList("modules.preferred.classes.list");
		if ((preferredModulesClasses == null) || (preferredModulesClasses.isEmpty()))
			return;

		preferredModules = new ArrayList<MaryModule>();
		for (String moduleInfo : preferredModulesClasses) {
			try {
				MaryModule mm = null;
				if (!moduleInfo.contains("(")) { // no constructor info
					mm = ModuleRegistry.getModule(Class.forName(moduleInfo));
				}
				preferredModules.add(mm);
			} catch (ClassNotFoundException e) {
				logger.warn("Cannot initialise preferred module " + moduleInfo + " -- skipping.", e);
			}
		}
	}

	// ////////////////////////////////////////////////////////////////
	// /////////////////////// modules /////////////////////////////
	// ////////////////////////////////////////////////////////////////

	/**
	 * Provide a list containing preferred modules for the specified input type
	 *
	 * @param wanted_input_type
	 *            the specified input type
	 * @return the list of system wide preferred modules, null if none
	 */
	public static synchronized List<MaryModule> getPreferredModulesForInputType(MaryDataType wanted_input_type) {
		if (preferredModules != null) {
			List<MaryModule> v = new ArrayList<MaryModule>();
			for (Iterator<MaryModule> it = preferredModules.iterator(); it.hasNext();) {
				MaryModule m = (MaryModule) it.next();
				if (m.inputType().equals(wanted_input_type)) {
					v.add(m);
				}
			}
			if (v.size() > 0)
				return v;
			else
				return null;
		}
		return null;
	}

	/**
	 * Provide a list containing all MaryModules instances. The order is not important.
	 *
	 * @throws IllegalStateException
	 *             if called while registration is not yet complete.
	 * @return Collections.unmodifiableList(allModules)
	 */
	public static List<MaryModule> getAllModules() {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about modules while registration is ongoing");
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
	// TODO: what should happen with this method when we parameterise modules, so that there can be several instances of the same
	// class?
	public static MaryModule getModule(Class<?> moduleClass) {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about modules while registration is ongoing");
		for (Iterator<MaryModule> it = allModules.iterator(); it.hasNext();) {
			MaryModule m = it.next();
			if (moduleClass.isInstance(m)) {
				return m;
			}
		}
		// Not found:
		return null;
	}

	/**
	 * A method for determining the list of modules required to transform the given source data type into the requested target
	 * data type.
	 *
	 * @param sourceType
	 *            sourceType
	 * @param targetType
	 *            targetType
	 * @param locale
	 *            locale
	 * @return the (ordered) list of modules required, or null if no such list could be found.
	 * @throws IllegalStateException
	 *             if called while registration is not yet complete.
	 * @throws NullPointerException
	 *             if source data type, target data type or locale is null.
	 */
	public static LinkedList<MaryModule> modulesRequiredForProcessing(MaryDataType sourceType, MaryDataType targetType,
			Locale locale) {
		return modulesRequiredForProcessing(sourceType, targetType, locale, null);
	}

	/**
	 * A method for determining the list of modules required to transform the given source data type into the requested target
	 * data type. If the voice given is not null, any preferred modules it may have are taken into account.
	 *
	 * @param sourceType
	 *            sourceType
	 * @param targetType
	 *            target type
	 * @param locale
	 *            locale
	 * @param voice
	 *            voice
	 * @return the (ordered) list of modules required, or null if no such list could be found.
	 * @throws IllegalStateException
	 *             if called while registration is not yet complete.
	 * @throws NullPointerException
	 *             if source data type, target data type or locale is null.
	 */
	public static LinkedList<MaryModule> modulesRequiredForProcessing(MaryDataType sourceType, MaryDataType targetType,
			Locale locale, Voice voice) {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about modules while registration is ongoing");
		if (sourceType == null)
			throw new NullPointerException("Received null source type");
		if (targetType == null)
			throw new NullPointerException("Received null target type");
		// if (locale == null)
		// throw new NullPointerException("Received null locale");
		LinkedList<MaryDataType> seenTypes = new LinkedList<MaryDataType>();
		seenTypes.add(sourceType);
		return modulesRequiredForProcessing(sourceType, targetType, locale, voice, seenTypes);
	}

	/**
	 * This method recursively calls itself. It forward-constructs a list of seen types (upon test), and backward-constructs a
	 * list of required modules (upon success).
	 *
	 * @param sourceType
	 *            sourceType
	 * @param targetType
	 *            targetType
	 * @param locale
	 *            locale
	 * @param voice
	 *            voice
	 * @param seenTypes
	 *            seenTypes
	 * @return LinkedList<MaryModule>() if sourceType equals targetType, null otherwise
	 */
	private static LinkedList<MaryModule> modulesRequiredForProcessing(MaryDataType sourceType, MaryDataType targetType,
			Locale locale, Voice voice, LinkedList<MaryDataType> seenTypes) {
		// Terminating condition:
		if (sourceType.equals(targetType)) {
			logger.debug("found path through modules");
			return new LinkedList<MaryModule>();
		}
		// Recursion step:
		// Any voice-specific modules?
		List<MaryModule> candidates = getPreferredModulesForInputType(sourceType);

		// TODO: the following should be obsolete as soon as we are properly using the voice index in ModuleRegistry
		if ((candidates == null || candidates.isEmpty()) && voice != null)
			candidates = voice.getPreferredModulesAcceptingType(sourceType);
		if (candidates == null || candidates.isEmpty()) { // default: use all available modules
			candidates = get(sourceType, locale, voice);
		}
		if (candidates == null || candidates.isEmpty()) {
			// no module can handle this type
			return null;
		}
		for (Iterator<MaryModule> it = candidates.iterator(); it.hasNext();) {
			MaryModule candidate = it.next();
			MaryDataType outputType = candidate.outputType();
			// Ignore candidates that would bring us to a data type that we
			// have already seen (i.e., that would lead to a loop):
			if (!seenTypes.contains(outputType)) {
				seenTypes.add(outputType);
				logger.debug("Module " + candidate.name() + " converts " + sourceType.name() + " into " + outputType
						+ " (locale " + locale + ", voice " + voice + ")");
				// recursive call:
				LinkedList<MaryModule> path = modulesRequiredForProcessing(outputType, targetType, locale, voice, seenTypes);
				if (path != null) {
					// success, found a path of which candidate is the first
					// step
					path.addFirst(candidate);
					return path;
				} // else, try next candidate
				seenTypes.removeLast();
			}
		}
		// We get here only if none of the candidates lead to a valid path
		return null; // failure
	}

	/**
	 * Lookup a list of modules for the given combination of type, locale and voice. A given lookup will return a list of modules
	 * accepting the datatype as input, ordered by specificity so that the most specific modules come first. For each output type
	 * produced by modules, there will be only one module producing that type, namely the most specific one found. Specificity is
	 * ordered as follows: 1. most specific is the full combination of datatype, locale and voice; 2. the combination of datatype,
	 * language-only locale and voice (i.e., for requested locale "en-US" will find modules with locale "en"); 3. the combination
	 * of datatype and locale, ignoring voice; 4. the combination of datatype and language-only locale, ignoring voice; 5. least
	 * specific is the datatype, ignoring locale and voice.
	 *
	 * @param type
	 *            the type of input data
	 * @param locale
	 *            the locale
	 * @param voice
	 *            the voice to use.
	 * @return a list of mary modules accepting the datatype and suitable for the given locale and voice, ordered by their
	 *         specificity, or an empty list if there is no suitable module.
	 * @throws IllegalStateException
	 *             if called when registration is not yet complete.
	 */
	@SuppressWarnings("unchecked")
	private static List<MaryModule> get(MaryDataType type, Locale locale, Voice voice) throws IllegalStateException {
		if (!registrationComplete)
			throw new IllegalStateException("Cannot inquire about modules while registration is ongoing");
		LinkedHashMap<MaryDataType, MaryModule> results = new LinkedHashMap<MaryDataType, MaryModule>();
		// First, get all results:
		List<List<MaryModule>> listOfLists = new ArrayList<List<MaryModule>>();
		listOfLists.add((List<MaryModule>) mkm.get(type, locale, voice));
		Locale langOnly = locale != null ? new Locale(locale.getLanguage()) : null;
		boolean haveCountry = langOnly == null ? false : !langOnly.equals(locale);
		if (haveCountry) {
			listOfLists.add((List<MaryModule>) mkm.get(type, langOnly, voice));
		}
		listOfLists.add((List<MaryModule>) mkm.get(type, locale, null));
		if (haveCountry) {
			listOfLists.add((List<MaryModule>) mkm.get(type, langOnly, null));
		}
		listOfLists.add((List<MaryModule>) mkm.get(type, null, null));
		// Now, for each mary output type, keep only the most specific module,
		// and return the list ordered by the specificity of modules (most specific first):
		for (List<MaryModule> list : listOfLists) {
			if (list != null) {
				for (MaryModule m : list) {
					if (!results.containsKey(m.outputType()))
						results.put(m.outputType(), m);
				}
			}
		}
		List<MaryModule> returnList = new ArrayList<MaryModule>();
		for (MaryDataType t : results.keySet()) {
			returnList.add(results.get(t));
		}
		return returnList;
	}

}
