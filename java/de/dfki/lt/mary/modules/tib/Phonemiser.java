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
 * The Tibetan Phonemiser
 * First checks, if the tokens are defined in the lexicon (lexicon_tib.txt)
 * If not, letter-to-sound rules (defined in syllableRules.xml) are applied
 * Created on 24.05.2005
 * @author Anna Hunecke
 */

public class Phonemiser extends InternalModule {


    /**
     * Constructor
     */
    public Phonemiser() {
        super("TibetanPhonemiser", MaryDataType.get("PARSEDSYL_TIB"), MaryDataType.get("PHONEMISED_TIB"));
    }
    
    //  document that is parsed 	
    private Document doc; 
    //  map that will be filled with the definitions, onset rules and rhyme rules
    private HashMap slotMap = new HashMap(); 
    //  map that will contain the lists  
    private HashMap listMap = new HashMap(); 
    //  map that will contain the maps 
    private HashMap mapMap = new HashMap(); 
    //Set of the wylie symbols allowed
    private HashSet wylieSymbols = new HashSet();
    //Set of the sampa symbols allowed
	private HashSet sampaSymbols = new HashSet();
	//Set of the conditions allowed
	private HashSet conditions = new HashSet();
    //the dictionary
	private Map lexicon;
	
    /**
     * Read in the data of the xml file; test if list, maps and rules are correct;
     * build the lexicon
     * @throws an Exception, if there are Errors in the xml file
     */
    public void startup() throws Exception {
        super.startup();
        loadSlotRules(); // fill the rule map
        buildDefinitions();  //fill in wylieSymbols, sampaSymbols and conditions
        buildListMap(); // fill the list map
        testFailDefinitions(); //test if there are Errors in definitions
        //test if there are Errors in the rules
        testFailRules(((Element) slotMap.get("onset")).getElementsByTagName("rule"));
        testFailRules(((Element) slotMap.get("rhyme")).getElementsByTagName("rule")); 
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
    	Document slotRules = b.parse(getClass().getResourceAsStream("syllableRules.xml"));
    	Element root = slotRules.getDocumentElement();
    	//put the nodes general, definitions, rhyme and onset in slotMap
    	for (Element e = MaryDomUtils.getFirstChildElement(root);
	     	e != null;
	     	e = MaryDomUtils.getNextSiblingElement(e)){ 
    			slotMap.put(e.getTagName(),e);
    		}
    	}
    
    /**
     * Read in the general definitions of wylie-symbols,
     * sampa-symbols and conditions
     */  
    public void buildDefinitions(){
    	//get the general definitions from slotMap
    	Element e = (Element) slotMap.get("general");
    	NodeList lists = e.getElementsByTagName("list");
    	//and  iterate over the lists
		for (int i = 0; i< lists.getLength(); i++){
				Element next = (Element) lists.item(i);
				String name = next.getAttribute("name");
				String items = next.getAttribute("items"); 
				HashSet itemSet = new HashSet(); // build a set with the elements in the list
				StringTokenizer st = new StringTokenizer(items,"#");
				while(st.hasMoreTokens()) {
					itemSet.add(st.nextToken());
				}
				//determine which list you just read and 
				//put it into appropriate variable
				if (name.equals("wylie_symbols")){
					wylieSymbols = itemSet;}
				else {
					if (name.equals("sampa_symbols")){
						sampaSymbols = itemSet;}
					else {conditions = itemSet;}
				}
		}
	}
    
    
    
    /**
     * Store the lists in the definitions section of the 
     * xml file in listMap and the maps in mapMap
     */
    private void buildListMap()
    {
    	//get the definitions
    	Element listDefinitions = (Element) slotMap.get("definitions");
    	// search for entries with tag "list"
    	TreeWalker tw =
    		((DocumentTraversal) listDefinitions.getOwnerDocument()).createTreeWalker(
    			listDefinitions,
				NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(new String[] {"list"}),
				false);
    	//iterate over the lists, read in the names and items
    	//and store them in listMap
    	Element list=null;
    	while((list = (Element) tw.nextNode()) != null) {
    		String name = list.getAttribute("name"); // list name
    		String items = list.getAttribute("items"); //items of the list
    		HashSet itemSet = new HashSet(); // build a set with the elements in the list
    		StringTokenizer elements = new StringTokenizer(items,"#");
    		while(elements.hasMoreTokens()) {
    			itemSet.add(elements.nextToken());
    		}
    		listMap.put(name,itemSet); // put the set in the map
       	}
    	//get the entries with tag "map"
    	TreeWalker tw2=
    		((DocumentTraversal) listDefinitions.getOwnerDocument()).createTreeWalker(
    			listDefinitions,
				NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(new String[] {"map"}),
				false);
    	//iterate over the maps, read in the names, keys and values
    	//and store them in mapsMap
    	Element map=null;
    	while((map = (Element) tw2.nextNode()) != null) {
    		String name = map.getAttribute("name"); // map name
    		String items = map.getAttribute("items"); //the items of the map
    		HashMap itemMap = new HashMap(); 
    		// build a set of the key/value pairs in the map
    		String[] pairs = items.split("\\|");
    		for (int i=0; i< pairs.length; i++) {
    			String[] onePair = pairs[i].split("#");
    			itemMap.put(onePair[0],onePair[1]);
    		}
    		mapMap.put(name,itemMap); // put the map in the map
      	}
    }
    
