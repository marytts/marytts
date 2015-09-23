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

import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uuml;rk
 */
public class StadiumEffect extends ChorusEffectBase {

	float amount;
	public static float DEFAULT_AMOUNT = 100.0f;
	public static float MAX_AMOUNT = 200.0f;
	public static float MIN_AMOUNT = 0.0f;

	public StadiumEffect() {
		this(16000);
	}

	public StadiumEffect(int samplingRate) {
		super(samplingRate);

		delaysInMiliseconds = new int[2];
		delaysInMiliseconds[0] = 466;
		delaysInMiliseconds[1] = 600;

		amps = new double[2];
		amps[0] = 0.54;
		amps[1] = -0.10;

		setExampleParameters("amount" + chParamEquals + "100.0");

		strHelpText = getHelpText();

		initialise();
	}

	public void parseParameters(String param) {
		super.parseChildParameters(param);

		if (param != "") {
			amount = expectFloatParameter("amount");

			if (amount == NULL_FLOAT_PARAM)
				amount = DEFAULT_AMOUNT;

			amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);

			for (int i = 0; i < delaysInMiliseconds.length; i++)
				delaysInMiliseconds[i] = (int) (delaysInMiliseconds[i] * amount / 100.0f);
		}

		initialise();
	}

	public String getHelpText() {

		String strHelp = "Stadium Effect:" + strLineBreak
				+ "Adds stadium effect by applying a specially designed multi-tap chorus." + strLineBreak + "Parameter:"
				+ strLineBreak + "   <amount>" + "   Definition : The amount of stadium effect at the output" + strLineBreak
				+ "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak
				+ "Example:" + strLineBreak + getExampleParameters();

		return strHelp;
	}

	public String getName() {
		return "Stadium";
	}
}
