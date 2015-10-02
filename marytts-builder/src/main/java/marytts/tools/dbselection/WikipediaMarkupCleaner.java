/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.tools.dbselection;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * WikipediaMarkupCleaner
 * 
 * @author Marcela Charfuelan.
 */
public class WikipediaMarkupCleaner {

	// locale
	private String locale = null;
	// mySql database
	private String mysqlHost = null;
	private String mysqlDB = null;
	private String mysqlUser = null;
	private String mysqlPasswd = null;
	// Wikipedia files:
	private String xmlWikiFile = null;
	private String wikiLog = null;
	private boolean debug = false;
	private String debugPageId = null;
	// Default settings for max page length and min and max text length
	private int minPageLength = 10000; // minimum size of a wikipedia page, to be used in the first filtering of pages
	private int minTextLength = 1000;
	private int maxTextLength = 15000; // the average length in one big xml file is approx. 12000

	// Use this variable to save time not loading Wiki tables, if they already exist in the DB
	private boolean loadWikiTables = true;

	// Use this variable to do NOT create a new cleanText table, but adding to an already existing cleanText table.
	private boolean deleteCleanTextTable = true;

	public void setLocale(String str) {
		locale = str;
	}

	public void setMysqlHost(String str) {
		mysqlHost = str;
	}

	public void setMysqlDB(String str) {
		mysqlDB = str;
	}

	public void setMysqlUser(String str) {
		mysqlUser = str;
	}

	public void setMysqlPasswd(String str) {
		mysqlPasswd = str;
	}

	public void setXmlWikiFile(String str) {
		xmlWikiFile = str;
	}

	public void setWikiLog(String str) {
		wikiLog = str;
	}

	public void setTestId(String str) {
		debugPageId = str;
	}

	public void setMinPageLength(int val) {
		minPageLength = val;
	}

	public void setMinTextLength(int val) {
		minTextLength = val;
	}

	public void setMaxTextLength(int val) {
		maxTextLength = val;
	}

	public void setDebug(boolean bval) {
		debug = bval;
	}

	public void setLoadWikiTables(boolean bval) {
		loadWikiTables = bval;
	}

	public void setDeleteCleanTextTable(boolean bval) {
		deleteCleanTextTable = bval;
	}

	public String getLocale() {
		return locale;
	}

	public String getMysqlHost() {
		return mysqlHost;
	}

	public String getMysqlDB() {
		return mysqlDB;
	}

	public String getMysqlUser() {
		return mysqlUser;
	}

	public String getMysqlPasswd() {
		return mysqlPasswd;
	}

	public String getXmlWikiFile() {
		return xmlWikiFile;
	}

	public String getWikiLog() {
		return wikiLog;
	}

	public String getTestId() {
		return debugPageId;
	}

	public int getMinPageLength() {
		return minPageLength;
	}

	public int getMinTextLength() {
		return minTextLength;
	}

	public int getMaxTextLength() {
		return maxTextLength;
	}

	public boolean getDebug() {
		return debug;
	}

	public boolean getLoadWikiTables() {
		return loadWikiTables;
	}

	public boolean getDeleteCleanTextTable() {
		return deleteCleanTextTable;
	}

