/**
 * Copyright 2000-2006 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary;

// General Java Classes
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

/**
 * A representation of the data types available as input/output of (partial)
 * processing. List the data types available as input/output of (partial)
 * processing. Provide static interfaces for obtaining a given data type.
 * @author Marc Schr&ouml;der
 */

public class MaryDataType
{
    private String name;
    private Locale locale;
    private boolean isInputType;
    private boolean isOutputType;
    private Traits traits;
    private String rootElement; // for XML types
    private String endMarker;
    private String exampleText;

    /** Just to make compiler happy: */
    protected MaryDataType() {
    }
    
    private MaryDataType(String name, Locale locale,  boolean isInputType, boolean isOutputType, Traits traits, String rootElement, String endMarker, String exampleText) {
        this.name         = name;
        this.locale       = locale;
        this.isInputType  = isInputType;
        this.isOutputType = isOutputType;
        this.traits = traits;
        this.rootElement  = rootElement;
        this.endMarker    = endMarker;
        this.exampleText  = exampleText;
    }
    public String name() { return name; }
    public Locale getLocale() { return locale; }
    public boolean isInputType() { return isInputType; }
    public boolean isOutputType() { return isOutputType; }
    public boolean isTextType() { return traits.isTextType(); }
    public boolean isXMLType() { return traits.isXMLType(); }
    public boolean isMaryXML() { return traits.isMaryXML(); }
    public boolean isUtterances() { return traits.isUtterances(); }
    public String rootElement() { return rootElement; }
    public String endMarker() {
        if (isXMLType())
            return "</" + rootElement() + ">";
        else
            return endMarker;
    }
    
    /**
     * Provide the example text for this data type.
     * This is simply a string provided when the data type
     * is defined. For some language-independent datatypes, no
     * example text is defined. In these cases, null is returned
     * @return an example text string, or null if none could be obtained.
     */
    public String exampleText()
    {
        return exampleText;
    }

    public String toString() { return name; }

    //////////////// static stuff ///////////////

    protected static final Traits PLAIN_TEXT = new Traits(true, false, false, false);
    protected static final Traits EXTERNAL_MARKUP = new Traits(true, true, false, false);
    protected static final Traits MARYXML = new Traits(true, true, true, false);
    protected static final Traits UTTERANCES = new Traits(false, false, false, true);
    protected static final Traits BINARY = new Traits(false, false, false, false);


    private static final Vector<MaryDataType> allTypes = new Vector<MaryDataType>();
    private static boolean allTypesIsSorted = false;
    private static final Map<String,MaryDataType> nameMap = new HashMap<String,MaryDataType>();
    
    /**
     * The preferred way to create a data type. In order to define a MaryDataType "XYZ",
     * create a de.dfki.lt.mary.datatyes.XYZ_Definer extends MaryDataType, which in a
     * static {} block calls this method define(). When a MaryDataType is requested 
     * using get("XYZ"), the definer class is initialised, which causes this define()
     * method to be called with the appropriate parameters, so that the data type is
     * provided. This approach makes the definition of data types fully flexible.
     * @param name name of the data type, to be used when calling get(name) to get this data type
     * @param locale the locale of this data type, or none if the data type is locale-independent
     * @param isInputType whether this data type can be used as an input type
     * @param isOutputType whether this data type can be produced as an output type
     * @param traits a set of traits describing this data type
     * @param rootElement if this is an XML data type, the name of the root element; null for non-xml data types
     * @param endMarker optional marker showing that the end of data of this type is read; null unless you know
     * what you are doing. This makes sense when communicating with external modules
     * which provide output of this type.
     * @param exampleText a working example of data of this type, which can be
     * provided to a client requesting data of this type. Should be  non-null for
     * all input types.
     */
    protected static void define(String name, Locale locale, boolean isInputType, boolean isOutputType, Traits traits, String rootElement, String endMarker, String exampleText) {
        if (nameMap.containsKey(name)) throw new IllegalArgumentException("MaryDataType `" + name + "' already defined.");
        MaryDataType t = new MaryDataType(name, locale, isInputType, isOutputType, traits, rootElement, endMarker, exampleText);
        allTypes.add(t);
        nameMap.put(name, t);
    }
    

