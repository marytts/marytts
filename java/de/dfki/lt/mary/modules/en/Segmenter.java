/**
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package de.dfki.lt.mary.modules.en;

import com.sun.speech.freetts.UtteranceProcessor;
import com.sun.speech.freetts.Utterance;
import com.sun.speech.freetts.FeatureSet;
import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.ProcessException;

import com.sun.speech.freetts.lexicon.Lexicon;
import com.sun.speech.freetts.lexicon.LexiconImpl;

import java.io.*;
import java.net.URL;
import java.util.*;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.modules.phonemiser.*;
import de.dfki.lt.mary.unitselection.featureprocessors.PhoneSet;
import de.dfki.lt.mary.unitselection.featureprocessors.PhoneSetImpl;

/**
 * Annotates an utterance with <code>Relation.SYLLABLE</code>,
 * <code>Relation.SYLLABLE_STRUCTURE</code>, and
 * <code>Relation.SEGMENT</code>.
 * To determine stress, the <code>isStressed</code> method relies upon
 * a phone ending in the number "1".  Subclasses should override
 * <code>isStressed</code> and <code>deStress</code> if stresses are
 * determined in other ways.
 *
 * @see Relation#SEGMENT
 * @see Relation#SYLLABLE
 * @see Relation#SYLLABLE_STRUCTURE
 */
public class Segmenter implements UtteranceProcessor {
    private final static String STRESS = "1";
    private final static String NO_STRESS = "0";
    private Map addenda;

    /**
     * Annotates an utterance with <code>Relation.SYLLABLE</code>,
     * <code>Relation.SYLLABLE_STRUCTURE</code>, and
     * <code>Relation.SEGMENT</code>.
     *
     * @param utterance the utterance to process/tokenize
     *
     * @see Relation#SEGMENT
     * @see Relation#SYLLABLE
     * @see Relation#SYLLABLE_STRUCTURE
     *
     * @throws ProcessException if an IOException is thrown during the
     *   processing of the utterance
     */
    public void processUtterance(Utterance utterance) throws ProcessException {

	// preconditions
        if (utterance.getRelation(Relation.WORD) == null) {
            throw new IllegalStateException(
                "Word relation has not been set");
        } else if (utterance.getRelation(Relation.SYLLABLE) != null) {
            throw new IllegalStateException(
                "Syllable relation has already been set");
        } else if (utterance.getRelation(Relation.SYLLABLE_STRUCTURE)
                   != null) {
            throw new IllegalStateException(
                "SylStructure relation has already been set");
        } else if (utterance.getRelation(Relation.SEGMENT) != null) {
            throw new IllegalStateException(
                "Segment relation has already been set");
        }

	String stress = NO_STRESS;
	Relation syl = utterance.createRelation(Relation.SYLLABLE);
	Relation sylstructure =
            utterance.createRelation(Relation.SYLLABLE_STRUCTURE);
	Relation seg = utterance.createRelation(Relation.SEGMENT);
	String voicename = utterance.getVoice().getName();
	Lexicon lex = utterance.getVoice().getLexicon();
	
	List syllableList = null;

	for (Item word = utterance.getRelation(Relation.WORD).getHead();
			word != null; word = word.getNext()) {
	    Item ssword = sylstructure.appendItem(word);
	    Item sylItem = null;   // item denoting syllable boundaries
	    Item segItem = null;   // item denoting phonelist (segments)
	    Item sssyl = null;     // item denoting syl in word

	    String[] phones = null;

        // Part-of-speech:
        String pos = "0";
        FeatureSet wordFeatures = word.getFeatures();
        if (wordFeatures != null && wordFeatures.isPresent("pos")) {
            pos = wordFeatures.getString("pos");
            pos = pos.toLowerCase();
            // TODO: create a proper mapping for parts of speech:
            if (pos.charAt(0) == 'd') pos = "d";
            // here we just distinguish determiners from the rest of the world.
        }

        Item token = word.getItemAs("Token");
	    FeatureSet featureSet = null;

	    if (token != null) {
	        Item parent = token.getParent();
	        featureSet = parent.getFeatures();
	    }
	    String wordString = word.toString();
	    if (featureSet != null && featureSet.isPresent("phones")) {
	        phones = (String[]) featureSet.getObject("phones");
	    } else {
	        boolean useLTSRules = 
	            MaryProperties.getBoolean("english.lexicon.useLTSrules", true);
	        
	        if (useLTSRules){
	            //System.out.println("Using LTSRules");
	            //if word not in lex, use letter to sound rules
	            phones = lex.getPhones(wordString, pos, true);
	        } else {
	            //System.out.println("Not using LTSRules");
	            //dont use letter to sound rules
	            phones = lex.getPhones(wordString, pos, false);
	        }
		
	    }

	    if (phones == null){
	        //System.out.println("Phones = null for word "+wordString);
	        //phones must be generated with lts rules
	        //add generated phones to addenda
	        phones = lex.getPhones(wordString, pos, true);	        
	        
	        StringBuffer ltsPhoneString = new StringBuffer();
	        for (int i=0;i<phones.length;i++){
	            ltsPhoneString.append(phones[i]+" ");
	        }
	        
	        lex.addAddendum(wordString,null,phones);
	        if (addenda == null) addenda = new HashMap();
	        addenda.put(wordString,ltsPhoneString);	           
	    } 
	    
	    for (int j = 0; j < phones.length; j++) {
		if (sylItem == null) {
		    sylItem = syl.appendItem();
		    sssyl = ssword.addDaughter(sylItem);
		    stress = NO_STRESS;
		    syllableList = new ArrayList();
		}
		segItem = seg.appendItem();
		if (isStressed(phones[j])) {
		    stress = STRESS;
		    phones[j] = deStress(phones[j]);
		}
		segItem.getFeatures().setString("name", phones[j]);
		sssyl.addDaughter(segItem);
		syllableList.add(phones[j]);
		if (lex.isSyllableBoundary(syllableList, phones, j + 1))  { 
		    sylItem =  null;
		    if (sssyl != null) {
			sssyl.getFeatures().setString("stress", stress);
		    }
		}
	    }
	    
	}

	assert utterance.getRelation(Relation.WORD) != null;
	assert utterance.getRelation(Relation.SYLLABLE) != null;
	assert utterance.getRelation(Relation.SYLLABLE_STRUCTURE) != null;
	assert utterance.getRelation(Relation.SEGMENT) != null;
    }

