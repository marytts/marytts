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
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;

import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.synthesis.WaveformSynthesizer;
import de.dfki.lt.mary.modules.synthesis.Voice.Gender;

public class HMMVoice extends Voice {
 
    HMMData hts_data = new HMMData();
    
   /** 
    * constructor */ 
   public HMMVoice(String[] nameArray, Locale locale, 
           AudioFormat dbAudioFormat, WaveformSynthesizer synthesizer, 
           Gender gender, int topStart, int topEnd, int baseStart, int baseEnd,
           String Ftd, String Ftf, String Ftm, String Fts, String Fta, 
           String Fmd, String Fmf, String Fmm, String Fms, String Fma,
           String FeaList, String Flab, String Fif, int nFilters, int norderFilters) {
       super(nameArray, locale, dbAudioFormat, synthesizer, gender, topStart, topEnd, baseStart, baseEnd);

       this.hts_data.setTreeDurFile(Ftd);  /* CHECk do i need this this. ??? */
       this.hts_data.setTreeLf0File(Ftf);           
       this.hts_data.setTreeMcpFile(Ftm);
       this.hts_data.setTreeStrFile(Fts);
       this.hts_data.setTreeMagFile(Fta);

       this.hts_data.setPdfDurFile(Fmd);
       this.hts_data.setPdfLf0File(Fmf);        
       this.hts_data.setPdfMcpFile(Fmm);
       this.hts_data.setPdfStrFile(Fms);
       this.hts_data.setPdfMagFile(Fma);

       /* Feature list file */
       this.hts_data.setFeaListFile(FeaList);

       /* Example context feature file in HTSCONTEXT_EN format */
       this.hts_data.setLabFile(Flab);

       /* Configuration for mixed excitation */
       this.hts_data.setMixFiltersFile(Fif); 
       this.hts_data.set_numFilters(nFilters);
       this.hts_data.set_orderFilters(norderFilters);

       /* Load TreeSet ts and ModelSet ms*/
       this.hts_data.LoadModelSet();  
       this.hts_data.LoadTreeSet();   

   }
   
   public HMMData getHMMData(){ return this.hts_data; }
    

} /* class HMMVoice */
