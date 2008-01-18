package de.dfki.lt.mary.unitselection.voiceimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.types.CommandlineJava.SysProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.speech.freetts.Item;
import com.sun.speech.freetts.Relation;
import com.sun.speech.freetts.Utterance;

import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.modules.TargetFeatureLister;
import de.dfki.lt.mary.modules.phonemiser.Phoneme;
import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.mary.modules.synthesis.FreeTTSVoices;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.unitselection.Target;


public class CorrectedTranscriptionAligner extends VoiceImportComponent {
    
    private DatabaseLayout db;
    
    // properties
    public final String ORIGTRANS = "CorrectedTranscriptionAligner.original";
    public final String CORRTRANS = "CorrectedTranscriptionAligner.corrected";
    public final String RESULTTRANS = "CorrectedTranscriptionAligner.results";
    public final String SYMCOSTS = "CorrectedTranscriptionAligner.costfile";
    public final String PHONSET = "CorrectedTranscriptionAligner.phonset";
    
    Map<String, Integer> aligncost;
    int defaultcost;
    int skipcost;
    PhonemeSet phonemeSet;
    
    public CorrectedTranscriptionAligner() {
        this.aligncost = new HashMap<String, Integer>();
        this.defaultcost = 1;
        this.skipcost = 1;
    }

    public String getName() {
        return "CorrectedTranscriptionAligner";
    }
    
    public void initialiseComp()
    {      
        
    }
    
    public SortedMap getDefaultProps(DatabaseLayout db) {
        this.db = db;
        if (props == null){
            props = new TreeMap();
            
            // original transcriptions (?LABDIR / TEXTDIR)
            String origTrans = System.getProperty(ORIGTRANS);
            if ( origTrans == null ) {
                origTrans = db.getProp(db.ROOTDIR)
                +"phonemisedXML"
                +System.getProperty("file.separator");
            }
            props.put(ORIGTRANS,origTrans);
            
            // corrected transcriptions
            String corrTrans = System.getProperty(CORRTRANS);
            if ( corrTrans == null ) {
                corrTrans = db.getProp(db.ROOTDIR)
                +"lab"
                +System.getProperty("file.separator");
            }
            props.put(CORRTRANS,corrTrans);
            
            // aligned corrected transcriptions
            String resultTrans = System.getProperty(RESULTTRANS);
            if ( resultTrans == null ) {
                resultTrans = db.getProp(db.ROOTDIR)
                +"correctedPhonemisedXML"
                +System.getProperty("file.separator");
            }
            props.put(RESULTTRANS,resultTrans);
            
            // alignment costs
            String symCosts = System.getProperty(SYMCOSTS);
            if ( symCosts == null ) {
                symCosts = db.getProp(db.ROOTDIR)
                +"temp"
                +System.getProperty("file.separator")
                +"alignmentcosts_de.txt";
                //"/project/mary/lib/modules/de/cap/alignmentcosts_de.txt";
            }
            props.put(SYMCOSTS,symCosts);
            
            // alignment costs
            String phonSet = System.getProperty(PHONSET);
            if ( phonSet == null ) {
                phonSet = db.getProp(db.ROOTDIR)
                +"temp"
                +System.getProperty("file.separator")
                +"phoneme-list-de.xml"; 
                //"/project/mary/lib/modules/de/cap/phoneme-list-de.xml";
            }
            props.put(PHONSET,phonSet);

        }
        return props;
    }
    
    protected void setupHelp(){
        props2Help = new TreeMap();
        props2Help.put(ORIGTRANS,"directory containing the files with text and automatic phonemization");
        props2Help.put(CORRTRANS,"directory containing manually corrected transcriptions");
        props2Help.put(RESULTTRANS,"directory for the texts with aligned corrected transcriptions");
        props2Help.put(SYMCOSTS,"file with the distance that is to be used for alignment");
    }
    
