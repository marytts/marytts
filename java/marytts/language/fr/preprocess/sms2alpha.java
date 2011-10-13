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


package marytts.language.fr.preprocess;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Simple class trying to transcribe the SMS language into proper french.
 * The SMS language is mostly based on phonetic language. So we only need
 * to transcribe abbreviations. Sometimes, some digits are written, for their
 * phonetic transcription, for example the sentence "c'est un singe" would be 
 * in SMS language "7 1 s1ge". The first two digit won't cause any problems.
 * But the "1" in "s1ge" is pretty though to transcribe, so we won't do anything
 * for this... More rarely, some people write caps letter for the sound. For
 * instance, the line "G mal" would be "J'ai mal", or "1TgriT", would be 
 * "intégrité". We don't have any good solution for this problem. So we
 * put in a map the most used words that are written this way.
 * 
 * @author florent
 *
 */

public class sms2alpha 
{
	public static String txtInput = new String();
	
	static final Map <String, String> SMS_TO_WORD = new HashMap <String, String>();
	static{
		SMS_TO_WORD.put("tkt", "t'inquiètes");
		SMS_TO_WORD.put("tp", "temps");
		SMS_TO_WORD.put("dc", "donc");
		SMS_TO_WORD.put("mn", "mon");
		SMS_TO_WORD.put("tn", "ton");
		SMS_TO_WORD.put("sn", "son");
		SMS_TO_WORD.put("cpdt", "cependant");
		SMS_TO_WORD.put("pdt", "pendant");
		SMS_TO_WORD.put("qd", "quand");
		SMS_TO_WORD.put("mm", "même");
		SMS_TO_WORD.put("lS", "laisse");
		SMS_TO_WORD.put("Gt", "j'étais");
		SMS_TO_WORD.put("tg", "ta gueule");
		SMS_TO_WORD.put("pk", "pourquoi");
		SMS_TO_WORD.put("pck", "parce que");
		SMS_TO_WORD.put("yaV", "y'avait");
		SMS_TO_WORD.put("aV", "avait");
		SMS_TO_WORD.put("bjr", "bonjour");
		SMS_TO_WORD.put("slt", "salut");
		SMS_TO_WORD.put("kn", "qu'on");
		SMS_TO_WORD.put("ntm", "n'aime que ta maman");
		SMS_TO_WORD.put("pr", "pour");
		SMS_TO_WORD.put("bn", "bon");
		SMS_TO_WORD.put("a+", "à pluss");
		SMS_TO_WORD.put("@+", "à pluss");
		SMS_TO_WORD.put("bi1", "bien");
		SMS_TO_WORD.put("tt", "tout");
		SMS_TO_WORD.put("2m1", "demain");
		SMS_TO_WORD.put("HT", "acheter");
		SMS_TO_WORD.put("ok1", "aucun");
		SMS_TO_WORD.put("aréT", "arrêté");
		SMS_TO_WORD.put("bi1to", "bientôt");
		SMS_TO_WORD.put("bsr", "bonsoir");
		SMS_TO_WORD.put("Kdo", "cadeau");
		SMS_TO_WORD.put("Kfé", "café");
		SMS_TO_WORD.put("Kc", "cassé");
		SMS_TO_WORD.put("ayé", "ca y'est");
		SMS_TO_WORD.put("chanG", "changé");
		SMS_TO_WORD.put("klR", "clair");
		SMS_TO_WORD.put("ds", "dans");
		SMS_TO_WORD.put("dslé", "désolé");
		SMS_TO_WORD.put("2mand", "demande");
		SMS_TO_WORD.put("tjr", "toujours");
		SMS_TO_WORD.put("ns", "nous");
		SMS_TO_WORD.put("vs", "vous");
		SMS_TO_WORD.put("jr", "jour");
		SMS_TO_WORD.put("asap", "aussi vite que possible");
		SMS_TO_WORD.put("fR", "faire");
		SMS_TO_WORD.put("alr", "alors");
		SMS_TO_WORD.put("msg", "message");
		SMS_TO_WORD.put("mnt", "maintenant");
		SMS_TO_WORD.put("OQP", "occupé");
		SMS_TO_WORD.put("OK", "oké");
		SMS_TO_WORD.put("nb", "nombre");
		SMS_TO_WORD.put("manG", "manger");
		SMS_TO_WORD.put("mat1", "matin");
		SMS_TO_WORD.put("aprM", "après midi");
		SMS_TO_WORD.put("dja", "déjà");
		SMS_TO_WORD.put("jT", "je t'ai");
		SMS_TO_WORD.put("tma", "tu m'as");
		SMS_TO_WORD.put("jtm", "je t'aime");
		SMS_TO_WORD.put("jtd", "je t'adore");
		SMS_TO_WORD.put("ac", "avec");
		SMS_TO_WORD.put("av", "avant");
		SMS_TO_WORD.put("st", "sont");
		SMS_TO_WORD.put("apL", "appelle");
		SMS_TO_WORD.put("svt", "souvent");
		SMS_TO_WORD.put("jC", "je sais");
		SMS_TO_WORD.put("aC", "assez");
		SMS_TO_WORD.put("stp", "s'il te plaît");
		SMS_TO_WORD.put("svp", "s'il vous plaît");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		SMS_TO_WORD.put("", "");
		
	};
	
	
	
	 /**
     * Method that replaces the sms words into their equivalent.
     * 
     * @return
     */
	public static String smsReplace ()
    {    	
		StringBuilder newTxt = new StringBuilder();        
        StringTokenizer st = new StringTokenizer(txtInput);
    	String cleanTxt = new String();
    	String str = new String();
        while (st.hasMoreTokens()) 
        {
        	str = st.nextToken();
        	cleanTxt = SMS_TO_WORD.get(str);
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
