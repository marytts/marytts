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
package marytts.util.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Vector;

/**
 * The BasenameList class produces and stores an alphabetically-sorted array of basenames issued from the .wav files present in a
 * given directory.
 * 
 * @author sacha
 * 
 */
public class BasenameList {
	private Vector bList = null;
	private String fromDir = null;
	private String fromExt = null;
	private boolean hasChanged;
	private static final int DEFAULT_INCREMENT = 128;

	/****************/
	/* CONSTRUCTORS */
	/****************/

	/**
	 * Default constructor for an empty list.
	 */
	public BasenameList() {
		fromDir = null;
		fromExt = null;
		bList = new Vector(DEFAULT_INCREMENT, DEFAULT_INCREMENT);
		hasChanged = false;
	}

	/**
	 * Default constructor from an existing vector and fields.
	 * 
	 * @param setFromDir
	 *            setFromDir
	 * @param setFromExt
	 *            setFromExt
	 * @param setVec
	 *            setVec
	 */
	public BasenameList(String setFromDir, String setFromExt, Vector setVec) {
		fromDir = setFromDir;
		fromExt = setFromExt;
		bList = setVec;
		hasChanged = false;
	}

	/**
	 * Constructor from an array of strings.
	 * 
	 * @param str
	 *            str
	 */
	public BasenameList(String[] str) {
		fromDir = null;
		fromExt = null;
		bList = new Vector(DEFAULT_INCREMENT, DEFAULT_INCREMENT);
		add(str);
		hasChanged = false;
	}

