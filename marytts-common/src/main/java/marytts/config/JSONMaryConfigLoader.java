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

// Regexp
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// IO
import java.io.InputStream;
import java.io.InputStreamReader;

// Collections
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/* Reflection */
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

// JSON part
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

// Exceptions
import marytts.exceptions.MaryConfigurationException;
import java.io.FileNotFoundException;

/**
 * Json configuration loader
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class JSONMaryConfigLoader extends MaryConfigLoader {

    /** Class keyword used for object representation */
    public static final String CLASS_KEY = "class";

    /** Arguments keyword used for object representation */
    public static final String ARG_KEY = "arguments";

    /** Argument class keyword used for object representation */
    public static final String ARG_CLASS_KEY = "class";

    /** Argument value keyword used for object representation */
    public static final String ARG_VALUE_KEY = "value";


    /**
     *  Default constructor which is just instantiate the default configuration
     *
     *  @throws MaryConfigurationException if the instanciation failed
     */
    public JSONMaryConfigLoader() throws MaryConfigurationException {
	super();
	InputStream input_stream = this.getClass().getResourceAsStream(MaryConfigurationFactory.DEFAULT_KEY + ".json");
	loadConfiguration(MaryConfigurationFactory.DEFAULT_KEY, input_stream);
    }


    /**
     *  Method to explicitely load a configuration stored in a given stream
     *
     *  @param input_stream the stream in which the configuration is stored
     *  @return the loaded MaryConfiguration object
     *  @throws MaryConfigurationException if the loading fails
     */
    public MaryConfiguration loadConfiguration(InputStream input_stream) throws MaryConfigurationException {
	MaryConfiguration mc = new MaryConfiguration();
        try {
	    JSONObject root_config = (JSONObject) new JSONParser().parse(new InputStreamReader(input_stream));
	    for (Object class_ob: root_config.keySet()) {
		String class_name = (String) class_ob;

		try {
		    Class.forName(class_name, false, this.getClass().getClassLoader());
		} catch (Exception ex) {
		    throw new MaryConfigurationException("\"" + class_name + "\" is not in the class path", ex);
		}

		JSONObject properties_map = (JSONObject) root_config.get(class_ob);
		for (Object prop_ob: properties_map.keySet()) {
		    String prop_name = (String) prop_ob;
		    Object val = properties_map.get(prop_ob);

		    // A little bit of adaptation
		    if (val instanceof JSONArray) {
			val = jsonArrayToList((JSONArray) val);
		    } else if (val instanceof JSONObject) {
			val = jsonObjectToHash((JSONObject) val);
		    }

		    // Check if the setter method exists
		    prop_name = adaptPropertyName(prop_name);
		    assertSetter(class_name, prop_name, val.getClass());

		    mc.addConfigurationValueProperty(class_name, prop_name, val);
		}

	    }

	    return mc;
        } catch (Exception e) {
            throw new MaryConfigurationException("cannot load json configuration", e);
        }
    }


    /**
     *  Internal method to instanciate an object stored in the json format
     *
     *  an object is composed by the following description:
     *
     *  {
     *     "class": "class of the object",
     *     "arguments": [
     *         {
     *             "class": "class of the argument",
     *             "value": "value of the argument"
     *         },
     *         ....
     *     ]
     *  }
     *
     *  the value of the argument is store in a string and is optional.
     *
     *  @param hash the jsonobject representing the object to instanciate
     *  @return the instanciated objects
     *  @throws MaryConfigurationException if anything fails during the instanciation
     */
    protected Object jsonObjectInstantiate(JSONObject hash) throws MaryConfigurationException {
	try {
	    Class<?> clazz = Class.forName((String) hash.get(CLASS_KEY));

	    JSONArray json_args = (JSONArray) hash.get(ARG_KEY);
	    Object[] args = null;
	    Constructor<?> ctor = null;

	    if (json_args == null) { // Default constructor
		args = new Object[] {};
		ctor = clazz.getConstructor();
	    } else { // With args constructor
		args = new Object[json_args.size()];
		Class<?>[] arg_classes = new Class<?>[json_args.size()];

		for (int i=0; i<json_args.size(); i++) {
		    JSONObject arg = (JSONObject) json_args.get(i);

		    // Get class
		    Class<?> cur_clazz = Class.forName((String) arg.get(ARG_CLASS_KEY));
		    arg_classes[i] = cur_clazz;

		    // Check argument (at most 1 for now and String mandatory)
		    Object[] arg_val = null;
		    Class<?>[] class_ctor = null;
		    if (arg.containsKey(ARG_VALUE_KEY)) {

			// Prepare arguments
			class_ctor = new Class<?>[1];
			class_ctor[0] = Class.forName("java.lang.String");

			// Get value argument
			arg_val = new Object[1];
			String value = arg.get(ARG_VALUE_KEY).toString();
			arg_val[0] = value;

		    } else {
			// Prepare arguments
			class_ctor = new Class<?>[]{};

			// Get value argument
			arg_val = new Object[] {};
		    }

		    // Ok now instanciate
		    Constructor<?> cur_ctor = cur_clazz.getConstructor(class_ctor);
		    args[i] = cur_ctor.newInstance(arg_val);
		}

		ctor = clazz.getConstructor(arg_classes);
	    }

	    return ctor.newInstance(args);
	} catch (Exception ex) {
	    throw new MaryConfigurationException("Couldn't instantiate object of class \"" +
						 hash.get(CLASS_KEY).toString()	 + "\"",  ex);
	}
    }


    /**
     *  Internal method to load a json hash
     *
     *  it can be a classic dictionnary or an object description. The last case is detected if a key
     *  "class" is present in the map.
     *
     *  @param hash the hash to load
     *  @return the object instanciated or the loaded dictionnary
     *  @throws MaryConfigurationException if the loading fails
     */
    protected Object jsonObjectToHash(JSONObject hash) throws MaryConfigurationException {

	// Is the hash is actually an object description
	Set<Object> key_set = hash.keySet();
	if (key_set.contains(CLASS_KEY))
	    return jsonObjectInstantiate(hash);

	// Load the dictionary
	HashMap<String, Object> output_hash = new HashMap<String, Object>();
	for (Object k_ob: key_set) {
	    String k = k_ob.toString();
	    Object v = hash.get(k_ob);
	    // A little bit of adaptation
	    if (v instanceof JSONArray) {
		v = jsonArrayToList((JSONArray) v);
	    } else if (v instanceof JSONObject) {
		v = jsonObjectToHash((JSONObject) v);
	    }

	    output_hash.put(k, v);
	}

	return output_hash;
    }

    /**
     *  Convert the json array to an arraylist
     *
     *  @param array the json array
     *  @return the arraylist of objects extracted from the json array
     *  @throws MaryConfigurationException if anything fails
     */
    protected ArrayList<Object> jsonArrayToList(JSONArray array) throws MaryConfigurationException {
	if (array.size() == 0)
	    throw new MaryConfigurationException("It is forbidden to have an empty array in the configuration");

	ArrayList<Object> ar = new ArrayList<Object>();
	for (Object v: array) {

	    // A little bit of adaptation
	    if (v instanceof JSONArray) {
		v = jsonArrayToList((JSONArray) v);
	    } else if (v instanceof JSONObject) {
		v = jsonObjectToHash((JSONObject) v);
	    }

	    ar.add(v);
	}

	return ar;
    }

    /**
     *  Assert if the setter method for a given property is available in the given class
     *
     *  @param class_name the name of the class
     *  @param property_name the name of the property
     *  @param class_arg the class of the argument
     *  @throws MaryConfigurationException if the setter doesn't exist
     */
    protected void assertSetter(String class_name, String property_name, Class<?> class_arg) throws MaryConfigurationException {
	Class cls = null;
	try {
	    cls =  Class.forName(class_name, false, this.getClass().getClassLoader());
	    Class[] cArg = new Class[1];
	    cArg[0] = class_arg;
	    cls.getMethod("set" + property_name, cArg);

	    return;
	} catch (Exception ex) {
	    throw new MaryConfigurationException("\"set" + property_name + "\" with argument of type \"" + class_arg.toString() +
						 "\"is not a method of the class \"" + class_name +  "\"", ex);
	}
    }

    /**
     *  Internal method to transform a property name to its setter name
     *
     *  By convention the first charatecter is always capitalized as all characters prefixed by
     *  underscores. For example, prop_name is going to be transformed in setPropName.
     *
     *  @param property_name the name of the property for which we want the setter method
     *  @return the setter method name
     */
    protected String adaptPropertyName(String property_name) {
	// Upper case the first letter
	property_name = property_name.substring(0,1).toUpperCase() + property_name.substring(1).toLowerCase();

	// Upper case the character prefixed by underscores
	StringBuffer result = new StringBuffer();
	Matcher m = Pattern.compile("_(\\w)").matcher(property_name);
	while (m.find()) {
	    m.appendReplacement(result,
				m.group(1).toUpperCase());
	}
	m.appendTail(result);

	return result.toString();
    }
}
