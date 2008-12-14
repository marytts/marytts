/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.nonverbal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.soap.Node;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.SynthesisException;
import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.server.MaryProperties;
import marytts.signalproc.effects.EffectsApplier;
import marytts.unitselection.concat.DatagramDoubleDataSource;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.TimelineReader;
import marytts.unitselection.data.Unit;
import marytts.unitselection.data.UnitFileReader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.DoubleDataSource;
import marytts.util.data.audio.AudioPlayer;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.jsresources.AppendableSequenceAudioInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;


/**
 * The backchannel synthesis module.
 *
 * @author Sathish Pammi
 */

public class BackchannelSynthesizer {
    
    TimelineReader audioTimeline;
    BackchannelUnitFileReader unitFileReader;
    int samplingRate;
    
    public BackchannelSynthesizer(Voice voice){
        try{
            String unitFileName = MaryProperties.getFilename("voice."+voice.getName()+".backchannel.unitfile");
            String timelineFile = MaryProperties.getFilename("voice."+voice.getName()+".backchannel.timeline");
            this.unitFileReader = new BackchannelUnitFileReader(unitFileName);
            this.samplingRate   = unitFileReader.getSampleRate();
            this.audioTimeline  = new TimelineReader(timelineFile);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
            
    public AudioInputStream synthesize(Voice voice, AudioFileFormat aft, Element domElement) throws Exception{
        
        if(!voice.hasBackchannelSupport()) return null;
        int numberOfBackChannels = unitFileReader.getNumberOfUnits();
        int min = 0;
        int max = numberOfBackChannels - 1;
        int rawRandomNumber = (int) (Math.random() * (max - min + 1) ) + min;
        BackchannelUnit bUnit = unitFileReader.getUnit(rawRandomNumber);
        long start = bUnit.getStart();
        int duration  = bUnit.getDuration();
        Datagram[] frames = audioTimeline.getDatagrams(start, duration); 
        assert frames != null : "Cannot generate audio from null frames";
        
        Unit[] units = bUnit.getUnits();
        String[] unitNames = bUnit.getUnitNames();
        long endTime = 0l;
        for(int i=0;i<units.length;i++){
            int unitDuration = units[i].getDuration() * 1000 / samplingRate;
            endTime += unitDuration;
            Element element = MaryXML.createElement(domElement.getOwnerDocument(), MaryXML.PHONE);
            element.setAttribute("d", ""+unitDuration);
            element.setAttribute("end", ""+endTime);
            element.setAttribute("p", unitNames[i]);
            domElement.appendChild(element);
        }
        
        // Generate audio from frames
        LinkedList<Datagram> datagrams = new LinkedList<Datagram>();
        datagrams.addAll(Arrays.asList(frames));
        DoubleDataSource audioSource = new DatagramDoubleDataSource(datagrams);
        return (new DDSAudioInputStream(new BufferedDoubleDataSource(audioSource), aft.getFormat()));
    }

}
