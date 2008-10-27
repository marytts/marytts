/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.signalproc.analysis;

import java.io.IOException;

import marytts.signalproc.window.Window;
import marytts.util.io.MaryRandomAccessFile;

/**
 * 
 * @author oytun.turk
 *
 * Implements a structured header with file I/O functionality 
 * for binary files that store frame based line spectral frequency vectors 
 * 
 */
public class LsfFileHeader extends FeatureFileHeader {
    public float preCoef; //Preemphasis coefficient
    public int windowType; //Type of analysis window (See class marytts.signalproc.window.Window for details
    
    public LsfFileHeader()
    {
        super();
        
        preCoef = 0.0f;
        windowType = Window.HAMMING;
    }
    
    public LsfFileHeader(LsfFileHeader existingHeader)
    {
        super((FeatureFileHeader)existingHeader);

        preCoef = existingHeader.preCoef;
        windowType = existingHeader.windowType;
    }
    
    public LsfFileHeader(String lsfFile)
    {
        super(lsfFile);
        
        try {
            readHeader(lsfFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public boolean isIdenticalAnalysisParams(LsfFileHeader hdr)
    {
        if (!(hdr instanceof LsfFileHeader))
            return false;
        
        boolean bRet = super.isIdenticalAnalysisParams((FeatureFileHeader)hdr);
       
        if (bRet==false)
            return false;

        if (this.preCoef!=((LsfFileHeader)hdr).preCoef)
            return false;
        if (this.windowType!=((LsfFileHeader)hdr).windowType)
            return false;
        
        return bRet;
    }
    
    public void readHeader(MaryRandomAccessFile stream, boolean bLeaveStreamOpen) throws IOException
    {
        if (stream!=null)
        {
            super.readHeader(stream, true);
            
            preCoef = stream.readFloat();
            windowType = stream.readInt();
            
            if (!bLeaveStreamOpen)
            {
                stream.close();
                stream = null;
            }
        }
    }
    
    public void writeHeader(MaryRandomAccessFile ler) throws IOException
    {   
        super.writeHeader(ler);
        
        ler.writeFloat(preCoef);
        ler.writeInt(windowType);
    }
}
