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

package de.dfki.lt.signalproc.process;

import de.dfki.lt.signalproc.util.InterpolationUtils;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class VoiceModificationParameters {

    public int fs; //Sampling rate in Hz
    public int lpOrder; //LP order
    
    protected double [] pscales;
    protected double [] tscales;
    protected double [] escales;
    protected double [] vscales;
    
    /**
     * 
     */
    public VoiceModificationParameters() {        
        this(16000, 18, null, null, null, null);
    }

    public VoiceModificationParameters(int samplingRate, int LPOrder, double [] pscalesIn, double [] tscalesIn, double [] escalesIn, double [] vscalesIn)
    {
        initialise(samplingRate, LPOrder, pscalesIn, tscalesIn, escalesIn, vscalesIn);
    }
    
    public VoiceModificationParameters(int samplingRate, int LPOrder, double pscaleIn, double tscaleIn, double escaleIn, double vscaleIn)
    {
        double [] pscalesIn = new double[1];
        double [] tscalesIn = new double[1];
        double [] escalesIn = new double[1];
        double [] vscalesIn = new double[1];
        pscalesIn[0] = pscaleIn;
        tscalesIn[0] = tscaleIn;
        escalesIn[0] = escaleIn;
        vscalesIn[0] = vscaleIn;

        initialise(samplingRate, LPOrder, pscalesIn, tscalesIn, escalesIn, vscalesIn);
    }
    
    private void initialise(int samplingRate, int LPOrder, double [] pscalesIn, double [] tscalesIn, double [] escalesIn, double [] vscalesIn)
    {
        if (pscalesIn!=null)
        {
            pscales = new double[pscalesIn.length];
            System.arraycopy(pscalesIn, 0, pscales, 0, pscalesIn.length);
        }
        
        if (tscalesIn!=null)
        {
            tscales = new double[tscalesIn.length];
            System.arraycopy(tscalesIn, 0, tscales, 0, tscalesIn.length);
        }
        
        if (escalesIn!=null)
        {
            escales = new double[escalesIn.length];
            System.arraycopy(escalesIn, 0, escales, 0, escalesIn.length);
        }
        
        if (vscalesIn!=null)
        {
            vscales = new double[vscalesIn.length];
            System.arraycopy(vscalesIn, 0, vscales, 0, vscalesIn.length);
        }

        fs = samplingRate;
        lpOrder = SignalProcUtils.getLPOrder(fs);
    }
}