    public int getProgress() {
        // TODO Auto-generated method stub
        return -1;
    }
    /**
     * align and change automatic transcriptions to manually 
     * corrected ones.
     * 
     * XML-Version: this changes mary xml-files (PHONEMISED)
     * @throws TransformerException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     */
    public boolean compute() throws IOException, TransformerException, ParserConfigurationException, SAXException{
        
        // set costs used for distance computation
        try{
            this.setDistance(new BufferedReader(new FileReader ((String) props.get(this.SYMCOSTS))));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("File with symbol costs not set: " + (String) props.get(this.SYMCOSTS));
        }
        
        this.setDefaultCost( this.getMaxCost() );
        this.setSkipCost( this.getMaxCost() / 3 );
        
        // phoneme set is used for splitting the sampa strings
        this.setPhonemeSet(PhonemeSet.getPhonemeSet((String) props.get(this.PHONSET)));

        
        //go through original xml files
        File xmlDir = new File((String) props.get(this.ORIGTRANS));
        if (!xmlDir.exists())
            throw new IllegalStateException("Path with original transcription files not found: " + (String) props.get(this.ORIGTRANS));
   
        File[] xmlFiles = xmlDir.listFiles();
        
        File xmlOutDir = new File((String) props.get(this.RESULTTRANS));
        if (!xmlOutDir.exists())
            xmlOutDir.mkdir();
            
        // for parsing xml files
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        
        // for writing xml files
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();

       
        for (int i=0;i<xmlFiles.length;i++){
            File nextFile = xmlFiles[i];
            System.out.println(nextFile.getName());
            
            // get original xml file
            Document doc = db.parse(nextFile);

            // open destination xml file
            FileWriter docDest  = new FileWriter((String) props.get(this.RESULTTRANS) + nextFile.getName());
            
            // open file with manual transcription that is to be aligned
            
            //BufferedReader manTrans;
            
            String manTransString;
            try{

                String trfdir = (String) props.get(this.CORRTRANS);

                
                //if (!trfdir.endsWith(System.getProperty("File.separator")))
                //        trfdir += System.getProperty("File.separator");
                
                String trfname = trfdir + 
                nextFile.getName().substring(0, nextFile.getName().length() - 4) + ".lab";
                
                //System.out.println(trfname);
                
                manTransString = this.readLabelFile(trfname);
                
            } catch ( FileNotFoundException e ) {
                System.out.println("No manual transcription found, copy original ...");
                
                // transform the unchanged xml-structure to a file
                DOMSource source = new DOMSource( doc );
                StreamResult output = new StreamResult(docDest);
                transformer.transform(source, output);

                continue;
            }
                   
            
            
            // get manual transcription
            //String manTransString = manTrans.readLine();
            //if (manTransString.equals("") || manTrans.readLine()!=null )
            //    throw new IllegalArgumentException("File with corrected transcription has to contain exactly one non-empty line. Not the case for " + nextFile.getName());
            //manTrans.close();
            
            // align transcriptions
            this.alignXmlTranscriptions(doc, manTransString);
            
            // write results to output
            DOMSource source = new DOMSource( doc );
            StreamResult output = new StreamResult(docDest);
            transformer.transform(source, output);
        }
            

        return true;
    }
    
