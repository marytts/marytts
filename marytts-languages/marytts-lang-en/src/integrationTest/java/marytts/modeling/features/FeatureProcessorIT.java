/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.modeling.features;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryDataType;
import marytts.datatypes.MaryXML;
import marytts.modeling.features.MaryGenericFeatureProcessors.TargetElementNavigator;
// import marytts.modeling.features.HalfPhoneTarget;
import marytts.modeling.features.FeatureVector;
import marytts.util.dom.MaryDomUtils;

import org.apache.log4j.BasicConfigurator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

import org.testng.Assert;
import org.testng.annotations.*;

/**
 * FIXME: commented half-phone part should be in another test part
 */
public class FeatureProcessorIT {
	private static MaryData acoustparams;
	private static List<Element> phoneTargets;
	private static FeatureProcessorManager mgr;

	@BeforeClass
	public static void setupClass() throws Exception {
		BasicConfigurator.configure();

		acoustparams = new MaryData(MaryDataType.ACOUSTPARAMS, Locale.ENGLISH);
		acoustparams.readFrom(FeatureProcessorIT.class.getResourceAsStream("test1.acoustparams"));
		phoneTargets = new ArrayList<Element>();
		Document doc = acoustparams.getDocument();
		NodeIterator segs = MaryDomUtils.createNodeIterator(doc, doc, MaryXML.PHONE, MaryXML.BOUNDARY);
		Element s;
		while ((s = (Element) segs.nextNode()) != null) {
			String phone;
			if (s.getTagName().equals(MaryXML.PHONE))
				phone = s.getAttribute("p");
			else
				phone = "_"; // boundary --> pause
			phoneTargets.add(s);
		}

		try {
			if (System.getProperty("mary.base") == null) {
				System.setProperty("mary.base", ".");
				Logger.global.warning("System property 'mary.base' is not defined -- trying " + new File(".").getAbsolutePath()
                                      + " -- if this fails, please start this using VM property \"-Dmary.base=/path/to/mary/runtime\"!");
			}
			mgr = new marytts.modeling.features.FeatureProcessorManager("en_US");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@BeforeMethod
	public void setUp() throws Exception {
	}

	@Test
	public void haveAcoustparams() {
		Assert.assertNotNull(acoustparams);
		Assert.assertNotNull(acoustparams.getDocument());
	}

	@Test
	public void haveTargets() {
		Assert.assertNotNull(phoneTargets);
		// Assert.assertNotNull(halfphoneTargets);
		Assert.assertTrue(phoneTargets.size() > 0);
		// Assert.assertTrue(phoneTargets.size() * 2 == halfphoneTargets.size());
	}

	@Test
	public void sameNumberOfTargetsAndSegments() {
		NodeList segs = acoustparams.getDocument().getElementsByTagName(MaryXML.PHONE);
		NodeList boundaries = acoustparams.getDocument().getElementsByTagName(MaryXML.BOUNDARY);
		Assert.assertEquals(segs.getLength() + boundaries.getLength(), phoneTargets.size());
	}

	private String getPhoneName(Element segmentOrBoundary) {
		if (segmentOrBoundary == null)
			return "null";
		if (segmentOrBoundary.getTagName().equals(MaryXML.PHONE)) {
			return segmentOrBoundary.getAttribute("p");
		} else {
			return "_";
		}

	}

	@Test
	public void segmentNavigators() {
		TargetElementNavigator segmentNavigator = new MaryGenericFeatureProcessors.SegmentNavigator();
		TargetElementNavigator prevSegmentNavigator = new MaryGenericFeatureProcessors.PrevSegmentNavigator();
		TargetElementNavigator nextSegmentNavigator = new MaryGenericFeatureProcessors.NextSegmentNavigator();
		TargetElementNavigator prevprevSegmentNavigator = new MaryGenericFeatureProcessors.PrevPrevSegmentNavigator();
		TargetElementNavigator nextnextSegmentNavigator = new MaryGenericFeatureProcessors.NextNextSegmentNavigator();
		Element prevprev = null;
		Element prev = null;
		Element seg = phoneTargets.get(0);
		Element next = phoneTargets.get(1);
		for (Element t : phoneTargets) {
			Assert.assertEquals(prevprev,
                                prevprevSegmentNavigator.getElement(t),
                                "Mismatch: expected " + getPhoneName(prevprev) + ", got "
                                + getPhoneName(prevprevSegmentNavigator.getElement(t)));
            Assert.assertEquals(prev, prevSegmentNavigator.getElement(t));
            Assert.assertEquals(seg, segmentNavigator.getElement(t));
            Assert.assertEquals(next, nextSegmentNavigator.getElement(t),
                                "Mismatch: expected " + getPhoneName(next) + ", got " + getPhoneName(nextSegmentNavigator.getElement(t)));
            prevprev = prev;
            prev = seg;
            seg = next;
            next = nextnextSegmentNavigator.getElement(t);
        }
    }

    @Test
    public void syllableNavigators() {
        TargetElementNavigator syllableNavigator = new MaryGenericFeatureProcessors.SyllableNavigator();
        TargetElementNavigator prevSyllableNavigator = new MaryGenericFeatureProcessors.PrevSyllableNavigator();
        TargetElementNavigator prevprevSyllableNavigator = new MaryGenericFeatureProcessors.PrevPrevSyllableNavigator();
        TargetElementNavigator nextSyllableNavigator = new MaryGenericFeatureProcessors.NextSyllableNavigator();
        TargetElementNavigator nextnextSyllableNavigator = new MaryGenericFeatureProcessors.NextNextSyllableNavigator();
        NodeList phrases = acoustparams.getDocument().getElementsByTagName(MaryXML.PHRASE);
        Assert.assertEquals(2, phrases.getLength());
        Element phrase1 = (Element) phrases.item(0);
        Element phrase2 = (Element) phrases.item(1);
        NodeList syllables1 = phrase1.getElementsByTagName(MaryXML.SYLLABLE);
        Assert.assertEquals(2, syllables1.getLength());
        NodeList syllables2 = phrase2.getElementsByTagName(MaryXML.SYLLABLE);
        Element lastSylPhrase1 = (Element) syllables1.item(syllables1.getLength() - 1);
        Element prevSylPhrase1 = (Element) syllables1.item(syllables1.getLength() - 2);
        Element firstSylPhrase2 = (Element) syllables2.item(0);
        Element secondSylPhrase2 = (Element) syllables2.item(1);
        Element thirdSylPhrase2 = (Element) syllables2.item(2);
        Element lastSegPhrase1 = MaryDomUtils.getLastChildElement(lastSylPhrase1);
        Element firstSegPhrase2 = MaryDomUtils.getFirstChildElement(firstSylPhrase2);
        Element t1 = lastSegPhrase1;
        Element t2 = firstSegPhrase2;
        Assert.assertEquals(lastSylPhrase1, syllableNavigator.getElement(t1));
        Assert.assertEquals(prevSylPhrase1, prevSyllableNavigator.getElement(t1));
        Assert.assertNull(prevprevSyllableNavigator.getElement(t1));
        // syllable navigator crosses phrase boundaries
        Assert.assertEquals(firstSylPhrase2, nextSyllableNavigator.getElement(t1));
        Assert.assertEquals(secondSylPhrase2, nextnextSyllableNavigator.getElement(t1));
        // and for the other target:
        Assert.assertEquals(prevSylPhrase1, prevprevSyllableNavigator.getElement(t2));
        Assert.assertEquals(lastSylPhrase1, prevSyllableNavigator.getElement(t2));
        Assert.assertEquals(firstSylPhrase2, syllableNavigator.getElement(t2));
        Assert.assertEquals(secondSylPhrase2, nextSyllableNavigator.getElement(t2));
        Assert.assertEquals(thirdSylPhrase2, nextnextSyllableNavigator.getElement(t2));
    }

    @Test
    public void otherNavigators() {
        TargetElementNavigator firstSegInWordNavigator = new MaryGenericFeatureProcessors.FirstSegmentInWordNavigator();
        TargetElementNavigator firstSegNextWordNavigator = new MaryGenericFeatureProcessors.FirstSegmentNextWordNavigator();
        TargetElementNavigator lastSegInWordNavigator = new MaryGenericFeatureProcessors.LastSegmentInWordNavigator();
        TargetElementNavigator firstSylInWordNavigator = new MaryGenericFeatureProcessors.FirstSyllableInWordNavigator();
        TargetElementNavigator lastSylInWordNavigator = new MaryGenericFeatureProcessors.LastSyllableInWordNavigator();
        TargetElementNavigator lastSylInPhraseNavigator = new MaryGenericFeatureProcessors.LastSyllableInPhraseNavigator();
        TargetElementNavigator wordNavigator = new MaryGenericFeatureProcessors.WordNavigator();
        TargetElementNavigator nextWordNavigator = new MaryGenericFeatureProcessors.NextWordNavigator();
        TargetElementNavigator lastWordInSentenceNavigator = new MaryGenericFeatureProcessors.LastWordInSentenceNavigator();

        NodeList phrases = acoustparams.getDocument().getElementsByTagName(MaryXML.PHRASE);
        Assert.assertEquals(2, phrases.getLength());
        Element phrase1 = (Element) phrases.item(0);
        Element phrase2 = (Element) phrases.item(1);
        NodeList syllables1 = phrase1.getElementsByTagName(MaryXML.SYLLABLE);
        Assert.assertEquals(2, syllables1.getLength());
        NodeList syllables2 = phrase2.getElementsByTagName(MaryXML.SYLLABLE);
        Element firstSylPhrase1 = (Element) syllables1.item(0);
        Element lastSylPhrase1 = (Element) syllables1.item(syllables1.getLength() - 1);
        Element prevSylPhrase1 = (Element) syllables1.item(syllables1.getLength() - 2);
        Element firstSylPhrase2 = (Element) syllables2.item(0);
        Element secondSylPhrase2 = (Element) syllables2.item(1);
        Element thirdSylPhrase2 = (Element) syllables2.item(2);
        Element lastSylPhrase2 = (Element) syllables2.item(syllables2.getLength() - 1);

        Element firstSegPhrase1 = MaryDomUtils.getFirstChildElement(firstSylPhrase1);
        Element lastSegPhrase1 = MaryDomUtils.getLastChildElement(lastSylPhrase1);
        Element firstSegPhrase2 = MaryDomUtils.getFirstChildElement(firstSylPhrase2);

        Element firstWord = (Element) firstSylPhrase1.getParentNode();
        Element firstWordPhrase2 = (Element) firstSylPhrase2.getParentNode();
        Element lastWord = (Element) lastSylPhrase2.getParentNode();

        Element t0 = firstSegPhrase1;
        Element t1 = lastSegPhrase1;
        Element t2 = firstSegPhrase2;

        Assert.assertEquals(firstSegPhrase1, firstSegInWordNavigator.getElement(t0));
        Assert.assertEquals(firstSegPhrase1, firstSegInWordNavigator.getElement(t1));

        Assert.assertEquals(firstSegPhrase2, firstSegNextWordNavigator.getElement(t0));
        Assert.assertEquals(firstSegPhrase2, firstSegNextWordNavigator.getElement(t1));

        Assert.assertEquals(lastSegPhrase1, lastSegInWordNavigator.getElement(t0));
        Assert.assertEquals(lastSegPhrase1, lastSegInWordNavigator.getElement(t1));

        Assert.assertEquals(firstSylPhrase1, firstSylInWordNavigator.getElement(t0));
        Assert.assertEquals(firstSylPhrase1, firstSylInWordNavigator.getElement(t1));

        Assert.assertEquals(lastSylPhrase1, lastSylInWordNavigator.getElement(t0));
        Assert.assertEquals(lastSylPhrase1, lastSylInWordNavigator.getElement(t1));

        Assert.assertEquals(lastSylPhrase1, lastSylInPhraseNavigator.getElement(t0));
        Assert.assertEquals(lastSylPhrase1, lastSylInPhraseNavigator.getElement(t1));
        Assert.assertEquals(lastSylPhrase2, lastSylInPhraseNavigator.getElement(t2));

        Assert.assertEquals(firstWord, wordNavigator.getElement(t0));
        Assert.assertEquals(firstWordPhrase2, wordNavigator.getElement(t2));

        Assert.assertEquals(firstWordPhrase2, nextWordNavigator.getElement(t0));

        Assert.assertEquals(lastWord, lastWordInSentenceNavigator.getElement(t0));
        Assert.assertEquals(lastWord, lastWordInSentenceNavigator.getElement(t2));
    }

    // @Test
    // public void phone() {
    //     ByteValuedFeatureProcessor phoneFP = (ByteValuedFeatureProcessor) mgr.getFeatureProcessor("phone");
    //     for (Target t : halfphoneTargets) {
    //         String phone = getPhoneName(t);
    //         String predicted = phoneFP.getValues()[phoneFP.process(t)];
    //         Assert.assertEquals(phone, predicted);
    //     }
    // }

    // TODO: write test methods for the all the other feature processors...
}
