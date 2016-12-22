/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.tools.emospeak;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * 
 * @author Marc Schr&ouml;der
 */
public class EmoSpeak extends javax.swing.JFrame {
	/**
	 * Creates new form EmoSpeak
	 * 
	 * @throws Exception
	 *             Exception
	 */
	public EmoSpeak() throws Exception {
		super("OpenMary EmoSpeak");
		initComponents();
		emoSpeakPanel1.initialiseMenu();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * 
	 * @throws IOException
	 *             IOException
	 * @throws UnknownHostException
	 *             UnknownHostException
	 */
	private void initComponents() throws IOException, UnknownHostException {
		emoSpeakPanel1 = new EmoSpeakPanel(true, System.getProperty("server.host", "cling.dfki.uni-sb.de"), Integer.getInteger(
				"server.port", 59125).intValue());

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
		setLocation((screenSize.width - 550) / 2, (screenSize.height - 630) / 2);
	}

	/**
	 * Exit the Application
	 * 
	 * @param evt
	 *            evt
	 */
	private void exitForm(java.awt.event.WindowEvent evt) {
		emoSpeakPanel1.requestExit();
		System.exit(0);
	}

	/**
	 * @param args
	 *            the command line arguments
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String args[]) throws Exception {
		new EmoSpeak().setVisible(true);
	}

	// Variables declaration - do not modify
	private marytts.tools.emospeak.EmoSpeakPanel emoSpeakPanel1;
	// End of variables declaration

}
