package de.dfki.lt.signalproc.effects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import de.dfki.lt.mary.unitselection.AudioEffect;
import de.dfki.lt.signalproc.filter.BandPassFilter;
import de.dfki.lt.signalproc.filter.BandRejectFilter;
import de.dfki.lt.signalproc.filter.HighPassFilter;
import de.dfki.lt.signalproc.filter.LowPassFilter;
import de.dfki.lt.signalproc.process.Chorus;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.VocalTractScalingProcessor;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.window.Window;

public class FilterEffectsBase implements AudioEffect {
    
    double cutOffFreqInHz1;
    double cutOffFreqInHz2;
    int filterType;
    static int NULL_FILTER = 0;
    static int LOWPASS_FILTER = 1;
    static int HIGHPASS_FILTER = 2;
    static int BANDPASS_FILTER = 3;
    static int BANDREJECT_FILTER = 4;
    
    public FilterEffectsBase(double cutOffHz, int type)
    {
        cutOffFreqInHz1 = cutOffHz;
        cutOffFreqInHz2 = -1.0;
        filterType = type;
    }
    
    public FilterEffectsBase(double cutOffHz)
    {
        this(cutOffHz, LOWPASS_FILTER);
    }
    
    public FilterEffectsBase(double cutOffHz1, double cutOffHz2, int type)
    {
        cutOffFreqInHz1 = cutOffHz1;
        cutOffFreqInHz2 = cutOffHz2;
        filterType = type;
    }
    
    public FilterEffectsBase(double cutOffHz1, double cutOffHz2)
    {
        this(cutOffHz1, cutOffHz2, BANDPASS_FILTER);
    }
    
    //A filter that does nothing
    public FilterEffectsBase()
    {
        cutOffFreqInHz1 = -1.0;
        cutOffFreqInHz2 = -1.0;
        filterType = NULL_FILTER;
    }
    
    public AudioInputStream apply(AudioInputStream inputAudio)
    {
        AudioFormat audioformat = inputAudio.getFormat();
        int fs = (int)audioformat.getSampleRate();
        
        int frameLength = 8*SignalProcUtils.getDFTSize(fs);
        double normalizedCutOffFreq1 = cutOffFreqInHz1/fs;
        double normalizedCutOffFreq2 = cutOffFreqInHz2/fs;
        InlineDataProcessor effect = null;
        
        if (filterType == LOWPASS_FILTER && normalizedCutOffFreq1>0.0)
            effect = new LowPassFilter(normalizedCutOffFreq1, true);
        else if (filterType == HIGHPASS_FILTER && normalizedCutOffFreq1>0.0)
            effect = new HighPassFilter(normalizedCutOffFreq1, true);
        else if (filterType == BANDPASS_FILTER && normalizedCutOffFreq1>0.0 && normalizedCutOffFreq2>0.0)
        {
            if (normalizedCutOffFreq1>normalizedCutOffFreq2)
            {
                double tmp = normalizedCutOffFreq1;
                normalizedCutOffFreq1 = normalizedCutOffFreq2;
                normalizedCutOffFreq2 = tmp;
            }
            
            effect = new BandPassFilter(normalizedCutOffFreq1, normalizedCutOffFreq2, true);
        }
        else if (filterType == BANDREJECT_FILTER  && normalizedCutOffFreq1>0.0 && normalizedCutOffFreq2>0.0)
        {
            if (normalizedCutOffFreq1>normalizedCutOffFreq2)
            {
                double tmp = normalizedCutOffFreq1;
                normalizedCutOffFreq1 = normalizedCutOffFreq2;
                normalizedCutOffFreq2 = tmp;
            }
            
            effect = new BandRejectFilter(normalizedCutOffFreq1, normalizedCutOffFreq2, true);
        }

        if (effect != null)
        {
            AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);   
            FrameOverlapAddSource foas = new FrameOverlapAddSource(signal, Window.HANN, true, frameLength, fs,
                effect);
            
            return new DDSAudioInputStream(new BufferedDoubleDataSource(foas), audioformat);
        }
        else
        {
            System.out.println("No filtering applied: Filter parameters and filter type mismatch!");
            return inputAudio;
        }
    }
}

