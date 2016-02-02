package marytts.client;

import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;

/**
 * Using an {@link OutputStream} in {@link MaryClient#process(String, String, String, String, String, String, OutputStream)} or
 * {@link MaryClient#process(String, String, String, String, String, String, OutputStream, long)} does not provide any hints on
 * the audio format that is being sent by mary. This poses difficulties in playing back the audio since the actual format depends
 * on the voice used.
 * <p>
 * This class encapsulates an {@link OutputStream} that would be used to carry the audio data as well as the {@link AudioFormat}of
 * that data.
 * <p>
 * It is also possible to extend this class and create a new internally used {@link OutputStream} once
 * {@link #setFormat(AudioFormat)} has been called.
 * <p>
 * An example usage might be
 * 
 * <pre>
 * MaryClient processor = MaryClient.getMaryClient();
 * ByteArrayOutputStream out = new ByteArrayOutputStream();
 * AudioFormatOutputStream afos = new AudioFormatOutputStream(out);
 * processor.process("this is a test", "TEXT", "AUDIO", "en-US",
 *     "WAVE", "cmu-slt-hsmm", afos, 5000);
 * AudioFormat format = afos.getFormat());
 * </pre>
 * 
 * @author Dirk Schnelle-Walka
 *
 */
public class AudioFormatOutputStream extends OutputStream {
	private OutputStream out;
	private AudioFormat format;

	protected AudioFormatOutputStream() {
	}

	public AudioFormatOutputStream(OutputStream out) {
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
	}

	public AudioFormat getFormat() {
		return format;
	}

	public void setFormat(AudioFormat format) throws IOException {
		this.format = format;
	}

	protected void setOutputStream(OutputStream out) {
		this.out = out;
	}

	public OutputStream getOutputStream() {
		return out;
	}
}
