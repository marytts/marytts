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


/**
 * Simple Synthesis script GUI.
 * 
 * @author Marcela Charfuelan, Holmer Hemsen.
 */
public class SynthesisScriptGUI extends JPanel implements TableModelListener{
 
    String colNames[] = {"Unwanted", "No.", "Sentence"};
    Object[][] data = null;
    DefaultTableModel dtm;
    private JTextArea output;
    private static JFrame frame;
    JTextField[] fields;
    
    
    public static String locale;
    //  mySql database args
    public static String mysqlHost;
    public static String mysqlUser;
    public static String mysqlPasswd;
    public static String mysqlDB;
    public static String tableName;
    public static DBHandler wikiToDB=null;
    public static int selIds[];
    public static String saveFile;
    
    SynthesisScriptGUI() {
        super();
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        output = new JTextArea(5, 40);
        output.setEditable(false);
        dtm = new DefaultTableModel(data,colNames);
        printSettings();
        
        // Get the sentences from DB
        selIds=null;
        if(tableName != null) {
          selIds = wikiToDB.getIdListOfSelectedSentences(locale, tableName, "unwanted=false");  
          if( selIds != null){
            String str;
            for(int i=0; i<selIds.length; i++){
              str = wikiToDB.getSentence("dbselection", selIds[i]);
              dtm.addRow(new Object[]{new Boolean(false), i, str });
            }
            
          } else
            output.append("No selected sentences in TABLE = " + locale + "_" + tableName + "_selectedSentences.");
        } 
        
        if(selIds==null || tableName==null) {
            for(int i=0; i<10; i++){
                dtm.addRow(new Object[]{new Boolean(false), "", "" });  
            } 
            output.append("Please use the menu to load a selected sentences table.");
        }
        
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
        
        output.append("event=" + command + " row=" + event.getLastRow() );
        
        boolean res = (Boolean)dtm.getValueAt(row, 0);
        
        if(res==true)
            output.append(" checked true");
        else
            output.append(" unchecked false");
        output.append(".\n");
          
    }
    
    public static void main (String[] args){
       /* 
        locale = args[0];
        //  mySql database args
        mysqlHost = args[1];
        mysqlUser = args[2];
        mysqlPasswd = args[3];
        mysqlDB = args[4];
        tableName = args[5];
        tableName=null;
        */
        
        //wikiToDB = new DBHandler(locale);          
        //wikiToDB.createDBConnection(mysqlHost,mysqlDB,mysqlUser,mysqlPasswd);    
        
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
        
        // close it on exit
        //wikiToDB.closeDBConnection();
    }
 
    public void printSettings(){
       output.append("\nCURRENT SETTINGS:\n");  
       output.append("Locale:      " + locale + "\n");
       output.append("Mysql Host:  " + mysqlHost + "\n");
       output.append("Mysql user:  " + mysqlUser + "\n");
       output.append("Mysql paswd: " + mysqlPasswd + "\n");
       output.append("Data base:   " + mysqlDB + "\n");
       output.append("Table name:  " + locale + "_" + tableName + "_selectedSentences"+ "\n");
    }
    
    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Define and add two drop down menu to the menubar
        JMenu fileMenu = new JMenu("File");
        //JMenu editMenu = new JMenu("Edit");
        menuBar.add(fileMenu);
        //menuBar.add(editMenu);
        
        // Create and add simple menu item to one of the drop down menu
        JMenuItem mysqlInfoAction = new JMenuItem("Enter Mysql DB parameters");
        JMenuItem loadTableAction = new JMenuItem("Load selected sentences table");
        JMenuItem saveScriptAction = new JMenuItem("Save synthesis script as");
        JMenuItem printTableDescriptionAction = new JMenuItem("Print table description");
        JMenuItem updateListAction = new JMenuItem("Update table");
        JMenuItem helpAction = new JMenuItem("Help");
        JMenuItem exitAction = new JMenuItem("Exit");
        
