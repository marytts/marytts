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
package marytts.signalproc.filter;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.signal.SignalProcUtils;

/**
 * This class implements a single channel of the complementary filter bank used in [Levine, et. al., 1999] for multiresolution
 * sinusoidal modeling.
 * 
 * [Levine, et. al., 1999] Levine, S. N., Verma, T. S., and Smith III, J. O., "Multiresolution sinusoidal modeling for wideband
 * audio with modifications", in Proc. of the IEEE ICASSP 1998, Volume 6, Issue , 12-15 May 1998, pp. 3585-3588.
 * 
 * @author Oytun T&uuml;rk
 */
public class ComplementaryFilterBankChannelAnalyser {
	public double[] lpfOut; // Output of lowpass sub-channel
	public double[] hpfOut; // Output of highpass sub-channel
	public double[] lpfOutInterpolated; // Upsampled version of lowpass sub-channel output
	public double lpfOutEnergy; // Energy of output lowpass sub-channel

	protected LowPassFilter Hd;
	protected LowPassFilter Hb;
	protected LowPassFilter Hi;
	protected int filterLengthMinusOne;
	protected double[] filterNumerator;

	public ComplementaryFilterBankChannelAnalyser(int N) {
		if (N % 2 != 0)
			N++;

		filterLengthMinusOne = N;
		Hd = new LowPassFilter(0.5 * 0.5, filterLengthMinusOne + 1);
		Hb = new LowPassFilter(0.5 * 0.95, (int) (0.5 * filterLengthMinusOne + 1));
		Hi = new LowPassFilter(0.5 * 0.5, filterLengthMinusOne + 1);

		filterNumerator = new double[1];
		filterNumerator[0] = 1.0;
	}

	public double[] applyToOutputHighComponent(double[] x) {
		apply(x);

		return hpfOut;
	}

	public double[] applyToOutputLowComponent(double[] x) {
		apply(x);

		return lpfOut;
	}

	public double[] applyToOutputLowInterpolatedComponent(double[] x) {
		apply(x);

		return lpfOutInterpolated;
	}

	public void apply(double[] x) {
		int i;
		double[] lpfOutTmp;

		// lpfOutTmp = SignalProcUtils.filter(Hd.getDenumeratorCoefficients(), filterNumerator, x);
		lpfOutTmp = Hd.apply(x);

		lpfOutTmp = SignalProcUtils.decimate(lpfOutTmp, 2.0);
		// lpfOutTmp = SignalProcUtils.filter(Hb.getDenumeratorCoefficients(), filterNumerator, lpfOutTmp);
		lpfOutTmp = Hb.apply(lpfOutTmp);

		// Interpolation of lowpass channel
		lpfOutInterpolated = SignalProcUtils.interpolate(lpfOutTmp, 2.0);
		// lpfOutInterpolated = SignalProcUtils.filter(Hi.getDenumeratorCoefficients(), filterNumerator, lpfOutInterpolated);
		lpfOutInterpolated = Hi.apply(lpfOutInterpolated);
		//

		// Energy compensation
		double enx = SignalProcUtils.energy(x);
		double enxloi = SignalProcUtils.energy(lpfOutInterpolated);
		double gxloi = Math.sqrt(enx / enxloi);

		for (i = 0; i < lpfOutInterpolated.length; i++)
			lpfOutInterpolated[i] *= gxloi;
		//

		int delay = (int) Math.floor(1.5 * filterLengthMinusOne + 0.5) - 1;

		hpfOut = new double[x.length];
		for (i = 0; i < x.length - delay; i++)
			hpfOut[i] = x[i] - lpfOutInterpolated[i + delay];
		for (i = x.length - delay; i < x.length; i++)
			hpfOut[i] = 0.0;

		delay = (int) Math.floor(0.5 * filterLengthMinusOne + 0.5);
		lpfOut = new double[lpfOutTmp.length - delay];
		for (i = delay; i < lpfOutTmp.length; i++)
			lpfOut[i - delay] = lpfOutTmp[i];

		lpfOutEnergy = SignalProcUtils.energy(lpfOut);
	}

	public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
		AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(args[0]));
		int samplingRate = (int) inputAudio.getFormat().getSampleRate();
		AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
		double[] x = signal.getAllData();

		int N = 512;
		ComplementaryFilterBankChannelAnalyser channel = new ComplementaryFilterBankChannelAnalyser(N);
		channel.apply(x);

		DDSAudioInputStream outputAudio;
		String outFileName;

		// Lowpass output
		AudioFormat loFormat = new AudioFormat(inputAudio.getFormat().getSampleRate() * 0.5f, inputAudio.getFormat()
				.getSampleSizeInBits(), inputAudio.getFormat().getChannels(), true, true);
		outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(channel.lpfOut), loFormat);
		outFileName = args[0].substring(0, args[0].length() - 4) + "_lo.wav";
		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		//

		// Lowpass interpolated output
		AudioFormat loiFormat = new AudioFormat(inputAudio.getFormat().getSampleRate(), inputAudio.getFormat()
				.getSampleSizeInBits(), inputAudio.getFormat().getChannels(), true, true);
		outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(channel.lpfOutInterpolated), loiFormat);
		outFileName = args[0].substring(0, args[0].length() - 4) + "_loi.wav";
		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		//

		// Highpass output
		outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(channel.hpfOut), inputAudio.getFormat());
		outFileName = args[0].substring(0, args[0].length() - 4) + "_hi.wav";
		AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outFileName));
		//
	}
}
