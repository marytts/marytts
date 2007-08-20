package de.dfki.lt.signalproc.process;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.SelectedUnit;
import de.dfki.lt.mary.unitselection.Unit;
import de.dfki.lt.mary.unitselection.concat.DatagramDoubleDataSource;
import de.dfki.lt.mary.unitselection.concat.DatagramOverlapDoubleDataSource;
import de.dfki.lt.signalproc.FFTMixedRadix;
import de.dfki.lt.signalproc.analysis.F0Reader;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.LEDataInputStream;
import de.dfki.lt.signalproc.util.LEDataOutputStream;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.MathUtils.Complex;
import de.dfki.lt.signalproc.window.Window;
import de.dfki.lt.signalproc.window.DynamicWindow;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;
import de.dfki.lt.signalproc.util.SignalProcUtils;
import de.dfki.lt.signalproc.util.InterpolationUtils;
import de.dfki.lt.signalproc.analysis.PitchMarker;

public class FDPSOLAProcessor extends VocalTractModifier {
    private DoubleDataSource input;
    private AudioInputStream inputAudio;
    private DDSAudioInputStream outputAudio;
    private VoiceModificationParametersPreprocessor modParams;
    private int numfrm;
    private int numfrmFixed;
    private int P; //LP analysis order
    private String outputFile;
    private String tempOutBinaryFile;
    private int origLen;
    private PitchMarker pm;
    private PSOLAFrameProvider psFrm;
    private double wsFixed;
    private double ssFixed;
    private int numPeriods;
    private static int NUM_PITCH_SYNC_PERIODS = 2;
    
    private static int FROM_CODE = 0;
    private static int FROM_FILE = 1;
    private static int FROM_TARGET = 2;
    
    private boolean bSilent;
    private LEDataOutputStream dout; //Output stream for big-endian wav tests
    private LEDataInputStream din; //Input stream for big-endian wav tests
    private DynamicWindow windowIn;
    private DynamicWindow windowOut;
    private double [] wgt;
    private double [] wgty;
    
    private int frmSize;
    private int newFrmSize;
    private int newPeriod;
    private int synthFrmInd;
    private double localDurDiff;
    private int repeatSkipCount; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame
    private double localDurDiffSaved;
    private double sumLocalDurDiffs;
    private double nextAdd;

    private int synthSt;
    private int synthTotal;
    
    private int maxFrmSize;
    private int maxNewFrmSize;
    private int synthFrameInd;
    private boolean bLastFrame;
    private boolean bBroke;
    private int newFftSize;
    private int newMaxFreq;
    
    private int outBuffLen;
    private double [] outBuff;
    private int outBuffStart;
    private int totalWrittenToFile;

    private double [] ySynthBuff;
    private double [] wSynthBuff;
    private int ySynthInd;
    private double [] frm;
    private boolean bWarp;
    
    private double [] Py;
    private double [] Py2;
    private Complex Hy;
    private double [] frmy;
    private double frmEn;
    private double frmyEn;
    private double gain;
    private int newSkipSize;
    private int halfWin;
    private double [] newVScales;
    private double [] tmpvsc;
    private boolean isWavFileOutput;
    private int inputFrameIndex;
    private static double MIN_PSCALE = 0.1;
    private static double MAX_PSCALE = 5.0;
    private static double MIN_TSCALE = 0.1;
    private static double MAX_TSCALE = 5.0;
 
    private double tscaleSingle;
    
    public FDPSOLAProcessor(String strInputFile, String strPitchFile, String strOutputFile,
                            double [] pscales, double [] tscales, double [] escales, double [] vscales) throws UnsupportedAudioFileException, IOException
    {
        super();
        
        isWavFileOutput = true;
        inputAudio = AudioSystem.getAudioInputStream(new File(strInputFile));
        input = new AudioDoubleDataSource(inputAudio);
        
        origLen = (int)input.getDataLength();
        fs = (int)inputAudio.getFormat().getSampleRate();
        P = SignalProcUtils.getLPOrder(fs);
        
        wsFixed = 0.02;
        ssFixed = 0.01;
        numPeriods = NUM_PITCH_SYNC_PERIODS;
        
        F0Reader f0 = new F0Reader(strPitchFile);
        pm = SignalProcUtils.pitchContour2pitchMarks(f0.getContour(), fs, origLen, f0.ws, f0.ss, true);
        
        numfrm = pm.pitchMarks.length-numPeriods; //Total pitch synchronous frames (This is the actual number of frames to be processed)
        numfrmFixed = (int)(Math.floor(((double)(origLen + pm.totalZerosToPadd)/fs-0.5*wsFixed)/ssFixed+0.5)+2); //Total frames if the analysis was fixed skip-rate
         
        modParams = new VoiceModificationParametersPreprocessor(fs, P,
                                                                pscales, tscales, escales, vscales,
                                                                pm.pitchMarks, wsFixed, ssFixed,
                                                                numfrm, numfrmFixed, numPeriods);
        tscaleSingle = modParams.tscaleSingle;
        
        outputFile = strOutputFile; 
        
        initialize();
    }
 
