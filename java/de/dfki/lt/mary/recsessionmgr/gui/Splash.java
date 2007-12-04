/*
 * Splash.java
 *
 * Created on June 28, 2007, 7:52 PM
 *
 */

package de.dfki.lt.mary.recsessionmgr.gui;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JWindow;

/**
 * Code for shadowed window taken from http://www.javalobby.org/java/forums/t17720
 * @author (taken by) Mat Wilson <mat.wilson@dfki.de>
 */
 
public class Splash extends JWindow {
    private BufferedImage splash = null;
 
    public Splash() throws IOException
    {
        this(ImageIO.read(Splash.class.getResourceAsStream("images/splash_bg.png")));
    }
    
    public Splash(BufferedImage image) {
        createShadowPicture(image);
    }
 
    public void paint(Graphics g) {
        if (splash != null) {
            g.drawImage(splash, 0, 0, null);
        }
    }
    
    private void createShadowPicture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int extra = 0;        
        if (System.getProperty("os.name").equalsIgnoreCase("Windows XP")) {
            extra = 14; // Only create shadow if Windows XP (avoids double shadow in Mac OS; not tested for other OSes)    
        }        
 
        setSize(new Dimension(width + extra, height + extra));
        setLocationRelativeTo(null);
        Rectangle windowRect = getBounds();
 
        splash = new BufferedImage(width + extra, height + extra, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) splash.getGraphics();
 
        try {
            Robot robot = new Robot(getGraphicsConfiguration().getDevice());
            BufferedImage capture = robot.createScreenCapture(new Rectangle(windowRect.x, windowRect.y, windowRect.width + extra, windowRect.height + extra));
            g2.drawImage(capture, null, 0, 0);
        } catch (AWTException e) { }
 
        BufferedImage shadow = new BufferedImage(width + extra, height + extra, BufferedImage.TYPE_INT_ARGB); 
        Graphics g = shadow.getGraphics();
        g.setColor(new Color(0.0f, 0.0f, 0.0f, 0.3f));
        g.fillRoundRect(6, 6, width, height, 12, 12);
 
        g2.drawImage(shadow, getBlurOp(7), 0, 0);
        g2.drawImage(image, 0, 0, this);
    }
 
    private ConvolveOp getBlurOp(int size) {
        float[] data = new float[size * size];
        float value = 1 / (float) (size * size);
        for (int i = 0; i < data.length; i++) {
            data[i] = value;
        }
        return new ConvolveOp(new Kernel(size, size, data));
    }
 
}

