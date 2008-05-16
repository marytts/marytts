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

package de.dfki.lt.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;

/**
 * 
 * This class represents a trie, i.e. a symbol (or 'letter') tree. Each trie 
 * node has arcs, to each of which a symbol is attached. The symbols guide the
 * lookup of entries in the trie.
 * 
 * The main purpose of this particular trie implementation is not the direct 
 * use of the trie (e.g. lookup) but its conversion to a finite-state machine 
 * that allows for an even more efficient storage and lookup.
 * 
 * We are very thankful to Andreas Eisele who had the idea of using tries and
 * transducers for our purposes and who provided us with a c-program 
 * this particular implemention is based on.
 * 
 * The symbols used in this trie are strings being a concatenation of an input
 * symbol, a delimiter (':') and an output symbol. To get the transducer 
 * representation first compute the minimization of the trie, then write the
 * transducer to disk. 
 * 
 * See main method for example usage.
 * 
 * @author benjaminroth
 *
 */
public class Trie {

    // class to store nodes of a trie
    class TrieNode{
        
        // maps a string to the node the corresponding arc leads to
        private Map<Integer,TrieNode> labelId2node = new HashMap<Integer, TrieNode>();
        
        // true if this state marks the end of an entry
        private boolean isFinal = false;
        
        // id for transducer representation. -1 means no id asigned.
        private int id = -1;
        
        // pointer to mother node - needed for minimization
        private TrieNode backPointer = null;
        
        // back and forth mapping from labels to ids
        private Map<String, Integer> label2id;
        private List<String> labels;
       
        /**
         * This constructs a TrieNode and specifies its predecessor.
         * 
         * @param predecessor
         */
        public TrieNode(TrieNode predecessor, Map<String, Integer> label2idMap, List<String> labels){
            this.backPointer = predecessor;
            this.label2id = label2idMap;
            this.labels = labels;
        }
        
        /**
         * 
         * This adds an entry (word...) to the node and its daughters. The 
         * entry is entered from the specified index on.
         * 
         * @param entry word to be entered.
         * @param index position from which on the entry is to be enetered at 
         *  this node.
         * @return the final node of this entry.
         */
        protected TrieNode add(String[] entry, int index){
            
            if (index == entry.length){
                // index points to the end of the word: already everything entered.
                this.isFinal = true;
                
                return this;
            }
            
            Integer labelId = this.label2id.get(entry[index]);
            if (null == labelId){
                labelId = this.labels.size();
                this.labels.add(entry[index]);
                this.label2id.put(entry[index], labelId);
            }
                
            // add rest of the entry to successors
            // get successor via id
            TrieNode successor = this.labelId2node.get(labelId);
            
            if ( null == successor ){
                
                successor = new TrieNode(this, this.label2id, this.labels);
                this.labelId2node.put(labelId, successor);
            }
            
            return successor.add(entry, index + 1);
                            
        }
        
        protected boolean hasSuccessor(){
            return this.labelId2node.size() > 0;
        }

        protected boolean identicalTo(TrieNode other) {
            
            // both nodes have to be final
            if (this.isFinal != other.isFinal)
                return false;

            // both nodes have to have same outgoing edges
            if (!this.labelId2node.keySet().equals(other.labelId2node.keySet()))
                return false;
            
            // edges must lead to nodes of same equivalence class
            for (Integer labelId : this.labelId2node.keySet()){
                if (labelId2node.get(labelId).id != other.labelId2node.get(labelId).id)
                    return false;
            }
                
            return true;
        }

        public int getId() {
            return this.id;
        }

        public void setId(int id2) {
            this.id = id2;
            
        }

        public boolean hasId() {
            return this.id != -1;
        }

        public TrieNode getBackPointer() {
            return this.backPointer;
        }

        /**
         * this checks if equivalent states in the right language of this node 
         * are already identified.
         * @return true iff so
         */
        public boolean rightIdentified() {
            
            for (TrieNode n : this.labelId2node.values()){
                if (! n.hasId())
                    return false;
            }
            return true;
        }
        
        public String toString(){
            StringBuffer sb = new StringBuffer();
            
            if (this.backPointer == null){
                sb.append(">");
            }
            
            if (this.isFinal){ sb.append("((" + this.id + "))"); } 
                else { sb.append("(" + this.id + ")");}
            
            for (Integer lId : this.labelId2node.keySet()){
                
                String l = labels.get(lId);
                
                sb.append("\n");
                sb.append("|-" + l);
                sb.append(" (" + this.labelId2node.get(lId).id + ")" );
            }
            
            return sb.toString();
        }

