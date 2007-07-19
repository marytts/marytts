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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * Preparate the directory of the voice for sphinx labelling
 * @author Anna Hunecke
 */
public class SphinxLabelingPreparator extends VoiceImportComponent {
    
    private DatabaseLayout db;
    private MaryClient mary;
    private File rootDir;
    private File st;
    private String voicename;
    private String outputDir;
    private String[] filenames;
    private int progress;
    private String locale;
    
    public final String STDIR = "SphinxLabelingPreparator.stDir";
    public final String SPHINXTRAINDIR = "SphinxLabelingPreparator.sphinxTrainDir";
    public final String ESTDIR = "SphinxLabelingPreparator.estDir";
    public final String TRANSCRIPTFILE = "SphinxLabelingPreparator.transcriptFile";
    public final String MARYSERVERHOST = "SphinxLabelingPreparator.maryServerHost";
    public final String MARYSERVERPORT = "SphinxLabelingPreparator.maryServerPort";
    
     public final String getName(){
        return "SphinxLabelingPreparator";
    }
    
   public SortedMap getDefaultProps(DatabaseLayout db){
       this.db = db;
       if (props == null){
           props = new TreeMap();
           String sphinxtraindir = System.getProperty("SPHINXTRAINDIR");
           if ( sphinxtraindir == null ) {
               sphinxtraindir = "/project/mary/anna/sphinx/SphinxTrain/";
           }
           props.put(SPHINXTRAINDIR,sphinxtraindir);
           String estdir = System.getProperty("ESTDIR");
           if ( estdir == null ) {
               estdir = "/project/mary/Festival/speech_tools/";
           }
           props.put(ESTDIR,estdir);
           props.put(STDIR,db.getProp(db.ROOTDIR)
           				+"st"
           				+System.getProperty("file.separator"));
           props.put(TRANSCRIPTFILE,db.getProp(db.ROOTDIR)
           				+"txt.done.data");
           props.put(MARYSERVERHOST,"localhost");
           props.put(MARYSERVERPORT,"59125");
       }
       return props;
   }
   
   protected void setupHelp(){
       props2Help = new TreeMap();
       props2Help.put(SPHINXTRAINDIR,"directory containing the local installation of SphinxTrain");
       props2Help.put(ESTDIR,"directory containing the local installation of the Edinburgh Speech Tools");
       props2Help.put(STDIR,"directory containing all files used for training and labeling. Will be created if it does not exist.");
       props2Help.put(TRANSCRIPTFILE,"file containing the transcripts in festvox format");
       props2Help.put(MARYSERVERHOST,"the host were the Mary server is running, default: \"localhost\"");
       props2Help.put(MARYSERVERPORT,"the port were the Mary server is listening, default: \"59125\"");
   }
    