	/**
	 * This constructor lists the . extension files from directory dir, and initializes an an array with their list of
	 * alphabetically sorted basenames.
	 * 
	 * @param dirName
	 *            The name of the directory to list the files from.
	 * @param extension
	 *            The extension of the files to list.
	 * 
	 */
	public BasenameList(String dirName, final String extension) {
		fromDir = dirName;
		if (extension.indexOf(".") != 0)
			fromExt = "." + extension; // If the dot was not included, add it.
		else
			fromExt = extension;
		/* Turn the directory name into a file, to allow for checking and listing */
		File dir = new File(dirName);
		/* Check if the directory exists */
		if (!dir.exists()) {
			throw new RuntimeException("Directory [" + dirName + "] does not exist. Can't find the [" + extension + "] files.");
		}
		/* List the .extension files */
		File[] selectedFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(extension);
			}
		});

		/* Sort the file names alphabetically */
		Arrays.sort(selectedFiles);

		/* Extract the basenames and store them in a vector of strings */
		bList = new Vector(selectedFiles.length, DEFAULT_INCREMENT);
		String str = null;
		int subtractFromFilename = extension.length();
		for (int i = 0; i < selectedFiles.length; i++) {
			str = selectedFiles[i].getName().substring(0, selectedFiles[i].getName().length() - subtractFromFilename);
			add(str);
		}
		hasChanged = false;
	}

	/**
	 * This constructor loads the basename list from a random access file.
	 * 
	 * @param fileName
	 *            The file to read from.
	 * @throws IOException
	 *             IOException
	 */
	public BasenameList(String fileName) throws IOException {
		load(fileName);
		hasChanged = false;
	}

	/*****************/
	/* I/O METHODS */
	/*****************/

	/**
	 * Write the basenameList to a file, identified by its name.
	 * 
	 * @param fileName
	 *            fileName
	 * @throws IOException
	 *             IOException
	 */
	public void write(String fileName) throws IOException {
		write(new File(fileName));
	}

	/**
	 * Write the basenameList to a File.
	 * 
	 * @param file
	 *            file
	 * @throws IOException
	 *             IOException
	 */
	public void write(File file) throws IOException {
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), true);
		if (fromDir != null) {
			pw.println("FROM: " + fromDir + "*" + fromExt);
		}
		String str = null;
		for (int i = 0; i < bList.size(); i++) {
			str = (String) (bList.elementAt(i));
			pw.println(str);
		}
	}

	/**
	 * Read the basenameList from a file
	 * 
	 * @param fileName
	 *            fileName
	 * @throws IOException
	 *             IOException
	 */
	public void load(String fileName) throws IOException {
		/* Open the file */
		BufferedReader bfr = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
		/* Make the vector */
		if (bList == null)
			bList = new Vector(DEFAULT_INCREMENT, DEFAULT_INCREMENT);
		/* Check if the first line contains the origin information (directory+ext) */
		String line = bfr.readLine();
		if (line.indexOf("FROM: ") != -1) {
			line = line.substring(6);
			String[] parts = new String[2];
			parts = line.split("\\*", 2);
			fromDir = parts[0];
			fromExt = parts[1];
		} else if (!(line.matches("^\\s*$")))
			add(line);
		/* Add the lines to the vector, ignoring the blank ones. */
		while ((line = bfr.readLine()) != null) {
			if (!(line.matches("^\\s*$")))
				add(line);
		}
	}

	/*****************/
	/* OTHER METHODS */
	/*****************/

	/**
	 * Adds a basename to the list.
	 * 
	 * @param str
	 *            str
	 */
	public void add(String str) {
		if (!bList.contains(str))
			bList.add(str);
		hasChanged = true;
	}

	/**
	 * Adds an array of basenames to the list.
	 * 
	 * @param str
	 *            str
	 */
	public void add(String[] str) {
		for (int i = 0; i < str.length; i++)
			add(str[i]);
		hasChanged = true;
	}

	/**
	 * Removes a basename from the list, if it was present.
	 * 
	 * @param str
	 *            The basename to remove.
	 * @return true if the list was containing the basename.
	 */
	public boolean remove(String str) {
		hasChanged = true;
		return (bList.remove(str));
	}

	/**
	 * Removes a list from another list.
	 * 
	 * @param bnl
	 *            The basename list to remove.
	 * @return true if the list was containing any element of the list to remove.
	 */
	public boolean remove(BasenameList bnl) {
		boolean ret = true;
		for (int i = 0; i < bnl.getLength(); i++) {
			bList.remove(bnl.getName(i));
		}
		hasChanged = true;
		return (ret);
	}

	/**
	 * Duplicates the list (i.e., emits an autonomous copy of it).
	 * 
	 * @return BasenameList (fromdir, fromext, (vector) (bList.clone)
	 */
	public BasenameList duplicate() {
		return (new BasenameList(this.fromDir, this.fromExt, (Vector) (this.bList.clone())));
	}

	/**
	 * Returns an autonomous sublist between fromIndex, inclusive, and toIndex, exclusive.
	 * 
	 * @param fromIndex
	 *            fromIndex
	 * @param toIndex
	 *            toIndex
	 * @return basenameList(this.fromDir, this.fromExt, subVec)
	 */
	public BasenameList subList(int fromIndex, int toIndex) {
		Vector subVec = new Vector(toIndex - fromIndex, DEFAULT_INCREMENT);
		for (int i = fromIndex; i < toIndex; i++)
			subVec.add(this.getName(i));
		return (new BasenameList(this.fromDir, this.fromExt, subVec));
	}

	/**
	 * An accessor for the list of basenames, returned as an array of strings
	 * 
	 * @return string ret
	 */
	public String[] getListAsArray() {
		String[] ret = new String[this.getLength()];
		ret = (String[]) bList.toArray(ret);
		return ((String[]) (ret));
	}

	/**
	 * Another accessor for the list of basenames, returned as a vector of strings
	 * 
	 * @return bList
	 */
	public Vector getListAsVector() {
		return (bList);
	}

	/**
	 * An accessor for the list's length
	 * 
	 * @return bList.size
	 */
	public int getLength() {
		return (bList.size());
	}

	/**
	 * An accessor for the original directory. Returns null if the original directory is undefined.
	 * 
	 * @return fromDir
	 */
	public String getDir() {
		return (fromDir);
	}

	/**
	 * An accessor for the original extension. Returns null if the original extension is undefined.
	 * 
	 * @return fromExt
	 */
	public String getExt() {
		return (fromExt);
	}

	/**
	 * Return a copy of the basename at index i.
	 * 
	 * @param i
	 *            The index of the basename to consider.
	 * @return The corresponding basename.
	 */
	public String getName(int i) {
		return (String) bList.elementAt(i);
	}

	/**
	 * Check if the given basename is part of the list.
	 * 
	 * @param str
	 *            The basename to check for.
	 * @return true if yes, false if no.
	 */
	public boolean contains(String str) {
		return (bList.contains(str));
	}

	/**
	 * Check if the list contains another given one.
	 * 
	 * @param bnl
	 *            The list of basenames to check for.
	 * @return true if yes, false if no.
	 */
	public boolean contains(BasenameList bnl) {
		/* The list cannot contain a bigger one: */
		if (bnl.getLength() > this.getLength())
			return (false);
		for (int i = 0; i < bnl.getLength(); i++) {
			if (!this.contains(bnl.getName(i)))
				return (false);
		}
		return (true);
	}

	/**
	 * Check if two lists are equal.
	 * 
	 * @param bnl
	 *            The list of basenames to check for.
	 * @return true if yes, false if no.
	 */
	public boolean equals(BasenameList bnl) {
		if (bnl.getLength() != this.getLength())
			return (false);
		for (int i = 0; i < bnl.getLength(); i++) {
			if (!this.contains(bnl.getName(i)))
				return (false);
		}
		return (true);
	}

	/**
	 * Ensure that the list is alphabetically sorted.
	 * 
	 */
	public void sort() {
		String[] str = getListAsArray();
		Arrays.sort(str);
		bList.removeAllElements();
		add(str);
		hasChanged = true;
	}

	/**
	 * Clear the list.
	 * 
	 */
	public void clear() {
		fromDir = null;
		fromExt = null;
		bList.removeAllElements();
		hasChanged = true;
	}

	public boolean hasChanged() {
		return hasChanged;
	}

}