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
package marytts.client;

import java.awt.FlowLayout;
import java.io.IOException;

import javax.swing.JApplet;

import marytts.util.http.Address;

/**
 * @author Marc Schr&ouml;der
 * 
 */
public class MaryInterfaceApplet extends JApplet {
	private MaryGUIClient maryExpertInterface;

	public void init() {
		String host = getCodeBase().getHost();
		if (host == null || host.equals("")) {
			host = "mary.dfki.de";
		}
		System.out.println("Connecting to " + host);
		int port = 59125;
		try {
			maryExpertInterface = new MaryGUIClient(new Address(host, port), this);
			getContentPane().setLayout(new FlowLayout());
			getContentPane().add(maryExpertInterface);
		} catch (IOException ioe) {
			System.err.println("Cannot connect to MARY server on " + host + ":" + port);
			ioe.printStackTrace();
		}
	}

	public void destroy() {

	}

}
