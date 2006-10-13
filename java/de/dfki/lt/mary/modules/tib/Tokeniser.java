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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.NoSuchPropertyException;
import de.dfki.lt.mary.modules.InternalModule;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/** This class tokenises the wylie-transcribed tibetan sentence
 *  in RAWMARYXML_tib into processed syllables in PARSEDSYL_tib, 
 * 	which is also in XML-format. 
 * 
 * @author Maria Staudte
 */
public class Tokeniser extends InternalModule {

	
	// ***************************************************************
	// Global Variables
	// ***************************************************************
	
	// String containing characters that separate sentences
	private String sentence_separators ="";
	// String containing characters that separate syllables
	private String syllable_separators ="";
	// String containing characters that start a paragraph
	private String paragraph_start ="";	
	// String containing characters that end paragraphs
	private String paragraph_end ="";
	// String containing all paragraph markers
	private String paragraph_markers ="";
    //map that will be filled with the lists and definitions
    private HashMap slotList = new HashMap(); 
    //Set of the wylie symbols allowed
    private HashSet wylieSymbols = new HashSet();
    //Set of the sampa symbols allowed
	private HashSet vowels = new HashSet();
	// map that will contain the lists  
	private HashSet consonants = new HashSet();
	// map that will contain the lists
    private HashMap listMap = new HashMap();  
    // List containing the lexicon
    private LinkedList lexicon = new LinkedList();
    // List containing partciles
    private HashMap particles = new HashMap();
    // MS, 20.09.05: Maximum length of wylie symbols
    private int wylieMaxlen = -1;
	
	
	// ***************************************************************
	// Constructors
	// ***************************************************************

    /** Constructor without arguments
     *  initialises the Data-inputTyes and -outputTypes
     */
    public Tokeniser () {
        super("TibetanTokeniser", MaryDataType.get("RAWMARYXML_TIB"), MaryDataType.get("PARSEDSYL_TIB"));
    }

    
	// ***************************************************************
	// Start-up & IO
	// ***************************************************************
    
    /**
     * Read in the data of the xml file
     */
    public void startup() throws Exception {
        super.startup();
        loadSlotDefinitions(); // fill the rule map
        buildAlphabet();  //fill in wylieSymbols
        buildListMap(); // fill the list map with slot definitions
        buildLexicon(); // fill lexicon list with xml-entries
        buildParticleList(); // fill particle list with xml-entries 
        assignSeparators(); // set the punctuation items
    }

    	
	/**
     * Read in the lists of the xml file and 
     * store them in slotList
     */
    public void loadSlotDefinitions () 
	throws FactoryConfigurationError, ParserConfigurationException, 
	       org.xml.sax.SAXException, IOException, NoSuchPropertyException {

    	// parsing the xml definition file
    	DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    	DocumentBuilder b = f.newDocumentBuilder();
    	Document slotRules = b.parse(getClass().getResourceAsStream("syllableSlotDefinitions.xml"));
    	
    	Element root = slotRules.getDocumentElement();
    	for (Element e = MaryDomUtils.getFirstChildElement(root);
	     	e != null;
	     	e = MaryDomUtils.getNextSiblingElement(e)){ 
    			slotList.put(e.getTagName(),e);
    		}
    	}
	
