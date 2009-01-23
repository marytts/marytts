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
package marytts.tools.dbselection;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import marytts.tools.transcription.TranscriptionGUI;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;


/**
 * Simple Synthesis script GUI.
 * 
 * @author Marcela Charfuelan, Holmer Hemsen.
 */
public class SynthesisScriptGUI extends JPanel implements TableModelListener{
 
    String colNames[] = {"Unwanted", "No.", "Sentence"};
    Object[][] data = null;
    DefaultTableModel dtm;
    private static JTextArea output;
    private static JFrame frame;
    JTextField[] fields;
    
    // Initialisation with default values
    private static String locale      = "en_US";
    //  mySql database args
    private static String mysqlHost   = "localhost";
    private static String mysqlUser   = "marcela";
    private static String mysqlPasswd = "wiki123";
    private static String mysqlDB     = "wiki";
    private static String tableName   = "test";
    private static String actualTableName   = locale + "_test_selectedSentences";
    private static DBHandler wikiToDB = null;
    private static int selIds[]=null;
    private static int numWanted=0;
    private static int numUnwanted=0;
    private static String saveFile;
    private static boolean mysqlInfo = false;
    private static boolean connectionProblems = false;
    
    
    SynthesisScriptGUI() {
        super();
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        output = new JTextArea(5, 40);
        output.setEditable(false);
        dtm = new DefaultTableModel(data,colNames);        
                   
        // Get the sentences from DB
        selIds=null;
        if( !connectionProblems && mysqlInfo) {
          // check if this table name exist in the DB
          if(wikiToDB.tableExist(actualTableName) ){
            selIds = wikiToDB.getIdListOfSelectedSentences(actualTableName, "unwanted=false");  
            if( selIds != null){
              String str;
              numWanted=selIds.length;
              numUnwanted=0;
              for(int i=0; i<selIds.length; i++){
                str = wikiToDB.getSelectedSentence(actualTableName, selIds[i]);
                dtm.addRow(new Object[]{new Boolean(false), (i+1), str });
              }
            } else
              output.append("There are not selected sentences in TABLE = " + actualTableName);
          } else
            output.append("\nERROR TABLE = " + actualTableName + " does not exist\n");    
        } 
        
        if(selIds==null || mysqlInfo==false || connectionProblems) {
            for(int i=0; i<30; i++){
                dtm.addRow(new Object[]{new Boolean(false), "", "" });  
            }
        }
        if(!mysqlInfo)
            output.append("Please use the options menu to Enter/correct the mysql DB parameters.\n");
        if(connectionProblems)    
          output.append("Problems trying to connect to the DB, please revise your parameters.\n"); 
         
        dtm.addTableModelListener(this);
 
        JTable table = new JTable(dtm);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        JScrollPane sp = new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        
        TableColumn tc = table.getColumnModel().getColumn(0);
        tc.setCellEditor(table.getDefaultEditor(Boolean.class));
        tc.setCellRenderer(table.getDefaultRenderer(Boolean.class));
        
        tc.setResizable(true);
        tc.setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(1000);
        
        add(sp); 
        add(new JScrollPane(output));
        
        setPreferredSize(new Dimension(1000,700));
        
    }
    
    public void tableChanged(TableModelEvent event) {
        int command = event.getType(); 
        int row = event.getLastRow();
        
        //output.append("event=" + command + " row=" + event.getLastRow() );
        if( selIds != null){
          boolean res = (Boolean)dtm.getValueAt(row, 0);
          // Marc the sentence as unwanted in both the dbselection table and selectedSentences table
          if(res==true) {
            numUnwanted++;
            numWanted--;
            output.append("id=" + selIds[row] + " set as unwanted (No. wanted=" + numWanted + " No. unwanted=" + numUnwanted + ")\n");
            wikiToDB.setUnwantedSentenceRecord(actualTableName, selIds[row], res);
          }
          else {
            numWanted++;
            numUnwanted--;
            output.append("id=" + selIds[row] + " set as wanted (No. wanted=" + numWanted + " No. unwanted=" + numUnwanted + ")\n");
            wikiToDB.setUnwantedSentenceRecord(actualTableName, selIds[row], res);
          }        
        }          
    }
    
    public static void main (String[] args){
       
        // Create and set up the window.
        frame = new JFrame("Synthesis script GUI");
        
        SynthesisScriptGUI select = new SynthesisScriptGUI();
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        select.setOpaque(true); //content panes must be opaque
        frame.setLocation(3, 3);
        frame.setJMenuBar(select.createMenuBar());
        frame.setContentPane(select);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        select.printHelp();
        
    }
 
