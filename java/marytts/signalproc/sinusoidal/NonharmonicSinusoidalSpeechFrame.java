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

import marytts.util.math.ComplexArray;


/**
 * Single speech frame sinusoids with spectrum
 * 
 * @author Oytun T&uumlrk
 */
public class NonharmonicSinusoidalSpeechFrame extends BaseSinusoidalSpeechFrame {
    public Sinusoid [] sinusoids;
    public double[] systemAmps;
    public double [] systemPhases;
    public double[] systemCeps; 
    public ComplexArray frameDfts;
    public float time;
    public float voicing;
    public float maxFreqOfVoicing;
    
    public NonharmonicSinusoidalSpeechFrame(int numSins)
    {
        if (numSins>0)
            sinusoids = new Sinusoid[numSins];
        else
            sinusoids = null;
        
        systemAmps = null;
        systemPhases = null;
        systemCeps = null;
        frameDfts = null;
        time = -1.0f;
        voicing = -1.0f;
        maxFreqOfVoicing = -1.0f;
    }
    
    public NonharmonicSinusoidalSpeechFrame(NonharmonicSinusoidalSpeechFrame existing)
    {
        this(existing.sinusoids.length);
        
        for (int i=0; i<existing.sinusoids.length; i++)
            sinusoids[i] = new Sinusoid(existing.sinusoids[i]);
        
        setSystemAmps(existing.systemAmps);
        setSystemPhases(existing.systemPhases);
        setSystemCeps(existing.systemCeps);
        setFrameDfts(existing.frameDfts);
        time = existing.time;
        voicing = existing.voicing;
        maxFreqOfVoicing = existing.maxFreqOfVoicing;
    }
    
    public void setSystemAmps(double[] newAmps)
    {
        if (newAmps!=null && newAmps.length>0)
        {
            systemAmps = new double[newAmps.length];
            System.arraycopy(newAmps, 0, systemAmps, 0, newAmps.length);
        }
        else
            systemAmps = null;
    }
    
    public void setSystemPhases(double[] newPhases)
    {
        if (newPhases!=null && newPhases.length>0)
        {
            systemPhases = new double[newPhases.length];
            System.arraycopy(newPhases, 0, systemPhases, 0, newPhases.length);
        }
        else
            systemPhases = null;
    }
    
    public void setSystemCeps(double[] newCeps)
    {
        if (newCeps!=null && newCeps.length>0)
        {
            systemCeps = new double[newCeps.length];
            System.arraycopy(newCeps, 0, systemCeps, 0, newCeps.length);
        }
        else
            systemCeps = null;
    }
    
    public void setFrameDfts(ComplexArray newDfts)
    {
        if (newDfts!=null)
            frameDfts = new ComplexArray(newDfts);
        else
            frameDfts = null;
    }
}
