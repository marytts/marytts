/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.modules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import marytts.cart.CART;
import marytts.cart.StringPredictionTree;
import marytts.cart.io.WagonCARTReader;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.features.FeatureDefinition;
import marytts.features.FeatureProcessorManager;
import marytts.features.TargetFeatureComputer;
import marytts.fst.FSTLookup;
import marytts.modules.synthesis.Voice;
import marytts.server.MaryProperties;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.select.Target;
import marytts.unitselection.select.UnitSelector;
import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import opennlp.maxent.MaxentModel;
import opennlp.maxent.io.SuffixSensitiveGISModelReader;
import opennlp.tools.postag.DefaultPOSContextGenerator;
import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.postag.TagDictionary;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;


/**
 * Predict phoneme durations using a CART.
 *
 * @author Marc Schr&ouml;der
 */

public class OpenNLPPosTagger extends InternalModule
{
    private String propertyPrefix;
    private POSTaggerME tagger;
    private Map<String,String> posMapper = null;
    private FSTLookup posFST = null;
    /**
     * Constructor which can be directly called from init info in the config file.
     * Different languages can call this code with different settings.
     * @param locale a locale string, e.g. "en"

     * @throws Exception
     */
    public OpenNLPPosTagger(String locale, String propertyPrefix)
    throws Exception
    {
        super("OpenNLPPosTagger",
                MaryDataType.WORDS,
                MaryDataType.PARTSOFSPEECH,
                MaryUtils.string2locale(locale));
        if (!propertyPrefix.endsWith(".")) propertyPrefix = propertyPrefix + ".";
        this.propertyPrefix = propertyPrefix;
    }

    /**
     * Constructor which can be directly called from init info in the config file.
     * when locale is not available, new language config settings can call this part of code   
     * @param propertyPrefix
     * @throws Exception
     */
    public OpenNLPPosTagger(String propertyPrefix)
    throws Exception
    {
        super("OpenNLPPosTagger",
                MaryDataType.WORDS,
                MaryDataType.PARTSOFSPEECH,
                null);
        if (!propertyPrefix.endsWith(".")) propertyPrefix = propertyPrefix + ".";
        this.propertyPrefix = propertyPrefix;
    }
    
    
    public void startup() throws Exception
    {
        super.startup();
        
        // If locale is null, use fallback procedure for a newlanguage
        if(this.getLocale() == null){
            posFST = new FSTLookup(MaryProperties.needFilename(propertyPrefix+"fst"));
            return;
        }
        
        String modelFile = MaryProperties.needFilename(propertyPrefix+"model");
        String tagdict = MaryProperties.getFilename(propertyPrefix+"tagdict");
        boolean caseSensitive = MaryProperties.getBoolean(propertyPrefix+"tagdict.isCaseSensitive", true);
        String posMapperFile = MaryProperties.getFilename(propertyPrefix+"posMap");

        MaxentModel model = new SuffixSensitiveGISModelReader(new File(modelFile)).getModel();
        if (tagdict != null) {
            TagDictionary dict = new POSDictionary(tagdict,caseSensitive);
            tagger = new POSTaggerME(model, new DefaultPOSContextGenerator(null),dict);
        } else {
            tagger = new POSTaggerME(model, new DefaultPOSContextGenerator(null));
        }
        if (posMapperFile != null) {
            posMapper = new HashMap<String, String>();
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(posMapperFile), "UTF-8"));
            String line;
            while ((line = br.readLine()) != null) {
                // skip comments and empty lines
                if (line.startsWith("#") || line.trim().equals("")) continue;
                // Entry format: POS GPOS, i.e. two space-separated entries per line
                StringTokenizer st = new StringTokenizer(line);
                String pos = st.nextToken();
                String gpos = st.nextToken();
                posMapper.put(pos, gpos);
            }
        }
    }

    public MaryData process(MaryData d)
    throws Exception
    {
        // If locale is null, use fallback procedure for a newlanguage
        if(this.getLocale() == null){
            return newLanguageProcess(d);
        }   
        
        Document doc = d.getDocument(); 
        NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, doc, MaryXML.SENTENCE);
        Element sentence;
        while ((sentence = (Element) sentenceIt.nextNode()) != null) {
            TreeWalker tokenIt = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
            List<String> tokens = new ArrayList<String>();
            Element t;
            while ((t = (Element) tokenIt.nextNode()) != null) {
                tokens.add(MaryDomUtils.tokenText(t));
            }
            List<String> partsOfSpeech = tagger.tag(tokens);
            tokenIt.setCurrentNode(sentence); // reset treewalker so we can walk through once again
            Iterator<String> posIt = partsOfSpeech.iterator();
            while ((t = (Element) tokenIt.nextNode()) != null) {
                assert posIt.hasNext();
                String pos = posIt.next();
                if (posMapper != null) {
                    String gpos = posMapper.get(pos);
                    if (gpos == null) logger.warn("POS map file incomplete: do not know how to map '"+pos+"'");
                    else pos = gpos;
                }
                t.setAttribute("pos", pos);
            }
        }
        
        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setDocument(doc);
        return output;
    }
    
    /**
     * fallback procedure for parts-of-speech tag (to newlanguage support)
     * @param d
     * @return
     */
    public MaryData newLanguageProcess(MaryData d){
        Document doc = d.getDocument(); 
        NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, doc, MaryXML.SENTENCE);
        Element sentence;
        while ((sentence = (Element) sentenceIt.nextNode()) != null) {
            TreeWalker tokenIt = MaryDomUtils.createTreeWalker(sentence, MaryXML.TOKEN);
            Element t;
            while ((t = (Element) tokenIt.nextNode()) != null) {
                String pos = "content";
                if (posFST != null) {
                    String[] result = posFST.lookup(MaryDomUtils.tokenText(t));
                    if(result.length != 0)
                        pos = "function";
                }
                t.setAttribute("pos", pos);
            }
        }
        MaryData output = new MaryData(outputType(), d.getLocale());
        output.setDocument(doc);
        return output;
    }
    

}
