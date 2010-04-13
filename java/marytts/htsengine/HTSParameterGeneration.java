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

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.Mfccs;
import marytts.util.MaryUtils;
import marytts.util.io.LEDataInputStream;

import org.apache.log4j.Logger;

/**
 * Parameter generation out of trained HMMs.
 * 
 * Java port and extension of HTS engine version 2.0
 * Extension: mixed excitation
 * @author Marcela Charfuelan
 */
public class HTSParameterGeneration {
	
  public static final double INFTY   = ((double) 1.0e+38);
  public static final double INFTY2  = ((double) 1.0e+19);
  public static final double INVINF  = ((double) 1.0e-38);
  public static final double INVINF2 = ((double) 1.0e-19);
  public static final double LTPI    = 1.83787706640935;    /* log(2*PI) */
	

  private HTSPStream mcepPst = null;
  private HTSPStream strPst  = null;
  private HTSPStream magPst  = null;
  private HTSPStream lf0Pst  = null;
  private boolean voiced[];
  
  private Logger logger = Logger.getLogger("ParameterGeneration");
  
  public double getMcep(int i, int j){ return mcepPst.getPar(i, j); }
  public int getMcepOrder(){ return mcepPst.getOrder(); }
  public int getMcepT(){ return mcepPst.getT(); }
  public HTSPStream getMcepPst(){ return mcepPst;}
  public void setMcepPst(HTSPStream var){ mcepPst = var; };
  
  public double getStr(int i, int j){ return strPst.getPar(i, j); }
  public int getStrOrder(){ return strPst.getOrder(); }
  public HTSPStream getStrPst(){ return strPst; }
  
  public double getMag(int i, int j){ return magPst.getPar(i, j); }
  public int getMagOrder(){ return magPst.getOrder(); }
  public HTSPStream getMagPst(){ return magPst; }
  
  public double getLf0(int i, int j){ return lf0Pst.getPar(i, j); }
  public int getLf0Order(){ return lf0Pst.getOrder(); }
  public HTSPStream getlf0Pst(){ return lf0Pst;}
  public void setlf0Pst(HTSPStream var){ lf0Pst = var; };
  
  public boolean getVoiced(int i){ return voiced[i]; }
  public void setVoiced(int i, boolean bval){ voiced[i]=bval; }
  public boolean [] getVoicedArray(){ return voiced; }
  public void setVoicedArray(boolean []var){ voiced = var; }
	
  /* Inverse of a given double */
  /* We actually need the inverse of the matrix of covariance, but since this matrix */ 
  /* is a diagonal matrix, then we just need to calculate the inverse of each of the  */
  /* numbers in the diagonal. */
  private double finv(double x) {
	  
	if( x >= INFTY2 ) return 0.0;
	if( x <= -INFTY2 ) return 0.0;
	if( x <= INVINF2 && x >= 0 ) return INFTY;
	if( x >= -INVINF2 && x < 0 ) return -INFTY;
	
	return ( 1.0 / x );
	  
  }
  
