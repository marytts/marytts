package marytts.util.data;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import javax.sound.sampled.AudioInputStream;

import marytts.htsengine.HTSVocoder;
import marytts.signalproc.process.InlineDataProcessor;
import marytts.util.data.audio.DDSAudioInputStream;

import org.junit.Before;
import org.junit.Test;

public class ProducingDoubleDataSourceTest {
    


    @Test
    public void canReadZeroDoubles() {
        TestProducer producer = new TestProducer(0);
        producer.start();
        assertEquals(0, producer.available());
        assertEquals(0, producer.getDataLength());
        double[] data = producer.getAllData();
        assertEquals(0, data.length);
    }
    
    @Test
    public void canReadZeroFrames() throws IOException {
        TestProducer producer = new TestProducer(0);
        producer.start();
        AudioInputStream ais = new DDSAudioInputStream(producer, HTSVocoder.getHTSAudioFormat());
        assertEquals(0, ais.getFrameLength());
    }


    @Test
    public void canReadTenDoubles() {
        int numDoubles = 10;
        TestProducer producer = new TestProducer(numDoubles);
        producer.start();
        assertEquals(numDoubles, producer.getDataLength());
        double[] data = producer.getAllData();
        assertEquals(numDoubles, data.length);
    }

    @Test
    public void canRead4000Doubles() {
        int numDoubles = 4000;
        TestProducer producer = new TestProducer(numDoubles);
        producer.start();
        assertEquals(numDoubles, producer.getDataLength());
        double[] data = producer.getAllData();
        assertEquals(numDoubles, data.length);
    }

    
    @Test
    public void canReadTenFrames() throws Exception {
        int numDoubles = 10;
        TestProducer producer = new TestProducer(numDoubles);
        producer.start();
        AudioInputStream ais = new DDSAudioInputStream(producer, HTSVocoder.getHTSAudioFormat());
        assertEquals(numDoubles, ais.getFrameLength());
        int bytesRead = ais.read(new byte[16000]);
        assertEquals(2*numDoubles, bytesRead);
        
    }

    @Test
    public void canRead4000Frames() throws Exception {
        int numDoubles = 4000;
        TestProducer producer = new TestProducer(numDoubles);
        producer.start();
        AudioInputStream ais = new DDSAudioInputStream(producer, HTSVocoder.getHTSAudioFormat());
        assertEquals(numDoubles, ais.getFrameLength());
        int bytesRead = ais.read(new byte[16000]);
        assertEquals(2*numDoubles, bytesRead);
    }
    
    @Test
    public void willApplyInlineProcessor() {
        InlineDataProcessor halver = new InlineDataProcessor() {
            public void applyInline(double[] data, int off, int len) {
                for (int i=off, max=off+len; i<max; i++) {
                    data[i] *= .5;
                }
            }
        };
        int numDoubles = 100;
        TestProducer producer = new TestProducer(numDoubles, halver);
        producer.start();
        double[] result = producer.getAllData();
        for (int i=0; i<numDoubles; i++) {
            assertEquals(producer.DUMMY * .5, result[i], 1.e-10);
        }
    }
    
    

    private static class TestProducer extends ProducingDoubleDataSource {
        double DUMMY = 0.23;
        
        public TestProducer(int numToSend) {
            super(numToSend);
        }
        
        public TestProducer(int numToSend, InlineDataProcessor dataProcessor) {
            super(numToSend, dataProcessor);
        }

        


        public void run() {
            long numToSend = getDataLength();
            while (numToSend > 0) {
                putOneDataPoint(DUMMY);
                numToSend--;
            }
            putEndOfStream();
        }
        
    }
}
