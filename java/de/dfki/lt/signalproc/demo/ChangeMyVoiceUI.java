/*
 * ChangeMyVoiceUI.java
 *
 * Created on June 21, 2007, 7:52 AM
 */

package de.dfki.lt.signalproc.demo;

import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.awt.Point;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.jsresources.AudioCommon;
import org.jsresources.AudioRecorder.BufferingRecorder;

import de.dfki.lt.mary.client.SimpleFileFilter;
import de.dfki.lt.mary.util.MaryUtils;
import de.dfki.lt.signalproc.FFT;
import de.dfki.lt.signalproc.process.FrameOverlapAddSource;
import de.dfki.lt.signalproc.process.InlineDataProcessor;
import de.dfki.lt.signalproc.process.LPCWhisperiser;
import de.dfki.lt.signalproc.process.Robotiser;
import de.dfki.lt.signalproc.process.Chorus;
import de.dfki.lt.signalproc.process.VocalTractScalingProcessor;
import de.dfki.lt.signalproc.process.VocalTractScalingSimpleProcessor;
import de.dfki.lt.signalproc.process.VocalTractModifier;
import de.dfki.lt.signalproc.process.Robotiser.PhaseRemover;
import de.dfki.lt.signalproc.filter.*;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.demo.OnlineAudioEffects;
import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.mary.util.MaryAudioUtils;

/**
 *
 * @author  oytun.turk
 */

public class ChangeMyVoiceUI extends javax.swing.JFrame {
    File outputFile;
    private int TOTAL_BUILT_IN_TTS_FILES;
    private double amount;
    private int targetIndex;
    private int inputIndex;
    private int recordIndex;
    private boolean bStarted;
    private boolean bRecording;
    private boolean bPlaying;
    OnlineAudioEffects online;
    TargetDataLine microphone;
    SourceDataLine loudspeakers;
    AudioInputStream inputStream;
    BufferingRecorder recorder;
    Clip m_clip;
    String playFile;
    
    private Vector listItems; //Just the names we see on the list
    private File lastDirectory;
    private File inputFile;
    private String [] inputFileNameList; //Actual full paths to files 
    private String [] builtInFileNameList;
    private String classPath; //Class run-time path
    private String strBuiltInFilePath;
    private String strRecordPath;
    
    VoiceModificationParameters modificationParameters;
    String [] targetNames = { "Robot", 
                              "Whisper", 
                              "Dwarf1",
                              "Dwarf2",
                              "Ogre1",
                              "Ogre2",
                              "Giant1",
                              "Giant2",
                              "Echo",
                              "Stadium",
                              "Jet Pilot", 
                              "Old Radio", 
                              "Telephone"
                              }; 
    
