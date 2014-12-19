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

import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;

/**
 * A NodeFilter accepting only nodes with names matching a given regular expression.
 * 
 * @author Marc Schr&ouml;der
 */

public class RENodeFilter implements NodeFilter {
	private Pattern re;

	public RENodeFilter(String reString) {
		this.re = Pattern.compile(reString);
	}

	public RENodeFilter(Pattern re) {
		this.re = re;
	}

	public short acceptNode(Node n) {
		if (re.matcher(n.getNodeName()).matches())
			return NodeFilter.FILTER_ACCEPT;
		else
			return NodeFilter.FILTER_SKIP;
	}
}
