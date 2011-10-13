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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Time cleaning. Still to do.
 *
 * @author Marc Schroeder and Florent Xavier
 */

public class dur2alpha
{
    private final String[] _knownTypes = {
        "duration",
        "duration:hms",
        "duration:hm",
        "duration:h"
    };
    
    // Domain-specific primitives:
    protected final String sHour = "(?:0?[0-9]|1[0-9]|2[0-4])";
    protected final String sMinute = "(?:[0-5][0-9])";
    protected final String sSecond = sMinute;
    protected final String sSep = "(?:\\:|\\.)";
    protected final String sFinal = "(?:h|Std\\.)";
    protected final String sMatchingChars = "[0-9:\\.hStd]";

    // Now the actual match patterns:
    protected final Pattern reHour = Pattern.compile("("+ sHour +")" + sFinal);
    protected final Pattern reHourMinute =
        Pattern.compile("(" + sHour + ")" + sSep + "(" + sMinute + ")" + sFinal);
    protected final Pattern reHourMinuteSecond =
        Pattern.compile("(" + sHour + ")" + sSep + "(" + sMinute + ")" + sSep +
                         "(" + sSecond + ")" + sFinal);

    private final Pattern reMatchingChars = Pattern.compile(sMatchingChars);
    public Pattern reMatchingChars() { return reMatchingChars; }

    

}
