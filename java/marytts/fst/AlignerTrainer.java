/**
 * Copyright 2003-2007 DFKI GmbH.
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
package marytts.fst;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import marytts.modules.phonemiser.Phoneme;
import marytts.modules.phonemiser.PhonemeSet;


/**
 * 
 * This trains an alignment model between Strings. Applications are for example 
 * letter-to-sound rule training (see LTSTrainer) or transducer 
 * construction/minimization.
 * 
 * @author benjaminroth
 *
 */
public class AlignerTrainer {

    // cost of translating first element of the pair into the second
    private HashMap<StringPair, Integer> aligncost;

    private int defaultcost = 10;
    // cost of deleting an element
    private int skipcost;

    private double logOf2 = Math.log(2.0);

    // optional info, eg. part-of-speech
    protected List<String> optInfo;
    // input side (eg. graphemes) of string pairs, split into symbols
    protected List<String[]> inSplit;
    // output side (eg. phonemes) of string pairs, split into symbols
    protected List<String[]> outSplit;

    protected Set<String> graphemeSet;
    
    private boolean inIsOut;

    /**
     * 
     * @param inIsOutAlphabet boolean indicating as input and output strings
     * should be considered as belonging to the same symbol sets (alignment
     * between identical symbol is then cost-free)
     */
    public AlignerTrainer(boolean inIsOutAlphabet){
        
        this.skipcost = this.defaultcost;
        this.aligncost = new HashMap<StringPair, Integer>();
        
        this.inSplit = new ArrayList<String[]>();
        this.outSplit = new ArrayList<String[]>();
        this.graphemeSet = new HashSet<String>();

        this.inIsOut = inIsOutAlphabet;
    }
    
    /**
     * New AlignerTrainer for pairs of different symbol sets
     */
    public AlignerTrainer(){
        this(false);

    }
    
    /**
     * 
     * This reads a lexicon where input and output strings are separated by a
     * delimiter that can be specified (splitSym). Strings are taken as they are
     * no normalization (eg. stress/syllable symbol removal, lower-casing ...)
     * is performed. In a third row additional info (eg. part of speech) can be
     * given.
     * Strings are stored split into symbols. For the phonemic part this split
     * can be done by using a phonemeset.
     * 
     * @param lexicon reader for lexicon
     * @param splitSym symbol to split columns of lexicon
     * @param hasOptInfo whether the lexicon has optional info in a third column
     * eg. POS
     * @throws IOException
     */
    public void readLexicon(BufferedReader lexicon, String splitSym, boolean hasOptInfo) throws IOException{
        
        
        if (hasOptInfo){
            this.optInfo = new ArrayList<String>();
        }
        
        String line;
        
        while ((line = lexicon.readLine()) != null){
            String[] lineParts = line.trim().split(splitSym);
            
            this.splitAndAdd(lineParts[0], lineParts[1]);
            
            if (hasOptInfo)
                this.optInfo.add(lineParts.length > 2 ? lineParts[2]:null);
            
        }
        
    }

    
    /**
     * This adds the input and output string in the most simple way: symbols
     * are simply the characters of the strings - no phonemisation/syllabification
     * or whatsoever is performed. No PhonemeSet has to be specified for that.
     * 
     * @param inString
     * @param outString
     */
    public void splitAndAdd(String inStr, String outStr){
        
        String[] inStrSplit = new String[inStr.length()];
        
        for ( int i = 0 ; i < inStr.length() ; i++ ){
            
            this.graphemeSet.add(inStr.substring(i, i+1));

            inStrSplit[i] = inStr.substring(i, i+1);
        }
        
        String[] outStrSplit = new String[outStr.length()];
        
        for ( int i = 0 ; i < outStr.length() ; i++ ){
            outStrSplit[i] = outStr.substring(i, i+1);
        }
        
        this.inSplit.add(inStrSplit);
        this.outSplit.add(outStrSplit);
        
    }
    
