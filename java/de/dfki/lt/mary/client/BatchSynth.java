package de.dfki.lt.mary.client;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

import java.util.*;



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

public class BatchSynth
{

    /**
     * Generate a set of audio files from text. Example call:
     * java -cp maryclient.jar -Dserver.host=localhost -Dserver.port=59125 -Dvoice=kevin16 de.dfki.lt.mary.client.BatchSynth target/dir path/to/texts.txt 
     * The text file must contain a target audio file name and the corresponding text in each line.
     * @param args first argument, the output directory; 
     * the rest, file names containing text files. Each text file contains, in each line, a file name followed by the sentence to generate as a .wav file.
     */
    public static void main(String[] args) throws Exception
    {
        File globalOutputDir = new File(args[0]);
        MaryClient mary = new MaryClient();
        String voice = System.getProperty("voice", "us1");
        String inputFormat = "TEXT_"+System.getProperty("locale", "en").toUpperCase();
        long globalStartTime = System.currentTimeMillis();
        int globalCounter = 0;
        for (int i=1; i<args.length; i++) {
           long genreStartTime = System.currentTimeMillis();
           int genreCounter = 0;
           File texts = new File(args[i]);
           String genre = texts.getName().substring(0, texts.getName().lastIndexOf('.'));
           File outputDir = new File(globalOutputDir.getPath()+"/"+genre);
           outputDir.mkdir();
           BufferedReader textReader = new BufferedReader(new FileReader(texts));
           String line;
           while ((line = textReader.readLine()) != null) {
               line = line.trim();
               if (line.length() == 0) continue;
               long startTime = System.currentTimeMillis();
               if (line.trim().startsWith("(")) {
                   line = line.substring(line.indexOf("(")+1, line.lastIndexOf(")"));
               }
               StringTokenizer st = new StringTokenizer(line);
               String basename = st.nextToken();
               String sentence = line.substring(line.indexOf(basename)+basename.length()+1).trim();
               //remove all backslashes
               sentence = sentence.replaceAll("\\\\","");
               FileOutputStream audio = new FileOutputStream(outputDir+"/"+basename+".wav");
               mary.process(sentence, inputFormat, "AUDIO", "WAVE", voice, audio);
               audio.close();
               long endTime = System.currentTimeMillis();
               System.out.println(basename+" synthesized in "+ ((float)(endTime-startTime)/1000.) + " s");
               globalCounter++;
               genreCounter++;
           }
           long genreEndTime = System.currentTimeMillis();
           System.out.println("Genre '"+genre+"' ("+genreCounter+" sentences) synthesized in "+ ((float)(genreEndTime-genreStartTime)/1000.) + " s");
        }
        long globalEndTime = System.currentTimeMillis();
        System.out.println("Total: "+globalCounter+" sentences synthesized in "+ ((float)(globalEndTime-globalStartTime)/1000.) + " s");

    }

}
