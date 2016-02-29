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
package marytts.signalproc.adaptation.prosody;

import java.io.IOException;

import marytts.util.io.MaryRandomAccessFile;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * 
 * Pitch statistics (could be for all recordings, for a group of recordings or even for a single utterance): - Mean of (voiced)
 * f0s - Standard deviation of (voiced) f0s - Global minimum of f0s - Global maximum of f0s - Average tilt of f0 contours, or tilt
 * of single contour
 * 
 * @author Oytun T&uuml;rk
 */
public class PitchStatistics {
	public static int STATISTICS_IN_HERTZ = 1;
	public static int STATISTICS_IN_LOGHERTZ = 2;
	public static int DEFAULT_STATISTICS = STATISTICS_IN_HERTZ;
	public int type;

	public boolean isSource;
	public boolean isGlobal;

	public double mean;
	public double standardDeviation;
	public double range;
	public double intercept;
	public double slope;

	public PitchStatistics(PitchStatistics existing) {
		type = existing.type;
		isSource = existing.isSource;
		isGlobal = existing.isGlobal;
		mean = existing.mean;
		standardDeviation = existing.standardDeviation;
		range = existing.range;
		intercept = existing.intercept;
		slope = existing.slope;
	}

	public PitchStatistics() {
		this(DEFAULT_STATISTICS, true, true);
	}

	public PitchStatistics(int typeIn, boolean isSourceIn, boolean isGlobalIn) {
		type = typeIn;

		init();

		isSource = isSourceIn;
		isGlobal = isGlobalIn;
	}

	public PitchStatistics(int typeIn, double[] f0s) {
		this(typeIn, true, true, f0s);
	}

	public PitchStatistics(int typeIn, boolean isSourceIn, boolean isGlobalIn, double[] f0s) {
		type = typeIn;

		init();

		isSource = isSourceIn;
		isGlobal = isGlobalIn;

		double[] voiceds = SignalProcUtils.getVoiceds(f0s);

		if (type == PitchStatistics.STATISTICS_IN_LOGHERTZ)
			voiceds = SignalProcUtils.getLogF0s(voiceds);

		if (voiceds != null) {
			mean = MathUtils.mean(voiceds);
			standardDeviation = MathUtils.standardDeviation(voiceds, mean);

			range = SignalProcUtils.getF0Range(voiceds);

			double[] contourInt = SignalProcUtils.interpolate_pitch_uv(f0s);
			double[] line = SignalProcUtils.getContourLSFit(contourInt, false);
			intercept = line[0];
			slope = line[1];
		} else {
			mean = 0.0;
			standardDeviation = 1.0;
		}
	}

	public void init() {
		mean = 0.0;
		standardDeviation = 0.0;
		range = 0.0;
		intercept = 0.0;
		slope = 0.0;
	}

	public void read(MaryRandomAccessFile ler) {
		try {
			type = ler.readInt();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			isSource = ler.readBoolean();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			isGlobal = ler.readBoolean();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			mean = ler.readDouble();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			standardDeviation = ler.readDouble();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			range = ler.readDouble();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			intercept = ler.readDouble();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			slope = ler.readDouble();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void write(MaryRandomAccessFile ler) {
		try {
			ler.writeInt(type);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ler.writeBoolean(isSource);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ler.writeBoolean(isGlobal);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ler.writeDouble(mean);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ler.writeDouble(standardDeviation);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ler.writeDouble(range);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ler.writeDouble(intercept);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ler.writeDouble(slope);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
