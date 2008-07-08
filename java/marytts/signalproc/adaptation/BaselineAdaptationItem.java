package marytts.signalproc.adaptation;

import marytts.util.StringUtils;

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
    
    //Mary TTS outputs to specify target features for tests, transplantation, etc
    public String targetFestivalUttFile;  //FESTIVAL_UTT output which contains target timing and f0s (also the labels)
                                          // This needs to be mapped with actual labels (i.e. labelFile) and f0s (pitchFile) to
                                          // obtain required prosody modification factors
    
    public String targetLabelFile;        //Target labels for mapping
    public String targetEnergyFile;       //Target energy file, to be used in transplantations
    public String targetWavFile;          //Target waveform file
    //
    
    public String audioFile;        //Original waveform file
    
    public BaselineAdaptationItem()
    {
        
    }
    
    public BaselineAdaptationItem(BaselineAdaptationItem existing)
    {
        sinesFile = existing.sinesFile;
        noiseFile = existing.noiseFile;
        transientsFile = existing.transientsFile;
        residualFile = existing.residualFile;
        
        labelFile = existing.labelFile;
        f0File = existing.f0File;
        pitchMarkFile = existing.pitchMarkFile;
        energyFile = existing.energyFile;
        textFile = existing.textFile;
        mfccFile = existing.mfccFile;
        lsfFile = existing.lsfFile;
        lpcFile = existing.lpcFile;
        lpResidualFile = existing.lpResidualFile;
        cepsFile = existing.cepsFile;
        eggFile = existing.eggFile;
        
        targetFestivalUttFile = existing.targetFestivalUttFile;
        
        targetLabelFile = existing.targetLabelFile;
        targetEnergyFile = existing.targetEnergyFile;
        targetWavFile = existing.targetWavFile;
        
        audioFile = existing.audioFile;
    }
    
    public void setFromWavFilename(String referenceFilename)
    {
        audioFile = referenceFilename;
        
        sinesFile = StringUtils.modifyExtension(audioFile, ".sin"); //Sinusoids
        noiseFile = StringUtils.modifyExtension(audioFile, ".noi"); //Noise
        transientsFile = StringUtils.modifyExtension(audioFile, ".tra"); //Transients
        residualFile = StringUtils.modifyExtension(audioFile, ".res"); //Residual (what remains after all model based decomposition)
        
        labelFile = StringUtils.modifyExtension(audioFile, ".lab");   //Labels
        f0File = StringUtils.modifyExtension(audioFile, ".ptc");      //f0 contour
        pitchMarkFile = StringUtils.modifyExtension(audioFile, ".pm");   //Pitch marks
        energyFile = StringUtils.modifyExtension(audioFile, ".ene"); //Energy contour
        textFile = StringUtils.modifyExtension(audioFile, ".txt");    //Text
        mfccFile = StringUtils.modifyExtension(audioFile, ".mfc");    //Mel frequency cepstral coefficients
        lsfFile = StringUtils.modifyExtension(audioFile, ".lsf");     //Line spectral frequencies
        lpcFile = StringUtils.modifyExtension(audioFile, ".lpc");     //Linear prediction coefficients
        lpResidualFile = StringUtils.modifyExtension(audioFile, ".lpr");   //Time-domain residual waveform after LP inverse filtering
        cepsFile = StringUtils.modifyExtension(audioFile, ".cep");    //Cepstrum coefficients file
        eggFile = StringUtils.modifyExtension(audioFile, ".egg");     //Electro-glottograph file
        
        targetFestivalUttFile = StringUtils.modifyExtension(audioFile, ".tutt"); //FESTIVAL_UTT file
        targetLabelFile = StringUtils.modifyExtension(audioFile, ".tlab");  //Target labels for mapping
        targetEnergyFile = StringUtils.getFileName(audioFile) + ".tene"; //Target energy file, to be used in transplantations
        targetWavFile = StringUtils.getFileName(audioFile) + ".twav"; //Target waveform file
    }
}
