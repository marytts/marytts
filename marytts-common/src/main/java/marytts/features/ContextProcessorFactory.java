package marytts.features;

import java.util.Hashtable;

/**
 * This is a context processor factory used by the marytts.features.FeatureComputer
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class ContextProcessorFactory {
    /** Map of context processors identified by a name */
	private Hashtable<String, ContextProcessor> context_processors;

    /**
     * Constructor
     *
     */
	public ContextProcessorFactory() {
		context_processors = new Hashtable<String, ContextProcessor>();
	}

    /**
     * Adding a context processor identified by the given name and which is defined in the classe from the given name
     * 
     * @param name the name identifying the processor
     * @param classname the name of class of the processor
     * @throws ClassNotFoundException if the classname doesn't correspond to a class
     * @throws InstantiationException if you can't instantiate the class given by the classname. It means the class should have a default constructor !
     * @throws IllegalAccessException if the defaultl constructor is not public
     */
	public void addContextProcessor(String name, String classname)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		context_processors.put(name, (ContextProcessor) Class.forName(classname).newInstance());
	}

    /**
     * From the name get the context processor
     * 
     * @param name the name of the wanted processor
     * @return the processor of null if the name doesn't correspond to a processor (FIXME: maybe replace by exception instead ?)
     */
	public ContextProcessor createContextProcessor(String name) {
		return context_processors.get(name);
	}
}
