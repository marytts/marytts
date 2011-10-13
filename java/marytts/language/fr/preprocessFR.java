package marytts.language.fr;
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


import marytts.language.fr.preprocess.abrev2alpha;
import marytts.language.fr.preprocess.devis2alpha;
import marytts.language.fr.preprocess.mesu2alpha;
import marytts.language.fr.preprocess.sms2alpha;

/**
 * Cleaning the text. Some preprocessing task are already executed 
 * in lia_phon (digit to words, roman numerals, special character)
 * 
 * @author Florent Xavier
 */




public class preprocessFR 
{
    
    public String textUnclean = new String();
    
    
    
    /**
     * Method that preprocess the input text. We make the different 
     * preprocessing from the heavier to the lighter.
     * @return
     */
    public String CleanText ()
    {
        String textClean = new String();
        
        //SMS cleaning.
        sms2alpha sm = new sms2alpha();
        sm.txtInput = textUnclean;
        textUnclean = (String) sm.smsReplace();
        
        //Abbreviations cleaning
        abrev2alpha ab = new abrev2alpha();
        ab.txtInput = textUnclean;
        textUnclean =(String) ab.abbrevReplace();
        
        //Currency cleaning
        devis2alpha dv = new devis2alpha();
        dv.txtInput = textUnclean;
        textUnclean =(String) dv.currencyReplace();
        
        //Measures cleaning
        mesu2alpha ms = new mesu2alpha();
        ms.txtInput = textUnclean;
        textUnclean = (String) ms.measureReplace();
        
        textClean = textUnclean;
        
        return textClean;       
    }
    

}
