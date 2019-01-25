package marytts.config;

/* Stream */
import java.io.InputStream;

/* Collections */
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import java.util.HashSet;
import java.util.ArrayList;

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
    /** String to indicate that the following value is refering to a stored configuration */
    public static final String REF_HEADER = "REF:";

    /** Key to indicate the baseline configuration to use (the default is activated by default) */
    public static final String BASELINE_PROPERTY_ID = "baseline";

    /** The logger */
    protected Logger logger;

    /** Baseline of the configuration */
    private String baseline;

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
     * Default constructor. By default, the configuration is not strict.
     *
     */
    public MaryConfiguration() {
        m_class_property_map = new HashMap<String, Set<String>>();
        m_configuration_value_map = new HashMap<StringPair, Object>();
        m_is_strict = false;
        setBaseline(MaryConfigurationFactory.DEFAULT_KEY);
        logger = LogManager.getLogger(this);
    }

    /**
     * Constructor to define the strictness of the configuration
     *
     * @param strict the strictness of the configuration (@see m_is_strict)
     */
    public MaryConfiguration(boolean is_strict) {
        this();
        m_is_strict = is_strict;
    }


    /**
     * Default constructor. By default, the configuration is not strict.
     *
     */
    public MaryConfiguration(String baseline) {
        this();
        setBaseline(baseline);
    }

    /**
     * Constructor to define the strictness of the configuration
     *
     * @param strict the strictness of the configuration (@see m_is_strict)
     */
    public MaryConfiguration(String baseline, boolean is_strict) {
        this(baseline);
        m_is_strict = is_strict;
    }

    public String getBaseline() {
        return baseline;
    }

    public void setBaseline(String baseline) {
        this.baseline = baseline;
    }

    public Set<String> getReferences() {
        Set<String> ids = new HashSet<String>();

        for (String class_name: m_class_property_map.keySet()) {
            for (String property: m_class_property_map.get(class_name)) {
                StringPair key = new StringPair(class_name, property);

                // Reference found!
                Object val = m_configuration_value_map.get(key);
                if ((val instanceof String) &&
                    ((String) val).startsWith(REF_HEADER)) {
                    String id = ((String) val).substring(REF_HEADER.length());
                    ids.add(id);
                }
            }
        }

        return ids;
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
     *  @param mc the other MaryConfiguration object
     */
    public void merge(MaryConfiguration mc) {
	m_class_property_map.putAll(mc.m_class_property_map);
	m_configuration_value_map.putAll(mc.m_configuration_value_map);
    }

    /**
     *  Integrate all the information from a given configuration to the current one by keeping what
     *  has already been filled
     *
     *  @param mc the given configuration
     */
    public void fill(MaryConfiguration mc) {
        for (String class_name: mc.m_class_property_map.keySet()) {

            // Fill the class if needed
            if (!m_class_property_map.keySet().contains(class_name)) {
                m_class_property_map.put(class_name, new HashSet<String>());
            }

            // fill the properties
            for (String property: mc.m_class_property_map.get(class_name)) {
                StringPair key = new StringPair(class_name, property);
                if (! m_configuration_value_map.containsKey(key)) {
                    m_class_property_map.get(class_name).add(property);
                    m_configuration_value_map.put(key, mc.m_configuration_value_map.get(key));
                }
            }
        }
    }

    /**
     *  Resolving a reference for a specific configuration
     *
     *  @param id the id of the configuration object
     *  @param mc the configuration object
     *  @throws MaryConfigurationException if a reference is done to an unknown field fo the given
     *  conference
     */
    public void resolve(String id, MaryConfiguration mc) throws MaryConfigurationException
    {
        for (String class_name: m_class_property_map.keySet()) {
            for (String property: m_class_property_map.get(class_name)) {
                StringPair key = new StringPair(class_name, property);

                // Reference found!
                Object val = m_configuration_value_map.get(key);
                if ((val instanceof String) && ((String) val).equals(REF_HEADER  + id)) {

                    // Sanity check
                    if (! mc.m_configuration_value_map.containsKey(key)) {
                        String msg = String.format("Configuration \"%s\" doesn't contain any value for the pair <%s, %s>",
                                                   id, class_name, property);
                        throw new MaryConfigurationException(msg);
                    }

                    m_configuration_value_map.put(key, mc.m_configuration_value_map.get(key));
                } else if (val instanceof ArrayList) {

                    // NOTE: Assumption is that the reference is containing the same kind of objects that the current value
                    ArrayList<Object> new_val = new ArrayList<Object>();
                    for (Object o: (ArrayList) val) {
                        if ((o instanceof String) && ((String) o).equals(REF_HEADER  + id)) {
                            Object other_o = mc.m_configuration_value_map.get(key);

                            // Merge list
                            if (other_o instanceof ArrayList) {
                                for (Object tmp: (ArrayList) other_o)  {
                                    new_val.add(tmp);
                                }
                            }
                            // Add object
                            else {
                                new_val.add(other_o);
                            }

                        } else {
                            new_val.add(o);
                        }
                    }

                    m_configuration_value_map.put(key, new_val);
                }
            }
        }
    }

    public void resolve() throws MaryConfigurationException {

        for (String class_name: m_class_property_map.keySet()) {
            for (String property: m_class_property_map.get(class_name)) {
                StringPair key = new StringPair(class_name, property);

                // Reference found!
                Object val = m_configuration_value_map.get(key);
                if ((val instanceof String) && ((String) val).startsWith(REF_HEADER)) {

                    // Get Id
                    String id = ((String) val).substring(REF_HEADER.length());

                    // Use Factory to get the configuration object
                    MaryConfiguration mc = MaryConfigurationFactory.getConfiguration(id);

                    // Sanity check
                    if (! mc.m_configuration_value_map.containsKey(key)) {
                        String msg = String.format("Configuration \"%s\" doesn't contain any value for the pair <%s, %s>",
                                                   id, class_name, property);
                        throw new MaryConfigurationException(msg);
                    }

                    m_configuration_value_map.put(key, mc.m_configuration_value_map.get(key));
                } else if (val instanceof ArrayList) {

                    // NOTE: Assumption is that the reference is containing the same kind of objects that the current value
                    ArrayList<Object> new_val = new ArrayList<Object>();
                    for (Object o: (ArrayList) val) {
                        if ((o instanceof String) && ((String) o).startsWith(REF_HEADER)) {
                            // Get the reference configuration object for the current property
                            String id = ((String) o).substring(REF_HEADER.length());
                            MaryConfiguration mc = MaryConfigurationFactory.getConfiguration(id);
                            Object other_o = mc.m_configuration_value_map.get(key);

                            // Merge list
                            if (other_o instanceof ArrayList) {
                                for (Object tmp: (ArrayList) other_o)  {
                                    new_val.add(tmp);
                                }
                            }
                            // Add object
                            else {
                                new_val.add(other_o);
                            }

                        } else {
                            new_val.add(o);
                        }
                    }

                    m_configuration_value_map.put(key, new_val);
                }
            }
        }
    }

    /**
     *  Apply the configuration to a given object
     *
     *  @param obj the given object
     *  @throws MaryConfiguration if the configuration failed
     */
    public void applyConfiguration(Object obj) throws MaryConfigurationException {

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
                setProperty(obj, property, new HashSet<String>());
            }
        } catch (Exception ex) {
            if (obj == null) {
                throw new MaryConfigurationException("The input object is null", ex);
            } else {
                throw new MaryConfigurationException("Configuration to object of class \"\"" +
                                                     obj.getClass().toString() + "\" failed", ex);
            }
        }
    }

    /**
     *  Set the property value to a given object
     *
     *  @param obj the given object
     *  @param property the property name
     *  @param ref_visited the set which prevents circle in the references
     *  @throw MaryConfigurationException if something goes wrong
     */
    protected void setProperty(Object obj, String property, HashSet<String> ref_visited) throws MaryConfigurationException {
        try {
            Object val = m_configuration_value_map.get(new StringPair(obj.getClass().getName(), property));

            // Deal with reference
            if ((val instanceof String) && (((String) val).startsWith(REF_HEADER))) {
                throw new MaryConfigurationException(String.format("the reference to \"%s\" is unsolved!", (String) val));
            }
            // Just set the values
            else {
                try {

                    logger.debug(String.format("Set property \"%s\" value to to \"%s\"", property, val.toString()));
                    Method m = obj.getClass().getMethod("set" + property, val.getClass());
                    m.invoke(obj, val);
                } catch (NoSuchMethodException ex) {
                    logger.warn("Object of class \"" + obj.getClass().toString() +
                                "\" doesn't have a setter for property \"" + property.toLowerCase() +
                                "\" with expected argument of class \"" + val.getClass().toString() + "\"");
                }
            }
        } catch (Exception ex) {
            if (obj == null) {
                throw new MaryConfigurationException("The input object is null", ex);
            } else {
                throw new MaryConfigurationException("Setting the property \"" + property + "\" of the object of class \"\"" + obj.getClass().toString() + "\" failed", ex);
            }
        }
    }

    /**
     *  Generate a string of the configuration for debug purpose
     *
     *  @return the debug string
     */
    @Override
    public String toString() {
        HashMap<String, HashMap<String, Object>> map_output = new HashMap<String, HashMap<String, Object>>();


	for (StringPair key: m_configuration_value_map.keySet()) {
            if (! map_output.containsKey(key.getLeft())) {
                map_output.put(key.getLeft(), new HashMap<String, Object>());
            }

            map_output.get(key.getLeft()).put(key.getRight(), m_configuration_value_map.get(key));
        }
	String configuration_str = "{\n";
        for (String key_class: map_output.keySet()) {
            configuration_str += String.format("\t\"%s\": {\n", key_class);
            for (String key_prop: map_output.get(key_class).keySet()) {
                configuration_str += String.format("\t\t\"%s\": %s\n", key_prop, map_output.get(key_class).get(key_prop));
            }
            configuration_str += "\t}\n";
        }
        configuration_str += "}\n";

	return configuration_str;
    }
}
