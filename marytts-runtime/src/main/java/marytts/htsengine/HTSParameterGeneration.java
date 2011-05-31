/* ----------------------------------------------------------------- */
/*           The HMM-Based Speech Synthesis Engine "hts_engine API"  */
/*           developed by HTS Working Group                          */
/*           http://hts-engine.sourceforge.net/                      */
/* ----------------------------------------------------------------- */
/*                                                                   */
/*  Copyright (c) 2001-2010  Nagoya Institute of Technology          */
/*                           Department of Computer Science          */
/*                                                                   */
/*                2001-2008  Tokyo Institute of Technology           */
/*                           Interdisciplinary Graduate School of    */
/*                           Science and Engineering                 */
/*                                                                   */
/* All rights reserved.                                              */
/*                                                                   */
/* Redistribution and use in source and binary forms, with or        */
/* without modification, are permitted provided that the following   */
/* conditions are met:                                               */
/*                                                                   */
/* - Redistributions of source code must retain the above copyright  */
/*   notice, this list of conditions and the following disclaimer.   */
/* - Redistributions in binary form must reproduce the above         */
/*   copyright notice, this list of conditions and the following     */
/*   disclaimer in the documentation and/or other materials provided */
/*   with the distribution.                                          */
/* - Neither the name of the HTS working group nor the names of its  */
/*   contributors may be used to endorse or promote products derived */
/*   from this software without specific prior written permission.   */
/*                                                                   */
/* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            */
/* CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       */
/* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          */
/* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          */
/* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS */
/* BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          */
/* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   */
/* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     */
/* DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON */
/* ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   */
/* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    */
/* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           */
/* POSSIBILITY OF SUCH DAMAGE.                                       */
/* ----------------------------------------------------------------- */
/**
 * Copyright 2011 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package marytts.htsengine;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.signalproc.analysis.Mfccs;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.MaryUtils;
import marytts.util.io.LEDataInputStream;

import org.apache.log4j.Logger;


/**
 * Parameter generation out of trained HMMs.
 * 
 * Java port and extension of HTS engine API version 1.04
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
  private int totalUttFrame;   // total number of frames in a mcep, str or mag Pst
  private int totalLf0Frame;   // total number of f0 voiced frames in a lf0 Pst
  
  private Logger logger = MaryUtils.getLogger("ParameterGeneration");
  
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
  static public double finv(double x) {
	  
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
  public void htsMaximumLikelihoodParameterGeneration(HTSUttModel um, HMMData htsData) throws Exception{
      htsMaximumLikelihoodParameterGeneration(um, htsData, "", false);
  }
  
  /** HTS maximum likelihood parameter generation
  * @param um  : utterance model sequence after processing Mary context features
  * @param ms  : HMM pdfs model set.
  * @param parFileName : file name to save parameters
  * @param debug : true for more debug information
  */
  public void htsMaximumLikelihoodParameterGeneration(HTSUttModel um, HMMData htsData, String parFileName, boolean debug) throws Exception{
	  
	int frame, uttFrame, lf0Frame;
	int state, lw, rw, k, n, i, numVoicedInModel;
	boolean nobound, gvSwitch;
    HTSModel m;
    CartTreeSet ms = htsData.getCartTreeSet();
    
	/* Initialisation of PStream objects */
  	/* Initialise Parameter generation using UttModel um and Modelset ms */
  	/* initialise PStream objects for all the parameters that are going to be generated: */
  	/* mceppst, strpst, magpst, lf0pst */
	/* Here i should pass the window files to initialise the dynamic windows dw */
	/* for the moment the dw are all the same and hard-coded */
    if( htsData.getPdfMgcFile() != null)
	  mcepPst = new HTSPStream(ms.getMcepVsize(), um.getTotalFrame(), HMMData.MGC, htsData.getMaxMgcGvIter());
    /* for lf0 count just the number of lf0frames that are voiced or non-zero */
    if( htsData.getPdfLf0File() != null)
      lf0Pst  = new HTSPStream(ms.getLf0Stream(), um.getLf0Frame(), HMMData.LF0, htsData.getMaxLf0GvIter());

    /* The following are optional in case of generating mixed excitation */
    if( htsData.getPdfStrFile() != null)
	  strPst  = new HTSPStream(ms.getStrVsize(), um.getTotalFrame(), HMMData.STR, htsData.getMaxStrGvIter());
    if (htsData.getPdfMagFile() != null )
	  magPst  = new HTSPStream(ms.getMagVsize(), um.getTotalFrame(), HMMData.MAG, htsData.getMaxMagGvIter());
	   
    
	uttFrame = lf0Frame = 0;
	voiced = new boolean[um.getTotalFrame()];
	
	for(i=0; i<um.getNumUttModel(); i++){
        m = um.getUttModel(i);
        numVoicedInModel = 0;
        for(state=0; state<ms.getNumStates(); state++)
      	 for(frame=0; frame<m.getDur(state); frame++) {
      		voiced[uttFrame] = m.getVoiced(state);
      		uttFrame++;
      		if(m.getVoiced(state)){
      		  lf0Frame++;
              numVoicedInModel++;
            }
      	 }
        m.setNumVoiced(numVoicedInModel);
    }
	/* mcepframe and lf0frame are used in the original code to initialise the T field */
	/* in each pst, but here the pst are already initialised .... */
	logger.debug("utteranceFrame=" + uttFrame + " lf0frame=" + lf0Frame);
	totalUttFrame = uttFrame;
	totalLf0Frame = lf0Frame;
	uttFrame = 0;
	lf0Frame = 0;
	
	/* copy pdfs */
	for(i=0; i<um.getNumUttModel(); i++){
      m = um.getUttModel(i);
      gvSwitch = m.getGvSwitch();
      for(state=0; state<ms.getNumStates(); state++) {
    	         
      	for(frame=0; frame<m.getDur(state); frame++) {
            
          //System.out.println("uttFrame=" + uttFrame + "  phone frame=" + frame + "  phone state=" + state);
             
      	  /* copy pdfs for mcep */
          if( mcepPst !=null ) {
      	    for(k=0; k<ms.getMcepVsize(); k++){
      		  mcepPst.setMseq(uttFrame, k, m.getMcepMean(state, k));
      		  // check the borders, if frame is bound or not
      		  if( (uttFrame == 0 || uttFrame == (totalUttFrame-1) ) && k >= mcepPst.getOrder() )
      		    mcepPst.setIvseq(uttFrame, k, 0.0);
      		  else
      		    mcepPst.setIvseq(uttFrame, k, finv(m.getMcepVariance(state, k)));
      	    }
      	    if(!gvSwitch) 
      	      mcepPst.setGvSwitch(uttFrame, false);
          }
      	  
      	  /* copy pdf for str */
          if( strPst !=null ) {
      	    for(k=0; k<ms.getStrVsize(); k++){
      		  strPst.setMseq(uttFrame, k, m.getStrMean(state, k));
      		  
      		  // check the borders, if frame is bound or not
              if( (uttFrame == 0 || uttFrame == (totalUttFrame-1) ) && k >= strPst.getOrder() )
                strPst.setIvseq(uttFrame, k, 0.0);
              else
                strPst.setIvseq(uttFrame, k, finv(m.getStrVariance(state, k)));
      	    }
      	    if(!gvSwitch) 
              strPst.setGvSwitch(uttFrame, false);
          }
      	  
      	  /* copy pdf for mag */
          if( magPst != null ) {
      	    for(k=0; k<ms.getMagVsize(); k++){
      		  magPst.setMseq(uttFrame, k, m.getMagMean(state, k));
      		  
      		  // check the borders, if frame is bound or not
              if( (uttFrame == 0 || uttFrame == (totalUttFrame-1) ) && k >= magPst.getOrder() )
                magPst.setIvseq(uttFrame, k, 0.0);
              else
                magPst.setIvseq(uttFrame, k, finv(m.getMagVariance(state, k)));
    	    }
      	    if(!gvSwitch) 
              magPst.setGvSwitch(uttFrame, false);
          }
      	  
      	  /* copy pdfs for lf0 */
          if( lf0Pst != null && !htsData.getUseAcousticModels() ) {
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
          }         
      	  if( voiced[uttFrame] ){
      	    if(!gvSwitch) 
                lf0Pst.setGvSwitch(lf0Frame, false); 
             lf0Frame++;      	 
      	  }
      	  uttFrame++;
      	  
      	} /* for each frame in this state */
      } /* for each state in this model */
	}  /* for each model in this utterance */ 
			
	/* parameter generation for mcep */  
    if( mcepPst != null ) {
	  logger.info("Parameter generation for MGC: ");
	  if(htsData.getUseGV())
	    mcepPst.setGvMeanVar(htsData.getGVModelSet().getGVmeanMgc(), htsData.getGVModelSet().getGVcovInvMgc()); 
      mcepPst.mlpg(htsData, htsData.getUseGV());
    }
   
    if(htsData.getUseAcousticModels())
        loadMaryXmlF0(um, htsData);
    else if ( lf0Pst != null ){
        logger.info("Parameter generation for LF0: ");
        if(htsData.getUseGV())
          lf0Pst.setGvMeanVar(htsData.getGVModelSet().getGVmeanLf0(), htsData.getGVModelSet().getGVcovInvLf0()); 
        lf0Pst.mlpg(htsData, htsData.getUseGV());
        // here we need set realisedF0
        //htsData.getCartTreeSet().getNumStates()
        setRealisedF0(lf0Pst, um, ms.getNumStates());
    }  
 
	/* parameter generation for str */
    boolean useGV=false;
    if( strPst != null ) {
      logger.info("Parameter generation for STR ");
      if(htsData.getUseGV() && (htsData.getPdfStrGVFile() != null) ){
        useGV = true;
        strPst.setGvMeanVar(htsData.getGVModelSet().getGVmeanStr(), htsData.getGVModelSet().getGVcovInvStr());
      }
      strPst.mlpg(htsData, useGV);
    }

	/* parameter generation for mag */
    useGV = false;
    if( magPst != null ) {
      logger.info("Parameter generation for MAG ");
      if(htsData.getUseGV() && (htsData.getPdfMagGVFile() != null) ){
        useGV = true;
        magPst.setGvMeanVar(htsData.getGVModelSet().getGVmeanMag(), htsData.getGVModelSet().getGVcovInvMag());
      }
	  magPst.mlpg(htsData, useGV);
    }
	   
    if(debug) {
        saveParam(parFileName+"mcep.bin", mcepPst, HMMData.MGC);  // no header
        saveParam(parFileName+"lf0.bin", lf0Pst, HMMData.LF0);    // no header
        //saveParamMaryFormat(parFileName, mcepPst, HMMData.MGC);
        //saveParamMaryFormat(parFileName, lf0Pst, HMMData.LF0);
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
             System.out.println("GEN f0s[" + t + "]=" + f0s[t]);
             
          }
          /* i am using this function but it changes the values of sw, and ss  *samplingrate+0.5??? for the HTS values ss=0.005 and sw=0.025 is not a problem though */
         PitchReaderWriter.write_pitch_file(fileName, f0s, (float)(ws), (float)(ss), fs);
          
          
      } else if(type == HMMData.MGC ){
          
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
          
      } else if(type == HMMData.MGC ){
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
  

  private void loadMaryXmlF0(HTSUttModel um, HMMData htsData) throws Exception{
      logger.info("Using f0 from maryXML acoustparams");      
      int i, n, numVoiced;      
      HTSModel m;         
      double[] dval;
      double lastF0 = 0.0;
      numVoiced=0; 
      Vector<Double> f0Vector = new Vector<Double>();
      
      for(i=0; i<um.getNumUttModel(); i++){
        m = um.getUttModel(i);                        
        //System.out.format("\nmodel=%s  totalDur=%d numVoicedFrames=%d F0=%s\n", m.getPhoneName(), m.getTotalDur(), m.getNumVoiced(), m.getMaryXmlF0());       
        // get contour for this model if voiced frames and maryXml has f0 values 
        dval = getContourSegment( m.getMaryXmlF0(), m.getNumVoiced());
        // accumulate the values
        for(n=0; n<dval.length; n++)
          f0Vector.add(dval[n]);    
      }
      // interpolate values if necessary
      interpolateSegments(f0Vector); 
      
      // create a new Lf0Pst with the values from maryXML
      HTSPStream newLf0Pst  = new HTSPStream(3, f0Vector.size(), HMMData.LF0, htsData.getMaxLf0GvIter());
      for(n=0; n<f0Vector.size(); n++)
        newLf0Pst.setPar(n, 0, Math.log(f0Vector.get(n)));
      
      setlf0Pst(newLf0Pst); 
      
  }
 
  private double[] getContourSegment(String maryXmlF0, int numVoiced) throws Exception{         
      int i, t=0, k=0, f=0;  // f is number of f0 in xml string 
      
      // just fill the values in approx. position
      double[] f0Vector = new double[numVoiced];
      
      int index[] = new int[2];
      double value[] = new double[2];
      Pattern p = Pattern.compile("(\\d+,\\d+)");       
      int key, n, interval;
      double valF0, lastValF0;  
      
      if(maryXmlF0 != null) {
          Matcher xml = p.matcher(maryXmlF0);        
          SortedMap<Integer,Double> f0Map = new TreeMap<Integer, Double>();
          int numF0s=0;
          while ( xml.find() ) {
            String[] f0Values = (xml.group().trim()).split(",");
            f0Map.put(new Integer(f0Values[0]), new Double(f0Values[1]));
            numF0s++;
          }          
          Set<Map.Entry<Integer,Double>> s = f0Map.entrySet();
          Iterator<Map.Entry<Integer,Double>> if0 = s.iterator();
          
          if(numF0s == numVoiced){
            t=0;
            while(if0.hasNext() && t < numVoiced) {              
              Map.Entry<Integer,Double> mf0 = if0.next();
              key   = (Integer)mf0.getKey();
              valF0 = (Double)mf0.getValue();
              f0Vector[t++] = valF0;
            }
          } else { 
            if(numF0s < numVoiced) {              
              for(i=0; i<numVoiced; i++)  // then just some values will be filled, so the other must be 0
                f0Vector[i] = 0.0;
            }
            while(if0.hasNext() && t < numVoiced) {              
              Map.Entry<Integer,Double> mf0 = if0.next();
              key   = (Integer)mf0.getKey();
              valF0 = (Double)mf0.getValue();
              if(key==0)
                n = 0;
              else if(key==100)
                n = numVoiced-1;
              else
                n = (int)((numVoiced*key)/100.0);              
              if(n >= 0 && n<numVoiced)
                f0Vector[n] = valF0;                            
          } // while(if0.hasNext())            
        } // numF0s == numVoiced          
      } // if maryXML != null
      
      //for(i=0; i<numVoiced; i++)  // then just some values will be filled, so the other must be 0
      //  System.out.format("%.1f ", f0Vector[i]);
      //System.out.println();
      
     return f0Vector;        
  }
  
  private void interpolateSegments(Vector<Double> f0){
      int i, n, interval;
      double slope;
      // check where there are zeros and interpolate
      int[] index = new int[2];
      double[] value = new double[2];
      
      index[0] = 0;
      value[0] = 0.0;
      for(i=0; i<f0.size(); i++){
        if(f0.get(i) > 0.0){
          index[1] = i;  
          value[1] = f0.get(i);
          
          interval = index[1] - index[0];
          if( interval > 1){
            //System.out.format("Interval to interpolate index[0]=%d  index[1]=%d\n",index[0],index[1]);
            slope = ((value[1]-value[0]) / interval);  
            for(n=index[0]; n<index[1]; n++) {
                double newVal = (slope * (n-index[0])) +  value[0];
                f0.set(n, newVal);
                //System.out.format("    n=%d  value:%.1f\n",n,newVal);
              }               
          }          
          index[0] = index[1];
          value[0] = value[1];          
        }          
      }      
  }
 
  
  private void setRealisedF0(HTSPStream lf0Pst, HTSUttModel um, int numStates) {
      int i, t, k, numVoicedInModel;      
      HTSModel m;
      int state, frame;            
      String formattedF0; 
      float f0;
      t=0;      
      for(i=0; i<um.getNumUttModel(); i++){
        m = um.getUttModel(i);
        numVoicedInModel = m.getNumVoiced();
        formattedF0 = "";
        k=1;
        for(state=0; state<numStates; state++) {
          for(frame=0; frame<m.getDur(state); frame++){
            if( voiced[t++] ){
                f0 = (float)Math.exp(lf0Pst.getPar(i,0));
                formattedF0 += "(" + Integer.toString((int)((k*100.0)/numVoicedInModel)) + "," + Integer.toString((int)f0) + ")";
                k++;  
            }
          } // for unvoiced frame                             
        } // for state
       if(!formattedF0.contentEquals("")){ 
           m.setMaryXmlF0(formattedF0);
         //m.setUnit_f0ArrayStr(formattedF0);  
         //System.out.println("ph=" + m.getPhoneName() + "  " + formattedF0);
       }
      }  // for model in utterance model list
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
      lf0Pst = new HTSPStream(lf0Vsize, totalFrame, HMMData.LF0, 0);
      
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
