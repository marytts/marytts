/**
 * Copyright 2006 DFKI GmbH.
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
package marytts.unitselection.select;

import java.util.ArrayList;
import java.util.List;

import marytts.unitselection.data.UnitDatabase;

import org.w3c.dom.Element;


public class HalfPhoneUnitSelector extends UnitSelector
{

    /**
     * Initialise the unit selector. Need to call load() separately.
     * @see #load(UnitDatabase)
     */
    public HalfPhoneUnitSelector() throws Exception
    {
        super();
    }

    /**
     * Create the list of targets from the XML elements to synthesize.
     * @param segmentsAndBoundaries a list of MaryXML phone and boundary elements
     * @return a list of Target objects
     */
    protected List<Target> createTargets(List<Element> segmentsAndBoundaries)
    {
        List<Target> targets = new ArrayList<Target>();
        for (Element sOrB : segmentsAndBoundaries) {
            String phone = getPhoneSymbol(sOrB);
            targets.add(new HalfPhoneTarget(phone+"_L", sOrB, true)); // left half
            targets.add(new HalfPhoneTarget(phone+"_R", sOrB, false)); // right half
        }
        return targets;
    }

}
