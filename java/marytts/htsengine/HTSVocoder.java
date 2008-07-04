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

package marytts.htsengine;


import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Locale;
import java.util.Scanner;
import java.util.Random;
import java.io.*;
import javax.sound.sampled.*;

import marytts.util.MathUtils;
import marytts.util.audio.BufferedDoubleDataSource;
import marytts.util.audio.DDSAudioInputStream;
import marytts.util.audio.AudioPlayer;
import marytts.util.audio.DDSAudioInputStream;
import marytts.util.audio.BufferedDoubleDataSource;
import marytts.util.audio.AudioDoubleDataSource;
import marytts.util.MathUtils;
import marytts.util.MathUtils.Complex;
import marytts.signalproc.FFTMixedRadix;
import marytts.signalproc.FFT;

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

    private double C[];  
    private int cc;     /* offsets for vector c */ 
    private int cinc;   /* offsets for vector c */ 
    private int d1;     /* offsets for vector c */ 
    
    private double rate;
    int pt1;
    int pt2;
    int pt3[];
    
    /* for postfiltering */
    private int size;
    private double d[];
    private int g;    /* offset applied to the index of d */
    private double mc[];
    private int cep;  /* offset applied to the index of mc */
    private int ir;   /* offset applied to the index of mc */
    private int o;
    private int irleng;

    /* mixed excitation variables */  
    private int numM;          /* Number of bandpass filters for mixed excitation */
    private int orderM;        /* Order of filters for mixed excitation */
    private double h[][];      /* filters for mixed excitation */  
    private double xpulseSignal[];   /* the size of this should be orderM */
    private double xnoiseSignal[];   /* the size of this should be orderM */
    private boolean mixedExcitation = false;
    private boolean fourierMagnitudes = false;
    
    
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
        C = new double[vector_size];
        /* offsets for vector C:
        * vector_size = ( 3*(m+1) + 3*(pd+1) + pd*(m+2) ), m=mcep_vsize;
        * vector_size = 623 with mcep_vsize=74
        * C    =                              --> C[0]...  
        * cc   = c+m+1                        --> C[75]... 
        * cinc = cc+m+1   = (c+m+1)+m+1       --> C[150]... 
        * d1   = cinc+m+1 = ((c+m+1)+m+1)+m+1 --> C[225]... */
        cc   = 1*(mcep_order+1);
        cinc = 2*(mcep_order+1);
        d1   = 3*(mcep_order+1);
        
        pt1 = pd+1;
        pt2 = ( 2 * (pd+1)) + (pd * (mcep_order+2));
        pt3 = new int[pd+1];
        for(int i=pd; i>=1; i--)
          pt3[i] = ( 2 * (pd+1)) + ((i-1)*(mcep_order+2));

        p1 = -1;
        pc = 0.0;  /* double */
        
        /* for postilter */
        mc = null;
        o = 0;
        d = null;
        irleng = IRLENG;
        
    
    } /* method initVocoder */
    
    
    /** Initialisation for mixed excitation : it loads the filter taps are read from 
     * MixFilterFile specified in the configuration file. */
    private void initMixedExcitation_old(HMMData htsData) 
    throws Exception {
        
        numM = htsData.getNumFilters();
        orderM = htsData.getOrderFilters();
        h = new double[numM][orderM];
        xpulseSignal = new double[orderM];
        xnoiseSignal = new double[orderM];
        String line;
        
        /* get the filter coefficients */
        Scanner s = null;
        int i,j;
        try {
            s = new Scanner(new BufferedReader(new FileReader(htsData.getMixFiltersFile())));
            s.useLocale(Locale.US);

            while ( s.hasNext("#") ) {  /* skip comment lines */
                line = s.nextLine();
                //System.out.println("comment: " + line ); 
            }
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
        double [] audio_double = null;
        
        audio_double = htsMLSAVocoder(pdf2par.getlf0Pst(), pdf2par.getMcepPst(), pdf2par.getStrPst(), pdf2par.getMagPst(),
                                      pdf2par.getVoicedArray(), htsData);
        
        long lengthInSamples = (audio_double.length * 2 ) / (sampleSizeInBits/8);
        logger.info("length in samples=" + lengthInSamples );
        
        /* Normalise the signal before return, this will normalise between 1 and -1 */
        double MaxSample = MathUtils.getAbsMax(audio_double);
        for (int i=0; i<audio_double.length; i++)
           audio_double[i] = 0.3 * ( audio_double[i] / MaxSample ); 
        
        DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);
        return oais;
        
        
    } /* method htsMLSAVocoder() */
    
    
    public double [] htsMLSAVocoder(HTSPStream lf0Pst, HTSPStream mcepPst, HTSPStream strPst, HTSPStream magPst, 
                                    boolean [] voiced, HMMData htsData)
    throws Exception {

      double inc, x, MaxSample;
      short sx;
      double xp=0.0,xn=0.0,fxp,fxn,mix;  /* samples for pulse and for noise and the filtered ones */
      int i, j, k, m, s, mcepframe, lf0frame, s_double; 
      double alpha = htsData.getAlpha();
      double beta  = htsData.getBeta();
      double aa = 1-alpha*alpha;
      int audio_size;                    /* audio size in samples, calculated as num frames * frame period */
      double [] audio_double = null;
      double [] magPulse = null;         /* pulse generated from Fourier magnitudes */
      int magSample, magPulseSize;
      boolean aperiodicFlag = false;
      
      /* --------------------------------------------------------------------------------
       * these variables for allow saving excitation and mixed excitation in a binary file 
       * if true please provide an appropriate path/name where to save these files, 
       * the generated files can be seen using SPTK tools. */
      boolean debug = false;
      DataOutputStream data_out = null;
      DataOutputStream data_out_mix = null;
      String excFile = "/project/mary/marcela/hmm-gen-experiment/exc.bin";
      String mixExcFile = "/project/mary/marcela/hmm-gen-experiment/exc-mix.bin";
      /* --------------------------------------------------------------------------------*/
      
        
      double f0, f0Std, f0Shift, f0MeanOri;
      double mcp[] = null;  /* feature vector for a particular frame */
      double hp[] = null;   /* pulse shaping filter, it is initialised once it is known orderM */  
      double hn[] = null;   /* noise shaping filter, it is initialised once it is known orderM */  
      
      /* Initialise vocoder and mixed excitation, once initialised it is known the order
       * of the filters so the shaping filters hp and hn can be initialised. */
      m = mcepPst.getOrder();
      mcp = new double[m];
      initVocoder(m-1, mcepPst.getVsize()-1, htsData);  /* Vsize-1 because the SlideVector offsets count from 0-->24 */
      
      mixedExcitation = htsData.getUseMixExc();
      fourierMagnitudes = htsData.getUseFourierMag();
      
     
      if( mixedExcitation && htsData.getPdfStrFile() != null ) {
        
        numM = htsData.getNumFilters();
        orderM = htsData.getOrderFilters();
        
        xpulseSignal = new double[orderM];
        xnoiseSignal = new double[orderM];
        /* initialise xp_sig and xn_sig */
        for(i=0; i<orderM; i++)
          xpulseSignal[i] = xnoiseSignal[i] = 0;    
        
        h = htsData.getMixFilters();
        hp = new double[orderM];  
        hn = new double[orderM]; 
              
        //Check if the number of filters is equal to the order of strpst 
        //i.e. the number of filters is equal to the number of generated strengths per frame.
        if(numM != strPst.getOrder()) {
          logger.debug("htsMLSAVocoder: error num mix-excitation filters =" + numM + " in configuration file is different from generated str order=" + strPst.getOrder());
          throw new Exception("htsMLSAVocoder: error num mix-excitation filters = " + numM + " in configuration file is different from generated str order=" + strPst.getOrder());
        }
        logger.info("HMM speech generation with mixed-excitation.");
      } else
        logger.info("HMM speech generation without mixed-excitation.");  
      
      if( fourierMagnitudes && htsData.getPdfMagFile() != null)
        logger.info("Pulse generated with Fourier Magnitudes.");
      else
        logger.info("Pulse generated as a unit pulse.");
      
      if(beta != 0.0)
        logger.info("Postfiltering applied with beta=" + beta);
      else
        logger.info("No postfiltering applied.");
          
      if(debug){
        data_out = new DataOutputStream (new FileOutputStream (excFile));
        data_out_mix = new DataOutputStream (new FileOutputStream (mixExcFile));
      }
      
      /* Clear content of c, should be done if this function is
      called more than once with a new set of generated parameters. */
      for(i=0; i< C.length; i++)
        C[i]=0.0;
     
    
      f0Std = htsData.getF0Std();
      f0Shift = htsData.getF0Mean();
      f0MeanOri = 0.0;

      for(mcepframe=0,lf0frame=0; mcepframe<mcepPst.getT(); mcepframe++) {
        if(voiced[mcepframe]){  
          f0MeanOri = f0MeanOri + Math.exp(lf0Pst.getPar(lf0frame, 0));
          //System.out.print(Math.exp(lf0Pst.getPar(lf0frame, 0)) + "  ");
          lf0frame++;
        }
        //else
          //System.out.print("0.0  ");  
      }
      f0MeanOri = f0MeanOri/lf0frame;
   
      /* _______________________Synthesize speech waveforms_____________________ */
      /* generate Nperiod samples per mcepframe */
      s = 0;   /* number of samples */
      s_double = 0;
      audio_size = (mcepPst.getT()) * (fprd) ;
      audio_double = new double[audio_size];  /* initialise buffer for audio */
      magSample = 1;
      magPulseSize = 0;
      for(mcepframe=0,lf0frame=0; mcepframe<mcepPst.getT(); mcepframe++) {
       
        /* get current feature vector mcp */ 
        for(i=0; i<m; i++)
          mcp[i] = mcepPst.getPar(mcepframe, i); 
        
        //System.out.println("htsData.getF0Std()=" + htsData.getF0Std() + " htsData.getF0Mean()="+ htsData.getF0Mean());
        
        /* f0 modification through the MARY audio effects */
        if(voiced[mcepframe]){
          f0 = f0Std * Math.exp(lf0Pst.getPar(lf0frame, 0)) + (1-f0Std) * f0MeanOri + f0Shift;       
          lf0frame++;
          if(f0 < 0.0)
            f0 = 0.0;
        }
        else{
          f0 = 0.0;          
        }
        //System.out.println("mcepframe=" + mcepframe +  "  f0=" + f0);
         
        /* if mixed excitation get shaping filters for this frame */
        if(mixedExcitation){
          for(j=0; j<orderM; j++) {
            hp[j] = hn[j] = 0.0;
            for(i=0; i<numM; i++) {
              //System.out.println("str=" + pdf2par.get_str(mcepframe, i) + "  h[i][j]=" + h[i][j])  ;
              hp[j] += strPst.getPar(mcepframe, i) * h[i][j];
              hn[j] += ( 1 - strPst.getPar(mcepframe, i) ) * h[i][j];
            }
          }
        }
        
        /* f0 -> pitch , in original code here it is used p, so f0=p in the c code */
        //System.out.println("\nmcepframe=" + mcepframe + "  f0=" + f0 );
        if(f0 != 0.0)
           f0 = rate/f0;
        
        if( p1 < 0 ) {  
          p1   = f0;           
          pc   = p1;           
        }
        //System.out.println("  [f0=(rate/f0)=  >>> " + f0 + " <<<  ]  ***pc=" + pc + "  p1=" + p1 );
        
        /* mc2b: transform mel-cepstrum to MLSA digital fillter coefficients */
         mc2b(mcp,m,alpha);
       
        /* postfiltering, this is done if beta>0.0 */
         postfilter(mcp, m, alpha, beta);
         
        
        for(k=0; k<m; k++){
          C[cinc+k] = ( C[cc+k] - C[k] ) * (double)iprd/(double)fprd;  
          //System.out.println("cinc["+ k + "]=" + c.getCINC(k) );
        }
        
        /* p=f0 in c code!!! */
        if( p1 != 0.0 && f0 != 0.0 ) {
          inc = (f0 - p1) * (double)iprd/(double)fprd;
          //System.out.println("  inc=(f0-p1)/80=" + inc );
        } else {
          inc = 0.0;
          pc = f0;
          p1 = 0.0; 
          //System.out.println("  inc=" + inc + "  ***pc=" + pc + "  p1=" + p1);
        }
        
        
        
        /* Here i need to generate both xp:pulse and xn:noise signals separately  */ 
        gauss = false; /* Mixed excitation works better with nomal noise */
      
        /* Generate fperiod samples per feature vector, normally 80 samples per frame */
        //p1=0.0;
        gauss=false;
        for(j=fprd-1, i=(iprd+1)/2; j>=0; j--) {
//          System.out.print("j=" + j + "  i=" + i);  
            
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
                //System.out.println("\n    j=" + j + "  ***pc=" + pc + "  >=   p1=" + p1); 
                if(fourierMagnitudes){
                  //logger.debug("Generating pulse: previousf0=" + Math.round(p1)); 
                  /* jitter is applied just in voiced frames when the stregth of the first band is < 0.5*/
                  /* this will work just if Radix FFT is used */  
                  /*if(strPst.getPar(mcepframe, 0) < 0.5)
                    aperiodicFlag = true;
                  else
                    aperiodicFlag = false;          
                  magPulse = genPulseFromFourierMagRadix(magPst, mcepframe, p1, aperiodicFlag);
                  */
                    
                  magPulse = genPulseFromFourierMag(magPst, mcepframe, p1, aperiodicFlag);
                  magSample = 0;
                  magPulseSize = magPulse.length;
                  x = magPulse[magSample];
                  magSample++;
                } else
                 x = Math.sqrt(p1);  
                
                pc = pc - p1;
               // System.out.println("    START COUNTER ***pc=" + pc + "  p1=" + p1);
              } else {
                
                if(fourierMagnitudes){
                  if(magSample >= magPulseSize ){ 
                    x = 0.0;
                    //System.out.println("  magSample = 0.0");
                  }
                  else
                    x = magPulse[magSample];                
                  magSample++;
                } else
                   x = 0.0;                 
              }
              
              if(mixedExcitation) {
                xp = x;
                if(gauss)
                  xn = rand.nextGaussian();
                else
                  xn = uniformRand();
              }
          } 
          //System.out.print("    x=" + x);
          
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
      
            if(debug){
              data_out.writeFloat((float)x);
              data_out_mix.writeFloat((float)mix);
            }
            
            /* comment this line if no mixed excitation, just pulse and noise */
            x = mix;   /* excitation sample */
          }
          //System.out.print(x + " ");          
          if(x != 0.0 )
            x *= Math.exp(C[0]);
 //         System.out.print("    [X=" + x + "]  ");
                   
          x = mlsadf(x, m, alpha, aa);
        
        
          
          audio_double[s_double] = x;
          s_double++;
          
          if((--i) == 0 ) {
            p1 += inc;
            for(k=0; k<m; k++){
              C[k] += C[cinc+k];  
              //System.out.println("   " + k + "=" + c.get(k));
            }
            i = iprd;
          }
 //         System.out.println("  i=" + i + "  inc=" + inc + "  pc=" + pc + "  p1=" + p1);
          
        } /* for each sample in a period fprd */
        
       // System.out.println();
        
        p1 = f0;
        
        /* move elements in c */
        for(i=0; i<m; i++){
          C[i] = C[cc+i];  
        }
      
      } /* for each mcep frame */
    
      if(debug){
        data_out.close();
        data_out_mix.close();
      }
      
      logger.info("Finish processing " + mcepframe + " mcep frames." + "  Num samples in bytes s=" + s );
        
      return(audio_double);
      
    } /* method htsMLSAVocoder() */
    

    
    public double [] htsMLSAVocoder_residual(HTSPStream lf0Pst, HTSPStream mcepPst, HTSPStream strPst, HTSPStream magPst, 
            boolean [] voiced, HMMData htsData, String resFile )
    throws Exception {

      double inc, x, MaxSample;
      short sx;
      double xp=0.0,xn=0.0,fxp,fxn,mix;  /* samples for pulse and for noise and the filtered ones */
      int i, j, k, m, s, mcepframe, lf0frame, s_double; 
      double a = htsData.getAlpha();
      double aa = 1-a*a;
      int audio_size;  /* audio size in samples, calculated as num frames * frame period */
      double [] audio_double = null;
      double [] magPulse = null;      /* pulse generated from Fourier magnitudes */
      int magSample, magPulseSize;
      boolean aperiodicFlag = false;
      
      /* --------------------------------------------------------------------------------
       * these variables for allow saving excitation and mixed excitation in a binary file 
       * if true please provide an appropriate path/name where to save these files, 
       * the generated files can be seen using SPTK tools. */
      boolean debug = true;
      DataOutputStream data_out = null;
      DataOutputStream data_out_mix = null;
      String excFile = "/project/mary/marcela/hmm-gen-experiment/exc.bin";
      String mixExcFile = "/project/mary/marcela/hmm-gen-experiment/exc-mix.bin";
      /* --------------------------------------------------------------------------------*/
      
        
      double f0, f0Std, f0Shift, f0MeanOri;
      double mcp[] = null;  /* feature vector for a particular frame */
      double hp[] = null;  /* pulse shaping filter, it is initialised once it is known orderM */  
      double hn[] = null;  /* noise shaping filter, it is initialised once it is known orderM */  
      
      /* Initialise vocoder and mixed excitation, once initialised it is known the order
       * of the filters so the shaping filters hp and hn can be initialised. */
      m = mcepPst.getOrder();
      mcp = new double[m];
      initVocoder(m-1, mcepPst.getVsize()-1, htsData);  /* Vsize-1 because the SlideVector offsets count from 0-->24 */
      
      mixedExcitation = htsData.getUseMixExc();
      fourierMagnitudes = htsData.getUseFourierMag();
      
      if( mixedExcitation && htsData.getPdfStrFile() != null ) {
        
       numM = htsData.getNumFilters();
       orderM = htsData.getOrderFilters();
          
       xpulseSignal = new double[orderM];
       xnoiseSignal = new double[orderM];
       /* initialise xp_sig and xn_sig */
       for(i=0; i<orderM; i++)
         xpulseSignal[i] = xnoiseSignal[i] = 0;    
          
        h = htsData.getMixFilters();
        hp = new double[orderM];  
        hn = new double[orderM]; 
        
        /* Check if the number of filters is equal to the order of strpst 
         * i.e. the number of filters is equal to the number of generated strengths per frame. */
        if(numM != strPst.getOrder()) {
          logger.debug("htsMLSAVocoder: error num mix-excitation filters =" + numM + " in configuration file is different from generated str order=" + strPst.getOrder());
          throw new Exception("htsMLSAVocoder: error num mix-excitation filters = " + numM + " in configuration file is different from generated str order=" + strPst.getOrder());
        }
        logger.info("HMM speech generation with mixed-excitation.");
      } else
        logger.info("HMM speech generation without mixed-excitation.");  
 
      if(debug){
        data_out = new DataOutputStream (new FileOutputStream (excFile));
        data_out_mix = new DataOutputStream (new FileOutputStream (mixExcFile));
      }
      
      /* Clear content of SlideVector c, should be done if this function is
      called more than once with a new set of generated parameters. */
      for(i=0; i< C.length; i++)
        C[i]=0.0;
    
      f0Std = htsData.getF0Std();
      f0Shift = htsData.getF0Mean();
      f0MeanOri = 0.0;

      
      for(mcepframe=0,lf0frame=0; mcepframe<mcepPst.getT(); mcepframe++) {
        if(voiced[mcepframe]){  
          f0MeanOri = f0MeanOri + Math.exp(lf0Pst.getPar(lf0frame, 0));
          //System.out.print(Math.exp(lf0Pst.getPar(lf0frame, 0)) + "  ");
          lf0frame++;
        }
        //else
          //System.out.print("0.0  ");  
      }
      f0MeanOri = f0MeanOri/lf0frame;
    
      
      
      
      /* read the residual file */
      AudioInputStream inputAudio = AudioSystem.getAudioInputStream(new File(resFile));
      int samplingRate = (int)inputAudio.getFormat().getSampleRate();
      AudioDoubleDataSource signal = new AudioDoubleDataSource(inputAudio);
      double [] res = signal.getAllData(); 
         
      
      
      /* _______________________Synthesize speech waveforms_____________________ */
      /* generate Nperiod samples per mcepframe */
      s = 0;   /* number of samples */
      s_double = 0;
      audio_size = (mcepPst.getT()) * (fprd) ;
      audio_double = new double[audio_size];  /* initialise buffer for audio */
      magSample = 1;
      magPulseSize = 0;
  
      
      
      
      int sample = 0;
      for(mcepframe=0,lf0frame=0; mcepframe<mcepPst.getT(); mcepframe++) {
       
        /* get current feature vector mc */ 
        for(i=0; i<m; i++)
          mcp[i] = mcepPst.getPar(mcepframe, i); 
        
        //System.out.println("htsData.getF0Std()=" + htsData.getF0Std() + " htsData.getF0Mean()="+ htsData.getF0Mean());
        
        /* f0 modification through the MARY audio effects */
        if(voiced[mcepframe]){
          f0 = f0Std * Math.exp(lf0Pst.getPar(lf0frame, 0)) + (1-f0Std) * f0MeanOri + f0Shift;       
          lf0frame++;
          if(f0 < 0.0)
            f0 = 0.0;
        }
        else{
          f0 = 0.0;          
        }
        //System.out.println("mcepframe=" + mcepframe +  "  f0=" + f0);
         
        /* if mixed excitation get shaping filters for this frame */
        if(mixedExcitation){
          for(j=0; j<orderM; j++) {
            hp[j] = hn[j] = 0.0;
            for(i=0; i<numM; i++) {
              //System.out.println("str=" + pdf2par.get_str(mcepframe, i) + "  h[i][j]=" + h[i][j])  ;
              hp[j] += strPst.getPar(mcepframe, i) * h[i][j];
              hn[j] += ( 1 - strPst.getPar(mcepframe, i) ) * h[i][j];
            }
          }
        }
        
        /* f0 -> pitch , in original code here it is used p, so f0=p in the c code */
        //System.out.println("\nmcepframe=" + mcepframe + "  f0=" + f0 );
        if(f0 != 0.0)
           f0 = rate/f0;
        
        if( p1 < 0 ) {  
          p1   = f0;           
          pc   = p1;           
        }
        //System.out.println("  [f0=(rate/f0)=  >>> " + f0 + " <<<  ]  ***pc=" + pc + "  p1=" + p1 );
        
        /* mc2b: transform mel-cepstrum to MLSA digital fillter coefficients */
         mc2b(mcp,m,a);
       
        /* postfiltering  ( Not implemented yet )*/
        /* this is done if beta>0.0 */
        
        for(k=0; k<m; k++){  
          C[cinc+k] = ( C[cc+k] - C[k] ) * (double)iprd/(double)fprd;    
          //System.out.println("cinc["+ k + "]=" + c.getCINC(k) );
        }
        
        /* p=f0 in c code!!! */
        if( p1 != 0.0 && f0 != 0.0 ) {
          inc = (f0 - p1) * (double)iprd/(double)fprd;
          //System.out.println("  inc=(f0-p1)/80=" + inc );
        } else {
          inc = 0.0;
          pc = f0;
          p1 = 0.0; 
          //System.out.println("  inc=" + inc + "  ***pc=" + pc + "  p1=" + p1);
        }
        
        
        
        /* Here i need to generate both xp:pulse and xn:noise signals separately  */ 
        gauss = false; /* Mixed excitation works better with nomal noise */
      
        /* Generate fperiod samples per feature vector, normally 80 samples per frame */
        //p1=0.0;
        gauss=false;
        for(j=fprd-1, i=(iprd+1)/2; j>=0; j--) {
//          System.out.print("j=" + j + "  i=" + i);  
            
          x = res[sample];       
          x = mlsadf(x, m, a, aa);
          
          audio_double[s_double] = x;
          s_double++;
          
          if((--i) == 0 ) {
            p1 += inc;
            for(k=0; k<m; k++){
              C[k] = C[k] + C[cinc+k];   
              //System.out.println("   " + k + "=" + c.get(k));
            }
            i = iprd;
          }
 //         System.out.println("  i=" + i + "  inc=" + inc + "  pc=" + pc + "  p1=" + p1);
          sample++;
        } /* for each sample in a period fprd */
        
       // System.out.println();
        
        p1 = f0;
        
        /* move elements in c */
        for(i=0; i<m; i++){
          C[i] = C[cc+i];
        }
      
      } /* for each mcep frame */
    
      if(debug){
        data_out.close();
        data_out_mix.close();
      }
      
      logger.info("Finish processing " + mcepframe + " mcep frames." + "  Num samples in bytes s=" + s );
        
      
      return(audio_double);
      
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
    
        
    
    /** mc2b: transform mel-cepstrum to MLSA digital filter coefficients */
    private void mc2b(double mcp[], int m, double a ) {
      int i;
      C[cc+m-1] = mcp[m-1];
      for(i=m-2; i>=0; i--) {
        C[cc+i] = mcp[i] - a * C[cc+i+1];  
      }
    }
    
    /** b2mc: transform MLSA digital filter coefficients to mel-cepstrum */
    private void b2mc(double mcp[], int m, double a){
      double daux, oaux;
      int i;
      daux = mcp[m-1] = C[cc+m-1];
      for(i=m-2; i>=0; i--) {
        oaux = C[cc+i] + (a * daux);
        daux = C[cc+i];
        mcp[i] = oaux;
      }
    }
    
 
   /** freqt: frequency transformation */
   private void freqt(double c1[], int m1, int cepIndex, int m2, double a){
     int i, j;
     double b;
     
     if( d == null ){
       size = m2;
       d = new double[size + size + 2];
       g = size+1;
     }
     
     if(m2 > size) {
       size = m2;
       d = new double[size + size +2];
       g = size +1;  
     }
     b = 1 - a * a;
     
     for(i = 0; i < m2+1; i++)
        d[g+i] = 0.0;
     
     for(i = -m1; i <= 0; i++){
       if(0 <= m2 )  
         d[g+0] = c1[-i] + a * (d[0] = d[g+0]);
       if(1 <= m2)
         d[g+1] = b * d[0] + a * (d[1] = d[g+1]);
           
       for(j=2; j<=m2; j++)
         d[g+j] = d[j-1] + a * (d[j] = d[g+j] - d[g+j-1]);
           
     }
     
     /* move memory */
     for(i=0; i<m2+1; i++)
       c1[cepIndex+i] = d[g+i];
       
   }
   
   /** c2ir: The minimum phase impulse response is evaluated from the minimum phase cepstrum */
   private void c2ir(double c1[], int cepIndex, int nc, int irIndex, int leng ){
     int n, k, up1;
     double daux;

     c1[irIndex + 0] = Math.exp(c1[cepIndex + 0]);
     for(n = 1; n < leng; n++) {
       daux = 0;
       if(n >= nc )
         up1 = nc -1;
       else
         up1 = n;
       for(k = 1; k <= up1; k++ )
         daux += k * c1[cepIndex + k] * c1[irIndex + n - k];
       c1[irIndex + n] = daux / n;
       
     }
   }
   
   /* b2en: functions for postfiltering */ 
   private double b2en(int m, double a){
      double en;
      int k;
      
      if (o < m ){
        mc = new double[(m+1) +2 * irleng ];
        cep = (m+1);
        ir = (m+1) + irleng;    
      }
      b2mc(mc, m, a);
      /* freqt(vs->mc, m, vs->cep, vs->irleng - 1, -a);*/
      freqt(mc, m, cep, irleng-1, -a);
      /* HTS_c2ir(vs->cep, vs->irleng, vs->ir, vs->irleng); */
      c2ir(mc, cep, irleng, ir, irleng);
      en = 0.0;
      
      for(k = 0; k < irleng; k++)
        en += mc[ir+k] * mc[ir+k];
      
      
      return(en);  
    }  
    
   
    /** posfilter: postfilter for mel-cepstrum. It uses alpha and beta defined in HMMData */
    private void postfilter(double mcp[], int m, double alpha, double beta) {
        
      double e1, e2;
      int k;
      
      if(beta > 0.0 && m > 1){
        e1 = b2en(m,alpha);
        C[cc+1] -= beta * alpha * mcp[2];
        for(k = 2; k < m; k++)
          C[cc+k] *= (1.0 +beta);
        e2 = b2en(m, alpha);
        C[cc+0] += Math.log(e1/e2) / 2;
        
        for(k = 0; k < m; k++)
          mcp[k] = C[k];
          
      }
            
    }
    
    
    /** mlsadf: HTS Mel Log Spectrum Approximation filter */
    private double mlsadf(double x, int m, double a, double aa) {
      int k;   
        
      x = mlsadf1(x, a, aa);  
      x = mlsadf2(x, m-1, a, aa);
       
      return x; 
    }
    
    /** mlsdaf1:  */
    private double mlsadf1(double x, double a, double aa) {
      double v;
      double out = 0.0;
      int i, k;
      //pt1 --> pt = &d1[pd+1]  
       
      for(i=pd; i>=1; i--) {
        C[d1+i] = aa * C[d1+pt1+i-1] + a * C[d1+i];  
        C[d1+pt1+i] = C[d1+i] * C[1];
        v = C[d1+pt1+i] * pade[ppade+i];
        
        if(i == 1 || i == 3 || i == 5)
          x += v;
        else 
          x += -v;
        out += v;
      }
      C[d1+pt1+0] = x;
      out += x;
      
      
      return(out);
      
    }

    /** mlsdaf2: */
    private double mlsadf2(double x, int m, double a, double aa) {
      double v;
      double out = 0.0;
      int i, k; 
      // pt2 --> pt = &d1[pd * (m+2)] 
      // pt3 --> pt = &d1[ 2*(pd+1) ] 
      
      
      for(i=pd; i>=1; i--) {   
        C[d1+pt2+i] = mlsafir(C[d1+(pt2+i)-1],m,a,aa,pt3[i]);
        v = C[d1+pt2+i] * pade[ppade+i];
          
        if(i == 1 || i == 3 || i == 5)
          x += v;
        else
          x += -v;
        out += v;
        
      }
      C[d1+pt2+0] = x;
      out += x;
       
      return out;     
    }
    
    /** mlsafir: */
    private double mlsafir(double x, int m, double a, double aa, int _pt3 ) {
      double y = 0.0;
      int i, k;

      C[d1+_pt3+0] = x;
      C[d1+_pt3+1] = aa * C[d1+_pt3+0] + ( a * C[d1+_pt3+1] );

      for(i=2; i<=m; i++){
        C[d1+_pt3+i] +=  a * ( C[d1+_pt3+i+1] - C[d1+_pt3+i-1]);
      }
      
      for(i=2; i<=m; i++){ 
        y += C[d1+_pt3+i] * C[i];
      }
       
      for(i=m+1; i>1; i--){
        C[d1+_pt3+i] = C[d1+_pt3+i-1];  
      }
       
      return(y);
    }
    
  
       /** Generate one pitch period from Fourier magnitudes */
    private double[] genPulseFromFourierMag(HTSPStream mag, int n, double f0, boolean aperiodicFlag){
        
      int numHarm = mag.getOrder();
      int i, j;
      int currentF0 = (int)Math.round(f0);
      int T, T2;
      double pulse[] = null;
      double real[] = null;
      double imag[] = null;
      
      if(currentF0 < 512)
        T = 512;
      else
        T = 1024;
      T2 = 2*T;
      
      /* since is FFT2 no aperiodicFlag or jitter of 25% is applied */
      
      /* get the pulse */      
      pulse = new double[T];
      real = new double[T2];
      imag = new double[T2];

      /* copy Fourier magnitudes (Wai C. Chu "Speech Coding algorithms foundation and evolution of standadized coders" pg. 460) */
      real[0] = real[T] = 0.0;   /* DC component set to zero */
      for(i=1; i<=numHarm; i++){     
        real[i] = real[T-i] = real[T+i] =  real[T2-i] = mag.getPar(n, i-1);  /* Symetric extension */
        imag[i] = imag[T-i] = imag[T+i] =  imag[T2-i] = 0.0;
      }
      for(i=(numHarm+1); i<(T-numHarm); i++){   /* Default components set to 1.0 */
        real[i] = real[T-i] = real[T+i] =  real[T2-i]  = 1.0;
        imag[i] = imag[T-i] = imag[T+i] =  imag[T2-i] = 0.0;
      }
          
      /* Calculate inverse Fourier transform */
      FFT.transform(real, imag, true);
      
      /* circular shift and normalise multiplying by sqrt(F0) */
      double sqrt_f0 = Math.sqrt(currentF0); 
      for(i=0; i<T; i++)  
        pulse[i] = real[modShift(i-numHarm,T)] * sqrt_f0;
      
      return pulse;
      
    }
    
    
    /** Generate one pitch period from Fourier magnitudes */
    private double[] genPulseFromFourierMagRadix(HTSPStream mag, int n, double f0, boolean aperiodicFlag){
        
      int numHarm = mag.getOrder();
      int i, j;
      int currentF0 = (int)Math.round(f0);
      int T;
      double pulse[] = null;
      double p[] = null;
      double erratic, jitter;
      
      T = currentF0;
      /* if aperiodicFlag then apply jitter of 25% */
      if(aperiodicFlag){
        erratic = uniformRand();
        jitter = 0.25;
        T = (int)Math.round(T * (1 + (jitter * erratic)));  
        System.out.print("  F0-jitter=" + T + "  jitter*erratic=" + (jitter*erratic));
      }
      
      
      /* get the pulse */      
      pulse = new double[T];
      MathUtils.Complex magPulse = new MathUtils.Complex(T*2);
      
      
//      System.out.println("  n: " + n + "  f0=" + T);
      //for(i=0; i<numHarm; i++)
      //  System.out.println("mag[" + i + "]=" + mag.getPar(n, i));
      /* copy Fourier magnitudes (Wai C. Chu "Speech Coding algorithms foundation and evolution of standadized coders" pg. 460) */
      pulse[0] = 0.0;                          /* DC component set to zero */
      for(i=1; i<=numHarm; i++){
        pulse[i] = mag.getPar(n, i-1);         /* Symetric extension */
        pulse[T-i] = pulse[i];
      }
      for(i=(numHarm+1); i<(T-numHarm); i++)   /* Default components set to 1.0 */
        pulse[i] = 1.0;
          
      for(i=0; i<T; i++){
        magPulse.real[i] = magPulse.real[i+T] = pulse[i];
      }
      
      /* Calculate inverse Fourier transform */
      //transform(double[] real, double[] imag, boolean inverse)
      //double [] ifftReal(Complex x, int ifftSize)
      //p = FFTMixedRadix.ifftReal(magPulse, T);
      //Complex ifft(Complex x)
      MathUtils.Complex ifftPulse;
      ifftPulse = FFTMixedRadix.ifft(magPulse);
      
      for(i=0; i<T; i++){
        pulse[i] = ifftPulse.real[2*i];
        //System.out.print(pulse[i] + " ");
      }
      //System.out.println();
      
      /* circular shift */
      circularShift(pulse, T, numHarm);
      
      /* normalise multiplying by sqrt(F0) this is done in generation */
      double sqrt_f0 = Math.sqrt(currentF0); 
      for(i=0; i<T; i++){
          pulse[i] = pulse[i] * sqrt_f0;
          //System.out.print(pulse[i] + " ");
      }      
      //System.out.println();
      
      return pulse;
      
    }
   
    
    private void circularShift(double y[], int T, int n){
       
      double x[] = new double[T];
      for(int i=0; i<T; i++)  
        x[i] = y[modShift(i-n,T)];
      for(int i=0; i<T; i++)  
        y[i] = x[i];  
    }
    
    private int modShift(int n, int N){
      if( n < 0 )
        while( n < 0 )
           n = n + N;
      else
        while( n >= N )
           n = n - N;
      
      return n;
    }
    
    
    /** 
     * Stand alone testing reading parameters form files in SPTK format and little-endian. */
    public static void main(String[] args) throws IOException, InterruptedException, Exception{
       /* configure log info */
       org.apache.log4j.BasicConfigurator.configure();
        
       HMMData htsData = new HMMData(); 
       HTSPStream lf0Pst, mcepPst, strPst, magPst;
       boolean [] voiced = null;
       DataInputStream lf0Data, mcepData, strData, magData;
       String lf0File, mcepFile, strFile, magFile, resFile;
       String voiceExample;
       
       int ex = 4;
       //Author of the danger trail, Philip Steels, etc.
       String MaryBase = "/project/mary/marcela/MARY-TTS/";
       htsData.setUseMixExc(true);
       htsData.setUseFourierMag(true);  /* use Fourier magnitudes for pulse generation */
      
       // bits1, german
       if(ex == 1){
           String voiceHMM = "bits1";
           String voiceName = "hmm-"+voiceHMM;
           voiceExample = "US10010003_0";
           htsData.initHMMData(voiceName, MaryBase, "german-hmm-"+voiceHMM+".config");    
       lf0File = "/project/mary/marcela/hmm-gen-experiment/lf0/US10010003_0-littend.lf0";
       mcepFile = "/project/mary/marcela/hmm-gen-experiment/mgc/US10010003_0-littend.mgc";
       strFile = "/project/mary/marcela/hmm-gen-experiment/str/US10010003_0-littend.str";
       magFile = "/project/mary/marcela/hmm-gen-experiment/mag/US10010003_0-littend.mag";
       resFile = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/US10010003_0_res.wav";
       //resFile = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/US10010003_0_res_sinResynth.wav";
       } else if (ex == 2){
       // bits2, german
           String voiceHMM = "bits2";
           String voiceName = "hmm-"+voiceHMM;
           voiceExample = "US10020003_0";
           htsData.initHMMData(voiceName, MaryBase, "german-hmm-"+voiceHMM+".config");    
           
       lf0File = "/project/mary/marcela/hmm-gen-experiment/lf0/US10020003_0-littend.lf0";
       mcepFile = "/project/mary/marcela/hmm-gen-experiment/mgc/US10020003_0-littend.mgc";
       strFile = "/project/mary/marcela/hmm-gen-experiment/str/US10020003_0-littend.str";
       magFile = "/project/mary/marcela/hmm-gen-experiment/mag/US10020003_0-littend.mag";
       resFile = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/US10020003_0_res.wav";
       //resFile = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/US10020003_0_res_sinResynth.wav";
       } else if (ex == 3){       
       // neutral, german
           String voiceHMM = "neutral";
           String voiceName = "hmm-"+voiceHMM;
           voiceExample = "a0093";
           htsData.initHMMData(voiceName, MaryBase, "german-hmm-"+voiceHMM+".config");    

       lf0File = "/project/mary/marcela/hmm-gen-experiment/lf0/a0093-littend.lf0";
       mcepFile = "/project/mary/marcela/hmm-gen-experiment/mgc/a0093-littend.mgc";
       strFile = "/project/mary/marcela/hmm-gen-experiment/str/a0093-littend.str";
       magFile = "/project/mary/marcela/hmm-gen-experiment/mag/a0093-littend.mag";
       //resFile = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/a0093_res.wav";
       resFile = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/a0093_res_sinResynth.wav";
       } else {       
       // slt, english
           String voiceHMM = "slt";
           String voiceName = "hmm-"+voiceHMM;
           voiceExample = "cmu_us_arctic_slt_a0001";
           htsData.initHMMData(voiceName, MaryBase, "english-hmm-"+voiceHMM+".config");    

       lf0File = "/project/mary/marcela/hmm-gen-experiment/lf0/cmu_us_arctic_slt_a0001-littend.lf0";
       mcepFile = "/project/mary/marcela/hmm-gen-experiment/mgc/cmu_us_arctic_slt_a0001-littend.mgc";
       strFile = "/project/mary/marcela/hmm-gen-experiment/str/cmu_us_arctic_slt_a0001-littend.str";
       magFile = "/project/mary/marcela/hmm-gen-experiment/mag/cmu_us_arctic_slt_a0001-littend.mag";
       //resFile = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/cmu_us_arctic_slt_a0001_res.wav";
       resFile = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/cmu_us_arctic_slt_a0001_res_sinResynth.wav";
       }
       
        
       int i, j;
       int mcepVsize = 75;  /* here the sizes include mcep + delta + delta^2, but just mcep will be loaded */
       int strVsize = 15;
       int lf0Vsize = 3;
       int magVsize = 30;
       int totalFrame = 0;
       int lf0VoicedFrame = 0;
       float fval;
       lf0Data = new DataInputStream (new BufferedInputStream(new FileInputStream(lf0File)));
       
       /* First i need to know the size of the vectors */
       try { 
         while (true) {
           fval = lf0Data.readFloat();
           totalFrame++;  
           if(fval>0)
            lf0VoicedFrame++;
         } 
       } catch (EOFException e) { }
       lf0Data.close();
       
       /* CHECK: I do not know why mcep has totalframe-2 frames less than lf0 and str ???*/
       //totalFrame = totalFrame - 2;
       System.out.println("Total number of Frames = " + totalFrame);
       voiced = new boolean[totalFrame];
       
       /* Initialise HTSPStream-s */
       //--lf0Pst = new HTSPStream(lf0Vsize, lf0VoicedFrame, HMMData.LF0);
       lf0Pst = new HTSPStream(lf0Vsize, totalFrame, HMMData.LF0);
       mcepPst = new HTSPStream(mcepVsize, totalFrame, HMMData.MCP);
       strPst = new HTSPStream(strVsize, totalFrame, HMMData.STR);
       magPst = new HTSPStream(magVsize, totalFrame, HMMData.MAG);             
       
       /* load lf0 data */
       /* for lf0 i just need to load the voiced values */
       lf0VoicedFrame = 0;
       lf0Data = new DataInputStream (new BufferedInputStream(new FileInputStream(lf0File)));
       for(i=0; i<totalFrame; i++){
         fval = lf0Data.readFloat();  
         //lf0Pst.setPar(i, 0, fval);
         if(fval < 0)
           voiced[i] = false;
         else{
           voiced[i] = true;
           lf0Pst.setPar(lf0VoicedFrame, 0, fval);
           lf0VoicedFrame++;
         }
       }
       lf0Data.close();
      
       /* load mgc data */
       mcepData = new DataInputStream (new BufferedInputStream(new FileInputStream(mcepFile)));
       for(i=0; i<totalFrame; i++){
         for(j=0; j<mcepPst.getOrder(); j++)
           mcepPst.setPar(i, j, mcepData.readFloat());
       }
       mcepData.close();
       
       /* load str data */
       strData = new DataInputStream (new BufferedInputStream(new FileInputStream(strFile))); 
       for(i=0; i<totalFrame; i++){
         for(j=0; j<strPst.getOrder(); j++)
           strPst.setPar(i, j, strData.readFloat());
       }
       strData.close();
       
       /* load mag data */
       magData = new DataInputStream (new BufferedInputStream(new FileInputStream(magFile)));
       System.out.println("Mag ori ");
       for(i=0; i<totalFrame; i++){
         for(j=0; j<magPst.getOrder(); j++)
           magPst.setPar(i, j, magData.readFloat());
         //System.out.println("i:" + i + "  f0=" + Math.exp(lf0Pst.getPar(i, 0)) + "  mag(1)=" + magPst.getPar(i, 0) + "  str(1)=" + strPst.getPar(i, 0) );
       }
       magData.close();
   
             
       
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
       double [] audio_double = null;
       
       HTSVocoder par2speech = new HTSVocoder();
       audio_double = par2speech.htsMLSAVocoder(lf0Pst, mcepPst, strPst, magPst, voiced, htsData);
       //audio_double = par2speech.htsMLSAVocoder_residual(lf0Pst, mcepPst, strPst, magPst, voiced, htsData, resFile);
       
       long lengthInSamples = (audio_double.length * 2 ) / (sampleSizeInBits/8);
       par2speech.logger.info("length in samples=" + lengthInSamples );
       
       /* Normalise the signal before return, this will normalise between 1 and -1 */
       double MaxSample = MathUtils.getAbsMax(audio_double);
       for (i=0; i<audio_double.length; i++)
          audio_double[i] = 0.3 * ( audio_double[i] / MaxSample );
       
       DDSAudioInputStream oais = new DDSAudioInputStream(new BufferedDoubleDataSource(audio_double), af);
       
       
       String fileOutName;         
      /* if(!htsData.getUseGV() && htsData.getUseMixExc())
           fileOutName = "/project/mary/marcela/hmm-gen-experiment/" +voiceHMM+"-nogv-mix-" + voiceExample + ".wav";
       else if(htsData.getUseGV() && !htsData.getUseMixExc())
           fileOutName = "/project/mary/marcela/hmm-gen-experiment/" +voiceHMM+"-gv-nomix-" + voiceExample + ".wav";
       else if(!htsData.getUseGV() && !htsData.getUseMixExc())
           fileOutName = "/project/mary/marcela/hmm-gen-experiment/" +voiceHMM+"-nogv-nomix-" + voiceExample + ".wav";
       else
           fileOutName = "/project/mary/marcela/hmm-gen-experiment/" +voiceHMM+"-gv-mix-" + voiceExample + ".wav";
       */
       fileOutName = "/project/mary/marcela/hmm-gen-experiment/residual_sinResynth/mlsa_res/" + voiceExample + ".wav"; 
       File fileOut = new File(fileOutName);
       System.out.println("saving to file: " + fileOutName);
           
         
       if (AudioSystem.isFileTypeSupported(AudioFileFormat.Type.WAVE,oais)) {
         AudioSystem.write(oais, AudioFileFormat.Type.WAVE, fileOut);
       }

       System.out.println("Calling audioplayer:");
       AudioPlayer player = new AudioPlayer(fileOut);
       player.start();  
       player.join();
       System.out.println("audioplayer finished...");
    
  }
    
}  /* class HTSVocoder */


