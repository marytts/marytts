/**
 * Copyright 2002 DFKI GmbH.
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

package marytts.language.de;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import marytts.config.MaryProperties;

import marytts.data.Utterance;
import marytts.datatypes.MaryXML;
import marytts.modules.MaryModule;
import marytts.util.dom.MaryDomUtils;
import marytts.util.dom.NameNodeFilter;

import org.apache.log4j.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * The preprocessing module.
 *
 * @author Marc Schr&ouml;der
 */

public class Preprocess extends MaryModule {

    public Preprocess() {
        super("Preprocess", Locale.GERMAN);
    }

    public Utterance process(Utterance utt, MaryProperties configuration) throws Exception {
	return utt;
    }
}
