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
import java.util.Scanner;
import java.util.Random;
import java.io.*;
import javax.sound.sampled.*;


/**
 * Synthesis of speech out of speech parameters.
 * Mixed excitation MLSA vocoder. 
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan 
 */
public class Vocoder {
  
	public static final int RANDMAX = 32767;
	
	public static final int IPERIOD = 1;
	public static final int SEED    = 1;
	public static final int B0      = 0x00000001;
	public static final int B28     = 0x10000000;
	public static final int B31     = 0x80000000;
	public static final int B31_    = 0x7fffffff;
	public static final int Z       = 0x00000000;
	
	/* OJO!! check what this value should be??? 
	 * for the moment i left like this...., it seems that it is not used during c compilation */
	// public static final boolean HTS_EMBEDDED = false; 	
	//if HTS_EMBEDDED ){
    //  public static final int GAUSS     = 0;
	//  public static final int PADEORDER = 4;  /* pade order (for MLSA filter) */
	//  public static final int IRLENG    = 64; /* length of impulse response */
	//} else {
	public static final boolean GAUSS = true;
	public static final int PADEORDER = 5;
	public static final int IRLENG    = 96;
	//}
	
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

	/* postfiltering variables */
	//private int size;
	/* private double d[]; 
	 * private double g[];
	 * the previous replaced by a class SlideVector d, also includes pointers:
	 * pt1 = &d[pd+1] in function mlsadf1() 
	 * pt2 = &d[pd * (m+2)]; */
	//private SlideVector d;
	 
	/* private double mc[];
	 * private double cep[];
	 * private double ir[]; 
	 * the previous replaced by a class SlideVector mc */
	//private SlideVector mc;
	
	//private int o;
	//private int irleng;  

