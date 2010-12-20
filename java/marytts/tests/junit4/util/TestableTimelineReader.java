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
package marytts.tests.junit4.util;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import marytts.exceptions.MaryConfigurationException;
import marytts.tools.voiceimport.TimelineWriter;
import marytts.unitselection.data.Datagram;
import marytts.unitselection.data.TimelineReader;
import marytts.util.Pair;

/**
 * Provides the actual timeline test case for the timeline reading/writing symmetry.
 */
public class TestableTimelineReader extends TimelineReader {
    
    public TestableTimelineReader(String fileName, boolean tryMemoryMapping) throws Exception {
        super(fileName, tryMemoryMapping);
    }

    @Override
    public Pair<ByteBuffer, Long> getByteBufferAtTime(long targetTimeInSamples) throws IOException, BufferUnderflowException {
        return super.getByteBufferAtTime(targetTimeInSamples);
    }

    @Override
    public long skipNextDatagram(ByteBuffer bb) throws BufferUnderflowException {
        return super.skipNextDatagram(bb);
    }

    @Override
    public Datagram getNextDatagram(ByteBuffer bb) throws IOException, BufferUnderflowException {
        return super.getNextDatagram(bb);
    }

}

