package marytts.language.en.features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.features.MaryGenericFeatureProcessors;
import marytts.features.MaryLanguageFeatureProcessors;
import marytts.features.PhoneSet;
import marytts.features.PhoneSetImpl;
import marytts.features.MaryGenericFeatureProcessors.TargetElementNavigator;
import marytts.server.MaryProperties;


public class FeatureProcessorManager extends
        marytts.features.FeatureProcessorManager {
    
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
            Map<String,String> posConverter = loadPosConverter();
            addFeatureProcessor(new MaryLanguageFeatureProcessors.Gpos(posConverter));

            //property is set in english.config
            URL phoneSetURL = new URL("file:"
                +MaryProperties.needFilename("english.phoneSetFile"));
            PhoneSet phoneSet  = new PhoneSetImpl(phoneSetURL);
            
          
            // Phonetic features of the current segment:
            
            
            // List of SAMPA phoneme values, this will be the result of calling
            // voice.voice2sampa(String mrpaSymbol).
            String[] phonemeValues = new String[] {
                "0", "V", "i", "I", "U", "{", "@", "r=", "A", "O", "u",
                "E", "EI", "AI", "OI", "aU", "@U", "j", "h", "N", "S",
                "T", "Z", "D", "tS", "dZ", "_", "p", "t", "k", "b", "d",
                "g", "f", "s", "v", "z", "m", "n", "l", "r", "w"
            };
            
            /**TODO: 
             *      Remove hardcoded phoneme values and automatically load values as below commented code.
             *      At present, automatic loading not working because all voices built with
             *      hardcoded phoneme sequence.  
             */
            
            /*String[] phonemes = phoneSet.listPhonemes();
            String[] phonemeValues = new String[phonemes.length+1];
            phonemeValues[0] = "0";
            System.arraycopy(phonemes, 0, phonemeValues, 1, phonemes.length);*/

            String pauseSymbol = "_";  // TODO: get pause symbol from phone set
            setupPhonemeFeatureProcessors(phoneSet, phonemeValues, pauseSymbol);

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
            Map<String, String> phone2Classes = new HashMap<String, String>();
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
           
            MaryGenericFeatureProcessors.TargetElementNavigator nextSegment = new MaryGenericFeatureProcessors.NextSegmentNavigator();

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
    private Map<String,String> loadPosConverter(){
        try{
            //property is set in english.shprot
        String file = 
            MaryProperties.getFilename("english.freetts.posConverterFile", "").trim();
        if (!file.equals("")){
            Map<String, String> posConverter = new HashMap<String, String>();
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
            return new HashMap<String, String>();}
    }catch(Exception e){
        e.printStackTrace();
        throw new Error("Error reading pos conversion map");
    }
    }

}
