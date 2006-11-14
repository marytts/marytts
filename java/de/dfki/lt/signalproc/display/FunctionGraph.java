/**
 * Copyright 2004-2006 DFKI GmbH.
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

package de.dfki.lt.signalproc.display;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import de.dfki.lt.util.PrintfFormat;

/**
 * @author Marc Schr&ouml;der
 *
 */
public class FunctionGraph extends JPanel implements CursorSource, CursorListener
{
    public static final int DEFAULT_WIDTH=640;
    public static final int DEFAULT_HEIGHT=480;
    public static final int DRAW_LINE = 1;
    public static final int DRAW_DOTS = 2;
    public static final int DRAW_LINEWITHDOTS = 3;
    public static final int DRAW_HISTOGRAM = 4;
    public static final int DOT_FULLCIRCLE = 1;
    public static final int DOT_FULLSQUARE = 2;
    public static final int DOT_FULLDIAMOND = 3;
    public static final int DOT_EMPTYCIRCLE = 11;
    public static final int DOT_EMPTYSQUARE = 12;
    public static final int DOT_EMPTYDIAMOND = 13;
    
    protected int paddingLeft = 40;
    protected int paddingRight = 10;
    protected int paddingTop = 10;
    protected int paddingBottom = 40;
    protected double x0;
    protected double xStep;
    protected double[] y;
    protected double ymin;
    protected double ymax;
    protected boolean showXAxis = true;
    protected boolean showYAxis = true;
    protected BufferedImage graphImage = null;
    protected Color backgroundColor = Color.WHITE;
    protected Color axisColor = Color.BLACK;
    protected Color graphColor = Color.BLUE;
    protected Color histogramBorderColor = Color.BLACK;
    protected int graphStyle = DRAW_LINE;
    protected int dotStyle = DOT_FULLCIRCLE;
    protected int dotSize = 6;
    protected int histogramWidth = 10;

    // data to be used for drawing cursor et al on the GlassPane:
    // x and y coordinates, in data space
    protected DoublePoint positionCursor = new DoublePoint();
    protected DoublePoint rangeCursor = new DoublePoint();
    protected List cursorListeners = new ArrayList();
    
    /**
     * Display a 2d graph showing y(x), with labelled scales.
     * This constructor is for subclasses only, which may need
     * to perform some operations before calling initialise().
     */
    protected FunctionGraph()
    {
        super();
    }