        /*
        JMenuItem cutAction = new JMenuItem("Cut");
        JMenuItem copyAction = new JMenuItem("Copy");
        JMenuItem pasteAction = new JMenuItem("Paste");
        
        
        // Create and add CheckButton as a menu item to one of the drop down
        // menu
        JCheckBoxMenuItem checkAction = new JCheckBoxMenuItem("Check Action");
        */
        /*
        // Create and add Radio Buttons as simple menu items to one of the drop
        // down menu
        JRadioButtonMenuItem radioAction1 = new JRadioButtonMenuItem("Radio Button1");
        JRadioButtonMenuItem radioAction2 = new JRadioButtonMenuItem("Radio Button2");
        // Create a ButtonGroup and add both radio Button to it. Only one radio
        // button in a ButtonGroup can be selected at a time.
        ButtonGroup bg = new ButtonGroup();
        bg.add(radioAction1);
        bg.add(radioAction2);
        */
        fileMenu.add(mysqlInfoAction);
        fileMenu.add(loadTableAction);
        fileMenu.add(saveScriptAction);
        fileMenu.add(printTableDescriptionAction);
        fileMenu.add(updateListAction);
        //fileMenu.add(checkAction);
        fileMenu.addSeparator();
        fileMenu.add(helpAction);
        fileMenu.add(exitAction);
        /*
        editMenu.add(cutAction);
        editMenu.add(copyAction);
        editMenu.add(pasteAction);
        editMenu.addSeparator();
        editMenu.add(radioAction1);
        editMenu.add(radioAction2);
        */
        

        mysqlInfoAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("You have clicked on the mysqlInfoActio action");
                loadMysqlParameters(); 
               
            }
        });
        
        loadTableAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("You have clicked on the loadTableAction action");
                
                String response = JOptionPane.showInputDialog(null,
                        "Selected sentences table name:",
                        "",
                        JOptionPane.QUESTION_MESSAGE);
                // tableName = response;
                loadTable();
            }
        });
        
        saveScriptAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("You have clicked on the saveScriptActio action");
                
                loadPhoneSetActionPerformed();
            }
        });
        
        
        exitAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("You have clicked on the Exit action");
                wikiToDB.closeDBConnection();
                System.exit(1);
            }
        });

        return menuBar;
    }
    
    public void loadTable(){
        SynthesisScriptGUI select = new SynthesisScriptGUI();
        frame.setSize(500, 300);
        //frame.setJMenuBar(select.createMenuBar());
        frame.setContentPane(select);

        //Display the window.
        frame.pack();
        frame.setVisible(true); 
    }
    
    private void loadPhoneSetActionPerformed() {
        
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save as");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fc.showSaveDialog(SynthesisScriptGUI.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            saveFile  = file.getAbsolutePath();
            
            System.out.println("saveFile" + saveFile);
        }        
    }
    
    public void loadMysqlParameters(){
        
        final JFrame f = new JFrame("Text Form Example");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.getContentPane().add(createTextForm(), BorderLayout.NORTH);
        JPanel p = new JPanel();
        
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                locale = getText(0);
                mysqlHost = getText(1);
                mysqlUser = getText(2);
                mysqlPasswd = getText(3);
                mysqlDB = getText(4);
                tableName = getText(5);
                
                if(wikiToDB != null)
                  wikiToDB.closeDBConnection();  
                wikiToDB = new DBHandler(locale);          
                wikiToDB.createDBConnection(mysqlHost,mysqlDB,mysqlUser,mysqlPasswd);    
                
                loadTable();
                
                System.out.println("THIS" + this);
                //f.setVisible(false);
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
    
    public String getText(int i) {
        return (fields[i].getText());
      }
    
    public JPanel createTextForm() {
        
        JPanel textForm = new JPanel();
        textForm.setLayout(new BorderLayout());
        
        String[] labels = { "Locale", "Host", "User Name", "Password", "Database Name", "Table Name" };
        int[] widths = { 15, 15, 15, 15, 15, 15 };
        String[] descs = { "en_US", "localhost", "marcela", "wiki123", "wiki", "test"};    
    
        JPanel labelPanel = new JPanel(new GridLayout(labels.length, 1));
        JPanel fieldPanel = new JPanel(new GridLayout(labels.length, 1));
        textForm.add(labelPanel, BorderLayout.WEST);
        textForm.add(fieldPanel, BorderLayout.CENTER);
        fields = new JTextField[labels.length];

        for (int i = 0; i < labels.length; i += 1) {
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
      
 }


    
  


