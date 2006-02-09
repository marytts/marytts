/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.emospeak;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 *
 * @author  Marc Schr&ouml;der
 */
public class EmoSpeak extends javax.swing.JFrame
{    
    /** Creates new form EmoSpeak */
    public EmoSpeak() throws Exception {
        super("OpenMary EmoSpeak");
        initComponents();
        emoSpeakPanel1.initialiseMenu();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     */
    private void initComponents() throws IOException, UnknownHostException {
        emoSpeakPanel1 = new EmoSpeakPanel(true, System.getProperty("server.host", "cling.dfki.uni-sb.de"), Integer.getInteger("server.port", 59125).intValue());

        getContentPane().setLayout(new java.awt.FlowLayout());

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        getContentPane().add(emoSpeakPanel1);

        pack();
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(new java.awt.Dimension(550, 630));
        setLocation((screenSize.width-550)/2,(screenSize.height-630)/2);
    }
                                                
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {
        emoSpeakPanel1.requestExit();
        System.exit(0);
    }    

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws Exception {
        new EmoSpeak().setVisible(true);
    }
    
    
    // Variables declaration - do not modify
    private de.dfki.lt.mary.emospeak.EmoSpeakPanel emoSpeakPanel1;
    // End of variables declaration
    
}
