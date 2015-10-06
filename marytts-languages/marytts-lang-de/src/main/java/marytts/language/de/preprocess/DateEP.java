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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An expansion pattern implementation for date patterns.
 *
 * @author Marc Schr&ouml;der
 */

public class DateEP extends ExpansionPattern {
	private final String[] _knownTypes = { "date", "date:dmy", "date:ymd", "date:dm", "date:my", "date:y", "date:m", "date:d",
			"date:mdy", "date:md" };
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

	private final String[] _monthabbr = { "Jan", "Januar", "Feb", "Februar", "Mrz", "März", "Apr",
			"April",
			// no abbreviation for Mai
			"Jun", "Juni", "Jul", "Juli", "Aug", "August", "Sep", "September", "Okt", "Oktober", "Nov", "November", "Dez",
			"Dezember", "1", "Januar", "2", "Februar", "3", "März", "4", "April", "5", "Mai", "6", "Juni", "7", "Juli", "8",
			"August", "9", "September", "10", "Oktober", "11", "November", "12", "Dezember" };
	private final Map<String, String> monthabbr = MaryUtils.arrayToMap(_monthabbr);

	// Domain-specific primitives:
	protected final String sDay = "(?:0?[1-9]|[12][0-9]|3[01])";
	protected final String sMonth = "(?:0[1-9]|1[0-2])";
	protected final String sMonthword = "(?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember|Frühjahr|Sommer|Herbst|Winter)";
	protected final String sMonthabbr = "(?:Jan|Feb|Mrz|Apr|Jun|Jul|Aug|Sep|Okt|Nov|Dez)";
	protected final String sYear = "(?:([0-9][0-9])?[0-9][0-9])";
	protected final String sDot = "[\\.]";
	protected final String sSlash = "[/]";
	protected final String sMinus = "[\\-]";
	protected final String sMatchingChars = "[0-9/\\.JFMASONDanurebäzpilgstmkov]";
	protected final String sSeparators = (sDot + "|" + sSlash + "|" + sMinus);

	// Now the actual match patterns:
	protected final Pattern reDayMonthYear = Pattern.compile("(" + sDay + ")" + "(?:" + sDot + "|" + sSlash + ")" + "(" + sMonth
			+ ")" + "(?:" + sDot + "|" + sSlash + ")" + "(" + sYear + ")");
	protected final Pattern reDayMonthwordYear = Pattern.compile("(" + sDay + ")" + sDot + "(" + sMonthword + ")" + "(" + sYear
			+ ")");
	protected final Pattern reDayMonthabbrYear = Pattern.compile("(" + sDay + ")" + sDot + "(" + sMonthabbr + ")" + sDot + "?"
			+ "(" + sYear + ")");

	protected final Pattern reYearMonthDay = Pattern.compile("(" + sYear + ")" + "(?:" + sDot + "|" + sSlash + ")" + "(" + sMonth
			+ ")" + "(?:" + sDot + "|" + sSlash + ")" + "(" + sDay + ")");
	protected final Pattern reYearMonthwordDay = Pattern.compile("(" + sYear + ")" + sDot + "(" + sMonthword + ")" + "(" + sDay
			+ ")");
	protected final Pattern reYearMonthabbrDay = Pattern.compile("(" + sYear + ")" + sDot + "(" + sMonthabbr + ")" + sDot + "?"
			+ "(" + sDay + ")");

	protected final Pattern reMonthDayYear = Pattern.compile("(" + sMonth + ")" + "(?:" + sSeparators + ")" + "(" + sDay + ")"
			+ "(?:" + sSeparators + ")" + "(" + sYear + ")");
	protected final Pattern reMonthwordDayYear = Pattern.compile("(" + sMonthword + ")" + "(" + sDay + ")(?:" + sSeparators + ")"
			+ "(" + sYear + ")");
	protected final Pattern reMonthabbrDayYear = Pattern
			.compile("(?:(Jan|Feb|Mrz|Apr|Jun|Jul|Aug|Sep|Okt|Nov|Dez)|(Jan|Feb|Mrz|Apr|Jun|Jul|Aug|Sep|Okt|Nov|Dez)\\.)" + "(?:"
					+ sSeparators + ")" + "(" + sDay + ")" + "(?:" + sSeparators + ")" + "(" + sYear + ")");

