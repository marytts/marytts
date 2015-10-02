/**
 * Copyright 2009 DFKI GmbH.
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

package marytts.tools.analysis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import marytts.client.http.MaryHttpClient;
import marytts.datatypes.MaryXML;
import marytts.modules.phonemiser.AllophoneSet;
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.Label;
import marytts.signalproc.analysis.Labels;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.text.LabelfileDoubleDataSource;
import marytts.util.data.text.PraatPitchTier;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.string.StringUtils;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

/**
 * Impose duration and/or intonation from one version of an utterance to another version. The segmental chain of both utterances
 * can have tiny deviations but must be sufficiently similar to allow the MaryTranscriptionAligner to match the two. The
 * destination must be in ALLOPHONES or ACOUSTPARAMS format, i.e. have syllabic and segmental substructure in the tokens. The
 * source can be in a number of formats.
 * 
 * @author marc
 * 
 */
public class CopySynthesis {
	AllophoneSet allophoneSet;

	/**
	 * Provide copy synthesis functionality for documents using the given allophone set.
	 * 
	 * @param allophoneSet
	 *            the allophone set to use, or null if no allophone verification is needed.
	 */
	public CopySynthesis(AllophoneSet allophoneSet) {
		this.allophoneSet = allophoneSet;
	}

	/**
	 * Make sure that the label sequence as provided in source is copied into the target document.
	 * 
	 * @param source
	 *            a label sequence consisting of valid allophones according to the allophone set given in the constructor.
	 * @param target
	 *            a MaryXML document at the stage of ALLOPHONES or ACOUSTPARAMS, i.e. with syllable/segment substructure in
	 *            phones.
	 */
	public void imposeSegments(Labels source, Document target) {
		MaryTranscriptionAligner aligner = new MaryTranscriptionAligner(allophoneSet, true);
		aligner.SetEnsureInitialBoundary(false);
		String labels = StringUtils.join(aligner.getEntrySeparator(), source.getLabelSymbols());
		aligner.alignXmlTranscriptions(target, labels);

	}

	/**
	 * Make sure that the label sequence as provided in source is copied into the target document, and that the phone and boundary
	 * durations in the target are adjusted to those in source.
	 * 
	 * @param source
	 *            a label sequence consisting of valid allophones according to the allophone set given in the constructor.
	 * @param target
	 *            a MaryXML document at the stage of ALLOPHONES or ACOUSTPARAMS, i.e. with syllable/segment substructure in
	 *            phones.
	 */
	public void imposeDurations(Labels source, Document target) {
		imposeSegments(source, target);
		// we trust that the following holds: (see CopySynthesisTest):
		// assert Arrays.deepEquals(source.getLabelSymbols(), new Labels(target).getLabelSymbols());
		// or in other words, the number of labels in source are now the same as the number of
		// phone and boundary items in target.
		String PHONE = "ph";
		String BOUNDARY = "boundary";
		NodeIterator it = DomUtils.createNodeIterator(target, PHONE, BOUNDARY);

		Element e = null;
		double prevEndTime = 0;
		int iSource = 0;
		while ((e = (Element) it.nextNode()) != null) {
			updateDurationAndEndTime(e, source.items[iSource], prevEndTime);
			prevEndTime = source.items[iSource].time;
			iSource++;
		}
	}

	private void updateDurationAndEndTime(Element e, Label label, double prevEndTime) {
		String PHONE = "ph";
		String A_PHONE_DURATION = "d";
		String A_PHONE_SYMBOL = "p";
		String A_PHONE_END = "end";
		String BOUNDARY = "boundary";
		String A_BOUNDARY_DURATION = "duration";

		assert label.time > prevEndTime;
		double durationSeconds = label.time - prevEndTime;
		String durationMillisString = String.valueOf((int) Math.round(1000 * durationSeconds));
		// System.out.println("For label "+label+", setting duration "+durationSeconds+" -> "+durationMillisString);
		if (e.getTagName().equals(PHONE)) {
			assert label.phn.equals(e.getAttribute(A_PHONE_SYMBOL));
			e.setAttribute(A_PHONE_DURATION, durationMillisString);
			e.setAttribute(A_PHONE_END, String.valueOf(label.time));
		} else {
			assert e.getTagName().equals(BOUNDARY);
			e.setAttribute(A_BOUNDARY_DURATION, durationMillisString);
		}

	}

	/**
	 * Make sure that 1. the label sequence as provided in source is copied into the target document, 2. the phone and boundary
	 * durations in the target are adjusted to those in source, and 3. the intonation targets in the target are replaced by those
	 * in the pitchSource.
	 * 
	 * @param durationAndSegmentSource
	 *            a label sequence consisting of valid allophones according to the allophone set given in the constructor.
	 * @param pitchSource
	 *            a specification of an intonation contour.
	 * @param target
	 *            a MaryXML document at the stage of ALLOPHONES or ACOUSTPARAMS, i.e. with syllable/segment substructure in
	 *            phones.
	 */
	public void imposeIntonation(Labels durationAndSegmentSource, PraatPitchTier pitchSource, Document target) {
		throw new RuntimeException("Not yet implemented");
	}