	public Vector<String> removeMarkup(String page) {
		StringBuffer str = new StringBuffer("");
		StringBuffer line = null;
		Vector<String> textList = new Vector<String>();

		boolean endOfText = false;
		Scanner s = null;
		try {
			s = new Scanner(page);
			while (s.hasNext() && !endOfText) {

				line = new StringBuffer(s.nextLine());
				// process text until it finds any of these labels:
				if (line.indexOf("==References") >= 0 || line.indexOf("== References") >= 0 || line.indexOf("==See also") >= 0
						|| line.indexOf("== See also") >= 0 || line.indexOf("==External links and sources") >= 0
						|| line.indexOf("==External links") >= 0 || line.indexOf("== External links") >= 0
						|| line.indexOf("== External Links") >= 0 || line.indexOf("== External links and sources") >= 0
						|| line.indexOf("==Notes") >= 0 || line.indexOf("== Notes") >= 0 || line.indexOf("==Sources") >= 0
						|| line.indexOf("== Sources") >= 0 || line.indexOf("==Foreign") >= 0 || line.indexOf("== Foreign") >= 0
						|| line.indexOf("==Discussion") >= 0) {
					endOfText = true;
				} else {
					// when removing sections it might add more lines that might contain again more labels to remove
					boolean clean = false;
					while (!clean && line.length() > 0) {
						clean = true;
						if (line.indexOf("<noinclude") >= 0) {
							line = removeSection(s, line, "<noinclude", "</noinclude>");
							clean = false;
						}

						if (line.indexOf("<includeonly") >= 0) {
							line = removeSection(s, line, "<includeonly", "</includeonly>");
							clean = false;
						}

						if (line.indexOf("<onlyinclude") >= 0) {
							line = removeSection(s, line, "<onlyinclude", "</onlyinclude>");
							clean = false;
						}

						if (line.indexOf("<table") >= 0) { // tables
							line = removeSection(s, line, "<table", "</table>");
							clean = false;
						}

						if (line.indexOf("<TABLE") >= 0) {
							line = removeSection(s, line, "<TABLE", "</TABLE>");
							clean = false;
						}

						if (line.indexOf("{{col-begin}}") >= 0) {
							line = removeSection(s, line, "{{col-begin}}", "{{col-end}}");
							clean = false;
						}

						if (line.indexOf("{|") >= 0) { // this is a table, this should go before {{ because a table can contain {{
														// }}
							line = removeSectionTable(s, line, "{|", "|}");
							clean = false;
						}

						if (line.indexOf("<ref") >= 0) { // references
							line = removeSectionRef(s, line); // This is special because it can be <ref>, <ref, </ref> or />
							clean = false;
						}

						if (line.indexOf("<REF") >= 0) {
							line = removeSection(s, line, "<REF", "</REF>");
							clean = false;
						}

						if (line.indexOf("<Ref") >= 0) {
							line = removeSection(s, line, "<Ref", "</Ref>");
							clean = false;
						}
						if (line.indexOf("<reF") >= 0) {
							line = removeSection(s, line, "<reF", "</reF>");
							clean = false;
						}

						if (line.indexOf("{{start box}}") >= 0) {
							line = removeSection(s, line, "{{start box}}", "{{end box}}");
							clean = false;
						}

						if (line.indexOf("{{") >= 0) {
							line = removeSection(s, line, "{{", "}}");
							clean = false;
						}

						if (line.indexOf("<!--") >= 0) {
							line = removeSection(s, line, "<!--", "-->");
							clean = false;
						}

						if (line.indexOf("\\mathrel{|") >= 0) {
							line = removeSection(s, line, "\\mathrel{|", "}");
							clean = false;
						}

						if (line.indexOf("<gallery") >= 0) { // gallery might contain several images
							line = removeSection(s, line, "<gallery", "</gallery>");
							clean = false;
						}

						if (line.indexOf("[[Image:") >= 0) {
							line = removeSectionImage(s, line, "[[Image:", "]]");
							clean = false;
						}

						if (line.indexOf("<div") >= 0) { // span and div tags are used to separate images from text
							line = removeSection(s, line, "<div", "</div>");
							clean = false;
						}

						if (line.indexOf("<DIV") >= 0) {
							line = removeSectionImage(s, line, "<DIV", "</DIV>");
							clean = false;
						}

						if (line.indexOf("<span") >= 0) {
							line = removeSection(s, line, "<span", "</span>");
							clean = false;
						}

						if (line.indexOf("<math>") >= 0) {
							line = removeSection(s, line, "<math>", "</math>");
							clean = false;
						}

						if (line.indexOf("<timeline>") >= 0) {
							line = removeSection(s, line, "<timeline>", "</timeline>");
							clean = false;
						}

						if (line.indexOf("<nowiki") >= 0) {
							line = removeSection(s, line, "<nowiki", "</nowiki>");
							clean = false;
						}

						if (line.indexOf("<source") >= 0) {
							line = removeSection(s, line, "<source", "</source>");
							clean = false;
						}

						if (line.indexOf("<code") >= 0) {
							line = removeSection(s, line, "<code", "</code>");
							clean = false;
						}

						if (line.indexOf("<imagemap") >= 0) {
							line = removeSection(s, line, "<imagemap", "</imagemap>");
							clean = false;
						}

						if (line.indexOf("<poem") >= 0) {
							line = removeSection(s, line, "<poem", "</poem>");
							clean = false;
						}

						if (line.indexOf("<h1") >= 0) {
							line = removeSection(s, line, "<h1", "</h1>");
							clean = false;
						}

						if (line.indexOf("<pre") >= 0) {
							line = removeSection(s, line, "<pre", "</pre>");
							clean = false;
						}

					} // while the line/text is not clean (or does not have tags to remove)

					// here filter bulleted and numbered short lines
					if (line.length() > 0) {
						if ((line.toString().startsWith("*") || line.toString().startsWith("#")
								|| line.toString().startsWith(";") || line.toString().startsWith(".")
								|| line.toString().startsWith(",") || line.toString().startsWith("&")
								|| line.toString().startsWith("}") || line.toString().startsWith("]")
								|| line.toString().startsWith("|") || line.toString().startsWith("ca:")
								|| line.toString().startsWith("cs:") || line.toString().startsWith("de:")
								|| line.toString().startsWith("es:") || line.toString().startsWith("fr:")
								|| line.toString().startsWith("it:") || line.toString().startsWith("hu:")
								|| line.toString().startsWith("ja:") || line.toString().startsWith("no:")
								|| line.toString().startsWith("pt:") || line.toString().startsWith("sl:")
								|| line.toString().startsWith("fi:") || line.toString().startsWith("sv:")
								|| line.toString().startsWith("tr:") || line.toString().startsWith("zh:")
								|| line.toString().startsWith("Category:") || line.toString().startsWith("!style=")
								|| line.toString().startsWith("!  style=") || line.toString().startsWith("!align=")
								|| line.toString().startsWith("::<code") || line.toString().endsWith("]]"))
								&& line.length() < 200)
							line = new StringBuffer("");
					}
					// Now if the line is not empty, remove:
					// '''''bold & italic'''''
					// '''bold'''
					// ''italic''
					// Internal links:
					// [[Name of page]]
					// [[Name of page|Text to display]]
					// External links:
					// [http://www.example.org Text to display]
					// [http://www.example.org]
					// http://www.example.org
					if (line.length() > 0) {

						line = new StringBuffer(line.toString().replaceAll("'''''", ""));
						line = new StringBuffer(line.toString().replaceAll("'''", ""));
						line = new StringBuffer(line.toString().replaceAll("''", ""));

						line = processInternalAndExternalLinks(line);

						// this will convert HTML &nbsp; &ndash; etc.
						String strlineNoHTML = StringEscapeUtils.unescapeHtml(line.toString());
						line = new StringBuffer(strlineNoHTML);

						// The previous does not remove all HTML stuff, so here it is done some manually
						line = new StringBuffer(line.toString().replaceAll("<big>", ""));
						line = new StringBuffer(line.toString().replaceAll("</big>", ""));
						line = new StringBuffer(line.toString().replaceAll("<blockquote>", ""));
						line = new StringBuffer(line.toString().replaceAll("</blockquote>", ""));
						line = new StringBuffer(line.toString().replaceAll("<BLOCKQUOTE>", ""));
						line = new StringBuffer(line.toString().replaceAll("</BLOCKQUOTE>", ""));
						line = new StringBuffer(line.toString().replaceAll("<sup>", ""));
						line = new StringBuffer(line.toString().replaceAll("</sup>", ""));
						line = new StringBuffer(line.toString().replaceAll("<sub>", ""));
						line = new StringBuffer(line.toString().replaceAll("</sub>", ""));
						line = new StringBuffer(line.toString().replaceAll("<small>", ""));
						line = new StringBuffer(line.toString().replaceAll("</small>", ""));
						line = new StringBuffer(line.toString().replaceAll("<ul>", ""));
						line = new StringBuffer(line.toString().replaceAll("</ul>", ""));
						line = new StringBuffer(line.toString().replaceAll("<UL>", ""));
						line = new StringBuffer(line.toString().replaceAll("</UL>", ""));
						line = new StringBuffer(line.toString().replaceAll("<br>", ""));
						line = new StringBuffer(line.toString().replaceAll("<br", ""));
						line = new StringBuffer(line.toString().replaceAll("<BR>", ""));
						line = new StringBuffer(line.toString().replaceAll("<br", ""));
						line = new StringBuffer(line.toString().replaceAll("<br/>", ""));
						line = new StringBuffer(line.toString().replaceAll("<Center>", ""));
						line = new StringBuffer(line.toString().replaceAll("<center>", ""));
						line = new StringBuffer(line.toString().replaceAll("</center>", ""));
						line = new StringBuffer(line.toString().replaceAll("<CENTER>", ""));
						line = new StringBuffer(line.toString().replaceAll("</CENTER>", ""));
						line = new StringBuffer(line.toString().replaceAll("<cite>", ""));
						line = new StringBuffer(line.toString().replaceAll("</cite>", ""));
						line = new StringBuffer(line.toString().replaceAll("<li>", ""));
						line = new StringBuffer(line.toString().replaceAll("</li>", ""));
						line = new StringBuffer(line.toString().replaceAll("<LI>", ""));
						line = new StringBuffer(line.toString().replaceAll("</LI>", ""));
						line = new StringBuffer(line.toString().replaceAll("<dl>", ""));
						line = new StringBuffer(line.toString().replaceAll("</dl>", ""));
						line = new StringBuffer(line.toString().replaceAll("<dt>", ""));
						line = new StringBuffer(line.toString().replaceAll("</dt>", ""));
						line = new StringBuffer(line.toString().replaceAll("<dd>", ""));
						line = new StringBuffer(line.toString().replaceAll("</dd>", ""));
						line = new StringBuffer(line.toString().replaceAll("<b>", ""));
						line = new StringBuffer(line.toString().replaceAll("</b>", ""));
						line = new StringBuffer(line.toString().replaceAll("<p>", ""));
						line = new StringBuffer(line.toString().replaceAll("</p>", ""));
						line = new StringBuffer(line.toString().replaceAll("<u>", ""));
						line = new StringBuffer(line.toString().replaceAll("</u>", ""));
						line = new StringBuffer(line.toString().replaceAll("<tt>", ""));
						line = new StringBuffer(line.toString().replaceAll("</tt>", ""));
						line = new StringBuffer(line.toString().replaceAll("<i>", ""));
						line = new StringBuffer(line.toString().replaceAll("</i>", ""));
						line = new StringBuffer(line.toString().replaceAll("<I>", ""));
						line = new StringBuffer(line.toString().replaceAll("</I>", ""));
						line = new StringBuffer(line.toString().replaceAll("<s>", ""));
						line = new StringBuffer(line.toString().replaceAll("</s>", ""));
						line = new StringBuffer(line.toString().replaceAll("<em>", ""));
						line = new StringBuffer(line.toString().replaceAll("</em>", ""));
						line = new StringBuffer(line.toString().replaceAll("</br>", ""));
						line = new StringBuffer(line.toString().replaceAll("</div>", ""));
						line = new StringBuffer(line.toString().replaceAll("</ref>", ""));
						line = new StringBuffer(line.toString().replaceAll("/>", ""));

						// Removing quotation marks
						line = new StringBuffer(line.toString().replaceAll("\"", ""));
						// these quotations have a strange/problematic symbol different from "
						line = new StringBuffer(line.toString().replaceAll("“", ""));
						line = new StringBuffer(line.toString().replaceAll("”", ""));
						// these symbol are also problematic, here they are changed.
						line = new StringBuffer(line.toString().replaceAll("’", "'"));
						line = new StringBuffer(line.toString().replaceAll("—", "-"));
						line = new StringBuffer(line.toString().replaceAll("–", "-"));

						line = new StringBuffer(line.toString().replaceAll(" ", " "));
						line = new StringBuffer(line.toString().replaceAll("…", " "));

						// finally sections and lists
						boolean is_title = false;
						if (line.toString().startsWith("==")) {
							is_title = true;
						}
						line = new StringBuffer(line.toString().replaceAll("\\s*==+$|==+", ""));
						if (is_title) {
							line.append(".");
						}

						// bulleted list and numbered list
						if (line.toString().startsWith("***") || line.toString().startsWith("*#*"))
							line.replace(0, 3, "");
						if (line.toString().startsWith("**") || line.toString().startsWith(":*")
								|| line.toString().startsWith("*#") || line.toString().startsWith("##")
								|| line.toString().startsWith("::"))
							line.replace(0, 2, "");
						if (line.toString().startsWith("*") || line.toString().startsWith("#"))
							line.replace(0, 1, "");
						if (line.toString().startsWith(";") || line.toString().startsWith(";")) // in glossaries definitions start
																								// with ;
							line.replace(0, 1, "");

						// remove this when the text is almost clean
						if (line.indexOf("<font") >= 0)
							line = removeSection(s, line, "<font", ">");
						line = new StringBuffer(line.toString().replaceAll("</font>", ""));

						if (line.indexOf("<blockquote") >= 0)
							line = removeSection(s, line, "<blockquote", ">");

						if (line.indexOf("<ol") >= 0)
							line = removeSection(s, line, "<ol", ">");

						if (line.indexOf("<http:") >= 0)
							line = removeSection(s, line, "<http:", ">");

						// finally concatenate the line
						str.append(line);
						if (!str.toString().endsWith("\n"))
							str.append("\n");

						line = null;

						// check length of the text
						if (str.length() > maxTextLength) {
							textList.add(str.toString());
							// System.out.println("\n-----------\n" + str.toString());
							str = new StringBuffer("");
						}

					}

				} // endOfText=false

			} // while has more lines

		} finally {
			if (s != null)
				s.close();
		}

		if (!str.toString().contentEquals(""))
			textList.add(str.toString());
		return textList;
	}

