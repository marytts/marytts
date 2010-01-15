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

import marytts.signalproc.analysis.PitchMarks;
import marytts.signalproc.window.Window;
import marytts.tools.voiceimport.SphinxTrainer.StreamGobbler;
import marytts.util.MaryUtils;
import marytts.util.data.text.SnackTextfileDoubleDataSource;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;



public class SnackVoiceQualityProcessor extends VoiceImportComponent {
    protected DatabaseLayout db = null;
    protected String snackExtension = ".snack";
    protected String voiceQualityExtension = ".vq";
    protected String scriptFileName;

    private int percent = 0;
    //private final String FRAMELENGTH  = "0.01";   // Default for snack
    //private final String WINDOWLENGTH = "0.025";  // Default for f0 snack ( formants uses a bigger window)
  
    public final String MINPITCH     = "SnackVoiceQualityParametersExtractor.minPitch";
    public final String MAXPITCH     = "SnackVoiceQualityParametersExtractor.maxPitch";
    public final String FRAMELENGTH  = "SnackVoiceQualityParametersExtractor.frameLength";
    public final String WINDOWLENGTH = "SnackVoiceQualityParametersExtractor.windowLength";
    public final String NUMFORMANTS  = "SnackVoiceQualityParametersExtractor.numFormants";
    public final String LPCORDER     = "SnackVoiceQualityParametersExtractor.lpcOrder";
    public final String VQDIR        = "SnackVoiceQualityParametersExtractor.vqDir";  
    
    protected void setupHelp()
    {
        if (props2Help ==null){
            props2Help = new TreeMap();
            props2Help.put(MINPITCH,"minimum value for the pitch (in Hz). Default: female 60, male 40");
            props2Help.put(MAXPITCH,"maximum value for the pitch (in Hz). Default: female 500, male 400");
            props2Help.put(FRAMELENGTH,"frame length (in seconds) for VQ calculation Default: 0.005 sec.");
            props2Help.put(WINDOWLENGTH,"window length (in seconds) for VQ calculation Default: 0.025 sec.");
            props2Help.put(NUMFORMANTS,"Default 4, maximum 7");
            props2Help.put(LPCORDER,"Default 12, if NUMFORMANTS=4 min LPCORDER=12\n" +
                                                "if NUMFORMANTS=5 min LPCORDER=14\n" +
                                                "if NUMFORMANTS=6 min LPCORDER=16\n" +
                                                "if NUMFORMANTS=7 min LPCORDER=18\n" );
            props2Help.put(VQDIR, "directory containing the voice quality files. Will be created if it does not exist");
            
        }
    }

    public final String getName(){
        return "SnackVoiceQualityParametersExtractor";
    }

