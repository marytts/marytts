/**
 * Copyright 2000-2006 DFKI GmbH.
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
package de.dfki.lt.mary.modules.synthesis;

import java.util.Vector;

public class MBROLAPhoneme
{
    /** The phoneme symbol, e.g. "a:". */
    private String symbol;
    /** The duration in ms. */
    private int duration;
    /** The f0 targets. This is a vector of int[]. Each target int[] has two
     * entries, the first [0] is the percent duration, the second [1] the f0 in
     * Hz. */
    private Vector<int []> targets;
    /** The voice quality */
    private String vq;

    public MBROLAPhoneme(String symbol, int duration, Vector<int []> targets, String vq)
    {
        this.symbol = symbol;
        this.duration = duration;
        this.targets = targets;
        this.vq = vq;
    }

    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSymbol() { return symbol; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getDuration() { return duration; }
    public void setTargets(Vector targets) { this.targets = targets; }
    public Vector<int []> getTargets() { return targets; }
    public void setVoiceQuality(String vq) { this.vq = vq; }
    public String getVoiceQuality() { return vq; }

    public String toString()
    {
        StringBuffer buf = new StringBuffer(symbol);
        if (vq != null && !vq.equals("")) {
            buf.append("_");
            buf.append(vq);
        }
        buf.append(" ");
        buf.append(duration);
        if (targets != null) {
            for (int i=0; i<targets.size(); i++) {
                int[] target = (int[]) targets.get(i);
                buf.append(" (");
                buf.append(target[0]); // percent
                buf.append(",");
                buf.append(target[1]); // f0
                buf.append(")");
            }
        }
        return buf.toString();
    }
}
