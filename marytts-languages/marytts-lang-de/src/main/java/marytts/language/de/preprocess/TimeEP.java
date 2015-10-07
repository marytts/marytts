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
 * An expansion pattern implementation for time patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class TimeEP extends ExpansionPattern {
	private final String[] _knownTypes = { "time", "time:hms", "time:hm", "time:h", "time:hms12", "time:hms24" };
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
	protected final String sHour12 = "(?:0?[0-9]|1[0-2])";
	protected final String sMinute = "(?:[0-5][0-9])";
	protected final String sSecond = sMinute;
	protected final String sSep = "(?:\\:|\\.)";
	protected final String sFinal = "(?:h|Uhr)";
	protected final String sMatchingChars = "[0-9:\\.Uhr]";
	protected final String timeOfDay = "a|A|am|AM|Am|aM|p|P|pm|PM|Pm|pM";

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
	// private Logger logger = MaryUtils.getLogger("TimeEP");

	public TimeEP() {
		super();
	}

	// reluctant match: for TEXT_DE input
	protected int match(String s, int type) {
		switch (type) {
		case 0:
			if (matchTimeHMS(s))
				return 1;
			if (matchTimeHM(s))
				return 2;
			// if (matchTimeH(s)) return 3;
			// shall not match in order to let DurationEP match:
			// -> "one hour" instead of "one o'clock"
			break;
		// cases 1 to 5 are say-as - cases and therefore left to
		// canDealWith();
		/*
		 * case 1: if (matchTimeHMS(s)) return 1; break; case 2: if (matchTimeHM(s)) return 2; break; case 3: if (matchTimeH(s))
		 * return 3; break; case 4: if (matchTimeHMS(s)) return 1; break; case 5: if (matchTimeHMS(s)) return 1; break;
		 */
		}
		return -1;
	}

	// greedy match: for SSML input
	protected int canDealWith(String s, int type) {
		switch (type) {
		// SSML doesn't distinguish between many different kinds
		// say-as-time-elements. so either it matches TimeHMS12 or
		// TimeHMS24 or none.
		// In consideration of this Class being written for
		// German, this method returns automatically "hms24", if
		// "hms12" doesn't fit. (for English it should better return
		// "hmsUnknown" or "hms12".)
		/*
		 * case 0: if (canDealWithTimeHMS12(s)) return 4; if (canDealWithTimeHMS24(s)) return 5; break; case 1: if
		 * (canDealWithTimeHMS12(s)) return 4; if (canDealWithTimeHMS24(s)) return 5; break;
		 */
		case 4:
			if (canDealWithTimeHMS12(s))
				return 4;
			break;
		case 5:
			if (canDealWithTimeHMS24(s))
				return 5;
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
			expanded = expandTimeHMS(doc, s);
			break;
		case 2:
			expanded = expandTimeHM(doc, s);
			break;
		case 3:
			expanded = expandTimeH(doc, s);
			break;
		case 4:
			expanded = expandTimeHMS12(doc, s);
			break;
		case 5:
			expanded = expandTimeHMS24(doc, s);
			break;
		}
		replaceTokens(tokens, expanded);
		return expanded;
	}

	protected boolean matchTimeHMS(String s) {
		return reHourMinuteSecond.matcher(s).matches();
	}

	protected boolean matchTimeHM(String s) {
		return reHourMinute.matcher(s).matches();
	}

	protected boolean matchTimeH(String s) {
		return reHour.matcher(s).matches();
	}

	protected List<Element> expandTimeHMS(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		StringBuilder sb = new StringBuilder();
		Matcher reMatcher = reHourMinuteSecond.matcher(s);
		if (!reMatcher.find()) {
			return null;
		}
		String hour = reMatcher.group(1); // first bracket pair in reHour: hour
		if (hour.equals("1") || hour.equals("01")) {
			sb.append("ein");
		} else {
			sb.append(number.expandInteger(hour));
		}
		String minute = reMatcher.group(2);
		sb.append(" Uhr");
		if (!minute.equals("00")) {
			sb.append(" ");
			sb.append(number.expandInteger(minute));
		}
		// Create one mtu from hour and minute:
		// !!!! (the original text for the mtu is actually not s)
		exp.addAll(makeNewTokens(doc, sb.toString(), true, s));
		String second = reMatcher.group(3);
		if (!second.equals("00")) {
			exp.addAll(makeNewTokens(doc, "und"));
			if (second.equals("01")) {
				exp.addAll(makeNewTokens(doc, "eine Sekunde"));
			} else {
				exp.addAll(number.expandInteger(doc, second, false));
				exp.addAll(makeNewTokens(doc, "Sekunden"));
			}
		}
		return exp;
	}

	protected List<Element> expandTimeHM(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		StringBuilder sb = new StringBuilder();
		Matcher reMatcher = reHourMinute.matcher(s);
		reMatcher.find();
		String hour = reMatcher.group(1);
		if (hour.equals("1") || hour.equals("01")) {
			sb.append("ein");
		} else {
			sb.append(number.expandInteger(hour));
		}
		String minute = reMatcher.group(2);
		sb.append(" Uhr");
		if (!minute.equals("00")) {
			sb.append(" ");
			sb.append(number.expandInteger(minute));
		}
		// Create one mtu from hour and minute:
		exp.addAll(makeNewTokens(doc, sb.toString(), true, s));
		return exp;
	}

	protected List<Element> expandTimeH(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reHour.matcher(s);
		reMatcher.find();
		String hour = reMatcher.group(1); // first bracket pair in reHour: hour
		if (hour.equals("1") || hour.equals("01")) {
			exp.addAll(makeNewTokens(doc, "ein"));
		} else {
			exp.addAll(number.expandInteger(doc, hour, false));
		}
		exp.addAll(makeNewTokens(doc, "Uhr"));
		return exp;
	}

	// this is extremly greedy in consideration of the content
	// really being expected to represent a time
	protected boolean canDealWithTimeHMS12(String s) {
		return containsOneOrMoreDigits(s);
	}

	protected boolean canDealWithTimeHMS24(String s) {
		return containsOneOrMoreDigits(s);
	}

	protected List<Element> expandTimeHMS12(Document doc, String s) {
		boolean isAfternoon = isAfternoon(s);
		s = extractDigits(s);
		if (s.length() == 0)
			return null; // alternatively it could return "Null Uhr"
							// (that means: "midnight");
		// add an initial "0" if hours are represented in one digit
		if (s.length() % 2 == 1)
			s = "0" + s;
		if (isAfternoon) {
			// add 12 hours, because it's pm
			if (s.length() > 2) {
				String hours = add12Hours(s.substring(0, 2));
				s = hours + s.substring(2, s.length());
			} else {
				s = add12Hours(s);
			}
		}
		return expandTimeHMS12or24(doc, s);
	}

	protected List<Element> expandTimeHMS24(Document doc, String s) {
		s = extractDigits(s);
		if (s.length() == 0)
			return null; // alternatively it could return "Null Uhr"
							// (that means: "midnight");
		// add an initial "0" if hours are represented in one digit
		if (s.length() % 2 == 1)
			s = "0" + s;
		return expandTimeHMS12or24(doc, s);
	}

	private List<Element> expandTimeHMS12or24(Document doc, String s) {
		// insert seperators:
		for (int i = s.length() - 1; i > 1; i--) {
			if (i % 2 == 0)
				s = s.substring(0, i) + ":" + s.substring(i);
		}
		// append sFinal, otherwise no matching
		s += "h";
		switch (s.length()) {
		case 3:
			// just hours:
			return expandTimeH(doc, s);
		case 6:
			// hours and minutes:
			return expandTimeHM(doc, s);
		case 9:
			// hours, minutes and seconds:
			return expandTimeHMS(doc, s);
		}
		// else (though it may not occur):
		return null;
	}

	// tells, whether (a) 'pm' indicating letter(s) is/are contained
	private boolean isAfternoon(String s) {
		for (int i = 0; i < s.length(); i++) {
			// "a", "A", "am" and "AM" indicate pre-noon
			if (s.toLowerCase().charAt(i) == 'p')
				return true;
		}
		// else
		return false;
	}

	// only makes sense for an expedient argument
	private String add12Hours(String s) {
		int iHour = 0;
		if (s.length() == 1) {
			iHour = (int) (s.charAt(0) - 32);
			iHour += 12;
		}
		if (s.length() == 2) {
			iHour = ((int) s.charAt(1)) - 48;
			iHour += (((int) s.charAt(0)) - 48) * 10;
			iHour += 12;
		}
		// ... longing for C methods atoi() and itoa() ...
		return (new Integer(iHour)).toString();
	}

	private String extractDigits(String s) {
		StringBuilder sB = new StringBuilder(s);
		for (int i = 0; i < sB.length(); i++)
			if (!('0' <= sB.charAt(i) && sB.charAt(i) <= '9'))
				sB.deleteCharAt(i--);
		return sB.toString();
	}

	private boolean containsOneOrMoreDigits(String s) {
		for (int i = 0; i < s.length(); i++) {
			if ('0' <= s.charAt(i) && s.charAt(i) <= '9')
				return true;
		}
		// else
		return false;
	}
}
