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
package de.dfki.lt.mary.modules.tib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.NoSuchPropertyException;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.util.dom.DomUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/**
 * The Tibetan tonebuilder
 * Checks first whether the tone is defined in the lexicon
 * If the tone is not defined in the lexicon the tone-rules (defined in toneRules.xml)
 * are applied.
 * Created on 03.09.2005
 * @author Anna Hunecke, Jens Apel
 *  
 */
public class ToneBuilder extends InternalModule {


    /**
     * Constructor
     */
    public ToneBuilder() {
        super("TibetanToneBuilder", MaryDataType.get("PHONEMISED_TIB"), MaryDataType.get("TONES_TIB"));
    }
    
    //  document that is parsed 	
    private Document doc; 
    //  map that will be filled with the definitions, tone rules
    private HashMap slotMap = new HashMap(); 
    //  map that will contain the lists  
    private HashMap listMap = new HashMap(); 
    //  map that will contain the maps 
    private HashMap mapMap = new HashMap();
    
    // map that will contain possible contents of slots
    private HashMap listPossibleContentsMap = new HashMap();
    // map that will contain contents of rules
    private HashMap listRuleContentsMap = new HashMap();
    
    // the dictionary
	private Map lexicon;
    
    
    
   
    
	
    /**
     * Read in the data of the xml file
     * rules in the xml-file (toneRules.xml) is corrected
     * builts the lexicon
     */
    public void startup() throws Exception {
        super.startup();
        loadSlotRules(); // fill the rule map
        buildListMap(); // fill the list map
        buildListMap2(); // fill the list map for listsPossibleContents lists
        buildListMap3(); // fill the list map for listsRuleContents lists
        checkXMLSyntax();
        buildLexicon();
        
    }

    /**
     * Read in the lists, maps and rules of the xml file
     * Store them in slotMap
     * @throws Exceptions if there are parsing problems
     */
    private void loadSlotRules ()
	throws FactoryConfigurationError, ParserConfigurationException, 
	       org.xml.sax.SAXException, IOException, NoSuchPropertyException {

    	// parsing the xml rule file
    	DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    	DocumentBuilder b = f.newDocumentBuilder();
    	Document slotRules = b.parse(getClass().getResourceAsStream("toneRules.xml"));
    	
    	Element root = slotRules.getDocumentElement();
    	for (Element e = MaryDomUtils.getFirstChildElement(root);
	     e != null;
	     e = MaryDomUtils.getNextSiblingElement(e)) { //HashMap with 3 entries 
    		if (e.getTagName().equals("definitions")) { //list defintions
    			slotMap.put("definitions",e);
    		}
    		if (e.getTagName().equals("tones")) { // all rules concerning the tones
    			slotMap.put("tones",e);
    		}
    		if (e.getTagName().equals("listsPossibleContents")){ // set of lists to define possible contents of slots
    			slotMap.put("listsPossibleContents",e);
    		}
    		if (e.getTagName().equals("listsRuleContents")){ // set of lists to define contents of rules
        		slotMap.put("listsRuleContents",e);
    		}
    		if (e.getTagName().equals("specifications")){ // specifications to define contents of rules 
        		slotMap.put("specifications",e);
    		}
    	}
    }
    