	protected final Pattern reDayMonth = Pattern.compile("(" + sDay + ")" + "(?:" + sDot + "|" + sSlash + ")" + "(" + sMonth
			+ ")" + "(?:" + sDot + "|" + sSlash + ")");
	protected final Pattern reDayMonthword = Pattern.compile("(" + sDay + ")" + sDot + "(" + sMonthword + ")");
	protected final Pattern reDayMonthabbr = Pattern.compile("(" + sDay + ")" + sDot + "(" + sMonthabbr + ")" + sDot + "?");

	protected final Pattern reMonthDay = Pattern.compile("(" + sMonth + ")" + "(?:" + sSeparators + ")" + "(" + sDay + ")"
			+ "(?:" + sSeparators + ")");
	protected final Pattern reMonthwordDay = Pattern.compile("(" + sMonthword + ")" + "(" + sDay + ")(?:" + sDot + ")");
	protected final Pattern reMonthabbrDay = Pattern
			.compile("(?:(Jan|Feb|Mrz|Apr|Jun|Jul|Aug|Sep|Okt|Nov|Dez)|(Jan|Feb|Mrz|Apr|Jun|Jul|Aug|Sep|Okt|Nov|Dez)\\.)" + "(?:"
					+ sSeparators + ")" + "(" + sDay + ")" + "(?:" + sSeparators + ")");

	protected final Pattern reMonthYear = Pattern.compile("(" + sMonth + ")(" + sSlash + "|" + sDot + ")(" + sYear + ")");
	protected final Pattern reMonthwordYear = Pattern.compile("(" + sMonthword + ")" + "(" + sYear + ")");
	protected final Pattern reMonthabbrYear = Pattern.compile("(" + sMonthabbr + ")" + sDot + "?" + "(" + sYear + ")");

	protected final Pattern reYear = Pattern.compile("(?:([0-9][0-9])?[0-9][0-9])");

	protected final Pattern reMonth = Pattern.compile("(" + sMonth + ")" + "(?:" + sDot + ")");
	protected final Pattern reMonthword = Pattern.compile("(" + sMonthword + ")");
	protected final Pattern reMonthabbr = Pattern
			.compile("((?:(Jan|Feb|Mrz|Apr|Jun|Jul|Aug|Sep|Okt|Nov|Dez)|(Jan|Feb|Mrz|Apr|Jun|Jul|Aug|Sep|Okt|Nov|Dez)\\.))");

	protected final Pattern reDay = Pattern.compile("(" + sDay + ")" + "?:" + sDot);

	private final Pattern reMatchingChars = Pattern.compile(sMatchingChars);

	public Pattern reMatchingChars() {
		return reMatchingChars;
	}

	/**
	 * Every subclass has its own logger. The important point is that if several threads are accessing the variable at the same
	 * time, the logger needs to be thread-safe or it will produce rubbish.
	 */
	private Logger logger = MaryUtils.getLogger("DateEP");

	public DateEP() {
		super();
	}

	protected int canDealWith(String s, int type) {
		return match(s, type);
	}

