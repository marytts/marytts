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
    private HashMap<StringPair, String> m_configuration_value_map;

    /** Map which associate a couple (class, property) to the stream of the property of the class */
    private HashMap<StringPair, InputStream> m_configuration_stream_map;

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
        m_configuration_value_map = new HashMap<StringPair, String>();
        m_configuration_stream_map = new HashMap<StringPair, InputStream>();
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
        m_configuration_value_map = new HashMap<StringPair, String>();
        m_configuration_stream_map = new HashMap<StringPair, InputStream>();
        m_is_strict = is_strict;
        logger = LogManager.getLogger(this);
    }

    /**
     *  Add the configuration for a given class
     *
     *  @param class_name the name of the class
     *  @param map_property_values the map which associates the property and its value
     */
    public void addConfigurationClassValues(String class_name, HashMap<String, String> map_property_values) {
        if (!m_class_property_map.containsKey(class_name)) {
            m_class_property_map.put(class_name, new HashSet<String>());
        }

        for (Map.Entry<String, String> entry : map_property_values.entrySet()) {
            String property = entry.getKey();
            property = property.substring(0, 1).toUpperCase() + property.substring(1).toLowerCase();
            m_class_property_map.get(class_name).add(property);
            m_configuration_value_map.put(new StringPair(class_name, property), entry.getValue());
        }
    }

    /**
     *  Add the configuration for a given class
     *
     *  @param class_name the name of the class
     *  @param map_property_values the map which associates the property and its streams
     */
    public void addConfigurationClassStreams(String class_name, HashMap<String, InputStream> map_property_values) {
        if (!m_class_property_map.containsKey(class_name)) {
            m_class_property_map.put(class_name, new HashSet<String>());
        }

        for (Map.Entry<String, InputStream> entry : map_property_values.entrySet()) {
            String property = entry.getKey();
            property = property.substring(0, 1).toUpperCase() + property.substring(1).toLowerCase();
            m_class_property_map.get(class_name).add(property);
            m_configuration_stream_map.put(new StringPair(class_name, property), entry.getValue());
        }
    }

    /**
     *  Add the configuration for a given class and a given property
     *
     *  @param class_name the name of the class
     *  @param property the name of the property
     *  @param value the value of the property for the class
     */
    public void addConfigurationValueProperty(String class_name, String property, String value) {
        property = property.substring(0, 1).toUpperCase() + property.substring(1).toLowerCase();
        if (!m_class_property_map.containsKey(class_name)) {
            m_class_property_map.put(class_name, new HashSet<String>());
        }

        m_class_property_map.get(class_name).add(property);
        m_configuration_value_map.put(new StringPair(class_name, property), value);
    }

    /**
     *  Add the configuration for a given class and a given property
     *
     *  @param class_name the name of the class
     *  @param property the name of the property
     *  @param value the value of the property for the class
     */
    public void addConfigurationStreamProperty(String class_name, String property, InputStream value) {
        property = property.substring(0, 1).toUpperCase() + property.substring(1).toLowerCase();
        if (!m_class_property_map.containsKey(class_name)) {
            m_class_property_map.put(class_name, new HashSet<String>());
        }

        m_class_property_map.get(class_name).add(property);
        m_configuration_stream_map.put(new StringPair(class_name, property), value);
    }

    /**
     *  Apply the configuration to a given object
     *
     *  @param obj the given object
     *  @throws MaryConfiguration if the configuration failed
     */
    public void applyConfiguration(Object obj) throws MaryConfigurationException {

        try {
            String class_name = obj.getClass().toString();
            Set<String> m_properties = m_class_property_map.get(class_name);

            for (String property : m_properties) {
		boolean b_is_value = false;
                try {
                    Method m = obj.getClass().getMethod("set" + property, String.class);
                    m.invoke(obj, m_configuration_value_map.get(new StringPair(class_name, property)));
		    b_is_value = true;
                } catch (NoSuchMethodException ex) {
                }


		// FIXME: see for "cloning the stream"
		if (! b_is_value) {
		    try {
			Method m = obj.getClass().getMethod("set" + property, InputStream.class);
			m.invoke(obj,
				 m_configuration_stream_map.get(new StringPair(class_name, property)));
			b_is_value = true;
		    } catch (NoSuchMethodException ex) {
			if (m_is_strict) {
			    throw ex;
			}

			logger.warn("Object of class \"" + obj.getClass().toString() +
				    "\" doesn't have a setter for property \"" + property.toLowerCase() + "\"");
		    }
		}
            }

        } catch (Exception ex) {
            throw new MaryConfigurationException("Configuration to object of class \"\"" + obj.getClass().toString() + "\" failed",
                                    ex);
        }
    }
}


/* MaryConfiguration.java ends here */
