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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import marytts.signalproc.process.FrameOverlapAddSource;
import marytts.signalproc.process.LPCWhisperiser;
import marytts.signalproc.process.VocalTractScalingProcessor;
import marytts.signalproc.window.Window;
import marytts.util.audio.AudioDoubleDataSource;
import marytts.util.audio.BufferedDoubleDataSource;
import marytts.util.audio.DDSAudioInputStream;
import marytts.util.audio.DoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
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
