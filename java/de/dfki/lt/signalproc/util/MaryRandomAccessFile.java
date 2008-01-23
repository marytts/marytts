package de.dfki.lt.signalproc.util;

import java.io.*;

/**
 * @author oytun.turk
 *
 * A class that extends RandomAccessFile to read/write arrays of different types while allowing random
 * access to a binary file (i.e. the file can be opened in both read/write mode and there is support for
 * moving the file pointer to any location as required
 * 
 */

public final class MaryRandomAccessFile extends RandomAccessFile
{
    public MaryRandomAccessFile(File arg0, String arg1) throws FileNotFoundException {
        super(arg0, arg1);
    }
    
    public MaryRandomAccessFile(String arg0, String arg1) throws FileNotFoundException {
        super(arg0, arg1);
    }

    public final boolean [] readBoolean(int len) throws IOException
    {
        boolean [] ret = new boolean[len];

        for (int i=0; i<len; i++)
            ret[i] = readBoolean();

        return ret;
    }

    public final byte [] readByte(int len) throws IOException
    {
        byte [] ret = new byte[len];

        for (int i=0; i<len; i++)
            ret[i] = readByte();

        return ret;
    }

    public final char [] readChar(int len) throws IOException
    {
        char [] ret = new char[len];

        for (int i=0; i<len; i++)
            ret[i] = readChar();

        return ret;
    }

    public final double [] readDouble(int len) throws IOException
    {
        double [] ret = new double[len];

        for (int i=0; i<len; i++)
            ret[i] = readDouble();

        return ret;
    } 

    public final int [] readDoubleToInt(int len) throws IOException
    {
        int [] ret = new int[len];

        for (int i=0; i<len; i++)
            ret[i] = (int)readDouble();

        return ret;
    } 

    public final float [] readFloat(int len) throws IOException
    {
        float [] ret = new float[len];

        for (int i=0; i<len; i++)
            ret[i] = readFloat();

        return ret;
    }
    
    public final int [] readInt(int len) throws IOException
    {
        int [] ret = new int[len];

        for (int i=0; i<len; i++)
            ret[i] = readInt();

        return ret;
    }

    public final long [] readLong(int len) throws IOException
    {
        long [] ret = new long[len];

        for (int i=0; i<len; i++)
            ret[i] = readLong();

        return ret;
    }

    public final short [] readShort(int len) throws IOException
    {
        short [] ret = new short[len];

        for (int i=0; i<len; i++)
            ret[i] = readShort();

        return ret;
    }

    public final int [] readUnsignedByte(int len) throws IOException
    {
        int [] ret = new int[len];

        for (int i=0; i<len; i++)
            ret[i] = readUnsignedByte();

        return ret;
    }

    public final int [] readUnsignedShort(int len) throws IOException
    {
        int [] ret = new int[len];

        for (int i=0; i<len; i++)
            ret[i] = readUnsignedShort();

        return ret;
    }

    public final void writeBoolean( boolean [] v, int startPos, int len) throws IOException
    {
        assert v.length<startPos+len;

        for (int i=startPos; i<startPos+len; i++)
            writeBoolean(v[i]);
    }

    public final void writeBoolean( boolean [] v) throws IOException
    {
        writeBoolean(v, 0, v.length);
    }

    public final void writeByte( byte [] v, int startPos, int len) throws IOException
    {
        assert v.length<startPos+len;

        for (int i=startPos; i<startPos+len; i++)
            writeByte(v[i]);
    }

    public final void writeByte( byte [] v) throws IOException
    {
        writeByte(v, 0, v.length);
    }

    public final void writeChar( char [] v, int startPos, int len) throws IOException
    {
        assert v.length<startPos+len;

        for (int i=startPos; i<startPos+len; i++)
            writeChar(v[i]);
    }

    public final void writeChar( char [] v) throws IOException
    {
        writeChar(v, 0, v.length);
    }

    public final void writeDouble( double [] v, int startPos, int len) throws IOException
    {        
        for (int i=startPos; i<startPos+len; i++)
            writeDouble(v[i]);
    }

    public final void writeDouble( double [] v) throws IOException
    {
        writeDouble(v, 0, v.length);
    }

    public final void writeFloat( float [] v, int startPos, int len) throws IOException
    {
        assert v.length<startPos+len;

        for (int i=startPos; i<startPos+len; i++)
            writeFloat(v[i]);
    }

    public final void writeFloat( float [] v) throws IOException
    {
        writeFloat(v, 0, v.length);
    }

    public final void writeInt( int [] v, int startPos, int len) throws IOException
    {
        assert v.length<startPos+len;

        for (int i=startPos; i<startPos+len; i++)
            writeInt(v[i]);
    }

    public final void writeInt( int [] v) throws IOException
    {
        writeInt(v, 0, v.length);
    }

    public final void writeLong (long [] v, int startPos, int len) throws IOException
    {
        assert v.length<startPos+len;

        for (int i=startPos; i<startPos+len; i++)
            writeLong(v[i]);
    }

    public final void writeLong( long [] v) throws IOException
    {
        writeLong(v, 0, v.length);
    }

    public final void writeShort( short [] v, int startPos, int len) throws IOException
    {
        assert v.length<startPos+len;

        for (int i=startPos; i<startPos+len; i++)
            writeShort(v[i]);
    }

    public final void writeShort( short [] v) throws IOException
    {
        writeShort(v, 0, v.length);
    }
}
