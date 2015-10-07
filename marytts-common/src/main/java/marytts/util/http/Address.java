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
package marytts.util.http;

/**
 * 
 * A class to keep host and port information in a structure
 * 
 * @author Oytun T&uuml;rk
 */
public class Address {
	private String host;
	private int port;
	private String fullAddress; // --> host:port
	private String httpAddress; // --> http://host:port

	public Address() {
		this("", "");
	}

	public Address(String hostIn, int portIn) {
		this(hostIn, String.valueOf(portIn));
	}

	public Address(String hostIn, String portIn) {
		init(hostIn, portIn);
	}

	public Address(String fullAddress) {
		String tmpAddress = fullAddress.trim();
		int index = tmpAddress.lastIndexOf(':');

		String hostIn = "";
		String portIn = "";
		if (index > 0) {
			hostIn = tmpAddress.substring(0, index);

			if (index + 1 < tmpAddress.length())
				portIn = tmpAddress.substring(index + 1);
		} else
			hostIn = tmpAddress;

		init(hostIn, portIn);
	}

	public void init(String hostIn, String portIn) {
		this.host = hostIn;

		if (portIn != "") {
			this.port = Integer.valueOf(portIn);
			this.fullAddress = this.host + ":" + portIn;
		} else // No port address specified, set fullAdrress equal to host address
		{
			this.port = Integer.MIN_VALUE;
			this.fullAddress = this.host;
		}

		if (this.fullAddress != null && this.fullAddress.length() > 0)
			this.httpAddress = "http://" + this.fullAddress;
		else
			this.httpAddress = null;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getFullAddress() {
		return fullAddress;
	}

	public String getHttpAddress() {
		return httpAddress;
	}
}