    /*
    public boolean compute() throws IOException{
       
        // set costs used for distance computation
        try{
            this.setDistance(new BufferedReader(new FileReader ((String) props.get(this.SYMCOSTS))));
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("File with symbol costs not set: " + (String) props.get(this.SYMCOSTS));
        }
        
        this.setDefaultCost(this.getMaxCost());
        this.setSkipCost(this.getDefaultCost()/4);
        
        //go through original lab files
        File labDir = new File((String) props.get(this.ORIGTRANS));
        if (!labDir.exists())
            throw new IllegalStateException("Path with original transcription files not found: " + (String) props.get(this.ORIGTRANS));
   
        File[] labFiles = labDir.listFiles();
        
        File labOutDir = new File((String) props.get(this.RESULTTRANS));
        if (!labOutDir.exists())
            labOutDir.mkdir();
            
       
        for (int i=0;i<labFiles.length;i++){
            File nextFile = labFiles[i];
            System.out.println(nextFile.getName());
            
            // open original lab file
            BufferedReader labIn = new BufferedReader(new FileReader(nextFile));
            
            // open destination lab file
            PrintWriter labOut = new PrintWriter(new FileWriter((String) props.get(this.RESULTTRANS) + nextFile.getName()));
            
            // open file with manual transcription that is to be aligned
            BufferedReader manTrans;
            try{
                String trfname = (String) props.get(this.CORRTRANS) + 
                    nextFile.getName().substring(0, nextFile.getName().length() - 4) + ".ph";
                manTrans = new BufferedReader(new FileReader(trfname ));
            } catch ( FileNotFoundException e ) {
                System.out.println("No manual transcription found, copy original ...");
                
                // just copy transcription...
                String line;
                while ((line = labIn.readLine()) != null)
                    labOut.println(line);
                
                // close files and go on to next file
                labIn.close();
                labOut.close();
                continue;
            }
            
            // get manual transcription
            String manTransString = manTrans.readLine();
            if (manTransString.equals("") || manTrans.readLine()!=null )
                throw new IllegalArgumentException("File with corrected transcription has to contain exactly one non-empty line. Not the case for " + nextFile.getName());
            manTrans.close();
            
            // write alignment to output file
            labOut.print( this.alignTranscriptions(labIn, manTransString));
            // System.out.println(this.alignTranscriptions(labIn, manTransString));
            labIn.close();
            labOut.close();
        }
            

        return true;
    }*/
    
    
    public void setPhonemeSet(PhonemeSet aPhonemeSet) {
        this.phonemeSet = aPhonemeSet;
    }

    /**
     * This reads in a label file and returns a String of the phonetic symbols,
     * seperated by white spaces. Pause symbols ("_") are disregarded (skipped).
     * 
     * @throws IOException if something goes wrong with opening/reading the file
     * 
     */
    private String readLabelFile(String trfname) throws IOException {
        // reader for label file.
        BufferedReader lab = new BufferedReader(new FileReader(trfname));
        
        // string with phonemes, seperated by white spaces
        String result = "";
        
        String line;
        
        while ((line = lab.readLine()) != null){
            if ( line.startsWith("#") ) 
                continue;
            
            String[] lineLmnts = line.split("\\s+");
            
           if ( lineLmnts.length != 3 )
               throw new IllegalArgumentException("Expected three columns in label file, got " + lineLmnts.length);
           
           // skip pauses
           if (!lineLmnts[2].equals("_")){
               result += lineLmnts[2] + " ";
           }
        }
        
        return result;
    }

    /**
     * This sets the distance between two symbols from a feature/cost 
     * description, usually a file.
     * 
     * This file may contain two types of lines.
     * 1.) feature enumerations: 
     *     suppose we have symbols s1 and s2, and features f1, f2, f3.
     *     possible lines are:
     *     
     * s1 f1_of_s1 f2_of_s1 f3_of_s1
     * s2 f1_of_s2 f2_of_s2 f3_of_s2
     * 
     *     The distance between two symbols is then the number of the
     *     cases where they disagree in a feature.
     *     
     * 2.) Explicit cost statements. The override the feature difference costs.
     *     For example, to state that mapping s2 to s1 has costs 4 simply add
     *     the line:
     *     
     * >> s2 s1 4
     * 
     */
    private void setDistance(BufferedReader input) throws IOException{
        String l;
        
        String[] lineLmnts;
        List<String[]> symFeats =  new ArrayList<String[]>();
        

        
        // read data for costs
        while ((l = input.readLine()) != null) {
            l = l.trim();
            
            // comment line
            if ( l.equals("") || l.startsWith("#"))
                continue;
            
            lineLmnts = l.split("\\s+");
            
            if ( lineLmnts[0].equals(">>") ){
                // collect specified costs
                
                String key = lineLmnts[1] + " " + lineLmnts[2];
                
                try{
                    Integer value = Integer.valueOf(lineLmnts[3]);
                    this.aligncost.put(key, value);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Expected number in alignment cost definition");
                }
                
            } else {
                // store features to compute costs if unspecified
                symFeats.add(lineLmnts);
            }
            
            // TODO: for efficiency reasons only compute "triangle matrix"
            // for the feature combinations of all symbols compute distances
            for (String[] feats1 : symFeats){
                for (String[] feats2 : symFeats){
                    
                    int distance = 0;
                    
                    // compare similarity (first element is not a feature but the symbol)
                    for (int i = 1; i < feats1.length; i++){
                        
                        if (!feats1[i].equals(feats2[i]))
                            distance++;
                    }
                    
                    String key = feats1[0] + " " + feats2[0];
                    
                    // if no value is set, set this distance
                    if (!this.aligncost.containsKey(key)){
                        this.aligncost.put(key, distance);
                    }                    
                }
            }    
        }
    }
    
