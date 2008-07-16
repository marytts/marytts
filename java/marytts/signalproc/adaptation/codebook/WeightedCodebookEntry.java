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

package marytts.signalproc.adaptation.codebook;

import marytts.util.io.MaryRandomAccessFile;

/**
 * 
 * @author oytun.turk
 *
 * Wrapper class for a single weighted codebook entry
 * 
 */
public class WeightedCodebookEntry {
    public WeightedCodebookSpeakerItem sourceItem;
    public WeightedCodebookSpeakerItem targetItem;
    
    public WeightedCodebookEntry()
    {
        this(0, 0);
    }
    
    public WeightedCodebookEntry(int lpOrder, int mfccDimension)
    {
        allocate(lpOrder, mfccDimension);
    }
    
    public WeightedCodebookEntry(double[] sourceLsfs, double[] targetLsfs, double[] sourceMfccs, double[] targetMfccs)
    {
        int lsfDimension = 0;
        int mfccDimension = 0;
        
        if (sourceLsfs!=null && targetLsfs!=null)
        {
            assert sourceLsfs.length==targetLsfs.length;
            lsfDimension = sourceLsfs.length;
        }

        if (sourceMfccs!=null && targetMfccs!=null)
        {
            assert sourceMfccs.length==targetMfccs.length;
            mfccDimension = sourceMfccs.length;
        }

        allocate(lsfDimension, mfccDimension);
        
        if (lsfDimension>0)
            setLsfs(sourceLsfs, targetLsfs);
        
        if (mfccDimension>0)
            setMfccs(sourceMfccs, targetMfccs);
    }
    
    public void allocate(int lpOrder, int mfccDimension)
    {
        if (lpOrder>0 || mfccDimension>0)
        {
            sourceItem = new WeightedCodebookSpeakerItem(lpOrder, mfccDimension);
            targetItem = new WeightedCodebookSpeakerItem(lpOrder, mfccDimension);
        }
        else
        {
            sourceItem = null;
            targetItem = null;
        }
    }
    
    public void setLsfs(double[] srcLsfs, double[] tgtLsfs)
    {
        sourceItem.setLsfs(srcLsfs);
        targetItem.setLsfs(tgtLsfs);
    }
    
    public void setMfccs(double[] srcMfccs, double[] tgtMfccs)
    {
        sourceItem.setMfccs(srcMfccs);
        targetItem.setMfccs(tgtMfccs);
    }
    
    public void write(MaryRandomAccessFile ler)
    {
        if (sourceItem!=null && targetItem!=null)
        {
            sourceItem.write(ler);
            targetItem.write(ler);
        }
    }
    
    public void read(MaryRandomAccessFile ler, int lpOrder, int mfccDimension)
    {
        sourceItem = new WeightedCodebookSpeakerItem();
        sourceItem.read(ler, lpOrder, mfccDimension);
        
        targetItem = new WeightedCodebookSpeakerItem();
        targetItem.read(ler, lpOrder, mfccDimension);
    }
}
