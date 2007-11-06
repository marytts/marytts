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

package de.dfki.lt.mary.sinusoidal;

/**
 * @author oytun.turk
 *
 */
public class TrackModifier {
    
    public static float DEFAULT_MODIFICATION_SKIP_SIZE = 0.005f; //Default skip size (in seconds) to be used in sinusoidal analysis, modification, and synthesis
                                                                 //Note that lower skip sizes might be required in order to obtain better performance for 
                                                                 // large duration modification factors or to realize more accurate final target lengths
                                                                 // because the time scaling resolution will only be as low as the skip size
    
    public static SinusoidalTracks modifyTimeScale(SinusoidalTracks trIn, float tScale)
    {
        return modifyTimeAndPitchScale(trIn, tScale, 1.0f);   
    }
    
    public static SinusoidalTracks modifyPitchScale(SinusoidalTracks trIn, float pScale)
    {
        return modifyTimeAndPitchScale(trIn, 1.0f, pScale);   
    }
    
    public static SinusoidalTracks modifyTimeAndPitchScale(SinusoidalTracks trIn, float tScale, float pScale)
    {
        float [] tScales = null;
        float [] pScales = null;
        
        if (tScale!=1.0f)
        {
            tScales = new float[1];
            tScales[0] = tScale;
        }
        
        if (pScale!=1.0f)
        {
            pScales = new float[1];
            pScales[0] = pScale;
        }
        
        return modify(trIn, null, tScales, null, pScales);   
    }
    
    public static SinusoidalTracks modifyTimeScale(SinusoidalTracks trIn, float [] tScales)
    {
        return modifyTimeScale(trIn, null, tScales);
    }
    
    public static SinusoidalTracks modifyTimeScale(SinusoidalTracks trIn, float [] tscaleTimes, float [] tScales)
    {
        return modify(trIn, tscaleTimes, tScales, null, null);
    }
    
    public static SinusoidalTracks modifyPitchScale(SinusoidalTracks trIn, float [] pScales)
    {
        return modifyPitchScale(trIn, null, pScales);
    }
    
    public static SinusoidalTracks modifyPitchScale(SinusoidalTracks trIn, float [] pscaleTimes, float [] pScales)
    {
        return modify(trIn, pscaleTimes, pScales, null, null);
    }
    
    //All-purpose function for time and pitch scale modification using shape invariant sinusoidal modeling.
    //    For simpler usage, please check the functions above
    //
    // trIn: Input sinusoidal tracks (refer to PitchSynchronousSinusoidalAnalyzer based classes for more information)
    // tscaleTimes: Specific time instants, in seconds, for which a desired time scale modification factor is specified in tScales
    // tScales: An array of time scale modification factors to be applied at corresponding time instants in tscaleTimes
    // pscaleTimes: Specific time instants, in seconds, for which a desired pitch scale modification factor is specified in pScales
    // pScales: An array of pitch scale modification factors to be applied at corresponding time instants in pscaleTimes
    //
    // If tscaleTimes (or pscaleTimes) is null, the greatest time instant is found in trIn, and a time vector is generated 
    //    using DEFAULT_MODIFICATION_SKIP_SIZE. Then, tScales(or pScales) is linearly interpolated to match the length of the generated time vector.
    //    Modification is performed as usual then.
    // If the length of tScales (or pScales) does not match the corresponding time instants vector (i.e. tscaleTimes for tScales and 
    //    or pScales for pscaleTimes
    public static SinusoidalTracks modify(SinusoidalTracks trIn, float [] tscaleTimes, float [] tScales, float [] pscaleTimes, float [] pScales)
    {
        
        return null;
    }
}
