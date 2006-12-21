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
 * A PsyClone / OpenAIR module processing BML ("Behavior Markup Language") 
 * input and generating audio and enriched-BML output.
 * This can be used as part of a "BML realiser" component,
 * e.g. together with the Greta agent.
 * @author Marc Schröder
 *
 */
public class BMLSpeechPsydule extends MarySpeechPsydule
{
    protected String BMLOUTPUTTYPE = "Greta.Data.EnrichedBMLCode";
    private Templates bml2ssmlStylesheet;
    private Templates mergeMaryxmlIntoBMLStylesheet;
    
    public BMLSpeechPsydule(String airhost, int airport) throws Exception
    {
        super(airhost, airport);
    }

    protected void initialize() throws Exception 
    {
        name = "BMLSpeechPsydule";
        WHITEBOARD = "Greta.Whiteboard";
        INPUTTYPE = "Greta.Data.BMLCode";
        AUDIOOUTPUTTYPE = "Greta.Data.Audio";
        DEFAULTVOICE = System.getProperty("voice.default", "kevin16");
        BMLOUTPUTTYPE = "Greta.Data.EnrichedBMLCode";

        TransformerFactory tFactory = TransformerFactory.newInstance();

        tFactory.setURIResolver(new URIResolver() {
            public Source resolve(String href, String base) {
                if (href.endsWith("ssml-to-mary.xsl")) {
                    return new StreamSource(this.getClass().getResourceAsStream("ssml-to-mary.xsl"));
                } else {
                    return null;
                }
            }
        });
        StreamSource stylesheetStream =
            new StreamSource(this.getClass().getResourceAsStream("bml-to-ssml.xsl"));
        bml2ssmlStylesheet = tFactory.newTemplates(stylesheetStream);
        stylesheetStream = new StreamSource(this.getClass().getResourceAsStream("merge-maryxml-into-bml.xsl"));
        mergeMaryxmlIntoBMLStylesheet = tFactory.newTemplates(stylesheetStream);
    }
    

    
    protected void processInput(String input) throws Exception
    {
        System.out.println(new Time().printTime()+" - started processing");
        String bml = input;
        String ssml = bml2ssml(bml);
        System.out.println(new Time().printTime()+" - converted to SSML");
        String acoustparams = ssml2acoustparams(ssml);
        System.out.println(new Time().printTime()+" - created ACOUSTPARAMS");
        String enrichedBML = mergeBmlAndAcoustparams(bml, acoustparams);
        System.out.println(new Time().printTime()+" - merged phoneme times into BML");
        // post enriched BML to whiteboard
        plug.postMessage(WHITEBOARD, BMLOUTPUTTYPE, enrichedBML, "", "");
        System.out.println(new Time().printTime()+" - posted enriched BML");
        byte[] audio = acoustparams2audio(acoustparams);
        System.out.println(new Time().printTime()+" - created audio ("+audio.length+" bytes)");
        // post audio to whiteboard
        DataSample audioData = new DataSample();
        //audioData.fromBinaryBuffer(0, audio, 0, audio.length);
        audioData.data = audio;
        audioData.size = audio.length;
        Message audioMessage = new Message(name, WHITEBOARD, AUDIOOUTPUTTYPE, audioData);
        plug.postMessage(WHITEBOARD, audioMessage, "");
        System.out.println(new Time().printTime()+" - posted audio");

    }

    /**
     * Extract the SSML section from the BML data.
     * @param bml string representation of a full BML document
     * containing SSML.
     * @return string representation of a standalone SSML document
     * corresponding to the embedded SSML tags
     */
    private String bml2ssml(String bml) throws Exception
    {
        // Extract from BML string the SSML string:
        StreamSource bmlSource = new StreamSource(new StringReader(bml));
        StringWriter ssmlWriter = new StringWriter();
        StreamResult ssmlResult = new StreamResult(ssmlWriter);
        // Transformer is not guaranteed to be thread-safe -- therefore, we
        // need one per thread.
        Transformer transformer = bml2ssmlStylesheet.newTransformer();
        transformer.transform(bmlSource, ssmlResult);
        return ssmlWriter.toString();

    }
    
    private String mergeBmlAndAcoustparams(String bml, final String acoustparams) throws Exception
    {
        StreamSource bmlSource = new StreamSource(new StringReader(bml));
        StringWriter mergedWriter = new StringWriter();
        StreamResult mergedResult = new StreamResult(mergedWriter);
        // Transformer is not guaranteed to be thread-safe -- therefore, we
        // need one per thread.
        Transformer mergingTransformer = mergeMaryxmlIntoBMLStylesheet.newTransformer();
        mergingTransformer.setURIResolver(new URIResolver() {
            public Source resolve(String href, String base) {
                if (href == null) {
                    return null;
                } else if (href.equals("mary.acoustparams")) {
                    return new StreamSource(new StringReader(acoustparams));
                } else {
                    return null;
                }
            }
        });

        mergingTransformer.transform(bmlSource, mergedResult);
        return mergedWriter.toString();
    }
    
    private String ssml2acoustparams(String ssml) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mary.process(ssml, "SSML", "ACOUSTPARAMS", "WAVE", DEFAULTVOICE, baos);
        return new String(baos.toByteArray(), "UTF-8");
    }
    
    private byte[] acoustparams2audio(String acoustparams) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        mary.process(acoustparams, "ACOUSTPARAMS", "AUDIO", "WAVE", DEFAULTVOICE, baos);
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
     * This server will listen for incoming BML files, and
     * generate Enriched BML and Audio data from it.
     * In the current version, the name of the OpenAIR whiteboard
     * is hard-coded as "Greta.Whiteboard"; the input data type is
     * "Greta.Data.BMLCode"; the enriched BML output type is
     * "Greta.Data.EnrichedBMLCode"; and the audio output is
     * "Greta.Data.Audio". 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception 
    {

        String airhost = System.getProperty("airserver.host", "localhost");
        int airport = Integer.getInteger("airserver.port", 10000).intValue();

        BMLSpeechPsydule reader = new BMLSpeechPsydule(airhost, airport);
        reader.listenAndProcess();
    }


}
