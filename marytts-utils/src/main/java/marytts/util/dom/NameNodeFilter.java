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
package marytts.util.dom;

import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;

/**
 * A NodeFilter accepting only nodes with a given name.
 * 
 * @author Marc Schr&ouml;der
 */

public class NameNodeFilter implements NodeFilter {
	private String[] names;

	public NameNodeFilter(String... names) {
		if (names == null)
			throw new NullPointerException("Cannot filter on null names");
		this.names = names;
		for (int i = 0; i < names.length; i++) {
			if (names[i] == null)
				throw new NullPointerException("Cannot filter on null name");
		}
	}

	public short acceptNode(Node n) {
		String name = n.getNodeName();
		for (int i = 0; i < names.length; i++) {
			if (name.equals(names[i]))
				return NodeFilter.FILTER_ACCEPT;
		}
		return NodeFilter.FILTER_SKIP;
	}
}
