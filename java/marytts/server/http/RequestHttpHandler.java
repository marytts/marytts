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
package marytts.server.http;

// General Java Classes
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.util.Map;
import java.util.Vector;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.xml.transform.TransformerException;

import marytts.client.http.MaryHtmlForm;
import marytts.client.http.MaryHttpClientUtils;
import marytts.datatypes.MaryDataType;
import marytts.server.RequestHandler.StreamingOutputPiper;
import marytts.server.RequestHandler.StreamingOutputWriter;
import marytts.util.ConversionUtils;
import marytts.util.MaryUtils;
import marytts.util.io.LoggingReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.entity.NByteArrayEntity;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.xml.sax.SAXParseException;


/**
 * A lightweight process handling one Request in a thread of its own.
 * This is to be used when running as a socket server.
 * @author Marc Schr&ouml;der, oytun.turk
 */

public class RequestHttpHandler extends Thread {
    private RequestHttp request;
    private LoggingReader inputReader;
    private Logger logger;
    private HttpResponse response;
    
    private Address serverAddressAtClient;
    private Map<String, String> keyValuePairs;
    private String version;
    private String voices;
    private String dataTypes;
    private String audioFileFormatTypes;
    private String audioEffectHelpTextLineBreak;
    private String defaultAudioEffects;
    private Vector<String> defaultVoiceExampleTexts;
    private String responseID;
    
    private StreamingOutputWriter rw;
    private StreamingOutputPiper rp;
    private PipedOutputStream pos;
    private PipedInputStream pis;
    private boolean isWebBrowserClient;
    private boolean isSecondCall;

    /**
     * Constructor to be used for Socket processing (running as a standalone
     * socket server).  <code>inputReader</code> is a Reader reading from from
     * <code>dataSocket.inputStream()</code>. Passing this on is necessary
     * because the mary server does a buffered read on that input stream, and
     * without passing that buffered reader on, data gets lost.
     */
    public RequestHttpHandler(RequestHttp request, 
                              HttpResponse response,
                              Reader inputReader,
                              Address serverAddressAtClient,
                              Map<String, String> keyValuePairs,
                              String version,
                              String voices,
                              String dataTypes,
                              String audioFileFormatTypes,
                              String audioEffectHelpTextLineBreak,
                              String defaultAudioEffects,
                              Vector<String> defaultVoiceExampleTexts,
                              String responseID,
                              boolean isSecondCall
                              ) 
    {
        if (request == null)
            throw new NullPointerException("Cannot handle null request");
        this.request = request;
        
        this.response = response;

        this.setName("RH " + request.getId());
        logger = Logger.getLogger(this.getName());
        this.inputReader = new LoggingReader(inputReader, logger);
        
        this.serverAddressAtClient = serverAddressAtClient;
        this.keyValuePairs = keyValuePairs;
        this.version = version;
        this.voices = voices;
        this.dataTypes = dataTypes;
        this.audioFileFormatTypes = audioFileFormatTypes;
        this.audioEffectHelpTextLineBreak = audioEffectHelpTextLineBreak;
        this.defaultAudioEffects = defaultAudioEffects;
        this.defaultVoiceExampleTexts = defaultVoiceExampleTexts;
        this.responseID = responseID;
        
        isWebBrowserClient = false;
        if (keyValuePairs!=null)
        {
            String tmpVal = keyValuePairs.get("WEB_BROWSER_CLIENT");
            if (tmpVal!=null && tmpVal.compareTo("true")==0)
                isWebBrowserClient = true;
        }
        
        this.isSecondCall = isSecondCall;
        
        rw = null;
        rp = null;
        pos = null;
        pis = null;
    }