    /** Creates new form ChangeMyVoiceUI */
    public ChangeMyVoiceUI() {
        playFile = null;
        recorder = null;
        outputFile = null;
        microphone = null;
        loudspeakers = null;
        inputStream = null;
        targetIndex = -1;
        inputIndex = -1;
        inputFile = null;
        bRecording = false;
        bPlaying = false;
        lastDirectory = null;
        inputFileNameList = null;
        listItems = new Vector();
        recordIndex = 0;
        
        classPath = new File(".").getAbsolutePath();
        strBuiltInFilePath = classPath + "/java/de/dfki/lt/signalproc/demo/demo/";
        strRecordPath = classPath + "/java/de/dfki/lt/signalproc/demo/demo/record/";
        
        listItems.addElement("Streaming Audio");    
        
        TOTAL_BUILT_IN_TTS_FILES = 6;
        builtInFileNameList = new String[TOTAL_BUILT_IN_TTS_FILES];
        
        listItems.addElement("Built-in TTS Output1 (wohin-bits3.wav)");
        builtInFileNameList[0] = strBuiltInFilePath + "wohin-bits3.wav";
        
        listItems.addElement("Built-in TTS Output2 (herta-neutral.wav)");
        builtInFileNameList[1] = strBuiltInFilePath + "herta-neutral.wav";
        
        listItems.addElement("Built-in TTS Output3 (herta-excited.wav)");
        builtInFileNameList[2] = strBuiltInFilePath + "herta-excited.wav";
        
        listItems.addElement("Built-in TTS Output4 (ausprobieren-bits3.wav)");
        builtInFileNameList[3] = strBuiltInFilePath + "ausprobieren-bits3.wav";
        
        listItems.addElement("Built-in TTS Output5 (ausprobieren.wav)");
        builtInFileNameList[4] = strBuiltInFilePath + "ausprobieren.wav";
        
        listItems.addElement("Built-in TTS Output6 (moment.wav)");
        builtInFileNameList[5] = strBuiltInFilePath + "moment.wav";
        
        initComponents();
        modificationParameters = new VoiceModificationParameters();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jComboBoxTargetVoice = new javax.swing.JComboBox();
        jButtonExit = new javax.swing.JButton();
        jLabelTargetVoice = new javax.swing.JLabel();
        jButtonAdd = new javax.swing.JButton();
        jButtonStart = new javax.swing.JButton();
        jButtonDel = new javax.swing.JButton();
        jButtonPlay = new javax.swing.JButton();
        jLabelLow = new javax.swing.JLabel();
        jScrollList = new javax.swing.JScrollPane();
        jListInput = new javax.swing.JList();
        jLabelChangeAmount = new javax.swing.JLabel();
        jLabelHigh = new javax.swing.JLabel();
        jSliderChangeAmount = new javax.swing.JSlider();
        jLabelInput = new javax.swing.JLabel();
        jButtonRec = new javax.swing.JButton();
        jLabelMedium = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Change My Voice");
        setResizable(false);
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jComboBoxTargetVoice.setMaximumRowCount(20);
        jComboBoxTargetVoice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxTargetVoiceActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 10);
        getContentPane().add(jComboBoxTargetVoice, gridBagConstraints);

        jButtonExit.setText("Exit");
        jButtonExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonExitActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        getContentPane().add(jButtonExit, gridBagConstraints);

        jLabelTargetVoice.setText("Target Voice");
        jLabelTargetVoice.setName("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(jLabelTargetVoice, gridBagConstraints);

        jButtonAdd.setText("Add");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        getContentPane().add(jButtonAdd, gridBagConstraints);

        jButtonStart.setText("Start");
        jButtonStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStartActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 20, 0);
        getContentPane().add(jButtonStart, gridBagConstraints);

        jButtonDel.setText("Del");
        jButtonDel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDelActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(jButtonDel, gridBagConstraints);

        jButtonPlay.setText("Play");
        jButtonPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        getContentPane().add(jButtonPlay, gridBagConstraints);

        jLabelLow.setText("Low");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(jLabelLow, gridBagConstraints);

