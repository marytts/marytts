package marytts.util.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class converting between a given set of bytes and strings.
 * @author schroed
 *
 */
public class ByteStringTranslator
{
    ArrayList<String> list;
    Map<String,Byte> map;

    
    /**
     * Initialize empty byte-string two-way translator.
     *
     */
    public ByteStringTranslator()
    {
        list = new ArrayList<String>();
        map = new HashMap<String, Byte>();
    }

    public ByteStringTranslator(byte initialRange)
    {
        list = new ArrayList<String>(initialRange);
        map = new HashMap<String, Byte>();
    }

    /**
     * Initialize a byte-string two-way translator,
     * setting byte values according to the position of strings
     * in the array.
     * @param strings
     */
    public ByteStringTranslator(String[] strings)
    {
        if (strings.length > Byte.MAX_VALUE) {
            StringBuffer buf = new StringBuffer();
            for (int i=0; i<strings.length; i++) {
                buf.append("\""+strings[i]+"\" ");
            }
            throw new IllegalArgumentException("Too many strings for a byte-string translator: \n"+buf.toString()+ "("+strings.length+" strings)");
        }
        list = new ArrayList<String>(Arrays.asList(strings));
        map = new HashMap<String, Byte>();
        for (int i=0; i<strings.length; i++) {
            map.put(strings[i], (byte)i);
        }

    }
    
    public void set(byte b, String s)
    {
        list.add(b, s);
        map.put(s, b);
    }
    
    public boolean contains(String s)
    {
        return map.containsKey(s);
    }
    
    public boolean contains(byte b)
    {
        int index = (int) b;
        if (index < 0 || index >= list.size()) return false;
        return true;
    }
    
    
    public byte get(String s)
    {
        Byte b = map.get(s);
        if (b == null)
            throw new IllegalArgumentException("No byte value known for string ["+s+"]");
        return b.byteValue();
    }
    
    public String get(byte b)
    {
        int index = (int) b;
        if (index < 0 || index >= list.size())
            throw new IndexOutOfBoundsException("Byte value out of range: "+index);
        return list.get(index);
    }
    
    public String[] getStringValues()
    {
        return list.toArray(new String[0]);
    }
    
    public byte getNumberOfValues()
    {
        return (byte) list.size();
    }

}
