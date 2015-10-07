/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package marytts.util.data;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import marytts.exceptions.MaryConfigurationException;

/**
 * Common helper class to read/write a standard Mary header to/from the various Mary data files.
 * 
 * @author sacha
 * 
 */
public class MaryHeader {
	/* Global constants */
	private final static int MAGIC = 0x4d415259; // "MARY"
	private final static int VERSION = 40; // 4.0

	/* List of authorized file type identifier constants */
	public final static int UNKNOWN = 0;
	public final static int CARTS = 100;
	public final static int DIRECTED_GRAPH = 110;
	public final static int UNITS = 200;
	public final static int LISTENERUNITS = 225;
	public final static int UNITFEATS = 300;
	public final static int LISTENERFEATS = 325;
	public final static int HALFPHONE_UNITFEATS = 301;
	public final static int JOINFEATS = 400;
	public final static int SCOST = 445;
	public final static int PRECOMPUTED_JOINCOSTS = 450;
	public final static int TIMELINE = 500;

	/* Private fields */
	private int magic = MAGIC;
	private int version = VERSION;
	private int type = UNKNOWN;

	// STATIC CODE

	/**
	 * For the given file, look inside and determine the file type.
	 * 
	 * @param fileName
	 *            file name
	 * @return the file type, or -1 if the file does not have a valid MARY header.
	 * @throws IOException
	 *             if the file cannot be read
	 */
	public static int peekFileType(String fileName) throws IOException {
		DataInputStream dis = null;
		dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
		/* Load the Mary header */
		try {
			MaryHeader hdr = new MaryHeader(dis);
			int type = hdr.getType();
			return type;
		} catch (MaryConfigurationException e) {
			// not a valid MARY header
			return -1;
		} finally {
			dis.close();
		}

	}

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Consruct a MaryHeader from scratch.
	 * 
	 * Fundamental guarantee: after construction, the MaryHeader has a valid magic number and a valid type.
	 * 
	 * @param newType
	 *            The type of MaryHeader to create. See public final constants in this class.
	 * 
	 * @throws IllegalArgumentException
	 *             if the input type is unknown.
	 */
	public MaryHeader(int newType) {
		if ((newType > TIMELINE) || (newType < UNKNOWN)) {
			throw new IllegalArgumentException("Unauthorized Mary file type [" + type + "].");
		}
		type = newType;

		// post-conditions:
		assert version == VERSION;
		assert hasLegalMagic();
		assert hasLegalType();
	}

	/**
	 * Construct a MaryHeader by reading from a file. Fundamental guarantee: after construction, the MaryHeader has a valid magic
	 * number and a valid type.
	 * 
	 * @param input
	 *            a DataInputStream or RandomAccessFile to read the header from.
	 * 
	 * @throws MaryConfigurationException
	 *             if no mary header can be read from input.
	 */
	public MaryHeader(DataInput input) throws MaryConfigurationException {
		try {
			this.load(input);
		} catch (IOException e) {
			throw new MaryConfigurationException("Cannot load mary header", e);
		}
		if (!hasLegalMagic() || !hasLegalType()) {
			throw new MaryConfigurationException("Ill-formed Mary header!");
		}

		// post-conditions:
		assert hasLegalMagic();
		assert hasLegalType();
	}

	/**
	 * Construct a MaryHeader by reading from a file. Fundamental guarantee: after construction, the MaryHeader has a valid magic
	 * number and a valid type.
	 * 
	 * @param input
	 *            a byte buffer to read the header from.
	 * 
	 * @throws MaryConfigurationException
	 *             if no mary header can be read from input.
	 */
	public MaryHeader(ByteBuffer input) throws MaryConfigurationException {
		try {
			this.load(input);
		} catch (BufferUnderflowException e) {
			throw new MaryConfigurationException("Cannot load mary header", e);
		}
		if (!hasLegalMagic() || !hasLegalType()) {
			throw new MaryConfigurationException("Ill-formed Mary header!");
		}

		// post-conditions:
		assert hasLegalMagic();
		assert hasLegalType();
	}

	/*****************/
	/* OTHER METHODS */
	/*****************/

	/**
	 * Mary header writer
	 * 
	 * @param output
	 *            The DataOutputStream or RandomAccessFile to write to
	 * 
	 * @return the number of written bytes.
	 * 
	 * @throws IOException
	 *             if the file type is unknown.
	 */
	public long writeTo(DataOutput output) throws IOException {

		long nBytes = 0;

		assert this.hasLegalType() : "Unknown Mary file type [" + type + "].";

		output.writeInt(magic);
		nBytes += 4;
		output.writeInt(version);
		nBytes += 4;
		output.writeInt(type);
		nBytes += 4;

		return (nBytes);
	}

	/**
	 * Load a mary header.
	 * 
	 * @param input
	 *            The data input (DataInputStream or RandomAccessFile) to read from.
	 * 
	 * @throws IOException
	 *             if the header data cannot be read
	 */
	private void load(DataInput input) throws IOException {

		magic = input.readInt();
		version = input.readInt();
		type = input.readInt();
	}

	/**
	 * Load a mary header.
	 * 
	 * @param input
	 *            the byte buffer from which to read the mary header.
	 * @throws BufferUnderflowException
	 *             if the header data cannot be read
	 */
	private void load(ByteBuffer input) {
		magic = input.getInt();
		version = input.getInt();
		type = input.getInt();
	}

	/* Accessors */
	public int getMagic() {
		return (magic);
	}

	public int getVersion() {
		return (version);
	}

	public int getType() {
		return (type);
	}

	/* Checkers */
	public boolean hasCurrentVersion() {
		return (version == VERSION);
	}

	private boolean hasLegalType() {
		return (type <= TIMELINE) && (type > UNKNOWN);
	}

	private boolean hasLegalMagic() {
		return (magic == MAGIC);
	}

}