	// This is special because it can be:
	// <ref> ... </ref>
	// <ref ... </ref>
	// <ref ... />
	private StringBuffer removeSectionRef(Scanner s, StringBuffer lineIn) {
		String next;
		int index1 = 0, index2 = -1, index3 = -1, endTagLength = 0, numRef = 0;
		boolean closeRef = true;
		StringBuffer line = new StringBuffer(lineIn);
		StringBuffer nextLine;

		while ((index1 = line.indexOf("<ref")) >= 0) { // in one line can be more than one reference
			numRef++;
			if ((index2 = line.indexOf("</ref>", index1)) >= 0)
				endTagLength = 6 + index2;
			else if ((index3 = line.indexOf("/>", index1)) >= 0)
				endTagLength = 2 + index3;

			if (index2 == -1 && index3 == -1) {// the </ref> most be in the next lines, so get more lines until the </ref> is
												// found
				while (s.hasNext() && numRef != 0) {
					nextLine = new StringBuffer(s.nextLine());
					if (nextLine.indexOf("<ref") >= 0)
						numRef++;
					line.append(nextLine);
					if ((index2 = line.indexOf("</ref>", index1)) >= 0) {
						numRef--;
						endTagLength = 6 + index2;
					} else if ((index3 = line.indexOf("/>", index1)) >= 0) {
						numRef--;
						endTagLength = 2 + index3;
					}
				}

			} else
				// the endTag was found
				numRef--;

			if (numRef == 0) {
				index1 = line.indexOf("<ref"); // get again this because the position might change
				if (endTagLength > index1) {
					line.delete(index1, endTagLength);
					// System.out.println("nextline="+line);
				} else {
					if (debug) {
						System.out.print("iniTag: <ref  index1=" + index1);
						System.out.print("  endTagLength=" + endTagLength);
						System.out.println("  line.length=" + line.length() + "  line: " + line);
						System.out.println("removeSectionRef: WARNING endTagLength > length of line: " + line);
						// line.delete(index1, line.length());
					}
					line = new StringBuffer("");
				}
			} else {
				if (debug)
					System.out.println("removeSectionRef: WARNING no </ref> or /> in " + line);
				// line.delete(index1, line.length());
				line = new StringBuffer("");
			}

		} // while this line contains iniTag-s

		return line;

	}

