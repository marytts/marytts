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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import marytts.util.dom.DomUtils;

/**
 * The class loads the perception test stimuli
 * @author sathish pammi
 *
 */
public class TestDataLoader {

    public final static String QUESTION = "question";
    public final static String OPTION = "option";
    public static final String TITLE = "title";
    public static final String TESTSET = "test-set";
    public static final String SAMPLE = "sample"; 
    
    private String testQuestion;
    private String[] answerOptions;
    private String testTitle;
    private String questionType;
    private Map<String, String> samples;
    
    public TestDataLoader(String fileName) throws ParserConfigurationException, SAXException, IOException {
        Document doc = DomUtils.parseDocument(new File(fileName));
        samples = new HashMap<String, String>();
        process(doc);
    }
    
    public String getTestTile() {
        return this.testTitle;
    }
     
    public String getTestQuestion() {
        return this.testQuestion;
    }
    
    public String getTestQuestionType() {
        return this.questionType;
    }
    
    public String[] getAnswerOptions() {
        return this.answerOptions;
    }
    
    public Map<String, String> getTestSamples(){
        return this.samples;
    }
    
    private void process(Document doc) {
        Element rootElement = doc.getDocumentElement();
        
        // GET QUESTION and OPTIONS
        //NodeList ndl = rootElement.getElementsByTagName(this.TITLE);
        
        
        NodeIterator nodeIterator = DomUtils.createNodeIterator(doc, rootElement, QUESTION);
        Node nd;
        while((nd = nodeIterator.nextNode()) != null){
            //System.out.println(getPlainTextBelow(nd).trim());
            this.testQuestion = getPlainTextBelow(nd).trim();
            this.answerOptions = getAnswerOptions(nd);
            this.questionType = getQuestionType(nd);
        }
    
        // Get title
        Element titleElement = DomUtils.getFirstElementByTagName(rootElement, TITLE);
        this.testTitle = titleElement.getAttribute("name");
        
        // Get Samples
        
        Element testSetElement = DomUtils.getFirstElementByTagName(rootElement, TESTSET);
        
        this.samples = getTestSampleSet((Node) testSetElement);
    }

    private String getQuestionType(Node qNode) {
        
        Element qElement = (Element)  qNode;
        return qElement.getAttribute("type");
        
    }

    /**
     * Return the concatenation of the values of all text nodes below the given
     * node. One space character is inserted between adjacent text nodes.
     */
    private String getPlainTextBelow(Node n)
    {
        if (n == null) return null;
        Document doc = null;
        if (n.getNodeType() == Node.DOCUMENT_NODE) {
            doc = (Document) n;
        } else {
            doc = n.getOwnerDocument();
        }
        StringBuilder buf = new StringBuilder();
        NodeIterator it = ((DocumentTraversal)doc).
            createNodeIterator(n, NodeFilter.SHOW_TEXT, null, true);
        Text text = null;
        while ((text = (Text) it.nextNode()) != null) {
            buf.append(text.getData().trim());
            buf.append(" ");
        }
        return buf.toString();
    }
    
    
    private String[] getAnswerOptions(Node qNode) {
        if (qNode == null) return null;
        Document doc = null;
        if (qNode.getNodeType() == Node.DOCUMENT_NODE) {
            doc = (Document) qNode;
        } else {
            doc = qNode.getOwnerDocument();
        }
        ArrayList<String> listOptions = new ArrayList<String>();
        NodeIterator it = DomUtils.createNodeIterator(doc, qNode, OPTION);
        Element nd = null;
        while ((nd = (Element) it.nextNode()) != null) {
            //System.out.println(nd.getAttribute("name"));
            listOptions.add(nd.getAttribute("name"));
        }
        
        return listOptions.toArray(new String[1]);
    }

    
    private Map<String, String> getTestSampleSet(Node testSampleNode) {
        if (testSampleNode == null) return null;
        Document doc = null;
        if (testSampleNode.getNodeType() == Node.DOCUMENT_NODE) {
            doc = (Document) testSampleNode;
        } else {
            doc = testSampleNode.getOwnerDocument();
        }
        
        Map<String, String> tSamples = new HashMap<String, String>();
        NodeIterator it = DomUtils.createNodeIterator(doc, testSampleNode, SAMPLE);
        Element nd = null;
        while ((nd = (Element) it.nextNode()) != null) {
            //System.out.println(nd.getAttribute("basename") + "  " + nd.getAttribute("path") );
            tSamples.put(nd.getAttribute("basename").trim(), nd.getAttribute("path").trim());
        }
        
        return tSamples;
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
        TestDataLoader tdl = new TestDataLoader(inFile);

    }

}