        public Map<Integer, TrieNode> getArcMap() {
            return this.labelId2node;
        }
        
    }
    
    // node that point to the beginning of all words
    private TrieNode root;
    
    // list of nodes that mark the end of a entry (word)
    private List<TrieNode> finalNodes;
    
    // mapping from nodes to representatives in a minimized transducer view.
    // mapping is done via list indices
    private List<TrieNode> reprs = null;
    
    // back and forth mapping from labels to ids
    private Map<String, Integer> label2id;
    private List<String> labels;
    
    /**
     * Standard constructor for a trie.
     */
    public Trie() {
        
        this.label2id = new HashMap<String, Integer>();
        this.labels = new ArrayList<String>();
        
        this.root = new TrieNode(null, label2id, labels);
        
        this.finalNodes = new ArrayList<TrieNode>();
    }
    
    /**
     * 
     * This adds an entry to the trie.
     * 
     * @param entry
     */
    public void add(String[] entry){
        // add the entry and remember backpointer to final node
        this.finalNodes.add(this.root.add(entry, 0));
    }
    
    /**
     * This computes the minimization of the trie, i.e. equivalent nodes are 
     * identified. This is necessary to store a compact version of this trie as
     * a minimal transducer. The trie itself is not represented more compactly.
     * 
     */
    public void computeMinimization(){
        // core idea: identify nodes with identical right language.
        
        // candidates are first all final nodes without successors
        LinkedList<TrieNode> identityCandidates = new LinkedList<TrieNode>();
        
        for (TrieNode fn  : this.finalNodes){
            if (!fn.hasSuccessor()){ identityCandidates.add(fn); }
        }
        
        // store the representants of the equivalence classes
        this.reprs = new ArrayList<TrieNode>();
        
        // for each identity candidate check to which nodes it is identical,
        // make new equivalence classes when needed and produce new candidates
        
        while ( !identityCandidates.isEmpty() ){
            // pop the head element
            TrieNode currCan = identityCandidates.remove();
            
            // does it belong to one of the already identified equiv. classes?
            for (TrieNode repr : reprs){
                if ( currCan.identicalTo(repr) ){
                    currCan.setId( repr.getId() );
                    break;
                }
            }
            
            // ... if not, let it represent a new class
            if (! currCan.hasId() ){
                currCan.setId(reprs.size());
                reprs.add(currCan);
            }
            
            TrieNode pred = currCan.getBackPointer();
            
            // add the predecessor of this node if...
            if ( null != pred && // 1. there is one
                    !pred.hasId() && // 2. it is not already processed
                    pred.rightIdentified() // 3. but its successors are processed
                    ) { identityCandidates.add(pred); }
           
        }
        
    }


    public String toString() {
        
        StringBuffer sb = new StringBuffer();
        
        for (TrieNode r : this.reprs){
            sb.append("\n");
            sb.append(r.toString());
        }
        
        return sb.toString();
        
    }
    
