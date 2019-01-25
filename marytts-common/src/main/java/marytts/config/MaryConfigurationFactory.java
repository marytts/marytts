package marytts.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import marytts.exceptions.MaryConfigurationException;

import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * The MaryConfiguration factory. All methods are static are we don't
 * need multiple instances of it.
 *
 * @author <a href="mailto:slemaguer@slemaguer-perso"></a>
 */
public class MaryConfigurationFactory
{
    /**
     *  The priority between kind of representation
     *
     */
    public enum Priority {
        /** Explicit reference */
        REFERENCE  (20),

        /** Baseline configuration */
        BASELINE   (10),

        /** Default configuration has the less priority */
        DEFAULT    (0)
        ;

        /** The code of the priority */
        private final int priority_code;

        /**
         *  Constructor
         *
         *  @param priority_code the code of the priority
         */
        Priority(int priority_code) {
            this.priority_code = priority_code;
        }

        /**
         *  Method to get the code of the priority
         *
         *  @return the code of the priority
         */
        public int getPriorityCode() {
            return this.priority_code;
        }
    }

    /** The default configuration name in the map */
    public static final String DEFAULT_KEY = "default";

    /** Graph linking the configurations */
    private static SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> configuration_graph;

    /** The map containing the configuration */
    private static HashMap<String, MaryConfiguration> configuration_map;

    /* Initialization */
    static {
        configuration_map = new HashMap<String, MaryConfiguration>();
        configuration_map.put(DEFAULT_KEY, new MaryConfiguration());
        configuration_graph = null;
    }

    /**
     *  Method to add a configuration to a set. A set can be a locale or anything else
     *
     *  @param set the set
     *  @param configuration the configuration to add
     *  @fixme maybe avoid overriding default?
     */
    public synchronized static void addConfiguration(String set, MaryConfiguration configuration)
        throws MaryConfigurationException
    {
        addConfiguration(set, configuration, 0);
    }

    /**
     *  Method to add a configuration to a set. A set can be a locale or anything else
     *
     *  @param set the set
     *  @param configuration the configuration to add
     *  @fixme maybe avoid overriding default?
     */
    public synchronized static void addConfiguration(String set, MaryConfiguration configuration, int i)
        throws MaryConfigurationException
    {
	if (configuration_map.containsKey(set)) {
            configuration_map.get(set).merge(configuration);
	} else {
            configuration_map.put(set, configuration);
        }
    }

    /**
     *  Method to get the first configuration knowning the set
     *
     *  @param set the set
     *  @return the configuration
     *  @fixme see for cloning
     */
    public synchronized static MaryConfiguration getConfiguration(String set) {
        return configuration_map.get(set);
    }

    /**
     *  Method to get the default configuration
     *
     *  @return the default configuration
     */
    public synchronized static MaryConfiguration getDefaultConfiguration() {
	return configuration_map.get(MaryConfigurationFactory.DEFAULT_KEY);
    }

    /**
     *  Method to apply the default configuration to intialize a given object
     *
     *  @param obj the given object to initialize
     *  @param MaryConfigurationException if something is goinc wrong during the initialization
     */
    public synchronized static void applyDefaultConfiguration(Object obj) throws MaryConfigurationException {
	getDefaultConfiguration().applyConfiguration(obj);
    }

    /**
     *  Helper to debug the current available configuration in logger mainly
     *
     *  @return the formatted string of the current status of the factory map
     */
    public synchronized static String dump() {
	String result = "{\n";
	for (String key: configuration_map.keySet()) {
	    result += key + ": \n";
            result += "\t" + configuration_map.get(key).toString().replaceAll("\\\n", "\\\n\\\t");
	    result += "\n}\n";
	}
	return result;
    }

    /**
     *  Method to resolve the references and have "ready to go" configuration
     *
     *  @throws MaryConfigurationException if the references are already resolved or if there is an
     *  inconstency in the configurations given to the system
     */
    public synchronized static void resolveReferences() throws MaryConfigurationException {

        // Fill the graph
        ArrayList<String> start_nodes = fillGraph();

        // Resolve references
        for (String conf: start_nodes) {
            fillConfiguration(conf, new HashSet<String>(), new ArrayList<String>());
        }

        // Now apply default to everything
        MaryConfiguration default_conf = MaryConfigurationFactory.configuration_map.get(DEFAULT_KEY);
        for (String conf: MaryConfigurationFactory.configuration_map.keySet()) {
            if (conf.equals(DEFAULT_KEY))
                continue;

            MaryConfigurationFactory.configuration_map.get(conf).fill(default_conf);
        }
    }

