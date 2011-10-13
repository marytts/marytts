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
 * cleaning the measures.
 *
 * @author Florent Xavier
 */

public class mesu2alpha
{
	
	public static String txtInput = new String();
	
	private final static Map<String, String> MEASURE_TO_WORD = new HashMap<String, String>();
	static{
		MEASURE_TO_WORD.put("km", "Kilomètre");
		MEASURE_TO_WORD.put("dm", "Décimètre");
		MEASURE_TO_WORD.put("cm", "Centimètre");
		MEASURE_TO_WORD.put("mm", "Millimètre");
		MEASURE_TO_WORD.put("g", "Gramme");
		MEASURE_TO_WORD.put("kg", "Kilogramme");
		MEASURE_TO_WORD.put("mg", "Milligramme");
		MEASURE_TO_WORD.put("A", "Ampère");
		MEASURE_TO_WORD.put("V", "Volt");
		MEASURE_TO_WORD.put("K", "Kelvin");
		MEASURE_TO_WORD.put(new Character((char)176).toString() + "C", "Degré Celsius");
		MEASURE_TO_WORD.put(new Character((char)730).toString() + "C", "Degrés Celsius");
		MEASURE_TO_WORD.put("\u2103", "Degrés Celsius"); // ℃
		MEASURE_TO_WORD.put(new Character((char)176).toString() + "F", "Degré Farenaaiite");
		MEASURE_TO_WORD.put(new Character((char)730).toString() + "F", "Degrés Farenaaiite");
		MEASURE_TO_WORD.put("\u2109", "Degrés Farenaaiite"); // ℉       
		MEASURE_TO_WORD.put("Hz", "Hertz");
		MEASURE_TO_WORD.put("kHz", "Kilohertz");
		MEASURE_TO_WORD.put("MHz", "Megahertz");
		MEASURE_TO_WORD.put("GHz", "GigaHertz");
		MEASURE_TO_WORD.put("N", "Newton");
		MEASURE_TO_WORD.put("Pa", "Pascal");
		MEASURE_TO_WORD.put("J", "Joule");
		MEASURE_TO_WORD.put("kJ", "Kilojoule");
		MEASURE_TO_WORD.put("W", "Watt");
		MEASURE_TO_WORD.put("kW", "Kilowatt");
		MEASURE_TO_WORD.put("MW", "Megawatt");
		MEASURE_TO_WORD.put("GW", "Gigawatt");
		MEASURE_TO_WORD.put("mW", "Milliwatt");
		MEASURE_TO_WORD.put("l", "Litre");
		MEASURE_TO_WORD.put("dl", "Décilitre");
		MEASURE_TO_WORD.put("cl", "Centilitre");
		MEASURE_TO_WORD.put("ml", "Millilitre");
		MEASURE_TO_WORD.put("Bq", "Becquerel");
		MEASURE_TO_WORD.put("m²", "Mètre carré");
		MEASURE_TO_WORD.put("m" + new Character((char)178).toString(), "Mètre carré");
		MEASURE_TO_WORD.put("m" + new Character((char)179).toString(), "Mètre cube");
		MEASURE_TO_WORD.put("ccm", "Centimètre cube");
		MEASURE_TO_WORD.put("m", "Mètre");
		MEASURE_TO_WORD.put("dB", "Décibel");
		MEASURE_TO_WORD.put("s", "Seconde");
		MEASURE_TO_WORD.put("sec", "Seconde");
		MEASURE_TO_WORD.put("ms", "Milliseconde");
		MEASURE_TO_WORD.put("msec", "Milliseconde");
		MEASURE_TO_WORD.put("min", "Minute");
		MEASURE_TO_WORD.put("kcal", "Kilocalorie");
		MEASURE_TO_WORD.put("oz.", "Once");
		MEASURE_TO_WORD.put("oz", "Once");
		};
		
		
		
	    /**
	     * Method that replaces the measures into their equivalent.
	     * 
	     * @return
	     */
		public static String measureReplace ()
	    {    	
			StringBuilder newTxt = new StringBuilder();        
	        StringTokenizer st = new StringTokenizer(txtInput);
	    	String cleanTxt = new String();
	    	String str = new String();
	        while (st.hasMoreTokens()) 
	        {
	        	str = st.nextToken();
	        	cleanTxt = MEASURE_TO_WORD.get(str);
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
