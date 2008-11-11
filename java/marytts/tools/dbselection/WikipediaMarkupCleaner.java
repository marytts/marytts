package marytts.tools.dbselection;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;

public class WikipediaMarkupCleaner {

    public String removeMarKup(String page, boolean debug) {
        StringBuffer str = new StringBuffer("");
        StringBuffer line = null;
               
        boolean endOfText=false;
        Scanner s = null;
        try {
          s = new Scanner(page);
          while (s.hasNext() && !endOfText) {
              
           line = new StringBuffer(s.nextLine()); 
           // process text until it finds any of these labels:
           if( line.indexOf("==References")>=0 || line.indexOf("== References")>=0 || 
                   line.indexOf("==See also")>=0 || line.indexOf("== See also")>=0 ||
                   line.indexOf("==External links and sources")>=0 || line.indexOf("==External links")>=0 || line.indexOf("== External links")>=0 ||
                   line.indexOf("== External links and sources")>=0 ||
                   line.indexOf("==Notes")>=0 || line.indexOf("== Notes")>=0 ||
                   line.indexOf("==Sources")>=0 || line.indexOf("== Sources")>=0 ||
                   line.indexOf("==Foreign")>=0 || line.indexOf("== Foreign")>=0 ||
                   line.indexOf("==Discussion")>=0  ){
                   endOfText=true;
           } else {
               
             if( line.indexOf("<noinclude") >= 0)
                   line = removeSection(s, line, "<noinclude", "</noinclude>", debug);
             
             if( line.indexOf("<includeonly") >= 0)
                 line = removeSection(s, line, "<includeonly", "</includeonly>", debug);
             
             if( line.indexOf("<onlyinclude") >= 0)
                 line = removeSection(s, line, "<includeonly", "</onlyinclude>", debug);
             
             if( line.indexOf("<TABLE") >= 0)
                 line = removeSection(s, line, "<TABLE", "</TABLE>", debug);
              
             if( line.indexOf("{|") >= 0)  // this hould go before {{ because a table can contain {{ }}
                 line = removeSectionTable(s, line, "{|", "|}", debug);  
             
             if( line.indexOf("<noinclude") >= 0)  // after table normally there is another noinclude
                 line = removeSection(s, line, "<noinclude", "</noinclude>", debug);
               
             if( line.indexOf("<ref") >= 0)
               line = removeSectionRef(s, line);  // This is special because it can be <ref>, <ref, </ref> or />
             if( line.indexOf("<REF") >= 0)
                 line = removeSection(s, line, "<REF", "</REF>", debug);
             if( line.indexOf("<Ref") >= 0)
                 line = removeSection(s, line, "<Ref", "</Ref>", debug);
            
             if( line.indexOf("{{start box}}") >= 0)
                 line = removeSection(s, line, "{{start box}}", "{{end box}}", debug);
             
             if( line.indexOf("{{") >= 0)
               line = removeSection(s, line, "{{", "}}", debug);  
           
             if( line.indexOf("<!--") >= 0)
                 line = removeSection(s, line, "<!--", "-->", debug);
             
             if( line.indexOf("\\mathrel{|") >= 0)
                 line = removeSection(s, line, "\\mathrel{|", "}", debug);
                         
             if( line.indexOf("<gallery") >= 0)  // gallery might contain several images
                 line = removeSection(s, line, "<gallery", "</gallery>", debug);
                   
            if( line.indexOf("[[Image:") >= 0)
                 line = removeSectionImage1(s, line, "[[Image:", "]]", debug);
                        
             if( line.indexOf("<math>") >= 0)
                 line = removeSection(s, line, "<math>", "</math>", debug);
             
             if( line.indexOf("<timeline>") >= 0)
                 line = removeSection(s, line, "<timeline>", "</timeline>", debug);
             
             if( line.indexOf("<table") >= 0)
                 line = removeSection(s, line, "<table", "</table>", debug);
             
             if( line.indexOf("<div") >= 0)
                 line = removeSection(s, line, "<div", "</div>", debug);
             
             if( line.indexOf("<nowiki") >= 0)
                 line = removeSection(s, line, "<nowiki", "</nowiki>", debug);
             
             if( line.indexOf("<source") >= 0)
                 line = removeSection(s, line, "<source", "</source>", debug);
             
             if( line.indexOf("<code") >= 0)
                 line = removeSection(s, line, "<code", "</code>", debug);
             
             if( line.indexOf("<imagemap") >= 0)
                 line = removeSection(s, line, "<imagemap", "</imagemap>", debug);
             
             if( line.indexOf("<code") >= 0)
                 line = removeSection(s, line, "<code", "</code>", debug);
             
             if( line.indexOf("<poem") >= 0)
                 line = removeSection(s, line, "<poem", "</poem>", debug);
                       
             if( line.indexOf("<span") >= 0)
                 line = removeSection(s, line, "<poem", "</span>", debug);
             
             if( line.indexOf("<h1") >= 0)
                 line = removeSection(s, line, "<h1", "</h1>", debug);
             
             // check again if after adding lines there is not additional ref
             if( line.indexOf("<ref") >= 0)
                 line = removeSectionRef(s, line);  // This is special because it can be <ref>, <ref, </ref> or />
             
             if( line.indexOf("<includeonly") >= 0)
                 line = removeSection(s, line, "<includeonly", "</includeonly>", debug);
             
             if( line.indexOf("<noinclude") >= 0)  // after table normally there is another noinclude
                 line = removeSection(s, line, "<noinclude", "</noinclude>", debug);
              
             
             // here filter bulleted and numbered short lines
             if( ( line.toString().startsWith("*") || 
                   line.toString().startsWith("#") ||
                   line.toString().startsWith(";") ||
                   line.toString().startsWith(".") ||
                   line.toString().startsWith(",") ||
                   line.toString().startsWith("&") ||
                   line.toString().startsWith("}") ||
                   line.toString().startsWith("]") || 
                   line.toString().startsWith("|") ||
                   line.toString().startsWith("ca:") ||
                   line.toString().startsWith("cs:") ||
                   line.toString().startsWith("de:") ||
                   line.toString().startsWith("es:") ||
                   line.toString().startsWith("fr:") ||
                   line.toString().startsWith("it:") ||
                   line.toString().startsWith("hu:") ||
                   line.toString().startsWith("ja:") ||
                   line.toString().startsWith("no:") ||
                   line.toString().startsWith("pt:") ||
                   line.toString().startsWith("sl:") ||
                   line.toString().startsWith("fi:") ||
                   line.toString().startsWith("sv:") ||
                   line.toString().startsWith("tr:") ||
                   line.toString().startsWith("zh:") ||
                   line.toString().startsWith("Category:") ||
                   line.toString().startsWith("!style=") || line.toString().startsWith("!  style=") ||
                   line.toString().startsWith("!align=") ||
                   line.toString().startsWith("::<code") ||
                   line.toString().endsWith("]]")
                   ) && line.length() < 200 )
               line = new StringBuffer("");
             
             // Now if the line is not empy, remove:
             //   '''''bold & italic'''''
             //   '''bold'''
             //   ''italic''
             // Internal links: 
             //   [[Name of page]]
             //   [[Name of page|Text to display]]
             // External links:
             //   [http://www.example.org Text to display]
             //   [http://www.example.org]
             //    http://www.example.org
             if(line.length() > 0) {
              
               line = new StringBuffer(line.toString().replaceAll("'''''", ""));              
               line = new StringBuffer(line.toString().replaceAll("'''", ""));
               line = new StringBuffer(line.toString().replaceAll("''", ""));
               
               line = processInternalAndExternalLinks(line, debug);
               
               
               // Make final replacements:
               // 
               line = new StringBuffer(line.toString().replaceAll("<big>", ""));
               line = new StringBuffer(line.toString().replaceAll("</big>", ""));
               line = new StringBuffer(line.toString().replaceAll("<blockquote>", ""));
               line = new StringBuffer(line.toString().replaceAll("</blockquote>", ""));
               line = new StringBuffer(line.toString().replaceAll("<BLOCKQUOTE>", ""));
               line = new StringBuffer(line.toString().replaceAll("</BLOCKQUOTE>", ""));
               line = new StringBuffer(line.toString().replaceAll("<sup>", ""));
               line = new StringBuffer(line.toString().replaceAll("</sup>", ""));
               line = new StringBuffer(line.toString().replaceAll("<sub>", ""));
               line = new StringBuffer(line.toString().replaceAll("</sub>", ""));
               line = new StringBuffer(line.toString().replaceAll("<small>", ""));
               line = new StringBuffer(line.toString().replaceAll("</small>", ""));
               line = new StringBuffer(line.toString().replaceAll("<ul>", ""));
               line = new StringBuffer(line.toString().replaceAll("</ul>", ""));
               line = new StringBuffer(line.toString().replaceAll("<br>", ""));
               line = new StringBuffer(line.toString().replaceAll("<br", ""));
               line = new StringBuffer(line.toString().replaceAll("/>", ""));
               line = new StringBuffer(line.toString().replaceAll("<center>", ""));
               line = new StringBuffer(line.toString().replaceAll("</center>", ""));
               line = new StringBuffer(line.toString().replaceAll("<li>", ""));
               line = new StringBuffer(line.toString().replaceAll("</li>", ""));
               line = new StringBuffer(line.toString().replaceAll("<dl>", ""));
               line = new StringBuffer(line.toString().replaceAll("</dl>", ""));
               line = new StringBuffer(line.toString().replaceAll("<dt>", ""));
               line = new StringBuffer(line.toString().replaceAll("</dt>", ""));
               line = new StringBuffer(line.toString().replaceAll("<dd>", ""));
               line = new StringBuffer(line.toString().replaceAll("</dd>", ""));
               line = new StringBuffer(line.toString().replaceAll("<b>", ""));
               line = new StringBuffer(line.toString().replaceAll("</b>", ""));
               line = new StringBuffer(line.toString().replaceAll("<p>", ""));
               line = new StringBuffer(line.toString().replaceAll("</p>", ""));
               line = new StringBuffer(line.toString().replaceAll("<u>", ""));
               line = new StringBuffer(line.toString().replaceAll("</u>", ""));
               line = new StringBuffer(line.toString().replaceAll("<tt>", ""));
               line = new StringBuffer(line.toString().replaceAll("</tt>", ""));
               
               // finally sections and lists
               line = new StringBuffer(line.toString().replaceAll("=====", ""));
               line = new StringBuffer(line.toString().replaceAll("====", ""));
               line = new StringBuffer(line.toString().replaceAll("===", ""));
               line = new StringBuffer(line.toString().replaceAll("==", ""));
               // bulleted list and numbered list
               if( line.toString().startsWith("***") || line.toString().startsWith("*#*") )
                  line.replace(0, 3, "");
               if( line.toString().startsWith("**") || line.toString().startsWith(":*") ||
                   line.toString().startsWith("*#") ||
                   line.toString().startsWith("##") || line.toString().startsWith("::") )
                   line.replace(0, 2, "");
               if( line.toString().startsWith("*") || line.toString().startsWith("#") )
                   line.replace(0, 1, "");
                 
               
               if( line.indexOf("<font") >= 0)
                   line = removeSection(s, line, "<font", ">", debug);
               line = new StringBuffer(line.toString().replaceAll("</font>", ""));
               
               // finally concatenate the line  
               str.append(line);
               if(!str.toString().endsWith("\n"))
                 str.append("\n");
            
             }
             
           } // endOfText=false
           
          }  // while has more lines
        
        } finally {
            if (s != null)
              s.close();
        }   
          
        return str.toString();  
      }
      
