
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
	
	public static final int HTS_NUMMTYPE = 5;
	public static final int DUR = 0;
	public static final int LF0 = 1;
	public static final int MCP = 2;
	public static final int STR = 3;
	public static final int MAG = 4;
	/* why i can not use enum??? */
	//public enum HTS_MType { DUR, LF0, MCP, STR, MAG };

	
	/* Global variables for some functions, initialised with default values, so these values 
	 * can be loaded from a configuration file. */
	private static int RATE       = 16000; /* sampling rate                              */
	private static int FPERIOD    = 80;    /* frame period (point)                       */
	private static double RHO     = 0.0;   /* variable for speaking rate control         */
	private static double ALPHA   = 0.42;  /* variable for frequency warping parameter   */
	private static double F0_STD  = 1.0;   /* variable for f0 control                    */
	private static double F0_MEAN = 0.0;   /* variable for f0 control                    */
	private static double BETA    = 0.0;   /* variable for postfiltering                 */
	private static double UV      = 0.5;   /* variable for U/V threshold                 */
	private static double LENGTH  = 0.0;   /* total number of frame for generated speech */
	private static boolean algnst = false; /* use state level alignment for duration     */
	private static boolean algnph = false; /* use phoneme level alignment for duration   */
	
	
	/* Trees and HMM model files */
	private static String TreeDurFile; /* durations tree file */
	private static String TreeLf0File; /* lf0 tree file */
	private static String TreeMcpFile; /* MCP tree file */
	private static String TreeStrFile; /* Strengths tree file */
	private static String TreeMagFile; /* Fourier magnitudes tree file */
	
	private static String PdfDurFile;  /* durations Pdf file */
	private static String PdfLf0File;  /* lf0 Pdf file */
	private static String PdfMcpFile;  /* MCP Pdf file */
	private static String PdfStrFile;  /* Strengths Pdf file */
	private static String PdfMagFile;  /* Fourier magnitudes Pdf file */
	
	/* Variables for mixed excitation */
	private static String MixFiltersFile; /* this file contains the filter taps for mixed excitation */
	private static int numFilters;
	private static int orderFilters;
	
	/* Feature list file */
	private String featureListFile;
    
    /* Example context feature file in HTSCONTEXT_EN format */
    private static String labFile;
	
	public static int RATE() { return RATE; }
	public static int FPERIOD() { return FPERIOD; } 
	public static double RHO() { return RHO; } 
	public static double ALPHA() { return ALPHA; }
	public static double F0_STD() { return F0_STD; }
	public static double F0_MEAN() { return F0_MEAN; }
	public static double BETA() { return BETA; }
	public static double UV() { return  UV; }
	public static double LENGTH() { return LENGTH; }
	public static boolean algnst() { return algnst; }
	public static boolean algnph() { return algnph; }
	
	public static String TreeDurFile() { return TreeDurFile; } 
	public static String TreeLf0File() { return TreeLf0File; } 
	public static String TreeMcpFile() { return TreeMcpFile; }  
	public static String TreeStrFile() { return TreeStrFile; } 
	public static String TreeMagFile() { return TreeMagFile; }  
	
	public static String PdfDurFile() { return PdfDurFile; }   
	public static String PdfLf0File() { return PdfLf0File; }   
	public static String PdfMcpFile() { return PdfMcpFile; } 
	public static String PdfStrFile() { return PdfStrFile; } 
	public static String PdfMagFile() { return PdfMagFile; } 
	
	public String FeaListFile() { return featureListFile; }
    public String LabFile() { return labFile; }
	
	public static String MixFiltersFile() { return MixFiltersFile; } 
	public static int get_numFilters(){ return numFilters; }
	public static int get_orderFilters(){ return orderFilters; }
	
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
    	  
    	  
    	  
//    	  System.out.println( "HMMData: Tree duration file: " + TreeDurFile );  	  
//    	  System.out.println( "HMMData: Pdf Mcp file: " + PdfMcpFile );
//    	  System.out.println();
//    	  System.out.println( "HMMData: Fourier Mag file: " + TreeMagFile );  	  
//    	  System.out.println( "HMMData: Pdf Fourier Mag file: " + PdfMagFile );
//    	  
//    	  System.out.println( "*************HMMData: numFilters= " + numFilters );
    	  props.clear();
    	  
      } 
      catch (IOException e) {
        System.err.println("Caught IOException: " +  e.getMessage());
	  }		 
   		
	}
	
	
}
