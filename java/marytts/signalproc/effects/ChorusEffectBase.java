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

import marytts.signalproc.process.Chorus;
import marytts.signalproc.process.FrameOverlapAddSource;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uumlrk
 */
public class ChorusEffectBase extends BaseAudioEffect {

    int [] delaysInMiliseconds;
    double [] amps;
    int frameLength;
    int maxDelayInMiliseconds;
    int maxDelayInSamples;
    int numTaps;
    public static int MAX_TAPS = 20;
    public static int MIN_DELAY = 0;
    public static int MAX_DELAY = 5000;
    public static double MIN_AMP = -5.0;
    public static double MAX_AMP = 5.0;
    
    public ChorusEffectBase()
    {
        this(16000);
    }
    
    public ChorusEffectBase(int samplingRate)
    {
        super(samplingRate);
        
        setExampleParameters("delay1" + chParamEquals + "466" + chParamSeparator + 
                             " amp1" + chParamEquals + "0.54" + chParamSeparator +
                             " delay2" + chParamEquals + "600" + chParamSeparator +
                             " amp2" + chParamEquals + "-0.10" + chParamSeparator +
                             " delay3" + chParamEquals + "250" + chParamSeparator +
                             " amp3" + chParamEquals + "0.30");
        
        strHelpText = getHelpText();  
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
        
        FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANNING, true, 1024, fs, chorus);
        
        return new BufferedDoubleDataSource(foas);
    }

    public String getHelpText() {        
        String strHelp = "Multi-Tap Chorus Effect:" + strLineBreak +
                         "Adds chorus effect by summing up the original signal with delayed and amplitude scaled versions." + strLineBreak +
                         "The parameters should consist of delay and amplitude pairs for each tap." + strLineBreak +
                         "A variable number of taps (max 20) can be specified by simply defining more delay-amplitude pairs." + strLineBreak +
                         "Each tap outputs a delayed and gain-scaled version of the original signal." + strLineBreak +
                         "All tap outputs are summed up with the oiginal signal with appropriate gain normalization." + strLineBreak +
                         "Parameters:"  + strLineBreak +
                         "   <delay1>"  + strLineBreak +
                         "   Definition : The amount of delay in miliseconds for tap #1"  + strLineBreak +
                         "   Range      : [0,5000]" + strLineBreak +
                         "   <amp1>"  + strLineBreak +
                         "   Definition : Relative amplitude of the channel gain as compared to original signal gain for tap #1" + strLineBreak +
                         "   Range      : [-5.0,5.0]" + strLineBreak +
                         "   <delay2>" + strLineBreak +
                         "   Definition : The amount of delay in miliseconds in delayed channel #2" + strLineBreak +
                         "   Range      : [0,5000]" + strLineBreak +
                         "   <amp2>" + strLineBreak +
                         "   Definition : Relative amplitude of the channel gain as compared to original signal gain for delayed channel #2" + strLineBreak +
                         "   Range      : [-5.0,5.0]" + strLineBreak +
                         "   ..." + strLineBreak +
                         "   <delayN>" + strLineBreak +
                         "   Definition : The amount of delay in miliseconds in delayed channel #N" + strLineBreak +
                         "   Range      : [0,5000]" + strLineBreak +
                         "   <ampN>" + strLineBreak +
                         "   Definition : Relative amplitude of the channel gain as compared to original signal gain for delayed channel #N" + strLineBreak +
                         "   Range      : [-5.0,5.0]" + strLineBreak +
                         "   Note: Maximum possible number of taps is N=20. Parameters for more taps will simply be neglected." + strLineBreak +
                         "Example: (A three-tap chorus effect)" +  strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "Chorus";
    }
    
    public DoubleDataSource apply(BufferedDoubleDataSource input)
    {
        Chorus c = new Chorus(delaysInMiliseconds, amps, fs);

        FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANNING, true, frameLength, fs, c);
        
        return (DoubleDataSource)(new BufferedDoubleDataSource(foas));
    }
}
