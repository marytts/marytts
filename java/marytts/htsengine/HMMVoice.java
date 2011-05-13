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

import java.util.Locale;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;

import marytts.modules.synthesis.Voice;
import marytts.modules.synthesis.WaveformSynthesizer;
import marytts.util.MaryUtils;

import org.apache.log4j.Logger;


public class HMMVoice extends Voice {
 
    private HMMData htsData = new HMMData();
    private Logger logger = MaryUtils.getLogger("HMMVoice");
    
   /** 
    * constructor */ 
    public HMMVoice(String[] nameArray, Locale locale, 
            AudioFormat dbAudioFormat, WaveformSynthesizer synthesizer, 
            Gender gender,
            int samplingRate, int framePeriod,
            String alpha, String gamma, String logGain, String beta,
            String Ftd, String Ftf, String Ftm, String Fts, String Fta, 
            String Fmd, String Fmf, String Fmm, String Fms, String Fma,
            boolean useAcousticModels, boolean useMixExc, boolean useFourierMag, 
            boolean useGV, boolean useContextDependentGV, String gvMethod, 
            int maxMgcGvIter, int maxLf0GvIter, int maxStrGvIter, 
            String gvWeightMgc, String gvWeightLf0, String gvWeightStr,  
            String Fgvf, String Fgvm, String Fgvs, String Fgva, 
            String FeaFile, String trickyPhonesFile,
            String Fif, int nFilters) throws Exception {
        super(nameArray, locale, dbAudioFormat, synthesizer, gender);
        
       if(samplingRate>0)
         this.htsData.setRate(samplingRate);
       if(framePeriod>0)
         this.htsData.setFperiod(framePeriod);
       if(alpha != null) 
         this.htsData.setAlpha(Double.parseDouble(alpha));
       if(gamma != null)
         this.htsData.setStage(Integer.parseInt(gamma));
       if(logGain != null)
         this.htsData.setUseLogGain(Boolean.valueOf(logGain).booleanValue());
       if(beta != null)
         this.htsData.setBeta(Double.parseDouble(beta));

       this.htsData.setFeatureDefinition(FeaFile);
       this.htsData.setTreeDurFile(Ftd);  
       this.htsData.setTreeLf0File(Ftf);           
       this.htsData.setTreeMgcFile(Ftm);
       this.htsData.setTreeStrFile(Fts);
       this.htsData.setTreeMagFile(Fta);

       this.htsData.setPdfDurFile(Fmd);
       this.htsData.setPdfLf0File(Fmf);        
       this.htsData.setPdfMgcFile(Fmm);
       this.htsData.setPdfStrFile(Fms);
       this.htsData.setPdfMagFile(Fma);

       //this.htsData.setUseAcousticModels(Boolean.valueOf(useAcousticModels).booleanValue());
       this.htsData.setUseAcousticModels(useAcousticModels);
       this.htsData.setUseMixExc(useMixExc);
       this.htsData.setUseFourierMag(useFourierMag);
       this.htsData.setUseGV(useGV);
       if(useGV){
         this.htsData.setUseContextDepenendentGV(useContextDependentGV);
         this.htsData.setGvMethod(gvMethod);
         // Number of iteration for GV
         if(maxMgcGvIter > 0)
           this.htsData.setMaxMgcGvIter(maxMgcGvIter);
         if(maxLf0GvIter > 0)
           this.htsData.setMaxLf0GvIter(maxLf0GvIter);
         if(maxStrGvIter > 0)
           this.htsData.setMaxStrGvIter(maxLf0GvIter);
         // weights for GV
         if(gvWeightMgc != null) 
             this.htsData.setGvWeightMgc(Double.parseDouble(gvWeightMgc));
         if(gvWeightLf0 != null) 
             this.htsData.setGvWeightLf0(Double.parseDouble(gvWeightLf0));
         if(gvWeightStr != null) 
             this.htsData.setGvWeightStr(Double.parseDouble(gvWeightStr));
         // GV pdf files: mean and variance (diagonal covariance)
         this.htsData.setPdfLf0GVFile(Fgvf);        
         this.htsData.setPdfMgcGVFile(Fgvm);
         this.htsData.setPdfStrGVFile(Fgvs);
         this.htsData.setPdfMagGVFile(Fgva);
       } 
       
            
       /* Example context feature file in TARGETFEATURES format */
       this.htsData.setFeaFile(FeaFile);
       
       /* trickyPhones file if any*/
       this.htsData.setTrickyPhonesFile(trickyPhonesFile);

       /* Configuration for mixed excitation */
       if(Fif != null){
         this.htsData.setMixFiltersFile(Fif); 
         this.htsData.setNumFilters(nFilters);
         logger.info("Loading Mixed Excitation Filters File:");
         this.htsData.readMixedExcitationFiltersFile();
       }

       /* Load TreeSet in CARTs. */
       logger.info("Loading Tree Set in CARTs:");
       this.htsData.loadCartTreeSet();
       
       /* Load GV ModelSet gv*/
       logger.info("Loading GV Model Set:");
       this.htsData.loadGVModelSet();
       
   }
   
   public HMMData getHMMData(){ return this.htsData; }
   
   /* set parameters for generation: f0Std, f0Mean and length, default values 1.0, 0.0 and 0.0 */
   /* take the values from audio effects component through a MaryData object */
   public void setF0Std(double dval) { htsData.setF0Std(dval); }
   public void setF0Mean(double dval) { htsData.setF0Mean(dval); }
   public void setLength(double dval) { htsData.setLength(dval); }
   public void setDurationScale(double dval) { htsData.setDurationScale(dval); }
    

} /* class HMMVoice */
