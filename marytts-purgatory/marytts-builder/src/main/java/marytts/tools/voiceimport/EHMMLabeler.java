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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.io.FileUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Automatic Labelling using EHMM labeller
 * 
 * @author Sathish Chandra Pammi
 */

public class EHMMLabeler extends VoiceImportComponent {

	private DatabaseLayout db;
	private File rootDir;
	private File ehmm;
	private String voicename;
	private String outputDir;
	protected String featsExt = ".pfeats";
	protected String labExt = ".lab";
	protected String xmlExt = ".xml";

	private int progress = -1;
	private int countShortSil = 0;
	private String locale;

	public final String EDIR = "EHMMLabeler.eDir";
	public final String EHMMDIR = "EHMMLabeler.ehmmDir";
	public final String ALLOPHONESDIR = "EHMMLabeler.promtallophonesDir";
	public final String OUTLABDIR = "EHMMLabeler.outputLabDir";
	public final String INITEHMMDIR = "EHMMLabeler.startEHMMModelDir";
	public final String RETRAIN = "EHMMLabeler.reTrainFlag";
	public final String NONDETENDFLAG = "EHMMLabeler.nonDetEndFlag";

	public final String PREPAREFILESFLAG = "EHMMLabeler.prepareFiles";
	public final String TRAININGFLAG = "EHMMLabeler.doTraining";
	public final String ALIGNMENTFLAG = "EHMMLabeler.doAlignment";

	private final int EHMM_TRAIN_NUM_THREADS = Math.min(4, Runtime.getRuntime().availableProcessors()); // more than 4 threads
																										// don't help for ehmm
																										// training
	private final int EHMM_ALIGN_NUM_THREADS = Runtime.getRuntime().availableProcessors();

	public final String getName() {
		return "EHMMLabeler";
	}

	public SortedMap getDefaultProps(DatabaseLayout db) {
		this.db = db;
		String phoneXml;
		locale = db.getProp(db.LOCALE);
		if (props == null) {
			props = new TreeMap();
			String ehmmdir = System.getProperty("EHMMDIR");
			if (ehmmdir == null) {
				ehmmdir = db.getExternal(db.EHMMPATH);
				if (ehmmdir == null) {
					ehmmdir = "/project/mary/Festival/festvox/src/ehmm/";
				}
			}
			props.put(EHMMDIR, ehmmdir);
			props.put(EDIR, db.getProp(db.ROOTDIR) + "ehmm" + System.getProperty("file.separator"));
			props.put(ALLOPHONESDIR, db.getProp(db.ROOTDIR) + "prompt_allophones" + System.getProperty("file.separator"));
			props.put(OUTLABDIR, db.getProp(db.ROOTDIR) + "lab" + System.getProperty("file.separator"));
			props.put(INITEHMMDIR, "/");
			props.put(RETRAIN, "false");
			props.put(NONDETENDFLAG, "0");
			props.put(PREPAREFILESFLAG, "true");
			props.put(TRAININGFLAG, "true");
			props.put(ALIGNMENTFLAG, "true");
		}
		return props;
	}

	protected void setupHelp() {
		props2Help = new TreeMap();
		props2Help.put(EHMMDIR, "directory containing the local installation of EHMM Labeller");
		props2Help.put(EDIR,
				"directory containing all files used for training and labeling. Will be created if it does not exist.");
		props2Help.put(ALLOPHONESDIR, "directory containing the IntonisedXML files.");
		props2Help.put(OUTLABDIR, "Directory to store generated lebels from EHMM.");
		props2Help
				.put(INITEHMMDIR,
						"If you provide a path to previous EHMM Directory, Models will intialize with those models. other wise EHMM Models will build with Flat-Start Initialization");
		props2Help.put(RETRAIN, "true - Do re-training by initializing with given models. false - Do just Decoding");
		props2Help.put(NONDETENDFLAG, "(0,1) - Viterbi decoding with non deterministic ending (festvox 2.4)");

		props2Help.put(PREPAREFILESFLAG, "(true/false) -- initialize files in the etc/ and feat/ directories");
		props2Help.put(TRAININGFLAG, "(true/false) -- perform model training");
		props2Help.put(ALIGNMENTFLAG, "(true/false) -- perform alignment");
	}

	@Override
	protected void initialiseComp() {
		locale = db.getProp(db.LOCALE);

	}

