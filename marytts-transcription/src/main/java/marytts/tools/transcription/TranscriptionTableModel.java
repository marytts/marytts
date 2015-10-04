/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.tools.transcription;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import marytts.fst.AlignerTrainer;
import marytts.fst.FSTLookup;
import marytts.fst.TransducerTrie;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.MaryUtils;
import marytts.util.io.FileUtils;

/**
 * TranscriptionTableModel, an AbstractTableModel, stores transcription data
 * 
 * @author sathish pammi
 *
 */
public class TranscriptionTableModel extends AbstractTableModel {

	private String[] columnNames = { "No.", "Word", "Transcription", "Functional" };
	private Object[][] data;
	private Object[][] lastSavedData; // Data at the time of loading
	private boolean[] hasManualVerification;
	private boolean[] hasCorrectSyntax;
	private int editableColumns = 2;

	public TranscriptionTableModel() {

		this.data = new Object[20][4];
		this.hasManualVerification = new boolean[20];
		this.hasCorrectSyntax = new boolean[20];
		for (int i = 0; i < 20; i++) {
			data[i][0] = "";
			data[i][1] = "";
			data[i][2] = "";
			data[i][3] = Boolean.FALSE;
			setAsManualVerify(i, false);
			setAsCorrectSyntax(i, true);
		}
		lastSavedData = storeLastSavedData();
	}

	public Object getDataAt(int x, int y) {
		return this.data[x][y];
	}

	public TranscriptionTableModel(String fileName) throws Exception {

		String fileData = FileUtils.getFileAsString(new File(fileName), "UTF-8");
		String[] words = fileData.split("\n");
		this.data = new Object[words.length][4];
		this.hasManualVerification = new boolean[words.length];
		this.hasCorrectSyntax = new boolean[words.length];
		for (int i = 0; i < words.length; i++) {
			data[i][0] = Integer.toString(i);
			data[i][1] = words[i];
			data[i][2] = "";
			data[i][3] = Boolean.FALSE;
			setAsManualVerify(i, false);
			setAsCorrectSyntax(i, true);
		}
		lastSavedData = storeLastSavedData();
	}

	public Object[][] getData() {
		return this.data;
	}

	public void setAsManualVerify(int x, boolean t) {
		this.hasManualVerification[x] = t;
	}

	public void setAsCorrectSyntax(int x, boolean t) {
		this.hasCorrectSyntax[x] = t;
	}

	public boolean[] getManualVerifiedList() {
		return this.hasManualVerification;
	}

	public boolean[] getCorrectSyntaxList() {
		return this.hasCorrectSyntax;
	}

	/**
	 * Save transcription to a file
	 * 
	 * @param fileName
	 *            fileName
	 * @throws Exception
	 *             Exception
	 */
	public void saveTranscription(String fileName) throws Exception {
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
		// Save copyright notice first
		MaryUtils.writeCopyrightNotice(out, "#");
		// If any transcriptions include spaces, save using | as separator, else use space
		boolean transContainsSpace = false;
		for (int i = 0; i < data.length; i++) {
			String trans = (String) data[i][2];
			if (trans.contains(" ")) {
				transContainsSpace = true;
				break;
			}
		}
		String separatorChar = transContainsSpace ? "|" : " ";

		for (int i = 0; i < data.length; i++) {
			String word = (String) data[i][1];
			String trans = (String) data[i][2];
			boolean isFunctional = (Boolean) data[i][3];
			StringBuilder line = new StringBuilder();
			line.append(word);
			if (!trans.equals("") && this.hasManualVerification[i] && this.hasCorrectSyntax[i]) {
				line.append(separatorChar).append(trans);
			}
			if (isFunctional) {
				line.append(separatorChar).append("functional");
			}
			out.println(line);
		}
		out.flush();
		out.close();

		// Saved the data - so, stored data have to modify
		lastSavedData = storeLastSavedData();

	}

