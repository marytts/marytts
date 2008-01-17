package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

public class RobotiserEffect extends BaseAudioEffect {
    float amount;
    public static float DEFAULT_AMOUNT = 100.0f;
    public static float MAX_AMOUNT = 100.0f;
    public static float MIN_AMOUNT = 0.0f;
    
    public RobotiserEffect()
    {
        this(16000);
    }
    
    public RobotiserEffect(int samplingRate)
    {
        super(samplingRate);
        
        //setExampleParameters("amount" + chEquals + "100.0" + chSeparator);
        setExampleParameters("amount=100.0,");
        
        strHelpText = getHelpText();
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        amount = expectFloatParameter("amount");
        
        if (amount == NULL_FLOAT_PARAM)
            amount = DEFAULT_AMOUNT;
        
        amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);
    }
    
    public DoubleDataSource process(DoubleDataSource input)
    {
        Robotiser robotiser = new Robotiser(input, fs, amount/100.0f);
        return new BufferedDoubleDataSource(robotiser);
    }

    public String getHelpText() {
        
        String strHelp = "Robotiser Effect:" + strLineBreak +
                         "Creates a robotic voice by setting all phases to zero." + strLineBreak +
        		         "Parameter:" + strLineBreak +
        		         "   <amount>" +
        		         "   Definition : The amount of robotic voice at the output" + strLineBreak +
        		         "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak +
        		         "Example:" + strLineBreak +
        		         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "Robot";
    }
}
