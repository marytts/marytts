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

import java.io.FileInputStream;
import java.util.Properties;
import java.io.IOException;

/**
 * Configuration files and global variables for HTS engine.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HMMData {
	
	public final int HTS_NUMMTYPE = 5;
	public final int DUR = 0;
	public final int LF0 = 1;
	public final int MCP = 2;
	public final int STR = 3;
	public final int MAG = 4;
	/* why i can not use enum??? */
	//public enum HTS_MType { DUR, LF0, MCP, STR, MAG };

	
	/* Global variables for some functions, initialised with default values, so these values 
	 * can be loaded from a configuration file. */
	private int RATE       = 16000; /* sampling rate                              */
	private int FPERIOD    = 80;    /* frame period (point)                       */
	private double RHO     = 0.0;   /* variable for speaking rate control         */
	private double ALPHA   = 0.42;  /* variable for frequency warping parameter   */
	private double F0_STD  = 1.0;   /* variable for f0 control                    */
	private double F0_MEAN = 0.0;   /* variable for f0 control                    */
	private double BETA    = 0.0;   /* variable for postfiltering                 */
	private double UV      = 0.5;   /* variable for U/V threshold                 */
	private double LENGTH  = 0.0;   /* total number of frame for generated speech */
	private boolean algnst = false; /* use state level alignment for duration     */
	private boolean algnph = false; /* use phoneme level alignment for duration   */
	
	
	/* Trees and HMM model files */
	private String TreeDurFile; /* durations tree file */
	private String TreeLf0File; /* lf0 tree file */
	private String TreeMcpFile; /* MCP tree file */
	private String TreeStrFile; /* Strengths tree file */
	private String TreeMagFile; /* Fourier magnitudes tree file */
    
     /** Contains the tree-xxx.inf, xxx: dur, lf0, mcp, str and mag 
     * these are all the trees trained for a particular voice. */
    private TreeSet ts = new TreeSet(HTS_NUMMTYPE);
	
	private String PdfDurFile;  /* durations Pdf file */
	private String PdfLf0File;  /* lf0 Pdf file */
	private String PdfMcpFile;  /* MCP Pdf file */
	private String PdfStrFile;  /* Strengths Pdf file */
	private String PdfMagFile;  /* Fourier magnitudes Pdf file */
    
     /** Contains the .pdf's (means and variances) for dur, lf0, mcp, str and mag
     * these are all the HMMs trained for a particular voice */   
    private ModelSet ms = new ModelSet();
	
	/* Variables for mixed excitation */
	private String MixFiltersFile; /* this file contains the filter taps for mixed excitation */
	private int numFilters;
	private int orderFilters;
	
	/* Feature list file */
	private String featureListFile;
    
    /* Example context feature file in HTSCONTEXT_EN format */
    private String labFile;
	
	public int RATE() { return RATE; }
	public int FPERIOD() { return FPERIOD; } 
	public double RHO() { return RHO; } 
	public double ALPHA() { return ALPHA; }
	public double F0_STD() { return F0_STD; }
	public double F0_MEAN() { return F0_MEAN; }
	public double BETA() { return BETA; }
	public double UV() { return  UV; }
	public double LENGTH() { return LENGTH; }
	public boolean algnst() { return algnst; }
	public boolean algnph() { return algnph; }
	
	public String TreeDurFile() { return TreeDurFile; } 
	public String TreeLf0File() { return TreeLf0File; } 
	public String TreeMcpFile() { return TreeMcpFile; }  
	public String TreeStrFile() { return TreeStrFile; } 
	public String TreeMagFile() { return TreeMagFile; }  
	
	public String PdfDurFile() { return PdfDurFile; }   
	public String PdfLf0File() { return PdfLf0File; }   
	public String PdfMcpFile() { return PdfMcpFile; } 
	public String PdfStrFile() { return PdfStrFile; } 
	public String PdfMagFile() { return PdfMagFile; } 
	
	public String FeaListFile() { return featureListFile; }
    public String LabFile() { return labFile; }
	
	public String MixFiltersFile() { return MixFiltersFile; } 
	public int get_numFilters(){ return numFilters; }
	public int get_orderFilters(){ return orderFilters; }
    
    public TreeSet getTreeSet() { return ts; }       
    public ModelSet getModelSet() { return ms; }
 
    public void setTreeDurFile(String str) { TreeDurFile = str; } 
    public void setTreeLf0File(String str) { TreeLf0File = str; } 
    public void setTreeMcpFile(String str) { TreeMcpFile = str; }  
    public void setTreeStrFile(String str) { TreeStrFile = str; } 
    public void setTreeMagFile(String str) { TreeMagFile = str; }  
    
    public void setPdfDurFile(String str) { PdfDurFile = str; }   
    public void setPdfLf0File(String str) { PdfLf0File = str; }   
    public void setPdfMcpFile(String str) { PdfMcpFile = str; } 
    public void setPdfStrFile(String str) { PdfStrFile = str; } 
    public void setPdfMagFile(String str) { PdfMagFile = str; } 
    
    public void setFeaListFile(String str) { featureListFile = str; }
    public void setLabFile(String str) { labFile = str; }
    
    public void setMixFiltersFile(String str) { MixFiltersFile = str; } 
    public void set_numFilters(int val){ numFilters = val; }
    public void set_orderFilters(int val){ orderFilters = val; }
    
    public void LoadTreeSet(){ ts.LoadTreeSet(this); }   
    public void LoadModelSet(){ ms.LoadModelSet(this); }  
	
	/* Reads from configuration file all the data files in this class */
	public void InitHMMData(String ConfigFile) {		
      Properties props = new Properties();
      
      try {
    	  FileInputStream fis = new FileInputStream( ConfigFile );
    	  props.load( fis );
    	  fis.close();
    	      	  
    	  TreeDurFile = props.getProperty( "Ftd" );
    	  TreeLf0File = props.getProperty( "Ftf" );     	  
    	  TreeMcpFile = props.getProperty( "Ftm" );
    	  TreeStrFile = props.getProperty( "Fts" );
    	  TreeMagFile = props.getProperty( "Fta" );
    	  
    	  PdfDurFile = props.getProperty( "Fmd" );
    	  PdfLf0File = props.getProperty( "Fmf" );     	  
    	  PdfMcpFile = props.getProperty( "Fmm" );
    	  PdfStrFile = props.getProperty( "Fms" );
    	  PdfMagFile = props.getProperty( "Fma" );
    	  
    	  /* Feature list file */
    	  featureListFile = props.getProperty( "FeaList" );
          
          /* Example context feature file in HTSCONTEXT_EN format */
          labFile = props.getProperty( "Flab" );
    	  
    	  /* Configuration for mixed excitation */
    	  MixFiltersFile = props.getProperty( "Fif" ); 
    	  numFilters     = Integer.parseInt(props.getProperty( "in" ));
    	  orderFilters   = Integer.parseInt(props.getProperty( "io" ));
    	  
    	  props.clear();
    	  
      } 
      catch (IOException e) {
        System.err.println("Caught IOException: " +  e.getMessage());
	  }	
      
      /* Load TreeSet ts and ModelSet ms*/
      ts.LoadTreeSet(this);   /* OJO!!! CHECK??? is this correct ???*/ 
      ms.LoadModelSet(this);  /* OJO!!! CHECK??? is this correct ???*/ 
   		
	}
	
	
}
