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
import de.dfki.lt.mary.util.FileUtils;
import de.dfki.lt.signalproc.FFTMixedRadix;
import de.dfki.lt.signalproc.analysis.F0ReaderWriter;
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
    public static int WAVEFORM_MODIFICATION = 1;
    public static int TTS_MODIFICATION = 2;
    
    protected DoubleDataSource input;
    protected AudioInputStream inputAudio;
    protected DDSAudioInputStream outputAudio;
    protected VoiceModificationParametersPreprocessor modParams;
    protected int numfrm;
    protected int numfrmFixed;
    protected int lpOrder; //LP analysis order
    protected String outputFile;
    protected String tempOutBinaryFile;
    protected int origLen;
    protected PitchMarker pm;
    protected double[] f0s;
    protected PSOLAFrameProvider psFrm;
    protected double wsFixedInSeconds;
    protected double ssFixedInSeconds;
    protected int numPeriods;
    protected static int NUM_PITCH_SYNC_PERIODS = 3;
    
    protected static int FROM_CODE = 0;
    protected static int FROM_FILE = 1;
    protected static int FROM_TARGET = 2;
    
    public boolean bSilent;
    protected LEDataOutputStream dout; //Output stream for big-endian wav tests
    protected LEDataInputStream din; //Input stream for big-endian wav tests
    protected DynamicWindow windowIn;
    protected DynamicWindow windowOut;
    protected double [] wgt;
    protected double [] wgty;
    
    protected int frmSize;
    protected int newFrmSize;
    protected int newPeriod;
    protected int synthFrmInd;
    protected double localDurDiff;
    protected int repeatSkipCount; // -1:skip frame, 0:no repetition (use synthesized frame as it is), >0: number of repetitions for synthesized frame
    protected double localDurDiffSaved;
    protected double sumLocalDurDiffs;
    protected double nextAdd;

    protected int synthSt;
    protected int synthTotal;
    
    protected int maxFrmSize;
    protected int maxNewFrmSize;
    protected int synthFrameInd;
    protected boolean bLastFrame;
    protected boolean bBroke;
    protected int newFftSize;
    protected int newMaxFreq;
    
    protected int outBuffLen;
    protected double [] outBuff;
    protected int outBuffStart;
    protected int totalWrittenToFile;

    protected double [] ySynthBuff;
    protected double [] wSynthBuff;
    protected int ySynthInd;
    protected double [] frm;
    protected boolean bWarp;
    
    protected double [] inputVT;
    protected double [] py2;
    protected Complex hy;
    protected double [] frmy;
    protected double frmEn;
    protected double frmyEn;
    protected double gain;
    protected int newSkipSize;
    protected int halfWin;
    protected double [] newVScales;
    protected double [] tmpvsc;
    protected boolean isWavFileOutput;
    protected int inputFrameIndex;
    protected static double MIN_PSCALE = 0.1;
    protected static double MAX_PSCALE = 5.0;
    protected static double MIN_TSCALE = 0.1;
    protected static double MAX_TSCALE = 5.0;
 
    protected double tscaleSingle;
    
    public FDPSOLAProcessor(String strInputFile, String strPitchFile, String strOutputFile,
                            double [] pscales, double [] tscales, double [] escales, double [] vscales) throws UnsupportedAudioFileException, IOException
    {
        this(strInputFile, strPitchFile, strOutputFile,
             pscales, tscales, escales, vscales, false);
    }

    public FDPSOLAProcessor(String strInputFile, String strPitchFile, String strOutputFile,
                            double [] pscales, double [] tscales, double [] escales, double [] vscales, boolean isFixedRate) throws UnsupportedAudioFileException, IOException
    {
        super();

        init(WAVEFORM_MODIFICATION, strInputFile, strPitchFile, strOutputFile,
                pscales, tscales, escales, vscales, isFixedRate);
    }

    public FDPSOLAProcessor()
    {
        super();
        
        init(TTS_MODIFICATION);
    }
    
    protected void init(int initialisationType)
    {
        init(initialisationType, null, null, null, null, null, null, null, false);
    }
    
    protected void init(int initialisationType, String strInputFile, String strPitchFile, String strOutputFile,
                        double [] pscales, double [] tscales, double [] escales, double [] vscales,
                        boolean isFixedRate)
    {
        isWavFileOutput = false;
        inputAudio = null;
        input = null;
        pm = null;
        f0s = null;
        
        wsFixedInSeconds = 0.02;
        ssFixedInSeconds = 0.01;
        numPeriods = NUM_PITCH_SYNC_PERIODS;
        
        origLen = 0;
        fs = 16000;
        
        numfrm = 0; //Total pitch synchronous frames (This is the actual number of frames to be processed)
        numfrmFixed = 0; //Total frames if the analysis was fixed skip-rate
         
        modParams = null;
        
        outputFile = null; 
        
        tscaleSingle = 1.0;

        boolean bContinue = true;
        
        if (initialisationType==WAVEFORM_MODIFICATION)
        {
            isWavFileOutput = true;
            
            if (!FileUtils.exists(strInputFile))
            {
                System.out.println("Error! Pitch file " + strInputFile + " not found.");
                bContinue = false;
            }

            if (!FileUtils.exists(strPitchFile))
            {
                System.out.println("Error! Pitch file " + strPitchFile + " not found.");
                bContinue = false;
            }

            if (strOutputFile==null || strOutputFile=="")
            {
                System.out.println("Invalid output file...");
                bContinue = false;
            }
            
            if (bContinue)
            {
                try {
                    inputAudio = AudioSystem.getAudioInputStream(new File(strInputFile));
                } catch (UnsupportedAudioFileException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                input = new AudioDoubleDataSource(inputAudio);

                origLen = (int)input.getDataLength();
                fs = (int)inputAudio.getFormat().getSampleRate();

                F0ReaderWriter f0 = new F0ReaderWriter(strPitchFile);
                pm = SignalProcUtils.pitchContour2pitchMarks(f0.contour, fs, origLen, f0.header.ws, f0.header.ss, true);

                numfrmFixed = (int)(Math.floor(((double)(origLen + pm.totalZerosToPadd)/fs-0.5*wsFixedInSeconds)/ssFixedInSeconds+0.5)+2); //Total frames if the analysis was fixed skip-rate
                if (!isFixedRate)
                    numfrm = pm.pitchMarks.length-numPeriods; //Total pitch synchronous frames (This is the actual number of frames to be processed)
                else
                    numfrm = numfrmFixed;
                
                f0s = SignalProcUtils.fixedRateF0Values(pm, wsFixedInSeconds, ssFixedInSeconds, numfrmFixed, fs);
                
                lpOrder = SignalProcUtils.getLPOrder(fs);
                
                modParams = new VoiceModificationParametersPreprocessor(fs, lpOrder,
                        pscales, tscales, escales, vscales,
                        pm.pitchMarks, wsFixedInSeconds, ssFixedInSeconds,
                        numfrm, numfrmFixed, numPeriods, isFixedRate);
                tscaleSingle = modParams.tscaleSingle;

                outputFile = strOutputFile;    
            }
        }
        else if (initialisationType==TTS_MODIFICATION)
        {
            //For test purposes, remove this line if you do not need additional wav file output
            //outputFile = "d:/tts_out.wav";
            lpOrder = SignalProcUtils.getLPOrder(fs);
        }
        
        if (bContinue)
        {
            tmpvsc = new double[1];
            bSilent = false;

            if (outputFile != null)
                tempOutBinaryFile = outputFile + ".bin";

            if (isWavFileOutput)
            {
                if (!isFixedRate)
                    psFrm = new PSOLAFrameProvider(input, pm, modParams.fs, modParams.numPeriods);
                else
                    psFrm = new PSOLAFrameProvider(input, wsFixedInSeconds, ssFixedInSeconds, modParams.fs, numfrm);

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
    }
    
    //FD-PSOLA using all concatenation units
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
 
        double pscale=1.0; //if pitchSpecs==FROM_CODE flag, this value will be used for pitch scaling
        double tscale=1.0; //if durationSpecs==FROM_CODE flag, this value will be used for duration scaling  
        double escale=1.0;
        double vscale=1.0;
        
        //Read pscale, tscale, escale and vscale from a text file.
        // (For quick testing purposes. It resets the input pichScales and timeScales to the fixed values in the text file.)
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
        
        boolean bLastInputFrame = false;
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
                if (i==datagrams.length-1 && j==datagrams[i].length-1)
                    bLastInputFrame = true;
                else
                    bLastInputFrame = false;
                
                frmIn = null;
                inputFrameSize = 0;
                
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
                   
                if (frmIn!=null) //We have a frame to be processed
                {
                    //isVoiced = voicings[i][j];
                    isVoiced = SignalProcUtils.getVoicing(frmIn, (int)(audioformat.getSampleRate()), 0.35f);

                    try {
                        output = processFrame(frmIn, isVoiced, pitchScales[i][j], timeScales[i][j], escale, vscale, bLastInputFrame, currentPeriod, inputFrameSize);
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
    
    //FD-PDSOLA on the whole signal with specified pitch marks
    public DDSAudioInputStream process(double[] x, int [] pitchMarks, AudioFormat audioformat, boolean [] voicings, double [] pitchScales, double [] timeScales)
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
 
        double pscale=1.0; //if pitchSpecs==FROM_CODE flag, this value will be used for pitch scaling
        double tscale=1.0; //if durationSpecs==FROM_CODE flag, this value will be used for duration scaling  
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
            if (pitchSpecs==FROM_FILE || pitchSpecs==FROM_CODE)
            {
                for (i=0; i<pitchScales.length; i++)
                    pitchScales[i] = pscale;
            }

            if (durationSpecs==FROM_FILE || durationSpecs==FROM_CODE)
            {
                for (i=0; i<timeScales.length; i++)
                    timeScales[i] = tscale;
            }
        }  
        
        double firstTScale = timeScales[0];
        tscaleSingle = firstTScale;
        for (i=0; i<timeScales.length; i++)
        {
            if (i!=0 && timeScales[i]!=firstTScale)
            {
                tscaleSingle = -1.0;
                break;
            } 
        }       
        
        boolean bLastInputFrame = false;
        int currentPeriod;
        int inputFrameSize;
        
        double [] frmIn = null;
        double [] frmTmp = null;
        int tmpLen;
        double [] yOut = null;
        double [] yOutTmp = null;
        
        origLen = x.length;
        numfrm = pitchMarks.length-numPeriods;
           
        int yCounter = -1;
        
        for (i=0; i<pitchMarks.length-numPeriods; i++)
        {
            if (i==pitchMarks.length-numPeriods-1)
                bLastInputFrame = true;
            else
                bLastInputFrame = false;

            inputFrameSize =  pitchMarks[i+numPeriods]-pitchMarks[i]+1;
            frmIn = new double[inputFrameSize];
            System.arraycopy(x, pitchMarks[i], frmIn, 0, inputFrameSize);

            currentPeriod = pitchMarks[i+1]-pitchMarks[i]+1;

            if (frmIn!=null) //We have a frame to be processed
            {
                //isVoiced = voicings[i][j];
                isVoiced = SignalProcUtils.getVoicing(frmIn, (int)(audioformat.getSampleRate()), 0.35f);

                try {
                    output = processFrame(frmIn, isVoiced, pitchScales[i], timeScales[i], escale, vscale, bLastInputFrame, currentPeriod, inputFrameSize);
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
    
    //FD-PSOLA on a single concatenation unit
    public double [] processDatagram(Datagram [] datagrams, Datagram rightContext, AudioFormat audioformat, boolean [] voicings, double [] pitchScales, double [] timeScales, boolean bLastDatagram)
    {
        int pitchSpecs = FROM_TARGET;
        //int pitchSpecs = FROM_FILE;
        //int pitchSpecs = FROM_CODE;
        int durationSpecs = FROM_TARGET;
        //int durationSpecs = FROM_FILE;
        //int durationSpecs = FROM_CODE;
        
        int j, k;
        double [] output = null;
        boolean isVoiced = true;
 
        double pscale=1.0; //if pitchSpecs==FROM_CODE flag, this value will be used for pitch scaling
        double tscale=1.0; //if durationSpecs==FROM_CODE flag, this value will be used for duration scaling  
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
            if (pitchSpecs==FROM_FILE || pitchSpecs==FROM_CODE)
            {
                for (j=0; j<pitchScales.length; j++)
                    pitchScales[j] = pscale;
            }

            if (durationSpecs==FROM_FILE || durationSpecs==FROM_CODE)
            {
                for (j=0; j<timeScales.length; j++)
                    timeScales[j] = tscale;
            }
        }  
        
        double firstTScale = timeScales[0];
        tscaleSingle = firstTScale;

        for (j=0; j<timeScales.length; j++)
        {
            if (j!=0 && timeScales[j]!=firstTScale)
            {
                tscaleSingle = -1.0;
                break;
            }
        }          
        
        boolean bLastInputFrame = false;
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

        for (j=0; j<datagrams.length; j++)
        {
            if (j==datagrams.length-1)
            {
                if (rightContext!=null)
                    origLen += datagrams[j].getDuration()+rightContext.getDuration();
                else
                    origLen += datagrams[j].getDuration();
            }
            else
                origLen += datagrams[j].getDuration();

            numfrm++;
        }
           
        int yCounter = -1;

        for (j=0; j<datagrams.length; j++)
        {   
            frmIn = null;
            inputFrameSize = 0;

            /*
            if (j==datagrams.length-1)
                bLastInputFrame = true;
                */
            
            if (bLastDatagram && j==datagrams.length-1)
                bLastInputFrame = true;

            currentPeriod = (int)datagrams[j].getDuration();

            if (j<datagrams.length-1)
            {
                inputFrameSize = (int)datagrams[j].getDuration()+(int)datagrams[j+1].getDuration();
                frmIn = new double[inputFrameSize];

                tmpDatagram[0] = datagrams[j];
                frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                tmpLen = frmTmp.length;
                System.arraycopy(frmTmp, 0, frmIn, 0, tmpLen);

                tmpDatagram[0] = datagrams[j+1];
                frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                System.arraycopy(frmTmp, 0, frmIn, tmpLen, frmTmp.length);
            }
            else
            {
                if (rightContext!=null)
                {
                    inputFrameSize = (int)datagrams[j].getDuration()+(int)rightContext.getDuration();
                    frmIn = new double[inputFrameSize];

                    tmpDatagram[0] = datagrams[j];
                    frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                    tmpLen = frmTmp.length;
                    System.arraycopy(frmTmp, 0, frmIn, 0, tmpLen);

                    tmpDatagram[0] = rightContext;
                    frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                    System.arraycopy(frmTmp, 0, frmIn, tmpLen, frmTmp.length);   
                }
                else
                {
                    inputFrameSize = 2*(int)datagrams[j].getDuration();
                    frmIn = new double[inputFrameSize];

                    Arrays.fill(frmIn, 0.0);

                    tmpDatagram[0] = datagrams[j];
                    frmTmp = new DatagramDoubleDataSource(tmpDatagram).getAllData();
                    tmpLen = frmTmp.length;
                    System.arraycopy(frmTmp, 0, frmIn, 0, tmpLen);
                }
            }

            if (frmIn!=null) //We have a frame to be processed
            {
                //isVoiced = voicings[j];
                isVoiced = SignalProcUtils.getVoicing(frmIn, (int)(audioformat.getSampleRate()), 0.35f);

                try {
                    output = processFrame(frmIn, isVoiced, pitchScales[j], timeScales[j], escale, vscale, bLastInputFrame, currentPeriod, inputFrameSize);
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
        
        return yOut;
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
        
        inputAudio.close();
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
                super.initialise(lpOrder, fs, fftSize, true); //Perform only analysis
                windowIn.applyInline(frm, 0, frmSize); //Windowing
                applyInline(frm, 0, frmSize); //LP analysis
                
                //Expand/Compress the vocal tract spectrum in inverse manner
                inputVT = MathUtils.interpolate(vtSpectrum, newMaxFreq); //Interpolated vocal tract spectrum
                
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
                    
                    py2 = new double[newMaxFreq];
                    
                    for (k=0; k<newMaxFreq; k++)
                    {
                        wInd = (int)Math.floor((k+1)/newVScales[k]+0.5); //Find new indices
                        if (wInd<1)
                            wInd = 1;
                        if (wInd>newMaxFreq)
                            wInd = newMaxFreq;
                        
                        py2[k] = inputVT[wInd-1];
                    }
                    
                    System.arraycopy(py2, 0, inputVT, 0, newMaxFreq);
                }

                //Create output DFT spectrum
                hy = new Complex(newFftSize);
                hy.real = MathUtils.zeros(newFftSize);
                hy.imag = MathUtils.zeros(newFftSize);

                System.arraycopy(this.h.real, 0, hy.real, 0, Math.min(maxFreq, newFftSize));
                System.arraycopy(this.h.imag, 0, hy.imag, 0, Math.min(maxFreq, newFftSize));

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
                        hy.real[j-1] = this.h.real[tmpMul*(tmpFix-j)+tmpAdd-1];
                        hy.imag[j-1] = this.h.imag[tmpMul*(tmpFix-j)+tmpAdd-1];
                    }
                }

                hy.real[newMaxFreq-1] = Math.sqrt(hy.real[newMaxFreq-1]*hy.real[newMaxFreq-1] + hy.imag[newMaxFreq-1]*hy.imag[newMaxFreq-1]);
                hy.imag[newMaxFreq-1] = 0.0;
                
                //Convolution
                for (k=1; k<=newMaxFreq; k++)
                {
                    hy.real[k-1] *= inputVT[k-1];
                    hy.imag[k-1] *= inputVT[k-1];
                }
                
                for (k=newMaxFreq+1; k<=newFftSize; k++)
                {
                    hy.real[k-1] = hy.real[2*newMaxFreq-1-k];
                    hy.imag[k-1] = -hy.imag[2*newMaxFreq-1-k];
                }

                //Convert back to time domain
                //FFT.transform(hy.real, hy.imag, true);
                //hy = FFTArbitraryLength.ifft(hy);
                hy = FFTMixedRadix.ifft(hy);
                
                frmy = new double[newFrmSize];
                System.arraycopy(hy.real, 0, frmy, 0, newFrmSize);
                
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
                
                totalWrittenToFile = origLen;
            } 
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
            if (tmpMax>1.0)
            {
                for (int n=0; n<yOut.length; n++)
                    yOut[n] /= tmpMax;
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
        
        double [] pscales = {0.52};
        double [] tscales = {1.0};
        double [] escales = {1.0};
        double [] vscales = {1.0};
       
        FDPSOLAProcessor fd = new FDPSOLAProcessor(args[0], strPitchFile, strOutputFile, 
                                                    pscales, tscales, escales, vscales);
        
        fd.fdpsolaOnline();
        
    }
}
