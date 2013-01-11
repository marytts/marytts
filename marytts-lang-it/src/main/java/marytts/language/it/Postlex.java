/**
 * Copyright 2002 DFKI GmbH.
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

package marytts.language.it;

import java.util.LinkedList;
import java.util.Locale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.language.it.phonemiser.JPhonemiser;
import marytts.modules.MaryModule;
import marytts.modules.ModuleRegistry;
import marytts.modules.PronunciationModel;
import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * The postlexical phonological processes module. Used as first option to solve
 * proclitics tokens and sillabification merging
 * 
 * @author Giulio Paci
 */

public class Postlex extends PronunciationModel {
	AllophoneSet as = null;
	boolean hasAllophoneSet = true;

	public Postlex() {
		super(Locale.ITALIAN);
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		mtuPostlex(doc);
		return super.process(d);
	}

	private void mergeIntoLastElement(Element c1, Element c2) {
		if ((c1 != null) && (c1 != null)) {
			c2.setAttribute("merged-token", "yes");
			c2.setAttribute("g2p_method", "compound");
			c2.setTextContent(c1.getTextContent() + "+" + c2.getTextContent());
			c2.setAttribute("ph", this.fixSyllabification(c1.getAttribute("ph") + " " + c2.getAttribute("ph")));
		}
	}



    /**
     * Convert a phone string into a list of string representations of
     * individual phones. The input can use the suffix "1" to indicate
     * stressed vowels.  
     * @param phoneString the phone string to split
     * @return a linked list of strings, each string representing an individual phone
     */
    protected LinkedList<String> splitIntoAllophones(String phoneString)
    {
        LinkedList<String> phoneList = new LinkedList<String>();
        for (int i=0; i<phoneString.length(); i++) {
            for (int j=3; j>=1; j--) {
                if (i+j <= phoneString.length()) {
                    phoneList.add(phoneString.substring(i, i+j));
                }
            }
        }
        return phoneList;
    }

	private String fixSyllabification(String s) {
		if (this.hasAllophoneSet == true && this.as == null) {
			JPhonemiser phonemiser = null;
			for (MaryModule module : ModuleRegistry.getAllModules()) {
				if (Locale.ITALIAN.equals(module.getLocale())
						&& JPhonemiser.class.equals(module.getClass())) {
					phonemiser = (JPhonemiser) module;
				}
			}
			if (phonemiser == null) {
				this.hasAllophoneSet = false;
			}else{
				this.as = phonemiser.getAllophoneSet();
				if (this.as == null) {
					this.hasAllophoneSet = false;
				}
			}
		}
		if(this.as == null){
			return s;
		}
		String phones[] = s.split("\\s+");
		int last_accent = -1;
		int last_second_accent = -1;
		for (int i = 0; i < phones.length; i++) {
			if(phones[i].equals("'")){
				if(last_accent != -1){
					phones[last_accent] = null;//"\"";
				}
				last_accent = i;
			}
			else if(phones[i].equals("\"")){
				last_second_accent = i;
				phones[i] = null;
			}
		}
		if((last_accent == -1)&&(last_second_accent > -1)){
			phones[last_second_accent] = "'";
		}

		String last_accent_text = null;	
		StringBuilder sb = new StringBuilder();
		int previous_vowel = -1;
		last_accent = -1;
		int last_printed_phone = -1;
		int k;
		for (int i = 0; i < phones.length; i++) {
			if(phones[i]==null){
				continue;
			}
			Allophone a = as.getAllophone(phones[i]);
			//System.out.println(i + " " + phones[i]);
			if (a != null) {
				if (a.isVowel()) {
					// This is a pointer to the last phone of the previous
					// syllable.
					int break_point = i-1;
					if(previous_vowel > -1){
						// TODO find better break point between vowels
						break_point = Math.min(i,Math.max(previous_vowel,(int)((i+previous_vowel)/2)));
					}
					// Flush phones until break point
					if (previous_vowel > -1) {
					if (last_printed_phone <= break_point) {
						for (last_printed_phone++; last_printed_phone <= break_point; last_printed_phone++) {
							if (phones[last_printed_phone] != null) {
								sb.append(phones[last_printed_phone]);
								sb.append(" ");
								//System.out.println("FLUSH(V) " + phones[last_printed_phone]);
							}
						}
						last_printed_phone--;
					}
						//System.out.println("ADD(V) -");
						sb.append("- ");
					}
					previous_vowel = i;
					// Check if there is an accent between vowels, if yes the
					// syllable should start with an accent
					if (last_accent > -1) {
						//System.out.println("ACCENT("+last_accent+") " + last_accent_text);
						// If there is an accent between vowels, the syllable
						// should start with an accent
						sb.append(last_accent_text);
						sb.append(" ");
					}
					last_accent = -1;
				}
			} else if (phones[i].equals("-")) {
				if(previous_vowel > -1){
				for (last_printed_phone++; last_printed_phone <= i; last_printed_phone++) {
					if (phones[last_printed_phone] != null) {
						//System.out.println("FLUSH(-) " + phones[last_printed_phone]);
						sb.append(phones[last_printed_phone]);
						sb.append(" ");
					}
				}
				last_printed_phone--;
				} else {
					phones[i] = null;
				}
				previous_vowel = -1;
			} else if( (phones[i].equals("'")) ||(phones[i].equals("\""))){
				last_accent = i;
				last_accent_text = phones[i];
				phones[i] = null;
			} else {

			}
		}
		for (k = last_printed_phone + 1; k < phones.length; k++) {
			if (phones[k] != null) {
			sb.append(phones[k]);
			sb.append(" ");
			}
		}
		return sb.toString();
	}

	/*
	 * This method is used when proclitics are found in mtu proclitics is c'X
	 * (if there is in it_clitics.xml)
	 */
	private void mtuPostlex(Document doc) throws DOMException {
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(MaryXML.MTU), false);
		Element m = null;
		while ((m = (Element) tw.nextNode()) != null) {
			if (MaryDomUtils.hasAncestor(m, MaryXML.MTU)) // not highest-level
				continue;
			// Now m is a highest-level mtu element
			Element c = m;
			while (c != null && !c.getTagName().equals(MaryXML.TOKEN)) {
				String whatToAccent = c.getAttribute("accent");
				// check for last-proclitics (c' t' d' X)
				if (whatToAccent != null
						&& whatToAccent.equals("last-proclitics")) {
					Element c1 = MaryDomUtils.getFirstChildElement(c);
					boolean done = false;
					if (c1 != null) {
						while (!done) {
							done = true;
							Element c2 = MaryDomUtils.getNextSiblingElement(c1);
							if ((c2 != null) && (MaryXML.TOKEN.equals(c1.getTagName()))) {
								if (MaryXML.TOKEN.equals(c2.getTagName())) {
									this.mergeIntoLastElement(c1, c2);
									c.removeChild(c1);
									c1 = c2;
									done = false;
								} else if (MaryXML.MTU.equals(c2.getTagName())) {
									while ((c2 != null) && (!MaryXML.TOKEN.equals(c2.getTagName()))) {
										c2 = MaryDomUtils.getFirstChildElement(c2);
									}
									if (c2 != null) {
										this.mergeIntoLastElement(c1, c2);
										c.removeChild(c1);
									}
								}
							}
						}
					}
				}
				c = MaryDomUtils.getLastChildElement(c);
			}
		} // for all highest-level mtu elements
	}

}
