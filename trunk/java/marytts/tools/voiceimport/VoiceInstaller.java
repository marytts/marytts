/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;

/**
 * Install a voice by copying the voice data to marybase/lib/voices/voicename/
 * and creating a config file marybase/conf/locale-voicename.config
 * 
 * @author Anna Hunecke
 *
 */
@Deprecated
public class VoiceInstaller extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "VoiceInstaller";
    
    public final String CARTFILE = name+".cartFile";
    public final String DURTREE = name+".durTree";
    public final String F0LEFTTREE = name+".f0LeftTree";
    public final String F0MIDTREE = name+".f0MidTree";
    public final String F0RIGHTTREE = name+".f0RightTree";
    public final String CONCATENATORCLASS = name + ".concatenatorClass";
    public final String TIMELINEREADERCLASS = name + ".timelineReaderClass";
    public final String HALFPHONEFEATSAC = name+".halfPhoneFeatsAc";
    public final String HALFPHONEFEATDEFAC = name+".halfPhoneFeatDefAc";
    public final String HALFPHONEUNITS = name+".halfPhoneUnits";
    public final String JOINCOSTFEATS = name+".joinCostFeats";
    public final String JOINCOSTFEATDEF = name+".joinCostFeatDef";
    public final String PHONEFEATDEF = name+".phoneFeatDef";
    public final String EXAMPLETEXT = name+".exampleText";
    public final String WAVETIMELINE = name+".waveTimeline";
    public final String BASETIMELINE = name+".basenameTimeline";
    
    public final String CREATEZIPFILE = name+".createZipFile";
    public final String ZIPCOMMAND = name+".zipCommand";
    public final String LEGACYACOUSTICMODELS = name+".legacyAcousticModels";

    public String getName(){
        return name;
    }

    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb)
    {
        this.db = theDb;
        if (props == null){
            props = new TreeMap<String, String>();
            String maryext = db.getProp(db.MARYEXT);
            props.put(CARTFILE, "cart"+maryext);
            props.put(DURTREE, "dur.tree");
            props.put(F0LEFTTREE, "f0.left.tree");
            props.put(F0MIDTREE, "f0.mid.tree");
            props.put(F0RIGHTTREE, "f0.right.tree");
            props.put(CONCATENATORCLASS, "OverlapUnitConcatenator");
            props.put(TIMELINEREADERCLASS, "TimelineReader");
            props.put(HALFPHONEFEATSAC, "halfphoneFeatures_ac"+maryext);
            props.put(HALFPHONEFEATDEFAC, "halfphoneUnitFeatureDefinition_ac.txt");
            props.put(HALFPHONEUNITS, "halfphoneUnits"+maryext);
            props.put(JOINCOSTFEATS, "joinCostFeatures"+maryext);
            props.put(JOINCOSTFEATDEF, "joinCostWeights.txt");
            props.put(PHONEFEATDEF, "phoneUnitFeatureDefinition.txt");
            props.put(EXAMPLETEXT, "examples.text");
            props.put(WAVETIMELINE, "timeline_waveforms"+maryext);
            props.put(BASETIMELINE, "timeline_basenames"+maryext);
            props.put(CREATEZIPFILE, "false");
            props.put(ZIPCOMMAND, "/usr/bin/zip");
            props.put(LEGACYACOUSTICMODELS, "false");
        }
        return props;
    }
    
    protected void setupHelp()
    {
        props2Help = new TreeMap<String, String>();
        props2Help.put(CARTFILE, "file containing the preselection CART");
        props2Help.put(DURTREE, "file containing the duration CART");
        props2Help.put(F0LEFTTREE, "file containing the left f0 CART");
        props2Help.put(F0MIDTREE, "file containing the mid f0 CART");
        props2Help.put(F0RIGHTTREE, "file containing the right f0 CART");
        props2Help.put(CONCATENATORCLASS, "class of unit concatenator (default: <tt>OverlapUnitConcatenator</tt>, "
                + "but experimental support for <tt>FdpsolaUnitConcatenator</tt> and <tt>HnmUnitConcatenator</tt>)");
        props2Help.put(TIMELINEREADERCLASS, "class of TimelineReader (default: <tt>TimelineReader</tt>, "
                + "but set to <tt>HnmTimelineReader</tt> for experimental HNM synthesis support)");
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
        props2Help.put(CREATEZIPFILE, "create zip file for Mary voices installation (used by Mary voices administrator only).");
        props2Help.put(ZIPCOMMAND, "zip command to create a voice.zip file for voice installation.");
        props2Help.put(LEGACYACOUSTICMODELS, "create the config file using legacy acoustic modellers (instead of AcousticModeller module).");        
    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        String fileSeparator = System.getProperty("file.separator");
        String maryBase = db.getProp(db.MARYBASE);

        /* create the config file */
        System.out.println("Creating config file ... ");
        // Normalise locale: (e.g., if user set en-US, change it to en_US)
        String locale = MaryUtils.string2locale(db.getProp(db.LOCALE)).toString();
        
        String configFileName = db.getProp(db.FILEDIR) + locale + "-" + db.getProp(db.VOICENAME).toLowerCase() + ".config";
        createConfigFile(configFileName, locale);
        String finalConfigFileName = configFileName.replace(db.getProp(db.FILEDIR), maryBase + fileSeparator + "conf" + fileSeparator);
        
        System.out.println("Installing voice: ");
        /* make a new directory for the voice */
        System.out.println("Making voice directory ... ");
        String filedir = db.getProp(db.FILEDIR);
        String configdir = db.getProp(db.CONFIGDIR);
        if (!maryBase.endsWith(fileSeparator)) maryBase = maryBase + fileSeparator;
        String newVoiceDir = maryBase
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
            in = new File(configFileName);
            out = new File(finalConfigFileName);
            copy(in, out);
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
        
        /* create a zip file for installation */        
        if( getProp(CREATEZIPFILE).contentEquals("true") ) {
          System.out.println("\nCreating voice installation file: ");  
          String maryBaseForShell = maryBase.replaceAll(" ", Pattern.quote("\\ "));
          String installZipFile = locale
                               + "-"+db.getProp(db.VOICENAME).toLowerCase()
                               + ".zip";
          configFileName = "conf"+fileSeparator
                         + locale
                         + "-"+db.getProp(db.VOICENAME).toLowerCase()
                         + ".config";
          
          
          String cmdLine = "cd "+ maryBaseForShell + "\n" + getProp(ZIPCOMMAND) + " " 
                         + installZipFile + " " 
                         + configFileName + " "
                         + "lib/voices/" + db.getProp(db.VOICENAME).toLowerCase() + fileSeparator + "*";  
                          
          General.launchBatchProc(cmdLine, "zip", filedir);
          
          System.out.println();
          System.out.println("Created voice installation file: " + db.getProp(db.MARYBASE) + fileSeparator + locale
                  + "-"+db.getProp(db.VOICENAME).toLowerCase() + ".zip\n");  
        }
        
        
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
    
    
    private void createExampleText(File exampleTextFile) throws IOException
    {
        try{
            //just take the first three transcript files as example text
            PrintWriter exampleTextOut = new PrintWriter(new FileWriter(exampleTextFile),true);
            for (int i=0;i<3;i++) {
                String basename = bnl.getName(i);
                BufferedReader transIn = new BufferedReader(new InputStreamReader(
                   new FileInputStream(new File(db.getProp(db.TEXTDIR)+basename+db.getProp(db.TEXTEXT)))));
                String text = transIn.readLine();
                transIn.close();            
                exampleTextOut.println(text);
            }
            exampleTextOut.close();
            
        } catch (IOException e) {
            IOException myIOE = new IOException("Problem creating example text file "
                    +exampleTextFile.getName());
            myIOE.initCause(e);
            throw myIOE;
        }
        
    }
    
    
    private void createConfigFile(String filename, String locale)
    throws IOException
    {
        try {
            PrintWriter configOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                                        new File(filename)),"UTF-8"), true);
            String voicename = db.getProp(db.VOICENAME).toLowerCase();
            //print the header
            configOut.println("#Auto-generated config file for voice "+voicename+"\n");
            //print name and version info
             configOut.println("name = "+voicename);
             //print providing info
             configOut.println("# Declare \"group names\" as component that other components can require.\n"+
                     	"# These correspond to abstract \"groups\" of which this component is an instance.\n"+
                     	"provides = \\\n"+locale+"-voice\n");
             configOut.println(locale+"-voice.version = "+db.getProp(db.MARYBASEVERSION)+"\n");
             configOut.println("voice.version = "+db.getProp(db.MARYBASEVERSION)+"\n");
             configOut.println("# List the dependencies, as a whitespace-separated list.\n"+
                     "# For each required component, an optional minimum version and an optional\n"+
                     "# download url can be given.\n"+
                     "# We can require a component by name or by an abstract \"group name\"\n"+ 
                     "# as listed under the \"provides\" element.\n"+
             		 "requires = \\\n"+locale+" \\\nmarybase\n\n"+
             		 "requires.marybase.version = 4.0.0\n"+
             		 "requires."+locale+".version = 4.0.0\n"+
             		 "requires."+locale+".download = http://mary.dfki.de/download/mary-install-4.x.x.jar\n");
             
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
                      voiceHeader+".viterbi.wTargetCosts = 0.7\n");
              // Viterbit beam size
              configOut.println("# Beam size in dynamic programming: smaller => faster but worse quality.\n"
                      +"# (set to -1 to disable beam search; very slow but best available quality)\n"
                      +voiceHeader+".viterbi.beamsize = 100\n");
              
              //language specific settings 
              if (locale.equals("de")) {
                  configOut.println("# Sampa mapping for German voices \n"+
                          voiceHeader+".sampamap = \\\n"+
                          "=6->6 \\\n"+"=n->n \\\n"+"=m->m \\\n"+
                          "=N->N \\\n"+"=l->l \\\n"+"i->i: \\\n"+
                          "e->e: \\\n"+"u->u: \\\n"+"o->o: \n\n");       
              }
              
              //unit selection classes
              configOut.println("# Java classes to use for the various unit selection components\n"+
                      voiceHeader+".databaseClass            = marytts.unitselection.data.DiphoneUnitDatabase\n"+
                      voiceHeader+".selectorClass            = marytts.unitselection.select.DiphoneUnitSelector\n"+
                      voiceHeader+".concatenatorClass        = marytts.unitselection.concat." + getProp(CONCATENATORCLASS) + "\n" +
                      voiceHeader+".targetCostClass          = marytts.unitselection.select.DiphoneFFRTargetCostFunction\n"+
                      voiceHeader+".joinCostClass            = marytts.unitselection.select.JoinCostFeatures\n"+
                      voiceHeader+".unitReaderClass          = marytts.unitselection.data.UnitFileReader\n"+
                      voiceHeader+".cartReaderClass          = marytts.cart.io.MARYCartReader\n"+
              		  voiceHeader+".audioTimelineReaderClass = marytts.unitselection.data." + getProp(TIMELINEREADERCLASS) + "\n");
             
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
              
              // allow creation of config entries to use legacy acoustic modeller modules (CARTF0Modeller, CARTDurationModeller)
              // for backward compatibility:
              if (getProp(LEGACYACOUSTICMODELS).equals("true")) {
                  configOut.println("# Voice-specific prosody CARTs:\n"+
                          voiceHeader+".duration.cart = MARY_BASE/lib/voices/"+voicename+"/"+getProp(DURTREE)+"\n"+
                          voiceHeader+".duration.featuredefinition = MARY_BASE/lib/voices/"+voicename+"/"+getProp(PHONEFEATDEF)+"\n"+
                          voiceHeader+".f0.cart.left = MARY_BASE/lib/voices/"+voicename+"/"+getProp(F0LEFTTREE)+"\n"+
                          voiceHeader+".f0.cart.mid = MARY_BASE/lib/voices/"+voicename+"/"+getProp(F0MIDTREE)+"\n"+
                          voiceHeader+".f0.cart.right = MARY_BASE/lib/voices/"+voicename+"/"+getProp(F0RIGHTTREE)+"\n"+
                          voiceHeader+".f0.featuredefinition = MARY_BASE/lib/voices/"+voicename+"/"+getProp(PHONEFEATDEF)+"\n");

                  configOut.println();

                  // And finally, determine how to predict acoustic features for this voice:
                  configOut.println("# Modules to use for predicting acoustic target features for this voice:\n"+
                          voiceHeader+".preferredModules =  \\\n"+
                          "    marytts.modules.CARTDurationModeller("+locale+","+voiceHeader+".duration.) \\\n"+
                          "    marytts.modules.CARTF0Modeller("+locale+","+voiceHeader+".f0.)\n");
              } else {
                  // TODO this is currently hard-coded for CARTs; the various voice installer components should probably be unified...
                  configOut.println("# Modules to use for predicting acoustic target features for this voice:");
                  configOut.println();
                  configOut.println(voiceHeader + ".acousticModels = duration F0 midF0 rightF0");
                  configOut.println();
                  configOut.println(voiceHeader + ".duration.model = cart");
                  configOut.println(voiceHeader + ".duration.data = MARY_BASE/lib/voices/" + voicename + "/" + getProp(DURTREE));
                  configOut.println(voiceHeader + ".duration.attribute = d");
                  configOut.println();
                  configOut.println(voiceHeader + ".F0.model = cart");
                  configOut.println(voiceHeader + ".F0.data = MARY_BASE/lib/voices/" + voicename + "/" + getProp(F0LEFTTREE));
                  configOut.println(voiceHeader + ".F0.attribute = f0");
                  configOut.println(voiceHeader + ".F0.attribute.format = (0,%.0f)");
                  configOut.println(voiceHeader + ".F0.predictFrom = firstVowels");
                  configOut.println(voiceHeader + ".F0.applyTo = firstVoicedSegments");
                  configOut.println();
                  configOut.println(voiceHeader + ".midF0.model = cart");
                  configOut.println(voiceHeader + ".midF0.data = MARY_BASE/lib/voices/" + voicename + "/" + getProp(F0MIDTREE));
                  configOut.println(voiceHeader + ".midF0.attribute = f0");
                  configOut.println(voiceHeader + ".midF0.attribute.format = (50,%.0f)");
                  configOut.println(voiceHeader + ".midF0.predictFrom = firstVowels");
                  configOut.println(voiceHeader + ".midF0.applyTo = firstVowels");
                  configOut.println();
                  configOut.println(voiceHeader + ".rightF0.model = cart");
                  configOut.println(voiceHeader + ".rightF0.data = MARY_BASE/lib/voices/" + voicename+ "/" + getProp(F0RIGHTTREE));
                  configOut.println(voiceHeader + ".rightF0.attribute = f0");
                  configOut.println(voiceHeader + ".rightF0.attribute.format = (100,%.0f)");
                  configOut.println(voiceHeader + ".rightF0.predictFrom = firstVowels");
                  configOut.println(voiceHeader + ".rightF0.applyTo = lastVoicedSegments");
              }
              
        } catch (IOException e) {
            IOException myIOE = new IOException("Problem writing config file:");
            myIOE.initCause(e);
            throw myIOE;
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
