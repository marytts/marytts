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
package de.dfki.lt.mary.emospeak;

import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;


/**
 *
 * @author Marc Schr&ouml;der
 */
public class EmoTransformer extends Thread {
    private int r;
    private ProsodyXMLDisplayer emoSpeak;

    private javax.xml.transform.TransformerFactory tFactory = null;
    private javax.xml.transform.Templates stylesheet = null;
    private javax.xml.transform.Transformer transformer = null;
    private javax.xml.parsers.DocumentBuilderFactory dbFactory = null;
    private javax.xml.parsers.DocumentBuilder docBuilder = null;

    private org.w3c.dom.Document emotionDocument = null;

    private boolean inputAvailable = false;
    private int activation;
    private int evaluation;
    private int power;
    private String text;
    private String maryxmlString;
    private Locale locale;
    
    private boolean exitRequested = false;
    
    
    /** Creates new EmoTransformer */
    public EmoTransformer(ProsodyXMLDisplayer emoSpeak)
    throws TransformerConfigurationException, ParserConfigurationException
    {
        this.emoSpeak = emoSpeak;
        // Try to find a suitable XSLT transformer
        tFactory = javax.xml.transform.TransformerFactory.newInstance();
/*        if (false && tFactory instanceof org.apache.xalan.processor.TransformerFactoryImpl) {
            Hashtable xalanEnv = (new org.apache.xalan.xslt.EnvironmentCheck()).getEnvironmentHash();
            String xalan2Version = (String) xalanEnv.get("version.xalan2x");
            if (xalan2Version == null || xalan2Version.equals(""))
                xalan2Version = (String) xalanEnv.get("version.xalan2");
            if (xalan2Version != null && !xalan2Version.equals(""))
                System.err.println("Using " + xalan2Version);
        } else {
                */
            System.err.println("Using XSL processor " + tFactory.getClass().getName());
//        }
        javax.xml.transform.stream.StreamSource stylesheetStream =
            new javax.xml.transform.stream.StreamSource (
                EmoTransformer.class.getResourceAsStream("emotion-to-mary.xsl")
            );
        stylesheet = tFactory.newTemplates( stylesheetStream );
        dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        docBuilder = dbFactory.newDocumentBuilder();
        transformer = stylesheet.newTransformer();

    }

    /**
     * Asynchronously set the latest emotion values. Overwrites any
     * previous, unprocessed data.
     */
    public synchronized void setEmotionValues(int activation, int evaluation, int power, String text, Locale locale, int r) {
        this.activation = activation;
        this.evaluation = evaluation;
        this.power      = power;
        this.text       = text;
        this.locale = locale;
        inputAvailable = true;
        this.r = r;
        notifyAll();
    }
    
    
    public synchronized void requestExit() {
        exitRequested = true;
        notifyAll();
    }

    private void createEmotionDocument() {
        emotionDocument = docBuilder.getDOMImplementation().
            createDocument(null, "emotion", null);
        org.w3c.dom.Element e = emotionDocument.getDocumentElement();
        e.setAttributeNS("http://www.w3.org/XML/1998/namespace", "lang", locale.getLanguage());
        e.setAttribute("activation", String.valueOf(activation));
        e.setAttribute("evaluation", String.valueOf(evaluation));
        e.setAttribute("power", String.valueOf(power));
        e.appendChild(emotionDocument.createTextNode(text));
    }
    
    private void transformToMaryXML()
    throws javax.xml.transform.TransformerException
    {
        javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource (emotionDocument);
        java.io.StringWriter sw = new java.io.StringWriter();
        javax.xml.transform.stream.StreamResult streamResult = new javax.xml.transform.stream.StreamResult (sw);
        transformer.transform(domSource, streamResult);
        maryxmlString = sw.toString();
    }
    
    private synchronized void doWait() {
        try {
            wait();
        } catch (InterruptedException e) {}
    }
    
    public void run() {
        while (!exitRequested) {
            if (inputAvailable) {
                inputAvailable = false;
                try {
                    int r1 = r;
                    System.err.println("EmoTransformer about to process request no. " + r1);
                    createEmotionDocument();
                    transformToMaryXML();
                    System.err.println("EmoTransformer has processed.");
                    emoSpeak.updateProsodyXML(maryxmlString, r1);
                } catch (javax.xml.transform.TransformerException e) {
                    e.printStackTrace();
                }
            } else {
                doWait();
                System.err.println("EmoTransformer waking up from wait.");
            }
        }
        System.err.println("EmoTransformer exiting.");
    }
}