    public void initialiseComp()
    {
        progress = 0;
        locale = db.getProp(db.LOCALE);
    }
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public boolean compute() throws Exception{
        progress = 0;
        System.out.println("Preparing voice database for labelling");
        
        //get the voicename        
        voicename = db.getProp(db.VOICENAME);
        //make new directories st and lab
        st = new File(getProp(STDIR));
        // get the output directory of files used by sphinxtrain 
        outputDir = st.getAbsolutePath()+"/etc";
        
        
        /* setup the Sphinx directory */
        System.out.println("Setting up sphinx directory ...");
        setup();
        System.out.println(" ... done.");
        progress = 1;
        
        
        
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
        progress = 50;
        
        /* dump the filenames */
        System.out.println("Dumping the filenames ...");
        dumpFilenames();
        System.out.println(" ... done.");
        progress++;
        
        /* dump phone file */
        System.out.println("Dumping phone set ...");
        dumpPhoneFile(phones);
        System.out.println(" ... done.");
        progress = 51;
        
        /* dump dictionary file */
        System.out.println("Dumping dictionary ...");
        dumpDictFile(dictionary);
        System.out.println(" ... done.");
        progress = 52;
        
        /* dump filler dictionary file */
        System.out.println("Dumping filler dictionary ...");
        dumpFillerDictFile();
        System.out.println(" ... done.");
        progress = 53;
        
        /* Convert MFCCs for Sphinxtrain */
        System.out.println("Converting MFCCs ...");
        convertMFCCs();
        System.out.println(" ... done.");
        progress = 99;
        
        /* rewrite the config file */
        System.out.println("Rewriting config file ...");
        rewriteConfigFile();
        System.out.println(" ... done.");
        //exit
        System.out.println("All done!");
        progress = 100;
        
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
                +"; "+getProp(SPHINXTRAINDIR)+"/scripts_pl/setup_SphinxTrain.pl -task "+voicename
                +"; exit )\n");
        pw.flush();
        //shut down
        pw.close();
        process.waitFor();
        process.exitValue();
        }
    
    /**
     * Dump the filenames
     * @throws IOException
     */
    private void dumpFilenames()throws IOException{
        //for training: open filename file
        PrintWriter baseNameOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+"_train.fileids")));
        //dump the filenames        
        for (int i=0;i<filenames.length;i++){
                baseNameOut.println(filenames[i]); 
        }
        baseNameOut.flush();
        baseNameOut.close();
        
        //for labeling: open filename file
        baseNameOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+".fileids")));
        //dump the filenames        
        for (int i=0;i<filenames.length;i++){
                baseNameOut.println(filenames[i]); 
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
        String inputFormat = "RAWMARYXML"; 
        String outputFormat = "ACOUSTPARAMS";
        
        //open etc/txt.done.data (transcription in)
        BufferedReader transIn = new BufferedReader(
                new InputStreamReader(new FileInputStream(getProp(TRANSCRIPTFILE)),"UTF-8"));
        
        //open transcription file used for training
        PrintWriter transTrainOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+"_train.transcription")));
        
        //open transcription file used for labeling
        PrintWriter transLabelOut = new PrintWriter(
                new FileOutputStream (new File(outputDir+"/"+voicename+".align")));
        //write beginning of labeling transcription file
        transLabelOut.println("*align_all*");

        //for the progress bar: calculate the progress of each transcription
        long nextPercentAfter = Math.round(1/(48.0/bnl.getLength()));
        int index = 1;
        
        //store the filenames
        ArrayList filenameList = new ArrayList();
        
        //loop through the transcriptions in txt.done.data;
        //for each transcription, get a segment representation with MARY client
        //then go through segment representation 
        //and collect words and phones 
        //for dictionary and phone set
        String line = transIn.readLine();
        boolean first = true;
        while (line != null){
            StringTokenizer tok = new StringTokenizer(line);
            //discard first token
            tok.nextToken();
            //next token is filename, 
            //put it in filename list
            String nextFilename = tok.nextToken();
            System.out.println(nextFilename);
            filenameList.add(nextFilename);
            
                //transcription is everything between " "
                String nextTrans = line.substring(line.indexOf("\"")+1,line.lastIndexOf("\""));
                nextTrans = getMaryXMLHeader(locale)
                		+ nextTrans + "</maryxml>";
                //System.out.println(nextTrans);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                //process and dump
                mary.process(nextTrans, inputFormat, outputFormat, null, null, os);
                //read into mary data object                
                MaryData maryData = new MaryData(MaryDataType.get(outputFormat));
                maryData.readFrom(new ByteArrayInputStream(os.toByteArray()));
                Document doc = maryData.getDocument();
                
                //go through the tokens
                NodeIterator tokensIt = ((DocumentTraversal)doc).createNodeIterator(doc, 
                        NodeFilter.SHOW_ELEMENT,
                	    new NameNodeFilter(MaryXML.TOKEN), 
                	    false);
                Element token = null;
                //collect the words
                int numTokens = 0;
                StringBuffer trainBuff = new StringBuffer();
                StringBuffer alignBuff = new StringBuffer();
                boolean sentenceBoundary = false;
                while ((token = (Element) tokensIt.nextNode()) != null) {
                    //get the word 
                    String word = MaryDomUtils.tokenText(token).toUpperCase();
                    String pos = token.getAttribute("pos");
                    if (word.equals(""))
                        continue;                    
                    if (pos.equals("$PUNCT") || pos.equals("$,")){
                    //if the word is a punctuation,
                        if (sentenceBoundary){                            
                            //if there was a punctuation before,
                            //there is an error in the transcript
                            Element tokenParent = (Element) token.getParentNode();
                            if (!tokenParent.getLocalName().equals("mtu")){                                  
                                StringBuffer xmlBuf = new StringBuffer();
                                getXMLAsString(doc,xmlBuf);
                                System.out.println(xmlBuf.toString());
                                System.out.println("Problem with token "+word
                                        +" in sentence "+nextFilename);
                                throw new IllegalArgumentException("Error in transcript of "
                                    +nextFilename+": multiple sentence boundary markers");
                            }
                        }                        
                        //else append sentence end marker
                        trainBuff.append(" </s>");
                        sentenceBoundary = true;
                        continue;
                    } else {
                        //skip all other non-words
                        if ((!Character.isLetter(word.charAt(0)))
                                    && word.length()<2)
                        continue;
                    }
                    //Convert ' to Q
                    word = word.replace('\'','Q');
                    //append word to transcription string
                    if (sentenceBoundary){
                        //there was a sentence boundary before
                        //append sentence start marker
                        trainBuff.append(" <s>");
                        sentenceBoundary = false;
                    }
                    
                        //go through the phones
                        NodeList phoneNodes = token.getElementsByTagName(MaryXML.PHONE);
 
                        for (int j=0; j<phoneNodes.getLength(); j++) {
                            //get the next phone node
                            Element phoneNode = (Element) phoneNodes.item(j);
                            //get the phone
                            String phone = phoneNode.getAttribute("p");
                            //convert the phone to Sphinx format
                            phone = convertPhone(phone);
                            if (!dictionary.containsKey(phone)){
                                //store word and pronounciation in dictionary:
                                //build new pronounciation List
                                List phoneList = new ArrayList();
                                phoneList.add(phone);
                                //add
                                dictionary.put(phone,phoneList);
                            }
                            //append the phone to the other phones 
                            //phoneList.add(phone);
                            //add the phone to the phone set if not already there
                            phones.add(phone);
                            trainBuff.append(" "+phone);
                            alignBuff.append(" "+phone);
                        }
                    //}//end of if not word in dictionary
                    numTokens++;
                } //end of loop through tokens
                //System.out.println("NumTokens: "+numTokens);
                //print transcription to transcription out
                transTrainOut.println("<s>"+trainBuff.toString()
                        +" ("+nextFilename+")");
                if (first){
                    transLabelOut.print(alignBuff.toString().trim());
                    //System.out.println(transBuff.toString());
                    first = false;
                } else {
                    transLabelOut.print("\n"+alignBuff.toString().trim());
                    //System.out.println(transBuff.toString());
                }
            line = transIn.readLine();
            //for the progress bar
            if (index == nextPercentAfter){
                //next percent is due
                progress++;
                index = 1;
            } else {
                index++;
            }
        } //end of loop through lines of txt.done.data

        //close the streams
        transIn.close();
        transTrainOut.flush();
        transTrainOut.close();
        transLabelOut.flush();
        transLabelOut.close();
        
        //store the filenames locally and in basenames
        int numFiles = filenameList.size();
        filenames = new String[numFiles];
        for (int i=0;i<numFiles;i++){
            filenames[i] = (String)filenameList.get(i);
        }
        bnl.clear();
        bnl.add(filenames);
    }
    
    /**
     * Convert the given xml-node and its subnodes to Strings
     * and collect them in the given Stringbuffer
     * 
     * @param motherNode the xml-node
     * @param ppText the Stringbuffer
     */
    private void getXMLAsString(Node motherNode,StringBuffer ppText){
        NodeList children = motherNode.getChildNodes();
        for (int i=0;i<children.getLength();i++){
            Node nextChild = children.item(i);
            String name = nextChild.getLocalName();
            if (name == null){
                continue;
            }          
           
            ppText.append("<"+name);
            if (nextChild instanceof Element){
                if (nextChild.hasAttributes()){
                    NamedNodeMap atts = nextChild.getAttributes();
                    for (int j=0;j<atts.getLength();j++){
                        String nextAtt = atts.item(j).getNodeName();
                        ppText.append(" "+nextAtt+"=\""
                            +((Element)nextChild).getAttribute(nextAtt)
                            +"\"");
                    }
                }
                
            }
            if (name.equals("boundary")){
                ppText.append("/>\n");
                continue;
            }
            ppText.append(">\n");
            if (name.equals("t")){
                ppText.append(MaryDomUtils.tokenText((Element)nextChild)
                        +"\n</t>\n");                
            } else {
                if (nextChild.hasChildNodes()){
                    getXMLAsString(nextChild,ppText);
                } 
                ppText.append("</"+name+">\n");
            }
        }
    }
    
    
    
    /**
     * Get a maryxml header with the given locale
     * @param locale the locale
     * @return the maryxml header
     */
    private String getMaryXMLHeader(String locale)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<maryxml version=\"0.4\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "xmlns=\"http://mary.dfki.de/2002/MaryXML\"\n" +
            "xml:lang=\"" + locale + "\">\n";
        
    }

    /**
     * Get a new MARY client
     * 
     * @return the MARY client
     */
    private MaryClient getMaryClient()throws IOException
    {
        if (mary == null) {
            try{
                mary = new MaryClient(getProp(MARYSERVERHOST), Integer.parseInt(getProp(MARYSERVERPORT)));        
            } catch (IOException e){
                throw new IOException("Could not connect to Maryserver at "
                        +getProp(MARYSERVERHOST)+" "+getProp(MARYSERVERPORT));
            }
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
        //add silence symbol
        phoneOut.println("SIL");
        phoneOut.flush();
        phoneOut.close();
        }
    
   /**
    * Convert the given phone to Sphinx-readable format
    * 
    * @param phone the phone
    * @return the converted phone
    */
    private String convertPhone(String phone){
        //since Sphinx is not case sensitive, 
        //convert everything to uppercase
        //and mark originally uppercase characters with *
        char[] phoneChars = phone.toCharArray();
        StringBuffer convertedPhone = new StringBuffer();
        for (int i=0;i<phoneChars.length;i++){
            char phoneChar = phoneChars[i];
            if (Character.isLetter(phoneChar)){
                if (Character.isLowerCase(phoneChar)){
                    //convert to uppercase
                    convertedPhone.append(Character.toUpperCase(phoneChar));
                } else {
                    //insert * before the char
                    convertedPhone.append("*"+phoneChar);
                }
            } else {
                //just append non-letter signs
                convertedPhone.append(phoneChar);
            }
        }
        return convertedPhone.toString();
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
        String wavDir = db.getProp(db.WAVDIR);
        String wavExt = db.getProp(db.WAVEXT);
        //loop through wav files
        Runtime rtime = Runtime.getRuntime();
        File wavDestDir = new File(st.getCanonicalPath()+"/wav");
        if (!wavDestDir.exists()){
            wavDestDir.mkdir();
        }
        //for the progress bar
        long nextPercentAfter = Math.round(1/(23.0/filenames.length));
        int index = 1;
        
        //loop through filenames
        for (int i=0;i<filenames.length;i++){
            String wavFileName = filenames[i];            
            //get a shell
            Process process = rtime.exec("/bin/bash");
            //get an output stream to write to the shell
            PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(process.getOutputStream()));
            //go to voicedir and call ch_wave
            pw.print("( cd "+db.getProp(db.ROOTDIR)
                +"; "+getProp(ESTDIR)+"/bin/ch_wave -otype nist -o st/wav/"+wavFileName+wavExt
                +" "+wavDir+wavFileName+wavExt
                +"; exit )\n");
            pw.close();
            
            process.waitFor();
            process.exitValue();
            
            //for the progress bar
            if (index == nextPercentAfter){
                //next percent is due
                progress++;
                index = 1;
            } else {
                index++;
            }
        }
       
        
        //correct st/bin/make_feats.pl
        File make_feats = new File(getProp(STDIR)+"bin/make_feats.pl");
        BufferedReader bufIn = new BufferedReader(
                new FileReader(make_feats));
        StringBuffer stBuf = new StringBuffer();
        String line = bufIn.readLine();
        while (line != null){
            line.trim();
            if (line.equals("\t \"-di wav -ei sph -do \\\"$CFG_FEATFILES_DIR\\\" \" .")){
                line = "\t \"-di wav -ei wav -do \\\"$CFG_FEATFILES_DIR\\\" \" . ";
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
        progress++;
        
        //call make_feats.pl
        rtime = Runtime.getRuntime();
        Process process = rtime.exec("/bin/bash");
        //get an output stream to write to the shell
        PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(process.getOutputStream()));
        //go to voicedir and call make_feats
        pw.print("(cd "+st.getAbsolutePath()
                +"; bin/make_feats.pl -ctl etc/"+voicename+"_train.fileids"
                +" -cfg etc/sphinx_train.cfg"
                +"; exit )\n");
        pw.flush();
        pw.close();
        //collect the output
        BufferedReader errReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()));
        while((line = errReader.readLine()) != null){
            System.out.println(line);
        }
        BufferedReader inReader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
        
        line = inReader.readLine();
        while(line!= null ) {
            System.out.println(line);
            line = inReader.readLine();
        }
        
        
        //shut down
        errReader.close();
        inReader.close();
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
                sb.append("$CFG_LISTOFFILES    = \"$CFG_BASE_DIR/etc/${CFG_DB_NAME}_train.fileids\";\n");
                continue;
            }
            if (line.startsWith("$CFG_TRANSCRIPTFILE")){
                //overwrite with transcription file name
                sb.append("$CFG_TRANSCRIPTFILE = \"$CFG_BASE_DIR/etc/${CFG_DB_NAME}_train.transcription\";\n");
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
   
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress()
    {
        return progress;
    }

}