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
package marytts.tools.dbselection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.Scanner;
import java.util.Vector;

/**
 * WikipediaProcessor This program processes one by one the xml files split with wikipediaDumpSplitter. Each xml file is converted
 * to an sql source file with mwdumper-2008-04-13.jar (org.mediawiki.dumper.Dumper) The tables names in the sql source are
 * prefixed with the local (ex. en_US, de etc.) Each sql source is loaded in a mysql database, basically the tables local_text,
 * local_page and local_revision are loaded. Once the tables are loaded the WikipediMarkupCleaner is used to extract clean text
 * and a wordList, as a result two tables will be created in the database: local_cleanText and local_wordList (the wordList is
 * also saved in a file).
 * 
 * @author Marcela Charfuelan.
 */
public class WikipediaProcessor {

	// locale
	private String locale = null;
	// mySql database
	private String mysqlHost = null;
	private String mysqlDB = null;
	private String mysqlUser = null;
	private String mysqlPasswd = null;
	// Wikipedia files:
	private String listFile = null;
	private String textFile = null;
	private String pageFile = null;
	private String revisionFile = null;
	private String wikiLog = null;
	private boolean debug = false;
	private String debugPageId = null;
	// Default settings for max page length and min and max text length
	private int minPageLength = 10000; // minimum size of a wikipedia page, to be used in the first filtering of pages
	private int minTextLength = 1000;
	private int maxTextLength = 15000; // the average lenght in one big xml file is approx. 12000

	// Use this variable to save time not loading Wiki tables, if they already exist in the DB
	private boolean loadWikiTables = true;

	// Use this variable to do not create a new cleanText table, but adding to an already existing cleanText table.
	private boolean deleteCleanTextTable = false;

	public void setLocale(String str) {
		locale = str;
	}

	public void setMysqlHost(String str) {
		mysqlHost = str;
	}

	public void setMysqlDB(String str) {
		mysqlDB = str;
	}

	public void setMysqlUser(String str) {
		mysqlUser = str;
	}

	public void setMysqlPasswd(String str) {
		mysqlPasswd = str;
	}

	public void setListFile(String str) {
		listFile = str;
	}

	public void setTextFile(String str) {
		textFile = str;
	}

	public void setPageFile(String str) {
		pageFile = str;
	}

	public void setRevisionFile(String str) {
		revisionFile = str;
	}

	public void setWikiLog(String str) {
		wikiLog = str;
	}

	public void setTestId(String str) {
		debugPageId = str;
	}

	public void setMinPageLength(int val) {
		minPageLength = val;
	}

	public void setMinTextLength(int val) {
		minTextLength = val;
	}

	public void setMaxTextLength(int val) {
		maxTextLength = val;
	}

	public void setDebug(boolean bval) {
		debug = bval;
	}

	public void setLoadWikiTables(boolean bval) {
		loadWikiTables = bval;
	}

	public void setDeleteCleanTextTable(boolean bval) {
		deleteCleanTextTable = bval;
	}

	public String getLocale() {
		return locale;
	}

	public String getMysqlHost() {
		return mysqlHost;
	}

	public String getMysqlDB() {
		return mysqlDB;
	}

	public String getMysqlUser() {
		return mysqlUser;
	}

	public String getMysqlPasswd() {
		return mysqlPasswd;
	}

	public String getListFile() {
		return listFile;
	}

	public String getTextFile() {
		return textFile;
	}

	public String getPageFile() {
		return pageFile;
	}

	public String getRevisionFile() {
		return revisionFile;
	}

	public String getWikiLog() {
		return wikiLog;
	}

	public String getTestId() {
		return debugPageId;
	}

	public int getMinPageLength() {
		return minPageLength;
	}

	public int getMinTextLength() {
		return minTextLength;
	}

	public int getMaxTextLength() {
		return maxTextLength;
	}

	public boolean getDebug() {
		return debug;
	}

	public boolean getLoadWikiTables() {
		return loadWikiTables;
	}

	public boolean getDeleteCleanTextTable() {
		return deleteCleanTextTable;
	}

