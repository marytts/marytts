/**
 * Copyright 2007 DFKI GmbH.
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
package de.dfki.lt.mary.recsessionmgr.gui;

import javax.swing.table.AbstractTableModel;

import de.dfki.lt.mary.recsessionmgr.lib.Prompt;

/**
 *
 * @author Mat Wilson <mat.wilson@dfki.de>
 */
public class PromptTableModel extends AbstractTableModel {
    
    /** Creates a new instance of PromptTableModel */
    private String[] columnNames;
    protected static final int REC_STATUS_COLUMN = 0;     // First column is recording status 
    protected static final int BASENAME_COLUMN = 1;       // Second column is basename
    protected static final int PROMPT_TEXT_COLUMN = 2;    // Third collumn is prompt text       
    
    final Object[][] data;
    
    public PromptTableModel(Prompt[] promptArray, String[] columnNames) {
        this.columnNames = columnNames;
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
            promptMatrix[row][PROMPT_TEXT_COLUMN] = promptArray[row].getPromptText();
        }
        return promptMatrix;
    }
    
    
    public void setValueAt(Object obj, int row, int col)
    {
        data[row][col] = obj;
        fireTableCellUpdated(row, col);
    }
    
}