	/**
	 * Test, if the lists and maps in the definitions-section 
	 * are correct
	 * @throws IllegalArgumentException, if the definitions are not correct
	 */  
    public void testFailDefinitions(){
    	//test, if lists contain correct Wylie-Symbols:
    	
    	for (Iterator it = listMap.keySet().iterator(); it.hasNext();){
    		String name = (String) it.next();//for each list
    		HashSet next = (HashSet) listMap.get(name); //get the name
    		for (Iterator it2 = next.iterator(); it2.hasNext();){
    			String ws = (String) it2.next();//for each item
    			if (!wylieSymbols.contains(ws)){//if it is not a Wylie symbol, throw Exception
    				throw new IllegalArgumentException (name+" contains invalid Wylie-Symbol "+ws);}
    		}
    	}
    	//test, if maps contain correct Wylie/Sampa pairs	
    	for (Iterator it = mapMap.keySet().iterator(); it.hasNext();){
    		String name = (String) it.next();//for each map
    		HashMap next = (HashMap) mapMap.get(name);//get the name
    		for (Iterator it2 = next.keySet().iterator(); it2.hasNext();){
    			String key = (String) it2.next();//for each key
    			String value = (String) next.get(key);//get the value
    			if (!wylieSymbols.contains(key)){//if key is not a wylie symbol, throw Exception
    				throw new IllegalArgumentException (name+" contains invalid Wylie-Symbol "+key);}
    			else {
    				if (!sampaSymbols.contains(value)){//if value is not a sampa symbol, throw Exception
        				throw new IllegalArgumentException (name+" contains invalid Sampa-Symbol "+value);}
    			}
    		}
    	}
    	
    }
    
    /**
     * Test, if the rules in the NodeList are correct
     * @param rules the NodeList of rules
     * @throws IllegalArgumentException, if there is a rule without a number
     */
    public void testFailRules(NodeList rules){
    	//iterate over the rules
    	for (int i = 0; i< rules.getLength(); i++){
    		Node rule = rules.item(i);
    		//if the rule has no number, throw an error
    		if (!((Element)rule).hasAttribute("num")){
    			throw new IllegalArgumentException ("Missing rule number");}
    		String num = ((Element)rule).getAttribute("num");
    		//go through the children and test them
    		//(an Exception is thrown, if one of them contains an Error)
    		HashSet atts = new HashSet();//collect the attributes of the attributes node in this set
    		//test, if attributes is allright
    		Element attributes = DomUtils.getFirstElementByTagName(rule,"attributes");
    		if (!(attributes==null)){
    			atts = testFailAttributes(attributes, num);
    		}
    		//test, if nextattributes is allright
    		Element nextattributes = DomUtils.getFirstElementByTagName(rule,"nextattributes");
    		if (!(nextattributes==null)){
    			testFailAttributes(nextattributes, num);
    		}
    		//test, if prevattributes is allright
    		Element prevattributes = DomUtils.getFirstElementByTagName(rule,"prevattributes");
    		if (!(prevattributes==null)){
    			testFailAttributes(prevattributes, num);
    		}
    		//test, if condition is allright
    		Element condition = DomUtils.getFirstElementByTagName(rule,"condition");
    		if (!(condition == null)){
    			testFailCondition(condition,num);
    		}
    		//test if action is allright, given the attributes of the attributes node
    		Element action = DomUtils.getFirstElementByTagName(rule,"action");
    		testFailAction(action,num,atts);
    		}
    }
    
