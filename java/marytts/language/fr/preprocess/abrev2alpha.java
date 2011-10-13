/**
 * Copyright 2011 DFKI GmbH.
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

/**
 * 
##########################################################################################
#                                                                                        #
#                       Lexique d'abréviations communes au français                      #                    
#                                    par Florent Xavier                                  #
#                   inspiré du site http://www.aidenet.eu/grammaire01h.htm               #
#                                           2011                                         #
#                                                                                        #
#                                                                                        #
#                                                                                        #
##########################################################################################

*/


package marytts.language.fr.preprocess;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;




/**
 * Preprocessing module for existing abbreviations in french.
 * 
 *
 * @author Florent Xavier
 */


public class abrev2alpha
{
	
	public static String txtInput = new String();
    
	//HashMap representing all the abbreviations and their equivalent in word.
	private final static Map<String,String> ABBREV_TO_WORD = new HashMap<String,String>();
    static {
    	ABBREV_TO_WORD.put("Dr", "Docteur");
    	ABBREV_TO_WORD.put("M.", "Monsieur"); 
    	ABBREV_TO_WORD.put("Mr", "Monsieur");
    	ABBREV_TO_WORD.put("Me", "Maître"); 
    	ABBREV_TO_WORD.put("Pr", "Professeur");
    	ABBREV_TO_WORD.put("MM.", "Messieurs"); 
    	ABBREV_TO_WORD.put("Mme", "Madame");
    	ABBREV_TO_WORD.put("Mmes", "Mesdames");
    	ABBREV_TO_WORD.put("Mlle", "Mademoiselle");
    	ABBREV_TO_WORD.put("Mlles", "Mesdemoiselles");
    	ABBREV_TO_WORD.put("Mgr", "Monseigneur");
    	ABBREV_TO_WORD.put("P.", "Père");
    	ABBREV_TO_WORD.put("R.P.", "Révérand père");
    	ABBREV_TO_WORD.put("P.D.G.", "Président directeur général");
    	ABBREV_TO_WORD.put("S.A.", "Son Altesse"); 
    	ABBREV_TO_WORD.put("S.A.R.", "Son Altesse Royale");
    	ABBREV_TO_WORD.put("S.E.", "Son Excellence"); 
    	ABBREV_TO_WORD.put("S.Exc.", "Son Excellence"); 
    	ABBREV_TO_WORD.put("S.M.", "Sa Majesté"); 
    	ABBREV_TO_WORD.put("Sr", "Sœur"); 
    	ABBREV_TO_WORD.put("S.S.", "Sa Sainteté"); 
    	ABBREV_TO_WORD.put("N.", "Nord");
    	ABBREV_TO_WORD.put("S.", "Sud");
    	ABBREV_TO_WORD.put("O.", "Ouest");
    	ABBREV_TO_WORD.put("E.", "Est"); 
    	ABBREV_TO_WORD.put("c.-à-d.", "c'est à dire"); 
    	ABBREV_TO_WORD.put("cap", "capitale d'un pays");
    	ABBREV_TO_WORD.put("C.c.", "Copie conforme");
    	ABBREV_TO_WORD.put("Cie", "Compagnie");
    	ABBREV_TO_WORD.put("dép.", "département");
    	ABBREV_TO_WORD.put("env.", "environ");
    	ABBREV_TO_WORD.put("Etc.", "étssétèra"); 
    	ABBREV_TO_WORD.put("fg", "faubourg");
    	ABBREV_TO_WORD.put("fo", "folio");
    	ABBREV_TO_WORD.put("fr.", "français");
        ABBREV_TO_WORD.put("franç.", "français");
        ABBREV_TO_WORD.put("hab.", "habitants");
        ABBREV_TO_WORD.put("ht", "hors taxes");
        ABBREV_TO_WORD.put("id.", "idem");
        ABBREV_TO_WORD.put("in-fo", "in-folio");
        ABBREV_TO_WORD.put("ms", "manuscrit");
        ABBREV_TO_WORD.put("P.i.", "Par intérim");
        ABBREV_TO_WORD.put("P.o.", "Par ordre");
        ABBREV_TO_WORD.put("P-S", "Post-scriptum");
        ABBREV_TO_WORD.put("P.-S.", "Post-scriptum");
        ABBREV_TO_WORD.put("ro", "recto");
        ABBREV_TO_WORD.put("RSVP", "Répondez s'il vous plaît");
        ABBREV_TO_WORD.put("SA", "Société anonyme");
        ABBREV_TO_WORD.put("SARL", "Société à responsabilité limitée");
        ABBREV_TO_WORD.put("Sté", "Société");
        ABBREV_TO_WORD.put("TVA", "taxe sur la valeur ajoutée");
        ABBREV_TO_WORD.put("Tél.", "téléphone");
        ABBREV_TO_WORD.put("ttc", "toutes taxes comprises");
        ABBREV_TO_WORD.put("vo", "verso");
        ABBREV_TO_WORD.put("W.C.", "V C");
        ABBREV_TO_WORD.put("J.C.", "Jésus-Christ");
        ABBREV_TO_WORD.put("adj.", "adjectif");
        ABBREV_TO_WORD.put("adv.", "adverbe");
        ABBREV_TO_WORD.put("art.", "article");
        ABBREV_TO_WORD.put("att.", "attribut");
        ABBREV_TO_WORD.put("auxil.", "auxiliaire");
        ABBREV_TO_WORD.put("av.", "avant");
        ABBREV_TO_WORD.put("card.", "cardinal");
        ABBREV_TO_WORD.put("chap.", "chapitre");
        ABBREV_TO_WORD.put("CC",  "complément circonstanciel");
        ABBREV_TO_WORD.put("COD", "complément d’objet direct");
        ABBREV_TO_WORD.put("COI", "complément d’objet indirect");
        ABBREV_TO_WORD.put("COS", "complément d’objet second");
        ABBREV_TO_WORD.put("conj.", "conjonction");
        ABBREV_TO_WORD.put("dém.", "démonstratif");
        ABBREV_TO_WORD.put("fém.", "féminin");
        ABBREV_TO_WORD.put("fr.", "français");
        ABBREV_TO_WORD.put("gramm.", "grammaire");
        ABBREV_TO_WORD.put("gr.", "groupe");
        ABBREV_TO_WORD.put("GN", "groupe nominal");
        ABBREV_TO_WORD.put("GNS", "groupe nominal sujet");
        ABBREV_TO_WORD.put("GV", "groupe verbal");
        ABBREV_TO_WORD.put("interj.", "interjection");
        ABBREV_TO_WORD.put("introd.", "introduction");
        ABBREV_TO_WORD.put("inv.", "invariable");
        ABBREV_TO_WORD.put("lat.", "latin");
        ABBREV_TO_WORD.put("loc.", "locution");
        ABBREV_TO_WORD.put("loc.", "adj locution adjective");  
        ABBREV_TO_WORD.put("loc. conj.", "locution conjonctive");
        ABBREV_TO_WORD.put("loc. interj.", "locution interjective");
        ABBREV_TO_WORD.put("loc. v.", "locution verbale");
        ABBREV_TO_WORD.put("masc.", "masculin");
        ABBREV_TO_WORD.put("n.", "nom");
        ABBREV_TO_WORD.put("n. c.", " nom commun");
        ABBREV_TO_WORD.put("N.B.", "nota bene"); 
        ABBREV_TO_WORD.put("numér.", "numéral");
        ABBREV_TO_WORD.put("ord.", "ordinal");
        ABBREV_TO_WORD.put("p.", "page");
        ABBREV_TO_WORD.put("ex.", "exercice");
        ABBREV_TO_WORD.put("ex", "exemple");
        ABBREV_TO_WORD.put("part.", "participe");
        ABBREV_TO_WORD.put("part. passé", "participe passé");
        ABBREV_TO_WORD.put("part. prés.", "participe présent");
        ABBREV_TO_WORD.put("pl.", "pluriel");
        ABBREV_TO_WORD.put("poss.", "possessif");
        ABBREV_TO_WORD.put("prép.", "préposition");
        ABBREV_TO_WORD.put("pron.", "pronom");
        ABBREV_TO_WORD.put("prop.", "proposition");
        ABBREV_TO_WORD.put("réf.", "référence");
        ABBREV_TO_WORD.put("sing.", "singulier");
        ABBREV_TO_WORD.put("sub.", "subordonnée");
        ABBREV_TO_WORD.put("suiv.", "suivant");
        ABBREV_TO_WORD.put("v.", "verbe");
        ABBREV_TO_WORD.put("v. a.", "verbe actif");
        ABBREV_TO_WORD.put("v. tr.", "verbe transitif");
        ABBREV_TO_WORD.put("v. impers.", "verbe impersonnel");
        ABBREV_TO_WORD.put("v. intr.", "verbe intransitif");
        ABBREV_TO_WORD.put("v.pr.", "verbe pronominal");
        ABBREV_TO_WORD.put("coll.", "collection");
            
            
    };
    
    
    
        
    
    /**
     * Method that replaces the abbreviations into their equivalent.
     * 
     * @return
     */
	public static String abbrevReplace ()
    {    	
		StringBuilder newTxt = new StringBuilder();        
        StringTokenizer st = new StringTokenizer(txtInput);
    	String cleanTxt = new String();
    	String str = new String();
        while (st.hasMoreTokens()) 
        {
        	str = st.nextToken();
        	cleanTxt = ABBREV_TO_WORD.get(str);
        	//if we don't have to replace anything.
        	if (cleanTxt == null)
            {
                cleanTxt = str;
            }
            newTxt.append(cleanTxt).append(" ");
        }
        return newTxt.toString();
    }
    
   

    
}



