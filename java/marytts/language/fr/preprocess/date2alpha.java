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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Date cleaning class. Still to do.
 *
 * @author Marc Schroeder and Florent Xavier
 */

public class date2alpha
{
    private final String[] _knownTypes = {
        "date",
        "date:dmy",
        "date:ymd",
        "date:dm",
        "date:my",
        "date:y",
        "date:m",
        "date:d",
        "date:mdy",
        "date:md"
    };
    

   private final String[] _monthabbr = {
        "Jan", "Janvier",
        "Fév", "Février",
        "Mar", "Mars",
        "Avr", "Avril",
        // no abbreviation for Mai
        "Jun", "Juin",
        "Jui", "Juillet",
        "Aoû", "Août",
        "Sep", "Septembre",
        "Oct", "Octobre",
        "Nov", "Novembre",
        "Déc", "Décembre",
        "1", "Janvier",
        "2", "Février",
        "3", "Mars",
        "4", "Avril",
        "5", "Mai",
        "6", "Juin",
        "7", "Juillet",
        "8", "Août",
        "9", "Septembre",
        "10", "Octobr",
        "11", "Novembre",
        "12", "Décembre",
        "01", "Janvier",
        "02", "Février",
        "03", "Mars",
        "04", "Avril",
        "05", "Mai",
        "06", "Juin",
        "07", "Juillet",
        "08", "Août",
        "09", "Sepetembre"
    };
   

    // Domain-specific primitives:
    protected final String sDay = "(?:0?[1-9]|[12][0-9]|3[01])";
    protected final String sMonth = "(?:0[1-9]|1[0-2])";
    protected final String sMonthword = "(?:Janvier|Février|Mars|Avril|Mai|Juin|Juillet|Août|Septembre|Octobre|Novembre|Décembre|Printemps|été|Automne|Hiver)";
    protected final String sMonthabbr = "(?:Jan|Fév|Mar|Avr|Jun|Jui|Aoû|Sep|Oct|Nov|Déc)";
    protected final String sYear = "(?:([0-9][0-9])?[0-9][0-9])";
    protected final String sDot = "[\\.]";
    protected final String sSlash = "[/]";
    protected final String sMinus = "[\\-]";
    protected final String sMatchingChars = /*[0-9/\\.JFMASONDanurebäzpilgstmkov]*/ " "; 
    protected final String sSeparators = (sDot + "|" + sSlash + "|" + sMinus);
    
    // Now the actual match patterns:
    protected final Pattern reDayMonthYear =
        Pattern.compile("(" + sDay + ")" + "(?:" + sDot + "|" + sSlash + ")" +
                         "(" + sMonth + ")" + "(?:" + sDot + "|" + sSlash + ")"+
                         "(" + sYear + ")");
    protected final Pattern reDayMonthwordYear =
        Pattern.compile("(" + sDay + ")" + sDot +
                         "(" + sMonthword + ")" +
                         "(" + sYear + ")");
    protected final Pattern reDayMonthabbrYear =
        Pattern.compile("(" + sDay + ")" + sDot +
                         "(" + sMonthabbr + ")" + sDot + "?" +
                         "(" + sYear + ")");
    
    protected final Pattern reYearMonthDay =
        Pattern.compile("(" + sYear + ")" + "(?:" + sDot + "|" + sSlash + ")" +
                         "(" + sMonth + ")" + "(?:" + sDot + "|" + sSlash + ")"+
                         "(" + sDay + ")");
    protected final Pattern reYearMonthwordDay =
        Pattern.compile("(" + sYear + ")" + sDot +
                         "(" + sMonthword + ")" +
                         "(" + sDay + ")");
    protected final Pattern reYearMonthabbrDay =
        Pattern.compile("(" + sYear + ")" + sDot +
                         "(" + sMonthabbr + ")" + sDot + "?" +
                         "(" + sDay + ")");
    
    protected final Pattern reMonthDayYear =
        Pattern.compile("(" + sMonth + ")" + "(?:" + sSeparators + ")" +
                         "(" + sDay + ")" + "(?:" + sSeparators + ")" +
                         "(" + sYear + ")");
    protected final Pattern reMonthwordDayYear =
        Pattern.compile("(" + sMonthword + ")" +
                         "(" + sDay + ")(?:" + sSeparators + ")" +
                         "(" + sYear + ")");
    protected final Pattern reMonthabbrDayYear =
        Pattern.compile("(?:(Jan|Fév|Mar|Avr|Jun|Jui|Aoû|Sep|Oct|Nov|Déc)|(Jan|Fév|Mar|Avr|Jun|Jui|Aoû|Sep|Oct|Nov|Déc)\\.)" + 
                         "(?:" + sSeparators + ")" + "(" + sDay + ")" +  
                         "(?:" + sSeparators + ")" + "(" + sYear + ")");
    
    protected final Pattern reDayMonth =
        Pattern.compile("(" + sDay + ")" + "(?:" + sDot + "|" + sSlash + ")" +
                         "(" + sMonth + ")" + "(?:" + sDot + "|" + sSlash + ")");
    protected final Pattern reDayMonthword =
        Pattern.compile("(" + sDay + ")" + sDot +
                         "(" + sMonthword + ")");
    protected final Pattern reDayMonthabbr =
        Pattern.compile("(" + sDay + ")" + sDot +
                         "(" + sMonthabbr + ")" + sDot + "?");
    
    protected final Pattern reMonthDay =
        Pattern.compile("(" + sMonth + ")" + "(?:" + sSeparators + ")" +
                         "(" + sDay + ")" + "(?:" + sSeparators + ")");
    protected final Pattern reMonthwordDay =
        Pattern.compile("(" + sMonthword + ")" +
                         "(" + sDay + ")(?:" + sDot + ")");
    protected final Pattern reMonthabbrDay =
        Pattern.compile("(?:(Jan|Fév|Mar|Avr|Jun|Jui|Aoû|Sep|Oct|Nov|Déc)|(Jan|Fév|Mar|Avr|Jun|Jui|Aoû|Sep|Oct|Nov|Déc)\\.)" +
                 "(?:" + sSeparators + ")" + "(" + sDay + ")" +  
                 "(?:" + sSeparators + ")");
    
    protected final Pattern reMonthYear =
        Pattern.compile("(" + sMonth + ")(" + sSlash + "|" + sDot +
                         ")(" + sYear + ")");
    protected final Pattern reMonthwordYear =
        Pattern.compile("(" + sMonthword + ")" +
                         "(" + sYear + ")");
    protected final Pattern reMonthabbrYear =
        Pattern.compile("(" + sMonthabbr + ")" + sDot + "?" +
                         "(" + sYear + ")");
    
    protected final Pattern reYear =
        Pattern.compile("(?:([0-9][0-9])?[0-9][0-9])");
    
    protected final Pattern reMonth =
        Pattern.compile("(" + sMonth + ")" + "(?:" + sDot + ")");
    protected final Pattern reMonthword =
        Pattern.compile("(" + sMonthword + ")");
    protected final Pattern reMonthabbr =
        Pattern.compile("((?:(Jan|Fév|Mar|Avr|Jun|Jui|Aoû|Sep|Oct|Nov|Déc)|(Jan|Fév|Mar|Avr|Jun|Jui|Aoû|Sep|Oct|Nov|Déc)\\.))");   

    protected final Pattern reDay =
        Pattern.compile("(" + sDay + ")" + "?:" + sDot);

    private final Pattern reMatchingChars = Pattern.compile(sMatchingChars);
    public Pattern reMatchingChars() { return reMatchingChars; }  

    

   
   

}