    /**
     * Display a 2d graph showing y(x), with labelled scales.
     */
    public FunctionGraph(double x0, double xStep,  double[] y) {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, x0, xStep, y);
    }

    /**
     * Display a 2d graph showing y(x), with labelled scales.
     */
    public FunctionGraph(int width, int height, 
                   double x0, double xStep,  double[] y) {
        super();
        initialise(width, height, x0, xStep, y);
   }
   
   public void initialise(int width, int height, 
                    double x0, double xStep,  double[] y)
   {
       setPreferredSize(new Dimension(width, height));
       setOpaque(true);
       this.addMouseListener(new MouseListener() {
           public void mouseClicked(MouseEvent e) {
               //System.err.println("Mouse clicked");
               if (e.getButton() == MouseEvent.BUTTON1) { // left mouse button
                   // set position cursor; if we are to the right of rangeCursor,
                   // delete rangeCursor.
                   positionCursor.x = imageX2X(e.getX()-paddingLeft);
                   positionCursor.y = imageY2Y(getHeight()-paddingBottom-e.getY());
                   if (!Double.isNaN(rangeCursor.x) && positionCursor.x > rangeCursor.x) {
                       rangeCursor.x = Double.NaN;
                   }
               } else if (e.getButton() == MouseEvent.BUTTON3) { // right mouse button
                   // set range cursor, but only if we are to the right of positionCursor
                   rangeCursor.x = imageX2X(e.getX()-paddingLeft);
                   rangeCursor.y = imageY2Y(getHeight()-paddingBottom-e.getY());
                   if (positionCursor.x > rangeCursor.x) {
                       rangeCursor.x = Double.NaN;
                   }                    
               }
               FunctionGraph.this.notifyCursorListeners();
               FunctionGraph.this.requestFocusInWindow();
           }
           public void mousePressed(MouseEvent e) {}
           public void mouseReleased(MouseEvent e) {}
           public void mouseEntered(MouseEvent e) {}
           public void mouseExited(MouseEvent e) {}
       });
       updateData(x0, xStep, y);
   }
   
   protected void updateData(double x0, double xStep, double[] y)
   {
       this.x0 = x0;
        this.xStep = xStep;
        this.y = new double[y.length];
        System.arraycopy(y, 0, this.y, 0, y.length);
        ymin = Double.NaN;
        ymax = Double.NaN;
        for (int i=0; i<y.length; i++) {
            if (Double.isNaN(y[i])) // missing value -- skip
                continue;
            if (Double.isNaN(ymin)) {
                assert Double.isNaN(ymax);
                ymin = y[i];
                ymax = y[i];
                continue;
            }
            if (y[i] < ymin) ymin = y[i];
            else if (y[i] > ymax) ymax = y[i];
        }
        
        // If the x axis is painted in the middle (ymin << 0),
        // we need much less paddingBottom:
        if (ymin < 0) {
            paddingBottom = paddingTop;
        }
        // And invalidate any previous graph image:
        graphImage = null;
    }

    public double getZoomX()
    {
        double zoom = ((double)getPreferredSize().width-paddingLeft-paddingRight) / y.length;
        //System.err.println("Current Zoom: " + zoom + "(pref. size: " + getPreferredSize().width + "x" + getPreferredSize().height + ")");
        return zoom;
    }

    /**
     * Set the zoom of the X 
     * @param factor the zoom factor for X; 1 means that each data point corresponds to one pixel;
     * 0.5 means that 2 data points are mapped onto one pixel; etc.
     */
    public void setZoomX(double factor)
    {
        //System.err.println("New zoom factor requested: " + factor);
        // Old visible rectangle:
        Rectangle r = getVisibleRect();
        int oldWidth = getPreferredSize().width;
        int newWidth = (int)(y.length*factor)+paddingLeft+paddingRight;
        if (isVisible()) {
            setVisible(false);
            setPreferredSize(new Dimension(newWidth, getPreferredSize().height));
            // Only scroll to center of what was previous visible if not at left end:
            if (r.x != 0) {
                Rectangle newVisibleRect = new Rectangle((r.x+r.width/2-paddingLeft)*newWidth/oldWidth-r.width/2+paddingLeft, r.y, r.width, r.height);
                scrollRectToVisible(newVisibleRect);
            }
            setVisible(true);
        } else {
            setPreferredSize(new Dimension(newWidth, getPreferredSize().height));
            createGraphImage();
        }
        //System.err.print("updated ");
        getZoomX();
    }

    protected void createGraphImage()
    {
        graphImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        if (graphImage == null) {
            throw new NullPointerException("Cannot create image for drawing graph");
        }
        Graphics2D g = (Graphics2D) graphImage.createGraphics();
        double width = getWidth();
        double height = getHeight();

        int image_fromX = 0;
        int image_toX = (int) width;
        
        g.setBackground(backgroundColor);
        g.clearRect(0, 0, (int) width, (int) height);
        g.setFont(new Font("Courier", 0, 10));
        // Now reduce the drawing area:
        int startX = paddingLeft;
        int startY = (int)height - paddingBottom;
        width -= paddingLeft + paddingRight;
        height -= paddingTop + paddingBottom;
        // Make sure we are not trying to draw the function outside its area:
        if (image_fromX < startX) image_fromX = startX;
        if (image_toX > startX + width) image_toX = (int) (startX + width);
        

        int image_y_origin;
        if (getYRange() == 0) image_y_origin = startY;
        else image_y_origin = startY - (int) ((-ymin/getYRange()) * height);
        int image_x_origin = startX + (int) ((-x0/getXRange()) * width);

        // Draw the function itself:
        if (getYRange() > 0) {
            drawData(g, image_fromX-startX, image_toX-startX, startX, image_y_origin, startY, (int) height);
        }
        
        // Draw the x axis, if requested:
        if (showXAxis) {
            if (startY >= image_y_origin && image_y_origin >= startY-height) {
                drawXAxis(g, width, startX, startY, image_y_origin);
            } else { // draw x axis at the bottom, even if that is not 0:
                drawXAxis(g, width, startX, startY, startY);
            }
        }

        // Draw the y axis, if requested:
        if (showYAxis) {
            if (image_fromX <= image_x_origin && image_x_origin <= image_toX) {
                drawYAxis(g, height, startX, startY, image_x_origin);
            } else { // draw y axis at the left, even if that is not 0:
                drawYAxis(g, height, startX, startY, startX);
            }
        }
    }

    /**
     * While painting the graph, draw the actual function data.
     * @param g the graphics2d object to paint in
     * @param image_fromX first visible X coordinate of the Graph display area (= after subtracting space reserved for Y axis)
     * @param image_toX last visible X coordinate of the Graph display area (= after subtracting space reserved for Y axis)
     * @param image_refX X coordinate of the origin, in the display area
     * @param image_refY Y coordinate of the origin, in the display area
     * @param xScaleFactor conversion factor between data space and image space, image_x = xScaleFactor * data_x
     * @param yScaleFactor conversion factor between data space and image space, image_y = yScaleFactor * data_y
     * @param startY the start position on the Y axis (= the lower bound of the drawing area)
     * @param image_height the height of the drawable region for the y values
     */
    protected void drawData(Graphics2D g,
        int image_fromX, int image_toX,
        int image_refX, int image_refY,
        int startY, int image_height) {
        int index_fromX = imageX2indexX(image_fromX);
        if (index_fromX < 0) index_fromX = 0;
        int index_toX   = imageX2indexX(image_toX);
        if (index_toX < y.length) index_toX += 20;
        if (index_toX > y.length) index_toX = y.length;
        //System.err.println("Drawing values " + index_fromX + " to " + index_toX + " of " + y.length);
        double xo = 0.0;
        double yo = 0.0;
        double xp = 0.0;
        double yp = 0.0;
        g.setColor(graphColor);
        for (int i = index_fromX; i < index_toX; i++) {
            if (!Double.isNaN(y[i])) {
                xp = indexX2imageX(i);
                yp = y2imageY(y[i]);
                //System.err.println("Point "+i+": ("+(image_refX+(int)xp)+","+(image_refY-(int)yp)+")");
                if (graphStyle == DRAW_LINE || graphStyle == DRAW_LINEWITHDOTS) {
                    g.drawLine(image_refX+(int)xo, image_refY-(int)yo, image_refX+(int)xp, image_refY-(int)yp);
                }
                if (graphStyle == DRAW_DOTS || graphStyle == DRAW_LINEWITHDOTS) {
                    drawDot(g, image_refX+(int)xp, image_refY-(int)yp);
                }
                if (graphStyle == DRAW_HISTOGRAM) {
                    int topY = image_refY;
                    if (yp>0) topY = image_refY-(int)yp;
                    int histHeight = (int) Math.abs(yp);
                    // cut to drawing area if x axis not at y==0:
                    if (topY+histHeight>startY) {
                        histHeight = startY-topY;
                    }
                    g.setColor(graphColor);
                    g.fillRect(image_refX+(int)xp-histogramWidth/2, topY, histogramWidth, histHeight);
                    g.setColor(histogramBorderColor);
                    g.drawRect(image_refX+(int)xp-histogramWidth/2, topY, histogramWidth, histHeight);
                }                
                xo = xp;
                yo = yp;
            }
        }
    }

    protected void drawDot(Graphics2D g, int x, int y)
    {
        switch(dotStyle) {
            case DOT_FULLCIRCLE:
                g.fillOval(x-dotSize/2, y-dotSize/2, dotSize, dotSize);
                break;
            case DOT_FULLSQUARE:
                g.fillRect(x-dotSize/2, y-dotSize/2, dotSize, dotSize);
                break;
            case DOT_FULLDIAMOND:
                g.fillPolygon(new int[]{x-dotSize/2,x, x+dotSize/2, x},
                              new int[]{y, y-dotSize/2, y, y+dotSize/2}, 4);
                break;
            case DOT_EMPTYCIRCLE:
                g.drawOval(x-dotSize/2, y-dotSize/2, dotSize, dotSize);
                break;
            case DOT_EMPTYSQUARE:
                g.drawRect(x-dotSize/2, y-dotSize/2, dotSize, dotSize);
                break;
            case DOT_EMPTYDIAMOND:
                g.drawPolygon(new int[]{x-dotSize/2,x, x+dotSize/2, x},
                              new int[]{y, y-dotSize/2, y, y+dotSize/2}, 4);
                break;
            default:
                break;
        }
    }
    
    protected void drawYAxis(Graphics2D g, double height, int startX, int startY, int image_x_origin) {
        g.setColor(axisColor);
        double yRange = getYRange();
        g.drawLine(image_x_origin, startY, image_x_origin, startY-(int)height);
        // Do not try to draw units if yRange is 0:
        if (yRange == 0) return;
        // Units on the y axis:
        // major units with labels every 50-100 pixels
        int unitOrder = (int)Math.floor(Math.log(yRange/5)/Math.log(10));
        double unitDistance = Math.pow(10, unitOrder);
        double image_unitDistance = unitDistance/yRange * height;
        if (image_unitDistance < 20) {
            unitDistance *= 5;
        } else if (image_unitDistance < 50) {
            unitDistance *= 2;
        }
        double unitStart = ymin;
        double modulo = ymin%unitDistance;
        if (modulo != 0) {
            if (modulo > 0)
                unitStart += unitDistance - modulo;
            else // < 0
                unitStart += Math.abs(modulo);
        }
        PrintfFormat labelFormat;
        if (unitOrder > 0) {
            labelFormat = new PrintfFormat("%.0f");
        } else {
            labelFormat = new PrintfFormat("%." + (-unitOrder) + "f");
        }
        boolean intLabels = ((int)unitDistance == (int)Math.ceil(unitDistance));
        //System.err.println("y axis: yRange=" + yRange + ", unitDistance=" + unitDistance + ", unitStart=" + unitStart + ", ymin=" + ymin + ", ymin%unitDistance=" + (ymin%unitDistance));
        for (double i=unitStart; i<ymax; i+= unitDistance) {
            double yunit = (i-ymin)/yRange * height;
            g.drawLine(image_x_origin+5, startY-(int)yunit, image_x_origin-5, startY-(int)yunit);
            // labels to the left of y axis:
            g.drawString(labelFormat.sprintf(i), image_x_origin-30, startY-(int)yunit+5);
        }
    }

    protected void drawXAxis(Graphics2D g, double width, int startX, int startY, int image_y_origin) {
        g.setColor(axisColor);
        double xRange = getXRange();
        //System.err.println("Drawing X axis from " + startX + " to " + startX+(int)width + "; startY="+startY+", image_y_origin="+image_y_origin);
        g.drawLine(startX, image_y_origin, startX+(int)width, image_y_origin);
        // Units on the x axis:
        // major units with labels every 50-100 pixels
        int nUnits = (int) width / 50;
        int unitOrder = (int) Math.floor(Math.log(xRange/nUnits)/Math.log(10));
        double unitDistance = Math.pow(10, unitOrder);
        double image_unitDistance = unitDistance/xRange * width;
        if (image_unitDistance < 20) {
            unitDistance *= 5;
        } else if (image_unitDistance < 50) {
            unitDistance *= 2;
        }
        double unitStart = x0;
        double modulo = x0%unitDistance;
        if (modulo != 0) {
            if (modulo > 0)
                unitStart += unitDistance - modulo;
            else // < 0
                unitStart += Math.abs(modulo);
        }
        PrintfFormat labelFormat;
        if (unitOrder > 0) {
            labelFormat = new PrintfFormat("%.0f");
        } else {
            labelFormat = new PrintfFormat("%." + (-unitOrder) + "f");
        }
        //System.err.println("x axis: xRange=" + xRange + ", unitDistance=" + unitDistance + ", unitStart=" + unitStart + ", x0=" + x0 + ", image_unitDistance=" + image_unitDistance);
        for (double i=unitStart; i<x0+xRange; i+= unitDistance) {
            double xunit = (i-x0)/xRange * width;
            //System.err.println("Drawing unit at " + (startX+(int)xunit));
            g.drawLine(startX+(int)xunit, image_y_origin+5, startX+(int)xunit, image_y_origin-5);
            // labels below x axis:
            g.drawString(labelFormat.sprintf(i), startX + (int)xunit-10, image_y_origin+20);
        }
    }
    
    public void paintComponent(Graphics gr) {
        if (graphImage == null
            || getWidth() != graphImage.getWidth()
            || getHeight() != graphImage.getHeight()) {
            createGraphImage();
        }
        Graphics2D g = (Graphics2D) gr;
        g.drawImage(graphImage, null, null);
    }

    protected int imageX2indexX(int imageX)
    {
        double xScaleFactor = ((double) getWidth()-paddingLeft-paddingRight)/y.length;
        return (int) (imageX / xScaleFactor);
    }

    protected double imageX2X(int imageX)
    {
        
        double xScaleFactor = ((double)getWidth()-paddingLeft-paddingRight)/(y.length*xStep);
        return x0 + imageX / xScaleFactor;
    }

    protected int indexX2imageX(int indexX)
    {
        double xScaleFactor = ((double)getWidth()-paddingLeft-paddingRight)/y.length;
        return (int) (indexX * xScaleFactor);
    }

    protected int X2imageX(double x)
    {
        double xScaleFactor = ((double)getWidth()-paddingLeft-paddingRight)/(y.length*xStep);
        return (int) ((x - x0) * xScaleFactor);
    }

    protected int X2indexX(double x)
    {
        return (int) ((x - x0) / xStep);
    }
    
    protected double imageY2Y(int imageY)
    {
        
        double yScaleFactor = ((double) getHeight()-paddingTop-paddingBottom)/getYRange();
        return imageY / yScaleFactor;
    }

    protected int y2imageY(double y)
    {
        
        double yScaleFactor = ((double) getHeight()-paddingTop-paddingBottom)/getYRange();
        return (int) (y * yScaleFactor);
    }

    protected double getYRange()
    {
        double yRange = ymax - ymin;
        if (Double.isNaN(yRange)) yRange = 0;
        return yRange;
    }
    
    protected double getXRange()
    {
        double xRange = y.length * xStep;
        return xRange;
    }
    
    public CursorDisplayer.CursorLine getPositionCursor()
    {
        if (Double.isNaN(positionCursor.x)) return null;
        return new CursorDisplayer.CursorLine(this, paddingLeft+X2imageX(positionCursor.x),
                paddingTop, getHeight()-paddingBottom);
    }

    public CursorDisplayer.CursorLine getRangeCursor()
    {
        if (Double.isNaN(rangeCursor.x)) return null;
        int imageX = X2imageX(rangeCursor.x);
        return new CursorDisplayer.CursorLine(this, paddingLeft+X2imageX(rangeCursor.x),
                paddingTop, getHeight()-paddingBottom, Color.YELLOW);
    }

    public CursorDisplayer.Label getValueLabel()
    {
        if (Double.isNaN(positionCursor.x)) return null;
        int imageX = X2imageX(positionCursor.x) + 10;
        int imageY = paddingTop + 10;
        return new CursorDisplayer.Label(this, getLabel(positionCursor.x, positionCursor.y),
                imageX, imageY);
    }

    public void addCursorListener(CursorListener l)
    {
        cursorListeners.add(l);
    }
    
    public CursorListener[] getCursorListeners()
    {
        return (CursorListener[]) cursorListeners.toArray(new CursorListener[0]);
    }
    
    public boolean removeCursorListener(CursorListener l)
    {
        return cursorListeners.remove(l);
    }
    
    protected void notifyCursorListeners()
    {
        for (Iterator it = cursorListeners.iterator(); it.hasNext(); ) {
            CursorListener l = (CursorListener) it.next();
            l.updateCursorPosition(new CursorEvent(this));
        }
    }
    
    /**
     * Used when keeping several FunctionGraphs' cursor positions in synchrony.
     * Register each other as cursor listeners before the glass pane; whichever gets
     * clicked causes the others to be updated. Make sure to add any peers _before_
     * any displaying cursor listeners, to make sure all are in line before being
     * displayed.
     */
    public void updateCursorPosition(CursorEvent e)
    {
        FunctionGraph source = e.getSource();
        positionCursor.x = source.positionCursor.x;
        rangeCursor.x = source.rangeCursor.x;
    }

    
    public JFrame showInJFrame(String title, boolean allowZoom, boolean exitOnClose)
    {
        return showInJFrame(title, DEFAULT_WIDTH, DEFAULT_HEIGHT + 50, allowZoom, true, exitOnClose);
    }
    
    public JFrame showInJFrame(String title, boolean allowZoom, boolean showControls, boolean exitOnClose)
    {
        return showInJFrame(title, DEFAULT_WIDTH, DEFAULT_HEIGHT + 50, allowZoom, showControls, exitOnClose);
    }

    public JFrame showInJFrame(String title, int width, int height, boolean allowZoom, boolean exitOnClose)
    {
        return showInJFrame(title, width, height, allowZoom, true, exitOnClose);
    }

    public JFrame showInJFrame(String title, int width, int height, boolean allowZoom, boolean showControls, boolean exitOnClose)
    {
        final JFrame main = new JFrame(title);
        int mainWidth = width;
        JScrollPane scroll = new JScrollPane(this);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        //JLayeredPane layers = new JLayeredPane();
        //layers.add(scroll, new Integer(1));
        //scroll.setBounds(0, 0, this.getPreferredSize().width, this.getPreferredSize().height);
        //glass.setBounds(0, 0, this.getPreferredSize().width, this.getPreferredSize().height);
        //layers.add(glass, new Integer(50));
        main.getContentPane().add(scroll, BorderLayout.CENTER);
        final CursorDisplayer glass = new CursorDisplayer();
        main.setGlassPane(glass);
        glass.setVisible(true);
        glass.addCursorSource(this);
        this.addCursorListener(glass);
        if (allowZoom) {
            JPanel zoomPanel = new JPanel();
            zoomPanel.setLayout(new BoxLayout(zoomPanel, BoxLayout.Y_AXIS));
            main.getContentPane().add(zoomPanel, BorderLayout.WEST);
            zoomPanel.add(Box.createVerticalGlue());
            JButton zoomIn = new JButton("Zoom In");
            zoomIn.setAlignmentX(CENTER_ALIGNMENT);
            zoomIn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    setZoomX(getZoomX()*2);
                    FunctionGraph.this.requestFocus();
                }
            });
            zoomPanel.add(zoomIn);
            JButton zoomOut = new JButton("Zoom Out");
            zoomOut.setAlignmentX(CENTER_ALIGNMENT);
            zoomOut.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    setZoomX(getZoomX()*0.5);
                    FunctionGraph.this.requestFocus();
                }
            });
            zoomPanel.add(zoomOut);
            if (showControls) {
                JPanel controls = getControls();
                if (controls != null) {
                    zoomPanel.add(Box.createVerticalGlue());
                    controls.setAlignmentX(CENTER_ALIGNMENT);
                    zoomPanel.add(controls);
                }
            }
            mainWidth += zoomPanel.getPreferredSize().width + 30;
            zoomPanel.add(Box.createVerticalGlue());
        }
        main.setSize(mainWidth, height);
        if (exitOnClose) {
            main.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent evt) {
                    System.exit(0);
                }
            });
        }
        main.setVisible(true);
        this.requestFocus();
        return main;
    }
    
    /**
     * Subclasses may provide specific controls here.
     * @return a JPanel filled with the controls, or null if none are to be provided.
     */
    protected JPanel getControls()
    {
        return null;
    }
    
    protected String getLabel(double x, double y)
    {
        // be about one order of magnitude less precise than there are pixels
        int pixelPrecisionX = 2;
        if (graphImage != null) {
            pixelPrecisionX = (int) (Math.log(graphImage.getWidth()/getXRange())/Math.log(10));
        }
        int precisionX = -(int)(Math.log(getXRange())/Math.log(10)) + pixelPrecisionX;
        if (precisionX < 0) precisionX = 0;
        // ignore imageY
        int precisionY = -(int)(Math.log(getYRange())/Math.log(10)) + 2;
        if (precisionY < 0) precisionY = 0;
        int indexX = X2indexX(x);
        return "f(" + new PrintfFormat("%."+precisionX+"f").sprintf(x)
            + ")=" + new PrintfFormat("%."+precisionY+"f").sprintf(this.y[indexX]);

    }
    
    public class DoublePoint
    {
        public DoublePoint()
        {
            this(Double.NaN, Double.NaN);
        }
        public DoublePoint(double x, double y)
        {
            this.x = x;
            this.y = y;
        }
        double x;
        double y;
    }
}

