/**
 * Portions Copyright 2006 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
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
package de.dfki.lt.mary.util;


/**
 * FloatList is used to maintain a circular buffer of float values. It is
 * essentially an index-free array of floats that can easily be iterated through
 * forwards or backwards. Keeping values in an index free list like this
 * eliminates index bounds checking which can save us some time.
 */
public class FloatList
{
    public float value;

    public FloatList next;

    public FloatList prev;

    /**
     * Creates a new node
     */
    FloatList()
    {
        value = 0.0F;
        next = null;
        prev = null;
    }

    /**
     * Creates a circular list of nodes of the given size
     * 
     * @param size
     *            the number of nodes in the list
     * 
     * @return an entry in the list.
     */
    public static FloatList createList(int size)
    {
        FloatList prev = null;
        FloatList first = null;

        for (int i = 0; i < size; i++) {
            FloatList cur = new FloatList();
            cur.prev = prev;
            if (prev == null) {
                first = cur;
            } else {
                prev.next = cur;
            }
            prev = cur;
        }
        first.prev = prev;
        prev.next = first;

        return first;
    }

    /**
     * prints out the contents of this list
     * 
     * @param title
     *            the title of the dump
     * @param list
     *            the list to dump
     */
    public static void dump(String title, FloatList list)
    {
        System.out.println(title);

        FloatList cur = list;
        do {
            System.out.println("Item: " + cur.value);
            cur = cur.next;
        } while (cur != list);
    }
}