    // This is special because it can be:
    // <ref> ... </ref>
    // <ref  ... </ref>
    // <ref  ... />
    private StringBuffer removeSectionRef(Scanner s, StringBuffer lineIn){
        String next;
        int index1=0, index2=-1, index3=-1, endTagLength=0, numRef=0;
        boolean closeRef=true;
        StringBuffer line = new StringBuffer(lineIn);
        StringBuffer nextLine;
        
        //index1 = line.indexOf("<ref");
        
        
        while ( (index1 = line.indexOf("<ref")) >= 0 ) {  // in one line can be more than one reference
        numRef++;           
        if( (index2 = line.indexOf("</ref>", index1)) >= 0 )
          endTagLength = 6 + index2;
        else if( (index3 = line.indexOf("/>", index1)) >= 0 )
          endTagLength = 2 + index3;
          
        if(index2 == -1 && index3 == -1) {// the </ref> most be in the next lines, so get more lines until the </ref> is found
          while ( s.hasNext() && numRef!=0 ) {
             nextLine = new StringBuffer(s.nextLine());
             if( nextLine.indexOf("<ref") >= 0 )
               numRef++;  
             line.append(nextLine);
             if( (index2 = line.indexOf("</ref>", index1)) >= 0 ){
               numRef--;
               endTagLength=6+index2;
             } else if( (index3 = line.indexOf("/>", index1)) >= 0 ){
               numRef--;
               endTagLength=2+index3;
             }
          } 
          
        } else  // the endTag was found
           numRef--;
            
        if(numRef == 0) {
          index1 = line.indexOf("<ref"); // get again this because the positiom might change
          if( endTagLength > index1 ){
            line.delete(index1, endTagLength);
            //System.out.println("nextline="+line);
          } else {
                System.out.print("iniTag: <ref  index1=" + index1);
                System.out.print("  endTagLength=" + endTagLength);
                System.out.println("  line.length=" + line.length() + "  line: " + line);  
                System.out.println("removeSectionRef: WARNING endTagLength > length of line: " + line);
                //line.delete(index1, line.length());
                line = new StringBuffer("");
           }
        } else {
          System.out.println("removeSectionRef: WARNING no </ref> or /> in " + line);
          //line.delete(index1, line.length());
          line = new StringBuffer("");
        } 
        
        }  // while this line contains iniTag-s
        
        return line;  
        
    }
    
