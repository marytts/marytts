package de.dfki.lt.mary.unitselection.voiceimport;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.dfki.lt.mary.util.FileUtils;

/**
 * Compare unit label and unit feature files.
 * If they don't align, flag a problem; let the user
 * decide how to fix it -- either by editing the unit label
 * file or by editing a rawmaryxml file and recomputing the
 * features file.
 * @author schroed
 *
 */
public class LabelFeatureAligner implements VoiceImportComponent
{
    protected File unitlabelDir;
    protected File unitfeatureDir;
    protected UnitFeatureComputer featureComputer;
    protected String pauseSymbol;
    
    protected DatabaseLayout db = null;
    protected BasenameList bnl = null;
    protected int percent = 0;
    
    protected static final int TRYAGAIN = 0;
    protected static final int SKIP = 1;
    protected static final int SKIPALL = 2;
    protected static final int REMOVE = 3;
    protected static final int REMOVEALL = 4;
    
    public LabelFeatureAligner( DatabaseLayout setdb, BasenameList setbnl ) throws IOException
    {
        this.db = setdb;
        this.bnl = setbnl;
    
        this.unitlabelDir = new File( db.unitLabDirName() );
        this.unitfeatureDir = new File( db.unitFeaDirName() );
        this.featureComputer = new UnitFeatureComputer( db, bnl );
        this.pauseSymbol = System.getProperty("pause.symbol", "pau");
    }
    
    /**
     * Align labels and features. For each .unitlab file in the unit label
     * directory, verify whether the chain of units given is identical to
     * the chain of units in the corresponding unit feature file.
     * For those files that are not perfectly aligned, give the user the
     * opportunity to correct alignment.
     * @return a boolean indicating whether or not the database is fully aligned.
     * @throws IOException
     */
    public boolean compute() throws IOException
    {
        int bnlLengthIn = bnl.getLength();
        System.out.println( "Verifying feature-label alignment for "+ bnlLengthIn + " utterances." );
        Map problems = new TreeMap();
        
        for (int i=0; i<bnl.getLength(); i++) {
            percent = 100*i/bnl.getLength();
            //call firstVerifyAlignment for first alignment test
            String errorMessage = firstVerifyAlignment(bnl.getName(i));
            System.out.print( "    " + bnl.getName(i) );
            if (errorMessage == null) {
                System.out.println(" OK");
            } else {
                problems.put( bnl.getName(i), errorMessage);
                System.out.println(errorMessage);
            }
        }
        System.out.println("Found "+problems.size() + " problems");

        int remainingProblems = problems.keySet().size();
        int guiReturn = SKIP;
        boolean removeAll = false;
        boolean skipAll = false;
        boolean tryAgain = true;
        for (Iterator it = problems.keySet().iterator(); it.hasNext(); ) {
            String basename = (String) it.next();
            String errorMessage;
            if ( !(removeAll || skipAll) ){ // These may be set true after a previous call to letUserCorrect()
                do {
                    errorMessage = (String)problems.get(basename);
                    System.out.println("    "+basename+": "+errorMessage);
                    /* Let the user make a first correction */
                    guiReturn = letUserCorrect(basename, errorMessage);
                    /* Check if an error remains */
                    errorMessage = verifyAlignment(basename);
                    /* If there is no error, proceed with the next file. */
                    if (errorMessage == null) {
                        System.out.println(" -> OK");
                        remainingProblems--;
                        tryAgain = false;
                    }
                    /* If the error message is (still) not null, manage the GUI return code: */
                    else {
                        problems.put(basename, errorMessage);
                        /* Manage the error according to the GUI return: */
                        switch ( guiReturn ) {
                    
                        case TRYAGAIN:
                            tryAgain = true;
                            break;
                        
                        case SKIP:
                            tryAgain = false;
                            System.out.println( " -> Skipped this utterance ! This problem remains." );
                            break;
                        
                        case SKIPALL:
                            tryAgain = false;
                            skipAll = true;
                            System.out.println( " -> Skipping all utterances ! The problems remain." );
                            break;
                        
                        case REMOVE:
                            tryAgain = false;
                            bnl.remove( basename );
                            remainingProblems--;
                            System.out.println( " -> Removed from the utterance list. OK" );
                            break;
                        
                        case REMOVEALL:
                            tryAgain = false;
                            removeAll = true;
                            System.out.println(" -> Removing all problematic utterances. OK" );
                            break;
                        
                        default:
                            throw new RuntimeException( "The letUserCorrect() GUI returned an unknown return code." );
                        }
                    }
                } while (tryAgain );
                
            } 
            
            /* Additional management for the removeAll option: */        
            if (removeAll) {
                bnl.remove( basename );
                remainingProblems--;
            }
        }
        
        
        System.out.println( "Removed [" + (bnlLengthIn-bnl.getLength()) + "/" + bnlLengthIn
                + "] utterances from the list, [" + bnl.getLength() + "] utterances remain,"+
                " among which [" + remainingProblems + "/" + bnl.getLength() + "] still have problems." );
        
        return remainingProblems == 0; // true exactly if all problems have been solved
    }
    
