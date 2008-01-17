package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.VocalTractScalingProcessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class VocalTractLinearScalerEffect extends BaseAudioEffect {
    
    float amount;
    public static float MAX_AMOUNT = 4.0f;
    public static float MIN_AMOUNT = 0.25f;
    public static float DEFAULT_AMOUNT = 1.5f;
    
    public VocalTractLinearScalerEffect()
    {
        this(16000);
    }
    
    public VocalTractLinearScalerEffect(int samplingRate)
    {
        super(samplingRate);
        
        setExampleParameters("amount" + chParamEquals + Float.toString(DEFAULT_AMOUNT) + chParamSeparator);
        
        strHelpText = getHelpText();  
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        amount = expectFloatParameter("amount");
        
        if (amount == NULL_FLOAT_PARAM)
            amount = DEFAULT_AMOUNT;
    }
    
    public DoubleDataSource process(DoubleDataSource inputAudio)
    {        
        amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);
        
        double [] vscales = {amount};

        int frameLength = SignalProcUtils.getDFTSize(fs);
        int predictionOrder = SignalProcUtils.getLPOrder(fs);
        
        VocalTractScalingProcessor p = new VocalTractScalingProcessor(predictionOrder, fs, frameLength, vscales);
        FrameOverlapAddSource foas = new FrameOverlapAddSource(inputAudio, Window.HANN, true, frameLength, fs, p);
        
        return new BufferedDoubleDataSource(foas);
    }
    
    public String getHelpText() {

        String strHelp = "Vocal Tract Linear Scaling Effect:" + strLineBreak +
                         "Creates a shortened or lengthened vocal tract effect by shifting the formants." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <amount>" +
                         "   Definition : The amount of formant shifting" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak +
                         "   For values of <amount> less than 1.0, the formants are shifted to lower frequencies" + strLineBreak +
                         "       resulting in a longer vocal tract (i.e. a deeper voice)." + strLineBreak +
                         "   Values greater than 1.0 shift the formants to higher frequencies." + strLineBreak +
                         "       The result is a shorter vocal tract.\n" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "TractScaler";
    }
}
