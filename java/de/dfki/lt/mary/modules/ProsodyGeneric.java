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
package de.dfki.lt.mary.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

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
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.MaryXML;
import de.dfki.lt.mary.NoSuchPropertyException;
import de.dfki.lt.mary.util.dom.DomUtils;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;

/**
 * The generic prosody module.
 * @author Stephanie Becker
 */

public class ProsodyGeneric extends InternalModule {
    protected String paragraphDeclination; // name of the config file entry
	protected boolean applyParagraphDeclination; // specified per language in mary config files
	
	protected String syllableAccents; // specified in mary config files: ToBI accents on words or syllables?
    protected boolean accentedSyllables;
	
	// path to accentPriorities file(contains attribute values(f.e. part of speechs)
	// and and a number which reflects the probability for their accentuation) specified in maryrc file
    protected String accentPriorities;
    protected Properties priorities;
	
	protected String tobiPredFilename; // xml rule file for prosody prediction
	protected HashMap tobiPredMap = new HashMap(); // map that will be filled with the rules
	protected HashMap listMap = new HashMap(); // map that will contain the lists defined in the xml rule file
	
    public ProsodyGeneric(MaryDataType inputType,MaryDataType outputType,String tobipredFileName,String accentPriorities, String syllableAccents, String paragraphDeclination)
    {
        super("Prosody",inputType,outputType);
        
        this.tobiPredFilename = tobipredFileName;
        this.accentPriorities = accentPriorities;
        this.syllableAccents = syllableAccents;
        this.paragraphDeclination = paragraphDeclination;
    }

    public void startup() throws Exception {
    	priorities = new Properties();
    	if (accentPriorities != null) {
    	    String fileName = MaryProperties.needFilename(accentPriorities);
    	    priorities.load(new FileInputStream(fileName));
        }
        
    	if (syllableAccents != null) {
    	    accentedSyllables = MaryProperties.getBoolean(syllableAccents);
        } else {
            accentedSyllables = false;
        }
        if (paragraphDeclination != null) {
            applyParagraphDeclination = MaryProperties.getBoolean(paragraphDeclination);
        } else {
            applyParagraphDeclination = false;
        }
        super.startup();
        loadTobiPredRules(); // fill the rule map
        buildListMap(); // fill the list map
    }

