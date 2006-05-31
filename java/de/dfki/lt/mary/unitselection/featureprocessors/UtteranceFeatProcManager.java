/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package de.dfki.lt.mary.unitselection.featureprocessors;

import java.util.*;
/**
 * Feature Processors Manager managing generic feature processors
 * that are operate on FreeTTS utterances
 */
public class UtteranceFeatProcManager extends UnitSelectionFeatProcManager{
    
   static{
        processors = new HashMap();
     
        processors.put("word_break", new GenericFeatureProcessors.WordBreak());
        processors.put("word_punc", new GenericFeatureProcessors.WordPunc());
        processors.put("word_numsyls",new GenericFeatureProcessors.WordNumSyls());
        processors.put("ssyl_in", new GenericFeatureProcessors.StressedSylIn());
        processors.put("syl_in", new GenericFeatureProcessors.SylIn());
        processors.put("syl_out", new GenericFeatureProcessors.SylOut());
        processors.put("ssyl_out", new GenericFeatureProcessors.StressedSylOut());
        processors.put("syl_break", new GenericFeatureProcessors.SylBreak());
        processors.put("old_syl_break", new GenericFeatureProcessors.SylBreak());
        processors.put("num_digits", new GenericFeatureProcessors.NumDigits());
        processors.put("month_range", new GenericFeatureProcessors.MonthRange());
        processors.put("segment_duration", 
                new GenericFeatureProcessors.SegmentDuration());
        processors.put("sub_phrases", new GenericFeatureProcessors.SubPhrases());
        processors.put("asyl_in", new GenericFeatureProcessors.AccentedSylIn());
        processors.put("last_accent", new GenericFeatureProcessors.LastAccent());
        processors.put("pos_in_syl", new GenericFeatureProcessors.PosInSyl());
        processors.put("position_type", new
                GenericFeatureProcessors.PositionType());
        processors.put("syl_final", 
                new GenericFeatureProcessors.SylFinal());
        processors.put("lisp_is_pau", 
                new GenericFeatureProcessors.LispIsPau());
        processors.put("accented", new GenericFeatureProcessors.Accented());
        processors.put("seg_pitch",
                new GenericFeatureProcessors.Seg_Pitch());
        processors.put("tobi_accent", new GenericFeatureProcessors.TobiAccent());
        processors.put("tobi_endtone", new GenericFeatureProcessors.TobiEndtone());
    
    }
   
   public static Map getProcessors(){
       return processors;
   }
}