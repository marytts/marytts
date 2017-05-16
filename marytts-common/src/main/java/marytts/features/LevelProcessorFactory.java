package marytts.features;

import java.util.Hashtable;

/**
 * This is the level processor factory used by the
 * marytts.features.FeatureComputer
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le
 *         Maguer</a>
 */
public class LevelProcessorFactory {
	/** Map of level processors identified by a name */
	private Hashtable<String, LevelProcessor> level_processors;

	/**
	 * Constructor
	 *
	 */
	public LevelProcessorFactory() {
		level_processors = new Hashtable<String, LevelProcessor>();
	}

	/**
	 * Adding a level processor identified by the given name and which is
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
	public void addLevelProcessor(String name, String classname)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		level_processors.put(name, (LevelProcessor) Class.forName(classname).newInstance());
	}

	/**
	 * From the name get the level processor
	 *
	 * @param name
	 *            the name of the wanted processor
	 * @return the processor
	 * @throws UnknownProcessorException
	 *             if the processor name doesn't exists
	 */
	public LevelProcessor createLevelProcessor(String name) throws UnknownProcessorException {
		LevelProcessor lp = level_processors.get(name);
		if (lp == null)
			throw new UnknownProcessorException("Level processor " + name + " doesn't exists");
		return lp;
	}
}
