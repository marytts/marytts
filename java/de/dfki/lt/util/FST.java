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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * An implementation of a finite state transducer. This class does nothing but
 * load and represent the FST. It is used by other classes doing something
 * reasonable with it.
 * @author Andreas Eisele
 */
public class FST
{
    // The following variables are package-readable, so that they can be
    // directly accessed by all classes in this package.
    int[] targets;
    short[] labels;
    boolean[] isLast;
    
    short[] offsets;
    byte[] bytes;
    int[] mapping;
    ArrayList strings=new ArrayList();

    /**
     * Initialise the finite state transducer. This constructor will
     * assume that the file uses the system default encoding.
     * @param fileName the name of the file from which to load the FST.
     * @throws IOException if the FST cannot be loaded from the given file.
     */
    public FST(String fileName) throws IOException
    {
        try {
            load(fileName, null, false);
        } catch (UnsupportedEncodingException e) {
            // default encoding not supported?! shouldn't happen
            e.printStackTrace();
        }
    }

    /**
     * Initialise the finite state transducer.
     * @param fileName the name of the file from which to load the FST.
     * @param encoding the name of the encoding used in the file (e.g., UTF-8
     * or ISO-8859-1).
     * @throws IOException if the FST cannot be loaded from the given file.
     * @throws UnsupportedEncodingException if the encoding is not supported.
     */
    public FST(String fileName, String encoding)
    throws IOException, UnsupportedEncodingException
    {
        load(fileName, encoding, false);
    }

    /**
     * Initialise the finite state transducer. This constructor will
     * assume that the file uses the system default encoding.
     * @param fileName the name of the file from which to load the FST.
     * @param verbose whether to write a report to stderr after loading.
     * @throws IOException if the FST cannot be loaded from the given file.
     */
    public FST(String fileName, boolean verbose) throws IOException
    {
        try {
            load(fileName, null, verbose);
        } catch (UnsupportedEncodingException e) {
            // default encoding not supported?! shouldn't happen
            e.printStackTrace();
        }
    }

    /**
     * Initialise the finite state transducer.
     * @param fileName the name of the file from which to load the FST.
     * @param encoding the name of the encoding used in the file (e.g., UTF-8
     * or ISO-8859-1).
     * @param verbose whether to write a report to stderr after loading.
     * @throws IOException if the FST cannot be loaded from the given file.
     * @throws UnsupportedEncodingException if the encoding is not supported.
     */
    public FST(String fileName, String encoding, boolean verbose)
    throws IOException, UnsupportedEncodingException
    {
        load(fileName, encoding, verbose);
    }

    private void load(String fileName, String encoding, boolean verbose)
    throws IOException, UnsupportedEncodingException
    {
        File f=new File(fileName);
        int i;
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        int fileSize=(new Long(f.length())).intValue();
        int nArcs=in.readInt();
        // arcs = new int[nArcs];
        
        targets = new int[nArcs];
        labels = new short[nArcs];
        isLast = new boolean[nArcs];
        
        for(i=0; i<nArcs; i++) {
            int thisArc = in.readInt();
            
            targets[i]= thisArc&1048575;
            labels[i]=(short)((thisArc>>20) & 2047);
            isLast[i]=((byte)(thisArc >> 31))!=0;
            
        }
        
        int nPairs=in.readInt();
        offsets = new short[2*nPairs];
        for(i=0; i<2*nPairs; i++)
            offsets[i] = in.readShort();
        int nBytes = fileSize - 8 - 4 * (nPairs + nArcs);
        mapping=new int[nBytes];
        bytes = new byte[nBytes];
        in.readFully(bytes);
        if(verbose) {
            System.err.println("FST (" 
                               + fileSize + " Bytes, "
                               + nArcs + " Arcs, " 
                               + nPairs + " Labels)"
                               + " loaded from " + fileName);
        }
        in.close();
        createMapping(mapping, bytes, encoding);
    }
    
    private void createMapping(int[] mapping, byte[] bytes, String encoding)
    throws UnsupportedEncodingException
    {
	mapping[0]=0;
	int last0=-1;
	String s;
	int len;
	for (int i=0;i<bytes.length;i++) {
            if (bytes[i]==0) {
                len=i-last0-1;
                if (len==0) strings.add(new String());
                else {
                    String str;
                    if (encoding != null) 
                        str = new String(bytes, last0+1, len, encoding);
                    else
                        str = new String(bytes, last0+1, len);
                    strings.add(str);
                }
                mapping[last0+1]=strings.size()-1;
                last0=i;
            }
        }
    }    
}


