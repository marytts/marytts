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
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.dfki.lt.mary.modules.phonemiser.PhonemeSet;
import de.dfki.lt.util.Trie.TrieNode;

/**
 * 
 * This is a particular Trie whose Symbols are List of Strings. It is required 
 * that every list has two elements, the first of which is interpreted as an 
 * input symbol and the second as an output symbol. The transducer obtained by 
 * trie minimization can be written to a file in the transducer format used by 
 * MARY.
 *  
 * To get the transducer 
 * representation first compute the minimization of the trie, then write the
 * transducer to disk. 
 * 
 * See main method for example usage.
 * 
 * @author benjaminroth
 *
 */
public class TransducerTrie extends Trie< List<String> > {
    static int ARCOFFSET_BITS = 20;
    static int LABELID_BITS = 11;


    // TODO: an add method that checks every label beforehand whether it
    // contains input and output symbol
    
    public void writeFST(DataOutputStream out) throws IOException{
        
        if (null == this.reprs)
            throw new IllegalStateException("Cannot write transducer. First compute minimization of trie.");
        
        // compute arc offsets
        int[] arcOffsets = new int[this.reprs.size()+1];
            
        // first has offset one (consider additional start arc)
        arcOffsets[0] = 1;
                   
        for (int i=0; i< rlist.size(); i++){
           arcOffsets[i+1] = arcOffsets[i] + rlist.get(i).getArcMap().size();
           
           // if final, consider the added "final arc"
           if (rlist.get(i).isFinal){
               arcOffsets[i+1] += 1;
           }
        }
        
        
        // write number of arcs
        int maxAO = arcOffsets[arcOffsets.length-1];
        out.writeInt(maxAO);
        
        // to ensure that number can be encoded:
        // shift to right by the number of available bits and look if something remains
        if ( (maxAO >> ARCOFFSET_BITS)!=0 )
            throw new IOException("To many arcs to be encoded in binary fst format.");

        int maxLID = this.labels.size() + 2;
        if ( (maxLID >> LABELID_BITS)!=0 )
            throw new IOException("To many arc-labels to be encoded in binary fst format.");      
        
        // write starting arc:
        //      pointing to start node offset - empty label - final
        int startArc = arcOffsets[root.getId()] | 1 << 20 | 1 <<31;
        
        out.writeInt(startArc);
                
        // write arcs, final nodes have final arc as last with same empty label as start
        // dont forget to add one
        for (TrieNode repr : rlist){
            
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
            
            List<String> ioSym = labels.get(i); 
            
            // offset of outS determined by offset of inS
            labelOffsets[i*2+1] = labelOffsets[i*2];
            // offset increased by length of inS
            labelOffsets[i*2+1] += ioSym.get(0).getBytes().length;// TODO: use encoding
            // additionally increased by one because of stop byte
            labelOffsets[i*2+1] += 1;
                
            if (i+1<labels.size()){
                // offset of next inS determined by this outS
                labelOffsets[(i+1)*2] = labelOffsets[i*2+1];
                labelOffsets[(i+1)*2] += ioSym.get(1).getBytes().length;// TODO: use encoding
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
            
            List<String> ioSym = labels.get(i); 
                       
            out.write(ioSym.get(0).getBytes());// TODO: use encoding
            out.writeByte(0);
            out.write(ioSym.get(1).getBytes());// TODO: use encoding
            out.writeByte(0);
        }
        
    }
    
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        // example usage
        
        String path = "/Users/benjaminroth/Desktop/mary/fst/";
        
        // specify phone set definition
        String  phFileLoc = path + "phoneme-list-engb.xml";

        // specify location of lexicon you want to encode
        BufferedReader lexReader = new BufferedReader(
                new InputStreamReader(
                new FileInputStream(path + "sampa-lexicon.txt"),"ISO-8859-1"));
        
        // specify location of output
        String fstLocation = path + "lexicon_hash1iter.fst";
        
     
        
        // initialize trainer 
        AlignerTrainer at = new AlignerTrainer(PhonemeSet.getPhonemeSet(phFileLoc), Locale.ENGLISH);
        
        System.out.println("reading lexicon...");
        
        // read lexicon for training
        at.readLexiconSimply(lexReader, " ");
        
        System.out.println("...done!");

        System.out.println("aligning...");
        // make some alignment iterations
        for ( int i = 0 ; i < 1 ; i++ ){
            System.out.println(" iteration " + (i+1));
            at.alignIteration();
            
        }
        System.out.println("...done!");
        
        TransducerTrie t = new TransducerTrie();
        
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
