package marytts.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Supported sequence label manager
 *
 */
public class SupportedSequenceType {
    public static final String PARAGRAPH = "PARAGRAPH";
    public static final String SENTENCE = "SENTENCE";
    public static final String PHRASE = "PHRASE";
    public static final String WORD = "WORD";
    public static final String SYLLABLE = "SYLLABLE";
    public static final String PHONE = "PHONE";
    public static final String NSS = "NSS";
    public static final String SEGMENT = "SEGMENT";
    public static final String FEATURES = "FEATURES";
    public static final String LABEL = "LABEL";
    public static final String AUDIO = "AUDIO";

    // Initialize default available sequences
    public static final String[] SET_VALUES = new String[] {
        PARAGRAPH, SENTENCE, PHRASE, WORD, SYLLABLE, PHONE, NSS, SEGMENT, FEATURES, LABEL, AUDIO
    };
    public static final Set<String> TYPE_SET = new HashSet<>(Arrays.asList(SET_VALUES));


    /**
     *  Add a new type to the list of available ones
     *
     *  @param id the label identifying the type
     */
    public static synchronized void addSupportedType(String type_id) {
        TYPE_SET.add(type_id);
    }

    /**
     *  List all the available types. The set returned is a copy !
     *
     *  @return a set containing the available types
     */
    public static synchronized Set<String> listAvailableTypes() {
        return new HashSet<String>(TYPE_SET);
    }

    /**
     *  Check if a type is available
     *
     *  @param id the type label to check
     *  @return true if it is available, false else
     */
    public static synchronized boolean isSequenceTypeAvailable(String id) {
        return TYPE_SET.contains(id);
    }


};