    public FDPSOLAProcessor()
    {
        super();
        
        isWavFileOutput = false;
        inputAudio = null;
        input = null;
        
        origLen = 0;
        fs = 16000;
        P = SignalProcUtils.getLPOrder(fs);
        
        wsFixed = 0.02;
        ssFixed = 0.01;
        numPeriods = NUM_PITCH_SYNC_PERIODS;
        
        pm = null;
        
        numfrm = 0; //Total pitch synchronous frames (This is the actual number of frames to be processed)
        numfrmFixed = 0; //Total frames if the analysis was fixed skip-rate
         
        modParams = null;
        
        //outputFile = null; 
        outputFile = "d:/tts_out.wav";
        
        tscaleSingle = 1.0;
        
        initialize();
    }
    
    protected void initialize()
    {
//      initialization
        tmpvsc = new double[1];
        bSilent = false;
        
        if (outputFile != null)
            tempOutBinaryFile = outputFile + ".bin";
        
        if (isWavFileOutput)
        {
            psFrm = new PSOLAFrameProvider(input, pm, modParams.fs, modParams.numPeriods);
            
            try {
                dout = new LEDataOutputStream(tempOutBinaryFile);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            psFrm = null;
            dout = null;
        }
        
        windowIn = new DynamicWindow(Window.HANN);
        windowOut = new DynamicWindow(Window.HANN);
        
        frmSize = 0;
        newFrmSize = 0;
        newPeriod = 0;
        synthFrmInd = 0;
        localDurDiff = 0.0;
        repeatSkipCount = 0; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame
        localDurDiffSaved = 0.0;
        sumLocalDurDiffs = 0.0;
        nextAdd = 0.0;

        if (isWavFileOutput)
            synthSt = pm.pitchMarks[0];
        else
            synthSt = 0;
        
        synthTotal = 0;
        
        maxFrmSize = (int)(numPeriods*fs/40.0);
        if ((maxFrmSize % 2) != 0)
            maxFrmSize++;
        
        maxNewFrmSize = (int)(Math.floor(maxFrmSize/MIN_PSCALE+0.5));
        if ((maxNewFrmSize % 2) != 0) 
            maxNewFrmSize++;

        synthFrameInd = 0;
        bLastFrame = false;
        bBroke = false;
        fftSize = (int)Math.pow(2, (Math.ceil(Math.log((double)maxFrmSize)/Math.log(2.0))));
        maxFreq = fftSize/2+1;
        
        outBuffLen = 500000;
        outBuff = MathUtils.zeros(outBuffLen);
        outBuffStart = 1;
        totalWrittenToFile = 0;

        ySynthBuff = MathUtils.zeros(maxNewFrmSize);
        wSynthBuff = MathUtils.zeros(maxNewFrmSize);
        ySynthInd = 1;
        //
    }
    
    public DDSAudioInputStream process(Datagram [][] datagrams, Datagram [] rightContexts, AudioFormat audioformat, boolean [][] voicings, double [][] pitchScales, double [][] timeScales)
    {
        int pitchSpecs = FROM_TARGET;
        //int pitchSpecs = FROM_FILE;
        //int pitchSpecs = FROM_CODE;
        int durationSpecs = FROM_TARGET;
        //int durationSpecs = FROM_FILE;
        //int durationSpecs = FROM_CODE;
        
        int i, j, k;
        double [] output = null;
        boolean isVoiced = true;
        double pscale=1.0;
        double tscale=1.0;
        double escale=1.0;
        double vscale=1.0;
        
        //Read pscale, tscale, escale and vscale from a text file.
        // (For quick testing purposes. It resest the input pichScales and timeScales to the fixed values in the text file.)
        if (pitchSpecs==FROM_FILE || durationSpecs==FROM_FILE)
        {
            double [] scales = getScalesFromTextFile("d:/psolaParam.txt");
            
            if (pitchSpecs==FROM_FILE)
                pscale = scales[0]; 
            
            if (durationSpecs==FROM_FILE)
                tscale = scales[1]; 
            
            escale = scales[2]; 
            vscale = scales[3]; 
        }
        //
        
        if (pitchSpecs==FROM_FILE || pitchSpecs==FROM_CODE || durationSpecs==FROM_FILE || durationSpecs==FROM_CODE)
        {
            for (i=0; i<timeScales.length; i++)
            {
                if (pitchSpecs==FROM_FILE || pitchSpecs==FROM_CODE)
                {
                    for (j=0; j<pitchScales[i].length; j++)
                        pitchScales[i][j] = pscale;
                }
                
                if (durationSpecs==FROM_FILE || durationSpecs==FROM_CODE)
                {
                    for (j=0; j<timeScales[i].length; j++)
                        timeScales[i][j] = tscale;
                }
            }
        }  
        
        double firstTScale = timeScales[0][0];
        tscaleSingle = firstTScale;
        for (i=0; i<timeScales.length; i++)
        {
            for (j=0; j<timeScales[i].length; j++)
            {
                if (i!=0 && j!=0 && timeScales[i][j]!=firstTScale)
                {
                    tscaleSingle = -1.0;
                    break;
                }
            }    
        }       
        
        boolean isLastInputFrame = false;
        int currentPeriod;
        int inputFrameSize;
        
        double [] frmIn = null;
        double [] frmTmp = null;
        int tmpLen;
        double [] yOut = null;
        double [] yOutTmp = null;
        Datagram [] tmpDatagram = new Datagram[1];
        
        origLen = 0;
        numfrm = 0;
        for (i=0; i<datagrams.length; i++)
        {
            for (j=0; j<datagrams[i].length; j++)
            {
                if (j==datagrams[i].length-1)
                {
                    if (rightContexts!=null && rightContexts[i]!=null)
                        origLen += datagrams[i][j].getDuration()+rightContexts[i].getDuration();
                    else
                        origLen += datagrams[i][j].getDuration();
                }
                else
                    origLen += datagrams[i][j].getDuration();
                
                numfrm++;
            }
        }
           
        int yCounter = -1;
        
        for (i=0; i<datagrams.length; i++)
        {
            for (j=0; j<datagrams[i].length; j++)
            {
                isVoiced = voicings[i][j];
                
                if (i==datagrams.length-1 && j==datagrams[i].length-1)
                    isLastInputFrame = true;
                
                currentPeriod = (int)datagrams[i][j].getDuration();
                
                if (j<datagrams[i].length-1)
                {
                    inputFrameSize = (int)datagrams[i][j].getDuration()+(int)datagrams[i][j+1].getDuration();
                    frmIn = new double[inputFrameSize];

                    tmpDatagram[0] = datagrams[i][j];
                    frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                    tmpLen = frmTmp.length;
                    System.arraycopy(frmTmp, 0, frmIn, 0, tmpLen);
                    
                    tmpDatagram[0] = datagrams[i][j+1];
                    frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                    System.arraycopy(frmTmp, 0, frmIn, tmpLen, frmTmp.length);
                }
                else
                {
                    if (rightContexts[i]!=null)
                    {
                        inputFrameSize = (int)datagrams[i][j].getDuration()+(int)rightContexts[i].getDuration();
                        frmIn = new double[inputFrameSize];
                        
                        tmpDatagram[0] = datagrams[i][j];
                        frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                        tmpLen = frmTmp.length;
                        System.arraycopy(frmTmp, 0, frmIn, 0, tmpLen);
                        
                        tmpDatagram[0] = rightContexts[i];
                        frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                        System.arraycopy(frmTmp, 0, frmIn, tmpLen, frmTmp.length);   
                    }
                    else
                    {
                        if (i<datagrams.length-1)
                        {
                            inputFrameSize = (int)datagrams[i][j].getDuration()+(int)datagrams[i+1][0].getDuration();
                            frmIn = new double[inputFrameSize];
                            
                            tmpDatagram[0] = datagrams[i][j];
                            frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                            tmpLen = frmTmp.length;
                            System.arraycopy(frmTmp, 0, frmIn, 0, tmpLen);
                            
                            tmpDatagram[0] = datagrams[i+1][0];
                            frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                            System.arraycopy(frmTmp, 0, frmIn, tmpLen, frmTmp.length);  
                        }
                        else
                        {
                            inputFrameSize = 2*(int)datagrams[i][j].getDuration();
                            frmIn = MathUtils.zeros(inputFrameSize);

                            tmpDatagram[0] = datagrams[i][j];
                            frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                            tmpLen = frmTmp.length;
                            System.arraycopy(frmTmp, 0, frmIn, 0, tmpLen);
                        }
                    }
                }
                    
                try {
                    output = processFrame(frmIn, isVoiced, pitchScales[i][j], timeScales[i][j], escale, vscale, isLastInputFrame, currentPeriod, inputFrameSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                boolean bBroken = false;
                if (output!=null)
                {
                    if (yOut==null)
                    {
                        yOut = new double[output.length];
                        System.arraycopy(output, 0, yOut, 0, output.length);
                    }
                    else
                    {
                        yOutTmp = new double[yOut.length];
                        System.arraycopy(yOut, 0, yOutTmp, 0, yOut.length);
                        yOut = new double[yOutTmp.length+output.length];
                        System.arraycopy(yOutTmp, 0, yOut, 0, yOutTmp.length);
                        System.arraycopy(output, 0, yOut, yOutTmp.length, output.length);
                    } 
                }
                
                if (bBroken)
                    break;
            }
        }
        
        try {
            output = writeFinal();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (output!=null)
        {
            if (yOut==null)
            {
                yOut = new double[output.length];
                System.arraycopy(output, 0, yOut, 0, output.length);
            }
            else
            {
                yOutTmp = new double[yOut.length];
                System.arraycopy(yOut, 0, yOutTmp, 0, yOut.length);
                yOut = new double[yOutTmp.length+output.length];
                System.arraycopy(yOutTmp, 0, yOut, 0, yOutTmp.length);
                System.arraycopy(output, 0, yOut, yOutTmp.length, output.length);
            }
        }
        
        double absMax = MathUtils.absMax(yOut);
        if (absMax>32700)
        {
            for (i=0; i<yOut.length; i++)
                yOut[i] = yOut[i]/absMax*32700;
        }
        
        return new DDSAudioInputStream(new BufferedDoubleDataSource(yOut), audioformat);
    }
    
    //Read scale factors from a text file for quick testing
    public double [] getScalesFromTextFile(String strScaleFile)
    {
        int i;
        double [] scales = new double[4];
        
        Reader r = null;
        try {
            r = new BufferedReader(new FileReader(strScaleFile));
        } catch (FileNotFoundException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        StreamTokenizer stok = new StreamTokenizer(r);
        stok.parseNumbers();

        try {
            stok.nextToken();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        for (i=0; i<scales.length; i++) {
            if (stok.ttype == StreamTokenizer.TT_NUMBER)
                scales[i] = stok.nval;

            try {
                stok.nextToken();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            r.close();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        return scales;
    }
    
    public void fdpsolaOnline() throws IOException
    {   
        int i;
        double [] frmIn;
        boolean isLastInputFrame;
        int inputFrameSize;
        int currentPeriod;
        
        inputFrameIndex = 0;
        for (i=0; i<numfrm; i++)
        {   
            frmIn = psFrm.getNextFrame();
            
            if (bBroke)
                break;
            
            if (i==numfrm-1)
                isLastInputFrame = true;
            else
                isLastInputFrame = false;

            currentPeriod = pm.pitchMarks[i+1]-pm.pitchMarks[i];
            inputFrameSize = pm.pitchMarks[i+modParams.numPeriods]-pm.pitchMarks[i]+1;
            
            processFrame(frmIn, pm.vuvs[i], modParams.pscalesVar[i], modParams.tscalesVar[i], modParams.escalesVar[i], modParams.vscalesVar[i], isLastInputFrame, 
                    currentPeriod, inputFrameSize);
        }

        writeFinal();
        
        convertToWav(inputAudio.getFormat());
    }
    
    public double [] processFrame(double [] frmIn, boolean isVoiced, double pscale, double tscale, double escale, double vscale, boolean isLastInputFrame, int currentPeriod, int inputFrameSize) throws IOException
    {   
        if (pscale<MIN_PSCALE)
            pscale = MIN_PSCALE;
        if (pscale>MAX_PSCALE)
            pscale = MAX_PSCALE;
        if (tscale<MIN_TSCALE)
            tscale = MIN_TSCALE;
        if (tscale>MAX_TSCALE)
            tscale = MAX_TSCALE;
        
        double [] output = null;
        double [] outputTmp = null;
        int j, k, wInd, kMax;
        int tmpFix, tmpAdd, tmpMul;
        int remain;
        int kInd;
        
        repeatSkipCount = 0; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame

        // Compute new frame sizes, change in durations due to pitch scaling, and required compensation amount in samples
        // &
        // Find out which pitch-scaled frames to repeat/skip for overall duration
        // compensation
        frmSize = inputFrameSize;
        if ((frmSize % 2) !=0) 
            frmSize++;
        if (frmSize<4)
            frmSize = 4;

        if (isVoiced)
        {
            newFrmSize = (int)(Math.floor(frmSize/pscale+0.5));
            if ((newFrmSize % 2) !=0)
                newFrmSize++;
            if (newFrmSize<4)
                newFrmSize = 4;
        }
        else
            newFrmSize = frmSize;

        newPeriod = (int)Math.floor(((double)newFrmSize)/NUM_PITCH_SYNC_PERIODS+0.5);
        //Compute duration compensation required:
        // localDurDiffs(i) = (DESIRED)-(AFTER PITCHSCALING)
        // (-) if expansion occured, (+) if compression occured
        // We aim to make this as close to zero as possible in the following duration compensation step
        localDurDiff = nextAdd + (frmSize*tscale-newFrmSize)/NUM_PITCH_SYNC_PERIODS;

        nextAdd = 0;
        if (localDurDiff<-0.1*newPeriod) //Expansion occured so skip this frame
        {
            repeatSkipCount--;
            if (!isLastInputFrame)
            {    
                nextAdd = localDurDiff+newPeriod;
                localDurDiff = 0;
            }
        }
        else if (localDurDiff>0.1*newPeriod) //Compression occured so repeat this frame
        {
            while (localDurDiff>0.1*newPeriod)
            {
                repeatSkipCount++;
                localDurDiff -= newPeriod;
            }

            if (!isLastInputFrame)
            {
                nextAdd = localDurDiff;
                localDurDiff = 0;
            }
        }

        sumLocalDurDiffs += localDurDiff;
        
        if (isLastInputFrame)
        {
            // Check the final length and perform additional repetitions if necessary
            localDurDiff = sumLocalDurDiffs;
            while (localDurDiff>0)
            {
                repeatSkipCount++;
                localDurDiff -= newPeriod;
            }
            //
        }
        
        if (isLastInputFrame)
        {
            repeatSkipCount++;
            bLastFrame = true;
        }
        
        if (repeatSkipCount>-1)
        {
            frm = MathUtils.zeros(frmSize);
            System.arraycopy(frmIn, 0, frm, 0, Math.min(frmIn.length, frmSize));
            wgt = windowIn.values(frmSize);

            if (vscale != 1.0)
                bWarp=true; 
            else
                bWarp=false; 
            
            if ((isVoiced && pscale!=1.0) || bWarp)
            {
                if (fftSize<frmSize)
                {
                    fftSize = (int)Math.pow(2, (Math.ceil(Math.log((double)frmSize)/Math.log(2.0))));
                    maxFreq = fftSize/2+1;
                }
                
                newMaxFreq = (int)Math.floor(maxFreq/pscale+0.5);
                
                if (newMaxFreq<3)
                    newMaxFreq=3;
                
                if ((newMaxFreq % 2) !=1)
                    newMaxFreq++;
                
                //This is for being able to use the FFT algorithm that works only with buffers of length power of two
                //If you have an FFT algorithm that works with any buffer size, simply remove this line
                //newMaxFreq = (int)Math.floor(0.5*MathUtils.closestPowerOfTwoAbove(2*(newMaxFreq-1))+1.5);
                //
                
                newFftSize = 2*(newMaxFreq-1);

                frmEn = SignalProcUtils.getEnergy(frm);

                //Compute LP and excitation spectrum
                initialise(P, fs, fftSize, true); //Perform only analysis
                windowIn.applyInline(frm, 0, frmSize); //Windowing
                applyInline(frm, 0, frmSize); //LP analysis
                
                //Expand/Compress the vocal tract spectrum in inverse manner
                Py = MathUtils.interpolate(vtSpectrum, newMaxFreq); //Interpolated vocal tract spectrum

                //Perform vocal tract scaling
                if (bWarp)
                {
                    tmpvsc[0] = vscale;
                    newVScales = InterpolationUtils.modifySize(tmpvsc, newMaxFreq); //Modify length to match current length of spectrum
                    
                    for (k=0; k<newVScales.length; k++)
                    {
                        if (newVScales[k]<0.05) //Put a floor to avoid divide by zero
                            newVScales[k]=0.05;
                    }
                    
                    Py2 = new double[newMaxFreq];
                    
                    for (k=0; k<newMaxFreq; k++)
                    {
                        wInd = (int)Math.floor((k+1)/newVScales[k]+0.5); //Find new indices
                        if (wInd<1)
                            wInd = 1;
                        if (wInd>newMaxFreq)
                            wInd = newMaxFreq;
                        
                        Py2[k] = Py[wInd-1];
                    }
                    
                    System.arraycopy(Py2, 0, Py, 0, newMaxFreq);
                }

                //Create output DFT spectrum
                Hy = new Complex(newFftSize);
                Hy.real = MathUtils.zeros(newFftSize);
                Hy.imag = MathUtils.zeros(newFftSize);

                System.arraycopy(this.h.real, 0, Hy.real, 0, Math.min(maxFreq, newFftSize));
                System.arraycopy(this.h.imag, 0, Hy.imag, 0, Math.min(maxFreq, newFftSize));

                //Copy & paste samples if required (COMPLEX VERSION TO SUPPORT PSCALE<=0.5)
                // This version fills the spectrum by flipping and pasting the original freq bins as many times as required.
                kMax = 1;
                while (newMaxFreq>(kMax+1)*(maxFreq-2))
                    kMax++;

                for (k=1; k<=kMax; k++)
                {
                    tmpFix = (maxFreq-2)*k;
                    if (k%2==1) //Odd mode
                    {
                        tmpAdd = maxFreq+2;
                        tmpMul = 1;
                    }
                    else
                    {
                        tmpAdd = -1;
                        tmpMul = -1;
                    }

                    for (j=tmpFix+3; j<=Math.min(newMaxFreq, maxFreq+tmpFix); j++)
                    {
                        Hy.real[j-1] = this.h.real[tmpMul*(tmpFix-j)+tmpAdd-1];
                        Hy.imag[j-1] = this.h.imag[tmpMul*(tmpFix-j)+tmpAdd-1];
                    }
                }

                Hy.real[newMaxFreq-1] = Math.sqrt(Hy.real[newMaxFreq-1]*Hy.real[newMaxFreq-1] + Hy.imag[newMaxFreq-1]*Hy.imag[newMaxFreq-1]);
                Hy.imag[newMaxFreq-1] = 0.0;
                
                //Convolution
                for (k=1; k<=newMaxFreq; k++)
                {
                    Hy.real[k-1] *= Py[k-1];
                    Hy.imag[k-1] *= Py[k-1];
                }
                
                for (k=newMaxFreq+1; k<=newFftSize; k++)
                {
                    Hy.real[k-1] = Hy.real[2*newMaxFreq-1-k];
                    Hy.imag[k-1] = -Hy.imag[2*newMaxFreq-1-k];
                }

                //Convert back to time domain
                //FFT.transform(Hy.real, Hy.imag, true);
                //Hy = FFTArbitraryLength.ifft(Hy);
                Hy = FFTMixedRadix.ifft(Hy);
                
                frmy = new double[newFrmSize];
                System.arraycopy(Hy.real, 0, frmy, 0, newFrmSize);
                
                frmyEn = SignalProcUtils.getEnergy(frmy);
                gain = (frmEn/Math.sqrt(frmSize))/(frmyEn/Math.sqrt(newFrmSize))*escale;
            }
            else
            {
                if (frmSize<newFrmSize)
                    newFrmSize = frmSize; 
                
                frmy = new double[newFrmSize];
                
                for (k=0; k<frmSize; k++)
                    frmy[k] = frm[k]*wgt[k];

                gain = escale;
            }            

            //Energy scale compensation + modification
            for (k=0; k<newFrmSize; k++)
            {
                frmy[k] *= gain;
            }
                
            for (j=1; j<=repeatSkipCount+1; j++)
            {
                if (isVoiced)
                    newSkipSize = (int)Math.floor(currentPeriod/pscale+0.5);
                else
                    newSkipSize = (int)Math.floor(currentPeriod+0.5);
                
                if ((isLastInputFrame && j==repeatSkipCount+1)) //| (i~=numfrm & all(repeatSkipCounts(i+1:numfrm)==-1)))
                    bLastFrame = true;
                else
                    bLastFrame = false;

                synthFrameInd++;

                wgty = windowOut.values(newFrmSize);
                
                if (synthFrameInd==1) //First frame: Do not window the first half of output speech frame to prevent overflow in normalization with hanning coeffs
                {
                    halfWin = (int)Math.floor(newFrmSize/2.0+0.5);
                    synthTotal = synthSt+newFrmSize;

                    //Keep output in an overlap-add buffer
                    if (ySynthInd+newFrmSize-1<=maxNewFrmSize)
                    {
                        for (k=ySynthInd; k<=ySynthInd+halfWin-1; k++)
                        {
                            ySynthBuff[k-1] = frmy[k-ySynthInd];
                            wSynthBuff[k-1] = 1.0;
                        }

                        for (k=ySynthInd+halfWin; k<=ySynthInd+newFrmSize-1; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                            wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                        }
                    }
                    else
                    {
                        for (k=ySynthInd; k<=maxNewFrmSize; k++)
                        {
                            if (k-ySynthInd<halfWin)
                            {
                                ySynthBuff[k-1] = frmy[k-ySynthInd];
                                wSynthBuff[k-1] = 1.0;
                            }
                            else
                            {
                                ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                                wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                            }
                        }

                        for (k=1; k<=newFrmSize-1-maxNewFrmSize+ySynthInd; k++)
                        {
                            if (maxNewFrmSize-ySynthInd+k<halfWin)
                            {
                                ySynthBuff[k-1] = frmy[maxNewFrmSize-ySynthInd+k];
                                wSynthBuff[k-1] = 1.0;
                            }
                            else
                            {
                                ySynthBuff[k-1] += frmy[maxNewFrmSize-ySynthInd+k]*wgty[maxNewFrmSize-ySynthInd+k];
                                wSynthBuff[k-1] += wgty[maxNewFrmSize-ySynthInd+k]*wgty[maxNewFrmSize-ySynthInd+k];
                            }
                        }
                    }
                    //

                    if (!bSilent)
                        System.out.println("Synthesized using frame "  + String.valueOf(inputFrameIndex+1)); 
                }
                else if (bLastFrame) //Last frame: Do not window the second half of output speech frame to prevent overflow in normalization with hanning coeffs
                {
                    halfWin = (int)Math.floor(newFrmSize/2.0+0.5);
                    remain = newFrmSize-halfWin;
                    synthTotal = synthSt+halfWin+remain-1;

                    //Keep output in an overlap-add buffer
                    if (ySynthInd+newFrmSize-1<=maxNewFrmSize)
                    {
                        for (k=ySynthInd; k<=ySynthInd+halfWin-1; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                            wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                        }
                        
                        for (k=ySynthInd+halfWin; k<=ySynthInd+newFrmSize-1; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd];
                            wSynthBuff[k-1] = 1.0;
                        }
                    }
                    else
                    {
                        for (k=ySynthInd; k<=maxNewFrmSize; k++)
                        {
                            if (k-ySynthInd<halfWin)
                            {
                                ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                                wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                            }
                            else
                            {
                                ySynthBuff[k-1] += frmy[k-ySynthInd];
                                wSynthBuff[k-1] = 1.0;
                            }
                        }

                        for (k=1; k<=newFrmSize-1-maxNewFrmSize+ySynthInd; k++)
                        {
                            if (maxNewFrmSize-ySynthInd+k<halfWin)
                            {
                                ySynthBuff[k-1] += frmy[maxNewFrmSize-ySynthInd+k]*wgty[maxNewFrmSize-ySynthInd+k];
                                wSynthBuff[k-1] += wgty[maxNewFrmSize-ySynthInd+k]*wgty[maxNewFrmSize-ySynthInd+k];
                            }
                            else
                            {
                                ySynthBuff[k-1] += frmy[maxNewFrmSize-ySynthInd+k];
                                wSynthBuff[k-1] = 1.0;
                            }
                        }
                    }
                    //
                    
                    if (!bSilent)
                        System.out.println("Synthesized using frame " + String.valueOf(inputFrameIndex+1)); 
                }
                else //Normal frame
                {
                    if (!isVoiced && ((repeatSkipCount%2)==1)) //Reverse unvoiced repeated frames once in two consecutive repetitions to reduce distortion
                        frmy = SignalProcUtils.reverse(frmy);

                    synthTotal = synthSt+newFrmSize;

                    //Keep output in an overlap-add buffer
                    if (ySynthInd+newFrmSize-1<=maxNewFrmSize)
                    {
                        for (k=ySynthInd; k<=ySynthInd+newFrmSize-1; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                            wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                        }
                    }
                    else
                    {
                        for (k=ySynthInd; k<=maxNewFrmSize; k++)
                        {
                            ySynthBuff[k-1] += frmy[k-ySynthInd]*wgty[k-ySynthInd];
                            wSynthBuff[k-1] += wgty[k-ySynthInd]*wgty[k-ySynthInd];
                        }

                        for (k=1; k<=newFrmSize-1-maxNewFrmSize+ySynthInd; k++)
                        {    
                            ySynthBuff[k-1] += frmy[k+maxNewFrmSize-ySynthInd]*wgty[k+maxNewFrmSize-ySynthInd];
                            wSynthBuff[k-1] += wgty[k+maxNewFrmSize-ySynthInd]*wgty[k+maxNewFrmSize-ySynthInd];
                        }
                    }
                    //

                    if (!bSilent)
                    {
                        if (j==1)
                            System.out.println("Synthesized using frame " + String.valueOf(inputFrameIndex+1)); 
                        else 
                            System.out.println("Repeated using frame " + String.valueOf(inputFrameIndex+1));  
                    }
                }

                //Write to output buffer
                for (k=0; k<=newSkipSize-1; k++)
                {
                    kInd = (k+ySynthInd) % maxNewFrmSize;
                    if (kInd==0)
                        kInd=maxNewFrmSize;

                    if (wSynthBuff[kInd-1]>0.0)
                        outBuff[outBuffStart-1] = ySynthBuff[kInd-1]/wSynthBuff[kInd-1];
                    else
                        outBuff[outBuffStart-1] = ySynthBuff[kInd-1];

                    ySynthBuff[kInd-1] = 0.0;
                    wSynthBuff[kInd-1] = 0.0;

                    outBuffStart++;
                    
                    if (outBuffStart>outBuffLen)
                    {
                        if (tscaleSingle!=1.0 || totalWrittenToFile+outBuffLen<=origLen)
                        {
                            if (isWavFileOutput)
                                dout.writeDouble(outBuff, 0, outBuffLen);
                            else
                            { 
                                if (output == null)
                                {
                                    output = new double[outBuffLen];
                                    System.arraycopy(outBuff, 0, output, 0, outBuffLen);
                                }
                                else
                                {
                                    outputTmp = new double[output.length];
                                    System.arraycopy(output, 0, outputTmp, 0, output.length);
                                    output = new double[outputTmp.length + outBuffLen];
                                    System.arraycopy(outputTmp, 0, output, 0, outputTmp.length);
                                    System.arraycopy(outBuff, 0, output, outputTmp.length, outBuffLen);
                                }
                            }
                            
                            totalWrittenToFile += outBuffLen;
                        }
                        else
                        {   
                            if (isWavFileOutput)
                                dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile);
                            else
                            { 
                                if (output == null)
                                {
                                    output = new double[origLen-totalWrittenToFile];
                                    System.arraycopy(outBuff, 0, output, 0, origLen-totalWrittenToFile);  
                                }
                                else
                                {
                                    outputTmp = new double[output.length];
                                    System.arraycopy(output, 0, outputTmp, 0, output.length);
                                    output = new double[outputTmp.length + origLen-totalWrittenToFile];
                                    System.arraycopy(outputTmp, 0, output, 0, outputTmp.length);
                                    System.arraycopy(outBuff, 0, output, outputTmp.length, origLen-totalWrittenToFile);
                                }
                            }
                            
                            totalWrittenToFile = origLen;
                        }
                        
                        outBuffStart=1;
                    }
                }
                //

                synthSt += newSkipSize;

                //if (!bLastFrame)
                //{
                    if (ySynthInd+newSkipSize<=maxNewFrmSize)
                        ySynthInd += newSkipSize;
                    else
                        ySynthInd += newSkipSize-maxNewFrmSize;
                //}
                /////////

                if (bLastFrame) 
                {
                    bBroke = true; 
                    break; 
                }
            }
        }
        else
        {
            if (!bSilent)
                System.out.println("Skipped frame " + String.valueOf(inputFrameIndex+1));
        }
        
        inputFrameIndex++;
        
        return output;
    }
    
    public double [] writeFinal() throws IOException
    {
        double [] output = null;
        double [] outputTmp = null;
        
        int k, kInd;
        
        if (tscaleSingle==1.0)
            synthTotal=origLen;

        if (outBuffLen>synthTotal)
            outBuffLen = synthTotal;

        //Write the final segment
        for (k=synthSt; k<=synthTotal; k++)
        {
            kInd = (k-synthSt+ySynthInd)%maxNewFrmSize; 

            if (kInd==0)
                kInd=maxNewFrmSize;

            if (wSynthBuff[kInd-1]>0.0)
                outBuff[outBuffStart-1] = ySynthBuff[kInd-1]/wSynthBuff[kInd-1];
            else
                outBuff[outBuffStart-1] = ySynthBuff[kInd-1];

            ySynthBuff[kInd-1] = 0.0;
            wSynthBuff[kInd-1] = 0.0;

            outBuffStart++;

            if (outBuffStart>outBuffLen)
            {                
                if (tscaleSingle!=1.0 || totalWrittenToFile+outBuffLen<=origLen)
                {
                    if (isWavFileOutput)
                        dout.writeDouble(outBuff, 0, outBuffLen);
                    else
                    { 
                        if (output == null)
                        {
                            output = new double[outBuffLen];
                            System.arraycopy(outBuff, 0, output, 0, outBuffLen);
                        }
                        else
                        {
                            outputTmp = new double[output.length];
                            System.arraycopy(output, 0, outputTmp, 0, output.length);
                            output = new double[outputTmp.length + outBuffLen];
                            System.arraycopy(outputTmp, 0, output, 0, outputTmp.length);
                            System.arraycopy(outBuff, 0, output, outputTmp.length, outBuffLen);
                        }
                    }
                    
                    totalWrittenToFile += outBuffLen;
                }
                else
                {
                    if (isWavFileOutput)
                        dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile);
                    else
                    {
                        if (output == null)
                        {
                            output = new double[origLen-totalWrittenToFile];
                            System.arraycopy(outBuff, 0, output, 0, origLen-totalWrittenToFile);
                        }
                        else
                        {
                            outputTmp = new double[output.length];
                            System.arraycopy(output, 0, outputTmp, 0, output.length);
                            output = new double[outputTmp.length + origLen-totalWrittenToFile];
                            System.arraycopy(outputTmp, 0, output, 0, outputTmp.length);
                            System.arraycopy(outBuff, 0, output, outputTmp.length, origLen-totalWrittenToFile);
                        }
                    }
                    
                    totalWrittenToFile = origLen;
                }
                outBuffStart=1;
            }
        }

        if (outBuffStart>1)
        {            
            if (tscaleSingle!=1.0 || totalWrittenToFile+outBuffStart-1<=origLen)
            {
                if (isWavFileOutput)
                    dout.writeDouble(outBuff, 0, outBuffStart-1);
                else
                {
                    if (output == null)
                    {
                        output = new double[outBuffStart-1];
                        System.arraycopy(outBuff, 0, output, 0, outBuffStart-1);
                    }
                    else
                    {
                        outputTmp = new double[output.length];
                        System.arraycopy(output, 0, outputTmp, 0, output.length);
                        output = new double[outputTmp.length + outBuffStart-1];
                        System.arraycopy(outputTmp, 0, output, 0, outputTmp.length);
                        System.arraycopy(outBuff, 0, output, outputTmp.length, outBuffStart-1);
                    }
                }
                
                totalWrittenToFile += outBuffStart-1;
            }
            else
            {
                if (isWavFileOutput)
                    dout.writeDouble(outBuff, 0, origLen-totalWrittenToFile);
                else
                { 
                    if (output == null)
                    {
                        output = new double[origLen-totalWrittenToFile];
                        System.arraycopy(outBuff, 0, output, 0, origLen-totalWrittenToFile);
                    }
                    else
                    {
                        outputTmp = new double[output.length];
                        System.arraycopy(output, 0, outputTmp, 0, output.length);
                        output = new double[outputTmp.length + origLen-totalWrittenToFile];
                        System.arraycopy(outputTmp, 0, output, 0, outputTmp.length);
                        System.arraycopy(outBuff, 0, output, outputTmp.length, origLen-totalWrittenToFile);
                    }
                }
            }

            totalWrittenToFile = origLen;
        }
        //

        if (dout!=null)
            dout.close();
        
        return output;
    }
    
    public void convertToWav(AudioFormat audioformat) throws IOException
    {
        //Read the temp binary file into a wav file and delete the temp binary file
        if (tempOutBinaryFile!=null)
        {
            double [] yOut = null;
            
            din = new LEDataInputStream(tempOutBinaryFile);
            yOut = din.readDouble(totalWrittenToFile);
            din.close();

            double tmpMax = MathUtils.getAbsMax(yOut);
            if (tmpMax>32735)
            {
                for (int n=0; n<yOut.length; n++)
                    yOut[n] *= 32735/tmpMax;
            }

            outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(yOut), audioformat);
            AudioSystem.write(outputAudio, AudioFileFormat.Type.WAVE, new File(outputFile));

            File tmpFile = new File(tempOutBinaryFile);
            tmpFile.delete();
            //
        }
    }
    
    public static void main(String[] args) throws Exception
    {  
        String strOutputFile = args[0].substring(0, args[0].length()-4) + "_fdJav.wav";
        String strPitchFile = args[0].substring(0, args[0].length()-4) + ".ptc";
        
        double [] pscales = {1.2, 0.3};
        double [] tscales = {1.5};
        double [] escales = {1.0};
        double [] vscales = {1.8, 0.4};
       
        FDPSOLAProcessor fd = new FDPSOLAProcessor(args[0], strPitchFile, strOutputFile, 
                                                    pscales, tscales, escales, vscales);
        
        fd.fdpsolaOnline();
        
    }
}
