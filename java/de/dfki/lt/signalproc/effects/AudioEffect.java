package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;

import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.util.DoubleDataSource;

public interface AudioEffect {    
    public String getName(); //Returns the unique name of the effect
    public void setName(String strName); //Sets the unique name of the effect 
    
    public String getExampleParameters(); //Returns typical parameters for the effect
    public void setExampleParameters(String strExampleParams); //Sets typical parameters for the effect
    
    public String getHelpText(); //Returns the help text for the effect
    public String getParamsAsString(); //Returns current parameters with parameter names and values 
                                       //  separated by a parameter separator character and surrounded by 
                                       //  parameter field start and end characters
    public String getParamsAsString(boolean bWithParantheses);
    
    public String getFullEffectAsString(); //Returns effect name, current parameters and their values
   
    public String getFullEffectWithExampleParametersAsString(); //Returns name with example parameters and values
    
    public float expectFloatParameter(String strParamName); //Return a float valued parameter from a string in the form param1=val1
    public double expectDoubleParameter(String strParamName); //Return a double valued parameter from a string in the form param1=val1
    public int expectIntParameter(String strParamName); //Return an integer valued parameter from a string in the form param1=val1
    
    public DoubleDataSource apply(DoubleDataSource input, String param);
    public DoubleDataSource process(DoubleDataSource input);
    
    public void setParams(String params);
    public String preprocessParams(String params);
    public void parseParameters(String param);
    public void checkParameters();
    
}
