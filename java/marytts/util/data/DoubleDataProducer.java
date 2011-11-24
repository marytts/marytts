/**
 * Copyright 2011 Bill Cox. (The Mary project may have the copyright - just ask)
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

package marytts.util.data;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author bill
 *
 */
public interface DoubleDataProducer extends Runnable {
    
    public static final Double END_OF_STREAM = Double.NEGATIVE_INFINITY;
    
    // Called before starting the producer.  When the run thread is called,
    // it needs to write double data to this queue until all data is generated.
    public void setQueue(ArrayBlockingQueue<Double> queue);
    public boolean hasMoreData();
    
}
