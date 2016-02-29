/**
 * Copyright 2000-2006 DFKI GmbH.
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

import java.io.File;

import javax.swing.filechooser.FileFilter;

import marytts.util.MaryUtils;

/**
 * A simple file filter accepting files with a given extension.
 * 
 * @author Marc Schr&ouml;der
 */
public class SimpleFileFilter extends FileFilter {
	String extension;
	String description;

	public SimpleFileFilter(String extension, String description) {
		this.extension = extension;
		this.description = description;
	}

	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}
		String ext = MaryUtils.getExtension(f);
		if (ext != null) {
			return ext.equals(extension);
		}
		return false;
	}

	public String getDescription() {
		return description;
	}

	public String getExtension() {
		return extension;
	}
}
