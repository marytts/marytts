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
package de.dfki.lt.mary.modules.en.prosody;

/**
 * Information on how to recognise and how to realise different sentence types.
 */
public class SentenceType
{
    public static final SentenceType declarative =
        new SentenceType( "declarative", "L-L%", "H-L%", "H-", "!H*", "L+H*");
    public static final SentenceType interrogative =
        new SentenceType("interrogative", "H-H%", "H-L%", "H-", "L*", "H*");
    public static final SentenceType exclamation =
        new SentenceType("exclamation", "L-L%", "H-L%", "H-", "H*", "H*");
    public static final SentenceType interrogYN =
        new SentenceType("interrogYN", "H-H%", "H-L%", "H-", "L*", "H*");
    public static final SentenceType interrogWH =
        new SentenceType("interrogWH", "L-L%", "H-L%", "H-", "H*", "H*");

    public static SentenceType punctuationType(String punct)
    {
        if (punct.equals(".")) return declarative;
        else if (punct.equals("?")) return interrogative;
        else if (punct.equals("!")) return exclamation;
        else return null;
    }

    private String name;
    private String sentenceFinalBoundary;
    private String nonFinalMajorBoundary;
    private String minorBoundary;
    private String nuclearAccent;
    private String nonNuclearAccent;

    private SentenceType(String name,
                         String sentenceFinalBoundary,
                         String nonFinalMajorBoundary,
                         String minorBoundary,
                         String nuclearAccent,
                         String nonNuclearAccent)
    {
        this.name = name;
        this.sentenceFinalBoundary = sentenceFinalBoundary;
        this.nonFinalMajorBoundary = nonFinalMajorBoundary;
        this.minorBoundary = minorBoundary;
        this.nuclearAccent = nuclearAccent;
        this.nonNuclearAccent = nonNuclearAccent;
    }

    public String name() { return name; }
    public String toString() { return name(); }
    public String sentenceFinalBoundary() { return sentenceFinalBoundary; }
    public String nonFinalMajorBoundary() { return nonFinalMajorBoundary; }
    public String minorBoundary() { return minorBoundary; }
    public String nuclearAccent() { return nuclearAccent; }
    public String nonNuclearAccent() { return nonNuclearAccent; }

}
