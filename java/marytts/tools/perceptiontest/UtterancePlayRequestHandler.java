/**
 * Copyright 2009 DFKI GmbH.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

import marytts.client.http.Address;
import marytts.modules.synthesis.Voice;
import marytts.server.Request;
import marytts.server.RequestHandler.StreamingOutputPiper;
import marytts.server.RequestHandler.StreamingOutputWriter;

import marytts.server.http.BaseHttpRequestHandler;
import marytts.server.http.MaryHttpServerUtils;
import marytts.util.MaryUtils;

/**
 * This handler class handles audio requests from Http clients  
 * @author sathish
 *
 */
public class UtterancePlayRequestHandler  extends BaseHttpRequestHandler {

    private static int id = 0;
    private DataRequestHandler infoRH;
    
    private static synchronized int getId()
    {
        return id++;
    }
    
    
    private StreamingOutputWriter outputToStream;
    private StreamingOutputPiper streamToPipe;
    private PipedOutputStream pipedOutput;
    private PipedInputStream pipedInput;
    
    public UtterancePlayRequestHandler(DataRequestHandler infoRH)
    {
        super();
        this.infoRH = infoRH;
        outputToStream = null;
        streamToPipe = null;
        pipedOutput = null;
        pipedInput = null;
    }
    
    @Override
    protected void handleClientRequest(String absPath,
            Map<String, String> queryItems, HttpResponse response,
            Address serverAddressAtClient) throws IOException {
        
        /*if (queryItems != null) {
            for (String key : queryItems.keySet()) {
                System.out.println("    "+key+"="+queryItems.get(key));
            }
        }*/
        
        process(serverAddressAtClient, queryItems, response);
    }

    private void process(Address serverAddressAtClient,
            Map<String, String> queryItems, HttpResponse response) {

        boolean streamingAudio = true;
        AudioFileFormat.Type audioFileFormatType = AudioFileFormat.Type.WAVE;
        AudioFormat audioFormat = Voice.AF16000;
        AudioFileFormat audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);

        final UtteranceRequest maryRequest = new UtteranceRequest(getId(), audioFileFormat, streamingAudio);
        // Process the request and send back the data
        boolean ok = true;
        //boolean okTest1 = queryItems.containsKey("EMAIL");
        //boolean okTest2 = queryItems.containsKey("PRESENT_SAMPLE_NUMBER");
        //boolean okTest3 = queryItems.containsKey("PERCEPTION_RESULT");
        
        boolean okTest = queryItems.containsKey("PRESENT_SAMPLE_NUMBER");
        
        //if(okTest1 && okTest2) {
        if(!okTest) {
            String message = "Problem reading input";
            logger.warn(message);
            MaryHttpServerUtils.errorInternalServerError(response, message, new Exception());
            ok = false;
        }
        
        if (ok) {
            int presentSample = (new Integer(queryItems.get("PRESENT_SAMPLE_NUMBER"))).intValue();
            final String waveFile = this.infoRH.getSampleWaveFile(presentSample); 
            
            if (streamingAudio) {
                // Start two separate threads:
                // 1. one thread to process the request;
                new Thread("RH "+maryRequest.getId()) {
                    public void run() 
                    {
                        Logger myLogger = MaryUtils.getLogger(this.getName());
                        try {
                            maryRequest.process(waveFile);
                            myLogger.info("Streaming request processed successfully.");
                        } catch (Throwable t) {
                            myLogger.error("Processing failed.", t);
                        }
                    }
                }.start();
                
                // 2. one thread to take the audio data as it becomes available
                //    and write it into the ProducingNHttpEntity.
                // The second one does not depend on the first one practically,
                // because the AppendableSequenceAudioInputStream returned by
                // maryRequest.getAudio() was already created in the constructor of Request.
                AudioInputStream audio = maryRequest.getAudio();
                assert audio != null : "Streaming audio but no audio stream -- very strange indeed! :-(";
                AudioFileFormat.Type audioType = maryRequest.getAudioFileFormat().getType();
                AudioStreamNHttpEntity entity = new AudioStreamNHttpEntity(maryRequest);
                new Thread(entity, "HTTPWriter "+maryRequest.getId()).start();
                // entity knows its contentType, no need to set explicitly here.
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                return;
            } else { // not streaming audio
                // Process input data to output data
                try {
                    maryRequest.process(waveFile); // this may take some time
                } catch (Throwable e) {
                    String message = "Processing failed.";
                    logger.error(message, e);
                    MaryHttpServerUtils.errorInternalServerError(response, message, e);
                    ok = false;
                }
                if (ok) {
                    // Write output data to client
                    try {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        maryRequest.writeOutputData(outputStream);
                        String contentType;
                        contentType = MaryHttpServerUtils.getMimeType(maryRequest.getAudioFileFormat().getType());
                        MaryHttpServerUtils.toHttpResponse(outputStream.toByteArray(), response, contentType);
                    } catch (Exception e) {
                        String message = "Cannot write output";
                        logger.warn(message, e);
                        MaryHttpServerUtils.errorInternalServerError(response, message, e);
                        ok = false;
                    } 
                }
            }
        }

       
        if (ok)
            logger.info("Request handled successfully.");
        else
            logger.info("Request couldn't be handled successfully.");
        if (MaryUtils.lowMemoryCondition()) {
            logger.info("Low memory condition detected (only " + MaryUtils.availableMemory() + " bytes left). Triggering garbage collection.");
            Runtime.getRuntime().gc();
            logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
        }
        
        
    }

}
