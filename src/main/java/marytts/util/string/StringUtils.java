/**
 * Copyright 2000-2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.util.string;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import java.util.Vector;

import marytts.modules.phonemiser.Allophone;
import marytts.signalproc.analysis.Label;
import marytts.util.io.FileUtils;
import marytts.util.math.ComplexArray;
import marytts.util.math.ComplexNumber;


public class StringUtils {
    
    //Removes blanks in the beginning and at the end of a string
    public static String deblank(String str) 
    {
          StringTokenizer s = new StringTokenizer(str," ",false);
          StringBuilder strRet = new StringBuilder();
          
          while (s.hasMoreElements()) 
              strRet.append(s.nextElement());
          
          return strRet.toString();
    }
    
    //Converts a String to a float
    public static float string2float(String str)
    {
        return Float.valueOf(str).floatValue();
    }
    
    //Converts a String to a double
    public static double string2double(String str)
    {
        return Double.valueOf(str).doubleValue();
    }
    
    //Converts a String to an int
    public static int string2int(String str)
    {
        return Integer.valueOf(str).intValue();
    }
    
    public static float[] string2float(String[] strs)
    {
        float[] values = null;
        if (strs!=null && strs.length>0)
        {
            values = new float[strs.length];
            for (int i=0; i<strs.length; i++)
                values[i] = string2float(strs[i]);
        }
        
        return values;
    }
    
    public static double[] string2double(String[] strs)
    {
        double[] values = null;
        if (strs!=null && strs.length>0)
        {
            values = new double[strs.length];
            for (int i=0; i<strs.length; i++)
                values[i] = string2double(strs[i]);
        }
        
        return values;
    }
    
    public static int[] string2int(String[] strs)
    {
        int[] values = null;
        if (strs!=null && strs.length>0)
        {
            values = new int[strs.length];
            for (int i=0; i<strs.length; i++)
                values[i] = string2int(strs[i]);
        }
        
        return values;
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
        if (str.length() == 0) return null;
        return find(str, ch, 0, str.length()-1);
    }
    
    //Check last folder separator character and append it if it does not exist
    public static String checkLastSlash(String strIn)
    {
        String strOut = strIn;
        
        char last = strIn.charAt(strIn.length()-1);
        
        if (last != File.separatorChar && last != '\\' && last != '/')
            strOut = strOut + "/";
        
        return strOut;
    }
    
    public static String removeLastSlash(String strIn)
    {
        String strOut = strIn;
        
        while (true)
        {
            char last = strOut.charAt(strOut.length()-1);

            if (last== File.separatorChar || last== '\\' || last== '/')
                strOut = strOut.substring(0, strOut.length()-1);
            else
                break;
        }

        return strOut;
    }
    
    /**
     * Purge non-breaking spaces from <b>input</b> by replacing them with regular spaces.
     * 
     * @param input
     *            to purge
     * @return purged <b>input</b>
     */
    public static String purgeNonBreakingSpaces(String input) {
        String output = input.replaceAll("\\xA0", " ");
        return output;
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
    
    /**
     * 
     * @param strFilename
     * @param isIncludeDot
     * @return
     * @deprecated use {@link org.apache.commons.io.FilenameUtils#getExtension(String)} instead
     */
    @Deprecated
    public static String getFileExtension(String strFilename, boolean isIncludeDot)
    {
        int lastDotIndex = strFilename.lastIndexOf('.');
        String strExtension = "";
        if (lastDotIndex>-1)
        {
            if (isIncludeDot)
                strExtension = strFilename.substring(lastDotIndex, strFilename.length());
            else
                strExtension = strFilename.substring(lastDotIndex+1, strFilename.length());
        }
        
        return strExtension;
    }
    
    
    //This version assumes that there can only be insertions and deletions but no substitutions 
    // (i.e. text based alignment with possible differences in pauses only)
    public static int[][] alignLabels(Label[] seq1, Label[] seq2)
    {
        return alignLabels(seq1, seq2, 0.05, 0.05, 0.05);
    }
    
    public static int[][] alignLabels(Label[] labs1, Label[] labs2, double PDeletion, double PInsertion, double PSubstitution)
    {
        double PCorrect = 1.0-(PDeletion+PInsertion+PSubstitution);
        int n = labs1.length;
        int m = labs2.length;
        double D;
        int[][] labelMap = null;

        if (n==0 || m==0)
        {
            D=m;
            return labelMap;
        }

        int i, j;
        double[][] d = new double[n+1][m+1];
        for (i=0; i<d.length; i++)
        {
            for (j=0; j<d[i].length; j++)
                d[i][j] = 0.0;
        }

        int[][] p = new int[n+1][m+1];
        for (i=0; i<p.length; i++)
        {
            for (j=0; j<p[i].length; j++)
                p[i][j] = 0;
        }

        double z = 1;
        d[0][0] = z;
        for (i=1; i<=n; i++)
            d[i][0] = d[i-1][0]*PDeletion;

        for (j=1; j<=m; j++)
            d[0][j] = d[0][j-1]*PInsertion;

        String strEvents = "DISC";
        double c;
        double tmp;
        for (i=1; i<=n; i++)
        {
            for (j=1; j<=m; j++)
            {
                if (labs1[i-1].phn.compareTo(labs2[j-1].phn)==0)
                    c = PCorrect;
                else
                    c = PSubstitution;

                int ind = 1;
                d[i][j] = d[i-1][j]*PDeletion;
                tmp = d[i][j-1]*PInsertion;
                if (tmp>d[i][j])
                {
                    d[i][j] = tmp;
                    ind = 2;
                }

                tmp = d[i-1][j-1]*c;
                if (tmp>d[i][j])
                {
                    d[i][j] = tmp;
                    ind = 3;
                }

                if (ind==3 && labs1[i-1].phn.compareTo(labs2[j-1].phn)==0)
                    ind = 4;

                //Events 1:Deletion, 2:Insertion, 3:Substitution, 4:Correct
                p[i][j] = ind;
            }
        }

        //Backtracking
        D = d[n][m];
        int k = 1;
        int[] E = new int[m*n];
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
                    E[k-1]=2;
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

        //Reverse the order
        int[] Events = new int[k];
        for (t=k; t>=1; t--)
            Events[t-1] = E[k-t];
        
        int[][] tmpLabelMap = new int[n*m][2];
        int ind = 0;
        int ind1 = 0;
        int ind2 = 0;
        for (t=1; t<=k; t++)
        {
            if (Events[t-1]==3 || Events[t-1]==4) //Substitution or correct
            {
                tmpLabelMap[ind][0] = ind1;
                tmpLabelMap[ind][1] = ind2;
                ind1++;
                ind2++;
                ind++;
            }
            else if (Events[t-1]==1) //An item in seq1 is deleted in seq2
            {
                ind1++;
            }
            else if (Events[t-1]==2) //An item is inserted in seq2
            {
                ind2++;
            }
        }
        
        if (ind>0)
        {
            labelMap = new int[ind][2];
            for (i=0; i<labelMap.length; i++)
            {
                labelMap[i][0] = tmpLabelMap[i][0];
                labelMap[i][1] = tmpLabelMap[i][1];
            }
        }

        return labelMap;
    }
    
    public static int findInMap(int[][] map, int ind0)
    {
        for (int i=0; i<map.length; i++)
        {
            if (map[i][0]==ind0)
                return map[i][1];
        }
        
        return -1;
    }
    
    public static int findInMapReverse(int[][] map, int ind1)
    {
        for (int i=0; i<map.length; i++)
        {
            if (map[i][1]==ind1)
                return map[i][0];
        }
        
        return -1;
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
        if (ind1<0)
            ind1=-1;
        
        if (ind1<fullpathFilename.length()-2)
            filename = fullpathFilename.substring(ind1+1);
        
        if (bRemoveExtension)
        {
            ind1 = filename.lastIndexOf('.');
            if (ind1>=1)
                filename = filename.substring(0, ind1);
        }
        
        return filename;
    }
    
    public static String getFileName(String fullpathFilename)
    {
        return getFileName(fullpathFilename, true);
    }
    
    /**
     * 
     * @param fullpathFilename
     * @return
     * @deprecated use {@link org.apache.commons.io.FilenameUtils#getFullPath(String)} instead
     */
    @Deprecated
    public static String getFolderName(String fullpathFilename)
    {
        String foldername = "";
        
        int ind1 = fullpathFilename.lastIndexOf('\\');
        int ind2 = fullpathFilename.lastIndexOf('/');
        
        ind1 = Math.max(ind1, ind2);
        
        if (ind1>=0 && ind1<fullpathFilename.length()-2)
            foldername = fullpathFilename.substring(0, ind1+1);
        
        return foldername;
    }
    
    //Reads all rows as one String
    public static String[] readTextFile(String textFile)
    {
        String[][] tmp = readTextFileInRows(textFile, "ASCII", 1);
        
        String[] strRet = new String[tmp.length];
        for (int i=0; i<tmp.length; i++)
            strRet[i] = tmp[i][0];
        
        return strRet;
    }
    
    public static String[] readTextFile(String textFile, String encoding)
    {
        String[][] tmp = readTextFileInRows(textFile, encoding, 1);
        
        String[] strRet = new String[tmp.length];
        for (int i=0; i<tmp.length; i++)
            strRet[i] = tmp[i][0];
        
        return strRet;
    }
    
    public static String[][] readTextFileInRows(String textFile, String encoding, int minimumItemsInOneLine)
    {
        String[][] entries = null;
        String allText = readTextFileIntoString(textFile, encoding);
        
        if (allText!=null)
        {
            String[] lines = allText.split("\n");

            entries = parseFromLines(lines, minimumItemsInOneLine, 0, lines.length-1);
        }
        
        return entries;
    }
    
    /**
     * Read text file as a single concatenated string
     * @param textFile
     * @param encoding
     * @return
     * @deprecated use {@link org.apache.commons.io.FileUtils#readFileToString(File, String)} instead
     */
    @Deprecated
    public static String readTextFileIntoString(String textFile, String encoding)
    {
        String allText = null;
        try {
            allText = FileUtils.getFileAsString(new File(textFile), encoding);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return allText;
    }
   
    public static String[][] parseFromLines(String[] lines, int minimumItemsInOneLine, int startLine, int endLine)
    {
        String[][] labels = null;
        String[][] labelsRet = null;

        if (startLine<=endLine)
        {
            int i, j;
            int count = 0;
            for (i=startLine; i<=endLine; i++)
            {
                String[] labelInfos = null;
                if (minimumItemsInOneLine>1)
                {
                    labelInfos = lines[i].split(" ");
                }
                else
                {
                    labelInfos = new String[1];
                    labelInfos[0] = lines[i];
                }
                
                boolean isNotEmpty = false;
                for (j=0; j<labelInfos.length; j++)
                {
                    labelInfos[j] = labelInfos[j].trim();
                    if (labelInfos[j].length()!=0)
                        isNotEmpty = true;
                }
                
                if (labelInfos.length>0 && isNotEmpty)
                    count++;
            }

            int tmpCount = 0;
            if (count>0)
            {
                labels = new String[count][];
                for (i=startLine; i<=endLine; i++)
                {
                    if (tmpCount>count-1)
                        break;

                    String[] labelInfos = null;
                    if (minimumItemsInOneLine>1)
                    {
                        labelInfos = lines[i].split(" ");
                    }
                    else
                    {
                        labelInfos = new String[1];
                        labelInfos[0] = lines[i];
                    }
                    
                    boolean isNotEmpty = false;
                    for (j=0; j<labelInfos.length; j++)
                    {
                        labelInfos[j] = labelInfos[j].trim();
                        if (labelInfos[j].length()!=0)
                            isNotEmpty = true;
                    }
                    
                    if (labelInfos.length>0 && isNotEmpty)
                    {
                        labels[tmpCount] = new String[minimumItemsInOneLine];
                        for (j=0; j<Math.min(labelInfos.length, minimumItemsInOneLine); j++) 
                            labels[tmpCount][j] = labelInfos[j].trim();

                        tmpCount++;
                    }
                }

                labelsRet = new String[tmpCount][];
                for (i=0; i<tmpCount; i++)
                {
                    labelsRet[i] = new String[minimumItemsInOneLine];
                    for (j=0; j<minimumItemsInOneLine; j++)
                        labelsRet[i][j] = labels[i][j];
                }
            }
        }
        
        return labelsRet;
    }
    
    public static int[] getDifferentItemsList(int[] items)
    {
        int[] differentItems = null;
        int[] indices = getDifferentItemsIndices(items);
        
        if (indices!=null)
        {
            differentItems = new int[indices.length];
            for (int i=0; i<indices.length; i++)
                differentItems[i] = items[indices[i]];
        }
        
        return differentItems;
    }
    
    public static int[] getDifferentItemsIndices(int[] items)
    {
        String[] strItems = new String[items.length];
        
        for (int i=0; i<items.length; i++)
            strItems[i] = String.valueOf(items[i]);
        
        return getDifferentItemsIndices(strItems);
    }
    
    public static String[] getDifferentItemsList(String[] items)
    {
        String[] differentItems = null;
        int[] indices = getDifferentItemsIndices(items);
        
        if (indices!=null)
        {
            differentItems = new String[indices.length];
            for (int i=0; i<indices.length; i++)
                differentItems[i] = items[indices[i]];
        }
        
        return differentItems;
    }
    
    public static int[] getDifferentItemsIndices(String[] items)
    {
        int[] differentItemIndices = null;

        if (items!=null)
        {
            int[] tmpDifferentItemIndices = new int[items.length];
            int differentCount = 1;
            int i, j;
            tmpDifferentItemIndices[0] = 0;
            boolean bDifferent;
            for (i=1; i<items.length; i++)
            {
                bDifferent = true;
                for (j=0; j<differentCount; j++)
                {
                    if (items[i].compareTo(items[tmpDifferentItemIndices[j]])==0)
                    {
                        bDifferent=false;
                        break;
                    }
                }

                if (bDifferent)
                {
                    tmpDifferentItemIndices[differentCount] = i;
                    differentCount++;

                    if (differentCount>=items.length)
                        break;
                }
            }

            differentItemIndices = new int[differentCount];
            System.arraycopy(tmpDifferentItemIndices, 0, differentItemIndices, 0, differentCount);
        }

        return differentItemIndices;
    }
    
    public static boolean isDesired(int currentFeature, int desiredFeatures)
    {
        return isDesired(currentFeature, desiredFeatures, 0);
    }
    
    public static boolean isDesired(int currentFeature, int desiredFeatures, int maxFeatureStringLen)
    {
        boolean bRet;
        
        String str1 = Integer.toBinaryString(desiredFeatures);
        String str2 = Integer.toBinaryString(currentFeature);

        if (maxFeatureStringLen<str1.length())
            maxFeatureStringLen = str1.length();
        if (maxFeatureStringLen<str2.length())
            maxFeatureStringLen = str2.length();
        
        while (str1.length()<maxFeatureStringLen)
            str1 = "0" + str1;
        
        while (str2.length()<maxFeatureStringLen)
            str2 = "0" + str2;
        
        bRet = false;
        for (int i=0; i<str1.length(); i++)
        {
            if (Integer.valueOf(String.valueOf(str1.charAt(i)))==1 && Integer.valueOf(String.valueOf(str2.charAt(i)))==1)
            {
                bRet = true;
                break;
            }
        }
        
        return bRet;
    }
    
    public static String getRandomName(int randomNameLength)
    {
        return getRandomName(null, randomNameLength);
    }
    
    public static String getRandomName(String preName, int randomNameLength)
    {
        return getRandomName(preName, randomNameLength, null);
    }
    
    public static String getRandomName(String preName, int randomNameLength, String postName)
    {
        String randomName = "";
        while (randomName.length()<randomNameLength)
            randomName += String.valueOf((int)(10*Math.random()));

        if (preName!=null)
            randomName = preName + randomName;
        
        if (postName!=null)
            randomName += postName;
        
        return randomName;
    }
    
    public static String getRandomFileName(String preName, int randomNameLength, String fileExtension)
    {
        if (fileExtension.charAt(0)!='.')
            fileExtension = "." + fileExtension;
        
        return getRandomName(preName, randomNameLength, fileExtension);
    }
    
    public static boolean isOneOf(String item, String[] list)
    {
        boolean isFound = false;
        for (int i=0; i<list.length; i++)
        {
            if (item.compareTo(list[i])==0)
            {
                isFound = true;
                break;
            }
        }
            
        return isFound;
    }
    
    public static String[] toStringArray(String allInOneLine)
    {
        if (allInOneLine!="")
        {
            Vector<String> result = new Vector<String>();

            StringTokenizer s = new StringTokenizer(allInOneLine, System.getProperty("line.separator"));
            String line = null;
            // Read until either end of file or an empty line
            while(s.hasMoreTokens() && ((line = s.nextToken())!=null) && (!line.equals("")))
                result.add(line);

            return result.toArray(new String[0]);
        }
        else
            return null;
    }
    
    public static InputStream toInputStream(String str)
    {
        ByteArrayInputStream stream = null;
        try {
            stream = new ByteArrayInputStream(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return stream;
    }
    
    public static InputStream toInputStream(String[] stringArray)
    {
        return toInputStream(stringArray, 0);
    }
    
    public static InputStream toInputStream(String[] stringArray, int startIndex)
    {
        return toInputStream(stringArray, startIndex, stringArray.length);
    }
    
    public static InputStream toInputStream(String[] stringArray, int startIndex, int endIndex)
    {
        String str = toString(stringArray, startIndex, endIndex);
        
        return toInputStream(str);
    }
    
    /**
     * Combine the elements of the given string array into a single string,
     * containing one array element per line.
     * @param stringArray
     * @return
     */
    public static String toString(String[] stringArray)
    {
        return toString(stringArray, 0);
    }
    
    /**
     * Combine the elements of the given string array into a single string,
     * containing one array element per line.
     * @param stringArray
     * @param startIndex
     * @return
     */
    public static String toString(String[] stringArray, int startIndex)
    {
        return toString(stringArray, startIndex, stringArray.length-1);
    }
    
    /**
     * Combine the elements of the given string array into a single string,
     * containing one array element per line.
     * @param stringArray
     * @param startIndex
     * @param endIndex
     * @return
     */
    public static String toString(String[] stringArray, int startIndex, int endIndex)
    {
        if (startIndex<0)
            startIndex=0;
        if (startIndex>stringArray.length-1)
            startIndex=stringArray.length-1;
        if (endIndex<startIndex)
            endIndex=startIndex;
        if (endIndex>stringArray.length-1)
            endIndex=stringArray.length-1;
        
        StringBuilder str = new StringBuilder();
        for (int i = startIndex; i <= endIndex; i++) {
            str.append(stringArray[i]).append(System.getProperty("line.separator"));
        }
        
        return str.toString();
    }
    
    public static String replace(String str, String pattern, String replacement) 
    {
        int s = 0;
        int e = 0;
        StringBuilder result = new StringBuilder();
    
        while ((e = str.indexOf(pattern, s)) >= 0) 
        {
            result.append(str.substring(s, e));
            result.append(replacement);
            s = e+pattern.length();
        }
        
        result.append(str.substring(s));
        
        return result.toString();
    }

    public static String urlEncode(String strRequest)
    {
        String encoded = strRequest;
        
        try {
            encoded = URLEncoder.encode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return encoded;
    }
    
    
    public static String urlDecode(String strRequest)
    {
        //decoded = StringUtils.replace(strRequest, "%20", " ");  

        String decoded = strRequest;
        try {
            decoded = URLDecoder.decode(decoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //decoded = StringUtils.replace(decoded, "_HTTPREQUESTLINEBREAK_", System.getProperty("line.separator"));

        return decoded;
    }
    
    /**
     * Divides the example text of a voice into
     * sentences in a vector
     * @param text the example text
     * @return vector of example sentences
     */
    public static Vector<String> processVoiceExampleText(String text)
    {
        StringTokenizer st = new StringTokenizer(text,"#");
        Vector<String> sentences = null;
        
        while (st.hasMoreTokens())
        {
            if (sentences==null)
                sentences = new Vector<String>();
            
            sentences.add(st.nextToken());
        }
        
        return sentences;
    }
    
    public static String toString(ComplexNumber[][] array)
    {
        String str = "";
        int i, j;
        for (i=0; i<array.length; i++)
        {
            for (j=0; j<array[i].length; j++)
            {
                str += array[i][j].toString();
                if (j<array[i].length-1)
                    str += " ";
            }
            str += System.getProperty("line.separator");
        }
        
        str += System.getProperty("line.separator");
        
        return str;
    }
    
    public static String toString(double[][] array)
    {
        String str = "";
        int i, j;
        for (i=0; i<array.length; i++)
        {
            for (j=0; j<array[i].length; j++)
            {
                str += String.valueOf(array[i][j]);
                if (j<array[i].length-1)
                    str += " ";
            }
            str += System.getProperty("line.separator");
        }
        
        str += System.getProperty("line.separator");
        
        return str;
    }
    
    /**
     * Determine whether the given codepoint is either a letter or
     * a modifier according to the Unicode standard. More precisely,
     * this returns true if codepoint belongs to one of the following categories
     * as defined at http://unicode.org/Public/UNIDATA/UCD.html#General_Category_Values:
     * <ul>
     * <li>Lu   Letter, Uppercase</li>
     * <li>Ll  Letter, Lowercase</li>
     * <li>Lt  Letter, Titlecase</li>
     * <li>Lm  Letter, Modifier</li>
     * <li>Lo  Letter, Other</li>
     * <li>Mn  Mark, Nonspacing</li>
     * <li>Mc  Mark, Spacing Combining</li>
     * <li>Me  Mark, Enclosing</li>
     * </ul>
     * Whether a given character is associated with this category can be looked up
     * at http://unicode.org/Public/UNIDATA/UnicodeData.txt
     * @param codePoint the unicode codepoint as determined e.g. by String.codePointAt().
     * @return true if the above condition is met, false otherwise
     */
    public static boolean isLetterOrModifier(int codePoint)
    {
        int type = Character.getType(codePoint);
        return type == Character.UPPERCASE_LETTER
            || type == Character.LOWERCASE_LETTER
            || type == Character.TITLECASE_LETTER
            || type == Character.MODIFIER_LETTER
            || type == Character.OTHER_LETTER
            || type == Character.NON_SPACING_MARK
            || type == Character.COMBINING_SPACING_MARK
            || type == Character.ENCLOSING_MARK;
    }
    
    public static String[] toStringLines(double[] x)
    {
        String[] y = null;
        
        if (x!=null && x.length>0)
        {
            y = new String[x.length];
            for (int i=0; i<x.length; i++)
                y[i] = String.valueOf(x[i]);
        }
        
        return y;
    }
    
    public static String[] toStringLines(float[] x)
    {
        String[] y = null;
        
        if (x!=null && x.length>0)
        {
            y = new String[x.length];
            for (int i=0; i<x.length; i++)
                y[i] = String.valueOf(x[i]);
        }
        
        return y;
    }
    
    public static String[] toStringLines(int[] x)
    {
        String[] y = null;
        
        if (x!=null && x.length>0)
        {
            y = new String[x.length];
            for (int i=0; i<x.length; i++)
                y[i] = String.valueOf(x[i]);
        }
        
        return y;
    }
    
    public static String[] toStringLines(ComplexNumber[] x)
    {
        String[] y = null;
        
        if (x!=null && x.length>0)
        {
            y = new String[x.length];
            for (int i=0; i<x.length; i++)
            {
                if (x[i].imag>=0)
                    y[i] = String.valueOf(x[i].real) + "+i*" + String.valueOf(x[i].imag);
                else
                    y[i] = String.valueOf(x[i].real) + "-i*" + String.valueOf(Math.abs(x[i].imag)); 
            }
        }
        
        return y;
    }
    
    public static String[] toStringLines(ComplexArray x)
    {
        String[] y = null;
        
        if (x!=null && x.real.length>0 && x.imag.length>0)
        {
            assert x.real.length==x.imag.length;
            y = new String[x.real.length];
            for (int i=0; i<x.real.length; i++)
            {
                if (x.imag[i]>=0)
                    y[i] = String.valueOf(x.real[i]) + "+i*" + String.valueOf(x.imag[i]);
                else
                    y[i] = String.valueOf(x.real[i]) + "-i*" + String.valueOf(Math.abs(x.imag[i])); 
            }
        }
        
        return y;
    }

    public static void main(String[] args)
    {
        String[] items1 = readTextFile("D:\\items.txt", "ASCII");
        int[] inds1 = StringUtils.getDifferentItemsIndices(items1);
        String[] diffItems1 = StringUtils.getDifferentItemsList(items1);
        
        int[] items2 = {1, 2, 3, 4, 1, 1, 2, 2, 4, 4, 10};
        int[] inds2 = StringUtils.getDifferentItemsIndices(items2);
        int[] diffItems2 = StringUtils.getDifferentItemsList(items2);
        
        System.out.println("Test completed....");
    }
}

