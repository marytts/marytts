package de.dfki.lt.mary.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A helper class converting between a given set of shorts and strings.
 * @author schroed
 *
 */
public class ShortStringTranslator
{
    ArrayList<String> list;
    
    /**
     * Initialize empty short-string two-way translator.
     *
     */
    public ShortStringTranslator()
    {
        list = new ArrayList<String>();
    }

    public ShortStringTranslator(short initialRange)
    {
        list = new ArrayList<String>(initialRange);
    }

    /**
     * Initialize a short-string two-way translator,
     * setting short values according to the position of strings
     * in the array.
     * @param strings
     */
    public ShortStringTranslator(String[] strings)
    {
        if (strings.length > Short.MAX_VALUE) throw new IllegalArgumentException("Too many strings for a short-string translator");
        list = new ArrayList<String>(Arrays.asList(strings));
    }
    
    public void set(short b, String s)
    {
        list.add(b, s);
    }
    
    public boolean contains(String s)
    {
        int index = list.indexOf(s);
        if (index == -1) return false;
        return true;
    }
    
    public boolean contains(short b)
    {
        int index = (int) b;
        if (index < 0 || index >= list.size()) return false;
        return true;
    }

    public short get(String s)
    {
        int index = list.indexOf(s);
        if (index == -1) throw new IllegalArgumentException("No short value known for string ["+s+"]");
        return (short)index;
    }
    
    public String get(short b)
    {
        int index = (int) b;
        if (index < 0 || index >= list.size())
            throw new IndexOutOfBoundsException("Short value out of range: "+index);
        return list.get(index);
    }
    
    public String[] getStringValues()
    {
        return list.toArray(new String[0]);
    }
    
    public short getNumberOfValues()
    {
        return (short) list.size();
    }

}