    private StringBuffer removeSection(Scanner s, StringBuffer lineIn, String iniTag, String endTag, boolean debug){
        String next;
        int index1=0, index2=-1, endTagLength=0, numRef=0, lastEndTag=0, lastIniTag=0;
        boolean closeRef=true;
        StringBuffer line = new StringBuffer(lineIn);
        StringBuffer nextLine;
        
        if(debug)
            System.out.println("Removing tag: " + iniTag + "  LINE (BEFORE): " + line);
        
        
        while ( (index1 = line.indexOf(iniTag)) >= 0 ) { // in one line can be more than one iniTag
            
        numRef++;           
        if( (index2 = line.indexOf(endTag, index1)) >= 0 )
          endTagLength = endTag.length() + index2;
          
        if(index2 == -1 ) {// the iniTag most be in the next lines, so get more lines until the endTag is found
          lastEndTag=0;  // start to look for the endTag in 0
          
          while ( s.hasNext() && numRef!=0 ) {
             lastIniTag=0; 
             nextLine = new StringBuffer(s.nextLine());
             //if(debug)
             //  System.out.println("  NEXTLINE: " + nextLine);
             
             while( (index1=nextLine.indexOf(iniTag, lastIniTag)) >= 0 ){
               numRef++;
               lastIniTag = iniTag.length() + index1;
             }
             
             line.append(nextLine);
              
             // next time it will look for the endTag after the position of the last it found.
             while( (index2 = line.indexOf(endTag, lastEndTag)) >= 0 ){
               numRef--;
               lastEndTag = index2 + endTag.length();  // I need to remember where the last endTag was found
               endTagLength = endTag.length() + index2;
             }
             
             //if(debug)
             //  System.out.println("LINE (numRef=" + numRef + "): " + line);
          } 
        } else  // the endTag was found
           numRef--;
            
        if(numRef == 0) {
          index1 = line.indexOf(iniTag); // get again this because the positiom might change
          if( endTagLength > index1 ){
            if(debug){ 
              System.out.println("    FINAL LINE: " + line);  
              //System.out.print("iniTag: " + iniTag + "  index1=" + index1);
              //System.out.print("  endTagLength=" + endTagLength);
              //System.out.println("  line.length=" + line.length() + "  line: " + line);
              //System.out.println("  line.length=" + line.length());
            }
            line.delete(index1, endTagLength);           
          }
          else{
            System.out.println("removeSection: WARNING endTagLength > length of line: ");  
            System.out.print("iniTag: " + iniTag + "  index1=" + index1);
            System.out.print("  endTagLength=" + endTagLength);
            System.out.println("  line.length=" + line.length() + "  line: " + line);  
            System.out.println("removeSection: WARNING endTagLength > length of line: " + line);
            //line.delete(index1, line.length());
            line = new StringBuffer("");
          } 
              
          //System.out.println("nextline="+line);
        } else {
          System.out.println("removeSection: WARNING no " + endTag);  
          System.out.println("removeSection: WARNING no " + endTag + " in line: " + line);
          //line.delete(index1, line.length());
          line = new StringBuffer("");
        } 
        
        }  // while this line contains iniTag-s
        
        if(debug)
            System.out.println("    LINE (AFTER): " + line);
        return line;  
    }

