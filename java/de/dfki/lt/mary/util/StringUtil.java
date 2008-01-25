package de.dfki.lt.mary.util;

import java.io.File;
import java.util.Arrays;
import java.util.StringTokenizer;

import de.dfki.lt.signalproc.util.ESTLabel;

public class StringUtil {
    
    //Removes blanks in the beginning and at the end of a string
    public static String deblank(String str) 
    {
          StringTokenizer s = new StringTokenizer(str," ",false);
          String strRet = "";
          
          while (s.hasMoreElements()) 
              strRet += s.nextElement();
          
          return strRet;
    }
    
    //Converts a String to a float
    public static float String2Float(String str)
    {
        return Float.valueOf(str).floatValue();
    }
    
    //Converts a String to a double
    public static double String2Double(String str)
    {
        return Double.valueOf(str).doubleValue();
    }
    
    //Converts a String to an int
    public static int String2Int(String str)
    {
        return Integer.valueOf(str).intValue();
    }
      
    //Find indices of multiple occurrences of a character in a String
    public static int[] find(String str, char ch, int stInd, int enInd)
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
    
    public static int[] find(String str, char ch, int stInd)
    {
        return find(str, ch, stInd, str.length()-1);
    }
    
    public static int[] find(String str, char ch)
    {
        return find(str, ch, 0, str.length()-1);
    }
    
    //Check last folder separator character and append it if it does not exist
    public static String checkLastSlash(String strIn)
    {
        String strOut = strIn;
        
        char last = strIn.charAt(strIn.length()-1);
        
        if (last != File.separatorChar)
            strOut = strOut + File.separatorChar;
        
        return strOut;
    }
    
   //Check first file extension separator character and add it if it does not exist
    public static String checkFirstDot(String strIn)
    {
        String strOut = strIn;
        
        char extensionSeparator = '.';
        
        char first = strIn.charAt(0);
        
        if (first != extensionSeparator)
            strOut = extensionSeparator + strOut;
        
        return strOut;
    }
    
    //Default start index is 1
    public static String[] indexedNameGenerator(String preName, int numFiles)
    {
        return indexedNameGenerator(preName, numFiles, 1);
    }
    
    public static String[] indexedNameGenerator(String preName, int numFiles, int startIndex)
    {
        return indexedNameGenerator(preName, numFiles, startIndex, "");
    }
    
    public static String[] indexedNameGenerator(String preName, int numFiles, int startIndex, String postName)
    {
        return indexedNameGenerator(preName, numFiles, startIndex, postName, ".tmp");
    }
    
    public static String[] indexedNameGenerator(String preName, int numFiles, int startIndex, String postName, String extension)
    {
        int numDigits = 0;
        if (numFiles>0)
            numDigits = (int)Math.floor(Math.log10(startIndex+numFiles-1));
        
        return indexedNameGenerator(preName, numFiles, startIndex, postName, extension, numDigits);
    }
    
    //Generate a list of files in the format:
    // <preName>startIndex<postName>.extension
    // <preName>startIndex+1<postName>.extension
    // <preName>startIndex+2<postName>.extension
    // ...
    // The number of required characters for the largest index is computed automatically if numDigits<required number of characters for the largest index
    // The minimum value of startIndex is 0 (negative values are converted to zero)
    public static String[] indexedNameGenerator(String preName, int numFiles, int startIndex, String postName, String extension, int numDigits)
    {
        String[] fileList = null;
        
        if (numFiles>0)
        {
            if (startIndex<0)
                startIndex = 0;
            
            int tmpDigits = (int)Math.floor(Math.log10(startIndex+numFiles-1));
            if (tmpDigits>numDigits)
                numDigits=tmpDigits;
            
            fileList = new String[numFiles];
  
            String strNum;

            for (int i=startIndex; i<startIndex+numFiles; i++)
            {
                strNum = String.valueOf(i);
                
                //Add sufficient 0´s in the beginning
                while (strNum.length()<numDigits)
                    strNum = "0" + strNum;
                //
                
                fileList[i-startIndex] = preName + strNum + postName + extension;
            }
        }
        
        return fileList;
    }
    
    public static String modifyExtension(String strFilename, String desiredExtension)
    {
        String strNewname = strFilename;
        String desiredExtension2 = checkFirstDot(desiredExtension);
        
        int lastDotIndex = strNewname.lastIndexOf('.');
        strNewname = strNewname.substring(0, lastDotIndex) + desiredExtension2;
        
        return strNewname;
    }
    
  //This version assumes that there can only be insertions and deletions but no substitutions 
    // (i.e. text based alignment with possible differences in pauses only)
    public static int[] alignLabels(ESTLabel[] seq1, ESTLabel[] seq2)
    {
        return alignLabels(seq1, seq2, 0.05, 0.05, 0.0);
    }
    