	protected int match(String s, int type) {
		switch (type) {
		case 0:
			if (matchDateDMY(s))
				return 1;
			if (matchDateYMD(s))
				return 2;
			if (matchDateDM(s))
				return 3;
			// if (matchDateMY(s)) return 4;
			// if (matchDateMDY(s)) return 8;
			// if (matchDateMD(s)) return 9;
			// if (matchDateM(s)) return 6;
			// if (matchDateD(s)) return 7;
			// if (matchDateY(s)) return 5;
			break;
		case 1:
			if (matchDateDMY(s))
				return 1;
			break;
		case 2:
			if (matchDateYMD(s))
				return 2;
			break;
		case 3:
			if (matchDateDM(s))
				return 3;
			break;
		case 4:
			if (matchDateMY(s))
				return 4;
			break;
		case 5:
			if (matchDateY(s))
				return 5;
			break;
		case 6:
			if (matchDateM(s))
				return 6;
			break;
		case 7:
			if (matchDateD(s))
				return 7;
			break;
		case 8:
			if (matchDateMDY(s))
				return 8;
			break;
		case 9:
			if (matchDateMD(s))
				return 9;
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
			expanded = expandDateDMY(doc, s);
			break;
		case 2:
			expanded = expandDateYMD(doc, s);
			break;
		case 3:
			expanded = expandDateDM(doc, s);
			break;
		case 4:
			expanded = expandDateMY(doc, s);
			break;
		case 5:
			expanded = expandDateYear(doc, s);
			break;
		case 6:
			expanded = expandDateMonth(doc, s);
			break;
		case 7:
			expanded = expandDateDay(doc, s);
			break;
		case 8:
			expanded = expandDateMDY(doc, s);
			break;
		case 9:
			expanded = expandDateMD(doc, s);
			break;
		}
		replaceTokens(tokens, expanded);
		return expanded;
	}

	protected boolean matchDateDMY(String s) {
		return reDayMonthYear.matcher(s).matches() || reDayMonthwordYear.matcher(s).matches()
				|| reDayMonthabbrYear.matcher(s).matches();
	}

	protected boolean matchDateYMD(String s) {
		return reYearMonthDay.matcher(s).matches() || reYearMonthwordDay.matcher(s).matches()
				|| reYearMonthabbrDay.matcher(s).matches();
	}

	protected boolean matchDateDM(String s) {
		return reDayMonth.matcher(s).matches() || reDayMonthword.matcher(s).matches() || reDayMonthabbr.matcher(s).matches();
	}

	protected boolean matchDateMY(String s) {
		return reMonthYear.matcher(s).matches() || reMonthwordYear.matcher(s).matches() || reMonthabbrYear.matcher(s).matches();
	}

	protected boolean matchDateY(String s) {
		return reYear.matcher(s).matches();
	}

	protected boolean matchDateM(String s) {
		return reMonth.matcher(s).matches() || reMonthword.matcher(s).matches() || reMonthabbr.matcher(s).matches();
	}

	protected boolean matchDateD(String s) {
		return reDay.matcher(s).matches();
	}

	protected boolean matchDateMDY(String s) {
		return reMonthDayYear.matcher(s).matches() || reMonthwordDayYear.matcher(s).matches()
				|| reMonthabbrDayYear.matcher(s).matches();
	}

	protected boolean matchDateMD(String s) {
		return reMonthDay.matcher(s).matches() || reMonthwordDay.matcher(s).matches() || reMonthabbrDay.matcher(s).matches();
	}

	protected List<Element> expandDateDMY(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reDayMonthYear.matcher(s);
		boolean found = reMatcher.find();
		int monthType = 1; // month == (0)1, (0)2, ... , 12
		if (!found) {
			reMatcher = reDayMonthwordYear.matcher(s);
			found = reMatcher.find();
			monthType = 2; // month == Januar, Februar, ...
		}
		if (!found) {
			reMatcher = reDayMonthabbrYear.matcher(s);
			found = reMatcher.find();
			monthType = 3; // month == Jan, Feb, ...
		}
		if (!found) {
			return null;
		}
		String day = reMatcher.group(1);
		exp.addAll(number.expandOrdinal(doc, day + ".", false));
		String month = reMatcher.group(2);
		if (monthType == 1) {
			exp.addAll(number.expandOrdinal(doc, month + ".", false));
		} else if (monthType == 2) {
			exp.addAll(makeNewTokens(doc, month));
		} else { // monthType == 3
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
		}
		String year = reMatcher.group(3);
		exp.addAll(expandDateYear(doc, year));
		return exp;
	}

