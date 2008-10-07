package marytts.tools.voiceimport;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.client.MaryClient;
import marytts.datatypes.MaryDataType;
import marytts.signalproc.analysis.EnergyAnalyser;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.io.FileUtils;
import marytts.util.signal.SignalProcUtils;
import marytts.util.string.PrintfFormat;



/**
 * Identify and Remove End-ponints (intitial and final silences) from
 * given set of wave files.
 * @author Sathish and Oytun
 *  
 */
public class EndpointDetector extends VoiceImportComponent
{
    
    protected File textDir;
    protected File inputWavDir;
    protected File outputWavDir;
    protected String waveExt = ".wav";
    private BasenameList bnlist;
    
    protected DatabaseLayout db = null;
    protected int percent = 0;
    
    public String INPUTWAVDIR = "EndpointDetector.inputWaveDirectory";
    public String OUTPUTWAVDIR = "EndpointDetector.outputWaveDirectory";
     
   public String getName(){
        return "EndpointDetector";
    }
     
   public void initialiseComp()
    {     

    }
     
     public SortedMap getDefaultProps(DatabaseLayout db){
         this.db = db;
         if (props == null){
             props = new TreeMap();
             props.put(INPUTWAVDIR, db.getProp(db.ROOTDIR)
                     +"inputwav"
                     +System.getProperty("file.separator"));
             props.put(OUTPUTWAVDIR, db.getProp(db.ROOTDIR)
                     +"outputwav"
                     +System.getProperty("file.separator"));
         } 
         return props;
     }
     
    protected void setupHelp(){
         props2Help = new TreeMap();
         props2Help.put(INPUTWAVDIR, "input wave files directory."); 
                 
         props2Help.put(OUTPUTWAVDIR, "output directory to store initial-end silences removed wave files." 
                 +"Will be created if it does not exist");
         
     }
     
    
    public boolean compute() throws IOException
    {
        //      Check existance of input directory  
        inputWavDir = new File(getProp(INPUTWAVDIR));
        if (!inputWavDir.exists()){
            throw new Error("Could not find input Directory: "+ getProp(INPUTWAVDIR));
        }   
        
        // Check existance of output directory
        // if not exists, create a new directory
        outputWavDir = new File(getProp(OUTPUTWAVDIR));
        if (!outputWavDir.exists()){
            System.out.print(OUTPUTWAVDIR+" "+getProp(OUTPUTWAVDIR)
                    +" does not exist; ");
            if (!outputWavDir.mkdir()){
                throw new Error("Could not create OUTPUTWAVDIR");
            }
            System.out.print("Created successfully.\n");
        }
        
        // Automatically collect all ".wav" files from given directory
        bnlist = new BasenameList(inputWavDir+File.separator,waveExt);
        
        /* TO DO
         * Get these two values from the interface
         */
        double minimumStartSilenceInSeconds = 1.0;
        double minimumEndSilenceInSeconds = 1.0;
        //
        
        System.out.println( "Removing endpoints for "+ bnlist.getLength() + " wave files" );
        for (int i=0; i<bnlist.getLength(); i++) {
            percent = 100*i/bnlist.getLength();
            String inputFile = inputWavDir + File.separator + bnlist.getName(i) + waveExt;
            String outputFile = outputWavDir + File.separator + bnlist.getName(i) + waveExt;
            removeEndponits(inputFile,outputFile,minimumStartSilenceInSeconds,minimumEndSilenceInSeconds);
            System.out.println( "    " + bnlist.getName(i) );
        }
        System.out.println("...Done.");
        return true;
    }

    
    /**
     * Removes endpoints from given file
     * @param inputFile 
     * @param outputFile
     * @throws IOException
     * @throws  
     */
    public void removeEndponits(String inputFile, String outputFile,
                                double minimumStartSilenceInSeconds,
                                double minimumEndSilenceInSeconds) throws IOException
    {
        /*
         * Add corresponding module to remove endpoints
         * 1. identify and remove end points
         * 2. make sure at least some desired amount of silence in the beginning and at the end
         * 3. store as output wavefile 
         */
        
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(new File(inputFile));
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if (ais!=null)
        {
            if (!ais.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                ais = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, ais);
            }
            if (ais.getFormat().getChannels() > 1) {
                throw new IllegalArgumentException("Can only deal with mono audio signals");
            }
            int samplingRate = (int) ais.getFormat().getSampleRate();
            DoubleDataSource signal = new AudioDoubleDataSource(ais);
            int framelength = (int)(0.01 /*seconds*/ * samplingRate);
            EnergyAnalyser ea = new EnergyAnalyser(signal, framelength, framelength, samplingRate);
            //double[][] speechStretches = ea.getSpeechStretches();
            double[][] speechStretches = ea.getSpeechStretchesUsingEnergyHistory();
            int numStretches = speechStretches.length;
            int speechStartIndex = (int)(samplingRate*speechStretches[0][0]);
            int speechEndIndex = (int)(samplingRate*speechStretches[numStretches-1][1]);
            speechStartIndex = Math.min(0, speechStartIndex);

            int silStartLen = Math.max(0, (int)(samplingRate*minimumStartSilenceInSeconds));
            double[] silStart = SignalProcUtils.getWhiteNoise(silStartLen, 1e-20);
            int silEndLen = Math.max(0, (int)(samplingRate*minimumEndSilenceInSeconds));
            double[] silEnd = SignalProcUtils.getWhiteNoise(silEndLen, 1e-20);

            double[] y = null;
            if (speechEndIndex-speechStartIndex+silStartLen+silEndLen>0)
                y = new double[speechEndIndex-speechStartIndex+silStartLen+silEndLen];
            else
                throw new Error("No output samples to write for " + inputFile);

            double[] x = signal.getAllData();

            int start = 0;
            if (silStartLen>0)
            {
                System.arraycopy(silStart, 0, y, start, silStartLen);
                start += silStartLen;
            }

            if (speechEndIndex-speechStartIndex>0)
            {
                System.arraycopy(x, speechStartIndex, y, start, speechEndIndex-speechStartIndex);
                start += (speechEndIndex-speechStartIndex);
            }

            if (silEndLen>0)
            {
                System.arraycopy(silEnd, 0, y, start, silEndLen);
                start += silEndLen;
            }

            DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(y), ais.getFormat());
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));
        }
        else
            throw new Error("Cannot open input file " + inputFile);
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