    public void addAlreadySplit(List<String> inStr, List<String> outStr){
        this.inSplit.add(inStr.toArray(new String[]{}));
        this.outSplit.add(outStr.toArray(new String[]{}));
    }

     /**
     * One iteration of alignment, using adapted Levenshtein distance.
     * After the iteration, the costs between a grapheme and a phoneme are
     * set by the log probability of the phoneme given the grapheme. Analogously,
     * The deletion cost is set by the log of deletion probability.
     * In the first iteration, all operations cost maxCost.
     *
     */
    public void alignIteration(){

        // this counts how many times a symbol is mapped to symbols
        Map<String, Integer> symMapCount = new HashMap<String, Integer>();

        // this counts how often particular mappings from one symbol to another occurred
        Map<StringPair, Integer> sym2symCount = new HashMap<StringPair, Integer>();

        // how many symbols are on input side
        int symCount = 0;

        // how many symbols are deleted
        int symDels = 0;
            
        // for every alignment pair collect counts
        for ( int i = 0; i < this.outSplit.size(); i++ ){

            String[] in = this.inSplit.get(i);
            String[] out = this.outSplit.get(i);
            int[] alignment = this.align(in, out);

            symCount += in.length;

            int pre = 0;

            // for every input symbol...
            for ( int inNr = 0; inNr < in.length; inNr++){
                
                if (alignment[inNr] == pre){
                    // is mapped to empty string
                    symDels++;
                } else {
                    // mapped to one or several symbols

                    // increase count of overall mappings for this symbol
                    Integer c = symMapCount.get(in[inNr]);
                    if (null == c){
                        symMapCount.put(in[inNr], alignment[inNr] - pre);
                    } else {
                        symMapCount.put(in[inNr], c + alignment[inNr] - pre);
                    }

                    // for every corresponding output symbol
                    for (int outNr = pre; outNr < alignment[inNr]; outNr ++){

                        // get key for mapping symbol to symbol
                        StringPair key = new StringPair(in[inNr], out[outNr]);

                        Integer mapC = sym2symCount.get(key);
                        if (null == mapC){
                            sym2symCount.put(key, 1);
                        } else {
                            sym2symCount.put(key, 1 + mapC);
                        }
                    } // ...for each output-symbol
                } // ...if > 0 output-symbols
                pre = alignment[inNr];
            } // ...for each input symbol
        } // ...for each input string

        // now build fractions, to estimate the new costs

        // first reset skip costs
        double delFraction = (double) symDels / symCount ;
        this.skipcost = (int) -this.log2(delFraction);

        // now reset aligncosts
        this.aligncost.clear();

        for (StringPair mapping : sym2symCount.keySet()){

            String firstSym = mapping.getString1();

            double fraction = (double) sym2symCount.get(mapping) / symMapCount.get(firstSym);
            int cost = (int) -this.log2( fraction );

            if ( cost < this.defaultcost ) {
                this.aligncost.put(mapping, cost);
            }
        }
    }
    
    public int lexiconSize(){
        return this.inSplit.size();
    }

     /**
     *
     * gets an alignment of the graphemes to the phonemes of an entry.
     * a StringPair array is returned, where every entry contains a grapheme
     * together with the phoneme sequence it is mapped to. The phoneme String is
      * just the concatenation of the symbols in the aligned sequence.
     *
     * @param entryNr nr of the lexicon entry
     */
    public StringPair[] getAlignment(int entryNr){

        String[] in = this.inSplit.get(entryNr);
        String[] out = this.outSplit.get(entryNr);
        int[] align = this.align(in, out);

        StringPair[] listArray = new StringPair[in.length];

        int pre = 0;
        for (int pos = 0; pos < in.length ; pos++){
            String inStr = in[pos];
            String oStr = "";

            for (int alPos = pre; alPos < align[pos]; alPos++){
                oStr += out[alPos];
            }
            pre = align[pos];

            listArray[pos] = new StringPair(inStr,oStr);
        }

        return listArray;
    }