	protected List<Element> expandDateYMD(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reYearMonthDay.matcher(s);
		boolean found = reMatcher.find();
		int monthType = 1; // month == (0)1, (0)2, ... , 12
		if (!found) {
			reMatcher = reYearMonthwordDay.matcher(s);
			found = reMatcher.find();
			monthType = 2; // month == Januar, Februar, ...
		}
		if (!found) {
			reMatcher = reYearMonthabbrDay.matcher(s);
			found = reMatcher.find();
			monthType = 3; // month == Jan, Feb, ...
		}
		if (!found) {
			return null;
		}
		String month = reMatcher.group(3);
		if (monthType == 1) {
			exp.addAll(number.expandOrdinal(doc, month + ".", false));
		} else if (monthType == 2) {
			exp.addAll(makeNewTokens(doc, month));
		} else { // monthType == 3
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
		}
		String day = reMatcher.group(4);
		exp.addAll(number.expandOrdinal(doc, day + ".", false));
		String year = reMatcher.group(1);
		exp.addAll(expandDateYear(doc, year));
		return exp;
	}

	protected List<Element> expandDateDM(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reDayMonth.matcher(s);
		boolean found = reMatcher.find();
		int monthType = 1; // month == (0)1, (0)2, ... , 12
		if (!found) {
			reMatcher = reDayMonthword.matcher(s);
			found = reMatcher.find();
			monthType = 2; // month == Januar, Februar, ...
		}
		if (!found) {
			reMatcher = reDayMonthabbr.matcher(s);
			found = reMatcher.find();
			monthType = 3; // month == Jan, Feb, ...
		}
		if (!found) {
			return null;
		}
		String day = reMatcher.group(1);
		exp.addAll(number.expandOrdinal(doc, day + ".", false));
		String month = reMatcher.group(2);
		if (monthType == 1) {
			exp.addAll(number.expandOrdinal(doc, month + ".", false));
		} else if (monthType == 2) {
			exp.addAll(makeNewTokens(doc, month));
		} else { // monthType == 3
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
		}
		return exp;
	}

	protected List<Element> expandDateMY(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reMonthYear.matcher(s);
		boolean found = reMatcher.find();
		int monthType = 1; // month == (0)1, (0)2, ... , 12
		if (!found) {
			reMatcher = reMonthwordYear.matcher(s);
			found = reMatcher.find();
			monthType = 2; // month == Januar, Februar, ...
		}
		if (!found) {
			reMatcher = reMonthabbrYear.matcher(s);
			found = reMatcher.find();
			monthType = 3; // month == Jan, Feb, ...
		}
		if (!found) {
			return null;
		}
		String month = reMatcher.group(1);
		if (monthType == 1) {
			if (month.charAt(0) == '0')
				month = month.substring(1);
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
			// alternatively: keep the number:
			// exp.addAll(number.expandInteger(doc, month, false));
		} else if (monthType == 2) {
			exp.addAll(makeNewTokens(doc, month));
		} else { // monthType == 3
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
		}
		String year = reMatcher.group(3);
		exp.addAll(expandDateYear(doc, year));
		return exp;
	}

	protected List<Element> expandDateYear(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		int value;
		try {
			value = Integer.decode(s).intValue();
		} catch (NumberFormatException e) {
			logger.info("Cannot convert string \"" + s + "\" to long.");
			throw e;
		}
		if (value >= 1100 && value <= 1999) {
			int hundreds = value / 100;
			int rest = value % 100;
			StringBuilder sb = new StringBuilder();
			sb.append(number.expandInteger(hundreds));
			sb.append(" Hundert");
			if (rest != 0) {
				sb.append(" ");
				sb.append(number.expandInteger(rest));
			}
			exp.addAll(makeNewTokens(doc, sb.toString(), true, s));
		} else if (value < 10 && s.charAt(0) == '0') { // 00, 01, 02, ...
			exp.addAll(makeNewTokens(doc, "null " + number.expandInteger(value), true, s));
		} else {
			exp.addAll(makeNewTokens(doc, number.expandInteger(value), true, s));
		}
		return exp;
	}