    /**
     * Perform the actual processing by calling the appropriate methods
     * of the associated <code>Request</code> object.
     * <p>
     * Note that while different request handlers run as different threads,
     * they all use the same module objects. How a given module deals with
     * several requests simultaneously is its own problem, the simplest
     * solution being a synchronized <code>process()</code> method.
     *
     * @see Request
     * @see marytts.modules.MaryModule
     * @see marytts.modules.ExternalModule
     * @see marytts.modules.InternalModule
     */
    public void run() {
        boolean ok = true;
        // tasks:
        // * read the input according to its type
        // * determine which modules are needed
        // * in turn, write to and read from each module according to data type
        // * write output according to its type

        try {
            request.readInputData(inputReader);
        } catch (Exception e) {
            String message = "Problem reading input";
            logger.warn(message, e);
            ok = false;
        }
        
        boolean streamingOutput = false;

        // Process input data to output data
        if (ok)
        {
            try 
            {
                if (request.getOutputType().equals(MaryDataType.get("AUDIO")) && request.getStreamAudio()) 
                {
                    streamingOutput = true;

                    pos = new PipedOutputStream();
                    pis = new PipedInputStream(pos);
                    rw = new StreamingOutputWriter(request, pos);
                    
                    //Pipe to an input stream
                    if (!isWebBrowserClient || isSecondCall) //Non-web browser clients or second call from web-browser client
                    {
                        //rp = new StreamingOutputPiper(pis, "d:\\a.txt"); //Pipe to a text file
                        //rp = new StreamingOutputPiper(pis, new File("d:\\a.wav")); //Pipe to a binary file
                        
                        //rp = new StreamingOutputPiper(pis, response, "text/plain"); //Pipe to an input stream in text/plain mode
                        rp = new StreamingOutputPiper(pis, response, "audio/" + request.getAudioFileFormat().getType().toString()); 
                        rw.start();
                        rp.start();
                        request.process();
                    }
                }
                else
                    request.process();
            } 
            catch (Throwable e) 
            {
                String message = "Processing failed.";
                logger.error(message, e);
                ok = false;
            }
        }

        // Write output
        String synthesisStatus = "";
        MaryHtmlForm htmlForm = null;
        MaryWebHttpClientHandler webHttpClient = null;
        ByteArrayOutputStream outputStream = null;
        
        if (isWebBrowserClient)
        {
            webHttpClient = new MaryWebHttpClientHandler();
            
            try {
                htmlForm = new MaryHtmlForm(serverAddressAtClient,
                        keyValuePairs,
                        version,
                        voices,
                        dataTypes,
                        audioFileFormatTypes,
                        audioEffectHelpTextLineBreak,
                        defaultAudioEffects,
                        defaultVoiceExampleTexts);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (ok) 
        {  
            if (!streamingOutput) //Non-streaming output
            {
                if (!isWebBrowserClient) //Non-streaming output sent to non-web-browser client
                {
                    try 
                    {
                        request.writeNonstreamingOutputData(response); 
                        synthesisStatus = "DONE";
                    } 
                    catch (Exception e) 
                    {
                        String message = "Cannot write output, client seems to have disconnected.";
                        logger.warn(message, e);
                        ok = false;
                        synthesisStatus = "FAILED";
                    }
                }
                else //Non-streaming output sent to web-browser client 
                {
                    outputStream = new ByteArrayOutputStream();
                    try 
                    {
                        request.writeOutputData(outputStream);
                        synthesisStatus = "DONE";
                    } 
                    catch (Exception e) 
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        synthesisStatus = "FAILED";
                    }
                }
            } 
            else //Streaming output
            {
                if (!isWebBrowserClient) //Streaming output to non-web-browser client
                {
                    try 
                    {
                        rp.join();
                        synthesisStatus = "DONE";
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        synthesisStatus = "FAILED";
                    }
                }
                else //Streaming output to web-browser client
                {
                    if (isSecondCall)
                        synthesisStatus = "DONE";
                    else //Second call is required, set synthesis status to pending
                        synthesisStatus = "PENDING";
                }
            }
        }   
        
        keyValuePairs.put("SYNTHESIS_OUTPUT", synthesisStatus);
        
        if (isSecondCall) //Return if this is the second call of synthesis request (the output already written to pis
            return;
        else if (isWebBrowserClient) //Handle html page output for web browser clients
        {
            if (htmlForm!=null)
            {
                if (!request.getOutputType().equals(MaryDataType.get("AUDIO"))) //Non-audio output to web browser client
                {
                    if (outputStream!=null)
                        htmlForm.outputText = ConversionUtils.toString(outputStream.toByteArray());
                    else
                        htmlForm.outputText = "Cannot convert this type of input to selected output...";
                }
                else //Initiate audio output request in web browser client for second request
                    htmlForm.outputAudioResponseID = String.valueOf(responseID) + "." + request.getAudioFileFormat().getType().getExtension();

                try {
                    webHttpClient.toHttpResponse(htmlForm, response); //Send the html page
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        if (ok)
            logger.info("Request handled successfully.");
        else
            logger.info("Request couldn't be handled successfully.");
        if (MaryUtils.lowMemoryCondition()) 
        {
            logger.info("Low memory condition detected (only " + MaryUtils.availableMemory() + " bytes left). Triggering garbage collection.");
            Runtime.getRuntime().gc();
            logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
        }

    } // run()
}