    /**
     * Store the lists of the xml file in listMap
     * and the maps in mapMap
     */
    private void buildListMap()
    {
    		Element listDefinitions = (Element) slotMap.get("definitions");
    		// search for entries with tag "list"
    		TreeWalker tw =
    			((DocumentTraversal) listDefinitions.getOwnerDocument()).createTreeWalker(
    					listDefinitions,
						NodeFilter.SHOW_ELEMENT,
						new NameNodeFilter(new String[] {"list"}),
						false);
    	
    		Element list=null;
    		while((list = (Element) tw.nextNode()) != null) {
    			String name = list.getAttribute("name"); // list name
    			if(list.hasAttribute("items")) { // list is defined in the xml file (no external list)
    				String items = list.getAttribute("items"); 
    				HashSet itemSet = new HashSet(); // build a set with the elements in the list
    				StringTokenizer st = new StringTokenizer(items,":");
    				while(st.hasMoreTokens()) {
    					itemSet.add(st.nextToken());
    				}
    				listMap.put(name,itemSet); // put the set on the map
    			}
    		}
    		TreeWalker tw2=
    			((DocumentTraversal) listDefinitions.getOwnerDocument()).createTreeWalker(
    					listDefinitions,
						NodeFilter.SHOW_ELEMENT,
						new NameNodeFilter(new String[] {"map"}),
						false);
    	
    		Element map=null;
    		while((map = (Element) tw2.nextNode()) != null) {
    			String name = map.getAttribute("name"); // map name
    			if(map.hasAttribute("items")) { // map is defined in the xml file (no external list)
    				String items = map.getAttribute("items"); 
    				HashMap itemMap = new HashMap(); // build a set with the elements in the map
    				String[] st = items.split("\\|");
    				for (int i=0; i< st.length; i++) {
    					String[] stItems = st[i].split("#");
    						itemMap.put(stItems[0],stItems[1]);
    				}
    				mapMap.put(name,itemMap); // put the set on the map
    			}
    		}  
    
    }
    /**
     * Store the list of possible contents of the xml file in listMap
     * and the maps in mapMap
     */
    private void buildListMap2()
    {
		Element listDefinitions = (Element) slotMap.get("listsPossibleContents");
		// search for entries with tag "list"
		TreeWalker tw =
			((DocumentTraversal) listDefinitions.getOwnerDocument()).createTreeWalker(
					listDefinitions,
					NodeFilter.SHOW_ELEMENT,
					new NameNodeFilter(new String[] {"list"}),
					false);
	
		Element list=null;
		while((list = (Element) tw.nextNode()) != null) {
			String name = list.getAttribute("name"); // list name
			if(list.hasAttribute("items")) { // list is defined in the xml file (no external list)
				String items = list.getAttribute("items"); 
				HashSet itemSet = new HashSet(); // build a set with the elements in the list
				StringTokenizer st = new StringTokenizer(items,":");
				while(st.hasMoreTokens()) {
					itemSet.add(st.nextToken());
				}
				listPossibleContentsMap.put(name,itemSet); // put the set on the map
				
			}
		}
    }
    
    /**
     * Store the list of listRuleContents of the xml file in listMap
     * and the maps in mapMap
     */
	private void buildListMap3()
    {
		Element listDefinitions = (Element) slotMap.get("listsRuleContents");
		// search for entries with tag "list"
		TreeWalker tw =
			((DocumentTraversal) listDefinitions.getOwnerDocument()).createTreeWalker(
					listDefinitions,
					NodeFilter.SHOW_ELEMENT,
					new NameNodeFilter(new String[] {"list"}),
					false);
	
		Element list=null;
		while((list = (Element) tw.nextNode()) != null) {
			String name = list.getAttribute("name"); // list name
			if(list.hasAttribute("items")) { // list is defined in the xml file (no external list)
				String items = list.getAttribute("items"); 
				HashSet itemSet = new HashSet(); // build a set with the elements in the list
				StringTokenizer st = new StringTokenizer(items,":");
				while(st.hasMoreTokens()) {
					itemSet.add(st.nextToken());
				}
				listRuleContentsMap.put(name,itemSet); // put the set on the map
				
			}
		}
    }
    
    
    /**
     * Process the data
     * @param d the data to be processed
     * @return the result of the processing
     */
    public MaryData process(MaryData d) throws Exception {
        doc = d.getDocument();
        NodeList tokens = doc.getElementsByTagName(MaryXML.TOKEN); // get the tokens
        for (int i = 0; i < tokens.getLength(); i++) {
            Element token = (Element) tokens.item(i);
            // And now the actual processing
            processToken(token);
        }     
        MaryData result = new MaryData(outputType());
        result.setDocument(doc);
        return result;
    }
     
