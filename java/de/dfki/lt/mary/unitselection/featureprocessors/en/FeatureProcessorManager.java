package de.dfki.lt.mary.unitselection.featureprocessors.en;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import de.dfki.lt.mary.MaryProperties;
import de.dfki.lt.mary.unitselection.featureprocessors.MaryLanguageFeatureProcessors;
import de.dfki.lt.mary.unitselection.featureprocessors.PhoneSet;
import de.dfki.lt.mary.unitselection.featureprocessors.PhoneSetImpl;

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

            Map posConverter = loadPosConverter();
            addFeatureProcessor(new MaryLanguageFeatureProcessors.Gpos(posConverter));

            //property is set in english.config
            URL phoneSetURL = new URL("file:"
                +MaryProperties.needFilename("english.freetts.phoneSetFile"));
            PhoneSet phoneSet  = new PhoneSetImpl(phoneSetURL);
            addFeatureProcessor(new MaryLanguageFeatureProcessors.Phoneme(phoneSet));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PH_CPlace(phoneSet));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PH_CType(phoneSet));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PH_CVox(phoneSet));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PH_VC(phoneSet));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PH_VFront(phoneSet));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PH_VHeight(phoneSet));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PH_VLength(phoneSet));
            addFeatureProcessor(new MaryLanguageFeatureProcessors.PH_VRnd(phoneSet));

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

        processors_en.put("seg_onsetcoda", 
                new LanguageFeatureProcessors.SegOnsetCoda(phoneSet));
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
            //and we do not need a conversion map
            return new HashMap();}
    }catch(Exception e){
        e.printStackTrace();
        throw new Error("Error reading pos conversion map");
    }
    }

}