    /**
     * Verify if the feature and label files for basename align OK.
     * This method tries to automatically correct misalignment caused 
     * by more pauses in the feature file.
     * It should only be called once; for successive alignment calls the 
     * method verifyAlignment should be used
     * @param basename
     * @return null if the alignment was OK, or a String containing an error message.
     * @throws IOException
     */
    protected String firstVerifyAlignment(String basename) throws IOException
    {
        String line;
        
        BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.unitLabDirName() + basename + db.unitLabExt() )), "UTF-8"));
        //store header of label file in StringBuffer
        StringBuffer labelFileHeader = new StringBuffer();
        while ((line = labels.readLine()) != null) {
            labelFileHeader.append(line+"\n");
            if (line.startsWith("#")) break; // line starting with "#" marks end of header
        }
        
        //store units of label file in List
        List labelUnits = new ArrayList();
        while ((line = labels.readLine()) != null) {
            labelUnits.add(line+"\n");
        }
        
        BufferedReader features = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.unitFeaDirName() + basename + db.unitFeaExt() )), "UTF-8"));    
        //store header of feature file in StringBuffer
        StringBuffer featureFileHeader = new StringBuffer();
        while ((line = features.readLine()) != null) {
            if (line.trim().equals("")) break; // empty line marks end of header
            featureFileHeader.append(line+"\n");
        }

        //store text units of feature file in list
        List featureTextUnits = new ArrayList();
        while ((line = features.readLine()) != null) {
            if (line.trim().equals("")) break; // empty line marks end of section
            featureTextUnits.add(line);
        }
        
        //store binary units of feature file in list
        List featureBinUnits = new ArrayList();
        while ((line = features.readLine()) != null) {
            featureBinUnits.add(line);
        }
        
        String labelUnit = getLabelUnit((String)labelUnits.get(0));
        String featureUnit = getFeatureUnit((String)featureTextUnits.get(0));
        String returnString = null;
        int unitIndex = 0;
        int numLabelUnits = labelUnits.size();
        int numFeatureUnits = featureTextUnits.size();
        
        if (!labelUnit.equals(featureUnit)){
            System.out.println("Inserting pause units at start of feature file");
            //insert a pause as first unit to the feature units
            if (labelUnit.equals("_") ){
                //copy the last unit
                String lastUnit = (String)featureTextUnits.get(numFeatureUnits-1);
                featureTextUnits.add(0,lastUnit);
                lastUnit = (String)featureBinUnits.get(numFeatureUnits-1);
                featureBinUnits.add(0,lastUnit);
                numFeatureUnits++;
            } else {
                if (labelUnit.equals("__L")){
                    //copy the two last units
                    String lastLeftUnit = (String)featureTextUnits.get(numFeatureUnits-2);
                    featureTextUnits.add(0,lastLeftUnit);
                    lastLeftUnit = (String)featureBinUnits.get(numFeatureUnits-2);
                    featureBinUnits.add(0,lastLeftUnit);
                    String lastRightUnit = (String)featureTextUnits.get(numFeatureUnits);
                    featureTextUnits.add(1,lastRightUnit);
                    lastRightUnit = (String)featureBinUnits.get(numFeatureUnits);
                    featureBinUnits.add(1,lastRightUnit);
                    numFeatureUnits+=2;
                }
            }
        }
        
        int i=0,j=0;
        while (i<numLabelUnits && j<numFeatureUnits){
            //System.out.println("featureUnit : "+featureUnit
              //      +" labelUnit : "+labelUnit);
            labelUnit = getLabelUnit((String)labelUnits.get(i));
            featureUnit = getFeatureUnit((String)featureTextUnits.get(j));
            unitIndex++;
            if (!featureUnit.equals(labelUnit)) {
                /**
                if (featureUnit.equals("_")){
                    //unnecessary pause unit in features, delete
                    System.out.println("Deleting unnecessary pause unit "+unitIndex);
                    featureTextUnits.set(j,null);
                    featureBinUnits.set(j,null);
                    j++;                   
                    continue;
                } else {
                    if (featureUnit.equals("__L")){
                        //two unnecessary pause units in features, delete
                        System.out.println("Deleting unnecessary pause units "+unitIndex
                                +" and "+unitIndex+1);
                        featureTextUnits.set(j,null);
                        featureBinUnits.set(j,null);
                        j++;
                        featureTextUnits.set(j,null);
                        featureBinUnits.set(j,null);
                        j++;
                        continue;
                    } else {**/
                        
                        //truely not matching
                        if (returnString == null){
                            //only remember the the first mismatch
                            returnString = "Non-matching units found: feature file '"
                                +featureUnit+"' vs. label file '"+labelUnit
                                +"' (Unit "+unitIndex+")";
                        }
                   // }   
                    
                //}
            }
            //increase both counters if you did not delete a pause
            i++;
            j++;            
        }
        //return an error if label file is longer than feature file
        if (returnString == null && numLabelUnits > numFeatureUnits){
            returnString = "Label file is longer than feature file: "
                +" unit "+unitIndex
                +" and greater do not exist in feature file";  
        }
        
        
        //now overwrite the feature file 
        PrintWriter featureFileWriter =
            new PrintWriter(
                    new FileWriter(
                            new File( db.unitFeaDirName() + basename + db.unitFeaExt() )));
        //print header
        featureFileWriter.print(featureFileHeader.toString()+"\n");
        //print text units
        for (int k=0;k<numFeatureUnits;k++){
            String nextUnit = (String)featureTextUnits.get(k);
            if (nextUnit != null){
                featureFileWriter.print(nextUnit+"\n");
            }
        }
        //print binary units
        featureFileWriter.print("\n");
        for (int k=0;k<numFeatureUnits;k++){
            String nextUnit = (String)featureBinUnits.get(k);
            if (nextUnit != null){
                featureFileWriter.print(nextUnit+"\n");
            }
        }
        featureFileWriter.flush();
        featureFileWriter.close();
        
        //returnString is null if all units matched,
        //otherwise the first error is given back
        return returnString;         
        
    }
    
    /**
     * Verify if the feature and label files for basename align OK.
     * This method should be called after firstVerifyAlignment
     * for subsequent alignment tries.
     * @param basename
     * @return null if the alignment was OK, or a String containing an error message.
     * @throws IOException
     */
    protected String verifyAlignment(String basename) throws IOException
    {
        BufferedReader labels = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.unitLabDirName() + basename + db.unitLabExt() )), "UTF-8"));
        BufferedReader features = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.unitFeaDirName() + basename + db.unitFeaExt() )), "UTF-8"));
        String line;
        // Skip label file header:
        while ((line = labels.readLine()) != null) {
            if (line.startsWith("#")) break; // line starting with "#" marks end of header
        }
        // Skip features file header:
        while ((line = features.readLine()) != null) {
            if (line.trim().equals("")) break; // empty line marks end of header
        }
        
        // Now go through all feature file units
        boolean correct = true;
        int unitIndex = 0;
        while (correct) {
            unitIndex++;
            String labelUnit = getLabelUnit(labels);
            String featureUnit = getFeatureUnit(features);
            if (featureUnit == null) throw new IOException("Incomplete feature file: "+basename);
            // when featureUnit is the empty string, we have found an empty line == end of feature section
            if ("".equals(featureUnit)){
               if (labelUnit == null){
                   break;
               } else {
                   return "Label file is longer than feature file: "
                   +" unit "+unitIndex
                   +" and greater do not exist in feature file";  
               }
            }
            if (!featureUnit.equals(labelUnit)) {
                return "Non-matching units found: feature file '"
                +featureUnit+"' vs. label file '"+labelUnit
                +"' (Unit "+unitIndex+")";
            }
        }
        return null; // success
    }
    
    private String getLabelUnit(String line)
    throws IOException
    {
        if (line == null) return null;
        StringTokenizer st = new StringTokenizer(line.trim());
        // The third token in each line is the label
        st.nextToken(); st.nextToken();
        String unit = st.nextToken();
        return unit;
    }
    
    private String getFeatureUnit(String line)
    throws IOException
    {
        if (line == null) return null;
        if (line.trim().equals("")) return ""; // empty line -- signal end of section
        StringTokenizer st = new StringTokenizer(line.trim());
        // The expect that the first token in each line is the label
        String unit = st.nextToken();
        return unit;
        
    }
    
    private String getLabelUnit(BufferedReader labelReader)
    throws IOException
    {
        String line = labelReader.readLine();
        if (line == null) return null;
        StringTokenizer st = new StringTokenizer(line.trim());
        // The third token in each line is the label
        st.nextToken(); st.nextToken();
        String unit = st.nextToken();
        return unit;
    }
    
    private String getFeatureUnit(BufferedReader featureReader)
    throws IOException
    {
        String line = featureReader.readLine();
        if (line == null) return null;
        if (line.trim().equals("")) return ""; // empty line -- signal end of section
        StringTokenizer st = new StringTokenizer(line.trim());
        // The expect that the first token in each line is the label
        String unit = st.nextToken();
        return unit;
        
    }
    
    protected int letUserCorrect(String basename, String errorMessage) throws IOException
    {
        int choice = JOptionPane.showOptionDialog(null,
                "Misalignment problem for "+basename+":\n"+
                errorMessage,
                "Correct alignment for "+basename,
                JOptionPane.YES_NO_CANCEL_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null,
                new String[] {"Edit RAWMARYXML", "Edit unit labels", "Remove utterance from list", "Remove all upcoming wrong", "Skip", "Skip all"},
                null);
        switch (choice) {
        case 0: 
            editMaryXML(basename);
            return TRYAGAIN;
        case 1:
            editUnitLabels(basename);
            return TRYAGAIN;
        case 2:
            return REMOVE;
        case 3:
            return REMOVEALL;
        case 4:
            return SKIP;
        case 5:
            return SKIPALL;
        default: // JOptionPane.CLOSED_OPTION
            return SKIP; // don't verify again.
        }
    }
    
    private void editMaryXML(String basename) throws IOException
    {
        final File maryxmlFile = new File( db.rmxDirName() + basename + db.rmxExt() );
        if (!maryxmlFile.exists()) {
            // need to create it
            String text = FileUtils.getFileAsString(new File( db.txtDirName() + basename + db.txtExt() ), "UTF-8");
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(maryxmlFile), "UTF-8"));
            pw.println(UnitFeatureComputer.getMaryXMLHeaderWithInitialBoundary(featureComputer.getLocale()));
            pw.println(text);
            pw.println("</maryxml>");
            pw.close();
        }
        boolean edited = new EditFrameShower(maryxmlFile).display();
        if (edited)
            featureComputer.computeFeaturesFor(basename);
    }

    
    private void editUnitLabels(String basename) throws IOException
    {
        new EditFrameShower(new File( db.unitLabDirName() + basename + db.unitLabExt() )).display();
    }

    public static void main(String[] args) throws IOException
    {
        boolean isAligned = new LabelFeatureAligner( null, null ).compute();
        System.out.println("The database is "+(isAligned?"":"NOT")+" perfectly aligned");
    }

    public static class EditFrameShower
    {
        protected final File file;
        protected boolean saved;
        public EditFrameShower(File file)
        {
            this.file = file;
            this.saved = false;
        }

        /**
         * Show a frame allowing the user to edit the file.
         * @param file the file to edit
         * @return a boolean indicating whether the file was saved.
         * @throws IOException
         * @throws UnsupportedEncodingException
         * @throws FileNotFoundException
         */
        public boolean display() throws IOException, UnsupportedEncodingException, FileNotFoundException
        {
            final JFrame frame = new JFrame("Edit "+file.getName());
            GridBagLayout gridBagLayout = new GridBagLayout();
            GridBagConstraints gridC = new GridBagConstraints();
            frame.getContentPane().setLayout( gridBagLayout );

            final JEditorPane editPane = new JEditorPane();
            editPane.setPreferredSize(new Dimension(500, 500));
            editPane.read(new InputStreamReader(new FileInputStream(file), "UTF-8"), null);
            JButton saveButton = new JButton("Save & Exit");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                        editPane.write(pw);
                        pw.flush();
                        pw.close();
                        frame.setVisible(false);
                        setSaved(true);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            });
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.setVisible(false);
                    setSaved(false);
                }
            });

            gridC.gridx = 0;
            gridC.gridy = 0;
            // resize scroll pane:
            gridC.weightx = 1;
            gridC.weighty = 1;
            gridC.fill = GridBagConstraints.HORIZONTAL;
            JScrollPane scrollPane = new JScrollPane(editPane);
            scrollPane.setPreferredSize(editPane.getPreferredSize());
            gridBagLayout.setConstraints( scrollPane, gridC );
            frame.getContentPane().add(scrollPane);
            gridC.gridy = 1;
            // do not resize buttons:
            gridC.weightx = 0;
            gridC.weighty = 0;
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            gridBagLayout.setConstraints( buttonPanel, gridC );
            frame.getContentPane().add(buttonPanel);
            frame.pack();
            frame.setVisible(true);
            do {
                try {
                    Thread.sleep(10); // OK, this is ugly, but I don't mind today...
                } catch (InterruptedException e) {}
            } while (frame.isVisible());
            frame.dispose();
            return saved;
        }
        
        protected void setSaved(boolean saved)
        {
            this.saved = saved;
        }

    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return percent;
    }

}
