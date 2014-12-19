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

/**
 * 
 * @author schroed
 */
public class EmoSpeakApplet extends javax.swing.JApplet {

	public void init() {
		initComponents();
		emoSpeakPanel1.initialiseMenu();
	}

	/**
	 * This method is called from within the init() method to initialize the form.
	 */
	private void initComponents() {
		String host = getCodeBase().getHost();
		if (host == null || host.equals("")) {
			host = "localhost";
		}
		try {
			emoSpeakPanel1 = new EmoSpeakPanel(false, host, 59125);
		} catch (Exception e) {
			System.err.println("Cannot initialise EmoSpeakPanel:");
			e.printStackTrace();
		}

		getContentPane().setLayout(new java.awt.FlowLayout());

		getContentPane().add(emoSpeakPanel1);

	}

	// Variables declaration
	private EmoSpeakPanel emoSpeakPanel1;

	// End of variables declaration

	public void destroy() {
		emoSpeakPanel1.requestExit();
	}
}