    /**
     * process a token (=syllable) of the data
     * @param token the syllable to be processed
     */
    public void processToken (Element token){
    	 // get the token text and look it up in the lexicon
      	 String text = MaryDomUtils.tokenText(token);
      	 String[] tonesFromLexicon = lookUp(text);
    	 NodeList syllables = token.getElementsByTagName(MaryXML.SYLLABLE);
    	 //	store the length of the word
        int max = syllables.getLength();
        for (int i=0; i<max; i++) {
            Element syllable = (Element) syllables.item(i);
            
            Element prevSyl;
            Element nextSyl;
            //the previous syllable is null, if this is the first syllable
            if (i == 0){
                prevSyl = null;}
            else {prevSyl = (Element) syllables.item(i-1);}
            //the next syllabel is null, if this is the last syllable
            if (i == (max-1)){
                nextSyl = null;}
            else {nextSyl = (Element) syllables.item(i+1);}
            
            String setTone;
            if (tonesFromLexicon != null && i < tonesFromLexicon.length
                    && !tonesFromLexicon[i].equals("-")) {
                setTone = tonesFromLexicon[i];
            } else { // determine tone by rule
                setTone = applyRules(((Element) slotMap.get("tones")), syllable, nextSyl, prevSyl, i, max-1);
            }
            syllable.setAttribute("tone", setTone);
        }
    }	
    /**
     * Find the first rule that matches and apply it
     * @param e the element that contains the rules
     * @param syllable the syllable to be processed
     * @param sylpos the position of the syllable in the word (0 to maxsyl)
     * @param maxsyl the number of syllables in the word (starting with 0)
     * @return the sampa string found in the rules 
     */
    public String applyRules(Element e, 
			     			Element syllable, 
							Element nextSyl, 
							Element prevSyl,
							int sylpos, 
							int maxsyl){
    	String result = "";
    	NodeList rules = e.getElementsByTagName("rule");
    	//iterate over the rules
    	for (int i = 0; i< rules.getLength(); i++){
    		Node next = rules.item(i);
    		//found is set to false, if the syllable does not fulfill the conditions
    		boolean found  = true;
    		//get the attributes-node
    		Element attributes = DomUtils.getFirstElementByTagName(next,"attribute");
    		//if it exists, check it
    		if (!(attributes==null)){
    			if (!checkAttributes(attributes, syllable)){
    				found = false;}
    		}
    		//get the nextattributes-node
    		Element nextattributes = DomUtils.getFirstElementByTagName(next,"nextattributes");
    		//if it exists and found is not false, check it
    		if (!(nextattributes==null) && found){
    			if (!checkAttributes(nextattributes, nextSyl)){
    				found = false;}
    		}
    		//get the prevattributes-node
    		Element prevattributes = DomUtils.getFirstElementByTagName(next,"prevattributes");
    		//if it exists and found is not false, check it
    		if (!(prevattributes==null) && found){
    			if (!checkAttributes(prevattributes, prevSyl)){
    				found = false;}
    		}
    		//get the condition-node
    		Element condition = DomUtils.getFirstElementByTagName(next,"condition");
    		//if it exists and found is not false, check it
    		if (!(condition == null) && found){
    			if (!checkCondition(condition,syllable,sylpos,maxsyl)){
    				found = false;}
    		}
    		//if the syllable fulfills the rule conditions
    		if (found){
		    //get the action-node
		    Element action = DomUtils.getFirstElementByTagName(next,"action");
		    //read the sampa specified in the action-node into result  
		    result = act(action,syllable);
		    String num = ((Element) next).getAttribute("num");
		    logger.debug("rule num "+num+" fired");
		    break;
    		}
    	}
    	//return the sampa
    	return result;
    }
    
	/**
     * Look at an attributes-node and determine if
     * its specifications match to the syllable
     * @param attributes the attributes-node
     * @param syllable the syllable to be matched
     * @return true, if the attributes-node matches 
     */
    public boolean checkAttributes(Element attributes, Element syllable){
    	//result is set to true, if all conditions are fulfilled
    	boolean result = false;
    	NamedNodeMap attNodes = attributes.getAttributes();
    	//if the syllable is not null
    	//(syllable is null, if next-/prevattributes is called
    	//for a final or initial syllable)
    	if (!(syllable==null)){
    		//iterate over the attributes
    		for(int z=0; z<attNodes.getLength(); z++) {
    			Node el = attNodes.item(z);
    			//get name and value of current attribute
    			String currentAtt = el.getNodeName();
    			String currentVal = el.getNodeValue();
    			//if the syllable has the attribute, 
    			if (syllable.hasAttribute(currentAtt)){
    				//read in the value in syllVal
    				String syllVal = syllable.getAttribute(currentAtt);
    				//if the value of current attribute is a list
    				if (currentVal.indexOf("INLIST")!=-1){
    					//split the value
    					String[] st = currentVal.split(":");
    					//get the list from listMap and check
    					//if it contains syllVal
    					if (((HashSet) listMap.get(st[1])).contains(syllVal)){
    						result = true;}
    					else {
    						result = false;
    						break;}
    				}
    				else {
    					//if the value of the current attribute is a map
    					if (currentVal.indexOf("INKEYS")!=-1){
    						//split the value
    						String[] st = currentVal.split(":");
    						//get the map from mapMap
    						HashMap hm = ((HashMap) mapMap.get(st[1]));
    						//check, if its keys contain syllVal
    						if (hm.containsKey(syllVal)){
    							result = true;}
    						else {
    							result = false;
    							break;}
    					}
    					//else compare syllVal with the value of the current attribute
    					else{		
    						if (syllVal.equals(currentVal)){
    							result = true;}
    						else {
    							result = false;
    							break;}
    					}
    				}
    			}
    			//if the syllable does not have this attribute
    			else {
    				//if the attribute is required to be empty, set result to true
    				if (currentVal.indexOf("EMPTY")!=-1){
    					result = true;
    				}
    				//else return false
    				else{result = false;
    				break;}
    			}
    		}
    	}
    	return result;
    }
    
