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

package marytts.signalproc.sinusoidal;

/**
 * @author oytun.turk
 *
 */
public class SinusoidalUtils {

    //Collect each track´s sinusoids in speech frame sinusoids
    //This way, we will have a collection of sinusoids representing each speech frame
    //Then, overlap-add synthesis can be performed to avoid concatenation artifacts and smoothness problems
    //Quatieri mentions that even single sinusoids can be used using this approach, i.e.
    // each track starts and ends within one frame
    //However, the skip rate should be dense enough, i.e. at most 0.01 s.(at least 100 Hz)
    public static SinusoidalSpeechFrame[] tracks2frameSins(SinusoidalTracks[] sts)
    {
        SinusoidalSpeechFrame[] frameSins = null;
        
        
        
        return frameSins;
    }
}
