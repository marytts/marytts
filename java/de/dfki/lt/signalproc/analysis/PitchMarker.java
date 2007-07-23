package de.dfki.lt.signalproc.analysis;

public class PitchMarker {

    public int [] pitchMarks;
    public boolean [] vuvs;
    public int totalZerosToPadd;
    
    //count=total pitch marks
    public PitchMarker(int count, int [] pitchMarksIn, boolean [] vuvsIn, int totalZerosToPaddIn)
    {
        if (count>1)
        {
            pitchMarks = new int[count];
            vuvs = new boolean[count-1];
        
            System.arraycopy(pitchMarksIn, 0, pitchMarks, 0, Math.min(pitchMarksIn.length, count));
            System.arraycopy(vuvsIn, 0, vuvs, 0, Math.min(vuvsIn.length, count-1));
        }
        else
        {
            pitchMarks = null;
            vuvs = null;
        }
        
        totalZerosToPadd = Math.max(0, totalZerosToPaddIn);
    }
}
