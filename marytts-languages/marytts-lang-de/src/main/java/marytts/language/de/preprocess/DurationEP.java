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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for duration patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class DurationEP extends ExpansionPattern {
	private final String[] _knownTypes = { "duration", "duration:hms", "duration:hm", "duration:h" };
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

	// Domain-specific primitives:
	protected final String sHour = "(?:0?[0-9]|1[0-9]|2[0-4])";
	protected final String sMinute = "(?:[0-5][0-9])";
	protected final String sSecond = sMinute;
	protected final String sSep = "(?:\\:|\\.)";
	protected final String sFinal = "(?:h|Std\\.)";
	protected final String sMatchingChars = "[0-9:\\.hStd]";

	// Now the actual match patterns:
	protected final Pattern reHour = Pattern.compile("(" + sHour + ")" + sFinal);
	protected final Pattern reHourMinute = Pattern.compile("(" + sHour + ")" + sSep + "(" + sMinute + ")" + sFinal);
	protected final Pattern reHourMinuteSecond = Pattern.compile("(" + sHour + ")" + sSep + "(" + sMinute + ")" + sSep + "("
			+ sSecond + ")" + sFinal);

	private final Pattern reMatchingChars = Pattern.compile(sMatchingChars);

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	/**
	 * Every subclass has its own logger. The important point is that if several threads are accessing the variable at the same
	 * time, the logger needs to be thread-safe or it will produce rubbish.
	 */
	// private Logger logger = MaryUtils.getLogger("DurationEP");

	public DurationEP() {
		super();
	}

	protected int canDealWith(String s, int type) {
		return match(s, type);
	}

	protected int match(String s, int type) {
		switch (type) {
		case 0:
			if (matchDurationHMS(s))
				return 1;
			if (matchDurationHM(s))
				return 2;
			if (matchDurationH(s))
				return 3;
			break;
		case 1:
			if (matchDurationHMS(s))
				return 1;
			break;
		case 2:
			if (matchDurationHM(s))
				return 2;
			break;
		case 3:
			if (matchDurationH(s))
				return 3;
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
		case 1:
			expanded = expandDurationHMS(doc, s);
			break;
		case 2:
			expanded = expandDurationHM(doc, s);
			break;
		case 3:
			expanded = expandDurationH(doc, s);
			break;
		}
		replaceTokens(tokens, expanded);
		return expanded;
	}

	protected boolean matchDurationHMS(String s) {
		return reHourMinuteSecond.matcher(s).matches();
	}

	protected boolean matchDurationHM(String s) {
		return reHourMinute.matcher(s).matches();
	}

	protected boolean matchDurationH(String s) {
		return reHour.matcher(s).matches();
	}

	protected List<Element> expandDurationHMS(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reHourMinuteSecond.matcher(s);
		if (!reMatcher.find()) {
			return null;
		}
		String hour = reMatcher.group(1); // first bracket pair in reHour: hour
		String minute = reMatcher.group(2);
		String second = reMatcher.group(3);
		if (hour.equals("01") || hour.equals("1")) {
			exp.addAll(makeNewTokens(doc, "eine Stunde"));
		} else {
			exp.addAll(number.expandInteger(doc, hour, false));
			exp.addAll(makeNewTokens(doc, "Stunden"));
		}
		if (!minute.equals("00")) {
			if (!second.equals("00")) {
				exp.addAll(makeNewTokens(doc, "und"));
			}
			if (minute.equals("01")) {
				exp.addAll(makeNewTokens(doc, "eine Minute"));
			} else {
				exp.addAll(number.expandInteger(doc, minute, false));
				exp.addAll(makeNewTokens(doc, "Minuten"));
			}
		}
		if (!second.equals("00")) {
			exp.addAll(makeNewTokens(doc, "und"));
			if (second.equals("01")) {
				exp.addAll(makeNewTokens(doc, "eine Sekunde"));
			} else {
				exp.addAll(number.expandInteger(doc, minute, false));
				exp.addAll(makeNewTokens(doc, "Sekunden"));
			}
		}
		return exp;
	}

	protected List<Element> expandDurationHM(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reHourMinute.matcher(s);
		reMatcher.find();
		String hour = reMatcher.group(1);
		String minute = reMatcher.group(2);
		if (hour.equals("01") || hour.equals("1")) {
			exp.addAll(makeNewTokens(doc, "eine Stunde"));
		} else {
			exp.addAll(number.expandInteger(doc, hour, false));
			exp.addAll(makeNewTokens(doc, "Stunden"));
		}
		if (!minute.equals("00")) {
			exp.addAll(makeNewTokens(doc, "und"));
			if (minute.equals("01")) {
				exp.addAll(makeNewTokens(doc, "eine Minute"));
			} else {
				exp.addAll(number.expandInteger(doc, minute, false));
				exp.addAll(makeNewTokens(doc, "Minuten"));
			}
		}
		return exp;
	}

	protected List<Element> expandDurationH(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reHour.matcher(s);
		reMatcher.find();
		String hour = reMatcher.group(1); // first bracket pair in reHour: hour
		if (hour.equals("01") || hour.equals("1")) {
			exp.addAll(makeNewTokens(doc, "eine Stunde"));
		} else {
			exp.addAll(number.expandInteger(doc, hour, false));
			exp.addAll(makeNewTokens(doc, "Stunden"));
		}
		return exp;
	}

}
