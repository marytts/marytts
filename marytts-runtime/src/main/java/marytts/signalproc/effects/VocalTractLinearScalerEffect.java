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

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.process.FrameOverlapAddSource;
import marytts.signalproc.process.VocalTractScalingProcessor;
import marytts.signalproc.window.Window;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

import org.apache.commons.io.FilenameUtils;

/**
 * @author Oytun T&uuml;rk
 */
public class VocalTractLinearScalerEffect extends BaseAudioEffect {

	float amount;
	public static float MAX_AMOUNT = 4.0f;
	public static float MIN_AMOUNT = 0.25f;
	public static float DEFAULT_AMOUNT = 1.5f;

	public VocalTractLinearScalerEffect() {
		this(16000);
	}

	public VocalTractLinearScalerEffect(int samplingRate) {
		super(samplingRate);

		setExampleParameters("amount" + chParamEquals + Float.toString(DEFAULT_AMOUNT) + chParamSeparator);

		strHelpText = getHelpText();
	}

	public void parseParameters(String param) {
		super.parseParameters(param);

		amount = expectFloatParameter("amount");

		if (amount == NULL_FLOAT_PARAM)
			amount = DEFAULT_AMOUNT;
	}

	public DoubleDataSource process(DoubleDataSource inputAudio) {
		amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);

		double[] vscales = { amount };

		int frameLength = SignalProcUtils.getDFTSize(fs);
		int predictionOrder = SignalProcUtils.getLPOrder(fs);

		VocalTractScalingProcessor p = new VocalTractScalingProcessor(predictionOrder, fs, frameLength, vscales);
		FrameOverlapAddSource foas = new FrameOverlapAddSource(inputAudio, Window.HANNING, true, frameLength, fs, p);

		return new BufferedDoubleDataSource(foas);
	}

	public String getHelpText() {

		String strHelp = "Vocal Tract Linear Scaling Effect:" + strLineBreak
				+ "Creates a shortened or lengthened vocal tract effect by shifting the formants." + strLineBreak + "Parameter:"
				+ strLineBreak + "   <amount>" + "   Definition : The amount of formant shifting" + strLineBreak
				+ "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak
				+ "   For values of <amount> less than 1.0, the formants are shifted to lower frequencies" + strLineBreak
				+ "       resulting in a longer vocal tract (i.e. a deeper voice)." + strLineBreak
				+ "   Values greater than 1.0 shift the formants to higher frequencies." + strLineBreak
				+ "       The result is a shorter vocal tract.\n" + strLineBreak + "Example:" + strLineBreak
				+ getExampleParameters();

		return strHelp;
	}

	public String getName() {
		return "TractScaler";
	}

	/**
	 * Command line interface to the vocal tract linear scaler effect.
	 * 
	 * @param args
	 *            the command line arguments. Exactly two arguments are expected: (1) the factor by which to scale the vocal tract
	 *            (between 0.25 = very long and 4.0 = very short vocal tract); (2) the filename of the wav file to modify. Will
	 *            produce a file basename_factor.wav, where basename is the filename without the extension.
	 * @throws Exception
	 *             if processing fails for some reason.
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: java " + VocalTractLinearScalerEffect.class.getName() + " <factor> <filename>");
			System.exit(1);
		}
		float factor = Float.parseFloat(args[0]);
		String filename = args[1];
		AudioDoubleDataSource input = new AudioDoubleDataSource(AudioSystem.getAudioInputStream(new File(filename)));
		AudioFormat format = input.getAudioFormat();
		VocalTractLinearScalerEffect effect = new VocalTractLinearScalerEffect((int) format.getSampleRate());
		DoubleDataSource output = effect.apply(input, "amount:" + factor);
		DDSAudioInputStream audioOut = new DDSAudioInputStream(output, format);
		String outFilename = FilenameUtils.removeExtension(filename) + "_" + factor + ".wav";
		AudioSystem.write(audioOut, AudioFileFormat.Type.WAVE, new File(outFilename));
		System.out.println("Created file " + outFilename);
	}
}
