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
 * Cleaning the currencies.
 *
 * @author Florent Xavier
 */

public class devis2alpha
{
	public static String txtInput = new String();
	
	private final static Map<String,String> CURRENCY_TO_WORD = new HashMap<String,String>();
    static { //according to the ISO 4217 standard.
    	CURRENCY_TO_WORD.put("DM", "Mark");CURRENCY_TO_WORD.put("$", "Dollar");
    	CURRENCY_TO_WORD.put(new Character((char)165).toString(), "Yènn"); //Yen
    	CURRENCY_TO_WORD.put(new Character((char)163).toString(), "Livre Sterling");
    	CURRENCY_TO_WORD.put("USD", "Dollar uèèss");
    	CURRENCY_TO_WORD.put("USD.", "Dollar uèss.");
    	CURRENCY_TO_WORD.put("USD,", "Dollar uèss,");
    	CURRENCY_TO_WORD.put("USD:", "Dollar uèss:");
    	CURRENCY_TO_WORD.put("USD!", "Dollar uèss!");
    	CURRENCY_TO_WORD.put("USD;", "Dollar uèss;");
    	CURRENCY_TO_WORD.put(new Character((char)36).toString(), "Dollar");
    	CURRENCY_TO_WORD.put("ATS", "schilling");  //schilling
    	CURRENCY_TO_WORD.put("BEF", "Francs belge");
    	CURRENCY_TO_WORD.put("GBP", "Livre Sterling");  //sterling
    	CURRENCY_TO_WORD.put("DKK", "Couronne Dannoise");
    	CURRENCY_TO_WORD.put("NLG", "Florin");
    	CURRENCY_TO_WORD.put(new Character((char)8364).toString(), "Euro");    	
    	CURRENCY_TO_WORD.put("EUR", "Euro");
    	CURRENCY_TO_WORD.put("Euro", "Euro");
    	CURRENCY_TO_WORD.put("FRF", "Franc");
    	CURRENCY_TO_WORD.put("DEM", "Mark");
    	CURRENCY_TO_WORD.put("GRD", "Drakme");  //drachme
    	CURRENCY_TO_WORD.put("IEP", "Livre irlandaise");
    	CURRENCY_TO_WORD.put("ITL", "Lire");
    	CURRENCY_TO_WORD.put("JPY", "Yènn");  //yen
    	CURRENCY_TO_WORD.put("LUF", "franc luxembourgeois");
    	CURRENCY_TO_WORD.put("PLN", "Zloty");
    	CURRENCY_TO_WORD.put("PTE", "Escoudo"); //Escudo
    	CURRENCY_TO_WORD.put("RUB", "Rouble");
    	CURRENCY_TO_WORD.put("ESP", "Péséta");  //peseta
    	CURRENCY_TO_WORD.put("SEK", "Couronne suèdeoise");
    	CURRENCY_TO_WORD.put("CHF", "Franc suisse");
    	CURRENCY_TO_WORD.put("HKD", "Dollar hong kongais");
    	CURRENCY_TO_WORD.put("CNY", "Youannn");  //yuan
    	CURRENCY_TO_WORD.put("KRW", "Wonn");  //won
    	CURRENCY_TO_WORD.put("INR", "Roupie");
    	CURRENCY_TO_WORD.put("BRL", "Real");
    	CURRENCY_TO_WORD.put("MXN", "Pesos");
    	CURRENCY_TO_WORD.put("ZAR", "Rand");
    	CURRENCY_TO_WORD.put("XPD", "Palladium");
    	CURRENCY_TO_WORD.put("XAU", "Or");
    	CURRENCY_TO_WORD.put("XFO", "Franc or");        
    	CURRENCY_TO_WORD.put("XAF", "Franc C F A");
    	CURRENCY_TO_WORD.put("AED", "Dirham");
    	CURRENCY_TO_WORD.put("DZD", "Dinar");
    	CURRENCY_TO_WORD.put("XAG", "Argent");
        
        
    };
    
    
    
    /**
     * Method that replaces the currencies into their equivalent.
     * 
     * @return
     */
	public static String currencyReplace ()
    {    	
		StringBuilder newTxt = new StringBuilder();        
        StringTokenizer st = new StringTokenizer(txtInput);
    	String cleanTxt = new String();
    	String str = new String();
        while (st.hasMoreTokens()) 
        {
        	str = st.nextToken();
        	cleanTxt = CURRENCY_TO_WORD.get(str);
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
