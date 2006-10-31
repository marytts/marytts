/**
 * Copyright 2006 DFKI GmbH.
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

import java.io.*;
import java.util.*;

import de.dfki.lt.mary.client.MaryClient;
import de.dfki.lt.mary.util.dom.MaryDomUtils;
import de.dfki.lt.mary.util.dom.NameNodeFilter;
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;
import de.dfki.lt.mary.MaryXML;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * Preparate the directory of the voice for sphinx labelling
 * @author Anna Hunecke
 */
public class SphinxLabelingPreparator implements VoiceImportComponent {
    
    private DatabaseLayout dbLayout;
    private BasenameList baseNames;
    private MaryClient mary;
    private String sphinxtraindir;
    private String estdir;
    private File rootDir;
    private File st;
    private String voicename;
    private String outputDir;
    private String[] filenames;
    
    /**
     * Create new LabelingPreparator
     * 
     * @param dbLayout the database layout
     * @param baseNames the list of file base names
     */
    public SphinxLabelingPreparator(DatabaseLayout dbLayout,
            				BasenameList baseNames){
        this.dbLayout = dbLayout;
        this.baseNames = baseNames;
    }
    
    /**
     * Do the computations required by this component.
     * TODO: check if this works for German, too
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        
        System.out.println("Preparing voice database for labelling");
        /* get the directories of sphinxtrain and edinburgh speech tools */
        sphinxtraindir = System.getProperty("SPHINXTRAINDIR");
        if ( sphinxtraindir == null ) {
            System.out.println( "Warning: The environment variable SPHINXTRAINDIR was not found on your system." );
            System.out.println( "         Defaulting SPHINXTRAINDIR to [ /project/mary/anna/sphinx/SphinxTrain/ ]." );
            sphinxtraindir = "/project/mary/anna/sphinx/SphinxTrain/";
        }
        estdir = System.getProperty("ESTDIR");
        if ( estdir == null ) {
            System.out.println( "Warning: The environment variable ESTDIR was not found on your system." );
            System.out.println( "         Defaulting ESTDIR to [ /project/mary/Festival/speech_tools/ ]." );
            estdir = "/project/mary/Festival/speech_tools/";
        }
        
        //get the root dir and the voicename
        rootDir = new File(dbLayout.rootDirName());
        voicename = rootDir.getName();
        //make new directories st and lab
        st = new File(rootDir.getAbsolutePath()+"/st");
        // get the output directory of files used by sphinxtrain 
        outputDir = st.getAbsolutePath()+"/etc";
        //get the filenames
        filenames = baseNames.getListAsArray();
        
        /* setup the Sphinx directory */
        System.out.println("Setting up sphinx directory ...");
        setup();
        System.out.println(" ... done.");
        
        /* dump the filenames */
        System.out.println("Dumping the filenames ...");
        dumpFilenames();
        System.out.println(" ... done.");
        
        /* read in the transcriptions, 
         * build up dictionary and phone set; 
         * dump the transcriptions */
        //dictionary 
        Map dictionary = new HashMap();
        //set of phones
        Set phones = new HashSet();
        //fill dictionary and phone set, dump transcriptions   
        System.out.println("Building dictionary, phone set and dumping transcriptions ...");
        buildDictAndDumpTrans(dictionary,phones);
        System.out.println(" ... done.");
        
        /* dump phone file */
        System.out.println("Dumping phone set ...");
        dumpPhoneFile(phones);
        System.out.println(" ... done.");
        
        /* dump dictionary file */
        System.out.println("Dumping dictionary ...");
        dumpDictFile(dictionary);
        System.out.println(" ... done.");
        
        /* dump filler dictionary file */
        System.out.println("Dumping filler dictionary ...");
        dumpFillerDictFile();
        System.out.println(" ... done.");
        
        /* Convert MFCCs for Sphinxtrain */
        System.out.println("Converting MFCCs ...");
        convertMFCCs();
        System.out.println(" ... done.");
        
