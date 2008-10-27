/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.unitselection.select;

import java.io.IOException;

import marytts.unitselection.data.Unit;


/**
 * A join cost function for evaluating the goodness-of-fit of 
 * a given pair of left and right unit.
 * @author Marc Schr&ouml;der
 *
 */
public interface JoinCostFunction
{
    /**
     * Compute the goodness-of-fit of joining two units, given the corresponding targets
     * @param t1 the left target
     * @param u1 the proposed left unit
     * @param t2 the right target
     * @param u3 the proposed right unit
     * @return a non-negative number; smaller values mean better fit, i.e. smaller cost.
     */
    public double cost(Target t1, Unit u1, Target t2, Unit u2);

    /**
     * Initialise this join cost function by reading the appropriate settings
     * from the MaryProperties using the given configPrefix.
     * @param configPrefix the prefix for the (voice-specific) config entries
     * to use when looking up files to load.
     */
    public void init(String configPrefix) throws IOException;
    
    /**
     * Load weights and values from the given file
     * @param joinFileName the file from which to read default weights and join cost features
     * @param weightsFileName an optional file from which to read weights, taking precedence over
     * @param precompiledCostFileName an optional file containing precompiled join costs
     * @param wSignal Relative weight of the signal-based join costs relative to the
     *                phonetic join costs computed from the target 
     */
    @Deprecated
    public void load(String joinFileName, String weightsFileName, String precompiledCostFileName,float wSignal) throws IOException;
    
}
