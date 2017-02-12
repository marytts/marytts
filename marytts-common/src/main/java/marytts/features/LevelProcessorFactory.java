package marytts.features;

import java.util.Hashtable;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class LevelProcessorFactory
{
    private Hashtable<String, LevelProcessor> level_processors;

    public LevelProcessorFactory()
    {
        level_processors = new Hashtable<String, LevelProcessor>();
    }

    public void addLevelProcessor(String name, String classname)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        level_processors.put(name, (LevelProcessor) Class.forName(classname).newInstance());
    }

    public LevelProcessor createLevelProcessor(String name)
    {
        return level_processors.get(name);
    }
}
