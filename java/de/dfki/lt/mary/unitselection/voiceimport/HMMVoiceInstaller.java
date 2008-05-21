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

import de.dfki.lt.mary.htsengine.HTSTreeSet;

/**
 * Install a voice by copying the voice data to marybase/lib/voices/voicename/
 * and creating a config file marybase/conf/locale-voicename.config
 * 
 * @author Anna Hunecke, Marcela Charfuelan (modifications for creating hmm voice config file)
 *
 */
public class HMMVoiceInstaller extends VoiceImportComponent{
    
    private DatabaseLayout db;
    private String name = "HMMVoiceInstaller";
       
    /** Tree files and TreeSet object */
    public final String treeDurFile = name+".Ftd";
    public final String treeLf0File = name+".Ftf";
    public final String treeMcpFile = name+".Ftm";
    public final String treeStrFile = name+".Fts";
    public final String treeMagFile = name+".Fta";
        
    /** HMM pdf model files and ModelSet object */
    public final String pdfDurFile = name+".Fmd";
    public final String pdfLf0File = name+".Fmf";
    public final String pdfMcpFile = name+".Fmm";
    public final String pdfStrFile = name+".Fms";
    public final String pdfMagFile = name+".Fma";
    
    /** GV pdf files*/
    /** Global variance file, it contains one global mean vector and one global diagonal covariance vector */
    public final String useGV        = name+".useGV";
    public final String pdfLf0GVFile = name+".Fgvf"; 
    public final String pdfMcpGVFile = name+".Fgvm";  
    public final String pdfStrGVFile = name+".Fgvs";  
    public final String pdfMagGVFile = name+".Fgva";
        
    /** Variables for mixed excitation */
    public final String useMixExc      = name+".useMixExc";
    public final String mixFiltersFile = name+".Fif";
    public final String numFilters     = name+".in";
    public final String orderFilters   = name+".io";
    
    /** Feature list file and Vector which will contain the loaded features from this file */
    public final String featureListFile = name+".FeaList";
       
    /** Example context feature file in HTSCONTEXT_EN format */
    public final String labFile = name+".Flab";
    
    public final String createZipFile = name+".createZipFile";
    public final String zipCommand = name+".zipCommand";

