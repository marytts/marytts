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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of a finite state transducer lookup.
 * @author Andreas Eisele
 */
public class FSTLookup
{
    private FST fst;

    /**
     * Initialise the finite state transducer lookup. This constructor will
     * assume that the file uses the system default encoding.
     * @param fileName the name of the file from which to load the FST.
     * @throws IOException if the FST cannot be loaded from the given file.
     */
    public FSTLookup(String fileName) throws IOException
    {
        fst = new FST(fileName);
    }

    /**
     * Initialise the finite state transducer lookup.
     * @param fileName the name of the file from which to load the FST.
     * @param encoding the name of the encoding used in the file (e.g., UTF-8
     * or ISO-8859-1).
     * @throws IOException if the FST cannot be loaded from the given file.
     * @throws UnsupportedEncodingException if the encoding is not supported.
     */
    public FSTLookup(String fileName, String encoding)
    throws IOException, UnsupportedEncodingException
    {
        fst = new FST(fileName, encoding);
    }

    /**
     * Initialise the finite state transducer lookup. This constructor will
     * assume that the file uses the system default encoding.
     * @param fileName the name of the file from which to load the FST.
     * @param verbose whether to write a report to stderr after loading.
     * @throws IOException if the FST cannot be loaded from the given file.
     */
    public FSTLookup(String fileName, boolean verbose) throws IOException
    {
	fst = new FST(fileName, verbose);
    }

    /**
     * Initialise the finite state transducer lookup.
     * @param fileName the name of the file from which to load the FST.
     * @param encoding the name of the encoding used in the file (e.g., UTF-8
     * or ISO-8859-1).
     * @param verbose whether to write a report to stderr after loading.
     * @throws IOException if the FST cannot be loaded from the given file.
     * @throws UnsupportedEncodingException if the encoding is not supported.
     */
    public FSTLookup(String fileName, String encoding, boolean verbose)
    throws IOException, UnsupportedEncodingException
    {
	fst = new FST(fileName, verbose);
    }

    /**
     * Look up a word in the FST. The FST runs in normal mode, i.e. it
     * generates the expanded forms from the original forms. This method is
     * thread-safe.
     * @param word the word to look up.
     * @return a string array containing all expansions of word. If no
     * expansion is found, an array of length 0 is returned.
     */
    public String[] lookup(String word)
    {
        return lookup(word, false);
    }

    /**
     * Look up a word in the FST. This method is thread-safe.
     * @param word the word to look up.
     * @param generate whether the FST is to run in inverse direction,
     * i.e. generating the original form from the expanded form.
     * @return a string array containing all expansions of word. If no
     * expansion is found, an array of length 0 is returned.
     */
    public String[] lookup(String word, boolean generate) {
        StringBuffer buffer2=new StringBuffer();
        List results=new ArrayList();
        
        lookup(word, 0, 0, generate, buffer2, results);
        
        String[] resultArray=new String[results.size()];
        resultArray = (String[]) results.toArray(resultArray);
        return resultArray;
    }
    
    private void lookup(String word, int offset1, int arc, boolean generate,
                        StringBuffer buffer2, List results) {
        do {
            int label = fst.labels[arc];
            int offset2 = buffer2.length();
            if(label==0) {
                if(offset1==word.length()) {
                    results.add(buffer2.toString());
                }
            } else {
                String s1;
                if (generate) s1 = (String) fst.strings.get(fst.mapping[fst.offsets[2*label+1]]);
                else s1 = (String) fst.strings.get(fst.mapping[fst.offsets[2*label]]);
                if(word.startsWith(s1, offset1)) {
                    String s2;
                    if (generate) s2 = (String) fst.strings.get(fst.mapping[fst.offsets[2*label]]);
                    else s2 = (String) fst.strings.get(fst.mapping[fst.offsets[2*label+1]]);
                    buffer2.append(s2);
                    lookup(word, offset1+s1.length(), fst.targets[arc], generate, buffer2, results);
                    if (offset2<buffer2.length()) buffer2.delete(offset2, buffer2.length());
                }
            }
        } while (!fst.isLast[arc++]);
    }
    
    /**
     * A simple command-line frontend for the FST.
     */
    public static void main(String[] args) throws IOException
    {
        long iBegin=System.currentTimeMillis();
        
        if(args.length==0) {
            System.err.println("usage: java de.dfki.lt.util.FSTLookup FstFile [-g] [word ...]");
            System.exit(-1);
        }
        
        FSTLookup fstLookup = new FSTLookup(args[0], true);
        
        if(args.length==1 || (args.length ==2 && args[1].equals("-g"))) {
            boolean generate = false;
            if (args.length == 2 && args[1].equals("-g")) generate = true;
            String line;
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                while((line = in.readLine()) != null) {
                    showResults(line, fstLookup.lookup(line, generate) ) ;
                }
            }
            catch(Exception e) {
                System.err.println("Invalid Input"); 
            }
        } else {
	    int i=1;
	    boolean generate = false;
	    if(args[1].equals("-g")) {
		generate=true;
		i=2;
	    }
	    for(; i<args.length; i++) {
		showResults(args[i], fstLookup.lookup(args[i], generate));
	    }
        }
        long iEnd=System.currentTimeMillis();
        System.err.println("processed in " + (iEnd - iBegin) + " ms.");
    }

    
    public static void showResults(String query, String[] args) {
        System.out.println("---- "+args.length+" result(s) for "+query+":");
        int i;
        for(i=0; i<args.length; i++)
            System.out.println(args[i]);
        System.out.println();
    }
}


