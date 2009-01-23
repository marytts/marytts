/**
 * Copyright 2004-2006 DFKI GmbH.
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
package marytts.signalproc.filter;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.util.data.audio.AudioDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;


/**
 * @author Marc Schr&ouml;der
 *
 */
public class AudioFilterBase
{
    public AudioFilterBase() {}
    
    public AudioInputStream apply(AudioInputStream ais)
    {
        AudioDoubleDataSource adds = new AudioDoubleDataSource(ais);
        return new DDSAudioInputStream(adds, adds.getAudioFormat());
    }
    
    public static void main(String[] args) throws Exception
    {
        AudioFilterBase filter = new AudioFilterBase();
        for (int i=0; i<args.length; i++) {
            File f = new File(args[i]);
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            AudioInputStream result = filter.apply(ais);
            File outFile = new File(args[i]+"_filtered.wav");
            AudioSystem.write(result, AudioFileFormat.Type.WAVE, outFile);
        }
    }
}

