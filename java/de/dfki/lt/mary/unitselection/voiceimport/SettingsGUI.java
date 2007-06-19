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

import java.io.File;
import java.util.*;


public class SettingsGUI {
   
    private DatabaseLayout db;
    private String[][] props;
    private PropTableModel tableModel;
    
    public void display(DatabaseLayout db, SortedMap props, String text)
    {
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
        String[][] propArray = new String[propList.size()][];
        for (int i=0;i<propList.size();i++){
            propArray[i] = (String[]) propList.get(i);            
        }
        display(db,propArray,text);
    }
    
    
    /**
     * Show a frame displaying the help file.
     * @param db the DatbaseLayout
     * @param props the properties and values to be displayed
     * @return true, if no error occurred
     */
    public void display(DatabaseLayout db, String[][] props,String text)
    {
        this.db = db;
        this.props = props;
        final JFrame frame = new JFrame("Settings Editor");
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints gridC = new GridBagConstraints();
        frame.getContentPane().setLayout( gridBagLayout );
        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(600,25));
        gridC.gridx = 0;
        gridC.gridy = 0;
        gridBagLayout.setConstraints( label, gridC );
        frame.getContentPane().add(label);
        //build a new JTable        
        String[] columnNames = {"Property",
        						"Value"};
        TableModel tableModel = new PropTableModel(columnNames);
        JTable table = new JTable(tableModel);        
        table.setPreferredScrollableViewportSize(new Dimension(600, 500));
        JScrollPane scrollPane = new JScrollPane(table);
        gridC.gridy = 1;
        // resize scroll pane:
        gridC.weightx = 1;
        gridC.weighty = 1;
        gridC.fill = GridBagConstraints.HORIZONTAL;        
        scrollPane.setPreferredSize(new Dimension(600, 500));
        gridBagLayout.setConstraints( scrollPane, gridC );
        
        frame.getContentPane().add(scrollPane);
        gridC.gridy = 2;
        // do not resize buttons:
        gridC.weightx = 0;
        gridC.weighty = 0;
        JButton helpButton = new JButton("Help");
        final String helpFile = db.getProp(db.SETTINGSHELPFILE);
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try{
                    new Thread("DisplayHelpGUIThread") {
                        public void run() {
                            File file = new File(helpFile);
                            
                            boolean ok = new HelpGUI(file).display();
                            if (ok=false){
                                System.out.println("Error displaying helpfile "
                                        +helpFile);
                            }
                        }}.start();
                }catch (Exception ex){
                    System.out.println("Can not load helpfile "
                            +helpFile+": "
                            +ex.getMessage());
                }
            }
        });
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateProps();
                frame.setVisible(false);
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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
    
    private void updateProps(){           
        db.updateProps(props);
    }
    
    private class PropTableModel extends AbstractTableModel {
        private String[] columnNames;
        private boolean DEBUG = true;

        public PropTableModel(String[] columnNames){
            this.columnNames = columnNames;
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
    
    
}
