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
package marytts.tools.voiceimport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import marytts.signalproc.analysis.VoiceQuality;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.MCepDatagram;
import marytts.unitselection.data.TimelineReader;

/**
 * The VoiceQualityTimelineMaker class takes a database root directory and a list of basenames, and converts the corresponding vq
 * files into a VQ timeline in Mary format.
 * 
 * @author steiner
 */
public class VoiceQualityTimelineMaker extends VoiceImportComponent {

    protected DatabaseLayout db = null;

    protected int percent = 0;

    protected String vqExt = ".vq";

    protected final String name = "VoiceQualityTimelineMaker";

    public final String VQDIR = name + ".vqDir";

    public final String VQTIMELINE = name + ".vqTimeline";

    public final String BASENAMETIMELINE = name + ".basenameTimeline";

    public String getName() {
        return name;
    }

    public SortedMap<String, String> getDefaultProps(DatabaseLayout theDb) {
        this.db = theDb;
        if (props == null) {
            props = new TreeMap<String, String>();
            props.put(VQDIR, db.getProp(db.ROOTDIR) + "vq" + System.getProperty("file.separator"));
            props.put(VQTIMELINE, db.getProp(db.FILEDIR) + "timeline_vq" + db.getProp(db.MARYEXT));
            props.put(BASENAMETIMELINE, db.getProp(db.FILEDIR) + "timeline_basenames" + db.getProp(db.MARYEXT));
        }
        return props;
    }

    protected void setupHelp() {
        props2Help = new TreeMap<String, String>();
        props2Help.put(VQDIR, "Directory containing the vq files");
        props2Help.put(VQTIMELINE, "File containing all vq files. Will be created by this module");
        props2Help.put(BASENAMETIMELINE, "File containing all basenames.");
    }

    /**
     * Read and concatenate a list of vq files into one single timeline file.
     */
    public boolean compute() {
        System.out.println("---- Importing VQ parameter files\n\n");
        System.out.println("Base directory: " + db.getProp(db.ROOTDIR) + "\n");

        // Prepare the output directory for the timelines if it does not exist
        File timelineDir = new File(db.getProp(db.FILEDIR));

        String basenameTimelineFilename = getProp(BASENAMETIMELINE);
        TimelineReader basenameTimelineReader = null;
        try {
            basenameTimelineReader = new TimelineReader(basenameTimelineFilename);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {

            TimelineWriter vqTimeline = null;
            int globNumParams = 0;
            int globSampleRate = 0;
            float frameSkip = 0;
            long basenameStart = 0;

            for (int b = 0; b < basenameList.getLength(); b++) {
                String basename = basenameList.getName(b);

                String vqFile = getProp(VQDIR) + basename + vqExt;
                VoiceQuality vq = new VoiceQuality(vqFile);

                if (b == 0) { // first vq file is used to initialize the timelinewriter
                    globNumParams = vq.params.dimension;
                    globSampleRate = vq.params.samplingRate;
                    frameSkip = vq.params.skipsize;

                    /* An example of processing header: */
                    Properties props1 = new Properties();
                    String hdrCmdLine = "\n$ESTDIR/bin/sig2fv "
                            + "-window_type hamming -factor 2.5 -otype est_binary -coefs melcep -melcep_order 12 -fbank_order 24 -shift 0.01 -preemph 0.97 "
                            + "-pm PITCHMARKFILE.pm -o melcepDir/mcepFile.mcep WAVDIR/WAVFILE.wav\n";
                    props1.setProperty("command", hdrCmdLine);
                    props1.setProperty("mcep.order", String.valueOf(globNumParams));
                    props1.setProperty("mcep.min", String.valueOf(0));
                    props1.setProperty("mcep.range", String.valueOf(1));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    props1.store(baos, null);
                    String processingHeader = baos.toString("latin1");

                    // initialize VQ TimelineWriter with bogus processingHeader, otherwise timeline cannot be loaded again...
                    vqTimeline = new TimelineWriter(getProp(VQTIMELINE), processingHeader, globSampleRate, frameSkip);
                }

                Datagram basenameDatagram = basenameTimelineReader.getDatagram(basenameStart);
                long basenameEnd = basenameStart + basenameDatagram.getDuration();

                int samplesPerFrame = (int) (globSampleRate * frameSkip);
                long frameStart = -1;
                long frameEnd = -1;
                long frameDuration = samplesPerFrame;

                for (int f = 0; f < vq.params.numfrm; f++) {
                    frameStart = basenameStart + f * samplesPerFrame;
                    frameEnd = frameStart + samplesPerFrame - 1;

                    if (frameStart < basenameEnd) {
                        if (frameEnd > basenameEnd) { // last VQ frame is shortened to end at basename datagram end (if ever)
                            frameDuration = frameEnd - basenameEnd;
                        }

                        float[] vqData = new float[vq.params.dimension];
                        for (int p = 0; p < vq.params.dimension; p++) {
                            vqData[p] = (float) vq.vq[p][f];
                        }

                        MCepDatagram vqDatagram = new MCepDatagram(frameDuration, vqData);
                        vqTimeline.feed(vqDatagram, globSampleRate);
                    } else {
                        continue; // VQ frames starting past the end of the basename datagram are dropped (although this shouldn't
                                  // happen)
                    }
                }

                // on the other hand, if the last frame ends before the end of the basename, insert a dummy datagram into the VQ
                // timeline so that the timeline can be synchronized with other ones
                if (basenameEnd > frameEnd) {
                    frameDuration = basenameEnd - frameEnd;
                    float[] vqData = new float[vq.params.dimension];
                    for (int p = 0; p < vq.params.dimension; p++) {
                        vqData[p] = Float.NaN;
                    }
                    MCepDatagram vqDatagram = new MCepDatagram(frameDuration, vqData);
                    vqTimeline.feed(vqDatagram, globSampleRate);

                    frameEnd += frameDuration;
                }

                percent = 100 * b / basenameList.getLength();
                System.out.println(basename);

                basenameStart = basenameEnd + 1; // set basenameStart to presumed start of next basename datagram
            }

            // close timeline
            vqTimeline.close();

            /* 7) Print some stats and close the file */
            System.out.println("---- VQ timeline result:");
            System.out.println("Number of files scanned: " + basenameList.getLength());
            System.out.println("Number of frames: [" + vqTimeline.getNumDatagrams() + "].");
            System.out.println("Size of the index: [" + vqTimeline.getIndex().getNumIdx() + "] ("
                    + (vqTimeline.getIndex().getNumIdx() * 16) + " bytes, i.e. "
                    + new DecimalFormat("#.##").format((double) (vqTimeline.getIndex().getNumIdx()) * 16.0 / 1048576.0)
                    + " megs).");
            System.out.println("---- VQ timeline done.");

        } catch (SecurityException e) {
            System.err.println("Error: you don't have write access to the target database directory.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
        }

        return (true);
    }

    /**
     * Provide the progress of computation, in percent, or -1 if that feature is not implemented.
     * 
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public int getProgress() {
        return percent;
    }

}
