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
package marytts.tools.dbselection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Vector;

public class WikipediaDumpSplitter {

	private int maxPages = 25000;
	private String xmlWikipediaDumpFile = null;
	private String dirOuputFiles = null;

	public void setXmlWikipediaDumpFile(String str) {
		xmlWikipediaDumpFile = str;
	}

	public void setDirOuputFiles(String str) {
		dirOuputFiles = str;
	}

	public void setMaxPages(int val) {
		maxPages = val;
	}

	public String getXmlWikipediaDumpFile() {
		return xmlWikipediaDumpFile;
	}

	public String getDirOuputFiles() {
		return dirOuputFiles;
	}

	public int getMaxPages() {
		return maxPages;
	}

	/***
	 * This function splits a big XML wikipedia file (ex. 19GB for enwiki) into small XML chunks according to the specified
	 * maximum number of pages per chunk.
	 * 
	 * @param xmlFile
	 *            name of the XML wikipedia file.
	 * @param dirFiles
	 *            directory where to save the small xml chunks.
	 * @param maxPagesPerChunk
	 *            maximum number of pages per chunk, it can be for example 250000 pages (~30MB).
	 */
	private void splitWikipediaDump(String xmlFile, String dirFiles, int maxPagesPerChunk) {

		int totalPageNumber = 0;
		int currentPageNumber = 0;
		int numFiles = 0;
		String outFileName = "";
		String nextLine;
		boolean checkSiteInfo = true;
		boolean siteInfo = false;
		StringBuilder strInfo = new StringBuilder();
		FileWriter outputStream = null;
		int num = (int) Math.round(maxPagesPerChunk * 0.50);

		// we need to scan line by line a big (for ex. 19GB for enwiki) xml file

		BufferedReader inputStream = null;

		try {

			inputStream = new BufferedReader(new FileReader(xmlFile));
			while ((nextLine = inputStream.readLine()) != null) {

				// get first the siteinfo
				if (checkSiteInfo) {
					if (nextLine.startsWith("  <siteinfo"))
						siteInfo = true;
					else if (nextLine.startsWith("  </siteinfo")) {
						siteInfo = false;
						checkSiteInfo = false;
						strInfo.append(nextLine + "\n");
						System.out.println("Extracted <siteinfo> from header, it will be added to all the xml files.\n");
						// System.out.println("siteInfo:" + strInfo);
					} else if (nextLine.startsWith("  </page")) {
						// if a page appears before the siteInfo maybe there is no siteinfo in the header
						System.out.println("Error: problem with siteInfo in file " + xmlFile);
						return;
					}
					if (siteInfo)
						strInfo.append(nextLine + "\n");

				} else if (!nextLine.startsWith("<mediawiki") && !nextLine.startsWith("</mediawiki>")) {

					if (currentPageNumber == maxPagesPerChunk) {
						outputStream.write("</mediawiki>\n");
						currentPageNumber = 0;
						outputStream.close();
						outputStream = null;
					}
					if (outputStream == null) {
						numFiles++;
						outFileName = dirFiles + "page" + Integer.toString(numFiles) + ".xml";
						System.out.println("outFileName(" + maxPagesPerChunk + "):" + outFileName);
						outputStream = new FileWriter(outFileName);
						outputStream.write("<mediawiki>\n");
						// we need the siteinfo at the begining of each chunk
						outputStream.write(strInfo.toString());
						outputStream.write(nextLine + "\n");
					} else
						outputStream.write(nextLine + "\n");

					if (nextLine.startsWith("  </page")) {
						currentPageNumber++;
						totalPageNumber++;
						if ((totalPageNumber % num) == 0)
							System.out.println("number of wikipages = " + totalPageNumber);
					}
				} // if no mediawiki line
			} // while next line

			// final part if remaining pages
			if (currentPageNumber > 0) {
				System.out.println("number of wikipages = " + totalPageNumber + " last chunk with " + currentPageNumber
						+ " pages.");
				outputStream.write("</mediawiki>\n");
				outputStream.close();
			}
		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception e) {
				System.err.println("Exception: " + e.getMessage());
			}
		}
	}

	/**
	 * Read and parse the command line args
	 * 
	 * @param args
	 *            the args
	 * @return true, if successful, false otherwise
	 */
	private boolean readArgs(String[] args) {

		String help = "\nUsage: java WikipediaDumpSplitter -xmlDump xmlDumpFile -dirOut outputFilesDir -maxPages maxNumberPages \n"
				+ "      -xmlDump xml wikipedia dump file. \n"
				+ "      -outDir directory where the small xml chunks will be saved.\n"
				+ "      -maxPages maximum number of pages of each small xml chunk (if no specified default 25000). \n\n";

		if (args.length >= 4) { // minimum 2 parameters
			for (int i = 0; i < args.length; i++) {
				if (args[i].contentEquals("-xmlDump") && args.length >= (i + 1))
					setXmlWikipediaDumpFile(args[++i]);

				else if (args[i].contentEquals("-outDir") && args.length >= (i + 1))
					setDirOuputFiles(args[++i]);
				// this argument is optional
				else if (args[i].contentEquals("-maxPages") && args.length >= (i + 1))
					setMaxPages(Integer.parseInt(args[++i]));
			}
		} else {
			System.out.println(help);
			return false;
		}

		if (getXmlWikipediaDumpFile() == null || getDirOuputFiles() == null) {
			System.out.println("\nMissing required parameter -xmlDump or -dirOut.");
			System.out.println(help);
			return false;
		}
		if (getMaxPages() == 0) {
			System.out.println("Number of pages per xml file not specified. Using defaul value maxPages = 25000");
			setMaxPages(25000);
		}
		return true;
	}

	private void printParameters() {
		System.out.println("\nWikipediaDumpSplitter parameters:" + "\n  -xmlDump  " + getXmlWikipediaDumpFile()
				+ "\n  -outDir   " + getDirOuputFiles() + "\n  -maxPages " + getMaxPages() + "\n");

	}

	public static void main(String[] args) throws Exception {
		String wFile, cmdLine;
		Vector<String> filesToProcess;
		Vector<String> filesDone;

		WikipediaDumpSplitter wiki = new WikipediaDumpSplitter();

		/* check the arguments */
		if (!wiki.readArgs(args))
			return;
		wiki.printParameters();

		wiki.splitWikipediaDump(wiki.getXmlWikipediaDumpFile(), wiki.getDirOuputFiles(), wiki.getMaxPages());

	}
}
