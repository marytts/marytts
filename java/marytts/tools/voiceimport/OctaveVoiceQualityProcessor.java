package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.VoiceQuality;
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;
import marytts.tools.voiceimport.SphinxTrainer.StreamGobbler;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;


public class OctaveVoiceQualityProcessor extends VoiceImportComponent {
    
    protected DatabaseLayout db;
    private String name = "OctaveVoiceQualityProcessor";
    
    protected String octaveExtension = ".octave";
    protected String voiceQualityExtension = ".vq";
    protected String scriptFileName;
    

    int numVqParams = 5;           // number of voice quality parameters extracted from the sound files:
                                   // OQG, GOG, SKG, RCG, IC
      
    private int percent = 0;
    //private final String FRAMELENGTH  = "0.01";   // Default for snack
    //private final String WINDOWLENGTH = "0.025";  // Default for f0 snack ( formants uses a bigger window)
  
    public final String SAMPLINGRATE = "OctaveVoiceQualityProcessor.samplingRate";
    public final String MINPITCH     = "OctaveVoiceQualityProcessor.minPitch";
    public final String MAXPITCH     = "OctaveVoiceQualityProcessor.maxPitch";
    public final String FRAMELENGTH  = "OctaveVoiceQualityProcessor.frameLength";
    public final String WINDOWLENGTH = "OctaveVoiceQualityProcessor.windowLength";
    public final String NUMFORMANTS  = "OctaveVoiceQualityProcessor.numFormants";
    public final String LPCORDER     = "OctaveVoiceQualityProcessor.lpcOrder";
    public final String FFTSIZE      = "OctaveVoiceQualityProcessor.fftSize";
    public final String VQDIR        = "OctaveVoiceQualityProcessor.vqDir";
    public final String OCTAVEPATH   = "OctaveVoiceQualityProcessor.octavePath";
    
    protected void setupHelp()
    {
        if (props2Help ==null){
            props2Help = new TreeMap();
            props2Help.put(SAMPLINGRATE,"Sampling frequency in Hertz. Default: 16000");
            props2Help.put(MINPITCH,"minimum value for the pitch (in Hz). Default: female 60, male 40");
            props2Help.put(MAXPITCH,"maximum value for the pitch (in Hz). Default: female 500, male 400");
            props2Help.put(FRAMELENGTH,"frame length (in seconds) for VQ calculation Default: 0.005 sec.");
            props2Help.put(WINDOWLENGTH,"window length (in seconds) for VQ calculation Default: 0.025 sec.");
            props2Help.put(NUMFORMANTS,"Default 4, maximum 7");
            props2Help.put(LPCORDER,"Default 12, if NUMFORMANTS=4 min LPCORDER=12\n" +
                                                "if NUMFORMANTS=5 min LPCORDER=14\n" +
                                                "if NUMFORMANTS=6 min LPCORDER=16\n" +
                                                "if NUMFORMANTS=7 min LPCORDER=18\n" );
            props2Help.put(FFTSIZE,"Default 512");
            props2Help.put(VQDIR, "directory containing the voice quality files. Will be created if it does not exist");
            props2Help.put(OCTAVEPATH, "octave executable path");
        }
    }

    public final String getName(){
        return name;
    }

