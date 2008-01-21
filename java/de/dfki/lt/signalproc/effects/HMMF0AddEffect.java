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

import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

/**
 * @author oytun.turk
 *
 */
public class HMMF0AddEffect extends BaseAudioEffect {
    public float f0Add;
    public static float NO_MODIFICATION = 0.0f;
    public static float DEFAULT_F0_ADD = 50.0f;
    public static float MAX_F0_ADD= 300.0f;
    public static float MIN_F0_ADD = -300.0f;
    
    public HMMF0AddEffect()
    {
        super(16000);
        
        setHMMEffect(true);
        
        setExampleParameters("f0Add" + chParamEquals + Float.toString(DEFAULT_F0_ADD) + chParamSeparator);
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        f0Add = expectFloatParameter("f0Add");
        
        if (f0Add == NULL_FLOAT_PARAM)
            f0Add = DEFAULT_F0_ADD;
        
        f0Add = MathUtils.CheckLimits(f0Add, MIN_F0_ADD, MAX_F0_ADD);
    }
    
    //Actual processing is done within the HMM synthesizer so do nothing here
    public DoubleDataSource process(DoubleDataSource input)
    {
        return input;
    }

    public String getHelpText() {
        
        String strHelp = "F0 mean shifting effect for HMM voices:" + strLineBreak +
                         "Shifts the mean F0 value by <f0Add> Hz for HMM voices." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <f0Add>" +
                         "   Definition : F0 shift of mean value in Hz for synthesized speech output" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_F0_ADD) + "," + String.valueOf(MAX_F0_ADD) + "]" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "F0Add";
    }
}
