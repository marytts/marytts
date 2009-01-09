package marytts.tools.transcription;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import marytts.cart.CART;
import marytts.cart.StringPredictionTree;
import marytts.features.FeatureDefinition;
import marytts.fst.AlignerTrainer;
import marytts.fst.StringPair;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.modules.phonemiser.TrainedLTS;
import marytts.tools.dbselection.DBHandler;
import marytts.tools.newlanguage.LTSTrainer;
import marytts.util.io.FileUtils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.Element;
import javax.swing.text.TableView.TableRow;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.xml.sax.SAXException;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dimension;

/**
 * 
 * TranscriptionTable, A Table panel, tracs events in user transcription entries    
 * @author sathish pammi
 *
 */
public class TranscriptionTable extends JPanel implements ActionListener { 
    private JTable table;
    int itsRow =0;
    TranscriptionTableModel transcriptionModel;
    private AllophoneSet phoneSet;
    private int editableColumns = 2;
    int previousRow = 0;
    boolean trainPredict = false;
    JScrollPane scrollpane;
    String locale;
    
    public TranscriptionTable() throws Exception{
        super();
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        transcriptionModel = new TranscriptionTableModel();
        
        table = new JTable(transcriptionModel);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(new RowListener());
        table.getColumnModel().getColumn(2).setCellRenderer(new CustomTableCellRenderer());
        table.addKeyListener(new KeyEventListener());
        
        scrollpane = new JScrollPane(table);
        add(scrollpane);
        TableColumn column = table.getColumnModel().getColumn(1);
        int columnSize = column.getPreferredWidth();
        column.setPreferredWidth(2*columnSize);
        table.getColumnModel().getColumn(2).setPreferredWidth(2*columnSize);
    }

    
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
    }

    /**
     *  verify previous row transcription syntax
     */
    private void checkTranscriptionSyntax(){
        
        int x = table.getSelectionModel().getLeadSelectionIndex();
        
        if(previousRow!=x){
            String transcription =  (String) transcriptionModel.getDataAt(previousRow, 2);
            transcription = transcription.replaceAll("\\s*", "");
            this.transcriptionModel.setValueAt(transcription , previousRow, 2);
            if(!transcription.equals("")){
                String check = phoneSet.checkAllophoneSyntax(transcription);
                if(check.equals("OK")){
                    transcriptionModel.setAsCorrectSyntax(previousRow, true);
                }
                else transcriptionModel.setAsCorrectSyntax(previousRow, false);
                transcriptionModel.setAsManualVerify(previousRow, true);
                if(x == itsRow){
                    transcriptionModel.setAsManualVerify(x, true);
                }
            }
        }
        previousRow = x;
    }
    
    /**
     * verify transcription syntax
     * @param row
     */
    private void checkTranscriptionSyntax(int row){
        
        String transcription =  (String) transcriptionModel.getDataAt(row, 2);
        transcription = transcription.replaceAll("\\s*", "");
        this.transcriptionModel.setValueAt(transcription , row, 2);
        if(!transcription.equals("")){
            String check = phoneSet.checkAllophoneSyntax(transcription);
            if(check.equals("OK")){
                transcriptionModel.setAsCorrectSyntax(row, true);
            }
            else transcriptionModel.setAsCorrectSyntax(row, false);
            if(row == itsRow){
                transcriptionModel.setAsManualVerify(row, true);
            }
        }
        else{
            transcriptionModel.setAsCorrectSyntax(row, false);
        }
    }
    
    private LTSTrainer trainLTS(String treeAbsolutePath) throws IOException{
        
        Object[][] tableData = transcriptionModel.getData();
        HashMap<String, String> map = new HashMap<String, String>();
        boolean[] hasManualVerify = transcriptionModel.getManualVerifiedList();
        boolean[] hasCorrectSyntax = transcriptionModel.getCorrectSyntaxList();
        
        for(int i=0;i<tableData.length; i++){
            if(hasManualVerify[i] && hasCorrectSyntax[i]){
                String grapheme = (String) tableData[i][1];
                String phoneme = (String) tableData[i][2];
                if(!phoneme.equals("")){
                    map.put(grapheme, phoneme);
                    transcriptionModel.setAsCorrectSyntax(i, true);
                }
            }
        }
        
        LTSTrainer tp = new LTSTrainer(phoneSet, true, true, 2);
        tp.readLexicon(map);
        System.out.println("training ... ");
        // make some alignment iterations
        for ( int i = 0 ; i < 5 ; i++ ){
            System.out.println("iteration " + i);
            tp.alignIteration();
        }
        System.out.println("training completed.");
        CART st = tp.trainTree(100);
        tp.save(st, treeAbsolutePath);
        return tp;        
    }
    
    /**
     * train and predict module
     * @param treeAbsolutePath
     */
    public void trainPredict(String treeAbsolutePath){
        Object[][] tableData = transcriptionModel.getData();
        boolean[] hasManualVerify = transcriptionModel.getManualVerifiedList();
        boolean[] hasCorrectSyntax = transcriptionModel.getCorrectSyntaxList();
        try {
            LTSTrainer tp = this.trainLTS(treeAbsolutePath); 
            TrainedLTS trainedLTS = new TrainedLTS(phoneSet,treeAbsolutePath);
            for(int i=0;i<tableData.length; i++){
                if(!(hasManualVerify[i] && hasCorrectSyntax[i])){
                    String grapheme = (String) tableData[i][1];
                    String phoneme = trainedLTS.syllabify(trainedLTS.predictPronunciation(grapheme));
                    transcriptionModel.setValueAt(phoneme.replaceAll("\\s+", ""), i, 2);
                    transcriptionModel.setAsCorrectSyntax(i, true);
                    transcriptionModel.setAsManualVerify(i, false);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        trainPredict = true;
    }
    
    /**
     * save transcrption into file
     * @param fileName
     */
    public void saveTranscription(String fileName) {
        try {
            this.transcriptionModel.saveTranscription(fileName);
            File parentDir = (new File(fileName)).getParentFile();
            String parentPath = parentDir.getAbsolutePath();
            String lexiconFile = parentPath+File.separator+"lexicon_"+locale+".dict";
            String fstFile = parentPath+File.separator+"lexicon_"+locale+".fst";
            String posFile = parentPath+File.separator+"functional_"+locale+".list";
            String posFst = parentPath+File.separator+"functional_"+locale+".fst";
            
            transcriptionModel.saveSampaLexiconFormat(lexiconFile);
            transcriptionModel.createLexicon(lexiconFile, fstFile);
            transcriptionModel.saveFunctionalWords(posFile);
            transcriptionModel.createPOSFst(posFile, posFst);
            //trainLTS(treeAbsolutePath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Load transcription from file
     * @param fileName
     */
    public void loadTranscription(String fileName) {
        try {
            this.transcriptionModel.loadTranscription(fileName);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        scrollpane.updateUI();
        table.repaint();
        this.repaint();
        this.updateUI();
    }
    
    /**
     * Load transcription from a hashmap
     * @param map
     * @throws Exception
     */
    public void loadTranscription(HashMap<String, Integer> map) throws Exception {
            this.transcriptionModel.loadTranscription(map);
            scrollpane.updateUI();
            table.repaint();
            this.repaint();
            this.updateUI();
    }
    
    /**
     * load phoneset
     * @param filePath
     */
    public void loadPhoneSet(String filePath){
        try {
            phoneSet = AllophoneSet.getAllophoneSet(filePath);
            locale = phoneSet.getLocale().toString();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public String getLocaleString(){
        return locale;
    }
    
    /**
     * Row event listener
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
            checkTranscriptionSyntax();
        }
    }

    /**
     * Column event listener
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
            checkTranscriptionSyntax();
        }
    }

    /**
     * Key event listener
     * @author sathish
     *
     */
    private class KeyEventListener implements KeyListener {
        
        public void keyPressed(KeyEvent arg0) {
            int[] selectedRows = table.getSelectedRows();
            if(arg0.getKeyCode() == 32){
                for(int i=0 ; i<selectedRows.length; i++){
                    if((Boolean) transcriptionModel.getValueAt(selectedRows[i], 3)){
                        transcriptionModel.setValueAt((Object)(false),selectedRows[i] , 3);
                    }
                    else{
                        transcriptionModel.setValueAt((Object)(true),selectedRows[i], 3);
                    }
                }
            }
        }

        public void keyReleased(KeyEvent arg0) {
            if(arg0.getKeyCode() == 10){
                table.editCellAt(itsRow, 2);
            }
        }

        public void keyTyped(KeyEvent arg0) {
            // TODO Auto-generated method stub
        }
    }
    
    /**
     * Color rendering class
     * @author sathish
     *
     */
    public class CustomTableCellRenderer extends DefaultTableCellRenderer{
        public Component getTableCellRendererComponent (JTable ttable, 
    Object obj, boolean isSelected, boolean hasFocus, int row, int column) {
          Component cell = super.getTableCellRendererComponent(
                             ttable, obj, isSelected, hasFocus, row, column);
          
          checkTranscriptionSyntax(row);
          boolean[] hasManualVerify = transcriptionModel.getManualVerifiedList();
          boolean[] hasCorrectSyntax = transcriptionModel.getCorrectSyntaxList(); 
          cell.setFont(new Font("Serif", Font.BOLD, 12));
          
          if(column == 2){
              
              if(!hasCorrectSyntax[row]){
                  cell.setForeground(Color.RED);
                  transcriptionModel.setAsManualVerify(row, false);
              }
              else if(!hasManualVerify[row]){
                  cell.setForeground(Color.LIGHT_GRAY);
              }
              else {
                  cell.setForeground(Color.BLACK);
              }
              if(row == itsRow){
                  cell.setForeground(Color.BLACK);
              }
          }
          else{
              cell.setForeground(Color.BLACK);
          }
          return cell;
        }
      }
   
    
}
