package de.dfki.lt.mary.util;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A helper class converting between a given set of integers and strings.
 * @author schroed
 *
 */
public class IntStringTranslator
{
    ArrayList<String> list;
    
    /**
     * Initialize empty int-string two-way translator.
     *
     */
    public IntStringTranslator()
    {
        list = new ArrayList<String>();
    }

    public IntStringTranslator(int initialRange)
    {
        list = new ArrayList<String>(initialRange);
    }
    
    /**
     * Initialize a int-string two-way translator,
     * setting int values according to the position of strings
     * in the array.
     * @param strings
     */
    public IntStringTranslator(String[] strings)
    {
        list = new ArrayList<String>(Arrays.asList(strings));
    }
    
    public void set(int i, String s)
    {
        list.add(i, s);
    }
    
    public boolean contains(String s)
    {
        int index = list.indexOf(s);
        if (index == -1) return false;
        return true;
    }
    
    public boolean contains(int b)
    {
        int index = b;
        if (index < 0 || index >= list.size()) return false;
        return true;
    }

    public int get(String s)
    {
        int index = list.indexOf(s);
        if (index == -1) throw new IllegalArgumentException("No byte value known for string ["+s+"]");
        return index;
    }
    
    public String get(int i)
    {
        if (i < 0 || i >= list.size())
            throw new IndexOutOfBoundsException("Int value out of range: "+i);
        return list.get(i);
    }
    
    public String[] getStringValues()
    {
        return list.toArray(new String[0]);
    }
    
    public int getHighestValue()
    {
        return list.size();
    }
}
