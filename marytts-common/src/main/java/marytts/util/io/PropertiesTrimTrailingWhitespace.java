package marytts.util.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

/**
 * extends properties class to allow trimming of trailing whitespace from input streams
 * 
 * @author Tristan
 * 
 */
public class PropertiesTrimTrailingWhitespace extends Properties {
	/**
	 * removes trailing whitespace
	 */
	public void load(InputStream fis) throws IOException {
		Scanner in = new Scanner(fis);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		while (in.hasNext()) {
			out.write(in.nextLine().trim().getBytes());
			out.write("\n".getBytes());
		}
		in.close();
		InputStream is = new ByteArrayInputStream(out.toByteArray());
		super.load(is);
	}
}
