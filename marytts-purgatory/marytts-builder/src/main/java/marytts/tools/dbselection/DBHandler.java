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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import marytts.util.Pair;
import marytts.util.io.FileUtils;

import org.mediawiki.importer.DumpWriter;
import org.mediawiki.importer.SqlServerStream;
import org.mediawiki.importer.SqlStream;
import org.mediawiki.importer.SqlWriter;
import org.mediawiki.importer.SqlWriter15;
import org.mediawiki.importer.XmlDumpReader;

/**
 * Various functions for handling connection, inserting and querying a mysql database.
 * 
 * @author Marcela Charfuelan, Holmer Hemsen.
 */
public class DBHandler {

	private String locale = "en_US";
	private Connection cn = null;

	private Statement st = null;
	private ResultSet rs = null;
	private String currentTable = null;
	private PreparedStatement psSentence = null;
	private PreparedStatement psSelectedSentence = null;
	private PreparedStatement psWord = null;
	private PreparedStatement psCleanText = null;
	private PreparedStatement psTablesDescription = null;

	private String cleanTextTableName = "_cleanText";
	private String wordListTableName = "_wordList";
	private String dbselectionTableName = "_dbselection";
	private String selectedSentencesTableName = "_selectedSentences";

	/**
	 * The constructor loads the database driver.
	 * 
	 * @param localeVal
	 *            database language.
	 */
	public DBHandler(String localeVal) {
		initDB_Driver();
		locale = localeVal;
		cleanTextTableName = locale + cleanTextTableName;
		wordListTableName = locale + wordListTableName;
		dbselectionTableName = locale + dbselectionTableName;
		selectedSentencesTableName = locale + selectedSentencesTableName;
		System.out.println("\nMysql driver loaded, set locale=" + locale);
	}

	/** Loading DB Driver */
	private void initDB_Driver() {
		try {
			String driver = "com.mysql.jdbc.Driver";
			Class.forName(driver);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/***
	 * By default the name of the selected sentence is "locale + _selectedSentences" with this function the name can be changed,
	 * the locale prefix will be kept and the suffix "_selectedSentences". NOTE: If the name is changed this function should be
	 * called before calling createDBConnection() because ther prepared statments for each table are initialised.
	 * 
	 * @param name
	 *            name
	 */
	public void setSelectedSentencesTableName(String name) {
		if (name.contentEquals(""))
			selectedSentencesTableName = locale + "_selectedSentences";
		else
			selectedSentencesTableName = locale + "_" + name + "_selectedSentences";
		System.out.println("Current selected sentences table name = " + selectedSentencesTableName);
	}

	public String getSelectedSentencesTableName() {
		return selectedSentencesTableName;
	}

	public String getCleanTextTableName() {
		return cleanTextTableName;
	}

	public String getWordListTableName() {
		return wordListTableName;
	}

	public String getDBselectionTableName() {
		return dbselectionTableName;
	}

	/**
	 * The <code>createDBConnection</code> method creates the database connection.
	 *
	 * @param host
	 *            a <code>String</code> value. The database host e.g. 'localhost'.
	 * @param db
	 *            a <code>String</code> value. The database to connect to.
	 * @param user
	 *            a <code>String</code> value. Database user that has excess to the database.
	 * @param passwd
	 *            a <code>String</code> value. The 'secret' password.
	 * @return true if connection was succesfull, false otherwise
	 */
	public boolean createDBConnection(String host, String db, String user, String passwd) {
		boolean result = false;
		String url = "jdbc:mysql://" + host + "/" + db + "?jdbcCompliantTruncation=false";
		try {
			Properties p = new Properties();
			p.put("user", user);
			p.put("password", passwd);
			p.put("database", db);
			p.put("useUnicode", "true");
			p.put("characterEncoding", "utf8");

			cn = DriverManager.getConnection(url, p);

			st = cn.createStatement();

			psCleanText = cn.prepareStatement("INSERT INTO " + cleanTextTableName + " VALUES (null, ?, ?, ?, ?)");
			psWord = cn.prepareStatement("INSERT INTO " + wordListTableName + " VALUES (null, ?, ?)");
			psSentence = cn.prepareStatement("INSERT INTO " + dbselectionTableName + " VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?)");
			psSelectedSentence = cn.prepareStatement("INSERT INTO " + selectedSentencesTableName + " VALUES (null, ?, ?, ?)");
			psTablesDescription = cn.prepareStatement("INSERT INTO tablesDescription VALUES (null, ?, ?, ?, ?, ?, ?, ?)");

			result = true;
			System.out.println("Mysql connection created successfully.");

		} catch (Exception e) {
			System.out.println("Problems creating Mysql connection.");
			e.printStackTrace();
		}
		return result;
	}

	/***
	 * Use mwdumper for extracting pages from a XML wikipedia dump file. The mwdumper reads a xml wikipedia file and extract the
	 * tables "text", "page" and "revision" in sql format. In this configuration the mwdumper creates a connection to the DB and
	 * load directly the tables, so the tables must be already created and they should be empty. Here these tables are created
	 * empty using the function createEmptyWikipediaTables, this function deletes any existing text, page and revision table
	 * before creating new ones.
	 * 
	 * @param xmlFile
	 *            xml dump file
	 * @param lang
	 *            locale language
	 * @param host
	 *            host
	 * @param db
	 *            db
	 * @param user
	 *            user
	 * @param passwd
	 *            passwd
	 * @throws Exception
	 *             Exception
	 */
	public void loadPagesWithMWDumper(String xmlFile, String lang, String host, String db, String user, String passwd)
			throws Exception {

		DBHandler wikiDB = new DBHandler(lang);
		System.out
				.println("Using mwdumper to convert xml file into sql source file and loading text, page and revision tables into the DB.");
		System.out.println("Creating connection to DB server...");
		wikiDB.createDBConnection(host, db, user, passwd);

		// Before runing the mwdumper the tables text, page and revision should be created empty.
		// If the tables exist, it could be that other user/process is using these tables so in that case
		// the program will stop. If no other user/program is using these tables the user has the option of deleting them.
		if (wikiDB.createEmptyWikipediaTables()) {

			// Very old: dump into sql file:
			// Run the mwdumper jar file xml -> sql
			// Use these parameters for saving the output in a dump.sql source file
			/*
			 * String sqlDump = "dump.sql"; String[] argsDump = new String[3]; argsDump[0] = "--output=file:"+sqlDump; argsDump[1]
			 * = "--format=sql:1.5"; argsDump[2] = xmlFile;
			 */

			/*
			 * Old: run Dumper as if it was called from the command line: // Use these parameters for loading the pages direclty
			 * in the DB String[] argsDump = new String[3]; argsDump[0] = "--output=mysql://" + host + "/" + db + "?user=" + user
			 * + "&password=" + passwd + "&useUnicode=true&characterEncoding=utf8"; argsDump[1] = "--format=sql:1.5"; argsDump[2]
			 * = xmlFile;
			 * 
			 * //--- The following ClassLoader code from: //
			 * http://java.sun.com/docs/books/tutorial/deployment/jar/examples/JarClassLoader.java // Class c = loadClass(name);
			 * // this does not work (example from sun) // Class c = Class.forName("org.mediawiki.dumper.Dumper"); // this works
			 * Class c = ClassLoader.getSystemClassLoader().loadClass("org.mediawiki.dumper.Dumper"); // this also works Method m
			 * = c.getMethod("main", new Class[] { argsDump.getClass() }); m.setAccessible(true); int mods = m.getModifiers(); if
			 * (m.getReturnType() != void.class || !Modifier.isStatic(mods) || !Modifier.isPublic(mods)) { throw new
			 * NoSuchMethodException("main"); } try { m.invoke(null, new Object[] { argsDump });
			 * 
			 * } catch (IllegalAccessException e) { // This should not happen, as we have disabled access checks }
			 */

			// Call mwdumper code directly:
			SqlStream sqlStream = new SqlServerStream(wikiDB.cn);
			DumpWriter sqlWriter = new SqlWriter15(new SqlWriter.MySQLTraits(), sqlStream);
			File tmpFile = new File(xmlFile + "_filtered");
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(xmlFile), "UTF-8"));
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
			int read = -1;
			char[] buf = new char[4096];
			while ((read = in.read(buf)) > 0) {
				out.write(buf, 0, read);
			}
			in.close();
			out.close();
			InputStream filteredInput = new BufferedInputStream(new FileInputStream(tmpFile));
			XmlDumpReader xmlReader = new XmlDumpReader(filteredInput, sqlWriter);
			xmlReader.readDump(); // this will close wikiDB.cn, so that it will have to be opened again:
			tmpFile.delete();
			if (wikiDB.cn.isClosed()) {
				wikiDB.createDBConnection(host, db, user, passwd);
			}

			// Now I need to add/change the prefix locale to the table names
			// Renaming tables
			wikiDB.addLocalePrefixToWikipediaTables(); // this change the name of already created and loaded tables

		} else {
			wikiDB.closeDBConnection();
			// problems creating the tables
			System.exit(1);
		}

	}