	/**
	 * Do the computations required by this component.
	 * 
	 * @return true on success, false on failure
	 * @throws Exception
	 *             Exception
	 */
	public boolean compute() throws Exception {

		File ehmmFile = new File(getProp(EHMMDIR) + "/bin/ehmm");
		if (!ehmmFile.exists()) {
			throw new IOException("EHMM path setting is wrong. Because file " + ehmmFile.getAbsolutePath() + " does not exist");
		}
		// get the voicename
		voicename = db.getProp(db.VOICENAME);
		// make new directories ehmm and etc
		ehmm = new File(getProp(EDIR));
		// get the output directory of files used by EHMM
		outputDir = ehmm.getAbsolutePath() + "/etc";
		if ("true".equals(getProp(PREPAREFILESFLAG))) {
			System.out.println("Preparing voice database for labelling using EHMM :");
			System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
			// setup the EHMM directory
			System.out.println("Setting up EHMM directory ...");
			setup();
			System.out.println(" ... done.");

			// Getting Phone Sequence for Force Alignment
			System.out.println("Getting Phone Sequence from Phone Features...");
			getPhoneSequence();
			System.out.println(" ... done.");

			System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
			// dump the filenames
			System.out.println("Dumping required files ....");
			dumpRequiredFiles();
			System.out.println(" ... done.");

			System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
			// Computing Features (MFCCs) for EHMM
			System.out.println("Computing MFCCs ...");
			computeFeatures();
			System.out.println(" ... done.");

			System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
			System.out.println("Scaling Feature Vectors ...");
			scaleFeatures();
			System.out.println(" ... done.");

			System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
			System.out.println("Converting Feature Vectors to Binary Format ...");
			convertToBinaryFeatures();
			System.out.println(" ... done.");

			System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
			System.out.println("Intializing EHMM Model ...");
			intializeEHMMModels();
			System.out.println(" ... done.");
		} else {
			System.out.println("Skipping preparatory steps.");
		}

		if ("true".equals(getProp(TRAININGFLAG))) {
			baumWelchEHMM();
		} else {
			System.out.println("Skipping training.");
		}

		if ("true".equals(getProp(ALIGNMENTFLAG))) {
			System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
			System.out.println("Aligning EHMM for labelling ...");
			alignEHMM();

			// System.out.println("And Copying label files into lab directory ...");
			// getProperLabelFormat();
			// System.out.println(" ... done.");

			System.out.println("Label file Generation Successfully completed using EHMM !");
		} else {
			System.out.println("Skipping alignment.");
		}

		return true;
	}