    private StringBuffer removeSectionTable(Scanner s, StringBuffer lineIn, String iniTag, String endTag, boolean debug){
        String next;
        int index1=0, index2=-1, endTagLength=0, numRef=0, lastEndTag=0, lastIniTag=0;
        boolean closeRef=true;
        StringBuffer line = new StringBuffer(lineIn);
        StringBuffer nextLine;
        
        if(debug)
            System.out.println("Removing tag: " + iniTag + "  LINE (BEFORE): " + line);
        
        
        while ( (index1 = line.indexOf(iniTag)) >= 0 ) { // in one line can be more than one iniTag
            
        numRef++;           
        if( (index2 = line.indexOf(endTag, index1)) >= 0 )
          endTagLength = endTag.length() + index2;
          
        if(index2 == -1 ) {// the iniTag most be in the next lines, so get more lines until the endTag is found
          lastEndTag=0;  // start to look for the endTag in 0
          
          while ( s.hasNext() && numRef!=0 ) {
             lastIniTag=0; 
             nextLine = new StringBuffer(s.nextLine());
             //if(debug)
             //  System.out.println("  NEXTLINE: " + nextLine);
             
             while( (index1=nextLine.indexOf(iniTag, lastIniTag)) >= 0 ){
               numRef++;
               lastIniTag = iniTag.length() + index1;
             }
             
            
              
             // next time it will look for the endTag after the position of the last it found.
             //while( (index2 = line.indexOf(endTag, lastEndTag)) >= 0 ){
             if( nextLine.toString().startsWith(endTag) ){
               numRef--;
               //index2 = line.length();
               //lastEndTag = index2 + endTag.length();  // I need to remember where the last endTag was found
               endTagLength = line.length() + endTag.length();
             }
             
             line.append(nextLine);
             
             //if(debug)
             //  System.out.println("LINE (numRef=" + numRef + "): " + line);
          } 
        } else  // the endTag was found
           numRef--;
            
        if(numRef == 0) {
          index1 = line.indexOf(iniTag); // get again this because the positiom might change
          if( endTagLength > index1 ){
            if(debug){ 
              System.out.println("    FINAL LINE: " + line);  
              //System.out.print("iniTag: " + iniTag + "  index1=" + index1);
              //System.out.print("  endTagLength=" + endTagLength);
              //System.out.println("  line.length=" + line.length() + "  line: " + line);
              //System.out.println("  line.length=" + line.length());
            }
            line.delete(index1, endTagLength);           
          }
          else{
            System.out.println("removeSection: WARNING endTagLength > length of line: ");  
            System.out.print("iniTag: " + iniTag + "  index1=" + index1);
            System.out.print("  endTagLength=" + endTagLength);
            System.out.println("  line.length=" + line.length() + "  line: " + line);  
            System.out.println("removeSection: WARNING endTagLength > length of line: " + line);
            //line.delete(index1, line.length());
            line = new StringBuffer("");
          } 
              
          //System.out.println("nextline="+line);
        } else {
          System.out.println("removeSection: WARNING no " + endTag);  
          System.out.println("removeSection: WARNING no " + endTag + " in line: " + line);
          //line.delete(index1, line.length());
          line = new StringBuffer("");
        } 
        
        }  // while this line contains iniTag-s
        
        if(debug)
            System.out.println("    LINE (AFTER): " + line);
        return line;  
    }
    
