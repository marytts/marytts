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

import marytts.signalproc.process.FrameOverlapAddSource;
import marytts.signalproc.process.LPCWhisperiser;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;


/**
 * @author oytun.turk
 *
 */
public class LpcWhisperiserEffect extends BaseAudioEffect {

    int frameLength;
    int predictionOrder;
    float amount;
    public static float DEFAULT_AMOUNT = 100.0f;
    public static float MAX_AMOUNT = 100.0f;
    public static float MIN_AMOUNT = 0.0f;
    
    public LpcWhisperiserEffect()
    {
        this(16000);
    }
    
    public LpcWhisperiserEffect(int samplingRate)
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
        
        FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANNING, true, frameLength, fs, whisperiser);
        
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