    public void initialiseComp()
    {
        scriptFileName = db.getProp(db.TEMPDIR) + "f0_formants.tcl";
    }

    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
        if (props == null){
            props = new TreeMap();       
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
            props.put(VQDIR, db.getProp(db.ROOTDIR) + "vq" + System.getProperty("file.separator"));
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
        toScript.println("# extracting pitch anf formants using snack");
        toScript.println("package require snack");
        toScript.println("snack::sound s");
        toScript.println("s read [lindex $argv 0]");
        toScript.println("set fd [open [lindex $argv 1] w]");
        toScript.println("set f0 [s pitch -method esps -maxpitch [lindex $argv 2] -minpitch [lindex $argv 3] -framelength [lindex $argv 4] ]");
        toScript.println("set f0_length [llength $f0]");
        toScript.println("puts \"f0 length = $f0_length\"");
        toScript.println("set formants [s formant -numformants [lindex $argv 5] -lpcorder [lindex $argv 6] -framelength [lindex $argv 4] -windowlength 0.03]");
        toScript.println("set formants_length [llength $formants]");
        toScript.println("puts \"formants length = $formants_length\"");
        toScript.println("set n 0");
        toScript.println("foreach line $f0 {");
        toScript.println("puts -nonewline $fd \"[lindex $line 0] \"");
        toScript.println("puts $fd [lindex $formants $n]");
        toScript.println("incr n");
        toScript.println("}");
        toScript.println("close $fd");
        toScript.println("exit"); 
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
        
        /* execute snack and voice quality parameters extraction */        
        for ( int i = 0; i < baseNameArray.length; i++ ) {
            percent = 100*i/baseNameArray.length;
            String wavFile   = db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
            String snackFile = getProp(VQDIR) + baseNameArray[i] + snackExtension;
            String vqFile    = getProp(VQDIR) + baseNameArray[i] + voiceQualityExtension;
            
            System.out.println("Writing f0+formants file to " + snackFile);

            boolean isWindows = true;
            String strTmp = scriptFileName + " " + wavFile + " " + snackFile + " " + getProp(MAXPITCH) + " " + getProp(MINPITCH)
                                           + " " + getProp(FRAMELENGTH) + " " + getProp(NUMFORMANTS) + " " + getProp(LPCORDER);

            if (MaryUtils.isWindows())
                strTmp = "cmd.exe /c " + db.getExternal(db.TCLPATH) + "/tclsh " + strTmp;
            else
                strTmp = db.getExternal(db.TCLPATH) + "/tclsh " + strTmp;
            
            System.out.println("Executing: " + strTmp);
                
            Process snack = Runtime.getRuntime().exec(strTmp);

            StreamGobbler errorGobbler = new 
            StreamGobbler(snack.getErrorStream(), "err");            

            //read from output stream
            StreamGobbler outputGobbler = new 
            StreamGobbler(snack.getInputStream(), "out");    

            //start reading from the streams
            errorGobbler.start();
            outputGobbler.start();

            //close everything down
            snack.waitFor();
            snack.exitValue();

            // Read F0, formants and bandwidths
            double[][] snackData = getData(Integer.parseInt(getProp(NUMFORMANTS)), snackFile);            
            System.out.println("f0_formants size=" + snackData.length);
      
            // Read the sound file
            WavReader wf = new WavReader(wavFile);
            int sampleRate = wf.getSampleRate();
            
            /*
            new ESTTrackWriter(pitchmarkSeconds, null, "pitchmarks").doWriteAndClose(pmFile, false, false);

            // And correct pitchmark locations
            //pitchmarkSeconds = adjustPitchmarks(wf, pitchmarkSeconds);
            //new ESTTrackWriter(pitchmarkSeconds, null, "pitchmarks").doWriteAndClose(correctedPmFile, false, false);
            */
        }
        return true;
    }
    
