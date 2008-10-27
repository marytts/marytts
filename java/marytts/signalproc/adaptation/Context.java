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

package marytts.signalproc.adaptation;

import marytts.signalproc.analysis.Labels;
import marytts.util.string.StringUtils;


/**
 * @author oytun.turk
 *
 * A wrapper class for representing phonetic context
 * 
 */
public class Context {
    public int numLeftNeighbours;
    public int numRightNeighbours;
    public String allContext;
    public String[] leftContexts;
    public String currentContext;
    public String[] rightContexts;
    private double[] scores;
    public static final char leftContextSeparator = '.';
    public static final char rightContextSeparator = ',';
    
    public static final double LOWEST_CONTEXT_SCORE = 1.0;
    
    public Context(Context existing)
    {
        numLeftNeighbours = existing.numLeftNeighbours;
        numRightNeighbours = existing.numRightNeighbours;
        allContext = existing.allContext;
        setLeftContext(existing.leftContexts);
        currentContext = existing.currentContext;
        setRightContext(existing.rightContexts);
        
        setScores();
        setAllContext();
    }
    
    //Create a full context entry using a concatenated allContext entry
    public Context(String allContextIn)
    {
        allContext = allContextIn;
        parseAllContext();
    }
    
    public Context(Labels labels, int currentLabelIndex, int totalNeighbours)
    {
        this(labels, currentLabelIndex, totalNeighbours, totalNeighbours);
    }
    
    // leftContexts[0]                      = labels.items[currentLabelIndex-totalLeftNeighbours].phn
    // leftContexts[1]                      = labels.items[currentLabelIndex-totalLeftNeighbours+1].phn
    // ...
    // leftContexts[totalLeftNeighbours-2]  = labels.items[currentLabelIndex-2].phn
    // leftContexts[totalLeftNeighbours-1]  = labels.items[currentLabelIndex-1].phn
    // currentContext                       = labels.items[currentLabelIndex].phn
    // rightContexts[0]                     = labels.items[currentLabelIndex+1].phn
    // rightContexts[1]                     = labels.items[currentLabelIndex+2].phn
    // ...
    // rightContexts[totalRightNeighbours-2] = labels.items[currentLabelIndex+totalRightNeighbours-1].phn
    // rightContexts[totalRightNeighbours-1] = labels.items[currentLabelIndex+totalRightNeighbours].phn
    // 
    // Note that non-existing context entries are represented by ""
    public Context(Labels labels, int currentLabelIndex, int totalLeftNeighbours, int totalRightNeighbours)
    {
        leftContexts = null;
        rightContexts = null;
        currentContext = "";
        int i;
        
        if (totalLeftNeighbours>0)
        {
            leftContexts = new String[totalLeftNeighbours];
            
            for (i=totalLeftNeighbours; i>0; i--)
            {
                if (labels!=null && currentLabelIndex-i>=0)
                    leftContexts[totalLeftNeighbours-i] = labels.items[currentLabelIndex-i].phn;
                else
                    leftContexts[totalLeftNeighbours-i] = "";
            }
        }
        
        currentContext = labels.items[currentLabelIndex].phn;
            
        if (totalRightNeighbours>0)
        {
            rightContexts = new String[totalRightNeighbours];
            
            for (i=0; i<totalRightNeighbours; i++)
            {
                if (labels!=null && currentLabelIndex+i+1<labels.items.length)
                    rightContexts[i] = labels.items[currentLabelIndex+i+1].phn;
                else
                    rightContexts[i] = "";
            }
        }
        
        setScores();
        setAllContext();
    }
    
    public void setLeftContext(String[] leftContextIn)
    {
        leftContexts = null;
        if (leftContextIn!=null)
        {
            leftContexts = new String[leftContextIn.length];
            for (int i=0; i<leftContextIn.length; i++)
            {
                leftContexts[i] = leftContextIn[i];
            }
        }
    }
    
    public void setRightContext(String[] rightContextIn)
    {
        rightContexts = null;
        if (rightContextIn!=null)
        {
            rightContexts = new String[rightContextIn.length];
            for (int i=0; i<rightContextIn.length; i++)
            {
                rightContexts[i] = rightContextIn[i];
            }
        }
    }
    
    public void setScores()
    {
        int maxContext = 0;
        
        if (leftContexts!=null)
            maxContext = leftContexts.length;
        
        if (rightContexts!=null)
            maxContext = Math.max(maxContext, rightContexts.length);
            
        scores = new double[maxContext+1];
        
        double tmpSum = LOWEST_CONTEXT_SCORE;
        for (int i=0; i<maxContext+1; i++)
        {
            scores[i] = tmpSum;
            tmpSum = 2*tmpSum+1;
        }
    }
    
