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
package marytts.tools.redstart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import marytts.util.Pair;

/**
 *
 * @author Mat Wilson &lt;mwilson@dfki.de&gt;
 */
public class LookAndFeel {

	private static boolean systemLookAndFeel = true;

	/** Creates a new instance of LookAndFeel */
	public LookAndFeel() {

		getSystemLookAndFeel();
		setSystemLookAndFeel(systemLookAndFeel);
	}

	public static boolean getSystemLookAndFeel() {
		return systemLookAndFeel;
	}

	public static void setSystemLookAndFeel(boolean asSystem) {
		systemLookAndFeel = asSystem;
	}

	// PRI4 Not used yet - problem with passing right object
	public static void centerWindow(javax.swing.JFrame window) {
		// Center window in the user's screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = window.getSize();
		window.setLocation(new Point((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2));
	}

	/** Sets the GUI look and feel to that of the system (e.g., Windows XP, Mac OS) */
	private static void setSystemLookAndFeel() {

		if (systemLookAndFeel) {

			// Use the look and feel of the system
			try {
				// Set to system look and feel
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (UnsupportedLookAndFeelException ex) {
				ex.printStackTrace();
			} catch (ClassNotFoundException ex) {
				ex.printStackTrace();
			} catch (InstantiationException ex) {
				ex.printStackTrace();
			} catch (IllegalAccessException ex) {
				ex.printStackTrace();
			}

		}
	}

	// Return length in pixels of longest cell content
	public static int getMaxColumnWidth(JTable table, int column) {

		// Set first to column header width as a minimum
		Font font = table.getFont();
		FontMetrics metrics = table.getGraphics().getFontMetrics(font);
		String header = table.getColumnName(column) + "   "; // Whitespace buffer as a crude way (hack)
																// to account for the rendering context
		Test.output("Column name: " + header);

		// Convert from string length in characters to pixels
		int widest = metrics.stringWidth(header) + (2 * table.getColumnModel().getColumnMargin());

		Test.output("Starting widest value: " + header + " (" + widest + " pixels)");

		// Now go through each row to see if there is a longer value in the column
		int rows = table.getRowCount();
		Test.output("Row count: " + rows);
		for (int index = 0; index < rows; index++) {
			String cellValue = table.getValueAt(index, column).toString();
			int cellWidth = metrics.stringWidth(cellValue) + (2 * table.getColumnModel().getColumnMargin());
			if (cellWidth > widest) {
				widest = cellWidth;
				Test.output("New widest value: " + widest + " pixels");
			}
		}

		return widest;
	}

	static void centerPromptText(JTextPane pane, String fullText, boolean redAlertMode) {
		int top = 10;
		int bottom = 10;
		int left = 10;
		int right = 10;
		SimpleAttributeSet set = new SimpleAttributeSet();
		StyleConstants.setAlignment(set, StyleConstants.ALIGN_LEFT);
		StyleConstants.setSpaceAbove(set, top);
		StyleConstants.setSpaceBelow(set, bottom);
		StyleConstants.setLeftIndent(set, left);
		StyleConstants.setRightIndent(set, right);
		pane.setParagraphAttributes(set, false);
		// define alert red style for underling the ambiguous words (alert text is delimited by underscore chararcter)
		StyledDocument doc = pane.getStyledDocument();
		final Style alertStyle = doc.addStyle("Alert", null);
		StyleConstants.setForeground(alertStyle, Color.red);

		/*
		 * boolean ok = false; while (!ok) { // Get font metrics for the display pane Font font = pane.getFont(); assert font !=
		 * null : "no font"; FontMetrics metrics = pane.getGraphics().getFontMetrics(font); assert metrics != null :
		 * "no font metrics";
		 * 
		 * // Get prompt text width in pixels int lineHeight = metrics.getHeight(); int paneWidth = pane.getWidth() - left -
		 * right; int paneHeight = pane.getHeight() - top - bottom; //Test.output("Text width: " + textWidth);
		 * //Test.output("Pane width: " + paneWidth);
		 * 
		 * int nLines = 1; StringTokenizer st = new StringTokenizer(fullText, " ", true); int spaceWidth =
		 * metrics.stringWidth(" "); int lineWidth = 0; while (st.hasMoreTokens()) { String word = st.nextToken(); int wordWidth =
		 * metrics.stringWidth(word); lineWidth += wordWidth; lineWidth += spaceWidth; if (lineWidth >= paneWidth) { nLines++;
		 * lineWidth = wordWidth + spaceWidth; } }
		 * 
		 * int textHeight = nLines * lineHeight + nLines * metrics.getLeading(); //Test.output("Pane height: "+paneHeight);
		 * //Test.output("Text height: "+textHeight); if (textHeight <= paneHeight-10) { ok = true; } else { Font smaller = new
		 * Font(font.getName(), font.getStyle(), font.getSize()-1); pane.setFont(smaller); }
		 * 
		 * }
		 */

		// pane.updateUI(); // Refresh or we'll see artefact from the previous prompt
		if (!redAlertMode)
			pane.setText(fullText);
		else {
			// red alert view
			Pair<String, ArrayList<Integer>> returnedPair = parseUnderescoredText(fullText);

			String parsedfullText = returnedPair.getFirst();
			ArrayList<Integer> pairIntList = returnedPair.getSecond();

			pane.setText(parsedfullText);

			// System.out.println("#1 normal for loop");
			for (int i = 0; i < pairIntList.size(); i++) {
				int start = pairIntList.get(i);
				int lenght = pairIntList.get(++i) - start + 1;
				// System.out.println("i a b " + i + " " + start + " " + lenght );
				doc.setCharacterAttributes(start, lenght, alertStyle, false);
			}
		}
	}

	/*
	 * Parse the red alert formatted text with the underscore char return the text without the underscore and a vector of pair
	 * indexes used to define the red alert text
	 */
	private static Pair<String, ArrayList<Integer>> parseUnderescoredText(String fullText) {
		ArrayList<Integer> pairIntList = new ArrayList<Integer>();
		String tmpString = "";
		int startindex = 0;
		int findindex = 0;
		boolean alertFlag = false;
		int decreaseintA = 0;
		int decreaseintB = 0;
		while ((findindex >= 0) && (startindex <= fullText.length())) {
			findindex = fullText.indexOf('_', startindex);

			if (findindex == -1) {
				tmpString = tmpString + fullText.substring(startindex, fullText.length());
				;
				break;
			}

			else if (findindex == startindex) {
				startindex = findindex + 1;
				alertFlag = !alertFlag;
				decreaseintA++;
				decreaseintB++;
				continue;
			}

			decreaseintB++;

			int startIndexA = startindex - decreaseintA;
			int startIndexB = findindex - decreaseintB;

			String token0 = fullText.substring(startindex, findindex);

			decreaseintA++;

			if (alertFlag) {
				// System.out.println("add " + alertFlag + " " + a + "," + b);
				pairIntList.add(startIndexA);
				pairIntList.add(startIndexB);
			}

			tmpString = tmpString + token0;
			startindex = findindex + 1;
			alertFlag = !alertFlag;
		}
		// System.out.println(tmp);
		Pair<String, ArrayList<Integer>> returnedPair = new Pair<String, ArrayList<Integer>>(tmpString, pairIntList);
		return returnedPair;
	}
}
