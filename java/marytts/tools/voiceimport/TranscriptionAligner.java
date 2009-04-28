/**
 * Copyright 2008 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import marytts.datatypes.MaryXML;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class TranscriptionAligner extends VoiceImportComponent {
    
    private DatabaseLayout db;
    private String locale;
    private int progress;
    
    private marytts.tools.analysis.TranscriptionAligner aligner;
    

    

    
    public TranscriptionAligner() {
    }

    public String getName() {
        return "TranscriptionAligner";
    }
    
    public void initialiseComp()
    throws ParserConfigurationException, IOException, SAXException
    {
        aligner = new marytts.tools.analysis.TranscriptionAligner(AllophoneSet.getAllophoneSet(db.getProp(db.ALLOPHONESET)));
        aligner.SetEnsureInitialBoundary(true);
    }
    
    public SortedMap<String,String> getDefaultProps(DatabaseLayout theDb)
    {
        this.db = theDb;
        locale = db.getProp(db.LOCALE);
        if (props == null){
            props = new TreeMap<String, String>();
        }
        return props;
    }
    
    protected void setupHelp()
    {
        props2Help = new TreeMap<String, String>();
    }
    
 
    
    public int getProgress()
    {
        return progress;
    }
    /**
     * align and change automatic transcriptions to manually 
     * corrected ones.
     * 
     * XML-Version: this changes mary xml-files (PHONEMISED)
     * @throws TransformerException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     * @throws XPathExpressionException 
     */
    public boolean compute()
    throws IOException, TransformerException, ParserConfigurationException, SAXException
    {
        File xmlOutDir = new File((String) db.getProp(db.ALLOPHONESDIR));
        if (!xmlOutDir.exists())
            xmlOutDir.mkdir();

        // for parsing xml files
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        
        // for writing xml files
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();

        System.out.println("traversing through " + bnl.getLength() + " files");
       
        String promptAllophonesDir = db.getProp(db.PROMPTALLOPHONESDIR);
        for (int i=0; i<bnl.getLength(); i++) {
            progress = 100*i/bnl.getLength();
            File nextFile = new File(promptAllophonesDir
                    +System.getProperty("file.separator")
                    +bnl.getName(i)+".xml");

            System.out.println(bnl.getName(i));
            
            // get original xml file
            Document doc = docBuilder.parse(nextFile);

            // open destination xml file
            Writer docDest  = new OutputStreamWriter(new FileOutputStream(xmlOutDir.getAbsolutePath() + nextFile.getName()), "UTF-8");

            // open file with manual transcription that is to be aligned
            String manTransString;
            try{

                String trfdir = db.getProp(db.LABDIR);
                
                String trfname = trfdir + 
                nextFile.getName().substring(0, nextFile.getName().length() - 4) + ".lab";
                
                System.out.println(trfname);
                
                manTransString = aligner.readLabelFile(trfname);
                
            } catch ( FileNotFoundException e ) {
                System.out.println("No manual transcription found, copy original ...");
                
                // transform the unchanged xml-structure to a file
                DOMSource source = new DOMSource( doc );
                StreamResult output = new StreamResult(docDest);
                transformer.transform(source, output);

                continue;
            }
            
            // align transcriptions
            aligner.alignXmlTranscriptions(doc, manTransString);
            
            // write results to output
            DOMSource source = new DOMSource( doc );
            StreamResult output = new StreamResult(docDest);
            transformer.transform(source, output);
        }
                
        return true;
    }
     
  

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        VoiceImportComponent vic  =  new TranscriptionAligner();
        DatabaseLayout db = new DatabaseLayout(vic);
        vic.compute();
    }
}

