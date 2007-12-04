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
package de.dfki.lt.mary.recsessionmgr.gui;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import de.dfki.lt.mary.recsessionmgr.Redstart;
import de.dfki.lt.mary.recsessionmgr.debug.Test;
import de.dfki.lt.mary.recsessionmgr.lib.Prompt;
import de.dfki.lt.mary.recsessionmgr.lib.RecSession;
import de.dfki.lt.mary.recsessionmgr.lib.Recording;
import de.dfki.lt.mary.recsessionmgr.lib.Speech;
import de.dfki.lt.signalproc.display.MultiDisplay;
import de.dfki.lt.signalproc.util.MonoAudioInputStream;

/**
 *
 * @author  Mat Wilson <mwilson@dfki.de>
 */
public class AdminWindow extends javax.swing.JFrame {
    
    // ______________________________________________________________________
    // Instance fields
        
    // ______________________________________________________________________
    // Class fields
        
    // Column indices for prompt set table
    protected static final int REC_STATUS_COLUMN = 0;     // First column is recording status 
    protected static final int BASENAME_COLUMN = 1;       // Second column is basename
    protected static final int PROMPT_TEXT_COLUMN = 2;    // Third collumn is prompt text       
    private JTable jTable_PromptSet;                      // Create our own table programmatically
    
    // Path for folder containing the voice - use AdminWindow.getVoiceFolderPath().getPath() to access externally
    private String voiceFolderPathString;              // e.g., "/project/mary/mat/voices/bundesliga"
    
    // Folder names (below the level of the voice folder)
    private static final String PROMPT_FOLDER_NAME = "/text/";
    private static final String REC_FOLDER_NAME = "/wav/";
    private static final String SYNTH_FOLDER_NAME = "/prompt_wav/";
    private static final String INSTRUCTIONS_FOLDER_NAME = "/instructions/";
      
    private static boolean beepDemoOn = true;                // Flag for toggling beep demo (clicking on status icon)
    
    // Forms and related fields
    private SpeakerWindow speakerWin = new SpeakerWindow();  // Speaker window instance
    private Options optionsDialog;
    
    // Boolean flags used for determining when buttons should be enabled and disabled
    private boolean playingStatus = false;                   // Boolean flag to indicate if playback is underway
    private boolean continuousMode = false;                  // Boolean flag to indicate if playback is underway
    private boolean recordingBlock = false;                  // Boolean flag to indicate if playback is part of a recording block
        
    // More objects    
    protected Prompt[] promptArray;                          // For storing the loaded prompt set
    private RecSession currentSession;                       // Recording session object
    private boolean stopPressed;                             // Internal setting, set to true by stop button
        
    private Font defaultPromptFont;
    
    // ______________________________________________________________________
    // Constructors
            
    /**
     * Creates new form AdminWindow and starts the recording session
     */
    public AdminWindow(String voiceFolderPathString) {
        this.voiceFolderPathString = voiceFolderPathString;
        
        optionsDialog = new Options(this);
        
        setSystemLookAndFeel();
        
        System.out.println("Initializing components...");  
        
        // GUI form setup
        initComponents();           // Initialize GUI components (auto-generated code)
        initMoreComponents();       // Initialize some components further (editable)

        setupVoice();
                
        Test.output("|AdminWindow| Ready for testing.");  // TESTCODE
    }

    private void setupVoice() {
        this.currentSession = createRecSession();  // Create new recording session
        System.out.println("Components initialized."); 
        buildPromptTable();    // Set up and fill the prompt table
        updateInstructions();  // Update instructions panel with instructions for current prompt set
        
        // PRI4 - Encapsulate this better; poor design but functional
        this.speakerWin.setupProgressBar(this.promptArray.length);
        this.speakerWin.updateProgressBar(1);  // Because we start at first prompt
        this.speakerWin.updatePromptCount(1);
        
        System.out.println("System is ready.");           
    }
    
    /** Updates the prompt table with prompt data
     *  @param newSession The current recording session object
     */
    private RecSession createRecSession() {
        
        RecSession newSession = null;
        try {
            newSession = new RecSession(this);
        } catch (FileNotFoundException ex) {
            
            String message = "File not found.";
            showMessage(message, true); // true = warning
            
            if (Test.isDebug) {
                ex.printStackTrace();
            }
            
        } catch (IOException ex) {
            
            String message = "File read error.";
            showMessage(message, true); // true = warning
            
            if (Test.isDebug) {
                ex.printStackTrace();
            }
            
        }
        Test.output("|AdminWindow| Recording session created.");  // TESTCODE
        
        return newSession;
    }
    
  
    /** Programmatically set the column widths of the prompt set table */
    private void setColumnWidths() {
        // only try to do this when we are visible
        if (!this.isVisible()) return;
        
        // Get columns from column model
        TableColumn recStatusColumn = jTable_PromptSet.getColumnModel().getColumn(0);
        TableColumn baseNameColumn = jTable_PromptSet.getColumnModel().getColumn(1);
        TableColumn promptColumn = jTable_PromptSet.getColumnModel().getColumn(2);

        // Get length of longest string in the column
        int widestBasename = LookAndFeel.getMaxColumnWidth(this.jTable_PromptSet, this.BASENAME_COLUMN);
                
        // These widths seem to be appropriate for the firs two columns - set them
        int recStatusColumnWidth = 50;  // Leave hardcoded since only three possible values
        int baseNameColumnWidth = widestBasename + 10;  // was 110 when hardcorded
        Test.output("Basename column width: " + widestBasename);
        recStatusColumn.setPreferredWidth(recStatusColumnWidth);
        baseNameColumn.setPreferredWidth(baseNameColumnWidth);
        
        // Now set prompt column to fill remaining width of the table
        int scrollbarBuffer = 3; // Buffer to avoid displaying a horizontal scrollbar
        int tableWidth = jScrollPane_PromptSet.getWidth();
        promptColumn.setPreferredWidth(tableWidth - (recStatusColumnWidth + baseNameColumnWidth + scrollbarBuffer));
        
    }
    
