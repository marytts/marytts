/**
 * Copyright 2007 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection.voiceimport;

import java.util.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;

/**
 * Install a voice by copying the voice data to marybase/lib/voices/voicename/
 * and creating a config file marybase/conf/locale-voicename.config
 * 
 * @author Anna Hunecke
 *
 */
public class VoiceInstaller extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "VoiceInstaller";
    
    public final String CARTFILE = name+".cartFile";
    public final String DURTREE = name+".durTree";
    public final String F0LEFTTREE = name+".f0LeftTree";
    public final String F0MIDTREE = name+".f0MidTree";
    public final String F0RIGHTTREE = name+".f0RightTree";
    public final String HALFPHONEFEATSAC = name+".halfPhoneFeatsAc";
    public final String HALFPHONEFEATDEFAC = name+".halfPhoneFeatDefAc";
    public final String HALFPHONEUNITS = name+".halfPhoneUnits";
    public final String JOINCOSTFEATS = name+".joinCostFeats";
    public final String JOINCOSTFEATDEF = name+".joinCostFeatDef";
    public final String PHONEFEATDEF = name+".phoneFeatDef";
    public final String EXAMPLETEXT = name+".exampleText";
    public final String WAVETIMELINE = name+".waveTimeline";
    public final String BASETIMELINE = name+".basenameTimeline";

    public String getName(){
        return name;
    }

    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap();
           String maryext = db.getProp(db.MARYEXT);
           props.put(CARTFILE, "cart"+maryext);
           props.put(DURTREE, "dur.tree");
           props.put(F0LEFTTREE, "f0.left.tree");
           props.put(F0MIDTREE, "f0.mid.tree");
           props.put(F0RIGHTTREE, "f0.right.tree");
           props.put(HALFPHONEFEATSAC, "halfphoneFeatures_ac"+maryext);
           props.put(HALFPHONEFEATDEFAC, "halfphoneUnitFeatureDefinition_ac.txt");
           props.put(HALFPHONEUNITS, "halfphoneUnits"+maryext);
           props.put(JOINCOSTFEATS, "joinCostFeatures"+maryext);
           props.put(JOINCOSTFEATDEF, "joinCostWeights.txt");
           props.put(PHONEFEATDEF, "phoneUnitFeatureDefinition.txt");
           props.put(EXAMPLETEXT, "examples.text");
           props.put(WAVETIMELINE, "timeline_waveforms"+maryext);
           props.put(BASETIMELINE, "timeline_basenames"+maryext);
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(CARTFILE, "file containing the preselection CART");
        props2Help.put(DURTREE, "file containing the duration CART");
        props2Help.put(F0LEFTTREE, "file containing the left f0 CART");
        props2Help.put(F0MIDTREE, "file containing the mid f0 CART");
        props2Help.put(F0RIGHTTREE, "file containing the right f0 CART");
        props2Help.put(HALFPHONEFEATSAC, "file containing all halfphone units and their target cost features"
								  +"plus the acoustic target cost features");
        props2Help.put(HALFPHONEFEATDEFAC, "file containing the list of halfphone target cost features, their values and weights");
        props2Help.put(HALFPHONEUNITS, "file containing all halfphone units");
        props2Help.put(JOINCOSTFEATS, "file containing all halfphone units and their join cost features");
        props2Help.put(JOINCOSTFEATDEF, "file containing the list of join cost weights and their weights ");
        props2Help.put(PHONEFEATDEF, "file containing the list of phone target cost features, their values and weights");
        props2Help.put(EXAMPLETEXT, "file containing example text (for limited domain voices only)");
        props2Help.put(WAVETIMELINE, "file containing all wave files");
        props2Help.put(BASETIMELINE, "file containing all basenames");
        
    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        System.out.println("Installing voice: ");
        /* make a new directory for the voice */
        System.out.println("Making voice directory ... ");
        String fileSeparator = System.getProperty("file.separator");
        String filedir = db.getProp(db.FILEDIR);
        String configdir = db.getProp(db.CONFIGDIR);
        String newVoiceDir = db.getProp(db.MARYBASE)
        					+"lib"+fileSeparator
        					+"voices"+fileSeparator
        					+db.getProp(db.VOICENAME).toLowerCase()
        					+fileSeparator;
        File newVoiceDirFile = new File(newVoiceDir);
        if (!newVoiceDirFile.exists()) newVoiceDirFile.mkdir();
        
        /* copy the files */
        System.out.println("Copying files ... ");
        try{
            File in, out;
            in = new File(filedir+getProp(CARTFILE));
            out = new File(newVoiceDir+getProp(CARTFILE));
            copy(in,out);   
            in = new File(filedir+getProp(DURTREE));
            out = new File(newVoiceDir+getProp(DURTREE));
            copy(in,out);   
            in = new File(filedir+getProp(F0LEFTTREE));
            out = new File(newVoiceDir+getProp(F0LEFTTREE));
            copy(in,out);   
            in = new File(filedir+getProp(F0MIDTREE));
            out = new File(newVoiceDir+getProp(F0MIDTREE));
            copy(in,out);   
            in = new File(filedir+getProp(F0RIGHTTREE));
            out = new File(newVoiceDir+getProp(F0RIGHTTREE));
            copy(in,out);   
            in = new File(filedir+getProp(HALFPHONEFEATSAC));
            out = new File(newVoiceDir+getProp(HALFPHONEFEATSAC));
            copy(in,out);   
            in = new File(configdir+getProp(HALFPHONEFEATDEFAC));
            out = new File(newVoiceDir+getProp(HALFPHONEFEATDEFAC));
            copy(in,out);   
            in = new File(filedir+getProp(HALFPHONEUNITS));
            out = new File(newVoiceDir+getProp(HALFPHONEUNITS));
            copy(in,out);   
            in = new File(filedir+getProp(JOINCOSTFEATS));
            out = new File(newVoiceDir+getProp(JOINCOSTFEATS));
            copy(in,out);  
            in = new File(configdir+getProp(JOINCOSTFEATDEF));
            out = new File(newVoiceDir+getProp(JOINCOSTFEATDEF));
            copy(in,out);   
            in = new File(configdir+getProp(PHONEFEATDEF));
            out = new File(newVoiceDir+getProp(PHONEFEATDEF));
            copy(in,out);   
            in = new File(filedir+getProp(EXAMPLETEXT));
            out = new File(newVoiceDir+getProp(EXAMPLETEXT));
            if (in.exists()){
                copy(in,out);   
            } else {
                createExampleText(out);
            }
            in = new File(filedir+getProp(WAVETIMELINE));
            out = new File(newVoiceDir+getProp(WAVETIMELINE));
            copy(in,out);   
            in = new File(filedir+getProp(BASETIMELINE));
            out = new File(newVoiceDir+getProp(BASETIMELINE));
            copy(in,out);
        }catch (IOException ioe){
            return false;
        }
        
        /* create the config file */
        System.out.println("Creating config file ... ");
        String cutLocale = db.getProp(db.LOCALE).toLowerCase();
        String longLocale;
        if (cutLocale.equals("en_us") || cutLocale.equals("en")){ 
            cutLocale = "en";
            longLocale = "english";
        } else {
            if (cutLocale.equals("de")){
                longLocale = "german";
            } else {
                //unsupported locale
                longLocale = "unsupported";
            }
        }
        
        String configFileName = db.getProp(db.MARYBASE)
        					+"conf"+fileSeparator
        					+longLocale
        					+"-"+db.getProp(db.VOICENAME).toLowerCase()
        					+".config";
        createConfigFile(configFileName, newVoiceDir, cutLocale, longLocale);
        System.out.println("... done! ");
        System.out.println("To run the voice, restart your Mary server");
        return true;
        }
    
    private void copy(File source, File dest)throws IOException{
        try {          
            FileChannel in = new FileInputStream(source).getChannel();
            FileChannel out = new FileOutputStream(dest).getChannel();   
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());            
            out.write(buf);
            in.close();
            out.close();
        } catch (Exception e){
            System.out.println("Error copying file "
                    +source.getAbsolutePath()+" to "+dest.getAbsolutePath()
                    +" : "+e.getMessage());
            throw new IOException();
        }
    }
    
    
    private void createExampleText(File exampleTextFile) throws IOException{
        try{
            //just take the first three transcript files as example text
            PrintWriter exampleTextOut =
                new PrintWriter(
                        new FileWriter(exampleTextFile),true);
            for (int i=0;i<3;i++){
                String basename = bnl.getName(i);
                BufferedReader transIn = 
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(
                                            new File(db.getProp(db.TEXTDIR)
                                                    +basename+db.getProp(db.TEXTEXT)))));
                String text = transIn.readLine();
                transIn.close();            
                exampleTextOut.println(text);
            }
            exampleTextOut.close();
            
        } catch (Exception e){
            System.out.println("Error creating example text file "
                    +exampleTextFile.getName());
            throw new IOException();
        }
        
    }
    
    
    private void createConfigFile(String filename, 
            					String newVoiceDir,
            					String cutLocale,
            					String longLocale){
        try{
            PrintWriter configOut = 
                new PrintWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(
                                        new File(filename)),"UTF-8"),true);
            String voicename = db.getProp(db.VOICENAME).toLowerCase();
            //print the header
            configOut.println("#Auto-generated config file for voice "+voicename+"\n");
            //print name and version info
             configOut.println("name = "+voicename);
             //print providing info
             configOut.println("# Declare \"group names\" as component that other components can require.\n"+
                     	"# These correspond to abstract \"groups\" of which this component is an instance.\n"+
                     	"provides = \\\n"+longLocale+"-voice\n");
             configOut.println(longLocale+"-voice.version = "+db.getProp(db.MARYBASEVERSION)+"\n");
             configOut.println("voice.version = "+db.getProp(db.MARYBASEVERSION)+"\n");
             configOut.println("# List the dependencies, as a whitespace-separated list.\n"+
                     "# For each required component, an optional minimum version and an optional\n"+
                     "# download url can be given.\n"+
                     "# We can require a component by name or by an abstract \"group name\"\n"+ 
                     "# as listed under the \"provides\" element.\n"+
             		 "requires = \\\n"+longLocale+" \\\nmarybase\n\n"+
             		 "requires.marybase.version = 3.1.0\n"+
             		 "requires."+longLocale+".version = 3.0.0\n"+
             		 "requires."+longLocale+".download = http://mary.dfki.de/download/mary-install-3.x.x.jar\n");
             
             //now follow the module settings
              configOut.println("####################################################################\n"+
                      "####################### Module settings  ###########################\n"+
                      "####################################################################\n"+
                      "# For keys ending in \".list\", values will be appended across config files,\n"+
                      "# so that .list keys can occur in several config files.\n"+
                      "# For all other keys, values will be copied to the global config, so\n"+
              		  "# keys should be unique across config files.\n");
              
              String voiceHeader = "voice."+voicename;
              
              //wants-to-be-default value
              configOut.println("# If this setting is not present, a default value of 0 is assumed.\n"+
                      voiceHeader+".wants.to.be.default = 20\n");
              
              //add to voice to list of unitselection voices
              configOut.println("# Add your voice to the list of Unit Selection Voices\n"+
                      "unitselection.voices.list = \\\n"+voicename+"\n");
            
              //properties of the voice
              configOut.println("# Set your voice specifications\n"+
                      voiceHeader+".gender = "+db.getProp(db.GENDER).toLowerCase()+"\n"+
                      voiceHeader+".locale = "+db.getProp(db.LOCALE)+"\n"+
                      voiceHeader+".domain = "+db.getProp(db.DOMAIN).toLowerCase()+"\n"+
                      voiceHeader+".samplingRate = "+db.getProp(db.SAMPLINGRATE)+"\n");
              
              //Weight of the target cost function vs. the join cost function
              configOut.println("# Relative weight of the target cost function vs. the join cost function\n"+
                      voiceHeader+".viterbi.wTargetCosts = "+"0.95"+"\n");
              
              //language specific settings 
              if (!cutLocale.equals("en")||cutLocale.equals("de")){
                  configOut.println("Unsupported locale "+db.getProp(db.LOCALE));
              }
              if (cutLocale.equals("en")){
                  configOut.println("# Only set the lexicon for English\n"+
                          voiceHeader+".lexiconClass = com.sun.speech.freetts.en.us.CMULexicon\n"+
                          voiceHeader+".lexicon = cmudict04\n\n"+
                          "# Phoneme conversion for English voices \n"+
                          voiceHeader+".sampamapfile = MARY_BASE/lib/modules/en/synthesis/sampa2mrpa_en.map\n\n"+
                          "# Language-specific feature processor manager:\n"+
                          voiceHeader+".featureProcessorsClass = de.dfki.lt.mary.unitselection.featureprocessors.en.FeatureProcessorManager\n");                  
              } else {
                  //cutLocale.equals("de")
                  configOut.println("# Sampa mapping for German voices \n"+
                          voiceHeader+".sampamap = \\\n"+
                          "=6->6 \\\n"+"=n->n \\\n"+"=m->m \\\n"+
                          "=N->N \\\n"+"=l->l \\\n"+"i->i: \\\n"+
                          "e->e: \\\n"+"u->u: \\\n"+"o->o: \n\n"+
                          "# Language-specific feature processor manager:\n"+
                          voiceHeader+".featureProcessorsClass = de.dfki.lt.mary.unitselection.featureprocessors.de.FeatureProcessorManager\n");       
              }
              
              //unit selection classes
              configOut.println("# Java classes to use for the various unit selection components\n"+
                      voiceHeader+".databaseClass            = de.dfki.lt.mary.unitselection.DiphoneUnitDatabase\n"+
                      voiceHeader+".selectorClass            = de.dfki.lt.mary.unitselection.DiphoneUnitSelector\n"+
                      voiceHeader+".concatenatorClass        = de.dfki.lt.mary.unitselection.concat.OverlapUnitConcatenator\n"+
                      voiceHeader+".targetCostClass          = de.dfki.lt.mary.unitselection.DiphoneFFRTargetCostFunction\n"+
                      voiceHeader+".joinCostClass            = de.dfki.lt.mary.unitselection.JoinCostFeatures\n"+
                      voiceHeader+".unitReaderClass          = de.dfki.lt.mary.unitselection.UnitFileReader\n"+
                      voiceHeader+".cartReaderClass          = de.dfki.lt.mary.unitselection.cart.ClassificationTree\n"+
              		  voiceHeader+".audioTimelineReaderClass = de.dfki.lt.mary.unitselection.TimelineReader\n");
             
              //voice data
              configOut.println("# Voice-specific files\n"+
                      voiceHeader+".featureFile       = MARY_BASE/lib/voices/"+voicename+"/"+getProp(HALFPHONEFEATSAC)+"\n"+
                      voiceHeader+".targetCostWeights = MARY_BASE/lib/voices/"+voicename+"/"+getProp(HALFPHONEFEATDEFAC)+"\n"+
                      voiceHeader+".joinCostFile      = MARY_BASE/lib/voices/"+voicename+"/"+getProp(JOINCOSTFEATS)+"\n"+
                      voiceHeader+".joinCostWeights   = MARY_BASE/lib/voices/"+voicename+"/"+getProp(JOINCOSTFEATDEF)+"\n"+
                      voiceHeader+".unitsFile         = MARY_BASE/lib/voices/"+voicename+"/"+getProp(HALFPHONEUNITS)+"\n"+
                      voiceHeader+".cartFile          = MARY_BASE/lib/voices/"+voicename+"/"+getProp(CARTFILE)+"\n"+
              		  voiceHeader+".audioTimelineFile = MARY_BASE/lib/voices/"+voicename+"/"+getProp(WAVETIMELINE)+"\n"+
                      voiceHeader+".basenameTimeline = MARY_BASE/lib/voices/"+voicename+"/"+getProp(BASETIMELINE)+"\n");
                      
              
              if (db.getProp(db.DOMAIN).equals("limited")){
                  configOut.println("# Location of example text\n"+
              		  voiceHeader+".exampleTextFile = MARY_BASE/lib/voices/"+voicename+"/"+getProp(EXAMPLETEXT)+"\n");
              }
              
              configOut.println("# Voice-specific prosody CARTs:\n"+
                      voiceHeader+".duration.cart = MARY_BASE/lib/voices/"+voicename+"/"+getProp(DURTREE)+"\n"+
                      voiceHeader+".duration.featuredefinition = MARY_BASE/lib/voices/"+voicename+"/"+getProp(PHONEFEATDEF)+"\n"+
                      voiceHeader+".f0.cart.left = MARY_BASE/lib/voices/"+voicename+"/"+getProp(F0LEFTTREE)+"\n"+
                      voiceHeader+".f0.cart.mid = MARY_BASE/lib/voices/"+voicename+"/"+getProp(F0MIDTREE)+"\n"+
                      voiceHeader+".f0.cart.right = MARY_BASE/lib/voices/"+voicename+"/"+getProp(F0RIGHTTREE)+"\n"+
              		  voiceHeader+".f0.featuredefinition = MARY_BASE/lib/voices/"+voicename+"/"+getProp(PHONEFEATDEF)+"\n");
              
              
        } catch (Exception e){
            throw new Error("Error writing config file : "
                    +e.getMessage());
        }
    }
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress(){
        return -1;
    }
    
}