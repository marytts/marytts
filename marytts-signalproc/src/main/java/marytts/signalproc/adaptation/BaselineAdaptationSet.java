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
package marytts.signalproc.adaptation;

import marytts.util.io.BasenameList;

/**
 * Baseline class for speaker specific voice conversion training set
 * 
 * @author Oytun T&uuml;rk
 */
public class BaselineAdaptationSet {
	public BaselineAdaptationItem[] items;
	public static final String WAV_EXTENSION_DEFAULT = ".wav";
	public static final String SINUSOID_EXTENSION_DEFAULT = ".sin";
	public static final String NOISE_EXTENSION_DEFAULT = ".noi";
	public static final String TRANSIENT_EXTENSION_DEFAULT = ".tra";
	public static final String RESIDUAL_EXTENSION_DEFAULT = ".res";
	public static final String LABEL_EXTENSION_DEFAULT = ".lab";
	public static final String PITCH_EXTENSION_DEFAULT = ".ptc";
	public static final String F0_EXTENSION_DEFAULT = ".f0";
	public static final String PITCHMARK_EXTENSION_DEFAULT = ".pm";
	public static final String ENERGY_EXTENSION_DEFAULT = ".ene";
	public static final String TEXT_EXTENSION_DEFAULT = ".txt";
	public static final String RAWMFCC_EXTENSION_DEFAULT = ".mgc";
	public static final String MFCC_EXTENSION_DEFAULT = ".mfc";
	public static final String LSF_EXTENSION_DEFAULT = ".lsf";
	public static final String LPC_EXTENSION_DEFAULT = ".lpc";
	public static final String LPRESIDUAL_EXTENSION_DEFAULT = ".lpr";
	public static final String CEPSTRUM_EXTENSION_DEFAULT = ".cep";
	public static final String EGG_EXTENSION_DEFAULT = ".egg";
	public static final String TARGETFESTIVALUTT_EXTENSION_DEFAULT = ".tutt";
	public static final String TARGETLABEL_EXTENSION_DEFAULT = ".tlab";
	public static final String TARGETPITCH_EXTENSION_DEFAULT = ".tptc";
	public static final String TARGETF0_EXTENSION_DEFAULT = ".tf0";
	public static final String TARGETENERGY_EXTENSION_DEFAULT = ".tene";
	public static final String TARGETWAV_EXTENSION_DEFAULT = ".twav";

	public BaselineAdaptationSet() {
		items = null;
	}

	public BaselineAdaptationSet(int numItems) {
		allocate(numItems);
	}

	public BaselineAdaptationSet(String folder) {
		this(folder, WAV_EXTENSION_DEFAULT);
	}

	public BaselineAdaptationSet(String folder, String referenceFileExt) {
		BasenameList b = new BasenameList(folder, referenceFileExt);

		allocate(b.getListAsVector().size());

		for (int i = 0; i < items.length; i++)
			items[i].setFromWavFilename(folder + b.getName(i) + referenceFileExt);
	}

	public void allocate(int numItems) {
		if (numItems > 0) {
			items = new BaselineAdaptationItem[numItems];
			for (int i = 0; i < numItems; i++)
				items[i] = new BaselineAdaptationItem();
		} else
			items = null;
	}

	public String[] getLabelFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].labelFile;
		}

		return fileList;
	}

	public String[] getLsfFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].lsfFile;
		}

		return fileList;
	}

	public String[] getAudioFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].audioFile;
		}

		return fileList;
	}

	public String[] getCepsFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].cepsFile;
		}

		return fileList;
	}

	public String[] getEggFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].eggFile;
		}

		return fileList;
	}

	public String[] getPitchFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].pitchFile;
		}

		return fileList;
	}

	public String[] getLpcFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].lpcFile;
		}

		return fileList;
	}

	public String[] getLpResidualFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].lpResidualFile;
		}

		return fileList;
	}

	public String[] getRawMfccFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].rawMfccFile;
		}

		return fileList;
	}

	public String[] getMfccFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].mfccFile;
		}

		return fileList;
	}

	public String[] getNoiseFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].noiseFile;
		}

		return fileList;
	}

	public String[] getPitchMarkFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].pitchMarkFile;
		}

		return fileList;
	}

	public String[] getResidualFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].residualFile;
		}

		return fileList;
	}

	public String[] getSinesFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].sinesFile;
		}

		return fileList;
	}

	public String[] getTextFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].textFile;
		}

		return fileList;
	}

	public String[] getTransientsFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].transientsFile;
		}

		return fileList;
	}

	public String[] getEnergyFiles() {
		String[] fileList = null;
		if (items != null && items.length > 0) {
			fileList = new String[items.length];
			for (int i = 0; i < items.length; i++)
				fileList[i] = items[i].energyFile;
		}

		return fileList;
	}
}
