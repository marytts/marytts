package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.signalproc.process.Chorus;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.process.VocalTractScalingProcessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class ChorusEffectBase extends BaseAudioEffect {

    int [] delaysInMiliseconds;
    double [] amps;
    int frameLength;
    int maxDelayInMiliseconds;
    int maxDelayInSamples;
    int numTaps;
    static int MAX_TAPS = 20;
    static int MIN_DELAY = 0;
    static int MAX_DELAY = 5000;
    static double MIN_AMP = -5.0;
    static double MAX_AMP = 5.0;
    
    public ChorusEffectBase(int samplingRate)
    {
        super(samplingRate);
    }
    
    public void parseChildParameters(String param)
    {
        super.parseParameters(param);
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        int i;
        int [] tmpDelays = new int[MAX_TAPS];
        double [] tmpAmps = new double[MAX_TAPS];
        String strSearch;
        numTaps = 0;
        
        for (i=0; i<MAX_TAPS; i++)
        {
            strSearch = "delay" + String.valueOf(i+1);
            tmpDelays[i] = expectIntParameter(strSearch);
            
            if (tmpDelays[i]>NULL_INT_PARAM)
            {
                numTaps++;
                
                strSearch = "amp" + String.valueOf(i+1);
                tmpAmps[i] = expectDoubleParameter(strSearch);
                
                if (tmpAmps[i]==NULL_DOUBLE_PARAM)
                    tmpAmps[i] = 0.5;
            }
        }
        
        if (numTaps>0)
        {
            delaysInMiliseconds = new int[numTaps];
            amps = new double[numTaps];
            int tapInd = 0;
            
            for (i=0; i<MAX_TAPS; i++)
            {
                if (tmpDelays[i]>NULL_INT_PARAM)
                {
                    if (tapInd<numTaps)
                    {
                        delaysInMiliseconds[tapInd] = tmpDelays[i];
                        amps[tapInd] = tmpAmps[i];
                        tapInd++;
                    }
                    else
                        break;
                }
            }
        }
        else
        {
            delaysInMiliseconds = null;
            amps = null;
        }    

        initialise();
    }
    
    public void initialise()
    {
        if (delaysInMiliseconds!=null)
        {
            numTaps = delaysInMiliseconds.length;
            
            if (numTaps>0)
            {
                for (int i=0; i<numTaps; i++)
                {
                    delaysInMiliseconds[i] = MathUtils.CheckLimits(delaysInMiliseconds[i], MIN_DELAY, MAX_DELAY);
                    amps[i] = MathUtils.CheckLimits(amps[i], MIN_AMP, MAX_AMP);
                }
                    
                maxDelayInMiliseconds = MathUtils.getMax(delaysInMiliseconds);
                maxDelayInSamples = (int)(maxDelayInMiliseconds/1000.0*fs);
                
                frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
                if (frameLength<maxDelayInSamples)
                    frameLength *= 2;
            }
        }
    }
    
    public DoubleDataSource process(DoubleDataSource input)
    {
        Chorus chorus = new Chorus(delaysInMiliseconds, amps, fs);
        
        FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANN, true, 1024, fs, chorus);
        
        return new BufferedDoubleDataSource(foas);
    }
    
    public String getExampleParameters() {
        String strParam = "delay1=466; amp1=0.54; delay2=600; amp2=-0.10; delay3=250; amp3=0.30;";
        
        return strParam;
    }

    public String getHelpText() {
        String strHelp = "Multi-Tap Chorus Effect:\n\n" +
                         "Adds chorus effect by summing up the original signal with delayed and amplitude scaled versions.\n\n" +
                         "The parameters should consist of delay and amplitude pairs for each tap.\n" +
                         "A variable number of taps (max 20) can be specified by simply defining more delay-amplitude pairs.\n" +
                         "Each tap outputs a delayed and gain-scaled version of the original signal.\n" +
                         "All tap outputs are summed up with the oiginal signal with appropriate gain normalization.\n\n" +
                         "Parameters:\n" +
                         "   <delay1>\n" +
                         "   Definition : The amount of delay in miliseconds for tap #1\n" +
                         "   Range      : [0,5000]\n\n" +
                         "   <amp1>\n" +
                         "   Definition : Relative amplitude of the channel gain as compared to original signal gain for tap #1\n" +
                         "   Range      : [-5.0,5.0]\n\n" +
                         "   <delay2>\n" +
                         "   Definition : The amount of delay in miliseconds in delayed channel #2\n" +
                         "   Range      : [0,5000]\n\n" +
                         "   <amp2>\n" +
                         "   Definition : Relative amplitude of the channel gain as compared to original signal gain for delayed channel #2\n" +
                         "   Range      : [-5.0,5.0]\n\n" +
                         "   ...\n\n" +
                         "   <delayN>\n" +
                         "   Definition : The amount of delay in miliseconds in delayed channel #N\n" +
                         "   Range      : [0,5000]\n\n" +
                         "   <ampN>\n" +
                         "   Definition : Relative amplitude of the channel gain as compared to original signal gain for delayed channel #N\n" +
                         "   Range      : [-5.0,5.0]\n\n" +
                         "   Note: Maximum possible number of taps is N=20. Parameters for more taps will simply be neglected.\n" +
                         "\n" +
                         "Example: (A three-tap chorus effect)\n" + 
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "Chorus";
    }
    
    public DoubleDataSource apply(BufferedDoubleDataSource input)
    {
        Chorus c = new Chorus(delaysInMiliseconds, amps, fs);

        FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANN, true, frameLength, fs, c);
        
        return (DoubleDataSource)(new BufferedDoubleDataSource(foas));
    }
}