	private StringBuffer removeSection(Scanner s, StringBuffer lineIn, String iniTag, String endTag) {
		String next;
		int index1 = 0, index2 = -1, endTagLength = 0, numRef = 0, lastEndTag = 0, lastIniTag = 0;
		boolean closeRef = true;
		StringBuffer line = new StringBuffer(lineIn);
		StringBuffer nextLine;

		if (debug)
			System.out.println("Removing tag: " + iniTag + "  LINE (BEFORE): " + line);

		while ((index1 = line.indexOf(iniTag)) >= 0) { // in one line can be more than one iniTag

			numRef++;
			if ((index2 = line.indexOf(endTag, index1)) >= 0)
				endTagLength = endTag.length() + index2;

			if (index2 == -1) {// the iniTag most be in the next lines, so get more lines until the endTag is found
				lastEndTag = 0; // start to look for the endTag in 0

				while (s.hasNext() && numRef != 0) {
					lastIniTag = 0;
					nextLine = new StringBuffer(s.nextLine());
					// if(debug)
					// System.out.println("  NEXTLINE: " + nextLine);

					while ((index1 = nextLine.indexOf(iniTag, lastIniTag)) >= 0) {
						numRef++;
						lastIniTag = iniTag.length() + index1;
					}

					line.append(nextLine);

					// next time it will look for the endTag after the position of the last it found.
					while ((index2 = line.indexOf(endTag, lastEndTag)) >= 0) {
						numRef--;
						lastEndTag = index2 + endTag.length(); // I need to remember where the last endTag was found
						endTagLength = endTag.length() + index2;
					}

					// if(debug)
					// System.out.println("LINE (numRef=" + numRef + "): " + line);
				}
			} else
				// the endTag was found
				numRef--;

			if (numRef == 0) {
				index1 = line.indexOf(iniTag); // get again this because the position might change
				if (endTagLength > index1) {
					if (debug) {
						System.out.println("    FINAL LINE: " + line);
						System.out.print("iniTag: " + iniTag + "  index1=" + index1);
						System.out.print("  endTagLength=" + endTagLength);
						System.out.println("  line.length=" + line.length() + "  line: " + line);
						System.out.println("  line.length=" + line.length());
					}
					line.delete(index1, endTagLength);
				} else {
					if (debug) {
						System.out.println("removeSection: WARNING endTagLength > length of line: ");
						System.out.print("iniTag: " + iniTag + "  index1=" + index1);
						System.out.print("  endTagLength=" + endTagLength);
						System.out.println("  line.length=" + line.length() + "  line: " + line);
						System.out.println("removeSection: WARNING endTagLength > length of line: " + line);
					}
					line = new StringBuffer("");
				}

				// System.out.println("nextline="+line);
			} else {
				if (debug)
					System.out.println("removeSection: WARNING no " + endTag);
				line = new StringBuffer("");
			}

		} // while this line contains iniTag-s

		if (debug)
			System.out.println("    LINE (AFTER): " + line);
		return line;
	}

