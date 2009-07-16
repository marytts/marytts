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

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioInputStream;

import marytts.signalproc.adaptation.prosody.BasicProsodyModifierParams;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmAnalyzerParams;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechFrame;
import marytts.signalproc.sinusoidal.hntm.analysis.HntmSpeechSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizedSignal;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizer;
import marytts.signalproc.sinusoidal.hntm.synthesis.HntmSynthesizerParams;
import marytts.unitselection.concat.BaseUnitConcatenator.UnitData;
import marytts.unitselection.concat.OverlapUnitConcatenator.OverlapUnitData;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.HnmDatagram;
import marytts.unitselection.data.Unit;
import marytts.unitselection.select.SelectedUnit;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.math.MathUtils;
import marytts.util.signal.SignalProcUtils;

/**
 * @author oytun.turk
 *
 */
public class HnmUnitConcatenator extends OverlapUnitConcatenator {

    public HnmUnitConcatenator()
    {
        super();
    }
    
    /**
     * Get the raw audio material for each unit from the timeline.
     * @param units
     */
    protected void getDatagramsFromTimeline(List<SelectedUnit> units) throws IOException
    {
        for (SelectedUnit unit : units) 
        {
            assert !unit.getUnit().isEdgeUnit() : "We should never have selected any edge units!";
            UnitData unitData = new UnitData();
            int nSamples = 0;
            int unitSize = unitToTimeline(unit.getUnit().getDuration()); // convert to timeline samples
            long unitStart = unitToTimeline(unit.getUnit().getStart()); // convert to timeline samples
            System.out.println(unitStart/((float)timeline.getSampleRate()));
            //System.out.println("Unit size "+unitSize+", pitchmarksInUnit "+pitchmarksInUnit);
            Datagram[] datagrams = timeline.getDatagrams(unitStart,(long)unitSize);
          
            unitData.setFrames(datagrams);
            unit.setConcatenationData(unitData);
        }
    }
    
    /**
     * Generate audio to match the target pitchmarks as closely as possible.
     * @param units
     * @return
     */
    protected AudioInputStream generateAudioStream(List<SelectedUnit> units)
    {
        int len = units.size();
        LinkedList<Datagram> datagrams = new LinkedList<Datagram>();
        
        int i, j;
        for (i=0; i<len; i++) 
        {
            SelectedUnit unit = units.get(i);
            UnitData unitData = (UnitData)unit.getConcatenationData();
            assert unitData != null : "Should not have null unitdata here";
            Datagram[] frames = unitData.getFrames();            
            assert frames != null : "Cannot generate audio from null frames";

            for (j=0; j<frames.length; j++)
                datagrams.add(frames[j]);
        }
        
         // Generate audio from frames
        //DoubleDataSource audioSource = new DatagramHnmDoubleDataSource(datagrams);
        //return new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), audioformat);
        
        BufferedDoubleDataSource audioSource = synthesize(datagrams);
        return new DDSAudioInputStream(audioSource, audioformat);
    }
    
    protected BufferedDoubleDataSource synthesize(LinkedList<Datagram> datagrams)
    {
        HntmSynthesizer s = new HntmSynthesizer();
        //TO DO: These should come from timeline and user choices...
        HntmAnalyzerParams analysisParams = new HntmAnalyzerParams();
        HntmSynthesizerParams synthesisParams = new HntmSynthesizerParams();
        BasicProsodyModifierParams pmodParams = new BasicProsodyModifierParams();
        int samplingRateInHz = 16000;
        
        int totalFrm = 0;
        int i, j;
        float originalDurationInSeconds = 0.0f;
        float deltaTimeInSeconds;
        
        for (i=0; i<datagrams.size(); i++)
        {
            if (datagrams.get(i)!=null)
            {
                if (datagrams.get(i) instanceof HnmDatagram)
                {
                    totalFrm++;
                    deltaTimeInSeconds = SignalProcUtils.sample2time(((HnmDatagram)datagrams.get(i)).getDuration(), samplingRateInHz);
                }
                else
                    deltaTimeInSeconds = SignalProcUtils.sample2time(datagrams.get(i).getDuration(), samplingRateInHz);

                originalDurationInSeconds += deltaTimeInSeconds;
            } 
        }
        
        HntmSpeechSignal hnmSignal = null;
        hnmSignal = new HntmSpeechSignal(totalFrm, samplingRateInHz, originalDurationInSeconds);
        //
        
        int frameCount = 0;
        float tAnalysisInSeconds = 0.0f;
        for (i=0; i<datagrams.size(); i++)
        {
            if (datagrams.get(i)!=null)
            {
                if (datagrams.get(i) instanceof HnmDatagram)
                {
                    tAnalysisInSeconds += SignalProcUtils.sample2time(((HnmDatagram)datagrams.get(i)).getDuration(), samplingRateInHz);

                    if  (frameCount<totalFrm)
                    {
                        hnmSignal.frames[frameCount] = new HntmSpeechFrame(((HnmDatagram)datagrams.get(i)).getFrame());
                        hnmSignal.frames[frameCount].tAnalysisInSeconds = tAnalysisInSeconds;
                        frameCount++;
                    }
                }
                else
                    tAnalysisInSeconds += SignalProcUtils.sample2time(datagrams.get(i).getDuration(), samplingRateInHz);
            }    
        }

        HntmSynthesizedSignal ss = null;
        if (totalFrm>0)
        {    
            ss = s.synthesize(hnmSignal, pmodParams, null, analysisParams, synthesisParams);
            //FileUtils.writeTextFile(hnmSignal.getAnalysisTimes(), "d:\\hnmAnalysisTimes1.txt");
            //FileUtils.writeTextFile(ss.output, "d:\\output.txt");
            if (ss.output!=null)
                ss.output = MathUtils.multiply(ss.output, 1.0/32768.0);
        }
        
        if (ss!=null && ss.output!=null)
            return new BufferedDoubleDataSource(ss.output);
        else
            return null;
    }
}