	/**
	 * @param args
	 *            args
	 * @throws Exception
	 *             Exception
	 */
	public static void main(String[] args) throws Exception {
		String wavFilename = null;
		String labFilename = null;
		String pitchFilename = null;
		String textFilename = null;

		String locale = System.getProperty("locale");
		if (locale == null) {
			throw new IllegalArgumentException("No locale given (-Dlocale=...)");
		}

		for (String arg : args) {
			if (arg.endsWith(".txt"))
				textFilename = arg;
			else if (arg.endsWith(".wav"))
				wavFilename = arg;
			else if (arg.endsWith(".ptc"))
				pitchFilename = arg;
			else if (arg.endsWith(".lab"))
				labFilename = arg;
			else
				throw new IllegalArgumentException("Don't know how to treat argument: " + arg);
		}

		// The intonation contour
		double[] contour = null;
		double frameShiftTime = -1;
		if (pitchFilename == null) { // need to create pitch contour from wav file
			if (wavFilename == null) {
				throw new IllegalArgumentException("Need either a pitch file or a wav file");
			}
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File(wavFilename));
			AudioDoubleDataSource audio = new AudioDoubleDataSource(ais);
			PitchFileHeader params = new PitchFileHeader();
			params.fs = (int) ais.getFormat().getSampleRate();
			F0TrackerAutocorrelationHeuristic tracker = new F0TrackerAutocorrelationHeuristic(params);
			tracker.pitchAnalyze(audio);
			frameShiftTime = tracker.getSkipSizeInSeconds();
			contour = tracker.getF0Contour();
		} else { // have a pitch file -- ignore any wav file
			PitchReaderWriter f0rw = new PitchReaderWriter(pitchFilename);
			if (f0rw.contour == null) {
				throw new NullPointerException("Cannot read f0 contour from " + pitchFilename);
			}
			contour = f0rw.contour;
			frameShiftTime = f0rw.header.skipSizeInSeconds;
		}
		assert contour != null;
		assert frameShiftTime > 0;

		// The ALLOPHONES data and labels
		if (labFilename == null) {
			throw new IllegalArgumentException("No label file given");
		}
		if (textFilename == null) {
			throw new IllegalArgumentException("No text file given");
		}
		MaryTranscriptionAligner aligner = new MaryTranscriptionAligner();
		aligner.SetEnsureInitialBoundary(false);
		String labels = MaryTranscriptionAligner.readLabelFile(aligner.getEntrySeparator(), aligner.getEnsureInitialBoundary(),
				labFilename);
		MaryHttpClient mary = new MaryHttpClient();
		String text = FileUtils.readFileToString(new File(textFilename), "ASCII");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mary.process(text, "TEXT", "ALLOPHONES", locale, null, null, baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(true);
		DocumentBuilder builder = docFactory.newDocumentBuilder();
		Document doc = builder.parse(bais);
		aligner.alignXmlTranscriptions(doc, labels);
		assert doc != null;

		// durations
		double[] endTimes = new LabelfileDoubleDataSource(new File(labFilename)).getAllData();
		assert endTimes.length == labels.split(Pattern.quote(aligner.getEntrySeparator())).length;

		// Now add durations and f0 targets to document
		double prevEnd = 0;
		NodeIterator ni = MaryDomUtils.createNodeIterator(doc, MaryXML.PHONE, MaryXML.BOUNDARY);
		for (int i = 0; i < endTimes.length; i++) {
			Element e = (Element) ni.nextNode();
			if (e == null)
				throw new IllegalStateException("More durations than elements -- this should not happen!");
			double durInSeconds = endTimes[i] - prevEnd;
			int durInMillis = (int) (1000 * durInSeconds);
			if (e.getTagName().equals(MaryXML.PHONE)) {
				e.setAttribute("d", String.valueOf(durInMillis));
				e.setAttribute("end", new Formatter(Locale.US).format("%.3f", endTimes[i]).toString());
				// f0 targets at beginning, mid, and end of phone
				StringBuilder f0String = new StringBuilder();
				double startF0 = getF0(contour, frameShiftTime, prevEnd);
				if (startF0 != 0 && !Double.isNaN(startF0)) {
					f0String.append("(0,").append((int) startF0).append(")");
				}
				double midF0 = getF0(contour, frameShiftTime, prevEnd + 0.5 * durInSeconds);
				if (midF0 != 0 && !Double.isNaN(midF0)) {
					f0String.append("(50,").append((int) midF0).append(")");
				}
				double endF0 = getF0(contour, frameShiftTime, endTimes[i]);
				if (endF0 != 0 && !Double.isNaN(endF0)) {
					f0String.append("(100,").append((int) endF0).append(")");
				}
				if (f0String.length() > 0) {
					e.setAttribute("f0", f0String.toString());
				}
			} else { // boundary
				e.setAttribute("duration", String.valueOf(durInMillis));
			}
			prevEnd = endTimes[i];
		}
		if (ni.nextNode() != null) {
			throw new IllegalStateException("More elements than durations -- this should not happen!");
		}

		// TODO: add pitch values

		String acoustparams = DomUtils.document2String(doc);
		System.out.println("ACOUSTPARAMS:");
		System.out.println(acoustparams);
	}

	/**
	 * For the given sampled contour and skipSize, provide the f0 value at time t if possible or Double.NaN otherwise.
	 * 
	 * @param contour
	 *            contour
	 * @param skipSize
	 *            skipSize
	 * @param t
	 *            t
	 * @return Double.NaN
	 */
	private static double getF0(double[] contour, double skipSize, double t) {
		int i = (int) (t / skipSize);
		if (i >= 0 && i < contour.length)
			return contour[i];
		return Double.NaN;
	}

}