    /**
     * Determine if the conditions in a condition-node 
     * are fulfilled by a syllable
     * @param condition the condition-node
     * @param syllable the syllable 
     * @param sylpos the position of the syllable in the word (0 to maxsyl)
     * @param maxsyl the number of syllables in the word (starting with 0)
     * @return true, if the conditions are satisfied
     */
    public boolean checkCondition(Element condition, Element syllable, int sylpos, int maxsyl){
	//result is set to true, if all conditions are fulfilled
	boolean result = false;
	if (condition.hasAttribute("position")){ 
	    //if this word is not monosyllabic
	    if (!(maxsyl == 0)){
		boolean hasnext = condition.getAttribute("position").equals("hasnext");
		//if pos is hasprevious (= !hasnext) and this is not the first syl
		//or if pos is hasnext and this is not the last syl, return true
		if ((!hasnext && sylpos > 0)  || (hasnext && sylpos < maxsyl)){
		    result = true;}
	    }
	}
	return result;
    }  
       
    /**
     * Carry out the action specified in an 
     * action-node 
     * @param action the action-node
     * @param syllable the syllable
     * @return the result of the action
     */
    public String act(Element action, Element syllable){
    	//result will be filled with the sampa
    	String result = "";
    	String toDo = action.getAttribute("tone");
    	//if there is more than one action
    	if (toDo.indexOf("+")!=-1){
    		//split up the actions
    		String[] actions = toDo.split("\\+");
    		StringBuffer sb = new StringBuffer(); 
    		//iterate over the actions
    		for (int i = 0; i< actions.length; i++){
    			//if the sampa is specified in a map
    			if (actions[i].indexOf("MAP")!=-1){ 	    	
    				String[] next = actions[i].split(":");
    				//get the map from mapMap and the key from the syllable
    				//store it in the StringBuffer
    				sb.append(((HashMap) mapMap.get(next[2])).get(syllable.getAttribute(next[1])));
    	    		}
    			//else the sampa is specified directly, store it in the StringBuffer
    			else {sb.append(actions[i]);}
    		}
    		//convert the StringBuffer and store it in result
    		result = sb.toString();
    	}
    	//else there is only one action
    	else{
    		//if the sampa is specified in a map
    		if (toDo.indexOf("MAP")!=-1){
    			String[] st = toDo.split(":");
    			//get the map from mapMap and the key from the syllable
    			//store it in result
    			result = ((String) ((HashMap) mapMap.get(st[2])).get(syllable.getAttribute(st[1])));
    	    }
    		//else the sampa is specified directly, store it in result
    		else {result = toDo;}
    		}
    	return result;
    }
    
    /**
     * checks whether the syntax of the rules in the 
     * XML-file is consistent with the allowed set of syntax
     * @throws IllegalArgumentException if an error was found 
     */ 
        
