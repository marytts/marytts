package marytts.features;

import java.util.Hashtable;

/**
 * This is the context processor factory used by the
 * marytts.features.FeatureComputer
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
     * Adding a context processor identified by the given name and which is
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
    public void addContextProcessor(String name, String classname)
    throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        context_processors.put(name, (ContextProcessor) Class.forName(classname).newInstance());
    }

    /**
     * From the name get the context processor
     *
     * @param name
     *            the name of the wanted processor
     * @return the processor
     * @throws UnknownProcessorException
     *             if the processor name doesn't exists
     */
    public ContextProcessor createContextProcessor(String name) throws UnknownProcessorException {
        ContextProcessor cp = context_processors.get(name);
        if (cp == null) {
            throw new UnknownProcessorException("Context processor " + name + " doesn't exists");
        }
        return cp;
    }


    /**
     * Check if we can create a context processor based on a given name
     *
     * @param name
     *            the name of the wanted processor
     * @return true if the processor exists, false else
     * @throws UnknownProcessorException
     *             if the processor name doesn't exists
     */
    public boolean canCreateContextProcessor(String name) throws UnknownProcessorException {
        ContextProcessor lp = context_processors.get(name);
        if (lp == null) {
            return false;
        }

        return true;
    }
}