    public double[] getPossibleScores()
    {
        double[] possibleScores = null;
        if (scores!=null)
        {
            possibleScores = new double[2*(scores.length-1)+1];
            double tmpSum = 0.0;
            for (int i=0; i<scores.length-2; i++)
            {
                possibleScores[2*i] = tmpSum + scores[i];
                possibleScores[2*i+1] = tmpSum + 2*scores[i];
                tmpSum += 2*scores[i];
            }
            possibleScores[2*(scores.length-1)] = tmpSum + scores[scores.length-1];
        }
        
        return possibleScores;
    }
    
    //allContext = L[0].L[1]...L[N-2].L[N-1].C,R[0],R[1],R[2],...,R[N-1]
    // where L:leftContexts, C:currentContext, R:rightContexts, and "." "," are the left and rightContextSeparators respectively
    public void setAllContext()
    {
        allContext = "";
        
        int i;
        for (i=0; i<leftContexts.length; i++)
            allContext += leftContexts[i] + leftContextSeparator;
        
        allContext += currentContext + rightContextSeparator;
        
        for (i=0; i<rightContexts.length-1; i++)
            allContext += rightContexts[i] + rightContextSeparator;
        
        allContext += rightContexts[rightContexts.length-1];
    }
    
    public void parseAllContext()
    {
        int[] leftInds = StringUtils.find(allContext, leftContextSeparator);
        int i, start;
        if (leftInds!=null)
        {
            leftContexts = new String[leftInds.length];
            start = 0;
            for (i=0; i<leftInds.length; i++)
            {
                leftContexts[i] = allContext.substring(start, leftInds[i]);
                start = leftInds[i]+1;
            }
        }
        else
            leftContexts = null;
        
        int[] rightInds = StringUtils.find(allContext, rightContextSeparator);
        if (rightInds!=null)
        {
            rightContexts = new String[rightInds.length];
            for (i=0; i<rightInds.length-1; i++)
                rightContexts[i] = allContext.substring(rightInds[i]+1, rightInds[i+1]);
            
            rightContexts[rightInds.length-1] = allContext.substring(rightInds[rightInds.length-1]+1, allContext.length());
        }
        else
            rightContexts = null;
        
        if (leftInds!=null)
        {
            if (rightInds!=null)
                currentContext = allContext.substring(leftInds[leftInds.length-1]+1, rightInds[0]);
            else
                currentContext = allContext.substring(leftInds[leftInds.length-1]+1, allContext.length());
        }
        else
        {
            if (rightInds!=null)
                currentContext = allContext.substring(0, rightInds[0]);
            else
                currentContext = allContext;
        }
        
        setScores();
    }
    
    public double matchScore(Context context)
    {
        assert leftContexts.length == context.leftContexts.length;
        assert rightContexts.length == context.rightContexts.length;
        
        int i;
        double score = 0.0;
        if (currentContext.compareTo(context.currentContext)==0)
        {
            score += scores[scores.length-1]; //current context
            for (i=leftContexts.length-1; i>=0; i--)
            {
                if (leftContexts[i].compareTo(context.leftContexts[i])==0)
                    score += scores[i];
                else
                    break;
            }
            
            for (i=0; i<rightContexts.length; i++)
            {
                if (rightContexts[i].compareTo(context.rightContexts[i])==0)
                    score += scores[scores.length-2-i];
                else
                    break;
            }
        }
        
        return score;
    }
    
    public static void main(String[] args)
    {
        Labels labels1 = new Labels(5);
        Labels labels2 = new Labels(5);
        
        labels1.items[0].phn = "A";
        labels1.items[1].phn = "B";
        labels1.items[2].phn = "C";
        labels1.items[3].phn = "D";
        labels1.items[4].phn = "E";
        
        labels2.items[0].phn = "A1";
        labels2.items[1].phn = "B";
        labels2.items[2].phn = "C";
        labels2.items[3].phn = "D1";
        labels2.items[4].phn = "E1";
        
        Context c1 = new Context(labels1, 2, 2);
        Context c2 = new Context(labels2, 2, 2);
        
        System.out.println(String.valueOf(c1.matchScore(c2)));
        
        Context c3 = new Context("t.u.nl,i,n");
        System.out.println(c3.currentContext);
        
        double[] possibleScores = c1.getPossibleScores();
        
        System.out.println("Test completed");
    }
}
