package de.dfki.lt.signalproc.effects;

import de.dfki.lt.signalproc.process.Chorus;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.window.Window;

public class StadiumEffect extends ChorusEffectBase {
    
    float amount;
    public static float DEFAULT_AMOUNT = 100.0f;
    public static float MAX_AMOUNT = 200.0f;
    public static float MIN_AMOUNT = 0.0f;
    
    public StadiumEffect()
    {
        this(16000);
    }
    
    public StadiumEffect(int samplingRate)
    {
        super(samplingRate);
        
        delaysInMiliseconds = new int[2];
        delaysInMiliseconds[0] = 466;
        delaysInMiliseconds[1] = 600;
        
        amps = new double[2];
        amps[0] = 0.54;
        amps[1] = -0.10;
        
        setExampleParameters("amount" + chParamEquals + "100.0");
        
        strHelpText = getHelpText();

        initialise();
    }
    
    public void parseParameters(String param)
    {      
        super.parseChildParameters(param);
        
        if (param!="")
        {
            amount = expectFloatParameter("amount");

            if (amount == NULL_FLOAT_PARAM)
                amount = DEFAULT_AMOUNT;

            amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);

            for (int i=0; i<delaysInMiliseconds.length; i++)
                delaysInMiliseconds[i] = (int)(delaysInMiliseconds[i]*amount/100.0f);
        }
        
        initialise();
    }

    public String getHelpText() {

        String strHelp = "Stadium Effect:" + strLineBreak +
                         "Adds stadium effect by applying a specially designed multi-tap chorus." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <amount>" +
                         "   Definition : The amount of stadium effect at the output" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "Stadium";
    }
}