    /**
     * Determines if the given phonemene is stressed.
     * To determine stress, this method relies upon
     * a phone ending in the number "1".  Subclasses should override this
     * method if stresses are determined in other ways.
     *
     * @param phone the phone to check
     *
     * @return true if the phone is stressed, otherwise false
     */
    protected boolean isStressed(String phone) {
	return phone.endsWith("1");
    }

    /**
     * Converts stressed phoneme to regular phoneme.  This method
     * merely removes the last character of the phone.  Subclasses
     * should override this if another method is to be used.
     *
     * @param phone the phone to convert
     *
     * @return de-stressed phone
     */
    protected String deStress(String phone) {
	String retPhone = phone;
	if (isStressed(phone)) {
	    retPhone = phone.substring(0, phone.length() - 1);
	}
	return retPhone;
    }
    
    
   
   
    
    public void saveAddenda() throws IOException{
        String path = MaryProperties.maryBase()+"/log/addenda.log";
        if (addenda != null && path != null){
            try{
                String line;
                StringBuffer addendaBuf = new StringBuffer();    
                addendaBuf.append("# Logfile contains words that are not in the lexicon"
                        +"\n# and their transcriptions predicted by the LTS-rules"
                        +"\n# (for English)\n");
                //add new entries
                Set words = addenda.keySet();
                for (Iterator it = words.iterator();it.hasNext();){
                    String nextWord = (String) it.next();
                    addendaBuf.append(nextWord+" "
                            +addenda.get(nextWord)+"\n");
                }
                
                //open addenda file
                PrintWriter addendaOut = new PrintWriter(
                        new FileOutputStream(new File(path)));
                //print addendaBuf and close
                addendaOut.print(addendaBuf.toString());
                addendaOut.flush();
                addendaOut.close();
            } catch (Exception e){
                throw new IOException("Could not save addenda because : "
                        +e.getMessage());
            }
            
        }        
    }

    /**
     * Returns the simple name of this class.
     *
     * @return the simple name of this class
     */
    public String toString() {
        return "Segmenter";
    }
}

