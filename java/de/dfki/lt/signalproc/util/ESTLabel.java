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

package de.dfki.lt.signalproc.util;

/**
 * @author oytun.turk
 *
 * A class to keep all information on a single EST format label
 * 
 */
public class ESTLabel {
    public float time; //Ending time of phonetic label
    public int status; //Status
    public String phn; //Phoneme
    public float ll; //log likelihood
    
    public ESTLabel(float newTime, int newStatus, String newPhn, float newll)
    {
        time = newTime;
        status = newStatus;
        phn = newPhn;
        ll = newll;
    }
    
    public ESTLabel()
    {
        time = -1.0f;
        status = 0;
        phn = "";
        ll = Float.NEGATIVE_INFINITY;
    }
    
    public ESTLabel(ESTLabel lab)
    {
        copyFrom(lab);
    }
    
    public void copyFrom(ESTLabel lab)
    {
        time = lab.time;
        status = lab.status;
        phn = lab.phn;
        ll = lab.ll;
    }
    
    //Display label entries
    public void print()
    {
        System.out.println("Time=" + String.valueOf(time) + " s. " + 
                           "Stat=" + String.valueOf(status) + " " +
                           "Phoneme=" + phn + " " +
                           "Log-likelihood=" + String.valueOf(ll));
    }
}