	/**
	 * Setup the EHMM directory
	 * 
	 * @throws IOException
	 *             , InterruptedException, MaryConfigurationException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	private void setup() throws IOException, InterruptedException, MaryConfigurationException {
		String task = "setup";
		String line = null;
		ehmm.mkdir();
		File lab = new File(ehmm.getAbsolutePath() + "/lab");
		// call setup of EHMM in this directory
		Runtime rtime = Runtime.getRuntime();
		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
		// go to ehmm directory and setup Directory Structure
		pw.print("( cd " + ehmm.getAbsolutePath() + "; mkdir -p feat" + "; mkdir -p etc" + "; mkdir -p mod" + "; mkdir -p lab"
				+ "; exit )\n");
		pw.flush();
		// shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}

		PrintWriter settings = new PrintWriter(new FileOutputStream(new File(outputDir + "/" + "ehmm" + ".featSettings")));

		// Feature Settings required for EHMM Training
		settings.println("WaveDir: " + db.getProp(db.WAVDIR) + " \n" + "HeaderBytes: 44 \n" + "SamplingFreq: 16000 \n"
				+ "FrameSize: 160 \n" + "FrameShift: 80 \n" + "Lporder: 12 \n" + "CepsNum: 16 \n" + "FeatDir: " + getProp(EDIR)
				+ "/feat \n" + "Ext: .wav \n");
		settings.flush();
		settings.close();
	}

	/**
	 * Creating Required files for EHMM Training
	 * 
	 * @throws IOException
	 *             , InterruptedException, MaryConfigurationException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	private void dumpRequiredFiles() throws IOException, InterruptedException, MaryConfigurationException {
		String task = "dumpRequiredFiles";
		String line = null;
		Runtime rtime = Runtime.getRuntime();
		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
		// go to ehmm directory and create required files for EHMM
		System.out.println("( cd " + ehmm.getAbsolutePath() + "; perl " + getProp(EHMMDIR) + "/bin/phfromutt.pl " + outputDir
				+ "/" + "ehmm" + ".align " + outputDir + "/" + "ehmm" + ".phoneList 5" + "; perl " + getProp(EHMMDIR)
				+ "/bin/getwavlist.pl " + outputDir + "/" + "ehmm" + ".align " + outputDir + "/" + "ehmm" + ".waveList"
				+ "; exit )\n");

		pw.print("( cd " + ehmm.getAbsolutePath() + "; perl " + getProp(EHMMDIR) + "/bin/phfromutt.pl " + outputDir + "/"
				+ "ehmm" + ".align " + outputDir + "/" + "ehmm" + ".phoneList 5 > log.txt" + "; perl " + getProp(EHMMDIR)
				+ "/bin/getwavlist.pl " + outputDir + "/" + "ehmm" + ".align " + outputDir + "/" + "ehmm"
				+ ".waveList >> log.txt" + "; exit )\n");

		pw.flush();
		// shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}
	}

	/**
	 * Computing Features Required files for EHMM Training
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	private void computeFeatures() throws IOException, InterruptedException, MaryConfigurationException {
		String task = "computeFeatures";
		String line = null;
		Runtime rtime = Runtime.getRuntime();
		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
		System.out.println("( cd " + ehmm.getAbsolutePath() + "; " + getProp(EHMMDIR) + "/bin/FeatureExtraction " + outputDir
				+ "/" + "ehmm" + ".featSettings " + outputDir + "/" + "ehmm" + ".waveList >> log.txt" + "; perl "
				+ getProp(EHMMDIR) + "/bin/comp_dcep.pl " + outputDir + "/" + "ehmm" + ".waveList " + ehmm.getAbsoluteFile()
				+ "/feat mfcc ft 0 0 >> log.txt " + "; exit )\n");
		pw.print("( cd " + ehmm.getAbsolutePath() + "; " + getProp(EHMMDIR) + "/bin/FeatureExtraction " + outputDir + "/"
				+ "ehmm" + ".featSettings " + outputDir + "/" + "ehmm" + ".waveList >> log.txt" + "; perl " + getProp(EHMMDIR)
				+ "/bin/comp_dcep.pl " + outputDir + "/" + "ehmm" + ".waveList " + ehmm.getAbsoluteFile()
				+ "/feat mfcc ft 0 0 >> log.txt" + "; exit )\n");
		pw.flush();
		// shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}
	}

	/**
	 * Scaling Features for EHMM Training
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	private void scaleFeatures() throws IOException, InterruptedException, MaryConfigurationException {

		String task = "scaleFeatures";
		String line = null;

		Runtime rtime = Runtime.getRuntime();
		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
		System.out.println("( cd " + ehmm.getAbsolutePath() + "; perl " + getProp(EHMMDIR) + "/bin/scale_feat.pl " + outputDir
				+ "/" + "ehmm" + ".waveList " + ehmm.getAbsoluteFile() + "/feat " + ehmm.getAbsolutePath()
				+ "/mod ft 4 >> log.txt" + "; exit )\n");
		pw.print("( cd " + ehmm.getAbsolutePath() + "; perl " + getProp(EHMMDIR) + "/bin/scale_feat.pl " + outputDir + "/"
				+ "ehmm" + ".waveList " + ehmm.getAbsoluteFile() + "/feat " + ehmm.getAbsolutePath() + "/mod ft 4 >> log.txt"
				+ "; exit )\n");
		pw.flush();
		// shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}

	}

	/**
	 * Convert Features to Binary Format (EHMM2.7 instead of EHMM2.1) for EHMM Training
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	private void convertToBinaryFeatures() throws IOException, InterruptedException, MaryConfigurationException {

		String task = "convertToBinaryFeatures";
		String line = null;

		Runtime rtime = Runtime.getRuntime();
		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
		String cmd = "( cd " + ehmm.getAbsolutePath() + "/feat; for file in *.ft; do " + getProp(EHMMDIR)
				+ "/bin/ConvertFeatsFileToBinaryFormat $file ${file%%.ft}.bft >> ../log.txt; done; exit )\n";
		System.out.println(cmd);
		pw.print(cmd);
		pw.flush();
		// shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}

	}

	/**
	 * Initializing EHMM Models
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	private void intializeEHMMModels() throws IOException, InterruptedException, MaryConfigurationException {
		String task = "intializeEHMMModels";
		String line = null;

		Runtime rtime = Runtime.getRuntime();
		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));

		if (getProp(INITEHMMDIR).equals("/")) {
			pw.print("( cd " + ehmm.getAbsolutePath() + "; perl " + getProp(EHMMDIR) + "/bin/seqproc.pl " + outputDir + "/"
					+ "ehmm" + ".align " + outputDir + "/" + "ehmm" + ".phoneList 2 2 13 >> log.txt" + "; exit )\n");
		} else {
			File modelFile = new File(getProp(INITEHMMDIR) + "/mod/model101.txt");
			if (!modelFile.exists()) {
				throw new IOException("Model file " + modelFile.getAbsolutePath() + " does not exist");
			}
			pw.print("( cd " + ehmm.getAbsolutePath() + "; " + "cp " + getProp(INITEHMMDIR) + "/etc/ehmm.phoneList " + outputDir
					+ "; " + "cp " + getProp(INITEHMMDIR) + "/mod/model101.txt " + getProp(EDIR) + "/mod/ " + "; perl "
					+ getProp(EHMMDIR) + "/bin/seqproc.pl " + outputDir + "/" + "ehmm" + ".align " + outputDir + "/" + "ehmm"
					+ ".phoneList 2 2 13 >> log.txt" + "; exit )\n");
		}
		pw.flush();
		// shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}

	}

	/**
	 * Training EHMM Models
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 * @throws InterruptedException
	 *             InterruptedException
	 */
	private void baumWelchEHMM() throws IOException, InterruptedException, MaryConfigurationException {
		String task = "baumWelchEHMM";
		String line = null;

		Runtime rtime = Runtime.getRuntime();
		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));

		String cmd = "( cd " + ehmm.getAbsolutePath() + "; " + getProp(EHMMDIR) + "/bin/ehmm " + outputDir + "/" + "ehmm"
				+ ".phoneList.int " + outputDir + "/" + "ehmm" + ".align.int 1 0 " + ehmm.getAbsolutePath() + "/feat bft "
				+ ehmm.getAbsolutePath() + "/mod 0 0 0 48 " + EHMM_TRAIN_NUM_THREADS + " >> log.txt" + "; exit )\n";
		System.out.println("See $ROOTDIR/ehmm/log.txt for EHMM Labelling status... ");
		if (getProp(INITEHMMDIR).equals("/")) {
			System.out.println("EHMM baum-welch re-estimation ...");
		} else if (getProp(RETRAIN).equals("true")) {
			System.out.println("EHMM baum-welch re-estimation ... Re-Training... ");
		}
		System.out.println("It may take more time (may be 1 or 2 days) depending on voice database ...");
		System.out.println(cmd);
		pw.print(cmd);
		pw.flush();
		// shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}
		System.out.println(".... Done.");
	}

	/**
	 * Aligning EHMM and Label file generation
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws InterruptedException
	 *             InterruptedException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	private void alignEHMM() throws IOException, InterruptedException, MaryConfigurationException {
		String task = "alignEHMM";
		String line = null;
		Runtime rtime = Runtime.getRuntime();
		// get a shell
		Process process = rtime.exec("/bin/bash");
		// get an output stream to write to the shell
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
		String cmd = "( cd " + ehmm.getAbsolutePath() + "; " + getProp(EHMMDIR) + "/bin/edec " + outputDir + "/" + "ehmm"
				+ ".phoneList.int " + outputDir + "/" + "ehmm" + ".align.int 1 " + ehmm.getAbsolutePath() + "/feat bft "
				+ outputDir + "/" + "ehmm" + ".featSettings " + ehmm.getAbsolutePath() + "/mod " + getProp(NONDETENDFLAG) + " "
				+ ehmm.getAbsolutePath() + "/lab " + EHMM_ALIGN_NUM_THREADS + " >> log.txt" + "; perl " + getProp(EHMMDIR)
				+ "/bin/sym2nm.pl " + ehmm.getAbsolutePath() + "/lab " + outputDir + "/" + "ehmm" + ".phoneList.int >> log.txt"
				+ "; exit )\n";

		System.out.println(cmd);
		pw.print(cmd);

		pw.flush();
		// shut down
		pw.close();
		process.waitFor();
		// check exit value
		if (process.exitValue() != 0) {
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				System.err.println("ERR> " + line);
			}
			errorReader.close();
			throw new MaryConfigurationException(task + " computation failed.\nError: " + line);
		}

	}

	/**
	 * Create phone sequence file, which is used for Alignment
	 * 
	 * @throws Exception
	 *             Exception
	 */

	private void getPhoneSequence() throws Exception {
		String phoneSeq;
		File alignFile = new File(outputDir + "/" + "ehmm" + ".align");
		// open transcription file used for labeling
		PrintWriter transLabelOut = new PrintWriter(new FileOutputStream(alignFile));
		for (int i = 0; i < bnl.getLength(); i++) {
			phoneSeq = getLineFromXML(bnl.getName(i));
			transLabelOut.println(phoneSeq.trim());
		}
		transLabelOut.flush();
		transLabelOut.close();

		String data = FileUtils.getFileAsString(alignFile, "UTF-8");
		PrintWriter pw = new PrintWriter(new FileWriter(alignFile));
		// Check for minimum number of short pauses should be in Text
		if (countShortSil > 10) { // delete word boundaries
			data = data.replaceAll("vssil ", "");
		} else { // else insert short pauses at each word boundary
			data = data.replaceAll("vssil ", "ssil ");
			data = data.replaceAll("ssil ssil ", "ssil ");
		}
		pw.print(data);
		pw.flush();
		pw.close();
	}

	/**
	 * Get phone sequence from a single feature file
	 * 
	 * @param basename
	 *            basename
	 * @return String
	 * @throws Exception
	 *             Exception
	 */
	private String getLineFromXML(String basename) throws Exception {

		String line;
		String phoneSeq;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new File(getProp(ALLOPHONESDIR) + "/" + basename + xmlExt));
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList tokens = (NodeList) xpath.evaluate("//t | //ph | //boundary", doc, XPathConstants.NODESET);

		StringBuilder alignBuff = new StringBuilder();
		alignBuff.append(basename);
		alignBuff.append(collectTranscription(tokens));

		phoneSeq = alignBuff.toString();
		phoneSeq = phoneSeq.replaceAll("pau ssil ", "pau ");
		phoneSeq = phoneSeq.replaceAll(" ssil pau$", " pau");

		return phoneSeq;
	}

	/**
	 * 
	 * This computes a string of phonetic symbols out of an allophones xml: - standard phones are taken from "ph" attribute
	 * 
	 * @param tokens
	 *            tokens
	 * @return transcription
	 */
	private String collectTranscription(NodeList tokens) {

		// String storing the original transcription begins with a pause
		String transcription = " pau ";
		// get original phone String
		for (int i = 0; i < tokens.getLength(); i++) {
			Element token = (Element) tokens.item(i);

			if (token.getTagName().equals("ph")) {
				transcription += token.getAttribute("p") + " ";
			} else if (token.getTagName().equals("t")) {
				// if the following element is no boundary, insert a non-pause delimiter
				if (i == tokens.getLength() - 1 || !((Element) tokens.item(i + 1)).getTagName().equals("boundary")) {
					transcription += "vssil "; // word boundary
				}
			} else if (token.getTagName().equals("boundary")) {
				transcription += "ssil "; // phrase boundary
				countShortSil++;
			} else {
				// should be "t" or "boundary" elements
				assert (false);
			}
		}// ... for each t-Element
		transcription += "pau";
		return transcription;
	}

	/**
	 * Post processing Step to convert Label files to MARY supportable format
	 * 
	 * @throws Exception
	 *             Exception
	 */
	private void getProperLabelFormat() throws Exception {
		for (int i = 0; i < bnl.getLength(); i++) {
			convertSingleLabelFile(bnl.getName(i));
			System.out.println("    " + bnl.getName(i));

		}
	}

	/**
	 * Post Processing single Label file
	 * 
	 * @param basename
	 *            basename
	 * @throws Exception
	 *             Exception
	 */
	private void convertSingleLabelFile(String basename) throws Exception {

		String line;
		String previous, current;
		String regexp = "\\spau|\\sssil";

		// Compile regular expression
		Pattern pattern = Pattern.compile(regexp);

		File labDir = new File(getProp(OUTLABDIR));
		if (!labDir.exists()) {
			labDir.mkdir();
		}

		PrintWriter labelOut = new PrintWriter(new FileOutputStream(new File(labDir + "/" + basename + labExt)));

		BufferedReader labelIn = new BufferedReader(new InputStreamReader(new FileInputStream(getProp(EDIR) + "/lab/" + basename
				+ labExt)));

		previous = labelIn.readLine();

		while ((line = labelIn.readLine()) != null) {
			// Replace all occurrences of pattern in input
			Matcher matcher = pattern.matcher(line);
			current = matcher.replaceAll(" _");

			if (previous.endsWith("_") && current.endsWith("_")) {
				previous = current;
				continue;
			}
			labelOut.println(previous);
			previous = current;
		}
		labelOut.println(previous);
		labelOut.flush();
		labelOut.close();
		labelIn.close();

	}

	/**
	 * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
	 * 
	 * @return -1 if not implemented, or an integer between 0 and 100.
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		VoiceImportComponent vic = new EHMMLabeler();
		DatabaseLayout db = new DatabaseLayout(vic);
		vic.compute();
	}

}
