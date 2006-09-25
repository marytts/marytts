package de.dfki.lt.mary.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A helper class converting between a given set of bytes and strings.
 * @author schroed
 *
 */
public class ByteStringTranslator
{
    ArrayList list;
    
    /**
     * Initialize empty byte-string two-way translator.
     *
     */
    public ByteStringTranslator()
    {
        list = new ArrayList();
    }

    public ByteStringTranslator(byte initialRange)
    {
        list = new ArrayList(initialRange);
    }

    /**
     * Initialize a byte-string two-way translator,
     * setting byte values according to the position of strings
     * in the array.
     * @param strings
     */
    public ByteStringTranslator(String[] strings)
    {
        if (strings.length > Byte.MAX_VALUE) throw new IllegalArgumentException("Too many strings for a byte-string translator");
        list = new ArrayList(Arrays.asList(strings));
    }
    
    public void set(byte b, String s)
    {
        list.add(b, s);
    }
    
    public byte get(String s)
    {
        int index = list.indexOf(s);
        if (index == -1) throw new IllegalArgumentException("No byte value known for string "+s);
        return (byte)index;
    }
    
    public String get(byte b)
    {
        int index = (int) b;
        if (index < 0 || index > list.size())
            throw new IndexOutOfBoundsException("Byte value out of range: "+index);
        return (String) list.get(index);
    }
    
    public String[] getStringValues()
    {
        return (String[]) list.toArray(new String[0]);
    }
    
    public byte getHighestValue()
    {
        return (byte) list.size();
    }

}
