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
package marytts.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import marytts.exceptions.MaryConfigurationException;
import marytts.modules.nlp.phonemiser.AllophoneSet;
import marytts.util.io.PropertiesAccessor;
import marytts.util.io.PropertiesTrimTrailingWhitespace;
import marytts.exceptions.NoSuchPropertyException;
import java.io.FileNotFoundException;

/**
 * @author marc
 *
 */
public abstract class PropertiesMaryConfig extends MaryConfigLoader {



    // ////////// Non-static / base class methods //////////////

    private Properties props;

    protected PropertiesMaryConfig(InputStream propertyStream) throws MaryConfigurationException {

        props = new PropertiesTrimTrailingWhitespace();
        try {
            props.load(propertyStream);
        } catch (Exception e) {
            throw new MaryConfigurationException("cannot load properties", e);
        }
    }



    public Properties getProperties() {
        return props;
    }

    /**
     * Convenience access to this config's properties.
     *
     * @param systemPropertiesOverride
     *            whether to use system properties in priority if they exist. If
     *            true, any property requested from this properties accessor
     *            will first be looked up in the system properties, and only if
     *            it is not defined there, it will be looked up in this config's
     *            properties.
     * @return PropertiesAccessor(props, systemPropertiesOverride, maryBaseMap)
     */
    public PropertiesAccessor getPropertiesAccessor(boolean systemPropertiesOverride) {
        Map<String, String> maryBaseMap = new HashMap<String, String>();
        return new PropertiesAccessor(props, systemPropertiesOverride, maryBaseMap);
    }

    /**
     * Get the given property. If it is not defined, the defaultValue is
     * returned.
     *
     * @param property
     *            name of the property to retrieve
     * @param defaultValue
     *            value to return if the property is not defined.
     * @return props.getProperty(property, defaultValue)
     */
    public String getProperty(String property, String defaultValue) {
        return props.getProperty(property, defaultValue);
    }

