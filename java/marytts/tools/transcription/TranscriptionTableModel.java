package marytts.tools.transcription;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;

import marytts.fst.AlignerTrainer;
import marytts.fst.FSTLookup;
import marytts.fst.TransducerTrie;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.tools.dbselection.DBHandler;
import marytts.tools.newlanguage.LexiconCreator;
import marytts.util.io.FileUtils;

/**
 * TranscriptionTableModel, an AbstractTableModel, stores transcription data
 * @author sathish pammi
 *
 */
public class TranscriptionTableModel extends AbstractTableModel {
    
    private String[] columnNames = {"No.",
                                    "Word",
                                    "Transcription",
                                    "Functional"};
    private Object[][] data; 
    private boolean[] hasManualVerification; 
    private boolean[] hasCorrectSyntax;
    private int editableColumns = 2;
    public TranscriptionTableModel(){
    
        this.data = new Object[20][4];
        this.hasManualVerification  =  new boolean[20];
        this.hasCorrectSyntax  =  new boolean[20];
        for(int i=0; i < 20; i++){
            data[i][0] = "";
            data[i][1] = "";
            data[i][2] = "";
            data[i][3] = new Boolean(false);
            setAsManualVerify(i, false);
            setAsCorrectSyntax(i, true);
        }
    }

    public Object getDataAt(int x, int y) {
        return this.data[x][y];
    }

    
    public TranscriptionTableModel(String fileName) throws Exception{
        
        String fileData   =  FileUtils.getFileAsString(new File(fileName), "UTF-8");
        String[] words    =  fileData.split("\n");
        this.data         =  new Object[words.length][4];
        this.hasManualVerification  =  new boolean[words.length];
        this.hasCorrectSyntax  =  new boolean[words.length];
        for(int i=0; i < words.length; i++){
            data[i][0] = (new Integer(i)).toString();
            data[i][1] = words[i];
            data[i][2] = "";
            data[i][3] = new Boolean(false);
            setAsManualVerify(i, false);
            setAsCorrectSyntax(i, true);
        }
    }
    
    
    public Object[][] getData(){
        return this.data;
    }
    
    public void setAsManualVerify(int x, boolean t){
        this.hasManualVerification[x] = t;
    }
    
    public void setAsCorrectSyntax(int x, boolean t){
        this.hasCorrectSyntax[x] = t;
    }
    
    public boolean[] getManualVerifiedList(){
        return this.hasManualVerification;
    }
    
    public boolean[] getCorrectSyntaxList(){
        return this.hasCorrectSyntax;
    }
    
    /**
     * Save transcription to a file
     * @param fileName
     * @throws Exception
     */
    public void saveTranscription(String fileName) throws Exception {
        
        PrintWriter out = new PrintWriter(new FileWriter(fileName));
        for(int i=0; i < data.length; i++){
            
            String line =  (String) data[i][1];
            if(!((String)data[i][2]).equals("") && this.hasManualVerification[i] && this.hasCorrectSyntax[i]){
                line += " "+(String)data[i][2];
            }
            if((Boolean)data[i][3]){
                line += " functional";
            }
            out.println(line);
        }
        out.flush();
        out.close();
    }
    
    /**
     * Load transcription from file 
     * @param fileName
     * @param phoneSet
     * @throws Exception
     */
    public void loadTranscription(String fileName) throws Exception{    
        String fileData   =  FileUtils.getFileAsString(new File(fileName), "UTF-8");
        fileData = fileData.replaceAll("\n\\s*\n", "\n");
        String[] lines    =  fileData.split("\n");
        this.data         =  new Object[lines.length][4];
        this.hasManualVerification  =  new boolean[lines.length];
        this.hasCorrectSyntax  =  new boolean[lines.length];
        
        for(int i=0; i < lines.length; i++){
            if(lines[i].trim().equals("")){
                data[i][0] = ("").toString();
                data[i][1] = "";
                data[i][2] = "";
                data[i][3] = new Boolean(false);
                continue; 
            }
            String[]  words = lines[i].trim().split("\\s+");
            data[i][0] = (new Integer(i)).toString();
            data[i][1] = words[0];
            if(lines[i].trim().endsWith("functional")){
                data[i][3] = new Boolean(true);
                if(words.length == 3){
                    data[i][2] = words[1];
                    setAsManualVerify(i, true);
                    setAsCorrectSyntax(i, true);
                }
                else{
                    data[i][2] = "";
                }
            }
            else{
                data[i][3] = new Boolean(false);
                if(words.length >= 2){
                    data[i][2] = words[1];
                    setAsManualVerify(i, true);
                    setAsCorrectSyntax(i, true);
                }
                else{
                    data[i][2] = "";
                }
            }
       }
    }
    
    /**
     * Load transcription from HashMap 
     * @param wordList
     * @throws Exception
     */
    public void loadTranscription(HashMap<String, Integer> wordList) throws Exception{
        
        int length = wordList.size();
        this.data  =  new Object[length][4];
        this.hasManualVerification  =  new boolean[length];
        this.hasCorrectSyntax  =  new boolean[length];
        Iterator<String> it = wordList.keySet().iterator();
        for(int i=0; it.hasNext(); i++){
            data[i][0] = (new Integer(i)).toString();
            data[i][1] = (String) it.next(); //wordList.get(i);
            data[i][2] = "";
            data[i][3] = new Boolean(false);
            setAsManualVerify(i, false);
            setAsCorrectSyntax(i, true);
                       
        }
        
    }
    