	private StringBuffer removeSectionTable(Scanner s, StringBuffer lineIn, String iniTag, String endTag) {
		String next;
		int index1 = 0, index2 = -1, endTagLength = 0, numRef = 0, lastEndTag = 0, lastIniTag = 0;
		boolean closeRef = true;
		StringBuffer line = new StringBuffer(lineIn);
		StringBuffer nextLine;

		if (debug)
			System.out.println("Removing tag: " + iniTag + "  LINE (BEFORE): " + line);

		while ((index1 = line.indexOf(iniTag)) >= 0) { // in one line can be more than one iniTag

			numRef++;
			if ((index2 = line.indexOf(endTag, index1)) >= 0)
				endTagLength = endTag.length() + index2;

			if (index2 == -1) {// the iniTag most be in the next lines, so get more lines until the endTag is found
				lastEndTag = 0; // start to look for the endTag in 0

				while (s.hasNext() && numRef != 0) {
					lastIniTag = 0;
					nextLine = new StringBuffer(s.nextLine());
					// if(debug)
					// System.out.println("  NEXTLINE: " + nextLine);

					while ((index1 = nextLine.indexOf(iniTag, lastIniTag)) >= 0) {
						numRef++;
						lastIniTag = iniTag.length() + index1;
					}
					// next time it will look for the endTag after the position of the last it found.
					// while( (index2 = line.indexOf(endTag, lastEndTag)) >= 0 ){
					if (nextLine.toString().startsWith(endTag)) {
						numRef--;
						// index2 = line.length();
						// lastEndTag = index2 + endTag.length(); // I need to remember where the last endTag was found
						endTagLength = line.length() + endTag.length();
					}

					line.append(nextLine);

					// if(debug)
					// System.out.println("LINE (numRef=" + numRef + "): " + line);
				}
			} else
				// the endTag was found
				numRef--;

			if (numRef == 0) {
				index1 = line.indexOf(iniTag); // get again this because the position might change
				if (endTagLength > index1) {
					if (debug) {
						System.out.println("    FINAL LINE: " + line);
						System.out.print("iniTag: " + iniTag + "  index1=" + index1);
						System.out.print("  endTagLength=" + endTagLength);
						System.out.println("  line.length=" + line.length() + "  line: " + line);
						System.out.println("  line.length=" + line.length());
					}
					line.delete(index1, endTagLength);
				} else {
					if (debug) {
						System.out.println("removeSection: WARNING endTagLength > length of line: ");
						System.out.print("iniTag: " + iniTag + "  index1=" + index1);
						System.out.print("  endTagLength=" + endTagLength);
						System.out.println("  line.length=" + line.length() + "  line: " + line);
						System.out.println("removeSection: WARNING endTagLength > length of line: " + line);
					}
					line = new StringBuffer("");
				}

				// System.out.println("nextline="+line);
			} else {
				if (debug)
					System.out.println("removeSection: WARNING no " + endTag);
				line = new StringBuffer("");
			}

		} // while this line contains iniTag-s

		if (debug)
			System.out.println("    LINE (AFTER): " + line);
		return line;
	}

	/****
	 * This is also special because the line might contain sections with [[ ... ]] so the ]] after a [[ is not the endTag of
	 * [[image: ... ]]
	 * 
	 * @param s
	 *            s
	 * @param lineIn
	 *            lineIn
	 * @param iniTag
	 *            iniTag
	 * @param endTag
	 *            endTag
	 * @return line
	 */
	private StringBuffer removeSectionImage(Scanner s, StringBuffer lineIn, String iniTag, String endTag) {
		String next;
		int index1 = 0, index2 = -1, index3 = -1, endTagLength = 0, numRef = 0, lastEndTag1 = 0, lastIniTag = 0;
		boolean closeRef = true;
		StringBuffer line = new StringBuffer(lineIn);
		StringBuffer nextLine;
		StringBuffer aux;

		if (debug)
			System.out.println("Removing tag: " + iniTag + "  LINE (BEFORE): " + line);

		while ((index1 = line.indexOf(iniTag)) >= 0) { // in one line can be more than one iniTag

			numRef++;
			index3 = endTagLength = index1;

			while (s.hasNext() && numRef > 0) {

				while ((index2 = line.indexOf("]]", endTagLength)) >= 0 && numRef > 0) {
					aux = new StringBuffer(line.subSequence(index1 + 2, index2 + 2));
					if (debug)
						System.out.println("    aux=" + aux);
					if ((index3 = aux.indexOf("[[")) == -1) {
						endTagLength = endTag.length() + index2;
						numRef--;
					} else { // The previous was a [[ ]] inside of a [[Image: so it has to be deleted
						index1 = index2;
						endTagLength = index2 + 2;
						index2 = -1;
					}
				}
				// so far it has not found the endTag, so get another line
				if (numRef > 0)
					line.append(s.nextLine());
			}

			if (numRef == 0) {
				index1 = line.indexOf(iniTag); // get again this because the position might change
				if (endTagLength > index1) {
					if (debug) {
						System.out.println("    FINAL LINE: " + line);
						System.out.print("iniTag: " + iniTag + "  index1=" + index1);
						System.out.print("  endTagLength=" + endTagLength);
						System.out.println("  line.length=" + line.length() + "  line: " + line);
						System.out.println("  line.length=" + line.length());
					}
					line.delete(index1, endTagLength);
				} else {
					if (debug) {
						System.out.println("removeSection: WARNING endTagLength > length of line: ");
						System.out.print("iniTag: " + iniTag + "  index1=" + index1);
						System.out.print("  endTagLength=" + endTagLength);
						System.out.println("  line.length=" + line.length() + "  line: " + line);
						System.out.println("removeSection: WARNING endTagLength > length of line: " + line);
					}
					line = new StringBuffer("");
				}

			} else {
				if (debug)
					System.out.println("removeSection: WARNING no " + endTag);
				line = new StringBuffer("");
			}

		} // while this line contains iniTag-s

		if (debug)
			System.out.println("    LINE (AFTER): " + line);
		return line;
	}

