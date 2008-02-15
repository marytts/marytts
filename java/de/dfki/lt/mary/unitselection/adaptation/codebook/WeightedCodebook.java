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

package de.dfki.lt.mary.unitselection.adaptation.codebook;

import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchStatistics;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchStatisticsCollection;
import de.dfki.lt.mary.unitselection.adaptation.prosody.PitchStatisticsMapping;

/**
 * @author oytun.turk
 *
 */
public class WeightedCodebook {
    public WeightedCodebookLsfEntry[] lsfEntries;
    public WeightedCodebookFileHeader header;
    
    //These two contain identical information in different forms
    //f0Statistics is always read from the codebook first
    //Then f0StatisticsMapping is created from f0Statistics using the function setF0StatisticsMapping()
    public PitchStatisticsCollection f0StatisticsCollection;
    public PitchStatisticsMapping f0StatisticsMapping;
    //
    
    public WeightedCodebook()
    {
        this(0, 0);
    }
    
    public WeightedCodebook(int totalLsfEntriesIn, int totalF0StatisticsIn)
    {
        if (header==null)
            header = new WeightedCodebookFileHeader(totalLsfEntriesIn, totalF0StatisticsIn);
        
        allocate(); 
    }
    
    public void allocate()
    {
        allocate(header.totalLsfEntries, header.totalF0StatisticsEntries);
    }
    
    public void allocate(int totalLsfEntriesIn, int totalF0StatisticsIn)
    {
       if (totalLsfEntriesIn>0)
       {
           lsfEntries = new WeightedCodebookLsfEntry[totalLsfEntriesIn];
           header.totalLsfEntries = totalLsfEntriesIn;
       }
       else
       {
           lsfEntries = null;
           header.totalLsfEntries = 0;
       }
       
       if (totalF0StatisticsIn>0)
       {
           f0StatisticsCollection = new PitchStatisticsCollection(totalF0StatisticsIn);
           header.totalF0StatisticsEntries = totalF0StatisticsIn;
       }
       else
       {
           f0StatisticsCollection = null;
           header.totalF0StatisticsEntries = 0;
       }
    }
    
    public void setF0StatisticsMapping()
    {
        if (f0StatisticsCollection!=null)
            f0StatisticsMapping = new PitchStatisticsMapping(f0StatisticsCollection);
        else
            f0StatisticsMapping = null;
    }
}
