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


import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Locale;
import java.util.Scanner;
import java.util.Random;
import java.io.*;
import javax.sound.sampled.*;
import de.dfki.lt.signalproc.util.DDSAudioInputStream;
import de.dfki.lt.signalproc.util.BufferedDoubleDataSource;
import de.dfki.lt.signalproc.util.MathUtils;

import org.apache.log4j.Logger;


/**
 * Synthesis of speech out of speech parameters.
 * Mixed excitation MLSA vocoder. 
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan 
 */
public class HTSVocoder {
  
	public static final int RANDMAX = 32767;
	
	public static final int IPERIOD = 1;
	public static final int SEED    = 1;
	public static final int B0      = 0x00000001;
	public static final int B28     = 0x10000000;
	public static final int B31     = 0x80000000;
	public static final int B31_    = 0x7fffffff;
	public static final int Z       = 0x00000000;	
	public static final boolean GAUSS = true;
	public static final int PADEORDER = 5;
	public static final int IRLENG    = 96;
	
    private Logger logger = Logger.getLogger("Vocoder");
    
	Random rand;
	
	private int fprd;
	private int iprd;
	private int pd;
	private boolean gauss;
	private double p1;
	private double pc;
	private double pade[];
	private int ppade;  /* offset for vector ppade */  
	private SlideVector c;
	
	private double rate;
	int pt1;
	int pt2;
	int pt3[];

