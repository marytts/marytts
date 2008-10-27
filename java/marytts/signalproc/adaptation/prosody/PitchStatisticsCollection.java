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

package marytts.signalproc.adaptation.prosody;


/**
 * @author oytun.turk
 *
 */
public class PitchStatisticsCollection {
    public PitchStatistics[] entries;
    
    public PitchStatisticsCollection()
    {
        this(0);
    }
    
    public PitchStatisticsCollection(int numEntries)
    {
        allocate(numEntries);
    }
    
    public PitchStatisticsCollection(PitchStatisticsCollection existing)
    {
        allocate(existing.entries.length);
        
        for (int i=0; i<existing.entries.length; i++)
            entries[i] = new PitchStatistics(existing.entries[i]);
    }
    
    public void allocate(int numEntries)
    {
        entries = null;
        if (numEntries>0)
            entries = new PitchStatistics[numEntries];
    }
    
    public PitchStatistics getGlobalStatisticsSourceHz()
    {
        return getGlobalStatistics(PitchStatistics.STATISTICS_IN_HERTZ, true);
    }

    public PitchStatistics getGlobalStatisticsTargetHz()
    {
        return getGlobalStatistics(PitchStatistics.STATISTICS_IN_HERTZ, false);
    }
    
    public PitchStatistics getGlobalStatisticsSourceLogHz()
    {
        return getGlobalStatistics(PitchStatistics.STATISTICS_IN_LOGHERTZ, true);
    }

    public PitchStatistics getGlobalStatisticsTargetLogHz()
    {
        return getGlobalStatistics(PitchStatistics.STATISTICS_IN_LOGHERTZ, false);
    }

    public PitchStatistics getGlobalStatistics(int statisticsType, boolean isSource)
    {
        PitchStatistics p = null;
        
        PitchStatisticsCollection c = getStatistics(true, statisticsType, isSource);
        if (c!=null)
            p = new PitchStatistics(c.entries[0]);
        
        return p;
    }
    
    public PitchStatisticsCollection getLocalStatisticsSourceHz()
    {
        return getLocalStatistics(PitchStatistics.STATISTICS_IN_HERTZ, true);
    }
    
    public PitchStatisticsCollection getLocalStatisticsTargetHz()
    {
        return getLocalStatistics(PitchStatistics.STATISTICS_IN_HERTZ, false);
    }
    
    public PitchStatisticsCollection getLocalStatisticsSourceLogHz()
    {
        return getLocalStatistics(PitchStatistics.STATISTICS_IN_LOGHERTZ, true);
    }
    
    public PitchStatisticsCollection getLocalStatisticsTargetLogHz()
    {
        return getLocalStatistics(PitchStatistics.STATISTICS_IN_LOGHERTZ, false);
    }
    
    public PitchStatisticsCollection getLocalStatistics(int statisticsType, boolean isSource)
    {
        return getStatistics(false, statisticsType, isSource);
    }
    
    public PitchStatisticsCollection getStatistics(boolean isGlobal, int statisticsType, boolean isSource)
    {
        PitchStatisticsCollection c = null;
        
        if (entries!=null)
        {
            int total = 0;
            int i;
            for (i=0; i<entries.length; i++)
            {
                if (entries[i].isGlobal==isGlobal && entries[i].type==statisticsType && entries[i].isSource==isSource)
                    total++; 
            }
            
            if (total>0)
            {
                c = new PitchStatisticsCollection(total);
                
                int count = 0;
                for (i=0; i<entries.length; i++)
                {
                    if (entries[i].isGlobal==isGlobal && entries[i].type==statisticsType && entries[i].isSource==isSource && count<total)
                    {
                        c.entries[count] = new PitchStatistics(entries[i]);
                        count++;
                    }
                }
                
            }
        }
        
        return c;
    }
}
