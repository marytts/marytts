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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import marytts.exceptions.MaryConfigurationException;
import marytts.util.dom.DomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class stores user ratings
 * @author sathish
 *
 */
public class UserRatingStorer {

    private String storeDirectoryPath = null;
    private DataRequestHandler infoRH = null;
    
    public UserRatingStorer( String dirPath, DataRequestHandler infoRH) {
        this.storeDirectoryPath = dirPath;
        this.infoRH = infoRH;
    }
    
    public int ratingsDoneSoFar(String eMailID) throws ParserConfigurationException, SAXException, IOException{
        
        if( !isEmailIDExists(eMailID) ) {
            return 0;
        }
        
        File filePath = new File(this.storeDirectoryPath+File.separator+eMailID);
        Document doc = DomUtils.parseDocument(filePath);
        Element rootElement = doc.getDocumentElement();
        //Element fSampleElement = DomUtils.getFirstElementByTagName(rootElement, TestDataLoader.SAMPLE);
               
        NodeList ndl = rootElement.getElementsByTagName(TestDataLoader.SAMPLE);
        int nSamples = ndl.getLength();
        for( int i=0; i<nSamples; i++ ) {
            Element nElement = (Element) ndl.item(i);
            if(!nElement.hasAttribute("result")){
                return i;
            }
            else if (nElement.hasAttribute("result") && (nElement.getAttribute("result")).equals("")){
                return i;
            }
        }
        
        return nSamples;
    }
    
    
    private boolean isEmailIDExists(String eMailID) {
        File filePath = new File(this.storeDirectoryPath+File.separator+eMailID);
        return filePath.exists();
    }
    
    private void createResultFile(String eMailID) throws IOException{
        File filePath = new File(this.storeDirectoryPath+File.separator+eMailID);
        PrintWriter pw = new PrintWriter(new FileWriter(filePath));
        
        pw.println("<test-set>");
        int nSamples = getNumberOfSamples();
        for( int i=0; i<nSamples; i++ ){
            pw.println("<sample id=\""+i+"\" basename=\""+getSampleBaseName(i)+"\" />");
        }
        pw.println("</test-set>");
        pw.flush();
        pw.close();
    }
    
    public void writeSampleResult(String eMailID, int presentSampleNumber, 
            String baseName, String result ) throws ParserConfigurationException, SAXException, IOException, ClassCastException, TransformerFactoryConfigurationError, TransformerException, ClassNotFoundException, InstantiationException, IllegalAccessException, MaryConfigurationException {
        
        if ( !isEmailIDExists(eMailID) ) {
            createResultFile(eMailID);
        }
        
        File filePath = new File(this.storeDirectoryPath+File.separator+eMailID);
        Document doc = DomUtils.parseDocument(filePath);
        Element rootElement = doc.getDocumentElement();
        //Element fSampleElement = DomUtils.getFirstElementByTagName(rootElement, TestDataLoader.SAMPLE);
               
        NodeList ndl = rootElement.getElementsByTagName(TestDataLoader.SAMPLE);
        
        if ( presentSampleNumber >= ndl.getLength()){
            throw new RuntimeException(" Given sample number is NOT available in given TEST SET \n");
        }
        
        Element nElement = (Element) ndl.item(presentSampleNumber);
        if(!(nElement.getAttribute("basename")).equals(baseName)){
            throw new RuntimeException(" Given Basename is NOT mached with present sample basename \n ");
        }
        
        if(!nElement.hasAttribute("result")){
            nElement.setAttribute("result", result);
        }
        else if (nElement.hasAttribute("result") && (nElement.getAttribute("result")).equals("")){
            nElement.setAttribute("result", result);
        }
        else {
            System.out.println("It already contains result... NOT EXPECTED! ");
        }
        
        DomUtils.document2File(doc, filePath);
    }
    
    private int getNumberOfSamples(){
        return this.infoRH.getNumberOfSamples();
    }
    
    private String getSampleBaseName(int num){
        return this.infoRH.getSampleBaseName(num);
    }
    
    
    /**
     * @param args
     * @throws IOException 
     * @throws SAXException 
     * @throws ParserConfigurationException 
     */
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        // TODO Auto-generated method stub
        String inFile = "/home/sathish/phd/PerceptionTest/perception.xml";
        String dirPath = "/home/sathish/phd/PerceptionTest/";
        //TestDataLoader tdl = new TestDataLoader(inFile);
        
        DataRequestHandler infoRH = null;
        UserRatingStorer urs = new UserRatingStorer(dirPath, infoRH);
        urs.createResultFile("satt@gmail.com");
        try {
            urs.writeSampleResult("satt@gmail.com", 2, 
                    "0014", "friendly" );
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