	protected List<Element> expandDateMonth(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reMonth.matcher(s);
		boolean found = reMatcher.find();
		int monthType = 1; // month == (0)1, (0)2, ... , 12
		if (!found) {
			reMatcher = reMonthword.matcher(s);
			found = reMatcher.find();
			monthType = 2; // month == Januar, Februar, ...
		}
		if (!found) {
			reMatcher = reMonthabbr.matcher(s);
			found = reMatcher.find();
			monthType = 3; // month == Jan, Feb, ...
		}
		if (!found) {
			return null;
		}
		String month = reMatcher.group(1);
		// Leading Zero must be deleted to get month from monthabbr.
		if (month.charAt(0) == '0')
			month = month.substring(1);
		if (monthType == 1 || monthType == 3) {
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
		} else if (monthType == 2) {
			exp.addAll(makeNewTokens(doc, month));
		}
		// StringBuffer sb
		return exp;
	}

	protected List<Element> expandDateDay(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reDay.matcher(s);
		boolean found = reMatcher.find();
		if (!found) {
			return null;
		}
		String day = reMatcher.group(1);
		// Leading Zero must be deleted to get month from monthabbr.
		if (day.charAt(0) == '0')
			day = day.substring(1);
		exp.addAll(number.expandOrdinal(doc, day + ".", false));
		return exp;
	}

	protected List<Element> expandDateMD(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reMonthDay.matcher(s);
		boolean found = reMatcher.find();
		int monthType = 1; // month == (0)1, (0)2, ... , 12
		if (!found) {
			reMatcher = reMonthwordDay.matcher(s);
			found = reMatcher.find();
			monthType = 2; // month == Januar, Februar, ...
		}
		if (!found) {
			reMatcher = reMonthabbrDay.matcher(s);
			found = reMatcher.find();
			monthType = 3; // month == Jan, Feb, ...
		}
		if (!found) {
			return null;
		}
		String day;
		if (monthType == 3)
			day = reMatcher.group(3);
		else
			day = reMatcher.group(2);
		exp.addAll(number.expandOrdinal(doc, day + ".", false));
		String month = reMatcher.group(1);
		if (monthType == 1) {
			if (month.charAt(0) == '0')
				month = month.substring(1);
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
			// alternatively: keep the number:
			// exp.addAll(number.expandInteger(doc, month, false));
		} else if (monthType == 2) {
			exp.addAll(makeNewTokens(doc, month));
		} else { // monthType == 3
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
		}
		return exp;
	}

	protected List<Element> expandDateMDY(Document doc, String s) {
		ArrayList<Element> exp = new ArrayList<Element>();
		Matcher reMatcher = reMonthDayYear.matcher(s);
		boolean found = reMatcher.find();
		int monthType = 1; // month == (0)1, (0)2, ... , 12
		if (!found) {
			reMatcher = reMonthwordDayYear.matcher(s);
			found = reMatcher.find();
			monthType = 2; // month == Januar, Februar, ...
		}
		if (!found) {
			reMatcher = reMonthabbrDayYear.matcher(s);
			found = reMatcher.find();
			monthType = 3; // month == Jan, Feb, ...
		}
		if (!found) {
			return null;
		}
		String day;
		String year;

		if (monthType == 3) {
			day = reMatcher.group(3);
			year = reMatcher.group(4);
		} else {
			day = reMatcher.group(2);
			year = reMatcher.group(3);
		}
		exp.addAll(number.expandOrdinal(doc, day + ".", false));
		String month = reMatcher.group(1);
		if (monthType == 1) {
			if (month.charAt(0) == '0')
				month = month.substring(1);
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
			// alternatively: keep the number:
			// exp.addAll(number.expandInteger(doc, month, false));
		} else if (monthType == 2) {
			exp.addAll(makeNewTokens(doc, month));
		} else { // monthType == 3
			String expandedMonth = (String) monthabbr.get(month);
			exp.addAll(makeNewTokens(doc, expandedMonth));
		}
		exp.addAll(expandDateYear(doc, year));
		return exp;
	}

}
