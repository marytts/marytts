package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.cart.StringPredictionTree;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.TargetFeatureComputer;
import marytts.modules.InternalModule;
import marytts.modules.phonemiser.Phoneme;
import marytts.modules.phonemiser.PhonemeSet;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.select.Target;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.TreeWalker;



/**
 * 
 * This module serves as a post-lexical pronunciation model.
 * Its appropriate place in the module chain is after intonisation.
 * The target features are taken
 * and fed into decision trees that predict the new pronunciation. 
 * A new mary xml is output, with the difference being that the old 
 * pronunciation is replaced by the newly predicted one, and a finer grained
 * xml structure.
 * 
 * @author ben
 *
 */
public class PronunciationModel extends InternalModule{

	// for prediction, core of the model - maps phonemes to decision trees
	private Map<String, StringPredictionTree> treeMap;
	  
    // used in startup() and later for convenience
    private FeatureDefinition featDef;
	    
    private TargetFeatureComputer featureComputer;
	
	/**
	 * Constructor, stating that the input is of type INTONATION, the output
	 * of type ALLOPHONES.
	 * 
	 */
	public PronunciationModel() {
        super("PronunciationModel",
                MaryDataType.INTONATION,
                MaryDataType.ALLOPHONES,
                null);
	}

    public void startup() throws Exception
    {
        super.startup();

        // TODO: pronunciation model tree and feature definition should be voice-specific
        // get featureDefinition used for trees - just to tell the tree that the
        // features are discrete
        String fdFilename = MaryProperties.getFilename("german.pronunciation.featuredefinition");
        if (fdFilename != null) {
            File fdFile = new File(fdFilename);
            //                                              reader for file, readweights = false
            featDef = new FeatureDefinition(new BufferedReader(new FileReader(fdFile)), false);

            // get path where the prediction trees lie
            File treePath = new File(MaryProperties.needFilename("german.pronunciation.treepath"));

            // valid predicion tree files are named prediction_<phone_symbol>.tree
            Pattern treeFilePattern = Pattern.compile("^prediction_(.*)\\.tree$");

            // initialize the map that contains the trees
            this.treeMap = new HashMap<String, StringPredictionTree>();

            // iterate through potential prediction tree files
            File[] fileArray = treePath.listFiles();
            for ( int fileIndex = 0 ; fileIndex < fileArray.length ; fileIndex++ ) {
                File f = fileArray[ fileIndex ];

                // is file name valid?
                Matcher filePatternMatcher = treeFilePattern.matcher(f.getName());

                if ( filePatternMatcher.matches() ){
                    // phoneme of file name is a group in the regex
                    String phonemeId = filePatternMatcher.group(1);

                    // construct tree from file and map phoneme to it
                    StringPredictionTree predictionTree = 
                        new StringPredictionTree( new BufferedReader( new FileReader( f ) ), featDef );

                    // back mapping from short id
                    int index = this.featDef.getFeatureIndex("phoneme");
                    this.treeMap.put( this.featDef.getFeatureValueAsString(index, Short.parseShort(phonemeId)), predictionTree);
                    //logger.debug("Read in tree for " + PhoneNameConverter.normForm2phone(phoneme));
                }
            }
            logger.debug("Reading in feature definition and decision trees finished.");

            // TODO: change property name to german.pronunciation.featuremanager/features
            String managerClass = MaryProperties.needProperty("german.pronunciation.targetfeaturelister.featuremanager");
            FeatureProcessorManager manager = (FeatureProcessorManager) Class.forName(managerClass).newInstance();
            String features = MaryProperties.needProperty("german.pronunciation.targetfeaturelister.features");
            this.featureComputer = new TargetFeatureComputer(manager, features);

        }


        logger.debug("Building feature computer finished.");

    }
    
