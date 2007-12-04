/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package de.dfki.lt.mary.recsessionmgr.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.StringTokenizer;

import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import de.dfki.lt.mary.recsessionmgr.debug.Test;

/**
 *
 * @author Mat Wilson <mwilson@dfki.de>
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
        window.setLocation(new Point((screenSize.width - frameSize.width) / 2,
                              (screenSize.height - frameSize.height) / 2));
    }
    
    /** Sets the GUI look and feel to that of the system (e.g., Windows XP, Mac OS) */
    private static void setSystemLookAndFeel() {
        
        if (systemLookAndFeel) {
        
            // Use the look and feel of the system
            try {
                // Set to system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } 
            catch (UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
            }
            catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            catch (InstantiationException ex) {
                 ex.printStackTrace();
            }
            catch (IllegalAccessException ex) {
                 ex.printStackTrace();
            }
            
        }
    }
    
    // Return length in pixels of longest cell content
    public static int getMaxColumnWidth(JTable table, int column) {
        
        // Set first to column header width as a minimum
        Font font = table.getFont();
        FontMetrics metrics = table.getGraphics().getFontMetrics(font);
        String header = table.getColumnName(column) + "   ";  // Whitespace buffer as a crude way (hack)
                                                              // to account for the rendering context
        Test.output("Column name: " + header);
        
        // Convert from string length in characters to pixels
        int widest = metrics.stringWidth(header) +
                (2 * table.getColumnModel().getColumnMargin());
        
        Test.output("Starting widest value: " + header + " (" + widest + " pixels)");
        
        // Now go through each row to see if there is a longer value in the column
        int rows = table.getRowCount();
        Test.output("Row count: " + rows);
        for (int index = 0; index < rows; index++) {
                String cellValue = table.getValueAt(index, column).toString();
                int cellWidth = metrics.stringWidth(cellValue) +
                    (2 * table.getColumnModel().getColumnMargin());
                if (cellWidth > widest) {
                    widest = cellWidth;
                    Test.output("New widest value: " + widest + " pixels");
                }        
        }
        
        return widest;
    }

    static void centerPromptText(JTextPane pane, String fullText) {
        int top = 10;
        int bottom = 10;
        int left = 10;
        int right = 10;
        SimpleAttributeSet set=new SimpleAttributeSet();
        StyleConstants.setAlignment(set,StyleConstants.ALIGN_CENTER);
        StyleConstants.setSpaceAbove(set, top);
        StyleConstants.setSpaceBelow(set, bottom);
        StyleConstants.setLeftIndent(set, left);
        StyleConstants.setRightIndent(set, right);
        pane.setParagraphAttributes(set,false);
        
        boolean ok = false;
        while (!ok) {
            // Get font metrics for the display pane
            Font font = pane.getFont();
            assert font != null : "no font";
            FontMetrics metrics = pane.getGraphics().getFontMetrics(font);
            assert metrics != null : "no font metrics";
            
            // Get prompt text width in pixels
            int lineHeight = metrics.getHeight();
            int paneWidth = pane.getWidth() - left - right;
            int paneHeight = pane.getHeight() - top - bottom;
            //Test.output("Text width: " + textWidth);
            //Test.output("Pane width: " + paneWidth);

            int nLines = 1;
            StringTokenizer st = new StringTokenizer(fullText, " ", true);
            int spaceWidth = metrics.stringWidth(" ");
            int lineWidth = 0;
            while (st.hasMoreTokens()) {
                String word = st.nextToken();
                int wordWidth = metrics.stringWidth(word);
                lineWidth += wordWidth;
                lineWidth += spaceWidth;
                if (lineWidth >= paneWidth) {
                    nLines++;
                    lineWidth = wordWidth + spaceWidth;
                }
            }

            int textHeight = nLines * lineHeight + nLines * metrics.getLeading();
            //Test.output("Pane height: "+paneHeight);
            //Test.output("Text height: "+textHeight);
            if (textHeight <= paneHeight-10) {
                ok = true;
            } else {
                Font smaller = new Font(font.getName(), font.getStyle(), font.getSize()-1);
                pane.setFont(smaller);
            }
           
        }
        
         
        //pane.updateUI();  // Refresh or we'll see artefact from the previous prompt
        pane.setText(fullText);
    }
    
}
