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
package marytts.tools.redstart;

import javax.swing.table.AbstractTableModel;

/**
 * 
 * @author Mat Wilson &lt;mat.wilson@dfki.de&gt;
 */
public class PromptTableModel extends AbstractTableModel {

	/** Creates a new instance of PromptTableModel */
	private String[] columnNames;
	protected static final int REC_STATUS_COLUMN = 0; // First column is recording status
	protected static final int BASENAME_COLUMN = 1; // Second column is basename
	protected static final int PROMPT_TEXT_COLUMN = 2; // Third collumn is prompt text
	private boolean redAlertMode = false; // if true show red alert portion of text

	final Object[][] data;

	public PromptTableModel(Prompt[] promptArray, String[] columnNames, boolean redAlertMode) {
		this.columnNames = columnNames;
		this.redAlertMode = redAlertMode;
		this.data = this.buildDataArray(promptArray);
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.length;
	}

	public String getColumnName(int column) {
		return columnNames[column];
	}

	public Object getValueAt(int row, int column) {
		return data[row][column];
	}

	public Class getColumnClass(int c) {
		return String.class;
	}

	private Object[][] buildDataArray(Prompt[] promptArray) {

		Object[][] promptMatrix = new Object[promptArray.length][columnNames.length];

		for (int row = 0; row < promptArray.length; row++) {
			promptMatrix[row][REC_STATUS_COLUMN] = ""; // update this asynchronously later, it takes time;
			promptMatrix[row][BASENAME_COLUMN] = promptArray[row].getBasename();
			if (!redAlertMode)
				promptMatrix[row][PROMPT_TEXT_COLUMN] = promptArray[row].getPromptText();
			else
				// the replace function in the line below is used only for the red alert mode
				promptMatrix[row][PROMPT_TEXT_COLUMN] = promptArray[row].getPromptText().replace("_", "");
		}
		return promptMatrix;
	}

	public void setValueAt(Object obj, int row, int col) {
		data[row][col] = obj;
		fireTableCellUpdated(row, col);
	}

}
