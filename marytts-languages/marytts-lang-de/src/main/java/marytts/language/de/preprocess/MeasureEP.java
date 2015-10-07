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
 * An expansion pattern implementation for measure patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class MeasureEP extends ExpansionPattern {
	private final String[] _knownTypes = { "measure" };
	/**
	 * Every subclass has its own list knownTypes, an internal string representation of known types. These are possible values of
	 * the <code>type</code> attribute to the <code>say-as</code> element, as defined in MaryXML.dtd. If there is more than one
	 * known type, the first type (<code>knownTypes[0]</code>) is expected to be the most general one, of which the others are
	 * specializations.
	 */
	private final List<String> knownTypes = Arrays.asList(_knownTypes);

	public List<String> knownTypes() {
		return knownTypes;
	}

	// number-dependent-feminine measure-symbol-names
	private final String[] _nuDeFeMeasureSymbolNames = { "s", "Sekunde", "sec", "Sekunde", "ms", "Millisekunde", "msec",
			"Millisekunde", "min", "Minute", "kcal", "Kilokalorie", "oz.", "Unze", "oz", "Unze", };

	// (number-independent) masculine (or neuter) measure-symbol-names
	private final String[] _maMeasureSymbolNames = { "km", "Kilometer", "dm", "Dezimeter", "cm", "Zentimeter", "mm",
			"Millimeter", "g", "Gramm", "kg", "Kilogramm", "mg", "Milligramm", "A", "Ampere[am-'pe:6]", "V", "Volt",
			"K",
			"Kelvin['kEl-vi:n]",
			new Character((char) 176).toString() + "C",
			"Grad Celsius['tsEl-zi:-Us]",
			new Character((char) 730).toString() + "C",
			"Grad Celsius['tsEl-zi:-Us]",
			"\u2103",
			"Grad Celsius['tsEl-zi:-Us]", // ℃
			new Character((char) 176).toString() + "F",
			"Grad Fahrenheit",
			new Character((char) 730).toString() + "F",
			"Grad Fahrenheit",
			"\u2109",
			"Grad Fahrenheit", // ℉
			"Hz", "Hertz", "kHz", "Kilohertz", "MHz", "Megahertz", "GHz", "GigaHertz", "N", "Newton['nju:-t@n]", "Pa", "Pascal",
			"J", "Joule['dZu:l]", "kJ", "Kilojoule['ki:-lo:-dZu:l]", "W", "Watt", "kW", "Kilowatt", "MW", "Megawatt", "GW",
			"Gigawatt", "mW", "Milliwatt", "l", "Liter", "dl", "Deziliter", "cl", "Zentiliter", "ml", "Milliliter", "Bq",
			"Becquerel[bE-k@-'rEl]", "EL", "Esslöffel", "TL", "Teelöffel", "qm", "Quadratmeter",
			"m" + new Character((char) 178).toString(), "Quadratmeter", "m" + new Character((char) 179).toString(), "Kubikmeter",
			"ccm", "Kubikzentimeter", "m", "Meter", "%", "Prozent", };

	private final Map<String, String> nuDeFeMeasureSymbolNames = MaryUtils.arrayToMap(_nuDeFeMeasureSymbolNames);

	private final Map<String, String> maMeasureSymbolNames = MaryUtils.arrayToMap(_maMeasureSymbolNames);

	// Domain-specific primitives:
	protected final String sMeasureSymbol = getMeasureSymbols();

	// We don't use sMatchingChars here, but override isCandidate().

	// Now the actual match patterns:
	protected final Pattern reMeasureSymbol = Pattern.compile("(" + sMeasureSymbol + ")");
	protected final Pattern reMeasure = Pattern.compile("(" + NumberEP.sInteger + "|" + NumberEP.sFloat + ")" + "("
			+ sMeasureSymbol + ")");

	private final Pattern reMatchingChars = null;

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	/**
	 * Every subclass has its own logger. The important point is that if several threads are accessing the variable at the same
	 * time, the logger needs to be thread-safe or it will produce rubbish.
	 * 
	 * @return _sMeasureSymbol.toString()
	 */
	// private Logger logger = MaryUtils.getLogger("MeasureEP");

	// Only used to initialize sMeasureSymbol from _measureSymbolNames[]:
	private String getMeasureSymbols() {
		StringBuilder _sMeasureSymbol = new StringBuilder("(?:");
		if (_nuDeFeMeasureSymbolNames.length > 0)
			_sMeasureSymbol.append(_nuDeFeMeasureSymbolNames[0]);
		for (int i = 2; i < _nuDeFeMeasureSymbolNames.length; i += 2) {
			_sMeasureSymbol.append("|" + _nuDeFeMeasureSymbolNames[i]);
		}
		if (_maMeasureSymbolNames.length > 0)
			_sMeasureSymbol.append(_maMeasureSymbolNames[0]);
		for (int i = 2; i < _maMeasureSymbolNames.length; i += 2) {
			_sMeasureSymbol.append("|" + _maMeasureSymbolNames[i]);
		}
		_sMeasureSymbol.append(")");
		return _sMeasureSymbol.toString();
	}

	public MeasureEP() {
		super();
	}

	protected boolean isCandidate(Element t) {
		String s = MaryDomUtils.tokenText(t);
		return (reMeasureSymbol.matcher(s).matches() || number.isCandidate(t) || reMeasure.matcher(s).matches());
	}

	protected int canDealWith(String s, int type) {
		return match(s, type);
	}

	protected int match(String s, int type) {
		switch (type) {
		case 0:
			if (reMeasure.matcher(s).matches())
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
			expanded = expandMeasure(doc, s);
			break;
		}
		replaceTokens(tokens, expanded);
		return expanded;
	}

	protected List<Element> expandMeasure(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		StringBuilder sb = new StringBuilder();
		String measure = null;
		String amount = null;
		Matcher reMatcher = reMeasure.matcher(s);
		if (!reMatcher.find())
			return null;
		amount = reMatcher.group(1);
		measure = reMatcher.group(2);
		// String measureName = (String)measureSymbolNames.get(measure);

		boolean measureIsMasculine = false;
		String measureName = (String) nuDeFeMeasureSymbolNames.get(measure);
		if (measureName == null) {
			measureName = (String) maMeasureSymbolNames.get(measure);
			measureIsMasculine = true;
			if (amount.equals("1"))
				sb.append("ein");
			else
				sb.append(number.expandFloat(amount));
		} else {
			if (amount.equals("1"))
				sb.append("eine");
			else
				sb.append(number.expandFloat(amount));
		}
		sb.append(" ");
		sb.append(measureName);
		if (!measureIsMasculine && !amount.equals("1"))
			sb.append("n");
		exp.addAll(makeNewTokens(doc, sb.toString(), true, s));
		return exp;
	}

}
