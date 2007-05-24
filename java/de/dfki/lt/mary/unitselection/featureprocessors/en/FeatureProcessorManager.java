package de.dfki.lt.mary.unitselection.featureprocessors.en;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryGenericFeatureProcessors;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryLanguageFeatureProcessors;
import de.dfki.lt.mary.unitselection.featureprocessors.PhoneSet;
import de.dfki.lt.mary.unitselection.featureprocessors.PhoneSetImpl;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryGenericFeatureProcessors.TargetItemNavigator;

public class FeatureProcessorManager extends
        de.dfki.lt.mary.unitselection.featureprocessors.FeatureProcessorManager {
    
    /**
     * Builds a new manager. 
     * This manager uses the english phoneset of FreeTTS
     * and a PoS conversion file if the english PoS tagger is used.
     * All feature processors loaded are language specific.
     */
    public FeatureProcessorManager()
    {
        super();
        try{
            MaryGenericFeatureProcessors.TargetItemNavigator segment = new MaryGenericFeatureProcessors.SegmentNavigator();
            MaryGenericFeatureProcessors.TargetItemNavigator prevSegment = new MaryGenericFeatureProcessors.PrevSegmentNavigator();
            MaryGenericFeatureProcessors.TargetItemNavigator nextSegment = new MaryGenericFeatureProcessors.NextSegmentNavigator();

            Map posConverter = loadPosConverter();
            addFeatureProcessor(new MaryLanguageFeatureProcessors.Gpos(posConverter));

            //property is set in english.config
            URL phoneSetURL = new URL("file:"
                +MaryProperties.needFilename("english.freetts.phoneSetFile"));
            PhoneSet phoneSet  = new PhoneSetImpl(phoneSetURL);
            
            // List of SAMPA phoneme values, this will be the result of calling
            // voice.voice2sampa(String mrpaSymbol).
            String[] phonemeValues = new String[] {
                "0", "V", "i", "I", "U", "{", "@", "r=", "A", "O", "u",
                "E", "EI", "AI", "OI", "aU", "@U", "j", "h", "N", "S",
                "T", "Z", "D", "tS", "dZ", "_", "p", "t", "k", "b", "d",
                "g", "f", "s", "v", "z", "m", "n", "l", "r", "w"
            };
            addFeatureProcessor(new MaryLanguageFeatureProcessors.Phoneme(
                    "mary_phoneme", phonemeValues, segment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.HalfPhoneUnitName(phonemeValues));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.SegOnsetCoda(phoneSet));
            // cplace: 0-n/a l-labial a-alveolar p-palatal b-labio_dental d-dental v-velar g-?
            String[] cplaceValues = new String[] { "0", "l", "a", "p", "b", "d", "v", "g"};
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_ph_cplace", "cplace", cplaceValues, segment));
            // ctype: 0-n/a s-stop f-fricative a-affricative n-nasal l-liquid r-r
            String[] ctypeValues = new String[] {"0", "s", "f", "a", "n", "l", "r"};
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_ph_ctype", "ctype", ctypeValues, segment));
            // cvox: 0=n/a +=on -=off
            String[] cvoxValues = new String[] {"0", "+", "-"};
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_ph_cvox", "cvox", cvoxValues, segment));
            // vc: 0=n/a +=vowel -=consonant
            String[] vcValues = new String[] {"0", "+", "-"};
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_ph_vc", "vc", vcValues, segment));
            // vfront: 0-n/a 1-front  2-mid 3-back
            String[] vfrontValues = new String[] {"0", "1", "2", "3"};
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_ph_vfront", "vfront", vfrontValues, segment));
            // vheight: 0-n/a 1-high 2-mid 3-low
            String[] vheightValues = new String[] {"0", "1", "2", "3"};
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_ph_vheight", "vheight", vheightValues, segment));
            // vlng: 0-n/a s-short l-long d-dipthong a-schwa
            String[] vlngValues = new String[] {"0", "s", "l", "d", "a"};
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_ph_vlng", "vlng", vlngValues, segment));
            // vrnd: 0=n/a +=on -=off
            String[] vrndValues = new String[] {"0", "+", "-"};
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_ph_vrnd", "vrnd", vrndValues, segment));

            // Phonetic features of the previous segment:
            addFeatureProcessor(new MaryLanguageFeatureProcessors.Phoneme(
                    "mary_prev_phoneme", phonemeValues, prevSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_prev_cplace", "cplace", cplaceValues, prevSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_prev_ctype", "ctype", ctypeValues, prevSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_prev_cvox", "cvox", cvoxValues, prevSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_prev_vc", "vc", vcValues, prevSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_prev_vfront", "vfront", vfrontValues, prevSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_prev_vheight", "vheight", vheightValues, prevSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_prev_vlng", "vlng", vlngValues, prevSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_prev_vrnd", "vrnd", vrndValues, prevSegment));

            // Phonetic features of the following segment:
            addFeatureProcessor(new MaryLanguageFeatureProcessors.Phoneme(
                    "mary_next_phoneme", phonemeValues, nextSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_next_cplace", "cplace", cplaceValues, nextSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_next_ctype", "ctype", ctypeValues, nextSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_next_cvox", "cvox", cvoxValues, nextSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_next_vc", "vc", vcValues, nextSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_next_vfront", "vfront", vfrontValues, nextSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_next_vheight", "vheight", vheightValues, nextSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_next_vlng", "vlng", vlngValues, nextSegment));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PhoneFeature(phoneSet,
                    "mary_next_vrnd", "vrnd", vrndValues, nextSegment));

            String wordFrequencyFilename = MaryProperties.getFilename("english.wordFrequency.fst");
            String wordFrequencyEncoding = MaryProperties.getProperty("english.wordFrequency.encoding");
            addFeatureProcessor(new MaryLanguageFeatureProcessors.WordFrequency(wordFrequencyFilename, wordFrequencyEncoding));

            /* for database selection*/ 
            String[] phoneClasses = new String[] {
                    "0", "c_labial", "c_alveolar", "c_palatal", 
                    "c_labiodental", "c_dental", "c_velar", 
                    "c_glottal", "v_i", "v_u", "v_o",
                    "v_E", "v_EI", "v_V", "v_@", "v_r=", "v_@U",
                    "v_OI", "v_{", "v_aU", "v_AI"
            };
            //map from phones to their classes
            Map phone2Classes = new HashMap();
            //put in vowels
            phone2Classes.put("I","v_i");
            phone2Classes.put("i","v_i");
            phone2Classes.put("U","v_u");
            phone2Classes.put("u","v_u");
            phone2Classes.put("A","v_o");
            phone2Classes.put("O","v_o");
            phone2Classes.put("E","v_E");
            phone2Classes.put("EI","v_EI");
            phone2Classes.put("V","v_V");
            phone2Classes.put("@","v_@");
            phone2Classes.put("r=","v_r=");
            phone2Classes.put("@U","v_@U");
            phone2Classes.put("OI","v_OI");
            phone2Classes.put("{","v_{");
            phone2Classes.put("aU","v_aU");
            phone2Classes.put("AI","v_AI");
            //put in consonants
            phone2Classes.put("b","c_labial");
            phone2Classes.put("m","c_labial");
            phone2Classes.put("p","c_labial");
            phone2Classes.put("w","c_labial");
            phone2Classes.put("d","c_alveolar");
            phone2Classes.put("l","c_alveolar");
            phone2Classes.put("n","c_alveolar");
            phone2Classes.put("r","c_alveolar");
            phone2Classes.put("s","c_alveolar");
            phone2Classes.put("t","c_alveolar");
            phone2Classes.put("z","c_alveolar");
            phone2Classes.put("tS","c_palatal");
            phone2Classes.put("dZ","c_palatal");
            phone2Classes.put("S","c_palatal");
            phone2Classes.put("j","c_palatal");
            phone2Classes.put("Z","c_palatal");
            phone2Classes.put("f","c_labiodental");
            phone2Classes.put("v","c_labiodental");
            phone2Classes.put("D","c_dental");
            phone2Classes.put("T","c_dental");
            phone2Classes.put("g","c_velar");
            phone2Classes.put("k","c_velar");
            phone2Classes.put("N","c_velar");
            phone2Classes.put("h","c_glottal");
            phone2Classes.put("_","0");
           
           addFeatureProcessor(new MaryLanguageFeatureProcessors.Selection_PhoneClass(
                   phone2Classes, phoneClasses, nextSegment));
            
            
/*
        processors_en.put("seg_coda_fric", 
                new LanguageFeatureProcessors.SegCodaFric(phoneSet));
        processors_en.put("seg_onset_fric", 
                new LanguageFeatureProcessors.SegOnsetFric(phoneSet));

        processors_en.put("seg_coda_stop", 
                new LanguageFeatureProcessors.SegCodaStop(phoneSet));
        processors_en.put("seg_onset_stop", 
                new LanguageFeatureProcessors.SegOnsetStop(phoneSet));

        processors_en.put("seg_coda_nasal", 
                new LanguageFeatureProcessors.SegCodaNasal(phoneSet));
        processors_en.put("seg_onset_nasal", 
                new LanguageFeatureProcessors.SegOnsetNasal(phoneSet));

        processors_en.put("seg_coda_glide", 
                new LanguageFeatureProcessors.SegCodaGlide(phoneSet));
        processors_en.put("seg_onset_glide", 
                new LanguageFeatureProcessors.SegOnsetGlide(phoneSet));

        processors_en.put("syl_codasize", 
                new LanguageFeatureProcessors.SylCodaSize(phoneSet));
        processors_en.put("syl_onsetsize", 
                new LanguageFeatureProcessors.SylOnsetSize(phoneSet));
        processors_en.put("accented", new GenericFeatureProcessors.Accented());
        
        processors_en.put("token_pos_guess", 
                new LanguageFeatureProcessors.TokenPosGuess());
  */      
        }   catch(Exception e){
            e.printStackTrace();
            throw new Error("Problem building Pos or PhoneSet");}
        }
    
    /**
     * Loads the PoS conversion file, if it is needed
     * @return the PoS conversion map
     */
    private Map loadPosConverter(){
        try{
            //property is set in english.shprot
        String file = 
            MaryProperties.getFilename("english.freetts.posConverterFile", "").trim();
        if (!file.equals("")){
            Map posConverter = new HashMap();
            BufferedReader reader = 
                new BufferedReader(new FileReader(new File (file)));
            String line = reader.readLine();
            while (line!=null){
                if(!(line.startsWith("***"))){
                    
                  StringTokenizer st = 
                    new StringTokenizer(line," ");
                  String word = st.nextToken();
                  String pos = st.nextToken();
                  posConverter.put(word,pos);}
                line = reader.readLine();
            }
            return posConverter;
        }else{
            //if file name is not given,
            //the english tagger is not loaded
            //and we do not need a conversion map;
            return new HashMap();}
    }catch(Exception e){
        e.printStackTrace();
        throw new Error("Error reading pos conversion map");
    }
    }

}