	/***
	 * Internal links: [[Name of page]] [[Name of page|Text to display]] External links: [http://www.example.org Text to display]
	 * [http://www.example.org] http://www.example.org
	 * 
	 * @param line
	 *            line
	 */
	private StringBuffer processInternalAndExternalLinks(StringBuffer line) {
		int index1, index2, index3;
		StringBuffer linetmp = null; // for debugging
		boolean changed = false;
		if (debug)
			linetmp = new StringBuffer(line);

		// Internal links:
		while ((index1 = line.indexOf("[[")) >= 0) {
			changed = true;
			if ((index2 = line.indexOf("]]")) >= 0) {
				if ((index3 = line.indexOf("|", index1)) >= 0 && index3 < index2) { // if there is text to display
					line.delete(index1, index3 + 1); // delete the link and [[ ]]
					index2 = line.indexOf("]]"); // since i delete some text i need to find again the next ]]
					line.delete(index2, index2 + 2);
				} else {
					line.delete(index1, index1 + 2); // delete the [[
					index2 = line.indexOf("]]"); // since i delete some text i need to find again the next ]]
					line.delete(index2, index2 + 2); // delete the ]] -2 because in the previous line i deleted two chars
				}
				// if(debug)
				// System.out.println("LINE (AFTER): " + line);

			} else {
				if (debug) {
					System.out.println("processInternalAndExternalLinks: WARNING no ]] tag in " + line);
					System.out.println("deleting [[");
				}
				line.delete(index1, index1 + 2); // delete the [[
			}
		}

		// External links: just the ones started with [http: and here I am deleting the whole reference
		// i am not keeping the text to display of this link.
		while ((index1 = line.indexOf("[http:")) >= 0 || (index1 = line.indexOf("[https:")) >= 0) {
			// System.out.println("LINE(BEFORE): " + line);
			if ((index2 = line.indexOf("]", index1)) >= 0) {
				// line.delete(index1, index2+1);
				if ((index3 = line.indexOf(" ", index1)) >= 0 && index3 < index2) { // if there is text to display
					line.delete(index1, index3 + 1); // delete the link and [http: until first black space before ]
					index2 = line.indexOf("]"); // since i delete some text i need to find again the next ]]
					line.delete(index2, index2 + 1);
				} else {
					line.delete(index1, index2 + 1); // no text to display, delete the whole ref
				}

				// System.out.println("LINE (AFTER): " + line + "\n");

			} else {
				if (debug) {
					System.out.println("processInternalAndExternalLinks: WARNING no ] tag when processing lines with http: line="
							+ line);
					System.out.println("deleting [");
				}
				line.delete(index1, index1 + 1); // delete the [
			}
		}

		if (debug && changed) {
			System.out.println("Removing links, LINE(BEFORE): " + linetmp);
			System.out.println("                LINE (AFTER): " + line);
		}

		return line;

	}

	public void addWordToHashMap(String text, HashMap<String, Integer> wordList) {
		String sentences[];
		String words[], w;
		Integer i;
		int m, n;

		sentences = text.split("\n");
		for (m = 0; m < sentences.length; m++) {
			// System.out.println("\n" + sentences[m]);
			words = sentences[m].split(" ");
			for (n = 0; n < words.length; n++) {
				w = words[n];
				// System.out.print("word=" + words[n] + "   -->");
				// Split into letter sections that we will consider atomic "words":
				int start = 0, end = 0;
				int minimumLength = 2;
				for (; end < w.length(); end++) {
					// if (Character.isLetter(w.charAt(end))) {
					if (marytts.util.string.StringUtils.isLetterOrModifier(w.codePointAt(end))) {
						if (start < 0)
							start = end;
						continue;
					}
					// not a letter
					if (start >= 0 && end - start >= minimumLength) {
						String oneWord = w.substring(start, end);
						// System.out.print(" oneWord1=" + oneWord);
						Integer count = (Integer) wordList.get(oneWord);
						// if key is not in the map then give it value one
						// otherwise increment its value by 1
						if (count == null)
							wordList.put(oneWord, new Integer(1));
						else
							wordList.put(oneWord, new Integer(count.intValue() + 1));
					}
					start = -1;
				}
				if (start >= 0 && end - start >= minimumLength) {
					String oneWord = w.substring(start, end);
					// System.out.print(" oneWord2=" + oneWord);
					Integer count = (Integer) wordList.get(oneWord);
					// if key is not in the map then give it value one
					// otherwise increment its value by 1
					if (count == null)
						wordList.put(oneWord, new Integer(1));
					else
						wordList.put(oneWord, new Integer(count.intValue() + 1));
				}
				/*
				 * // remove punctuation if( w.endsWith(",") || w.endsWith(";") || w.endsWith(".") || w.endsWith(":") ||
				 * w.endsWith("'") || w.endsWith(")") || w.endsWith("?") ) w = w.substring(0, (w.length()-1)); if(
				 * w.endsWith("'s") ) w = w.substring(0, (w.length()-2)); if(w.startsWith("(") ) w = w.substring(1, w.length());
				 * 
				 * if( w.length()>1 && StringUtils.isAlpha(w) && StringUtils.isNotBlank(w) && StringUtils.isNotEmpty(w) &&
				 * StringUtils.isAsciiPrintable(w)) { //System.out.print(w + " "); i = (Integer) wordList.get(w); // if key is not
				 * in the map then give it value one // otherwise increment its value by 1 if(i==null) wordList.put(w, new
				 * Integer(1)); else wordList.put(w, new Integer( i.intValue() + 1)); } // if word is > 1 and isAlpha
				 */
				// System.out.println("\n");
			}
			// System.out.println("\n");
			words = null;
		}
		sentences = null;
	}

	public void updateWordList(DBHandler wikiToDB, HashMap<String, Integer> wlNew) {
		String w;
		HashMap<String, Integer> wlOld;
		Integer freq;
		Integer i;

		// Checking if word list exist
		if (wikiToDB.tableExist(locale + "_wordList")) {
			System.out.println("Updating " + locale + "_wordList in DB table....");
			wlOld = wikiToDB.getMostFrequentWords(0, 0);

			// combine the two tables
			Iterator iterator = wlNew.keySet().iterator();
			while (iterator.hasNext()) {
				w = iterator.next().toString();
				freq = wlNew.get(w);

				i = (Integer) wlOld.get(w);
				// if key is not in the map then give it value freq
				// otherwise increment its value by freq
				if (i == null)
					wlOld.put(w, new Integer(freq));
				else
					wlOld.put(w, new Integer(i.intValue() + freq));
			}
			wikiToDB.insertWordList(wlOld);
			System.out.println("Final size of wordList after combining old and new lists: wordList=[" + wlOld.size() + "]");

		} else {
			System.out.println("Saving " + locale + "_wordList table....");
			wikiToDB.insertWordList(wlNew);
		}

	}