    public void checkXMLSyntax() throws IllegalArgumentException
    {
    	try{
    	//boolean returnParameter = true;
    	NodeList rules = ((Element) slotMap.get("tones")).getElementsByTagName("rule");
    	for (int i = 0; i< rules.getLength(); i++){
    		Element next = (Element) rules.item(i);
    		
    		//if the rule has no number, throw an error
    		if (!(next).hasAttribute("num")){
    			throw new IllegalArgumentException ("Missing rule number");}
    		String num = (next).getAttribute("num");
    		   		
     		// iterates over "attribute" and "action" features in XML file
    		for (Element attributes = DomUtils.getFirstChildElement(next);attributes!=null;attributes = DomUtils.getNextSiblingElement(attributes)){
    			NamedNodeMap attNodes = attributes.getAttributes(); // map of all attributes
    			for(int z=0; z<attNodes.getLength(); z++) { // loop over MaryXML attributes in rule part
    				Node el = attNodes.item(z);
    				String currentAtt = el.getNodeName();  // e.g. slot1, root
    				String currentVal = el.getNodeValue(); // e.g. "2", "INLIST:defaultHighTone"
    				
    				checkAttributes(attributes.getNodeName(),currentAtt,num);
    				
    				if(currentVal.indexOf("INLIST")!=-1){
    					String[]st = currentVal.split(":");
        			        			
    					for (Iterator j= ((HashSet)listMap.get(st[1])).iterator();j.hasNext();){
    					  					
    						String currEle = (String)j.next();
    						if (!comparePossibleContent(currEle,currentAtt,attributes.getNodeName())){
    							//returnParameter = false;
    							throw new IllegalArgumentException ("Rule number "+num+" is not possible at "+currentAtt+" Value:"+currEle);
    						}
    						
    						if (!compareRuleContent(currEle,currentAtt,attributes.getNodeName())){ //attributes.getNodeName() <-- to determine whether "attribute" or "action"  
    							//returnParameter = false; 
    							throw new IllegalArgumentException ("Rule number "+num+" is not in prespecified list at "+currentAtt+" Value:"+currEle);
    						}
    					}
        			}
    				else {
    					if (!comparePossibleContent (currentVal,currentAtt,attributes.getNodeName())){
    						//returnParameter = false;
    						throw new IllegalArgumentException ("Rule number "+num+" is not possible at "+currentAtt+" Value:"+currentVal);
    					}
    					
    					if (!compareRuleContent (currentVal,currentAtt,attributes.getNodeName())){
    						//returnParameter = false;
    						throw new IllegalArgumentException ("Rule number "+num+" is not in prespecified list at "+currentAtt+" Value:"+currentVal);
    					}
    				}
    			}
    		}
    	}
    	
    	//return returnParameter;
    	}
    	catch (NullPointerException e1){
    		throw new IllegalArgumentException ("Syntax Error :\n "+e1.getCause());
    		
    	}
    }
    
    /**
     * compares whether letters and tones in the XML file match
     * with the prespecified letters and tones   
     * @param letter letters or tones
     * @param attribute attribute where the letter or tone belongs too   
     * @param nodeName tagName of the attributes (attribute, action)
     * @return true if the letter or tone exists
     */
    
    public boolean compareRuleContent(String letter, String attribute, String nodeName){
       	NodeList rules = ((Element) slotMap.get("specifications")).getElementsByTagName("rule");
    	Element next = (Element) rules.item(0);
    	
    	Element attributes = DomUtils.getFirstElementByTagName(next,nodeName);
    	
    	NamedNodeMap attNodes = attributes.getAttributes(); // map of all attributes
    	Node el = attNodes.getNamedItem(attribute);   // attribute = slot1, slot2...
    	String currentAtt = el.getNodeName();
    	String currentVal = el.getNodeValue();
    	
    	if(currentVal.indexOf("INLIST")!=-1){
			String[]st = currentVal.split(":");
			if(((HashSet) listRuleContentsMap.get(st[1])).contains(letter)){
				return true;
			}
			else {
				return false;
			}
    	}
    	else {
    		if(((HashSet) listRuleContentsMap.get(currentVal)).contains(letter)){
  				return true;
			}
			else {
				return false;
			}
    	}
    }
    
    
    /**
     * compares whether letters and tones in the XML file match
     * with the letters and tones that could possibly occur
     * in the certain slots in tibetan   
     * @param letter or tones
     * @param attribute attribute where the letter or tone belongs too   
     * @param nodeName tagName of the attributes (attribute, action)
     * @return true if the letter or tone matches
     */
    
