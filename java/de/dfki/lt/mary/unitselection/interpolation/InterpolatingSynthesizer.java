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

package de.dfki.lt.mary.unitselection.interpolation;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioInputStream;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.modules.synthesis.SynthesisException;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.synthesis.WaveformSynthesizer;
import de.dfki.lt.mary.unitselection.Datagram;
import de.dfki.lt.mary.unitselection.SelectedUnit;
import de.dfki.lt.mary.unitselection.UnitConcatenator;
import de.dfki.lt.mary.unitselection.UnitDatabase;
import de.dfki.lt.mary.unitselection.UnitSelectionSynthesizer;
import de.dfki.lt.mary.unitselection.UnitSelectionVoice;
import de.dfki.lt.mary.unitselection.UnitSelectionVoiceBuilder;
import de.dfki.lt.mary.unitselection.UnitSelector;
import de.dfki.lt.mary.unitselection.concat.BaseUnitConcatenator;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.signalproc.process.FramewiseMerger;
import de.dfki.lt.signalproc.process.LSFInterpolator;
import de.dfki.lt.signalproc.util.AudioDoubleDataSource;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.DoubleDataSource;

/**
 * @author marc
 *
 */
public class InterpolatingSynthesizer implements WaveformSynthesizer
{
    protected Logger logger;
    
    /**
     * 
     */
    public InterpolatingSynthesizer()
    {
    }

    /** 
     * Start up the waveform synthesizer. This must be called once before
     * calling synthesize(). 
     */
    public void startup() throws Exception
    {
        logger = Logger.getLogger("InterpolatingSynthesizer");
        // Register interpolating voice:
        Voice.registerVoice(new InterpolatingVoice(this, "interpolatingvoice"));    
        logger.info("started.");
    }

    
    /**
     * Perform a power-on self test by processing some example input data.
     * @throws Error if the module does not work properly.
     */
    public void powerOnSelfTest() throws Error
    {
    }
    
