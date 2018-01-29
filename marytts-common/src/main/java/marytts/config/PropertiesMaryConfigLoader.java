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



import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;
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
import marytts.util.io.PropertiesAccessor;
import marytts.util.io.PropertiesTrimTrailingWhitespace;
import marytts.exceptions.NoSuchPropertyException;
import java.io.FileNotFoundException;

/**
 * This class is designed to load configuration stored in "extended" java properties.
 *
 *
 * @author marc
 *
 */
public class PropertiesMaryConfigLoader extends MaryConfigLoader {

    // ////////// Non-/ base class methods //////////////

    /** The properties available in the file */
    private Properties props;

    public PropertiesMaryConfigLoader() throws MaryConfigurationException {
	super();
	InputStream input_stream = this.getClass().getResourceAsStream(MaryConfigurationFactory.DEFAULT_KEY + ".config");
	loadConfiguration(MaryConfigurationFactory.DEFAULT_KEY, input_stream);
    }

    /**
     * Configuration loading method
     *
     *  @param set the name of the configuration set for later reference in the configuration hash
     *  @param input_stream the stream containing the configuration
     */
    public void loadConfiguration(String set, InputStream propertyStream) throws MaryConfigurationException {
	MaryConfiguration mc = new MaryConfiguration();
        props = new PropertiesTrimTrailingWhitespace();
        try {
            props.load(propertyStream);
	    for (String key_prop: props.stringPropertyNames()) {
		int d_index  = key_prop.lastIndexOf(".");
		if (d_index < 0)
		    throw new MaryConfigurationException("Property \"" + key_prop + "\" is malformed. See PropertiesMaryConfigLoader documentation");

		// Check if class exists
		String class_name = key_prop.substring(0, d_index);
		try {
		    Class.forName(class_name, false, this.getClass().getClassLoader());
		} catch (Exception ex) {
		    throw new MaryConfigurationException("\"" + class_name + "\" is not in the class path", ex);
		}

		// Check if method exists
		String method_name = key_prop.substring(d_index+1);
		method_name = adaptMethodName(method_name);
		assessMethod(class_name, method_name, String.class);


		String property = props.getProperty(key_prop);
		mc.addConfigurationValueProperty(class_name, method_name, property);
	    }

	    MaryConfigurationFactory.addConfiguration(set, mc);
        } catch (Exception e) {
            throw new MaryConfigurationException("cannot load properties", e);
        }
    }


    public void assessMethod(String class_name, String property_name, Class<?> class_arg) throws MaryConfigurationException {
	Class cls = null;
	try {
	    cls =  Class.forName(class_name, false, this.getClass().getClassLoader());
	    Class[] cArg = new Class[1];
	    cArg[0] = class_arg;
	    cls.getMethod("set" + property_name, cArg);

	    return;
	} catch (Exception ex) {

	}

	throw new MaryConfigurationException("\"set" + property_name + "\" with argument of type \"" + class_arg.toString() +
					     "\"is not a method of the class \"" + class_name +  "\"");
    }

    public String adaptMethodName(String method_name) {
	method_name = method_name.substring(0,1).toUpperCase() + method_name.substring(1).toLowerCase();
	StringBuffer result = new StringBuffer();
	Matcher m = Pattern.compile("_(\\w)").matcher(method_name);
	while (m.find()) {
	    m.appendReplacement(result,
				m.group(1).toUpperCase());
	}
	m.appendTail(result);
	return result.toString();

    }

    protected Properties getProperties() {
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
    protected PropertiesAccessor getPropertiesAccessor(boolean systemPropertiesOverride) {
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
    protected String getProperty(String property, String defaultValue) {
        return props.getProperty(property, defaultValue);
    }



    /////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Get a property from the underlying properties.
     *
     * @param property
     *            the property requested
     * @return the property value if found, null otherwise.
     */
    protected String getProperty(String property) {

        // First, try system properties:
        String val = System.getProperty(property);
        if (val != null) {
            return val;
        }

        // Then, try the configs. First one wins:
	val = this.getProperties().getProperty(property);
	if (val != null) {
	    return val;
        }

	return null;
    }


}