    //Finds the optimum alignment between two label sequences by considering insertions, deletions, and skippings
    // using simple edit distance
    // Deletion, insertion and substitution probabilities
    public static int[] alignLabels(ESTLabel[] seq1, ESTLabel[] seq2, double PDeletion, double PInsertion, double PSubstitution)
    {
        int[] labelMap = null;
        double PCorrect = 1.0-(PDeletion+PInsertion+PSubstitution);
        double TINY = 1e-30;

        PCorrect = PCorrect+TINY;
        PDeletion = PDeletion+TINY;
        PInsertion = PInsertion+TINY;
        PSubstitution = PSubstitution+TINY;
        double summ = PCorrect+PDeletion+PInsertion+PSubstitution;
        PCorrect = PCorrect/summ;
        PDeletion = PDeletion/summ;
        PInsertion = PInsertion/summ;
        PSubstitution = PSubstitution/summ;

        int n = seq1.length;
        int m = seq2.length;
        if (n!=0 && m!=0)
        {
            double[][] d = new double[n+1][m+1];
            int[][] p = new int[n+1][m+1];
            int i, j;
            for (i=0; i<n+1; i++)
            {
                for (j=0; j<m+1; j++)
                {
                    d[i][j] = TINY;
                    p[i][j] = 0;
                }
            }
    
            double c;
            double maxVal, tmpVal;
            int event;
            
            d[0][0] = 1.0;
            for (i=1; i<=n; i++)
                d[i][0] = d[i-1][0]*PDeletion;
  
            for (j=1; j<=m; j++)
                d[0][j] = d[0][j-1]*PInsertion;
    
            for (i=1; i<=n; i++)
            {
                for (j=1; j<=m; j++)
                {
                    if (seq1[i-1].phn.compareTo(seq2[j-1].phn)==0)
                        c = PCorrect;
                    else
                        c = PSubstitution;

                    maxVal = d[i-1][j]*PDeletion;
                    event = 1;
                    tmpVal = d[i][j-1]*PInsertion;
                    if (tmpVal>maxVal)
                    {
                        maxVal = tmpVal;
                        event = 2;
                    }
                    
                    tmpVal = d[i-1][j-1]*c;
                    if (tmpVal>maxVal)
                    {
                        maxVal = tmpVal;
                        event = 3;
                    }

                    if (event==3 && seq1[i-1].phn.compareTo(seq2[j-1].phn)==0)
                        event = 4;
 
                    //1:Deletion, 2:Insertion, 3:Substitution, 4:Correct
                    p[i][j] = event;
                }
            }
            
            // Backtracking
            int k = 1;
            int[] E = new int[(n+1)*(m+1)];
            
            E[k-1] = p[n][m];
            i=n+1;
            j=m+1;
            int t=m;
            while (true)
            {
                if (E[k-1]==3 || E[k-1]==4)
                {
                    i=i-1;
                    j=j-1;
                }
                else if (E[k-1]==2)
                    j=j-1;
                else if (E[k-1]==1)
                    i=i-1;
                
                if (p[i-1][j-1]==0)
                {
                    while (j>1)
                    {
                        k=k+1;
                        j=j-1;
                        E[k-1] = 2;
                    }
                    break;
                }
                else
                {
                    k=k+1;
                    E[k-1]=p[i-1][j-1];
                }
                t=t-1;
            }
            
            // Reverse the order
            int[] Events = new int[k];
            for (t=k; t>=1; t--)
                Events[t-1] = E[k-t];  
            //
            
            int ind = 0;
            labelMap = new int[Events.length];
            
            for (i=0; i<Events.length; i++)
            {
                if (Events[i]==1) //Deletion
                    labelMap[i] = -Events[i];
                else if (Events[i]==2) //Insertion
                {
                    labelMap[i] = -Events[i];
                    ind++;  
                }
                else if (Events[i]==3 || Events[i]==4) //You may want to discriminate between Substitution(3) and Correct(4) here
                {
                    labelMap[i] = ind;
                    ind++;
                }
            }
        }

        return labelMap;
    }
    
    public static boolean isNumeric(String str) 
    {
        for (int i=0; i<str.length(); i++)
        {
            char ch = str.charAt(i);
            if (!Character.isDigit(ch) && ch!='.') 
                return false;
        }
        
        return true;
    }
    
    //Retrieves filename from fullpathname
    // Also works for removing file extension from a filename with extension
    public static String getFileName(String fullpathFilename, boolean bRemoveExtension)
    {
        String filename = "";
        
        int ind1 = fullpathFilename.lastIndexOf('\\');
        int ind2 = fullpathFilename.lastIndexOf('/');
        
        ind1 = Math.max(ind1, ind2);
        
        if (ind1>=0 && ind1<fullpathFilename.length()-2)
            filename = fullpathFilename.substring(ind1+1);
        
        if (bRemoveExtension)
        {
            ind1 = filename.lastIndexOf('.');
            if (ind1>0 && ind1-1>=0)
                filename = filename.substring(0, ind1);
        }
        
        return filename;
    }
    
    public static String getFileName(String fullpathFilename)
    {
        return getFileName(fullpathFilename, true);
    }
}
