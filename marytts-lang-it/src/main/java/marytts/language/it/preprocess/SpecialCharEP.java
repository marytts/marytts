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

package marytts.language.it.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * An expansion pattern implementation for specialChar patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class SpecialCharEP extends ExpansionPattern
{
    private final String[] _knownTypes = {
        "specialChar"
    };
    /**
     * Every subclass has its own list knownTypes, 
     * an internal string representation of known types.
     * These are possible values of the <code>type</code> attribute to the
     * <code>say-as</code> element, as defined in MaryXML.dtd.
     * If there is more than one known type, the first type
     * (<code>knownTypes[0]</code>) is expected to be the most general one,
     * of which the others are specialisations.
     */
    private final List<String> knownTypes = Arrays.asList(_knownTypes);
    public List<String> knownTypes() { return knownTypes; }

    /** Helper class for the specialCharNames map */
    class SCEntry {
        /** The expanded form of this special character. */
        protected String expand;
        /** Determines whether a symbol, when found in a
         * token, will cause the token to be split into its parts.
         */
        protected boolean splitAt;
        /** Determines whether it will be pronounced when
         * found as a single token after all other expansion patterns
         * have been applied.
         */
        protected boolean pronounce;
        protected SCEntry(String expand, boolean splitAt, boolean pronounce)
        {
            this.expand = expand;
            this.splitAt = splitAt;
            this.pronounce = pronounce;
        }
    }
    /** Only needed to fill specialCharNames */
    private Map<String, SCEntry> createSpecialCharNames()
    {
        HashMap<String, SCEntry> m = new HashMap<String, SCEntry>();
        
        m.put("$",  new SCEntry("dollaro",                 false, true));
        m.put("@",  new SCEntry("chiocciola",                 true, true));
/*
        m.put("'",  new SCEntry("apostrofoXXX",                 false, true));
        m.put(":",  new SCEntry("duepuntiXXX",                 true, true));
*/
        /*
        m.put(",",  new SCEntry("virgola",                     false, false));
        m.put("\\", new SCEntry("Backslash['|b|E1|k|-|z|l|E1|S]",      true,  true));
        m.put("&",  new SCEntry("and['E1|n|d]",                       true,  true));
        m.put("/",  new SCEntry("Slash['|z|l|E1|S]",              true,  false));
        // m.put("@",  new SCEntry("at['|E1|t]",                   false, true));
        m.put("!",  new SCEntry("punto esclamativo",            false, false));
        m.put("#",  new SCEntry("cancelletto", true,  true));
        m.put("$",  new SCEntry("dollaro",                    false, true));
        m.put(new Character((char)167).toString(),  new SCEntry("paragrafo",                 true, true));
        m.put("%",  new SCEntry("percento",                   false, true));
        m.put(new Character((char)8364).toString(),  new SCEntry("euro", false, true));
        m.put("'",  new SCEntry("apostrofo",                 true,  false));
        m.put("*",  new SCEntry("asterisco",                     true,  true));
        m.put("+",  new SCEntry("più",                      true,  true));
        m.put("-",  new SCEntry("meno",               false, false));
        m.put("=",  new SCEntry("uguale",                    true,  true));
        m.put("?",  new SCEntry("punto di domanda",              true,  false));
        m.put("^",  new SCEntry("accento circonflesso",                      true,  false));
        m.put("_",  new SCEntry("sottolineato",               true,  false));
        m.put("`",  new SCEntry("apostrofo",                 true,  false));
        m.put("{",  new SCEntry("aperta graffa",   true,  false));
        m.put("|",  new SCEntry("pipe['|p|a1|-|i|p]",        true,  false));
        m.put("}",  new SCEntry("chiusa graffa",    true,  false));
        m.put("~",  new SCEntry("tilde",                     true,  true));
        m.put("(",  new SCEntry("aperta tonda",               true,  false));
        m.put(")",  new SCEntry("chiusa tonda",                true,  false));
        m.put("[",  new SCEntry("aperta quadra",        true,  false));
        m.put("]",  new SCEntry("chiusa quadra",         true,  false));
        m.put(":",  new SCEntry("due punti",               false, false));
        m.put(";",  new SCEntry("punto e virgola",                 true,  false));
        m.put("\"", new SCEntry("virgolette",         true,  false));
        m.put("<",  new SCEntry("minore",               true,  true));
        m.put(">",  new SCEntry("maggiore",                true,  true));
        m.put(".",  new SCEntry("punto",                     false, false));
        */
        return m;
    };
    private final Map<String, SCEntry> specialCharNames = createSpecialCharNames();
    protected final String sMatchingChars = createMatchingChars();
    //    protected final String sMatchingChars = "[\\,\\\\\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~\\(\\)\\[\\]\\@\\:\\;\\\"\\<\\>\\.]";
    protected final String sMatchingCharsSimpleString = createMatchingCharsSimpleString();
    //protected final String sMatchingCharsSimpleString = ",\\!#$%&'*+-/=?^_`{|}~()[]@:;\"<>.";
    private final String sSplitAtChars = createSplitAtChars();
    private final String sSplitAtCharsSimpleString = createSplitAtCharsSimpleString();
    /** Only needed to fill sMatchingChars from specialCharNames */
    private String createMatchingChars()
    {
        StringBuilder sb = new StringBuilder("[");
        for (Iterator<String> it = specialCharNames.keySet().iterator(); it.hasNext();) {
            sb.append("\\" + (String) it.next());
        }
        sb.append("]");
        return sb.toString();
    }

    /** Only needed to fill sMatchingCharsSimpleString from _specialCharNames[] */
    private String createMatchingCharsSimpleString()
    {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = specialCharNames.keySet().iterator(); it.hasNext();) {
            sb.append((String)it.next());
        }
        return sb.toString();
    }

    /** Only needed to fill sSplitAtChars from _specialCharNames[] */
    private String createSplitAtChars()
    {
        StringBuilder sb = new StringBuilder("[");
        for (Iterator<String> it = specialCharNames.keySet().iterator(); it.hasNext();) {
            String sc = (String) it.next();
            if (((SCEntry)specialCharNames.get(sc)).splitAt) {
                sb.append("\\" + sc);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /** Only needed to fill sSplitAtCharsSimpleString from _specialCharNames[] */
    private String createSplitAtCharsSimpleString()
    {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = specialCharNames.keySet().iterator(); it.hasNext();) {
            String sc = (String) it.next();
            if (((SCEntry)specialCharNames.get(sc)).splitAt) {
                sb.append(sc);
            }
        }
        return sb.toString();
    }

    private final Pattern reMatchingChars = Pattern.compile(sMatchingChars);
    public Pattern reMatchingChars() { return reMatchingChars; }

    private final Pattern reSplitAtChars = Pattern.compile(sSplitAtChars);
    /**
     * A regular expression matching the characters at which
     * a token should be split into parts
     * before any preprocessing patterns are applied.
     */
    protected Pattern getRESplitAtChars() { return reSplitAtChars; }
    /**
     * A string containing the  characters at which
     * a token should be split into parts
     * before any preprocessing patterns are applied.
     */
    protected String splitAtChars() { return sSplitAtCharsSimpleString; }

    /**
     * Every subclass has its own logger.
     * The important point is that if several threads are accessing
     * the variable at the same time, the logger needs to be thread-safe
     * or it will produce rubbish.
     */
    private Logger logger = MaryUtils.getLogger("SpecialCharEP");

    public SpecialCharEP()
    {
        super();
    }

    
    protected int canDealWith(String s, int type){
        return match(s, type);
    }
    
    
    protected int match(String s, int type)
    {
        switch (type) {
        case 0:
            if (matchSpecialChar(s)) return 0;
            break;
        }
        return -1;
    }

    protected List<Element> expand(List<Element> tokens, String s, int type)
    {
        if (tokens == null) 
            throw new NullPointerException("Received null argument");
        if (tokens.isEmpty()) 
            throw new IllegalArgumentException("Received empty list");
        Document doc = ((Element)tokens.get(0)).getOwnerDocument();
        // we expect type to be one of the return values of match():
        List<Element> expanded = null;
        switch (type) {
        case 0:
            expanded = expandSpecialChar(doc, s);
            break;
        }
        if (expanded != null && !expanded.isEmpty())
	        replaceTokens(tokens, expanded);
        return expanded;
    }

    /**
     * Tell whether String <code>s</code> is a specialChar.
     */
    public boolean matchSpecialChar(String s)
    {
        return reMatchingChars.matcher(s).matches();
    }

    protected boolean doPronounce(String specialChar)
    {
        SCEntry entry = (SCEntry)specialCharNames.get(specialChar);
        if (entry == null) return false;
        return entry.pronounce;
    }        

    protected List<Element> expandSpecialChar(Document doc, String s)
    {
        ArrayList<Element> exp = new ArrayList<Element>();
        if (doPronounce(s)) {
            String specialCharName = expandSpecialChar(s);
            exp.addAll(makeNewTokens(doc, specialCharName, true, s));
        } // if not to be pronounced, return an empty list
        return exp;
    }

    protected String expandSpecialChar(String s)
    {
        SCEntry entry = (SCEntry)specialCharNames.get(s);
        if (entry == null) return null;
        return entry.expand;
    }
}
