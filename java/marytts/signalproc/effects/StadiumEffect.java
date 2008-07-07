/**
 * Copyright 2004-2006 DFKI GmbH.
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
import marytts.signalproc.process.Robotiser;
import marytts.signalproc.window.Window;
import marytts.util.audio.BufferedDoubleDataSource;
import marytts.util.audio.DoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * @author oytun.turk
 *
 */
public class StadiumEffect extends ChorusEffectBase {
    
    float amount;
    public static float DEFAULT_AMOUNT = 100.0f;
    public static float MAX_AMOUNT = 200.0f;
    public static float MIN_AMOUNT = 0.0f;
    
    public StadiumEffect()
    {
        this(16000);
    }
    
    public StadiumEffect(int samplingRate)
    {
        super(samplingRate);
        
        delaysInMiliseconds = new int[2];
        delaysInMiliseconds[0] = 466;
        delaysInMiliseconds[1] = 600;
        
        amps = new double[2];
        amps[0] = 0.54;
        amps[1] = -0.10;
        
        setExampleParameters("amount" + chParamEquals + "100.0");
        
        strHelpText = getHelpText();

        initialise();
    }
    
    public void parseParameters(String param)
    {      
        super.parseChildParameters(param);
        
        if (param!="")
        {
            amount = expectFloatParameter("amount");

            if (amount == NULL_FLOAT_PARAM)
                amount = DEFAULT_AMOUNT;

            amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);

            for (int i=0; i<delaysInMiliseconds.length; i++)
                delaysInMiliseconds[i] = (int)(delaysInMiliseconds[i]*amount/100.0f);
        }
        
        initialise();
    }

    public String getHelpText() {

        String strHelp = "Stadium Effect:" + strLineBreak +
                         "Adds stadium effect by applying a specially designed multi-tap chorus." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <amount>" +
                         "   Definition : The amount of stadium effect at the output" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "Stadium";
    }
}
