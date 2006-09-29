package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
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
        int guiReturn = SKIP;
        boolean removeAll = false;
        boolean skipAll = false;
        for (Iterator it = problems.keySet().iterator(); it.hasNext(); ) {
            String basename = (String) it.next();
            String errorMessage;
            boolean tryAgain;
            do {
                System.out.print("    "+basename+": ");
                
                /* Let the user make a first correction */
                if ( (!removeAll) && (!skipAll) ) // These may be set true after a previous call to letUserCorrect()
                    guiReturn = letUserCorrect(basename, (String)problems.get(basename));
                /* Check if an error remains */
                errorMessage = verifyAlignment(basename);
                /* If there is no error, proceed with the next file. */
                if (errorMessage == null) {
                    System.out.println("OK");
                    remainingProblems--;
                    tryAgain = false;
                }
                /* If the error message is (still) not null, print the error and manage the GUI return code: */
                else {
                    System.out.print(errorMessage);
                    //problems.put(basename, errorMessage);
                    /* Manage the error according to the GUI return: */
                    switch ( guiReturn ) {
                    case TRYAGAIN:
                        tryAgain = true;
                        break;
                        
                    case SKIP:
                        tryAgain = false;
                        System.out.println( " -> Skipping this utterance ! This problem remains." );
                        break;
                        
                    case SKIPALL:
                        tryAgain = false;
                        skipAll = true;
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
                        break;
                        
                    default:
                        throw new RuntimeException( "The letUserCorrect() GUI returned an unknown return code." );
                    }
                    /* Additional management for the skipAll and removeAll options: */
                    if (skipAll ) {
                        System.out.println( " -> Skipping this utterance ! This problem remains." );
                        continue;
                    }
                    if (removeAll) {
                        bnl.remove( basename );
                        remainingProblems--;
                        System.out.println( " -> Removed from the utterance list. OK" );
                        continue;
                    }
                }
                
            } while (tryAgain );
        }
        
        System.out.println( "Removed [" + (bnlLengthIn-bnl.getLength()) + "/" + bnlLengthIn
                + "] utterances from the list, [" + bnl.getLength() + "] utterances remain,"+
                " among which [" + remainingProblems + "/" + bnl.getLength() + "] still have problems." );
        
        return remainingProblems == 0; // true exactly if all problems have been solved
    }
    
    /**
     * Verify if the feature and label files for basename align OK.
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
        while (correct) {
            String labelUnit = getLabelUnit(labels);
            String featureUnit = getFeatureUnit(features);
            // when featureUnit is the empty string, we have found an empty line == end of feature section
            if ("".equals(featureUnit)) break;
            if (!featureUnit.equals(labelUnit)) {
                return "Non-matching units found: feature file '"+featureUnit+"' vs. label file '"+labelUnit+"'";
            }
        }
        return null; // success
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
            String text = FileUtils.getFileAsString(new File( db.txtDirName() + basename + db.txtDirName() ), "UTF-8");
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
            frame.setLayout( gridBagLayout );

            final JEditorPane editPane = new JEditorPane();
            editPane.setPreferredSize(new Dimension(500, 500));
            editPane.read(new InputStreamReader(new FileInputStream(file), "UTF-8"), null);
            JButton saveButton = new JButton("Save & Exit");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                        editPane.write(pw);
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
            frame.add(scrollPane);
            gridC.gridy = 1;
            // do not resize buttons:
            gridC.weightx = 0;
            gridC.weighty = 0;
            JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new FlowLayout());
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            gridBagLayout.setConstraints( buttonPanel, gridC );
            frame.add(buttonPanel);
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
}