    public static Vector<String> getInputTypeStrings() {
        if (!allTypesIsSorted) sortAllTypes();
        Vector<String> result = new Vector<String>(10);
        for (MaryDataType t : allTypes) {
            if (t.isInputType())
                result.add(t.name());
        }
        return result;
    }

    public static Vector<String> getOutputTypeStrings() {
        if (!allTypesIsSorted) sortAllTypes();
        Vector<String> result = new Vector<String>(10);
        for (MaryDataType t : allTypes) {
            if (t.isOutputType())
                result.add(t.name());
        }
        return result;
    }
      public static Vector<MaryDataType> getInputTypes() {
          if (!allTypesIsSorted) sortAllTypes();
        Vector<MaryDataType> result = new Vector<MaryDataType>(10);
        for (MaryDataType t : allTypes) {
            if (t.isInputType())
                result.add(t);
        }
        return result;
    }

    public static Vector<MaryDataType> getOutputTypes() {
        if (!allTypesIsSorted) sortAllTypes();
        Vector<MaryDataType> result = new Vector<MaryDataType>(10);
        for (MaryDataType t : allTypes) {
            if (t.isOutputType())
                result.add(t);
        }
        return result;
    }

    /**
     * Test whether the type with the given name exists.
     * This method will return true only if the type name is known and has
     * already been activated by a call to get(). In other words, if exists()
     * returns true, get() will return a non-null value, and if get() returns
     * a non-null value, exists() will return true; also, if get() returns null,
     * exists() will return false; but when exists() returns false, it is possible
     * either that get has been called (and returned a null value), or that get 
     * has not yet been called (and would return a non-null value).
     * 
     *  Therefore, exists() should be called to non-intrusively test the system
     *  configuration, whereas get() should be called to see if some data type
     *  can be made available.
     * @param typeName the type name in question
     * @return true if the type exists, false if it is unknown.
     */
    public static boolean exists(String typeName)
    {
        return nameMap.containsKey(typeName);
    }

    /**
     * Try to get an instance of this data type. This code will thry to initialize
     * the XYZ_Definer when the XYZ data type is first requested.
     * @param name the name of the data type
     * @return the requested data type
     * @throws Error if the data type cannot be found
     */
    public static MaryDataType get(String name) {
        MaryDataType t = nameMap.get(name);
        if (t == null) { // try to initialize the _Definer class
        // This should initialize the class, i.e. execute the static { } blocks.
	    try {
                Class.forName("de.dfki.lt.mary.datatypes." + name + "_Definer");
            } catch (ClassNotFoundException e) {
                throw new Error("Unknown MaryDataType `" + name + "'", e);
            }
            // and try again:
            t = nameMap.get(name);
        }
        return t;
    }

    public static Vector<MaryDataType> getDataTypes() {
        if (!allTypesIsSorted) sortAllTypes();
        return (Vector<MaryDataType>) allTypes.clone();
    }

