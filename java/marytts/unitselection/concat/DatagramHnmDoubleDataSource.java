/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.unitselection.concat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmPlusTransientsSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.signalproc.window.DynamicTwoHalvesWindow;
import marytts.signalproc.window.Window;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.HnmDatagram;
import marytts.util.MaryUtils;
import marytts.util.io.FileUtils;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class DatagramHnmDoubleDataSource extends DatagramOverlapDoubleDataSource {

    private HntmSynthesizedSignal ss;
    private int outputCounter;
    
    public DatagramHnmDoubleDataSource(Datagram[][] datagrams) 
    {
        super(datagrams, null);
        // TODO Auto-generated constructor stub
        
        HntmSynthesizer s = new HntmSynthesizer();
        //TO DO: These should come from timeline and user choices...
        HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
        HntmSynthesizerParams synthesisParams = new HntmSynthesizerParams();
        BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams();
        int samplingRateInHz = 16000;
        PitchReaderWriter f0 = new PitchReaderWriter("D:\\hnmTimelineTest\\ptc\\arctic_a0001.ptc");
        
        int totalFrm = 0;
        int i, j;
        float originalDurationInSeconds = 0.0f;
        float deltaTimeInSeconds;
        
        for (i=0; i<datagrams.length; i++)
        {
            if (datagrams[i]!=null)
            {
                for (j=0; j<datagrams[i].length; j++)
                {
                    if (datagrams[i][j]!=null)
                    {
                        if (datagrams[i][j] instanceof HnmDatagram)
                        {
                            totalFrm++;
                            deltaTimeInSeconds = SignalProcUtils.sample2time(((HnmDatagram)datagrams[i][j]).getDuration(), samplingRateInHz);
                        }
                        else
                            deltaTimeInSeconds = SignalProcUtils.sample2time(datagrams[i][j].getDuration(), samplingRateInHz);

                        originalDurationInSeconds += deltaTimeInSeconds;
                    }
                }
            } 
        }
        
        HntmSpeechSignal hnmSignal = null;
        hnmSignal = new HntmSpeechSignal(totalFrm, samplingRateInHz, originalDurationInSeconds, (float)f0.header.windowSizeInSeconds, (float)f0.header.skipSizeInSeconds, analysisParams.noiseAnalysisWindowDurationInSeconds, analysisParams.preemphasisCoefNoise);
        //
        
        int frameCount = 0;
        float tAnalysisInSeconds = 0.0f;
        for (i=0; i<datagrams.length; i++)
        {
            if (datagrams[i]!=null)
            {
                for (j=0; j<datagrams[i].length; j++)
                {
                    if (datagrams[i][j]!=null)
                    {
                        if (datagrams[i][j] instanceof HnmDatagram)
                        {
                            if  (frameCount<totalFrm)
                            {
                                hnmSignal.frames[frameCount] = new HntmSpeechFrame(((HnmDatagram)datagrams[i][j]).getFrame());
                                hnmSignal.frames[frameCount].tAnalysisInSeconds = tAnalysisInSeconds;
                                frameCount++;
                            }
                            
                            tAnalysisInSeconds += SignalProcUtils.sample2time(((HnmDatagram)datagrams[i][j]).getDuration(), samplingRateInHz);
                        }
                        else
                            tAnalysisInSeconds += SignalProcUtils.sample2time(datagrams[i][j].getDuration(), samplingRateInHz);
                    }
                }
            }    
        }

        ss = null;
        outputCounter = 0;
        if (totalFrm>0)
        {    
            ss = s.synthesize(hnmSignal, pmodParams, null, analysisParams, synthesisParams);
            //FileUtils.writeTextFile(hnmSignal.getAnalysisTimes(), "d:\\hnmAnalysisTimes1.txt");
            ss.output = MathUtils.multiply(ss.output, 1.0/32768.0);
        }
    }

    /**
     * Attempt to get more data from the input source. If less than this can be read,
     * the possible amount will be read, but canReadMore() will return false afterwards.
     * @param minLength the amount of data to get from the input source
     * @return true if the requested amount could be read, false if none or less data could be read.
     */
    protected boolean readIntoBuffer(int minLength)
    {
        if (bufferSpaceLeft()<minLength) {
            // current buffer cannot hold the data requested;
            // need to make it larger
            increaseBufferSize(minLength+currentlyInBuffer());
        } else if (buf.length-writePos<minLength) {
            compact(); // create a contiguous space for the new data
        }
        // Now we have a buffer that can hold at least minLength new data points
        int readSum = 0;
        // read blocks:
        
        while (readSum < minLength && p < datagrams.length) {
            if (q >= datagrams[p].length) {
                p++;
                q = 0;
            } else {
                Datagram next = datagrams[p][q];
                int length = (int) next.getDuration();
                if (buf.length < writePos + length) {
                    increaseBufferSize(writePos+length);
                }
                
                int read = (int) next.getDuration();
                
                if (outputCounter+read>ss.output.length)
                    read = ss.output.length-outputCounter;

                for (int i=0; i<read; i++) 
                    buf[writePos+i] += ss.output[outputCounter++];
               
                writePos += read;
                readSum += read;
                totalRead += read;
                q++;
            }
        }
        if (dataProcessor != null) {
            dataProcessor.applyInline(buf, writePos-readSum, readSum);
        }
        return readSum >= minLength;
    }
    
    /*
    protected boolean readIntoBuffer(int minLength)
    {
        if (bufferSpaceLeft()<minLength) {
            // current buffer cannot hold the data requested;
            // need to make it larger
            increaseBufferSize(minLength+currentlyInBuffer());
        } else if (buf.length-writePos<minLength) {
            compact(); // create a contiguous space for the new data
        }
        // Now we have a buffer that can hold at least minLength new data points
        int readSum = 0;
        
        // read blocks:
        HntmSynthesizer s = new HntmSynthesizer();
        //TO DO: These should come from timeline and user choices...
        HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
        HntmSynthesizerParams synthesisParams = new HntmSynthesizerParams();
        BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams();
        int samplingRateInHz = 16000;
        PitchReaderWriter f0 = new PitchReaderWriter("D:\\hnmTimelineTest\\ptc\\arctic_a0001.ptc");
        
        int totalFrm = 0;
        int i, j;
        float originalDurationInSeconds = 0.0f;
        
        for (i=0; i<datagrams.length; i++)
        {
            if (datagrams[i]!=null)
            {
                for (j=0; j<datagrams[i].length; j++)
                {
                    if (datagrams[i][j]!=null)
                    {
                        totalFrm++;
                        originalDurationInSeconds += SignalProcUtils.sample2time(((HnmDatagram)datagrams[i][j]).getDuration(), samplingRateInHz);
                    }
                }
            }    
        }
        
        HntmSpeechSignal hnmSignal = null;
        hnmSignal = new HntmSpeechSignal(totalFrm, samplingRateInHz, originalDurationInSeconds, (float)f0.header.windowSizeInSeconds, (float)f0.header.skipSizeInSeconds, analysisParams.noiseAnalysisWindowDurationInSeconds, analysisParams.preemphasisCoefNoise);
        //
        
        int frameCount = 0;
        float tAnalysisInSeconds = 0.0f;
        for (i=0; i<datagrams.length; i++)
        {
            if (datagrams[i]!=null)
            {
                for (j=0; j<datagrams[i].length; j++)
                {
                    if (datagrams[i][j]!=null)
                    {
                        if (frameCount<totalFrm)
                        {
                            hnmSignal.frames[frameCount] = new HntmSpeechFrame(((HnmDatagram)datagrams[i][j]).getFrame());
                            tAnalysisInSeconds += SignalProcUtils.sample2time(((HnmDatagram)datagrams[i][j]).getDuration(), samplingRateInHz);
                            hnmSignal.frames[frameCount].tAnalysisInSeconds = tAnalysisInSeconds;
                        }
                        
                        frameCount++;
                    }
                }
            }    
        }

        HntmSynthesizedSignal ss = null;
        if (totalFrm>0)
        {    
            ss = s.synthesize(hnmSignal, pmodParams, null, analysisParams, synthesisParams);
            FileUtils.writeToTextFile(ss.output, "d:\\ttsOut.txt");
            ss.output = MathUtils.multiply(ss.output, 1.0/32768.0);
            
            if (buf.length < writePos + ss.output.length)
                increaseBufferSize(writePos+ss.output.length);

            int iMax;
            for (i=0, iMax = ss.output.length; i<iMax; i++)
                buf[writePos+i] += ss.output[i];

            writePos += ss.output.length;
            readSum += ss.output.length;
            totalRead += ss.output.length;
        }

        if (dataProcessor != null)
            dataProcessor.applyInline(buf, writePos-readSum, readSum);
        
        return readSum >= minLength;
    }
    */
}