    /**
     * This computes a new pronunciation for the elements of some MaryData, that
     * is phonemised.
     */
    public MaryData process(MaryData d)
    throws Exception
    {
        // get the xml document
    	Document doc = d.getDocument();
        logger.debug("Getting xml-data from document finished.");

        TreeWalker tw = MaryDomUtils.createTreeWalker(doc, doc, MaryXML.TOKEN);
        Element t;
        PhonemeSet phonemeSet = null;
        while ((t = (Element) tw.nextNode()) != null) {
            // First, create the substructure of <t> elements: <syllable> and <ph>.
            if (phonemeSet == null) { // need to determine it once, then assume it is the same for all
                Element voice = (Element) MaryDomUtils.getAncestor(t, MaryXML.VOICE);
                Voice maryVoice = Voice.getVoice(voice);
                if (maryVoice == null) {                
                    maryVoice = d.getDefaultVoice();
                }
                if (maryVoice == null) {
                    // Determine Locale in order to use default voice
                    Locale locale = MaryUtils.string2locale(doc.getDocumentElement().getAttribute("xml:lang"));
                    maryVoice = Voice.getDefaultVoice(locale);
                }
                assert maryVoice != null;
                phonemeSet = maryVoice.getSampaPhonemeSet();
            }
            createSubStructure(t, phonemeSet);
            
            if (treeMap == null) continue;
            
            assert featureComputer != null;
            
            // Now, predict modified pronunciations, adapt <ph> elements accordingly,
            // and update ph for syllable and t elements where necessary
            StringBuilder tPh = new StringBuilder();
            TreeWalker sylWalker = MaryDomUtils.createTreeWalker(doc, t, MaryXML.SYLLABLE);
            Element syllable;
            while ((syllable = (Element) sylWalker.nextNode()) != null) {
                StringBuilder sylPh = new StringBuilder();
                String stressed = syllable.getAttribute("stress");
                if (stressed.equals("1")) {
                    sylPh.append("'");
                } else if (stressed.equals("2")) {
                    sylPh.append(",");
                }
                TreeWalker segWalker = MaryDomUtils.createTreeWalker(doc, syllable, MaryXML.PHONE);
                Element seg;
                // Cannot use tree walker directly, because we concurrently modify the tree:
                List<Element> originalSegments = new ArrayList<Element>();
                while ((seg = (Element) segWalker.nextNode()) != null) {
                    originalSegments.add(seg);
                }
                for (Element s : originalSegments) {
                    String phonemeString = s.getAttribute("p");
                    String[] predicted;
                    // in case we have a decision tree for phoneme, predict - otherwise leave unchanged
                    if ( treeMap.containsKey( phonemeString ) ) {
                        Target tgt = new Target(phonemeString, s);
                        tgt.setFeatureVector(featureComputer.computeFeatureVector(tgt));
                        StringPredictionTree tree = ( StringPredictionTree ) treeMap.get(phonemeString);            
                        String predictStr = tree.getMostProbableString(tgt);
                        if (sylPh.length() > 0) sylPh.append(" ");
                        sylPh.append(predictStr);
                         // if phoneme is deleted:
                         if ( predictStr.equals("") ){
                             predicted = null;
                         } else {
                             // predictStr contains whitespace between phones
                             predicted = predictStr.split(" ");                     
                         }
                    } else {
                        logger.debug("didn't find decision tree for phoneme ("+ phonemeString + "). Just keeping it.");
                        predicted = new String[]{phonemeString};
                    }
                    logger.debug("  Predicted phoneme in sequence of " + predicted.length + " phones." );
                    // deletions:
                    if (predicted == null || predicted.length == 0) {
                        syllable.removeChild(s);
                        continue; // skip what follows
                    }
                    assert predicted != null && predicted.length > 0;
                    // insertions: for each but the last predicted phone, make a new element
                    for ( int lc = 0 ; lc < predicted.length - 1 ; lc++) {
                        Element newPh = MaryXML.createElement(doc, MaryXML.PHONE);
                        newPh.setAttribute("p", predicted[lc]);
                        syllable.insertBefore(newPh, s);
                    }
                    // for the last (or only) predicted segment, just update the phone label
                    if (!phonemeString.equals(predicted[predicted.length - 1])) {
                        s.setAttribute("p", predicted[predicted.length - 1]);
                    }
                } // for each segment in syllable
                String newSylPh = sylPh.toString(); 
                syllable.setAttribute("ph", newSylPh);
                if (tPh.length() > 0) tPh.append(" -"); // syllable boundary
                tPh.append(newSylPh);
            } // for each syllable in token
            t.setAttribute("ph", tPh.toString());
           
        } // for each token in document
                
        // return new MaryData with changed phonology
        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setDocument(doc);
        
        logger.debug("Setting the changed xml document finished.");
        return result;
    }
    
    
    private void createSubStructure(Element token, PhonemeSet phonemeSet)
    {
        String phone = token.getAttribute("ph");
        if (phone.equals(""))
            return; // nothing to do

        StringTokenizer tok = new StringTokenizer(phone, "-_");
        Document document = token.getOwnerDocument();
        while (tok.hasMoreTokens()) {
            String sylString = tok.nextToken();
            Element syllable = MaryXML.createElement(document, MaryXML.SYLLABLE);
            token.appendChild(syllable);
            // Check for stress signs:
            String first = sylString.substring(0, 1);
            if (first.equals("'")) {
                syllable.setAttribute("stress", "1");
                // The primary stressed syllable of a word
                // inherits the accent:
                if (token.hasAttribute("accent")) {
                    syllable.setAttribute("accent", token.getAttribute("accent"));
                }
                sylString = sylString.substring(1);
            } else if (first.equals(",")) {
                syllable.setAttribute("stress", "2");
                sylString = sylString.substring(1);
            }
            // Remember transcription without stress sign in ph attribute:
            syllable.setAttribute("ph", sylString);
            // Now identify the composing segments:
            Phoneme[] phonemes = phonemeSet.splitIntoPhonemes(sylString);
            for (int i = 0; i < phonemes.length; i++) {
                Element segment = MaryXML.createElement(document, MaryXML.PHONE);
                syllable.appendChild(segment);
                segment.setAttribute("p", phonemes[i].name());
                // TODO: need to set loudness-specific voice quality attribute "vq" for de6 and de7
            }
        }
    }
}
