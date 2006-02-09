/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.client;

// General Java Classes
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;

import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.audio.JavaStreamingAudioPlayer;

import de.dfki.lt.mary.util.MaryUtils;


/**
 * A GUI Interface to the Mary Client, allowing to access and modify
 * intermediate processing results.
 * @author Marc Schr&ouml;der
 * @see MaryClient The client implementation
 */

public class MaryExpertInterface extends JPanel
    implements ActionListener, DocumentListener, ItemListener, MaryClient.AudioPlayerListener
{

    /* -------------------- GUI stuff -------------------- */

    // Input
    private JComboBox cbInputType;
    private JScrollPane inputScrollPane;
    private JTextPane inputText;
    private boolean doReplaceInput = true;
    // When the user changes input type, he is offered an example text for
    // the new input type. In order to prevent this when setting a new input
    // type from within the program, doReplaceInput must be set to false
    // before triggering the selection changed event.

    // Output
    private boolean showingTextOutput = true;
    private JComboBox cbOutputType;
    private JButton bSaveOutput;
    private JTextPane outputText;
    private JScrollPane outputScrollPane;
    private JPanel audioPanel;
    private JButton bPlay;

    // Processing Buttons
    private JButton bProcess;
    private JButton bEdit;
    private JButton bCompare;
    private JComboBox cbDefaultVoice;
    private JComboBox cbVoiceExampleText;
    
    // Layout
    private GridBagLayout gridBag = new GridBagLayout();
    private GridBagConstraints gridC = new GridBagConstraints();

    /* -------------------- Data and Processing stuff -------------------- */
    private MaryClient processor;

    private AudioPlayer audioPlayer = null;
    private Vector availableVoices = null;
    private Vector inputTypes = null;
    private Vector outputTypes = null;
    private boolean allowSave;
    private boolean streamMp3 = Boolean.getBoolean("stream.mp3");
    
    //Map of limited Domain Voices and their example Texts
    private Map limDomVoices = new HashMap();

    /**
     * Create a MaryExpertInterface instance that connects to the server host
     * and port as specified in the system properties "server.host" and "server.port",
     * which default to "cling.dfki.uni-sb.de" and 59125, respectively.
     * @throws IOException
     * @throws UnknownHostException
     */
    public MaryExpertInterface() throws IOException, UnknownHostException
    {
        super();
        // First the MaryClient processor class, because it may provide
        // information needed in the GUI creation.
        try {
            processor = new MaryClient();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    e.getMessage(),
                    "Cannot connect to server",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        allowSave = true;
        init();
    }

    /**
     * Create a MaryExpertInterface instance that connects to the given server host
     * and port. This is meant to be used from Applets.
     * @param host
     * @param port
     * @throws IOException
     * @throws UnknownHostException
     */
    public MaryExpertInterface(String host, int port) throws IOException, UnknownHostException
    {
        super();
        // First the MaryClient processor class, because it may provide
        // information needed in the GUI creation.
        try {
            processor = new MaryClient(host, port, false, false);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    e.getMessage(),
                    "Cannot connect to server",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        allowSave = false;
        init();
    }

    /**
     * Create an instance of the MaryClient class which does the processing,
     * and initialise the GUI.
     */
    public void init() throws IOException, UnknownHostException {
        
        Dimension scrollPaneDimension = new Dimension(250,400);
        
        gridC.insets = new Insets( 2,2,2,2 );
        gridC.weightx = 0.1;
        gridC.weighty = 0.1;
        setLayout( gridBag );

        // Input type
        JLabel inputTypeLabel = new JLabel( "Input Type: " );
        gridC.gridx = 0;
        gridC.gridy = 0;
        gridBag.setConstraints( inputTypeLabel, gridC );
        add( inputTypeLabel );
        inputTypes = processor.getInputDataTypes();
        outputTypes = processor.getOutputDataTypes();
        assert inputTypes.size() > 0;
        assert outputTypes.size() > 0;
        cbInputType = new JComboBox( inputTypes );
        cbInputType.setToolTipText( "Specify the type of data contained " +
                                    "in the input text area below." );
        cbInputType.addItemListener(this);
        gridC.gridx = 1;
        gridC.gridy = 0;
        gridC.gridwidth = 2;
        gridBag.setConstraints( cbInputType, gridC );
        add( cbInputType );
        gridC.gridwidth = 1;

        //Input Text area
        inputText = new JTextPane();
        inputText.getDocument().addDocumentListener(this);
        inputScrollPane = new JScrollPane(inputText);
        inputScrollPane.setMinimumSize(scrollPaneDimension);
        inputScrollPane.setPreferredSize(scrollPaneDimension);
        gridC.gridx = 0;
        gridC.gridy = 1;
        gridC.gridwidth = 3;
        gridC.gridheight = 2;
        gridC.weightx = 0.4;
        gridC.weighty = 0.8;
        //gridC.ipadx = 270;
        //gridC.ipady = 200;
        gridC.fill = GridBagConstraints.BOTH;
        gridBag.setConstraints( inputScrollPane, gridC );
        add( inputScrollPane );
        gridC.gridwidth = 1;
        gridC.gridheight = 1;
        gridC.weightx = 0.1;
        gridC.weighty = 0.1;
        gridC.ipadx = 0;
        gridC.ipady = 0;
        gridC.fill = GridBagConstraints.NONE;

        //example text for limDom voices
        cbVoiceExampleText = new JComboBox();
        
        cbVoiceExampleText.addItemListener(this);
        gridC.gridx = 0;
        gridC.gridy = 3;
        gridC.gridwidth = 3;
        gridC.fill = GridBagConstraints.HORIZONTAL;
        gridBag.setConstraints( cbVoiceExampleText, gridC );
        add( cbVoiceExampleText );     

        // Output type
        JLabel outputTypeLabel = new JLabel( "Output Type: " );
        gridC.gridx = 4;
        gridC.gridy = 0;
        gridBag.setConstraints( outputTypeLabel, gridC );
        add( outputTypeLabel );
        cbOutputType = new JComboBox();
        setOutputTypeItems();
        // The last possible output type (= audio) is the default
        // output type:
        cbOutputType.setSelectedIndex(cbOutputType.getItemCount() - 1);
        cbOutputType.setToolTipText( "Specify the output type for the next " +
                                     "processing action (Process button)." );
        cbOutputType.addItemListener(this);
        gridC.gridx = 5;
        gridC.gridy = 0;
        gridC.gridwidth = 2;
        gridBag.setConstraints( cbOutputType, gridC );
        add( cbOutputType );
        gridC.gridwidth = 1;

        // Output Text area
        if (((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType())
            showingTextOutput = true;
        else
            showingTextOutput = false;
        outputText = new JTextPane();
        //        outputText.setLineWrap(false);
        outputText.setEditable(false);
        outputScrollPane = new JScrollPane(outputText);
        outputScrollPane.setMinimumSize(scrollPaneDimension);
        outputScrollPane.setPreferredSize(scrollPaneDimension);
        gridC.gridx = 4;
        gridC.gridy = 1;
        gridC.gridwidth = 3;
        gridC.gridheight = 3;
        gridC.weightx = 0.4;
        gridC.weighty = 0.8;
        //gridC.ipadx = 270;
        //gridC.ipady = 200;
        gridC.fill = GridBagConstraints.BOTH;
        gridBag.setConstraints( outputScrollPane, gridC );
        if (!showingTextOutput)
            outputScrollPane.setVisible(false);
        add( outputScrollPane );
        gridC.gridwidth = 1;
        gridC.gridheight = 1;
        gridC.weightx = 0.1;
        gridC.weighty = 0.1;
        gridC.ipadx = 0;
        gridC.ipady = 0;
        gridC.fill = GridBagConstraints.NONE;
        // Overlapping location: Audio play button
        audioPanel = new JPanel();
        //audioPanel.setPreferredSize(new Dimension(300,400));
        audioPanel.setLayout( new BorderLayout() );
        bPlay = new JButton( "Play" );
        bPlay.setToolTipText( "Synthesize and play the resulting audio stream." );
        bPlay.setActionCommand( "play" );
        bPlay.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (audioPlayer != null) { // an audioPlayer is currently playing
                    audioPlayer.cancel();
                    audioPlayer = null;
                    bPlay.setText("Play");
                } else {
                    processInput();
                }

            }
        });
        audioPanel.add(bPlay, BorderLayout.CENTER);
        bPlay.setMaximumSize(bPlay.getPreferredSize());
        if (showingTextOutput)
            audioPanel.setVisible(false);
        gridC.gridx = 4;
        gridC.gridy = 2;
        gridC.gridwidth = 3;
        gridBag.setConstraints( audioPanel, gridC );
        add( audioPanel );
        gridC.gridwidth = 1;

        // Action buttons in centre
        bProcess = new JButton( "Process ->" );
        bProcess.setToolTipText( "Call the Mary Server." +
                                 "The input will be transformed into the specified output type." );
        bProcess.setActionCommand( "process" );
        bProcess.addActionListener( this );
        gridC.gridx = 3;
        gridC.gridy = 1;
        gridBag.setConstraints( bProcess, gridC );
        add( bProcess );
        bEdit = new JButton( "<- Edit" );
        bEdit.setToolTipText( "Edit the content of the output text area as the new input." +
                              " The current content of the input text area will be discarded." );
        bEdit.setActionCommand( "edit" );
        bEdit.addActionListener( this );
        gridC.gridx = 3;
        gridC.gridy = 2;
        gridBag.setConstraints( bEdit, gridC );
        add( bEdit );
        bCompare = new JButton( "<- Compare ->" );
        bCompare.setToolTipText( "Compare input and output" +
                               "(available only if both are MaryXML types)." );
        bCompare.setActionCommand( "compare" );
        bCompare.addActionListener( this );
        gridC.gridx = 3;
        gridC.gridy = 3;
        gridBag.setConstraints( bCompare, gridC );
        add( bCompare );

        // Input related action buttons
        JLabel voiceLabel = new JLabel("default voice:");
        gridC.gridx = 1;
        gridC.gridy = 4;
        gridBag.setConstraints( voiceLabel, gridC );
        add( voiceLabel );
        Locale inputLocale = ((MaryClient.DataType)cbInputType.getSelectedItem()).getLocale();
        // Get the voices 
        availableVoices = processor.getVoices();
        cbDefaultVoice = new JComboBox();
        
        Iterator it = availableVoices.iterator();
        while (it.hasNext()) {
            MaryClient.Voice v = (MaryClient.Voice) it.next();
            if (v.getLocale().equals(inputLocale))
                cbDefaultVoice.addItem(v);
            if (v.isLimitedDomain()){
                String exampleText = processor.getVoiceExampleText(v.name());
                limDomVoices.put(v.name(),
                        processVoiceExampleText(exampleText));
            }
        }
        
        // First in list is default voice:
        cbDefaultVoice.setSelectedIndex(0);
        gridC.gridx = 2;
        gridC.gridy = 4;
        gridBag.setConstraints( cbDefaultVoice, gridC );
        add( cbDefaultVoice );
        cbDefaultVoice.addItemListener(this);
        MaryClient.Voice defaultVoice = (MaryClient.Voice)cbDefaultVoice.getItemAt(0);
        MaryClient.DataType inputType = (MaryClient.DataType) cbInputType.getSelectedItem();
        
        if (defaultVoice.isLimitedDomain()) {
            paintLimDom(defaultVoice);
        } else {
            paintGeneralDom(inputType);
        }
        
        if (allowSave) {
            // Output Save button
            ImageIcon saveIcon = new ImageIcon("save.gif");
            bSaveOutput = new JButton( "Save...", saveIcon );
            bSaveOutput.setToolTipText( "Save the output as a file." );
            bSaveOutput.setActionCommand( "saveOutput" );
            bSaveOutput.addActionListener(this);
            gridC.gridx = 4;
            gridC.gridy = 4;
            gridBag.setConstraints( bSaveOutput, gridC );
            add( bSaveOutput );
            setPreferredSize(new Dimension(720,480));
        }

        verifyEnableButtons();
    }

    private void paintGeneralDom(MaryClient.DataType inputType) 
    throws IOException,UnknownHostException
    {
        cbVoiceExampleText.setVisible(false);
        String exampleText = processor.getServerExampleText(inputType.name());
        setInputText(exampleText);
    }
    
    private void paintLimDom(MaryClient.Voice voice)
    {
        Vector sentences = (Vector)limDomVoices.get(voice.name());
        setInputText((String)sentences.get(0));
        cbVoiceExampleText.removeAllItems();
        for (int i = 0; i<sentences.size(); i++) {
            cbVoiceExampleText.addItem(sentences.get(i));
        }
        cbVoiceExampleText.setSelectedIndex(0);
        cbVoiceExampleText.setVisible(true);
    }
    
    private void verifyEnableButtons() {
        if (((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType()) {
            bProcess.setVisible(true);
            bEdit.setVisible(true);
            bCompare.setVisible(true);
        } else { // do not show these three buttons for audio output:
            bProcess.setVisible(false);
            bEdit.setVisible(false);
            bCompare.setVisible(false);
        }
        // Edit button:
        if (showingTextOutput) {
            if (outputText.getText().length() == 0) {
                if (allowSave) bSaveOutput.setEnabled(false);
                bEdit.setEnabled(false);
            } else {
                if (allowSave) bSaveOutput.setEnabled(true);
                bEdit.setEnabled(true);
            }
        } else { // audio output
            if (allowSave) bSaveOutput.setEnabled(true);
        }
        // Compare button:
        // Only enabled if both input and output are text types
        if (((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType() &&
            outputText.getText().length() > 0) {
            bCompare.setEnabled(true);
        } else {
            bCompare.setEnabled(false);
        }
    }

    /**
     * Verify if the language of the input format has changed. If so,
     * adapt the value of cbDefaultVoices.
     */
    private void verifyDefaultVoices() throws IOException{
        Locale inputLocale = ((MaryClient.DataType)cbInputType.getSelectedItem()).getLocale();
        Locale voiceLocale = ((MaryClient.Voice)cbDefaultVoice.getSelectedItem()).getLocale();
        if (inputLocale == null
            || !inputLocale.equals(voiceLocale)) {
            cbDefaultVoice.removeAllItems();
            Iterator it = availableVoices.iterator();
            while (it.hasNext()) {
                MaryClient.Voice v = (MaryClient.Voice) it.next();
                if (inputLocale == null || v.getLocale().equals(inputLocale))
                    cbDefaultVoice.addItem(v);
            }
            cbDefaultVoice.setSelectedIndex(0);
        }
        	
    }

    /**
     * Divides the example text of a voice into
     * sentences in a vector
     * @param text the example text
     * @return vector of example sentences
     */
    private Vector processVoiceExampleText(String text){
        StringTokenizer st = new StringTokenizer(text,"#");
        Vector sentences = new Vector();
        while (st.hasMoreTokens()){
            sentences.add(st.nextToken());}
        return sentences;
    }
    
    private void setOutputTypeItems()
    {
        MaryClient.DataType inputType = (MaryClient.DataType) cbInputType.getSelectedItem();
        Locale inputLocale = inputType.getLocale();
        MaryClient.DataType selectedItem = (MaryClient.DataType) cbOutputType.getSelectedItem();
        cbOutputType.removeAllItems();
        for (Iterator it = outputTypes.iterator(); it.hasNext(); ) {
            MaryClient.DataType d = (MaryClient.DataType) it.next();
            Locale locale = d.getLocale();
            if (inputLocale == null ||
                locale == null ||
                inputLocale.equals(locale)) {
                cbOutputType.addItem(d);
            }
        }
        cbOutputType.setSelectedItem(selectedItem);
    }

    /**
     * This action listener registers to all button pushes.
     * It dispatches the work to the appropriate classes,
     * providing them with the data they need.
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("saveOutput")) {
            saveOutput();
        } else if (e.getActionCommand().equals("process")) {
            processInput();
        } else if (e.getActionCommand().equals("edit")) {
            editOutput();
        } else if (e.getActionCommand().equals("compare")) {
            compareTexts();
        }
        verifyEnableButtons();
    }

    /**
     * This document listener notices all changes in the input text area.
     */
    public void insertUpdate(DocumentEvent e)
    {
        showUnverifiedInput();
    }
    public void removeUpdate(DocumentEvent e)
    {
        showUnverifiedInput();
    }
    public void changedUpdate(DocumentEvent e)
    {
        showUnverifiedInput();
    }
    private void showUnverifiedInput()
    {
    }

    /**
     * This item listener cares for the changes in input/output type
     * combo boxes. It erases the text areas as appropriate.
     */
    public void itemStateChanged(ItemEvent e){
        if (e.getSource() == cbInputType) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                
                if (doReplaceInput == false) {
                    // nothing to be done
                    doReplaceInput = true;
                } else {
                    // confirm before changing input text to example text for
                    // current type
                    int answer = JOptionPane.showConfirmDialog(this,
                         "Do you want to discard the current contents of the input text area?",
                         "Discard input?",
                         JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        // Do replace the input text
                        try {
                            String exampleText = processor.getServerExampleText(((MaryClient.DataType)e.getItem()).name());
                            setInputText(exampleText);
                        } catch (Exception uhe) {
                            // ignore
                        }
                    }
                    try{
                    verifyDefaultVoices();
                    }catch(IOException ioe){}
                }
                setOutputTypeItems();
                verifyEnableButtons();
            }
        } else if (e.getSource() == cbOutputType) {
            // We only care for the newly selected item.
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (((MaryClient.DataType)e.getItem()).isTextType()) {
                    setOutputText(""); // erase the output text
                    if (!showingTextOutput) { // showing Audio Output
                        // need to change output display
                        audioPanel.setVisible(false);
                        outputScrollPane.setVisible(true);
                        showingTextOutput = true;
                        revalidate();
                    }
                } else { // Audio output
                    if (showingTextOutput) {
                        // change output display
                        outputScrollPane.setVisible(false);
                        audioPanel.setVisible(true);
                        showingTextOutput = false;
                        revalidate();
                    }
                }
		verifyEnableButtons();
            } // SELECTED
        }
        else {if (e.getSource() == cbDefaultVoice) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                MaryClient.Voice voice= 
                    (MaryClient.Voice) cbDefaultVoice.getSelectedItem();
                MaryClient.DataType input = 
                    (MaryClient.DataType)cbInputType.getSelectedItem();
                try{
                    if (voice.isLimitedDomain() && input.name().startsWith("TEXT")){
                        paintLimDom(voice);
                    } else {
                        paintGeneralDom(input);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        else {if (e.getSource() == cbVoiceExampleText) {
            	if (e.getStateChange() == ItemEvent.SELECTED) {                
            	    setInputText((String)cbVoiceExampleText.getSelectedItem());}
        		}
        	}
        }
    }

    /* -------------------- Processing callers -------------------- */
    private void saveOutput()
    {
        if (!allowSave) return;
        try {
            if (showingTextOutput) {
                JFileChooser fc = new JFileChooser();
                int returnVal = fc.showSaveDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File saveFile = fc.getSelectedFile();
                    PrintWriter w = new PrintWriter(new FileWriter(saveFile));
                    w.print(outputText.getText());
                    w.close();
                }
            } else { // audio data
                JFileChooser fc = new JFileChooser();
                AudioFileFormat.Type[] knownAudioTypes = AudioSystem.getAudioFileTypes();
                for (int i=0; i<knownAudioTypes.length; i++) {
                    fc.addChoosableFileFilter(new SimpleFileFilter
                        (knownAudioTypes[i].getExtension(),
                         knownAudioTypes[i].toString() + "(." + knownAudioTypes[i].getExtension() + ")"));
                }
                int returnVal = fc.showSaveDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File saveFile = fc.getSelectedFile();
                    String ext = MaryUtils.getExtension(saveFile);
                    AudioFileFormat.Type audioType = null;
                    for (int i=0; i<knownAudioTypes.length; i++) {
                        if (knownAudioTypes[i].getExtension().equals(ext)) {
                            audioType = knownAudioTypes[i];
                            break;
                        }
                    }
                    if (audioType == null) { // file has unknown extension
                        showErrorMessage("Unknown audio type",
                                "Cannot write file of type `." + ext + "'");
                    } else { // OK, we know what to do
                        processor.process(inputText.getText(),
                            ((MaryClient.DataType)cbInputType.getSelectedItem()).name(),
                            "AUDIO",
                            audioType.toString(),
                            ((MaryClient.Voice)cbDefaultVoice.getSelectedItem()).name(),
                            new FileOutputStream(saveFile));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("IOException",e.getMessage());
        }
    }

    private void makeTextPlain(StyledDocument doc) {
        SimpleAttributeSet emptyAttributes = new SimpleAttributeSet();
        doc.setCharacterAttributes(0, doc.getLength(), emptyAttributes, true);
    }

    /**
     * Set everything that is not between < and > to bold.
     * This is "dumb", i.e. it will not try to analyse the contents of
     * tags, and fail at situations like <tag attr="3>i<4">, where i would
     * be printed in bold as well.
     */
    private void highlightText(StyledDocument doc) {
        SimpleAttributeSet highlighted = new SimpleAttributeSet();
        StyleConstants.setBold(highlighted, true);
        boolean insideTag = false;
        int beginText = -1; // will contain beginning of text to be highlighted
        for (int i=0; i<doc.getLength(); i++) {
            char c = ' '; // Initialisation to keep compiler happy
            try { c = doc.getText(i,1).charAt(0); }
            catch (BadLocationException e) {}
            if (insideTag) {
                if (c == '>')
                    insideTag = false;
            } else { // not inside a tag
                if (c == '<') {
                    // Start of new tag
                    if (beginText != -1) { // anything to highlight?
                        // highlight it
                        doc.setCharacterAttributes(beginText, i-beginText,
                                                   highlighted, false);
                        beginText = -1;
                    }
                    insideTag = true;
                } else { // normal text character
                    if (beginText == -1) {
                        // This is the first text character
                        beginText = i;
                    }
                }
            }
        } // for all characters in document
        // Any text at the very end of the document?
        if (beginText != -1) {
            doc.setCharacterAttributes(beginText, doc.getLength()-beginText,
                                       highlighted, false);
        }
    }

    // Call the mary client
    private void processInput()
    {
        OutputStream os;
        MaryClient.DataType outputType = (MaryClient.DataType)cbOutputType.getSelectedItem();
        if (outputType.name().equals("AUDIO")) {
            try {
                audioPlayer = new JavaStreamingAudioPlayer();
                processor.streamAudio(inputText.getText(), 
                        ((MaryClient.DataType)cbInputType.getSelectedItem()).name(),
                        streamMp3 ? "MP3":"WAVE",
                        ((MaryClient.Voice)cbDefaultVoice.getSelectedItem()).name(),
                        audioPlayer,
                        this);
                bPlay.setText("Stop");
            } catch (NullPointerException e){
                System.out.println("ups");}
              catch (Exception e) {
                e.printStackTrace();
                showErrorMessage(e.getClass().getName(), e.getMessage());
                playerFinished();
            }
            
        } else {
            try {
                // Write to a byte array (to be converted to a string later)
                os = new ByteArrayOutputStream();
                processor.process(inputText.getText(),
                ((MaryClient.DataType)cbInputType.getSelectedItem()).name(),
                outputType.name(),
                null,
                ((MaryClient.Voice)cbDefaultVoice.getSelectedItem()).name(),
                os);
                try {
                    setOutputText(((ByteArrayOutputStream)os).toString("UTF-8"));
                } catch (UnsupportedEncodingException uee) {
                    uee.printStackTrace();
                }
                bEdit.setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
                showErrorMessage(e.getClass().getName(), e.getMessage());
            }
        }
    }

    private void editOutput() {
        MaryClient.DataType type = (MaryClient.DataType) cbOutputType.getSelectedItem();
        if (type == null || !type.isTextType() || !type.isInputType())
            return;
        // Ask for confirmation before doing anything
        int answer = JOptionPane.showConfirmDialog(this,
                         "Replace current input text?",
                         "Replace input?",
                         JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            setInputText(outputText.getText());
            setOutputText("");
            // We need to make sure the item handler doesn't try to replace
            // the input with a default example:
            if (cbInputType.getSelectedItem().equals(cbOutputType.getSelectedItem())) {
                // No problem, type won't change anyway
            } else {
                // Signal to the item handler that we don't want replacement
                doReplaceInput = false;
                cbInputType.setSelectedItem(cbOutputType.getSelectedItem());
            }
        }
    }

    private void compareTexts() {
        // Only try to compare if both are MaryXML and non-empty:
        if (!((MaryClient.DataType)cbOutputType.getSelectedItem()).isTextType() ||
            inputText.getText().length() == 0 ||
            outputText.getText().length() == 0) {
            return;
        }
        try {
        // First, make both documents plain text:
        makeTextPlain(inputText.getStyledDocument());
        makeTextPlain(outputText.getStyledDocument());

        // Now, highlight text in both documents:
        highlightText(inputText.getStyledDocument());
        highlightText(outputText.getStyledDocument());

        // Define text attributes for added/removed chunks:
        SimpleAttributeSet removed = new SimpleAttributeSet();
        SimpleAttributeSet added = new SimpleAttributeSet();
        StyleConstants.setBold(removed, true);
        StyleConstants.setBold(added, true);
        StyleConstants.setItalic(removed, true);
        StyleConstants.setItalic(added, true);
        StyleConstants.setUnderline(added, true);
        StyleConstants.setForeground(removed, Color.red);
        StyleConstants.setForeground(added, Color.green.darker());
        // Calculate the differences between input and output:
        String input = inputText.getStyledDocument().getText(0, inputText.getStyledDocument().getLength());
        String[] inputWords = MaryUtils.splitIntoSensibleXMLUnits(input);
        int[] inputIndex = new int[inputWords.length+1];
        int total = 0;
        for (int i=0; i<inputWords.length; i++) {
            inputIndex[i] = total;
            total += inputWords[i].length();
            //System.err.println("Input Word nr. " + i + ": [" + inputWords[i] + "], indexes " + inputIndex[i] + "-" + (inputIndex[i]+inputWords[i].length()) + "[" + input.substring(inputIndex[i], inputIndex[i]+inputWords[i].length()) + "] / [" + inputText.getStyledDocument().getText(inputIndex[i], inputWords[i].length()) + "]");
        }
        inputIndex[inputWords.length] = total;
        String output = outputText.getStyledDocument().getText(0, outputText.getStyledDocument().getLength());
        String[] outputWords = MaryUtils.splitIntoSensibleXMLUnits(output);
        int[] outputIndex = new int[outputWords.length+1];
        total = 0;
        for (int i=0; i<outputWords.length; i++) {
            outputIndex[i] = total;
            total += outputWords[i].length();
            //System.err.println("Output Word nr. " + i + ": [" + outputWords[i] + "], indexes " + outputIndex[i] + "-" + (outputIndex[i]+outputWords[i].length()) + "[" + output.substring(outputIndex[i], outputIndex[i]+outputWords[i].length()) + "]");
        }
        outputIndex[outputWords.length] = total;
        List diffs = new Diff(inputWords, outputWords).diff();
        Iterator it = diffs.iterator();
        while (it.hasNext()) {
            Difference diff = (Difference)it.next();
            int delStart = diff.getDeletedStart();
            int delEnd = diff.getDeletedEnd();
            int addStart = diff.getAddedStart();
            int addEnd = diff.getAddedEnd();
            if (delEnd != Difference.NONE) {
                inputText.getStyledDocument().setCharacterAttributes(inputIndex[delStart], inputIndex[delEnd+1]-inputIndex[delStart], removed, false);
                //System.err.println("deleted "+delStart+"-"+(delEnd+1)+": [" + input.substring(inputIndex[delStart], inputIndex[delEnd+1]) + "] / [" + inputText.getStyledDocument().getText(inputIndex[delStart], inputIndex[delEnd+1]-inputIndex[delStart]) + "]");
            }
            if (addEnd != Difference.NONE) {
                outputText.getStyledDocument().setCharacterAttributes(outputIndex[addStart], outputIndex[addEnd+1]-outputIndex[addStart], added, false);                
                //System.err.println("added "+addStart+"-"+(addEnd+1)+": [" + output.substring(outputIndex[addStart], outputIndex[addEnd+1]) + "] / [" + outputText.getStyledDocument().getText(outputIndex[addStart], outputIndex[addEnd+1]-outputIndex[addStart]) + "]");
            }
        }
        } catch(Exception ex) { ex.printStackTrace(); }
    }

    protected void setInputText(String text)
    {
        inputText.setText(text);
        makeTextPlain(inputText.getStyledDocument());
        inputText.setCaretPosition(0);
    }
    
    protected void setOutputText(String text)
    {
        outputText.setText(text);
        makeTextPlain(outputText.getStyledDocument());
        outputText.setCaretPosition(0);
    }
    
    public void playerFinished()
    {
        bPlay.setText("Play");
        if (audioPlayer != null) {
            audioPlayer.cancel();
            audioPlayer = null;
        }
    }

    protected void showErrorMessage(String title, String message)
    {
        JOptionPane.showMessageDialog(this,
                message +
                "\n\nIf you think this is a bug in the MARY system,\n" +
                "please help improve the system by filing a bug report\n" +
                "on the MARY development page: \n" +
                "https://mary.opendfki.de/newticket\n",
                title,
                JOptionPane.ERROR_MESSAGE);

    }
    
    
    public static void main(String[] args) throws Exception {
        JFrame mainFrame = new JFrame("Mary Expert Interface");
        mainFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {System.exit(0);}
            });
        MaryExpertInterface m = new MaryExpertInterface();
        mainFrame.setContentPane(m);
        mainFrame.pack();
        mainFrame.setVisible(true);

    }

}