	void processWikipediaSQLTablesDebug() throws Exception {

		DBHandler wikiToDB = new DBHandler(locale);

		wikiToDB.createDBConnection(mysqlHost, mysqlDB, mysqlUser, mysqlPasswd);
		String text;
		StringBuilder textId = new StringBuilder();
		int numPagesUsed = 0;

		PrintWriter pw = null;
		if (wikiLog != null)
			pw = new PrintWriter(new FileWriter(new File(wikiLog)));

		// get text from the DB
		text = wikiToDB.getTextFromWikiPage(debugPageId, minPageLength, textId, pw);
		System.out.println("\nPAGE SIZE=" + text.length() + "  text:\n" + text);

		Vector<String> textList;

		if (text != null) {
			textList = removeMarkup(text);
			System.out.println("\nCLEANED TEXT:");
			for (int i = 0; i < textList.size(); i++)
				System.out.println("text(" + i + "): \n" + textList.get(i));

		} else
			System.out.println("NO CLEANED TEXT.");

		if (pw != null)
			pw.close();

		wikiToDB.closeDBConnection();

	}

	/***
	 * Using mwdumper extracts pages from a xmlWikiFile and load them in a mysql DB (it loads the tables "locale_text",
	 * "locale_page" and "locale_revision", where locale is the corresponding wikipedia language). Once the tables are loaded,
	 * extract/clean text from the pages and create a cleanText table. It also creates a wordList table including frequencies.
	 * 
	 * @throws Exception
	 *             Exception
	 */
	void processWikipediaPages() throws Exception {
		// Load wikipedia pages, extract clean text and create word list.
		String dateStringIni = "", dateStringEnd = "";
		DateFormat fullDate = new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss");
		Date dateIni = new Date();
		dateStringIni = fullDate.format(dateIni);

		DBHandler wikiToDB = new DBHandler(locale);

		// hashMap for the dictionary, HashMap is faster than TreeMap so the list of words will
		// be kept it in a hashMap. When the process finish the hashMap will be dump in the database.
		HashMap<String, Integer> wordList;

		System.out.println("Creating connection to DB server...");
		wikiToDB.createDBConnection(mysqlHost, mysqlDB, mysqlUser, mysqlPasswd);

		// This loading can take a while
		// create and load TABLES: page, text and revision

		if (loadWikiTables) {
			System.out.println("Creating and loading TABLES: page, text and revision. (The loading can take a while...)");
			wikiToDB.loadPagesWithMWDumper(xmlWikiFile, locale, mysqlHost, mysqlDB, mysqlUser, mysqlPasswd);
		} else {
			// Checking if tables are already created and loaded in the DB
			if (wikiToDB.checkWikipediaTables())
				System.out.println("TABLES " + locale + "_page, " + locale + "_text and " + locale
						+ "_revision already loaded (WARNING USING EXISTING WIKIPEDIA TABLES).");
			else
				throw new Exception("WikipediaMarkupCleaner: ERROR IN TABLES " + locale + "_page, " + locale + "_text and "
						+ locale + "_revision, they are not CREATED/LOADED.");
		}

		System.out.println("\nGetting page IDs");
		String pageId[];
		pageId = wikiToDB.getIds("page_id", locale + "_page");
		System.out.println("Number of page IDs to process: " + pageId.length + "\n");

		// create cleanText TABLE
		if (deleteCleanTextTable) {
			System.out.println("Creating (deleting if already exist) " + locale + "_cleanText TABLE");
			wikiToDB.createWikipediaCleanTextTable();
		} else {
			if (wikiToDB.tableExist(locale + "_cleanText"))
				System.out.println(locale + "_cleanText TABLE already exist (ADDING TO EXISTING cleanText TABLE)");
			else {
				System.out.println("Creating " + locale + "_cleanText TABLE");
				wikiToDB.createWikipediaCleanTextTable();
			}
		}

		System.out.println("Starting Hashtable for wordList.");
		int initialCapacity = 200000;
		wordList = new HashMap<String, Integer>(initialCapacity);

		String text;
		PrintWriter pw = null;
		if (wikiLog != null)
			pw = new PrintWriter(new FileWriter(new File(wikiLog)));

		StringBuilder textId = new StringBuilder();
		int numPagesUsed = 0;

		Vector<String> textList;
		System.out.println("\nStart processing Wikipedia pages.... Start time:" + dateStringIni + "\n");

		for (int i = 0; i < pageId.length; i++) {

			// first filter
			text = wikiToDB.getTextFromWikiPage(pageId[i], minPageLength, textId, pw);

			if (text != null) {
				textList = removeMarkup(text);
				numPagesUsed++;
				for (int j = 0; j < textList.size(); j++) {
					text = textList.get(j);
					if (text.length() > minTextLength) {
						// if after cleaning the text is not empty or
						wikiToDB.insertCleanText(text, pageId[i], textId.toString());
						// insert the words in text in wordlist
						addWordToHashMap(text, wordList);
						if (debug)
							System.out.println("Cleanedpage_id[" + i + "]=" + pageId[i] + "  textList (" + (j + 1) + "/"
									+ textList.size() + ") length=" + text.length() + "  numPagesUsed=" + numPagesUsed
									+ "  Wordlist[" + wordList.size() + "] ");

						if (pw != null)
							pw.println("CLEANED PAGE page_id[" + i + "]=" + pageId[i] + " textList (" + (j + 1) + "/"
									+ textList.size() + ") length=" + text.length() + " Wordlist[" + wordList.size() + "] "
									+ "  NUM_PAGES_USED=" + numPagesUsed + " text:\n\n" + text);
					} else if (pw != null)
						pw.println("PAGE NOT USED AFTER CLEANING page_id[" + i + "]=" + pageId[i] + " length=" + text.length());
				} // for each text in textList
				System.out.println("Cleanedpage_id[" + i + "]=" + pageId[i] + "  numPagesUsed=" + numPagesUsed + "  Wordlist["
						+ wordList.size() + "] ");
				textList.clear(); // clear the list of text
			}
		}
		Date dateEnd = new Date();
		dateStringEnd = fullDate.format(dateEnd);

		if (pw != null) {
			pw.println("Number of PAGES USED=" + numPagesUsed + " Wordlist[" + wordList.size() + "] " + " minPageLength="
					+ minPageLength + " minTextLength=" + minTextLength + " Start time:" + dateStringIni + "  End time:"
					+ dateStringEnd);
			pw.close();
		}

		// save the wordList in the DB
		updateWordList(wikiToDB, wordList);

		wikiToDB.printWordList("./wordlist-freq.txt", "frequency", 0, 0);

		System.out.println("\nNumber of pages used=" + numPagesUsed + " Wordlist[" + wordList.size() + "] " + " Start time:"
				+ dateStringIni + "  End time:" + dateStringEnd);

		// Once created the cleantext table delete the wikipedia text, page and revision tables.
		wikiToDB.deleteWikipediaTables();

		wikiToDB.closeDBConnection();

	}