	/* mixed excitation variables */  
	private int numM;          /* Number of bandpass filters for mixed excitation */
	private int orderM;        /* Order of filters for mixed excitation */
	private double h[][];      /* filters for mixed excitation */  
	private double xp_sig[];   /* the size of this should be orderM */
	private double xn_sig[];   /* the size of this should be orderM */
	
	
	/** The initialisation of VocoderSetup should be done when there is already 
	  * information about the number of feature vectors to be processed,
	  * size of the mcep vector file, etc. */
	private void InitVocoder(int mcep_order, int mcep_vsize, HMMData hts_data) {
		int vector_size;
		fprd  = hts_data.FPERIOD();
		rate  = hts_data.RATE();
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
		System.out.println("C SIZE="+ vector_size + "  mcep_vsize=" + mcep_vsize );
		c = new SlideVector(vector_size, mcep_order,pd);
		pt1 = pd+1;
		pt2 = ( 2 * (pd+1)) + (pd * (mcep_order+2));
		pt3 = new int[pd+1];
		for(int i=pd; i>=1; i--)
		  pt3[i] = ( 2 * (pd+1)) + ((i-1)*(mcep_order+2));

		p1 = -1;
 		pc = 0.0;  /* double */
	
		/* for postfiltering */
		//irleng = IRLENG;

	} /* method InitVocoder */
	
	
	/** Initialisation for mixed excitation : it loads the filter taps are read from 
	 * MixFilterFile specified in the configuration file. */
	private void InitMixedExcitation(HMMData hts_data) {
		
		numM = hts_data.get_numFilters();
		orderM = hts_data.get_orderFilters();
		h = new double[numM][orderM];
		xp_sig = new double[orderM];
		xn_sig = new double[orderM];

		/* get the filter coefficients */
		Scanner s = null;
		int i,j;
		try {
			s = new Scanner(new BufferedReader(new FileReader(hts_data.MixFiltersFile())));

			for(i=0; i<numM; i++){
				for(j=0; j<orderM; j++) {
					if (s.hasNextDouble()) {
						h[i][j] = s.nextDouble();
						//System.out.println("h["+i+"]["+j+"]="+h[i][j]);
					}
					else
						System.err.println("VocoderSetup: error reading fiter taps file =" + hts_data.MixFiltersFile());
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("VocoderSetup FileNotFoundException: " + e.getMessage());
		} finally {
			s.close();
		} 	

		/* initialise xp_sig and xn_sig */
		for(i=0; i<orderM; i++)
		  xp_sig[i] = xn_sig[i] = 0;	
		
	} /* method InitMixedExcitation */

	
	/** 
	 * HTS_MLSA_Vocoder: Synthesis of speech out of mel-cepstral coefficients. 
	 * This procedure uses the parameters generated in pdf2par stored in:
	 *   PStream mceppst: Mel-cepstral coefficients
     *   PStream strpst : Filter bank stregths for mixed excitation
     *   PStream magpst : Fourier magnitudes ( OJO!! this is not used yet)
     *   PStream lf0pst : Log F0  
     */
	public AudioInputStream HTS_MLSA_Vocoder(ParameterGeneration pdf2par, HMMData hts_data) {

	  double inc, x;
	  short sx;
	  double xp,xn,fxp,fxn,mix;  /* samples for pulse and for noise and the filtered ones */
	  //float x_exc,x_mix;         /* for saving samples of excitation and mix-excitation   */ 
	  int i, j, k, m, s, mcepframe, lf0frame; 
	  double a = hts_data.ALPHA();
	  double aa = 1-a*a;
	 // double beta = hts_data.BETA();
	  int audio_size;  /* audio size in samples, calculated as num frames * frame period */
	  byte[] audio = null;    /* buffer for audio data */
      ModelSet ms = hts_data.getModelSet();
	    
      double f0;
      double mc[];  /* feature vector for a particular frame */
	  double hp[];  /* pulse shaping filter, it is initialised once it is known orderM */  
	  double hn[];  /* noise shaping filter, it is initialised once it is known orderM */  
	  
	  /* Initialise vocoder and mixed excitation, once initialised it is known the order
	   * of the filters so the shaping filters hp and hn can be initialised. */
	  m = pdf2par.get_mcep_order();
	  mc = new double[m];
	  InitVocoder(m-1, ms.get_mcepvsize()-1, hts_data);  /* m-1 because the SlideVector offsets count from 0-->24 */
	  InitMixedExcitation(hts_data);
	  hp = new double[orderM];  
	  hn = new double[orderM];   
      c.clearContent();   /* Clear content of SlideVector c, should be done if this function is
                             called more than once with a new set of generated parameters. */
	    
	  /* Check if the number of filters is equal to the order of strpst 
	   * i.e. the number of filters is equal to the number of generated strengths per frame. */
	  if(numM != pdf2par.get_str_order()) {
		System.err.println("HTS_MLSA_Vocoder: error numM=" + numM + " in configuration file is different of generated str order=" + pdf2par.get_str_order());
	  }
	  
	 // try{
    
	  // these binaries for testing...
     // DataOutputStream data_out = new DataOutputStream (new FileOutputStream ("/project/mary/marcela/MaryClientUserHMM/gen/tmp.raw"));
     // DataOutputStream pulse_out = new DataOutputStream (new FileOutputStream ("/project/mary/marcela/MaryClientUserHMM/gen/pulse.bin"));
     // DataOutputStream mix_out = new DataOutputStream (new FileOutputStream ("/project/mary/marcela/MaryClientUserHMM/gen/mix.bin"));
      
	  /* _______________________Synthesize speech waveforms_____________________ */
	  /* generate Nperiod samples per mcepframe */
      s = 0;   /* number of samples */
      audio_size = (pdf2par.get_mcep_T()) * (fprd) ;
	  audio = new byte[audio_size*2];  /* initialise buffer for audio */
	  for(mcepframe=0,lf0frame=0; mcepframe<pdf2par.get_mcep_T(); mcepframe++) {
       
		/* get current feature vector mc */ 
		for(i=0; i<m; i++) 
		  mc[i] = pdf2par.get_mcep(mcepframe, i);	  
		 
        /* f0 modification */
	    if(pdf2par.get_voiced(mcepframe)){
	      f0 = hts_data.F0_STD() * Math.exp(pdf2par.get_lf0(lf0frame, 0)) + hts_data.F0_MEAN(); 
	      lf0frame++;
	    }
	    else
	      f0 = 0.0;
	   
	    //System.out.println("\nmcepframe=" + mcepframe + " --> f0=" + f0 );
	  
	    /* shaping filters for this frame */
        for(j=0; j<orderM; j++) {
          hp[j] = hn[j] = 0.0;
          for(i=0; i<numM; i++) {
        	//System.out.println("str=" + pdf2par.get_str(mcepframe, i) + "  h[i][j]=" + h[i][j])  ;
        	hp[j] += pdf2par.get_str(mcepframe, i) * h[i][j];
        	hn[j] += ( 1 - pdf2par.get_str(mcepframe, i) ) * h[i][j];
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
	   
	    /* postfiltering  ( OJO!!! not implemented yet )*/
	    /* this is done if beta>0.0 */
	    
	    for(k=0; k<m; k++){
	      c.setCINC(k, ( (c.getCC(k) - c.get(k)) * (double)iprd/(double)fprd ) );
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
	    gauss = false; /* OJO! mixed excitation works better with nomal noise??? check!!! */
	  
	    /* Generate fperiod samples per feature vector, normally 80 samples per frame */
	    //p1=0.0;
	    gauss=false;
	    for(j=fprd-1, i=(iprd+1)/2; j>=0; j--) {
          if(p1 == 0.0) {
            if(gauss)
              x = rand.nextGaussian();  /* returns double, gaussian distribution mean=0.0 and var=1.0 */
            else
              x = uniform_rand(); /* returns 1.0 or -1.0 uniformly distributed */
            xn = x;
            xp = 0.0;            
          } else {
        	  if( (pc += 1.0) >= p1 ){
        	    x = Math.sqrt(p1);
        	    pc = pc - p1;
        	  } else
        	    x = 0.0;
        	  
        	  xp = x;
        	  if(gauss)
        		xn = rand.nextGaussian();
        	  else
        		xn = uniform_rand();
          } 
        	  
          /* apply the shaping filters to the pulse and noise samples */
          /* i need memory of at least for M samples in both signals */
          fxp = 0.0;
          fxn = 0.0;
          for(k=orderM-1; k>0; k--) {
        	fxp += hp[k] * xp_sig[k];
        	fxn += hn[k] * xn_sig[k];
        	xp_sig[k] = xp_sig[k-1];
        	xn_sig[k] = xn_sig[k-1];
          }
          fxp += hp[0] * xp;
          fxn += hn[0] * xn;
          xp_sig[0] = xp;
          xn_sig[0] = xn;
          
          /* x is a pulse noise excitation and mix is mixed excitation */
          mix = fxp+fxn;
         // pulse_out.writeFloat((float)x);
         // mix_out.writeFloat((float)mix);
      
          /* comment this line if no mixed excitation, just pulse and noise */
          x = mix;   /* excitation sample */
                    
          if(x != 0.0 )
        	x *= Math.exp(c.get(0));
                   
          //System.out.println("j:" + j + "  x=" + x + "  c[0]=" + c.get(0));
          x = mlsadf(x, m, a, aa);
          
          /* i need the low and high bytes!!!  */
          sx = (short)x;
          
          /* BIG-ENDIAN  bigEndian = true*/
          //audio[s]   = (byte)(sx >>> 8); /* byte high */
          //audio[s+1] = (byte)sx;         /* byte low, cast implies & 0xff*/
          //s = s+2;                       /* increment number of samples in bytes */
          
          /* LITTLE-ENDIAN bigEndian = false */
          audio[s]   = (byte)sx;         /* byte low, cast implies & 0xff*/
          audio[s+1] = (byte)(sx >>> 8); /* byte high */
          s = s+2;                       /* increment number of samples in bytes */
          
          /* write speech sample in a binary file */
        //  data_out.writeShort((short)x);
          
          if((--i) == 0 ) {
            p1 += inc;
        	for(k=0; k<m; k++){
        	  c.set(k, c.get(k) + c.getCINC(k));
        	  //System.out.println("   " + k + "=" + c.get(k));
        	}
        	i = iprd;
          }
          
	    } /* for each sample in a period fprd */
	    
        p1 = f0;
        
        /* move elements in c */
        for(i=0; i<m; i++)
          c.set(i, c.getCC(i));
	  
	  } /* for each mcep frame */
	  
      // Close file when finished with it..
     // mix_out.close();
     // pulse_out.close();
     // data_out.close();
      System.out.println("Finish processing " + mcepframe + " mcep frames." + "  Num samples in bytes s=" + s );
      //System.out.println("Finish generating: /project/mary/marcela/MaryClientUserHMM/gen/tmp.raw");
      //System.out.println("Finish generating: /project/mary/marcela/MaryClientUserHMM/gen/pulse.bin");
      //System.out.println("Finish generating: /project/mary/marcela/MaryClientUserHMM/gen/mix.bin");
	  
	 // }
	   // catch (IOException e) {
	   //    System.out.println ("IO exception = " + e );
	 // }
		
	  ByteArrayInputStream bais = new ByteArrayInputStream(audio);
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
      
      long lengthInSamples = audio.length / (sampleSizeInBits/8);
      System.out.println("lengthInSamples=" + lengthInSamples + "  audio.length=" + audio.length);
      AudioInputStream ais = new AudioInputStream(bais, af, lengthInSamples);
     
      return ais;
	  
	} /* method HTS_MLSA_Vocoder() */
	
	
	/** uniform_rand: generate uniformly distributed random numbers 1 or -1 */
	private double uniform_rand() {	
	  double x;
	  x = rand.nextDouble(); /* double uniformly distributed between 0.0 <= Math.random() < 1.0.*/
	  if(x > 0.5)
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
		c.setD1(pt1+i, (c.getD1(i) * c.get(1)) );
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
		y += c.getD1(_pt3+i) * c.get(i);
	 	  
	  for(i=m+1; i>1; i--)
		c.setD1(_pt3+i, c.getD1((_pt3+i)-1));
	
	  return(y);
	}
	
	
}  /* class Vocoder */















