    private static void sortAllTypes() {
        Collections.sort(allTypes, new Comparator<MaryDataType>() {
            public int compare(MaryDataType one, MaryDataType two) {
                // First, sort by input type / output type status:
                if (one.isInputType() && !two.isInputType()) {
                    return -1; // one is first
                } else if (two.isInputType() && !one.isInputType()) {
                    return 1; // two is first
                    // Now, either both or none of them are input types
                } else if (one.isOutputType() && !two.isOutputType()) {
                    return 1; // two is first
                } else if (two.isOutputType() && !one.isOutputType()) {
                    return -1; // one is first
                } else if (one.isInputType() && two.isInputType() &&
                            !one.isOutputType() && !two.isOutputType()) {
                    // Both are only input types -> if one is plain text, the other XML,
                    // text goes first
                    if (!one.isXMLType() && two.isXMLType()) {
                        return -1; // one is first
                    } else if (!two.isXMLType() && one.isXMLType()) {
                        return 1; // two is first
                    }
                } else if (!one.isInputType() && !two.isInputType() &&
                            !one.isOutputType() && !two.isOutputType()) {
                    return 0; // both are equal
                }
                if (one.isMaryXML() && two.isXMLType() && !two.isMaryXML()) {
                    return 1; // xml input format two is first
                } else if (two.isMaryXML() && one.isXMLType() && !one.isMaryXML()) {
                    return -1; // xml input format one is first
                }
                if (one.name().equals("TEXT") && !two.name().equals("TEXT")) {
                    return -1; // one is first
                } else if (two.name().equals("TEXT") && !one.name().equals("TEXT")) {
                    return 1; // two is first
                }                
                if (one.name().startsWith("TEXT") && !two.name().startsWith("TEXT")) {
                    return -1; // one is first
                } else if (two.name().startsWith("TEXT") && !one.name().startsWith("TEXT")) {
                    return 1; // two is first
                }
                if (one.getLocale() != null && two.getLocale() != null &&
                    !two.getLocale().equals(one.getLocale())) {
                    // different locales -- sort by locale
                    return one.getLocale().toString().compareTo(two.getLocale().toString());
                }
                // Use German Locale for creating language-specific versions from general ones
                Locale locale = one.getLocale();
                if (locale == null) locale = two.getLocale();
                if (locale == null) locale = Locale.GERMAN;
                if (Mary.modulesRequiredForProcessing(one, two, locale) == null) {
                    //if (Mary.modulesRequiredForProcessing(two, one, locale) == null)
                    //    System.err.println("cannot compare the datatypes " + one.name() + " and " + two.name());
                    return 1; // two is first
                } else {
                    return -1; // one is first
                }
            }
        });
        // A test said this was sorted in 12 ms, so no harm done.
        allTypesIsSorted = true;
    }

    /**
     * Try to find a language-specific version of a
     * language-independent data type. Correspondence is found via a
     * language-specific suffix in the data type name, e.g. RAWMARYXML_DE is a
     * language-specific version of RAWMARYXML, for the Locale.GERMAN locale.
     * @return the language-specific data type corresponding to this data type,
     * or null if no such data type is found. Also returns null if the input
     * type is not language-independent.
     */
    public static MaryDataType getLanguageSpecificVersion
        (MaryDataType languageIndependentType, Locale locale)
    {
        if (languageIndependentType.getLocale() != null) {
            // The type is already language-specific!
            return null;
        }
        if (locale == null) {
            return null;
        }
        String name = languageIndependentType.name() + "_" +
            locale.getLanguage().toUpperCase();
        MaryDataType languageSpecificType = null;
        try {
            languageSpecificType = get(name);
        } catch (Error err) { // Error thrown when type does not exist
        }
        return languageSpecificType;
    }
    
    public static class Traits
    {
        private boolean isTextType; // alternative: binary data
        private boolean isXMLType;
        private boolean isMaryXML;
        private boolean isUtterances;
        
        public Traits(boolean isTextType, boolean isXMLType, boolean isMaryXML, boolean isUtterances) {
            // Some plausibility checks:
            if (isMaryXML) assert isXMLType;
            if (isXMLType) assert isTextType;
            if (isTextType) assert !isUtterances;
            
            this.isTextType = isTextType;
            this.isXMLType = isXMLType;
            this.isMaryXML = isMaryXML;
            this.isUtterances = isUtterances;
        }
        public boolean isTextType() { return isTextType; }
        public boolean isXMLType() { return isXMLType; }
        public boolean isMaryXML() { return isMaryXML; }
        public boolean isUtterances() { return isUtterances; }
    }
}
