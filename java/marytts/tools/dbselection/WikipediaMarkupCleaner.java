package marytts.tools.dbselection;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.Vector;

public class WikipediaMarkupCleaner {

    public Vector<String> removeMarKup(String page, int maxTextLength, boolean debug) {
        StringBuffer str = new StringBuffer("");
        StringBuffer line = null;
        Vector<String> textList = new Vector<String>();
               
        boolean endOfText=false;
        Scanner s = null;
        try {
          s = new Scanner(page);
          while (s.hasNext() && !endOfText) {
              
           line = new StringBuffer(s.nextLine()); 
           // process text until it finds any of these labels:
           if( line.indexOf("==References")>=0 || line.indexOf("== References")>=0 || 
                   line.indexOf("==See also")>=0 || line.indexOf("== See also")>=0 ||
                   line.indexOf("==External links and sources")>=0 || line.indexOf("==External links")>=0 || 
                   line.indexOf("== External links")>=0 || line.indexOf("== External Links")>=0 ||
                   line.indexOf("== External links and sources")>=0 ||
                   line.indexOf("==Notes")>=0 || line.indexOf("== Notes")>=0 ||
                   line.indexOf("==Sources")>=0 || line.indexOf("== Sources")>=0 ||
                   line.indexOf("==Foreign")>=0 || line.indexOf("== Foreign")>=0 ||
                   line.indexOf("==Discussion")>=0  ){
                   endOfText=true;
           } else {
             // when emoving sections it might add more lines that might contain again more labes to remove
             boolean clean = false;
             while (!clean && line.length()>0 ){
             clean = true;    
             if( line.indexOf("<noinclude") >= 0){
                line = removeSection(s, line, "<noinclude", "</noinclude>", debug);
                clean = false;
             }
             
             if( line.indexOf("<includeonly") >= 0){
                 line = removeSection(s, line, "<includeonly", "</includeonly>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<onlyinclude") >= 0){
                 line = removeSection(s, line, "<onlyinclude", "</onlyinclude>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<table") >= 0){  // tables
                 line = removeSection(s, line, "<table", "</table>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<TABLE") >= 0){
                 line = removeSection(s, line, "<TABLE", "</TABLE>", debug);
                 clean = false;
             }
             
             if( line.indexOf("{{col-begin}}") >= 0){
                 line = removeSection(s, line, "{{col-begin}}", "{{col-end}}", debug);
                 clean = false;
             }
              
             if( line.indexOf("{|") >= 0){  // this is a table, this should go before {{ because a table can contain {{ }}
                 line = removeSectionTable(s, line, "{|", "|}", debug);
                 clean = false;
             }
               
             if( line.indexOf("<ref") >= 0){  // references
               line = removeSectionRef(s, line);  // This is special because it can be <ref>, <ref, </ref> or />
               clean = false;
             }
             
             if( line.indexOf("<REF") >= 0){
                 line = removeSection(s, line, "<REF", "</REF>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<Ref") >= 0){
                 line = removeSection(s, line, "<Ref", "</Ref>", debug);
                 clean = false;
             }
             if( line.indexOf("<reF") >= 0){
                 line = removeSection(s, line, "<reF", "</reF>", debug);
                 clean = false;
             }
            
             if( line.indexOf("{{start box}}") >= 0){
                 line = removeSection(s, line, "{{start box}}", "{{end box}}", debug);
                 clean = false;
             }
             
             if( line.indexOf("{{") >= 0){
               line = removeSection(s, line, "{{", "}}", debug);
               clean = false;
             }
           
             if( line.indexOf("<!--") >= 0){
                 line = removeSection(s, line, "<!--", "-->", debug);
                 clean = false;
             }
             
             if( line.indexOf("\\mathrel{|") >= 0){
                 line = removeSection(s, line, "\\mathrel{|", "}", debug);
                 clean = false;
             }
                         
             if( line.indexOf("<gallery") >= 0){  // gallery might contain several images
                 line = removeSection(s, line, "<gallery", "</gallery>", debug);
                 clean = false;
             }
                   
             if( line.indexOf("[[Image:") >= 0){
                 line = removeSectionImage1(s, line, "[[Image:", "]]", debug);
                 clean = false;
             }
            
             if( line.indexOf("<div") >= 0){ // span and div tags are used to separate images from text
                 line = removeSection(s, line, "<div", "</div>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<DIV") >= 0){
                line = removeSectionImage1(s, line, "<DIV", "</DIV>", debug);
                clean = false;
             }
             
             if( line.indexOf("<span") >= 0){
                 line = removeSection(s, line, "<span", "</span>", debug);
                 clean = false;
             }
                        
             if( line.indexOf("<math>") >= 0){
                 line = removeSection(s, line, "<math>", "</math>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<timeline>") >= 0){
                 line = removeSection(s, line, "<timeline>", "</timeline>", debug);
                 clean = false;
             }
            
             if( line.indexOf("<nowiki") >= 0){
                 line = removeSection(s, line, "<nowiki", "</nowiki>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<source") >= 0){
                 line = removeSection(s, line, "<source", "</source>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<code") >= 0){
                 line = removeSection(s, line, "<code", "</code>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<imagemap") >= 0){
                 line = removeSection(s, line, "<imagemap", "</imagemap>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<poem") >= 0){
                 line = removeSection(s, line, "<poem", "</poem>", debug);
                 clean = false;
             }
             
             if( line.indexOf("<h1") >= 0){
                 line = removeSection(s, line, "<h1", "</h1>", debug);
                 clean = false;
             }
            
             if( line.indexOf("<pre") >= 0){
                 line = removeSection(s, line, "<pre", "</pre>", debug);
                 clean = false;
             }
             
             } // while the line/text is not clean (or does not have tags to remove)
             
             // here filter bulleted and numbered short lines
             if(line.length() > 0){
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
             }
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
               line = new StringBuffer(line.toString().replaceAll("<UL>", ""));
               line = new StringBuffer(line.toString().replaceAll("</UL>", ""));
               line = new StringBuffer(line.toString().replaceAll("<br>", ""));
               line = new StringBuffer(line.toString().replaceAll("<br", ""));
               line = new StringBuffer(line.toString().replaceAll("<BR>", ""));
               line = new StringBuffer(line.toString().replaceAll("<br", ""));
               line = new StringBuffer(line.toString().replaceAll("<br/>", ""));
               line = new StringBuffer(line.toString().replaceAll("<Center>", ""));
               line = new StringBuffer(line.toString().replaceAll("<center>", ""));
               line = new StringBuffer(line.toString().replaceAll("</center>", ""));
               line = new StringBuffer(line.toString().replaceAll("<CENTER>", ""));
               line = new StringBuffer(line.toString().replaceAll("</CENTER>", ""));
               line = new StringBuffer(line.toString().replaceAll("<cite>", ""));
               line = new StringBuffer(line.toString().replaceAll("</cite>", ""));
               line = new StringBuffer(line.toString().replaceAll("<li>", ""));
               line = new StringBuffer(line.toString().replaceAll("</li>", ""));
               line = new StringBuffer(line.toString().replaceAll("<LI>", ""));
               line = new StringBuffer(line.toString().replaceAll("</LI>", ""));
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
               line = new StringBuffer(line.toString().replaceAll("<i>", ""));
               line = new StringBuffer(line.toString().replaceAll("</i>", ""));
               line = new StringBuffer(line.toString().replaceAll("<I>", ""));
               line = new StringBuffer(line.toString().replaceAll("</I>", ""));
               line = new StringBuffer(line.toString().replaceAll("<s>", ""));
               line = new StringBuffer(line.toString().replaceAll("</s>", ""));
               line = new StringBuffer(line.toString().replaceAll("<em>", ""));
               line = new StringBuffer(line.toString().replaceAll("</em>", ""));
               line = new StringBuffer(line.toString().replaceAll("</br>", ""));
               line = new StringBuffer(line.toString().replaceAll("</div>", ""));
               line = new StringBuffer(line.toString().replaceAll("</ref>", ""));
               // i am not sure about this
               line = new StringBuffer(line.toString().replaceAll("\"", ""));
               
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
               if( line.toString().startsWith(";") || line.toString().startsWith(";") )  // in glosaries definitions start with ;
                   line.replace(0, 1, "");
                 
               // remove this when the text is almost clean
               if( line.indexOf("<font") >= 0)
                   line = removeSection(s, line, "<font", ">", debug);
               line = new StringBuffer(line.toString().replaceAll("</font>", ""));
               
               if( line.indexOf("<blockquote") >= 0)
                   line = removeSection(s, line, "<blockquote", ">", debug);
               
               if( line.indexOf("<ol") >= 0)
                   line = removeSection(s, line, "<ol", ">", debug);
               
               if( line.indexOf("<http:") >= 0)
                   line = removeSection(s, line, "<http:", ">", debug);
               
               // finally concatenate the line  
               str.append(line);
               if(!str.toString().endsWith("\n"))
                 str.append("\n");
               
               // check length of the text 
               if(str.length() > maxTextLength ){
                 textList.add(str.toString());
                 //System.out.println("\n-----------\n" + str.toString());
                 str = new StringBuffer(""); 
               }
                             
             }
             
           } // endOfText=false
           
          }  // while has more lines
        
        } finally {
            if (s != null)
              s.close();
        }   
         