        jListInput.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListInput.setPreferredSize(new java.awt.Dimension(0, 100));
        jListInput.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListInputValueChanged(evt);
            }
        });
        jListInput.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jListInputMouseClicked(evt);
            }
        });

        jScrollList.setViewportView(jListInput);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipady = 200;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        getContentPane().add(jScrollList, gridBagConstraints);

        jLabelChangeAmount.setText("Change Amount");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        getContentPane().add(jLabelChangeAmount, gridBagConstraints);

        jLabelHigh.setText("High");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        getContentPane().add(jLabelHigh, gridBagConstraints);

        jSliderChangeAmount.setMajorTickSpacing(50);
        jSliderChangeAmount.setMinorTickSpacing(5);
        jSliderChangeAmount.setPaintTicks(true);
        jSliderChangeAmount.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderChangeAmountStateChanged(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 154;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        getContentPane().add(jSliderChangeAmount, gridBagConstraints);

        jLabelInput.setText("Input");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(20, 10, 0, 0);
        getContentPane().add(jLabelInput, gridBagConstraints);

        jButtonRec.setText("Rec");
        jButtonRec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        getContentPane().add(jButtonRec, gridBagConstraints);

        jLabelMedium.setText("Medium");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(jLabelMedium, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 30;
        getContentPane().add(jLabel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.ipadx = 30;
        getContentPane().add(jLabel2, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-382)/2, (screenSize.height-560)/2, 382, 560);
    }// </editor-fold>//GEN-END:initComponents

    private void jListInputMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jListInputMouseClicked
        int numClicks = evt.getClickCount();
        if (numClicks==2)
        {
            getInputIndex();
            
            if (inputIndex>0)
            {
                if (!bPlaying && !bRecording)
                    jButtonPlay.doClick();
            }
            else if (inputIndex==0 && !bStarted)
                jButtonStart.doClick();   
        }
    }//GEN-LAST:event_jListInputMouseClicked

    private Clip playClip = null;
    private void jButtonPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayActionPerformed
        if (!bRecording && playFile!=null)
        {
            if (!bPlaying)
            {
                bPlaying = true;
                try
                {
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(playFile));
                    AudioFormat format = audioInputStream.getFormat();
                    DataLine.Info lineInfo = new DataLine.Info(Clip.class, format);
                    playClip = (Clip) AudioSystem.getLine(lineInfo);
                    playClip.addLineListener(new LineListener() {
                        public void update(LineEvent le) {
                            if (le.getType().equals(LineEvent.Type.STOP)) {
                                bPlaying = false;
                                playClip = null;
                                updateGUIPlaying();
                            }
                        }
                    });
                    playClip.open(audioInputStream);
                    playClip.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                bPlaying = false; 
                if (playClip != null) {
                    playClip.stop();
                    playClip = null;
                }
            }
            updateGUIPlaying();
        }
    }//GEN-LAST:event_jButtonPlayActionPerformed

    private void updateGUIPlaying()
    {
        if (bPlaying) {
            jButtonPlay.setText("Stop");
        } else {
            jButtonPlay.setText("Play");
        }
        jButtonRec.setEnabled(!bPlaying);
        jButtonAdd.setEnabled(!bPlaying);
        jButtonDel.setEnabled(!bPlaying);
        jListInput.setEnabled(!bPlaying);
        jButtonStart.setEnabled(!bPlaying);
        
    }
    
    
    private void jButtonDelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDelActionPerformed
        if (inputIndex>=TOTAL_BUILT_IN_TTS_FILES+1)
        {
            listItems.remove(inputIndex);
            inputIndex--;
            UpdateInputList();
        }
    }//GEN-LAST:event_jButtonDelActionPerformed

    private void jListInputValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListInputValueChanged
        
        getInputIndex();
        
        if (inputIndex==0)
            jButtonPlay.setEnabled(false);
        else
            jButtonPlay.setEnabled(true);
        
        if (inputIndex<this.TOTAL_BUILT_IN_TTS_FILES+1)
            jButtonDel.setEnabled(false);
        else
            jButtonDel.setEnabled(true);
        
        if (inputIndex <= 0)
            playFile = null;
        else if (inputIndex>this.TOTAL_BUILT_IN_TTS_FILES)
            playFile = (String)listItems.get(inputIndex);
        else
            playFile = builtInFileNameList[inputIndex-1];
        
    }//GEN-LAST:event_jListInputValueChanged

    //Browse for a new wav file
    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        JFileChooser fc = new JFileChooser();
        if (lastDirectory != null) {
            fc.setCurrentDirectory(lastDirectory);
        }

        FileFilter ff = new SimpleFileFilter("wav", "Wav Files (*.wav)");
        fc.addChoosableFileFilter(ff);
        
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) 
        {
            inputFile = fc.getSelectedFile();
            lastDirectory = inputFile.getParentFile();
            listItems.add(inputFile.getPath()); //Keep full path
            UpdateInputList();
            jListInput.setSelectedIndex(listItems.size()-1);
        }
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonRecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecActionPerformed
        if (!bRecording) //Start recording
        {
            if (recorder != null)
            {
                recorder.stopRecording();
                recorder = null;
            }
            
            int channels = 1;
            
            recordIndex++;
            String strRecordIndex = Integer.toString(recordIndex);
            while (strRecordIndex.length()<5)
                strRecordIndex = "0" + strRecordIndex;
            
            String strFilename = strRecordPath + "NewFile_" + strRecordIndex + ".wav";
            outputFile = new File(strFilename);

            AudioFormat audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                modificationParameters.fs, 16, channels, 2*channels, modificationParameters.fs, false);

            AudioFileFormat.Type    targetType = AudioFileFormat.Type.WAVE;

            if (microphone != null)
                microphone.close();

            microphone = getMicrophone(audioFormat);

            recorder = new BufferingRecorder(microphone, targetType, outputFile, 0);

            bRecording = true;
            jButtonRec.setText("Stop");
            
            recorder.start();
        }
        else //Stop recording
        {
            recorder.stopRecording();
            recorder = null;
            microphone.close();
            microphone = null;
            
            bRecording = false;
            jButtonRec.setText("Rec");
            
            inputFile = outputFile;
            listItems.add(inputFile.getPath()); //Keep full path
            UpdateInputList();
            jListInput.setSelectedIndex(listItems.size()-1);
        }
        
        jButtonPlay.setEnabled(!bRecording);
        jButtonAdd.setEnabled(!bRecording);
        jButtonDel.setEnabled(!bRecording);
        jListInput.setEnabled(!bRecording);
        jButtonStart.setEnabled(!bRecording);
        
    }//GEN-LAST:event_jButtonRecActionPerformed

    private void jSliderChangeAmountStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderChangeAmountStateChanged
        double prevAmount = amount;

        getAmount();

        if (bStarted && Math.abs(prevAmount-amount)>0.001) //If currently processing and changed modification amount
        {
            jButtonStart.doClick(); //Stop
            jButtonStart.doClick(); //and restart to adapt to new target voice
        }
    }//GEN-LAST:event_jSliderChangeAmountStateChanged

    private void jButtonExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonExitActionPerformed
    System.exit(0);
    }//GEN-LAST:event_jButtonExitActionPerformed

    private void jComboBoxTargetVoiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxTargetVoiceActionPerformed
        int prevTargetIndex = targetIndex;

        getTargetIndex();

        if (bStarted && prevTargetIndex != targetIndex) //If currently processing and changed target voice type
        {
            jButtonStart.doClick(); //Stop
            jButtonStart.doClick(); //and restart to adapt to new target voice
        }
    }//GEN-LAST:event_jComboBoxTargetVoiceActionPerformed
    
    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        
    }//GEN-LAST:event_formMouseClicked

   public void getTargetIndex()
   {
       targetIndex = jComboBoxTargetVoice.getSelectedIndex();
       if (targetNames[targetIndex]=="Telephone")
           modificationParameters.fs = 8000;
       else
           modificationParameters.fs = 16000;
       
       boolean bChangeEnabled = true;
       if (targetNames[targetIndex]=="Jet Pilot" || 
           targetNames[targetIndex]=="Old Radio" ||
           targetNames[targetIndex]=="Telephone")
       {
           bChangeEnabled = false;
       }
       
       jLabelChangeAmount.setEnabled(bChangeEnabled);
       jLabelLow.setEnabled(bChangeEnabled);
       jLabelMedium.setEnabled(bChangeEnabled);
       jLabelHigh.setEnabled(bChangeEnabled);
       jSliderChangeAmount.setEnabled(bChangeEnabled);
       
       if (targetNames[targetIndex]=="Robot")
           jLabelChangeAmount.setText("Pitch");
       else
           jLabelChangeAmount.setText("Change Amount");
   }
   
    private void jButtonStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonStartActionPerformed
        if (!bStarted)
        { 
            bStarted = true;
            updateGUIStart();
            getParameters();
            changeVoice();
        } else {
            bStarted = false;
            updateGUIStart();
            online.requestStop();

            //Close the source and the target datalines to be able to use them repeatedly
            if (microphone!=null)
            {
                microphone.close();
                microphone = null;
            }

            if (loudspeakers != null)
            {
                loudspeakers.close();
                loudspeakers = null;
            }

            if (inputStream != null)
            {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                inputStream = null;
            }
            //

            jButtonStart.setText("Start");
            jButtonRec.setEnabled(true);
            jButtonPlay.setEnabled(true);
            jButtonAdd.setEnabled(true);
            if (inputIndex>TOTAL_BUILT_IN_TTS_FILES)
                jButtonDel.setEnabled(true);
            jListInput.setEnabled(true);

        }   
    }//GEN-LAST:event_jButtonStartActionPerformed

    private void updateGUIStart()
    {
        if (bStarted)
        { 
            jButtonStart.setText("Stop");
        } else {
            jButtonStart.setText("Start");
        }
        jButtonRec.setEnabled(!bStarted);
        jButtonPlay.setEnabled(!bStarted);
        jButtonAdd.setEnabled(!bStarted);
        jButtonDel.setEnabled(!bStarted && inputIndex>TOTAL_BUILT_IN_TTS_FILES);
        jListInput.setEnabled(!bStarted);
    }
    
    /* This function gets the modification parameters from the GUI
     * and fills in the modificationParameters object
    */ 
    private void getParameters() {
        getInputIndex();
        getTargetIndex();
        getAmount();
    }
    
    /*This function opens source and target datalines and starts real-time voice modification  
     * using the parameters in the modificationParameters object
     */ 
    private void changeVoice() {
        int channels = 1;

        AudioFormat audioFormat = null;

        if (inputIndex == 0) //Online processing using microphone
        {
            audioFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, modificationParameters.fs, 16, channels, 2*channels, modificationParameters.fs,
                    false);

            if (microphone != null)
                microphone.close();

            microphone = getMicrophone(audioFormat);
            
            if (microphone != null)
            {
                audioFormat = microphone.getFormat();
                modificationParameters.fs = (int)audioFormat.getSampleRate();
            }

        }
        else //Online processing using pre-recorded wav file
        {
            if (inputIndex>0)
            {
                if (inputIndex>this.TOTAL_BUILT_IN_TTS_FILES)
                {
                    String inputFileNameFull = (String)listItems.get(inputIndex);
                    inputFile = new File(inputFileNameFull);
                }
                else
                    inputFile = new File(builtInFileNameList[inputIndex-1]); 
            }
            else
                inputFile = null;
            
            if (inputFile != null)
            {
                try {
                    inputStream = AudioSystem.getAudioInputStream(inputFile);
                } catch (UnsupportedAudioFileException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (inputStream != null)
            {
                audioFormat = inputStream.getFormat();
                modificationParameters.fs = (int)audioFormat.getSampleRate();
            }

        }

        if (loudspeakers != null)
            loudspeakers.close();

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                    audioFormat);
            loudspeakers = (SourceDataLine) AudioSystem.getLine(info);
            loudspeakers.open(audioFormat);
            System.out.println("Loudspeaker format: " + loudspeakers.getFormat());
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        // Choose an audio effect
        InlineDataProcessor effect = null;
        int bufferSize = SignalProcUtils.getDFTSize(modificationParameters.fs);

        if (targetNames[targetIndex]=="Robot")
        {  
            double targetHz = 200+(amount-0.5)*200;
            bufferSize = (int) (modificationParameters.fs / targetHz * 4 /*-fold overlap in ola*/ );

            effect = new Robotiser.PhaseRemover(MathUtils.closestPowerOfTwoAbove(bufferSize), 1.0);
        }
        else if (targetNames[targetIndex]=="Whisper")
        {  
            effect = new LPCWhisperiser(SignalProcUtils.getLPOrder((int)modificationParameters.fs), 0.4+0.6*amount);
        }
        else if (targetNames[targetIndex]=="Dwarf1") //Using freq. domain LP spectrum modification
        {  
            double [] vscales = {1.3+0.5*amount};
            int p = SignalProcUtils.getLPOrder((int)modificationParameters.fs);
            //int fftSize = Math.max(SignalProcUtils.getDFTSize((int)modificationParameters.fs), 1024);
            //effect = new VocalTractScalingProcessor(p, (int)modificationParameters.fs, fftSize, vscales);
            effect = new VocalTractScalingProcessor(p, (int)modificationParameters.fs, bufferSize, vscales);
        }
        else if (targetNames[targetIndex]=="Dwarf2") //Using freq. domain DFT magnitude spectrum modification
        {  
            double [] vscales = {1.3+0.5*amount};
            //effect = new VocalTractScalingSimpleProcessor(1024, vscales);
            effect = new VocalTractScalingSimpleProcessor(bufferSize, vscales);
        }
        else if (targetNames[targetIndex]=="Ogre1") //Using freq. domain LP spectrum modification
        { 
            double [] vscales = {0.90-0.1*amount};            
            int p = SignalProcUtils.getLPOrder((int)modificationParameters.fs);
            //int fftSize = Math.max(SignalProcUtils.getDFTSize((int)modificationParameters.fs), 1024);
            //effect = new VocalTractScalingProcessor(p, (int)modificationParameters.fs, fftSize, vscales);
            effect = new VocalTractScalingProcessor(p, (int)modificationParameters.fs, bufferSize, vscales);
        }
        else if (targetNames[targetIndex]=="Ogre2") //Using freq. domain DFT magnitude spectrum modification
        { 
            double [] vscales = {0.90-0.1*amount};
            //effect = new VocalTractScalingSimpleProcessor(1024, vscales);
            effect = new VocalTractScalingSimpleProcessor(bufferSize, vscales);
        }
        else if (targetNames[targetIndex]=="Giant1") //Using freq. domain LP spectrum modification
        {  
            double [] vscales = {0.75-0.1*amount};
            int p = SignalProcUtils.getLPOrder((int)modificationParameters.fs);
            //int fftSize = Math.max(SignalProcUtils.getDFTSize((int)modificationParameters.fs), 1024);
            //effect = new VocalTractScalingProcessor(p, (int)modificationParameters.fs, fftSize, vscales);
            effect = new VocalTractScalingProcessor(p, (int)modificationParameters.fs, bufferSize, vscales);
        }
        else if (targetNames[targetIndex]=="Giant2") //Using freq. domain DFT magnitude spectrum modification
        {  
            double [] vscales = {0.75-0.1*amount};
            //effect = new VocalTractScalingSimpleProcessor(1024, vscales);
            effect = new VocalTractScalingSimpleProcessor(bufferSize, vscales);
        }
        else if (targetNames[targetIndex]=="Echo")
        {
            int [] delaysInMiliseconds = {100+(int)(20*amount), 200+(int)(50*amount), 300+(int)(100*amount)};
            double [] amps = {0.8, -0.7, 0.9};
            
            int maxDelayInMiliseconds = MathUtils.getMax(delaysInMiliseconds);
            int maxDelayInSamples = (int)(maxDelayInMiliseconds/1000.0*modificationParameters.fs);
            
            if (bufferSize<maxDelayInSamples)
                bufferSize *= 2;
                
            effect = new Chorus(delaysInMiliseconds, amps, (int)(modificationParameters.fs));
        }
        else if (targetNames[targetIndex]=="Stadium")
        {
            int [] delaysInMiliseconds = {266+(int)(200*amount), 400+(int)(200*amount)};
            double [] amps = {0.54, -0.10};
            
            int maxDelayInMiliseconds = MathUtils.getMax(delaysInMiliseconds);
            int maxDelayInSamples = (int)(maxDelayInMiliseconds/1000.0*modificationParameters.fs);
            
            if (bufferSize<maxDelayInSamples)
                bufferSize *= 2;
            
            effect = new Chorus(delaysInMiliseconds, amps, (int)(modificationParameters.fs));
        }
        else if (targetNames[targetIndex]=="Jet Pilot")
        {  
            bufferSize = 8*bufferSize;
            double normalizedCutOffFreq1 = 500.0/modificationParameters.fs;
            double normalizedCutOffFreq2 = 2000.0/modificationParameters.fs;
            effect = new BandPassFilter(normalizedCutOffFreq1, normalizedCutOffFreq2, true);
        }
        else if (targetNames[targetIndex]=="Telephone")
        {  
            bufferSize = 8*bufferSize;
            double normalizedCutOffFreq1 = 300.0/modificationParameters.fs;
            double normalizedCutOffFreq2 = 3400.0/modificationParameters.fs;
            effect = new BandPassFilter(normalizedCutOffFreq1, normalizedCutOffFreq2, true);
        }
        else if (targetNames[targetIndex]=="Old Radio")
        {  
            bufferSize = 8*bufferSize;
            double normalizedCutOffFreq = 3000.0/modificationParameters.fs;
            effect = new LowPassFilter(normalizedCutOffFreq, true);
        }
        //            

        // Create the output thread and make it run in the background:
        if (effect!=null && loudspeakers!=null)
        {
            if (microphone != null)
                online = new OnlineAudioEffects(effect, microphone, loudspeakers, bufferSize);
            else if (inputStream !=null) {
                loudspeakers.addLineListener(new LineListener() {
                   public void update(LineEvent le) {
                       if (le.getType().equals(LineEvent.Type.STOP)) {
                           bStarted = false;
                           updateGUIStart();
                       }
                   }
                });
                online = new OnlineAudioEffects(effect, inputStream, loudspeakers, bufferSize);
                
            }

            online.start();
        }
    }
    
    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        int i;
        
        //Move the window to somewhere closer to middle o screen
        Point p = this.getLocation();
        Dimension d = this.getSize();
        p.x = (int)(0.5*(1500-d.getWidth()));
        p.y = (int)(0.5*(1000-d.getHeight()));
        this.setLocation(p);
        //
        
        bStarted = false;
        
        //Fill-in target voice combo-box
        for (i=0; i<targetNames.length; i++) {
            jComboBoxTargetVoice.addItem(targetNames[i]);
        }
        //
        
        //Fill-in input combo-box
        inputIndex = 0;
        UpdateInputList();
        //
        
        getParameters();
        
    }//GEN-LAST:event_formWindowOpened
    
    public void UpdateInputList()
    {
        File fTmp;
        int i;
        inputFileNameList = new String[listItems.size()];
        
        for (i=0; i<listItems.size(); i++)
        {
            fTmp = new File((String)listItems.get(i));
            inputFileNameList[i] = fTmp.getName();
        }
        
        inputIndex = Math.min(listItems.size()-1, inputIndex);
        inputIndex = Math.max(0, inputIndex);
        
        int prevInputIndex = inputIndex;
        
        jListInput.setListData(inputFileNameList);
        
        inputIndex = prevInputIndex;
        
        jListInput.setSelectedIndex(inputIndex);
    }
    
    public void getAmount()
    { 
        amount = (((double)jSliderChangeAmount.getValue())-jSliderChangeAmount.getMinimum())/(((double)jSliderChangeAmount.getMaximum())-jSliderChangeAmount.getMinimum());
        amount = Math.min(amount, 1.0);
        amount = Math.max(amount, 0.0); 
    }
    
    public void getInputIndex()
    {
        inputIndex = jListInput.getSelectedIndex();
    }
    
    private TargetDataLine getMicrophone(AudioFormat preferredFormat)
    {
        TargetDataLine line = null;
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, preferredFormat);
            int mixerRequested = Integer.getInteger("mixer", -1).intValue();
            if (mixerRequested == -1) { // no specific mixer requested
                line = (TargetDataLine) AudioSystem.getLine(info);
            } else {
                Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
                Mixer.Info mixerInfo = mixerInfos[mixerRequested];
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] lineInfos = mixer.getTargetLineInfo();
                Line.Info lineInfo = lineInfos[0];
                DataLine.Info datalineInfo = (DataLine.Info) lineInfo;
                line = (TargetDataLine) mixer.getLine(lineInfo);
            }
            DataLine.Info lineInfo = (DataLine.Info) line.getLineInfo();
            AudioFormat lineFormat = null;
            if (lineInfo.isFormatSupported(preferredFormat)) {
                lineFormat = preferredFormat;
            } else {
                System.err.println("Preferred format not supported: "+preferredFormat);
                AudioFormat[] formats = lineInfo.getFormats();
                for (int i=formats.length-1; i >= 0; i--) {
                    if (formats[i].getChannels() == 1
                        && formats[i].getFrameSize() == 2) {
                        lineFormat = formats[i];
                        break;
                    }
                }
                System.err.println("Using instead: "+lineFormat);
            }
            if (lineFormat == null) {
                throw new LineUnavailableException("Cannot get any mono line with 16 bit");
            }
            line.open(lineFormat, 4096);

        } catch (LineUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return line;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ChangeMyVoiceUI().setVisible(true);
            }
        });
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonDel;
    private javax.swing.JButton jButtonExit;
    private javax.swing.JButton jButtonPlay;
    private javax.swing.JButton jButtonRec;
    private javax.swing.JButton jButtonStart;
    private javax.swing.JComboBox jComboBoxTargetVoice;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabelChangeAmount;
    private javax.swing.JLabel jLabelHigh;
    private javax.swing.JLabel jLabelInput;
    private javax.swing.JLabel jLabelLow;
    private javax.swing.JLabel jLabelMedium;
    private javax.swing.JLabel jLabelTargetVoice;
    private javax.swing.JList jListInput;
    private javax.swing.JScrollPane jScrollList;
    private javax.swing.JSlider jSliderChangeAmount;
    // End of variables declaration//GEN-END:variables
}