    /**
     * Loads in snackData the f0 + formants[numFormants] + band widths[numFormants] from the snackFile 
     * @param numFormants
     * @param snackFile   
     * @return
     * @throws IOException
     */
    public double[][] getData(int numFormants, String snackFile) throws IOException
    {
        double[][] snackData = null;
        BufferedReader reader = new BufferedReader(new FileReader(snackFile));
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
            int numLines = lines.size();
            // numFormants*2 + 1 : because the array will contain f0 + 4 formants + 4 bandwidths
            int numData = numFormants*2 + 1;
            snackData = new double[numLines][numData];
            for (i=0; i<numLines; i++){
         
                strVal = (String)lines.get(i);
                s = new StringTokenizer(strVal);
                
                for (j=0; j<numData; j++)
                {
                  if(s.hasMoreTokens())
                    snackData[i][j] = Double.parseDouble(s.nextToken());
                }            
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();            
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        return snackData;  
    }
    
    /**
     * 
     * @param snack: array containing f0+formants+band widths     
     * @param frameLength: in samples
     * @param windowLength: in samples
     * @param sound
     */
    public void calculateVoiceQuality(double snack[][], int frameLength, int windowLength, WavReader sound)throws Exception{
        
      int i, j, k, n, T, T2, index, index1, index2, index3;  
      short x_signal[] = sound.getSamples();
      double x[] = new double[windowLength];
      int fftSize = 512;
      int samplingRateInHz = 16000;
      double X[] = null;  // spectrum of a window in dB      
      double Xpeak[] = null;  // the harmonic peaks
      int windowType = 1; // 1: Hamming window
      int maxFreqIndex = fftSize/2;
      double Fp, Fp2, Fp3, F1, F2, F3, F4, B1, B2, B3, B4, H1, H2, H3, F1p, F2p, F3p, F4p, A1p, A2p, A3p, A4p;
      double hatH1, hatH2, hatA1p, hatA2p, hatA3p;
      double OQ, OQG, GO, GOG, SK, SKG, RC, RCG, IC;
      double f0;
           
      //process per window
      int numFrame = 0;
      for (n=0; n<(sound.getNumSamples() - windowLength); n=n+frameLength ){
        
        f0 = snack[numFrame][0];
        System.out.format("\npitch=%.2f numFrame=%d n=%d \n", f0, numFrame,n);
        
        if( f0 > 0.0 ) {
        
          // get the window frame
          for(i=0; i<windowLength; i++)
            x[i] = x_signal[n+i];    // here the shorts become doubles          
          //MaryUtils.plot(x, "x");
        
          // get the spectrum in dB (20*log10(F))       
          //SignalProcUtils.displayDFTSpectrumInDB(x, fftSize, windowType);
          // should be this in dB??? 
          X = MathUtils.amp2db(SignalProcUtils.getFrameHalfMagnitudeSpectrum(x, fftSize, windowType));
          //MaryUtils.plot(X, "X");
         
          // ---These steps of finding peaks and magnitudes, need to be improved
          // the f0 from snack not always get close to the first peak found, also for 2f0 
          // get the harmonic peak frequencies
          Xpeak = SignalProcUtils.getPeakAmplitudeFrequencies(X, f0, 30, fftSize, samplingRateInHz, false);                                          
          //MaryUtils.plot(Xpeak, "Xpeak");
          //for(j=1; j<Xpeak.length; j++)
          //  System.out.println("peak[" + j + "]=" + Xpeak[j]);
          
          // Amplitude at Fp and 2Fp, it should be at the first two peak amplitude frquencies
          Fp = Xpeak[0];
          index1 =  SignalProcUtils.freq2index(Xpeak[0], samplingRateInHz, maxFreqIndex);
          H1 = X[index1];
          Fp2 = Xpeak[1];
          index2 =  SignalProcUtils.freq2index(Xpeak[1], samplingRateInHz, maxFreqIndex);
          H2 = X[index2];
          Fp3 = Xpeak[2];
          index3 =  SignalProcUtils.freq2index(Xpeak[2], samplingRateInHz, maxFreqIndex);
          H2 = X[index3];
          
          System.out.format("f0=%.2f\n", f0);
          System.out.format(" Fp=%.2f   nFp=%d\n", Fp, index1);
          System.out.format("2Fp=%.2f  nFp2=%d\n", Fp2, index2);
          System.out.format("3Fp=%.2f  nFp3=%d\n", Fp3, index3);
          
          // formants
          F1 = snack[numFrame][1];  
          F2 = snack[numFrame][2];
          F3 = snack[numFrame][3];
          F4 = snack[numFrame][4];
          B1 = snack[numFrame][5];
          B2 = snack[numFrame][6];
          B3 = snack[numFrame][7];
          B4 = snack[numFrame][8];
          
          // F1p, F2p, F3p, find the peaks and their frequencies that are close to the formant frequencies
          index = findClosestHarmonicPeak(Xpeak, F1, maxFreqIndex);
          F1p = Xpeak[index];
          A1p = X[index];
          
          index = findClosestHarmonicPeak(Xpeak, F2, maxFreqIndex);
          F2p = Xpeak[index];
          A2p = X[index];
          
          index = findClosestHarmonicPeak(Xpeak, F3, maxFreqIndex);
          F3p = Xpeak[index];
          A3p = X[index];
          
          index = findClosestHarmonicPeak(Xpeak, F4, maxFreqIndex);
          F4p = Xpeak[index];
          A4p = X[index];
          
          System.out.format("F1=%.2f F1p=%.2f A1p=%.2f B1=%.2f\n", F1, F1p, A1p, B1);
          System.out.format("F2=%.2f F2p=%.2f A2p=%.2f B2=%.2f\n", F2, F2p, A2p, B2);
          System.out.format("F3=%.2f F3p=%.2f A3p=%.2f B3=%.2f\n", F3, F3p, A3p, B3);
          System.out.format("F4=%.2f F4p=%.2f A4p=%.2f B4=%.2f\n", F4, F4p, A4p, B4);
          
          //--- need to improve previous steps to find preciselly the formants amplitudes
          
          // Here I will not use yet bark scale
          // Remove vocal tract influence  
          // H1 is in dB already, and the vocalTractcompensation returns in db as well (20*log10)
          hatH1 = H1 - ( vocalTractCompensation(Fp,F1,B1) + vocalTractCompensation(Fp,F2,B2) + vocalTractCompensation(Fp,F3,B3) + vocalTractCompensation(Fp,F4,B4) );          
          hatH2 = H2 - ( vocalTractCompensation(Fp2,F1,B1) + vocalTractCompensation(Fp2,F2,B2) + vocalTractCompensation(Fp2,F3,B3) + vocalTractCompensation(Fp2,F4,B4) );          
          hatA1p = A1p - ( vocalTractCompensation(F1p,F2,B2) + vocalTractCompensation(F1p,F3,B3) + vocalTractCompensation(F1p,F4,B4) );          
          hatA2p = A2p - ( vocalTractCompensation(F2p,F1,B1) + vocalTractCompensation(F2p,F3,B3) + vocalTractCompensation(F2p,F4,B4) );          
          hatA3p = A3p - ( vocalTractCompensation(F3p,F1,B1) + vocalTractCompensation(F3p,F2,B2) + vocalTractCompensation(F3p,F4,B4) );
          
          // Open Quotient Gradient
          OQ = (hatH1 - hatH2);
          OQG = OQ / Fp;
          
          // Glottal Opening Gradient
          GO = (hatH1 - hatA1p);
          if(F1p == Fp)
            GOG = 0.0;
          else
            GOG = GO / (F1p - Fp);
          
          // SKewness Gradient
          SK = (hatH1 - hatA2p);
          if( F2p == Fp )
            SKG = 0.0;
          else
            SKG = SK / (F2p - Fp);
          
          // Rate of Closure Gradient
          RC = (hatH1 - hatA3p);
          if(F3p == Fp)
            RCG = 0.0;
          else
            RCG = RC / (F3p - Fp);
          
          // Incompleteness of Closure
          IC = B1 / F1;
          
          System.out.format("OQ=%.2f OQG=%.4f\n", OQ,OQG);
          System.out.format("GO=%.2f GOG=%.4f\n", GO,GOG);
          System.out.format("SK=%.2f SKG=%.4f\n", SK,SKG);
          System.out.format("RC=%.2f RCG=%.4f\n", RC,RCG);
          System.out.format("IC=%.2f\n", IC);
          
         
          System.in.read();
          
        }
        numFrame++;  
      }
        
    }
    
    /**
     * returns the index where the closset harmonic peak to f is found
     * @return
     */
    public int findClosestHarmonicPeak(double peaks[], double f, int maxFreqIndex){
      int index = 0;
      double iclosest = 0;
      double distance = maxFreqIndex;
      
      for(int i=0; i<peaks.length; i++){
        if( Math.abs(f-peaks[i]) < distance ){
          iclosest = peaks[i];
          distance = Math.abs(f-peaks[i]);
          index = i;
        }
      }
      return index;
    }
    
    /**
     * Compensation of the vocal tract influence
     * @param freq
     * @param formant
     * @param bandWidth
     * @return
     */   
    public double vocalTractCompensation(double freq, double formant, double bandWidth){
      double num, denom, aux, val;     
      aux = Math.pow((bandWidth/2), 2.0);
      num = Math.pow(formant, 2.0) + aux;
      denom = Math.sqrt( ( Math.pow((freq-formant), 2.0) + aux )*( Math.pow((freq+formant), 2.0) + aux) );
      val = (num/denom);
      if(val > 0.0)
        return (20 * Math.log10(val));
      else{
        System.out.println("vocalTractCompensation: warning value < 0.0");
        return 0.0;
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
    
    
    public static void main( String[] args ) throws Exception
    {
        int numFormants = 4;
        String wavFile = "/project/mary/marcela/HMM-voices/arctic_test/wav/curious.wav";
        String snackFile = "/project/mary/marcela/HMM-voices/arctic_test/vq/curious.snack";
        
        SnackVoiceQualityProcessor vq = new SnackVoiceQualityProcessor();
        
        // Read F0, formants and bandwidths
        double[][] snackData = vq.getData(numFormants, snackFile);            
        System.out.println("f0_formants size=" + snackData.length);
  
        // Read the sound file
        WavReader wf = new WavReader(wavFile);
        int sampleRate = wf.getSampleRate();
        
        // calculate voice quality parameters for this file
        int frameLength = 80;
        int windowLength = 240;
        vq.calculateVoiceQuality(snackData, frameLength, windowLength, wf);
        
    }
    
}

