/*
 * Icon.java
 *
 * Created on June 24, 2007, 12:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
