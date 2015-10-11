package marytts.tools.transcription;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import marytts.cart.CART;
import marytts.cart.StringPredictionTree;
import marytts.exceptions.MaryConfigurationException;
import marytts.features.FeatureDefinition;
import marytts.fst.AlignerTrainer;
import marytts.fst.StringPair;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.TrainedLTS;
import marytts.tools.dbselection.DBHandler;
import marytts.tools.newlanguage.LTSTrainer;
import marytts.tools.newlanguage.en_US.CMUDict2MaryFST;
import marytts.tools.transcription.TranscriptionTableModel;
import marytts.util.io.FileUtils;

/**
 * 
 * LTSLexiconPOSBuilder has the same functionalities of TRANSCRIPTION TOOL but without GUI (better for a remote use for large
 * lexicon and for scripting). This class is a light version of TranscriptionTable
 * 
 * Train Letter-to-sound(LTS) rules, create FST dictionary and POS tagger
 * 
 * @author Fabio Tesser &lt;fabio.tesser@gmail.com&gt;
 * 
 *         Example use:
 *         <code>java -showversion -ea -Xmx4096m  -cp "$MARY_BASE/lib/*" marytts.tools.transcription.LTSLexiconPOSBuilder ./marytts-lang-it/src/main/resources/marytts/language/it/lexicon/allophones.it.xml ./marytts-lang-it/lib/modules/it/lexicon/it.txt </code>
 * 
 * 
 */

public class LTSLexiconPOSBuilder {
	TranscriptionTableModel transcriptionModel;
	protected boolean removeTrailingOneFromPhones = true;

	private AllophoneSet phoneSet;

	String locale;

	public LTSLexiconPOSBuilder(String phoneSetFileName, String transcriptionFileName, boolean removeTrailingOneFromPhones)
			throws Exception {
		loadPhoneSet(phoneSetFileName);
		transcriptionModel = new TranscriptionTableModel();
		loadTranscription(transcriptionFileName);
		this.removeTrailingOneFromPhones = removeTrailingOneFromPhones;
	}

	public LTSLexiconPOSBuilder(String phoneSetFileName, String transcriptionFileName) throws Exception {
		this(phoneSetFileName, transcriptionFileName, true);
	}

	/**
	 * load phoneset
	 * 
	 * @param filePath
	 *            filePath
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	private void loadPhoneSet(String filePath) throws MaryConfigurationException {
		phoneSet = AllophoneSet.getAllophoneSet(filePath);
		locale = phoneSet.getLocale().toString();
	}

	/**
	 * Load transcription from file
	 * 
	 * @param fileName
	 *            fileName
	 * @throws Exception
	 *             Exception
	 */
	private void loadTranscription(String fileName) throws Exception {
		transcriptionModel.loadTranscription(fileName, false);
		checkTranscription();
	}

	private void checkTranscription() {
		int size = transcriptionModel.getData().length;
		for (int row = 0; row < size; row++) {
			String transcription = (String) transcriptionModel.getDataAt(row, 2);
			if (transcription == null)
				continue;
			if (transcription.matches("\\s+")) {
				transcription = transcription.replaceAll("\\s+", "");
				this.transcriptionModel.setValueAt(transcription, row, 2);
			}
			if (!transcription.equals("")) {
				boolean ok = phoneSet.checkAllophoneSyntax(transcription);
				transcriptionModel.setAsCorrectSyntax(row, ok);
			} else {
				transcriptionModel.setAsCorrectSyntax(row, false);
			}
		}
	}

