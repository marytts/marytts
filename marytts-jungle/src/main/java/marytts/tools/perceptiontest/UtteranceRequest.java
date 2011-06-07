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
package marytts.tools.perceptiontest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.server.MaryProperties;
import marytts.util.MaryUtils;
import marytts.util.data.audio.AppendableSequenceAudioInputStream;

import org.apache.log4j.Logger;


/**
 * This class handles a simple audio requests. 
 * @author sathish
 *
 */
public class UtteranceRequest {
    
    protected AudioFileFormat audioFileFormat;
    protected AppendableSequenceAudioInputStream appendableAudioStream;

    protected int id;
    protected Logger logger;

    protected boolean streamAudio = false;;
    protected boolean abortRequested = false;



    public UtteranceRequest(int id, AudioFileFormat audioFileFormat)
    {
    	this(id, audioFileFormat, false);
    }
    
    public UtteranceRequest(int id, AudioFileFormat audioFileFormat, boolean streamAudio)
    {
        
        this.id = id;
        this.audioFileFormat = audioFileFormat;
        this.streamAudio = streamAudio;
        if (audioFileFormat == null) 
            throw new NullPointerException("audio file format is needed for output type AUDIO");
        this.appendableAudioStream = new AppendableSequenceAudioInputStream(audioFileFormat.getFormat(), null);
        this.logger = MaryUtils.getLogger("R " + id);
    }

    public int getId() {
        return id;
    }
    public AudioFileFormat getAudioFileFormat() {
        return audioFileFormat;
    }

    public AppendableSequenceAudioInputStream getAudio() {
        return appendableAudioStream;
    }

    public boolean getStreamAudio()
    {
    	return streamAudio;
    }
    
    /**
     * Inform this request that any further processing does not make sense.
     */
    public void abort() {
        logger.info("Requesting abort.");
        abortRequested = true;
    }
    
  
    
    /**
     * Process the input data to produce the output data.
     * @see #getOutputData for direct access to the resulting output data
     * @see #writeOutputData for writing the output data to a stream
     */
    public void process(String waveFile) throws Exception {
        
        AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(waveFile));
        
        // Enforce PCM_SIGNED encoding
        if (!inputAudio.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            inputAudio = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, inputAudio);
        }
        
        appendableAudioStream.append(inputAudio);
        
        if (appendableAudioStream != null) appendableAudioStream.doneAppending();
    }

    /**
     * Write the output data to the specified OutputStream.
     */
    public void writeOutputData(OutputStream outputStream) throws Exception {
        
        if (outputStream == null)
            throw new NullPointerException("cannot write to null output stream");
        // Safety net: if the output is not written within a certain amount of
        // time, give up. This prevents our thread from being locked forever if an
        // output deadlock occurs (happened very rarely on Java 1.4.2beta).
        final OutputStream os = outputStream;
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            public void run() {
                logger.warn("Timeout occurred while writing output. Forcefully closing output stream.");
                try {
                    os.close();
                } catch (IOException ioe) {
                    logger.warn(ioe);
                }
            }
        };
        int timeout = MaryProperties.getInteger("modules.timeout", 50000);
        
        timer.schedule(timerTask, timeout);
        try {
            AudioSystem.write(getAudio(), audioFileFormat.getType(), os);
            os.flush();
            os.close();
        } catch (Exception e) {
            timer.cancel();
            throw e;
        }
        timer.cancel();
    }
    

}

