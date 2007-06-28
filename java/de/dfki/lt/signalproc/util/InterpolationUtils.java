package de.dfki.lt.signalproc.util;

public class InterpolationUtils {
   
    // This funciton is NOT an interpolation function
    // It just repeats/removes entries in x to create y that is of size newLen
    static public double [] modifySize(double [] x, int newLen)
    {
        double [] y = null;
        
        if (newLen<1)
            return y;

        if (x.length==newLen || newLen==1)
        {
            y = new double[x.length];
            System.arraycopy(x, 0, y, 0, x.length);
            return y;
        }
        else
        {
            y = new double[newLen];
            int mappedInd;
            int i;
            for (i=1;i<=newLen; i++)
            { 
                mappedInd = (int)(Math.floor((i-1.0)/(newLen-1.0)*(x.length-1.0)+1.5));
                if (mappedInd<1)
                    mappedInd=1;
                else if (mappedInd>x.length)
                    mappedInd=x.length; 

                y[i-1] = x[mappedInd-1];
            }
            
            return y;
        } 
    }
}
