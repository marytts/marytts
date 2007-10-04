package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.signalproc.filter.BandPassFilter;
import de.dfki.lt.signalproc.filter.BandRejectFilter;
import de.dfki.lt.signalproc.filter.HighPassFilter;
import de.dfki.lt.signalproc.filter.LowPassFilter;
import de.dfki.lt.signalproc.process.Chorus;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.process.VocalTractScalingProcessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class FilterEffectsBase extends BaseAudioEffect {
    
    double cutOffFreqInHz1;
    double cutOffFreqInHz2;
    int filterType;
    int frameLength;
    double normalizedCutOffFreq1;
    double normalizedCutOffFreq2;
    InlineDataProcessor filter;
    
    static int NULL_FILTER = 0;
    static int LOWPASS_FILTER = 1;
    static int HIGHPASS_FILTER = 2;
    static int BANDPASS_FILTER = 3;
    static int BANDREJECT_FILTER = 4;
    
    static int DEFAULT_FILTER = BANDPASS_FILTER;
    static double DEFAULT_CUTOFF1 = 500.0;
    static double DEFAULT_CUTOFF2 = 2000.0;
   
    //Unlike most of the other effect parameters the following are sampling rate dependent
    double MIN_CUTOFF1;
    double MAX_CUTOFF1;
    double MAX_CUTOFF2;
    double MIN_CUTOFF2;
    //
    
    public FilterEffectsBase(double cutOffHz, int samplingRate, int type)
    {
        super(samplingRate);
        
        cutOffFreqInHz1 = cutOffHz;
        cutOffFreqInHz2 = -1.0;
        filterType = type;
        
        MIN_CUTOFF1 = 20.0;
        MAX_CUTOFF1 = 0.5*samplingRate-20.0;
        MIN_CUTOFF2 = 20.0;
        MAX_CUTOFF2 = 0.5*samplingRate-20.0;
    }
    
    public FilterEffectsBase(double cutOffHz, int samplingRate)
    {
        this(cutOffHz, samplingRate, LOWPASS_FILTER);
    }
    
    public FilterEffectsBase(double cutOffHz1, double cutOffHz2, int samplingRate, int type)
    {
        super(samplingRate);
        
        cutOffFreqInHz1 = cutOffHz1;
        cutOffFreqInHz2 = cutOffHz2;
        filterType = type;
        
        MIN_CUTOFF1 = 20.0;
        MAX_CUTOFF1 = 0.5*samplingRate-20.0;
        MIN_CUTOFF2 = 20.0;
        MAX_CUTOFF2 = 0.5*samplingRate-20.0;
    }
    
    public FilterEffectsBase(double cutOffHz1, double cutOffHz2, int samplingRate)
    {
        this(cutOffHz1, cutOffHz2, samplingRate, BANDPASS_FILTER);
    }
    
    //A filter that does nothing
    public FilterEffectsBase(int samplingRate)
    {
        super(samplingRate);
        
        cutOffFreqInHz1 = -1.0;
        cutOffFreqInHz2 = -1.0;
        filterType = NULL_FILTER;
        
        MIN_CUTOFF1 = 20.0;
        MAX_CUTOFF1 = 0.5*samplingRate-20.0;
        MIN_CUTOFF2 = 20.0;
        MAX_CUTOFF2 = 0.5*samplingRate-20.0;
    }
    
    public String getExampleParameters() {
        String strParam = "type=" + String.valueOf(BANDPASS_FILTER) + "; " + 
                          "fc1=" + String.valueOf(DEFAULT_CUTOFF1) + "; " + 
                          "fc2=" + String.valueOf(DEFAULT_CUTOFF2) + "; ";
        
        return strParam;
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);

        filterType = expectIntParameter("type");
        
        if (filterType == NULL_INT_PARAM)
            filterType = DEFAULT_FILTER;
        
        if (filterType==LOWPASS_FILTER || filterType==HIGHPASS_FILTER)
            cutOffFreqInHz1 = expectDoubleParameter("fc1");
        else
            cutOffFreqInHz1 = expectDoubleParameter("fc1");
        
        if (cutOffFreqInHz1==NULL_DOUBLE_PARAM)
            cutOffFreqInHz1 = DEFAULT_CUTOFF1;

        cutOffFreqInHz2 = expectDoubleParameter("fc2");
        
        if (cutOffFreqInHz2==NULL_DOUBLE_PARAM)
            cutOffFreqInHz2 = DEFAULT_CUTOFF2;
        
        cutOffFreqInHz1 = MathUtils.CheckLimits(cutOffFreqInHz1, MIN_CUTOFF1, MAX_CUTOFF1);
        cutOffFreqInHz2 = MathUtils.CheckLimits(cutOffFreqInHz2, MIN_CUTOFF1, MAX_CUTOFF2);
        
        if (cutOffFreqInHz2<cutOffFreqInHz1)
        {
            double tmp = cutOffFreqInHz1;
            cutOffFreqInHz1 = cutOffFreqInHz2;
            cutOffFreqInHz2 = tmp;
        }

        initialise();
    }
    
    public void initialise()
    {
        frameLength = 8*SignalProcUtils.getDFTSize(fs);
        normalizedCutOffFreq1 = cutOffFreqInHz1/fs;
        normalizedCutOffFreq2 = cutOffFreqInHz2/fs;
        filter = null;
        
        if (filterType == LOWPASS_FILTER && normalizedCutOffFreq1>0.0)
            filter = new LowPassFilter(normalizedCutOffFreq1, true);
        else if (filterType == HIGHPASS_FILTER && normalizedCutOffFreq1>0.0)
            filter = new HighPassFilter(normalizedCutOffFreq1, true);
        else if (filterType == BANDPASS_FILTER && normalizedCutOffFreq1>0.0 && normalizedCutOffFreq2>0.0)
        {
            if (normalizedCutOffFreq1>normalizedCutOffFreq2)
            {
                double tmp = normalizedCutOffFreq1;
                normalizedCutOffFreq1 = normalizedCutOffFreq2;
                normalizedCutOffFreq2 = tmp;
            }
            
            filter = new BandPassFilter(normalizedCutOffFreq1, normalizedCutOffFreq2, true);
        }
        else if (filterType == BANDREJECT_FILTER  && normalizedCutOffFreq1>0.0 && normalizedCutOffFreq2>0.0)
        {
            if (normalizedCutOffFreq1>normalizedCutOffFreq2)
            {
                double tmp = normalizedCutOffFreq1;
                normalizedCutOffFreq1 = normalizedCutOffFreq2;
                normalizedCutOffFreq2 = tmp;
            }
            
            filter = new BandRejectFilter(normalizedCutOffFreq1, normalizedCutOffFreq2, true);
        }
    }
    
    public DoubleDataSource process(DoubleDataSource input)
    {
        if (filter != null)
        {
            FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANN, true, frameLength, fs, filter);
            
            return new BufferedDoubleDataSource(foas);
        }
        else
        {
            return input;
        }
    }

    public String getHelpText() {
        String strRange = "";
        for (int i=LOWPASS_FILTER; i<BANDREJECT_FILTER; i++)
            strRange += String.valueOf(i) + ",";
        
        strRange += String.valueOf(BANDREJECT_FILTER);
        
        String strHelp = "FIR filtering:\n\n" +
                         "Filters the input signal by an FIR filter.\n\n" +
                         "Parameters:\n" +
                         "   <type>" +
                         "   Definition : Type of filter (1:Lowpass, 2:Highpass, 3:Bandpass, 4:Bandreject)\n" +
                         "   Range      : {" + strRange + "}\n\n" +
                         "   <fc>" +
                         "   Definition : Cutoff frequency in Hz for lowpass and highpass filters\n" +
                         "   Range      : {" + strRange + "}\n\n" +
                         "   <fc1>" +
                         "   Definition : Lower frequency cutoff in Hz for bandpass and bandreject filters\n" +
                         "   Range      : {" + strRange + "}\n\n" +
                         "   <fc2>" +
                         "   Definition : Higher frequency cutoff in Hz for bandpass and bandreject filters\n" +
                         "   Range      : {" + strRange + "}\n" +
                         "\n" +
                         "Example:\n" + 
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "FIRFilter";
    }   
}

