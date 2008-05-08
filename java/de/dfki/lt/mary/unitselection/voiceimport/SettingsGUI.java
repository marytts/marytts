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
package de.dfki.lt.mary.unitselection.voiceimport;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import java.io.File;
import java.util.*;


public class SettingsGUI {
   
    private final JFrame frame = new JFrame("Settings Editor");
    private DatabaseLayout db;
    private String[][] tableProps;
    private PropTableModel tableModel;
    private JTable table;
    private String[] compNames;
    private JScrollPane scrollPane;
    private JComboBox componentsComboBox;
    private final Map comps2HelpText;
    private String simpleModeHelpText;
    private String guiText; 
    private boolean wasSaved;
    
    public SettingsGUI(DatabaseLayout db, SortedMap props,String simpleModeHelpText,String guiText)
    {
        this.db = db;
        this.simpleModeHelpText = simpleModeHelpText;
        this.guiText = guiText;
        comps2HelpText = null;
         Set propSet = props.keySet();
         java.util.List propList = new ArrayList();
        for (Iterator it = propSet.iterator();it.hasNext();){
            String key = (String)it.next();            
            Object value = props.get(key);
            if (value instanceof String){
                //this is a global prop
                if (db.isEditable((String)value)){
                    String[] keyAndValue = new String[2];
                    keyAndValue[0] = key;
                    keyAndValue[1] = (String) value;
                    propList.add(keyAndValue);
                }
            } else {
                //these are props for a component
                if (value instanceof SortedMap){
                    SortedMap newLocalProps = new TreeMap();
                    Set keys = ((SortedMap)value).keySet();
                    for (Iterator it2 = keys.iterator();it2.hasNext();){
                        String nextKey = (String)it2.next();
                        String nextValue = (String)((SortedMap)value).get(nextKey);
                        String[] keyAndValue = new String[2];
                        keyAndValue[0] = nextKey;
                        keyAndValue[1] = nextValue;
                        propList.add(keyAndValue);
                    }
                }
            }
        }//end of loop over props
        tableProps = new String[propList.size()][];
        for (int i=0;i<propList.size();i++){
            tableProps[i] = (String[]) propList.get(i);            
        }
        
        display(null, true);
    }
    
    public SettingsGUI(DatabaseLayout db, 
            			String[][] props,
            			String selectedComp,
            			Map comps2HelpText)
    {
        this.db = db;        
        this.tableProps = props;
        this.comps2HelpText = comps2HelpText;
        display(selectedComp,false);
    }
    
