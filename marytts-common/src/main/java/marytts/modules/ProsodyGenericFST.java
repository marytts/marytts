/*
 * Copyright (C) 2005 DFKI GmbH. All rights reserved.
 */
package marytts.modules;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

import marytts.datatypes.MaryDataType;
import marytts.fst.FSTLookup;

public class ProsodyGenericFST extends ProsodyGeneric {

	public ProsodyGenericFST(MaryDataType inputType, MaryDataType outputType, Locale locale, String tobipredFileName,
			String accentPriorities, String syllableAccents, String paragraphDeclination) throws IOException {
		super(inputType, outputType, locale, tobipredFileName, accentPriorities, syllableAccents, paragraphDeclination);
	}

	/**
	 * Read a list from an external file. This implementation can read from finite state transducer files (filenames ending in
	 * <code>.fst</code>).
	 * 
	 * @param resourceName
	 *            resource file in classpath from which to read the list; suffix identifies list format.
	 * @return An Object representing the list; checkList() must be able to make sense of this. This implementation returns an
	 *         FSTLookup for .fst files or a Set for .txt files.
	 * @throws IllegalArgumentException
	 *             if the fileName suffix cannot be identified as a list file format.
	 */
	@Override
	protected Object readListFromResource(String resourceName) throws IOException {
		String suffix = resourceName.substring(resourceName.length() - 4, resourceName.length());
		if (suffix.equals(".fst")) { // FST file
			InputStream resourceStream = this.getClass().getResourceAsStream("prosody/" + resourceName);
			// put the external FST on the map
			return new FSTLookup(resourceStream, resourceName, "ISO-8859-1");
		} else {
			return super.readListFromResource(resourceName);
		}
	}

	/**
	 * Checks if tokenValue is contained in list. This implementation is able to deal with list types represented as FSTLookups or
	 * as Sets.
	 * 
	 * @param currentVal
	 *            the condition to check; can be either <code>INLIST:</code> or <code>!INLIST:</code> followed by the list name to
	 *            check.
	 * @param tokenValue
	 *            value to look up in the list
	 * @return whether or not tokenValue is contained in the list.
	 */
	@Override
	protected boolean checkList(String currentVal, String tokenValue) {
		if (currentVal == null || tokenValue == null) {
			throw new NullPointerException("Received null argument");
		}
		if (!currentVal.startsWith("INLIST") && !currentVal.startsWith("!INLIST")) {
			throw new IllegalArgumentException("currentVal does not start with INLIST or !INLIST");
		}
		boolean negation = currentVal.startsWith("!");
		String listName = currentVal.substring(currentVal.indexOf(":") + 1);
		Object listObj = listMap.get(listName);
		if (listObj == null)
			return false; // no list found
		boolean contains;
		if (listObj instanceof Set) {
			Set set = (Set) listObj;
			contains = set.contains(tokenValue);
		} else if (listObj instanceof FSTLookup) {
			FSTLookup fst = (FSTLookup) listObj;
			contains = fst.lookup(tokenValue).length > 0;
		} else {
			throw new IllegalArgumentException("Unknown list representation: " + listObj);
		}
		if (contains && negation || !contains && !negation)
			return false;
		else
			return true;
	}

}