     /**
     *
     * gets an alignment of the graphemes to the phonemes of an entry.
     * a StringPair array is returned, where every entry contains a grapheme
     * together with the phoneme sequence it is mapped to. The phoneme String is
      * just the concatenation of the symbols in the aligned sequence.
      * In addition, the extra info (eg. POS) is appended as one symbol on the
      * input side.
     *
     * @param entryNr nr of the lexicon entry
     */
    public StringPair[] getInfoAlignment(int entryNr){

        if (null == optInfo.get(entryNr))
            return getAlignment(entryNr);

        String[] in = this.inSplit.get(entryNr);
        String[] out = this.outSplit.get(entryNr);
        int[] align = this.align(in, out);

        StringPair[] listArray = new StringPair[in.length+1];

        int pre = 0;
        for (int pos = 0; pos < in.length ; pos++){
            String inStr = in[pos];
            String oStr = "";

            for (int alPos = pre; alPos < align[pos]; alPos++){
                oStr += out[alPos];
            }
            pre = align[pos];

            listArray[pos] = new StringPair(inStr,oStr);
        }

        listArray[in.length] = new StringPair(optInfo.get(entryNr),"");

        return listArray;
    }
    
    private double log2(double d){
        return Math.log(d) / logOf2;
    }

    private int symDist(StringPair key) {

        Integer cost = aligncost.get(key);

        if (null == cost){
            if (this.inIsOut)
                return (key.getString1().equals(key.getString2()))? 0:this.defaultcost;
            else 
                return this.defaultcost;
        }

        return cost;
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
     * The method returns for each input symbol the indix of the right alignment
     * boundary. eg. for input ['a','b'] and output ['a','a','b'] a correct
     * alignment would be: [2,3]
     *
     * @param in
     * @param out
     * @return
     */
    public int[] align(String[] istr, String[] ostr ) {

        StringPair key = new StringPair(null, null);

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

        // arrays storing the alignment boundaries
        int[][] p_al = new int[ ostr.length + 1 ][istr.length];
        int[][] al   = new int[ ostr.length + 1 ][istr.length];
        int[][] _al;

        // initialize values
        p_d[0]  = 0;
        p_sk[0] = true;

        // ... still initializing
        for (int j = 1; j < ostr.length + 1; j++){
            // only possibility first is to align the first letter
            // of the input string to everything
            p_al[j][0] = j;

            key.setString1(istr[0]);
            key.setString2(ostr[j-1]);
            p_d[j] = p_d[j-1] + symDist(key);
            p_sk[j] = false;
        }

        // constant penalty for not aligning a character
        int skConst = this.skipcost;

        // align
        // can start at 1, since 0 has been treated in initialization
        for (int i=1; i < istr.length; i++) {

            // zero'st row stands for skipping from the beginning on
            d[0] = p_d[0] + skConst ;
            sk[0] = true;
           
            for (int j = 1 ; j < ostr.length + 1; j++ ) {

                // translation cost between symbols ( j-1, because 0 row
                // inserted for not aligning at beginning)
                key.setString1(istr[i]);
                key.setString2(ostr[j-1]);
                int tr_cost = symDist(key);

                // skipping cost greater zero if not yet aligned
                int sk_cost = p_sk[j]? skConst : 0;

                if ( sk_cost + p_d[j] < tr_cost + d[j-1]) {
                    // skipping cheaper

                    // cost is cost from previous input char + skipping
                    d[j]  = sk_cost + p_d[j];
                    // alignment is from prev. input + delimiter
                    al[j] = p_al[j];
                    al[j][i] = j;
                    // yes, we skipped
                    sk[j] = true;

                } else {
                    // aligning cheaper

                    // cost is that from previously aligned output + distance
                    d[j]  = tr_cost + d[j-1];
                    // alignment continues from previously aligned
                    System.arraycopy(al[j-1],0,al[j],0,i);// copy of...
                    al[j][i] = j;

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
    
}

