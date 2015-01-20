package marytts.language.fr;

import java.util.Locale;

import com.ibm.icu.util.ULocale;
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.InternalModule;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import com.ibm.icu.text.RuleBasedNumberFormat;

/**
 * @author Tristan Hamilton
 */
public class NumPreprocess extends InternalModule {

	private RuleBasedNumberFormat rbnf;
	
	public NumPreprocess() {
		super("NumPreprocess", MaryDataType.TOKENS, MaryDataType.WORDS, Locale.FRENCH);
		this.rbnf = new RuleBasedNumberFormat(ULocale.FRENCH, RuleBasedNumberFormat.SPELLOUT);
	}

	public MaryData process(MaryData d) throws Exception {
		Document doc = d.getDocument();
		checkForNumbers(doc);
		MaryData result = new MaryData(getOutputType(), d.getLocale());
		result.setDocument(doc);
		return result;
	}

	protected void checkForNumbers(Document doc) {
		TreeWalker tw = ((DocumentTraversal) doc).createTreeWalker(doc, NodeFilter.SHOW_ELEMENT,
				new NameNodeFilter(MaryXML.TOKEN), false);
		Element t = null;
		while ((t = (Element) tw.nextNode()) != null) {
			if (MaryDomUtils.hasAncestor(t, MaryXML.SAYAS) || t.hasAttribute("ph") || t.hasAttribute("sounds_like")) {
				// ignore token
				continue;
			}
			if (MaryDomUtils.tokenText(t).matches("\\d+")) {
				MaryDomUtils.setTokenText(t, expandNumber(Double.parseDouble(MaryDomUtils.tokenText(t))));
			}
		}
	}
	
	protected String expandNumber(double number){
		return this.rbnf.format(number);
	}
}
