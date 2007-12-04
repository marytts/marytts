/**
 * Copyright 2007 DFKI GmbH.
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
package de.dfki.lt.mary.recsessionmgr.gui;

import java.net.URL;

import javax.swing.ImageIcon;

/**
 *
 * @author Mat Wilson <mat.wilson@dfki.de>
 */
public class IconSet {
    
    // Icon locations
    public static final ImageIcon STOP_16X16      = new ImageIcon(AdminWindow.class.getResource("icons/stopped_16x16.png"));
    public static final ImageIcon STOP_48X48      = new ImageIcon(AdminWindow.class.getResource("icons/stopped_48x48.png"));
    public static final ImageIcon STOP_64X64      = new ImageIcon(AdminWindow.class.getResource("icons/stopped_64x64.png"));
    public static final ImageIcon PLAY_16X16      = new ImageIcon(AdminWindow.class.getResource("icons/playing_16x16.png"));
    public static final ImageIcon PLAY_48X48      = new ImageIcon(AdminWindow.class.getResource("icons/playing_48x48.png"));
    public static final ImageIcon PLAY_64X64      = new ImageIcon(AdminWindow.class.getResource("icons/playing_64x64.png"));
    public static final ImageIcon REC_48X48       = new ImageIcon(AdminWindow.class.getResource("icons/recording_48x48.png"));
    public static final ImageIcon REC_64X64       = new ImageIcon(AdminWindow.class.getResource("icons/recording_64x64.png"));
    public static final ImageIcon REC_16X16       = new ImageIcon(AdminWindow.class.getResource("icons/recording_16x16.png"));
    public static final ImageIcon INFO_16X16      = new ImageIcon(AdminWindow.class.getResource("icons/info_16x16.png"));
    public static final ImageIcon WARNING_16X16   = new ImageIcon(AdminWindow.class.getResource("icons/warning_16x16.png"));
    public static final ImageIcon LOGO_48x48      = new ImageIcon(AdminWindow.class.getResource("icons/redstop_48x48.png"));
    public static final URL       LOGO_16x16_URL  = AdminWindow.class.getResource("icons/redstop_16x16.png");
    
    // Icons for recording status in prompt table; may be re-used in Options dialog message bar
    //public static final ImageIcon CLIP_NO_16x16     = new ImageIcon(AdminWindow.class.getResource("icons/no-clipping_16x16.png"));
    //public static final ImageIcon CLIP_YES_16x16    = new ImageIcon(AdminWindow.class.getResource("icons/clipping_16x16.png"));
    //public static final ImageIcon CLIP_ALMOST_16x16 = new ImageIcon(AdminWindow.class.getResource("icons/clip-warning_16x16.png"));
    
    /** Creates a new instance of Icon */
    public IconSet() {
    }
    
}
