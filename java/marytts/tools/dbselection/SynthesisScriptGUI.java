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

package marytts.tools.dbselection;


import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * Simple Synthesis script GUI.
 * 
 * @author Marcela Charfuelan, Holmer Hemsen.
 */
public class SynthesisScriptGUI extends JFrame implements TableModelListener{
 
    String colNames[] = {"Unwanted", "Sentence"};
    Object[][] data = null;
    DefaultTableModel dtm;
    
    public static String locale;
    //  mySql database args
    public static String mysqlHost;
    public static String mysqlUser;
    public static String mysqlPasswd;
    public static String mysqlDB;
    public static String tableName;
    
    public static DBHandler wikiToDB;
    public static int selIds[];
    
    SynthesisScriptGUI() {
        setLocation(200,100);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        dtm = new DefaultTableModel(data,colNames);
        
        // Get the sentences from DB
        selIds = wikiToDB.getIdListOfSelectedSentences(locale, tableName, "unwanted=false");  
        if( selIds != null){
          String str;
          for(int i=0; i<selIds.length; i++){
            str = wikiToDB.getSentence("dbselection", selIds[i]);
            dtm.addRow(new Object[]{new Boolean(false),str });
            //System.out.println("sel[" + i +"]=" + sel[i] + " :" + str);  
          }
        } else
            System.out.println("No selected sentences in TABLE = " + locale + "_" + tableName + "_selectedSentences.");    
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
        table.getColumnModel().getColumn(1).setPreferredWidth(1000);
        
        
        JButton button1 = new JButton("Save wanted to a file");
        button1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
             System.out.println("Save wanted to a file......");
             System.exit(0);
            }
        });
        JButton button2 = new JButton("Exit");
        button2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
             System.out.println("Save wanted to a file......");
             System.exit(0);
            }
        });
        
        setLayout(new BorderLayout());
   
        //getContentPane().add(button1,BorderLayout.PAGE_END);
        getContentPane().add(button2,BorderLayout.SOUTH);
        getContentPane().add(sp);
        
        pack();
    }
    
    public void tableChanged(TableModelEvent event) {
        int command = event.getType(); 
        int row = event.getLastRow();
        
        System.out.print("event=" + command + " row=" + event.getLastRow() );
        
        boolean res = (Boolean)dtm.getValueAt(row, 0);
        
        if(res==true)
            System.out.println(" checked true");
        else
            System.out.println(" unchecked false");
          
    }
    
    public static void main (String[] args){
        
        locale = args[0];
        //  mySql database args
        mysqlHost = args[1];
        mysqlUser = args[2];
        mysqlPasswd = args[3];
        mysqlDB = args[4];
        tableName = args[5];
        
        
        wikiToDB = new DBHandler(locale);          
        wikiToDB.createDBConnection(mysqlHost,mysqlDB,mysqlUser,mysqlPasswd);    
        
        SynthesisScriptGUI select = new SynthesisScriptGUI();
        select.setTitle("Synthesis script GUI"); 
        select.setSize(700, 700);
        select.setVisible(true);
        
        wikiToDB.closeDBConnection();
    }
 
 }


    
  