    /**
     * Save user entered and verified transcription in to lexicon format 
     * @param fileName
     * @throws IOException
     */
    public void saveSampaLexiconFormat(String fileName) throws IOException{
        if(!hasLexiconData()) return;
        PrintWriter out = new PrintWriter(new FileWriter(fileName));
        for(int i=0; i < data.length; i++){
            
            String line;
            if(!((String)data[i][2]).equals("") && this.hasManualVerification[i] && this.hasCorrectSyntax[i]){
                line =  (String) data[i][1];
                //line += "\\"+(String)data[i][2]+"\\";
                line += "|"+(String)data[i][2];
                out.println(line);
            }
        }
        out.flush();
        out.close();
    }
    
    /**
     * Save all functional words into text file
     * @param fileName
     * @throws IOException
     */
    public void saveFunctionalWords(String fileName) throws IOException{
        if(!hasFunctionalData()) return;
        PrintWriter out = new PrintWriter(new FileWriter(fileName));
        for(int i=0; i < data.length; i++){
            if((Boolean)data[i][3]){
                out.println((String) data[i][1] + "|functional");
            }
        }
        out.flush();
        out.close();
        
    }
    
    public boolean hasFunctionalData(){
        int countData = 0;
        for(int i=0; i < data.length; i++){
            if((Boolean)data[i][3]){
                countData++;
            }
        }
        if(countData == 0) return false;
        else return true;
    }

    public boolean hasLexiconData(){
        int countData = 0;
        for(int i=0; i < data.length; i++){
            if(!((String)data[i][2]).equals("") && this.hasManualVerification[i] && this.hasCorrectSyntax[i]){
                countData++;
            }
        }
        if(countData == 0) return false;
        else return true;
    }
    
    
    public void testFST(String fstFilename) throws IOException{
        FSTLookup fst = new FSTLookup(fstFilename);
        String key = "Item";
        String[] result = fst.lookup(key);
        if(result == null) System.out.println("Not available");
        System.out.println("Result Len: " + result.length);
        for(int i=0;i<result.length;i++){
            System.out.print("Available: ");
            System.out.print(result[i]);
            System.out.println();
        }
        //System.out.println(" ************** ");
        key = "Spieltags";
        result = fst.lookup(key);
        if(result == null) System.out.println("Not available");
        System.out.println("Result Len: " + result.length);
        for(int i=0;i<result.length;i++){
            System.out.print("Available: ");
            System.out.print(result[i]);
            System.out.println();
        }
    }
    
    public void createPOSFst(String posFilename, String fstFilename) throws Exception{
        
        if(!hasFunctionalData()) return;
        
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(posFilename), "UTF-8"));
        AlignerTrainer at = new AlignerTrainer(false, true);
        at.readLexicon(br, "\\s*\\|\\s*");
        br.close();

        // make some alignment iterations
        for ( int i = 0 ; i < 4 ; i++ ){
            at.alignIteration();
        }
        TransducerTrie t = new TransducerTrie();
        for (int i = 0, size = at.lexiconSize(); i<size; i++){
            t.add(at.getAlignment(i));
            t.add(at.getInfoAlignment(i));
        }
        t.computeMinimization();
        File of = new File(fstFilename);
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
        t.writeFST(os,"UTF-8");
        os.flush();
        os.close();
        //testFST(fstFilename);    
    }
    
     /**
      * Creates lexicon in FST format and letter-to-sound models
      * @param lexiconFilename
      * @param fstFilename
      * @throws Exception
      */
    public void createLexicon(String lexiconFilename, String fstFilename) throws Exception{
        
        if(!hasLexiconData()) return;
        
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), "UTF-8"));
        AlignerTrainer at = new AlignerTrainer(false, true);
        at.readLexicon(br, "\\s*\\|\\s*");
        br.close();

        // make some alignment iterations
        for ( int i = 0 ; i < 4 ; i++ ){
            at.alignIteration();
        }
        TransducerTrie t = new TransducerTrie();
        for (int i = 0, size = at.lexiconSize(); i<size; i++){
            t.add(at.getAlignment(i));
            t.add(at.getInfoAlignment(i));
        }
        t.computeMinimization();
        File of = new File(fstFilename);
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
        t.writeFST(os,"UTF-8");
        os.flush();
        os.close();
        //testFST(fstFilename);
    }
    
    /**
     * get column count
     */
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * get row count
     */
    public int getRowCount() {
        return data.length;
    }

    /**
     * get column name
     */
    public String getColumnName(int col) {
        return columnNames[col];
    }

    /**
     * get value at given location
     */
    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box.
     */
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    /*
     * Don't need to implement this method unless your table's
     * editable.
     */
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        if (col < editableColumns) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    public void setValueAt(Object value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }

}