	/* mixed excitation variables */  
	private int numM;          /* Number of bandpass filters for mixed excitation */
	private int orderM;        /* Order of filters for mixed excitation */
	private double h[][];      /* filters for mixed excitation */  
	private double xpulseSignal[];   /* the size of this should be orderM */
	private double xnoiseSignal[];   /* the size of this should be orderM */
    private boolean mixedExcitation = false;
	
	
	/** The initialisation of VocoderSetup should be done when there is already 
	  * information about the number of feature vectors to be processed,
	  * size of the mcep vector file, etc. */
	private void initVocoder(int mcep_order, int mcep_vsize, HMMData htsData) {
		int vector_size;
		fprd  = htsData.getFperiod();
		rate  = htsData.getRate();
		iprd  = IPERIOD;
		pd    = PADEORDER;
		gauss = GAUSS;
		
		rand = new Random();

		vector_size=21;
		pade = new double[vector_size];		 
	    ppade = pd*(pd+1)/2;  /* offset for vector pade */
		pade[0] = 1.0;
		pade[1] = 1.0; 
		pade[2] = 0.0;
		pade[3] = 1.0; 
		pade[4] = 0.0;       
		pade[5] = 0.0;
		pade[6] = 1.0; 
		pade[7] = 0.0;       
		pade[8] = 0.0;        
		pade[9] = 0.0;
		pade[10] = 1.0;
		pade[11] = 0.4999273; 
		pade[12] = 0.1067005; 
		pade[13] = 0.01170221; 
		pade[14] = 0.0005656279;
		pade[15] = 1.0; 
		pade[16] = 0.4999391; 
		pade[17] = 0.1107098; 
		pade[18] = 0.01369984; 
		pade[19] = 0.0009564853;
		pade[20] = 0.00003041721;
        /* ppade will be a copy of pade in mlsadf() function
         * ppade = &( pade[pd*(pd+1)/2] ); */
		
		/* mcep_order=74 and pd=PADEORDER=5 (if no HTS_EMBEDDED is used) 
		 * so the order of c is c[623].
		 * cc, cinc and d1 are copies of c:
		 * c    = new double[623]              --> c[0]   c[1]   ... c[622] size=623
		 * cc   = c+m+1                        --> c[75]  c[76]  ... c[622] size=548
		 * cinc = cc+m+1   = (c+m+1)+m+1       --> c[150] c[151] ... c[622] size=473
		 * d1   = cinc+m+1 = ((c+m+1)+m+1)+m+1 --> c[225] c[226] ... c[622] size=398 
		 * pt1 = &d1[pd+1]        in function mlsadf1() 
		 * pt2 = &d1[pd * (m+2)]  in function mlsadf2()
	     * pt3 = &d1[ 2*(pd+1) ]  in function mlsadf() */
		/* All the previous is replaced by the class SlideVector, it has to be 
		 * initialised with mcep_order and pd=PADEORDER */
		vector_size = ( 3*(mcep_vsize+1) + 3*(pd+1) + pd*(mcep_vsize+2) );
		//System.out.println("C SIZE="+ vector_size + "  mcep_vsize=" + mcep_vsize );
		c = new SlideVector(vector_size, mcep_order,pd);
		pt1 = pd+1;
		pt2 = ( 2 * (pd+1)) + (pd * (mcep_order+2));
		pt3 = new int[pd+1];
		for(int i=pd; i>=1; i--)
		  pt3[i] = ( 2 * (pd+1)) + ((i-1)*(mcep_order+2));

		p1 = -1;
 		pc = 0.0;  /* double */
	
	} /* method initVocoder */
	
	
	/** Initialisation for mixed excitation : it loads the filter taps are read from 
	 * MixFilterFile specified in the configuration file. */
	private void initMixedExcitation(HMMData htsData) 
    throws Exception {
		
		numM = htsData.getNumFilters();
		orderM = htsData.getOrderFilters();
		h = new double[numM][orderM];
		xpulseSignal = new double[orderM];
		xnoiseSignal = new double[orderM];

		/* get the filter coefficients */
		Scanner s = null;
		int i,j;
		try {
			s = new Scanner(new BufferedReader(new FileReader(htsData.getMixFiltersFile())));
            s.useLocale(Locale.US);

			for(i=0; i<numM; i++){
				for(j=0; j<orderM; j++) {
					if (s.hasNextDouble()) {
						h[i][j] = s.nextDouble();
						//System.out.println("h["+i+"]["+j+"]="+h[i][j]);
					}
					else{
                        logger.debug("initMixedExcitation: not enough fiter taps in file = " + htsData.getMixFiltersFile());
                        throw new Exception("initMixedExcitation: not enough fiter taps in file = " + htsData.getMixFiltersFile());
                    }
				}
			}
		} catch (FileNotFoundException e) {
			logger.debug("initMixedExcitation: " + e.getMessage());
            throw new FileNotFoundException("initMixedExcitation: " + e.getMessage());
		} finally {
			s.close();
		} 	

		/* initialise xp_sig and xn_sig */
		for(i=0; i<orderM; i++)
		  xpulseSignal[i] = xnoiseSignal[i] = 0;	
		
	} /* method initMixedExcitation */

	
	/** 
	 * HTS_MLSA_Vocoder: Synthesis of speech out of mel-cepstral coefficients. 
	 * This procedure uses the parameters generated in pdf2par stored in:
	 *   PStream mceppst: Mel-cepstral coefficients
     *   PStream strpst : Filter bank stregths for mixed excitation
     *   PStream magpst : Fourier magnitudes ( OJO!! this is not used yet)
     *   PStream lf0pst : Log F0  
     */
	public AudioInputStream htsMLSAVocoder(HTSParameterGeneration pdf2par, HMMData htsData) 
    throws Exception {

	  double inc, x, MaxSample;
	  short sx;
	  double xp=0.0,xn=0.0,fxp,fxn,mix;  /* samples for pulse and for noise and the filtered ones */
	  int i, j, k, m, s, mcepframe, lf0frame, s_double; 
	  double a = htsData.getAlpha();
	  double aa = 1-a*a;
	  int audio_size;  /* audio size in samples, calculated as num frames * frame period */
      double [] audio_double = null;
      HTSModelSet ms = htsData.getModelSet();
	    
      double f0, f0Std, f0Mean;
      double mc[] = null;  /* feature vector for a particular frame */
	  double hp[] = null;  /* pulse shaping filter, it is initialised once it is known orderM */  
	  double hn[] = null;  /* noise shaping filter, it is initialised once it is known orderM */  
	  
	  /* Initialise vocoder and mixed excitation, once initialised it is known the order
	   * of the filters so the shaping filters hp and hn can be initialised. */
	  m = pdf2par.getMcepOrder();
	  mc = new double[m];
	  initVocoder(m-1, ms.getMcepVsize()-1, htsData);  /* m-1 because the SlideVector offsets count from 0-->24 */
      
      if( htsData.getPdfStrFile() != null ) {
	    initMixedExcitation(htsData);
        mixedExcitation = true;
	    hp = new double[orderM];  
	    hn = new double[orderM]; 
        
        /* Check if the number of filters is equal to the order of strpst 
         * i.e. the number of filters is equal to the number of generated strengths per frame. */
        if(numM != pdf2par.getStrOrder()) {
          logger.debug("htsMLSAVocoder: error num mix-excitation filters =" + numM + " in configuration file is different from generated str order=" + pdf2par.getStrOrder());
          throw new Exception("htsMLSAVocoder: error num mix-excitation filters = " + numM + " in configuration file is different from generated str order=" + pdf2par.getStrOrder());
        }
        logger.info("HMM speech generation with mixed-excitation.");
      } else
        logger.info("HMM speech generation without mixed-excitation.");  
      
      /* Clear content of SlideVector c, should be done if this function is
      called more than once with a new set of generated parameters. */
      c.clearContent();   
	    
      
	  /* _______________________Synthesize speech waveforms_____________________ */
	  /* generate Nperiod samples per mcepframe */
      s = 0;   /* number of samples */
      s_double = 0;
      audio_size = (pdf2par.getMcepT()) * (fprd) ;
      audio_double = new double[audio_size];  /* initialise buffer for audio */
      f0Std = htsData.getF0Std();
      f0Mean = htsData.getF0Mean();
	  for(mcepframe=0,lf0frame=0; mcepframe<pdf2par.getMcepT(); mcepframe++) {
       
		/* get current feature vector mc */ 
		for(i=0; i<m; i++)
		  mc[i] = pdf2par.getMcep(mcepframe, i); 
        
        //System.out.println("htsData.getF0Std()=" + htsData.getF0Std() + " htsData.getF0Mean()="+ htsData.getF0Mean());
        
        /* f0 modification */
	    if(pdf2par.getVoiced(mcepframe)){
	      f0 = f0Std * Math.exp(pdf2par.getLf0(lf0frame, 0)) + f0Mean;       
	      lf0frame++;
	    }
	    else{
	      f0 = 0.0;          
        }
	     
	    /* if mixed excitation get shaping filters for this frame */
        if(mixedExcitation){
          for(j=0; j<orderM; j++) {
            hp[j] = hn[j] = 0.0;
            for(i=0; i<numM; i++) {
        	  //System.out.println("str=" + pdf2par.get_str(mcepframe, i) + "  h[i][j]=" + h[i][j])  ;
        	  hp[j] += pdf2par.getStr(mcepframe, i) * h[i][j];
        	  hn[j] += ( 1 - pdf2par.getStr(mcepframe, i) ) * h[i][j];
            }
          }
        }
	    
        /* f0 -> pitch , in original code here it is used p, so f0=p in the c code */
	    if(f0 != 0.0)
	       f0 = rate/f0;
	    
	   	if( p1 < 0 ) {  
	   	  p1   = f0;           
	   	  pc   = p1;           
	   	}
	   	
	   	/* mc2b: transform mel-cepstrum to MLSA digital fillter coefficients */
	     mc2b(mc,m,a);
	   
	    /* postfiltering  ( Not implemented yet )*/
	    /* this is done if beta>0.0 */
	    
	    for(k=0; k<m; k++){
	      c.setCINC(k, ( (c.getCC(k) - c.getContent(k)) * (double)iprd/(double)fprd ) );
	      //System.out.println("cinc["+ k + "]=" + c.getCINC(k) );
	    }
	    
	    /* p=f0 in c code!!! */
	    if( p1 != 0.0 && f0 != 0.0 ) {
	      inc = (f0 - p1) * (double)iprd/(double)fprd;	      
	    } else {
	      inc = 0.0;
	      pc = f0;
	      p1 = 0.0; 
	    }
	    
	    /* Here i need to generate both xp:pulse and xn:noise signals separately  */ 
	    gauss = false; /* Mixed excitation works better with nomal noise */
	  
	    /* Generate fperiod samples per feature vector, normally 80 samples per frame */
	    //p1=0.0;
	    gauss=false;
	    for(j=fprd-1, i=(iprd+1)/2; j>=0; j--) {
          if(p1 == 0.0) {
            if(gauss)
              x = rand.nextGaussian();  /* returns double, gaussian distribution mean=0.0 and var=1.0 */
            else
              x = uniformRand(); /* returns 1.0 or -1.0 uniformly distributed */
            
            if(mixedExcitation) {
              xn = x;
              xp = 0.0;
            }
          } else {
        	  if( (pc += 1.0) >= p1 ){
        	    x = Math.sqrt(p1);
        	    pc = pc - p1;
        	  } else
        	    x = 0.0;
        	  
              if(mixedExcitation) {
        	    xp = x;
        	    if(gauss)
        		  xn = rand.nextGaussian();
        	    else
        	  	  xn = uniformRand();
              }
          } 
        	  
          /* apply the shaping filters to the pulse and noise samples */
          /* i need memory of at least for M samples in both signals */
          if(mixedExcitation) {
            fxp = 0.0;
            fxn = 0.0;
            for(k=orderM-1; k>0; k--) {
        	  fxp += hp[k] * xpulseSignal[k];
        	  fxn += hn[k] * xnoiseSignal[k];
        	  xpulseSignal[k] = xpulseSignal[k-1];
        	  xnoiseSignal[k] = xnoiseSignal[k-1];
            }
            fxp += hp[0] * xp;
            fxn += hn[0] * xn;
            xpulseSignal[0] = xp;
            xnoiseSignal[0] = xn;
          
            /* x is a pulse noise excitation and mix is mixed excitation */
            mix = fxp+fxn;
      
            /* comment this line if no mixed excitation, just pulse and noise */
            x = mix;   /* excitation sample */
          }
                    
          if(x != 0.0 )
        	x *= Math.exp(c.getContent(0));
                   
          x = mlsadf(x, m, a, aa);
          
          audio_double[s_double] = x;
          s_double++;
          
          if((--i) == 0 ) {
            p1 += inc;
        	for(k=0; k<m; k++){
        	  c.setContent(k, c.getContent(k) + c.getCINC(k));
        	  //System.out.println("   " + k + "=" + c.get(k));
        	}
        	i = iprd;
          }
          
	    } /* for each sample in a period fprd */
	    
        p1 = f0;
        
        /* move elements in c */
        for(i=0; i<m; i++)
          c.setContent(i, c.getCC(i));
	  
	  } /* for each mcep frame */
	
      logger.info("Finish processing " + mcepframe + " mcep frames." + "  Num samples in bytes s=" + s );
    	
	  float sampleRate = 16000.0F;  //8000,11025,16000,22050,44100
      int sampleSizeInBits = 16;  //8,16
      int channels = 1;     //1,2
      boolean signed = true;    //true,false
      boolean bigEndian = false;  //true,false
      AudioFormat af = new AudioFormat(
    		  sampleRate,
    		  sampleSizeInBits,
    		  channels,
    		  signed,
    		  bigEndian);
      
      long lengthInSamples = (audio_double.length * 2 ) / (sampleSizeInBits/8);
      logger.info("length in samples=" + lengthInSamples );
      
      /* Normalise the signal before return, this will normalise between 1 and -1 */
      MaxSample = MathUtils.getAbsMax(audio_double);
      for (i=0; i<audio_double.length; i++)
         audio_double[i] = audio_double[i] / MaxSample;
      
      DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);
      return oais;
	  
	} /* method htsMLSAVocoder() */
	
    
	/** uniform_rand: generate uniformly distributed random numbers 1 or -1 */
	private double uniformRand() {	
	  double x;
	  x = rand.nextDouble(); /* double uniformly distributed between 0.0 <= Math.random() < 1.0.*/
	  if(x >= 0.5)
	    return 1.0;
	  else
		return -1.0;
	}
	
        
	
	/** mc2b: transform mel-cepstrum to MLSA digital fillter coefficients */
	private void mc2b(double mc[], int m, double a ) {
	  int i;
      c.setCC(m-1, mc[m-1]);
	  for(i=m-2; i>=0; i--) {
	    c.setCC(i, mc[i] - a * c.getCC(i+1));
	  }
	}
	
	/** mlsadf: HTS Mel Log Spectrum Approximation filter */
	private double mlsadf(double x, int m, double a, double aa) {
	  x = mlsadf1(x, a, aa);  
	  x = mlsadf2(x, m-1, a, aa);
	  return x;	
	}
	
	/** mlsdaf1:  */
	private double mlsadf1(double x, double a, double aa) {
	  double v;
	  double out = 0.0;
	  int i;
	  
	  for(i=pd; i>=1; i--) {
	    c.setD1(i, (aa*c.getD1((pt1+i)-1) + a*c.getD1(i)));
		c.setD1(pt1+i, (c.getD1(i) * c.getContent(1)) );
		v = c.getD1(pt1+i) * pade[ppade+i];
		if(i == 1 || i == 3 || i == 5)
		  x += v;
		else 
		  x += -v;
		out += v;
	  }
	  c.setD1(pt1+0, x);
	  out += x;
	  
	  return(out);
	  
	}

	/** mlsdaf2: */
	private double mlsadf2(double x, int m, double a, double aa) {
	  double v;
	  double out = 0.0;
	  int i; 
	  
	  for(i=pd; i>=1; i--) {
		c.setD1(pt2+i, mlsafir(c.getD1((pt2+i)-1),m,a,aa,pt3[i]));
		v = c.getD1(pt2+i) * pade[ppade+i];

	    if(i == 1 || i == 3 || i == 5)
		  x += v;
		else
		  x += -v;
		out += v;
		
	  }
	  c.setD1(pt2+0, x);
	  out += x;
	  return out;
	  
	}
	
	/** mlsafir: */
	private double mlsafir(double x, int m, double a, double aa, int _pt3 ) {
	  double y = 0.0;
	  int i;
	 
	 c.setD1(_pt3+0, x);
	 c.setD1(_pt3+1, (aa*c.getD1(_pt3+0) + (a*c.getD1(_pt3+1)) ));

	  for(i=2; i<=m; i++)
		c.setD1(_pt3+i, (c.getD1(_pt3+i) + (a*(c.getD1(_pt3+i+1) - c.getD1((_pt3+i)-1) ) ) ) );
		  
	  for(i=2; i<=m; i++) 
		y += c.getD1(_pt3+i) * c.getContent(i);
	 	  
	  for(i=m+1; i>1; i--)
		c.setD1(_pt3+i, c.getD1((_pt3+i)-1));
	
	  return(y);
	}
	
	
}  /* class Vocoder */















