	private void printParameters() {
		System.out.println("WikipediaMarkupCleaner parameters:" + "\n  -mysqlHost " + getMysqlHost() + "\n  -mysqlUser "
				+ getMysqlUser() + "\n  -mysqlPasswd " + getMysqlPasswd() + "\n  -mysqlDB " + getMysqlDB() + "\n  -listFile "
				+ getListFile() + "\n  -minPage " + getMinPageLength() + "\n  -minText " + getMinTextLength() + "\n  -maxText "
				+ getMaxTextLength());

	}

	/**
	 * Read and parse the command line args
	 * 
	 * @param args
	 *            the args
	 * @return true, if successful, false otherwise
	 */
	private boolean readArgs(String[] args) {

		String help = "\nUsage: java WikipediaProcessor -locale language -mysqlHost host -mysqlUser user -mysqlPasswd passwd \n"
				+ "                                   -mysqlDB wikiDB -listFile wikiFileList.\n"
				+ "                                   [-minPage 10000 -minText 1000 -maxText 15000] \n\n"
				+ "      -listFile is a a text file that contains the xml wikipedia file names to be procesed. \n"
				+ "      This program requires the jar file mwdumper-2008-04-13.jar (or latest). \n\n"
				+ "      default/optional: [-minPage 10000 -minText 1000 -maxText 15000] \n"
				+ "      -minPage is the minimum size of a wikipedia page that will be considered for cleaning.\n"
				+ "      -minText is the minimum size of a text to be kept in the DB.\n"
				+ "      -maxText is used to split big articles in small chunks, this is the maximum chunk size. \n";

		if (args.length >= 12) { // minimum 12 parameters
			for (int i = 0; i < args.length; i++) {
				if (args[i].contentEquals("-locale") && args.length >= (i + 1))
					setLocale(args[++i]);

				else if (args[i].contentEquals("-mysqlHost") && args.length >= (i + 1))
					setMysqlHost(args[++i]);

				else if (args[i].contentEquals("-mysqlUser") && args.length >= (i + 1))
					setMysqlUser(args[++i]);

				else if (args[i].contentEquals("-mysqlPasswd") && args.length >= (i + 1))
					setMysqlPasswd(args[++i]);

				else if (args[i].contentEquals("-mysqlDB") && args.length >= (i + 1))
					setMysqlDB(args[++i]);

				else if (args[i].contentEquals("-listFile") && args.length >= (i + 1))
					setListFile(args[++i]);

				// From here the arguments are optional
				else if (args[i].contentEquals("-minPage") && args.length >= (i + 1))
					setMinPageLength(Integer.parseInt(args[++i]));

				else if (args[i].contentEquals("-minText") && args.length >= (i + 1))
					setMinTextLength(Integer.parseInt(args[++i]));

				else if (args[i].contentEquals("-maxText") && args.length >= (i + 1))
					setMaxTextLength(Integer.parseInt(args[++i]));

				else { // unknown argument
					System.out.println("\nOption not known: " + args[i]);
					System.out.println(help);
					return false;
				}

			}
		} else { // num arguments less than 12
			System.out.println(help);
			return false;
		}

		if (getMysqlHost() == null || getMysqlUser() == null || getMysqlPasswd() == null || getMysqlDB() == null) {
			System.out.println("\nMissing required mysql parameters (one/several required variables are null).");
			printParameters();
			System.out.println(help);
			return false;
		}

		if (getListFile() == null) {
			System.out.println("\nMissing required parameter -listFile wikiFileList.\n");
			printParameters();
			System.out.println(help);
			return false;
		}

		return true;
	}