    /**
     * Synthesize a given part of a MaryXML document. This method is expected
     * to be thread-safe.
     * @param tokensAndBoundaries the part of the MaryXML document to
     * synthesize; a list containing a number of adjacent <t> and <boundary>
     * elements.
     * @return an AudioInputStream in synthesizer-native audio format.
     * @throws IllegalArgumentException if the voice requested for this section
     * is incompatible with this WaveformSynthesizer.
     */
    public AudioInputStream synthesize(List<Element> tokensAndBoundaries, Voice voice)
        throws SynthesisException
    {
        if (tokensAndBoundaries.size() == 0) return null;

        // 1. determine the two voices involved;
        Element first = (Element) tokensAndBoundaries.get(0);
        Element voiceElement = (Element) MaryDomUtils.getAncestor(first, MaryXML.VOICE);
        String name = voiceElement.getAttribute("name");
        // name has the form: voice1 with XY% voice2
        // We trust that InerpolatingVoice.hasName() has done its job to verify that
        // name actually has that form.
        String[] parts = name.split("\\s+");
        assert parts.length == 4;
        assert parts[1].equals("with");
        assert parts[2].endsWith("%");
        int percent = Integer.parseInt(parts[2].substring(0, parts[2].length()-1));
        Voice voice1 = Voice.getVoice(parts[0]);
        assert voice1 != null;
        Voice voice2 = Voice.getVoice(parts[3]);
        assert voice2 != null;

        // 2. do unit selection with each;
        if (!(voice1 instanceof UnitSelectionVoice)) {
            throw new IllegalArgumentException("Voices of type "+voice.getClass().getName()+" not supported!");
        }
        if (!(voice2 instanceof UnitSelectionVoice)) {
            throw new IllegalArgumentException("Voices of type "+voice.getClass().getName()+" not supported!");
        }
        UnitSelectionVoice usv1 = (UnitSelectionVoice) voice1;
        UnitSelectionVoice usv2 = (UnitSelectionVoice) voice2;
        
        UnitSelector unitSel1 = usv1.getUnitSelector();
        List<SelectedUnit> selectedUnits1 = unitSel1.selectUnits(tokensAndBoundaries, voice);
        UnitSelector unitSel2 = usv2.getUnitSelector();
        List<SelectedUnit> selectedUnits2 = unitSel2.selectUnits(tokensAndBoundaries, voice);
        assert selectedUnits1.size() == selectedUnits2.size() : 
            "Unexpected difference in number of units: "+selectedUnits1.size()+" vs. "+selectedUnits2.size();
        int numUnits = selectedUnits1.size();
        
        // 3. do unit concatenation with each, retrieve actual unit durations from list of units;
        UnitConcatenator unitConcatenator1 = usv1.getConcatenator();
        AudioInputStream audio1;
        try {
            audio1 = unitConcatenator1.getAudio(selectedUnits1);
        } catch (IOException ioe) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            for (Iterator selIt=selectedUnits1.iterator(); selIt.hasNext(); )
                pw.println(selIt.next());
            throw new SynthesisException("For voice "+voice1.getName()+", problems generating audio for unit chain: "+sw.toString(), ioe);
        }
        DoubleDataSource audioSource1 = new AudioDoubleDataSource(audio1);
        UnitConcatenator unitConcatenator2 = usv2.getConcatenator();
        AudioInputStream audio2;
        try {
            audio2 = unitConcatenator2.getAudio(selectedUnits2);
        } catch (IOException ioe) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            for (Iterator selIt=selectedUnits2.iterator(); selIt.hasNext(); )
                pw.println(selIt.next());
            throw new SynthesisException("For voice "+voice2.getName()+", problems generating audio for unit chain: "+sw.toString(), ioe);
        }
        DoubleDataSource audioSource2 = new AudioDoubleDataSource(audio2);
        // Retrieve actual durations from list of units:
        int sampleRate1 = (int) usv1.dbAudioFormat().getSampleRate();
        double[] label1 = new double[numUnits];
        double t1 = 0;
        int sampleRate2 = (int) usv2.dbAudioFormat().getSampleRate();
        double[] label2 = new double[numUnits];
        double t2 = 0;
        for (int i=0; i<numUnits; i++) {
            SelectedUnit u1 = selectedUnits1.get(i);
            SelectedUnit u2 = selectedUnits2.get(i);
            BaseUnitConcatenator.UnitData ud1 = (BaseUnitConcatenator.UnitData) u1.getConcatenationData();
            int unitDuration1 = ud1.getUnitDuration();
            if (unitDuration1 < 0) { // was not set by the unit concatenator, have to count ourselves
                unitDuration1 = 0;
                Datagram[] d = ud1.getFrames();
                for (int id=0; id<d.length; id++) {
                    unitDuration1 += d[id].getDuration();
                }
            }
            t1 += unitDuration1 / (float) sampleRate1;
            label1[i] = t1;
            BaseUnitConcatenator.UnitData ud2 = (BaseUnitConcatenator.UnitData) u2.getConcatenationData();
            int unitDuration2 = ud2.getUnitDuration();
            if (unitDuration2 < 0) { // was not set by the unit concatenator, have to count ourselves
                unitDuration2 = 0;
                Datagram[] d = ud2.getFrames();
                for (int id=0; id<d.length; id++) {
                    unitDuration2 += d[id].getDuration();
                }
            }
            t2 += unitDuration2 / (float) sampleRate2;
            label2[i] = t2;
            logger.debug(usv1.getName()+" ["+u1.getTarget()+"] "+label1[i]+" -- "+usv2.getName()+" ["+u2.getTarget()+"] "+label2[i]);
        }
        
        // 4. with these unit durations, run the LSFInterpolator on the two audio streams.
        int frameLength = Integer.getInteger("signalproc.lpcanalysisresynthesis.framelength", 512).intValue();
        int predictionOrder = Integer.getInteger("signalproc.lpcanalysisresynthesis.predictionorder", 20).intValue();
        double r = (double) percent / 100;
        assert r >= 0;
        assert r <= 1;
        FramewiseMerger foas = new FramewiseMerger(audioSource1, frameLength, sampleRate1, new BufferedDoubleDataSource(label1),
                audioSource2, sampleRate2, new BufferedDoubleDataSource(label2), 
                new LSFInterpolator(predictionOrder, r));
        DDSAudioInputStream outputAudio = new DDSAudioInputStream(new BufferedDoubleDataSource(foas), audio1.getFormat());

        return outputAudio;
    }

}
