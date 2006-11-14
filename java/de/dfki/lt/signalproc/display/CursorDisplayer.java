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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;


/**
 * Represent the glass layer in front of the function graph.
 * @author Marc Schr&ouml;der
 *
 */
public class CursorDisplayer extends JPanel implements CursorListener
{
    protected List cursorSources;
    public CursorDisplayer()
    {
        super();
        setOpaque(false);
        setVisible(true);
        cursorSources = new ArrayList();
    }
    
    public void addCursorSource(CursorSource s)
    {
        cursorSources.add(s);
    }
    
    public CursorSource[] getCursorSources()
    {
        return (CursorSource[]) cursorSources.toArray(new CursorSource[0]);
    }
    
    public boolean removeCursorSource(CursorSource s)
    {
        return cursorSources.remove(s);
    }
    
    public void paintComponent(Graphics gr)
    {
        Graphics2D g = (Graphics2D) gr;
        for (Iterator it = cursorSources.iterator(); it.hasNext(); ) {
            CursorSource source = (CursorSource) it.next();
            CursorLine positionCursor = source.getPositionCursor();
            if (positionCursor != null) {
                int x = positionCursor.getX(this);
                g.setColor(positionCursor.getColor());
                g.drawLine(x, positionCursor.getYMin(this), x, positionCursor.getYMax(this));
            }
            CursorLine rangeCursor = source.getRangeCursor();
            if (rangeCursor != null) {
                int x = rangeCursor.getX(this);
                g.setColor(rangeCursor.getColor());
                g.drawLine(x, rangeCursor.getYMin(this), x, rangeCursor.getYMax(this));
            }
            if (positionCursor != null && rangeCursor != null) {
                Composite origC = g.getComposite();
                AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
                g.setComposite(ac);
                g.fillRect(positionCursor.getX(this), positionCursor.getYMin(this),
                        rangeCursor.getX(this)-positionCursor.getX(this), rangeCursor.getYMax(this)-positionCursor.getYMin(this));
                g.setComposite(origC);
            }
            
            Label valueLabel = source.getValueLabel();
            if (valueLabel != null) {
                g.setColor(valueLabel.getColor());
                g.drawString(valueLabel.getText(), valueLabel.getX(this), valueLabel.getY(this));
            }
        }
    }
    
    public void updateCursorPosition(CursorEvent e)
    {
        repaint();
    }
    
    public static class CursorLine
    {
        protected Component source;
        protected int sourceX;
        protected int sourceYMin;
        protected int sourceYMax;
        protected Color color;

        public CursorLine(Component source)
        {
            this(source, -1, -1, -1);
        }
        
        public CursorLine(Component source, int sourceX, int sourceYMin, int sourceYMax)
        {
            this(source, sourceX, sourceYMin, sourceYMax, Color.RED);
        }
        
        public CursorLine(Component source, int sourceX, int sourceYMin, int sourceYMax, Color color)
        {
            this.source = source;
            this.sourceX = sourceX;
            this.sourceYMin = sourceYMin;
            this.sourceYMax = sourceYMax;
            this.color = color;
        }
        
        public void setSourceX(int sourceX) { this.sourceX = sourceX; }
        public void setSourceYMin(int sourceYMin) { this.sourceYMin = sourceYMin; }
        public void setSourceYMax(int sourceYMax) { this.sourceYMax = sourceYMax; }
        public void setColor(Color color) { this.color = color; }

        public int getSourceX() { return sourceX; }
        public int getSourceYMin() { return sourceYMin; }
        public int getSourceYMax() { return sourceYMax; }
        public Color getColor() { return color; }
        
        public int getX(Component target)
        {
            if (sourceX < 0) return -1;
            return SwingUtilities.convertPoint(source, sourceX, 0, target).x;
        }
        
        public int getYMin(Component target)
        {
            if (sourceYMin < 0) return -1;
            return SwingUtilities.convertPoint(source, 0, sourceYMin, target).y;            
        }

        public int getYMax(Component target)
        {
            if (sourceYMax < 0) return -1; 
            return SwingUtilities.convertPoint(source, 0, sourceYMax, target).y;
        }

    }

    public static class Label
    {
        protected Component source;
        protected String text;
        protected int sourceX;
        protected int sourceY;
        protected Color color;
        
        public Label(Component source)
        {
            this(source, null, -1, -1);
        }
        
        public Label(Component source, String text, int sourceX, int sourceY)
        {
            this(source, text, sourceX, sourceY, Color.RED);
        }
        
        public Label(Component source, String text, int sourceX, int sourceY, Color color)
        {
            this.source = source;
            this.text = text;
            this.sourceX = sourceX;
            this.sourceY = sourceY;
            this.color = color;
        }
        
        public void setText(String text) { this.text = text; }
        public void setSourceX(int sourceX) { this.sourceX = sourceX; }
        public void setSourceY(int sourceY) { this.sourceY = sourceY; }

        public int getSourceX() { return sourceX; }
        public int getSourceY() { return sourceY; }
        public String getText() { return text; }
        public Color getColor() { return color; }

        public int getX(Component target)
        {
            if (sourceX < 0) return -1;
            return SwingUtilities.convertPoint(source, sourceX, 0, target).x;
        }
        
        public int getY(Component target)
        {
            if (sourceY < 0) return -1;
            return SwingUtilities.convertPoint(source, 0, sourceY, target).y;            
        }
        

    }
}

