/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.util.data.text;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;

import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * Reader for basename classification definition file, which allows mapping of basenames to classification strings in a lazy way,
 * using glob expressions.
 * <p>
 * Definition file format:
 * <ol>
 * <li>lines with leading <tt>#</tt> are comments and are ignored
 * <li>empty lines are ignored
 * <li>every other line must conform to the format
 * 
 * <pre>
 * GLOB = class
 * </pre>
 * 
 * where <tt>GLOB</tt> is a glob expression and <tt>class</tt> is a classification string. Within the glob, the following
 * characters have special meaning:
 * <dl>
 * <dt>*</dt>
 * <dd>zero or more characters</dd>
 * <dt>?</dt>
 * <dd>any one character</dd>
 * </dl>
 * </ol>
 * The idea is that a line like
 * 
 * <pre>
 *   foo_* = bar
 * </pre>
 * 
 * in the classification definition file will cause all basenames starting with <tt>foo_</tt> to be handled as belonging to the
 * "bar" class. One scenario where this is useful is the classification of prompts by speaking style in a multi-style voice
 * database.
 * 
 * @author steiner
 * 
 */
public class BasenameClassificationDefinitionFileReader {
	protected BufferedReader reader;

	public boolean fileOK = true;

	protected LinkedHashMap<Pattern, String> styleDefinitions = new LinkedHashMap<Pattern, String>();

	/**
	 * constructor to call main constructor with a filename String
	 * 
	 * @param filename
	 *            as a String
	 * @throws IOException
	 *             IOException
	 */
	public BasenameClassificationDefinitionFileReader(String filename) throws IOException {
		this(new FileReader(filename));
	}

	/**
	 * main constructor
	 * 
	 * @param reader
	 *            as a Reader
	 * @throws IOException
	 *             IOException
	 */
	public BasenameClassificationDefinitionFileReader(Reader reader) throws IOException {
		this.reader = new BufferedReader(reader);
		parseDefinitionFile();
	}

	/**
	 * parse style definition file (see class documentation above for format), putting &lt;glob expression, style string&gt; pairs
	 * in styleDefinitions
	 * 
	 * @throws IOException
	 *             IOException
	 */
	private void parseDefinitionFile() throws IOException {
		String line;
		String globString;
		String styleString;
		GlobCompiler glob = new GlobCompiler();
		Pattern globPattern;
		// read lines...
		while ((line = reader.readLine()) != null) {
			// ...trimming whitespace:
			line = line.trim();
			// ignore lines that are empty or start with #:
			if (line.equals("") || line.startsWith("#")) {
				continue;
			} else {
				// split lines into fields
				String[] fields = line.split("=");
				try {
					globString = fields[0].trim();
					styleString = fields[1].trim();
				} catch (IndexOutOfBoundsException iob) {
					System.err.println("Warning: could not parse line: " + line);
					fileOK = false;
					continue;
				}
				// create GlobCompiler for glob expression:
				try {
					globPattern = glob.compile(globString);
				} catch (MalformedPatternException mpe) {
					System.err.println("Warning: could not parse line: ");
					fileOK = false;
					continue;
				}
				// put (glob expression, style string) pair in styleDefinions:
				styleDefinitions.put(globPattern, styleString);
			}
		}
		if (styleDefinitions.isEmpty()) {
			System.err.println("Warning: no style definitions were found!");
		}
	}

	/**
	 * match basename against the glob expressions in styleDefinitions
	 * 
	 * @param basename
	 *            basename
	 * @return style String of first matching glob expression, or empty String if no glob matches
	 */
	public String getValue(String basename) {
		Perl5Matcher globMatcher = new Perl5Matcher();
		String style = "";
		for (Pattern globPattern : styleDefinitions.keySet()) {
			if (globMatcher.matches(basename, globPattern)) {
				style = styleDefinitions.get(globPattern);
				break; // enable this line to change behavior to return style of *first* matching glob expr
				// return style; // enable this line to change behavior to return style of *last* matching glob expr
			}
		}
		// no globPattern in styleDefinitions matched... return empty string:
		return style;
	}
}