	private void printParameters() {
		System.out.println("WikipediaMarkupCleaner parameters:" + "\n  -mysqlHost " + getMysqlHost() + "\n  -mysqlUser "
				+ getMysqlUser() + "\n  -mysqlPasswd " + getMysqlPasswd() + "\n  -mysqlDB " + getMysqlDB() + "\n  -xmlFile "
				+ getXmlWikiFile() + "\n  -minPage " + getMinPageLength() + "\n  -minText " + getMinTextLength()
				+ "\n  -maxText " + getMaxTextLength() + "\n  -log " + getWikiLog() + "\n  -debugPageId " + getTestId());

		if (getDebug())
			System.out.println("  -debug true");
		else
			System.out.println("  -debug false");
		if (getLoadWikiTables())
			System.out.println("  -loadWikiTables true");
		else
			System.out.println("  -loadWikiTables false");
		if (getDeleteCleanTextTable())
			System.out.println("  -deleteCleanTextTable true\n");
		else
			System.out.println("  -deleteCleanTextTable false\n");
	}

	//
	/**
	 * Read and parse the command line args
	 * 
	 * @param args
	 *            the args
	 * @return true, if successful, false otherwise
	 */
	private boolean readArgs(String[] args) {

		String help = "\nUsage: java WikipediaMarkupCleaner -locale language -mysqlHost host -mysqlUser user  \n"
				+ "                       -mysqlPasswd passwd -mysqlDB wikiDB -xmlFile xmlWikiFile \n"
				+ "      default/optional: [-minPage 10000 -minText 1000 -maxText 15000] \n"
				+ "      optional: [-log wikiLogFile -id pageId -debug]\n\n"
				+ "      -minPage is the minimum size of a wikipedia page that will be considered for cleaning.\n"
				+ "      -minText is the minimum size of a text to be kept in the DB.\n"
				+ "      -maxText is used to split big articles in small chunks, this is the maximum chunk size. \n"
				+ "      -log the wikiLogFile will contain the cleaned text and information about the pages used.\n"
				+ "      -debug will produce more output and it is mainly used to debug a particular Wikipedia page.\n"
				+ "      -debugPageId is the page_id number in a wikipedia page table (ex. 18702442), when used this option\n"
				+ "           the tables will not be loaded, so it is asumed that page, text and revision tables are already loaded.\n"
				+ "      -noLoadWikiTables use this variable to save time NOT loading wiki tables, they must already exist in the the DB.\n"
				+ "      -noDeleteCleanTextTable use this variable to do NOT create a new cleanText table, but adding to an already existing\n"
				+ "       cleanText table.\n";

		if (args.length >= 12) { // minimum 12 parameters
			for (int i = 0; i < args.length; i++) {
				if (args[i].contentEquals("-locale") && args.length >= (i + 1))
					setLocale(args[++i]);

				else if (args[i].contentEquals("-mysqlHost") && args.length >= (i + 1))
					setMysqlHost(args[++i]);

				else if (args[i].contentEquals("-mysqlUser") && args.length >= (i + 1))
					setMysqlUser(args[++i]);

				else if (args[i].contentEquals("-mysqlPasswd") && args.length >= (i + 1))
					setMysqlPasswd(args[++i]);

				else if (args[i].contentEquals("-mysqlDB") && args.length >= (i + 1))
					setMysqlDB(args[++i]);

				else if (args[i].contentEquals("-xmlFile") && args.length >= (i + 1))
					setXmlWikiFile(args[++i]);

				// From here the arguments are optional
				else if (args[i].contentEquals("-minPage") && args.length >= (i + 1))
					setMinPageLength(Integer.parseInt(args[++i]));

				else if (args[i].contentEquals("-minText") && args.length >= (i + 1))
					setMinTextLength(Integer.parseInt(args[++i]));

				else if (args[i].contentEquals("-maxText") && args.length >= (i + 1))
					setMaxTextLength(Integer.parseInt(args[++i]));

				else if (args[i].contentEquals("-log") && args.length >= (i + 1))
					setWikiLog(args[++i]);

				else if (args[i].contentEquals("-debugPageId") && args.length >= (i + 1))
					setTestId(args[++i]);

				else if (args[i].contentEquals("-debug"))
					setDebug(true);

				// Use this variable to save time NOT loading wiki tables, they must already exist in the DB
				else if (args[i].contentEquals("-noLoadWikiTables"))
					setLoadWikiTables(false);

				// Use this variable to do not create a new cleanText table, but adding to an already existing cleanText table.
				else if (args[i].contentEquals("-noDeleteCleanTextTable"))
					setDeleteCleanTextTable(false);

				else { // unknown argument
					System.out.println("\nOption not known: " + args[i]);
					System.out.println(help);
					return false;
				}

			}
		} else { // num arguments less than 16
			System.out.println(help);
			return false;
		}

		if (getLocale() == null) {
			System.out.println("\nMissing locale.");
			printParameters();
			System.out.println(help);
			return false;
		}

		if (getMysqlHost() == null || getMysqlUser() == null || getMysqlPasswd() == null || getMysqlDB() == null) {
			System.out.println("\nMissing required mysql parameters (one/several required variables are null).");
			printParameters();
			System.out.println(help);
			return false;
		}

		if (getXmlWikiFile() == null) {
			System.out.println("\nMissing required parameter, the XML wikipedia file\n");
			printParameters();
			System.out.println(help);
			return false;
		}

		return true;
	}

	public static void main(String[] args) throws Exception {

		WikipediaMarkupCleaner wikiCleaner = new WikipediaMarkupCleaner();

		/* check the arguments */
		if (!wikiCleaner.readArgs(args))
			return;

		wikiCleaner.printParameters();

		if (wikiCleaner.getTestId() != null)
			wikiCleaner.processWikipediaSQLTablesDebug();
		else
			wikiCleaner.processWikipediaPages();

	}

}