        if(!str.toString().contentEquals(""))
          textList.add(str.toString());
        return textList;  
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
          //System.out.println("removeSection: WARNING no " + endTag + " in line: " + line);
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
          //System.out.println("removeSection: WARNING no " + endTag + " in line: " + line);
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
          //System.out.println("removeSection: WARNING no " + endTag + " in line: " + line);
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
          //System.out.println("removeSection: WARNING no " + endTag + " in line: " + line);
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
           //   line.delete(index1, index2+1);
             if ( ( index3 = line.indexOf(" ", index1)) >= 0  && index3 < index2){  // if there is text to display
               line.delete(index1, index3+1);   // delete the link and [http:    until forst black space before ]
               index2 = line.indexOf("]");     // since i delete some text i need to find egain the next ]]
               line.delete(index2, index2+1);
             } else {                            
               line.delete(index1, index2+1);   // no text to display, delete the whole ref
             }
                
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
    
    
    void processWikipediaSQLTablesDebug(String textFile, String pageFile, String revisionFile)throws Exception{
        
        DBHandler wikiToDB = new DBHandler();

        wikiToDB.createDBConnection("localhost","wiki","marcela","wiki123");    
        String text;
        StringBuffer textId = new StringBuffer();
        int numPagesUsed=0;
        int minPageLength=1;  
        
        //String idtest="18702442";
        String idtest="18951367";
        
        // get text from the DB
        text = wikiToDB.getTextFromWikiPage(idtest, minPageLength, textId, null);
        System.out.println("\nPAGE SIZE=" + text.length() + "  text:\n" + text);
        //System.out.println("text:" + text);
        
        int maxTextLength=15000;
        Vector<String> textList;
        
        if(text!=null){         
          textList = removeMarKup(text, maxTextLength, false); 
          for(int i=0; i<textList.size(); i++)
            System.out.println("text(" + i + "): \n" + textList.get(i));  
          
        } else
            System.out.println("NO CLEANED TEXT");   
        
    }
    
    
    void processWikipediaSQLTables(String textFile, String pageFile, String revisionFile, String wikiLog)throws Exception{
        //Put sentences and features in the database.
        
        DateFormat fullDate = new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss");
        Date dateIni = new Date();
        String dateStringIni = fullDate.format(dateIni);
        
        DBHandler wikiToDB = new DBHandler();
        
        System.out.println("Creating connection to DB server...");
        wikiToDB.createDBConnection("localhost","wiki","marcela","wiki123");
        // in semaine
        //wikiToDB.createDBConnection("penguin.dfki.uni-sb.de","MaryDBSelector","MaryDBSel_admin","p4rpt3jr");
        
        
        // This loading can take a while
        // create and load TABLES: page, text and revision
    //    System.out.println("Creating and loading TABLES: page, text and revision. (The loading can take a while...)");
    //    wikiToDB.createAndLoadWikipediaTables(textFile, pageFile, revisionFile);
        
        System.out.println("Getting page IDs");
        String pageId[];
        pageId = wikiToDB.getIds("page_id","page");
        
        // create clean_text TABLE
        System.out.println("Creating clean_text TABLE");
        wikiToDB.createWikipediaCleanTextTable();
               
        String text;
        PrintWriter pw = new PrintWriter(new FileWriter(new File(wikiLog)));
        
        StringBuffer textId = new StringBuffer();
        int numPagesUsed=0;
        
        int minPageLength=10000;  // minimum size of a wikipedia page, to be used in the first filtering of pages
        int minTextLength=1000;
        int maxTextLength=15000;  // the average lenght in one big xml file is approx. 12000
        Vector<String> textList;
        System.out.println("\nStart processing of wikipedia pages....\n");
 
        for(int i=0; i<pageId.length; i++){
          // first filter  
          text = wikiToDB.getTextFromWikiPage(pageId[i], minPageLength, textId, pw);
          
          if(text!=null){ 
            textList = removeMarKup(text, maxTextLength, false); 
            
            for(int j=0; j<textList.size(); j++){
                
              text = textList.get(j);
              
              if( text.length() > minTextLength ){
                // if after cleaning the text is not empty or 
                numPagesUsed++;  
                //System.out.println("numPagesUsed=" + numPagesUsed);   
                wikiToDB.insertCleanText(text, pageId[i], textId.toString()); 
                System.out.println("text_id=" + textId.toString() + " textList (" + (j+1) + "/"+ textList.size() + ")  Length=" + text.length());
              
                if(pw != null)
                  pw.println("CLEANED PAGE page_id[" + i + "]=" + pageId[i] 
                         + " textList (" + (j+1) + "/"+ textList.size() + ") length=" + text.length() 
                         + "  NUM_PAGES_USED=" +numPagesUsed + " text:\n\n" + text);               
              } else
                if(pw != null)  
                  pw.println("PAGE NOT USED AFTER CLEANING length=" + text.length());
            }  // for each text in textList
            textList.clear();  // clear the list of text
          }
              
          
        }
        Date dateEnd = new Date();
        String dateStringEnd = fullDate.format(dateEnd);
        
        
        if(pw != null){
          pw.println("Number of PAGES USED=" + numPagesUsed 
                    + "minPageLength=" + minPageLength + "minTextLength=" + minTextLength
                    + "Start time:" + dateStringIni + "  End time:" + dateStringEnd);  
          pw.close(); 
        }
        
        System.out.println("\nNumber of PAGES USED=" + numPagesUsed 
                + "minPageLength=" + minPageLength + "minTextLength=" + minTextLength
                + "Start time:" + dateStringIni + "  End time:" + dateStringEnd);
        
    }
    
    
    public static void main(String[] args) throws Exception{
        
        String textFile, pageFile, revisionFile, wikiLog;
        WikipediaMarkupCleaner wikiCleaner = new WikipediaMarkupCleaner(); 
        
        if (args.length == 4){
          textFile = args[0];
          pageFile = args[1];  
          revisionFile = args[2];
          wikiLog = args[3];
          
          wikiCleaner.processWikipediaSQLTables(textFile, pageFile, revisionFile, wikiLog);
          //wikiCleaner.processWikipediaSQLTablesDebug(textFile, pageFile, revisionFile);
        } else
          System.out.println("use: WikipediaMarkupCleaner textFile pageFile revisionFile");   
        
    }

   }
