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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.CellEditor;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import marytts.cart.CART;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.TrainedLTS;
import marytts.tools.newlanguage.LTSTrainer;

/**
 * 
 * TranscriptionTable, A Table panel, tracs events in user transcription entries
 * 
 * @author sathish pammi
 *
 */
public class TranscriptionTable extends JPanel implements ActionListener {
	private JTable table;
	int itsRow = 0;
	int rowEnterPressed = -1;
	TranscriptionTableModel transcriptionModel;
	private AllophoneSet phoneSet;
	private int editableColumns = 2;
	int previousRow = 0;
	boolean trainPredict = false;
	protected boolean removeTrailingOneFromPhones = true;
	JScrollPane scrollpane;
	String locale;
	CellEditorListener editorListener = null;

	public TranscriptionTable() throws Exception {
		super();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		transcriptionModel = new TranscriptionTableModel();

		table = new JTable(transcriptionModel);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
		// table.setFillsViewportHeight(true);
		table.getSelectionModel().addListSelectionListener(new RowListener());
		table.getColumnModel().getColumn(0).setCellRenderer(new CustomTableCellRenderer());
		table.getColumnModel().getColumn(2).setCellRenderer(new CustomTableCellRenderer());
		table.addKeyListener(new KeyEventListener());
		table.setFont(new Font("Serif", Font.TRUETYPE_FONT, 12));
		scrollpane = new JScrollPane(table);
		add(scrollpane);
		TableColumn column = table.getColumnModel().getColumn(1);
		int columnSize = column.getPreferredWidth();
		column.setPreferredWidth(2 * columnSize);
		table.getColumnModel().getColumn(2).setPreferredWidth(2 * columnSize);

		table.setSurrendersFocusOnKeystroke(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	}

	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();
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
	 * verify transcription syntax
	 * 
	 * @param row
	 *            row
	 */
	private void checkTranscriptionSyntax(int row) {

		String transcription = (String) transcriptionModel.getDataAt(row, 2);
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

	public boolean isDataModified() {
		return this.transcriptionModel.isDataModified();
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
		System.out.println("alignment ... ");
		// make some alignment iterations
		for (int i = 0; i < 5; i++) {
			System.out.println("iteration " + i);
			tp.alignIteration();
		}
		System.out.println("alignment completed.");
		System.out.println("training ... ");
		CART st = tp.trainTree(100);
		tp.save(st, treeAbsolutePath);
		System.out.println("training completed.");
		return tp;
	}

	/**
	 * train and predict module
	 * 
	 * @param treeAbsolutePath
	 *            treeAbsolutePath
	 * @param myRemoveTrailingOneFromPhones
	 *            myRemoveTrailingOneFromPhones
	 * @throws MaryConfigurationException
	 *             MaryConfigurationException
	 */
	public void trainPredict(String treeAbsolutePath, boolean myRemoveTrailingOneFromPhones) throws MaryConfigurationException {
		Object[][] tableData = transcriptionModel.getData();
		boolean[] hasManualVerify = transcriptionModel.getManualVerifiedList();
		boolean[] hasCorrectSyntax = transcriptionModel.getCorrectSyntaxList();
		this.removeTrailingOneFromPhones = myRemoveTrailingOneFromPhones;

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

		try {
			LTSTrainer tp = this.trainLTS(treeAbsolutePath);
			FileInputStream fis = new FileInputStream(treeAbsolutePath);
			TrainedLTS trainedLTS = new TrainedLTS(phoneSet, fis, this.removeTrailingOneFromPhones);
			fis.close();
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
			if (((String) transcriptionModel.getDataAt(itsRow, 2)).equals("")) {
				String grapheme = (String) tableData[itsRow][1];
				String phone = trainedLTS.syllabify(trainedLTS.predictPronunciation(grapheme));
				transcriptionModel.setValueAt(phone.replaceAll("\\s+", ""), itsRow, 2);
				transcriptionModel.setAsCorrectSyntax(itsRow, true);
				transcriptionModel.setAsManualVerify(itsRow, false);
			}
		} catch (IOException e) {
			throw new MaryConfigurationException("Problem training/predicting", e);
		}
		trainPredict = true;
	}

	/**
	 * save transcription into file
	 * 
	 * @param fileName
	 *            fileName
	 */
	public void saveTranscription(String fileName) {
		try {
			this.transcriptionModel.saveTranscription(fileName);
			// File parentDir = (new File(fileName)).getParentFile();
			// String parentPath = parentDir.getAbsolutePath();
			File saveFile = new File(fileName);
			String dirName = saveFile.getParentFile().getAbsolutePath();
			String filename = saveFile.getName();
			String baseName, suffix;
			if (filename.lastIndexOf(".") == -1) {
				baseName = filename;
				suffix = "";
			} else {
				baseName = filename.substring(0, filename.lastIndexOf("."));
				suffix = filename.substring(filename.lastIndexOf("."), filename.length());
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Load transcription from file
	 * 
	 * @param fileName
	 *            fileName
	 */
	public void loadTranscription(String fileName) {
		try {
			this.transcriptionModel.loadTranscription(fileName, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		checkTranscription();
		scrollpane.updateUI();
		table.repaint();
		this.repaint();
		this.updateUI();
	}

	/**
	 * Add words from file
	 * 
	 * @param fileName
	 *            fileName
	 */
	public void addWordsToTranscription(String fileName) {
		try {
			this.transcriptionModel.loadTranscription(fileName, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		checkTranscription();
		scrollpane.updateUI();
		table.repaint();
		this.repaint();
		this.updateUI();
	}

	/**
	 * Load transcription from a hashmap
	 * 
	 * @param map
	 *            map
	 * @throws Exception
	 *             Exception
	 */
	@Deprecated
	// doesn't seem to get used -- remove?
	public void loadTranscription(HashMap<String, Integer> map) throws Exception {
		this.transcriptionModel.loadTranscription(map);
		checkTranscription();
		scrollpane.updateUI();
		table.repaint();
		this.repaint();
		this.updateUI();
	}

	/**
	 * Load transcription from a arrayList
	 * 
	 * @param arrList
	 *            arrList
	 * @throws Exception
	 *             Exception
	 */
	public void loadTranscription(ArrayList<String> arrList) throws Exception {
		this.transcriptionModel.loadTranscription(arrList);
		checkTranscription();
		scrollpane.updateUI();
		table.repaint();
		this.repaint();
		this.updateUI();
	}

	/**
	 * load phoneset
	 * 
	 * @param filePath
	 *            filePath
	 */
	public void loadPhoneSet(String filePath) {
		try {
			phoneSet = AllophoneSet.getAllophoneSet(filePath);
			locale = phoneSet.getLocale().toString();
		} catch (MaryConfigurationException e) {
			e.printStackTrace();
		}
	}

	public String getLocaleString() {
		return locale;
	}

	/**
	 * Row event listener
	 * 
	 * @author sathish
	 *
	 */
	private class RowListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			if (event.getValueIsAdjusting()) {
				return;
			}
			itsRow = table.getSelectionModel().getLeadSelectionIndex();
			table.repaint();
			checkTranscription();
		}
	}

	/**
	 * Column event listener
	 * 
	 * @author sathish
	 *
	 */
	private class ColumnListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent event) {
			if (event.getValueIsAdjusting()) {
				return;
			}
			itsRow = table.getSelectionModel().getLeadSelectionIndex();
			table.repaint();
		}
	}

	/**
	 * Key event listener
	 * 
	 * @author sathish
	 *
	 */
	private class KeyEventListener implements KeyListener {

		public void keyPressed(KeyEvent arg0) {
			if (arg0.getKeyCode() == 10) { // enter key
				rowEnterPressed = itsRow;
			}
		}

		public void keyReleased(KeyEvent arg0) {
			System.out.println("Received key release event, key nr. " + arg0.getKeyCode());
			System.out.println("Key released in row " + itsRow);
			if (arg0.getKeyCode() == 10) { // enter key
				if (rowEnterPressed != -1) {
					// enter was pressed in a row in non-editing mode; edit this row
					System.out.println("RowEnterPressed = " + rowEnterPressed);
					table.getSelectionModel().setLeadSelectionIndex(rowEnterPressed);
					table.editCellAt(rowEnterPressed, 2);
				} else {
					// coming out of edit, start editing next line
					System.out.println("row nr. " + itsRow);
					// table.changeSelection(itsRow, 2, false, false);
					int nextRow = itsRow + 1;
					table.getSelectionModel().setLeadSelectionIndex(nextRow);
					table.editCellAt(nextRow, 2);
				}
				if (editorListener == null) {
					CellEditor editor = table.getCellEditor();
					editorListener = new CellEditListener();
					editor.addCellEditorListener(editorListener);
				}
			}
			int[] selectedRows = table.getSelectedRows();
			if (arg0.getKeyCode() == 32) {
				for (int i = 0; i < selectedRows.length; i++) {
					if ((Boolean) transcriptionModel.getValueAt(selectedRows[i], 3)) {
						transcriptionModel.setValueAt((Object) (false), selectedRows[i], 3);
					} else {
						transcriptionModel.setValueAt((Object) (true), selectedRows[i], 3);
					}
				}
			}
		}

		public void keyTyped(KeyEvent arg0) {
		}
	}

	private class CellEditListener implements CellEditorListener {
		public void editingCanceled(ChangeEvent e) {
			System.out.println("cancelled");
			rowEnterPressed = -1;
		}

		public void editingStopped(ChangeEvent e) {
			System.out.println("stopped: " + itsRow);
			checkTranscriptionSyntax(itsRow);
			transcriptionModel.setAsManualVerify(itsRow, true);
			rowEnterPressed = -1;
		}
	}

	/**
	 * Color rendering class
	 * 
	 * @author sathish
	 *
	 */
	public class CustomTableCellRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable ttable, Object obj, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Component cell = super.getTableCellRendererComponent(ttable, obj, isSelected, hasFocus, row, column);

			boolean[] hasManualVerify = transcriptionModel.getManualVerifiedList();
			boolean[] hasCorrectSyntax = transcriptionModel.getCorrectSyntaxList();
			if (column == 0) {
				cell.setFont(new Font("Serif", Font.TRUETYPE_FONT, 12));
			}

			if (column == 2) {
				cell.setFont(new Font("Serif", Font.BOLD, 12));
				if (!hasCorrectSyntax[row]) {
					cell.setForeground(Color.RED);
					transcriptionModel.setAsManualVerify(row, false);
				} else if (!hasManualVerify[row]) {
					cell.setForeground(Color.LIGHT_GRAY);
				} else {
					cell.setForeground(Color.BLACK);
				}
			} else {
				cell.setForeground(Color.BLACK);
			}
			return cell;
		}
	}

	public void changeTableFont(String fontName) {

		int fontSize = table.getFont().getSize();
		System.out.println("prev: " + table.getFont().getName());
		table.setFont(new Font(fontName, Font.TRUETYPE_FONT, fontSize));
		System.out.println("next: " + fontName);
	}

}
