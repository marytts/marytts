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
package marytts.signalproc.adaptation.codebook;

import java.io.IOException;

import marytts.signalproc.adaptation.BaselineFeatureExtractor;
import marytts.signalproc.analysis.EnergyFileHeader;
import marytts.signalproc.analysis.LsfFileHeader;
import marytts.signalproc.analysis.MfccFileHeader;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.util.io.MaryRandomAccessFile;

/**
 * 
 * A class for handling file I/O of weighted codebook file headers
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookFileHeader {
	public int totalEntries;

	// Codebook type
	public int codebookType;
	public static int FRAMES = 1; // Frame-by-frame mapping of features
	public static int FRAME_GROUPS = 2; // Mapping of frame average features (no label information but fixed amount of
										// neighbouring frames is used)
	public static int LABELS = 3; // Mapping of label average features
	public static int LABEL_GROUPS = 4; // Mapping of average features collected across label groups (i.e. vowels, consonants,
										// etc)
	public static int SPEECH = 5; // Mapping of average features collected across all speech parts (i.e. like spectral
									// equalization)
	//

	public String sourceTag; // Source name tag (i.e. style or speaker identity)
	public String targetTag; // Target name tag (i.e. style or speaker identity)

	public LsfFileHeader lsfParams;
	public PitchFileHeader ptcParams;
	public EnergyFileHeader energyParams;
	public MfccFileHeader mfccParams;

	public int numNeighboursInFrameGroups; // Functional only when codebookType == FRAME_GROUPS
	public int numNeighboursInLabelGroups; // Functional only when codebookType == LABEL_GROUPS

	public int vocalTractFeature; // Feature to be used for representing vocal tract

	public WeightedCodebookFileHeader() {
		this(0);
	}

	public WeightedCodebookFileHeader(int totalEntriesIn) {
		totalEntries = totalEntriesIn;

		codebookType = FRAMES;

		sourceTag = "source"; // Source name tag (i.e. style or speaker identity)
		targetTag = "target"; // Target name tag (i.e. style or speaker identity)

		lsfParams = new LsfFileHeader();
		ptcParams = new PitchFileHeader();
		energyParams = new EnergyFileHeader();
		mfccParams = new MfccFileHeader();

		vocalTractFeature = BaselineFeatureExtractor.LSF_FEATURES;
	}

	public WeightedCodebookFileHeader(WeightedCodebookFileHeader h) {
		totalEntries = h.totalEntries;

		codebookType = h.codebookType;

		sourceTag = h.sourceTag;
		targetTag = h.targetTag;

		lsfParams = new LsfFileHeader(h.lsfParams);
		ptcParams = new PitchFileHeader(h.ptcParams);
		energyParams = new EnergyFileHeader(h.energyParams);
		mfccParams = new MfccFileHeader(h.mfccParams);

		numNeighboursInFrameGroups = h.numNeighboursInFrameGroups;
		numNeighboursInLabelGroups = h.numNeighboursInLabelGroups;

		vocalTractFeature = h.vocalTractFeature;
	}

	public void resetTotalEntries() {
		totalEntries = 0;
	}

	public void read(MaryRandomAccessFile ler) throws IOException {
		totalEntries = ler.readInt();

		lsfParams = new LsfFileHeader();
		lsfParams.readHeader(ler);

		ptcParams = new PitchFileHeader();
		ptcParams.readPitchHeader(ler);

		energyParams = new EnergyFileHeader();
		energyParams.read(ler, true);

		mfccParams.readHeader(ler);

		codebookType = ler.readInt();
		numNeighboursInFrameGroups = ler.readInt();
		numNeighboursInLabelGroups = ler.readInt();

		int tagLen = ler.readInt();
		sourceTag = String.copyValueOf(ler.readChar(tagLen));
		tagLen = ler.readInt();
		targetTag = String.copyValueOf(ler.readChar(tagLen));

		vocalTractFeature = ler.readInt();
	}

	public void write(MaryRandomAccessFile ler) throws IOException {
		ler.writeInt(totalEntries);

		lsfParams.writeHeader(ler);
		ptcParams.writePitchHeader(ler);
		energyParams.write(ler);
		mfccParams.writeHeader(ler);

		ler.writeInt(codebookType);
		ler.writeInt(numNeighboursInFrameGroups);
		ler.writeInt(numNeighboursInLabelGroups);

		int tagLen = sourceTag.length();
		ler.writeInt(tagLen);
		ler.writeChar(sourceTag.toCharArray());

		tagLen = targetTag.length();
		ler.writeInt(tagLen);
		ler.writeChar(targetTag.toCharArray());

		ler.writeInt(vocalTractFeature);
	}
}