    public void writeFST(DataOutputStream out) throws IOException{
        
        if (null == this.reprs)
            throw new IllegalStateException("Cannot write transducer. First compute minimization of trie.");
        
        // compute arc offsets
        int[] arcOffsets = new int[this.reprs.size()+1];
            
        // first has offset one (consider additional start arc)
        arcOffsets[0] = 1;
           
        for (int i=0; i< reprs.size(); i++){
           arcOffsets[i+1] = arcOffsets[i] + reprs.get(i).getArcMap().size();
           
           // if final, consider the added "final arc"
           if (reprs.get(i).isFinal){
               arcOffsets[i+1] += 1;
           }
        }
        
        // write number of arcs
        out.writeInt(arcOffsets[arcOffsets.length-1]);
        
        // write starting arc:
        //      pointing to start node offset - empty label - final
        int startArc = arcOffsets[root.getId()] | 1 << 20 | 1 <<31;
        
        out.writeInt(startArc);
                
        // write arcs, final nodes have final arc as last with same empty label as start
        // dont forget to add one
        for (TrieNode repr : reprs){
            
            List<Integer> arcVals = new ArrayList<Integer>();
            
            for (Integer labelId : repr.getArcMap().keySet()){
                int targetId = repr.getArcMap().get(labelId).getId();
                arcVals.add( arcOffsets[targetId] | (labelId + 2) << 20 );
            }
            
            // if final, consider the added "final arc"
            if (repr.isFinal){
                arcVals.add( arcOffsets[repr.getId()] | 0 << 20 | 1 << 31 );
                
            } else {
                // mark last of the arcs as "last"
                int last = arcVals.size() -1;
                arcVals.set(last, arcVals.get(last) | 1 << 31);                
            }
            
            for (Integer val : arcVals){ out.writeInt(val); }
            
         }
        
        // compute label offsets
        int[] labelOffsets = new int[this.labels.size()*2];
            
        // first has offset two (input and output of empty arc)
        labelOffsets[0] = 4;
           
        for (int i=0; i< labels.size(); i++){
            
            // TODO: make split symbol argument
            String[] ioSym = labels.get(i).split(":",2); 
            
            // offset of outS determined by offset of inS
            labelOffsets[i*2+1] = labelOffsets[i*2];
            // offset increased by length of inS
            labelOffsets[i*2+1] += ioSym[0].getBytes().length;// TODO: use encoding
            // additionally increased by one because of stop byte
            labelOffsets[i*2+1] += 1;
                
            if (i+1<labels.size()){
                // offset of next inS determined by this outS
                labelOffsets[(i+1)*2] = labelOffsets[i*2+1];
                labelOffsets[(i+1)*2] += ioSym[1].getBytes().length;// TODO: use encoding
                labelOffsets[(i+1)*2] += 1;
            }
            
        }
        
        // write number of pairs
        out.writeInt(labels.size() + 2);
        
        // write empty label id/offset
        out.writeShort(0);
        out.writeShort(1);
        out.writeShort(2);
        out.writeShort(3);
        
        // write pair offsets
        for (int i=0; i< labels.size(); i++){
            out.writeShort( labelOffsets[i*2] );
            out.writeShort(labelOffsets[i*2+1]);
        }
        
        // write first two pairs: just empty symbols
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);
        out.writeByte(0);

        // write pairs
        for (int i=0; i< labels.size(); i++){
            
            // TODO: make split symbol argument
            String[] ioSym = labels.get(i).split(":",2); 
                       
            out.write(ioSym[0].getBytes());// TODO: use encoding
            out.writeByte(0);
            out.write(ioSym[1].getBytes());// TODO: use encoding
            out.writeByte(0);
        }
        
    }
    


public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
    // example usage
    
    // specify phone set definition
    String  phFileLoc = "phoneme-list-engb.xml";

    // specify location of lexicon you want to encode
    BufferedReader lexReader = new BufferedReader(
            new InputStreamReader(
            new FileInputStream("sampa-lexicon.txt"),"ISO-8859-1"));
    
    // specify location of output
    String fstLocation = "lexicon.fst";
    
 
    
    // initialize trainer 
    AlignerTrainer at = new AlignerTrainer(PhonemeSet.getPhonemeSet(phFileLoc), Locale.ENGLISH);
    
    System.out.println("reading lexicon...");
    
    // read lexicon for training
    at.readSampaLexicon(lexReader, true);
    
    System.out.println("...done!");

    System.out.println("aligning...");
    // make some alignment iterations
    for ( int i = 0 ; i < 8 ; i++ ){
        System.out.println(" iteration " + i);
        at.alignIteration();
        
    }
    System.out.println("...done!");
    
    Trie t = new Trie();
    
    System.out.println("entering alignments in trie...");
    for (int i = 0; i<at.lexiconSize(); i++){
        t.add(at.getAlignment(i));
    }
    System.out.println("...done!");

    System.out.println("minimizing trie...");
    t.computeMinimization();
    System.out.println("...done!");
    
    System.out.println("writing transducer to disk...");
    File of = new File(fstLocation);

    
    DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
    
    t.writeFST(os);
    os.flush();
    os.close();
    System.out.println("...done!");

    
    System.out.println("looking up test words...");
    FSTLookup fst = new FSTLookup(fstLocation);

    System.out.println(" zoroastrians -> " + Arrays.toString(fst.lookup("zoroastrians")));
    System.out.println(" the -> " + Arrays.toString(fst.lookup("the")));
    System.out.println(" thanks -> " + Arrays.toString(fst.lookup("thanks")));
    System.out.println(" xylophone -> " + Arrays.toString(fst.lookup("xylophone")));

    
    System.out.println("...done!");
    
    }
    
}