    /**
     * For the given property name, return the value of that property as a list
     * of items (interpreting the property value as a space-separated list).
     *
     * @param propertyName
     *            propertyName
     * @return the list of items, or an empty list if the property is not
     *         defined or contains no items
     */
    public List<String> getList(String propertyName) {
        String val = props.getProperty(propertyName);
        if (val == null) {
            return new ArrayList<String>();
        }
        return Arrays.asList(StringUtils.split(val));

    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * From a path entry in the properties, create an expanded form. Replace the
     * string MARY_BASE with the value of property "mary.base"; replace all "/"
     * and "\\" with the platform-specific file separator.
     *
     * @param path
     *            path
     * @return buf.toString
     */
    private static String expandPath(String path) {
        final String MARY_BASE = "MARY_BASE";
        StringBuilder buf = null;
        if (path.startsWith(MARY_BASE)) {
            buf = new StringBuilder(maryBase());
            buf.append(path.substring(MARY_BASE.length()));
        } else {
            buf = new StringBuilder(path);
        }
        if (File.separator.equals("/")) {
            int i = -1;
            while ((i = buf.indexOf("\\")) != -1) {
                buf.replace(i, i + 1, "/");
            }
        } else if (File.separator.equals("\\")) {
            int i = -1;
            while ((i = buf.indexOf("/")) != -1) {
                buf.replace(i, i + 1, "\\");
            }
        } else {
            throw new Error("Unexpected File.separator: `" + File.separator + "'");
        }
        return buf.toString();
    }

    /**
     * Get a property from the underlying properties.
     *
     * @param property
     *            the property requested
     * @return the property value if found, null otherwise.
     */
    public static String getProperty(String property) {
        return getProperty(property, null);
    }

    /**
     * Get a boolean property from the underlying properties.
     *
     * @param property
     *            the property requested
     * @return the boolean property value if found, false otherwise.
     */
    public static boolean getBoolean(String property) {
        return getBoolean(property, false);
    }

    /**
     * Get or infer a boolean property from the underlying properties. Apart
     * from the values "true"and "false", a value "auto" is permitted; it will
     * resolve to "true" in server mode and to "false" in non-server mode.
     *
     * @param property
     *            the property requested
     * @return the boolean property value if found, false otherwise.
     */
    public static boolean getAutoBoolean(String property) {
        return getAutoBoolean(property, false);
    }

    /**
     * Get an integer property from the underlying properties.
     *
     * @param property
     *            the property requested
     * @return the integer property value if found, -1 otherwise.
     */
    public static int getInteger(String property) {
        return getInteger(property, -1);
    }

    /**
     * Get a filename property from the underlying properties. The string
     * MARY_BASE is replaced with the value of the property mary.base, and path
     * separators are adapted to the current platform.
     *
     * @param property
     *            the property requested
     * @return the filename corresponding to the property value if found, null
     *         otherwise.
     */
    public static String getFilename(String property) {
        return getFilename(property, null);
    }

    /**
     * Get a boolean property from the underlying properties.
     *
     * @param property
     *            the property requested
     * @param defaultValue
     *            the value to return if the property is not defined
     * @return the boolean property value if found and valid, defaultValue
     *         otherwise.
     */
    public static boolean getBoolean(String property, boolean defaultValue) {
        String value = getProperty(property);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Boolean.valueOf(value).booleanValue();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get or infer a boolean property from the underlying properties. Apart
     * from the values "true"and "false", a value "auto" is permitted; it will
     * resolve to "true" in server mode and to "false" in non-server mode.
     *
     * @param property
     *            the property requested
     * @param defaultValue
     *            the value to return if the property is not defined
     * @return the boolean property value if found and valid, false otherwise.
     */
    public static boolean getAutoBoolean(String property, boolean defaultValue) {
        String value = getProperty(property);
        if (value == null) {
            return defaultValue;
        }
        if (value.equals("auto")) {
            return (getProperty("server").equals("commandline") ? false : true);
        } else {
            return getBoolean(property, defaultValue);
        }
    }

    /**
     * Get a property from the underlying properties.
     *
     * @param property
     *            the property requested
     * @param defaultValue
     *            the value to return if the property is not defined
     * @return the integer property value if found and valid, defaultValue
     *         otherwise.
     */
    public static int getInteger(String property, int defaultValue) {
        String value = getProperty(property);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.decode(value).intValue();
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get a filename property from the underlying properties. The string
     * MARY_BASE is replaced with the value of the property mary.base, and path
     * separators are adapted to the current platform.
     *
     * @param property
     *            the property requested
     * @param defaultValue
     *            the value to return if the property is not defined
     * @return the filename corresponding to the property value if found,
     *         defaultValue otherwise.
     */
    public static String getFilename(String property, String defaultValue) {
        String filename = getProperty(property);
        if (filename == null) {
            return defaultValue;
        }
        return expandPath(filename);
    }

    /**
     * Get a property from the underlying properties, throwing an exception if
     * it is not defined.
     *
     * @param property
     *            the property required
     * @return the property value
     * @throws NoSuchPropertyException
     *             if the property is not defined.
     */
    public static String needProperty(String property) throws NoSuchPropertyException {
        String value = getProperty(property);
        if (value == null) {
            throw new NoSuchPropertyException("Missing value `" + property + "' in configuration files");
        }
        return value;
    }

    /**
     * Get a boolean property from the underlying properties, throwing an
     * exception if it is not defined.
     *
     * @param property
     *            the property requested
     * @return the boolean property value
     * @throws NoSuchPropertyException
     *             if the property is not defined.
     */
    public static boolean needBoolean(String property) throws NoSuchPropertyException {
        String value = getProperty(property);
        if (value == null) {
            throw new NoSuchPropertyException("Missing property `" + property + "' in configuration files");
        }
        try {
            return Boolean.valueOf(value).booleanValue();
        } catch (NumberFormatException e) {
            throw new NoSuchPropertyException(
                "Boolean property `" + property + "' in configuration files has wrong value `" + value + "'");
        }
    }

    /**
     * Get or infer a boolean property from the underlying properties, throwing
     * an exception if it is not defined. Apart from the values "true"and
     * "false", a value "auto" is permitted; it will resolve to "true" in server
     * mode and to "false" in non-server mode.
     *
     * @param property
     *            the property requested
     * @return the boolean property value
     * @throws NoSuchPropertyException
     *             if the property is not defined.
     */
    public static boolean needAutoBoolean(String property) throws NoSuchPropertyException {
        String value = getProperty(property);
        if (value == null) {
            throw new NoSuchPropertyException("Missing property `" + property + "' in configuration files");
        }
        if (value.equals("auto")) {
            return (needProperty("server").equals("commandline") ? false : true);
        } else {
            return needBoolean(property);
        }
    }

    /**
     * Get an integer property from the underlying properties, throwing an
     * exception if it is not defined.
     *
     * @param property
     *            the property requested
     * @return the integer property value
     * @throws NoSuchPropertyException
     *             if the property is not defined.
     */
    public static int needInteger(String property) throws NoSuchPropertyException {
        String value = getProperty(property);
        if (value == null) {
            throw new NoSuchPropertyException("Missing property `" + property + "' in configuration files");
        }
        try {
            return Integer.decode(value).intValue();
        } catch (NumberFormatException e) {
            throw new NoSuchPropertyException(
                "Integer property `" + property + "' in configuration files has wrong value `" + value + "'");
        }
    }

    /**
     * Get a filename property from the underlying properties, throwing an
     * exception if it is not defined. The string MARY_BASE is replaced with the
     * value of the property mary.base, and path separators are adapted to the
     * current platform.
     *
     * @param property
     *            the property requested
     * @return the filename corresponding to the property value
     * @throws NoSuchPropertyException
     *             if the property is not defined or the value is not a valid
     *             filename
     */
    public static String needFilename(String property) throws NoSuchPropertyException {
        String filename = expandPath(needProperty(property));
        if (!new File(filename).canRead()) {
            throw new NoSuchPropertyException(
                "Cannot read file `" + filename + "'. Check property `" + property + "' in configuration files");
        }
        return filename;
    }

    /**
     * For the named property, attempt to get an open input stream. If the
     * property value starts with "jar:", the remainder of the value is
     * interpreted as an absolute path in the classpath. Otherwise it is
     * interpreted as a file name.
     *
     * @param propertyName
     *            the name of a property defined in one of the mary config
     *            files.
     * @return an InputStream representing the given resource
     * @throws NoSuchPropertyException
     *             if the property is not defined
     * @throws FileNotFoundException
     *             if the property value is a file name and the file cannot be
     *             opened
     * @throws MaryConfigurationException
     *             if the property value is a classpath entry which cannot be
     *             opened
     */
    public static InputStream needStream(String propertyName)
    throws NoSuchPropertyException, FileNotFoundException, MaryConfigurationException {
        MaryProperties.needProperty(propertyName); // to throw exceptions if not
        // defined
        return getStream(propertyName);
    }

    /**
     * For the named property, attempt to get an open input stream. If the
     * property value starts with "jar:", the remainder of the value is
     * interpreted as an absolute path in the classpath. Otherwise it is
     * interpreted as a file name.
     *
     * @param propertyName
     *            the name of a property defined in one of the mary config
     *            files.
     * @return an InputStream representing the given resource, or null if the
     *         property was not defined.
     * @throws FileNotFoundException
     *             if the property value is a file name and the file cannot be
     *             opened
     * @throws MaryConfigurationException
     *             if the property value is a classpath entry which cannot be
     *             opened
     */
    public static InputStream getStream(String propertyName) throws FileNotFoundException,
        MaryConfigurationException {
        InputStream stream;
        String propertyValue = getProperty(propertyName);
        if (propertyValue == null) {
            return null;
        } else if (propertyValue.startsWith("jar:")) { // read from classpath
            String classpathLocation = propertyValue.substring("jar:".length());
            stream = MaryProperties.class.getResourceAsStream(classpathLocation);
            if (stream == null) {
                throw new MaryConfigurationException("For property '" + propertyName
                                                     + "', no classpath resource available at '" + classpathLocation + "'");
            }
        } else {
            String fileName = MaryProperties.getFilename(propertyName);
            stream = new FileInputStream(fileName);
        }
        return stream;

    }

    /**
     * Get a Class property from the underlying properties, throwing an
     * exception if it is not defined.
     *
     * @param property
     *            the property requested
     * @return the Class corresponding to the property value
     * @throws NoSuchPropertyException
     *             if the property is not defined or the value is not a valid
     *             class
     */
    public static Class needClass(String property) throws NoSuchPropertyException {
        String value = needProperty(property);
        Class c = null;
        try {
            c = Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new NoSuchPropertyException(
                "Cannot find class `" + value + "'. Check property `" + property + "' in configuration files");
        }
        return c;
    }

    /**
     * Provide the config file prefix used for different locales in the config
     * files. Will return the string representation of the locale as produced by
     * locale.toString(), e.g. "en_GB"; if locale is null, return null.
     *
     * @param locale
     *            locale
     * @return locale converted to string
     */
    public static String localePrefix(Locale locale) {
        if (locale == null) {
            return null;
        }
        return locale.toString();
    }
}