  /** HTS maximum likelihood parameter generation
  * @param um  : utterance model sequence after processing Mary context features
  * @param ms  : HMM pdfs model set.
  */
  public void htsMaximumLikelihoodParameterGeneration(HTSUttModel um, HMMData htsData, String parFileName, boolean debug) throws Exception{
	  
	int frame, uttFrame, lf0Frame;
	int state, lw, rw, k, n, i;
	boolean nobound;
    HTSModel m;
    CartTreeSet ms = htsData.getCartTreeSet();
 //---   HTSModelSet ms = htsData.getModelSet();
    
	/* Initialisation of PStream objects */
  	/* Initialise Parameter generation using UttModel um and Modelset ms */
  	/* initialise PStream objects for all the parameters that are going to be generated: */
  	/* mceppst, strpst, magpst, lf0pst */
	/* Here i should pass the window files to initialise the dynamic windows dw */
	/* for the moment the dw are all the same and hard-coded */
	mcepPst = new HTSPStream(ms.getMcepVsize(), um.getTotalFrame(), HMMData.MCP);
    /* for lf0 count just the number of lf0frames that are voiced or non-zero */
    lf0Pst  = new HTSPStream(ms.getLf0Stream(), um.getLf0Frame(), HMMData.LF0);
    /* The following are optional in case of generating mixed excitation */
    if( htsData.getPdfStrFile() != null)
	  strPst  = new HTSPStream(ms.getStrVsize(), um.getTotalFrame(), HMMData.STR);
    if (htsData.getPdfMagFile() != null )
	  magPst  = new HTSPStream(ms.getMagVsize(), um.getTotalFrame(), HMMData.MAG);
	   
    
	uttFrame = lf0Frame = 0;
	voiced = new boolean[um.getTotalFrame()];
	
	for(i=0; i<um.getNumUttModel(); i++){
        m = um.getUttModel(i);          		
        for(state=0; state<ms.getNumStates(); state++)
      	 for(frame=0; frame<m.getDur(state); frame++) {
      		voiced[uttFrame] = m.getVoiced(state);
      		uttFrame++;
      		if(m.getVoiced(state))
      		  lf0Frame++;
      	 }
    }
	/* mcepframe and lf0frame are used in the original code to initialise the T field */
	/* in each pst, but here the pst are already initialised .... */
	logger.debug("utteranceFrame=" + uttFrame + " lf0frame=" + lf0Frame);
	
	
	uttFrame = 0;
	lf0Frame = 0;
	/* copy pdfs */
	for(i=0; i<um.getNumUttModel(); i++){
      m = um.getUttModel(i);          		
      for(state=0; state<ms.getNumStates(); state++) {
    	         
      	for(frame=0; frame<m.getDur(state); frame++) {
            
          //System.out.println("uttFrame=" + uttFrame + "  phone frame=" + frame + "  phone state=" + state);
             
      	  /* copy pdfs for mcep */
      	  for(k=0; k<ms.getMcepVsize(); k++){
      		mcepPst.setMseq(uttFrame, k, m.getMcepMean(state, k));
      		mcepPst.setIvseq(uttFrame, k, finv(m.getMcepVariance(state, k)));
      	  }
      	  
      	  /* copy pdf for str */
          if( strPst !=null ) {
      	    for(k=0; k<ms.getStrVsize(); k++){
      		  strPst.setMseq(uttFrame, k, m.getStrMean(state, k));
      		  strPst.setIvseq(uttFrame, k, finv(m.getStrVariance(state, k)));
      	    }
          }
      	  
      	  /* copy pdf for mag */
          if( magPst != null ) {
      	    for(k=0; k<ms.getMagVsize(); k++){
      		  magPst.setMseq(uttFrame, k, m.getMagMean(state, k));
      		  magPst.setIvseq(uttFrame, k, finv(m.getMagVariance(state, k)));
    	    }
          }
      	  
      	  /* copy pdfs for lf0 */ 
      	  for(k=0; k<ms.getLf0Stream(); k++){
      		lw = lf0Pst.getDWwidth(k, HTSPStream.WLEFT);
      		rw = lf0Pst.getDWwidth(k, HTSPStream.WRIGHT);
      		nobound = true;
      		/* check if current frame is voiced/unvoiced boundary or not */
      		for(n=lw; n<=rw; n++)
      		  if( (uttFrame+n) <= 0 || um.getTotalFrame() <= (uttFrame+n))
      			 nobound = false;
      		  else
      			 nobound = ( nobound && voiced[uttFrame+n] );
      		/* copy pdfs */
      		if( voiced[uttFrame] ) {
      		  lf0Pst.setMseq(lf0Frame, k, m.getLf0Mean(state, k));
      		  if( nobound || k==0 )
      			lf0Pst.setIvseq(lf0Frame, k, finv(m.getLf0Variance(state, k)));
      		  else  /* the variances for dynamic feature are set to inf on v/uv boundary */
      			lf0Pst.setIvseq(lf0Frame, k, 0.0);
      		}
      		
    	  }
      	  
      	  if( voiced[uttFrame] )
             lf0Frame++;      	  
      	  uttFrame++;
      	  
      	} /* for each frame in this state */
      } /* for each state in this model */
	}  /* for each model in this utterance */ 
	
	//System.out.println("After copying pdfs to PStreams uttFrame=" + uttFrame + " lf0frame=" + lf0Frame);
	//System.out.println("mseq[" + uttFrame + "][" + k + "]=" + mceppst.get_mseq(uttFrame, k) + "   " + m.get_mcepmean(state, k));
		
	/* parameter generation for mcep */    
	logger.info("Parameter generation for MCEP: ");
    mcepPst.mlpg(htsData, htsData.getUseGV());

    /* parameter generation for lf0 */ 
    // CHECK!!
    // generate the parameters any way, if there is external they will be replaced
    // for the moment these values are used with the mbrola pfeats when f0 is 0.0 and genf0 is not 0.0
    if (lf0Frame>0){
      logger.info("Parameter generation for LF0: "); 
      lf0Pst.mlpg(htsData, htsData.getUseGV());
    }   
    
    if(htsData.getUseUnitLogF0ContinuousFeature())
      loadUnitLogF0ContinuousFeature(um, htsData);
    else if(htsData.getUseLogF0FromExternalFile())
      loadLogF0FromExternalFile(htsData.getExternalLf0File(), uttFrame);
    //else if (lf0Frame>0){
    //  logger.info("Parameter generation for LF0: "); 
    //  lf0Pst.mlpg(htsData, htsData.getUseGV());
    //}
    
    
	/* parameter generation for str */
    boolean useGV=false;
    if( strPst != null ) {
      logger.info("Parameter generation for STR ");
      if(htsData.getUseGV() && (htsData.getPdfStrGVFile() != null) )
        useGV = true;
      strPst.mlpg(htsData, useGV);
    }

	/* parameter generation for mag */
    useGV = false;
    if( magPst != null ) {
      logger.info("Parameter generation for MAG ");
      if(htsData.getUseGV() && (htsData.getPdfMagGVFile() != null) )
        useGV = true;  
	  magPst.mlpg(htsData, useGV);
    }
	   
    if(debug) {
        // saveParam(parFileName+"mcep.bin", mcepPst, HMMData.MCP);  // no header
        // saveParam(parFileName+"lf0.bin", lf0Pst, HMMData.LF0);    // no header
        saveParamMaryFormat(parFileName, mcepPst, HMMData.MCP);
        saveParamMaryFormat(parFileName, lf0Pst, HMMData.LF0);
     }
    

	  
  }  /* method htsMaximumLikelihoodParameterGeneration */
  
  
  
