/*
 * PromptTableModel.java
 *
 * Created on June 27, 2007, 4:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