    private StringBuffer removeSectionImage(Scanner s, StringBuffer lineIn, String iniTag, String endTag, boolean debug){
        String next;
        int index1=0, index2=-1, index3=-1, endTagLength=0, numRef=0, lastEndTag1=0, lastIniTag=0;
        boolean closeRef=true;
        StringBuffer line = new StringBuffer(lineIn);
        StringBuffer nextLine;
        StringBuffer aux;
        
        if(debug)
            System.out.println("Removing tag: " + iniTag + "  LINE (BEFORE): " + line);
        
        
        while ( (index1 = line.indexOf(iniTag)) >= 0 ) { // in one line can be more than one iniTag
            
        numRef++; 
        index3 = endTagLength = index1;
        while( (index2 = line.indexOf("]]", endTagLength)) >= 0 && numRef>0 ){
          aux = new StringBuffer(line.subSequence(index1+2, index2)); 
          //System.out.println("aux=" + aux);
          if( (index3 = aux.indexOf("[[")) == -1 ){   
            endTagLength = endTag.length() + index2;
            numRef--;
          }
          else {  // There is a [[ ]]
            endTagLength = endTag.length() + index2; 
            index1 = index1 + index3;
            index2 = -1;
          }
        }
        
        if(index2 == -1 ) {// the iniTag most be in the next lines, so get more lines until the endTag is found
         //-- lastEndTag=endTagLength;  // start to look for the endTag in 0
          
          while ( s.hasNext() && numRef!=0 ) {
             lastIniTag=0; 
             nextLine = new StringBuffer(s.nextLine());
             //if(debug)
             //  System.out.println("  NEXTLINE: " + nextLine);
             
             while( (index1=nextLine.indexOf(iniTag, lastIniTag)) >= 0 ){
               numRef++;
               lastIniTag = iniTag.length() + index1;
             }
             
             line.append(nextLine);
              
             // next time it will look for the endTag after the position of the last it found.
             while( (index2 = line.indexOf("]]", endTagLength)) >= 0 && numRef>0) {
               aux = new StringBuffer(line.subSequence(index1+2, index2));    
               if( (index3 = line.indexOf("[[")) == -1 ){
                 numRef--;
                //-- lastEndTag = index2 + endTag.length();  // I need to remember where the last endTag was found
                 endTagLength = endTag.length() + index2;
               }
               else{  // There is a [[ ]]
                   endTagLength = endTag.length() + index3;
                   index1 = index1 + index3;
                   index2 = -1;
                 }
             }
             
             //if(debug)
             //  System.out.println("LINE (numRef=" + numRef + "): " + line);
          } 
        } //else  // the endTag was found
          // numRef--;
            
        if(numRef == 0) {
          index1 = line.indexOf(iniTag); // get again this because the positiom might change
          if( endTagLength > index1 ){
            if(debug){ 
              System.out.println("    FINAL LINE: " + line);  
              //System.out.print("iniTag: " + iniTag + "  index1=" + index1);
              //System.out.print("  endTagLength=" + endTagLength);
              //System.out.println("  line.length=" + line.length() + "  line: " + line);
              //System.out.println("  line.length=" + line.length());
            }
            line.delete(index1, endTagLength); 
            //System.out.println("line=" + line);
          }
          else{
            System.out.println("removeSection: WARNING endTagLength > length of line: ");  
            System.out.print("iniTag: " + iniTag + "  index1=" + index1);
            System.out.print("  endTagLength=" + endTagLength);
            System.out.println("  line.length=" + line.length() + "  line: " + line);  
            System.out.println("removeSection: WARNING endTagLength > length of line: " + line);
            //line.delete(index1, line.length());
            line = new StringBuffer("");
          } 
              
          //System.out.println("nextline="+line);
        } else {
          System.out.println("removeSection: WARNING no " + endTag);  
          System.out.println("removeSection: WARNING no " + endTag + " in line: " + line);
          //line.delete(index1, line.length());
          line = new StringBuffer("");
        } 
        
        }  // while this line contains iniTag-s
        
        if(debug)
            System.out.println("    LINE (AFTER): " + line);
        return line;  
    }
    
    
    /****
     * This is also special because the line might contain sections with [[ ...  ]] so the ]] after a [[
     * is not the endTag of [[image:  ... ]]
     * @param s
     * @param lineIn
     * @param iniTag
     * @param endTag
     * @param debug
     * @return
     */
    private StringBuffer removeSectionImage1(Scanner s, StringBuffer lineIn, String iniTag, String endTag, boolean debug){
        String next;
        int index1=0, index2=-1, index3=-1, endTagLength=0, numRef=0, lastEndTag1=0, lastIniTag=0;
        boolean closeRef=true;
        StringBuffer line = new StringBuffer(lineIn);
        StringBuffer nextLine;
        StringBuffer aux;
        
        if(debug)
            System.out.println("Removing tag: " + iniTag + "  LINE (BEFORE): " + line);
        
        
        while ( (index1 = line.indexOf(iniTag)) >= 0 ) { // in one line can be more than one iniTag
            
        numRef++; 
        index3 = endTagLength = index1;
        
        while ( s.hasNext() && numRef>0 ) {
        
          while( (index2 = line.indexOf("]]", endTagLength)) >= 0 && numRef>0 ){
            aux = new StringBuffer(line.subSequence(index1+2, index2+2)); 
            if(debug)
              System.out.println("    aux=" + aux);
            if( (index3 = aux.indexOf("[[")) == -1 ){   
              endTagLength = endTag.length() + index2;
              numRef--;
            }
            else {  // The previous was a [[ ]] inside of a [[Image: so it has to be deleted
              index1 = index2;
              endTagLength = index2+2; 
              index2 = -1;
            }
          }
          // so far it has not found the endTag, so get another line
          if(numRef>0)
            line.append(s.nextLine());
        } 
      
        if(numRef == 0) {
          index1 = line.indexOf(iniTag); // get again this because the positiom might change
          if( endTagLength > index1 ){
            if(debug){ 
              System.out.println("    FINAL LINE: " + line);  
              //System.out.print("iniTag: " + iniTag + "  index1=" + index1);
              //System.out.print("  endTagLength=" + endTagLength);
              //System.out.println("  line.length=" + line.length() + "  line: " + line);
              //System.out.println("  line.length=" + line.length());
            }
            line.delete(index1, endTagLength); 
            //System.out.println("line=" + line);
          }
          else{
            System.out.println("removeSection: WARNING endTagLength > length of line: ");  
            System.out.print("iniTag: " + iniTag + "  index1=" + index1);
            System.out.print("  endTagLength=" + endTagLength);
            System.out.println("  line.length=" + line.length() + "  line: " + line);  
            System.out.println("removeSection: WARNING endTagLength > length of line: " + line);
            //line.delete(index1, line.length());
            line = new StringBuffer("");
          } 
              
          //System.out.println("nextline="+line);
        } else {
          System.out.println("removeSection: WARNING no " + endTag);  
          System.out.println("removeSection: WARNING no " + endTag + " in line: " + line);
          //line.delete(index1, line.length());
          line = new StringBuffer("");
        } 
        
        }  // while this line contains iniTag-s
        
        if(debug)
            System.out.println("    LINE (AFTER): " + line);
        return line;  
    }
       