  /* Save generated parameters in a binary file */
  public void saveParamMaryFormat(String fileName, HTSPStream par, int type){
    int t, m, i;
    double ws = 0.025; /* window size in seconds */
    double ss = 0.005; /* skip size in seconds */
    int fs = 16000;    /* sampling rate */
    
    try{  
        
      if(type == HMMData.LF0 ) {          
          fileName += ".ptc";
          /*
          DataOutputStream data_out = new DataOutputStream (new FileOutputStream (fileName));
          data_out.writeFloat((float)(ws*fs));
          data_out.writeFloat((float)(ss*fs));
          data_out.writeFloat((float)fs);          
          data_out.writeFloat(voiced.length);
          
          i=0;
          for(t=0; t<voiced.length; t++){    // here par.getT are just the voiced!!! so the actual length of frames can be taken from the voiced array 
             if( voiced[t] ){
               data_out.writeFloat((float)Math.exp(par.getPar(i,0)));
               i++;
             }System.out.println("GEN f0s[" + t + "]=" + Math.exp(lf0Pst.getPar(i,0)));  
             else
               data_out.writeFloat((float)0.0);
          }
          data_out.close();
          */
          
          i=0;
          double f0s[] = new double[voiced.length];
          //System.out.println("voiced.length=" + voiced.length);
          for(t=0; t<voiced.length; t++){    // here par.getT are just the voiced!!! so the actual length of frames can be taken from the voiced array 
             if( voiced[t] ){
               f0s[t] = Math.exp(par.getPar(i,0));               
               i++;
             }
             else
               f0s[t] = 0.0;
             //System.out.println("GEN f0s[" + t + "]=" + f0s[t]);
             
          }
          /* i am using this function but it changes the values of sw, and ss  *samplingrate+0.5??? for the HTS values ss=0.005 and sw=0.025 is not a problem though */
         PitchReaderWriter.write_pitch_file(fileName, f0s, (float)(ws), (float)(ss), fs);
          
          
      } else if(type == HMMData.MCP ){
          
        int numfrm =  par.getT();
        int dimension = par.getOrder();
        Mfccs mgc = new Mfccs(numfrm, dimension);  
               
        fileName += ".mfc";
                 
        for(t=0; t<par.getT(); t++)
         for (m=0; m<par.getOrder(); m++)
           mgc.mfccs[t][m] = par.getPar(t,m);
        
        mgc.params.samplingRate = fs;         /* samplingRateInHz */
        mgc.params.skipsize     = (float)ss;  /* skipSizeInSeconds */
        mgc.params.winsize      = (float)ws;  /* windowSizeInSeconds */
        
        
        mgc.writeMfccFile(fileName);
        
        /* The whole set for header is in the following order:   
        ler.writeInt(numfrm);
        ler.writeInt(dimension);
        ler.writeFloat(winsize);
        ler.writeFloat(skipsize);
        ler.writeInt(samplingRate);
        */
        
      }
      
      
      logger.info("saveParam in file: " + fileName);
    
      
    } catch (IOException e) {
        logger.info("IO exception = " + e );
    }    
  }

  
  
