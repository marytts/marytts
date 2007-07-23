/*
 * Copyright (C) 2005 DFKI GmbH. All rights reserved.
 */
package de.dfki.lt.signalproc.process;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat.Encoding;

import de.dfki.lt.signalproc.display.FunctionGraph;
import de.dfki.lt.signalproc.display.SignalGraph;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BaseDoubleDataSource;
import de.dfki.lt.signalproc.util.BlockwiseDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;
import de.dfki.lt.signalproc.util.ESTTextfileDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;
import de.dfki.lt.signalproc.util.SequenceDoubleDataSource;
import de.dfki.lt.signalproc.window.DynamicTwoHalvesWindow;
import de.dfki.lt.signalproc.window.DynamicWindow;
import de.dfki.lt.signalproc.window.Window;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class PitchFrameOverlapAddSource extends FrameOverlapAddSource
{
    /**
     * Dummy constructor for subclasses that will call initialise() separately.
     *
     */
    protected PitchFrameOverlapAddSource()
    {
        
    }

    /**
     * @param inputSource
     * @param pitchmarks
     * @param pitchFactor
     * @param samplingRate
     */
    public PitchFrameOverlapAddSource(DoubleDataSource inputSource, DoubleDataSource pitchmarks,
            int samplingRate)
    {
        initialise(inputSource, pitchmarks, samplingRate, null);
    }

    /**
     * @param inputSource
     * @param pitchmarks
     * @param pitchFactor
     * @param samplingRate
     * @param processor an optional data processor, applied to each frame.
     */
    public PitchFrameOverlapAddSource(DoubleDataSource inputSource, DoubleDataSource pitchmarks,
            int samplingRate, InlineDataProcessor processor)
    {
        initialise(inputSource, pitchmarks, samplingRate, processor);
    }

    protected void initialise(DoubleDataSource inputSource, DoubleDataSource pitchmarks, 
            int samplingRate, InlineDataProcessor processor)
    {
        InlineDataProcessor analysisWindow = new DynamicTwoHalvesWindow(Window.HANN);
        // Overlap-add a properly windowed first period by hand:
    	// Read out the first pitchmark:
    	double firstPitchmark = pitchmarks.getData(1)[0];
    	assert firstPitchmark > 0;
    	pitchmarks = new SequenceDoubleDataSource(new DoubleDataSource[] {new BufferedDoubleDataSource(new double[]{firstPitchmark}), pitchmarks});
    	int firstPeriodLength = (int) (firstPitchmark*samplingRate);
    	double[] firstPeriod = new double[firstPeriodLength];
        inputSource.getData(firstPeriod, 0, firstPeriodLength);
        inputSource = new SequenceDoubleDataSource(new DoubleDataSource[] {new BufferedDoubleDataSource(firstPeriod), inputSource});
        this.memory = new double[2*firstPeriodLength];
        System.arraycopy(firstPeriod, 0, memory, firstPeriodLength, firstPeriodLength);
        analysisWindow.applyInline(memory, 0, memory.length);  //windowing applied here
        if (processor != null)
            processor.applyInline(memory, 0, memory.length); //windoed signal processed here
        // Shift the data left in memory:
        System.arraycopy(memory, firstPeriodLength, memory, 0, firstPeriodLength);
        Arrays.fill(memory, firstPeriodLength, memory.length, 0);
    	this.frameProvider = new PitchFrameProvider(inputSource, pitchmarks, analysisWindow, samplingRate, 2, 1);
        this.processor = processor;
    }
    
    /**
     * Get the next frame of input data. This method is called by prepareBlock()
     * when preparing the output data to be read. This implementation
     * simply reads the data from the frameProvider.
     * @return
     */
    protected double[] getNextFrame()
    {
        return frameProvider.getNextFrame();
    }

    /**
     * Output blocksize -- here, this is the same as the input frame shift.
     */
    protected int getBlockSize()
    {
        return frameProvider.getFrameShiftSamples();
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        /*
    	double[] data = new double[32000]; // 2 seconds at 16000 Hz sampling rate
    	AudioFormat audioFormat = new AudioFormat(Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, true);
		// a 100 Hz tone with overtones at 200, 300, and 400 Hz
		int t0 = (int) audioFormat.getSampleRate() / 100;
    	for (int i=0; i<data.length; i++) {
    		data[i] = 10000*Math.sin(2*Math.PI*i/t0) + 5000*Math.sin(2*Math.PI*2*i/t0) + 2500*Math.sin(2*Math.PI*3*i/t0) + 1250*Math.sin(2*Math.PI*4*i/t0);
    	}
    	// So the pitchmarks are every t0, i.e. there are data.length / t0 + 1 of them:
    	double[] pitchmarks = new double[data.length / t0];
    	for (int i=0; i<pitchmarks.length; i++) {
    		pitchmarks[i] = (i+1)*t0/audioFormat.getSampleRate();
    	}
        System.out.println("signal length: "+data.length);
        FunctionGraph signalGraph = new SignalGraph(data, (int) audioFormat.getSampleRate());
        signalGraph.showInJFrame("signal", true, true);
        */
        /*
    	PitchFrameOverlapAddSource ola = new PitchFrameOverlapAddSource(new BufferedDoubleDataSource(data), new BufferedDoubleDataSource(pitchmarks), (int) audioFormat.getSampleRate());
    	double[] olaResult = ola.getAllData();
        System.out.println("olaResult length: "+olaResult.length);
        FunctionGraph olaGraph = new SignalGraph(olaResult, (int) audioFormat.getSampleRate());
        olaGraph.showInJFrame("ola", true, true);
    	*/
        /*
        // Now cut the signal into lots of pieces (10 periods each), and process them separately:
        List pieces = new ArrayList();
        int step = 10*t0;
    	double[] piece = new double[step];
        double[] piecePitchmarks = new double[10];
        System.arraycopy(pitchmarks, 0, piecePitchmarks, 0, 10);
        for (int i=0; i+step<=data.length; i+=step) {
        	System.arraycopy(data, i, piece, 0, step);
            PitchFrameOverlapAddSource pieceSource = new PitchFrameOverlapAddSource(new BufferedDoubleDataSource(piece), new BufferedDoubleDataSource(piecePitchmarks), (int) audioFormat.getSampleRate());
            pieces.add(pieceSource);
        }
        SequenceDoubleDataSource sdds = new SequenceDoubleDataSource(pieces);
        double[] piecesResult = sdds.getAllData();
        FunctionGraph piecesGraph = new SignalGraph(piecesResult, (int) audioFormat.getSampleRate());
        piecesGraph.showInJFrame("pieces", true, true);
        */
        
        
        File audioFile = new File(args[0]);
        File pitchmarkFile = new File(args[1]);
        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFile);
        int samplingRate = (int) ais.getFormat().getSampleRate();
        DoubleDataSource signal = new AudioDoubleDataSource(ais);
        double[] origSignal = signal.getAllData();
        signal = new BufferedDoubleDataSource(origSignal);
        DoubleDataSource pitchmarks = new ESTTextfileDoubleDataSource(new FileReader(pitchmarkFile));
        double[] origPitchmarks = pitchmarks.getAllData();
        double audioDuration = origSignal.length/(double)samplingRate;
        if (origPitchmarks[origPitchmarks.length-1] < audioDuration) {
            System.out.println("correcting last pitchmark to total audio duration: "+audioDuration);
            origPitchmarks[origPitchmarks.length-1] = audioDuration;
        }
        pitchmarks = new BufferedDoubleDataSource(origPitchmarks);
        InlineDataProcessor processor = new LPCAnalysisResynthesis(16);
        PitchFrameOverlapAddSource ola = new PitchFrameOverlapAddSource(signal, pitchmarks, samplingRate, processor);
        double[] result = ola.getAllData();
        FunctionGraph signalGraph = new SignalGraph(origSignal, samplingRate);
        signalGraph.showInJFrame("signal", true, true);
        FunctionGraph resultGraph = new SignalGraph(result, samplingRate);
        resultGraph.showInJFrame("result", true, true);
        System.err.println("Signal has length " + origSignal.length + ", result " + result.length);
        if (origSignal.length == result.length){
            double err = MathUtils.sumSquaredError(origSignal, result);
            System.err.println("Sum squared error: " + err);
            double[] difference = MathUtils.substract(origSignal, result);
            FunctionGraph diffGraph = new SignalGraph(difference, samplingRate);
            diffGraph.showInJFrame("difference", true, true);
        }
    }

}
