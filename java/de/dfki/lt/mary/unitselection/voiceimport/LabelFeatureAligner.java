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
    protected Map problems;
    protected boolean correctedPauses = false;
    
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
        problems = new TreeMap();
        
        for (int i=0; i<bnl.getLength(); i++) {
            percent = 100*i/bnl.getLength();
            //call firstVerifyAlignment for first alignment test
            String errorMessage = verifyAlignment(bnl.getName(i));
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
        
        if (remainingProblems>0){
            //show option for automatically correcting pauses
            correctPausesYesNo(remainingProblems);
            remainingProblems = problems.keySet().size();
        }
                
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
     * Let the user select if he wants to run the
     * the automatic correction of pauses.
     * @param numProblems the number of problems
     * @throws IOException
     */
    protected void correctPausesYesNo(int numProblems) throws IOException
    {
        int choice = JOptionPane.showOptionDialog(null,
                "Found "+numProblems+" problems. Automatically correct pauses?",
                "Automatic pause correction",
                JOptionPane.YES_NO_CANCEL_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null,
                new String[] {"Yes", "No"},
                null);
        
        if (choice == 0) 
            correctPauses();
    }
    
    /**
     * Try to automatically correct misalignment caused 
     * by pauses: 
     * If there is a pause in the label file and not in the 
     * feature file, it is removed in the label file.
     * If there is a pause in the feature file and not in
     * the label file, a pause of length zero is inserted
     * in the label file
     * @param basename
     * @return null if the alignment was OK, or a String containing an error message.
     * @throws IOException
     */
    protected void correctPauses() throws IOException
    {
        correctedPauses = true;
        //clear the list of problems
        problems = new TreeMap();
        //go through all files
        for (int l=0; l<bnl.getLength(); l++) {
            String basename = bnl.getName(l);
            System.out.print( "    " + basename );
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
        
        	BufferedReader features;    
        	try {
                 features = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.unitFeaDirName() + basename + db.unitFeaExt() )), "UTF-8"));
            }catch (FileNotFoundException fnfe){
                 return;
            }
        	while ((line = features.readLine()) != null) {
        	    if (line.trim().equals("")) break; // empty line marks end of header
        	}

        	//store text units of feature file in list
        	List featureUnits = new ArrayList();
        	while ((line = features.readLine()) != null) {
        	    if (line.trim().equals("")) break; // empty line marks end of section
        	    featureUnits.add(line);
        	}
        
        	ArrayList labelUnitData;        
        	String labelUnit;
        	String featureUnit;
        	String returnString = null;
	        
	        int numLabelUnits = labelUnits.size();
	        int numFeatureUnits = featureUnits.size();
       
        
	        int i=0,j=0;
	        while (i<numLabelUnits && j<numFeatureUnits){
	            //System.out.println("featureUnit : "+featureUnit
	            //      +" labelUnit : "+labelUnit);
	            labelUnitData = getLabelUnitData((String)labelUnits.get(i));
	            labelUnit = (String) labelUnitData.get(2);
	            featureUnit = getFeatureUnit((String)featureUnits.get(j));
	            
	            if (!featureUnit.equals(labelUnit)) {
                
	                if (featureUnit.equals("_")){
	                    //add pause in labels
	                    System.out.println(" Adding pause unit in labels before unit "+i);
	                    String pauseUnit = (String)labelUnitData.get(0)+" "
                    			+(String) labelUnitData.get(1)+" _\n";
                    		
	                    labelUnits.add(i,pauseUnit);
	                    i++;
	                    j++;
	                    numLabelUnits =labelUnits.size();
	                    continue;
	                } else {
	                    if (featureUnit.equals("__L")){
	                        //add two pause units in labels
	                        System.out.println(" Adding pause units in labels before unit "
                                +i);
	                        String pauseUnit = (String)labelUnitData.get(0)+" "
	                        	+(String) labelUnitData.get(1)+" __L\n";
            		
	                        labelUnits.add(i,pauseUnit);
	                        i++;
	                        pauseUnit = (String)labelUnitData.get(0)+" "
	                        	+(String) labelUnitData.get(1)+" __R\n";
	                        labelUnits.add(i,pauseUnit);
	                        i++;
	                        j+=2;
	                        numLabelUnits =labelUnits.size();
	                        continue;
	                    } else {
	                        if (labelUnit.equals("_")){
	    	                    //remove pause in labels
	    	                    System.out.println(" Removing pause unit in labels at index "+i);                        		
	    	                    labelUnits.remove(i);
	    	                    numLabelUnits=labelUnits.size();
	    	                    continue;
	    	                } else {
	    	                    if (labelUnit.equals("__L")){
	    	                        //remove two pause units in labels
	    	                        System.out.println(" Removing pause units in labels at index "
	                                    +i);
	    	                        //lengthen the unit before the pause
	    	                        ArrayList previousUnitData = 
	    	                            getLabelUnitData((String)labelUnits.get(i-1));
	    	                        labelUnits.set(i-1,(String)labelUnitData.get(0)
	    	                                +" "+(String)previousUnitData.get(1)
	    	                                +" "+(String)previousUnitData.get(2)+"\n");
	    	                        //remove the pauses
	    	                        labelUnits.remove(i);
	    	                        labelUnits.remove(i);
		    	                    numLabelUnits=labelUnits.size();
	    	                        continue;
	    	                    } else {
	    	                        //truely not matching
	    	                        if (returnString == null){
	    	                            //only remember the the first mismatch
	    	                            int unitIndex = i-1;
	    	                            returnString = " Non-matching units found: feature file '"
	    	                                +featureUnit+"' vs. label file '"+labelUnit
	    	                                +"' (Unit "+unitIndex+")";
	    	                        }
	    	                    }
	    	                }
	                    }   
                    
	                }
	            }
	            //increase both counters if you did not delete a pause
	            i++;
	            j++;            
	        }
	        //return an error if label file is longer than feature file
	        if (returnString == null && numLabelUnits > numFeatureUnits){
	            returnString = " Label file is longer than feature file: "
	                +" unit "+numFeatureUnits
	                +" and greater do not exist in feature file";  
	        }
        
        
	        //now overwrite the label file 
	        PrintWriter labelFileWriter =
	            new PrintWriter(
                    new FileWriter(
                            new File( db.unitLabDirName() + basename + db.unitLabExt() )));
	        //print header
	        labelFileWriter.print(labelFileHeader.toString());
	        //print units
	        numLabelUnits = labelUnits.size();
	        for (int k=0;k<numLabelUnits;k++){
	            String nextUnit = (String)labelUnits.get(k);
	            if (nextUnit != null){
	                //correct the unit index
	                ArrayList nextUnitData = getLabelUnitData(nextUnit);
	                labelFileWriter.print((String)nextUnitData.get(0)
	                        +" "+k+" "+(String) nextUnitData.get(2)+"\n");
	            }
	        }
	        
	        labelFileWriter.flush();
	        labelFileWriter.close();
        
	        //returnString is null if all units matched,
	        //otherwise the first error is given back
	        if (returnString == null) {
         	   System.out.println(" OK");
	        } else {
	            problems.put( basename, returnString);
	            System.out.println(returnString);
	        }
        }
        System.out.println("Remaining problems: "+problems.size());
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
        BufferedReader features; 
        try {
            features = new BufferedReader(new InputStreamReader(new FileInputStream(new File( db.unitFeaDirName() + basename + db.unitFeaExt() )), "UTF-8"));
        } catch (FileNotFoundException fnfe){
            return "No feature file";
        }
        
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
        int unitIndex = -1;
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
    
   
    
    private ArrayList getLabelUnitData(String line)
    throws IOException
    {
        if (line == null) return null;
        ArrayList unitData = new ArrayList();
        StringTokenizer st = new StringTokenizer(line.trim());
        // The third token in each line is the label
        unitData.add(st.nextToken()); 
        unitData.add(st.nextToken());
        unitData.add(st.nextToken());
        return unitData;
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
        String[] options;
        if (correctedPauses){
            options = new String[] {"Edit RAWMARYXML", "Edit unit labels", "Remove from list", "Remove all problems", "Skip", "Skip all","Replace labels in unit file"};
        } else {
            options = new String[] {"Edit RAWMARYXML", "Edit unit labels", "Remove from list", "Remove all problems", "Skip", "Skip all"};
        }
        int choice = JOptionPane.showOptionDialog(null,
                "Misalignment problem for "+basename+":\n"+
                errorMessage,
                "Correct alignment for "+basename,
                JOptionPane.YES_NO_CANCEL_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null,
                options,
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
        case 6:
            if (correctedPauses){
                replaceUnitLabels(basename);
            }
            return TRYAGAIN;
        default: // JOptionPane.CLOSED_OPTION
            return SKIP; // don't verify again.
        }
    }
    
    /**
     * Replace all label units which do not match the feature units
     * with the feature units
     * This method should only be called after automatic pause alignment.
     * @param basename the filename of the label/feature file
     * @throws IOException
     */
    private void replaceUnitLabels(String basename) throws IOException
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
        	while ((line = features.readLine()) != null) {
        	    if (line.trim().equals("")) break; // empty line marks end of header
        	}

        	//store text units of feature file in list
        	List featureTextUnits = new ArrayList();
        	while ((line = features.readLine()) != null) {
        	    if (line.trim().equals("")) break; // empty line marks end of section
        	    featureTextUnits.add(line);
        	}
        
        	ArrayList labelUnitData;        
        	String labelUnit;
        	String featureUnit;
        	String returnString = null;
	        
	        int numLabelUnits = labelUnits.size();
	        int numFeatureUnits = featureTextUnits.size();
       
        
	        int i=0,j=0;
	        while (i<numLabelUnits && j<numFeatureUnits){
	            //System.out.println("featureUnit : "+featureUnit
	            //      +" labelUnit : "+labelUnit);
	            labelUnitData = getLabelUnitData((String)labelUnits.get(i));
	            labelUnit = (String) labelUnitData.get(2);
	            featureUnit = getFeatureUnit((String)featureTextUnits.get(j));
	            
	            if (!featureUnit.equals(labelUnit)) {
                	//take over label of feature file
	    	        labelUnits.set(i,(String)labelUnitData.get(0)
	    	                +" "+(String)labelUnitData.get(1)
	    	                +" "+featureUnit+"\n");
	            }
	            i++;
	            j++;            
	        }
	        
	        //now overwrite the label file 
	        PrintWriter labelFileWriter =
	            new PrintWriter(
                    new FileWriter(
                            new File( db.unitLabDirName() + basename + db.unitLabExt() )));
	        //print header
	        labelFileWriter.print(labelFileHeader.toString());
	        //print units
	        numLabelUnits = labelUnits.size();
	        for (int k=0;k<numLabelUnits;k++){
	            String nextUnit = (String)labelUnits.get(k);
	            if (nextUnit != null){
	                //correct the unit index
	                ArrayList nextUnitData = getLabelUnitData(nextUnit);
	                labelFileWriter.print((String)nextUnitData.get(0)
	                        +" "+k+" "+(String) nextUnitData.get(2)+"\n");
	            }
	        }
	        
	        labelFileWriter.flush();
	        labelFileWriter.close();
        
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