    // Internal links: 
    //   [[Name of page]]
    //   [[Name of page|Text to display]]
    // External links:
    //   [http://www.example.org Text to display]
    //   [http://www.example.org]
    //    http://www.example.org
    private StringBuffer  processInternalAndExternalLinks(StringBuffer line, boolean debug){
       int index1, index2, index3;
       StringBuffer linetmp=null;  // for debuging
       boolean changed = false;
       if(debug)
         linetmp = new StringBuffer(line);
       
       // Internal links:
       while ( (index1 = line.indexOf("[[")) >= 0 ) {
         changed=true;
         if( (index2 = line.indexOf("]]")) >= 0 ) {
            if ( ( index3 = line.indexOf("|", index1)) >= 0  && index3 < index2){  // if there is text to display
              line.delete(index1, index3+1);   // delete the link and [[ ]]
              index2 = line.indexOf("]]");     // since i delete some text i need to find egain the next ]]
              line.delete(index2, index2+2);
            } else {
              line.delete(index1, index1+2);   // delete the [[ 
              index2 = line.indexOf("]]");     // since i delete some text i need to find egain the next ]]
              line.delete(index2, index2+2);   // delete the ]]  -2 because in the previous line i deleted two chars
            }
            //if(debug)
            //  System.out.println("LINE (AFTER): " + line);    
             
         } else {
           System.out.println("processInternalAndExternalLinks: WARNING no ]] tag in " + line);
           System.out.println("deleting [[");
           line.delete(index1, index1+2);   // delete the [[
         }
       }
        
       
       // External links: just the ones started with [http: and here I am deleting the whole reference
       // i am not keeping the text to display of this link.
       while ( (index1 = line.indexOf("[http:")) >= 0 || (index1 = line.indexOf("[https:")) >= 0) {
           //System.out.println("LINE(BEFORE): " + line); 
           if( (index2 = line.indexOf("]", index1)) >= 0 ) {
              line.delete(index1, index2+1);   
                
           //System.out.println("LINE (AFTER): " + line + "\n");    
               
           } else {
             System.out.println("processInternalAndExternalLinks: WARNING no ] tag when processing lines with http: line=" + line);
             System.out.println("deleting [");
             line.delete(index1, index1+1);   // delete the [
           }
       }
       
       if(debug && changed){
         System.out.println("Removing links, LINE(BEFORE): " + linetmp);  
         System.out.println("                LINE (AFTER): " + line);
       }
       
       return line; 
       
     }
    
    
    void processWikipediaSQLTables(String textFile, String pageFile, String revisionFile)throws Exception{
        //Put sentences and features in the database.
        DBHandler wikiToDB = new DBHandler();

        wikiToDB.createDBConnection("localhost","wiki","marcela","wiki123");
        
        textFile = "/project/mary/marcela/anna_wikipedia/pages_xml_splits/text.txt";
        pageFile = "/project/mary/marcela/anna_wikipedia/pages_xml_splits/page.txt";
        revisionFile = "/project/mary/marcela/anna_wikipedia/pages_xml_splits/revision.txt";
        
      //  wikiToDB.createAndLoadWikipediaTables(textFile, pageFile, revisionFile);
        
        String pageId[];
        pageId = wikiToDB.getPageIds();
       
        
        String text;
        String pwFile="/project/mary/marcela/anna_wikipedia/wiki-filter.txt";
        PrintWriter pw = new PrintWriter(new FileWriter(new File(pwFile)));
        
        int numPagesUsed=0;
        int minPageLength=20000;  // minimum size of a wikipedia page, to be used in the first filtering of pages
        for(int i=0; i<pageId.length; i++){
    
           
          //System.out.print("PAGE page_id[" + i + "]=" + pageId[i] + "  "); 
          // first filter  
          text = wikiToDB.getTextFromPage(pageId[i], minPageLength);
        
            //text = wikiToDB.getTextFromPage("18783089", minPageLength);
            //text = wikiToDB.getTextFromPage("18717261", minPageLength);
            //text = wikiToDB.getTextFromPage("18717338", minPageLength);
            //text = wikiToDB.getTextFromPage("18836159", minPageLength);
            //text = wikiToDB.getTextFromPage("18695129", minPageLength);
            //System.out.println("text=" + text);
            
            if(text!=null){         
            System.out.println("numPagesUsed=" + numPagesUsed); 
            //if(numPagesUsed==308)
            //  text = wikiCleaner.removeMarKup(text, true);  
            //else
              text = removeMarKup(text, false); 
            //if(numPagesUsed==308)
            //  System.out.println("\n\nnumPagesUsed=" +numPagesUsed+ " CLEANED PAGE page_id[" + i + "]=" + pageId[i] + " :\n" + text);
            pw.println("\n\nnumPagesUsed=" +numPagesUsed+ " CLEANED PAGE page_id[" + i + "]=" + pageId[i] + " :\n" + text);  
            //System.out.println("\n\nnumPagesUsed=" +numPagesUsed+ " CLEANED PAGE :\n" + text);
            numPagesUsed++;
          }
       
          //System.out.println("PAGE page_id[" + i + "]=" + pageId[i] + " : " + text);
            
        }
        System.out.println("Number of PAGES USED=" + numPagesUsed);
        
        pw.close(); 
        
    }
    
    
       
   }
