/**   
*           The HMM-Based Speech Synthesis System (HTS)             
*                       HTS Working Group                           
*                                                                   
*                  Department of Computer Science                   
*                  Nagoya Institute of Technology                   
*                               and                                 
*   Interdisciplinary Graduate School of Science and Engineering    
*                  Tokyo Institute of Technology                    
*                                                                   
*                Portions Copyright (c) 2001-2006                       
*                       All Rights Reserved.
*                         
*              Portions Copyright 2000-2007 DFKI GmbH.
*                      All Rights Reserved.                  
*                                                                   
*  Permission is hereby granted, free of charge, to use and         
*  distribute this software and its documentation without           
*  restriction, including without limitation the rights to use,     
*  copy, modify, merge, publish, distribute, sublicense, and/or     
*  sell copies of this work, and to permit persons to whom this     
*  work is furnished to do so, subject to the following conditions: 
*                                                                   
*    1. The source code must retain the above copyright notice,     
*       this list of conditions and the following disclaimer.       
*                                                                   
*    2. Any modifications to the source code must be clearly        
*       marked as such.                                             
*                                                                   
*    3. Redistributions in binary form must reproduce the above     
*       copyright notice, this list of conditions and the           
*       following disclaimer in the documentation and/or other      
*       materials provided with the distribution.  Otherwise, one   
*       must contact the HTS working group.                         
*                                                                   
*  NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF TECHNOLOGY,   
*  HTS WORKING GROUP, AND THE CONTRIBUTORS TO THIS WORK DISCLAIM    
*  ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL       
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   
*  SHALL NAGOYA INSTITUTE OF TECHNOLOGY, TOKYO INSTITUTE OF         
*  TECHNOLOGY, HTS WORKING GROUP, NOR THE CONTRIBUTORS BE LIABLE    
*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY        
*  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,  
*  WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTUOUS   
*  ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR          
*  PERFORMANCE OF THIS SOFTWARE.                                    
*                                                                   
*/

