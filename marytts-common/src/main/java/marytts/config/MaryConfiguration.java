package marytts.config;

/* Stream */
import java.io.InputStream;

/* Collections */
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/* Reflection */
import java.lang.reflect.Method;

/* Exceptions */
import marytts.exceptions.MaryConfigurationException;

/* Logging */
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;

/**
 * Configuration class
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class MaryConfiguration {
    /** The logger */
    protected Logger logger;

    /** Map to associate a class and a list of properties to define during the configuration stage */
    private HashMap<String, Set<String>> m_class_property_map;

    /** Map which associate a couple (class, property) to the actual value of the property of the class */
    private HashMap<StringPair, Object> m_configuration_value_map;


    /**
     * Boolean to see indicate if the behaviour of the configuration
     * is to throw an exception if the property doesn't have a setter
     * in the class
     */
    protected boolean m_is_strict;

    /**
     * Default constructor. By default, the configuration is not strict
     *
     */
    public MaryConfiguration() {
        m_class_property_map = new HashMap<String, Set<String>>();
        m_configuration_value_map = new HashMap<StringPair, Object>();
        m_is_strict = false;
        logger = LogManager.getLogger(this);
    }

    /**
     * Constructor to define the strictness of the configuration
     *
     * @param strict the strictness of the configuration (@see m_is_strict)
     */
    public MaryConfiguration(boolean is_strict) {
        m_class_property_map = new HashMap<String, Set<String>>();
        m_configuration_value_map = new HashMap<StringPair, Object>();
        m_is_strict = is_strict;
        logger = LogManager.getLogger(this);
    }

    /**
     *  Add the configuration for a given class
     *
     *  @param class_name the name of the class
     *  @param map_property_values the map which associates the property and its value
     */
    public void addConfigurationClassValues(String class_name, HashMap<String, Object> map_property_values) {
        if (!m_class_property_map.containsKey(class_name)) {
            m_class_property_map.put(class_name, new HashSet<String>());
        }

        for (Map.Entry<String, Object> entry : map_property_values.entrySet()) {
            String property = entry.getKey();
            m_class_property_map.get(class_name).add(property);
            m_configuration_value_map.put(new StringPair(class_name, property), entry.getValue());
        }
    }

    /**
     *  Add the configuration for a given class and a given property
     *
     *  @param class_name the name of the class
     *  @param property the name of the property
     *  @param value the value of the property for the class
     */
    public void addConfigurationValueProperty(String class_name, String property, Object value) {
        if (!m_class_property_map.containsKey(class_name)) {
            m_class_property_map.put(class_name, new HashSet<String>());
        }

        m_class_property_map.get(class_name).add(property);
        m_configuration_value_map.put(new StringPair(class_name, property), value);
    }

    /**
     *  Merging configuration from another MaryConfiguration object to the current one
     *  The current one has the priority if conflicts
     *
     *  @param mc2 the other MaryConfiguration object
     */
    public void merge(MaryConfiguration mc2) {
	m_class_property_map.putAll(mc2.m_class_property_map);
	m_configuration_value_map.putAll(mc2.m_configuration_value_map);
    }

    /**
     *  Apply the configuration to a given object
     *
     *  @param obj the given object
     *  @throws MaryConfiguration if the configuration failed
     */
    public synchronized void applyConfiguration(Object obj) throws MaryConfigurationException {
        try {
            String class_name = obj.getClass().getName();
            Set<String> properties = m_class_property_map.get(class_name);
	    if (properties == null)
		return;

	    // Logger is always comming first, and just the level is defined
	    if (properties.remove("logger_level")) {
		String property = "logger_level";
		Object val = null;
                try {
		    val = m_configuration_value_map.get(new StringPair(class_name, property));
                    Method m = obj.getClass().getMethod("set" + property, val.getClass());
                    m.invoke(obj, val);
                } catch (NoSuchMethodException ex) {
		    logger.warn("Object of class \"" + obj.getClass().toString() +
				"\" doesn't have a setter for property \"" + property.toLowerCase() +
				"\" with expected argument of class \"" + val.getClass().toString() + "\"");
                }
	    }

	    // Other parts
            for (String property : properties) {
		Object val = null;
                try {
		    val = m_configuration_value_map.get(new StringPair(class_name, property));
                    Method m = obj.getClass().getMethod("set" + property, val.getClass());
                    m.invoke(obj, val);
                } catch (NoSuchMethodException ex) {
		    logger.warn("Object of class \"" + obj.getClass().toString() +
				"\" doesn't have a setter for property \"" + property.toLowerCase() +
				"\" with expected argument of class \"" + val.getClass().toString() + "\"");
                }
            }

        } catch (Exception ex) {
            throw new MaryConfigurationException("Configuration to object of class \"\"" + obj.getClass().toString() + "\" failed",
                                    ex);
        }
    }

    @Override
    public String toString() {
	String configuration_str = "";
	for (StringPair key: m_configuration_value_map.keySet()) {
	    configuration_str += "(" + key.getLeft() + "," + key.getRight() + ") => " + m_configuration_value_map.get(key) + ";\n";
	}

	return configuration_str;
    }
}


/* MaryConfiguration.java ends here */
