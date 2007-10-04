package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;

import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.util.DoubleDataSource;

public interface AudioEffect {
    
    String getName(); //Returns the unique name of the effect
    String getExampleParameters(); //Returns typical parameters for the effect
    String getHelpText(); //Returns the help text for the effect
    
    float expectFloatParameter(String strParamName);
    double expectDoubleParameter(String strParamName);
    int expectIntParameter(String strParamName);
    
    public DoubleDataSource apply(DoubleDataSource input, String param);
    public DoubleDataSource process(DoubleDataSource input);
}
