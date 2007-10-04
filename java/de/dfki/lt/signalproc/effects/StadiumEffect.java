package de.dfki.lt.signalproc.effects;

import de.dfki.lt.signalproc.util.MathUtils;

public class StadiumEffect extends ChorusEffectBase {
    
    float amount;
    static float DEFAULT_AMOUNT = 100.0f;
    static float MAX_AMOUNT = 200.0f;
    static float MIN_AMOUNT = 0.0f;
    
    public StadiumEffect(int samplingRate)
    {
        super(samplingRate);
        
        delaysInMiliseconds = new int[2];
        delaysInMiliseconds[0] = 466;
        delaysInMiliseconds[1] = 600;
        
        amps = new double[2];
        amps[0] = 0.54;
        amps[1] = -0.10;
        
        initialise();
    }
    
    public void parseParameters(String param)
    {      
        super.parseChildParameters(param);
        
        amount = expectFloatParameter("amount");
        
        if (amount == NULL_FLOAT_PARAM)
            amount = DEFAULT_AMOUNT;
        
        amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);
        
        for (int i=0; i<delaysInMiliseconds.length; i++)
            delaysInMiliseconds[i] = (int)(delaysInMiliseconds[i]*amount/100.0f);
        
        initialise();
    }
    
    public String getExampleParameters() {
        String strParam = "amount=100;";
        
        return strParam;
    }

    public String getHelpText() {
        String strHelp = "Stadium Effect:\n\n" +
                         "Adds stadium effect by applying a specially designed multi-tap chorus.\n\n" +
                         "Parameter:\n" +
                         "   <amount>" +
                         "   Definition : The amount of stadium effect at the output\n" +
                         "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]\n" +
                         "\n" +
                         "Example:\n" + 
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "Stadium";
    }
}