    /**
     * Test, if the attributes node is correct
     * Is also used for nextattributes and prevattributes
     * @param att the attributes node
     * @param num the number of the rule
     * @return a set of the attributes
     * @throws IllegalArgumentException, if the attributes node is not correct
     */
    public HashSet testFailAttributes(Element att, String num){
    	try{
    		HashSet result = new HashSet();
    		NamedNodeMap attNodes = att.getAttributes();
    		//loop over attributes in attributes node
    		for(int z=0; z<attNodes.getLength(); z++) { 
    			Node el = attNodes.item(z);
    			String val = el.getNodeValue();
    			String name = el.getNodeName();
    			//if the attribute refers to a map, verify that this map exists
    			if (val.indexOf("INKEYS")!=-1){
    				String[] s = val.split(":");
    				if (!mapMap.containsKey(s[1])){
    					throw new IllegalArgumentException("Rule "+num+":\n"+name
    								+" refers to invalid map name "+s[1]);}
    			}	
    			else {
    				//if the attribute refers to a list, verify that this list exists
    				if (val.indexOf("INLIST")!=-1){
    					String[] s = val.split(":");
    					if (!listMap.containsKey(s[1])){
    						throw new IllegalArgumentException("Rule "+num+":\n"+name
    								+" refers to invalid list name "+s[1]);}
    				}
    				else {
    					//if the attribute is a wylie-symbol, verify that it is a correct symbol
    					if (!(val.indexOf("EMPTY")!=-1) && !wylieSymbols.contains(val)){
    						throw new IllegalArgumentException("Rule "+num+":\n"+name
    								+" contains invalid Wylie-Symbol "+val);}
    				}
    			}
    			//add the attribute to the attribute set
    			result.add(name);
    		}
    		return result;
    		}
    		//catch NullPointerException and throw IllegalArgumentException
    		catch (NullPointerException e1){
    			throw new IllegalArgumentException ("Syntax Error in attributes of rule "+num+":\n"+
    					e1.getCause());}
    		//catch ArrayIndexOutOfBoundsException and throw IllegalArgumentException
    		catch (ArrayIndexOutOfBoundsException e2){
    			throw new IllegalArgumentException ("Syntax Error in attributes of rule "+num+":\n"+
    					e2.getCause());}
    }
    
    /**
     * Test, if a condition node is correct
     * @param cond the condition node
     * @param num the number of the rule
     * @throws IllegalArgumentException, if the condition node is not correct
     */
    public void testFailCondition(Element cond, String num){
    	try{
	   		NamedNodeMap attNodes = cond.getAttributes();
	  	    //loop over the attributes of the condition node
	    	for(int z=0; z<attNodes.getLength(); z++){
    			Node el = attNodes.item(z);
    			String name = el.getNodeName();
    			//check, if the condition is valid
    			if (!conditions.contains(name)){
		    	throw new IllegalArgumentException ("Rule "+num+":\n"+name+" is an invalid condition");}
	    	}
    	}
    	//catch NullPointerException and throw IllegalArgumentException
    	catch (NullPointerException e1){
	    	throw new IllegalArgumentException ("Syntax Error in condition of rule "+num+":\n"+
						e1.getCause());}
		//catch ArrayIndexOutOfBoundsException and throw IllegalArgumentException
    	catch (ArrayIndexOutOfBoundsException e2){
	    	throw new IllegalArgumentException ("Syntax Error in condition of rule "+num+":\n"+
						e2.getCause());}
    	
    }
    
    /**
     * Test, if an action node is correct
     * @param act the action node
     * @param num the number of the rule
     * @param atts the attributes of the attributes node of the rule
     * @throws IllegalArgumentException, if the action node is not correct
     */
    public void testFailAction(Element act, String num, HashSet atts){
    	try{
    		String toDo = act.getAttribute("sampa");
    		//if the sampa consists of several parts
    		if (toDo.indexOf("+")!=-1){
    			String[] st = toDo.split("\\+");
	    		//iterate over all sampa specifications
	    		for (int i = 0; i< st.length; i++){
	    			//check if the definition is correct
	    			checkSampaDef(num,st[i],atts);
	        		}
	    	}
    		//else the sampa is defined in one expression
    		else{
    			//check, if toDo is correct
    			checkSampaDef(num,toDo,atts);
    		}
    	}
    	//catch NullPointerException and throw IllegalArgumentException
    	catch (NullPointerException e1){
    		throw new IllegalArgumentException ("Syntax Error in action of rule "+num+":\n"+
    				e1.getCause());}
    	//catch ArrayIndexOutOfBoundsException and throw IllegalArgumentException			
    	catch (ArrayIndexOutOfBoundsException e2){
    		throw new IllegalArgumentException ("Syntax Error in action of rule "+num+":\n"+
    				e2.getCause());}
    
    }
    
    /**
	 * Check a sampa definition of an action node
	 * @param num the name of the current rule
	 * @param sampa the sampa definition
	 * @param atts the attributes of the rule (from attribute node)
	 * @throws IllegalArgumentException if definition not correct
	 */	
    
