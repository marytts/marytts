package de.dfki.lt.mary.dbselection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;

public class MaryScriptCreator {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception 
    {
        // input: one line ( dummy "text" )
        // output: file named voice0001.txt containing text
        String prefix = System.getProperty("prefix", "prompt");
        File outDir = new File(System.getProperty("outdir", "./text"));
        String inputEncoding = System.getProperty("encoding", "UTF-8");
        boolean ignoreFirst = Boolean.parseBoolean(System.getProperty("ignoreFirst", "true"));
        if (!outDir.exists()) outDir.mkdir();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, inputEncoding));
        String line;
        DecimalFormat f = new DecimalFormat("0000");
        int i=0;
        while ((line = in.readLine()) != null) {
            i++;
            // all we do is search for the first and last quotation marks; the rest is ignored
            //int first = line.indexOf('"');
            //int last = line.lastIndexOf('"');
            int first = 0;
            if (ignoreFirst) first = line.indexOf(" ") + 1;
            int last = line.length();
            if (first == -1 || last == -1 || last <= first) {
                System.err.println("Line no. "+i+" has no space -- skipping: "+line);
            }
            File outFile = new File(outDir, prefix+f.format(i)+".txt");
            PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
            out.println(line.substring(first, last));
            out.close();
        }
        
    }

}