    /** Determines recording status given how many recordings a prompt has, as well as clipping status
     *  @param fileCount The number of files a prompt has
     *  @return The appropriate string, indicating the prompt's recording status
     */
    protected static String determineStatus(Recording rec) {
        String recStatus;
        String clipStatus;
        String combinedStatus;
        
        int fileCount = rec.getFileCount();
        
        // First determine recording status (number of recordings)
        if (fileCount == 0) {
            // Recording status is ""
            recStatus = "";
        }
        else if (fileCount == 1) {
            // Recording status is "R"
            recStatus = "R";  // "R" indicates 1 recording exists
        }
        else {
            // Recording status is "R+"
            recStatus = "R+";  // "R+" indicates >1 recording exists            
        }
        
        if (fileCount > 0) {
            // Now determine clipping status (audio or temporal clipping)
            rec.checkForAmpClipping();
            rec.checkForTempClipping();
        }
        
        // Determine message content related to clipping status
        if ((rec.isAmpClipped) || (rec.isTempClipped)) {
            clipStatus = "!";  // Show "!" if either amplitude or temporal clipping
        }
        else if (rec.isAmpWarning) {
            clipStatus = "?";  // Show "?" if amplitude clipping almost occurred (near threshold)
        }
        else {
            clipStatus = "";   // Otherwise no clipping of any sort
        }
        
        // Create the combined status
        combinedStatus = recStatus + clipStatus;
   
        Test.output("|AdminWindow.determineStatus| Combined status: " + combinedStatus);
        
        return combinedStatus;
    } 
    
    /** Public method to allow updating of instructions pane 
     *  @param session The current recording session, which has access to the instructions
     */
    public void updateInstructions() {
        
        // Get instructions and display them in instructions pane in Admin window
        try {
            
            String instructionsText = this.currentSession.getInstructions().getText();
            if (instructionsText != null) {
                jEditorPane_Instructions.setText(instructionsText);

                // Also update in Speaker window
                speakerWin.updateInstructions(instructionsText);
            }
        
        }
        catch (Exception ex) {
         
            String message = "No instructions, but you may continue.";
            showMessage(message, true); // true = warning
            
        }
        
    }    
    
    // Specifically for SpeakerWindow.java to determine if it should show prompt count
    public Options getOptions() {
        return this.optionsDialog;
    }
    
    public SpeakerWindow getSpeakerWindow()
    {
        return speakerWin;
    }
    
    /** Gets the currently selected row in the prompt set table */
    public int getCurrentRow() { return jTable_PromptSet.getSelectedRow(); }
    
    /** Displays the prompt text in the prompt display pane */
    private void displayPromptText() {
        
        Prompt prompt = promptArray[getCurrentRow()];  // Current prompt
        String promptText = prompt.getPromptText();
        Recording rec = prompt.getRecording();  // Most recent recording for selected prompt
        
        jTextPane_PromptDisplay.setFont(defaultPromptFont);
        if (this.isVisible())
            LookAndFeel.centerPromptText(jTextPane_PromptDisplay, promptText);
      
        // Also update in Speaker window
        this.speakerWin.updatePromptDisplay(promptText);
        int promptNumber = getCurrentRow() + 1;
        this.speakerWin.updateProgressBar(promptNumber);
        this.speakerWin.updatePromptCount(promptNumber);
        
        // Display number of recordings in the message bar
        int recCount = prompt.getRecCount();
        String inflectedRecordings = " recordings";
        if (recCount == 1) {
            inflectedRecordings = " recording";
        }
        
        String clippingMessage = "";  // No clipping messasge (changed below if clipping)
        boolean warning = false;      // Warning flag to show correct icon (false = info; true = warning)
        
        // If a recording exists
        if (recCount > 0) {
            
            // Enable play button
            jButton_Play.setEnabled(true);
            jButton_Display.setEnabled(true);
            
            // Get clipping status of most recent recording
            clippingMessage = getClippingMessage(rec);
            warning = rec.isAmpClipped || rec.isTempClipped; // Set warning level accordingly      
            
        } else {
            jButton_Play.setEnabled(false);  // Disable Play button if no recording to play
            jButton_Display.setEnabled(false);
        }
        
        
        String message = "Ready: " + prompt.getBasename() +
            " selected (" + recCount + inflectedRecordings + "). " + clippingMessage;
        showMessage(message, warning); // false = info, not a warning; true = warning (clipping)
        
    }
    
    /** Further initialization of components on startup
     *  (These can be edited in NetBeans, initComponents() cannot)
     */
    private void initMoreComponents() {
        
        // Center window in the user's screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();
        setLocation(new Point((screenSize.width - frameSize.width) / 2,
                              (screenSize.height - frameSize.height) / 2));
        
        // Set application icon in title bar left-hand corner
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(IconSet.LOGO_16x16_URL)); 
        
        // Change status icon in Admin window
        jLabel_SessionStatus.setIcon(IconSet.STOP_48X48);
        
        defaultPromptFont = jTextPane_PromptDisplay.getFont();
        
