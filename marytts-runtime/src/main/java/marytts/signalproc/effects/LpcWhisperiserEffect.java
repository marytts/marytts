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

import marytts.signalproc.process.FrameOverlapAddSource;
import marytts.signalproc.process.LPCWhisperiser;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uuml;rk
 */
public class LpcWhisperiserEffect extends BaseAudioEffect {

	int frameLength;
	int predictionOrder;
	float amount;
	public static float DEFAULT_AMOUNT = 100.0f;
	public static float MAX_AMOUNT = 100.0f;
	public static float MIN_AMOUNT = 0.0f;

	public LpcWhisperiserEffect() {
		this(16000);
	}

	public LpcWhisperiserEffect(int samplingRate) {
		super(samplingRate);

		setExampleParameters("amount" + chParamEquals + "100.0" + chParamSeparator);

		strHelpText = getHelpText();
	}

	public void parseParameters(String param) {
		super.parseParameters(param);

		amount = expectFloatParameter("amount");

		if (amount == NULL_FLOAT_PARAM)
			amount = DEFAULT_AMOUNT;

		amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);

		frameLength = Integer.getInteger("signalproc.lpcanalysissynthesis.framelength", 512).intValue();
		predictionOrder = Integer.getInteger("signalproc.lpcwhisperiser.predictionorder", 20).intValue();
	}

	public DoubleDataSource process(DoubleDataSource input) {
		LPCWhisperiser whisperiser = new LPCWhisperiser(predictionOrder, amount / 100.0f);

		FrameOverlapAddSource foas = new FrameOverlapAddSource(input, Window.HANNING, true, frameLength, fs, whisperiser);

		return new BufferedDoubleDataSource(foas);
	}

	public String getHelpText() {

		String strHelp = "Whisper Effect:" + strLineBreak
				+ "Creates a whispered voice by replacing the LPC residual with white noise." + strLineBreak + "Parameter:"
				+ strLineBreak + "   <amount>" + "   Definition : The amount of whisperised voice at the output" + strLineBreak
				+ "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak
				+ "Example:" + strLineBreak + getExampleParameters();

		return strHelp;
	}

	public String getName() {
		return "Whisper";
	}
}
