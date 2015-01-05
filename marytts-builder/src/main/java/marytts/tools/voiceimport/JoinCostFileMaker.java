/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureVector;
import marytts.unitselection.data.FeatureFileReader;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.UnitFileReader;
import marytts.unitselection.select.JoinCostFeatures;
import marytts.util.data.Datagram;
import marytts.util.data.ESTTrackReader;
import marytts.util.data.MaryHeader;

public class JoinCostFileMaker extends VoiceImportComponent {

	private DatabaseLayout db = null;
	private int percent = 0;
	private String mcepExt = ".mcep";
	private int numberOfFeatures = 0;
	private float[] fw = null;
	private String[] wfun = null;

	public final String JOINCOSTFILE = "JoinCostFileMaker.joinCostFile";
	public final String MCEPTIMELINE = "JoinCostFileMaker.mcepTimeline";
	public final String UNITFILE = "JoinCostFileMaker.unitFile";
	public final String FEATUREFILE = "JoinCostFileMaker.acfeatureFile";
	public final String WEIGHTSFILE = "JoinCostFileMaker.weightsFile";
	public final String MCEPDIR = "JoinCostFileMaker.mcepDir";

	public String getName() {
		return "JoinCostFileMaker";
	}

	@Override
	protected void initialiseComp() {
		// make sure that we have a weights file
		File weightsFile = new File(getProp(WEIGHTSFILE));
		if (!weightsFile.exists()) {
			try {
				PrintWriter weightsOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(weightsFile), "UTF-8"));
				printWeightsFile(weightsOut);
			} catch (Exception e) {
				System.out.println("Warning: no join cost weights file " + getProp(WEIGHTSFILE)
						+ "; JoinCostFileMaker will not run.");
			}
		}
	}

	public SortedMap<String, String> getDefaultProps(DatabaseLayout db) {
		this.db = db;
		if (props == null) {
			props = new TreeMap<String, String>();
			String filedir = db.getProp(db.FILEDIR);
			props.put(JOINCOSTFILE, filedir + "joinCostFeatures" + db.getProp(db.MARYEXT));
			props.put(MCEPTIMELINE, filedir + "timeline_mcep" + db.getProp(db.MARYEXT));
			props.put(UNITFILE, filedir + "halfphoneUnits" + db.getProp(db.MARYEXT));
			props.put(FEATUREFILE, filedir + "halfphoneFeatures_ac" + db.getProp(db.MARYEXT));
			props.put(WEIGHTSFILE, db.getProp(db.CONFIGDIR) + "joinCostWeights.txt");
			props.put(MCEPDIR, db.getProp(db.ROOTDIR) + "mcep" + System.getProperty("file.separator"));
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap<String, String>();
		props2Help.put(JOINCOSTFILE, "file containing all halfphone units and their join cost features."
				+ " Will be created by this module");
		props2Help.put(MCEPTIMELINE, "file containing all mcep files");
		props2Help.put(UNITFILE, "file containing all halfphone units");
		props2Help.put(FEATUREFILE, "file containing all halfphone features including acoustic features");
		props2Help.put(WEIGHTSFILE, "file containing the list of join cost weights and their weights");
		props2Help.put(MCEPDIR, "directory containing the mcep files");

	}

	@Override
	public boolean compute() throws IOException, MaryConfigurationException {
		System.out.print("---- Making the join cost file\n");

		/* Export the basename list into an array of strings */
		String[] baseNameArray = bnl.getListAsArray();

		/* Read the number of mel cepstra from the first melcep file */
		ESTTrackReader firstMcepFile = new ESTTrackReader(getProp(MCEPDIR) + baseNameArray[0] + mcepExt);
		int numberOfMelcep = firstMcepFile.getNumChannels();
		firstMcepFile = null; // Free the memory taken by the file

		/* Make a new join cost file to write to */
		DataOutputStream jcf = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getProp(JOINCOSTFILE))));

		/**********/
		/* HEADER */
		/**********/
		/* Make a new mary header and ouput it */
		MaryHeader hdr = new MaryHeader(MaryHeader.JOINFEATS);
		hdr.writeTo(jcf);
		hdr = null;

		/****************************/
		/* WEIGHTING FUNCTION SPECS */
		/****************************/
		/* Load the weight vectors */
		Object[] weightData = JoinCostFeatures.readJoinCostWeightsFile(getProp(WEIGHTSFILE));
		fw = (float[]) weightData[0];
		wfun = (String[]) weightData[1];
		numberOfFeatures = fw.length;
		int numberOfProsodyFeatures = 2;

		/* Output those vectors */
		jcf.writeInt(fw.length);
		for (int i = 0; i < fw.length; i++) {
			jcf.writeFloat(fw[i]);
			jcf.writeUTF(wfun[i]);
		}
		/* Clean the house */
		fw = null;
		wfun = null;

		/************/
		/* FEATURES */
		/************/

		/* Open the melcep timeline */
		TimelineReader mcep = new TimelineReader(getProp(MCEPTIMELINE));

		/* Open the unit file */
		UnitFileReader ufr = new UnitFileReader(getProp(UNITFILE));

		// And the feature file
		FeatureFileReader features = new FeatureFileReader(getProp(FEATUREFILE));

		/* Start writing the features: */

		/* - write the number of features: */
		jcf.writeInt(ufr.getNumberOfUnits());
		int unitSampleFreq = ufr.getSampleRate();
		if (unitSampleFreq != mcep.getSampleRate()) {
			throw new MaryConfigurationException("Cannot currently deal with different sample rates in unit and mcep files.");
		}
		long unitPosition = 0l;
		int unitDuration = 0;

		/*
		 * Check the consistency between the number of join cost features and the number of Mel-cepstrum coefficients
		 */
		Datagram dat = mcep.getDatagram(0);
		if (dat.getData().length != (4 * (numberOfFeatures - numberOfProsodyFeatures))) {
			throw new MaryConfigurationException("The number of join cost features [" + numberOfFeatures
					+ "] read from the join cost weight config file [" + getProp(WEIGHTSFILE)
					+ "] does not match the number of Mel Cepstra [" + (dat.getData().length / 4)
					+ "] found in the Mel-Cepstrum timeline file [" + getProp(MCEPTIMELINE) + "], plus ["
					+ numberOfProsodyFeatures + "] for the prosody features.");
		}

		/* Loop through the units */
		int fiLogF0 = features.getFeatureDefinition().getFeatureIndex("unit_logf0");
		int fiLogF0Delta = features.getFeatureDefinition().getFeatureIndex("unit_logf0delta");
		for (int i = 0; i < ufr.getNumberOfUnits(); i++) {
			percent = 100 * i / ufr.getNumberOfUnits();

			FeatureVector fv = features.getFeatureVector(i);
			float logf0 = fv.getContinuousFeature(fiLogF0);
			float logf0Delta = fv.getContinuousFeature(fiLogF0Delta);

			// logf0 is the value in the middle, i.e.
			// leftF0 + 0.5 * logf0Delta == logf0, and
			// logf0 + 0.5 * logf0Delta == rightF0
			float leftLogF0 = logf0 - 0.5f * logf0Delta;
			float rightLogF0 = logf0 + 0.5f * logf0Delta;

			/* Read the unit */
			unitPosition = ufr.getUnit(i).startTime;
			unitDuration = ufr.getUnit(i).duration;

			/*
			 * If the unit is not a START or END marker and has length > 0:
			 */
			if (unitDuration != -1 && unitDuration > 0) {

				Datagram leftMCEP = mcep.getDatagram(unitPosition);
				jcf.write(leftMCEP.getData());
				jcf.writeFloat(leftLogF0);
				jcf.writeFloat(logf0Delta);

				/* -- COMPUTE the RIGHT JCFs: */
				Datagram rightMCEP;
				boolean useRightContext = false;

				if (useRightContext) {
					// the right context datagram:
					rightMCEP = mcep.getDatagram(unitPosition + unitDuration);
				} else {
					/* Crawl along the datagrams until we trespass the end of the unit: */
					long endPoint = unitPosition;
					dat = mcep.getDatagram(endPoint);
					while (endPoint + dat.getDuration() < unitPosition + unitDuration) {
						endPoint += dat.getDuration();
						dat = mcep.getDatagram(endPoint);
					}
					rightMCEP = dat;
				}
				jcf.write(rightMCEP.getData());
				jcf.writeFloat(rightLogF0);
				jcf.writeFloat(logf0Delta);
			}

			/* If the unit is a START or END marker, output NaN for all features */
			else {
				for (int j = 0; j < 2 * numberOfFeatures; j++) {
					jcf.writeFloat(Float.NaN);
				}
			}
		}

		jcf.close();
		System.out.println("---- Join Cost file done.\n\n");
		System.out.println("Number of processed units: " + ufr.getNumberOfUnits());

		JoinCostFeatures tester = new JoinCostFeatures(getProp(JOINCOSTFILE));
		int unitsOnDisk = tester.getNumberOfUnits();
		if (unitsOnDisk == ufr.getNumberOfUnits()) {
			System.out.println("Can read right number of units");
			return true;
		} else {
			System.out.println("Read wrong number of units: " + unitsOnDisk);
			return false;
		}
	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return percent;
	}

	private void printWeightsFile(PrintWriter weightsOut) throws Exception {
		weightsOut.println("# This file lists the weights and weighting functions to be used for\n"
				+ "# creating the MARY join cost file, joinCostFeature.mry .\n" + "#\n"
				+ "# Lines starting with '#' are ignored; they can be used for comments\n"
				+ "# anywhere in the file. Empty lines are also ignored.\n" + "# Entries must have the following form:\n"
				+ "# \n" + "# <feature index> : <weight value> <weighting function> <optional weighting function parameter>\n"
				+ "# \n" + "# The <feature index> is an integer value from 0 to the number of join cost features\n"
				+ "# minus one. It is used for readability, but is ignored when parsing the file.\n"
				+ "# The database import process will nevertheless check that the number of valid\n"
				+ "# lines corresponds to the number of join cost features specified from external\n"
				+ "# constraints (such as the order of the Mel-Cepstra).\n" + "#\n"
				+ "# The <weight value> is a float value in text format.\n" + "#\n"
				+ "# The <weighting function> is a string, for the moment one of \"linear\" or \"step\".\n" + "#\n"
				+ "# The <optional weighting function parameter> is a string giving additional optional\n"
				+ "# info about the weighting function:\n" + "# - \"linear\" does not take an optional argument;\n"
				+ "# - \"step\" takes a threshold position argument, e.g. \"step 20%\" means a step function\n"
				+ "#   with weighs 0 when the join feature difference is less than 20%, and applies\n"
				+ "#   the weight value when the join feature difference is 20% or more.\n" + "#\n"
				+ "# THIS FILE WAS GENERATED AUTOMATICALLY\n" + "\n" + "# Weights applied to the Mel-cepstra:\n"
				+ "0  : 1.0 linear\n" + "1  : 1.0 linear\n" + "2  : 1.0 linear\n" + "3  : 1.0 linear\n" + "4  : 1.0 linear\n"
				+ "5  : 1.0 linear\n" + "6  : 1.0 linear\n" + "7  : 1.0 linear\n" + "8  : 1.0 linear\n" + "9  : 1.0 linear\n"
				+ "10 : 1.0 linear\n" + "11 : 1.0 linear\n" + "\n"
				+ "# Weight applied to the log F0 and log F0 delta parameters:\n" + "12 : 10.0 linear\n" + "13 : 0.5 linear");
		weightsOut.flush();
		weightsOut.close();
	}

}