	/**
	 * Load transcription from file, either replacing or adding to any existing data. If adding, only words not contained in the
	 * existing data will be added.
	 * 
	 * @param fileName
	 *            fileName
	 * @param keepCurrentData
	 *            whether to keep any current data and add, or use only the loaded data.
	 * @throws Exception
	 *             Exception
	 */
	public void loadTranscription(String fileName, boolean keepCurrentData) throws Exception {
		List<String> lines = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.equals("") || line.startsWith("#"))
				continue;
			lines.add(line);
		}
		if (lines.size() == 0) {
			return;
		}

		int offset;
		Set<String> currentWords = new HashSet<String>();
		if (keepCurrentData && this.data != null) {
			Object[][] oldData = this.data;
			boolean[] oldHasManualVerification = this.hasManualVerification;
			boolean[] oldHasCorrectSyntax = this.hasCorrectSyntax;
			this.data = new Object[oldData.length + lines.size()][4];
			for (int i = 0; i < oldData.length; i++) {
				System.arraycopy(oldData[i], 0, this.data[i], 0, 4);
				currentWords.add((String) oldData[i][1]);
			}
			this.hasManualVerification = new boolean[this.data.length];
			System.arraycopy(oldHasManualVerification, 0, this.hasManualVerification, 0, oldData.length);
			this.hasCorrectSyntax = new boolean[this.data.length];
			System.arraycopy(oldHasCorrectSyntax, 0, this.hasCorrectSyntax, 0, oldData.length);
			offset = oldData.length;
		} else {
			this.data = new Object[lines.size()][4];
			this.hasManualVerification = new boolean[data.length];
			this.hasCorrectSyntax = new boolean[data.length];
			offset = 0;
		}

		int pos = offset;
		// Accept input in two types of format:
		// 1. space-separated (in which case the transcription is not supposed to contain any space characters);
		// 2. fields separated by a "pipe" symbol ("|"), as in userdict.
		String separatorRE;
		if (lines.get(0).contains("|")) {
			separatorRE = "\\|";
		} else {
			separatorRE = "\\s+";
		}
		for (int i = 0; i < lines.size(); i++) {
			String[] words = lines.get(i).split(separatorRE);
			String word = words[0].trim();
			if (currentWords.contains(word))
				continue; // do not add new words already contained in old list
			data[pos][0] = Integer.toString(pos);
			data[pos][1] = words[0].trim();
			if (lines.get(i).endsWith("functional")) {
				data[pos][3] = Boolean.TRUE;
				if (words.length == 3) {
					data[pos][2] = words[1].trim();
					setAsManualVerify(pos, true);
					setAsCorrectSyntax(pos, true);
				} else {
					data[pos][2] = "";
					setAsManualVerify(pos, false);
				}
			} else {
				data[pos][3] = Boolean.FALSE;
				if (words.length >= 2) {
					data[pos][2] = words[1].trim();
					setAsManualVerify(pos, true);
					setAsCorrectSyntax(pos, true);
				} else {
					data[pos][2] = "";
					setAsManualVerify(pos, false);
				}
			}
			pos++;
		}
		lastSavedData = storeLastSavedData();
	}

	/**
	 * Load transcription from HashMap
	 * 
	 * @param wordList
	 *            wordList
	 * @throws Exception
	 *             Exception
	 */
	@Deprecated
	// doesn't seem to get used -- remove?
	public void loadTranscription(HashMap<String, Integer> wordList) throws Exception {

		int length = wordList.size();
		this.data = new Object[length][4];
		this.hasManualVerification = new boolean[length];
		this.hasCorrectSyntax = new boolean[length];
		Iterator<String> it = wordList.keySet().iterator();
		for (int i = 0; it.hasNext(); i++) {
			data[i][0] = Integer.toString(i);
			data[i][1] = (String) it.next(); // wordList.get(i);
			data[i][2] = "";
			data[i][3] = Boolean.FALSE;
			setAsManualVerify(i, false);
			setAsCorrectSyntax(i, true);

		}
		lastSavedData = storeLastSavedData();
	}

	public void loadTranscription(ArrayList<String> wordList) {

		int length = wordList.size();
		this.data = new Object[length][4];
		this.hasManualVerification = new boolean[length];
		this.hasCorrectSyntax = new boolean[length];
		Iterator<String> it = wordList.iterator();
		for (int i = 0; it.hasNext(); i++) {
			data[i][0] = Integer.toString(i);
			data[i][1] = (String) it.next(); // wordList.get(i);
			data[i][2] = "";
			data[i][3] = Boolean.FALSE;
			setAsManualVerify(i, false);
			setAsCorrectSyntax(i, true);
		}
		lastSavedData = storeLastSavedData();
	}

	/**
	 * Save user entered and verified transcription in to lexicon format
	 * 
	 * @param fileName
	 *            fileName
	 * @throws IOException
	 *             IOException
	 */
	public void saveSampaLexiconFormat(String fileName) throws IOException {
		if (!hasLexiconData())
			return;
		PrintWriter out = new PrintWriter(new FileWriter(fileName));
		for (int i = 0; i < data.length; i++) {

			String line;
			if (!((String) data[i][2]).equals("") && this.hasManualVerification[i] && this.hasCorrectSyntax[i]) {
				line = (String) data[i][1];
				// line += "\\"+(String)data[i][2]+"\\";
				line += "|" + (String) data[i][2];
				out.println(line);
			}
		}
		out.flush();
		out.close();

		// Saved the data - so, stored data have to modify
		lastSavedData = storeLastSavedData();

	}

	/**
	 * Save user entered and verified transcription in to lexicon format
	 * 
	 * @param fileName
	 *            fileName
	 * @param phoneSet
	 *            phoneSet
	 * @throws IOException
	 *             IOException
	 */
	public void saveSampaLexiconFormat(String fileName, AllophoneSet phoneSet) throws IOException {
		if (!hasLexiconData())
			return;
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
		for (int i = 0; i < data.length; i++) {
			String line;
			if (!((String) data[i][2]).equals("") && this.hasManualVerification[i] && this.hasCorrectSyntax[i]) {
				line = (String) data[i][1];
				String grapheme = phoneSet.splitAllophoneString((String) data[i][2]);
				line += "|" + grapheme;
				out.println(line);
			}
		}
		out.flush();
		out.close();

		// Saved the data - so, stored data have to modify
		lastSavedData = storeLastSavedData();

	}

	/**
	 * Save all functional words into text file
	 * 
	 * @param fileName
	 *            fileName
	 * @throws IOException
	 *             IOException
	 */
	public void saveFunctionalWords(String fileName) throws IOException {
		if (!hasFunctionalData())
			return;
		PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
		for (int i = 0; i < data.length; i++) {
			if ((Boolean) data[i][3]) {
				out.println((String) data[i][1] + "|functional");
			}
		}
		out.flush();
		out.close();

		// Saved the data - so, stored data have to modify
		lastSavedData = storeLastSavedData();

	}

	public boolean hasFunctionalData() {
		int countData = 0;
		for (int i = 0; i < data.length; i++) {
			if ((Boolean) data[i][3]) {
				countData++;
			}
		}
		return countData != 0;
	}

	public boolean hasLexiconData() {
		int countData = 0;
		for (int i = 0; i < data.length; i++) {
			if (!((String) data[i][2]).equals("") && this.hasManualVerification[i] && this.hasCorrectSyntax[i]) {
				countData++;
			}
		}
		return countData != 0;
	}

	/**
	 * For all words in lexicon, verify if they can be looked up in fst file.
	 * 
	 * @param lexiconFilename
	 *            lexiconFilename
	 * @param fstFilename
	 *            fstFilename
	 * @throws IOException
	 *             IOException
	 */
	public void testFST(String lexiconFilename, String fstFilename) throws IOException {
		System.err.println("Testing FST...");
		FSTLookup fst = new FSTLookup(fstFilename);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), "UTF-8"));
		String line;
		int nCorrect = 0;
		int nFailed = 0;
		while ((line = br.readLine()) != null) {
			String[] parts = line.split("\\s*\\|\\s*");
			String key = parts[0];
			String value = parts[1];
			String[] lookupResult = fst.lookup(key);
			assert lookupResult != null;
			if (lookupResult.length == 1 && value.equals(lookupResult[0])) {
				nCorrect++;
			} else {
				nFailed++;
				System.err.print("Problem looking up key '" + key + "': Expected value '" + value + "', but got ");
				if (lookupResult.length == 0) {
					System.err.println("no result");
				} else if (lookupResult.length == 1) {
					System.err.println("result '" + lookupResult[0] + "'");
				} else {
					System.err.print(+lookupResult.length + " results:");
					for (String res : lookupResult) {
						System.err.print(" '" + res + "'");
					}
					System.err.println();
				}
			}
		}
		br.close();
		System.err.println("Testing complete. " + (nCorrect + nFailed) + " entries (" + nCorrect + " correct, " + nFailed
				+ " failed)");
	}

	public void createPOSFst(String posFilename, String fstFilename) throws Exception {

		if (!hasFunctionalData())
			return;

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(posFilename), "UTF-8"));
		AlignerTrainer at = new AlignerTrainer(false, true);
		at.readLexicon(br, "\\s*\\|\\s*");
		br.close();

		// make some alignment iterations
		for (int i = 0; i < 4; i++) {
			at.alignIteration();
		}
		TransducerTrie t = new TransducerTrie();
		for (int i = 0, size = at.lexiconSize(); i < size; i++) {
			t.add(at.getAlignment(i));
			t.add(at.getInfoAlignment(i));
		}
		t.computeMinimization();
		File of = new File(fstFilename);
		DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
		t.writeFST(os, "UTF-8");
		os.flush();
		os.close();
		// testFST(fstFilename);
	}

	/**
	 * Creates lexicon in FST format and letter-to-sound models
	 * 
	 * @param lexiconFilename
	 *            lexiconFilename
	 * @param fstFilename
	 *            fstFilename
	 * @throws Exception
	 *             Exception
	 */
	public void createLexicon(String lexiconFilename, String fstFilename) throws Exception {

		if (!hasLexiconData())
			return;

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), "UTF-8"));
		AlignerTrainer at = new AlignerTrainer(false, true);
		at.readLexicon(br, "\\s*\\|\\s*");
		br.close();

		// make some alignment iterations
		for (int i = 0; i < 4; i++) {
			at.alignIteration();
		}
		TransducerTrie t = new TransducerTrie();
		for (int i = 0, size = at.lexiconSize(); i < size; i++) {
			t.add(at.getAlignment(i));
			t.add(at.getInfoAlignment(i));
		}
		t.computeMinimization();
		File of = new File(fstFilename);
		DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
		t.writeFST(os, "UTF-8");
		os.flush();
		os.close();
		testFST(lexiconFilename, fstFilename);
	}

	/**
	 * get column count
	 * 
	 * @return columnNames.length
	 */
	public int getColumnCount() {
		return columnNames.length;
	}

	/**
	 * get row count
	 * 
	 * @return data.length
	 */
	public int getRowCount() {
		return data.length;
	}

	/**
	 * get column name
	 * 
	 * @param col
	 *            col
	 * @return columnNames[col]
	 */
	public String getColumnName(int col) {
		return columnNames[col];
	}

	/**
	 * get value at given location
	 * 
	 * @param row
	 *            row
	 * @param col
	 *            col
	 * @return data[row][col]
	 */
	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	/*
	 * JTable uses this method to determine the default renderer/ editor for each cell. If we didn't implement this method, then
	 * the last column would contain text ("true"/"false"), rather than a check box.
	 */
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	/*
	 * Don't need to implement this method unless your table's editable.
	 */
	public boolean isCellEditable(int row, int col) {
		// Note that the data/cell address is constant,
		// no matter where the cell appears onscreen.
		return col >= editableColumns;
	}

	/*
	 * Don't need to implement this method unless your table's data can change.
	 */
	public void setValueAt(Object value, int row, int col) {
		data[row][col] = value;
		fireTableCellUpdated(row, col);
	}

	public boolean isDataModified() {
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				Object k1 = data[i][j];
				Object k2 = lastSavedData[i][j];
				if (!data[i][j].equals(lastSavedData[i][j])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Store last saved data
	 * 
	 * @return newData
	 */
	private Object[][] storeLastSavedData() {
		Object[][] newData = new Object[data.length][data[0].length];
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				newData[i][j] = data[i][j];
			}
		}
		return newData;
	}

}
