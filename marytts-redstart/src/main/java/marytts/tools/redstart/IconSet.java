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
package marytts.tools.redstart;

import java.net.URL;

import javax.swing.ImageIcon;

/**
 * 
 * @author Mat Wilson &lt;mat.wilson@dfki.de&gt;
 */
public class IconSet {

	// Icon locations
	public static final ImageIcon STOP_16X16 = new ImageIcon(AdminWindow.class.getResource("stopped_16x16.png"));
	public static final ImageIcon STOP_48X48 = new ImageIcon(AdminWindow.class.getResource("stopped_48x48.png"));
	public static final ImageIcon STOP_64X64 = new ImageIcon(AdminWindow.class.getResource("stopped_64x64.png"));
	public static final ImageIcon PLAY_16X16 = new ImageIcon(AdminWindow.class.getResource("playing_16x16.png"));
	public static final ImageIcon PLAY_48X48 = new ImageIcon(AdminWindow.class.getResource("playing_48x48.png"));
	public static final ImageIcon PLAY_64X64 = new ImageIcon(AdminWindow.class.getResource("playing_64x64.png"));
	public static final ImageIcon REC_48X48 = new ImageIcon(AdminWindow.class.getResource("recording_48x48.png"));
	public static final ImageIcon REC_64X64 = new ImageIcon(AdminWindow.class.getResource("recording_64x64.png"));
	public static final ImageIcon REC_16X16 = new ImageIcon(AdminWindow.class.getResource("recording_16x16.png"));
	public static final ImageIcon INFO_16X16 = new ImageIcon(AdminWindow.class.getResource("info_16x16.png"));
	public static final ImageIcon WARNING_16X16 = new ImageIcon(AdminWindow.class.getResource("warning_16x16.png"));
	public static final ImageIcon LOGO_48x48 = new ImageIcon(AdminWindow.class.getResource("redstop_48x48.png"));
	public static final URL LOGO_16x16_URL = AdminWindow.class.getResource("redstop_16x16.png");

	// Icons for recording status in prompt table; may be re-used in Options dialog message bar
	// public static final ImageIcon CLIP_NO_16x16 = new ImageIcon(AdminWindow.class.getResource("no-clipping_16x16.png"));
	// public static final ImageIcon CLIP_YES_16x16 = new ImageIcon(AdminWindow.class.getResource("clipping_16x16.png"));
	// public static final ImageIcon CLIP_ALMOST_16x16 = new ImageIcon(AdminWindow.class.getResource("clip-warning_16x16.png"));

	/** Creates a new instance of Icon */
	public IconSet() {
	}

}
