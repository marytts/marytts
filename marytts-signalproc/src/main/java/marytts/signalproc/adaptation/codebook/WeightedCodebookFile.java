/**
 * Copyright 2007 DFKI GmbH.
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
package marytts.signalproc.adaptation.codebook;

import java.io.FileNotFoundException;
import java.io.IOException;

import marytts.util.io.FileUtils;
import marytts.util.io.MaryRandomAccessFile;

/**
 * 
 * A class for handling file I/O of binary weighted codebook files
 * 
 * @author Oytun T&uuml;rk
 */
public class WeightedCodebookFile {
	int status;
	public static int NOT_OPENED = -1;
	public static int OPEN_FOR_READ = 0;
	public static int OPEN_FOR_WRITE = 1;

	public MaryRandomAccessFile stream;
	public String currentFile;

	public static final String DEFAULT_EXTENSION = ".wcf";

	public WeightedCodebookFile() {
		this("");
	}

	public WeightedCodebookFile(String codebookFile) {
		this(codebookFile, NOT_OPENED);
	}

	public WeightedCodebookFile(String codebookFile, int desiredStatus) {
		init(codebookFile, desiredStatus);
	}

	private void init(String codebookFile, int desiredStatus) {
		status = NOT_OPENED;
		stream = null;
		currentFile = "";

		if (desiredStatus == OPEN_FOR_READ) {
			status = desiredStatus;
			try {
				stream = new MaryRandomAccessFile(codebookFile, "r");
				currentFile = codebookFile;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (desiredStatus == OPEN_FOR_WRITE) {
			FileUtils.delete(codebookFile);

			status = desiredStatus;
			try {
				stream = new MaryRandomAccessFile(codebookFile, "rw");
				currentFile = codebookFile;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void close() {
		if (status != NOT_OPENED) {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			stream = null;
			status = NOT_OPENED;
		}
	}

	public WeightedCodebookFileHeader readCodebookHeader(String codebookFile, boolean bCloseAfterReading) {
		init(codebookFile, OPEN_FOR_READ);
		return readCodebookHeader();
	}

	public WeightedCodebookFileHeader readCodebookHeader() {
		try {
			return readCodebookHeader(stream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static WeightedCodebookFileHeader readCodebookHeader(MaryRandomAccessFile ler) throws IOException {
		WeightedCodebookFileHeader header = new WeightedCodebookFileHeader();

		header.read(ler);

		return header;
	}

	public MaryRandomAccessFile writeCodebookHeader(String codebookFile, WeightedCodebookFileHeader header) {
		init(codebookFile, OPEN_FOR_WRITE);
		return writeCodebookHeader(header);
	}

	public MaryRandomAccessFile writeCodebookHeader(WeightedCodebookFileHeader header) {
		try {
			writeCodebookHeader(stream, header);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return stream;
	}

	public void writeCodebookHeader(MaryRandomAccessFile ler, WeightedCodebookFileHeader header) throws IOException {
		header.write(ler);
	}

	public WeightedCodebook readCodebookFile() throws IOException {
		return readCodebookFile(currentFile);
	}

	// Read whole codebook from a file into memory
	public WeightedCodebook readCodebookFile(String codebookFile) throws IOException {
		WeightedCodebook codebook = null;

		if (FileUtils.exists(codebookFile)) {
			if (status != OPEN_FOR_READ) {
				if (status != NOT_OPENED)
					close();

				init(codebookFile, OPEN_FOR_READ);
			}

			if (status == OPEN_FOR_READ) {
				codebook = new WeightedCodebook();

				codebook.header = readCodebookHeader();

				readCodebookFileExcludingHeader(codebook);
			}
		}

		return codebook;
	}

	public void readCodebookFileExcludingHeader(WeightedCodebook codebook) {
		codebook.allocate();

		System.out.println("Reading codebook file: " + currentFile + "...");

		int i;
		for (i = 0; i < codebook.header.totalEntries; i++)
			codebook.entries[i] = readEntry(codebook.header.lsfParams.dimension, codebook.header.mfccParams.dimension);

		close();

		System.out.println("Reading completed...");
	}

	public void WriteCodebookFile(String codebookFile, WeightedCodebook codebook) {
		if (status != OPEN_FOR_WRITE) {
			if (status != NOT_OPENED)
				close();

			init(codebookFile, OPEN_FOR_WRITE);
		}

		codebook.header.totalEntries = codebook.entries.length;

		writeCodebookHeader(codebookFile, codebook.header);

		int i;
		for (i = 0; i < codebook.header.totalEntries; i++)
			writeEntry(codebook.entries[i]);

		close();
	}

	// Append a new codebook entry to a codebook file opened with write permission
	public void writeEntry(WeightedCodebookEntry w) {
		if (status != OPEN_FOR_WRITE) {
			if (status != NOT_OPENED)
				close();

			init(currentFile, OPEN_FOR_WRITE);
		}

		if (status == OPEN_FOR_WRITE) {
			w.write(stream);
			incrementTotalEntries();
		}
	}

	// Read a codebook entry from a codebook file opened with read permission
	public WeightedCodebookEntry readEntry(int lpOrder, int mfccDimension) {
		WeightedCodebookEntry w = new WeightedCodebookEntry();

		if (status != OPEN_FOR_READ) {
			if (status != NOT_OPENED)
				close();

			init(currentFile, OPEN_FOR_READ);
		}

		if (status == OPEN_FOR_READ)
			w.read(stream, lpOrder, mfccDimension);

		return w;
	}

	//

	public void incrementTotalEntries() {
		if (status == OPEN_FOR_WRITE) {
			try {
				long currentPos = stream.getFilePointer();
				stream.seek(0);
				int totalEntries = stream.readInt();
				totalEntries++;
				stream.seek(0);
				stream.writeInt(totalEntries);
				System.out.println("Wrote codebook entry " + String.valueOf(totalEntries));
				stream.seek(currentPos);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
