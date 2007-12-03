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
package de.dfki.lt.mary.unitselection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import javax.sound.sampled.AudioFormat;

import com.sun.speech.freetts.lexicon.Lexicon;

import de.dfki.lt.freetts.ClusterUnitNamer;
import de.dfki.lt.mary.modules.synthesis.Voice;
import de.dfki.lt.mary.modules.synthesis.WaveformSynthesizer;
import de.dfki.lt.mary.unitselection.cart.CART;
import de.dfki.lt.mary.unitselection.featureprocessors.FeatureDefinition;

/**
 * A Unit Selection Voice
 * 
 */
public class UnitSelectionVoice extends Voice { 

    protected UnitDatabase database;
    protected UnitSelector unitSelector;
    protected UnitConcatenator concatenator;
    protected String domain;
    protected String name;
    protected CART durationCart;
    protected CART[] f0Carts;
    protected String exampleText;
    protected FeatureDefinition durationCartFeatDef;
    protected FeatureDefinition f0CartsFeatDef;
    
    
    /**
     * Build a new UnitSelectionVoice
     * @param database the database of this voice
     * @param unitSelector the unit selector of this voice
     * @param concatenator the concatenator of this voice
     * @param path ?
     * @param nameArray the name(s) of this voice
     * @param locale the language of this voice
     * @param dbAudioFormat the AudioFormat of this voice
     * @param synthesizer the synthesizer of this voice
     * @param gender the gender of this voice
     * @param topStart ?
     * @param topEnd ?
     * @param baseStart ?
     * @param baseEnd ?
     * @param knownVoiceQualities ?
     * @param lexicon the name of the lexicon of this voice
     * @param domain the domain of this voice
     * @param exampleTextFile name of file containing example text for
     * 						  this voice (null for general domain voice)
     */
    public UnitSelectionVoice(UnitDatabase database, UnitSelector unitSelector,
            UnitConcatenator concatenator, String[] nameArray, Locale locale, 
            AudioFormat dbAudioFormat, WaveformSynthesizer synthesizer, 
            Gender gender, int topStart, int topEnd, int baseStart, int baseEnd,
            String domain,
            String exampleTextFile, CART durationCart, CART[] f0Carts,
            FeatureDefinition durationCartFeatDef, FeatureDefinition f0CartsFeatDef)
    {
        super(nameArray, locale, dbAudioFormat, synthesizer, gender, topStart, topEnd, baseStart, baseEnd);
        this.database = database; 
        this.unitSelector = unitSelector;
        this.concatenator = concatenator;
        this.domain = domain;
        this.name = nameArray[0];
        if (exampleTextFile != null)
            readExampleText(exampleTextFile);
        this.durationCart = durationCart;
        this.f0Carts = f0Carts;
        this.durationCartFeatDef = durationCartFeatDef;
        this.f0CartsFeatDef = f0CartsFeatDef;
    }
    
    
    /**
     * Gets the database of this voice
     * @return the database
     */
    public UnitDatabase getDatabase()
    {
        return database;
    }
    
    
    /**
     * Gets the unit selector of this voice
     * @return the unit selector
     */
    public UnitSelector getUnitSelector()
    {
        return unitSelector;
    }
    
    /**
     * Gets the unit concatenator of this voice
     * @return the unit selector
     */
    public UnitConcatenator getConcatenator()
    {
        return concatenator;
    }
    
    /**
     * Gets the domain of this voice
     * @return the domain
     */
    public String getDomain()
    {
        return domain;
    }
    
    public String getExampleText()
    {
        if (exampleText == null){
            return ("Sorry, no example text here#"+
            		"Hier gibt es leider keine Beispiele");}
        else {return exampleText;}
    }
    
    public void readExampleText(String file)
    {
        try {
            BufferedReader reader =
            	new BufferedReader(new InputStreamReader(new FileInputStream(new File(file)),"UTF-8"));
        	StringBuffer sb = new StringBuffer();
        	String line = reader.readLine();
        	while (line != null){
        	    if (!line.startsWith("***")){
        	        sb.append(line+"#");
        	    }
        	    line = reader.readLine();
        	}
            exampleText = sb.toString();            
        } catch(Exception e) {
            e.printStackTrace();
            throw new Error("Can not read in example text for voice "+name);
        }
    }
    
    public CART getDurationTree()
    {
        return durationCart;
    }
    
    public CART[] getF0Trees()
    {
        return f0Carts;
    }
    
    public FeatureDefinition getDurationCartFeatDef()
    {
        return durationCartFeatDef;
    }
    
    public FeatureDefinition getF0CartsFeatDef()
    {
        return f0CartsFeatDef;
    }
    
}
