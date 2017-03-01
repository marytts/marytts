/**
 * Copyright 2000-2009 DFKI GmbH.
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
package marytts.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.sound.sampled.UnsupportedAudioFileException;

import marytts.util.MaryUtils;
import marytts.util.string.StringUtils;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;

/**
 * A collection of public static utility methods, doing file operations.
 * 
 * @author schroed
 *
 */
public class FileUtils {

	/*
	 * Close a socket and closeables. Use this in a finally clause. Exists because Sockets are only closeable in jdk 1.7.
	 * 
	 * @param socket to close.
	 * 
	 * @param closeables to close.
	 */
	public static void close(Socket socket, Closeable... closeables) {
		for (Closeable c : closeables) {
			if (c != null) {
				try {
					c.close();
				} catch (Exception ex) {
					MaryUtils.getLogger(FileUtils.class.getName()).log(Level.WARN, "Couldn't close Closeable.", ex);
				}
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception ex) {
				MaryUtils.getLogger(FileUtils.class.getName()).log(Level.WARN, "Couldn't close Socket.", ex);
			}
		}
	}

	/**
	 * Close a PreparedStatement and a series of result sets. Use this in a finally clause. While closing the PreparedStatement
	 * supposedly closes it's resultsets, i was told that some buggy implementations don't. Exists because PreparedStatement and
	 * ResultŜet are only closeable on jdk 1.7
	 * 
	 * @param ps
	 *            ps
	 * @param rs
	 *            rs
	 */
	public static void close(PreparedStatement ps, ResultSet... rs) {
		for (ResultSet c : rs) {
			if (c != null) {
				try {
					c.close();
				} catch (Exception ex) {
					MaryUtils.getLogger(FileUtils.class.getName()).log(Level.WARN, "Couldn't close ResultSet.", ex);
				}
			}
		}
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception ex) {
				MaryUtils.getLogger(FileUtils.class.getName()).log(Level.WARN, "Couldn't close PreparedStatement.", ex);
			}
		}
	}

	/**
	 * Close closeables. Use this in a finally clause.
	 * 
	 * @param closeables
	 *            closeables to close.
	 */
	public static void close(Closeable... closeables) {
		for (Closeable c : closeables) {
			if (c != null) {
				try {
					c.close();
				} catch (Exception ex) {
					MaryUtils.getLogger(FileUtils.class.getName()).log(Level.WARN, "Couldn't close Closeable.", ex);
				}
			}
		}
	}

	/**
	 * List the basenames of all files in directory that end in suffix, without that suffix. For example, if suffix is ".wav",
	 * return the names of all .wav files in the directory, but without the .wav extension. The file names are sorted in
	 * alphabetical order, according to java's string search.
	 * 
	 * @param directory
	 *            directory
	 * @param suffix
	 *            suffix
	 * @return filenames
	 */
	public static String[] listBasenames(File directory, String suffix) {
		final String theSuffix = suffix;
		String[] filenames = directory.list(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.endsWith(theSuffix);
			}
		});

		/* Sort the file names alphabetically */
		Arrays.sort(filenames);

		for (int i = 0; i < filenames.length; i++) {
			filenames[i] = filenames[i].substring(0, filenames[i].length() - suffix.length());
		}
		return filenames;
	}

	/**
	 * Read a file into a string, using the given encoding, and return that string.
	 * 
	 * @param file
	 *            file
	 * @param encoding
	 *            encoding
	 * @throws IOException
	 *             IOException
	 * @return stream as string(fis, encoding)
	 * @deprecated use {@link org.apache.commons.io.FileUtils#readFileToString(File, String)} instead
	 */
	@Deprecated
	public static String getFileAsString(File file, String encoding) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			return getStreamAsString(fis, encoding);
		} finally {
			fis.close();
		}
	}

	/**
	 * @deprecated use {@link org.apache.commons.io.IOUtils#toString(InputStream, String)} instead
	 * @param inputStream
	 *            inputStream
	 * @param encoding
	 *            encoding
	 * @throws IOException
	 *             IOException
	 * @return getReaderAsString
	 */
	@Deprecated
	public static String getStreamAsString(InputStream inputStream, String encoding) throws IOException {
		return getReaderAsString(new InputStreamReader(inputStream, encoding));
	}

	public static String getReaderAsString(Reader reader) throws IOException {
		StringWriter sw = new StringWriter();
		BufferedReader in = new BufferedReader(reader);
		char[] buf = new char[8192];
		int n;
		while ((n = in.read(buf)) > 0) {
			sw.write(buf, 0, n);
		}
		return sw.toString();

	}

	public static byte[] getFileAsBytes(String filename) throws IOException {
		return getFileAsBytes(new File(filename));
	}

	public static byte[] getFileAsBytes(File file) throws IOException {
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			long length = file.length();

			if (length > Integer.MAX_VALUE) {
				return null;
			}

			// Create the byte array to hold the data
			byte[] bytes = new byte[(int) length];

			// Read in the bytes
			int offset = 0;
			int numRead = 0;
			while ((offset < bytes.length) && ((numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)) {
				offset += numRead;
			}

			if (offset < bytes.length) {
				throw new IOException("Could not read file " + file.getName());
			}
			return bytes;
		} finally {
			close(is);
		}
	}

	public static void writeToTextFile(double[] array, String textFile) {
		FileWriter outFile = null;
		PrintWriter out = null;
		try {
			outFile = new FileWriter(textFile);
			out = new PrintWriter(outFile);

			for (int i = 0; i < array.length; i++) {
				out.println(String.valueOf(array[i]));
			}

		} catch (IOException e) {
			System.out.println("Error! Cannot create file: " + textFile);
		} finally {
			close(outFile, out);
		}
	}

	public static void writeBinaryFile(short[] x, String filename) throws IOException {
		DataOutputStream d = new DataOutputStream(new FileOutputStream(new File(filename)));

		d.writeInt(x.length);

		writeBinaryFile(x, d);
	}

	public static void writeBinaryFile(short[] x, DataOutputStream d) throws IOException {
		for (int i = 0; i < x.length; i++)
			d.writeShort(x[i]);
	}

	public static void writeBinaryFile(float[] x, String filename) throws IOException {
		DataOutputStream d = new DataOutputStream(new FileOutputStream(new File(filename)));

		d.writeInt(x.length);

		writeBinaryFile(x, d);
	}

	public static void writeBinaryFile(float[] x, DataOutputStream d) throws IOException {
		for (int i = 0; i < x.length; i++)
			d.writeFloat(x[i]);
	}

	public static void writeBinaryFile(double[] x, String filename) throws IOException {
		DataOutputStream d = new DataOutputStream(new FileOutputStream(new File(filename)));

		d.writeInt(x.length);

		writeBinaryFile(x, d);
	}

	public static void writeBinaryFile(double[] x, DataOutputStream d) throws IOException {
		for (int i = 0; i < x.length; i++)
			d.writeDouble(x[i]);
	}

	public static void writeBinaryFile(int[] x, String filename) throws IOException {
		DataOutputStream d = new DataOutputStream(new FileOutputStream(new File(filename)));

		d.writeInt(x.length);

		writeBinaryFile(x, d);
	}

	public static void writeBinaryFile(int[] x, DataOutputStream d) throws IOException {
		for (int i = 0; i < x.length; i++)
			d.writeInt(x[i]);
	}

	public static int[] readFromBinaryFile(String filename) throws IOException {
		DataInputStream d = null;
		try {
			d = new DataInputStream(new FileInputStream(new File(filename)));
			int[] x = null;
			int len = d.readInt();

			if (len > 0) {
				x = new int[len];

				for (int i = 0; i < len; i++) {
					x[i] = d.readInt();
				}
			}
			return x;
		} finally {
			close(d);
		}
	}

	public static boolean exists(String file) {
		boolean bRet = false;

		if (file != null) {
			File f = new File(file);
			if (f.exists()) {
				bRet = true;
			}
		}

		return bRet;
	}

	// Checks for the existence of file and deletes if existing
	public static void delete(String file, boolean bDisplayInfo) {
		boolean bRet = false;
		File f = new File(file);
		if (f.exists()) {
			bRet = f.delete();
		}

		if (!bRet) {
			System.out.println("Unable to delete file: " + file);
		} else {
			if (bDisplayInfo) {
				System.out.println("Deleted: " + file);
			}
		}
	}

	// Silent version
	public static void delete(String file) {
		if (exists(file)) {
			delete(file, false);
		}
	}

	public static void delete(String[] files, boolean bDisplayInfo) {
		for (int i = 0; i < files.length; i++) {
			delete(files[i], bDisplayInfo);
		}
	}

	// Silnet version
	public static void delete(String[] files) {
		delete(files, false);
	}

	public static void copy(String sourceFile, String destinationFile) throws IOException {
		File fromFile = new File(sourceFile);
		File toFile = new File(destinationFile);

		if (!fromFile.exists()) {
			throw new IOException("FileCopy: " + "no such source file: " + sourceFile);
		}
		if (!fromFile.isFile()) {
			throw new IOException("FileCopy: " + "can't copy directory: " + sourceFile);
		}
		if (!fromFile.canRead()) {
			throw new IOException("FileCopy: " + "source file is unreadable: " + sourceFile);
		}

		if (toFile.isDirectory()) {
			toFile = new File(toFile, fromFile.getName());
		}

		if (toFile.exists()) {
			if (!toFile.canWrite()) {
				throw new IOException("FileCopy: " + "destination file cannot be written: " + destinationFile);
			}
		}

		String parent = toFile.getParent();
		if (parent == null) {
			parent = System.getProperty("user.dir");
		}
		File dir = new File(parent);
		if (!dir.exists()) {
			throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
		}
		if (dir.isFile()) {
			throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
		}
		if (!dir.canWrite()) {
			throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
		}

		FileInputStream from = null;
		FileOutputStream to = null;
		try {
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1) {
				to.write(buffer, 0, bytesRead); // write
			}
		} finally {
			close(from, to);
		}
	}

	public static void copy(File source, File dest) throws IOException {
		FileChannel in = null, out = null;
		try {
			System.out.println("copying: " + source + "\n    --> " + dest);
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(dest).getChannel();
			MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
			out.write(buf);
		} catch (Exception e) {
			System.out.println("Error copying file " + source.getAbsolutePath() + " to " + dest.getAbsolutePath() + " : "
					+ e.getMessage());
			throw new IOException();
		} finally {
			FileUtils.close(in, out);
		}
	}

	public static void copyFolder(String sourceFolder, String targetFolder) throws IOException {
		copyFolder(sourceFolder, targetFolder, false);
	}

	public static void copyFolder(String sourceFolder, String targetFolder, boolean bForceDeleteTarget) throws IOException {
		if (exists(sourceFolder)) {
			if (exists(targetFolder) && bForceDeleteTarget)
				delete(targetFolder);

			createDirectory(targetFolder);

			if (exists(targetFolder)) {
				String[] fileList = FileUtils.getFileList(sourceFolder, "*.*");
				if (fileList != null) {
					for (int i = 0; i < fileList.length; i++) {
						String targetFile = StringUtils.checkLastSlash(targetFolder)
								+ StringUtils.getFileName(fileList[i], false);
						copy(fileList[i], targetFile);
					}
				}
			} else
				System.out.println("Could not create target folder!");
		} else
			System.out.println("Source folder does not exist!");

	}

	// this function does not copy files that start with .
	public static void copyFolderRecursive(String sourceFolder, String targetFolder, boolean bForceDeleteTarget)
			throws IOException {
		String fileSeparator = System.getProperty("file.separator");
		if (exists(sourceFolder)) {
			if (exists(targetFolder) && bForceDeleteTarget)
				delete(targetFolder);

			createDirectory(targetFolder);

			if (exists(targetFolder)) {
				String[] fileList = new File(sourceFolder).list();
				if (fileList != null) {
					for (int i = 0; i < fileList.length; i++) {
						if (!fileList[i].startsWith(".")) {
							String source = StringUtils.checkLastSlash(sourceFolder) + fileList[i];
							if (new File(source).isDirectory()) {
								String newTargetFolder = StringUtils.checkLastSlash(targetFolder) + fileList[i];
								copyFolderRecursive(source, newTargetFolder, bForceDeleteTarget);
							} else {
								String targetFile = StringUtils.checkLastSlash(targetFolder) + fileList[i];
								copy(source, targetFile);
							}
						}
					}
				}
			} else
				System.out.println("Could not create target folder!");
		} else
			System.out.println("Source folder does not exist!");

	}

	public static void createDirectory(String trainingBaseFolder) {
		File f = new File(trainingBaseFolder);
		if (!f.exists()) {
			f.mkdirs();
		}
	}

	public static boolean isDirectory(String dirName) {
		File f = new File(dirName);
		return f.isDirectory();
	}

	public static void rename(String existingFile, String newFilename) {
		if (exists(existingFile)) {
			File oldFile = new File(existingFile);
			oldFile.renameTo(new File(newFilename));
		}
	}

	// Renames all files
	public static void changeFileExtensions(String folder, String oldExt, String newExt) {
		String[] files = getFileNameList(folder, oldExt);

		folder = StringUtils.checkLastSlash(folder);
		newExt = StringUtils.checkFirstDot(newExt);

		if (files != null) {
			for (int i = 0; i < files.length; i++) {
				int ind = files[i].lastIndexOf(oldExt);
				String newFile = folder + files[i].substring(0, ind) + newExt;
				FileUtils.rename(files[i], newFile);
				System.out.println("Changed extension " + String.valueOf(i + 1) + " of " + String.valueOf(files.length));
			}
		}
	}

	/**
	 * Given a file name with path it return the file name
	 * 
	 * @param fileNameWithPath
	 *            fileNameWithPath
	 * @return str
	 */
	public static String getFileName(String fileNameWithPath) {
		String str;
		int i;

		i = fileNameWithPath.lastIndexOf("/");
		str = fileNameWithPath.substring(i + 1);

		return str;

	}

	// Gets filenames only without paths!
	public static String[] getFileNameList(String directory, String extension) {
		return getFileNameList(directory, extension, true);
	}

	// Gets filenames only without paths!
	public static String[] getFileNameList(String directory, String extension, boolean recurse) {
		File[] files = listFilesAsArray(new File(directory), new FileFilter(extension), recurse);
		String[] fileList = null;
		if (files != null && files.length > 0) {
			fileList = new String[files.length];
			for (int i = 0; i < files.length; i++) {
				fileList[i] = files[i].getName();
			}
		}

		return fileList;
	}

	// Gets filenames with full path
	public static String[] getFileList(String directory, String extension) {
		return getFileList(directory, extension, true);
	}

	// Gets filenames with full path
	public static String[] getFileList(String directory, String extension, boolean recurse) {
		File[] files = listFilesAsArray(new File(directory), new FileFilter(extension), recurse);
		String[] fileList = null;
		if (files != null && files.length > 0) {
			fileList = new String[files.length];
			for (int i = 0; i < files.length; i++) {
				fileList[i] = files[i].getAbsolutePath();
			}
		}

		return fileList;
	}

	public static File[] listFilesAsArray(File directory, FilenameFilter filter, boolean recurse) {
		Collection<File> files = listFiles(directory, filter, recurse);

		File[] arr = new File[files.size()];
		return files.toArray(arr);
	}

	public static Collection<File> listFiles(File directory, FilenameFilter filter, boolean recurse) {
		// List of files / directories
		Vector<File> files = new Vector<File>();

		// Get files / directories in the directory
		File[] entries = directory.listFiles();

		// Go over entries
		for (File entry : entries) {
			// If there is no filter or the filter accepts the
			// file / directory, add it to the list
			if (filter == null || filter.accept(directory, entry.getName())) {
				files.add(entry);
			}

			// If the file is a directory and the recurse flag
			// is set, recurse into the directory
			if (recurse && entry.isDirectory()) {
				files.addAll(listFiles(entry, filter, recurse));
			}
		}

		// Return collection of files
		return files;
	}

	public static void writeTextFile(String[][] textInRows, String textFile) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(textFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (out != null) {
			for (int n = 0; n < textInRows.length; n++) {
				String line = "";
				for (int i = 0; i < textInRows[n].length; i++)
					line += textInRows[n][i] + " ";

				out.println(line);
			}

			out.close();
		} else
			System.out.println("Error! Cannot create file: " + textFile);
	}

	public static void writeTextFile(String[] textInRows, String textFile) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(textFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (out != null) {
			for (int i = 0; i < textInRows.length; i++)
				out.println(textInRows[i]);

			out.close();
		} else
			System.out.println("Error! Cannot create file: " + textFile);
	}

	public static void writeTextFile(Vector<String> textInRows, String textFile) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(textFile));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (out != null) {
			for (int i = 0; i < textInRows.size(); i++)
				out.println(textInRows.get(i));

			out.close();
		} else
			System.out.println("Error! Cannot create file: " + textFile);
	}

	public static void writeTextFile(float[] x, String filename) {
		writeTextFile(StringUtils.toStringLines(x), filename);
	}

	public static void writeTextFile(double[] x, String filename) {
		writeTextFile(StringUtils.toStringLines(x), filename);
	}

	public static void writeTextFile(double[][] x, String filename) {
		String[][] lines = new String[x.length][];
		for (int i = 0; i < x.length; i++)
			lines[i] = StringUtils.toStringLines(x[i]);

		writeTextFile(lines, filename);
	}

	public static void writeTextFile(int[] x, String filename) {
		writeTextFile(StringUtils.toStringLines(x), filename);
	}

	/**
	 * Unzip a zip archive into a directory on the file system. Thanks to Piotr Gabryanczyk for making this code available at
	 * http://piotrga.wordpress.com/2008/05/07/how-to-unzip-archive-in-java/
	 * 
	 * @param archive
	 *            the zip file to extract
	 * @param outputDir
	 *            the directory below which to extract the contents of the zip file. If this does not exist, it is created.
	 * @throws IOException
	 *             if any part of the process fails.
	 */
	public static void unzipArchive(File archive, File outputDir) throws IOException {
		ZipFile zipfile = new ZipFile(archive);
		for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements();) {
			ZipEntry entry = e.nextElement();
			unzipEntry(zipfile, entry, outputDir);
		}
	}

	private static void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {

		if (entry.isDirectory()) {
			createDir(new File(outputDir, entry.getName()));
			return;
		}

		File outputFile = new File(outputDir, entry.getName());
		if (!outputFile.getParentFile().exists()) {
			createDir(outputFile.getParentFile());
		}

		BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
		BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

		try {
			IOUtils.copy(inputStream, outputStream);
		} finally {
			outputStream.close();
			inputStream.close();
		}
	}

	private static void createDir(File dir) throws IOException {
		if (!dir.mkdirs())
			throw new IOException("Can not create dir " + dir);
	}

}
