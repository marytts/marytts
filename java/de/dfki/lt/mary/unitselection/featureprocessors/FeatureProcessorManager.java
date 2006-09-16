package de.dfki.lt.mary.unitselection.featureprocessors;

import java.util.HashMap;
import java.util.Map;

public class FeatureProcessorManager 
{
    // TODO: use this to replace UnitSelectionFeatProcManager et al.
    
    protected Map processors;
    
    public FeatureProcessorManager()
    {
        processors = new HashMap();
        addFeatureProcessor(new MaryGenericFeatureProcessors.Accented());
        addFeatureProcessor(new MaryGenericFeatureProcessors.AccentedSylIn());
        addFeatureProcessor(new MaryGenericFeatureProcessors.WordNumSyls());
        // TODO: add all relevant generic feature processors

    }
    
    protected void addFeatureProcessor(MaryFeatureProcessor fp)
    {
        processors.put(fp.getName(), fp);
    }
    
    public MaryFeatureProcessor getFeatureProcessor(String name)
    {
        return (MaryFeatureProcessor) processors.get(name);
    }
}