    /**
     * 
     * This computes the alignment that has the lowest distance between two 
     * Strings.
     * 
     * There are three differences to the normal Levenshtein-distance:
     * 
     * 1. Only insertions and deletions are allowed, no replacements (i.e. no 
     *    "diagonal" transitions)
     * 2. insertion costs are dependent on a particular phone on the input side
     *    (the one they are aligned to)
     * 3. deletion is equivalent to a symbol on the input side that is not 
     *    aligned. There are costs associated with that.
     *    
     * The method returns the output string with alignment boundaries ('#') 
     * inserted.
     * 
     * @param in
     * @param out
     * @return
     */
    private String distanceAlign(String in, String out ) {
        String[] istr = in.split(" ");
        String[] ostr = out.split(" ");
        String delim = "#";

        
        // distances:
        // 1. previous distance (= previous column in matrix)
        int[] p_d = new int[ostr.length+1];
        // 2. current distance
        int[] d = new int[ostr.length+1];
        // 3. dummy array for swapping, when switching to new column
        int[] _d;
        
        // array indicating if a skip was performed (= if current character has not been aligned)
        // same arrays as for distances
        boolean[] p_sk = new boolean[ ostr.length + 1 ];
        boolean[] sk   = new boolean[ ostr.length + 1 ];
        boolean[] _sk;
        
        // arrays storing the alignments corresponding to distances
        String[] p_al = new String[ ostr.length + 1 ];
        String[] al   = new String[ ostr.length + 1 ];
        String[] _al;
        
        // initialize values
        p_d[0]  = 0;
        p_al[0] = "";
        p_sk[0] = true;

        
        // ... still initializing
        for (int j = 1; j < ostr.length + 1; j++){
            // only possibility first is to align the first letter 
            // of the input string to everything
            p_al[j] = p_al[j-1] + " " + ostr[j-1]; 
            p_d[j] = p_d[j-1] + symDist(istr[0],ostr[j-1]);
            p_sk[j] = false;        
        }
        
        // constant penalty for not aligning a character
        int skConst = this.skipcost;
        
        // align
        // can start at 1, since 0 has been treated in initialization
        for (int i=1; i < istr.length; i++) {
            
            // zero'st row stands for skipping from the beginning on
            d[0] = p_d[0] + skConst;
            al[0] = p_al[0] + " " + delim;
            sk[0] = true;
            
            for (int j = 1 ; j < ostr.length + 1; j++ ) {
                
                // translation cost between symbols ( j-1, because 0 row 
                // inserted for not aligning at beginning)
                int tr_cost = symDist(istr[i], ostr[j-1]);
                
                // skipping cost greater zero if not yet aligned 
                int sk_cost = p_sk[j]? skConst : 0;
                
                if ( sk_cost + p_d[j] < tr_cost + d[j-1]) {
                    // skipping cheaper
                    
                    // cost is cost from previous input char + skipping
                    d[j]  = sk_cost + p_d[j];
                    // alignment is from prev. input + delimiter
                    al[j] = p_al[j] + " " + delim;
                    // yes, we skipped
                    sk[j] = true;
                    
                } else {
                    // aligning cheaper
                    
                    // cost is that from previously aligned output + distance
                    d[j]  = tr_cost + d[j-1];
                    // alignment continues from previously aligned
                    al[j] = al[j-1] + " " + ostr[j-1];
                    // nope, didn't skip
                    sk[j] = false;
                    
                }
            }
            
            // swapping
            _d  = p_d;
            p_d = d;
            d   = _d;

            _sk  = p_sk;
            p_sk = sk;
            sk   = _sk;
            
            _al  = p_al;
            p_al = al;
            al   = _al;
        }
        
        return p_al[ostr.length];
        
    }
    