	/****************************************************************************************
	 * FUNCTIONS FOR CREATING TABLES
	 ****************************************************************************************/
	/***
	 * Ask the user if the table should be deleted
	 * 
	 * @param table
	 *            table
	 * @return true if user answers false otherwise.
	 */
	public boolean askIfDeletingTable(String table) {
		char c;
		boolean result = false;
		InputStreamReader isr = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(isr);
		boolean wikipediaTables = false;

		String helpToContinue = "\n ***To continue please check if the table \""
				+ table
				+ "\" is used by another user/process.*** \n"
				+ "    Tables \"text\", \"page\" and \"revision\" are temporary tables used by mwdumper to extract wikipedia pages. \n"
				+ "    Just one user/process can use these tables at the same time in the same DataBase, please use other DataBase if possible.\n"
				+ "    If the tables are not in use by any other user/process please use the option for deleting.\n";

		if (table.contentEquals("text") || table.contentEquals("page") || table.contentEquals("revision")) {
			System.out.println(helpToContinue);
			wikipediaTables = true;
		}

		System.out.print("    TABLE = \"" + table + "\" already exists deleting (y/n)?");
		try {
			String s = br.readLine();
			if (s.contentEquals("y")) {
				result = true;
			} else {
				if (wikipediaTables)
					System.out.println("\nTo continue please check if the table \"" + table + "\" can be deleted "
							+ "or is used by another user/process.\n");
				else
					System.out.println("    Adding data to TABLE = \"" + table + "\".\n");
				result = false;
			}

		} catch (Exception e) {
			System.out.println(e);
		}
		return result;
	}

