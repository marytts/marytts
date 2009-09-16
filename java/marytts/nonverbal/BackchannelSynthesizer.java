/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
        int backchannelNumber  = 0;
        
        if(domElement.hasAttribute("variant")){
            backchannelNumber  = Integer.parseInt(domElement.getAttribute("variant"));
        }
        if(backchannelNumber >= numberOfBackChannels){
            backchannelNumber = 0;
        }
       
        BackchannelUnit bUnit = unitFileReader.getUnit(backchannelNumber);
        long start = bUnit.startTime;
        int duration  = bUnit.duration;
        Datagram[] frames = audioTimeline.getDatagrams(start, duration); 
        assert frames != null : "Cannot generate audio from null frames";
        
        Unit[] units = bUnit.getUnits();
        String[] unitNames = bUnit.getUnitNames();
        long endTime = 0l;
        for(int i=0;i<units.length;i++){
            int unitDuration = units[i].duration * 1000 / samplingRate;
            endTime += unitDuration;
            Element element = MaryXML.createElement(domElement.getOwnerDocument(), MaryXML.PHONE);
            element.setAttribute("d", Integer.toString(unitDuration));
            element.setAttribute("end", Long.toString(endTime));
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

