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

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uuml;rk
 */
public class VolumeEffect extends BaseAudioEffect {
	float amount;
	public static float DEFAULT_AMOUNT = 2.0f;
	public static float MAX_AMOUNT = 10.0f;
	public static float MIN_AMOUNT = 0.0f;

	public VolumeEffect() {
		super(16000);

		setExampleParameters("amount=" + String.valueOf(DEFAULT_AMOUNT) + ",");

		strHelpText = getHelpText();
	}

	public void parseParameters(String param) {
		super.parseParameters(param);

		amount = expectFloatParameter("amount");

		if (amount == NULL_FLOAT_PARAM)
			amount = DEFAULT_AMOUNT;

		amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);
	}

	public DoubleDataSource process(DoubleDataSource input) {
		double[] x = input.getAllData();
		if (x != null) {
			for (int i = 0; i < x.length; i++)
				x[i] *= amount;

			input = new BufferedDoubleDataSource(x);
		}

		return input;
	}

	public String getHelpText() {

		String strHelp = "Volume Effect:" + strLineBreak + "Scales the output volume by a fixed amount." + strLineBreak
				+ "Parameter:" + strLineBreak + "   <amount>"
				+ "   Definition : Amount of scaling (the output is simply multiplied by amount)" + strLineBreak
				+ "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak
				+ "Example:" + strLineBreak + getExampleParameters();

		return strHelp;
	}

	public String getName() {
		return "Volume";
	}

}