        /* rewrite the config file */
        System.out.println("Rewriting config file ...");
        rewriteConfigFile();
        System.out.println(" ... done.");
        //exit
        System.out.println("All done!");
        return true;
    }
    
    
   /**
    * Setup the sphinx directory
    * @throws IOException, InterruptedException
    */
    private void setup() throws IOException,InterruptedException{
        
        st.mkdir();
        File lab = new File(st.getAbsolutePath()+"/lab");
        //call setup of sphinxtrain in this directory
        Runtime rtime = Runtime.getRuntime();
        //get a shell
        Process process = rtime.exec("/bin/bash");
        //get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(process.getOutputStream()));
        //go to st directory and call sphinx train setup script
        pw.print("( cd "+st.getAbsolutePath()
                +"; "+sphinxtraindir+"/scripts_pl/setup_SphinxTrain.pl -task "+voicename
                +" )");
        pw.flush();
        //shut down
        pw.print("exit\n");
        pw.flush();
        pw.close();
        process.waitFor();
        process.exitValue();
        }
    
    /**
     * Dump the filenames
     * @throws IOException
     */
    private void dumpFilenames()throws IOException{
        //open filename file
        PrintWriter baseNameOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+".fileids")));
        //dump the filenames        
        for (int i=0;i<filenames.length;i++){
         baseNameOut.print("\n"+filenames[i]);   
        }
        baseNameOut.flush();
        baseNameOut.close();
        }
    
    /**
     * Build a dictionary and dump the transcription file
     * @param dictionary the dictionary to be filled
     * @param phones the phone set to be filled
     * @throws Exception
     */
    private void buildDictAndDumpTrans(Map dictionary, Set phones) throws Exception {
        //build a new MaryClient
        mary = getMaryClient();
        String inputFormat = "TEXT"; 
        String outputFormat = "ACOUSTPARAMS";
        //open transcription out
        PrintWriter transcriptionOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+".transcription")));
        //open etc/txt.done.data (transcription in)
        BufferedReader transcriptionIn = new BufferedReader(
                new FileReader(new File(dbLayout.baseTxtFileName())));


        //loop through the transcriptions in txt.done.data;
        //for each transcription, get a segment representation with MARY client
        //then go through segment representation 
        //and collect words and phones 
        //for dictionary and phone set
        String line = transcriptionIn.readLine();
        while (line != null){
            StringTokenizer tok = new StringTokenizer(line);
            //discard first token
            tok.nextToken();
            //next token is filename, check if it is in the baseNames
            String nextFilename = tok.nextToken();
            if (baseNames.contains(nextFilename)){
                //next token is transcription
                String nextTrans = tok.nextToken();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                //process and dump
                mary.process(nextTrans, inputFormat, outputFormat, null, null, os);
                //read into mary data object                
                MaryData maryData = new MaryData(MaryDataType.get(outputFormat));
                maryData.readFrom(new ByteArrayInputStream(os.toByteArray()));
                Document doc = maryData.getDocument();
   
                //go through the tokens
                //TODO: find cause for ClassCastException
                NodeIterator tokensIt = ((DocumentTraversal)doc).createNodeIterator(doc, 
                        NodeFilter.SHOW_ELEMENT,
                	    new NameNodeFilter(MaryXML.TOKEN), 
                	    false);
                Element token = null;
                //collect the words
                StringBuffer transBuff = new StringBuffer();
                while ((token = (Element) tokensIt.nextNode()) != null) {
                    //get the word 
                    String word = MaryDomUtils.tokenText(token).toUpperCase();
                    //if the word is a punctuation, ignore it
                    if (!Character.isLetter(word.charAt(0))){
                        continue;
                    }
                    //append word to transcription string
                    transBuff.append(" "+word);
            
                    if (!dictionary.containsKey(word)){
                        //store word and pronounciation in dictionary:
                        //build new pronounciation List
                        List phoneList = new ArrayList();
                        //add
                        dictionary.put(word,phoneList);
            
                        //go through the phones
                        NodeList phoneNodes = token.getElementsByTagName(MaryXML.PHONE);
 
                        for (int j=0; j<phoneNodes.getLength(); j++) {
                            //get the next phone node
                            Element phoneNode = (Element) phoneNodes.item(j);
                            //get the phone
                            String phone = phoneNode.getAttribute("p");
                            //append the phone to the other phones 
                            phoneList.add(phone);
                            //add the phone to the phone set if not already there
                            phones.add(phone);
                        }
                    }//end of if not word in dictionary
                } //end of loop through tokens
    
                //print transcription to transcription out
                transcriptionOut.println("<s>"+transBuff.toString()+" </s>"
                        +" ("+nextFilename+")");
            } //end of if filename is in basename
            line = transcriptionIn.readLine();
        } //end of loop through lines of txt.done.data

        //close the streams
        transcriptionIn.close();
        transcriptionOut.flush();
        transcriptionOut.close();
    }
    

    /**
     * Get a new MARY client
     * 
     * @return the MARY client
     */
    private MaryClient getMaryClient() throws IOException
    {
        if (mary == null) {
            if (System.getProperty("server.host") == null) {
                System.setProperty("server.host", "attila");
            }
            if (System.getProperty("server.port") == null) {
                System.setProperty("server.port", "59125");
            }
            mary = new MaryClient();
        }
        return mary;
    }
    
    /**
     * Dump the phone set
     * @param phones the phone set
     * @throws IOException
     */
    private void dumpPhoneFile(Set phones)throws IOException{
        PrintWriter phoneOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+".phone")));
        //dump phone set
        for (Iterator it = phones.iterator();it.hasNext();){
            phoneOut.println((String) it.next());
        }
        phoneOut.flush();
        phoneOut.close();
        }
    
    /** 
     * Dump the dictionary
     * @param dictionary the dictionary
     * @throws IOException
     */
    private void dumpDictFile(Map dictionary)throws IOException {
        PrintWriter dictOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+".dic")));
        //dump dictionary
        Set words = dictionary.keySet();
        for (Iterator it = words.iterator();it.hasNext();){
            String nextWord = (String) it.next();
            dictOut.print(nextWord);
            List nextPhones = (List) dictionary.get(nextWord);
            for (int i=0;i<nextPhones.size();i++){
                dictOut.print(" "+(String)nextPhones.get(i));
            }
            if (it.hasNext()){
                dictOut.print("\n");
            }
        }
        dictOut.flush();
        dictOut.close();
        }
    
    /**
     * Dump the filler dictionary
     * @throws IOException
     */
    private void dumpFillerDictFile() throws IOException {
        PrintWriter fillerDictOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+".filler")));
        //print silence symbol
        fillerDictOut.print("<s> SIL\n</s> SIL\n<sil> SIL");
        fillerDictOut.flush();
        fillerDictOut.close();
        }
    
    /**
     * Convert the MFCCs to Sphinx format
     * @throws Exception
     */
    private void convertMFCCs() throws Exception {
        String wavDir = dbLayout.wavDirName();
        //loop through wav files
        Runtime rtime = Runtime.getRuntime();
        for (int i=0;i<filenames.length;i++){
            String wavFileName = filenames[i];            
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            //go to voicedir and call ch_wave
            pw.print("( cd "+rootDir.getCanonicalPath()
                +"; "+estdir+"/bin/ch_wave -otype nist -o st/"+wavFileName
                +" "+wavDir+wavFileName
                +" )");
            //shut down
            pw.print("exit\n");
            pw.flush();
            pw.close();
            process.waitFor();
            process.exitValue();
        }
       
        
        //correct st/bin/make_feats.pl
        File make_feats = new File(rootDir+"/st/bin/make_feats.pl");
        BufferedReader bufIn = new BufferedReader(
                new FileReader(make_feats));
        StringBuffer stBuf = new StringBuffer();
        String line = bufIn.readLine();
        while (line != null){
            if (line.startsWith("system(\"bin/wave2feat")){
                line = "system(\"bin/wave2feat -verbose yes -c \\\"$ctl\\\" -nist yes \"-di wav -ei wav -do \\$CFG_FEATFILES_DIR\\\" \" . \"-eo \\\"$CFG_FEATFILE_EXTENSION\\\"\");";
            }
            stBuf.append(line+"\n");
            line = bufIn.readLine();
        }
        bufIn.close();
        PrintWriter printOut = new PrintWriter(
                new FileWriter(make_feats));
        printOut.print(stBuf.toString());
        printOut.flush();
        printOut.close();
        
        //call make_feats.pl
        rtime = Runtime.getRuntime();
        Process process = rtime.exec("/bin/bash");
        //get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(process.getOutputStream()));
        //go to voicedir and call ch_wave
        pw.print("(cd "+st.getAbsolutePath()
                +"; ./bin/make_feats -ctl etc/"+voicename+".fileids"
                +" )");
        //shut down
        pw.print("exit\n");
        pw.flush();
        pw.close();
        process.waitFor();
        process.exitValue();
        }
    
    /**
     * Rewrite the config file so that 
     * it matches the voice database
     * 
     * @throws Exception
     */
    private void rewriteConfigFile() throws Exception {
        //open the config file
        BufferedReader reader = new BufferedReader(
                new FileReader(outputDir+"/sphinx_train.cfg"));
        //StringBuffer to rewrite the file
        StringBuffer sb = new StringBuffer();
        String line;
        //go through lines of config file
        while ((line = reader.readLine()) != null){
            if (line.startsWith("$CFG_DB_NAME")){
                //overwrite db_name with voicename
                sb.append("$CFG_DB_NAME = \'"+voicename+"\';\n");
                continue;
            } 
            if (line.startsWith("$CFG_DICTIONARY")){
                //overwrite with dictionary file name
                sb.append("$CFG_DICTIONARY     = \"$CFG_BASE_DIR/etc/$CFG_DB_NAME.dic\";\n");
                continue;
            } 
            if (line.startsWith("$CFG_RAWPHONEFILE")){
                //overwrite with phone set file name
                sb.append("$CFG_RAWPHONEFILE   = \"$CFG_BASE_DIR/etc/$CFG_DB_NAME.phone\";\n");
                continue;
            }
            if (line.startsWith("$CFG_FILLERDICT")){
                //overwrite with filler dictionary file name
                sb.append("$CFG_FILLERDICT     = \"$CFG_BASE_DIR/etc/$CFG_DB_NAME.filler\";\n");
                continue;
            }
            if (line.startsWith("$CFG_LISTOFFILES")){
                //overwrite with basename list file name
                sb.append("$CFG_LISTOFFILES    = \"$CFG_BASE_DIR/etc/$CFG_DB_NAME.fileids\";\n");
                continue;
            }
            if (line.startsWith("$CFG_TRANSCRIPTFILE")){
                //overwrite with transcription file name
                sb.append("$CFG_TRANSCRIPTFILE = \"$CFG_BASE_DIR/etc/$CFG_DB_NAME.transcription\";\n");
                continue;
            }
            if (line.startsWith("$CFG_HMM_TYPE")){
                //set HMM_type to semi
                sb.append("$CFG_HMM_TYPE  = '.semi.'; # Sphinx II\n");
                continue;
            }
            //no special line, just append it as it is
            sb.append(line+"\n");
        }
        reader.close();
        //overwrite config file with contents of StringBuffer
        PrintWriter writer = new PrintWriter(
                new FileWriter(outputDir+"/sphinx_train.cfg"));
        writer.print(sb.toString());
        writer.flush();
        writer.close();
    }
   
    
}