	/***
	 * This table contains information about tables in the DB, specially for selected sentences tables.
	 */
	public void createTablesDescriptionTable() {

		String tables = "CREATE TABLE IF NOT EXISTS tablesDescription ( id INT NOT NULL AUTO_INCREMENT, " + "name TINYTEXT,"
				+ "description MEDIUMTEXT," + "stopCriterion TINYTEXT," + "featuresDefinitionFileName TINYTEXT,"
				+ "featuresDefinitionFile MEDIUMTEXT," + "covDefConfigFileName TINYTEXT," + "covDefConfigFile MEDIUMTEXT,"
				+ "primary key(id)) CHARACTER SET utf8;";
		try {
			boolean res = st.execute(tables);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/***
	 * Creates dbselectionTable
	 * 
	 * 
	 */
	public void createDataBaseSelectionTable() {
		String dbselection = "CREATE TABLE " + dbselectionTableName + " ( id INT NOT NULL AUTO_INCREMENT, "
				+ "sentence MEDIUMBLOB NOT NULL, " + "features BLOB, " + "reliable BOOLEAN, " + "unknownWords BOOLEAN, "
				+ "strangeSymbols BOOLEAN, " + "selected BOOLEAN, " + "unwanted BOOLEAN, "
				+ "cleanText_id INT UNSIGNED NOT NULL, " + // the cleanText id where this sentence comes from
				"primary key(id)) CHARACTER SET utf8;";
		String str;
		boolean dbExist = false;
		// if database does not exist create it, if it exists it will continue adding sentences to this table
		System.out.println("Checking if " + dbselectionTableName + " already exist.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			while (rs.next()) {
				str = rs.getString(1);
				if (str.contentEquals(dbselectionTableName)) {
					System.out.println("TABLE = " + str + " already exist, adding sentences to this table.");
					dbExist = true;
				}
			}
			boolean res;

			// if DB exists does not exist it will be created
			if (!dbExist) {
				res = st.execute(dbselection);
				System.out.println("TABLE = " + dbselectionTableName + " succesfully created.");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/***
	 * Creates a selectedSentencesTable.
	 *
	 * @param stopCriterion
	 *            stopCriterion
	 * @param featDefFileName
	 *            featDefFileName
	 * @param covDefConfigFileName
	 *            covDefConfigFileName
	 */
	public void createSelectedSentencesTable(String stopCriterion, String featDefFileName, String covDefConfigFileName) {
		String selected = "CREATE TABLE " + selectedSentencesTableName + " ( id INT NOT NULL AUTO_INCREMENT, "
				+ "sentence MEDIUMBLOB NOT NULL, " + "unwanted BOOLEAN, " + "dbselection_id INT UNSIGNED NOT NULL, " + // the
																														// dbselection
																														// id
																														// where
																														// this
																														// sentence
																														// comes
																														// from
				"primary key(id)) CHARACTER SET utf8;";

		String str;
		boolean dbExist = false;
		// if database does not exist create it
		System.out.println("\nChecking if " + selectedSentencesTableName + " already exist.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			while (rs.next()) {
				str = rs.getString(1);
				if (str.contentEquals(selectedSentencesTableName)) {
					System.out.println("  TABLE = " + str + " already exist. New selected sentences "
							+ "will be added to this table.");
					dbExist = true;
				}
			}

			// Ask if delete?
			// if(dbExist && !askIfDeletingTable(selectedSentencesTableName) )
			// result = false;

			if (!dbExist) {
				// if creating a new table then the set the fields selected=false and unwanted=false
				// in dbselection table
				System.out.println("  TABLE = " + selectedSentencesTableName + " does not exist, creating a new table and ");
				// System.out.println("  Initialising fields selected=false and unwanted=false in TABLE = " +
				// dbselectionTableName);
				System.out.println("  Initialising fields selected=false in TABLE = " + dbselectionTableName
						+ " (Previously selected sentences marked as unwanted will be kept)");
				updateTable("UPDATE " + dbselectionTableName + " SET selected=false;");
				// updateTable("UPDATE " + dbselectionTableName + " SET unwanted=false;");
				boolean res = st.execute(selected);
				System.out.println("  TABLE = " + selectedSentencesTableName + " succesfully created.");

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/***
	 * This function creates text, page and revision tables loading them from text files.
	 * 
	 * @param textFile
	 *            textFile
	 * @param pageFile
	 *            pageFile
	 * @param revisionFile
	 *            revisionFile
	 */
	public void createAndLoadWikipediaTables(String textFile, String pageFile, String revisionFile) {

		String createTextTable = "CREATE TABLE " + locale + "_text (" + " old_id int UNSIGNED NOT NULL AUTO_INCREMENT,"
				+ " old_text mediumblob NOT NULL," + " old_flags tinyblob NOT NULL," + " PRIMARY KEY old_id (old_id)"
				+ " ) MAX_ROWS=250000 AVG_ROW_LENGTH=10240;";

		String createPageTable = "CREATE TABLE " + locale + "_page (" + "page_id int UNSIGNED NOT NULL AUTO_INCREMENT,"
				+ "page_namespace int(11) NOT NULL," + "page_title varchar(255) NOT NULL,"
				+ "page_restrictions tinyblob NOT NULL," + "page_counter bigint(20) unsigned NOT NULL,"
				+ "page_is_redirect tinyint(3) unsigned NOT NULL," + "page_is_new tinyint(3) unsigned NOT NULL,"
				+ "page_random double unsigned NOT NULL," + "page_touched binary(14) NOT NULL,"
				+ "page_latest int(10) unsigned NOT NULL," + "page_len int(10) unsigned NOT NULL,"
				+ "PRIMARY KEY page_id (page_id)," + "KEY page_namespace (page_namespace)," + "KEY page_random (page_random),"
				+ "KEY page_len (page_len) ) MAX_ROWS=250000 AVG_ROW_LENGTH=10240; ";

		String createRevisionTable = "CREATE TABLE " + locale + "_revision (" + "rev_id int UNSIGNED NOT NULL AUTO_INCREMENT,"
				+ "rev_page int(10) unsigned NOT NULL," + "rev_text_id int(10) unsigned NOT NULL,"
				+ "rev_comment tinyblob NOT NULL," + "rev_user int(10) unsigned NOT NULL,"
				+ "rev_user_text varchar(255) NOT NULL, " + "rev_timestamp binary(14) NOT NULL, "
				+ "rev_minor_edit tinyint(3) unsigned NOT NULL," + " rev_deleted tinyint(3) unsigned NOT NULL,"
				+ "rev_len int(10) unsigned NULL," + "rev_parent_id int(10) unsigned NULL,"
				+ "KEY rev_user (rev_user),KEY rev_user_text (rev_user_text)," + "KEY rev_timestamp (rev_timestamp),"
				+ "PRIMARY KEY rev_id (rev_id)) MAX_ROWS=250000 AVG_ROW_LENGTH=10240;";

		// If database does not exist create it, if it exists delete it and create an empty one.
		// System.out.println("Checking if the TABLE=text already exist.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean resText = false, resPage = false, resRevision = false;
		try {

			while (rs.next()) {
				String str = rs.getString(1);
				if (str.contentEquals(locale + "_text"))
					resText = true;
				else if (str.contentEquals(locale + "_page"))
					resPage = true;
				else if (str.contentEquals(locale + "_revision"))
					resRevision = true;

			}
			if (resText) {
				System.out.println("TABLE = " + locale + "_text already exist deleting.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_text;");
			}
			if (resPage) {
				System.out.println("TABLE = " + locale + "_page already exist deleting.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_page;");
			}
			if (resRevision) {
				System.out.println("TABLE = " + locale + "_revision already exist deleting.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_revision;");
			}

			boolean res1;
			int res2;
			// creating TABLE=text
			// System.out.println("\nCreating table:" + createTextTable);
			System.out.println("\nCreating table:" + locale + "_text");
			res1 = st.execute(createTextTable);
			System.out.println("Loading sql file: " + textFile);
			res2 = st.executeUpdate("LOAD DATA LOCAL INFILE '" + textFile + "' into table " + locale + "_text;");
			System.out.println("TABLE = " + locale + "_text succesfully created.");

			// creating TABLE=page
			// System.out.println("\nCreating table:" + createPageTable);
			System.out.println("\nCreating table:" + locale + "_page");
			res1 = st.execute(createPageTable);
			System.out.println("Loading sql file: " + pageFile);
			res2 = st.executeUpdate("LOAD DATA LOCAL INFILE '" + pageFile + "' into table " + locale + "_page;");
			System.out.println("TABLE = " + locale + "_page succesfully created.");

			// creating TABLE=revision
			// System.out.println("\n\nCreating table:" + createRevisionTable);
			System.out.println("\nCreating table:" + locale + "_revision");
			res1 = st.execute(createRevisionTable);
			System.out.println("Loading sql file: " + revisionFile);
			System.out.println("SOURCE " + revisionFile + ";");
			res2 = st.executeUpdate("LOAD DATA LOCAL INFILE '" + revisionFile + "' into table " + locale + "_revision;");
			System.out.println("TABLE = " + locale + "_revision succesfully created.");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/***
	 * This function creates empty text, page and revision tables (without locale prefix). If the tables already exist it will
	 * delete them.
	 * 
	 * @return true if the tables were created succesfully, false otherwise.
	 */
	public boolean createEmptyWikipediaTables() {

		boolean result = true;
		System.out.println("Creating empty wikipedia tables: checking if tables text, page and revision already exist.");
		String createTextTable = "CREATE TABLE text (" + " old_id int UNSIGNED NOT NULL AUTO_INCREMENT,"
				+ " old_text mediumblob NOT NULL," + " old_flags tinyblob NOT NULL," + " PRIMARY KEY old_id (old_id)"
				+ " ) MAX_ROWS=250000 AVG_ROW_LENGTH=10240 CHARACTER SET utf8;";

		String createPageTable = "CREATE TABLE page (" + "page_id int UNSIGNED NOT NULL AUTO_INCREMENT,"
				+ "page_namespace int(11) NOT NULL," + "page_title varchar(255) NOT NULL,"
				+ "page_restrictions tinyblob NOT NULL," + "page_counter bigint(20) unsigned NOT NULL,"
				+ "page_is_redirect tinyint(3) unsigned NOT NULL," + "page_is_new tinyint(3) unsigned NOT NULL,"
				+ "page_random double unsigned NOT NULL," + "page_touched binary(14) NOT NULL,"
				+ "page_latest int(10) unsigned NOT NULL," + "page_len int(10) unsigned NOT NULL,"
				+ "PRIMARY KEY page_id (page_id)," + "KEY page_namespace (page_namespace)," + "KEY page_random (page_random),"
				+ "KEY page_len (page_len) ) MAX_ROWS=250000 AVG_ROW_LENGTH=10240 CHARACTER SET utf8;";

		String createRevisionTable = "CREATE TABLE revision (" + "rev_id int UNSIGNED NOT NULL AUTO_INCREMENT,"
				+ "rev_page int(10) unsigned NOT NULL," + "rev_text_id int(10) unsigned NOT NULL,"
				+ "rev_comment tinyblob NOT NULL," + "rev_user int(10) unsigned NOT NULL,"
				+ "rev_user_text varchar(255) NOT NULL, " + "rev_timestamp binary(14) NOT NULL, "
				+ "rev_minor_edit tinyint(3) unsigned NOT NULL," + " rev_deleted tinyint(3) unsigned NOT NULL,"
				+ "rev_len int(10) unsigned NULL," + "rev_parent_id int(10) unsigned NULL,"
				+ "KEY rev_user (rev_user),KEY rev_user_text (rev_user_text)," + "KEY rev_timestamp (rev_timestamp),"
				+ "PRIMARY KEY rev_id (rev_id)) MAX_ROWS=250000 AVG_ROW_LENGTH=10240 CHARACTER SET utf8;";

		// If database does not exist create it, if it exists delete it and create an empty one.
		// System.out.println("Checking if the TABLE=text already exist.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean resText = false, resPage = false, resRevision = false;
		try {

			while (rs.next()) {
				String str = rs.getString(1);
				if (str.contentEquals("text"))
					resText = true;
				else if (str.contentEquals("page"))
					resPage = true;
				else if (str.contentEquals("revision"))
					resRevision = true;

			}
			boolean res0, res1;
			if (resText && !askIfDeletingTable("text"))
				result = false;
			else if (resPage && !askIfDeletingTable("page"))
				result = false;
			else if (resRevision && !askIfDeletingTable("revision"))
				result = false;

			if (result) {
				res0 = st.execute("DROP TABLE IF EXISTS text;");
				System.out.println("  Creating table: text");
				res1 = st.execute(createTextTable);
				res0 = st.execute("DROP TABLE IF EXISTS page;");
				System.out.println("  Creating table: page");
				res1 = st.execute(createPageTable);
				res0 = st.execute("DROP TABLE IF EXISTS revision;");
				System.out.println("  Creating table: revision");
				res1 = st.execute(createRevisionTable);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return result;
	}

	/****
	 * Rename the Wikipedia tables adding the prefix locale: locale_text locale_page and locale_revision. if any of the locale_*
	 * tables exist will be deleted before renaming the table.
	 */
	public void addLocalePrefixToWikipediaTables() {

		System.out.println("Adding local prefix " + locale + " to tables text, page and revision, checking if "
				+ "already exist tables with that prefix.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean resText = false, resPage = false, resRevision = false;
		boolean resLocaleText = false, resLocalePage = false, resLocaleRevision = false;
		try {

			while (rs.next()) {
				String str = rs.getString(1);
				if (str.contentEquals("text"))
					resText = true;
				else if (str.contentEquals(locale + "_text"))
					resLocaleText = true;
				else if (str.contentEquals("page"))
					resPage = true;
				else if (str.contentEquals(locale + "_page"))
					resLocalePage = true;
				else if (str.contentEquals("revision"))
					resRevision = true;
				else if (str.contentEquals(locale + "_revision"))
					resLocaleRevision = true;
			}
			if (resLocaleText) {
				System.out.println("  Deleting TABLE = " + locale + "_text.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_text;");
			}
			if (resLocalePage) {
				System.out.println("  Deleting TABLE = " + locale + "_page.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_page;");
			}
			if (resLocaleRevision) {
				System.out.println("  Deleting TABLE = " + locale + "_revision.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_revision;");
			}

			if (resText) {
				System.out.println("  RENAME TABLE = text TO " + locale + "_text.");
				boolean res0 = st.execute("RENAME TABLE text TO " + locale + "_text;");
			}
			if (resPage) {
				System.out.println("  RENAME TABLE = page TO " + locale + "_page.");
				boolean res0 = st.execute("RENAME TABLE page TO " + locale + "_page;");
			}
			if (resRevision) {
				System.out.println("  RENAME TABLE = revision TO " + locale + "_revision.");
				boolean res0 = st.execute("RENAME TABLE revision TO " + locale + "_revision;");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/****
	 * Delete the Wikipedia tables: locale_text, locale_page and locale_revision tables.
	 *
	 */
	public void deleteWikipediaTables() {

		System.out.println("Deleting already used wikipedia tables.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean resText = false, resPage = false, resRevision = false;
		try {

			while (rs.next()) {
				String str = rs.getString(1);
				if (str.contentEquals(locale + "_text"))
					resText = true;
				else if (str.contentEquals(locale + "_page"))
					resPage = true;
				else if (str.contentEquals(locale + "_revision"))
					resRevision = true;

			}
			if (resText) {
				System.out.println("  Deleting TABLE = " + locale + "_text.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_text;");
			}
			if (resPage) {
				System.out.println("  Deleting TABLE = " + locale + "_page.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_page;");
			}
			if (resRevision) {
				System.out.println("  Deleting TABLE = " + locale + "_revision.");
				boolean res0 = st.execute("DROP TABLE " + locale + "_revision;");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/***
	 * check if tables: locale_text, locale_page and locale_revision exist.
	 * 
	 * @return resText &amp;&amp; resPage &amp;&amp; resRevision
	 */
	public boolean checkWikipediaTables() {
		// wiki must be already created

		// If database does not exist create it, if it exists delete it and create an empty one.
		System.out.println("Checking if the TABLE=" + locale + "_text already exist.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean resText = false, resPage = false, resRevision = false;
		try {

			while (rs.next()) {
				String str = rs.getString(1);
				if (str.contentEquals(locale + "_text"))
					resText = true;
				else if (str.contentEquals(locale + "_page"))
					resPage = true;
				else if (str.contentEquals(locale + "_revision"))
					resRevision = true;

			}
			if (resText)
				System.out.println("TABLE =" + locale + "_text already exist.");
			if (resPage)
				System.out.println("TABLE =" + locale + "_page already exist.");
			if (resRevision)
				System.out.println("TABLE =" + locale + "_revision already exist.");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return resText && resPage && resRevision;
	}

	public void createWikipediaCleanTextTable() {
		// wiki must be already created
		// String creteWiki = "CREATE DATABASE wiki;";
		String createCleanTextTable = "CREATE TABLE " + cleanTextTableName + " (" + " id int UNSIGNED NOT NULL AUTO_INCREMENT,"
				+ " cleanText MEDIUMBLOB NOT NULL," + " processed BOOLEAN, " + " page_id int UNSIGNED NOT NULL, "
				+ " text_id int UNSIGNED NOT NULL, " + " PRIMARY KEY id (id)"
				+ " ) MAX_ROWS=250000 AVG_ROW_LENGTH=10240 CHARACTER SET utf8;";

		// If database does not exist create it, if it exists delete it and create an empty one.
		System.out.println("Checking if the TABLE=" + cleanTextTableName + " already exist.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean resText = false;
		try {

			while (rs.next()) {
				String str = rs.getString(1);
				if (str.contentEquals(cleanTextTableName))
					resText = true;
			}
			if (resText) {
				System.out.println("TABLE = " + cleanTextTableName + " already exist deleting.");
				boolean res0 = st.execute("DROP TABLE " + cleanTextTableName + ";");
			}

			boolean res1;
			int res2;
			// creating TABLE=cleanText
			// System.out.println("\nCreating table:" + createCleanTextTable);
			System.out.println("\nCreating table:" + cleanTextTableName);
			res1 = st.execute(createCleanTextTable);
			System.out.println("TABLE = " + cleanTextTableName + " succesfully created.");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/***
	 * 
	 * @return true if TABLE=tableName exist.
	 * @param tableName
	 *            tableName
	 */
	public boolean tableExist(String tableName) {
		// System.out.println("  Checking if the TABLE=" + tableName + " exist.");
		try {
			rs = st.executeQuery("SHOW TABLES;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean res = false;
		try {
			while (rs.next()) {
				String str = rs.getString(1);
				if (str.contentEquals(tableName))
					res = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;
	}

	/***
	 * Get a list of ids from field in table.
	 * 
	 * @param field
	 *            field
	 * @param table
	 *            table
	 * @return idSet
	 */
	public String[] getIds(String field, String table) {
		int num, i, j;
		String idSet[] = null;

		String str = queryTable("SELECT count(" + field + ") FROM " + table + ";");
		num = Integer.parseInt(str);
		idSet = new String[num];

		try {
			rs = st.executeQuery("SELECT " + field + " FROM " + table + ";");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			i = 0;
			while (rs.next()) {
				idSet[i] = rs.getString(1);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return idSet;
	}

	/***
	 * This function will select just the unprocessed cleanText records.
	 * 
	 * @return idSet
	 */
	public int[] getUnprocessedTextIds() {
		int num, i, j;
		int idSet[] = null;

		String str = queryTable("select count(id) from " + cleanTextTableName + " where processed=false;");
		num = Integer.parseInt(str);
		idSet = new int[num];

		try {
			rs = st.executeQuery("select id from " + cleanTextTableName + " where processed=false;");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			i = 0;
			while (rs.next()) {
				idSet[i] = rs.getInt(1);
				i++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return idSet;
	}

	/***
	 * Get the list of tables for this locale
	 * 
	 * @return ArrayList&lt;String&gt;
	 */
	public ArrayList<String> getListOfTables() {
		ArrayList<String> tablesList = new ArrayList<String>();
		try {
			rs = st.executeQuery("show tables like '" + locale + "%';");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			while (rs.next())
				tablesList.add(rs.getString(1));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return tablesList;
	}

	public void setDBTable(String table) {
		currentTable = table;
	}

	public void setWordListTable(String table) {
		wordListTableName = table;
	}

	public void insertCleanText(String text, String page_id, String text_id) {
		// System.out.println("inserting in cleanText: ");
		byte cleanText[] = null;

		try {
			cleanText = text.getBytes("UTF8");
		} catch (Exception e) { // UnsupportedEncodedException
			e.printStackTrace();
		}

		try {
			// ps = cn.prepareStatement("INSERT INTO cleanText VALUES (null, ?, ?, ?, ?)");
			if (cleanText != null) {
				psCleanText.setBytes(1, cleanText);
				psCleanText.setBoolean(2, false); // it will be true after processed by the FeatureMaker
				psCleanText.setInt(3, Integer.parseInt(page_id));
				psCleanText.setInt(4, Integer.parseInt(text_id));
				psCleanText.execute();
				psCleanText.clearParameters();
			} else
				System.out.println("WARNING: can not insert in " + cleanTextTableName + ": " + text);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/****
	 * Insert processed sentence in dbselection
	 * 
	 * @param sentence
	 *            text of the sentence.
	 * @param features
	 *            features if sentences is reliable.
	 * @param reliable
	 *            true/false.
	 * @param unknownWords
	 *            true/false.
	 * @param strangeSymbols
	 *            true/false.
	 * @param cleanText_id
	 *            the id of the cleanText this sentence comes from.
	 */
	public void insertSentence(String sentence, byte features[], boolean reliable, boolean unknownWords, boolean strangeSymbols,
			int cleanText_id) {

		byte strByte[] = null;
		try {
			strByte = sentence.getBytes("UTF8");
		} catch (Exception e) { // UnsupportedEncodedException
			e.printStackTrace();
		}

		try {
			psSentence.setBytes(1, strByte);
			psSentence.setBytes(2, features);
			psSentence.setBoolean(3, reliable);
			psSentence.setBoolean(4, unknownWords);
			psSentence.setBoolean(5, strangeSymbols);
			psSentence.setBoolean(6, false);
			psSentence.setBoolean(7, false);
			psSentence.setInt(8, cleanText_id);
			psSentence.execute();

			psSentence.clearParameters();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/***
	 * With the dbselection_id get first the sentence and then insert it in the locale_selectedSentences table.
	 * 
	 * @param dbselection_id
	 *            dbselection_id
	 * @param unwanted
	 *            unwanted
	 */
	public void insertSelectedSentence(int dbselection_id, boolean unwanted) {

		String dbQuery = "Select sentence FROM " + dbselectionTableName + " WHERE id=" + dbselection_id;
		byte[] sentenceBytes = null;

		try {
			// First get the sentence
			sentenceBytes = queryTableByte(dbQuery);

			psSelectedSentence.setBytes(1, sentenceBytes);
			psSelectedSentence.setBoolean(2, unwanted);
			psSelectedSentence.setInt(3, dbselection_id);
			psSelectedSentence.execute();

			psSelectedSentence.clearParameters();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/****
	 * Creates a wordList table, if already exists deletes it and creates a new to insert current wordList.
	 * 
	 * @param wordList
	 *            wordList
	 */
	public void insertWordList(HashMap<String, Integer> wordList) {
		String word;
		Integer value;
		boolean res;
		byte wordByte[];

		String wordListTable = "CREATE TABLE " + wordListTableName + " ( id INT NOT NULL AUTO_INCREMENT, "
				+ "word TINYBLOB NOT NULL, " + "frequency INT UNSIGNED NOT NULL, " + "primary key(id)) CHARACTER SET utf8;";

		try {
			System.out.println("Inserting wordList in DB...");
			// if wordList table already exist it should be deleted before inserting this list
			if (tableExist(wordListTableName)) {
				res = st.execute("DROP TABLE " + wordListTableName + ";");
				res = st.execute(wordListTable);
			} else
				res = st.execute(wordListTable);

			try {
				Iterator iteratorSorted = wordList.keySet().iterator();
				int count = 0;

				cn.setAutoCommit(false);

				while (iteratorSorted.hasNext()) {
					word = iteratorSorted.next().toString();
					value = wordList.get(word);
					wordByte = null;
					wordByte = word.getBytes("UTF8");
					psWord.setBytes(1, wordByte);
					psWord.setInt(2, value);
					psWord.addBatch();

					if (count++ == 1000) {
						psWord.executeBatch();
						cn.commit();
						count = 0;
						System.out.println("Adding batch.");
					}

					psWord.clearParameters();
				}

				psWord.executeBatch(); // the leftovers.
				cn.commit();
				cn.setAutoCommit(true);

			} catch (Exception e) { // UnsupportedEncodedException
				e.printStackTrace();
			}
			System.out.println("Inserted new words in " + wordListTableName + " table.");

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void closeDBConnection() {
		try {
			cn.close();
			System.out.println("\nMysql connection closed.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getNumberOfReliableSentences() {
		String dbQuery = "SELECT count(sentence) FROM " + dbselectionTableName + " where reliable=true;";
		String str = queryTable(dbQuery);
		return Integer.parseInt(str);
	}

	/***
	 * Get a list of id's
	 * 
	 * @param table
	 *            cleanText, wordList, dbselection (no need to add locale) (NOTE: this function does not work for the
	 *            selectedSentences table, for this table use the function getIdListOfSelectedSentences ).
	 * @param condition
	 *            reliable, unknownWords, strangeSymbols, selected, unwanted = true/false (combined are posible:
	 *            "reliable=true and unwanted=false"); or condition=null for querying without condition.
	 * @return int array or null if the list of id's is empty.
	 */
	public int[] getIdListOfType(String table, String condition) {
		int num, i, j;
		int idSet[] = null;
		int maxNum = 500000;
		String getNum, getIds, getIdsShort;

		if (condition != null) {
			getNum = "SELECT count(id) FROM " + locale + "_" + table + " where " + condition + ";";
			getIds = "SELECT id FROM " + locale + "_" + table + " where " + condition;
		} else {
			getNum = "SELECT count(id) FROM " + locale + "_" + table + ";";
			getIds = "SELECT id FROM " + locale + "_" + table;
		}

		String str = queryTable(getNum);
		num = Integer.parseInt(str);
		// System.out.println("num = " + num);
		if (num > 0) {
			idSet = new int[num];
			i = 0;
			if (num > maxNum) {
				for (j = 0; j < num; j += maxNum) {
					try {
						getIdsShort = getIds + " limit " + j + "," + maxNum;
						// System.out.println("getIdsShort=" + getIdsShort);

						rs = st.executeQuery(getIdsShort);
						while (rs.next()) {
							idSet[i] = rs.getInt(1);
							i++;
						}
						System.out.println("  Num of ids retrieved = " + i);
						rs.close();

					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			} else { // if num < maxNum
				try {
					rs = st.executeQuery(getIds);
					while (rs.next()) {
						idSet[i] = rs.getInt(1);
						i++;
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		} else
			System.out.println("WARNING empty list for: " + getIds);
		// System.out.println("idSet.length=" + idSet.length);
		return idSet;
	}

	/**
	 * For the set of sentences identified by table and condition, retrieve from Mysql both the sentence ids and the corresponding
	 * features.
	 * 
	 * @param table
	 *            table
	 * @param condition
	 *            condition
	 * @return Pair&lt;int[], byte[][]&gt;(idSet, features)
	 */
	public Pair<int[], byte[][]> getIdsAndFeatureVectors(String table, String condition) {
		int num, i, j;
		int idSet[] = null;
		byte features[][] = null;
		int maxNum = 500000;
		String getNum, getIds, getIdsShort;

		if (condition != null) {
			getNum = "SELECT count(id) FROM " + locale + "_" + table + " where " + condition + ";";
			getIds = "SELECT id,features FROM " + locale + "_" + table + " where " + condition;
		} else {
			getNum = "SELECT count(id) FROM " + locale + "_" + table + ";";
			getIds = "SELECT id,features FROM " + locale + "_" + table;
		}

		String str = queryTable(getNum);
		num = Integer.parseInt(str);
		System.out.println(num + " sentences to retrieve...");
		// System.out.println("num = " + num);
		if (num > 0) {
			idSet = new int[num];
			features = new byte[num][];
			i = 0;
			if (num > maxNum) {
				for (j = 0; j < num; j += maxNum) {
					try {
						getIdsShort = getIds + " limit " + j + "," + maxNum;

						rs = st.executeQuery(getIdsShort);
						while (rs.next()) {
							idSet[i] = rs.getInt(1);
							features[i] = rs.getBytes(2);
							i++;
						}
						System.out.println("  Num of ids+features retrieved = " + i);
						rs.close();

					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			} else { // if num < maxNum
				try {
					rs = st.executeQuery(getIds);
					while (rs.next()) {
						idSet[i] = rs.getInt(1);
						features[i] = rs.getBytes(2);
						i++;
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		} else
			System.out.println("WARNING empty list for: " + getIds);
		// System.out.println("idSet.length=" + idSet.length);
		return new Pair<int[], byte[][]>(idSet, features);
	}

	/***
	 * Get a list of id's from a selected sentences table.<br>
	 * NOTE: use the actual table name: local + tableName + selectedsentences
	 * 
	 * 
	 * 
	 * @param actualTableName
	 *            = locale_tableName_selectedSentences
	 * @param condition
	 *            unwanted=true/false
	 * @return idSet
	 */
	public int[] getIdListOfSelectedSentences(String actualTableName, String condition) {
		int num, i, j;
		int idSet[] = null;
		String getNum, getIds;
		// String actualTableName = lang + "_" + tableName + "_selectedSentences";

		getNum = "SELECT count(dbselection_id) FROM " + actualTableName + " where " + condition + ";";
		getIds = "SELECT dbselection_id FROM " + actualTableName + " where " + condition + ";";

		String str = queryTable(getNum);
		num = Integer.parseInt(str);

		if (num > 0) {
			idSet = new int[num];

			try {
				rs = st.executeQuery(getIds);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				i = 0;
				while (rs.next()) {
					idSet[i] = rs.getInt(1);
					i++;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else
			System.out.println("WARNING empty list for: " + getIds);

		return idSet;
	}

	/***
	 * Get number of words in the wordList table.
	 * 
	 * @return int number of words.
	 * @param maxFrequency
	 *            max frequency of a word to be considered in the list, if maxFrequency=0 it will retrieve all the words with
	 *            frequency&ge;1.
	 */
	public int getNumberOfWords(int maxFrequency) {
		String where = "";
		if (maxFrequency > 0)
			where = "where frequency > " + maxFrequency;
		String dbQuery = "SELECT count(word) FROM " + wordListTableName + " " + where + ";";
		String str = queryTable(dbQuery);
		return Integer.parseInt(str);
	}

	/****
	 * Get the most frequent words and its frequency in a HashMap.
	 * 
	 * @param numWords
	 *            max number of words to retrieve, if numWords=0 then it will retrieve all the words in the list in descending
	 *            order of frequency.
	 * @param maxFrequency
	 *            max frequency of a word to be considered in the list, if maxFrequency=0 it will retrieve all the words with
	 *            frequency&ge;1.
	 * @return wordList
	 */
	public HashMap<String, Integer> getMostFrequentWords(int numWords, int maxFrequency) {

		HashMap<String, Integer> wordList;
		String dbQuery, where = "", word;
		int initialCapacity = 200000;
		byte wordBytes[];

		if (maxFrequency > 0)
			where = "where frequency > " + maxFrequency;

		if (numWords > 0) {
			dbQuery = "SELECT word,frequency FROM " + wordListTableName + " " + where + " order by frequency desc limit "
					+ numWords + ";";
			wordList = new HashMap<String, Integer>(numWords);
		} else {
			dbQuery = "SELECT word,frequency FROM " + wordListTableName + " " + where + " order by frequency desc";
			wordList = new HashMap<String, Integer>(initialCapacity);
		}

		try {
			rs = st.executeQuery(dbQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			while (rs.next()) {
				wordBytes = rs.getBytes(1);
				word = new String(wordBytes, "UTF8");
				wordList.put(word, new Integer(rs.getInt(2)));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) { // catch unsupported encoding exception
			e.printStackTrace();
		}

		return wordList;
	}

	/****
	 * Get the most frequent words sorted by frequency (descendent) in an ArrayList.
	 * 
	 * @param numWords
	 *            max number of words to retrieve, if numWords=0 then it will retrieve all the words in the list in descending
	 *            order of frequency.
	 * @param maxFrequency
	 *            max frequency of a word to be considered in the list, if maxFrequency=0 it will retrieve all the words with
	 *            frequency&ge;1.
	 * @return words
	 */
	public ArrayList<String> getMostFrequentWordsArray(int numWords, int maxFrequency) {

		ArrayList<String> words = new ArrayList<String>();
		;
		String dbQuery, where = "", word;
		int initialCapacity = 200000;
		byte wordBytes[];

		if (maxFrequency > 0)
			where = "where frequency > " + maxFrequency;

		if (numWords > 0)
			dbQuery = "SELECT word,frequency FROM " + wordListTableName + " " + where + " order by frequency desc limit "
					+ numWords + ";";
		else
			dbQuery = "SELECT word,frequency FROM " + wordListTableName + " " + where + " order by frequency desc";

		try {
			rs = st.executeQuery(dbQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			while (rs.next()) {
				wordBytes = rs.getBytes(1);
				word = new String(wordBytes, "UTF8");
				words.add(word);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) { // catch unsupported encoding exception
			e.printStackTrace();
		}

		return words;
	}

	/****
	 * 
	 * @param fileName
	 *            file to write the list
	 * @param order
	 *            word or frequency
	 * @param numWords
	 *            max number of words, if numWords=0 then it will retrieve all the words in the list.
	 * @param maxFrequency
	 *            max frequency of a word to be considered in the list, if maxFrequency=0 it will retrieve all the words with
	 *            frequency&ge;1.
	 */
	public void printWordList(String fileName, String order, int numWords, int maxFrequency) {
		PrintWriter pw;
		String dbQuery, where = "";
		String orderBy;
		byte wordBytes[];
		String word;

		if (maxFrequency > 0)
			where = "where frequency > " + maxFrequency;

		if (order.contentEquals("word"))
			orderBy = "word asc";
		else
			orderBy = "frequency desc";

		if (numWords > 0)
			dbQuery = "SELECT word,frequency from " + wordListTableName + " " + where + " order by " + orderBy + " limit "
					+ numWords + ";";
		else
			dbQuery = "SELECT word,frequency from " + wordListTableName + " " + where + " order by " + orderBy;

		try {
			rs = st.executeQuery(dbQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(fileName)), "UTF-8"));
			wordBytes = null;
			while (rs.next()) {
				wordBytes = rs.getBytes(1);
				word = new String(wordBytes, "UTF8");
				pw.println(word + " " + rs.getInt(2));
			}
			pw.close();

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) { // catch unsupported encoding exception
			e.printStackTrace();
		}
		System.out.println(wordListTableName + " printed in file: " + fileName + " ordered by " + order);

	}

	/***
	 * Get a sentence from a locale_dbselection table.
	 * 
	 * @param id
	 *            dbselection (no need to add locale)
	 * @return String sentence
	 */
	public String getDBSelectionSentence(int id) {
		String sentence = "";
		String dbQuery = "Select sentence FROM " + dbselectionTableName + " WHERE id=" + id;
		byte[] sentenceBytes = null;

		sentenceBytes = queryTableByte(dbQuery);
		try {
			sentence = new String(sentenceBytes, "UTF8");
			// System.out.println("  TEXT: " + text);
		} catch (Exception e) { // UnsupportedEncodedException
			e.printStackTrace();
		}

		return sentence;
	}

	/**
	 * Get a
	 * 
	 * @param tableName
	 *            tableName
	 * @param id
	 *            id
	 * @return sentence
	 */
	public String getSelectedSentence(String tableName, int id) {
		String sentence = "";
		String dbQuery = "Select sentence FROM " + tableName + " WHERE dbselection_id=" + id;
		byte[] sentenceBytes = null;

		sentenceBytes = queryTableByte(dbQuery);
		try {
			sentence = new String(sentenceBytes, "UTF8");
			// System.out.println("  TEXT: " + text);
		} catch (Exception e) { // UnsupportedEncodedException
			e.printStackTrace();
		}

		return sentence;
	}

	// Firts filtering:
	// get first the page_title and check if it is not Image: or Wikipedia:Votes_for_deletion/
	// maybe we can check also the length
	public String getTextFromWikiPage(String id, int minPageLength, StringBuilder old_id, PrintWriter pw) {
		String pageTitle, pageLen, dbQuery, textId, text = null;
		byte[] textBytes = null;
		int len;

		dbQuery = "Select page_title FROM " + locale + "_page WHERE page_id=" + id;
		pageTitle = queryTable(dbQuery);

		dbQuery = "Select page_len FROM " + locale + "_page WHERE page_id=" + id;
		pageLen = queryTable(dbQuery);
		len = Integer.parseInt(pageLen);

		if (len < minPageLength || pageTitle.contains("Wikipedia:") || pageTitle.contains("Image:")
				|| pageTitle.contains("Template:") || pageTitle.contains("Category:") || pageTitle.contains("List_of_")) {
			// System.out.println("PAGE NOT USED page title=" + pageTitle + " Len=" + len);
			/*
			 * dbQuery = "select rev_text_id from revision where rev_page=" + id; textId = queryTable(dbQuery); dbQuery =
			 * "select old_text from text where old_id=" + textId; text = queryTable(dbQuery); System.out.println("TEXT: " +
			 * text);
			 */
		} else {
			// System.out.println("PAGE page_id=" + id + " PAGE SELECTED page title=" + pageTitle + " Len=" + len);
			if (pw != null)
				pw.println("\nSELECTED PAGE TITLE=" + pageTitle + " Len=" + len);

			dbQuery = "select rev_text_id from " + locale + "_revision where rev_page=" + id;
			textId = queryTable(dbQuery);
			old_id.delete(0, old_id.length());
			old_id.insert(0, textId);

			dbQuery = "select old_text from " + locale + "_text where old_id=" + textId;
			textBytes = queryTableByte(dbQuery);
			try {
				text = new String(textBytes, "UTF8");
				// System.out.println("  TEXT: " + text);
			} catch (Exception e) { // UnsupportedEncodedException
				e.printStackTrace();
			}

		}
		return text;
	}

	public String getCleanText(int id) {
		String dbQuery, text = null;
		byte[] textBytes = null;

		dbQuery = " select cleanText from " + cleanTextTableName + " where id=" + id;
		textBytes = queryTableByte(dbQuery);

		try {
			text = new String(textBytes, "UTF8");
			// System.out.println("  TEXT: " + text);
		} catch (Exception e) { // UnsupportedEncodedException
			e.printStackTrace();
		}
		// once retrieved the text record mark it as processed
		updateTable("UPDATE " + cleanTextTableName + " SET processed=true WHERE id=" + id);

		return text;
	}

	/***
	 * Set a sentence record field as true/false in dbselection table.
	 * 
	 * @param id
	 *            id
	 * @param field
	 *            reliable, unknownWords, strangeSymbols, selected or unwanted = true/false
	 * @param fieldValue
	 *            true/false (as string)
	 */
	public void setSentenceRecord(int id, String field, boolean fieldValue) {
		String bval;
		if (fieldValue)
			bval = "true";
		else
			bval = "false";

		updateTable("UPDATE " + dbselectionTableName + " SET " + field + "=" + bval + " WHERE id=" + id);

	}

	/***
	 * This function updates the unwanted field as true/false of dbselection TABLE and selectedSentencesTable TABLE. <br>
	 * NOTE: use the actual table name: local + tableName + selectedsentences
	 * 
	 * @param actualTableName
	 *            including local and _selectedSentences
	 * @param id
	 *            id in dbselection table
	 * @param fieldValue
	 *            true/false
	 */
	public void setUnwantedSentenceRecord(String actualTableName, int id, boolean fieldValue) {
		String bval;
		if (fieldValue)
			bval = "true";
		else
			bval = "false";

		updateTable("UPDATE " + actualTableName + " SET unwanted=" + bval + " WHERE dbselection_id=" + id);
		updateTable("UPDATE " + dbselectionTableName + " SET unwanted=" + bval + " WHERE id=" + id);

	}

	/***
	 * Set a description for table = name, it checks if the table tablesDescription exist, if not it creates it.
	 * 
	 * @param tableName
	 *            the name of the table, it can not be null
	 * @param description
	 *            if no description set to null
	 * @param stopCriterion
	 *            if no stopCriterion set to null
	 * @param featuresDefinitionFileName
	 *            if no featuresDefinitionFileName set to null
	 * @param covDefConfigFileName
	 *            if no covDefConfigFileNamen set to null
	 */
	public void setTableDescription(String tableName, String description, String stopCriterion,
			String featuresDefinitionFileName, String covDefConfigFileName) {
		boolean descExists = false;
		int val = 0;

		if (tableName != null) {
			// check if tablesDescription exists
			if (tableExist("tablesDescription")) {
				// check if a description for that name already exist
				try {
					rs = st.executeQuery("SELECT id from tablesDescription where name='" + tableName + "';");
					while (rs.next()) {
						val = rs.getInt(1);
						if (val > 0)
							descExists = true;
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} else
				createTablesDescriptionTable();

			if (!descExists) {
				try {
					System.out.println("  Adding a description for the table " + tableName + " in TABLE = tablesDescription.");
					psTablesDescription.setString(1, tableName);

					if (description != null)
						psTablesDescription.setString(2, description);
					else
						psTablesDescription.setString(2, "");

					if (stopCriterion != null)
						psTablesDescription.setString(3, stopCriterion);
					else
						psTablesDescription.setString(3, "");

					// get the file and the content of the file
					if (featuresDefinitionFileName != null) {
						psTablesDescription.setString(4, featuresDefinitionFileName);
						String str = "", features = "";
						try {
							BufferedReader in = new BufferedReader(new FileReader(featuresDefinitionFileName));
							while ((str = in.readLine()) != null)
								features += str + "\n";
							in.close();
						} catch (IOException e) {
						}
						psTablesDescription.setString(5, features);
					} else {
						psTablesDescription.setString(4, "");
						psTablesDescription.setString(5, "");
					}

					// get the file and the content of the file
					if (covDefConfigFileName != null) {
						psTablesDescription.setString(6, covDefConfigFileName);
						String str = "", config = "";
						try {
							BufferedReader in = new BufferedReader(new FileReader(covDefConfigFileName));
							while ((str = in.readLine()) != null)
								config += str + "\n";
							in.close();
						} catch (IOException e) {
						}
						psTablesDescription.setString(7, config);
					} else {
						psTablesDescription.setString(6, "");
						psTablesDescription.setString(7, "");
					}

					psTablesDescription.execute();
					psTablesDescription.clearParameters();

				} catch (SQLException e) {
					e.printStackTrace();
				}

			} else
				// if a description for this name already exist
				System.out.println("  A description for the table " + tableName + " already exist in TABLE = tablesDescription.");
		} else
			// tableName can not be null
			System.out.println("  Error setting table description: tableName can not be null");

	}

	/***
	 * Get the description of the tableName
	 * 
	 * @param tableName
	 *            tableName
	 * @return and String array where: desc[0] tableName desc[1] description desc[2] stopCriterion desc[3]
	 *         featuresDefinitionFileName desc[4] featuresDefinitionFile desc[5] covDefConfigFileName desc[6] covDefConfigFile
	 */
	public String[] getTableDescription(String tableName) {
		String[] desc = new String[7];
		PreparedStatement psDesc = null;
		try {
			psDesc = cn.prepareStatement("SELECT * from tablesDescription where name='" + tableName + "';");
			rs = psDesc.executeQuery();
			while (rs.next()) {
				for (int i = 2; i < 9; i++) {
					desc[(i - 2)] = rs.getString(i); // the id is not returned
					// System.out.println("desc[" + i + "]=" + desc[(i-2)]);
				}
			}
			psDesc.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return desc;

	}

	private boolean updateTable(String sql) {
		String str = "";
		boolean res = false;

		try {
			res = st.execute(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;

	}

	private String queryTable(String dbQuery) {
		String str = "";
		// String dbQuery = "Select * FROM " + currentTable;
		// System.out.println("querying: " + dbQuery);
		try {
			rs = st.executeQuery(dbQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			while (rs.next()) {
				// String url = rs.getString(2);
				// str = rs.getString(field);
				str = rs.getString(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return str;

	}

	private byte[] queryTableByte(String dbQuery) {
		byte strBytes[] = null;
		// String dbQuery = "Select * FROM " + currentTable;
		// System.out.println("querying: " + dbQuery);
		try {
			rs = st.executeQuery(dbQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			while (rs.next()) {
				// String url = rs.getString(2);
				// str = rs.getString(field);
				// str = rs.getString(1);
				strBytes = rs.getBytes(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return strBytes;

	}

	public byte[] getFeatures(int id) {
		byte[] fea = null;
		String dbQuery = "Select features FROM " + dbselectionTableName + " WHERE id=" + id;
		// System.out.println("querying: " + dbQuery);
		try {
			rs = st.executeQuery(dbQuery);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			while (rs.next()) {
				fea = rs.getBytes(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return fea;

	}

	/**
	 * Bulk load a set of features as identified by their IDs.
	 * 
	 * @param ids
	 *            a sorted array of feature IDs.
	 * @return an array of coverage features, of the same length as the input array.
	 */
	public byte[][] getFeaturesBulk(int[] ids) {
		HashMap<Integer, byte[]> featuresSet = getFeaturesSet(0, ids.length - 1, ids);
		byte[][] data = new byte[ids.length][];
		for (int i = 0; i < ids.length; i++) {
			data[i] = featuresSet.get(ids[i]);
			if (data[i] == null) {
				throw new NullPointerException("Could not get features for sentence ID " + ids[i]);
			}
		}
		return data;
	}

	public HashMap<Integer, byte[]> getFeaturesSet(int ini, int end, int[] idList) {
		int id;
		int iniId = idList[ini];
		int endId = idList[end];
		byte[] f;

		PreparedStatement psFeaturesSet = null;
		boolean IdInRange = iniId > 0 && endId > 0;
		int initialCapacity = IdInRange ? endId - iniId : end - ini;
		HashMap<Integer, byte[]> feas = new HashMap<Integer, byte[]>(initialCapacity);

		try {
			if (IdInRange) {
				psFeaturesSet = cn.prepareStatement("SELECT id,features FROM " + dbselectionTableName
						+ " WHERE reliable=true and selected=false and unwanted=false and id>=? and id<=?");
				psFeaturesSet.setInt(1, iniId);
				psFeaturesSet.setInt(2, endId);
				rs = psFeaturesSet.executeQuery();

				while (rs.next()) {
					id = rs.getInt(1);
					f = rs.getBytes(2);
					feas.put(id, f);
					// System .out.println("adding id=" + id);
				}
			} else { // one or both ids are negative so we need to retrieve one by one
				System.out.println("getFeaturesSet: negative indexes retrieving the features one by one....");
				psFeaturesSet = cn.prepareStatement("Select features FROM " + dbselectionTableName + " WHERE id=?");

				for (int i = ini; i <= end; i++) {
					if (idList[i] > 0) {
						try {
							psFeaturesSet.setInt(1, idList[i]);
							rs = psFeaturesSet.executeQuery();
							while (rs.next()) {
								f = rs.getBytes(1);
								feas.put(idList[i], f);
								// System .out.println("adding id=" + id);
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				} // end for loop
			} // end else, the indexes are negative
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			FileUtils.close(psFeaturesSet);
		}
		return feas;
	}

	/**
	 * The following characteres should be escaped: \0 An ASCII 0 (NUL) character. \' A single quote (“'”) character.
	 * \"  A double quote (“"”) character.
	 * 
	 * @param str
	 *            str
	 * @return str
	 */
	public String mysqlEscapeCharacters(String str) {

		str = str.replace("\0", "");
		str = str.replace("'", "\'");
		str = str.replace("\"", "\\\"");

		return str;
	}

	public static void main(String[] args) throws Exception {

		DBHandler wikiDB = new DBHandler("es");

		wikiDB.createDBConnection(args[0], args[1], args[2], args[3]);

		int numWords = wikiDB.getNumberOfWords(0);
		System.out.println("numWords=" + numWords);

		// wikiDB.printWordList("./tmp.txt", "frequency", 0, 0);
		/*
		 * ArrayList<String> w = new ArrayList<String>(); HashMap<String, Integer> wordList = wikiDB.getMostFrequentWords(0, 0,
		 * w);
		 * 
		 * for(int i=0; i<w.size(); i++){ System.out.println(w.get(i) + "  :  " + wordList.get(w.get(i))); }
		 */

		ArrayList<String> w = wikiDB.getMostFrequentWordsArray(0, 0);
		for (int i = 0; i < w.size(); i++)
			System.out.println(w.get(i));

		wikiDB.closeDBConnection();

	}

}
