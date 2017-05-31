package marytts.features;

import java.util.Hashtable;

/**
 * This is the feature processor factory used by the
 * marytts.features.FeatureComputer
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class FeatureProcessorFactory {
	/** Map of feature processors identified by a name */
	private Hashtable<String, FeatureProcessor> feature_processors;

	/**
	 * Constructor
	 *
	 */
	public FeatureProcessorFactory() {
		feature_processors = new Hashtable<String, FeatureProcessor>();
	}

	/**
	 * Adding a feature processor identified by the given name and which is
	 * defined in the classe from the given name
	 *
	 * @param name
	 *            the name identifying the processor
	 * @param classname
	 *            the name of class of the processor
	 * @throws ClassNotFoundException
	 *             if the classname doesn't correspond to a class
	 * @throws InstantiationException
	 *             if you can't instantiate the class given by the classname. It
	 *             means the class should have a default constructor !
	 * @throws IllegalAccessException
	 *             if the defaultl constructor is not public
	 */
	public void addFeatureProcessor(String name, String classname)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		feature_processors.put(name, (FeatureProcessor) Class.forName(classname).newInstance());
	}

	/**
	 * From the name get the feature processor
	 *
	 * @param name
	 *            the name of the wanted processor
	 * @return the processor
	 * @throws UnknownProcessorException
	 *             if the processor name doesn't exists
	 */
	public FeatureProcessor createFeatureProcessor(String name) throws UnknownProcessorException {
		FeatureProcessor fp = feature_processors.get(name);
		if (fp == null)
			throw new UnknownProcessorException("Feature processor " + name + " doesn't exists");
		return fp;
	}
}
