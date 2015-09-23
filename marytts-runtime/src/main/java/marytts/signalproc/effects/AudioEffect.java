/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.signalproc.effects;

import marytts.util.data.DoubleDataSource;

/**
 * 
 * @author Oytun T&uuml;rk
 */
public interface AudioEffect {
	public String getName(); // Returns the unique name of the effect

	public void setName(String strName); // Sets the unique name of the effect

	public String getExampleParameters(); // Returns typical parameters for the effect

	public void setExampleParameters(String strExampleParams); // Sets typical parameters for the effect

	public String getHelpText(); // Returns the help text for the effect

	public String getParamsAsString(); // Returns current parameters with parameter names and values
										// separated by a parameter separator character and surrounded by
										// parameter field start and end characters

	public String getParamsAsString(boolean bWithParantheses);

	public String getFullEffectAsString(); // Returns effect name, current parameters and their values

	public String getFullEffectWithExampleParametersAsString(); // Returns name with example parameters and values

	public float expectFloatParameter(String strParamName); // Return a float valued parameter from a string in the form
															// param1=val1

	public double expectDoubleParameter(String strParamName); // Return a double valued parameter from a string in the form
																// param1=val1

	public int expectIntParameter(String strParamName); // Return an integer valued parameter from a string in the form
														// param1=val1

	public DoubleDataSource apply(DoubleDataSource input, String param);

	public DoubleDataSource process(DoubleDataSource input);

	public void setParams(String params);

	public String preprocessParams(String params);

	public void parseParameters(String param);

	public void checkParameters();

	public boolean isHMMEffect();
}
