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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JOptionPane;

import de.dfki.lt.mary.recsessionmgr.Redstart;
import de.dfki.lt.mary.recsessionmgr.debug.Test;
import de.dfki.lt.mary.recsessionmgr.lib.Recording;
import de.dfki.lt.mary.recsessionmgr.lib.Speech;
import de.dfki.lt.signalproc.display.MultiDisplay;
import de.dfki.lt.signalproc.util.AudioPlayer;
import de.dfki.lt.signalproc.util.MonoAudioInputStream;

/**
 *
 * @author  Mat Wilson <mwilson@dfki.de>
 */

public class Options extends javax.swing.JFrame {
    //  Location of beeps for recording countdown, start and end
    private static final URL BEEP_HIGH_URL         = Redstart.class.getResource("sounds/beep_high.wav");
    private static final URL BEEP_LOW_URL          = Redstart.class.getResource("sounds/beep_low.wav");
    
    private AdminWindow adminWindow;
    
    // Relevant paths
    private String optionsFolderString;
    private File optionsFolder;
    private String optionsPathString;
    private String testPlaybackPathString;
    
    // User options (pause duration preferences) with default values
    private Integer bufferToAdd = new Integer(0);         // Buffer duration (in ms) to add when opening mic
    private Integer pauseAfterSynth = new Integer(0);     // Pause duration after playing synthesized prompt
    private Integer timePerChar = new Integer(0);     // Pause duration between basenames in continuous mode
    private Integer silenceDuration = new Integer(0);     // Silence duration before recording
        
    // User-defined display options
    private boolean systemLookAndFeel = true;  // Use system look and feel instead (e.g., Windows, Mac OS) 
    private boolean printTestOutput = true;    // Obsolete? Nearly - clean up

    // Playback flags and objects
    private boolean playingStatus;
    private boolean stopPressed;
    private Recording testRecording;
    
    // Progress bar fields
    private static int minAmp = -30;
    private static int maxAmp = 0;
    
    // Target data line
    private TargetDataLine targetDataLine = null;
    
    /** Creates new form Options */
    public Options(AdminWindow adminWindow) {
        this.adminWindow = adminWindow;
        optionsFolderString = adminWindow.getVoiceFolderPath().getPath() + "/config/";
        optionsFolder = new File(optionsFolderString);
        optionsPathString = optionsFolderString + "recsessionmgr.config";
        testPlaybackPathString = optionsFolderString + "test.wav";
        
        testRecording = new Recording(optionsFolder, "test");

        initComponents();
       
        populateAudioCombo();
        
        // Center window in the user's screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();
        setLocation(new Point((screenSize.width - frameSize.width) / 2,
                              (screenSize.height - frameSize.height) / 2));
        
        // Set the tool icon in the upper left corner to the 16 x 16 pixel image
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(IconSet.LOGO_16x16_URL)); 
        loadOptionValues();
       
