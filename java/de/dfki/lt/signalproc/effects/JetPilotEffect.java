package de.dfki.lt.signalproc.effects;

public class JetPilotEffect extends FilterEffectBase {
    
    public JetPilotEffect()
    {
        this(16000);
    }

    public JetPilotEffect(int samplingRate)
    {
        super(500.0, 2000.0, samplingRate, BANDPASS_FILTER);
        
        setExampleParameters("");
        
        strHelpText = getHelpText();
    }
    

    public void parseParameters(String param)
    {
        initialise();
    }
    
    public String getHelpText() {
        
        String strHelp = "Jet pilot effect:" + strLineBreak +
                         "Filters the input signal using an FIR bandpass filter." + strLineBreak +
                         "Parameters: NONE" + strLineBreak;
                        
        return strHelp;
    }

    public String getName() {
        return "JetPilot";
    }
}
