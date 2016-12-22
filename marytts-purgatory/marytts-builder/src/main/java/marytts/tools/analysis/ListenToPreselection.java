/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.tools.analysis;

import java.io.ByteArrayOutputStream;
import java.io.File;

import marytts.cart.impose.FeatureArrayIndexer;
import marytts.cart.impose.FeatureFileIndexingResult;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureVector;
import marytts.tools.voiceimport.DatabaseLayout;
import marytts.tools.voiceimport.HalfPhoneFeatureFileWriter;
import marytts.tools.voiceimport.VoiceImportComponent;
import marytts.tools.voiceimport.WaveTimelineMaker;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.Datagram;
import marytts.util.data.audio.WavWriter;

public class ListenToPreselection {

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {

		WaveTimelineMaker wtlm = new WaveTimelineMaker();
		HalfPhoneFeatureFileWriter ffw = new HalfPhoneFeatureFileWriter();
		VoiceImportComponent[] comps = new VoiceImportComponent[2];
		comps[0] = wtlm;
		comps[1] = ffw;
		DatabaseLayout dbl = new DatabaseLayout(new File(System.getProperty("user.dir", "."), "database.config"), comps);
		UnitFileReader ufr = new UnitFileReader(ffw.getProp(ffw.UNITFILE));
		TimelineReader tlr = new TimelineReader(wtlm.getProp(wtlm.WAVETIMELINE));
		// TimelineReader tlr = new TimelineReader( dbl.lpcTimelineFileName() );
		FeatureFileReader ffr = new FeatureFileReader(ffw.getProp(ffw.FEATUREFILE));
		FeatureArrayIndexer ffi = new FeatureArrayIndexer(ffr.getFeatureVectors(), ffr.getFeatureDefinition());
		FeatureDefinition feaDef = ffi.getFeatureDefinition();
		WavWriter ww = new WavWriter();

		System.out.println("Indexing the phones...");
		String[] feaSeq = { "phone" }; // Sort by phone name
		ffi.deepSort(feaSeq);

		/* Loop across possible phones */
		long tic = System.currentTimeMillis();
		int mary_phoneIndex = feaDef.getFeatureIndex("phone");
		int nbPhonVal = feaDef.getNumberOfValues(feaDef.getFeatureIndex("phone"));
		for (int phon = 1; phon < nbPhonVal; phon++) {
			// for ( int phon = 14; phon < nbPhonVal; phon++ ) {
			String phonID = feaDef.getFeatureValueAsString(0, phon);
			/* Loop across all instances */
			byte[] phonFeature = new byte[mary_phoneIndex + 1];
			phonFeature[mary_phoneIndex] = (byte) (phon);
			FeatureVector target = new FeatureVector(phonFeature, new short[0], new float[0], 0);
			FeatureFileIndexingResult instances = ffi.retrieve(target);
			int[] ui = instances.getUnitIndexes();
			System.out.println("Concatenating the phone [" + phonID + "] which has [" + ui.length + "] instances...");
			ByteArrayOutputStream bbis = new ByteArrayOutputStream();
			/* Concatenate the instances */
			for (int i = 0; i < ui.length; i++) {
				/* Concatenate the datagrams from the instances */
				Datagram[] dat = tlr.getDatagrams(ufr.getUnit(ui[i]), ufr.getSampleRate());
				for (int k = 0; k < dat.length; k++) {
					bbis.write(dat[k].getData());
				}
			}
			/* Get the bytes as an array */
			byte[] buf = bbis.toByteArray();
			/* Output the header of the wav file */
			String fName = (dbl.getProp(dbl.ROOTDIR) + "/tests/" + phonID + ".wav");
			System.out.println("Outputting file [" + fName + "]...");
			ww.export(fName, 16000, buf);
			/* Sanity check */
			/*
			 * WavReader wr = new WavReader( dbl.rootDirName() + "/tests/" + phonID + ".wav" ); System.out.println( "File [" + (
			 * dbl.rootDirName() + "/tests/" + phonID + ".wav" ) + "] has [" + wr.getNumSamples() + "] samples." );
			 */
		}
		long toc = System.currentTimeMillis();
		System.out.println("Copying the phones took [" + (toc - tic) + "] milliseconds.");
	}

}