    /**
     * 
     * This changes the transcription of a Marydata object into a corrected
     * transcription. The Mary data is changed. 
     * The symbols of the original transcription aligned aligned to corrected 
     * ones, with which they are replaced in turn.
     * 
     * @param d
     * @param oSymStr
     * @return
     * @throws ParserConfigurationException 
     * @throws IOException 
     * @throws SAXException 
     */
    public Document alignXmlTranscriptions(Document doc, String correct) throws SAXException, IOException, ParserConfigurationException    {
        // get all <t .. /> - elements in xml data
        NodeList tokens = doc.getElementsByTagName("t");
                                
        // String Tokenizer devides transcriptions into syllables
        // syllable delimiters and stress symbols are retained
        String delims = "',-";

        // String storing the original transcription
        String orig = "";

        // first looping: get original phoneme String
        for (int tNr = 0; tNr < tokens.getLength() ; tNr++ ){
            
            // disregard if there is no sampa to change
            if ( !((Element) tokens.item(tNr)).hasAttribute("sampa") )
                continue;
            
            String sampa = ((Element) tokens.item(tNr)).getAttribute("sampa");
            
            // TODO: remove redundant checking
            if (null == sampa){
                // no sampa transcription - skip element
                continue;  
            } else {
                List<String> sylsAndDelims = new ArrayList<String>();
                StringTokenizer sTok = new StringTokenizer(sampa, delims, true);
                
                while(sTok.hasMoreElements()){
                    String currTok = sTok.nextToken();
                    
                    if (delims.indexOf(currTok) == -1) {
                        // current Token is no delimiter
                        for ( Phoneme ph : phonemeSet.splitIntoPhonemes(currTok)){
                            orig += ph.name() + " ";
                        }// ... for each phoneme
                    }// ... if no delimiter
                }// ... while there are more tokens         
            }// ... if there is transcription 
        }// ... for each t-Element
        
        // now we align the transcriptions and split it at the delimiters
        String al = this.distanceAlign(orig.trim(),correct.trim()) + " ";
        String[] alignments = al.split("#");
        
        //System.out.println("Alignment: " + al);
        
        // counter to keep track of the position in alignment array
        int currAl = 0;
        
        // second looping: get original phoneme String
        for (int tNr = 0; tNr < tokens.getLength() ; tNr++ ){
            Element token = (Element) tokens.item(tNr);
            
            // disregard if there is no sampa to change
            if ( !token.hasAttribute("sampa") )
                continue;
            
            String sampa = token.getAttribute("sampa");
            
            // the transcription to which the old is aligned
            String newSampa = "";
            
            if (null == sampa){
                // no sampa transcription - skip element
                continue;  
            } else {
                List<String> sylsAndDelims = new ArrayList<String>();
                StringTokenizer sTok = new StringTokenizer(sampa, delims, true);
                
                while(sTok.hasMoreElements()){
                    String currTok = sTok.nextToken();
                    
                    if (delims.indexOf(currTok) == -1) {
                        // current Token is no delimiter
                        for ( Phoneme ph : phonemeSet.splitIntoPhonemes(currTok)){
                            orig += ph.name();

                            //System.out.print(ph.name() + " >>" + alignments[currAl] + "; ");
                            
                            // new transciption is the aligned ones without white spaces
                            newSampa += alignments[currAl].replaceAll(" ", "");
                            currAl +=1;
                        }
                        
                        
                    } else {
                        // all delimiters have to be copied
                        
                        
                        /* exceptions treated below...*/
                        String previousChar;
                        if (newSampa.length() == 0)
                            previousChar = "";
                        else
                            previousChar = newSampa.substring(newSampa.length()-1);
                        
                        
                        // with a few exceptions:
                        // 1. a syllable is only indicated after a phoneme symbol
                        // 2. no two subsequent stress symbols are allowed
                        if ( ( previousChar.equals("") && currTok.equals("-") ) || 
                             ( previousChar.equals("-") && currTok.equals("-") )||
                             ( previousChar.equals("'") && currTok.equals("-") )||
                             ( previousChar.equals(",") && currTok.equals("-") )||
                             ( previousChar.equals("'") && currTok.equals("'") )||
                             ( previousChar.equals(",") && currTok.equals("'") )||
                             ( previousChar.equals("'") && currTok.equals(",") )||
                             ( previousChar.equals(",") && currTok.equals(",") )){
                            // continue
                        } else { //...
                            newSampa += currTok;
                        }
                        

                        
                    }
                }// ... while there are more tokens 
                

                /*
                // replace illegal delimiter sequences
                newSampa =  newSampa.replaceAll("--", "-");
                newSampa =  newSampa.replaceAll("'-", "-");
                newSampa =  newSampa.replaceAll(",-", "-");
                newSampa =  newSampa.replaceAll("^-", "");
                newSampa =  newSampa.replaceAll("'$", "");
                newSampa =  newSampa.replaceAll(",$", "");
                newSampa =  newSampa.replaceAll("-$", "");
                */
                
                // if new sampa ends with delimiters, delete them
                while (newSampa.length() > 0 &&
                        delims.indexOf( newSampa.substring(newSampa.length()-1) ) != -1 )
                {
                    newSampa = newSampa.substring(0,newSampa.length()-1);
                }
                
                // set new sampa
                token.setAttribute("sampa", newSampa);
                
            }// ... if there is transcription 
        }// ... for each t-Element
                          
        //System.out.println();
        
        return doc;
    }
    