        speakerWin.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt)
            {
                jCheckBoxMenuItem_SpeakerWindow.setSelected(false);
            }
        });
        
        optionsDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                speakerWin.showOrHidePromptCount();
            }
        });
    }
   
    private void buildPromptTable() {
        
        this.promptArray = this.currentSession.getPromptArray();
        
        System.out.println("Loading prompts...");
        Test.output("Array contains " + promptArray.length + " prompts.");
        
        // Make column names array
        String[] columnNames = new String[3];
        columnNames[REC_STATUS_COLUMN] = "Status";
        columnNames[BASENAME_COLUMN] = "Basename";
        columnNames[PROMPT_TEXT_COLUMN] = "Prompt Preview";
                
        // Now create the table itself        
        JTable table = new JTable(new PromptTableModel(promptArray, columnNames));        
        table.setColumnSelectionAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);        
               
        // Set alignment for the status colum to centered
        DefaultTableCellRenderer renderer = new ClippingColorRenderer();
        renderer.setHorizontalAlignment(JTextField.CENTER);
        table.getColumnModel().getColumn(REC_STATUS_COLUMN).setCellRenderer(renderer);
        
        // Set selection highlight colour to light blue
        table.setSelectionBackground(new java.awt.Color(153, 204, 255));
        
        // Add listeners
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                displayPromptText();
            }
        });

        // Store the table in an instance field accessible to the entire class
        this.jTable_PromptSet = table;
        
        Thread recordingStatusInitialiser = new Thread() {
            public void run() {
                updateAllRecStatus();
            }
        };
        recordingStatusInitialiser.start();
        
        // Display table in the appropriate component pane
        jScrollPane_PromptSet.setViewportView(table);
    
        table.setRowSelectionInterval(0, 0);  // Show first row of prompt table as selected               
        displayPromptText();  // Display the prompt text for the first prompt in the prompt display pane 
        setColumnWidths();

        System.out.println("Total " + table.getRowCount() + " prompts loaded.");
        
        
    }
    
    private void updateAllRecStatus()
    {
        for (int row = 0; row < promptArray.length; row++) {
            String recStatus = determineStatus(promptArray[row].getRecording());
            // PRI2 Assumes saturation status is false; needs to actually check
            jTable_PromptSet.setValueAt(recStatus, row, REC_STATUS_COLUMN);
        }
    }

    
    
    /** Updates session status icon and calls method to play a synthesized file */
    private void playSynthesis() {
        
        preparePlayback();
            
        Prompt selectedPrompt = promptArray[getCurrentRow()];
        String basename = selectedPrompt.getBasename();
        
        // Change status icon in Admin window
        jLabel_SessionStatus.setIcon(IconSet.PLAY_48X48);
        
        // Change status icon in Speaker window
        speakerWin.updateSessionStatus(IconSet.PLAY_64X64);
        
        // PRI2 Get path name with DatabaseLayout method instead?
        String synthFolderPathString = getSynthFolderPath().getPath();
                
        // Synthesis files are prepended with "p_" (e.g., p_spike0001.wav)
        String soundFilePathString = synthFolderPathString + File.separator + "p_" + basename + ".wav";
        
        SourceDataLine speakerOutput = optionsDialog.getSpeakerOutputLine();
        Speech.play(soundFilePathString, speakerOutput, optionsDialog.getSpeakerOutputMode());
        
        // TESTCODE
        Test.output("|AdminWindow.playSynthesis| [Synthesis]:" + selectedPrompt.getPromptText());
        
        // Change status icon in Admin window
        jLabel_SessionStatus.setIcon(IconSet.STOP_48X48);
        
        // Change status icon in Speaker window
        speakerWin.updateSessionStatus(IconSet.STOP_64X64);
               
        // Enforce pause after synthesis playback (as set by user in Options dialog)
        try {
            Thread.sleep(optionsDialog.getPauseAfterSynth());
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
    }
    
    /** Stops playback
        Separate method needed to stop recording */
    private void stopPlayback() {
        Speech.stopPlaying();  // Immediately stop any playback
        stopPressed = true;
    }
    
    /** Calls methods for making a recording */
    private void makeRecording() {
        
        // Get currently selected prompt
        int promptIndex = getCurrentRow();
        Prompt currentPrompt = this.promptArray[promptIndex];
        String currentBasename = currentPrompt.getBasename();        
                
        
        // Change status icon in Admin Window
        jLabel_SessionStatus.setIcon(IconSet.REC_48X48);

        // Change status icon in Speaker Window
        speakerWin.updateSessionStatus(IconSet.REC_64X64);

        int nChars = currentPrompt.getPromptText().length();
        // Calculate mic open time, using bufferToAdd from the user options
        int micOpenTime = nChars * optionsDialog.getTimePerChar() + optionsDialog.getBufferToAdd();
        
        Test.output("|AdminWindow.makeRecording()| nChars: " + nChars + " a " + optionsDialog.getTimePerChar() + " ms");
        Test.output("|AdminWindow.makeRecording()| buffer: " + optionsDialog.getBufferToAdd() + " ms");
        Test.output("|AdminWindow.makeRecording()| micOpenTime: " + micOpenTime + " ms");

        // Create recording object and rename any latest recording
        Recording currentRecording = currentPrompt.getRecording();
        if (currentRecording.getFileCount() > 0)
            currentRecording.archiveLatestRecording();

        try {
            
            boolean warning = false;
            
            // Update button text (change from "Record" to "Stop")
            toggleRecordToStop();
            
            // Update message bar
            String openDuration = new DecimalFormat("#.###").format(micOpenTime * 0.001);
            String message = "Recording for " + currentBasename
                + ": Microphone open for " + openDuration + " s...";
            showMessage(message, warning);  // warning = false; just info
        
            // TESTCODE
            Test.output("|AdminWindow.makeRecording| [Recording]:" + 
                currentPrompt.getPromptText());
            
            // Open the microphone
            currentRecording.timedRecord(optionsDialog.getTargetDataLine(), micOpenTime);
                        
            // Update recording status for current prompt            
            currentRecording.updateFileCount();
            String recStatus = determineStatus(currentRecording);
            message = getClippingMessage(currentRecording);
            warning = currentRecording.isAmpClipped || currentRecording.isTempClipped;
            Test.output("|AdminWindow.makeRecording()| New status: " + recStatus);
            jTable_PromptSet.setValueAt(recStatus, promptIndex, REC_STATUS_COLUMN);
            
            // Enable Play button only if at least one recording exists for selected basename
            if ((currentRecording.getFileCount() > 0) && (!this.recordingBlock)) {
                jButton_Play.setEnabled(true);
                jButton_Display.setEnabled(true);
            }
            
            // TESTCODE 
            Test.output("|AdminWindow.makeRecording| " + currentBasename +
                    " now has " + currentRecording.getFileCount() + " file(s)");

            // Finished recording
                        
            // Update message bar
            message = "Recording for " + currentBasename + ": Microphone is closed. " + message;
            showMessage(message, warning); // false = info, not a warning; true if clipping occurred
            
        }
        catch (Exception ex) {
            ex.printStackTrace();
            String message = "Recording error. Check your audio options and see the console for details.";
            showMessage(message, true); // true = warning, not info
        }
              
        // Change status icon in Admin window and Speaker Window
        jLabel_SessionStatus.setIcon(IconSet.STOP_48X48);
        speakerWin.updateSessionStatus(IconSet.STOP_64X64);
        
        // Only do below if option is not selected to continue with next prompt
        if (!this.jCheckBox_ContinueWithNext.isSelected()) {
            
            // Re-enable the Play/Stop button
            this.jButton_Play.setEnabled(true);
            jButton_Display.setEnabled(true);
            
            // Update button text (change from "Stop" to "Record")
            toggleStopToRecord();
        
        }
        
    } 
    
    /** Plays latest recording for the currently selected promtp */
    private void playRecording() {

        preparePlayback();
        
        Prompt selectedPrompt = promptArray[getCurrentRow()];
        String basename = selectedPrompt.getBasename();
        int recCount = selectedPrompt.getRecCount();              
        
        // TESTCODE
        Test.output("|AdminWindow.playRecording| Number of recordings for " + basename + " is: " + recCount);
        
        // If selected prompt has a recording
        if (recCount > 0)  {
            
            // Change status icon in Admin Window
            jLabel_SessionStatus.setIcon(IconSet.PLAY_48X48);
                        
            // Change status icon in Speaker Window
            speakerWin.updateSessionStatus(IconSet.PLAY_64X64);
            
            // Update message bar            
            String message = "Playing latest recording for " + basename + ".";
            showMessage(message, false); // false = info, not a warning
            
            // Determine complete path for the .wav file
            String recFolderPathString = getRecFolderPath().getPath();
            String soundFilePathString = recFolderPathString + File.separator + basename + ".wav";
                        
            // TESTCODE
            Test.output("|AdminWindow.playRecording| Sound file path: " + soundFilePathString);
            
            try {
            
                if (!this.recordingBlock) {
                    // We're using the Play button as the Stop button
                    // Update button text (change from "Play" to "Stop")
                    togglePlayToStop();
                }
                
                SourceDataLine expertOutput = optionsDialog.getExpertOutputLine();
                Speech.play(soundFilePathString, expertOutput, optionsDialog.getExpertOutputMode());
                                
                // Update message bar            
                message = "Ready.";
                showMessage(message, false); // false = info, not a warning
                // PRI3 Change this to show current selection (as before)
                
            } catch (Exception ex) {
                ex.printStackTrace();
                message = "Playback error. Check your audio options and see the console for details.";
                showMessage(message, true); // true = warning, not info
            }  
            
            // Change status icon in Admin window
            jLabel_SessionStatus.setIcon(IconSet.STOP_48X48);

            // Change status icon in Speaker window
            speakerWin.updateSessionStatus(IconSet.STOP_64X64);
            
        }
        else {
            // Update message bar
            String message = "No recording for " + basename + ".";
            showMessage(message, false); // false = info, not a warning
        }       
        
        endPlayback();
    }    
    
    /** Establishes logic for running a recording (with optional playback, etc.) */ 
    private void manageRecording() {
        
        this.recordingBlock = true;  // To help know which buttons to enable and disable
        this.stopPressed = false;
        setCheckBoxes(false);
        
        int startingRow = getCurrentRow();
        int endRow = startingRow + 1;
        
        // When user has selected "Continue with next prompt"
        if (jCheckBox_ContinueWithNext.isSelected()) {
            
            // Get number of rows so that we continue to the end of the prompt set
            endRow = this.promptArray.length;        
            
        }
        
        for (int row = startingRow; !stopPressed && row < endRow; row++) {

            // TESTCODE
            Test.output("|AdminWindow.jButton_RecordActionPerformed| [Current row]:" + row);
            Test.output("|AdminWindow.jButton_RecordActionPerformed| [End row]:" + endRow);
 
            
            // Get currently selected prompt
            int promptIndex = getCurrentRow();
            Prompt currentPrompt = this.promptArray[promptIndex];

            // Update prompt display
            displayPromptText();
             
            // Play synthesized prompt if this option is enabled
            if (jCheckBox_PlaySynthesis.isSelected()) {                
                playSynthesis();                
            } else if (row > startingRow) {
                // Apply user-defined silence before recording, only for non-initial recording
                try {
                    // Indicate pause duration in the message bar
                    String silenceDuration = new DecimalFormat("#.###").format(optionsDialog.getSilenceDuration() * 0.001);
                    String message = currentPrompt.getBasename() + ": " + silenceDuration + " s pause to allow reading of the prompt...";
                    showMessage(message, false);  // warning = false; just info
                    
                    // Enforce the pause duration between prompt recordings (as set by user in Options dialog)
                    Thread.sleep(optionsDialog.getSilenceDuration());

                } catch (InterruptedException ex) {
                        ex.printStackTrace();
                }

            }
            
            if (!stopPressed) {
                // Disable the Play/Stop button so that user can't accidentally begin playing the file we're overwriting
                this.jButton_Play.setEnabled(false);
                jButton_Display.setEnabled(false);
                
                // Play beep to indicate microphone is open
                optionsDialog.playOpenBeep();
            }

            if (!stopPressed) {
                // Call method that coordinates the actual recording
                makeRecording();

                // Play beep to indicate microphone is closed (if option selected in GUI
                if (jCheckBox_PlayClosingBeep.isSelected()) {
                    optionsDialog.playClosedBeep();
                    Test.output("Second beep enabled.");
                }
                else {
                    Test.output("Second beep disabled.");
                }

            }
            
            // Listen to recording if this option is selected
            if (!stopPressed && jCheckBox_PlayBackRec.isSelected()) {
                playRecording();
            }
            // TESTCODE
            else Test.output("|AdminWindow.jButton_RecordActionPerformed| [No playback of recording]");
            
            // Select next row
            if ((row < (endRow - 1)) && (!stopPressed))  {
                jTable_PromptSet.setRowSelectionInterval(row + 1, row + 1);
                
                // Advance vertical scrollbar
                try {
                    int column = 1; //jTable_PromptSet.getSelectedColumn();                
                    Rectangle cellRect = jTable_PromptSet.getCellRect(row + 4, column, false);
                    jTable_PromptSet.scrollRectToVisible(cellRect);
                    jTable_PromptSet.updateUI();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            
        }  // row
    
        // Re-enable buttons in the event they were disabled due to continuous mode
        this.jButton_Record.setEnabled(true);
        this.jButton_Play.setEnabled(true);
        jButton_Display.setEnabled(true);
        
        // Update button text (change from "Stop" to "Record" or "Play")
        toggleStopToRecord();
        toggleStopToPlay();
        
        setCheckBoxes(true);
        this.recordingBlock = false;
        this.continuousMode = false;
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel_SpeakerView = new javax.swing.JPanel();
        jScrollPane_Instructions = new javax.swing.JScrollPane();
        jEditorPane_Instructions = new javax.swing.JEditorPane();
        jLabel_SessionStatus = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jTextPane_PromptDisplay = new javax.swing.JTextPane();
        jPanel_AdminControls = new javax.swing.JPanel();
        jScrollPane_PromptSet = new javax.swing.JScrollPane();
        jCheckBox_PlaySynthesis = new javax.swing.JCheckBox();
        jCheckBox_PlayBackRec = new javax.swing.JCheckBox();
        jCheckBox_ContinueWithNext = new javax.swing.JCheckBox();
        jButton_Record = new javax.swing.JButton();
        jButton_Play = new javax.swing.JButton();
        jSeparator_MessageBar = new javax.swing.JSeparator();
        jLabel_MessageBar = new javax.swing.JLabel();
        jCheckBox_PlayClosingBeep = new javax.swing.JCheckBox();
        jLabel_MessageBarIcon = new javax.swing.JLabel();
        jButton_Display = new javax.swing.JButton();
        jMenuBar_AdminWindow = new javax.swing.JMenuBar();
        jMenu_File = new javax.swing.JMenu();
        jMenuItem_Open = new javax.swing.JMenuItem();
        jSeparator_File = new javax.swing.JSeparator();
        jMenuItem_Exit = new javax.swing.JMenuItem();
        jMenu_Tools = new javax.swing.JMenu();
        jMenuItem_Options = new javax.swing.JMenuItem();
        jMenu_Window = new javax.swing.JMenu();
        jCheckBoxMenuItem_AdminWindow = new javax.swing.JCheckBoxMenuItem();
        jCheckBoxMenuItem_SpeakerWindow = new javax.swing.JCheckBoxMenuItem();
        jMenu_Help = new javax.swing.JMenu();
        jMenu_About = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Redstart - Admin Window");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jPanel_SpeakerView.setBorder(javax.swing.BorderFactory.createTitledBorder("Speaker View"));
        jScrollPane_Instructions.setBorder(null);
        jEditorPane_Instructions.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        jEditorPane_Instructions.setBorder(null);
        jEditorPane_Instructions.setEditable(false);
        jEditorPane_Instructions.setFont(new java.awt.Font("Tahoma", 0, 14));
        jEditorPane_Instructions.setAutoscrolls(false);
        jEditorPane_Instructions.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                jEditorPane_InstructionsComponentResized(evt);
            }
        });

        jScrollPane_Instructions.setViewportView(jEditorPane_Instructions);

        jLabel_SessionStatus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/stopped_48x48.png")));
        jLabel_SessionStatus.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel_SessionStatusMouseClicked(evt);
            }
        });

        jTextPane_PromptDisplay.setEditable(false);
        jTextPane_PromptDisplay.setFont(new java.awt.Font("Tahoma", 0, 48));
        jTextPane_PromptDisplay.setAutoscrolls(false);
        jTextPane_PromptDisplay.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                jTextPane_PromptDisplayComponentResized(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTextPane_PromptDisplay, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 810, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTextPane_PromptDisplay, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 113, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout jPanel_SpeakerViewLayout = new org.jdesktop.layout.GroupLayout(jPanel_SpeakerView);
        jPanel_SpeakerView.setLayout(jPanel_SpeakerViewLayout);
        jPanel_SpeakerViewLayout.setHorizontalGroup(
            jPanel_SpeakerViewLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_SpeakerViewLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_SpeakerViewLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_SpeakerViewLayout.createSequentialGroup()
                        .add(jScrollPane_Instructions, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 738, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 24, Short.MAX_VALUE)
                        .add(jLabel_SessionStatus)))
                .addContainerGap())
        );
        jPanel_SpeakerViewLayout.setVerticalGroup(
            jPanel_SpeakerViewLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_SpeakerViewLayout.createSequentialGroup()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_SpeakerViewLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jLabel_SessionStatus)
                    .add(jScrollPane_Instructions, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel_AdminControls.setBorder(javax.swing.BorderFactory.createTitledBorder("Admin Controls"));

        jCheckBox_PlaySynthesis.setText("Play synthesized prompt");
        jCheckBox_PlaySynthesis.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox_PlaySynthesis.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jCheckBox_PlayBackRec.setText("Play back after recording");
        jCheckBox_PlayBackRec.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox_PlayBackRec.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jCheckBox_ContinueWithNext.setText("Continue with next prompt");
        jCheckBox_ContinueWithNext.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox_ContinueWithNext.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jCheckBox_ContinueWithNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox_ContinueWithNextActionPerformed(evt);
            }
        });

        jButton_Record.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/recording_16x16.png")));
        jButton_Record.setText("Record");
        jButton_Record.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton_Record.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton_Record.setMaximumSize(new java.awt.Dimension(95, 25));
        jButton_Record.setMinimumSize(new java.awt.Dimension(95, 25));
        jButton_Record.setPreferredSize(new java.awt.Dimension(95, 25));
        jButton_Record.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_RecordActionPerformed(evt);
            }
        });

        jButton_Play.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/playing_16x16.png")));
        jButton_Play.setText("Play");
        jButton_Play.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jButton_Play.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        jButton_Play.setMaximumSize(new java.awt.Dimension(95, 25));
        jButton_Play.setMinimumSize(new java.awt.Dimension(95, 25));
        jButton_Play.setPreferredSize(new java.awt.Dimension(95, 25));
        jButton_Play.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_PlayActionPerformed(evt);
            }
        });

        jLabel_MessageBar.setText("Ready.");

        jCheckBox_PlayClosingBeep.setText("Play beep when microphone is closed");
        jCheckBox_PlayClosingBeep.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox_PlayClosingBeep.setMargin(new java.awt.Insets(0, 0, 0, 0));

        jLabel_MessageBarIcon.setText("Message:");

        jButton_Display.setText("Display");
        jButton_Display.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_DisplayActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel_AdminControlsLayout = new org.jdesktop.layout.GroupLayout(jPanel_AdminControls);
        jPanel_AdminControls.setLayout(jPanel_AdminControlsLayout);
        jPanel_AdminControlsLayout.setHorizontalGroup(
            jPanel_AdminControlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_AdminControlsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_AdminControlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane_PromptSet, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 810, Short.MAX_VALUE)
                    .add(jSeparator_MessageBar, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 810, Short.MAX_VALUE)
                    .add(jPanel_AdminControlsLayout.createSequentialGroup()
                        .add(jLabel_MessageBarIcon)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel_MessageBar))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_AdminControlsLayout.createSequentialGroup()
                        .add(jPanel_AdminControlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jCheckBox_PlaySynthesis)
                            .add(jCheckBox_PlayBackRec)
                            .add(jCheckBox_PlayClosingBeep)
                            .add(jCheckBox_ContinueWithNext))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 455, Short.MAX_VALUE)
                        .add(jPanel_AdminControlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jButton_Display, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 98, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton_Record, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jButton_Play, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel_AdminControlsLayout.setVerticalGroup(
            jPanel_AdminControlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_AdminControlsLayout.createSequentialGroup()
                .add(jScrollPane_PromptSet, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 118, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AdminControlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(jPanel_AdminControlsLayout.createSequentialGroup()
                        .add(jButton_Record, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton_Play, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel_AdminControlsLayout.createSequentialGroup()
                        .add(jCheckBox_PlaySynthesis)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jCheckBox_PlayClosingBeep)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jCheckBox_PlayBackRec)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AdminControlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jCheckBox_ContinueWithNext)
                    .add(jButton_Display))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator_MessageBar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AdminControlsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_MessageBar)
                    .add(jLabel_MessageBarIcon))
                .addContainerGap())
        );

        jPanel_AdminControlsLayout.linkSize(new java.awt.Component[] {jButton_Play, jButton_Record}, org.jdesktop.layout.GroupLayout.VERTICAL);

        jMenu_File.setText("File");
        jMenuItem_Open.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/open_16x16.png")));
        jMenuItem_Open.setText("Open Voice...");
        jMenuItem_Open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem_OpenActionPerformed(evt);
            }
        });

        jMenu_File.add(jMenuItem_Open);

        jMenu_File.add(jSeparator_File);

        jMenuItem_Exit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/exit_16x16.png")));
        jMenuItem_Exit.setText("Exit");
        jMenuItem_Exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem_ExitActionPerformed(evt);
            }
        });

        jMenu_File.add(jMenuItem_Exit);

        jMenuBar_AdminWindow.add(jMenu_File);

        jMenu_Tools.setText("Tools");
        jMenuItem_Options.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/options_16x16.png")));
        jMenuItem_Options.setText("Options...");
        jMenuItem_Options.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem_OptionsActionPerformed(evt);
            }
        });

        jMenu_Tools.add(jMenuItem_Options);

        jMenuBar_AdminWindow.add(jMenu_Tools);

        jMenu_Window.setText("Window");
        jCheckBoxMenuItem_AdminWindow.setSelected(true);
        jCheckBoxMenuItem_AdminWindow.setText("Admin Window");
        jCheckBoxMenuItem_AdminWindow.setEnabled(false);
        jMenu_Window.add(jCheckBoxMenuItem_AdminWindow);

        jCheckBoxMenuItem_SpeakerWindow.setText("Speaker Window");
        jCheckBoxMenuItem_SpeakerWindow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItem_SpeakerWindowActionPerformed(evt);
            }
        });

        jMenu_Window.add(jCheckBoxMenuItem_SpeakerWindow);

        jMenuBar_AdminWindow.add(jMenu_Window);

        jMenu_Help.setText("Help");
        jMenu_About.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/about_16x16.png")));
        jMenu_About.setText("About Redstart");
        jMenu_About.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu_AboutActionPerformed(evt);
            }
        });

        jMenu_Help.add(jMenu_About);

        jMenuBar_AdminWindow.add(jMenu_Help);

        setJMenuBar(jMenuBar_AdminWindow);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_AdminControls, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_SpeakerView, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jPanel_SpeakerView, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AdminControls, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton_DisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_DisplayActionPerformed
        Prompt selectedPrompt = promptArray[getCurrentRow()];
        try {
            File f = selectedPrompt.getRecording().getFile();
            AudioInputStream audio = AudioSystem.getAudioInputStream(f);
            if (audio.getFormat().getChannels() > 1) {
                audio = new MonoAudioInputStream(audio, optionsDialog.getInputMode());
            }
            MultiDisplay d = new MultiDisplay(audio, selectedPrompt.getBasename(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButton_DisplayActionPerformed

    private void jTextPane_PromptDisplayComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jTextPane_PromptDisplayComponentResized
        //this.displayPromptText();
    }//GEN-LAST:event_jTextPane_PromptDisplayComponentResized

    private void jCheckBox_ContinueWithNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox_ContinueWithNextActionPerformed
        if (this.jCheckBox_ContinueWithNext.isSelected()) {
            this.continuousMode = true;
        }
        else {
            this.continuousMode = false;
        }
    }//GEN-LAST:event_jCheckBox_ContinueWithNextActionPerformed

    private void jLabel_SessionStatusMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel_SessionStatusMouseClicked
        // Play a beep (for demo purposes to the speaker; also useful for quickly testing audio setup)
        if (beepDemoOn) {          
            optionsDialog.playOpenBeep();
            if (jCheckBox_PlayClosingBeep.isSelected()) { 
                beepDemoOn = false;
            }            
        }
        else {
            optionsDialog.playClosedBeep();
            beepDemoOn = true;
        }
    }//GEN-LAST:event_jLabel_SessionStatusMouseClicked

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        System.out.println("Exiting Recording Session Manager... Done.");
    }//GEN-LAST:event_formWindowClosing

    private void jEditorPane_InstructionsComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jEditorPane_InstructionsComponentResized
        // PRI4 Ensure horizontal scrollbar isn't enabled
        // TODO
    }//GEN-LAST:event_jEditorPane_InstructionsComponentResized

    private void jMenuItem_OpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem_OpenActionPerformed
        
        // Allow user to choose a different voice (prompt set) without exiting the tool
        
        // Create a file chooser
        final JFileChooser openDialog = new JFileChooser();
        
        // Set the current directory to the voice currently in use
        openDialog.setCurrentDirectory(getVoiceFolderPath());
        openDialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = openDialog.showDialog(AdminWindow.this, "Open Voice");
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File voice = openDialog.getSelectedFile();
            setVoiceFolderPath(voice);  // Set to the selected the voice folder path
            Test.output("Open voice: " + voice);
            setupVoice();
        } else {
            Test.output("Open command cancelled.");
        }
        
    }//GEN-LAST:event_jMenuItem_OpenActionPerformed

    /** Exits the application
     *  @param evt An action event
     */
    private void jMenuItem_ExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem_ExitActionPerformed
        System.out.println("Exiting Recording Session Manager... Done.");
        System.exit(0);
    }//GEN-LAST:event_jMenuItem_ExitActionPerformed

    /** Shows an options dialog
     *  @param evt An action event
     */
    private void jMenuItem_OptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem_OptionsActionPerformed
        optionsDialog.setVisible(true);
    }//GEN-LAST:event_jMenuItem_OptionsActionPerformed

    /** Shows a simple About dialog for the application
     *  @param evt An action event
     */
    private void jMenu_AboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu_AboutActionPerformed
        // PRI4 Could show a more elaborate dialog
        // More info, DFKI logo, etc.
        //JOptionPane.showMessageDialog(this, "Recording Session Manager\n(c) 2007 DFKI GmbH");
        new About(this).setVisible(true);
    }//GEN-LAST:event_jMenu_AboutActionPerformed

    /* Activates or deactivates the speaker window, depending on current state
     * @param evt Action event
     */
    private void jCheckBoxMenuItem_SpeakerWindowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItem_SpeakerWindowActionPerformed
        
        // Activate speaker window if checkbox menu item was not selected
        // Else close speaker window if checkbox menu item was already selected
        if (jCheckBoxMenuItem_SpeakerWindow.isSelected()) {
            speakerWin.updateSessionStatus(IconSet.STOP_64X64);
            speakerWin.setVisible(true);
        }
        else {
            speakerWin.setVisible(false);
        }
        
    }//GEN-LAST:event_jCheckBoxMenuItem_SpeakerWindowActionPerformed

    /** Resizes columns to fit expected data
     *  @param evt Component event
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        // Note: event also appears to include component initialization, not just resizing
        if (this.isVisible()) {
            setColumnWidths();    
            displayPromptText();  // Re-center prompt text
        }
    }//GEN-LAST:event_formComponentResized

    public void setVisible(boolean visible) 
    {
        super.setVisible(visible);
        if (visible) {
            setColumnWidths();
            displayPromptText();
        }
    }
    
    /* Interrupts recording or playback
     * @param evt Action event
     */
    /** Plays the latest recorded version of the currently selected prompt */
    private void jButton_PlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_PlayActionPerformed
        if (this.playingStatus) {
            stopPlayback();  // Stop playback if underway
            this.stopPressed = true;
            toggleStopToRecord(); // Needed if Play/Stop is pressed during continuous record mode
            this.jButton_Record.setEnabled(true);
            toggleStopToPlay();            
        }
        // Otherwise play the recording
        else {
            togglePlayToStop();
            new Thread() {
                public void run() { playRecording(); }
            }.start();
        }
    }//GEN-LAST:event_jButton_PlayActionPerformed

    /** Handles the recording logic, including potential playback and continuous mode (if selected)
     *  @param evt Action event
     */
    private void jButton_RecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_RecordActionPerformed
        
        if (this.playingStatus) {
            stopRecord();  // Stop recording if underway
            this.stopPressed = true;
            this.playingStatus = false;  // PRI3 Can we combine these two flags or is stopPressed needed for setupRecording()?
            toggleStopToRecord();
            toggleStopToPlay();
            
            // Disable Record button so that user can't press it while waiting to stop
            this.jButton_Record.setEnabled(false);
            // Re-enabled at end of manageRecording();
            
            // Temporary solution until method implemented to stop actual recording
            String message = "Stop button pressed.";
            showMessage(message, true); // true = warning, not info
        }
        // Otherwise make the recording
        else {
            
            // Don't toggle yet if playing synthesized prompt first
            if (!jCheckBox_ContinueWithNext.isSelected()) {
                toggleRecordToStop();
            }
            new Thread() {
                public void run() { manageRecording(); }
            }.start();
            Test.output("Dispatched recording thread");
            
        }
        
        
    }//GEN-LAST:event_jButton_RecordActionPerformed
    
    
    /** Sets the GUI look and feel to that of the system (e.g., Windows XP, Mac OS) */
    private void setSystemLookAndFeel() {

        // Get the properties from the options file
        Properties options = new Properties();
        try {
            // First, load defaults from resource in classpath:
            options.load(Redstart.class.getResourceAsStream("options/user.options"));

            // Then, overwrite from file if present:
            File fileHandle = new File(optionsDialog.getOptionsPathString());
            if (fileHandle.exists()) {
                FileInputStream optionsStream;
                try {
                    optionsStream = new FileInputStream(fileHandle);
                    options.load(optionsStream);

                    // Close the input stream
                    optionsStream.close();
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
                
        String systemLookAndFeelProp = options.getProperty("systemLookAndFeel");

        if (systemLookAndFeelProp.equals("true")) {
        
            // Use the look and feel of the system
            try {
                // Set to system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } 
            catch (UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }
            catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            catch (InstantiationException ex) {
                 ex.printStackTrace();
            }
            catch (IllegalAccessException ex) {
                 ex.printStackTrace();
            }
            
        }
    }
    
    /** Returns path for folder containing basenames and prompts
     *  @return Folder path containing basenames and prompts (e.g., /project/mary/mat/voices/bundesliga/text)
     */
    public File getPromptFolderPath() {
        
        // Create the instructions folder path from the voice folder path
        String pathString = voiceFolderPathString + AdminWindow.PROMPT_FOLDER_NAME; 
        File promptFolderPath = new File(pathString);
        
        return promptFolderPath;
    }
    
    /** Returns file path for folder containing the speaker recordings
     *  @return File path for folder containing the speaker recordings (e.g., /project/mary/mat/voices/bundesliga/wav)
     */
    public File getRecFolderPath() {
        
        // Create the recordings folder path from the voice folder path
        String pathString = voiceFolderPathString + AdminWindow.REC_FOLDER_NAME; 
        File recFolderPath = new File(pathString);
        
        // TESTCODE
        Test.output("|AdminWindow.getRecFolderPath()| Recordings Path = " + recFolderPath.getPath());
        
        return recFolderPath;        
        
    }
    
    /** Returns file path for folder containing the synthesized recordings
     *  @return File path for folder containing the synthesized recordings (e.g., /project/mary/mat/voices/bundesliga/prompt_wav)
     */
    public File getSynthFolderPath() {
        
        // Create the recordings folder path from the voice folder path
        String pathString = voiceFolderPathString + AdminWindow.SYNTH_FOLDER_NAME; 
        File synthFolderPath = new File(pathString);
        
        // TESTCODE
        Test.output("|AdminWindow.getSynthFolderPath()| Synthesis Path = " + synthFolderPath.getPath());
        
        return synthFolderPath;        
        
    }
    
    /** Returns file path for folder containing the synthesized recordings
     *  @return File path for folder containing the synthesized recordings (e.g., /project/mary/mat/voices/bundesliga/prompt_wav)
     */
    public File getInstructionsFolderPath() {
        
        // Create the recordings folder path from the voice folder path
        String pathString = voiceFolderPathString + AdminWindow.INSTRUCTIONS_FOLDER_NAME; 
        File instructionsFolderPath = new File(pathString);
        
        // TESTCODE
        Test.output("|AdminWindow.getInstructionsFolderPath()| Instructions Path = " + instructionsFolderPath.getPath());
        
        return instructionsFolderPath;        
        
    }
    
    /** Returns file path for folder containing the voice
     *  @return File path for folder containing the voice (e.g., /project/mary/mat/voices/bundesliga)
     */
    public File getVoiceFolderPath() {
        
        File voiceFolderPath = new File(voiceFolderPathString);        
        return voiceFolderPath;        
        
    }

    protected void setVoiceFolderPath(File newPath) {
        
        // PRI2 Check that the path is valid
        
        // Set the new path
        voiceFolderPathString = newPath.getPath();
        optionsDialog.saveVoicePath(newPath);
        
    }
    
    public void deselectSpeakerWindow() {
        // Deselect Speaker Window in Window menu now that window has been closed by the user
        this.jMenu_Window.setSelected(false);
    }


    private void stopRecord() {
        Recording currentRecording = promptArray[getCurrentRow()].getRecording();
        currentRecording.stopRecording();
    }

    private void togglePlayToStop() {
        jButton_Play.setText("Stop");
        jButton_Play.setIcon(IconSet.STOP_16X16);
        this.playingStatus = true;        
    }

    private void toggleStopToPlay() {
        jButton_Play.setText("Play");
        jButton_Play.setIcon(IconSet.PLAY_16X16);
        this.playingStatus = false;        
    }
    
    private void toggleRecordToStop() {
        jButton_Record.setText("Stop");
        jButton_Record.setIcon(IconSet.STOP_16X16);
        this.playingStatus = true;        
    }
    
    private void toggleStopToRecord() {
        jButton_Record.setText("Record");
        jButton_Record.setIcon(IconSet.REC_16X16);
        this.playingStatus = false;        
    }

    protected void showMessage(String message, boolean warning) {
        jLabel_MessageBarIcon.setText("");  // No text, just an icon
        if (warning) {
            jLabel_MessageBarIcon.setIcon(IconSet.WARNING_16X16);
            jLabel_MessageBar.setForeground(Color.RED); // PRI4 Make darker red (more readable than default red)
        }
        else {
            jLabel_MessageBarIcon.setIcon(IconSet.INFO_16X16);
            jLabel_MessageBar.setForeground(Color.BLACK);
        }
        jLabel_MessageBar.setText(message);  // Display the actual message next to the icon
        jLabel_MessageBar.setForeground(Color.BLACK);
    }

    private void setCheckBoxes(boolean flag) {
        this.jCheckBox_ContinueWithNext.setEnabled(flag);
        this.jCheckBox_PlayBackRec.setEnabled(flag);
        this.jCheckBox_PlayClosingBeep.setEnabled(flag);
        this.jCheckBox_PlaySynthesis.setEnabled(flag);
    }

    private void preparePlayback() {
        this.setCheckBoxes(false);
        
        if (this.recordingBlock) {
        
            // We're in a recording block so we want to use the Record button as the Stop button
            this.toggleRecordToStop();
            
            // Disable the Play/Stop button so that user can't accidentally begin playback
            this.jButton_Play.setEnabled(false);
            jButton_Display.setEnabled(false);
            
        }
        else {
            
            // Disable the Record/Stop button so that user can't accidentally begin a recording
            this.jButton_Record.setEnabled(false);
            
        }
    }

    private void endPlayback() {
        
        if (!this.continuousMode) {

            // We're not in continuous mode, so return the Record button to normal
            
            if (this.recordingBlock) {
            
                // Record button was used as the Stop button
                
                toggleStopToRecord();  // Update button text (change from "Stop" to "Record")
                this.jButton_Play.setEnabled(true);  // Re-enable the Play/Stop button
                jButton_Display.setEnabled(true);
            }
            else {
            
                // Play button was used as the Stop button
                
                toggleStopToPlay();  // Update button text (change from "Stop" to "Play")
                this.jButton_Record.setEnabled(true);  // Re-enable the Record/Stop button
                
            }            
            
        }
        // If we're in continuous mode then we want to leave these buttons as they are
        
        this.setCheckBoxes(true);
        
    }


    private String getClippingMessage(Recording rec) {
        
        String message;
        
        if ((rec.isAmpClipped) && (rec.isTempClipped)) {            
            message = "Amplitude and temporal clipping detected.";
        }
        else if (rec.isAmpClipped) {
            message = "Amplitude clipping detected.";
        }
        else if (rec.isTempClipped) {
            message = "Temporal clipping detected. ";
        }
        else if (rec.isAmpWarning) {
            message = "Near amplitude clipping threshold";
        }        
        else {
            message = "No clipping detected.";
        }
        
        return message;
        
    }
    
    public class ClippingColorRenderer extends DefaultTableCellRenderer {
        public ClippingColorRenderer() {
            super();
            setOpaque(true); //MUST do this for background to show up.
        }

        public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {
            if (promptArray != null && promptArray.length >= row) {
                Prompt currentPrompt = promptArray[row];
                if (currentPrompt != null) {
                    Recording currentRecording = currentPrompt.getRecording();
                    if (currentRecording != null) {
                        // Determine cell colour
                        Color backgroundColor = Color.WHITE;  // Reset first to white
                        // If amplitude clipping or temporal clipping occurred, then cell background is pink
                        // Note: Red is too harsh (too saturated)
                        if ((currentRecording.isAmpClipped) || (currentRecording.isTempClipped)) {
                            backgroundColor = Color.PINK;
                        }
                        // Otherwise if within the warning threshold, cell background is yellow
                        else if (currentRecording.isAmpWarning) {
                            backgroundColor = Color.YELLOW;
                        }

                        setBackground(backgroundColor);
                    }
                }
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton_Display;
    private javax.swing.JButton jButton_Play;
    private javax.swing.JButton jButton_Record;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem_AdminWindow;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem_SpeakerWindow;
    private javax.swing.JCheckBox jCheckBox_ContinueWithNext;
    private javax.swing.JCheckBox jCheckBox_PlayBackRec;
    private javax.swing.JCheckBox jCheckBox_PlayClosingBeep;
    private javax.swing.JCheckBox jCheckBox_PlaySynthesis;
    private javax.swing.JEditorPane jEditorPane_Instructions;
    private javax.swing.JLabel jLabel_MessageBar;
    private javax.swing.JLabel jLabel_MessageBarIcon;
    private javax.swing.JLabel jLabel_SessionStatus;
    private javax.swing.JMenuBar jMenuBar_AdminWindow;
    private javax.swing.JMenuItem jMenuItem_Exit;
    private javax.swing.JMenuItem jMenuItem_Open;
    private javax.swing.JMenuItem jMenuItem_Options;
    private javax.swing.JMenuItem jMenu_About;
    private javax.swing.JMenu jMenu_File;
    private javax.swing.JMenu jMenu_Help;
    private javax.swing.JMenu jMenu_Tools;
    private javax.swing.JMenu jMenu_Window;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel_AdminControls;
    private javax.swing.JPanel jPanel_SpeakerView;
    private javax.swing.JScrollPane jScrollPane_Instructions;
    private javax.swing.JScrollPane jScrollPane_PromptSet;
    private javax.swing.JSeparator jSeparator_File;
    private javax.swing.JSeparator jSeparator_MessageBar;
    private javax.swing.JTextPane jTextPane_PromptDisplay;
    // End of variables declaration//GEN-END:variables
    
}