    public void checkSampaDef(String num, 
    						String sampa, 
    						HashSet atts)
    						throws IllegalArgumentException{
    	//if sampa is a reference to a map
	    if (sampa.indexOf("MAP")!=-1){  	
    	   	String[] next = sampa.split(":");
    	    //if the attributes do not contain the key
    	   	if (!atts.contains(next[1])){
    	   		//if the map is also not valid, throw exception
    			if (!mapMap.containsKey(next[2])){
    	    		throw new IllegalArgumentException ("Rule "+num+":\nInvalid key "+next[1]+
    	    				" and invalid map "+next[2]+" in action");}
    	    	//else throw another exception
    	    	else {
    	    		throw new IllegalArgumentException ("Rule "+num+":\nInvalid key "+next[1]+
    		    			" in action");}
    	    	}
    	    //if the map is not valid, throw exception
    	    if (!mapMap.containsKey(next[2])){
    	    	throw new IllegalArgumentException ("Rule "+num+":\nInvalid map "
    	    			+next[2]+" in action");}
    	    }
    	    //else it is a sampa string, check if it is valid		
	    		else {
	    			String[] sampaArray = sampa.split("");
	        		for (int j = 1; j<sampaArray.length; j++){
	        			if (!sampaSymbols.contains(sampaArray[j])){
	        				throw new IllegalArgumentException ("Rule "+num+":\nInvalid Sampa-Symbol "+sampaArray[j]+" in action");}
	        		}
	        	}
    }	 
    
    /**
     * Build the lexicon
     * @throws Exception if there are problems reading the file
     */ 
    public void buildLexicon ()throws Exception{
    	lexicon = Collections.synchronizedMap(new HashMap());
    	BufferedReader lex = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("lexicon_tib.txt")));
    	
    	String line;
    	while ((line = lex.readLine()) != null) {
    		// Ignore empty lines and comments:
    		if (line.trim().equals("") || line.startsWith("#"))
    			continue;
    		StringTokenizer st = new StringTokenizer(line, "\\");
    		// In the lexicon, the first field is wylie, the second is sampa, the third the translation.
    		// Only the wylie and the sampa are read in
    		lexicon.put(st.nextToken(), st.nextToken());
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
        //store the result in a new MaryData object and return it
        MaryData result = new MaryData(outputType());
        result.setDocument(doc);
        return result;
    }
    
    /**
     * process a token of the data
     * @param token the syllable to be processed
     */
    public void processToken (Element token){
    	//get the token text and look it up in the lexicon
      	String text = MaryDomUtils.tokenText(token);
    	String[] result = lookUp(text);
        assert result == null || result.length > 0;
    	//get the syllables
    	NodeList syllables = token.getElementsByTagName(MaryXML.SYLLABLE);
    	//build a new buffer to store the sound of the entire token
    	StringBuffer tokenSound = new StringBuffer();
    	//store the length of the word
    	int max = syllables.getLength();
    	//loop over the syllables
    	for (int i=0; i<max; i++) {
    		Element syllable = (Element) syllables.item(i);
    		//if token is not in lexicon
    		if (result == null){
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
    			//apply the onset and rhyme rules
    			String onsetSampa = applyRules(((Element) slotMap.get("onset")), syllable, nextSyl, prevSyl, i, max-1); 
    			String rhymeSampa = applyRules(((Element) slotMap.get("rhyme")), syllable, nextSyl, prevSyl, i, max-1);
    			//if this is the first syllable, add an accent to the token sound
    			//plus onset and rhyme sound 
    			if (i==0){
    				tokenSound.append("'"+onsetSampa+rhymeSampa);}
    			//else add a "-" plus onset and rhyme sound
    			else {tokenSound.append("-"+onsetSampa+rhymeSampa);}
    			//store onset and rhyme sound also in the syllable node
    			syllable.setAttribute("sampa", onsetSampa+rhymeSampa);
    		}
    		//else token is in lexicon
    		else {
    			//if this is the first syllable, add an accent 
    			//plus the syllable sound to the token sound
    			if (i==0){
    				tokenSound.append("'"+result[i]);}
    			//else add a "-" plus the syllable soudn
    			else {tokenSound.append("-"+result[i]);}
    			//store syllable sound also in the syllable node
    			syllable.setAttribute("sampa", result[i]);
    		}
    	}
    	//store the overall token sound in the token node
    	token.setAttribute("sampa", tokenSound.toString());
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
    		Element attributes = DomUtils.getFirstElementByTagName(next,"attributes");
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
    	String toDo = action.getAttribute("sampa");
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

    

}