    public String getName(){
        return name;
    }

    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public SortedMap<String,String> getDefaultProps(DatabaseLayout db){
        this.db = db;
       if (props == null){
           props = new TreeMap<String,String>();
           
           String rootdir = db.getProp(db.ROOTDIR);
           
           props.put(treeDurFile, "voices/qst001/ver1/tree-dur.inf"); 
           props.put(treeLf0File, "voices/qst001/ver1/tree-lf0.inf");
           props.put(treeMcpFile, "voices/qst001/ver1/tree-mgc.inf");
           props.put(treeStrFile, "voices/qst001/ver1/tree-str.inf");
           props.put(treeMagFile, "voices/qst001/ver1/tree-mag.inf");
           props.put(pdfDurFile, "voices/qst001/ver1/dur.pdf"); 
           props.put(pdfLf0File, "voices/qst001/ver1/lf0.pdf"); 
           props.put(pdfMcpFile, "voices/qst001/ver1/mgc.pdf"); 
           props.put(pdfStrFile, "voices/qst001/ver1/str.pdf");
           props.put(pdfMagFile, "voices/qst001/ver1/mag.pdf");
           props.put(useGV, "true");
           props.put(pdfLf0GVFile, "data/gv/gv-lf0-littend.pdf"); 
           props.put(pdfMcpGVFile, "data/gv/gv-mgc-littend.pdf"); 
           props.put(pdfStrGVFile, "data/gv/gv-str-littend.pdf");
           props.put(pdfMagGVFile, "data/gv/gv-mag-littend.pdf");
           props.put(useMixExc, "true");
           props.put(mixFiltersFile, "data/filters/mix_excitation_filters.txt"); 
           props.put(numFilters, "5");
           props.put(orderFilters, "48");
           props.put(featureListFile, "data/feature_list_en.pl");
           props.put(labFile, "data/labels/gen/gen_EM001_ARCTIC_0001.lab");
           props.put(createZipFile, "false");
           props.put(zipCommand, "/usr/bin/zip");
           
       }
       return props;
       }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        
        props2Help.put(treeDurFile, "durations tree file"); 
        props2Help.put(treeLf0File, "log F0 tree file");
        props2Help.put(treeMcpFile, "Mel-cepstral (mcp or Mel-generalized cepstral mgc, HTS Version 2.0.1 used mgc) tree file");
        props2Help.put(treeStrFile, "Bandpass voicing strengths tree file (optional: used for mixed excitation)");
        props2Help.put(treeMagFile, "Fourier Magnitudes tree (optional: used for mixed excitation)");
        props2Help.put(pdfDurFile, "Duration means and variances PDF file"); 
        props2Help.put(pdfLf0File, "Log F0 means and variances PDF file"); 
        props2Help.put(pdfMcpFile, "Mel-cepstral (or Mel-generalized cepstral mgc) means and variances PDF file"); 
        props2Help.put(pdfStrFile, "Bandpass voicing strengths means and variances PDF file (optional: used for mixed excitation)");
        props2Help.put(pdfMagFile, "Fourier Magnitudes means and variances PDF file (optional: used for mixed excitation)");
        props2Help.put(useGV, "Use global variance in parameter generation (true/false)");
        props2Help.put(pdfLf0GVFile, "Global variance for Log F0, mean and (diagonal) variance PDF file"); 
        props2Help.put(pdfMcpGVFile, "Global variance for Mel-cepstral (or Mel-generalized cepstral mgc) mean and (diagonal) variance PDF file"); 
        props2Help.put(pdfStrGVFile, "Global variance for Bandpass voicing strengths mean and (diagonal) variance PDF file (optional: used for mixed excitation)");
        props2Help.put(pdfMagGVFile, "Global variance for Fourier Magnitudes mean and (diagonal) variance PDF file (optional: used for mixed excitation)");
        props2Help.put(useMixExc, "Use mixed excitation in speech generation (true/false)");
        props2Help.put(mixFiltersFile, "Filter taps of bandpass filters for mixed excitation (optional: used for mixed excitation)"); 
        props2Help.put(numFilters, "Number of filters in bandpass bank, default 5 filters (optional: used for mixed excitation)");
        props2Help.put(orderFilters, "Number of taps in bandpass filters, default 48 taps (optional: used for mixed excitation)");
        props2Help.put(featureListFile, "Requested features for the fullcontext names and tree questions");
        props2Help.put(labFile, "File for testing the HMMSynthesiser, example of a file in HTSCONTEXT format. If the file is not provided or does not exist a file from data/labels/gen/ will be used.");
        props2Help.put(createZipFile, "Create zip file for Mary voices installation (used by Mary voices administrator only).");
        props2Help.put(zipCommand, "zip command to create a voice.zip file for voice installation.");
        
    }

    
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        System.out.println("Installing hmm voice: ");
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
        
        System.out.println(" newVoiceDir = " +  newVoiceDir);
        File newVoiceDirFile = new File(newVoiceDir);
        if (!newVoiceDirFile.exists()) newVoiceDirFile.mkdir();
        
        /* copy the files */
        System.out.println("Copying files ... ");
        try{
            File in, out;
            in = new File(getProp(treeDurFile));
            out = new File(newVoiceDir + getFileName(getProp(treeDurFile)));
            copy(in,out);   
            in = new File(getProp(treeLf0File));
            out = new File(newVoiceDir + getFileName(getProp(treeLf0File)));
            copy(in,out);   
            in = new File(getProp(treeMcpFile));
            out = new File(newVoiceDir + getFileName(getProp(treeMcpFile)));
            copy(in,out);
            
            /* optional file for mixed excitation */
            in = new File(getProp(treeStrFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(treeStrFile)));
              copy(in,out);    
            }
            /* optional file for mixed excitation */
            in = new File(getProp(treeMagFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(treeMagFile)));
              copy(in,out);   
            }
            
            in = new File(getProp(pdfDurFile));
            out = new File(newVoiceDir + getFileName(getProp(pdfDurFile)));
            copy(in,out);   
            in = new File(getProp(pdfLf0File));
            out = new File(newVoiceDir + getFileName(getProp(pdfLf0File)));
            copy(in,out);   
            in = new File(getProp(pdfMcpFile));
            out = new File(newVoiceDir + getFileName(getProp(pdfMcpFile)));
            copy(in,out);   
            
            /* optional file for mixed excitation */
            in = new File(getProp(pdfStrFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfStrFile)));
              copy(in,out);
            }
            /* optional file for mixed excitation */
            in = new File(getProp(pdfMagFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfMagFile)));
              copy(in,out);   
            }
            
            /* global variance files */
            in = new File(getProp(pdfMcpGVFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfMcpGVFile)));
              copy(in,out);   
            }
            in = new File(getProp(pdfLf0GVFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfLf0GVFile)));
              copy(in,out);   
            }
            in = new File(getProp(pdfStrGVFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfStrGVFile)));
              copy(in,out);   
            }
            in = new File(getProp(pdfMagGVFile));
            if(in.exists()) {
              out = new File(newVoiceDir + getFileName(getProp(pdfMagGVFile)));
              copy(in,out);   
            }
            
            in = new File(getProp(mixFiltersFile));
            out = new File(newVoiceDir + getFileName(getProp(mixFiltersFile)));
            copy(in,out);
            in = new File(getProp(featureListFile));
            out = new File(newVoiceDir + getFileName(getProp(featureListFile)));
            copy(in,out);
            
            
            in = new File(getProp(labFile));
            if(in.exists()){
              out = new File(newVoiceDir + getFileName(getProp(labFile)));
              copy(in,out); 
            } else {
              /* copy one example of hts context features file, it can be one of the 
               * files used for testing in data/labels/gen/*.lab*/
              File dirGenLab  = new File("data/labels/gen");
              if( dirGenLab.exists() && dirGenLab.list().length > 0 ){ 
                String[] labFiles = dirGenLab.list();
                in = new File("data/labels/gen/"+labFiles[0]);
                out = new File(newVoiceDir + getFileName(labFiles[0]));
                copy(in,out);
                props.put(labFile, "data/labels/gen/"+labFiles[0]);
              } else
                System.out.println("Problem copying one example of HTS context features, the directory data/labels/gen/ is empty or directory does not exist.");
            }   
               
        }catch (IOException ioe){
            return false;
        }
        
        /* create the config file */
        
        String cutLocale = db.getProp(db.LOCALE).toLowerCase();
        String longLocale;
        if (cutLocale.equals("en_us") || cutLocale.equals("en")){ 
            cutLocale = "en_US";
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
        System.out.println("\nCreating config file: " + configFileName);
        createConfigFile(configFileName, newVoiceDir, cutLocale, longLocale);
        System.out.println("... done! ");        
        System.out.println("To run the voice, restart your Mary server");
        
        /* create a zip file for installation */
        if( getProp(createZipFile).contentEquals("true") ) {
          System.out.println("\nCreating voice installation file: " + db.getProp(db.MARYBASE)+longLocale 
                               + "-"+db.getProp(db.VOICENAME).toLowerCase() + ".zip\n");  
          String maryBase = db.getProp(db.MARYBASE);
          if(maryBase.contains(" ") ){  
            int i = maryBase.indexOf(" ");
            maryBase = maryBase.substring(0, i) + "\\ " + maryBase.substring(i+1);
          }
          String installZipFile = longLocale
                               + "-"+db.getProp(db.VOICENAME).toLowerCase()
                               + ".zip";
          configFileName = "conf"+fileSeparator
                         + longLocale
                         + "-"+db.getProp(db.VOICENAME).toLowerCase()
                         + ".config";
          String cmdLine = "cd "+ maryBase + "\n" + getProp(zipCommand) + " " 
                         + installZipFile + " " 
                         + configFileName + " "
                         + "lib/voices/" + db.getProp(db.VOICENAME).toLowerCase() + fileSeparator + "*";  
          System.out.println("CommandLine:" + cmdLine)
;          launchBatchProc(cmdLine, "zip", filedir);
          System.out.println();
        }
        
        return true;
        }
 
    private void copy(File source, File dest)throws IOException{
        try { 
            System.out.println("copying: " + source + "\n    --> " + dest);
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
            File in;
            String voicename = db.getProp(db.VOICENAME).toLowerCase();
            //print the header
            configOut.println("#Auto-generated config file for voice "+voicename+"\n");
            //print name and version info
             configOut.println("name = " + longLocale + "-" + voicename);
             String version = "3.5.0"; // TODO: turn this into a config setting
             configOut.println(longLocale + "-voice.version = "+version+"\n");
             configOut.println("voice.version = "+version+"\n");
             //print providing info
             configOut.println("# Declare \"group names\" as component that other components can require.\n"+
                     	"# These correspond to abstract \"groups\" of which this component is an instance.\n"+
                     	"provides = \\\n         "+longLocale+"-voice \\\n" + 
                        "         hmm-voice\n");
             configOut.println("# List the dependencies, as a whitespace-separated list.\n"+
                     "# For each required component, an optional minimum version and an optional\n"+
                     "# download url can be given.\n"+
                     "# We can require a component by name or by an abstract \"group name\"\n"+ 
                     "# as listed under the \"provides\" element.\n"+
             		 "requires = \\\n   "+longLocale+" \\\n   marybase \\");
             configOut.println("   " + longLocale + "-targetfeatures \\");  
             configOut.println("   hmm \n\n");
             configOut.println("requires.marybase.version = 3.1.0\n"+
                     "requires.marybase.download = http://mary.dfki.de/download/mary-install-3.x.x.jar\n" +
             		 "requires."+longLocale+".version = 3.1.0\n"+
             		 "requires."+longLocale+".download = http://mary.dfki.de/download/mary-install-3.x.x.jar\n"+
                     "requires.hmm.version = 3.5.0\n");
             
             //now follow the module settings
              configOut.println("####################################################################\n"+
                      "####################### Module settings  ###########################\n"+
                      "####################################################################\n"+
                      "# For keys ending in \".list\", values will be appended across config files,\n"+
                      "# so that .list keys can occur in several config files.\n"+
                      "# For all other keys, values will be copied to the global config, so\n"+
              		  "# keys should be unique across config files.\n");              
              configOut.println("hmm.voices.list = \\\n   " + voicename + "\n");
              
              
              String voiceHeader = "voice."+voicename;
              
              //wants-to-be-default value
              configOut.println("# If this setting is not present, a default value of 0 is assumed.\n"+
                      voiceHeader+".wants.to.be.default = 0\n");
      
              //properties of the voice
              configOut.println("# Set your voice specifications\n"+
                      voiceHeader+".gender = "+db.getProp(db.GENDER).toLowerCase()+"\n"+
                      voiceHeader+".locale = "+ cutLocale +"\n"+
                      voiceHeader+".domain = "+db.getProp(db.DOMAIN).toLowerCase()+"\n"+
                      voiceHeader+".samplingRate = "+db.getProp(db.SAMPLINGRATE)+"\n");
              
              //language specific settings 
              if (!cutLocale.equals("en_US")||cutLocale.equals("de")){
                  configOut.println("Unsupported locale "+db.getProp(db.LOCALE));
              }
              if (cutLocale.equals("en_US")){
                  configOut.println("# Only set the lexicon for English\n"+
                          voiceHeader+".lexiconClass = com.sun.speech.freetts.en.us.CMULexicon\n"+
                          voiceHeader+".lexicon = cmudict04\n\n"+
                          "# Phoneme conversion for English voices \n"+
                          voiceHeader+".sampamapfile = MARY_BASE/lib/modules/en/synthesis/sampa2mrpa_en.map\n\n");                                            
              } else {
                  //cutLocale.equals("de")
                  configOut.println("# Sampa mapping for German voices \n"+
                          voiceHeader+".sampamap = \\\n"+
                          "=6->6 \\\n"+"=n->n \\\n"+"=m->m \\\n"+
                          "=N->N \\\n"+"=l->l \\\n"+"i->i: \\\n"+
                          "e->e: \\\n"+"u->u: \\\n"+"o->o: \n\n");                    
              }
                     
              //voice data
              configOut.println("# HMM Voice-specific files\n# Information about trees\n"+
                      voiceHeader+".Ftd = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeDurFile))+"\n"+
                      voiceHeader+".Ftf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeLf0File))+"\n"+
                      voiceHeader+".Ftm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeMcpFile)));
              if( new File(getProp(treeStrFile)).exists())
                configOut.println(voiceHeader+".Fts = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeStrFile))+"\n");
              if( new File(getProp(treeMagFile)).exists())
                configOut.println(voiceHeader+".Fta = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(treeMagFile))+"\n");
              configOut.println("\n# Information about means and variances PDFs \n"+
                      voiceHeader+".Fmd = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfDurFile))+"\n"+
                      voiceHeader+".Fmf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfLf0File))+"\n"+
                      voiceHeader+".Fmm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMcpFile)));
              if( new File(getProp(pdfStrFile)).exists())
               configOut.println(voiceHeader+".Fms = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfStrFile))+"\n");
              if( new File(getProp(pdfMagFile)).exists())
               configOut.println(voiceHeader+".Fma = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMagFile))+"\n");
              
              configOut.println("\n# Information about Global Mean and Variance PDFs");
              configOut.println(voiceHeader+".useGV = "+ getProp(useGV));
              if( new File(getProp(pdfLf0GVFile)).exists())
                  configOut.println(voiceHeader+".Fgvf = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfLf0GVFile)));
              if( new File(getProp(pdfMcpGVFile)).exists())
                  configOut.println(voiceHeader+".Fgvm = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMcpGVFile)));
              if( new File(getProp(pdfStrGVFile)).exists())
                  configOut.println(voiceHeader+".Fgvs = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfStrGVFile)));
              if( new File(getProp(pdfMagGVFile)).exists())
                  configOut.println(voiceHeader+".Fgva = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(pdfMagGVFile)));
              configOut.println();
              
              configOut.println("\n# File for testing the HMMSynthesiser, example of a file in HTSCONTEXT format\n" +
                      voiceHeader+".FLab = MARY_BASE/lib/voices/"+voicename+"/"+getFileName(getProp(labFile))+"\n\n"+
                      "# Requested features for the fullcontext names and tree questions \n" +
                      voiceHeader+".FeaList = MARY_BASE/lib/voices/"+voicename+"/"+getFileName(getProp(featureListFile))+"\n\n");
              
              configOut.println("\n# Information about Mixed Excitation");
              configOut.println(voiceHeader+".useMixExc = "+ getProp(useMixExc));
              configOut.println();
              if( new File(getProp(treeStrFile)).exists()) {
                configOut.println("# Filter taps of bandpass filters for mixed excitation \n" +
                                "# File format: for example if we have 5 filters each with 48 taps \n" +
                                "# then the taps are in a vector \n" +
                                "# tap[1][1] \n" +
                                "# ... \n" +
                                "# tap[1][48] \n" +
                                "# tap[2][1] \n" +
                                "# ... \n" +
                                "# tap[2][48] \n" +
                                "# ... \n" +
                                "# tap[5][1] \n" +
                                "# ... \n" +
                                "# tap[5][48] \n" +
                                voiceHeader+".Fif = MARY_BASE/lib/voices/"+voicename+"/" + getFileName(getProp(mixFiltersFile))+"\n"+
                                "# Number of filters in bandpass bank, default 5 filters \n" +
                                voiceHeader+".in = " + getProp(numFilters)+"\n" +
                                "# Number of taps in bandpass filters, default 48 taps \n" +
                                voiceHeader+".io = " + getProp(orderFilters) );
              }
                     
              
        } catch (Exception e){
            throw new Error("Error writing config file : "
                    +e.getMessage());
        }
    }
    
    
    /**
     * Given a file name with path it return the file name
     * @param fileNameWithPath
     * @return
     */
    private String getFileName(String fileNameWithPath) {
       String str;
       int i;
       
       i = fileNameWithPath.lastIndexOf("/");
       str = fileNameWithPath.substring(i+1); 
       
       return str;
        
    }
    
    /**
     * A general process launcher for the various tasks but using an intermediate batch file
     * (copied from ESTCaller.java)
     * @param cmdLine the command line to be launched.
     * @param task a task tag for error messages, such as "Pitchmarks" or "LPC".
     * @param the basename of the file currently processed, for error messages.
     */
    private void launchBatchProc( String cmdLine, String task, String baseName ) {
        
        Process proc = null;
        Process proctmp = null;
        BufferedReader procStdout = null;
        String line = null;
        String filedir = db.getProp(db.ROOTDIR);
        String tmpFile = filedir+"tmp.bat";

        // String[] cmd = null; // Java 5.0 compliant code
        
        try {
            FileWriter tmp = new FileWriter(tmpFile);
            tmp.write(cmdLine);
            tmp.close();
            
            /* make it executable... */
            proctmp = Runtime.getRuntime().exec( "chmod +x "+tmpFile );
            
            /* Java 5.0 compliant code below. */
            /* Hook the command line to the process builder: */
            /* cmd = cmdLine.split( " " );
            pb.command( cmd ); /*
            /* Launch the process: */
            /*proc = pb.start(); */
            
            /* Java 1.0 equivalent: */
            proc = Runtime.getRuntime().exec( tmpFile );
            
            /* Collect stdout and send it to System.out: */
            procStdout = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            while( true ) {
                line = procStdout.readLine();
                if ( line == null ) break;
                System.out.println( line );
            }
            /* Wait and check the exit value */
            proc.waitFor();
            if ( proc.exitValue() != 0 ) {
                throw new RuntimeException( task + " computation failed on file [" + baseName + "]!\n"
                        + "Command line was: [" + cmdLine + "]." );
            }
            
            
        }
        catch ( IOException e ) {
            throw new RuntimeException( task + " computation provoked an IOException on file [" + baseName + "].", e );
        }
        catch ( InterruptedException e ) {
            throw new RuntimeException( task + " computation interrupted on file [" + baseName + "].", e );
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