    public boolean comparePossibleContent(String letter, String attribute, String nodeName){
    	NodeList rules = ((Element) slotMap.get("specifications")).getElementsByTagName("rule");
    	Element next = (Element) rules.item(0);
    	Element attributes = DomUtils.getFirstElementByTagName(next,nodeName);
    	NamedNodeMap attNodes = attributes.getAttributes(); // map of all attributes
    	Node el = attNodes.getNamedItem(attribute);   // attribute = slot1, slot2...
    	String currentAtt = el.getNodeName();
    	String currentVal = el.getNodeValue();
    	
    	if(currentVal.indexOf("INLIST")!=-1){
			String[]st = currentVal.split(":");
			if(((HashSet) listPossibleContentsMap.get(st[1])).contains(letter)){
				return true;
			}
			else {
				return false;
			}
    	}
    	else {
    		if(((HashSet) listPossibleContentsMap.get(currentVal)).contains(letter)){
  				return true;
			}
			else {
				return false;
			}
    	}	
    
    }
    
    
    /**
     * checks the correctness of the tags and attributes of the rule file. In case
     * a missmatch was found, an error massage is generated. 
     * @param tag tags of the rule file (e.g. action, attribute)
     * @param attr attributes of the rule file (e.g. slot4, root)
     * @param num number of the checked rule
     */
    
    public void checkAttributes(String tag, String attr,String num) throws IllegalArgumentException{   // tag <-- e.g. action attr <-- e.g. slot4, root
    	try{
    		NodeList rules = ((Element) slotMap.get("specifications")).getElementsByTagName("rule");
    		Element next = (Element) rules.item(0);
    		if (DomUtils.getFirstElementByTagName(next,tag) == null) {  //tag not found
    			throw new IllegalArgumentException ("Possible typo in XML rules at rule numer: "+num+ " : "+tag);
    		}
            NamedNodeMap attNodes = DomUtils.getFirstElementByTagName(next,tag).getAttributes(); // map of all attributes
            if(attNodes.getNamedItem(attr) == null){            // attribute = slot1, slot2...
                throw new IllegalArgumentException ("Possible typo in XML rules at rule numer: "+num+ " : "+attr);
            }
    	}
    	catch (NullPointerException e1){
    		throw new IllegalArgumentException ("Syntax Error :\n "+e1.getCause());
    	}
    }
    
    /**
     * Build the lexicon
     * @throws Exception if there are problems reading the file
     */ 
    public void buildLexicon ()throws Exception {
    	try{
    		lexicon = Collections.synchronizedMap(new HashMap());
            BufferedReader lex = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("lexicon_tib.txt")));
    		String line;
    		while ((line = lex.readLine()) != null) {
    			// Ignore empty lines and comments:
    			if (line.trim().equals("") || line.startsWith("#"))
    				continue;
    			StringTokenizer st = new StringTokenizer(line, "\\");
    			// In the lexicon, the first field is wylie, the second is sampa, the third the translation.
    			// Only the wylie and the sampa are read in
    			String key = st.nextToken();
    			String value_sampa = st.nextToken();
    			String value_tone = st.nextToken();
    			// to ckeck whether a tone is on 3rd possition in the lexicon
    			String[] result = value_tone.split("#");
    			for (int i=0; i < result.length;i++){
    				boolean res = comparePossibleContent(result[i], "tone", "action");
    				//throw an exception if there is neither a "-" nor a valid tone at 3rd position,
    				if (!result[i].equals("-") && res==false){
    					throw new IllegalArgumentException ("Possible typo in lexicon at word: \""+key+"\" at tone: "+result[i]);
    				}
    			}	
    			lexicon.put(key, value_tone);
      		}
    	}
    	catch (NullPointerException e1){
    		throw new IllegalArgumentException ("Syntax Error :\n "+e1.getCause());
    	}
    }
    
    /**
     * Look up a token in the lexicon
     * @param text the text of the token
     * @return a string array of the entry, or null, if text is 
     * not in lexicon
     */
    public String[] lookUp(String text){
    	//set result to null
    	String[] result = null;
    	//if text is in lexicon
    	if (lexicon.containsKey(text)){
    		//get the entry
    		String newText = (String) lexicon.get(text);
    		//split entry into syllables and store in result
       		result = newText.split("#");}
    	return result;
    }

}
