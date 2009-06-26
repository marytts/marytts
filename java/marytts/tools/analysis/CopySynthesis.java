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
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;

import marytts.client.http.MaryHttpClient;
import marytts.datatypes.MaryXML;
import marytts.signalproc.analysis.PitchReaderWriter;
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic;
import marytts.signalproc.analysis.PitchFileHeader;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.text.LabelfileDoubleDataSource;
import marytts.util.dom.DomUtils;
import marytts.util.dom.MaryDomUtils;
import marytts.util.string.StringUtils;

/**
 * @author marc
 *
 */
public class CopySynthesis
{

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
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
                throw new IllegalArgumentException("Don't know how to treat argument: "+arg);
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
                throw new NullPointerException("Cannot read f0 contour from "+pitchFilename);
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
        TranscriptionAligner aligner = new TranscriptionAligner();
        aligner.SetEnsureInitialBoundary(false);
        String labels = aligner.readLabelFile(labFilename);
        MaryHttpClient mary = new MaryHttpClient();
        String text = StringUtils.readTextFileIntoString(textFilename, "ASCII");
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
        for (int i=0; i<endTimes.length; i++) {
            Element e = (Element) ni.nextNode();
            if (e == null) throw new IllegalStateException("More durations than elements -- this should not happen!");
            double durInSeconds = endTimes[i] - prevEnd;
            int durInMillis = (int) (1000 * durInSeconds);
            if (e.getTagName().equals(MaryXML.PHONE)) {
                e.setAttribute("d", String.valueOf(durInMillis));
                e.setAttribute("end", new Formatter(Locale.US).format("%.3f", endTimes[i]).toString());
                // f0 targets at beginning, mid, and end of phone
                StringBuilder f0String = new StringBuilder();
                double startF0 = getF0(contour, frameShiftTime, prevEnd);
                if (startF0 != 0 && !Double.isNaN(startF0)) {
                    f0String.append("(0,").append((int)startF0).append(")");
                }
                double midF0 = getF0(contour, frameShiftTime, prevEnd+0.5*durInSeconds);
                if (midF0 != 0 && !Double.isNaN(midF0)) {
                    f0String.append("(50,").append((int)midF0).append(")");
                }
                double endF0 = getF0(contour, frameShiftTime, endTimes[i]);
                if (endF0 != 0 && !Double.isNaN(endF0)) {
                    f0String.append("(100,").append((int)endF0).append(")");
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
     * For the given sampled contour and skipSize, provide the f0 value at time t if
     * possible or Double.NaN otherwise.
     * @param contour
     * @param skipSize
     * @param t
     * @return
     */
    private static double getF0(double[] contour, double skipSize, double t)
    {
        int i = (int) (t/skipSize);
        if (i>=0 && i<contour.length) return contour[i];
        return Double.NaN;
    }


}
