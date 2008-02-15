package de.dfki.lt.mary.unitselection.adaptation;

import de.dfki.lt.mary.util.StringUtil;

// This class keeps information on each specific training item
// For example, a training item for a sentence based voice conversion training database
// could be a wav file, the corresponding text transcription, label file, pitch contour file, etc.
// The training set is a collection of BaseTrainingItem objects
public class BaselineAdaptationItem {
    //A decomposition of the file into its sinus+noise+transients+residual components
    // audioFile = sinesFile+noiseFile+transientsFile+residualFile
    public String sinesFile;        //Sinusoids
    public String noiseFile;        //Noise
    public String transientsFile;   //Transients
    public String residualFile;     //Residual (what remains after all model based decomposition)
    //
    
    public String labelFile;        //Labels
    public String f0File;           //f0 contour
    public String pitchMarkFile;    //Pitch marks
    public String energyFile;       //Energy contour
    public String textFile;         //Text
    public String mfccFile;         //Mel frequency cepstral coefficients
    public String lsfFile;          //Line spectral frequencies
    public String lpcFile;          //Linear prediction coefficients
    public String lpResidualFile;   //Time-domain residual waveform after LP inverse filtering
    public String cepsFile;         //Cepstrum coefficients file
    public String eggFile;          //Electro-glottograph file
    
    public String audioFile;        //Original waveform file
    
    public BaselineAdaptationItem()
    {
        
    }
    
    public void setFromWavFilename(String wavFilename)
    {
        audioFile = wavFilename;
        
        sinesFile = StringUtil.modifyExtension(audioFile, ".sin"); //Sinusoids
        noiseFile = StringUtil.modifyExtension(audioFile, ".noi"); //Noise
        transientsFile = StringUtil.modifyExtension(audioFile, ".tra"); //Transients
        residualFile = StringUtil.modifyExtension(audioFile, ".res"); //Residual (what remains after all model based decomposition)
        
        labelFile = StringUtil.modifyExtension(audioFile, ".lab");   //Labels
        f0File = StringUtil.modifyExtension(audioFile, ".ptc");      //f0 contour
        pitchMarkFile = StringUtil.modifyExtension(audioFile, ".pm");   //Pitch marks
        energyFile = StringUtil.modifyExtension(audioFile, ".ene"); //Energy contour
        textFile = StringUtil.modifyExtension(audioFile, ".txt");    //Text
        mfccFile = StringUtil.modifyExtension(audioFile, ".mfc");    //Mel frequency cepstral coefficients
        lsfFile = StringUtil.modifyExtension(audioFile, ".lsf");     //Line spectral frequencies
        lpcFile = StringUtil.modifyExtension(audioFile, ".lpc");     //Linear prediction coefficients
        lpResidualFile = StringUtil.modifyExtension(audioFile, ".lpr");   //Time-domain residual waveform after LP inverse filtering
        cepsFile = StringUtil.modifyExtension(audioFile, ".cep");    //Cepstrum coefficients file
        eggFile = StringUtil.modifyExtension(audioFile, ".egg");     //Electro-glottograph file
    }
}