        // Intialize meter values for audio testing
        this.jProgressBar_Amplitude.setMinimum(minAmp);
        this.jProgressBar_Amplitude.setMaximum(maxAmp);
        this.jProgressBar_Amplitude.setValue(minAmp);
        
    }
    
    public boolean getSystemLookAndFeel() {
        return this.systemLookAndFeel;
    }
        
    /** Gets buffer to to add duration of synthesized playback (for open mic time)
     *  @return The amount of buffer to add (in milliseconds)
     */
    public int getBufferToAdd() {
        return bufferToAdd.intValue();
    }
    
    /** Gets pause duration between synthesis playback and recording
     *  @return The pause duration (in milliseconds) after synthesis playback
     */
    public int getPauseAfterSynth() {
        return pauseAfterSynth.intValue();
    }
    
    /** Gets pause duration between basenames in continuous mode
     *  @return The pause duration (in milliseconds) between prompts when in continuous recording mode
     */
    public int getTimePerChar() {
        return timePerChar.intValue();
    }
    
    public boolean getPrintTestOutput() {
        return printTestOutput;
    }
    
    public boolean getShowPromptCount() {
        return jCheckBox_ShowPromptCount.isSelected();
    }
    
    /**
     * Get the target data line corresponding to the selected
     * AudioSource, Line and Format.
     */
    public TargetDataLine getTargetDataLine() throws LineUnavailableException 
    {
        if (targetDataLine != null) {
            if (targetDataLine.isOpen()) {
                Test.output("targetDataLine was open, with format: "+targetDataLine.getFormat());
                targetDataLine.close();
            }
        }
        Object audioDescriptor = cbAudioSource.getSelectedItem();
        assert audioDescriptor instanceof Mixer.Info || audioDescriptor instanceof String;
        if (audioDescriptor instanceof Mixer.Info) {
            Mixer.Info mixerInfo = (Mixer.Info) audioDescriptor;
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] lineInfos = mixer.getTargetLineInfo();
            assert lineInfos.length > 0 : "Strange, there is no more line info for mixer: "+mixer;
            Line.Info lineInfo = lineInfos[0];
            targetDataLine = (TargetDataLine) mixer.getLine(lineInfo);
        } else {
            assert ((String)audioDescriptor).equals("AudioSystem");
            Line.Info lineInfo = new DataLine.Info(TargetDataLine.class, getAudioFormat());
            targetDataLine = (TargetDataLine) AudioSystem.getLine(lineInfo);
        }
        targetDataLine.open(getAudioFormat());
        Test.output("Target line opened:");
        Test.output("Format requested: "+getAudioFormat());
        Test.output("Format opened   : "+targetDataLine.getFormat());
        return targetDataLine;
    }
    
    public AudioFormat getAudioFormat()
    {
        return new AudioFormat(Float.parseFloat((String)cbSamplingRate.getSelectedItem()),
                Integer.parseInt((String)cbBitsPerSample.getSelectedItem()),
                ((String)cbMonoStereo.getSelectedItem()).equals("mono") ? 1 : 2,
                true, // signed
                false); // little-endian
    }
    
    /**
     * Try to get a line for output to the speaker. The line is not yet opened
     * with a specific format. 
     * @return a line object, or null if the line could not be created.
     */
    public SourceDataLine getSpeakerOutputLine()
    {
        Object audioDescriptor = cbSpeakerOutput.getSelectedItem();
        assert audioDescriptor instanceof Mixer.Info;
        Mixer.Info mixerInfo = (Mixer.Info) audioDescriptor;
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        Line.Info[] lineInfos = mixer.getSourceLineInfo();
        assert lineInfos.length > 0 : "Strange, there are no more source lines for mixer: "+mixer;
        Line.Info lineInfo = lineInfos[0];
        SourceDataLine line = null;
        try {
            line = (SourceDataLine) mixer.getLine(lineInfo);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return line;
    }
    
    /**
     * From the GUI, get the output mode setting: one of
     * AudioPlayer.MONO, AudioPlayer.STEREO, AudioPlayer.LEFT_ONLY or AudioPlayer.RIGHT_ONLY.
     * @return
     */
    public int getSpeakerOutputMode() 
    {
        String mode = (String) cbSpeakerMonoStereo.getSelectedItem();
        if (mode.equals("mono")) return AudioPlayer.MONO;
        else if (mode.equals("left only")) return AudioPlayer.LEFT_ONLY;
        else if (mode.equals("right only")) return AudioPlayer.RIGHT_ONLY;
        // default:
        return AudioPlayer.STEREO;
    }

    /**
     * Try to get a line for output to the expert. The line is not yet opened
     * with a specific format. 
     * @return a line object, or null if the line could not be created.
     */
    public SourceDataLine getExpertOutputLine()
    {
        Object audioDescriptor = cbExpertOutput.getSelectedItem();
        assert audioDescriptor instanceof Mixer.Info;
        Mixer.Info mixerInfo = (Mixer.Info) audioDescriptor;
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        Line.Info[] lineInfos = mixer.getSourceLineInfo();
        assert lineInfos.length > 0 : "Strange, there are no more source lines for mixer: "+mixer;
        Line.Info lineInfo = lineInfos[0];
        SourceDataLine line = null;
        try {
            line = (SourceDataLine) mixer.getLine(lineInfo);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return line;
        
    }

    /**
     * From the GUI, get the input mode setting: one of
     * AudioPlayer.MONO, AudioPlayer.STEREO, AudioPlayer.LEFT_ONLY or AudioPlayer.RIGHT_ONLY.
     * @return
     */
    public int getInputMode() 
    {
        String mode = (String) cbMonoStereo.getSelectedItem();
        if (mode.equals("mono")) return AudioPlayer.MONO;
        else if (mode.equals("left only")) return AudioPlayer.LEFT_ONLY;
        else if (mode.equals("right only")) return AudioPlayer.RIGHT_ONLY;
        // default:
        return AudioPlayer.STEREO;
    }
    

    /**
     * From the GUI, get the output mode setting: one of
     * AudioPlayer.MONO, AudioPlayer.STEREO, AudioPlayer.LEFT_ONLY or AudioPlayer.RIGHT_ONLY.
     * @return
     */
    public int getExpertOutputMode() 
    {
        String mode = (String) cbExpertMonoStereo.getSelectedItem();
        if (mode.equals("mono")) return AudioPlayer.MONO;
        else if (mode.equals("left only")) return AudioPlayer.LEFT_ONLY;
        else if (mode.equals("right only")) return AudioPlayer.RIGHT_ONLY;
        // default:
        return AudioPlayer.STEREO;
    }

    public void playOpenBeep() {
        try {
            SourceDataLine speakerOutput = getSpeakerOutputLine();
            AudioPlayer beepPlayer = new AudioPlayer(AudioSystem.getAudioInputStream(BEEP_HIGH_URL), speakerOutput, null, getSpeakerOutputMode());
            beepPlayer.start();
            beepPlayer.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playClosedBeep() {
        try {
            SourceDataLine speakerOutput = getSpeakerOutputLine();
            AudioPlayer beepPlayer = new AudioPlayer(AudioSystem.getAudioInputStream(BEEP_LOW_URL), speakerOutput, null, getSpeakerOutputMode());
            beepPlayer.start();
            beepPlayer.join();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

        
    /** Loads options from a properties file */
    private void read() {
        try {
            // Get the properties from the options file
            Properties options = new Properties();
            // First, load defaults from resource in classpath:
            options.load(Redstart.class.getResourceAsStream("options/user.options"));
            // Then, overwrite from file if present:
            File fileHandle = new File(optionsPathString);
            if (fileHandle.exists()) {
                FileInputStream optionsStream = new FileInputStream(fileHandle);
                options.load(optionsStream);
                // Close the input stream
                optionsStream.close();
            }
            
            bufferToAdd = Integer.valueOf(options.getProperty("bufferAdded", "1000"));
            pauseAfterSynth = Integer.valueOf(options.getProperty("pauseAfterSynth", "0"));
            timePerChar = Integer.valueOf(options.getProperty("timePerChar", "60"));
            int audioSourceIndex = Integer.parseInt(options.getProperty("audioSourceIndex", "1"));
            int expertOutputIndex = Integer.parseInt(options.getProperty("expertOutputIndex", "0"));
            int speakerOutputIndex = Integer.parseInt(options.getProperty("speakerOutputIndex", "0"));
            silenceDuration = Integer.valueOf(options.getProperty("silenceDuration", "0"));
            boolean showPromptCount = Boolean.valueOf(options.getProperty("showPromptCount", "true")).booleanValue();
                        
            // Set values in the GUI to match what's in the options file
            jSpinner_BufferAdded.setValue(bufferToAdd);
            jSpinner_PauseAfterSynth.setValue(pauseAfterSynth);
            jSpinner_TimePerChar.setValue(timePerChar);
            jSpinner_SilenceDuration.setValue(silenceDuration);
            cbAudioSource.setSelectedIndex(audioSourceIndex);
            cbExpertOutput.setSelectedIndex(expertOutputIndex);
            cbSpeakerOutput.setSelectedIndex(speakerOutputIndex);
            
            cbSamplingRate.setSelectedItem(options.getProperty("samplingRate", "44100"));
            cbBitsPerSample.setSelectedItem(options.getProperty("bitsPerSample", "16"));
            cbMonoStereo.setSelectedItem(options.getProperty("monoStereo", "stereo"));
            
            cbExpertBitsPerSample.setSelectedItem(options.getProperty("expertBitsPerSample", "16"));
            cbExpertMonoStereo.setSelectedItem(options.getProperty("expertMonoStereo", "stereo"));

            cbSpeakerBitsPerSample.setSelectedItem(options.getProperty("speakerBitsPerSample", "16"));
            cbSpeakerMonoStereo.setSelectedItem(options.getProperty("speakerMonoStereo", "stereo"));

            systemLookAndFeel = Boolean.valueOf(options.getProperty("systemLookAndFeel", "true")).booleanValue();
            jCheckBox_SystemLookAndFeel.setSelected(systemLookAndFeel);
            
            printTestOutput = Boolean.valueOf(options.getProperty("printTestOutput", "true")).booleanValue();
            jCheckBox_ShowTestOutput.setSelected(printTestOutput);
            Test.setDebugMode(printTestOutput);        // PRI3 Consolidate these fields
            
            jCheckBox_ShowPromptCount.setSelected(showPromptCount);
            adminWindow.getSpeakerWindow().setShowPromptCount(showPromptCount);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void saveVoicePath(File path) {
        File fileHandle = new File(optionsPathString);
        File parentDir = fileHandle.getParentFile(); // config subdirectory in voice dir
        if (!parentDir.exists()) parentDir.mkdir();
        FileOutputStream optionsStream;
        try {
            optionsStream = new FileOutputStream(fileHandle);

            Test.output("|Options.saveVoicePath()| New voice path: " + path);

            // Set and save properties to config file
            Properties options = new Properties();                        
            options.setProperty("defaultVoicePath", String.valueOf(adminWindow.getVoiceFolderPath().getPath()));
            try {
                options.store(optionsStream, "Settings for Redstart Recording Session Manager"); 
                // Close the output stream
                optionsStream.close();
                
                Test.output("|Options.write()| Settings saved to " + optionsPathString);  // TESTCODE
                
            } catch (IOException ex) {
                ex.printStackTrace();
            } 
            
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }        
    }    
    
    /** Writes options to a properties file */
    private void write() {
        try {
            
            File fileHandle = new File(optionsPathString);
            File parentDir = fileHandle.getParentFile(); // config subdirectory in voice dir
            if (!parentDir.exists()) parentDir.mkdir();
            FileOutputStream optionsStream = new FileOutputStream(fileHandle);
            
            // Set and save properties to config file
            Properties options = new Properties();                        

            this.bufferToAdd = (Integer) jSpinner_BufferAdded.getValue();
            this.pauseAfterSynth = (Integer) jSpinner_PauseAfterSynth.getValue();
            this.timePerChar = (Integer) jSpinner_TimePerChar.getValue();
            this.silenceDuration = (Integer) jSpinner_SilenceDuration.getValue();

            options.setProperty("bufferAdded", String.valueOf(bufferToAdd));
            options.setProperty("pauseAfterSynth", String.valueOf(pauseAfterSynth));
            options.setProperty("pauseBetweenRec", String.valueOf(timePerChar));
            options.setProperty("silenceDuration", String.valueOf(silenceDuration));

            int audioSourceIndex = cbAudioSource.getSelectedIndex();
            int expertOutputIndex = cbExpertOutput.getSelectedIndex();
            int speakerOutputIndex = cbSpeakerOutput.getSelectedIndex();
            
            options.setProperty("audioSourceIndex", String.valueOf(audioSourceIndex));
            options.setProperty("expertOutputIndex", String.valueOf(expertOutputIndex));
            options.setProperty("speakerOutputIndex", String.valueOf(speakerOutputIndex));
            
            options.setProperty("systemLookAndFeel", String.valueOf(this.systemLookAndFeel));             
            options.setProperty("printTestOutput", String.valueOf(this.printTestOutput));

            boolean showPromptCount = jCheckBox_ShowPromptCount.isSelected();
            options.setProperty("showPromptCount", String.valueOf(showPromptCount));

            options.setProperty("samplingRate", (String)cbSamplingRate.getSelectedItem());
            options.setProperty("bitsPerSample", (String)cbBitsPerSample.getSelectedItem());
            options.setProperty("monoStereo", (String) cbMonoStereo.getSelectedItem());
            options.setProperty("expertBitsPerSample", (String)cbExpertBitsPerSample.getSelectedItem());
            options.setProperty("expertMonoStereo", (String) cbExpertMonoStereo.getSelectedItem());
            options.setProperty("speakerBitsPerSample", (String)cbSpeakerBitsPerSample.getSelectedItem());
            options.setProperty("speakerMonoStereo", (String) cbSpeakerMonoStereo.getSelectedItem());
            
            options.store(optionsStream, "Settings for Redstart Recording Session Manager");            
            
            // Close the output stream
            optionsStream.close();
            Test.output("|Options.write()| Settings saved to " + optionsPathString);  // TESTCODE
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jButton_SaveOptions = new javax.swing.JButton();
        jButton_CancelOptions = new javax.swing.JButton();
        jTabbedPane_Options = new javax.swing.JTabbedPane();
        jPanel_AudioOptions = new javax.swing.JPanel();
        jLabel_AudioMixer = new javax.swing.JLabel();
        cbAudioSource = new javax.swing.JComboBox();
        jButton_Record = new javax.swing.JButton();
        jButton_Play = new javax.swing.JButton();
        jProgressBar_Amplitude = new javax.swing.JProgressBar();
        jLabel_dBMin = new javax.swing.JLabel();
        jLabel_dBMax = new javax.swing.JLabel();
        jLabel_Status = new javax.swing.JLabel();
        jLabel_Message = new javax.swing.JLabel();
        cbExpertOutput = new javax.swing.JComboBox();
        cbSpeakerOutput = new javax.swing.JComboBox();
        jLabel_ExpertOutput = new javax.swing.JLabel();
        jLabel_SpeakerOutput = new javax.swing.JLabel();
        cbSamplingRate = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        cbBitsPerSample = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        cbMonoStereo = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        cbExpertMonoStereo = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        cbExpertBitsPerSample = new javax.swing.JComboBox();
        cbSpeakerMonoStereo = new javax.swing.JComboBox();
        bDisplay = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        cbSpeakerBitsPerSample = new javax.swing.JComboBox();
        jPanel_TimingOptions = new javax.swing.JPanel();
        jLabel_BufferAdded = new javax.swing.JLabel();
        jSpinner_BufferAdded = new javax.swing.JSpinner();
        jLabel_PauseBufferAddedUnits = new javax.swing.JLabel();
        jLabel_PauseBetweenrecUnits = new javax.swing.JLabel();
        jSpinner_TimePerChar = new javax.swing.JSpinner();
        jLabel_TimePerChar = new javax.swing.JLabel();
        jLabel_PauseAfterSynth = new javax.swing.JLabel();
        jSpinner_PauseAfterSynth = new javax.swing.JSpinner();
        jLabel_PauseAfterSynthUnits = new javax.swing.JLabel();
        jLabel_SilenceDuration = new javax.swing.JLabel();
        jSpinner_SilenceDuration = new javax.swing.JSpinner();
        jLabel_SilenceDurationBeforeRecUnits = new javax.swing.JLabel();
        jPanel_DisplayOptions = new javax.swing.JPanel();
        jCheckBox_SystemLookAndFeel = new javax.swing.JCheckBox();
        jCheckBox_ShowTestOutput = new javax.swing.JCheckBox();
        jCheckBox_ShowPromptCount = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Redstart - Options");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });

        jButton_SaveOptions.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/ok_16x16.png")));
        jButton_SaveOptions.setText("Save");
        jButton_SaveOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_SaveOptionsActionPerformed(evt);
            }
        });

        jButton_CancelOptions.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/dfki/lt/mary/recsessionmgr/gui/icons/cancel_16x16.png")));
        jButton_CancelOptions.setText("Cancel");
        jButton_CancelOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton_CancelOptionsActionPerformed(evt);
            }
        });

        jTabbedPane_Options.setBackground(javax.swing.UIManager.getDefaults().getColor("TabbedPane.highlight"));
        jPanel_AudioOptions.setBackground(javax.swing.UIManager.getDefaults().getColor("TabbedPane.highlight"));
        jLabel_AudioMixer.setText("Audio source:");

        cbAudioSource.setMaximumSize(new java.awt.Dimension(250, 27));
        cbAudioSource.setPreferredSize(new java.awt.Dimension(250, 27));
        cbAudioSource.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbAudioSourceActionPerformed(evt);
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

        jLabel_dBMin.setText("-30 dB");

        jLabel_dBMax.setText("0 dB");

        jLabel_Status.setText("Status:");

        jLabel_Message.setText("Ready for testing.");

        cbExpertOutput.setMaximumSize(new java.awt.Dimension(250, 27));
        cbExpertOutput.setPreferredSize(new java.awt.Dimension(250, 27));
        cbExpertOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbExpertOutputActionPerformed(evt);
            }
        });

        cbSpeakerOutput.setMaximumSize(new java.awt.Dimension(250, 27));
        cbSpeakerOutput.setPreferredSize(new java.awt.Dimension(250, 27));
        cbSpeakerOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbSpeakerOutputActionPerformed(evt);
            }
        });

        jLabel_ExpertOutput.setText("Expert output:");

        jLabel_SpeakerOutput.setText("Speaker output:");

        cbSamplingRate.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "44100", "48000" }));
        cbSamplingRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbSamplingRateActionPerformed(evt);
            }
        });

        jLabel1.setText("Hz");

        cbBitsPerSample.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "16", "24" }));

        jLabel2.setText("bit");

        cbMonoStereo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "mono", "stereo", "left only", "right only" }));

        cbExpertMonoStereo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "mono", "stereo", "left only", "right only" }));

        jLabel4.setText("bit");

        cbExpertBitsPerSample.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "16", "24" }));

        cbSpeakerMonoStereo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "mono", "stereo", "left only", "right only" }));

        bDisplay.setText("Display");
        bDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bDisplayActionPerformed(evt);
            }
        });

        jLabel5.setText("bit");

        cbSpeakerBitsPerSample.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "16", "24" }));

        org.jdesktop.layout.GroupLayout jPanel_AudioOptionsLayout = new org.jdesktop.layout.GroupLayout(jPanel_AudioOptions);
        jPanel_AudioOptions.setLayout(jPanel_AudioOptionsLayout);
        jPanel_AudioOptionsLayout.setHorizontalGroup(
            jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_AudioOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_AudioOptionsLayout.createSequentialGroup()
                        .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel_AudioOptionsLayout.createSequentialGroup()
                                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel_ExpertOutput)
                                    .add(jLabel_SpeakerOutput))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jPanel_AudioOptionsLayout.createSequentialGroup()
                                        .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                            .add(jPanel_AudioOptionsLayout.createSequentialGroup()
                                                .add(24, 24, 24)
                                                .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 56, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 230, Short.MAX_VALUE))
                                            .add(jPanel_AudioOptionsLayout.createSequentialGroup()
                                                .add(cbExpertBitsPerSample, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 54, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                                .add(jLabel4)
                                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)))
                                        .add(cbExpertMonoStereo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 116, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(cbExpertOutput, 0, 426, Short.MAX_VALUE)
                                    .add(cbSpeakerOutput, 0, 426, Short.MAX_VALUE)))
                            .add(jPanel_AudioOptionsLayout.createSequentialGroup()
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel_AudioMixer)
                                .add(17, 17, 17)
                                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel_AudioOptionsLayout.createSequentialGroup()
                                        .add(cbSamplingRate, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 81, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jLabel1)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 120, Short.MAX_VALUE)
                                        .add(cbBitsPerSample, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(jLabel2)
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                        .add(cbMonoStereo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 121, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                                    .add(org.jdesktop.layout.GroupLayout.LEADING, cbAudioSource, 0, 426, Short.MAX_VALUE))))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED))
                    .add(jPanel_AudioOptionsLayout.createSequentialGroup()
                        .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel_Status)
                            .add(jLabel_dBMin))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_AudioOptionsLayout.createSequentialGroup()
                                .add(cbSpeakerBitsPerSample, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 54, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel5)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(cbSpeakerMonoStereo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 113, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_AudioOptionsLayout.createSequentialGroup()
                                .add(jLabel_Message, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 197, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(jButton_Record, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jButton_Play, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(bDisplay))
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_AudioOptionsLayout.createSequentialGroup()
                                .add(jProgressBar_Amplitude, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jLabel_dBMax)))))
                .add(107, 107, 107))
        );
        jPanel_AudioOptionsLayout.setVerticalGroup(
            jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_AudioOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_AudioMixer)
                    .add(cbAudioSource, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(cbSamplingRate, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1)
                    .add(cbMonoStereo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2)
                    .add(cbBitsPerSample, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_ExpertOutput)
                    .add(cbExpertOutput, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel3)
                    .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(cbExpertMonoStereo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(jLabel4)
                        .add(cbExpertBitsPerSample, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_SpeakerOutput)
                    .add(cbSpeakerOutput, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(6, 6, 6)
                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jLabel_dBMin)
                    .add(jPanel_AudioOptionsLayout.createSequentialGroup()
                        .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(cbSpeakerMonoStereo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel5)
                            .add(cbSpeakerBitsPerSample, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(jLabel_dBMax)
                            .add(jProgressBar_Amplitude, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_AudioOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_Status)
                    .add(bDisplay)
                    .add(jButton_Play, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton_Record, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel_Message))
                .addContainerGap())
        );
        jTabbedPane_Options.addTab("Audio", jPanel_AudioOptions);

        jPanel_TimingOptions.setBackground(javax.swing.UIManager.getDefaults().getColor("TabbedPane.highlight"));
        jLabel_BufferAdded.setText("Buffer added to recording time:");

        jLabel_PauseBufferAddedUnits.setText("ms");

        jLabel_PauseBetweenrecUnits.setText("ms");

        jLabel_TimePerChar.setText("Recording time per character:");

        jLabel_PauseAfterSynth.setText("Pause duration after synthesized playback:");

        jLabel_PauseAfterSynthUnits.setText("ms");

        jLabel_SilenceDuration.setText("Silence duration before recording:");

        jLabel_SilenceDurationBeforeRecUnits.setText("ms");

        org.jdesktop.layout.GroupLayout jPanel_TimingOptionsLayout = new org.jdesktop.layout.GroupLayout(jPanel_TimingOptions);
        jPanel_TimingOptions.setLayout(jPanel_TimingOptionsLayout);
        jPanel_TimingOptionsLayout.setHorizontalGroup(
            jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel_TimingOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_BufferAdded)
                    .add(jLabel_TimePerChar)
                    .add(jLabel_PauseAfterSynth)
                    .add(jLabel_SilenceDuration))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 91, Short.MAX_VALUE)
                .add(jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jSpinner_BufferAdded, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSpinner_TimePerChar, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSpinner_PauseAfterSynth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSpinner_SilenceDuration, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel_PauseBufferAddedUnits)
                    .add(jLabel_PauseBetweenrecUnits)
                    .add(jLabel_PauseAfterSynthUnits)
                    .add(jLabel_SilenceDurationBeforeRecUnits))
                .add(90, 90, 90))
        );
        jPanel_TimingOptionsLayout.setVerticalGroup(
            jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_TimingOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_BufferAdded)
                    .add(jLabel_PauseBufferAddedUnits, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jSpinner_BufferAdded, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_TimePerChar)
                    .add(jLabel_PauseBetweenrecUnits, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSpinner_TimePerChar))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel_PauseAfterSynth)
                    .add(jLabel_PauseAfterSynthUnits)
                    .add(jSpinner_PauseAfterSynth, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel_TimingOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jSpinner_SilenceDuration, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel_SilenceDuration)
                    .add(jLabel_SilenceDurationBeforeRecUnits))
                .add(141, 141, 141))
        );
        jTabbedPane_Options.addTab("Timing", jPanel_TimingOptions);

        jPanel_DisplayOptions.setBackground(javax.swing.UIManager.getDefaults().getColor("TabbedPane.highlight"));
        jCheckBox_SystemLookAndFeel.setBackground(javax.swing.UIManager.getDefaults().getColor("TabbedPane.highlight"));
        jCheckBox_SystemLookAndFeel.setText("Use system look and feel (requires tool restart)");
        jCheckBox_SystemLookAndFeel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox_SystemLookAndFeel.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jCheckBox_SystemLookAndFeel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox_SystemLookAndFeelActionPerformed(evt);
            }
        });

        jCheckBox_ShowTestOutput.setBackground(javax.swing.UIManager.getDefaults().getColor("TabbedPane.highlight"));
        jCheckBox_ShowTestOutput.setText("Display test output in console");
        jCheckBox_ShowTestOutput.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox_ShowTestOutput.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jCheckBox_ShowTestOutput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox_ShowTestOutputActionPerformed(evt);
            }
        });

        jCheckBox_ShowPromptCount.setBackground(javax.swing.UIManager.getDefaults().getColor("TabbedPane.highlight"));
        jCheckBox_ShowPromptCount.setText("Show prompt count and progress bar in Speaker Window ");
        jCheckBox_ShowPromptCount.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jCheckBox_ShowPromptCount.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jCheckBox_ShowPromptCount.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox_ShowPromptCountActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel_DisplayOptionsLayout = new org.jdesktop.layout.GroupLayout(jPanel_DisplayOptions);
        jPanel_DisplayOptions.setLayout(jPanel_DisplayOptionsLayout);
        jPanel_DisplayOptionsLayout.setHorizontalGroup(
            jPanel_DisplayOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_DisplayOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel_DisplayOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jCheckBox_SystemLookAndFeel)
                    .add(jCheckBox_ShowTestOutput)
                    .add(jCheckBox_ShowPromptCount))
                .addContainerGap(162, Short.MAX_VALUE))
        );
        jPanel_DisplayOptionsLayout.setVerticalGroup(
            jPanel_DisplayOptionsLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel_DisplayOptionsLayout.createSequentialGroup()
                .addContainerGap()
                .add(jCheckBox_SystemLookAndFeel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jCheckBox_ShowTestOutput)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jCheckBox_ShowPromptCount)
                .addContainerGap(207, Short.MAX_VALUE))
        );
        jTabbedPane_Options.addTab("Display", jPanel_DisplayOptions);

        jTabbedPane_Options.getAccessibleContext().setAccessibleName("Timing");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(jButton_SaveOptions)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton_CancelOptions))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jTabbedPane_Options, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 581, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        layout.linkSize(new java.awt.Component[] {jButton_CancelOptions, jButton_SaveOptions}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jTabbedPane_Options, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 317, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jButton_SaveOptions)
                    .add(jButton_CancelOptions))
                .addContainerGap(17, Short.MAX_VALUE))
        );
        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bDisplayActionPerformed
        try {
            AudioInputStream audio = AudioSystem.getAudioInputStream(new File(testPlaybackPathString));
            if (audio.getFormat().getChannels() > 1) {
                audio = new MonoAudioInputStream(audio, getInputMode());
            }
            MultiDisplay d = new MultiDisplay(audio, "Test recording", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_bDisplayActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        this.read();  // Load options from resource path
    }//GEN-LAST:event_formWindowOpened

    private void cbSamplingRateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbSamplingRateActionPerformed
    }//GEN-LAST:event_cbSamplingRateActionPerformed

    private void cbSpeakerOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbSpeakerOutputActionPerformed
    }//GEN-LAST:event_cbSpeakerOutputActionPerformed

    private void cbExpertOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbExpertOutputActionPerformed
    }//GEN-LAST:event_cbExpertOutputActionPerformed

    private void jCheckBox_ShowPromptCountActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox_ShowPromptCountActionPerformed
    }//GEN-LAST:event_jCheckBox_ShowPromptCountActionPerformed

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
            String message = "Please wait for mic to close...";
            showMessage(message);
        }
        // Otherwise make the recording
        else {
            
            // Don't toggle yet if playing synthesized prompt first
            new Thread() {
                public void run() { makeRecording(); }
            }.start();
            Test.output("Dispatched setup recording");
            
        }
    }//GEN-LAST:event_jButton_RecordActionPerformed

    private void jCheckBox_ShowTestOutputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox_ShowTestOutputActionPerformed
        this.printTestOutput = !this.printTestOutput; // Toggle setting
        Test.setDebugMode(this.printTestOutput);      // PRI3 Consolidate these fields
    }//GEN-LAST:event_jCheckBox_ShowTestOutputActionPerformed

    private void jCheckBox_SystemLookAndFeelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox_SystemLookAndFeelActionPerformed
        this.systemLookAndFeel = !this.systemLookAndFeel; // Toggle setting
    }//GEN-LAST:event_jCheckBox_SystemLookAndFeelActionPerformed

    private void cbAudioSourceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbAudioSourceActionPerformed
    }//GEN-LAST:event_cbAudioSourceActionPerformed
    
    /** Populate the spin controls with values loaded from the options properties file
     *  @param evt A window event
     */
    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
    }//GEN-LAST:event_formWindowActivated

    
     /** Closes Options dialog without saving changes
     *  @param evt An action event
     **/
    private void jButton_CancelOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_CancelOptionsActionPerformed
        // Close dialog without saving changes
        this.read();  // Re-instate option values prior to any changes made
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jButton_CancelOptionsActionPerformed

    /** Writes options to a properties file
     *  @param evt An action event
     **/
    private void jButton_SaveOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton_SaveOptionsActionPerformed
        write();  //  Save options to user.options file
        
        // Hide or show prompt count in Speaker Window, accordingly
        adminWindow.getSpeakerWindow().setShowPromptCount(getShowPromptCount());
        
        // Hide Options dialog
        this.setVisible(false);
    }//GEN-LAST:event_jButton_SaveOptionsActionPerformed
    

    private void loadOptionValues() {
        
        // Use read() to fill the pause duration instance fields
        this.read();
        
        // TESTCODE
        Test.output("|Options.loadOptionValues()| Buffer To Add: " + this.bufferToAdd);
        Test.output("|Options.loadOptionValues()| Pause After Synthesis: " + this.pauseAfterSynth);
        Test.output("|Options.loadOptionValues()| Time per Character: " + this.timePerChar);
        Test.output("|Options.loadOptionValues()| Silence Duration Before Recording: " + this.silenceDuration);

    }
    
    public String getOptionsPathString() {
        return optionsPathString;
    }

    private void stopRecord() {
        // PRI3
        // Stop recording (not yet implemented)
    }

    private void toggleStopToRecord() {
        jButton_Record.setEnabled(true);
        jButton_Record.setText("Record");
        jButton_Record.setIcon(IconSet.REC_16X16);
        this.playingStatus = false;
    }

    private void toggleStopToPlay() {
        jButton_Play.setText("Play");
        jButton_Play.setIcon(IconSet.PLAY_16X16);
        this.playingStatus = false; 
    }

    private void showMessage(String message) {
        jLabel_Message.setText(message);
    }

    private void makeRecording() {
     
        // Disable the Play/Stop button so that user can't accidentally begin playing the file we're overwriting
        this.jButton_Play.setEnabled(false);
        
        // Update button text (change from "Record" to "Stop")
        toggleRecordToStop();
        
        try {
        
            // Play beep to indicate microphone is open
            playOpenBeep();
            
            String message = "Recording...";
            showMessage(message);
            
            // Get the recording
            int micOpenTime = 3000;
            testRecording.timedRecord(getTargetDataLine(), micOpenTime);
            
            playClosedBeep(); // Mic closed

        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.toString());
            String message = "Recording error. See console for details.";
            showMessage(message);
        }
        
        // Determine whether recording involved amplitude clipping (saturated)
        testRecording.checkForAmpClipping();
        testRecording.checkForTempClipping();
        
        // Re-enable the Play/Stop button
        this.jButton_Play.setEnabled(true);
            
        // Update button text (change from "Stop" to "Record")
        toggleStopToRecord();
        
        // Check for amplitude clipping
        checkAmpClipping(testRecording);
        
    }

    private void togglePlayToStop() {
        jButton_Play.setText("Stop");
        jButton_Play.setIcon(IconSet.STOP_16X16);
        this.playingStatus = true;  
    }

    private void stopPlayback() {
        Speech.stopPlaying();   // Immediately stop any playback
        stopPressed = true;
    }

    private void playRecording() {
        
        // Disable the Record/Stop button so that user can't accidentally begin a recording
        this.jButton_Record.setEnabled(false);
        
        // Now play the test file
        SourceDataLine expertOutput = getExpertOutputLine();
        Speech.play(testPlaybackPathString, expertOutput, getExpertOutputMode());
        
        toggleStopToPlay();  // Update button text (change from "Stop" to "Play")
        this.jButton_Record.setEnabled(true);  // Re-enable the Record/Stop button
        
    }

    long getSilenceDuration() {
        return this.silenceDuration.intValue();
    }

    private void toggleRecordToStop() {
        jButton_Record.setText("Stop");
        jButton_Record.setIcon(IconSet.STOP_16X16);
        this.playingStatus = true;     
    }

    private void checkAmpClipping(Recording testRecording) {
        
        String message;
        testRecording.checkForAmpClipping();
        int db = (int) testRecording.getPeakAmplitude();
        
        // Determine colour of bar in amplitude meter
        Color barColor = Color.GREEN;  // Set to default (green)
        // If amplitude clipping occurred, then bar colour is red
        if (testRecording.isAmpClipped) {
            barColor = Color.RED;
            message = "Amplitude clipping detected: "+ db + " dB";
        }
        // Otherwise if within the warning threshold, bar colour is yellow
        else if (testRecording.isAmpWarning) {
            barColor = Color.YELLOW;
            message = "Near clipping threshold: " + db + " dB";
        } else {
            // Else the bar remains green
            message = "No clipping detected:" + db + " dB";
        }
    
        // Display results of check for amplitude clipping
        this.jProgressBar_Amplitude.setForeground(barColor); // PRI2 Doesn't seem to have any effect
        this.jProgressBar_Amplitude.setValue(db);
                
        // Update the message bar with the appropriate message
        this.showMessage(message);
    }
    
    private void populateAudioCombo() {
        Mixer.Info[]    mixerInfos = AudioSystem.getMixerInfo();
        // audio input:
        cbAudioSource.addItem("AudioSystem");
        for (int i=0; i<mixerInfos.length; i++) {
            Mixer mixer = AudioSystem.getMixer(mixerInfos[i]);
            boolean hasTargetLine = false;
            Line.Info[] lines = mixer.getTargetLineInfo();
            for (int j=0; j<lines.length; j++) {
                if (lines[j] instanceof DataLine.Info) {
                    hasTargetLine = true;
                    break;
                }
            }
            if (hasTargetLine) cbAudioSource.addItem(mixerInfos[i]);
        }
        // Speaker and expert audio output:
        for (int i=0; i<mixerInfos.length; i++) {
            Mixer mixer = AudioSystem.getMixer(mixerInfos[i]);
            boolean hasSourceLine = false;
            Line.Info[] lines = mixer.getSourceLineInfo();
            for (int j=0; j<lines.length; j++) {
                if (lines[j] instanceof DataLine.Info) {
                    hasSourceLine = true;
                    break;
                }
            }
            if (hasSourceLine) {
                cbExpertOutput.addItem(mixerInfos[i]);
                cbSpeakerOutput.addItem(mixerInfos[i]);
            }
        }

    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bDisplay;
    private javax.swing.JComboBox cbAudioSource;
    private javax.swing.JComboBox cbBitsPerSample;
    private javax.swing.JComboBox cbExpertBitsPerSample;
    private javax.swing.JComboBox cbExpertMonoStereo;
    private javax.swing.JComboBox cbExpertOutput;
    private javax.swing.JComboBox cbMonoStereo;
    private javax.swing.JComboBox cbSamplingRate;
    private javax.swing.JComboBox cbSpeakerBitsPerSample;
    private javax.swing.JComboBox cbSpeakerMonoStereo;
    private javax.swing.JComboBox cbSpeakerOutput;
    private javax.swing.JButton jButton_CancelOptions;
    private javax.swing.JButton jButton_Play;
    private javax.swing.JButton jButton_Record;
    private javax.swing.JButton jButton_SaveOptions;
    private javax.swing.JCheckBox jCheckBox_ShowPromptCount;
    private javax.swing.JCheckBox jCheckBox_ShowTestOutput;
    private javax.swing.JCheckBox jCheckBox_SystemLookAndFeel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel_AudioMixer;
    private javax.swing.JLabel jLabel_BufferAdded;
    private javax.swing.JLabel jLabel_ExpertOutput;
    private javax.swing.JLabel jLabel_Message;
    private javax.swing.JLabel jLabel_PauseAfterSynth;
    private javax.swing.JLabel jLabel_PauseAfterSynthUnits;
    private javax.swing.JLabel jLabel_PauseBetweenrecUnits;
    private javax.swing.JLabel jLabel_PauseBufferAddedUnits;
    private javax.swing.JLabel jLabel_SilenceDuration;
    private javax.swing.JLabel jLabel_SilenceDurationBeforeRecUnits;
    private javax.swing.JLabel jLabel_SpeakerOutput;
    private javax.swing.JLabel jLabel_Status;
    private javax.swing.JLabel jLabel_TimePerChar;
    private javax.swing.JLabel jLabel_dBMax;
    private javax.swing.JLabel jLabel_dBMin;
    private javax.swing.JPanel jPanel_AudioOptions;
    private javax.swing.JPanel jPanel_DisplayOptions;
    private javax.swing.JPanel jPanel_TimingOptions;
    private javax.swing.JProgressBar jProgressBar_Amplitude;
    private javax.swing.JSpinner jSpinner_BufferAdded;
    private javax.swing.JSpinner jSpinner_PauseAfterSynth;
    private javax.swing.JSpinner jSpinner_SilenceDuration;
    private javax.swing.JSpinner jSpinner_TimePerChar;
    private javax.swing.JTabbedPane jTabbedPane_Options;
    // End of variables declaration//GEN-END:variables

    
}
