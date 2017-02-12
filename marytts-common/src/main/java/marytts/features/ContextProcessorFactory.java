package marytts.features;

import java.util.Hashtable;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class ContextProcessorFactory
{
    private Hashtable<String, ContextProcessor> context_processors;

    public ContextProcessorFactory()
    {
        context_processors = new Hashtable<String, ContextProcessor>();
    }

    public void addContextProcessor(String name, String classname)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        context_processors.put(name, (ContextProcessor) Class.forName(classname).newInstance());
    }

    public ContextProcessor createContextProcessor(String name)
    {
        return context_processors.get(name);
    }
}
