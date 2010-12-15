/**
 * Copyright 2010 DFKI GmbH.
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

package marytts.modules;

// DOM classes
import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modules.synthesis.Voice;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.select.UnitSelector;
import marytts.util.data.text.PraatInterval;
import marytts.util.data.text.PraatIntervalTier;
import marytts.util.data.text.PraatTextGrid;
import marytts.util.dom.NameNodeFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * Transforms a full MaryXML document into a Praat TextGrid containing various interesting information; in particular, the source
 * units and basenames used in unit selection synthesis
 * 
 * @author steiner
 */

public class PraatTextGridGenerator extends InternalModule {

    public PraatTextGridGenerator() {
        super("Praat TextGrid generator", MaryDataType.AUDIO, MaryDataType.PRAAT_TEXTGRID, null);
    }

    public MaryData process(MaryData d) throws Exception {
        // prevUnitIndex;
        // numberOfConsecutiveUnits;
        // basenameDuration;
        // phoneTier;
        // PraatIntervalTier unitTier;
        // PraatIntervalTier sourceTier;
        // sourceInterval;

        Document doc = d.getDocument();

        // initialize various variables:
        Double xmin = 0.0;
        Double xmax = 0.0;
        Double duration;
        String phone = null;

        Double sentenceStart = 0.0; // this is cumulatively added to counter sentence-initial timing resets

        // initialize some class variables:
        PraatIntervalTier phoneTier = new PraatIntervalTier("phones");
        Double basenameDuration = 0.0;
        int prevUnitIndex = Integer.MIN_VALUE;
        int numberOfConsecutiveUnits = 0; // counter to track consecutive units
        PraatInterval sourceInterval = new PraatInterval(basenameDuration);

        // until we have a robust way of checking the voice type, just initialize unit and source tiers anyway:
        PraatIntervalTier unitTier = new PraatIntervalTier("units");
        PraatIntervalTier sourceTier = new PraatIntervalTier("sources");

        // prepare to iterate only over the PHONE, SENTENCE, and BOUNDARY nodes in the MaryXML:
        NodeIterator ni = ((DocumentTraversal) doc).createNodeIterator(doc, NodeFilter.SHOW_ELEMENT, new NameNodeFilter(
                new String[] { MaryXML.SENTENCE, MaryXML.PHONE, MaryXML.BOUNDARY }), false);
        Element element;

        // now iterate over these nodes:
        while ((element = (Element) ni.nextNode()) != null) {
            String tagName = element.getTagName(); // <s>, <ph>, or <boundary> as specified above
            if (tagName.equals(MaryXML.SENTENCE)) {
                sentenceStart = xmax;
                continue; // goto next node, do not collect $200
            } else if (tagName.equals(MaryXML.PHONE)) {
                phone = element.getAttribute("p");
                xmin = xmax;
                xmax = Double.parseDouble(element.getAttribute("end")) + sentenceStart; // TODO: diphone voices have end in ms!
                duration = xmax - xmin;
                xmin = xmax;
            } else { // boundary
                phone = "_"; // TODO: perhaps we should access TargetFeatureComputer.getPauseSymbol() instead
                try {
                    duration = Double.parseDouble(element.getAttribute("duration")) / 1000.0; // duration is always in ms
                    xmax += duration;
                } catch (NumberFormatException nfe) {
                    continue; // HMM voices can have duration-less <boundary/> tags, which can't be processed here
                }
            }

            PraatInterval phoneInterval = new PraatInterval(duration, phone);

            // TODO: crude way of checking for unit selection voice; also, name of attribute could change!
            if (element.hasAttribute("units")) {
                // unitselectionProcessing(element, unitTier, prevUnitIndex, numberOfConsecutiveUnits, basenameDuration,
                // sourceInterval, sourceTier);
                String units = element.getAttribute("units");
                String[] unitStrings = units.split("; "); // boundaries have only one unit string
                boolean differentSource = false;
                String basename = null;
                String unitRange = null;
                for (String unitString : unitStrings) {
                    // TODO verify that unit string matches "UNITNAME BASENAME UNITINDEX UNITDURATION"
                    String[] unitFields = unitString.split(" ");
                    String unitName = unitFields[0];
                    basename = unitFields[1];
                    int unitIndex = Integer.parseInt(unitFields[2]);
                    Double unitDuration = Double.parseDouble(unitFields[3]);

                    // units are straightforward, just like phones:
                    unitTier.appendInterval(new PraatInterval(unitDuration, unitString));

                    // unit source processing is a little more elaborate:

                    /*
                     * Note: the following assumes that consecutive selected units are ALWAYS from the same basename! That could
                     * change if basename boundaries are no longer marked by null units in the timeline.
                     */
                    differentSource = unitIndex != prevUnitIndex + 1; // is source unit from a different part of the timeline?;
                    if (differentSource) {
                        // reset primary variables:
                        numberOfConsecutiveUnits = 0;
                        basenameDuration = 0.0;
                    }
                    // increment/increase primary variables:
                    numberOfConsecutiveUnits++;
                    basenameDuration += unitDuration;

                    // construct unit index range string:
                    unitRange = Integer.toString(unitIndex - numberOfConsecutiveUnits + 1);
                    if (numberOfConsecutiveUnits > 1) {
                        unitRange = unitRange + "-" + unitIndex;
                    }

                    // append source intervals to source tier:
                    if (differentSource) {
                        sourceInterval = new PraatInterval(basenameDuration, basename + ": " + unitRange);
                        sourceTier.appendInterval(sourceInterval);
                    } else {
                        sourceInterval.setDuration(basenameDuration);
                        sourceInterval.setText(basename + ": " + unitRange);
                    }

                    prevUnitIndex = unitIndex;
                }
                // HACK: arbitrary threshold to detect end points in ms (in the case of diphone voice or boundary segment)
            } else if (duration > 10) {
                // TODO: there is still a bug somewhere regarding boundary durations with mbrola...
                phoneInterval.setDuration(duration / 1000.0);
            }
            phoneTier.appendInterval(phoneInterval);
        }

        PraatTextGrid textGrid = new PraatTextGrid();
        phoneTier.updateBoundaries(); // force full specification of timings
        textGrid.appendTier(phoneTier);

        // fragile way of checking whether this is a unit selection voice:
        if (unitTier.getNumberOfIntervals() > 0) {
            // complete and append unit and source tiers:
            unitTier.updateBoundaries();
            textGrid.appendTier(unitTier);
            sourceTier.updateBoundaries();
            textGrid.appendTier(sourceTier);
        }

        // return raw TextGrid as result:
        MaryData result = new MaryData(outputType(), d.getLocale());
        result.setPlainText(textGrid.toString());
        return result;
    }

}
