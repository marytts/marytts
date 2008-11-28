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

import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uumlrk
 */
public class HMMDurationScaleEffect extends BaseAudioEffect {
    public float durScale;
    public static float NO_MODIFICATION = 1.0f;
    public static float DEFAULT_DUR_SCALE = 1.5f;
    public static float MAX_DUR_SCALE = 3.0f;
    public static float MIN_DUR_SCALE = 0.1f;
    
    public HMMDurationScaleEffect()
    {
        super(16000);
        
        setHMMEffect(true);
        
        setExampleParameters("durScale" + chParamEquals + Float.toString(DEFAULT_DUR_SCALE) + chParamSeparator);
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        durScale = expectFloatParameter("durScale");
        
        if (durScale == NULL_FLOAT_PARAM)
            durScale = DEFAULT_DUR_SCALE;
        
        durScale = MathUtils.CheckLimits(durScale, MIN_DUR_SCALE, MAX_DUR_SCALE);
    }
    
    //Actual processing is done within the HMM synthesizer so do nothing here
    public DoubleDataSource process(DoubleDataSource input)
    {
        return input;
    }

    public String getHelpText() {
        
        String strHelp = "Duration scaling for HMM voices:" + strLineBreak +
                         "Scales the HMM output speech duration by <durScale>." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <durScale>" +
                         "   Definition : Duration scaling factor for synthesized speech output" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_DUR_SCALE) + "," + String.valueOf(MAX_DUR_SCALE) + "]" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "Rate";
    }
}