    public void initialiseComp()
    {
        scriptFileName = db.getProp(db.TEMPDIR) + "octave_call.m";
    }

    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
        if (props == null){
            props = new TreeMap();  
            props.put(SAMPLINGRATE,"16000");
            if (db.getProp(db.GENDER).equals("female")){
                props.put(MINPITCH,"60");
                props.put(MAXPITCH,"400");
            } else {
                props.put(MINPITCH,"60");
                props.put(MAXPITCH,"400");
            }
            props.put(FRAMELENGTH,"0.005");
            props.put(WINDOWLENGTH,"0.025");
            props.put(NUMFORMANTS,"4");
            props.put(LPCORDER,"12");
            props.put(FFTSIZE, "512");
            props.put(VQDIR, db.getProp(db.ROOTDIR) + "vq" + System.getProperty("file.separator"));
            props.put(OCTAVEPATH, "/usr/bin/octave");
        }
        return props;
    }

    /**
     * The standard compute() method of the VoiceImportComponent interface.
     */
    public boolean compute() throws Exception {
              
        File script = new File(scriptFileName);
        /* In order to get the same number of frames when calculating f0 and formants with snack, we should keep constant the following variables:
         * -maxpitch 400 for F0 calculation
         * -minpitch 60 for F0 calculation
         * -windowlength 0.03 for formants calculation
         * -framelength should be the same for f0, formants and this SnackVoiceQualityProcessor, this value can be change, ex: 0.005, 0.01 etc.
         */        
        if (script.exists()) script.delete();
        PrintWriter toScript = new PrintWriter(new FileWriter(script));
        toScript.println("arg_list = argv ();");
        toScript.println("cd /project/mary/marcela/quality_parameters/snack/");
        toScript.println("calculateVoiceQuality(arg_list{1}, arg_list{2}, arg_list{3}, str2num(arg_list{4}));");
        toScript.close();

        
        String[] baseNameArray = bnl.getListAsArray();
        // to test String[] baseNameArray = {"curious", "u"};
        System.out.println( "Computing voice quality for " + baseNameArray.length + " utterances." );

        /* Ensure the existence of the target pitchmark directory */
        File dir = new File(getProp(VQDIR));
        if (!dir.exists()) {
            System.out.println( "Creating the directory [" + getProp(VQDIR) + "]." );
            dir.mkdir();
        }        
        
        
        // Some general parameters that apply to all the sound files
        int samplingRate = Integer.parseInt(getProp(SAMPLINGRATE));         
        // frameLength and windowLength in samples
        int frameLength = Math.round(Float.parseFloat(getProp(FRAMELENGTH)) * samplingRate);
        int windowLength = Math.round(Float.parseFloat(getProp(WINDOWLENGTH)) * samplingRate);
        
      
        
        /* execute octave and voice quality parameters extraction */        
        for ( int i = 0; i < baseNameArray.length; i++ ) {
            percent = 100*i/baseNameArray.length;
            String wavFile   = db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
            String octaveFile = getProp(VQDIR) + baseNameArray[i] + octaveExtension;
            String vqFile    = getProp(VQDIR) + baseNameArray[i] + voiceQualityExtension;
            
            System.out.println("Writing  OQG GOG SKG RCG IC to " + octaveFile);

            boolean isWindows = true;
            String strTmp = getProp(OCTAVEPATH) + " --silent " + scriptFileName + " " + wavFile + " " + getProp(db.GENDER) + " " + octaveFile + " 0";
            
            
            System.out.println("Executing: " + strTmp);
                
            Process octave = Runtime.getRuntime().exec(strTmp);
            StreamGobbler errorGobbler = new 
            StreamGobbler(octave.getErrorStream(), "err");            
            //read from output stream
            StreamGobbler outputGobbler = new 
            StreamGobbler(octave.getInputStream(), "out");    
            //start reading from the streams
            errorGobbler.start();
            outputGobbler.start();
            //close everything down
            octave.waitFor();
            octave.exitValue();
           
            // Read the sound file
            WavReader soundFile = new WavReader(wavFile);           
            // Check sampling rate of sound file
            assert samplingRate==soundFile.getSampleRate();
            
            // get a wrapper voice quality class for this file           
            VoiceQuality vq = new VoiceQuality(numVqParams,samplingRate, frameLength/(float)samplingRate,windowLength/(float)samplingRate);
            
            readOctaveData(vq, octaveFile);
           
            System.out.println("Writing vq parameters to " + vqFile);
            vq.writeVqFile(vqFile);
            
        }
        return true;
    }
    
  
    private void readOctaveData(VoiceQuality vq, String octaveFile) throws IOException
    {
        double[][] octaveData = null;
        int numLines, numData;
        BufferedReader reader = new BufferedReader(new FileReader(octaveFile));
        int i, j;
        try {            
            String line;
            String strVal;
            StringTokenizer s;
            double value;
            
            // find out the number of lines in the file
            List<String> lines = new ArrayList<String>();
            while ((line = reader.readLine())!=null){
                lines.add(line);
            }
            numLines = lines.size();
            numData = vq.params.dimension;
            octaveData = new double[numData][numLines];
            for (i=0; i<numLines; i++){
         
                strVal = (String)lines.get(i);
                s = new StringTokenizer(strVal);
                
                for (j=0; j<numData; j++)
                {
                  if(s.hasMoreTokens())
                    octaveData[j][i] = Double.parseDouble(s.nextToken());
                }            
            }
            vq.allocate(numLines, octaveData);
            
        } catch (IOException ioe) {
            ioe.printStackTrace();            
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
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
    
    
    // to test/compare vq values of several files
    public static void main1( String[] args ) throws Exception    
    {
      
        String path = "/project/mary/marcela/HMM-voices/arctic_test/vq-octave/";                 
        String whisperFile = path + "whisper.vq";
        String modalFile = path + "modal.vq";
        String creakFile = path + "creak.vq";
        String harshFile = path + "harsh.vq";
                
        VoiceQuality vq1 = new VoiceQuality(); 
        System.out.println("Reading: " + whisperFile);
        vq1.readVqFile(whisperFile); 
        //vq1.printPar();
        vq1.printMeanStd();

        VoiceQuality vq2 = new VoiceQuality();        
        System.out.println("Reading: " + modalFile);
        vq2.readVqFile(modalFile);  
        //vq2.printPar();
        vq2.printMeanStd();
        
        VoiceQuality vq3 = new VoiceQuality();
        System.out.println("Reading: " + creakFile);
        vq3.readVqFile(creakFile);  
        //vq3.printPar();
        vq3.printMeanStd();
        
        VoiceQuality vq4 = new VoiceQuality();
        System.out.println("Reading: " + harshFile);
        vq4.readVqFile(harshFile);  
        //vq4.printPar();
        vq4.printMeanStd();
        
    }    

   
    public static void main( String[] args ) throws Exception { 
      /*OctaveVoiceQualityProcessor vq = new OctaveVoiceQualityProcessor(); 
      DatabaseLayout db = new DatabaseLayout(vq);
      vq.compute();
      */
      // values extracted with Java program
      main1(args);
      
      /* 
      
      String file   = "/project/mary/marcela/HMM-voices/arctic_test/vq-octave/curious.vq";
      VoiceQuality vq1 = new VoiceQuality(); 
      System.out.println("Reading: " + file);
      vq1.readVqFile(file); 
      vq1.printPar();
      vq1.printMeanStd();
      MaryUtils.plot(vq1.getGOG(), "Normal");
      vq1.applyZscoreNormalization();
      MaryUtils.plot(vq1.getGOG(), "after z-score");
      
      */    
      
    }
    
    
}

