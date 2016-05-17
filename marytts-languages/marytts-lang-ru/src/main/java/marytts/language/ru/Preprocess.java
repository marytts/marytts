package marytts.language.ru;

import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;


import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.InternalModule;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

public class Preprocess extends InternalModule {
	
	static final ULocale RU_LOCALE = new ULocale("ru_RU");
	String formatRules;
	RuleBasedNumberFormat format;
	String cardinal;
	
	public Preprocess(String name, MaryDataType inputType, MaryDataType outputType, Locale locale) {
		super("Preprocess", MaryDataType.TEXT, MaryDataType.WORDS, RU_LOCALE.toLocale());
		// TODO Auto-generated constructor stub
		
		try{
			formatRules = IOUtils.toString(this.getClass().getResourceAsStream("formatRules.txt"),
					"UTF-8");
		}catch(IOException io){
			logger.error(io);
		}
		format = new RuleBasedNumberFormat(formatRules, RU_LOCALE.toLocale());
		cardinal = "%spellout-numbering";
	}
	
	public Preprocess(){
		super("Preprocess", MaryDataType.TEXT, MaryDataType.WORDS, RU_LOCALE.toLocale());
	}
	
	public MaryData process(MaryData input) throws Exception{
		Document doc = input.getDocument();
		expand(doc);
		
		MaryData rlt = new MaryData(getOutputType(), input.getLocale());
		rlt.setDocument(doc);
		return rlt;
	}
	
	protected void expand(Document doc) throws ParseException, IOException, MaryConfigurationException {
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(MaryXML.TOKEN), false);
		Element t = null;

		// loop through each node in dom tree
		while ((t = (Element) tw.nextNode()) != null) {

			/*
			 * PRELIM FOR EACH NODE
			 */

			// save the original token text
			String origText = MaryDomUtils.tokenText(t);
			/*
			 * ACTUAL PROCESSING
			 */
			if(NumberUtils.isNumber(MaryDomUtils.tokenText(t))){
				MaryDomUtils.setTokenText(t, expandCardinal(Double.parseDouble(MaryDomUtils.tokenText(t))));
			}
		
		}
	}
	
	protected String expandCardinal(double number) {
        format.setDefaultRuleSet(cardinal);
        return format.format(number);
    }
	
	
	
}
