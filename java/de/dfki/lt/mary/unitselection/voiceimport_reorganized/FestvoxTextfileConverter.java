package de.dfki.lt.mary.unitselection.voiceimport_reorganized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/**
 * A class which converts a text file in festvox format
 * into a one-file-per-utterance format in a given directory.
 * @author schroed
 *
 */
public class FestvoxTextfileConverter
{
    protected File textFile;
    protected File textDir;
    
    public FestvoxTextfileConverter(File textFile, File textDir) throws IOException
    {
        if (!textFile.exists()) throw new IOException("No such file: "+ textFile);
        if (!textDir.exists()) textDir.mkdir();
        this.textFile = textFile;
        this.textDir = textDir;
    }
    
    public void compute() throws IOException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), "UTF-8"));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.substring(line.indexOf("(")+1, line.lastIndexOf(")"));
            StringTokenizer st = new StringTokenizer(line);
            String basename = st.nextToken();
            String text = line.substring(line.indexOf("\"")+1, line.lastIndexOf("\""));
            PrintWriter out = new PrintWriter(new File(textDir, basename+".txt"), "UTF-8");
            out.print(text);
            out.flush();
            out.close();
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException 
    {
        String textfile = System.getProperty("text.file", "etc/domain.data");
        String textdir = System.getProperty("text.dir", "text");
        FestvoxTextfileConverter ftc = new FestvoxTextfileConverter(new File(textfile), new File(textdir));
        ftc.compute();
    }

}