	/**
     * Read in the generally admitted wylie-symbols, and vowels 
     * and consonants in particular
     */  
    public void buildAlphabet(){
    	Element e = (Element) slotList.get("general");
    	NodeList lists = e.getElementsByTagName("list");
		for (int i = 0; i< lists.getLength(); i++){
				Element next = (Element) lists.item(i);
				String name = next.getAttribute("name");
				String items = next.getAttribute("items"); 
				HashSet itemSet = new HashSet(); // build a set with the elements in the list
				StringTokenizer st = new StringTokenizer(items,"%");
				while(st.hasMoreTokens()) {
                    String item = st.nextToken();
                    if (item.length() > wylieMaxlen) wylieMaxlen = item.length(); 
					itemSet.add(item);
				}
				if (name.equals("wylie_symbols")){
					wylieSymbols = itemSet;}
				else {
					if (name.equals("vowels")){
						vowels = itemSet;}
					else {consonants = itemSet;}
				}
		}
	}
    
    
    /**
     * Store the lists in the definitions section of the 
     * xml file in listMap
     */
    public void buildListMap()
    {
    		Element listDefinitions = (Element) slotList.get("definitions");
    		// get all entries with tag "list"
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
    				StringTokenizer st = new StringTokenizer(items,"%");
    				while(st.hasMoreTokens()) {
    					itemSet.add(st.nextToken());
    				}
    				listMap.put(name,itemSet); // put the set on the map
    			}
    		}
    }
    
    /**
     * Build the lexicon, i.e. read in all words in wylie
     * @throws Exception if there are problems reading the file
     */ 
    public void buildLexicon ()throws IOException{
    	lexicon = new LinkedList();
    	BufferedReader lex = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("lexicon_tib.txt")));
    	String line;
    	while ((line = lex.readLine()) != null) {
    		// Ignore empty lines and comments:
    		if (line.trim().equals("") || line.startsWith("#"))
    			continue;
    		StringTokenizer st = new StringTokenizer(line, "\\");
    		// In the lexicon, the first field is wylie, the second is sampa, the third the translation.
    		// Only the wylie word is needed here
    		lexicon.addLast(st.nextToken());
    	}
    }
    
    
    /**
     * Build the particle list
     * @throws IOException if there are problems reading the file
     */ 
    public void buildParticleList () throws IOException {
        BufferedReader part = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("particle_tib.txt")));
    	String line;
    	// store particles according to how they can occur
    	particles.put("e",new LinkedList());
    	particles.put("s",new LinkedList());
    	while ((line = part.readLine()) != null) {
    		// Ignore empty lines and comments:
    		if (line.trim().equals("") || line.startsWith("#"))
    			continue;
    		// Get 1.token (particle) and 2.token (indicator) of a line and ignore spaces
    		String[] tok = line.split("[\\s+/]");
    		((LinkedList)particles.get(tok[1])).addLast(tok[0]);
    	}
    }
    
    /**
     * Stores the punctuation items in the appropriate
     * Strings used by the StringTokeniser.
     */
    public void assignSeparators() {
    	String regexp1 = "";
    	String regexp2 = "";
    	String regexp3 = "";
    	String regexp4 = "";
    	// go through Sets and append all items to a String, add a + for each
    	// to build a (overgenerating) regular expression that can itself occur
    	// one or more times (+)
    	// Syllable delimiters
    	HashSet sylpunct = ((HashSet)listMap.get("syllable_separators"));
    	Iterator iterSyl = sylpunct.iterator();
    	while (iterSyl.hasNext()) {
    		//regexp1 = regexp1.concat((String)iterSyl.next()).concat("+");
    		regexp1 = regexp1.concat((String)iterSyl.next());
    	}
    	syllable_separators = "["+regexp1+"]+";
    	// Sentence delimiters
    	HashSet sentpunct = ((HashSet)listMap.get("sentence_separators"));
    	Iterator iterSent = sentpunct.iterator();
    	while (iterSent.hasNext()) {
    		//regexp2 = regexp2.concat((String)iterSent.next()).concat("+");
      		regexp2 = regexp2.concat((String)iterSent.next());
    	}
    	sentence_separators = "["+regexp2+"]+"; 	
    	// Paragraph delimiters: start
    	HashSet para_start = ((HashSet)listMap.get("paragraph_start"));
    	Iterator iterPara = para_start.iterator();
    	while (iterPara.hasNext()) {
    		regexp3 = regexp3.concat((String)iterPara.next());
    	}
    	paragraph_start = "["+regexp3+"]+";
    	logger.info("RegExp for paragraph start: "+paragraph_start);
    	// Paragraph delimiters: end    	
    	HashSet para_end = ((HashSet)listMap.get("paragraph_end"));
    	Iterator iterPara2 = para_end.iterator();
    	while (iterPara2.hasNext()) {
    		regexp4 = regexp4.concat((String)iterPara2.next());
    	}
    	paragraph_end = "["+regexp4+"]{2,}";
    	// Combine start- & end- paragraph markers
    	paragraph_markers = paragraph_end+"\\s+"+paragraph_start;
    }
    
    // ***************************************************************
	// Computation Methods
	// ***************************************************************

    /** This method extracts the text to be parsed from the XML-Document
     *  and places the calls to parseSen and parseSyll in order to
     *  get the parsed result of that sentence.
     *  This is in xml-form and is then appended to the MaryData result.
     *  @param d XML-inputType
     *  @return result, new MaryData of outputType 
     */
	public MaryData process(MaryData d) throws Exception
	{
		MaryData result = new MaryData(outputType());
		
		Document doc = d.getDocument();
		NodeIterator ni = ((DocumentTraversal)doc).createNodeIterator(
	            doc, NodeFilter.SHOW_TEXT, null, false);
	    Text textNode;
	    while ((textNode = (Text)ni.nextNode()) != null) {
            String text = textNode.getData().trim();
            Element parent = (Element) textNode.getParentNode();
            // before splitting the text into paragraphs
        	// find and store the punctuation marks
            HashMap map = getParaDelims(text);
            HashMap regexpsParaEnd = (HashMap)map.get("end");
            HashMap regexpsParaStart = (HashMap)map.get("start");
            logger.info("paragraph markers: "+paragraph_markers);
    		
            // split text into paragraphs
            // first by rough (by combined paragraph start & end)
            String[] roughSplit = text.split(paragraph_markers);
            LinkedList paraArray = new LinkedList();
            for (int l=0;l<roughSplit.length;l++) {
            	// then by separate paragraph markers
            	String[] fineSplit = roughSplit[l].split("("+paragraph_end+"|"+paragraph_start+")");
            	for (int m=0;m<fineSplit.length;m++) {
            		paraArray.addLast(fineSplit[m]);
            		logger.info("fine split para no: "+m+" : "+ fineSplit[m]);
            	}
            }
            
            LinkedList trimmedParaArray = new LinkedList();
            int limit = paraArray.size();
            // trim, i.e. remove trailing empty paragraphs produces by split()
            if (((String)paraArray.getFirst()).length()<1) {
            	if (((String)paraArray.get(limit-1)).length()<1) limit=limit-1;
            	for (int c=1;c<limit;c++) {trimmedParaArray.add(c-1,(String)paraArray.get(c));
            	logger.info("paras: "+trimmedParaArray.get(c-1));
            	}
            } else if (((String)paraArray.get(limit-1)).length()<1) {
            	for (int c=0;c<limit-1;c++) {trimmedParaArray.add(c,(String)paraArray.get(c));
            	logger.info("paras: "+trimmedParaArray.get(c));
            	}
            } else {trimmedParaArray = paraArray;}
            
            // for each paragraph
            String paraText;
            for (int i=0;i<trimmedParaArray.size();i++) {
            	paraText = (String)trimmedParaArray.get(i);
            	logger.info("single para: "+paraText);
            	Element paraElement= MaryXML.createElement(doc,MaryXML.PARAGRAPH);
            	parent.insertBefore(paraElement,textNode);
            	// before splitting the paragraph into sentences
            	// find and store the punctuation marks
            	LinkedList regexpsSentEnd = getSentenceDelims(paraText);
        		// parse paragraph into sentences
        		// but be sure that the delimiter strings are not null
            	logger.info("show end & start: "+(String)regexpsParaEnd.get(new Integer(i))+" / "+(String)regexpsParaStart.get(new Integer(i)));
        		if (regexpsParaStart.get(new Integer(i))!=null) {
        			if (regexpsParaEnd.get(new Integer(i))!=null) {
        				parseParagraph(paraElement, paraText, regexpsSentEnd,(String)regexpsParaStart.get(new Integer(i)),(String)regexpsParaEnd.get(new Integer(i)));
        			} else {parseParagraph(paraElement, paraText, regexpsSentEnd, (String)regexpsParaStart.get(new Integer(i)), "");}
        		} else {
        			if (regexpsParaEnd.get(new Integer(i))!=null) {
        				parseParagraph(paraElement, paraText, regexpsSentEnd,"", (String)regexpsParaEnd.get(new Integer(i)));
        			} else {parseParagraph(paraElement, paraText, regexpsSentEnd, "", "");}
        		}
            } // end for-loop
            // parent.removeChild(textNode);
        }// end while
        result.setDocument(doc);
        return result;
        
	} // end process()
	
	
	/** This method searches the given text for occurrences of the 
	 *  paragraph-delimiter regular expression and stores these 
	 *  in a hashmap 
	 * 
	 * @param text to be searched
	 * @return HashMap containing the two delimiter maps,
	 *  start- & end- paragraph markers
	 */
	public HashMap getParaDelims(String text) {

		String copyText = text.substring(0,text.length());
		HashMap regexpsParaEnd = new HashMap();
		HashMap regexpsParaStart = new HashMap();
		HashMap mapMap = new HashMap();
		int endPara; // steps through paragraph 
		int startPara; 
		int countPara = 0; // array pointer for storing start/end markers
		boolean foundMatch = false; // indicates whether match was found
		boolean position = true; // indicates paragraph marker position (start/end)
		// Go through paragraph
		while (copyText.length()>1) {
			startPara = 0; endPara = 1;
   			String regionMatchPar = "";
   			foundMatch = false;
   			// find the whole sequence of punctuation marks
   			while((copyText.length()>=endPara)) {
   				// paragraph end is marked by e.g. at least two slashes
   				if ((copyText.substring(startPara,endPara+1)).matches(paragraph_end)) {
   					regionMatchPar = copyText.substring(startPara,endPara+1);
   					endPara++;
   					position=true; foundMatch=true;
   				} else if ((copyText.substring(startPara,endPara)).matches(paragraph_start)) {
   					regionMatchPar = copyText.substring(startPara,endPara);
   					endPara++;
   					position=false; foundMatch=true;
   				}else {break;}
   			}
   			// store Strings in an array according to their position in paragraph
   			if (foundMatch==true && position==true) {regexpsParaEnd.put(new Integer(countPara),regionMatchPar);countPara++;}
   			else if (foundMatch==true && position==false) {regexpsParaStart.put(new Integer(countPara),regionMatchPar);}
   			// move text window forward
   			if (copyText.length()>endPara) copyText = copyText.substring(endPara);
   			else break;
		} // end while text has more tokens
		
		mapMap.put("start",regexpsParaStart);
		mapMap.put("end",regexpsParaEnd);
		return mapMap;
	} // end getParaDelims()
	
	
	
	/** This method searches the given text for occurrences of the 
	 *  sentence-delimiter regular expression and stores these 
	 *  in a list 
	 * 
	 * @param paraText - paragraph text to be searched
	 * @return LinkedList containing the found delimiters
	 */
	public LinkedList getSentenceDelims(String paraText) {
		String search = paraText.substring(0,paraText.length());
		LinkedList regexpsSentEnd = new LinkedList();
		String step2 = "";
		int endSent; // steps through sentence
		int startSent;
		int countSent = 0; // count number of sentences
		// Go through paragraph
		while (search.length()>0) {
			startSent = 0; endSent = 1;
    		step2 = search.substring(startSent,endSent);
    		// stop if a sentence separator was found
    		if (sentence_separators.indexOf(step2)!=-1) {
    			String regionMatchSent = "";
    			// find the whole sequence of punctuation marks
    			while(search.length()>=endSent) {
    				if ((search.substring(startSent,endSent)).matches(sentence_separators)) {
    					regionMatchSent = search.substring(startSent,endSent);
    					endSent++;
    				}else {break;}
    			}
    			// store Strings in dynamic list
    			regexpsSentEnd.add(countSent,regionMatchSent);
    			countSent++;
    		} 
    		if (search.length()>endSent) search = search.substring(endSent);
    		else break;
		} // end while
		return regexpsSentEnd;
	} // end getSentenceDelims()
	
	
	
	/** This method parses a given sentence into syllables and 
	 *  calls the parseSyllable-method for each token 
	 * @param paraElement to which syllable are appended
     * @param paraText
     * @param regexpsEnd
     * @param paraStart
     * @param paraEnd
	 */
	public void parseParagraph(Element paraElement, String 
			paraText, LinkedList regexpsEnd, String paraStart, String paraEnd)
			     {
		String[] sentenceArray = paraText.split(sentence_separators);
		
		int limit = sentenceArray.length;
		String[] trimmedSentArray;
		// if split produces a last empty sentence remove it
        if (sentenceArray[limit-1].length()<1) {
        	trimmedSentArray = new String[limit-1];
        	for (int c=0;c<limit-1;c++) {trimmedSentArray[c]=sentenceArray[c];}
        } else {trimmedSentArray = sentenceArray;}
		
        // for each sentence...
		Iterator sentIter = regexpsEnd.iterator();
        for (int j=0;j<trimmedSentArray.length;j++) {
        	
            String sentenceText = trimmedSentArray[j].trim();
            logger.info("sentence: "+sentenceText);
            Element sentenceElement = MaryXML.appendChildElement(paraElement, MaryXML.SENTENCE);
            // if regexpsEnd/sentIter contains a sentence boundary pass it else an empty String
            if (sentIter.hasNext()) {
            	// if there is only one sentenceare in a paragraph pass paraStart and paraEnd
            	if (sentenceArray.length==1) {
            		parseSentence(sentenceElement, sentenceText, (String)sentIter.next(),paraStart, paraEnd);
                // if we are in a first sentence of a paragraph pass paraStart
            	} else if (j==0) {
            		parseSentence(sentenceElement, sentenceText, (String)sentIter.next(),paraStart, "");
            	// if we are in the last pass paraEnd	
            	} else if (j==sentenceArray.length-1) {
            		parseSentence(sentenceElement, sentenceText, (String)sentIter.next(),"",paraEnd);
            	} else { // MS: added, 20.09.05
                    parseSentence(sentenceElement, sentenceText, (String)sentIter.next(),"","");
                }
            } else {
            	// if there is only one sentenceare in a paragraph pass paraStart and paraEnd
            	if (sentenceArray.length==1) {
            		parseSentence(sentenceElement, sentenceText, "",paraStart, paraEnd);
                // if we are in a first sentence of a paragraph pass paraStart
            	} else if (j==0) {
            		parseSentence(sentenceElement, sentenceText, "",paraStart, "");
            	// if we are in the last pass paraEnd
            	} else if (j==sentenceArray.length-1) {
            		parseSentence(sentenceElement, sentenceText, "","",paraEnd);
            	} 
            } // end if there is a sentence boundary
        } // end for-loop
	} // end parseParagraph()
	
	
	/** This method parses a given sentence into syllables and 
	 *  calls the parseSyllable-method for each token 
	 * @param sentenceElement to which syllables are appended
     * @param sentenceText
     * @param sentEnd
     * @param paraStart
     * @param paraEnd
	 */
	public void parseSentence(Element sentenceElement, String 
			sentenceText, String sentEnd, String paraStart, String paraEnd)
			     {
		
		String[] syllableArray = sentenceText.split(syllable_separators);
		HashMap wordMap = new HashMap();
		String[] words;
		// first add paragraph opening token if existing
		if (paraStart.length()>0) {
			Element separatorElement1 = MaryXML.appendChildElement(sentenceElement, MaryXML.TOKEN);
			separatorElement1.setAttribute("separator", paraStart);
		}
		// first find all multi-syllabic words
        for (int j=0;j<syllableArray.length;j++) {
            String sylText = syllableArray[j];
            // if particle of type "stem" was found (s) search for possible word
            int syllcount = 1; // number of syllables beloning to one word
            if (((LinkedList)particles.get("s")).contains(sylText)) {
            	logger.info("found particle: "+sylText);
            	syllcount = checkWord(sylText,syllableArray,j);
            	if (syllcount > 1) {
            		words = new String[syllcount];
            		int pos = j-syllcount+1;
            		// count backwards starting at particle and add belonging syllables
            		for (int i=0;i<syllcount;i++) {
            			words[i] = syllableArray[pos++];
            		}
            		wordMap.put(new Integer(j-syllcount+1),words);
            		logger.info("store found word-syls: "+wordMap.entrySet().toString());
            	}
            } // end if particle found
        } // end for-loop through syllable array
        
        // then add all words and the syllables in between
        int scan = 0;
        while (scan<syllableArray.length) {
        	// if this syllable isn't part of a word add separate token
        	if (wordMap.get(new Integer(scan))==null) {
        		String sylText = syllableArray[scan];
        		Element token = MaryXML.appendChildElement(sentenceElement, MaryXML.TOKEN);
        		parseSyllable(token, sylText);
    			MaryDomUtils.setTokenText(token, sylText);
    			scan++;
        	}
        	// else add one token which contains all syllable-elements
        	else {
        		logger.info("Word at: "+scan);
        		Element token = MaryXML.appendChildElement(sentenceElement, MaryXML.TOKEN);
        		String[] word = (String[])wordMap.get(new Integer(scan));
                StringBuffer tokenText = new StringBuffer();
        		for (int j=0;j<word.length;j++)  {
        			String sylText = word[j];
        			parseSyllable(token, sylText);
        			tokenText.append(sylText);
                    tokenText.append(" ");
        		}
                MaryDomUtils.setTokenText(token, tokenText.toString().trim());
        		scan+=word.length;
        	} // end if word or syllable 
        } //end while adding all syllable and word tokens
        
		// append sentence delimiters
        if (sentEnd.length()>0) {
			Element separatorElement2 = MaryXML.appendChildElement(sentenceElement, MaryXML.TOKEN);
			separatorElement2.setAttribute("separator", sentEnd);
        }
        // add paragraph ending tokens if existing
        if (paraEnd.length()>0) {
        	Element separatorElement3 = MaryXML.appendChildElement(sentenceElement, MaryXML.TOKEN);
			separatorElement3.setAttribute("separator", paraEnd);
        }
	}
    
	
	/** This method parses a given syllable into
	 *  its slots 1-5 and shows the distribution of each
	 *  character onto the slots. The resulting slot elements are 
	 *  appended to an xml-document. 
	 * @param token 
     * @param sylText 
	 */
	public void parseSyllable(Element token, String sylText)
    {
        Element sylElement = MaryXML.appendChildElement(token, MaryXML.SYLLABLE);
        // and now the logic for parsing the syllable structure...
        try {
        	LinkedList syllable = preprocessSyll(sylText);
        
			for (int i=0;i<syllable.size();i++){
				String current = (String)syllable.get(i);
				// when vowel found check before it: 
				// in principle -> root but if slot3-consonant found maybe slot3  
				if (vowels.contains(current)) {
					if (((HashSet)listMap.get("slot3_consonants")).contains(syllable.get(i-1))) {	
						// chech whether its root or slot3 and then get prefix
						checkRoot((String)syllable.get(i-1),syllable,sylElement);
					} // else root is before vowel, get prefix
					else {
						checkPrefix((String)syllable.get(i-1),syllable,sylElement);
						sylElement.setAttribute("root", (String)syllable.get(i-1));
					}
					// set vowel
					sylElement.setAttribute("vowel",current);
					// if after the vowel there are more chars, check for suffix
					if (i<syllable.size()-1) {
						checkSuffix((String)syllable.get(i),syllable,sylElement);
					}
					break;
				}
			} // end for-loop
	
	    } catch (IllegalArgumentException e) {
	    	logger.error("Error in Tokeniser: "+e.getMessage());
	    }
	} //end parseSyll
	
	
	
	/** This method checks for a given char whether it is
	 *  the root or possibly a slot3-filler such that
	 *  the actual root is before it.
	 *  When root is identified whatever is before the root, 
	 *  i.e. the prefix is examined by checkPrefix
	 * @param root The (suggested) root
	 * @param syllable LinkedList of Strings for further processing
     * @param sylElement
	 * @throws IllegalArgumentException for unparseable syllable structure
	 */
	public void checkRoot(String root, LinkedList syllable,Element sylElement) {

		for (int i=0;i<syllable.size();i++) {
			String current = (String)syllable.get(i);
			// loop until suggested root is reached
			if (current.equals(root)) {
			  // in case there are letters before suggested root
			  if (i>0) { 
				// if it is slot3 determine root and check prefix with determined root
				if (((HashSet)listMap.get("slot3_consonants")).contains(current)) {
					// if dot is before suggested slot3, it is root instead
					// and letter before dot is slot1
					if (((String)syllable.get(i-1)).equals(".")) {
						sylElement.setAttribute("root", current);
						sylElement.setAttribute("slot1",(String)syllable.get(i-2));
					} // else slot3 is assumed and before it is the root 
					else {
						sylElement.setAttribute("root", (String)syllable.get(i-1));
						sylElement.setAttribute("slot3", current);
						// parse prefix with determined root
						try {
							if (i>1) checkPrefix((String)syllable.get(i-1),syllable,sylElement);
						} catch (IllegalArgumentException e) {
							throw e;
						}
					}
				}
				// if it's not slot3 it must be root
				else {
					sylElement.setAttribute("root", current);
				}
			  }
			  // if suggested root is first letter, set to root automatically
			  else {
			  	sylElement.setAttribute("root", current);
			  }
			  break;
			} //end if found root char
		} //end for-loop
	} //end checkRoot
	
	
	/** This method gets the syllable and the root character such that
	 *  it can extract the chars that precede the root
	 *  Those are checked for slot2 and slot1. 
	 * @param root the syllable root
	 * @param syllable LinkedList of Strings for further processing
     * @param sylElement 	
	 * @throws IllegalArgumentException for unparseable syllable structure
	 *  
	 **/
	public void checkPrefix (String root, LinkedList syllable, Element sylElement) {

		for (int i=0;i<syllable.size();i++) {
			String current = (String)syllable.get(i);
			// if root has letters before it (including implicit 'a')
			if (current.equals(root) && i>0) {
				// in wylie an implicit a of a prefix consonant can occur explicitely
				// if implicit 'a' before root: slot1/2 consonant is at i-2
                /* MS, 20.09.05: This is not true. The 'a' which is implicit in Tibetan writing
                 * is always explicit in wylie, and always follows root (+slot3);
                 * it never precedes root.
				if (((String)syllable.get(i-1)).equals("a")) {
					if (((HashSet)listMap.get("slot2_consonants")).contains(syllable.get(i-2))) {
						sylElement.setAttribute("slot2", (String)syllable.get(i-2));
					} else if (((HashSet)listMap.get("slot1_consonants")).contains(syllable.get(i-2))) {
						sylElement.setAttribute("slot1", (String)syllable.get(i-2));
					} else {
						throw new IllegalArgumentException(syllable.get(i-2)+" can neither be slot1 nor slot2: invalid structure!");
					}
				// else slot1/2 is at i-1
				} else */ 
                if (((HashSet)listMap.get("slot2_consonants")).contains(syllable.get(i-1))) {
					sylElement.setAttribute("slot2", (String)syllable.get(i-1));
                    // MS, 20.09.05: Forgot to check for slot1 if slot2 is present
                    if (i>=2 && ((HashSet)listMap.get("slot1_consonants")).contains(syllable.get(i-2))) {
                        sylElement.setAttribute("slot1", (String)syllable.get(i-2));
                    }
				} else if (((HashSet)listMap.get("slot1_consonants")).contains(syllable.get(i-1))) {
					sylElement.setAttribute("slot1", (String)syllable.get(i-1));
				} else {
					throw new IllegalArgumentException(syllable.get(i-1)+" is neither slot1 nor slot2: invalid structure!");
				}
                break; // leave for loop
			} 
		} // end for-loop
	} // end checkPrefix
	
	
	
	/** This method gets the syllable and the vowel such that
	 *  it can extract the chars that follow the vowel
	 *  Those are checked for slot4 (and slot4vowel) and slot5. 
	 * @param root The syllable root letter
	 * @param syllable LinkedList of Strings for further processing
     * @param sylElement     
	 * @throws IllegalArgumentException for unparseable syllable structure
	 */
	public void checkSuffix (String root,LinkedList syllable, Element sylElement) {

		for (int i=0;i<syllable.size();i++) {
			String current = (String)syllable.get(i);
			// loop until root is reached
			if (current.equals(root)) {
				// if there is a suffix
				if (syllable.size() > i+1) {
					// slot4 and slot5
					if (((HashSet)listMap.get("slot4_consonants")).contains(syllable.get(i+1))) {
						sylElement.setAttribute("slot4", (String)syllable.get(i+1));
						// if there is a potential slot5 or stlot4vowel letter
						if (syllable.size() > i+2) {
							if (((HashSet)listMap.get("slot5_consonants")).contains(syllable.get(i+2))) {
								sylElement.setAttribute("slot5", (String)syllable.get(i+2));
							}
							// if there is a slot4vowel there may still be a slot5 letter
							else if (vowels.contains((String)syllable.get(i+2))) {
								sylElement.setAttribute("slot4vowel", (String)syllable.get(i+2));
								if ((syllable.size()>i+3) && ((HashSet)listMap.get("slot5_consonants")).contains(syllable.get(i+3))) sylElement.setAttribute("slot5", (String)syllable.get(i+3));
							}
							else {
								throw new IllegalArgumentException((String)syllable.get(i+2)+" is neither slot4vowel nor slot5: invalid structure!");
							}
						}//end if suffix is longer 
					}//end if found slot4-consonant
					else {
						throw new IllegalArgumentException((String)syllable.get(i+2)+" is not slot4: invalid structure!");
					}
				} //end if there is a suffix
			} //end if reached root
		} // end for loop 
	} // end checkSuffix 
	
	
	/** This method is a preprocessing for the syllable where
	 *  the longest prefix/root letter is determined and all letters
	 *  are stored in a List which is then used for further
	 *  processing
	 * @param sylText
	 * @return LinkedList containing wylie letters
	 * (with longest possible letter combinations)
	 */
	public LinkedList preprocessSyll(String sylText) {
        
	    /*LinkedList sylList = new LinkedList();
		int step = 1;
		// default pref is first character
		String pref = sylText.substring(0,1);
		// find longest prefix (before vowel)
		while (!vowels.contains(sylText.substring(step-1,step)) && step<sylText.length()) {
			if (consonants.contains(sylText.substring(0,step))) {
				pref = sylText.substring(0,step);
			}
			step++;
		}
		sylList.add(pref);
		for (int i=pref.length();i<sylText.length();i++) {
			// add the rest of the letters to the list if they are real wylie symbols
			if (wylieSymbols.contains(sylText.substring(i,i+1))) {
				sylList.addLast(sylText.substring(i,i+1));
			} else {
				throw new IllegalArgumentException (sylText.substring(i,i+1)+" is not a valid wylie letter!");
			}
		}
        */
        
        // MS: Re-programmed this, 20.09.05
        LinkedList sylList = new LinkedList();
        for(int pos=0; pos<sylText.length(); ) {
            for (int len=Math.min(wylieMaxlen,sylText.length()-pos); len>0; len--) {
                String s = sylText.substring(pos, pos+len);
                if (wylieSymbols.contains(s)) { // it's a valid wylie symbol
                    sylList.add(s);
                    assert len == s.length();
                    pos += len;
                    break; // leave len loop
                } else if (len == 1) { // we have found nothing
                    logger.info("Unknown character in input: `"+s+"' -- ignoring.");
                    pos++;
                }
            }
        }
        
        
        StringBuffer buf = new StringBuffer();
        for (Iterator it=sylList.iterator(); it.hasNext();) {
            buf.append(it.next());
            buf.append(" ");
        }
        logger.debug("preprocessSyll: '"+buf+"'");
		return sylList;
	}// end preprocessSyll
			
	
	/**
	 * This method checks whether there are multi-syllabic words containing
	 * the given syllable (particle) and matches them against the whole syllable array
	 *   
	 * @param syllable a syllable which is a particle
	 * @param syllArray all syllable of the sentence
	 * @param sylPos position of syllable in syllable array
	 * @return integer that marks how many syllables (preceding it) belong
	 *  to the found word
	 */
	public int checkWord(String syllable, String[] syllArray, int sylPos) {
		
		int wordLength = 1;
		boolean found = false;
		String currentWord = "";
		String[] wordArray;
		int wordScan = 0;
		int sylScan = sylPos;
		
		while (!found) {
			for (int i=0;i<lexicon.size();i++) {
				// if an entry ends with the given particle...
				currentWord = ((String)lexicon.get(i));
				wordArray = currentWord.split("\\s");
				wordScan = wordArray.length-1; //for more potential particles search from end of word
				sylScan = sylPos;
				if (currentWord.endsWith(syllable)) {
					// check whether preceding syllables of word match the actual syllArray
					// starting with the second last syllable (or before)
					for (int j=wordScan-1;j>-1;j--) {
						sylScan--;
						logger.info("compare lexicon with actual word: "+syllArray[sylScan]);
						if (sylPos>-1 && wordArray[j].equals(syllArray[sylScan])){
							wordLength++;
							found = true;
							logger.info("Word Match! length: "+wordLength);
						}
						// if mismatch, reset wordLength and try new word
						else {wordLength = 1; found = false; break;}
					}
					// for now we're happy with the first word match 
					if (found==true) break;
				} // end if word ends with syllable
			}//end for whole lexicon
			
			// if unsuccessful check whether current syllable is also a particle and try next
			if (sylPos>0 && ((LinkedList)particles.get("s")).contains(syllArray[sylPos])) {
						sylPos--;
						syllable = syllArray[sylPos];
						wordLength++;
						logger.info("not in lexicon but try: "+syllArray[sylPos]);
			} else 	break;
			
		} //end while word not found 
		
		return wordLength;
	} // end checkWord()
	
	
} //end class Tokeniser
