package delme;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import marytts.fst.StringPair;
import marytts.fst.TransducerTrie;

public class TransducerTest {

    /**
     * @param args
     */
    public static void main(String[] args)
    throws Exception
    {
        // TODO Auto-generated method stub
        BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "ISO-8859-1"));
        TransducerTrie tt = new TransducerTrie();
        // map each line to its index number
        int index = 0;
        String line;
        System.out.println("entering alignments in trie...");
        while ((line = bf.readLine()) != null) {
            StringPair[] pairs = new StringPair[line.length()];
            for (int k=0; k<line.length()-1; k++) {
                pairs[k] = new StringPair(line.substring(k, k+1), "");
            }
            // and the last one maps to the index number
            pairs[pairs.length-1] = new StringPair(line.substring(pairs.length-1), String.valueOf(index));
            tt.add(pairs);
            index++;
        }
        System.out.println("minimizing trie...");
        tt.computeMinimization();

        System.out.println("writing transducer to disk...");
        File of = new File(args[1]);
        DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
        tt.writeFST(os, "UTF-8");
        os.flush();
        os.close();
    }

}