    /**
     * 
     * This aligns a transcription of the form
     * 
     * words w 3: r d z
     * and A: n d
     * symbols s I m b @ l s
     * 
     * and a phonetic symbol string of the form
     * w 3: r d z A: n d s I m b l s
     * 
     * to return a new transcription that aligns the words with subsequences 
     * of the chain of symbols 
     * 
     * @return
     * @throws IOException 
     */
    private String alignTranscriptions(BufferedReader transcr, String oSymStr) throws IOException{
        
        StringBuffer sb = new StringBuffer();
        
        List<String> words = new ArrayList<String>();
        List<Integer> lengths = new ArrayList<Integer>();
        
        String iSymStr = "";
        
        String l;
        
        // read input alignment in 
        while ((l = transcr.readLine()) != null) {
            String[] lineArr = l.split("\\s+");
            
            words.add(lineArr[0]);
            lengths.add(lineArr.length - 1);
            
            for ( int i=1 ; i<lineArr.length ; i++ )
                //if (lineArr[i].length() > 0)
                    iSymStr += lineArr[i] + " ";        
        }
        
        // align with output string
        String align = distanceAlign(iSymStr, oSymStr);

        
        String[] alignments = align.split("#");
        
        String lineSep = System.getProperty("line.separator");
        
        int symCount = 0;
        
        // caoncatenate the new alignment
        for (int wordNr = 0 ; wordNr < words.size() ; wordNr++){
            
            
            String word = words.get(wordNr);
            
            sb.append(word);
            sb.append(" ");
            
            for ( int symNr = symCount ; symNr < symCount + lengths.get(wordNr); symNr++ ){
                sb.append(alignments[symNr].replaceFirst(" ",""));
                //sb.append(" ");
            }
            symCount += lengths.get(wordNr);
            
            sb.append(lineSep);
            
        }
        
        return sb.toString();
        
    }
    
    private int getMaxCost(){
        int maxMapping = Collections.max(this.aligncost.values());
        return (maxMapping > this.defaultcost) ? maxMapping : this.defaultcost;
    }
    
    private void setDefaultCost(int aCost){
        this.defaultcost = aCost;
    }
    
    private int getDefaultCost(){
        return this.defaultcost;
    }
    
    private void setSkipCost(int aCost){
        this.skipcost = aCost;
    }
    
    private int symDist(String aString1, String aString2) {
        
        String key = aString1 + " " + aString2;
        
        // if a value is stored, return it
        if (this.aligncost.containsKey(key)){
            return aligncost.get(key);
        } else {       
            // otherwise use 0 for equal symbols and defaultcost for different symbols
            return (aString1.equals(aString2))? 0:this.defaultcost;
        }
    }


}
