/**
 * Copyright 2011 DFKI GmbH.
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

package marytts.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;

import marytts.Version;
import marytts.config.LanguageConfig;
import marytts.config.MaryConfig;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.fst.FSTLookup;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.server.Mary;
import marytts.server.MaryProperties;
import marytts.util.dom.MaryDomUtils;
import marytts.util.string.StringUtils;

import org.w3c.dom.Element;

/**
 * @author marc
 *
 */
public class MaryRuntimeUtils {

    public static void ensureMaryStarted() throws Exception {
        synchronized (MaryConfig.getMainConfig()) {
            if (Mary.currentState() == Mary.STATE_OFF) {
                Mary.startup();
            }
        }
    }

    /**
     * Instantiate an object by calling one of its constructors.
     *
     * @param objectInitInfo
     *            a string description of the object to instantiate. The
     *            objectInitInfo is expected to have one of the following forms:
     *            <ol>
     *            <li>my.cool.Stuff</li>
     *            <li>my.cool.Stuff(any,string,args,without,spaces)</li>
     *            <li>my.cool.Stuff(arguments,$my.special.property,other,args)</li>
     *            </ol>
     *            where 'my.special.property' is a property in one of the MARY
     *            config files.
     * @return the newly instantiated object.
     * @throws MaryConfigurationException
     *             MaryConfigurationException
     */
    public static Object instantiateObject(String objectInitInfo) throws MaryConfigurationException {
        Object obj = null;
        String[] args = null;
        String className = null;
        try {
            if (objectInitInfo.contains("(")) { // arguments
                int firstOpenBracket = objectInitInfo.indexOf('(');
                className = objectInitInfo.substring(0, firstOpenBracket);
                int lastCloseBracket = objectInitInfo.lastIndexOf(')');
                args = objectInitInfo.substring(firstOpenBracket + 1, lastCloseBracket).split(",");
                for (int i = 0; i < args.length; i++) {
                    if (args[i].startsWith("$")) {
                        // replace value with content of property named after
                        // the $
                        args[i] = MaryProperties.getProperty(args[i].substring(1));
                    }
                    args[i] = args[i].trim();
                }
            } else { // no arguments
                className = objectInitInfo;
            }
            Class<? extends Object> theClass = Class.forName(className).asSubclass(Object.class);
            // Now invoke Constructor with args.length String arguments
            if (args != null) {
                Class<String>[] constructorArgTypes = new Class[args.length];
                Object[] constructorArgs = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    constructorArgTypes[i] = String.class;
                    constructorArgs[i] = args[i];
                }
                Constructor<? extends Object> constructor = (Constructor<? extends Object>) theClass
                        .getConstructor(constructorArgTypes);
                obj = constructor.newInstance(constructorArgs);
            } else {
                obj = theClass.newInstance();
            }
        } catch (Exception e) {
            // try to make e's message more informative if possible
            throw new MaryConfigurationException("Cannot instantiate object from '" + objectInitInfo + "': "
                                                 + MaryUtils.getFirstMeaningfulMessage(e), e);
        }
        return obj;
    }

    /**
     * Verify if the java virtual machine is in a low memory condition. The
     * memory is considered low if less than a specified value is still
     * available for processing. "Available" memory is calculated using
     * <code>availableMemory()</code>.The threshold value can be specified as
     * the Mary property mary.lowmemory (in bytes). It defaults to 20000000
     * bytes.
     *
     * @return a boolean indicating whether or not the system is in low memory
     *         condition.
     */
    public static boolean lowMemoryCondition() {
        return MaryUtils.availableMemory() < lowMemoryThreshold();
    }

    /**
     * Verify if the java virtual machine is in a very low memory condition. The
     * memory is considered very low if less than half a specified value is
     * still available for processing. "Available" memory is calculated using
     * <code>availableMemory()</code> .The threshold value can be specified as
     * the Mary property mary.lowmemory (in bytes). It defaults to 20000000
     * bytes.
     *
     * @return a boolean indicating whether or not the system is in very low
     *         memory condition.
     */
    public static boolean veryLowMemoryCondition() {
        return MaryUtils.availableMemory() < lowMemoryThreshold() / 2;
    }

    private static long lowMemoryThreshold() {
        if (lowMemoryThreshold < 0) { // not yet initialised
            lowMemoryThreshold = (long) MaryProperties.getInteger("mary.lowmemory", 10000000);
        }
        return lowMemoryThreshold;
    }

    private static long lowMemoryThreshold = -1;

    /**
     * Try to determine the Allophone set to use for the given locale.
     *
     * @param locale
     *            locale
     * @return the allophone set defined for the given locale, or null if no
     *         such allophone set can be determined.
     * @throws MaryConfigurationException
     *             if an allophone set exists for the given locale in principle,
     *             but there were problems loading it.
     */
    public static AllophoneSet determineAllophoneSet(Locale locale) throws MaryConfigurationException {
        AllophoneSet allophoneSet = null;
        String propertyPrefix = MaryProperties.localePrefix(locale);
        if (propertyPrefix != null) {
            String propertyName = propertyPrefix + ".allophoneset";
            allophoneSet = needAllophoneSet(propertyName);
        }
        return allophoneSet;
    }

    /**
     * Convenience method to access the allophone set referenced in the MARY
     * property with the given name.
     *
     * @param propertyName
     *            name of the property referring to the allophone set
     * @throws MaryConfigurationException
     *             if the allophone set cannot be obtained
     * @return the requested allophone set. This method will never return null;
     *         if it cannot get the allophone set, it throws an exception.
     */
    public static AllophoneSet needAllophoneSet(String propertyName) throws MaryConfigurationException {
        String propertyValue = MaryProperties.getProperty(propertyName);
        if (propertyValue == null) {
            throw new MaryConfigurationException("No such property: " + propertyName);
        }
        if (AllophoneSet.hasAllophoneSet(propertyValue)) {
            return AllophoneSet.getAllophoneSetById(propertyValue);
        }
        InputStream alloStream;
        try {
            alloStream = MaryProperties.needStream(propertyName);
        } catch (FileNotFoundException e) {
            throw new MaryConfigurationException("Cannot open allophone stream for property " + propertyName,
                                                 e);
        }
        assert alloStream != null;
        return AllophoneSet.getAllophoneSet(alloStream, propertyValue);
    }

    public static String[] checkLexicon(String propertyName, String token)
    throws IOException, MaryConfigurationException {
        String lexiconProperty = propertyName + ".lexicon";
        InputStream lexiconStream = MaryProperties.needStream(lexiconProperty);
        FSTLookup lexicon = new FSTLookup(lexiconStream, lexiconProperty);
        return lexicon.lookup(token.toLowerCase());
    }

    public static String getMaryVersion() {
        String output = "Mary TTS server " + Version.specificationVersion() + " (impl. "
                        + Version.implementationVersion() + ")";

        return output;
    }

    public static String getLocales() {
        StringBuilder out = new StringBuilder();
        for (LanguageConfig conf : MaryConfig.getLanguageConfigs()) {
            for (Locale locale : conf.getLocales()) {
                out.append(locale).append('\n');
            }
        }
        return out.toString();
    }
}