package de.dfki.lt.mary.htsengine;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;
import java.util.*;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Configuration files and global variables for HTS engine.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HMMData {
    	
    /** Number of model and identificator for the models*/
	public static final int HTS_NUMMTYPE = 5;
	public static final int DUR = 0;
	public static final int LF0 = 1;
	public static final int MCP = 2;
	public static final int STR = 3;
	public static final int MAG = 4;

    private Logger logger = Logger.getLogger("HMMData");
    
	/** Global variables for some functions, initialised with default values, so these values 
	 * can be loaded from a configuration file. */
	private int rate       = 16000; /* sampling rate                              */
	private int fperiod    = 80;    /* frame period (point)                       */
	private double rho     = 0.0;   /* variable for speaking rate control         */
	private double alpha   = 0.42;  /* variable for frequency warping parameter   */
	private double f0Std   = 1.0;   /* variable for f0 control                    */
	private double f0Mean  = 0.0;   /* variable for f0 control                    */
	private double beta    = 0.0;   /* variable for postfiltering                 */
	private double uv      = 0.5;   /* variable for U/V threshold                 */
	private double length  = 0.0;   /* total number of frame for generated speech */
	private boolean algnst = false; /* use state level alignment for duration     */
	private boolean algnph = false; /* use phoneme level alignment for duration   */
	
	
	/** Tree files and TreeSet object */
	private String treeDurFile; /* durations tree file */
	private String treeLf0File; /* lf0 tree file */
	private String treeMcpFile; /* MCP tree file */
	private String treeStrFile; /* Strengths tree file */
	private String treeMagFile; /* Fourier magnitudes tree file */
    
     /** TreeSet contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
     * these are all the trees trained for a particular voice. */
    private TreeSet ts = new TreeSet(HTS_NUMMTYPE);
	
    /** HMM pdf model files and ModelSet object */
	private String pdfDurFile;  /* durations Pdf file */
	private String pdfLf0File;  /* lf0 Pdf file */
	private String pdfMcpFile;  /* MCP Pdf file */
	private String pdfStrFile;  /* Strengths Pdf file */
	private String pdfMagFile;  /* Fourier magnitudes Pdf file */
    
     /** ModelSet contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
     * these are all the HMMs trained for a particular voice */   
    private ModelSet ms = new ModelSet();
	
	/** Variables for mixed excitation */
	private String mixFiltersFile; /* this file contains the filter taps for mixed excitation */
	private int numFilters;
	private int orderFilters;
	
	/* Feature list file and Vector which will contain the loaded features from this file */
	private String featureListFile;
    private Vector<String> featureList = new Vector<String>();
    
    
    /* Example context feature file in HTSCONTEXT_EN format */
    private String labFile;
	
	public int getRate() { return rate; }
	public int getFperiod() { return fperiod; } 
	public double getRho() { return rho; } 
	public double getAlpha() { return alpha; }
	public double getF0Std() { return f0Std; }
	public double getF0Mean() { return f0Mean; }
	public double getBeta() { return beta; }
	public double getUV() { return  uv; }
	public double getLength() { return length; }
	public boolean getAlgnst() { return algnst; }
	public boolean getAlgnph() { return algnph; }
	
	public String getTreeDurFile() { return treeDurFile; } 
	public String getTreeLf0File() { return treeLf0File; } 
	public String getTreeMcpFile() { return treeMcpFile; }  
	public String getTreeStrFile() { return treeStrFile; } 
	public String getTreeMagFile() { return treeMagFile; }  
	
	public String getPdfDurFile() { return pdfDurFile; }   
	public String getPdfLf0File() { return pdfLf0File; }   
	public String getPdfMcpFile() { return pdfMcpFile; } 
	public String getPdfStrFile() { return pdfStrFile; } 
	public String getPdfMagFile() { return pdfMagFile; } 
	
	public String getFeatureListFile() { return featureListFile; }
    /* This function returns the feature list already loaded in a Vector */
    public Vector<String> getFeatureList() { return featureList; }  
    public String getLabFile() { return labFile; }
	
	public String getMixFiltersFile() { return mixFiltersFile; } 
	public int getNumFilters(){ return numFilters; }
	public int getOrderFilters(){ return orderFilters; }
    
    public TreeSet getTreeSet() { return ts; }       
    public ModelSet getModelSet() { return ms; }
 
    public void setTreeDurFile(String str) { treeDurFile = str; } 
    public void setTreeLf0File(String str) { treeLf0File = str; } 
    public void setTreeMcpFile(String str) { treeMcpFile = str; }  
    public void setTreeStrFile(String str) { treeStrFile = str; } 
    public void setTreeMagFile(String str) { treeMagFile = str; }  
    
    public void setPdfDurFile(String str) { pdfDurFile = str; }   
    public void setPdfLf0File(String str) { pdfLf0File = str; }   
    public void setPdfMcpFile(String str) { pdfMcpFile = str; } 
    public void setPdfStrFile(String str) { pdfStrFile = str; } 
    public void setPdfMagFile(String str) { pdfMagFile = str; } 
    
    public void setFeaListFile(String str) { featureListFile = str; }
    public void setLabFile(String str) { labFile = str; }
    
    public void setMixFiltersFile(String str) { mixFiltersFile = str; } 
    public void setNumFilters(int val){ numFilters = val; }
    public void setOrderFilters(int val){ orderFilters = val; }
    
    public void loadTreeSet() throws Exception { ts.loadTreeSet(this); }   
    public void loadModelSet() throws Exception { ms.loadModelSet(this); }  
	
	/** Reads from configuration file all the data files in this class 
     * this method is used when running stand alone, for example when calling
     * from MaryClientUserHMM */
	public void initHMMData(String ConfigFile) {		
      Properties props = new Properties();
      
      try {
    	  FileInputStream fis = new FileInputStream( ConfigFile );
    	  props.load( fis );
    	  fis.close();
    	      	  
    	  treeDurFile = props.getProperty( "Ftd" );
    	  treeLf0File = props.getProperty( "Ftf" );     	  
    	  treeMcpFile = props.getProperty( "Ftm" );
    	  treeStrFile = props.getProperty( "Fts" );
    	  treeMagFile = props.getProperty( "Fta" );
    	  
    	  pdfDurFile = props.getProperty( "Fmd" );
    	  pdfLf0File = props.getProperty( "Fmf" );     	  
    	  pdfMcpFile = props.getProperty( "Fmm" );
    	  pdfStrFile = props.getProperty( "Fms" );
    	  pdfMagFile = props.getProperty( "Fma" );
    	  
    	  /* Feature list file */
    	  featureListFile = props.getProperty( "FeaList" );
          
          /* Example context feature file in HTSCONTEXT_EN format */
          labFile = props.getProperty( "Flab" );
    	  
    	  /* Configuration for mixed excitation */
          if( treeStrFile != null ) {
    	    mixFiltersFile = props.getProperty( "Fif" ); 
    	    numFilters     = Integer.parseInt(props.getProperty( "in" ));
    	    orderFilters   = Integer.parseInt(props.getProperty( "io" ));
          }
    	  
    	  props.clear();
          
      } 
      catch (IOException e) {
          logger.debug("Caught IOException: " +  e.getMessage());
	  }	
      
      try {
        /* Load TreeSet ts and ModelSet ms for current voice*/
        logger.info("Loading Model Set:");
        ts.loadTreeSet(this);   
       
        logger.info("Loading Tree Set:");
        ms.loadModelSet(this);  
      
        /* Load (un-commented) context feature list from featureListFile */
        logger.info("Loading Feature List:");
        readFeatureList();
      }
      catch (Exception e) {
          logger.debug(e.getMessage()); 
      }
      
      if( featureList.size() == 0)
          logger.debug("initHMMData: Warning feature list file empty or feature list not loaded. ");
   		
	}
	
    
    /** This function reads the feature list file, for example feature_list_en_05.pl
     * and fills in a vector the elements in that list that are un-commented 
     */
    public void readFeatureList() throws FileNotFoundException {
      String line;
      int i;
      
      Scanner s = null;
      try {
        s = new Scanner(new BufferedReader(new FileReader(featureListFile))).useDelimiter("\n");
        
        while (s.hasNext()) {
          line = s.next();
          //System.out.println("fea: "+ line);
          if(!line.contains("#") && line.length()>0){    /* if it is not commented */
            String[] elem = line.split(",");
            for(i=0; i<elem.length; i++)
              if(elem[i].contains("mary_")){  /* if starts with mary_ */                 
                featureList.addElement(elem[i].substring(elem[i].indexOf("\"")+1, elem[i].lastIndexOf("\"")));
                //System.out.println("  -->  "+ featureList.lastElement()); 
              }
          }
        }
                
        if (s != null) { 
          s.close();
        }
        
      } catch (FileNotFoundException e) {
          logger.debug("readFeatureList:  " + e.getMessage());
          throw new FileNotFoundException("readFeatureList " + e.getMessage());
      }
      
      logger.info("readFeatureList: loaded " + featureList.size() + " context features from " + featureListFile);
      
    } /* method ReadFeatureList */

    
    
	
}