    /**
     *  Method which genenerate the graphs based on the references listed from the configurations
     *
     *  @return the list of configuration which are not referencing anything and could be used as
     *  starting points.
     */
    private static ArrayList<String> fillGraph() {

        // Initialize everything
        ArrayList<String> start_nodes = new ArrayList<String>();
        MaryConfigurationFactory.configuration_graph = new SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);

        // Add default
        MaryConfigurationFactory.configuration_graph.addVertex(DEFAULT_KEY);

        // Generate the graph
        for (String key_conf: MaryConfigurationFactory.configuration_map.keySet()) {
            // default is ignored as it should not have any references!
            if (key_conf.equals(DEFAULT_KEY))
                continue;

            // Check if the key is already in the graph (so already referenced) or not
            if (! MaryConfigurationFactory.configuration_graph.containsVertex(key_conf))
                MaryConfigurationFactory.configuration_graph.addVertex(key_conf);

            // Get the configuration
            MaryConfiguration conf = MaryConfigurationFactory.configuration_map.get(key_conf);

            // Check that is a start node
            if ((conf.getBaseline().equals(DEFAULT_KEY)) && (conf.getReferences().size() == 0))
                start_nodes.add(key_conf);

            // Update the graph using explicit relations
            for (String ref: conf.getReferences()) {
                if (! MaryConfigurationFactory.configuration_graph.containsVertex(ref))
                    MaryConfigurationFactory.configuration_graph.addVertex(ref);

                MaryConfigurationFactory.configuration_graph.addEdge(key_conf, ref);
                MaryConfigurationFactory.configuration_graph.setEdgeWeight(key_conf, ref, Priority.REFERENCE.getPriorityCode());
            }

            // Update the graph with the baseline
            if (! conf.getBaseline().equals(DEFAULT_KEY)) {
                if (! MaryConfigurationFactory.configuration_graph.containsVertex(conf.getBaseline()))
                    MaryConfigurationFactory.configuration_graph.addVertex(conf.getBaseline());

                MaryConfigurationFactory.configuration_graph.addEdge(key_conf, conf.getBaseline());
                MaryConfigurationFactory.configuration_graph.setEdgeWeight(key_conf, conf.getBaseline(), Priority.BASELINE.getPriorityCode());
            }
        }

        // Return the configuration labels which are not refering to anything and so can be consider as final!
        return start_nodes;
    }

    /**
     *  The actual method which resolve the references of a configuration.
     *
     *  @param key_conf the source configuration id
     *  @param done the set of done configurations
     *  @param in_process the set of configurations in process (used for loop detection)
     *  @throws MaryConfigurationException if a loop is detected or a problem happen during the configuration merging
     */
    private static void fillConfiguration(String key_conf, Set<String> done, ArrayList<String> in_process) throws MaryConfigurationException {

        // We should not arrive here if the key_conf is already in process!
        if (in_process.contains(key_conf)) {
            String msg = String.format("Configuration \"%s\" is referencing it self through the path %s -> %s",
                                       key_conf, String.join("->", in_process), key_conf);
                throw new MaryConfigurationException(msg);
        }

        // Sanity check that there is an actual conference
        if (! MaryConfigurationFactory.configuration_map.containsKey(key_conf)) {
            String msg = String.format("Configuration \"%s\" doesn't have an equivalent configuration object",
                                       key_conf);
            throw new MaryConfigurationException(msg);
        }

        // Done so we ignore !
        if (done.contains(key_conf))
            return;

        in_process.add(key_conf);
        MaryConfiguration cur_conf = MaryConfigurationFactory.configuration_map.get(key_conf);

        String baseline_id = null; // Save baseline for after
        for (DefaultWeightedEdge edge: MaryConfigurationFactory.configuration_graph.incomingEdgesOf(key_conf)) {
            String ref_id = MaryConfigurationFactory.configuration_graph.getEdgeTarget(edge);
            // Baseline for later!
            if (MaryConfigurationFactory.configuration_graph.getEdgeWeight(edge) == Priority.BASELINE.getPriorityCode()) {
                baseline_id = ref_id;
                continue;
            }

            // Resolve first referenced configuration
            fillConfiguration(ref_id, done, in_process);

            // Resolve the references for current configuration
            cur_conf.resolve(ref_id, MaryConfigurationFactory.configuration_map.get(ref_id));
        }

        // Fill the baseline
        if (baseline_id != null) {
            cur_conf.fill(MaryConfigurationFactory.configuration_map.get(baseline_id));
        }

        // Indicate that everything is done
        done.add(key_conf);
    }
}