	private Vector<String> getWikipediaFiles(String fileName) throws Exception {

		BufferedReader in = null;
		String line;
		Vector<String> files = null;

		// check if the file exist
		File f = new File(fileName);
		if (f.exists()) {
			files = new Vector<String>();
			try {
				in = new BufferedReader(new FileReader(fileName));
				while ((line = in.readLine()) != null) {
					files.add(line);
				}
				in.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return files;

	}

	private void setWikipediaFileDone(String fileName, String fileDone) {

		RandomAccessFile out = null;

		try {
			out = new RandomAccessFile(fileName, "rw");
			out.seek(out.length());
			out.writeBytes(fileDone + "\n");
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void addLocalePrefixToTables(String sqlFile, String outFile) {
		String line, localLine;
		Scanner s = null;
		FileWriter outputStream = null;

		try {
			s = new Scanner(new BufferedReader(new FileReader(sqlFile)));

			System.out.println("Adding local prefix to sql tables.");
			outputStream = new FileWriter(outFile);

			while (s.hasNext()) {
				line = s.nextLine();

				if (line.contains("INSERT INTO ")) {
					localLine = line.replaceAll("INSERT INTO ", "INSERT INTO " + locale + "_");
					outputStream.write(localLine + "\n");
				} else
					outputStream.write(line + "\n");

			}
			outputStream.close();
			System.out.println("Added local=" + locale + " to tables in outFile:" + outFile);

		} catch (Exception e) {
			System.err.println("Exception: " + e.getMessage());
		} finally {
			if (s != null)
				s.close();
		}

	}

	public static void main(String[] args) throws Exception {
		String wFile; // xml wiki file
		String doneFile = "./done.txt"; // file that contains the xml files already processed
		Vector<String> filesToProcess;
		Vector<String> filesDone;
		WikipediaProcessor wiki = new WikipediaProcessor();

		/* check the arguments */
		if (!wiki.readArgs(args))
			return;
		wiki.printParameters();

		// checking if cleanText table exist
		DBHandler wikiToDB = new DBHandler(wiki.getLocale());
		wikiToDB.createDBConnection(wiki.getMysqlHost(), wiki.getMysqlDB(), wiki.getMysqlUser(), wiki.getMysqlPasswd());
		char c;
		boolean result = false, processFiles = true;
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);

		String table = wiki.getLocale() + "_cleanText";
		if (wikiToDB.tableExist(table)) {
			System.out.print("    TABLE = \"" + table + "\" already exists, should it be deleted (y/n)?");
			try {
				String s = br.readLine();
				if (s.contentEquals("y")) {
					wikiToDB.createWikipediaCleanTextTable();
				} else {
					System.out.print("    ADDING clean text TO EXISTING cleanText TABLE \"" + wiki.getLocale()
							+ "_cleanText\" (y/n)?");
					s = br.readLine();
					if (s.contentEquals("y"))
						processFiles = true;
					else {
						processFiles = false;
						System.out
								.print("    please check the \"locale\" prefix of the locale_cleanText TABLE you want to create or add to.");
					}
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		} else
			System.out.print("    TABLE = \"" + table + "\" does not exist, it will be created.");
		wikiToDB.closeDBConnection();

		if (processFiles) {
			filesToProcess = wiki.getWikipediaFiles(wiki.getListFile());
			filesDone = wiki.getWikipediaFiles(doneFile);
			if (filesDone == null)
				filesDone = new Vector<String>();

			if (filesToProcess != null) {
				for (int i = 0; i < filesToProcess.size(); i++) {
					wFile = filesToProcess.elementAt(i);
					if (filesDone.indexOf(wFile) == -1) {
						System.out.println("\n_______________________________________________________________________________");

						System.out.println("\nProcessing xml file:" + wFile);

						WikipediaMarkupCleaner wikiCleaner = new WikipediaMarkupCleaner();

						// Set parameters in the WikipediaMarkupCleaner
						wikiCleaner.setDebug(false);
						wikiCleaner.setDeleteCleanTextTable(false);
						wikiCleaner.setLoadWikiTables(true);
						wikiCleaner.setLocale(wiki.getLocale());

						wikiCleaner.setMaxTextLength(wiki.getMaxTextLength());
						wikiCleaner.setMinPageLength(wiki.getMinPageLength());
						wikiCleaner.setMinTextLength(wiki.getMinTextLength());
						wikiCleaner.setMysqlDB(wiki.getMysqlDB());
						wikiCleaner.setMysqlHost(wiki.getMysqlHost());
						wikiCleaner.setMysqlPasswd(wiki.getMysqlPasswd());
						wikiCleaner.setMysqlUser(wiki.getMysqlUser());

						// process xml file
						wikiCleaner.setXmlWikiFile(wFile);
						wikiCleaner.processWikipediaPages();
						wikiCleaner = null;

						// when finished
						wiki.setWikipediaFileDone("./done.txt", wFile);

					} else
						System.out.println("File already procesed: " + wFile);
				}
			} else
				System.out.println("Empty list of files to process.");
		} else
			System.out.println("WikipediaProcessor terminated.");

	}

}