    protected void loadTobiPredRules () throws FactoryConfigurationError, ParserConfigurationException, org.xml.sax.SAXException, IOException,
    NoSuchPropertyException {
    	// parsing the xml rule file
    	DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
    	f.setValidating(false);
    	DocumentBuilder b = f.newDocumentBuilder();
    	Document tobiPredRules = b.parse(new FileInputStream(MaryProperties.needFilename(tobiPredFilename)));
    	
    	Element root = tobiPredRules.getDocumentElement();
    	for (Element e = MaryDomUtils.getFirstChildElement(root);
        e != null;
        e = MaryDomUtils.getNextSiblingElement(e)) { //HashMap with 4 entries 
    		if (e.getTagName().equals("definitions")) { //list defintions
    			tobiPredMap.put("definitions",e);
    		}
    		if (e.getTagName().equals("accentposition")) { // these rules determine which words receive accents
    			tobiPredMap.put("accentposition",e);
    		}
    		if (e.getTagName().equals("accentshape")) { // these rules determine which type of accent a word receives
    			tobiPredMap.put("accentshape",e);
    		}
    		if (e.getTagName().equals("boundaries")) { // these rules determine locatian and type of boundaries
    			tobiPredMap.put("boundaries",e);
    		}
    	}
    }
    
    
    protected void buildListMap() throws IOException {
    	Element listDefinitions = null;
    	listDefinitions = (Element) tobiPredMap.get("definitions");
    	
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
			
			if(list.hasAttribute("file")) { // external list definition
                String fileName = list.getAttribute("file");
                listMap.put(name, readListFromFile(fileName));
			}
    	}
    }
    
    /**
     * Read a list from an external file. This generic implementation
     * can read from text files (filenames ending in <code>.txt</code>).
     * Subclasses may override this class to provide additional file formats.
     * They must make sure that <code>checkList()</code> can deal with all
     * list formats.
     * @param fileName external file from which to read the list; suffix identifies
     * list format.
     * @return An Object representing the list; checkList() must be able to
     * make sense of this. This base implementation returns a Set.
     * @throws IllegalArgumentException if the fileName suffix cannot be
     * identified as a list file format.
     * @throws IOException if the file given in fileName cannot be found or read from
     */
    protected Object readListFromFile(String fileName) throws IOException
    {
        String suffix = fileName.substring(fileName.length() - 4, fileName.length()); 
         if (suffix.equals(".txt")) { // txt file
             StringTokenizer st = new StringTokenizer(fileName, "/");
             String txtPath = MaryProperties.maryBase();
             while (st.hasMoreTokens()) {
                 txtPath = txtPath + File.separator + st.nextToken();
             }
             // build a set that contains every word contained in the
             // external text file
             HashSet listSet = new HashSet(); 
             BufferedReader in = new BufferedReader(new FileReader(txtPath));
             while (in.ready()) {
                 String line = in.readLine();
                 listSet.add(line);
             }
             return listSet; // put the set on the map
         } else {
             throw new IllegalArgumentException("Unknown list file format: " + suffix);
         }

     }

    
    public MaryData process(MaryData d) throws Exception {
        Document doc = d.getDocument();
        // get the sentences
        NodeIterator sentenceIt = ((DocumentTraversal)doc).
            createNodeIterator(doc.getDocumentElement(), NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.SENTENCE), false);
        Element sentence = null;
        while ((sentence = (Element)sentenceIt.nextNode()) != null) {
            // And now the actual processing
            logger.debug("Processing next sentence");
            processSentence(sentence);
        }
        if(accentedSyllables == true) {
        	copyAccentsToSyllables(doc); // ToBI accents on syllables or words?
        }
        if (applyParagraphDeclination) {
            NodeList paragraphs = doc.getElementsByTagName(MaryXML.PARAGRAPH);
            for (int i=0; i< paragraphs.getLength(); i++) {
                Element paragraph = (Element) paragraphs.item(i);
                NodeList phrases = paragraph.getElementsByTagName(MaryXML.PHRASE);
                int steps = phrases.getLength();
                if (steps <= 1) continue; 
                for (int j=0; j<steps; j++) {
                    // Paragraph intonation: embed each <phrase> in a <prosody>
                    // element simulating a paragraph-wide declination phenomenon
                    // superimposed to the phrase-internal declination.
                    int pitchDiff = 10; // difference in percent between first and last phrase
                    int rangeDiff = 40; // difference in percent between first and last phrase
                    double factor = (0.5 - j/(steps-1f));
                    int pitchValue = (int) (pitchDiff * factor);
                    String pitchString = (pitchValue >= 0 ? "+" : "") + pitchValue + "%";
                    int rangeValue = (int) (rangeDiff * factor);
                    String rangeString = (rangeValue >= 0 ? "+" : "") + rangeValue + "%";
                    Element phrase = (Element) phrases.item(j);
                    Element prosody = MaryXML.createElement(phrase.getOwnerDocument(), MaryXML.PROSODY);
                    phrase.getParentNode().insertBefore(prosody, phrase);
                    prosody.appendChild(phrase);
                    prosody.setAttribute("pitch", pitchString);
                    prosody.setAttribute("range", rangeString);
                }
            }            
        }
        MaryData result = new MaryData(outputType());
        result.setDocument(doc);
        return result;
    }

    
    protected void processSentence (Element sentence)  {
    	
        NodeList tokens = sentence.getElementsByTagName(MaryXML.TOKEN);
        if (tokens.getLength() < 1) {
            return; // no tokens -- what can we do?
        }
        Element firstTokenInPhrase = null;
        
        // properties of the whole sentence
        // first determine the sentence type
        String sentenceType = "decl";
        sentenceType = getSentenceType(tokens);
        // determine if it is the last sentence in a paragraph
        boolean paragraphFinal =
            MaryDomUtils.isLastOfItsKindIn(sentence, MaryXML.PARAGRAPH)
                && !MaryDomUtils.isFirstOfItsKindIn(sentence, MaryXML.PARAGRAPH);
        
        // check if it is a sentence with vorfeld
        boolean inVorfeld = true; // default
        for(int i=0; i<tokens.getLength();i++) { // search for the first word in sentence
        	Element token = ((Element)tokens.item(i));
        	if(!token.getAttribute("sampa").equals("")) { // first word found
        		String posFirstWord = token.getAttribute("pos");
        		// if pos value of first word in sentence is contained in set noVorfeld, vorfeld doens't exist 
        		HashSet noVorfeld = (HashSet)listMap.get("noVorfeld");
        		if(noVorfeld != null) {
                	if(noVorfeld.contains(posFirstWord)) {
                		inVorfeld = false;
                	}
        		}
        		break;
        	}
        }
        // default: no special position
        String specialPositionType = "noValue"; // can get the values "endofvorfeld" and "endofpar"(=end of paragraph)
        int numEndOfVorfeld = -1;
        
        boolean hasAccent=false; // tests if phrase has an accent
        Element bestCandidate = null; // will become token with highest accent priority if a phrase has no accent; 
        							  // avoids phrases without accent
        
        // loop over the tokens in sentence
        // assignment of accent position and boundaries
        for (int i = 0; i < tokens.getLength(); i++) {
        	Element token = (Element) tokens.item(i); 
            logger.debug("Now looking at token `"+MaryDomUtils.tokenText(token)+"'");
            if(firstTokenInPhrase==null) {
            	firstTokenInPhrase=token; // begin of an intonation phrase
            }
            
            // determine if token is at end of vorfeld
            if(inVorfeld != false) { // only if vorfeld exists and if token's position is not after vorfeld
               	if(i<tokens.getLength()-1) {
               		Element nextToken = (Element) tokens.item(i+1);
               		String posNextToken = nextToken.getAttribute("pos");
               		// if pos value of next token is contained in set beginOfMittelfeld, 
               		// current token is at the end of the vorfeld
               		HashSet beginOfMittelfeld = (HashSet)listMap.get("beginOfMittelfeld"); 
               		if(beginOfMittelfeld != null && beginOfMittelfeld.contains(posNextToken)) {
               			//for(int z=0; z<attNodes.getLength(); z++) {
               	    		//Node el = (Node)attNodes.item(z);
               	    		//String currentVal = el.getNodeValue();
               	    		//if(beginOfMittelfeld.contains(currentVal)) {
               			//if(beginOfMittelfeld.contains(posNextToken)) {
               			specialPositionType = "endofvorfeld";
               			numEndOfVorfeld = i; // position of current token 
               			inVorfeld = false;
               		} else specialPositionType = "vorfeld";
               	}
            }
            
            // determine token position in text
            boolean isFinalToken = (i >= tokens.getLength() - 1); // last token in sentence?
            if(paragraphFinal && isFinalToken) { // last token in sentence and in paragraph?
              	specialPositionType = "endofpar";
            }
            
            boolean applyRules = applyRules(token);
                
            if (applyRules) { // rule application not turned off
            	// first: assignment of accent = "tone", accent="force"(for force-accents(Druckakzent)) or accent=""
              	// --> determine if the token receives an accent or not
               	// the type of accent(f.e. L+H*) is assigend later
                	
               	/*** begin user input check,accent position ***/
               	String forceAccent = getForceAccent(token);
               	if (token.getAttribute("accent").equals("unknown")
                    || !token.hasAttribute("accent") && (forceAccent.equals("word") || forceAccent.equals("syllable"))) {
               		setAccent(token, "tone"); // the token receives an accent according to user input
               	} else if(token.getAttribute("accent").equals("none") || forceAccent.equals("none")) {
               	    // no accent according to user input
               	} else if(!token.getAttribute("accent").equals("")) {
               		// accent type is already assigned by the user, f.e. accent="L+H*"
               		/*** end user input check, accent position ***/
               		// no user input
               		// the rules in the xml file are applied
               	} else if(token.getAttribute("sampa").equals("")) { // test if token is punctuation
               		token.removeAttribute("accent"); // doesn't receive an accent
               	} else { // default behaviour: determine by rule whether to assign an accent
                    getAccentPosition(token, tokens, i, sentenceType, specialPositionType);
                }

                // check if the phrase has an accent (avoid intermediate phrases without accent)
                if(token.hasAttribute("accent") && !token.getAttribute("accent").equals("none")) {
                    hasAccent = true;
                }
                // if not, check if current token is the best candidate
                if(hasAccent == false &&
                        !(token.getAttribute("accent").equals("none") || forceAccent.equals("none")) &&
                        !token.getAttribute("sampa").equals("")) {
                    if(bestCandidate == null) { // no candidate yet
                        bestCandidate=token;
                    } else {
                        int priorToken = -1;
                        int priorBestCandidate = -1;
                        // search for pos in accentPriorities property list
                        // first check priority for current token
                        String posCurrentToken = token.getAttribute("pos");
                        try {
                            priorToken = Integer.parseInt(priorities.getProperty(posCurrentToken));
                        } catch (NumberFormatException e) { }
                        // now check priority for bestCandidate
                        String posBestCandidate = bestCandidate.getAttribute("pos");
                        try {
                            priorBestCandidate = Integer.parseInt(priorities.getProperty(posBestCandidate));
                        } catch (NumberFormatException e) { }
                        // if the current token has higher priority than the best candidate,
                        // current token becomes the best candidate for accentuation
                        if(priorToken != -1 && priorBestCandidate != -1) {
                            if(priorToken <= priorBestCandidate) bestCandidate = token;
                        }
                    }
                }
                if(token.getAttribute("accent").equals("none") || forceAccent.equals("none")) {
                    token.removeAttribute("accent");
                }

            } // end of accent position assignment
            
            
            // now the informations relevant only for boundary assignment
            boolean invalidXML = false;
            if(!isFinalToken) { // We only set a majorIP boundary if the XML structure
            	// allows the phrase to be closed before the next token
               	invalidXML =
               		MaryDomUtils.isAncestor(
               				MaryDomUtils.closestCommonAncestor(firstTokenInPhrase, tokens.item(i)),
							MaryDomUtils.closestCommonAncestor(tokens.item(i), tokens.item(i + 1))); 
            }
            
            if(applyRules) {
            	// insertion of ip- and IP-boundaries
              	// returns value for firstTokenInPhrase(begin of new phrase): if a boundary was inserted, firstTokenInPhrase gets null
               	// if not, firstTokenInPhrase has the same value as before
            	firstTokenInPhrase = getBoundary(token,tokens,i,sentenceType,specialPositionType,invalidXML,firstTokenInPhrase);
                
            	// check if every intermediate an intonation phrase has at least one accent
            	// first check if a boundary was inserted
            	Element boundary = null;
                Document doc = token.getOwnerDocument();
                TreeWalker tw = ((DocumentTraversal)doc).createTreeWalker(
                	DomUtils.getAncestor(token, MaryXML.SENTENCE), NodeFilter.SHOW_ELEMENT,
                           new NameNodeFilter(new String[] {MaryXML.BOUNDARY, MaryXML.TOKEN}), false);
                tw.setCurrentNode(token);
                logger.debug("Starting treewalker at token " + MaryDomUtils.tokenText(token));
                Element next = (Element) tw.nextNode();
                if (next != null && next.getTagName().equals(MaryXML.BOUNDARY)) {
                    logger.debug("tw found a boundary");
                	boundary = next;
                    int bi = 0;
                    try {
                        bi = Integer.parseInt(boundary.getAttribute("breakindex"));
                    } catch (NumberFormatException nfe) {}
                    if(bi>=3) { // is it an intermediate or an intoantion phrase?
                    	if(hasAccent == false && bestCandidate != null) { // no accent!
                    		setAccent(bestCandidate, "tone"); // best candidate receives accent
                    	}
                    	hasAccent=false;
                        bestCandidate = null;
                    }
                }
            }
            if(specialPositionType.equals("endofvorfeld")) specialPositionType="noValue";
        } // loop tokens for accent position and boundary assignment

        /*** user input check, boundaries ***/
        NodeList boundaries = sentence.getElementsByTagName(MaryXML.BOUNDARY);
        for (int i = 0; i < boundaries.getLength(); i++) {
            Element boundary = (Element) boundaries.item(i);
            if (boundary.getAttribute("breakindex").equals("none")) { // the boundary is to be deleted
                // delete boundary
                Node parent = boundary.getParentNode();
                parent.removeChild(boundary);
            } else if (boundary.getAttribute("tone").equals("unknown")) { // boundary, but no tone is given
            	// is there a preferred tone for boundaries?
            	Element prosody =
                    MaryDomUtils.getClosestAncestorWithAttribute(boundary, MaryXML.PROSODY, "preferred-boundary-type");
                String preferred = null;
                if (prosody != null) preferred = prosody.getAttribute("preferred-boundary-type");
                String h = boundary.getAttribute("breakindex");
                int bi = 0;
                String tone = null;
                try {
                    bi = Integer.parseInt(h);
                } catch (NumberFormatException e) { } // ignore invalid values
                if (bi >= 4) {
                    // major boundary (but we cannot insert a phrase,
                    // because we don't know where it should start)
                    if (preferred != null) {
                        if (preferred.equals("high")) {
                        	HashSet set = (HashSet)listMap.get("high_major_boundary");
                			Iterator it = set.iterator();
                			while(it.hasNext()) tone = (String)it.next();
                        } else { // low
                        	HashSet set = (HashSet)listMap.get("low_major_boundary");
                			Iterator it = set.iterator();
                			while(it.hasNext()) tone = (String)it.next();
                        }
                    } else { // there isn't any information about the tone, so we use default values specified in the xml file
                    	if (i == boundaries.getLength() - 1) { // final boundary
                    		if(sentenceType.equals("decl") || sentenceType.equals("excl")) { //declarative or exclamative sentence
                    			HashSet set = (HashSet)listMap.get("default_IP_endOfSent");
                    			Iterator it = set.iterator();
                    			while(it.hasNext()) tone = (String)it.next();
                    		} else {
                    			HashSet set = (HashSet)listMap.get("default_IP_endOfInterrogSent"); // interrogative
                    			Iterator it = set.iterator();
                    			while(it.hasNext()) tone = (String)it.next();
                    		}
                    	} else { // non-final boundary
                    		HashSet set = (HashSet)listMap.get("default_IP_midOfSent"); 
                			Iterator it = set.iterator();
                			while(it.hasNext()) tone = (String)it.next();
                    	}
                    }
                } else if (bi == 3) {
                    // minor boundary
                    if (preferred != null) {
                        if (preferred.equals("high")) {
                        	HashSet set = (HashSet)listMap.get("high_minor_boundary");
                			Iterator it = set.iterator();
                			while(it.hasNext()) tone = (String)it.next();
                        } else { // low
                        	HashSet set = (HashSet)listMap.get("low_minor_boundary");
                			Iterator it = set.iterator();
                			while(it.hasNext()) tone = (String)it.next();
                        }
                    } else {// there is no information about the tone, so we use the default values specified in the xml file
                    	HashSet set = (HashSet)listMap.get("default_ip"); 
            			Iterator it = set.iterator();
            			while(it.hasNext()) tone = (String)it.next();
                    }
                }
                if (tone != null) boundary.setAttribute("tone", tone);
            }
        } // for all boundaries
        /*** end user input check, boundaries ***/
        
        // now the information relevant for accent type assignment
        boolean nucleusAssigned = false;
        String lastAssignedTone = null; // for user input preferred-accent-shape="alternating_accents"
        
        for (int j = tokens.getLength() - 1; j >= 0; j--) { // accent type assignment
            Element token = (Element) tokens.item(j);
            
            // determine specialpositionType
            boolean isFinalToken = (j >= tokens.getLength() - 1); // last token in sentence?
            if(paragraphFinal && isFinalToken) { // last token in paragraph?
            	specialPositionType = "endofpar"; // last token in sentence and in paragraph
            }
            if(j == numEndOfVorfeld) specialPositionType = "endofvorfeld";
            
            if(token.getAttribute("accent").equals("tone") || token.getAttribute("accent").equals("force")) {
            	
            	/*** begin user input check, accent type ***/
            	Element prosody = MaryDomUtils.getClosestAncestorWithAttribute(token, MaryXML.PROSODY, "preferred-accent-shape");
            	if (prosody != null) {
            		if(token.getAttribute("accent").equals("tone")) { // no force accents in this case
            			String tone = null;
            			String preferred = prosody.getAttribute("preferred-accent-shape");
            			if (preferred.equals("alternating")) {
            				HashSet set = (HashSet)listMap.get("alternating_accents");
            				Iterator it = set.iterator();
            				while(it.hasNext()) {
            					String next = (String)it.next();
            					if(lastAssignedTone == null || !lastAssignedTone.equals(next)) {
            						tone = next;
            					}
            				}
            			} else if (preferred.equals("rising")) {
            				HashSet set = (HashSet)listMap.get("rising_accents");
            				Iterator it = set.iterator();
            				if(it.hasNext()) tone = (String)it.next();
            			} else if (preferred.equals("falling")) {
            				HashSet set = (HashSet)listMap.get("falling_accents");
            				Iterator it = set.iterator();
            				if(it.hasNext()) tone = (String)it.next();
            			}
            			token.setAttribute("accent",tone);
                		if(nucleusAssigned == false) nucleusAssigned = true;
            		}
            	} else if(!(token.getAttribute("accent").equals("force") || token.getAttribute("accent").equals("tone")||
				    token.getAttribute("accent").equals(""))) {
            		  nucleusAssigned = true; // user has already assigned an accent type
            	/*** end user input check, accent type ***/
            				
            	} else if(token.getAttribute("sampa").equals("")) { // test if token is a word (no punctuation)
           			// punctuation, doesn't receive an accent
            	} else // xml file rules are applied
           			// assignment of accent type
           			// returns true, if nuclear accent is assigned, false otherwise 
            		nucleusAssigned = getAccentShape(token,tokens,j,sentenceType,specialPositionType,nucleusAssigned); 
            }
        
            if(token.getAttribute("accent").equals("") || token.getAttribute("accent").equals("force")) {
    			token.removeAttribute("accent"); // if there is no accent, the accent attribute can be removed
    		}
            if(token.hasAttribute("accent")) {
            	lastAssignedTone = token.getAttribute("accent"); 
            }
        } //loop over tokens for accent type assignment
    } //processSentence
    
     
     
    /** checks if token receives an accent or not 
     * the information is contained in the accentposition part of rules in xml file
     * the token attribute "accent" receives the value "tone","force"(force accent(Druckakzent)) or ""(no accent) 
     * @param token (current token)
     * @param tokens (list of all tokens in sentence)
     * @param position (position in token list)
     * @param sentenceType (declarative, exclamative or interrogative)
     * @param specialPositionType (end of vorfeld or end of paragraph)
     */
    
    protected void getAccentPosition(Element token, NodeList tokens, int position, String sentenceType, String specialPositionType) {
    	
    	String tokenText = MaryDomUtils.tokenText(token); // text of current token
    	
    	Element ruleList = null;
    	// only the "accentposition" rules are relevant
    	ruleList = (Element) tobiPredMap.get("accentposition");
    	// search for concrete rules, with tag "rule"
    	TreeWalker tw =
            ((DocumentTraversal) ruleList.getOwnerDocument()).createTreeWalker(
                ruleList,
                NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(new String[] {"rule"}),
               false);
    	
    	boolean rule_fired = false;
    	String accent = ""; // default
    	Element rule = null;
    	
    	// search for appropriate rules; the top rule has highest prority
    	// if a rule fires (that is: all the conditions are fulfilled), 
    	// the accent value("tone","force" or "") is assigned and the loop stops
    	// if no rule is found, the accent value is ""
    	                	
        while (rule_fired == false && (rule = (Element) tw.nextNode()) != null) {
        	// rule = the whole rule
    		// currentRulePart = part of the rule (type of condition (f.e. attributes pos="NN") or action)
    		Element currentRulePart = DomUtils.getFirstChildElement(rule);
    		
			while(rule_fired == false && currentRulePart != null) {
				
				boolean conditionSatisfied=false;
				
				// if rule part with tag "action": accent assignment
				if(currentRulePart.getTagName().equals("action")) {
					accent = currentRulePart.getAttribute("accent");
					token.setAttribute("accent",accent);
					rule_fired = true;
					break;
				}
				// check if the condition is satisfied
				conditionSatisfied = checkRulePart(currentRulePart,token,tokens,position,sentenceType,specialPositionType,tokenText);
				if(conditionSatisfied == false) break; // condition violated, try next rule
				
				// the previous conditions are satisfied --> check the next rule part
				currentRulePart = DomUtils.getNextSiblingElement(currentRulePart);
			} //while loop that checks the rule parts
        } // while loop that checks the whole rule
    }
 
    
    /** determines accent types;
     * tokens with accent="tone" will receive an accent type (f.e."L+H*"), accent="force" becomes "*" 
     * the relevant information is contained in the accentshape part of rules in xml file
     * @param token (current token)
     * @param tokens (list of all tokens in sentence)
     * @param position
     * @param sentenceType (declarative, exclamative or interrogative)
     * @param specialPositionType (position in sentence)
     * @param nucleusAssigned (test, if nuclear accent is already assigned)
     * @return nucleusAssigned 
     */
    
    protected boolean getAccentShape(Element token, NodeList tokens, int position, String sentenceType,
    	String specialPositionType, boolean nucleusAssigned) 
    {
        String tokenText = MaryDomUtils.tokenText(token); // text of current token
          
        // prosodic position (prenuclear, nuclear, postnuclear)
        String prosodicPositionType = null;
        if(nucleusAssigned == false){ // no nucleus assigned
        	if(token.getAttribute("accent").equals("tone")) { // current token will become the nucleus 
        		if(specialPositionType.equals("endofpar")) {
        			prosodicPositionType= "nuclearParagraphFinal";
        		} else {
        			prosodicPositionType= "nuclearNonParagraphFinal";
        		}
        	} else 	prosodicPositionType = "postnuclear"; // no nucleus, current token is postnuclear
        } else  prosodicPositionType = "prenuclear"; // nucleus is assigned --> prenuclear
            
        	        	
        Element ruleList = null;
        // only the "accentshape" rules are relevant
        ruleList = (Element) tobiPredMap.get("accentshape");
        // search for concrete rules (search for tag "rule")
        TreeWalker tw =
               ((DocumentTraversal) ruleList.getOwnerDocument()).createTreeWalker(
               	ruleList,
				NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(new String[] {"rule"}),
				false);
        	
        boolean rule_fired = false;
        String accent = "";
        Element rule = null;
        
        // search for appropriate rules; the top rule has highest prority
        // if a rule fires (that is: all the conditions are fulfilled), the accent type (f.e. "L+H*") is assigned and the loop stops
        // if no rule is found, the accent value is "" 	                	
        while (rule_fired == false && (rule = (Element) tw.nextNode()) != null) {
           	// rule = the whole rule
        	// currentRulePart = part of the rule (type of condition (f.e. attributes pos="NN") or action)
        	Element currentRulePart = DomUtils.getFirstChildElement(rule);
        	
			while(rule_fired == false && currentRulePart != null) {
				boolean conditionSatisfied=false;
				
				// if rule part with tag "action": accent type assignment
				if(currentRulePart.getTagName().equals("action")) {
					accent = currentRulePart.getAttribute("accent");
					token.setAttribute("accent",accent);
					rule_fired = true;
					if(nucleusAssigned == false && !accent.equals("*")) {
						nucleusAssigned = true;
					}
					break;
				}
				// check if the condition is satisfied
				// special case: prosodic position (only in the accentshape rule part) 
				// values: prenuclear,nuclearParagraphFinal,nuclearNonParagraphFinal,postnuclear
				if(currentRulePart.getTagName().equals("prosodicPosition")) {
					if(!checkProsodicPosition(currentRulePart,prosodicPositionType)) {
						break;
					}
				}
				// the usual check
				conditionSatisfied = checkRulePart(currentRulePart,token,tokens,position,sentenceType,specialPositionType,tokenText);
				if(conditionSatisfied == false) break; // condition violated, try next rule
				
				// the previous conditions are satisfied --> check the next rule part
				currentRulePart = DomUtils.getNextSiblingElement(currentRulePart);
			}//while loop that checks the rule parts
        } // while loop that checks the whole rule
     	return nucleusAssigned;
    }
    
    
    /** checks if a boundary is to be inserted after the current token
     * the information is contained in the boundaries part of rules in xml file
     * @param token (current token)
     * @param tokens (list of tokens in sentence)
     * @param position (position in token list)
     * @param sentenceType (declarative, exclamative or interrogative)
     * @param specialPositionType (endofvorfeld if sentence has vorfeld and the next token is a finite verb or end of paragraph)
     * @param invalidXML (true if xml structure allows boundary insertion)
     * @param firstTokenInPhrase (begin of intonation phrase)
     * @return firstTokenInPhrase (if a boundary was inserted, firstTokenInPhrase gets null)
     */
    
    protected Element getBoundary(Element token, NodeList tokens, int position, String sentenceType, String specialPositionType,
    	boolean invalidXML, Element firstTokenInPhrase) 
    {
        String tokenText = MaryDomUtils.tokenText(token); // text of current token
    	
    	Element ruleList = null;
    	// only the "boundaries" rules are relevant
    	ruleList = (Element) tobiPredMap.get("boundaries");
    	// search for concrete rules (search for tag "rule")
    	TreeWalker tw =
            ((DocumentTraversal) ruleList.getOwnerDocument()).createTreeWalker(
                ruleList,
                NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(new String[] {"rule"}),
               false);
    	
    	boolean rule_fired = false;
    	Element rule = null;
    	
    	// search for appropriate rules; the top rule has highest prority
    	// if a rule fires (that is: all the conditions are fulfilled), the boundary is inserted and the loop stops
        while (rule_fired == false && (rule = (Element) tw.nextNode()) != null) {
        	// rule = the whole rule
    		// currentRulePart = part of the rule (condition or action)
    		Element currentRulePart = DomUtils.getFirstChildElement(rule);
    		
			while(rule_fired == false && currentRulePart != null) {
				boolean conditionSatisfied = false;
				
				// if rule part with tag "action": boundary insertion
				if(currentRulePart.getTagName().equals("action")){
					int bi = Integer.parseInt(currentRulePart.getAttribute("bi"));
					if(bi == 0) {
						// no boundary insertion
					} else if(currentRulePart.hasAttribute("tone")) {
						String tone = currentRulePart.getAttribute("tone");
						if(tone.endsWith("%")) {
							if(!invalidXML) {
								Element boundary = insertMajorBoundary(tokens,position,firstTokenInPhrase,tone,bi);
								if (boundary != null) firstTokenInPhrase = null;
							}
						} else if(tone.endsWith("-")) {
							insertBoundary(token,tone,bi);
						} else insertBoundary(token,null,bi);
					} else 	insertBoundary(token,null,bi);
					rule_fired = true;
					break;
				}
				
				// check if the condition is satisfied
				conditionSatisfied = checkRulePart(currentRulePart,token,tokens,position,sentenceType,specialPositionType,tokenText);
				if(conditionSatisfied == false) break; // condition violated, try next rule
				
				// the previous conditions are satisfied --> check the next rule part
				currentRulePart = DomUtils.getNextSiblingElement(currentRulePart);
			}//while loop that checks the rule parts
        } // while loop that checks the whole rule
        return firstTokenInPhrase;
	}
 
    protected static final Pattern nextPlusXTextPattern = Pattern.compile("nextPlus[0-9]+Text");
    protected static final Pattern previousMinusXTextPattern = Pattern.compile("previousMinus[0-9]+Text");
    protected static final Pattern nextPlusXAttributesPattern = Pattern.compile("nextPlus[0-9]+Attributes");
    protected static final Pattern previousMinusXAttributesPattern = Pattern.compile("previousMinus[0-9]+Attributes");
    
    /** checks condition of a rule part, f.e. attributes pos="NN"
     * @param currentRulePart
     * @param token (current token)
     * @param tokens (list of all tokens)
     * @param position (position in token list) 
     * @param sentenceType (declarative, exclamative or interrogative)
     * @param specialPositionType (special position in sentence(end of vorfeld) or text(end of paragraph))
     * @param tokenText (text of token)
     * @return true if condition is satisfied 
     */
    protected boolean checkRulePart(Element currentRulePart,Element token,NodeList tokens,int position,String sentenceType,String specialPositionType,
    		String tokenText) {
    	String currentRulePartTagName = currentRulePart.getTagName();
    	// if rule part with tag text and attribute word, check if text of token equals text in rule
    	if(currentRulePartTagName.equals("text") & currentRulePart.hasAttribute("word")) { // text of the token
			return checkText(currentRulePart,tokenText);
		}
    	// text of following+X token or preceding-X token
		else if(currentRulePart.hasAttribute("word") &&
           (currentRulePartTagName.equals("nextText") || 
		   nextPlusXTextPattern.matcher(currentRulePartTagName).find() ||
		   currentRulePartTagName.equals("previousText") ||
		   previousMinusXTextPattern.matcher(currentRulePartTagName).find())) { 
			return checkTextOfOtherToken(currentRulePartTagName,currentRulePart,position,tokens);
		}
		// check number of following tokens
    	else if(currentRulePartTagName.equals("folTokens") && currentRulePart.hasAttribute("num")) {
			return checkFolTokens(currentRulePart,position,tokens);
		}
		// check number of preceding tokens
		else if(currentRulePartTagName.equals("prevTokens") && currentRulePart.hasAttribute("num")) {
			return checkPrevTokens(currentRulePart,position,tokens);
		}
		// check number of following words
		else if(currentRulePartTagName.equals("folWords") && currentRulePart.hasAttribute("num")) {
			return checkFolWords(currentRulePart,position,tokens);
		}
		// check number of preceding words
		else if(currentRulePartTagName.equals("prevWords") && currentRulePart.hasAttribute("num")) {
			return checkPrevWords(currentRulePart,position,tokens);
		}
		// check sentence type (f.e. declarative sentence)
		else if(currentRulePartTagName.equals("sentence") && currentRulePart.hasAttribute("type")) { 
			return checkSentence(currentRulePart,sentenceType);
		}
		// check for special position of token in sentence/text(endofvorfeld,endofpar)
		else if(currentRulePartTagName.equals("specialPosition") && currentRulePart.hasAttribute("type")) { 
			return checkSpecialPosition(currentRulePart,specialPositionType);
		}	
		// if rule part with tag "attributes"  
		// --> check the MaryXML attribute values of the token
		else if(currentRulePartTagName.equals("attributes")) {
			return checkAttributes(currentRulePart,token);
		}
		// if rule part with tag nextPlusXAttributes or previousMinusXAttributes 
		// --> check the MaryXML attribute values of the corresponding token
		else if(currentRulePartTagName.equals("nextAttributes") ||
		   nextPlusXAttributesPattern.matcher(currentRulePart.getTagName()).find() ||
		   currentRulePartTagName.equals("previousAttributes") ||
		   previousMinusXAttributesPattern.matcher(currentRulePart.getTagName()).find()) {
			return checkAttributesOfOtherToken(currentRulePart.getTagName(),currentRulePart,position,tokens);
		}
		else {
            // unknown rules always match
		    return true;
        }
    }
    
    /** checks rule part with tag "text";
     * there is only the "word" attribute right now: checks if text of a token is the same 
     * as the value of the word attribute in the rule 
     */
    protected boolean checkText(Element currentRulePart, String tokenText) {
    	
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
	
    	for(int z=0; z<attNodes.getLength(); z++) { 
    		Node el = attNodes.item(z);
    		String currentAtt = el.getNodeName();
    		String currentVal = el.getNodeValue();
		
    		if(currentAtt.equals("word")) { // there is only the "word" attribute right now
    			if(!currentVal.startsWith("INLIST") && !currentVal.startsWith("INFSTLIST")
    			   && !currentVal.startsWith("!INLIST") && !currentVal.startsWith("!INFSTLIST")) { // no list
    				if(!currentVal.startsWith("!")) { // no negation
    					if(!tokenText.equals(currentVal)) return false;
    				} else { // negation
    					currentVal = currentVal.substring(1,currentVal.length()); 
    					if(tokenText.equals(currentVal)) return false;
    				}
    			} else  return checkList(currentVal,tokenText); // list
    		}
    	} // for-loop
    	return true;
    }
    
    /** checks rule part with tag "nextText","previousText","nextPlusXText" or "previousMinusXText";
     * there is only the "word" attribute right now: checks if text of a token is the same 
     * as the value of the word attribute in the rule 
     */
    protected boolean checkTextOfOtherToken(String tag, Element currentRulePart, int position, NodeList tokens) {
    	
    	Element otherToken = null;
		
    	if(tag.equals("nextText")) { // text of next token
			if(position<tokens.getLength()-1) {
				otherToken = (Element)tokens.item(position+1);
			}
		}
		
    	if(nextPlusXTextPattern.matcher(tag).find()) { // text of some other token following the next token
    		String tempString = tag.replaceAll("nextPlus","");
    		String newString = tempString.replaceAll("Text","");
    		int num = Integer.parseInt(newString);
    		if(position<tokens.getLength()-(num+1)) otherToken = (Element)tokens.item(position+1+num);
    	}
    	
		if(tag.equals("previousText")) { // text of previous token
			if(position>0) otherToken = (Element)tokens.item(position-1);
		}
		
		if(previousMinusXTextPattern.matcher(tag).find()) { // text of some other token preceding the previous token
    		String tempString = tag.replaceAll("previousMinus","");
    		String newString = tempString.replaceAll("Text","");
    		int num = Integer.parseInt(newString);
    		
    		if(position>num) otherToken = (Element)tokens.item(position-(num+1));
    	}
		
		if(otherToken == null) return false;
		String otherTokenText = MaryDomUtils.tokenText(otherToken);
    	return checkText(currentRulePart,otherTokenText);
    }
    
    /** checks rule part with tag "folTokens"; 
     * there is only the "num" attribute right now; checks if the number of the following tokens after the current token
     *  is the same as the value of the num attribute;
     * 	f.e. the value "3+" means: at least 3 following tokens, "3-": not more than 3, "3": exactly 3 
     */
    protected boolean checkFolTokens(Element currentRulePart, int position, NodeList tokens) {
    	
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
	
    	for(int z=0; z<attNodes.getLength(); z++) {
    		Node el = attNodes.item(z);
    		String currentAtt = el.getNodeName();
    		String currentVal = el.getNodeValue();
		
    		if(currentAtt.equals("num")) { // there is only the "num" attribute right now
    			int num = Integer.parseInt(currentVal.substring(0,1));
    			int requiredLastTokenPosition = position+num;
    			
    			if(currentVal.length() == 1) { // rule requires exactly num tokens after current token  
    				if(!(tokens.getLength()-1 == requiredLastTokenPosition))return false;   			
    			} else if(currentVal.substring(1,2).equals("+")) { // rule requires at least num tokens after current token
        			if(!(tokens.getLength()-1 >= requiredLastTokenPosition)) return false;
    			} else if(currentVal.substring(1,2).equals("-")) { // rule requires not more than num tokens after current token
            		if(!(tokens.getLength()-1 <= requiredLastTokenPosition)) return false;
    			}
    		}
    	}
    	return true;
    }
    
    /** checks rule part with tag "prevTokens"; 
     * there is only the "num" attribute right now; checks if the number of the tokens preceding the current token
     *  is the same as the value of the num attribute;
     * 	f.e. the value "3+" means: at least 3 preceding tokens, "3-": not more than 3, "3": exactly 3 
     */
    protected boolean checkPrevTokens(Element currentRulePart, int position, NodeList tokens) {
    	
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
	
    	for(int z=0; z<attNodes.getLength(); z++) {
    		Node el = attNodes.item(z);
    		String currentAtt = el.getNodeName();
    		String currentVal = el.getNodeValue();
		
    		if(currentAtt.equals("num")) { // there is only the "num" attribute right now
    			int num = Integer.parseInt(currentVal.substring(0,1));
    			int requiredFirstTokenPosition=position-num;
    			
    			if(currentVal.length() == 1) {// rule requires exactly num tokens preceding current token
    				if(!(requiredFirstTokenPosition == 0)) return false;
    			} else if(currentVal.substring(1,2).equals("+")) { // rule requires at least num tokens preceding current token
        			if(!(0 <= requiredFirstTokenPosition)) return false;
    			} else if(currentVal.substring(1,2).equals("-")) { // rule requires not more than num tokens preceding current token
            		if(!(0 >= requiredFirstTokenPosition)) return false;
    			}
    		}
    	}
    	return true;
    }
    
    /** checks rule part with tag "folWords"; 
     * there is only the "num" attribute right now; checks if the number of the following words after the current token
     *  is the same as the value of the num attribute;
     * 	f.e. the value "3+" means: at least 3 following tokens, "3-": not more than 3, "3": exactly 3 
     */
    protected boolean checkFolWords(Element currentRulePart, int position, NodeList tokens) {
    	
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
    	
    	for(int z=0; z<attNodes.getLength(); z++) {
    		Node el = attNodes.item(z);
    		String currentAtt = el.getNodeName();
    		String currentVal = el.getNodeValue();
		
    		if(currentAtt.equals("num")) { // there is only the "num" attribute right now
    			int requiredNum = Integer.parseInt(currentVal.substring(0,1));
    			int num = 0;
    			for(int i=position+1;i<tokens.getLength();i++) {
    				if(!((Element)tokens.item(i)).getAttribute("sampa").equals("")) num++;
    			}
    			if(currentVal.length() == 1) { // rule requires exactly num words after current token  
       				if(num != requiredNum) return false;
    			} else if(currentVal.substring(1,2).equals("+")) { // rule requires at least num words after current token
    				if(!(num >= requiredNum)) return false;
    			} else if(currentVal.substring(1,2).equals("-")) { // rule requires not more than num words after current token
            		if(!(num <= requiredNum)) return false;	
    			}
    		}
    	}
    	return true;
    }
    
    /** checks rule part with tag "prevWords"; 
     * there is only the "num" attribute right now; checks if the number of the words preceding the current token
     *  is the same as the value of the num attribute;
     * 	f.e. the value "3+" means: at least 3 preceding tokens, "3-": not more than 3, "3": exactly 3 
     */
    protected boolean checkPrevWords(Element currentRulePart, int position, NodeList tokens) {
    	
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
    	
    	for(int z=0; z<attNodes.getLength(); z++) {
    		Node el = attNodes.item(z);
    		String currentAtt = el.getNodeName();
    		String currentVal = el.getNodeValue();
		
    		if(currentAtt.equals("num")) { // there is only the "num" attribute right now
    			int requiredNum = Integer.parseInt(currentVal.substring(0,1));
    			int num = 0;
    			for(int i=position-1;i>=0;i--) {
    				if(!((Element)tokens.item(i)).getAttribute("sampa").equals("")) num++;
    			}
    			if(currentVal.length() == 1) { // rule requires exactly num words after current token  
       				if(num != requiredNum) return false;
    			} else if(currentVal.substring(1,2).equals("+")) { // rule requires at least num words after current token
    				if(!(num >= requiredNum)) return false;
    			} else if(currentVal.substring(1,2).equals("-")) { // rule requires not more than num words after current token
            		if(!(num <= requiredNum)) return false;
    			}
    		}
    	}
    	return true;
    }
    
    /** checks rule part with tag "sentence";
     * there is only the "type" attribute right now: checks if sentence type of a token is the same 
     * as the value of the type attribute in the rule 
     */
    protected boolean checkSentence (Element currentRulePart, String sentenceType) {
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
	
    	for(int z=0; z<attNodes.getLength(); z++) {
    		Node el = attNodes.item(z);
    		String currentAtt = el.getNodeName();
    		String currentVal = el.getNodeValue();
		
    		if(currentAtt.equals("type")) { // there is only the "type" attribute right now
    			if(!currentVal.startsWith("!")) { // no negation
    				if(!sentenceType.equals(currentVal)) return false;
    			} else { // negation
    				currentVal = currentVal.substring(1,currentVal.length()); 
    				if(sentenceType.equals(currentVal)) return false;
    			}
    		}
    	}
    	return true;
    }
    
    /** checks rule part with tag "specialPosition";
     * there is only the "type" attribute right now: checks if specialPosition value of a token is the same 
     * as the value of the type attribute in the rule;
     * values: endofvorfeld, endofpar (end of paragraph)
     */
    protected boolean checkSpecialPosition (Element currentRulePart, String specialPositionType) {
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
	
    	for(int z=0; z<attNodes.getLength(); z++) {
    		Node el = attNodes.item(z);
    		String currentAtt = el.getNodeName();
    		String currentVal = el.getNodeValue();
		
    		if(currentAtt.equals("type")) { // there is only the "type" attribute right now
    			if(!currentVal.startsWith("!")) { // no negation
    				if(!specialPositionType.equals(currentVal)) return false;
    			} else { // negation
    				currentVal = currentVal.substring(1,currentVal.length()); 
    				if(specialPositionType.equals(currentVal)) return false;
    			}
    		}
    	}
    	return true;
    }
    
    /** checks rule part with tag "prosodicPosition";
     * there is only the "type" attribute right now: checks if prosodic position of a token is the same 
     * as the value of the type attribute in the rule;
     * values: prenuclear, nuclearParagraphFinal, nuclearParagraphNonFinal, postnuclear
     */
    protected boolean checkProsodicPosition (Element currentRulePart, String prosodicPositionType) {
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
	
    	for(int z=0; z<attNodes.getLength(); z++) {
    		Node el = attNodes.item(z);
    		String currentAtt = el.getNodeName();
    		String currentVal = el.getNodeValue();
		
    		if(currentAtt.equals("type")) { // there is only the "type" attribute right now
    			if(!currentVal.startsWith("!")) { // no negation
    				if(!prosodicPositionType.equals(currentVal)) return false;
    			} else {  // negation
    				currentVal = currentVal.substring(1,currentVal.length()); 
    				if(prosodicPositionType.equals(currentVal)) return false;
    			}
    		}
    	}
    	return true;
    }
    
    
    
    /** checks rule part with tag "attributes"; 
     *  checks if the MaryXML attributes and values of current token are the same as in the rule
     */
    protected boolean checkAttributes(Element currentRulePart, Element token) {
    	
    	NamedNodeMap attNodes = currentRulePart.getAttributes();
		
		if(token == null) return false; // token doesn't exist
		
		for(int z=0; z<attNodes.getLength(); z++) { // loop over MaryXML attributes in rule part
			Node el = attNodes.item(z);
			String currentAtt = el.getNodeName();
			String currentVal = el.getNodeValue();
			
			// first the special cases
			if(!token.hasAttribute(currentAtt)) { // token doesn't have attribute
				if(currentVal.equals("!")) { // rule says that token shouldn't have it --> return true
					return true;
				} 
                // rule says that token should have it --> return false
				return false;
			}
            // token has attribute ...
            if(currentVal.equals("!")) { // .. but rule says that token shouldn't have it --> return false
                return false;
            }
            if(currentVal.equals("")) { // rule says that value doesn't matter, but attribute has to be present --> return true
                return true;
            }
            // first case: the value of the rule attribute is not a list
            if(!currentVal.startsWith("INLIST") && !currentVal.startsWith("INFSTLIST") 
                && !currentVal.startsWith("!INLIST") && !currentVal.startsWith("!INFSTLIST")) {
                
                if(!currentVal.startsWith("!")) {
                    if(!token.getAttribute(currentAtt).equals(currentVal)) { // condition violated
                        return false;
                    }
                } else { // value is negated --> token shouldn't have the value in currentVal
                    currentVal = currentVal.substring(1,currentVal.length());
                    if(token.getAttribute(currentAtt).equals(currentVal)) { // condition violated
                        return false;
                    }
                }
            } else { //second case: the value of the rule attribute is a list
                return checkList(currentVal,token.getAttribute(currentAtt));
            }
		}  // for-loop
		return true;
    }
    
    /** checks rule part with tag "nextAttributes","previousAttributes","nextPlusXAttributes","previousMinusXAttributes";
     *  checks if the MaryXML attributes and values of other token than the current one
     * are the same as in rule (f.e. the 3th token after current token)
     */
    protected boolean checkAttributesOfOtherToken(String tag, Element currentRulePart, int position, NodeList tokens) {
    	
    	Element otherToken = null;
		
    	if(tag.equals("nextAttributes")) { // MaryXML attributes of next token
			if(position<tokens.getLength()-1) {
				otherToken = (Element)tokens.item(position+1);
			}
		}
		
    	if(nextPlusXAttributesPattern.matcher(tag).find()) { //MaryXML attributes of some token following the next token 
    		String tempString = tag.replaceAll("nextPlus","");
    		String newString = tempString.replaceAll("Attributes","");
    		int num = Integer.parseInt(newString);
    		if(position<tokens.getLength()-(num+1)) {
				otherToken = (Element)tokens.item(position+1+num);
			}
    	}
    	
		if(tag.equals("previousAttributes")) { // MaryXML attributes of previous token
			if(position>0) {
				otherToken = (Element)tokens.item(position-1);
			}
		}
		
		if(previousMinusXAttributesPattern.matcher(tag).find()) { // MaryXML attributes of some token preceding the previous token
    		String tempString = tag.replaceAll("previousMinus","");
    		String newString = tempString.replaceAll("Attributes","");
    		int num = Integer.parseInt(newString);
    		
    		if(position>num) {
				otherToken = (Element)tokens.item(position-(num+1));
			}
    	}
		
		return checkAttributes(currentRulePart,otherToken);
    }
    
    /** Checks if tokenValue is contained in list.
     * This base implementation is able to deal with list types
     * represented as Sets; subclasses may override this method
     * to be able to deal with different list representations.
     * @param currentVal the condition to check; can be either <code>INLIST:</code>
     * or <code>!INLIST:</code> followed by the list name to check.
     * @param tokenValue value to look up in the list
     * @return whether or not tokenValue is contained in the list.
     */
    protected boolean checkList(String currentVal, String tokenValue) {
        if (currentVal == null || tokenValue == null) {
            throw new NullPointerException("Received null argument");
        }
        if (!currentVal.startsWith("INLIST") && !currentVal.startsWith("!INLIST")) {
            throw new IllegalArgumentException("currentVal does not start with INLIST or !INLIST");
        }
        boolean negation = currentVal.startsWith("!");
        String listName = currentVal.substring(currentVal.indexOf(":")+1);
        Object listObj = listMap.get(listName);
        if (listObj == null) return false; // no list found
        boolean contains;
        if (listObj instanceof Set) {
            Set set = (Set) listObj;
            contains = set.contains(tokenValue);
        } else {
            throw new IllegalArgumentException("Unknown list representation: " + listObj);
        }
        if (contains && negation || !contains && !negation) return false;
        else return true;
    }
    
    /** determination of sentence type
     * values: decl, excl, interrog, interrogYN or interrogW
     */ 
    protected String getSentenceType(NodeList tokens) {
    	String sentenceType="decl";
    	
    	for (int i = tokens.getLength() - 1; i >= 0; i--) { // search for sentence finishing punctuation mark
            Element t = (Element) tokens.item(i);
            String punct = MaryDomUtils.tokenText(t);
            if(punct.equals(".")) {
            	sentenceType = "decl";
            	break;
            }
            else if(punct.equals("!")) {
            	sentenceType = "excl";
            	break;
            }
            else if(punct.equals("?")) {
            	sentenceType = "interrog";
            	break;
            }
        }
        
    	if(sentenceType.equals("interrog")) {
    		for (int i=0; i<tokens.getLength()-1; i++) { // search for the first word in sentence
    			Element t = (Element) tokens.item(i);
    			if(!t.getAttribute("sampa").equals("")) {
    				Element firstToken = (Element)tokens.item(i);
            	
    				// setInterrogYN contains possible part of speechs of first word in yes-no question
    				HashSet setInterrogYN = ((HashSet)listMap.get("firstPosInQuestionYN"));
    				// setInterrogW contains possible part of speechs of first word in wh-question
    				HashSet setInterrogW = ((HashSet)listMap.get("firstPosInQuestionW"));
            	
    				String posFirstWord = firstToken.getAttribute("pos");
    				if(setInterrogYN != null && setInterrogYN.contains(posFirstWord)) {
    					sentenceType = "interrogYN";
    				}
    				if(setInterrogW != null && setInterrogW.contains(posFirstWord)) {
    					sentenceType = "interrogW";
    				}
    				break;
    			}
    		}
    	}
        return sentenceType;
    }

    /**
     * Assign an accent to the given token.
     * @param token a token element
     * @param accent the accent string to assign.
     */
    protected void setAccent(Element token, String accent)
    {
        token.setAttribute("accent", accent);
    }
    

    
    /**
     * Insert a boundary after token, with the given tone and breakindex.
     * If a boundary element already exists after token (but before the
     * following token), it is reused, if both token and boundary have the 
     * same parent node. In addition, if token is punctuation, a boundary 
     * preceding token can be reused, if both have the same parent node.
     * When choosing between the values already given in the existing element
     * and the ones passed as arguments to this function, the higher /
     * more concrete values are taken:
     * Only if bi is higher than an already existing breakindex, the old
     * value is replaced with bi. Only if tone is a concrete tone (like "h-")
     * and the previous tone was "unknown" or not specified at all,
     * tone is taken into account.
     * @return the boundary element on success, null on failure.
     */
    protected Element insertBoundary(Element token, String tone, int bi) {
        // Search for an existing boundary after token
        Element boundary = null;
        logger.debug("insertBoundary: after token `"+MaryDomUtils.tokenText(token)+"', tone "+tone+", bi "+bi);
        Document doc = token.getOwnerDocument();
        TreeWalker tw = ((DocumentTraversal)doc).createTreeWalker(
                DomUtils.getAncestor(token, MaryXML.SENTENCE), NodeFilter.SHOW_ELEMENT,
                new NameNodeFilter(new String[] {MaryXML.BOUNDARY, MaryXML.TOKEN}), false);
        tw.setCurrentNode(token);
        Element next = (Element) tw.nextNode();
        if (next != null && next.getTagName().equals(MaryXML.BOUNDARY)) {
            boundary = next;
        } else if (isPunctuation(token)) {
            // if the current token is punctuation, we also look for a
            // boundary before the current token
            tw.setCurrentNode(token);
            Element prev = (Element) tw.previousNode();
            if (prev != null && prev.getTagName().equals(MaryXML.BOUNDARY)) {
                boundary = prev;
            }
        }
        // Reuse a boundary tag if it has the same parent as the token
        if (boundary != null
            && boundary.getParentNode().equals(token.getParentNode())) {
            // the tone:
            if (tone != null) {
                String tagTone = boundary.getAttribute("tone");
                // Use tone given as parameter to this method:
                // - if no tone attribute is given in the tag, or
                // - if tone parameter is a concrete tone symbol and
                //   tagTone is "unknown"
                if (tagTone.equals("") || !tone.equals("unknown") && tagTone.equals("unknown")) {
                    boundary.setAttribute("tone", tone);
                }
            }
            // the break index:
            if (bi > 0) {
                String tagBIString = boundary.getAttribute("breakindex");
                // Use bi given as parameter to this method:
                // - if no breakindex attribute is given in the tag
                // - if bi is a larger breakindex than tagBI
                if (tagBIString.equals("") || tagBIString.equals("unknown")) {
                    boundary.setAttribute("breakindex", String.valueOf(bi));
                } /*else {
                    try {
                        int tagBI = Integer.parseInt(tagBIString);
                        if (tagBI < bi) {
                            boundary.setAttribute("breakindex", String.valueOf(bi));
                        }
                    } catch (NumberFormatException e) {
                    } // ignore, do nothing
                }*/
            }
        } else { // no boundary tag yet, introduce one
            // First verify that we have a valid parent element
            if (token.getParentNode() == null) {
                return null;
            }
            // Make sure not to insert the new boundary
            // in the middle of an <mtu> element:
            Element eIn = (Element) token.getParentNode();
            Element eBefore = MaryDomUtils.getNextSiblingElement(token);
            // Now change these insertion references in case token
            // is the last one in an <mtu> tag.
            Element mtu = (Element) MaryDomUtils.getHighestLevelAncestor(token, MaryXML.MTU);
            if (mtu != null) {
                if (MaryDomUtils.isLastOfItsKindIn(token, mtu)) {
                    eIn = (Element) mtu.getParentNode();
                    eBefore = MaryDomUtils.getNextSiblingElement(mtu);
                } else {
                    // token is in the middle of an mtu - don't insert boundary
                    return null;
                }
            }
            // Now the boundary tag is to be inserted.
            boundary = MaryXML.createElement(doc, MaryXML.BOUNDARY);
            if (tone != null) {
                boundary.setAttribute("tone", tone);
            }
            if (bi > 0) {
                boundary.setAttribute("breakindex", String.valueOf(bi));
            }
            eIn.insertBefore(boundary, eBefore);
        } // add new boundary
        return boundary;
    }
    
    
    /**
     * Insert a major boundary after token number <code>i</code> in
     * <code>tokens</code>.
     * <p>
     * Also inserts a phrase tag at the appropriate position.
     * @return The boundary element.
     */
    protected Element insertMajorBoundary(NodeList tokens, int i, Element firstToken, String tone, int breakindex) {
        Element boundary = insertBoundary((Element) tokens.item(i), tone, breakindex);
        insertPhraseNode(firstToken, boundary);
        return boundary;
    }

    /**
     * Inserte a phrase element, enclosing the first and last element,
     * into the tree. Typically first element would be a token, last element
     * a boundary.
     * @return true on success, false on failure.
     */
    protected boolean insertPhraseNode(Element first, Element last) {
        // Allow for the exotic case that a <phrase> should start with a <boundary/> element:
        Element encloseFromHere = first;
        Element maybeBoundary = DomUtils.getPreviousSiblingElement(first);
        if (maybeBoundary != null && maybeBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
            encloseFromHere = maybeBoundary;
        }
        // Take existing trailing boundary elements into the new phrase:
        Element encloseToHere = last;
        maybeBoundary = DomUtils.getNextSiblingElement(last);
        if (maybeBoundary != null && maybeBoundary.getTagName().equals(MaryXML.BOUNDARY)) {
            encloseToHere = maybeBoundary;
        }
        Element phrase = MaryDomUtils.encloseNodesWithNewElement(encloseFromHere, encloseToHere, MaryXML.PHRASE);
        return phrase != null;
    }

    /**
     * Verify whether this Node has a parent preventing the application
     * of intonation rules.
     * @return <code>true</code> if rules are to be applied,
     * <code>false</code> otherwise.
     */
    protected boolean applyRules(Node n) {
        Element intonation = (Element) MaryDomUtils.getAncestor(n, MaryXML.PROSODY);
        if (intonation != null && intonation.getAttribute("rules").equals("off")) {
            return false;
        } else {
            return true;
        }
    }

    
    /**
     * Go through all tokens in a document, and copy any accents to the first
     * accented syllable.
     */
    protected void copyAccentsToSyllables(Document doc)
    {
        NodeIterator tIt = ((DocumentTraversal)doc).createNodeIterator
            (doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.TOKEN), false);
        Element t = null;
        while ((t = (Element) tIt.nextNode()) != null) {
            if (t.hasAttribute("accent")) {
                NodeIterator sylIt = ((DocumentTraversal)doc).createNodeIterator
                    (t, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.SYLLABLE), false);
                boolean assignedAccent = false;
                Element syl = null;
                while ((syl = (Element) sylIt.nextNode()) != null) {
                    if (syl.getAttribute("stress").equals("1")) {
                        // found
                        syl.setAttribute("accent", t.getAttribute("accent"));
                        assignedAccent = true;
                        break; // done for this token
                    }
                }
                if (!assignedAccent) {
                    // Hmm, this token does not have a stressed syllable --
                    // take the first syllable then:
                    syl = MaryDomUtils.getFirstElementByTagName(t, MaryXML.SYLLABLE);
                    if (syl != null) {
                        syl.setAttribute("accent", t.getAttribute("accent"));
                    }
                }
            }
        }
    }

    /**
     * Check whether <code>token</code> is enclosed by a
     * <code>&lt;prosody&gt;</code> element containing an attribute
     * <code>force-accent</code>.
     * @return the value of the <code>force-accent</code> attribute,
     * if one exists, or the empty string otherwise.
     */
    protected String getForceAccent(Element token) {
        // Search for the closest ancestor <prosody> element
        // which has a "force-accent" attribute:
        Element p = MaryDomUtils.getClosestAncestorWithAttribute(token, MaryXML.PROSODY, "force-accent");
        if (p != null)
            return p.getAttribute("force-accent");
        else
            return "";
    }
    
    /**
     * Verify whether a given token is a punctuation.
     * @param token the t element to be tested.
     * @return true if token is a punctuation, false otherwise.
     */
    protected boolean isPunctuation(Element token) {
        if (token == null)
            throw new NullPointerException("Received null token");
        if (!token.getTagName().equals(MaryXML.TOKEN))
            throw new IllegalArgumentException("Expected <" + MaryXML.TOKEN + "> element, got <" + token.getTagName() + ">");
        
        String tokenText = MaryDomUtils.tokenText(token);
        
        if (tokenText.equals(",") || tokenText.equals(".") || tokenText.equals("?") || tokenText.equals("!") || tokenText.equals(":")
        		|| tokenText.equals(";"))
            return true;
        else
            return false;
    }
	
}