    /**
     * Show a frame displaying the help file.
     * @param db the DatbaseLayout
     * @param props the properties and values to be displayed
     * @return true, if no error occurred
     */
    public void display(String selectedComp,boolean simpleMode)
    {
        wasSaved = false;
        //final JFrame frame = new JFrame("Settings Editor");
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridC = new GridBagConstraints();
        frame.getContentPane().setLayout( gridBagLayout );
        if (simpleMode){
            JLabel guiTextLabel = new JLabel(guiText);
            gridC.gridx = 0;
            gridC.gridy = 0;
            gridBagLayout.setConstraints(guiTextLabel, gridC );
            frame.getContentPane().add(guiTextLabel);
            String[] columnNames = {"Property",
            "Value"};
            tableModel = new PropTableModel(columnNames,tableProps);
        } else {
            compNames = db.getCompNamesForDisplay();        
            componentsComboBox = new JComboBox(compNames);
            componentsComboBox.setSelectedItem(selectedComp);
            componentsComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JComboBox cb = (JComboBox)e.getSource();
                    String compName = (String)cb.getSelectedItem();
                    updateTable(compName);
                }
            });
            gridC.gridx = 0;
            gridC.gridy = 0;
            gridBagLayout.setConstraints(componentsComboBox , gridC );
            frame.getContentPane().add(componentsComboBox);
            
            //build a new JTable        
            String[] columnNames = {"Property",
            "Value"};
            String[][] currentProps = getPropsForCompName(selectedComp);
            tableModel = new PropTableModel(columnNames,currentProps);
        }
        table = new JTable(tableModel); 
        //set the focus traversal keys for the table
        Set forwardKeys = new HashSet();
        forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0, false));
        table.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,forwardKeys);
        Set backwardKeys = new HashSet();
        backwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,KeyEvent.SHIFT_MASK+KeyEvent.SHIFT_DOWN_MASK, false));
        table.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,backwardKeys);
        //table.setPreferredScrollableViewportSize(new Dimension(600, 500));
        scrollPane = new JScrollPane(table);
        gridC.gridy = 1;
        // resize scroll pane:
        gridC.weightx = 1;
        gridC.weighty = 1;
        gridC.fill = GridBagConstraints.HORIZONTAL;        
        scrollPane.setPreferredSize(new Dimension(600, 300));
        gridBagLayout.setConstraints( scrollPane, gridC );
        
        frame.getContentPane().add(scrollPane);
        gridC.gridy = 2;
        // do not resize buttons:
        gridC.weightx = 0;
        gridC.weighty = 0;
        JButton helpButton = new JButton("Help");
        helpButton.setMnemonic(KeyEvent.VK_H);
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    new Thread("DisplayHelpGUIThread") {
                        public void run() {    
                            if (componentsComboBox == null){
                                new HelpGUI(simpleModeHelpText).display();  
                            } else {
                                String helpText = 
                                    (String) comps2HelpText.get(componentsComboBox.getSelectedItem());
                                new HelpGUI(helpText).display();      
                            }
                        }}.start();
            }
        });
        JButton saveButton = new JButton("Save");
        saveButton.setMnemonic(KeyEvent.VK_S);
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int col = table.getEditingColumn();
                if (col!=-1){
                    TableCellEditor cellEditor =
                        table.getColumnModel().getColumn(col).getCellEditor();
                    if (cellEditor != null){
                        cellEditor.stopCellEditing();
                    } else {
                        table.getCellEditor().stopCellEditing();
                    }
                }
                updateProps();
                wasSaved = true;
                frame.setVisible(false);
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wasSaved = false;
                frame.setVisible(false);
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(helpButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        gridBagLayout.setConstraints( buttonPanel, gridC );
        frame.getContentPane().add(buttonPanel);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                wasSaved = false;
                frame.setVisible(false);
            }
        });
        frame.pack();
        frame.setVisible(true);
        do {
            try {
                Thread.sleep(10); // OK, this is ugly, but I don't mind today...
            } catch (InterruptedException e) {
            }
        } while (frame.isVisible());
        
        frame.dispose(); 
    }
    
    public boolean wasSaved(){
        return wasSaved;
    }
    
    private String[][] getPropsForCompName(String name){
        if (name.equals("Global properties"))
            name = "db";
        java.util.List propList = new ArrayList();
        for (int i=0;i<tableProps.length;i++){
            String[] keyAndValue = tableProps[i];
            //System.err.println(keyAndValue[0]+" --- "+name);
            if (keyAndValue[0].startsWith(name+".")){
                propList.add(keyAndValue);
            }
        }
        String[][] result = new String[propList.size()][];
        for (int i=0;i<propList.size();i++){
            result[i] = (String[]) propList.get(i);
        }
        return result;        
    }
    
    private void updateProps(){           
        db.updateProps(tableProps);
    }
    
    private void updateTable(String compName){
        String[][] currentProps = getPropsForCompName(compName);
        tableModel.setProps(currentProps);
        table.tableChanged(new TableModelEvent(tableModel));
        //int hsize = currentProps.length*20;
        //scrollPane.setPreferredSize(new Dimension(600,hsize));
        //frame.pack();
    }
    
    private class PropTableModel extends AbstractTableModel {
        
        
        private String[] columnNames;
        private boolean DEBUG = false;
        private String[][] props;

        public PropTableModel(String[] columnNames,
                			String[][] props){
            this.columnNames = columnNames;
            this.props = props;
        }
        
        public void setProps(String[][] props){
            this.props = props;
        }
        
        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return props.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return props[row][col];
        }
        
        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        public Class getColumnClass(int c) {
            return String.class;
        }

        
        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col == 0) {
                return false;
            } else {
                return true;
            }
        }


        public void setValueAt(Object value, int row, int col) {
            if (DEBUG) {
                System.out.println("Setting value at " + row + "," + col
                                   + " to " + value);
            }
            props[row][col] = (String)value;
            fireTableCellUpdated(row, col);
        }

        private void printDebugData() {
            int numRows = getRowCount();
            int numCols = getColumnCount();

            for (int i=0; i < numRows; i++) {
                System.out.print("    row " + i + ":");
                for (int j=0; j < numCols; j++) {
                    System.out.print("  " + props[i][j]);
                }
                System.out.println();
            }
            System.out.println("--------------------------");
        }
    }
    
    class HelpButtonActionListener implements ActionListener{
        
        
        
            public void actionPerformed(ActionEvent e) {
                    new Thread("DisplayHelpGUIThread") {
                        public void run() {
                                                     
                        }}.start();
            }
        }
    
}
