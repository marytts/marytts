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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// JSON part
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import org.apache.commons.lang.StringUtils;

import marytts.exceptions.MaryConfigurationException;
import java.io.FileNotFoundException;

/**
 * This class is designed to load configuration stored in "extended" java properties.
 *
 *
 * @author marc
 *
 */
public class JSONMaryConfigLoader extends MaryConfigLoader {

    // ////////// Non-/ base class methods //////////////


    public JSONMaryConfigLoader() throws MaryConfigurationException {
	super();
	InputStream input_stream = this.getClass().getResourceAsStream(MaryConfigurationFactory.DEFAULT_KEY + ".json");
	loadConfiguration(MaryConfigurationFactory.DEFAULT_KEY, input_stream);
    }

    /**
     * Configuration loading method
     *
     *  @param set the name of the configuration set for later reference in the configuration hash
     *  @param input_stream the stream containing the configuration
     */
    public void loadConfiguration(String set, InputStream input_stream) throws MaryConfigurationException {

	MaryConfiguration mc = new MaryConfiguration();
        try {
	    JSONObject root_config = (JSONObject)new JSONParser().parse(new InputStreamReader(input_stream));
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
		    }


		    // Check if method exists
		    prop_name = adaptPropertyName(prop_name);
		    assessMethod(class_name, prop_name, val.getClass());



		    mc.addConfigurationValueProperty(class_name, prop_name, val);
		}

	    }

	    MaryConfigurationFactory.addConfiguration(set, mc);
        } catch (Exception e) {
            throw new MaryConfigurationException("cannot load json configuration", e);
        }
    }


    public Object jsonArrayToList(JSONArray array) throws MaryConfigurationException {
	if (array.size() == 0)
	    throw new MaryConfigurationException("It is forbiddent to have an empty array in the configuration");

	ArrayList<Object> ar = new ArrayList<Object>();
	for (Object v: array)
	    ar.add(v);

	return ar;
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

    public String adaptPropertyName(String property_name) {
	property_name = property_name.substring(0,1).toUpperCase() + property_name.substring(1).toLowerCase();
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
