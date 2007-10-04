package de.dfki.lt.mary.util;

import java.util.StringTokenizer;

public class StringUtil {
    
    //Removes blanks in the beginning and at the end of a string
    static public String deblank(String str) 
    {
          StringTokenizer s = new StringTokenizer(str," ",false);
          String strRet = "";
          
          while (s.hasMoreElements()) 
              strRet += s.nextElement();
          
          return strRet;
    }
    
    //Converts a String to a float
    static public float String2Float(String str)
    {
        return Float.valueOf(str).floatValue();
    }
    
    //Converts a String to a double
    static public double String2Double(String str)
    {
        return Double.valueOf(str).doubleValue();
    }
    
    //Converts a String to an int
    static public int String2Int(String str)
    {
        return Integer.valueOf(str).intValue();
    }
      
    //Find indices of multiple occurrences of a character in a String
    static public int[] find(String str, char ch, int stInd, int enInd)
    {
        int [] indices = null;
        int i;
        int count = 0;
        
        if (stInd<0)
            stInd = 0;
        if (stInd>str.length()-1)
            stInd=str.length()-1;
        if (enInd<stInd)
            enInd=stInd;
        if (enInd>str.length()-1)
            enInd=str.length()-1;
        
        for (i=stInd; i<=enInd; i++)
        {
            if (str.charAt(i)==ch)
                count++;
        }
        
        if (count>0)
            indices = new int[count];
        
        int total = 0;
        for (i=stInd; i<=enInd; i++)
        {
            if (str.charAt(i)==ch && total<count)
                indices[total++] = i; 
        }
        
        return indices;
    }
    
    static public int[] find(String str, char ch, int stInd)
    {
        return find(str, ch, stInd, str.length()-1);
    }
    
    static public int[] find(String str, char ch)
    {
        return find(str, ch, 0, str.length()-1);
    }
}
