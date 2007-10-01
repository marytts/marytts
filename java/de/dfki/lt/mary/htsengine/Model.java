package de.dfki.lt.mary.htsengine;

/**
 * HMM model for a particular phoneme (or line in context feature file)
 * This model is the unit when building a utterance model sequence.
 * For every phoneme (or line)in the context feature file, one of these
 * models is created.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class Model {
  
  private String name;              /* the name of this HMM */
  private int durpdf;               /* duration pdf index for this HMM */
  private int lf0pdf[];             /* mel-cepstrum pdf indexes for each state of this HMM */  
  private int mceppdf[];            /* log f0 pdf indexes for each state of this HMM */
  private int strpdf[];             /* str pdf indexes for each state of this HMM  */
  private int magpdf[];             /* str pdf indexes for each state of this HMM  */

  private int dur[];                /* duration for each state of this HMM */
  private int totaldur;             /* total duration of this HMM */
  private double lf0mean[][];       /* mean vector of log f0 pdfs for each state of this HMM */
  private double lf0variance[][];   /* variance (diag) elements of log f0 for each state of this HMM */
  private double mcepmean[][];      /* mean vector of mel-cepstrum pdfs for each state of this HMM */
  private double mcepvariance[][];  /* variance (diag) elements of mel-cepstrum for each state of this HMM */

  private double strmean[][];       /* mean vector of strengths pdfs for each state of this HMM */
  private double strvariance[][];   /* variance (diag) elements of strengths for each state of this HMM */
  private double magmean[][];       /* mean vector of fourier magnitude pdfs for each state of this HMM */
  private double magvariance[][];   /* variance (diag) elements of fourier magnitudes for each state of this HMM */

  private boolean voiced[];         /* voiced/unvoiced decision for each state of this HMM */
  
  public void setName(String var){ name = var; }
  public String getName(){return name;}
  
  public void set_durpdf(int val){ durpdf = val; }
  public int get_durpdf(){return durpdf;}
  
  public void set_dur(int i, int val){ dur[i] = val; }
  public int get_dur(int i){ return dur[i]; } 
  
  public void set_totaldur(int val){ totaldur = val; }
  public int get_totaldur(){return totaldur;}
  
  public void set_lf0pdf(int i, int val){ lf0pdf[i] = val; }
  public int get_lf0pdf(int i){ return lf0pdf[i]; } 
  
  public void set_mceppdf(int i, int val){ mceppdf[i] = val; }
  public int get_mceppdf(int i){ return mceppdf[i]; } 
  
  public void set_strpdf(int i, int val){ strpdf[i] = val; }
  public int get_strpdf(int i){ return strpdf[i]; } 
  
  public void set_magpdf(int i, int val){ magpdf[i] = val; }
  public int get_magpdf(int i){ return magpdf[i]; } 
  
  public void set_lf0mean(int i, int j, double val){ lf0mean[i][j] = val; }
  public double get_lf0mean(int i, int j){ return lf0mean[i][j]; } 
  public void set_lf0variance(int i, int j, double val){ lf0variance[i][j] = val; }
  public double get_lf0variance(int i, int j){ return lf0variance[i][j]; } 
  
  public void set_mcepmean(int i, int j, double val){ mcepmean[i][j] = val; }
  public double get_mcepmean(int i, int j){ return mcepmean[i][j]; } 
  public void set_mcepvariance(int i, int j, double val){ mcepvariance[i][j] = val; }
  public double get_mcepvariance(int i, int j){ return mcepvariance[i][j]; }
  
  public void PrintMcepMean(){  
	for(int i=0; i<mcepmean.length; i++) {
	  System.out.print("mcepmean[" + i + "]: ");
	  for(int j=0; j<mcepmean[i].length; j++)
		  System.out.print(mcepmean[i][j] + "  ");
	  System.out.println();
	}
  }
  
  public void set_strmean(int i, int j, double val){ strmean[i][j] = val; }
  public double get_strmean(int i, int j){ return strmean[i][j]; } 
  public void set_strvariance(int i, int j, double val){ strvariance[i][j] = val; }
  public double get_strvariance(int i, int j){ return strvariance[i][j]; }
  
  public void set_magmean(int i, int j, double val){ magmean[i][j] = val; }
  public double get_magmean(int i, int j){ return magmean[i][j]; } 
  public void set_magvariance(int i, int j, double val){ magvariance[i][j] = val; }
  public double get_magvariance(int i, int j){ return magvariance[i][j]; }
  
  public void set_voiced(int i, boolean val){ voiced[i] = val; }
  public boolean get_voiced(int i){ return voiced[i]; }
  
  
  /* Constructor */
  /* Every Model is initialised with the information in ModelSet*/
  public Model(ModelSet ms){
	int i, nstate;  
	totaldur = 0;
	nstate = ms.get_nstate();
	dur = new int[nstate];
	lf0pdf = new int[nstate];
	lf0mean = new double[nstate][];
    lf0variance = new double[nstate][];
    voiced = new boolean[nstate];

    mceppdf = new int[nstate];
	mcepmean = new double[nstate][];
    mcepvariance = new double[nstate][];
	 
    strpdf = new int[nstate];
	strmean = new double[nstate][];
    strvariance = new double[nstate][];
    
    magpdf = new int[nstate];
	magmean = new double[nstate][];
    magvariance = new double[nstate][];
    
    for(i=0; i<nstate; i++ ){
        lf0mean[i] = new double[ms.get_lf0stream()];
        lf0variance[i] = new double[ms.get_lf0stream()];
        
        mcepmean[i] = new double[ms.get_mcepvsize()];
        mcepvariance[i] = new double[ms.get_mcepvsize()];
        
        strmean[i] = new double[ms.get_strvsize()];
        strvariance[i] = new double[ms.get_strvsize()];
        
        magmean[i] = new double[ms.get_magvsize()];
        magvariance[i] = new double[ms.get_magvsize()];
    }
    
  } /* method Model, initialise a Model object */
  
  
} /* class Model */
