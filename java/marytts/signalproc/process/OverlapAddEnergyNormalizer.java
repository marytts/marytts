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

package marytts.signalproc.process;

import java.util.Arrays;

import marytts.signalproc.window.Window;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class OverlapAddEnergyNormalizer {
    public static double[] normalize(double[] x, 
                                     int samplingRate, 
                                     double windowSizeInSeconds, 
                                     double skipSizeInSeconds, 
                                     int windowType,
                                     double targetEnergy)
    {
        double[] y = new double[x.length];
        double[] w = new double[x.length];
        Arrays.fill(y, 0.0);
        Arrays.fill(w, 0.0);
        
        int ws = (int)Math.floor(windowSizeInSeconds*samplingRate+0.5);
        if (ws%2!=0)
            ws++;
        int half_ws = (int)(ws/2.0);
        int ss = (int)Math.floor(skipSizeInSeconds*samplingRate+0.5);

        Window win = Window.get(windowType, ws);
        win.normalize(1.0f); //Normalize to sum up to unity
        double[] wgt = new double[ws];
        double[] frm = new double[ws];
        
        int start = 0;
        boolean bLastFrame = false;
        double frmEn, gain;
        int i;
        while (true)
        {   
            if (start+ws-1>=x.length-1)
                bLastFrame = true;
            
            wgt = win.getCoeffs();
            if (start==0) //First frame
                Arrays.fill(wgt, 0, half_ws-1, 1.0);
            else if (bLastFrame)
                Arrays.fill(wgt, half_ws, ws-1, 1.0);

            Arrays.fill(frm, 0.0);
            System.arraycopy(x, start, frm, 0, Math.min(ws, x.length-start));
            
            frm = MathUtils.multiply(frm, wgt);
            
            frmEn = SignalProcUtils.energy(frm);
            gain = Math.sqrt(targetEnergy/frmEn);
            System.out.println(String.valueOf(gain));
            
            frm = MathUtils.multiply(frm, gain);
            frm = MathUtils.multiply(frm, wgt);
            
            for (i=0; i<ws; i++)
            {
                if (i+start>=y.length)
                {
                    bLastFrame = true;
                    break;
                }
                
                y[i+start] += frm[i];
                w[i+start] += wgt[i]*wgt[i];
            }
            
            if (bLastFrame)
                break;
            
            start += ss;
        }
        
        for (i=0; i<y.length; i++)
        {
            if (w[i]>0.0)
                y[i] /= w[i];
        }
        
        
        return y;
    }
}
