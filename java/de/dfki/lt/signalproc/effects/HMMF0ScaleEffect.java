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

package de.dfki.lt.signalproc.effects;

import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 */
public class HMMF0ScaleEffect extends BaseAudioEffect {
    public float f0Scale;
    public static float NO_MODIFICATION = 1.0f;
    public static float DEFAULT_F0_SCALE = 2.0f;
    public static float MAX_F0_SCALE= 3.0f;
    public static float MIN_F0_SCALE = 0.0f;
    
    public HMMF0ScaleEffect()
    {
        super(16000);
        
        setHMMEffect(true);
        
        setExampleParameters("f0Scale" + chParamEquals + Float.toString(DEFAULT_F0_SCALE) + chParamSeparator);
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        f0Scale = expectFloatParameter("f0Scale");
        
        if (f0Scale == NULL_FLOAT_PARAM)
            f0Scale = DEFAULT_F0_SCALE;
        
        f0Scale = MathUtils.CheckLimits(f0Scale, MIN_F0_SCALE, MAX_F0_SCALE);
    }
    
    //Actual processing is done wthin the HMM synthesizer so do nothing here
    public DoubleDataSource process(DoubleDataSource input)
    {
        return input;
    }

    public String getHelpText() {
        
        String strHelp = "F0 scaling effect for HMM voices:" + strLineBreak +
                         "All voiced f0 values are multiplied by <f0Scale> for HMM voices." + strLineBreak +
                         "This operation effectively scales the range of f0 values." + strLineBreak +
                         "Note that mean f0 is preserved during the operation." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <f0Scale>" +
                         "   Definition : Scale ratio for modifying the dynamic range of the f0 contour" + strLineBreak +
                         "                If f0Scale>1.0, the range is expanded (i.e. voice with more variable pitch)" + strLineBreak +
                         "                If f0Scale<1.0, the range is compressed (i.e. more monotonic voice)" + strLineBreak +
                         "                If f0Scale=1.0 results in no changes in range" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_F0_SCALE) + "," + String.valueOf(MAX_F0_SCALE) + "]" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "F0Scale";
    }
}
