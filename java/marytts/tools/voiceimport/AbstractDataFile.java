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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import marytts.unitselection.data.Datagram;

/**
 * Abstract class to wrap a data file in a manner suitable for feeding it as {@link Datagram}s into an
 * {@link AbstractTimelineMaker}.
 * 
 * @author steiner
 * 
 */
abstract class AbstractDataFile {

    protected int sampleRate;

    protected float frameSkip;

    protected int numFrames;

    protected int frameDuration;

    protected ArrayList<DataFrame> dataFrames;

    /**
     * main constructor
     * 
     * @param file
     *            to load
     */
    public AbstractDataFile(File file) {
        load(file);
    }

    /**
     * load the File; only extension classes know how to do this
     * 
     * @param file
     *            to load
     */
    protected abstract void load(File file);

    /**
     * get the sample rate (in Hz)
     * 
     * @return the sampleRate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * get the frame skip (in seconds)
     * 
     * @return the frameSkip
     */
    public float getFrameSkip() {
        return frameSkip;
    }

    /**
     * get array of {@link Datagram}s constructed from the frames of this; no special requirements for their total duration
     * 
     * @return Datagrams array
     * @throws IOException
     */
    public Datagram[] getDatagrams() throws IOException {
        int expectedTotalDuration = frameDuration * numFrames;
        return getDatagrams(expectedTotalDuration);
    }

    /**
     * get array of {@link Datagram}s constructed from the frames of this; if the total duration of all Datagrams is <i>longer</i>
     * than <b>forcedDuration</b>, excess Datagrams will be silently dropped, and the duration of the last included one is
     * shortened to match forcedDuration. If the total duration of all Datagrams is <i>shorter</i> than <b>forcedDuration</b>, a
     * final "filler" Datagram is appended which contains no data, but whose duration increases the total duration to match
     * <b>forcedDuration</b>.
     * 
     * @param forcedDuration
     *            of all Datagrams (in samples)
     * @return Datagrams array
     * @throws IOException
     */
    public Datagram[] getDatagrams(int forcedDuration) throws IOException {
        // initialize datagrams as a List:
        ArrayList<Datagram> datagrams = new ArrayList<Datagram>(numFrames);
        int durationMismatch = forcedDuration;

        // iterate over all data frames:
        for (DataFrame dataFrame : dataFrames) {
            durationMismatch -= dataFrame.duration;
            if (durationMismatch < 0) {
                // if forcedDuration is exceeded, adjust the final frame's duration and break out of loop:
                dataFrame.duration += durationMismatch;
                datagrams.add(dataFrame.toDatagram());
                break;
            }
            datagrams.add(dataFrame.toDatagram());
        }

        // if total duration of all Datagrams is less than forcedDuration, pad with empty filler:
        if (durationMismatch > 0) {
            byte[] nothing = new byte[] {};
            Datagram filler = new Datagram(durationMismatch, nothing);
            datagrams.add(filler);
        }

        // return datagrams as array:
        Datagram[] datagramArray = datagrams.toArray(new Datagram[datagrams.size()]);
        return datagramArray;
    }

    /**
     * Class to wrap a data frame and convert between this and a {@link Datagram}.
     * 
     * @author steiner
     * 
     */
    protected class DataFrame {

        private int duration;

        private float[] data;

        /**
         * main constructor
         * 
         * @param duration
         *            in samples
         * @param data
         *            in this frame
         */
        public DataFrame(int duration, float[] data) {
            this.duration = duration;
            this.data = data;
        }

        /**
         * constructor to convert from a {@link Datagram}
         * 
         * @param datagram
         *            to wrap
         * @throws IOException
         * @throws ClassNotFoundException
         */
        public DataFrame(Datagram datagram) throws IOException, ClassNotFoundException {
            this.duration = (int) datagram.getDuration();
            byte[] byteArray = datagram.getData();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
            ObjectInputStream input = new ObjectInputStream(inputStream);
            this.data = (float[]) input.readObject();
        }

        /**
         * provide this data frame as a {@link Datagram}
         * 
         * @return Datagram
         * @throws IOException
         */
        public Datagram toDatagram() throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream output = new ObjectOutputStream(outputStream);
            output.writeObject(data);
            byte[] byteArray = outputStream.toByteArray();
            Datagram datagram = new Datagram((long) duration, byteArray);
            return datagram;
        }
    }
}
