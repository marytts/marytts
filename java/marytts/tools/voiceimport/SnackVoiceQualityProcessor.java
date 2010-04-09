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
import marytts.signalproc.window.HammingWindow;
import marytts.signalproc.window.Window;
import marytts.signalproc.analysis.VoiceQuality;
import marytts.tools.voiceimport.SphinxTrainer.StreamGobbler;
import marytts.util.MaryUtils;
import marytts.util.data.text.SnackTextfileDoubleDataSource;
import marytts.util.math.FFT;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;



public class SnackVoiceQualityProcessor extends VoiceImportComponent {
    
    protected DatabaseLayout db;
    private String name = "SnackVoiceQualityProcessor";
    
    protected String snackExtension = ".snack";
    protected String voiceQualityExtension = ".vq";
    protected String scriptFileName;
    

    int numVqParams = 5;           // number of voice quality parameters extracted from the sound files:
                                   // OQG, GOG, SKG, RCG, IC
      
    private int percent = 0;
    //private final String FRAMELENGTH  = "0.01";   // Default for snack
    //private final String WINDOWLENGTH = "0.025";  // Default for f0 snack ( formants uses a bigger window)
  
    public final String SAMPLINGRATE = "SnackVoiceQualityProcessor.samplingRate";
    public final String MINPITCH     = "SnackVoiceQualityProcessor.minPitch";
    public final String MAXPITCH     = "SnackVoiceQualityProcessor.maxPitch";
    public final String FRAMELENGTH  = "SnackVoiceQualityProcessor.frameLength";
    public final String WINDOWLENGTH = "SnackVoiceQualityProcessor.windowLength";
    public final String NUMFORMANTS  = "SnackVoiceQualityProcessor.numFormants";
    public final String LPCORDER     = "SnackVoiceQualityProcessor.lpcOrder";
    public final String FFTSIZE     = "SnackVoiceQualityProcessor.fftSize";
    public final String VQDIR        = "SnackVoiceQualityProcessor.vqDir";  
    
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
            
        }
    }

    public final String getName(){
        return name;
    }

    public void initialiseComp()
    {
        scriptFileName = db.getProp(db.TEMPDIR) + "f0_formants.tcl";
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
        //toScript.println("puts \"f0 length = $f0_length\"");
        toScript.println("set formants [s formant -numformants [lindex $argv 5] -lpcorder [lindex $argv 6] -framelength [lindex $argv 4] -windowlength 0.03]");
        toScript.println("set formants_length [llength $formants]");
        //toScript.println("puts \"formants length = $formants_length\"");
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
        
        
        // Some general parameters that apply to all the sound files
        int samplingRate = Integer.parseInt(getProp(SAMPLINGRATE));         
        // frameLength and windowLength in samples
        int frameLength = Math.round(Float.parseFloat(getProp(FRAMELENGTH)) * samplingRate);
        int windowLength = Math.round(Float.parseFloat(getProp(WINDOWLENGTH)) * samplingRate);
        
        // get a Hamming window
        Window hammWin = new HammingWindow(windowLength);
        
        // Matrix for calculating Bark spectrum
        int fftSize = Integer.parseInt(getProp(FFTSIZE));        
        int nfilts = (int)Math.ceil(SignalProcUtils.hz2bark(samplingRate/2)) + 1;        
        int minfreq = 0;
        int maxfreq =  samplingRate/2;
        int bwidth =  1;
        double barkMatrix[][] = SignalProcUtils.fft2barkmx(fftSize, samplingRate, nfilts, bwidth, minfreq, maxfreq);
        
        
        /* execute snack and voice quality parameters extraction */        
        for ( int i = 0; i < baseNameArray.length; i++ ) {
            percent = 100*i/baseNameArray.length;
            String wavFile   = db.getProp(db.WAVDIR) + baseNameArray[i] + db.getProp(db.WAVEXT);
            String snackFile = getProp(VQDIR) + baseNameArray[i] + snackExtension;
            String vqFile    = getProp(VQDIR) + baseNameArray[i] + voiceQualityExtension;
            
            System.out.println("Writing f0+formants+bandWidths to " + snackFile);

            boolean isWindows = true;
            String strTmp = scriptFileName + " " + wavFile + " " + snackFile + " " + getProp(MAXPITCH) + " " + getProp(MINPITCH)
                                           + " " + getProp(FRAMELENGTH) + " " + getProp(NUMFORMANTS) + " " + getProp(LPCORDER);

            if (MaryUtils.isWindows())
                strTmp = "cmd.exe /c " + db.getExternal(db.TCLPATH) + "/tclsh " + strTmp;
            else
                strTmp = db.getExternal(db.TCLPATH) + "/tclsh " + strTmp;
            
            //System.out.println("Executing: " + strTmp);
                
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
            double[][] snackData = readSnackData(Integer.parseInt(getProp(NUMFORMANTS)), snackFile);            
            //System.out.println("f0_formants size=" + snackData.length);
      
            // Read the sound file
            WavReader soundFile = new WavReader(wavFile);
           
            // Check sampling rate of sound file
            assert samplingRate==soundFile.getSampleRate();
            
            // calculate voice quality parameters for this file           
            VoiceQuality vq = new VoiceQuality(numVqParams,samplingRate, frameLength/samplingRate,windowLength/samplingRate);                 
            calculateVoiceQuality(snackData, frameLength, windowLength, soundFile, hammWin, barkMatrix, fftSize, vq, false);
            System.out.println("Writing vq parameters to " + vqFile);
            vq.writeVqFile(vqFile);
            
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
    public double[][] readSnackData(int numFormants, String snackFile) throws IOException
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
    public void calculateVoiceQuality(double snack[][], int frameLength, int windowLength, WavReader sound, 
        Window hammWin, double[][] barkMatrix, int fftSize, VoiceQuality vq, boolean debug)throws Exception{
        
      int i, j, k, n, T, T2, index, index1, index2, index3;  
      short x_signal[] = sound.getSamples();      
      double x[] = new double[windowLength];
      int samplingRateInHz = 16000;
      double magf[] = null;           // spectrum of a window 
      double magfdB[] = null;         // spectrum of a window in dB
      double barkmagfdB[] = null;     // Bark spectrum of a window in dB
      double Xpeak[] = null;          // the harmonic peaks
      int windowType = 1;             // 1: Hamming window
      int maxFreqIndex = fftSize/2;
      double Fp, Fp2, Fp3, F1, F2, F3, F4, B1, B2, B3, B4, H1, H2, H3, F1p, F2p, F3p, F4p, A1p, A2p, A3p, A4p;
      double hatH1, hatH2, hatA1p, hatA2p, hatA3p;
      double OQ, OQG, GO, GOG, SK, SKG, RC, RCG, IC;
      double f0;
      int f0Length = snack.length;
      double parVq[][] = new double[vq.params.dimension][f0Length];
     
      // Normalise the signal before processing between 1 and -1 
      //This was for getting similar values as in octave
      /*
      double x_signal_double[] = new double[sound.getNumSamples()];
      for (i=0; i<sound.getNumSamples(); i++)        
        x_signal_double[i] = x_signal[i];
      double MaxSample = MathUtils.getAbsMax(x_signal_double);
      for (i=0; i<sound.getNumSamples(); i++)        
        x_signal_double[i] = ( x_signal_double[i] / MaxSample );
      */
      
      //Lugger conditions
      // 1. H1 > 60 HZ
      // 2. 1.5 H1 < H2 < 3H1
      // 3. H1 < F1 - B1
      
      //process per window
      int numFrame = 0;
      int numFrameVq = 0;
      for (n=0; n<(sound.getNumSamples() - windowLength) && numFrame <f0Length; n=n+frameLength ){
        
        f0 = snack[numFrame][0];
        if(debug)
          System.out.format("\npitch=%.2f numFrame=%d n=%d \n", f0, (numFrame+1),n);
        
        // First Lugger conditions
        // 1. H1 > 60 HZ
        if( f0 > 60.0 ) {
        
          // get the window frame
          for(i=0; i<windowLength; i++)
            x[i] = x_signal[n+i];
            //x[i] = x_signal_double[n+i];    // HERE USING NORMALISED SIGNAL to get similiar values as in octave                  
          
          // apply Hamming window
          x = hammWin.apply(x);
          //MaryUtils.plot(x, "x");
          
          // get the spectrum in dB (20*log10(F))       
          //SignalProcUtils.displayDFTSpectrumInDB(x, fftSize, windowType);
          // should be this in dB??? 
          // SPECTRUM
          magf = SignalProcUtils.getFrameMagnitudeSpectrum(x, fftSize, windowType);
          //magf = SignalProcUtils.getFrameHalfMagnitudeSpectrum(x, fftSize, windowType);
          //MaryUtils.plot(magf, "magf");
          //System.out.print("A=[");
          //for(i=0; i<(fftSize/2); i++)
          //  System.out.format("%.3f ", magf[i]);
          //System.out.println("];\n");
          
          // SPECTRUM dB
          magfdB = MathUtils.amp2db(magf);          
          //MaryUtils.plot(magfdB, "magfdB");
          
          // BARK SPECTRUM
          // double barkmagf[] = MathUtils.matrixProduct(wts, magf);
          //MaryUtils.plot(barkmagf, "barkX");          
          barkmagfdB = MathUtils.amp2db(MathUtils.matrixProduct(barkMatrix, magf));
          //MaryUtils.plot(barkmagfdB, "barkmagfdB");
          
         
          //TODO: These steps of finding peaks and magnitudes, need to be improved
          // the f0 from snack not always get close to the first peak found in the spectrum calculated here, also for 2f0. 
          // get the harmonic peak frequencies
          Xpeak = SignalProcUtils.getPeakAmplitudeFrequencies(magf, f0, 30, fftSize, samplingRateInHz, false);                                          
          //MaryUtils.plot(Xpeak, "Xpeak");
          //for(j=1; j<Xpeak.length; j++)
          //  System.out.println("peak[" + j + "]=" + Xpeak[j]);
          
          // Amplitude at Fp and 2Fp, it should be at the first two peak amplitude frquencies          
          Fp = Xpeak[0];
          index1 =  SignalProcUtils.freq2index(Xpeak[0], samplingRateInHz, maxFreqIndex);
          H1 = barkmagfdB[index1];
          Fp2 = Xpeak[1];
          index2 =  SignalProcUtils.freq2index(Xpeak[1], samplingRateInHz, maxFreqIndex);
          H2 = magf[index2];
          Fp3 = Xpeak[2];
          index3 =  SignalProcUtils.freq2index(Xpeak[2], samplingRateInHz, maxFreqIndex);
          H3 = magf[index3];
          
          if(debug){
            System.out.format("f0=%.2f\n", f0);
            System.out.format(" Fp=%.2f   nFp=%d\n", Fp, index1);
            System.out.format("2Fp=%.2f  nFp2=%d\n", Fp2, index2);
            System.out.format("3Fp=%.2f  nFp3=%d\n", Fp3, index3);
          }
          

          //TODO: These formants need to be localised in the spectrum as close as possible, here it is not done.
          // formants and bandwidths
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
          A1p = magf[index];
          
          index = findClosestHarmonicPeak(Xpeak, F2, maxFreqIndex);
          F2p = Xpeak[index];
          A2p = magf[index];
          
          index = findClosestHarmonicPeak(Xpeak, F3, maxFreqIndex);
          F3p = Xpeak[index];
          A3p = magf[index];
          
          index = findClosestHarmonicPeak(Xpeak, F4, maxFreqIndex);
          F4p = Xpeak[index];
          A4p = magf[index];
          
          if(debug){
            System.out.println("F1, B1 ori, F1p closestHarmonicPeak:");
            System.out.format("F1=%.2f F1p=%.2f A1p=%.2f B1=%.2f\n", F1, F1p, A1p, B1);
            System.out.format("F2=%.2f F2p=%.2f A2p=%.2f B2=%.2f\n", F2, F2p, A2p, B2);
            System.out.format("F3=%.2f F3p=%.2f A3p=%.2f B3=%.2f\n", F3, F3p, A3p, B3);
            System.out.format("F4=%.2f F4p=%.2f A4p=%.2f B4=%.2f\n", F4, F4p, A4p, B4);
          }
          
          //TODO: need to improve previous steps to find more preciselly the formants amplitudes 
          // it seems that the fft is not so good, at least not as precise as the one used in octave
          
          /* Second and third Lugger conditions:
             2. 1.5 H1 < H2 < 3H1
             3. H1 < F1 - B1
             if( (1.5*Fp) < Fp2 && Fp2 < (3*Fp) && Fp < (F1-B1) )
             apply this conditions after localize the peaks, otherwise condition 2 is always true
            
          */
          if( ( (1.5*Fp) < Fp2 ) && (Fp2 < Fp3) && Fp < (F1-B1) ) {
          
          // Put frequencies and amplitudes in Bark scale
          Fp  = SignalProcUtils.hz2bark(Fp);
          Fp2 = SignalProcUtils.hz2bark(Fp2);
          F1  = SignalProcUtils.hz2bark(F1);
          F2  = SignalProcUtils.hz2bark(F2);
          F3  = SignalProcUtils.hz2bark(F3);
          F4  = SignalProcUtils.hz2bark(F4);
          F1p = SignalProcUtils.hz2bark(F1p);
          F2p = SignalProcUtils.hz2bark(F2p);
          F3p = SignalProcUtils.hz2bark(F3p);
          B1  = SignalProcUtils.hz2bark(B1);
          B2  = SignalProcUtils.hz2bark(B2);
          B3  = SignalProcUtils.hz2bark(B3);
          B4  = SignalProcUtils.hz2bark(B4);
          
          // Remove vocal tract influence  
          // H1 is in dB already, and the vocalTractcompensation returns in db as well (20*log10)
          // Amplitud at Fp
          if(Fp<1)
            H1 = barkmagfdB[1];
          else
            H1 = barkmagfdB[(int)(Math.ceil(Fp))];
          hatH1 = H1 - ( vocalTractCompensation(Fp,F1,B1) + vocalTractCompensation(Fp,F2,B2) + vocalTractCompensation(Fp,F3,B3) + vocalTractCompensation(Fp,F4,B4) );          

          // Amplitud at 2Fp
          H2 = barkmagfdB[(int)(Math.ceil(Fp2))];
          hatH2 = H2 - ( vocalTractCompensation(Fp2,F1,B1) + vocalTractCompensation(Fp2,F2,B2) + vocalTractCompensation(Fp2,F3,B3) + vocalTractCompensation(Fp2,F4,B4) );          

          // Amplitud at A1p
          A1p = barkmagfdB[(int)(Math.ceil(F1p))];
          hatA1p = A1p - ( vocalTractCompensation(F1p,F2,B2) + vocalTractCompensation(F1p,F3,B3) + vocalTractCompensation(F1p,F4,B4) );          

          // Amplitud at A2p
          A2p = barkmagfdB[(int)(Math.ceil(F2p))];
          hatA2p = A2p - ( vocalTractCompensation(F2p,F1,B1) + vocalTractCompensation(F2p,F3,B3) + vocalTractCompensation(F2p,F4,B4) );          

          // Amplitud at A3p
          A3p = barkmagfdB[(int)(Math.ceil(F3p))];
          hatA3p = A3p - ( vocalTractCompensation(F3p,F1,B1) + vocalTractCompensation(F3p,F2,B2) + vocalTractCompensation(F3p,F4,B4) );
          
          if(debug) {
            System.out.format("H1=%.2f hatH1=%.2f\n", H1, hatH1);
            System.out.format("H2=%.2f hatH2=%.2f\n", H2, hatH2);
            System.out.format("A1p=%.2f hatA1p=%.2f\n", A1p, hatA1p);
            System.out.format("A2p=%.2f hatA2p=%.2f\n", A2p, hatA2p);
            System.out.format("A3p=%.2f hatA3p=%.2f\n", A3p, hatA3p);
          }
          
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
        
          parVq[0][numFrameVq] = OQG;
          parVq[1][numFrameVq] = GOG;
          parVq[2][numFrameVq] = SKG;
          parVq[3][numFrameVq] = RCG;
          parVq[4][numFrameVq] = IC;
          numFrameVq++;
                    
          if(debug) {
            System.out.println("numFrame=" + numFrame);
            System.out.format("OQ=%.2f OQG=%.4f\n", OQ,OQG);
            System.out.format("GO=%.2f GOG=%.4f\n", GO,GOG);
            System.out.format("SK=%.2f SKG=%.4f\n", SK,SKG);
            System.out.format("RC=%.2f RCG=%.4f\n", RC,RCG);
            System.out.format("IC=%.2f\n", IC);
            // pause
            System.in.read();
          }
          
          } else  // if( ( (1.5*Fp) < Fp2 ) && (Fp2 < Fp3) && Fp < (F1-B1) )
            if(debug)
              System.out.println("2,3 Lugger cond.");
        } else // if f0 > 60.0
          if(debug)
            System.out.println("1 Lugger cond.");
        
        numFrame++;  
      }
      
      vq.allocate(numFrameVq, parVq);
        
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
    
    // to test/compare vq values of several files
    public static void main3( String[] args ) throws Exception    
    {
               
        int numFormants = 4;
        //String wavFile = "/project/mary/marcela/HMM-voices/arctic_test/wav/curious.wav";
        //String wavFile = "/project/mary/marcela/HMM-voices/arctic_test/wav/a.wav";
        String whisperFile = "/project/mary/marcela/HMM-voices/arctic_test/vq/whisper.vq";
        String modalFile = "/project/mary/marcela/HMM-voices/arctic_test/vq/modal.vq";
        String harshFile = "/project/mary/marcela/HMM-voices/arctic_test/vq/harsh.vq";
                
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
        System.out.println("Reading: " + harshFile);
        vq3.readVqFile(harshFile);  
        //vq3.printPar();
        vq3.printMeanStd();
    }    
    
    // to test write and read vq files
    public static void main2( String[] args ) throws Exception    
    {
               
        int numFormants = 4;
        //String wavFile = "/project/mary/marcela/HMM-voices/arctic_test/wav/curious.wav";
        //String snackFile = "/project/mary/marcela/HMM-voices/arctic_test/vq/curious.snack";
        String wavFile = "/project/mary/marcela/HMM-voices/arctic_test/wav/a.wav";
        String snackFile = "/project/mary/marcela/HMM-voices/arctic_test/vq/a.snack";
        
        SnackVoiceQualityProcessor vqCalc = new SnackVoiceQualityProcessor();
        //double vqPar[][];
        
        // This example assumes that the F0, formants and bandwidths have been already calculated and stored
        // in a file name.snack.
        // Read F0, formants and bandwidths
        double[][] snackData = vqCalc.readSnackData(numFormants, snackFile);            
        //System.out.println("f0_formants size=" + snackData.length);
  
        // Read the sound file
        WavReader sounfFile = new WavReader(wavFile);
        int sampleRate = sounfFile.getSampleRate();
        
        // calculate voice quality parameters for this file
        int frameLength = 80;
        int windowLength = 400;
        int numVqParams = 5;
        int samplingRate = 16000;
        // get a Hamming window
        Window hammWin = new HammingWindow(windowLength);
        
        // Matrix for calculating Bark spectrum
        int fftSize = 512;        
        int nfilts = (int)Math.ceil(SignalProcUtils.hz2bark(samplingRate/2)) + 1;        
        int minfreq = 0;
        int maxfreq =  samplingRate/2;
        int bwidth =  1;
        double barkMatrix[][] = SignalProcUtils.fft2barkmx(fftSize, samplingRate, nfilts, bwidth, minfreq, maxfreq);
        
        VoiceQuality vq = new VoiceQuality(numVqParams,sampleRate, frameLength/sampleRate,windowLength/sampleRate);                 
        vqCalc.calculateVoiceQuality(snackData, frameLength, windowLength, sounfFile, hammWin, barkMatrix, fftSize, vq, false);
        vq.writeVqFile("/project/mary/marcela/HMM-voices/arctic_test/vq/a.vq");        
        vq.printPar();
        
        VoiceQuality vq1 = new VoiceQuality();        
        vq1.readVqFile("/project/mary/marcela/HMM-voices/arctic_test/vq/a.vq");  
        vq1.printPar();   
        
    }
    
    // to test the spectrum in bark scale        
    public static void main1( String[] args ) throws Exception    
    {
        String wavFile = "/project/mary/marcela/HMM-voices/arctic_test/wav/a.wav";     
       
        int i;
        int Fs = 16000;
        int Nfft = 512;
        int nfilts = (int)Math.ceil(SignalProcUtils.hz2bark(Fs/2)) + 1;        
        int minfreq = 0;
        int maxfreq =  Fs/2;
        int bwidth =  1;
        double wts[][] = SignalProcUtils.fft2barkmx(Nfft, Fs, nfilts, bwidth, minfreq, maxfreq);
        MaryUtils.plot(wts[10]);
        //for(int i=0; i<nfilts; i++){          
          //MaryUtils.plot(wts[i]);  
          //pause
          //System.in.read();           
          //for(int j=0; j<100; j++)
            //System.out.print(wts[i][j] + " ");            
          //System.out.println();
        //}
        // Read the sound file
        WavReader soundFile = new WavReader(wavFile);
        double magf[];
        double X[];
        double x_signal[] = new double[soundFile.getNumSamples()];
        double x[] = new double[400];
        short xs[] = soundFile.getSamples();
        
        for(i=0; i<soundFile.getNumSamples(); i++)
          x_signal[i] = (double)xs[i];
        
        // Normalise the signal before processing between 1 and -1 
        double MaxSample = MathUtils.getAbsMax(x_signal);
        for (i=0; i<soundFile.getNumSamples(); i++)        
          x_signal[i] = ( x_signal[i] / MaxSample );
        
        x_signal = MathUtils.normalizeToAbsMax(x_signal, MathUtils.getAbsMax(x_signal));
        
        for(i=300; i<700; i++)
          x[i-300] = x_signal[i];
        MaryUtils.plot(x, "x");
        magf = SignalProcUtils.getFrameMagnitudeSpectrum(x, 512, 1);
        MaryUtils.plot(magf, "magf");
        X = MathUtils.amp2db(magf);
        MaryUtils.plot(X, "X");
        
        double barkX[] = MathUtils.matrixProduct(wts, magf);
        MaryUtils.plot(barkX, "barkX");
        
        barkX = MathUtils.amp2db(barkX);
        MaryUtils.plot(barkX, "barkXdB");
        
    }
    
    
    public static void main( String[] args ) throws Exception {
      
      // to test the spectrum in bark scale
      // main1(args);
      
      // to test write and read vq files
      //main2(args);
      
      // to test/compare vq values of several files
      main3(args);
      
    }
    
}

