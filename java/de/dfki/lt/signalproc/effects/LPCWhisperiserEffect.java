package de.dfki.lt.signalproc.effects;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import de.dfki.lt.signalproc.process.Chorus;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.window.Window;

/*

    AudioFormat audioformat = inputAudio.getFormat();
    int fs = (int)audioformat.getSampleRate();
    AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
    int frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
    int predictionOrder = Integer.getInteger("signalproc.lpcwhisperiser.predictionorder", 20).intValue();
    FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, frameLength, fs,
            new LPCWhisperiser(predictionOrder));
    return new DDSAudioInputStream(new BufferedDoubleDataSource(foas), audioformat);
*/

public class LPCWhisperiserEffect extends BaseAudioEffect {

    int frameLength;
    int predictionOrder;
    float amount;
    public static float DEFAULT_AMOUNT = 100.0f;
    public static float MAX_AMOUNT = 100.0f;
    public static float MIN_AMOUNT = 0.0f;
    
    public LPCWhisperiserEffect()
    {
        this(16000);
    }
    
    public LPCWhisperiserEffect(int samplingRate)
    {
        super(samplingRate);
        
        setExampleParameters("amount" + chParamEquals + "100.0" + chParamSeparator);
        
        strHelpText = getHelpText(); 
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        amount = expectFloatParameter("amount");
        
        if (amount == NULL_FLOAT_PARAM)
            amount = DEFAULT_AMOUNT;
        
        amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);
        
        frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
        predictionOrder = Integer.getInteger("signalproc.lpcwhisperiser.predictionorder", 20).intValue();
    }
    
    public DoubleDataSource process(DoubleDataSource input)
    {
        LPCWhisperiser whisperiser = new LPCWhisperiser(predictionOrder, amount/100.0f);
        
        FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANN, true, frameLength, fs, whisperiser);
        
        return new BufferedDoubleDataSource(foas);
    }

    public String getHelpText() {
        
        String strHelp = "Whisper Effect:" + strLineBreak +
                         "Creates a whispered voice by replacing the LPC residual with white noise." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <amount>" +
                         "   Definition : The amount of whisperised voice at the output" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "Whisper";
    }
}