  /* Save generated parameters in a binary file */
  public void saveParam(String fileName, HTSPStream par, int type){
    int t, m, i;
    try{  
      
      
      if(type == HMMData.LF0 ) {
          fileName += ".f0";
          DataOutputStream data_out = new DataOutputStream (new FileOutputStream (fileName));
          i=0;
          for(t=0; t<voiced.length; t++){    /* here par.getT are just the voiced!!!*/
             if( voiced[t] ){
               data_out.writeFloat((float)Math.exp(par.getPar(i,0)));
               i++;
             }
             else
               data_out.writeFloat((float)0.0);
          }
          data_out.close();
          
      } else if(type == HMMData.MCP ){
        fileName += ".mgc";
        DataOutputStream data_out = new DataOutputStream (new FileOutputStream (fileName));  
        for(t=0; t<par.getT(); t++)
         for (m=0; m<par.getOrder(); m++)
           data_out.writeFloat((float)par.getPar(t,m));
        data_out.close();
      }
      
      
      logger.info("saveParam in file: " + fileName);
      
    } catch (IOException e) {
        logger.info("IO exception = " + e );
    }    
  }
  
  public void loadUnitLogF0ContinuousFeature(HTSUttModel um, HMMData htsData) throws Exception{
      int i, j, n, t;  
      // Use the ContinuousFeatureProcessors unit_logf0 and unit_logf0delta, they are saved in each model of um
      // Modify the f0 generated values according to the external ones
      logger.info("Using external prosody for lf0: using unit_logf0 and unit_logF0delta from ContinuousFeatureProcessors.");
      int totalDur=0; 
      
      double externalLf0=0, nextExternalLf0=0;
      double externalLf0Delta=0;
      int numVoiced=0;
      i=0;
      float fperiodsec = ((float)htsData.getFperiod() / (float)htsData.getRate());
      double genLf0Hmms[] = new double[um.getNumModel()];
      
      boolean newVoiced[] = new boolean[um.getTotalFrame()];
      HTSPStream newLf0Pst  = new HTSPStream(3, um.getTotalFrame(), HMMData.LF0); // actually the size of lf0Pst is 
                                                                                  // just the number of voiced frames   
      
      double lf0Frame[] = new double[um.getTotalFrame()];
      
      // this is the generated F0
      //String genF0 = "/project/mary/marcela/f0-hsmm-experiment/genF0.txt";
      //String extF0 = "/project/mary/marcela/f0-hsmm-experiment/extF0.txt";
      //FileWriter genFile = new FileWriter(genF0);
      //FileWriter extFile = new FileWriter(extF0);
      
      
      HTSModel m;
      HTSModel mNext;
      numVoiced=0;
      int state, frame, numLf0NonZero;
      int lastPos=0, nextPos=0;
      double lf0Model, durModel;
      
      /*
      t=0;      
      for(i=0; i<um.getNumUttModel(); i++){
          m = um.getUttModel(i);   
          lf0Model = 0;
          durModel = 0;
          numLf0NonZero = 0;
          for(state=0; state<5; state++) {
            durModel += m.getDur(state);
            for(frame=0; frame<m.getDur(state); frame++) {                
                //genFile.write(Integer.toString(t) + " " + m.getPhoneName() + " " + Integer.toString(m.getTotalDur()) + " " 
                //        + Integer.toString(m.getDur(state)) + " " + Integer.toString(frame+1) + " ");
                if(voiced[t]){
                    //genFile.write(Double.toString(Math.exp(lf0Pst.getPar(numVoiced,0))) + "\n");                    
                    lf0Model = lf0Model + lf0Pst.getPar(numVoiced,0);
                    numLf0NonZero++;
                    numVoiced++;
                } else {
                  //genFile.write("0.0\n");  
                }
                t++;
            } // for each frame in this state 
          } // for each state in this model
          
          if(numLf0NonZero == 0){
            System.out.format("%s genDur=%.3f genF0=%.3f \n", m.getPhoneName(), durModel*fperiodsec, lf0Model);
            genLf0Hmms[i] = lf0Model;
          }
          else{
            System.out.format("%s genDur=%.3f genF0=%.3f \n", m.getPhoneName(), durModel*fperiodsec, (lf0Model/numLf0NonZero));
            genLf0Hmms[i] = (lf0Model/numLf0NonZero);
          }
          
        }  // for each model in this utterance        
      //genFile.close();
      //System.out.println("Created file:" + genF0);
      */
      double slope;
      // this is how the external prosody will look like
      numVoiced=0;
      t=0;
       
      int totalDurFrames=0;
      for(i=0; i<um.getNumUttModel(); i++){
          m = um.getUttModel(i);
          if((i+1)<um.getNumUttModel())
            mNext = um.getUttModel(i+1);
          else
            mNext=null;
          
          externalLf0 = m.getUnit_logF0();           
          lastPos = totalDurFrames;
                    
          externalLf0Delta = m.getUnit_logF0delta();
          if(mNext!=null){
            if(mNext.getUnit_logF0()>0.0){
              nextExternalLf0 = mNext.getUnit_logF0();
              nextPos = totalDurFrames + m.getTotalDur();
              
              slope = (nextExternalLf0 - externalLf0) / (nextPos - lastPos);
              
            } else{
              nextPos = totalDurFrames + m.getTotalDur();
              slope = 0.0;
            }
          }
          else {
            nextExternalLf0 = 0.0;
            slope = 0.0;
            nextPos = um.getTotalFrame();
          }
          totalDurFrames += m.getTotalDur();
          
          /*
          System.out.format("externalLf0=%.3f nextExternalLf0=%.3f lastPos=%d nextPos=%d slope=%.3f\n", externalLf0, nextExternalLf0, lastPos, nextPos, slope);          
          if(externalLf0 == 0.0)
            System.out.format("%s  extDur=%.3f(%d)   extF0=%f  extF0Delta=%f\n", m.getPhoneName(), m.getUnit_duration(), m.getTotalDurMillisec() , externalLf0,  externalLf0Delta);
          else
            System.out.format("%s  extDur=%.3f(%d)   extF0=%f  extF0Delta=%f\n", m.getPhoneName(), m.getUnit_duration(),  m.getTotalDurMillisec(), externalLf0,  externalLf0Delta);
          */
            
          lastPos = t;          
          for(state=0; state<5; state++) {                     
            for(frame=0; frame<m.getDur(state); frame++) {                      
                //extFile.write(Integer.toString(t) + " " + m.getPhoneName() + " " + Integer.toString(m.getTotalDur()) + " " 
                //        + Integer.toString(m.getDur(state)) + " " + Integer.toString(frame+1) + " ");
                if(externalLf0 > 0 ){ 
                    newVoiced[t] = true;
                    // without slope
                    //newLf0Pst.setPar(numVoiced, 0, externalLf0);
                    // with slope
                    newLf0Pst.setPar(numVoiced, 0, (externalLf0 + slope*(t-lastPos)));
                    
                    //extFile.write(Double.toString(Math.exp(newLf0Pst.getPar(numVoiced,0))) + "\n");
                    lf0Frame[t] = Math.exp(newLf0Pst.getPar(numVoiced, 0));
                    numVoiced++;                  
                } else {
                    newVoiced[t] = false;
                    //extFile.write("0.0\n");
                    lf0Frame[t] = externalLf0;
                }   
                
                //System.out.format("%d  %.3f\n", t, lf0Frame[t]); 
                t++;
            } // for each frame in this state 
          } // for each state in this model 
        }  // for each model in this utterance
      //extFile.close();
      //System.out.println("Created file:" + genF0);
   
      // set the external prosody as if it were generated
      setVoicedArray(newVoiced);
           
      //MaryUtils.plot(lf0Frame, "F0 contour");
      
      setlf0Pst(newLf0Pst);  
      
  }
  
  
  /***
   * Load logf0, in HTS format, create a voiced array and set this values in pdf2par
   * This contour should be aligned with the durations, so the total duration in frames should be the same as in the lf0 file 
   * @param lf0File: in HTS formant 
   * @param totalDurationFrames: the total duration in frames can be calculated as:
   *                             totalDurationFrames = totalDurationInSeconds / (framePeriodInSamples / SamplingFrequencyInHz)
   * @param pdf2par: HTSParameterGeneration object
   * @throws Exception If the number of frames in the lf0 file is not the same as represented in the total duration (in frames).
   */
  public void loadLogF0FromExternalFile(String lf0File, int totalDurationFrames) throws Exception{
      
      LEDataInputStream lf0Data;
      
      int lf0Vsize = 3;
      int totalFrame = 0;
      int lf0VoicedFrame = 0;
      float fval;  
      lf0Data = new LEDataInputStream (new BufferedInputStream(new FileInputStream(lf0File)));
      
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
      
      // Here we need to check that the total duration in frames is the same as the number of frames
      // (NOTE: it can be a problem afterwards when the durations per phone are aligned to the lenght of each state
      // in htsEngine._processUtt() )         
      if( totalDurationFrames > totalFrame){
        throw new Exception("The total duration in frames (" +  totalDurationFrames + ") is greater than the number of frames in the lf0File (" + 
             totalFrame + "): " + lf0File + "\nIt can be fixed to some extend using a smaller value for the variable: newStateDurationFactor");
      } else if( totalDurationFrames < totalFrame ){
        if (Math.abs(totalDurationFrames-totalFrame) < 5)
          System.out.println("Warning: The total duration in frames (" +  totalDurationFrames + ") is smaller than the number of frames in the lf0 file (" 
            + totalFrame + "): " + lf0File + "\n         It can be fixed to some extend using a greater value for the variable: newStateDurationFactor");
        else
          throw new Exception("The total duration in frames (" +  totalDurationFrames + ") is smaller than the number of frames in the lf0File (" + 
              totalFrame + "): " + lf0File + "\nIt can be fixed to some extend using a greater value for the variable: newStateDurationFactor");
        
      } else
        System.out.println("totalDurationFrames = " + totalDurationFrames + "  totalF0Frames = " + totalFrame);  
        
     
      voiced = new boolean[totalFrame];
      lf0Pst = new HTSPStream(lf0Vsize, totalFrame, HMMData.LF0);
      
      /* load lf0 data */
      /* for lf0 i just need to load the voiced values */
      lf0VoicedFrame = 0;
      lf0Data = new LEDataInputStream (new BufferedInputStream(new FileInputStream(lf0File)));
      for(int i=0; i<totalFrame; i++){
        fval = lf0Data.readFloat();  
        if(fval < 0){
          voiced[i] = false;
          //System.out.println("frame: " + i + " = 0.0");
        }
        else{
          voiced[i] = true;
          lf0Pst.setPar(lf0VoicedFrame, 0, fval);
          lf0VoicedFrame++;
          //System.out.format("frame: %d = %.2f\n", i, fval);
        }
      }
      lf0Data.close();
      
  }
 
  
  
} /* class ParameterGeneration */
