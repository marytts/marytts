/**
 * Copyright 2002 DFKI GmbH.
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

package marytts.language.de.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;
import marytts.util.dom.MaryDomUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for currency patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class CurrencyEP extends ExpansionPattern {
	private final String[] _knownTypes = { "currency" };
	/**
	 * Every subclass has its own list knownTypes, an internal string representation of known types. These are possible values of
	 * the <code>type</code> attribute to the <code>say-as</code> element, as defined in MaryXML.dtd. If there is more than one
	 * known type, the first type (<code>knownTypes[0]</code>) is expected to be the most general one, of which the others are
	 * specialisations.
	 */
	private final List<String> knownTypes = Arrays.asList(_knownTypes);

	public List<String> knownTypes() {
		return knownTypes;
	}

	private final String[] _currencySymbolNames = { "DM", "Mark", new Character((char) 8364).toString(), "Euro", "$", "Dollar",
			"FF", "Francs['frO~]", new Character((char) 165).toString(), "Yen['jEn]", new Character((char) 163).toString(),
			"Pfund", "sFr.", "Franken", "Kr.", "Kronen", "USD", "U S Dollar", "ATS", "Schilling", "BEF",
			"belgische Francs['frO~]", "GBP", "britische Pfund",
			// avoid "CAD", "kanadische Dollar", because of Computer aided design
			"DKK", "daenische Kronen", "NLG", "Gulden", "EUR", "Euro", "Euro", "Euro", "FRF", "Francs['frO~]", "DEM", "Mark",
			"GRD", "Drachmen", "IEP", "irische Pfund", "ITL", "Lire", "JPY", "Yen['jEn]", "LUF", "luxemburgische Francs['frO~]",
			// avoid PLZ polish Zloty because of PLZ Postleitzahl
			"PTE", "Escudo[Es-'ku:-do:]", "RUB", "Rubel", "ESP", "Peseten", "SEK", "schwedische Kronen", "CHF", "Franken", };
	private final Map<String, String> currencySymbolNames = MaryUtils.arrayToMap(_currencySymbolNames);

	private final String[] _currencySymbolNamesSingular = { "DM", "eine Mark", new Character((char) 8364).toString(), "ein Euro",
			"$", "ein Dollar", "FF", "ein Francs['frO~]", new Character((char) 165).toString(), "ein Yen['jEn]",
			new Character((char) 163).toString(), "ein Pfund", "sFr.", "ein Franken", "Kr.", "eine Krone", "USD",
			"ein U S Dollar", "ATS", "ein Schilling", "BEF", "ein belgischer Francs['frO~]",
			"GBP",
			"ein britisches Pfund",
			// avoid "CAD", "kanadische Dollar", because of Computer aided design
			"DKK", "eine daenische Krone", "NLG", "ein Gulden", "EUR", "ein Euro", "FRF", "ein Francs['frO~]", "DEM",
			"eine Mark", "GRD", "eine Drachme", "IEP", "ein irisches Pfund", "ITL", "eine Lire", "JPY", "ein Yen['jEn]", "LUF",
			"ein luxemburgischer Francs['frO~]",
			// avoid PLZ polish Zloty because of PLZ Postleitzahl
			"PTE", "ein Escudo[Es-'ku:-do:]", "RUB", "ein Rubel", "ESP", "eine Pesete", "SEK", "eine schwedische Krone", "CHF",
			"ein Franken", };
	private final Map<String, String> currencySymbolNamesSingular = MaryUtils.arrayToMap(_currencySymbolNamesSingular);

	// Domain-specific primitives:
	protected final String sCurrencySymbol = getCurrencySymbols();
	protected final String sCurrencyAmount = "(?:" + NumberEP.sInteger + "(?:[,.](?:-|[0-9][0-9]))?)";
	protected final String sCurrencyAmountSubstructure = "(?:(" + NumberEP.sInteger + ")(?:[,.](-|[0-9][0-9]))?)";
	// in this, first parenthesis are the wholes and second paren are the cents.
	// We don't use sMatchingChars here, but override isCandidate().

	// Now the actual match patterns:
	protected final Pattern reCurrencyLeading = Pattern.compile("(" + sCurrencySymbol + ")(" + sCurrencyAmount + ")");
	protected final Pattern reCurrencyTrailing = Pattern.compile("(" + sCurrencyAmount + ")(" + sCurrencySymbol + ")");
	protected final Pattern reCurrencyAmountSubstructure = Pattern.compile(sCurrencyAmountSubstructure);

	private final Pattern reMatchingChars = null;

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	/**
	 * Every subclass has its own logger. The important point is that if several threads are accessing the variable at the same
	 * time, the logger needs to be thread-safe or it will produce rubbish.
	 * 
	 * @return _sCurrencySymbol.toString()
	 */
	// private Logger logger = MaryUtils.getLogger("CurrencyEP");

	// Only used to initialise sCurrencySymbol from _currencySymbolNames[]:
	private String getCurrencySymbols() {
		StringBuilder _sCurrencySymbol = new StringBuilder("(?:\\$");
		for (int i = 0; i < _currencySymbolNames.length; i += 2) {
			if (!_currencySymbolNames[i].equals("$")) {
				// $ needs to be quoted in regular expression
				_sCurrencySymbol.append("|" + _currencySymbolNames[i]);
			}
		}
		_sCurrencySymbol.append(")");
		return _sCurrencySymbol.toString();
	}

	public CurrencyEP() {
		super();
	}

	protected boolean isCandidate(Element t) {
		String s = MaryDomUtils.tokenText(t);
		return (s.length() <= 4 || number.isCandidate(t) || matchCurrency(s));
	}

	protected int canDealWith(String s, int type) {
		return match(s, type);
	}

	protected int match(String s, int type) {
		switch (type) {
		case 0:
			if (matchCurrency(s))
				return 0;
			break;
		}
		return -1;
	}

	protected List<Element> expand(List<Element> tokens, String s, int type) {
		if (tokens == null)
			throw new NullPointerException("Received null argument");
		if (tokens.isEmpty())
			throw new IllegalArgumentException("Received empty list");
		Document doc = ((Element) tokens.get(0)).getOwnerDocument();
		// we expect type to be one of the return values of match():
		List<Element> expanded = null;
		switch (type) {
		case 0:
			expanded = expandCurrency(doc, s);
			break;
		}
		replaceTokens(tokens, expanded);
		return expanded;
	}

	private boolean matchCurrency(String s) {
		return reCurrencyLeading.matcher(s).matches() || reCurrencyTrailing.matcher(s).matches();
	}

	protected List<Element> expandCurrency(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		StringBuilder sb = new StringBuilder();
		String currency = null;
		String amount = null;
		Matcher reMatcher = reCurrencyLeading.matcher(s);
		if (reMatcher.find()) { // OK, matched
			currency = reMatcher.group(1);
			amount = reMatcher.group(2);
		} else {
			reMatcher = reCurrencyTrailing.matcher(s);
			if (!reMatcher.find())
				return null;
			amount = reMatcher.group(1);
			currency = reMatcher.group(2);
		}
		// Now in amount, find wholes and cents:
		reMatcher = reCurrencyAmountSubstructure.matcher(amount);
		reMatcher.find();
		String wholes = reMatcher.group(1);
		// Special treatment of singular.
		// This does not accout for case (dativ, akkusativ).
		if (wholes.equals("1")) {
			String singularExpansion = (String) currencySymbolNamesSingular.get(currency);
			sb.append(singularExpansion);
		} else {
			sb.append(number.expandInteger(wholes));
			sb.append(" ");
			String currencyName = (String) currencySymbolNames.get(currency);
			sb.append(currencyName);
		}
		String cents = reMatcher.group(2);
		if (cents != null && cents.length() > 0 && !cents.equals("-")) {
			// OK, cents are two digits
			sb.append(" ");
			sb.append(number.expandInteger(cents));
		}
		exp.addAll(makeNewTokens(doc, sb.toString(), true, s));
		return exp;
	}

}
