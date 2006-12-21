/**
 * Portions Copyright 2005, Communicative Machines
 * Portions Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.client.air;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;

import com.cmlabs.air.DataSample;
import com.cmlabs.air.JavaAIRPlug;
import com.cmlabs.air.Message;
import com.cmlabs.air.Time;
import com.cmlabs.air.Utils;

import de.dfki.lt.mary.client.MaryClient;

/**
 * A PsyClone / OpenAIR module processing SSML
 * (Speech Synthesis Markup Language) 
 * input and generating audio output.
 * @author Marc Schröder
 *
 */
public class MarySpeechPsydule
{
    protected String WHITEBOARD;
    protected String INPUTTYPE;
    protected String AUDIOOUTPUTTYPE;
    protected String DEFAULTVOICE;
    protected String name;

    protected JavaAIRPlug plug;
    protected MaryClient mary;
    
    public MarySpeechPsydule(String airhost, int airport) throws Exception
    {
        initialize();
        mary = new MaryClient();
        plug = new JavaAIRPlug(name, airhost, airport);
        if (!plug.init()) {
            System.out.println("Could not connect to the Server on " + airhost +
                    " on port " + airport + "...");
            System.exit(0);
        }

        System.out.println("Connected to the AIR Server on " + airhost +
                ":" + airport);

        if (!plug.openTwoWayConnectionTo(WHITEBOARD)) {
            System.out.println("Could not open callback connection to "+WHITEBOARD+"...");
        }

        String xml = "<module name=\""+name+"\"><trigger from=\""+WHITEBOARD+"\" type=\""+INPUTTYPE+"\" /></module>";

        if (!plug.sendRegistration(xml)) {
            System.out.println("Could not register for messages of type "+INPUTTYPE+"...");
        } else {
            System.out.println("Listening on whiteboard "+WHITEBOARD+" for messages of type "+INPUTTYPE+"...");
        }
    }
    
    protected void initialize() throws Exception
    {
        name = "MarySpeechPsydule";
        WHITEBOARD = System.getProperty("mary.psyclone.whiteboard", "WB1");
        INPUTTYPE = "Mary.Input.SSML";
        AUDIOOUTPUTTYPE = "Mary.Output.Audio";
        DEFAULTVOICE = System.getProperty("voice.default", "kevin16");
    }


    public void listenAndProcess()
    {
        Message message;
        while (true) {
            if ( (message = plug.waitForNewMessage(100)) != null) {
                Time start = new Time();
                System.out.println(start.printTime() + ":" + name
                               + ": received wakeup message from " +
                               message.from);
                try {
                    String input = message.getContent();
                    processInput(input);
                    Time end = new Time();
                    System.out.println("Processing took "+end.difference(start)+" ms");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
    
    protected void processInput(String input) throws Exception
    {
        byte[] audio = ssml2audio(input);
        System.out.println("Audio: "+audio.length+" bytes");
        // post audio to whiteboard
        DataSample audioData = new DataSample();
        //audioData.fromBinaryBuffer(0, audio, 0, audio.length);
        audioData.data = audio;
        audioData.size = audio.length;
        Message audioMessage = new Message(name, WHITEBOARD, AUDIOOUTPUTTYPE, audioData);
        plug.postMessage(WHITEBOARD, audioMessage, "");

    }
    
    
    private byte[] ssml2audio(String acoustparams) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mary.process(acoustparams, "SSML", "AUDIO", "WAVE", DEFAULTVOICE, baos);
        return baos.toByteArray();
    }
    
    
    
    
    
    

    
    
    
    


    /**
     * A standalone program which routes messages between a 
     * PsyClone/OpenAIR server and a MARY server.
     * The server host and port for both servers can be given as
     * system properties: Mary server: "server.host" 
     * (default: cling.dfki.uni-sb.de) and "server.port" (default: 59125);
     * OpenAIR server: "airserver.host" (default: localhost) and 
     * "airserver.port" (default: 10000).
     * This server will listen for incoming SSML files, and
     * generate Audio data from it.
     * In the current version, the name of the OpenAIR whiteboard
     * can be set via the system property "mary.psyclone.whiteboard"
     * (default: "WB1"); the input data type is
     * "Mary.Input.SSML"; and the audio output is
     * "Greta.Data.Audio". 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception 
    {

        String airhost = System.getProperty("airserver.host", "localhost");
        int airport = Integer.getInteger("airserver.port", 10000).intValue();

        MarySpeechPsydule reader = new MarySpeechPsydule(airhost, airport);
        reader.listenAndProcess();
    }


}
