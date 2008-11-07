package marytts.tools.dbselection;

import java.util.Scanner;

public class WikipediaMarkupCleaner {

    public String removeMarKup(String page) {
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
                   line.indexOf("==External links and sources")>=0 || line.indexOf("==External links")>=0 ||
                   line.indexOf("== External links and sources")>=0 ||
                   line.indexOf("==Foreign")>=0 || line.indexOf("== Foreign")>=0  ){
                   endOfText=true;
           } else {
              
           //  if( line.indexOf("<ref") >= 0)
           //    line = removeSectionRef(s, line);  // This is special because it can be <ref>, <ref, </ref> or />
             if( line.indexOf("<ref") >= 0)
                   line = removeSection(s, line, "<ref", "</ref>");  
          
             if( line.indexOf("{{start box}}") >= 0)
                 line = removeSection(s, line, "{{start box}}", "{{end box}}");
             
             if( line.indexOf("{{") >= 0)
               line = removeSection(s, line, "{{", "}}");  
           
             if( line.indexOf("{|") >= 0)
               line = removeSection(s, line, "{|", "|}");  
                   
             if( line.indexOf("[[Image:") >= 0)
                 line = removeSection(s, line, "[[Image:", "]]");
             
             if( line.indexOf("<!--") >= 0)
                 line = removeSection(s, line, "<!--", "-->");
             
             if( line.indexOf("<gallery>") >= 0)
                 line = removeSection(s, line, "<gallery>", "</gallery>");
                          
             str.append(line);
             if(!str.toString().endsWith("\n"))
               str.append("\n");
             
           } // endOfText=false
           
          }  // while has more lines
        
        } finally {
            if (s != null)
              s.close();
        }   
          
        return str.toString();  
      }
      
    // This is special because it can be:
    // <ref>  ... </ref>
    //  <ref  ... </ref>
    //  <ref  ... />
    private StringBuffer removeSectionRef(Scanner s, StringBuffer lineIn){
        String next;
        int index1, index2, index3=0, endTagLength=0;
        boolean closeRef=true;
        StringBuffer line = new StringBuffer(lineIn);
        StringBuffer nextLine;
        
        while( (index1 = line.indexOf("<ref")) >= 0){  // in some text appears as <ref> and in others <ref
          //System.out.println("nextline="+line);  
          if( (index2 = line.indexOf("</ref>",index1)) == -1 ) 
             index2 = line.indexOf("/>",index1);
          if(index2 == -1 ) {// the the </ref> most be in the next lines, so get more lines until the </ref> is found
            do {
              nextLine = new StringBuffer(s.nextLine());  
              //line.append(s.nextLine()); 
              // there is a problem is the new line has again an iniTag
              // this will remove nested tags recursively.
              if( nextLine.indexOf("<ref") >= 0)
                line.append(removeSectionRef(s, nextLine));
              else
                line.append(nextLine);  
              
            } while (s.hasNext() && ( (index2 = line.indexOf("</ref>")) == -1 || (index3 = line.indexOf("/>")) == -1 ));
            if(index2 == -1){
              closeRef=false;
              endTagLength=6;
            }
            else if(index3 == -1){
              closeRef=false;
              endTagLength=2;
            }
          }    
          if(closeRef) {
            index1 = line.indexOf("<ref"); // get again this because the positiom might change (i do not know why???)  
            line.delete(index1, index2+endTagLength);
            //System.out.println("nextline="+line);
          } else {
            System.out.println("WARNING no </ref> or /> in " + line);
            line.delete(index1, line.length()); 
          } 
      }
        
      return line;  
        
    }
     
    private StringBuffer removeSection(Scanner s, StringBuffer lineIn, String iniTag, String endTag){
        String next;
        int index1, index2;
        boolean closeRef=true;
        StringBuffer line = new StringBuffer(lineIn);
        StringBuffer nextLine;
        
        while( (index1 = line.indexOf(iniTag)) >= 0){  // in some text appears as <ref> and in others <ref
          //System.out.println("nextline="+line);  
          index2 = line.indexOf(endTag,index1);
          if(index2 == -1 ) {// the the </ref> most be in the next lines, so get more lines until the </ref> is found
            do {
              nextLine = new StringBuffer(s.nextLine());  
              //line.append(s.nextLine()); 
              // there is a problem is the new line has again an iniTag
              // this will remove nested tags recursively.
              if( nextLine.indexOf(iniTag) >= 0)
                line.append(removeSection(s, nextLine, iniTag, endTag));
              else
                line.append(nextLine);        
            } while (s.hasNext() && (index2 = line.indexOf(endTag)) == -1);
            if(index2 == -1)
              closeRef=false;
          }    
          if(closeRef) {
            index1 = line.indexOf(iniTag); // get again this because the positiom might change (i do not know why???)  
            line.delete(index1, index2+endTag.length());
            //System.out.println("nextline="+line);
          } else {
            System.out.println("WARNING no " + endTag + " in " + line);
            line.delete(index1, line.length()); 
          } 
      }
        
      return line;  
        
    }
    
       
       
   }
