/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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

package de.dfki.lt.mary.unitselection.weightingfunctions;

import de.dfki.lt.mary.unitselection.weightingfunctions.WeightFunc;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * This class connects weighting function names with the actual
 * instances of the weighting functions.
 * 
 * @author sacha
 *
 */
public class WeightFunctionManager {
    
    public static Map weightFuncMap;
    
    static{
        weightFuncMap = new HashMap();
        weightFuncMap.put( "linear", new WeightingFunction.linear() );
        weightFuncMap.put( "step", new WeightingFunction.step() );
    }
    
    /**
     * Dummy empty contructor.
     */
    public WeightFunctionManager(){}
    
    
    /**
     * Accessor for the hash map mapping names to interface instances.
     * @return a hash map.
     */
    public static Map getWeightFunc() {
        return weightFuncMap;
    }
    
    /**
     * Returns the weighting function from its name.
     * 
     * @param funcName The name of the weighting function.
     * @return an interface to a weighting function.
     */
    public WeightFunc getWeightFunction( String funcName ){
        String[] strPart = funcName.split( "\\s", 2 );
        WeightFunc wf = ( WeightFunc ) weightFuncMap.get( strPart[0] );
        if ( strPart.length > 1 ) wf.setParam( strPart[1] );
        return( wf );
    }
    
}
