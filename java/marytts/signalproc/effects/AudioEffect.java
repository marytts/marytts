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

package marytts.signalproc.effects;

import marytts.util.data.DoubleDataSource;

/**
 * 
 * @author Oytun T&uumlrk
 */
public interface AudioEffect {    
    public String getName(); //Returns the unique name of the effect
    public void setName(String strName); //Sets the unique name of the effect 
    
    public String getExampleParameters(); //Returns typical parameters for the effect
    public void setExampleParameters(String strExampleParams); //Sets typical parameters for the effect
    
    public String getHelpText(); //Returns the help text for the effect
    public String getParamsAsString(); //Returns current parameters with parameter names and values 
                                       //  separated by a parameter separator character and surrounded by 
                                       //  parameter field start and end characters
    public String getParamsAsString(boolean bWithParantheses);
    
    public String getFullEffectAsString(); //Returns effect name, current parameters and their values
   
    public String getFullEffectWithExampleParametersAsString(); //Returns name with example parameters and values
    
    public float expectFloatParameter(String strParamName); //Return a float valued parameter from a string in the form param1=val1
    public double expectDoubleParameter(String strParamName); //Return a double valued parameter from a string in the form param1=val1
    public int expectIntParameter(String strParamName); //Return an integer valued parameter from a string in the form param1=val1
    
    public DoubleDataSource apply(DoubleDataSource input, String param);
    public DoubleDataSource process(DoubleDataSource input);
    
    public void setParams(String params);
    public String preprocessParams(String params);
    public void parseParameters(String param);
    public void checkParameters();
    
}
