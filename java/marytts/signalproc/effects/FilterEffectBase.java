/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.signalproc.effects;

import marytts.signalproc.filter.BandPassFilter;
import marytts.signalproc.filter.BandRejectFilter;
import marytts.signalproc.filter.HighPassFilter;
import marytts.signalproc.filter.LowPassFilter;
import marytts.signalproc.process.FrameOverlapAddSource;
import marytts.signalproc.process.InlineDataProcessor;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class FilterEffectBase extends BaseAudioEffect {
    
    double cutOffFreqInHz1;
    double cutOffFreqInHz2;
    int filterType;
    int frameLength;
    double normalizedCutOffFreq1;
    double normalizedCutOffFreq2;
    InlineDataProcessor filter;
    
    public static int NULL_FILTER = 0;
    public static int LOWPASS_FILTER = 1;
    public static int HIGHPASS_FILTER = 2;
    public static int BANDPASS_FILTER = 3;
    public static int BANDREJECT_FILTER = 4;
    
    public static int DEFAULT_FILTER = BANDPASS_FILTER;
    public static double DEFAULT_CUTOFF1 = 500.0;
    public static double DEFAULT_CUTOFF2 = 2000.0;
   
    //Unlike most of the other effect parameters the following are sampling rate dependent
    double MIN_CUTOFF1;
    double MAX_CUTOFF1;
    double MAX_CUTOFF2;
    double MIN_CUTOFF2;
    //
    
    public FilterEffectBase()
    {
        this(4000.0, 16000);
    }
    
    public FilterEffectBase(double cutOffHz, int samplingRate, int type)
    {
        super(samplingRate);
        
        setExampleParameters("type" + chParamEquals + String.valueOf(BANDPASS_FILTER) + chParamSeparator + " " + 
                "fc1" + chParamEquals + String.valueOf(DEFAULT_CUTOFF1) + chParamSeparator + " " + 
                "fc2" + chParamEquals + String.valueOf(DEFAULT_CUTOFF2));
        
        strHelpText = getHelpText();
        
        
        cutOffFreqInHz1 = cutOffHz;
        cutOffFreqInHz2 = -1.0;
        filterType = type;
        
        MIN_CUTOFF1 = 20.0;
        MAX_CUTOFF1 = 0.5*samplingRate-20.0;
        MIN_CUTOFF2 = 20.0;
        MAX_CUTOFF2 = 0.5*samplingRate-20.0;
    }
    
    public FilterEffectBase(double cutOffHz, int samplingRate)
    {
        this(cutOffHz, samplingRate, LOWPASS_FILTER);
    }
    
    public FilterEffectBase(double cutOffHz1, double cutOffHz2, int samplingRate, int type)
    {
        super(samplingRate);
        
        setExampleParameters("type" + chParamEquals + String.valueOf(BANDPASS_FILTER) + chParamSeparator + " " + 
                "fc1" + chParamEquals + String.valueOf(DEFAULT_CUTOFF1) + chParamSeparator + " " + 
                "fc2" + chParamEquals  + String.valueOf(DEFAULT_CUTOFF2));
        
        strHelpText = getHelpText();

        cutOffFreqInHz1 = cutOffHz1;
        cutOffFreqInHz2 = cutOffHz2;
        filterType = type;
        
        MIN_CUTOFF1 = 20.0;
        MAX_CUTOFF1 = 0.5*samplingRate-20.0;
        MIN_CUTOFF2 = 20.0;
        MAX_CUTOFF2 = 0.5*samplingRate-20.0;
    }
    
    public FilterEffectBase(double cutOffHz1, double cutOffHz2, int samplingRate)
    {
        this(cutOffHz1, cutOffHz2, samplingRate, BANDPASS_FILTER);
    }
    
    //A filter that does nothing
    public FilterEffectBase(int samplingRate)
    {
        super(samplingRate);
        
        setExampleParameters("type" + chParamEquals + String.valueOf(BANDPASS_FILTER) + chParamSeparator + " " + 
                "fc1" + chParamEquals + String.valueOf(DEFAULT_CUTOFF1) + chParamSeparator + " " + 
                "fc2" + chParamEquals + String.valueOf(DEFAULT_CUTOFF2));
        
        strHelpText = getHelpText();
        
        cutOffFreqInHz1 = -1.0;
        cutOffFreqInHz2 = -1.0;
        filterType = NULL_FILTER;
        
        MIN_CUTOFF1 = 20.0;
        MAX_CUTOFF1 = 0.5*samplingRate-20.0;
        MIN_CUTOFF2 = 20.0;
        MAX_CUTOFF2 = 0.5*samplingRate-20.0;
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
            filter = new LowPassFilter(normalizedCutOffFreq1);
        else if (filterType == HIGHPASS_FILTER && normalizedCutOffFreq1>0.0)
            filter = new HighPassFilter(normalizedCutOffFreq1);
        else if (filterType == BANDPASS_FILTER && normalizedCutOffFreq1>0.0 && normalizedCutOffFreq2>0.0)
        {
            if (normalizedCutOffFreq1>normalizedCutOffFreq2)
            {
                double tmp = normalizedCutOffFreq1;
                normalizedCutOffFreq1 = normalizedCutOffFreq2;
                normalizedCutOffFreq2 = tmp;
            }
            
            filter = new BandPassFilter(normalizedCutOffFreq1, normalizedCutOffFreq2);
        }
        else if (filterType == BANDREJECT_FILTER  && normalizedCutOffFreq1>0.0 && normalizedCutOffFreq2>0.0)
        {
            if (normalizedCutOffFreq1>normalizedCutOffFreq2)
            {
                double tmp = normalizedCutOffFreq1;
                normalizedCutOffFreq1 = normalizedCutOffFreq2;
                normalizedCutOffFreq2 = tmp;
            }
            
            filter = new BandRejectFilter(normalizedCutOffFreq1, normalizedCutOffFreq2);
        }
    }
    
    public DoubleDataSource process(DoubleDataSource input)
    {
        if (filter != null)
        {
            FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANNING, true, frameLength, fs, filter);
            
            return new BufferedDoubleDataSource(foas);
        }
        else
        {
            return input;
        }
    }

    public String getHelpText() {
        String strRange1 = "";
        for (int i=LOWPASS_FILTER; i<BANDREJECT_FILTER; i++)
            strRange1 += String.valueOf(i) + ",";
        
        strRange1 += String.valueOf(BANDREJECT_FILTER);
        
        String strRange2 = "[0.0, fs/2.0] where fs is the sampling rate in Hz";
        
        String strHelp = "FIR filtering:" + strLineBreak +
                         "Filters the input signal by an FIR filter." + strLineBreak +
                         "Parameters:" + strLineBreak +
                         "   <type>" + strLineBreak +
                         "   Definition : Type of filter (1:Lowpass, 2:Highpass, 3:Bandpass, 4:Bandreject)" + strLineBreak +
                         "   Range      : {" + strRange1 + "}" + strLineBreak +
                         "   <fc>" +
                         "   Definition : Cutoff frequency in Hz for lowpass and highpass filters" + strLineBreak +
                         "   Range      : " + strRange2 + strLineBreak +
                         "   <fc1>" +
                         "   Definition : Lower frequency cutoff in Hz for bandpass and bandreject filters" +  strLineBreak +
                         "   Range      : " + strRange2 + strLineBreak +
                         "   <fc2>" +
                         "   Definition : Higher frequency cutoff in Hz for bandpass and bandreject filters" + strLineBreak +
                         "   Range      : " + strRange2 + strLineBreak +
                         "Example: (A band-pass filter)" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "FIRFilter";
    }   
}