	/**
	 * train and predict module
	 * 
	 * @param treeAbsolutePath
	 *            treeAbsolutePath
	 * @throws IOException
	 *             IOException
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void trainPredict(String treeAbsolutePath) throws IOException, MaryConfigurationException {
		Object[][] tableData = transcriptionModel.getData();
		boolean[] hasManualVerify = transcriptionModel.getManualVerifiedList();
		boolean[] hasCorrectSyntax = transcriptionModel.getCorrectSyntaxList();

		// Check for number of manual entries available
		int numberOfManualEntries = 0;
		for (int i = 0; i < hasManualVerify.length; i++) {
			if (hasManualVerify[i])
				numberOfManualEntries++;
		}
		if (numberOfManualEntries == 0) {
			System.out.println("No manual entries available for train and predict ... do nothing!");
			return;
		}

		trainLTS(treeAbsolutePath);
		FileInputStream fis = new FileInputStream(treeAbsolutePath);
		TrainedLTS trainedLTS = new TrainedLTS(phoneSet, fis, this.removeTrailingOneFromPhones);
		for (int i = 0; i < tableData.length; i++) {
			if (!(hasManualVerify[i] && hasCorrectSyntax[i])) {
				String grapheme = (String) tableData[i][1];
				if (grapheme == null)
					continue;
				String phone = trainedLTS.syllabify(trainedLTS.predictPronunciation(grapheme));
				transcriptionModel.setValueAt(phone.replaceAll("\\s+", ""), i, 2);
				transcriptionModel.setAsCorrectSyntax(i, true);
				transcriptionModel.setAsManualVerify(i, false);
			}
		}

	}

	private LTSTrainer trainLTS(String treeAbsolutePath) throws IOException {

		Object[][] tableData = transcriptionModel.getData();
		HashMap<String, String> map = new HashMap<String, String>();
		boolean[] hasManualVerify = transcriptionModel.getManualVerifiedList();
		boolean[] hasCorrectSyntax = transcriptionModel.getCorrectSyntaxList();

		for (int i = 0; i < tableData.length; i++) {
			if (hasManualVerify[i] && hasCorrectSyntax[i]) {
				String grapheme = (String) tableData[i][1];
				String phone = (String) tableData[i][2];
				if (!phone.equals("")) {
					map.put(grapheme, phone);
					transcriptionModel.setAsCorrectSyntax(i, true);
				}
			}
		}

		LTSTrainer tp = new LTSTrainer(phoneSet, true, true, 2);
		tp.readLexicon(map);
		System.out.println("training ... ");
		// make some alignment iterations
		for (int i = 0; i < 5; i++) {
			System.out.println("iteration " + i);
			tp.alignIteration();
		}
		System.out.println("training completed.");
		CART st = tp.trainTree(100);
		tp.save(st, treeAbsolutePath);
		return tp;
	}

	private String getLocaleString() {
		return locale;
	}

	/**
	 * save transcription into file
	 * 
	 * @param fileName
	 *            fileName
	 * @throws Exception
	 *             Exception
	 */
	public void saveTranscription(String fileName) throws Exception {

		this.transcriptionModel.saveTranscription(fileName);
		// File parentDir = (new File(fileName)).getParentFile();
		// String parentPath = parentDir.getAbsolutePath();
		File saveFile = new File(fileName);
		String dirName = saveFile.getParentFile().getAbsolutePath();
		String filename = saveFile.getName();
		String baseName, suffix;
		if (filename.lastIndexOf(".") == -1) {
			baseName = filename;
		} else {
			baseName = filename.substring(0, filename.lastIndexOf("."));
		}
		String lexiconFile = dirName + File.separator + baseName + "_lexicon.dict";
		String fstFile = dirName + File.separator + baseName + "_lexicon.fst";
		String posFile = dirName + File.separator + baseName + "_pos.list";
		String posFst = dirName + File.separator + baseName + "_pos.fst";

		transcriptionModel.saveSampaLexiconFormat(lexiconFile, phoneSet);
		transcriptionModel.createLexicon(lexiconFile, fstFile);
		transcriptionModel.saveFunctionalWords(posFile);
		transcriptionModel.createPOSFst(posFile, posFst);
		// trainLTS(treeAbsolutePath);

	}

	/**
	 * @param args
	 *            first argument, PhoneSet file name; second argument, Transcriptions file name
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		try {
			// Get filenames from args
			String phoneSetFileName = args[0];
			String transcriptionsFileName = args[1];
			boolean myRemoveTrailingOneFromPhones = true;
			if (args.length > 2) {
				myRemoveTrailingOneFromPhones = Boolean.getBoolean(args[2]);
			}

			// Create object from files
			LTSLexiconPOSBuilder myLTSLexiconPOSBuilder = new LTSLexiconPOSBuilder(phoneSetFileName, transcriptionsFileName,
					myRemoveTrailingOneFromPhones);

			// Splitting of dirName baseName suffix
			String dirName = null;
			String baseName = null;
			String suffix = null;
			File transcriptionFile = new File(transcriptionsFileName);
			dirName = transcriptionFile.getParentFile().getAbsolutePath();
			String filename = transcriptionFile.getName();
			if (filename.lastIndexOf(".") == -1) {
				baseName = filename;
			} else {
				baseName = filename.substring(0, filename.lastIndexOf("."));
			}

			String treeAbsolutePath = dirName + File.separator + baseName + ".lts";
			String lexout = dirName + File.separator + baseName + ".lextxt";

			// Procedure
			System.out.println("trainPredict ...");
			myLTSLexiconPOSBuilder.trainPredict(treeAbsolutePath);
			System.out.println("... done.");

			System.out.println("saveTranscription ...");
			myLTSLexiconPOSBuilder.saveTranscription(lexout);
			System.out.println("... done.");

		} catch (Exception e) {
			System.out.println("Problem processing the following input:");
			System.out.println("args[0]:" + args[0]);
			System.out.println("args[1]:" + args[1]);
			System.out.println("\n");
			e.printStackTrace();
			System.out.println("");
			System.out.println("Failure exit");
			System.exit(-1);
		}

	}
}