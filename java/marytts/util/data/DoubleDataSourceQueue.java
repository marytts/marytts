/**
 * Copyright 2009 Bill Cox. (Note, the Mary project is welcome to own the copyright - just ask)
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

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * @author Bill Cox
 *
 */
public class DoubleDataSourceQueue extends BaseDoubleDataSource{

    DoubleDataProducer producer;
    ArrayBlockingQueue<Double> queue;
    double maxValue = 17000.0;
    
    public DoubleDataSourceQueue(DoubleDataProducer producer, int capacity) {
        this.producer = producer;
        queue = new ArrayBlockingQueue<Double>(capacity);
        producer.setQueue(queue);
        Thread producerThread = new Thread(producer);
        producerThread.start();
    }
    
    private double abs(double value)
    {
        if(value >= 0.0) {
            return value;
        }
        return -value;
    }

    public int getData(double[] target, int targetPos, int length)
    {
        for(int i=0; i < length; i++) {
            try {
                if(!producer.hasMoreData() && queue.isEmpty()) {
                    //System.out.printf("max volume: %f\n", maxValue); 
                    return i;
                }
                double value = queue.take();
                if (value == DoubleDataProducer.END_OF_STREAM) {
                    return i;
                }
                //System.out.printf("Got value: %f\n", value); 
                if(abs(value) > maxValue) {
                    maxValue = abs(value);
                }
                target[targetPos + i] = value/maxValue;
            } catch (InterruptedException e) {
                return i;
            }
        }
        return length;
    }

    /**
     * Whether or not any more data can be read from this data source.
     * @return true if another call to getData() will return data, false otherwise.
     */
    public boolean hasMoreData()
    {
        return producer.hasMoreData();
    }
}