    public void loadTable(){
        
        if(wikiToDB != null)
            wikiToDB.closeDBConnection();  
        
        wikiToDB=null;
        mysqlInfo=false;
        connectionProblems=false;       
        wikiToDB = new DBHandler(locale); 
        if( wikiToDB.createDBConnection(mysqlHost,mysqlDB,mysqlUser,mysqlPasswd) )
          mysqlInfo=true;
        else{
          connectionProblems=true;
          wikiToDB=null;
        }
        
        
        SynthesisScriptGUI select = new SynthesisScriptGUI();
        frame.setContentPane(select);
        //Display the window.
        frame.pack();
        frame.setVisible(true);
        printSettings();
    }
    
    public void printSettings(){
       output.append("\nCURRENT SETTINGS:\n");  
       output.append("Locale:      " + locale + "\n");
       output.append("Mysql Host:  " + mysqlHost + "\n");
       output.append("Mysql user:  " + mysqlUser + "\n");
       output.append("Mysql paswd: " + mysqlPasswd + "\n");
       output.append("Data base:   " + mysqlDB + "\n");
       output.append("Table name:  " + actualTableName + "\n");
    }
    
    
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();        
        JMenu fileMenu = new JMenu("Options");
        menuBar.add(fileMenu);
        
        JMenuItem mysqlInfoAction = new JMenuItem("Enter Mysql DB parameters");
        JMenuItem loadTableAction = new JMenuItem("Load new selected sentences table");
        JMenuItem saveScriptAction = new JMenuItem("Save synthesis script as");
        JMenuItem printTablePropertiesAction = new JMenuItem("Print table properties");
        JMenuItem updateListAction = new JMenuItem("Update table");
        JMenuItem helpAction = new JMenuItem("Help");
        JMenuItem exitAction = new JMenuItem("Exit");
       
        fileMenu.add(mysqlInfoAction);
        fileMenu.add(loadTableAction);
        fileMenu.add(saveScriptAction);
        fileMenu.add(printTablePropertiesAction);
        fileMenu.add(updateListAction);
        fileMenu.addSeparator();
        fileMenu.add(helpAction);
        fileMenu.add(exitAction);
        
        mysqlInfoAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                readMysqlParameters();                
            }
        });
        
        loadTableAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
               String response = JOptionPane.showInputDialog(null, "Selected sentences table name:",
                                                              "", JOptionPane.QUESTION_MESSAGE);
                if(response.length() > 0){
                  tableName = response;
                  actualTableName = locale + "_" + tableName + "_selectedSentences";
                  if( wikiToDB.tableExist(actualTableName) )
                    loadTable();
                  else
                    output.append("\nERROR TABLE = " + actualTableName + " does not exist\n");  
                } else {
                 output.append("\nERROR: New table name empty.\n");
                }
            }
        });
        
        saveScriptAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {               
                saveSynthesisScriptAs();
            }
        });
              
        printTablePropertiesAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                printTableProperties();              
            }
        });
        
        updateListAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                loadTable();            
            }
        });
        
        helpAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
               printHelp();               
            }
        });
        
        exitAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                wikiToDB.closeDBConnection();
                System.exit(1);
            }
        });
        return menuBar;
    }
    
    
    private void saveSynthesisScriptAs() {       
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save as");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fc.showSaveDialog(SynthesisScriptGUI.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            saveFile  = file.getAbsolutePath();
            output.append("Saving synthesis script to file:" + saveFile + "\n");
            
            int sel[] = wikiToDB.getIdListOfSelectedSentences(actualTableName, "unwanted=false");
            
            if( sel != null){
              // saving sentences in a file
              try{              
                PrintWriter selectedLog = new PrintWriter(new FileWriter(new File(saveFile)));                
                String str;
                for(int i=0; i<sel.length; i++){
                  str = wikiToDB.getSelectedSentence(actualTableName, sel[i]);  
                  selectedLog.println(sel[i] + " " + str);
                }
                selectedLog.close();
              } catch(Exception e){
                  System.out.println(e); 
              }              
            } else
                System.out.println("No selected sentences to save.");              
        }        
    }
    
    public void readMysqlParameters(){
        
        final JFrame f = new JFrame("Read Mysql parameters");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.getContentPane().add(createTextForm(), BorderLayout.NORTH);
        JPanel p = new JPanel();
 
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
                locale = fields[0].getText();
                mysqlHost = fields[1].getText();
                mysqlUser = fields[2].getText();
                mysqlPasswd = fields[3].getText();
                mysqlDB = fields[4].getText();
                tableName = fields[5].getText();
                actualTableName   = locale + "_" + tableName + "_selectedSentences";
               
                if(locale.length()>0 && mysqlHost.length()>0 && mysqlDB.length()>0 && 
                        mysqlUser.length()>0 && mysqlPasswd.length()>0 )
                   loadTable();
                else {
                  if(locale.length()==0)  
                    output.append("\nERROR PARAMETER: locale is empty\n");
                  if(mysqlHost.length()==0)  
                      output.append("\nERROR PARAMETER: Mysql host name is empty\n");
                  if(mysqlDB.length()==0)  
                      output.append("\nERROR PARAMETER: Mysql Data base name is empty\n");
                  if(mysqlUser.length()==0)  
                      output.append("\nERROR PARAMETER: Mysql user name is empty\n");
                  if(mysqlPasswd.length()==0)  
                      output.append("\nERROR PARAMETER: Mysql password is empty\n");
                }
                
                f.dispose();
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                f.dispose();
            }
        });
        p.add(okButton);
        p.add(cancelButton);
        f.getContentPane().add(p, BorderLayout.SOUTH);
        f.pack();
        f.setVisible(true);        
    }
    
    
    public JPanel createTextForm() {
        
        JPanel textForm = new JPanel();
        textForm.setLayout(new BorderLayout());
        
        String[] labels = { "Locale", "Host", "User Name", "Password", "Database Name", "Table Name" };
        int[] widths = { 15, 15, 15, 15, 15, 15 };
        String[] descs = { locale, mysqlHost, mysqlUser, mysqlPasswd, mysqlDB, tableName};
    
        JPanel labelPanel = new JPanel(new GridLayout(labels.length, 1));
        JPanel fieldPanel = new JPanel(new GridLayout(labels.length, 1));
        textForm.add(labelPanel, BorderLayout.WEST);
        textForm.add(fieldPanel, BorderLayout.CENTER);
        fields = new JTextField[labels.length];

        for (int i = 0; i < labels.length; i++) {
          fields[i] = new JTextField();
          if (i < descs.length)
            fields[i].setText(descs[i]);  
            //fields[i].setToolTipText(descs[i]);
          if (i < widths.length)
            fields[i].setColumns(widths[i]);

          JLabel lab = new JLabel(labels[i], JLabel.RIGHT);
          lab.setLabelFor(fields[i]);
         
          labelPanel.add(lab);
          JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
          p.add(fields[i]);
          fieldPanel.add(p);            
        }
        return textForm;  
    }
    
    public void printTableProperties(){
      String table[] = wikiToDB.getTableDescription(actualTableName);
      output.append("\nTABLE PROPERTIES: \nPROPERTY=tableName " + table[0] + "\n");
      output.append("PROPERTY=description: " + table[1] + "\n");
      output.append("PROPERTY=stopCriterion: " + table[2] + "\n");
      output.append("PROPERTY=featuresDefinitionFileName: " + table[3] + "\n");
      output.append("PROPERTY=featuresDefinitionFile:\n" + table[4] + "\n");
      output.append("PROPERTY=covDefConfigFileName: " + table[5] + "\n");
      output.append("PROPERTY=covDefConfigFile:\n" + table[6] + "\n");
    }
    
    
    public void printHelp(){
        output.append("\n Synthesis script options:\n");
        output.append("1. Enter Mysql DB parameters: reads mysql parameters and load a selected sentences table.\n");
        output.append("   - Once the sentences are loaded, use the checkboxes to mark sentences as unwanted/wanted.\n" +
                      "   - Sentences marked as unwanted can be unselected and set as wanted again. \n" +
                      "   - The DB is updated every time a checkbox is selected. \n" +
                      "   - There is no need to save changes. Changes can be made before the table is updated or the program exits.\n");
        output.append("3. Save synthesis script as: saves the selected sentences, without unwanted, in a file.\n");
        output.append("4. Load new selected sentences table: loads another table in the same DB. \n");        
        output.append("5. Print table properties: prints the properties used to generate the list of sentences.\n");
        output.append("6. Update table: presents the table without the sentences marked as unwanted.\n" +
                      "   - If the table has changed in the database after one run of the DatabaseSelector, this option \n" +
                      "     will include new selected sentences added to the selectedSentences table.\n");
        output.append("7. Help: presents this description.\n");
        output.append("8. Exit: terminates the program.\n");
        
    }
      
 }

  
