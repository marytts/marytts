/**
 * 
 */
package marytts.util.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import marytts.exceptions.MaryConfigurationException;

/**
 * provide comfortable access to a properties, optionally overriding settings with system properties, and optionally providing
 * string replacements.
 * 
 * @author marc
 *
 */
public class PropertiesAccessor {
	private boolean isSystemOverrides;
	private Map<String, String> replacements;
	private Properties p;

	public PropertiesAccessor(Properties p) {
		this(p, false, null);
	}

	/**
	 * Initialise the properties accessor.
	 * 
	 * @param p
	 *            the properties to access
	 * @param letSystemPropertiesOverride
	 *            if true, any property will first be looked up in the system properties; only if it is not found there, it will
	 *            be looked up in p
	 * @param stringReplacements
	 *            before &rarr; after pairs of string replacements to perform before returning any property values.
	 */
	public PropertiesAccessor(Properties p, boolean letSystemPropertiesOverride, Map<String, String> stringReplacements) {
		this.p = p;
		this.isSystemOverrides = letSystemPropertiesOverride;
		this.replacements = stringReplacements;
	}

	/**
	 * Get a property from the underlying properties.
	 * 
	 * @param key
	 *            the property key requested
	 * @return the property value if found, null otherwise.
	 */
	public String getProperty(String key) {
		return getProperty(key, null);
	}

	/**
	 * Get a property from the underlying properties.
	 * 
	 * @param key
	 *            the property key requested
	 * @param defaultValue
	 *            the value to return if the property is not defined
	 * @return the property value if found, defaultValue otherwise.
	 */
	public String getProperty(String key, String defaultValue) {
		String value;
		if (isSystemOverrides && System.getProperties().contains(key)) {
			value = System.getProperty(key);
		} else {
			value = p.getProperty(key, defaultValue);
		}
		if (value != null && replacements != null) {
			for (String s : replacements.keySet()) {
				if (value.contains(s)) {
					value = value.replace(s, replacements.get(s));
				}
			}
		}
		return value;
	}

	/**
	 * Get a boolean property from the underlying properties.
	 * 
	 * @param property
	 *            the property requested
	 * @return the boolean property value if found, false otherwise.
	 */
	public boolean getBoolean(String property) {
		return getBoolean(property, false);
	}

	/**
	 * Get a boolean property from the underlying properties.
	 * 
	 * @param property
	 *            the property requested
	 * @param defaultValue
	 *            the value to return if the property is not defined
	 * @return the boolean property value if found and valid, defaultValue otherwise.
	 */
	public boolean getBoolean(String property, boolean defaultValue) {
		String value = getProperty(property);
		if (value == null)
			return defaultValue;
		try {
			return Boolean.valueOf(value).booleanValue();
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Get an integer property from the underlying properties.
	 * 
	 * @param property
	 *            the property requested
	 * @return the integer property value if found, -1 otherwise.
	 */
	public int getInteger(String property) {
		return getInteger(property, -1);
	}

	/**
	 * Get a property from the underlying properties.
	 * 
	 * @param property
	 *            the property requested
	 * @param defaultValue
	 *            the value to return if the property is not defined
	 * @return the integer property value if found and valid, defaultValue otherwise.
	 */
	public int getInteger(String property, int defaultValue) {
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
	 * Get a double property from the underlying properties.
	 * 
	 * @param property
	 *            the property requested
	 * @return the double property value if found, Double.NaN otherwise.
	 */
	public double getDouble(String property) {
		return getDouble(property, Double.NaN);
	}

	/**
	 * Get a property from the underlying properties.
	 * 
	 * @param property
	 *            the property requested
	 * @param defaultValue
	 *            the value to return if the property is not defined
	 * @return the double property value if found and valid, defaultValue otherwise.
	 */
	public double getDouble(String property, double defaultValue) {
		String value = getProperty(property);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * For the named property, attempt to get an open input stream. If the property value starts with "jar:", the remainder of the
	 * value is interpreted as an absolute path in the classpath. Otherwise it is interpreted as a file name.
	 * 
	 * @param propertyName
	 *            the name of a property defined in one of the mary config files.
	 * @return an InputStream representing the given resource, or null if the property was not defined.
	 * @throws FileNotFoundException
	 *             if the property value is a file name and the file cannot be opened
	 * @throws MaryConfigurationException
	 *             if the property value is a classpath entry which cannot be opened
	 */
	public InputStream getStream(String propertyName) throws FileNotFoundException, MaryConfigurationException {
		InputStream stream;
		String propertyValue = getProperty(propertyName);
		if (propertyValue == null) {
			return null;
		} else if (propertyValue.startsWith("jar:")) { // read from classpath
			String classpathLocation = propertyValue.substring("jar:".length());
			stream = this.getClass().getResourceAsStream(classpathLocation);
			if (stream == null) {
				throw new MaryConfigurationException("For property '" + propertyName + "', no classpath resource available at '"
						+ classpathLocation + "'");
			}
		} else {
			String fileName = propertyValue;
			stream = new FileInputStream(fileName);
		}
		return stream;

	}

}
