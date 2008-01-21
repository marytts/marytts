package de.dfki.lt.signalproc.util;

import java.io.*;

/**
 * LERandomAccessFile.java copyright (c) 1998-2008 Roedy Green, Canadian Mind Products
 *
 * @noinspection WeakerAccess
 */
public final class LERandomAccessFile implements DataInput, DataOutput
{
    // ------------------------------ FIELDS ------------------------------

    /**
     * undisplayed copyright notice.
     *
     * @noinspection UnusedDeclaration
     */
    private static final String EMBEDDEDCOPYRIGHT =
        "copyright (c) 1999-2008 Roedy Green, Canadian Mind Products, http://mindprod.com";

    /**
     * to get at the big-endian methods of RandomAccessFile .
     *
     * @noinspection WeakerAccess
     */
    protected RandomAccessFile raf;

    /**
     * work array for buffering input/output.
     *
     * @noinspection WeakerAccess
     */
    protected byte work[];

    // -------------------------- PUBLIC INSTANCE  METHODS --------------------------
    /**
     * constructor.
     *
     * @param file file to read/write.
     * @param rw   like {@link java.io.RandomAccessFile} where "r" for read "rw" for read and write, "rws" for
     *             read-write sync, and "rwd" for read-write dsync. Sync ensures the physical i/o has completed befor
     *             the method returns.
     *
     * @throws java.io.FileNotFoundException if open fails.
     */
    public LERandomAccessFile( File file, String rw ) throws FileNotFoundException
    {
        raf = new RandomAccessFile(file, rw);
        work = new byte[8];
    }

    /**
     * constructors.
     *
     * @param file name of file.
     * @param rw   string "r" or "rw" depending on read or read/write.
     *
     * @throws java.io.FileNotFoundException if open fails.
     * @noinspection SameParameterValue
     */
    public LERandomAccessFile( String file,
            String rw ) throws FileNotFoundException
            {
        raf = new RandomAccessFile( file, rw );
        work = new byte[8];
            }

    /**
     * close the file.
     *
     * @throws IOException if close fails.
     */
    public final void close() throws IOException
    {
        raf.close();
    }

    /**
     * Get file descriptor.
     *
     * @return file descriptor (handle to open file)
     *
     * @throws IOException if get fails.
     */
    public final FileDescriptor getFD() throws IOException
    {
        return raf.getFD();
    }

    /**
     * get position of marker in the file.
     *
     * @return offset where we are in the file.
     *
     * @throws IOException if get fails.
     */
    public final long getFilePointer() throws IOException
    {
        return raf.getFilePointer();
    }

    /**
     * get length of the file.
     *
     * @return length in bytes, note value is a long.
     *
     * @throws IOException if get fails.
     */
    public final long length() throws IOException
    {
        return raf.length();
    }

    /**
     * ready one unsigned byte.
     *
     * @return unsigned byte read.
     *
     * @throws IOException if read fails.
     */
    public final int read() throws IOException
    {
        return raf.read();
    }

    /**
     * read an array of bytes.
     *
     * @param ba byte array to accept the bytes.
     *
     * @return how many bytes actually read.
     *
     * @throws IOException if read fails.
     */
    public final int read( byte ba[] ) throws IOException
    {
        return raf.read( ba );
    }

    /**
     * Read a byte array.
     *
     * @param ba  byte array to accept teh bytes.
     * @param off offset into the array to place the bytes, <b>not</b> offset in file.
     * @param len how many bytes to read.
     *
     * @return how many bytes actually read.
     *
     * @throws IOException if read fails.
     */
    public final int read( byte ba[], int off, int len ) throws IOException
    {
        return raf.read( ba, off, len );
    }

    /**
     * OK, reads only only 1 byte boolean.
     *
     * @return true or false.
     *
     * @throws IOException if read fails.
     */
    public final boolean readBoolean() throws IOException
    {
        return raf.readBoolean();
    }

    public final boolean [] readBoolean(int len) throws IOException
    {
        boolean [] ret = new boolean[len];

        for (int i=0; i<len; i++)
            ret[i] = readBoolean();

        return ret;
    }

    /**
     * read byte.
     *
     * @return byte read.
     *
     * @throws IOException if read fails.
     */
    public final byte readByte() throws IOException
    {
        return raf.readByte();
    }

    public final byte [] readByte(int len) throws IOException
    {
        byte [] ret = new byte[len];

        for (int i=0; i<len; i++)
            ret[i] = readByte();

        return ret;
    }

    /**
     * Read a char. like RandomAcessFile.readChar except little endian.
     *
     * @return char read.
     *
     * @throws IOException if read fails.
     */
    public final char readChar() throws IOException
    {
        raf.readFully( work, 0, 2 );
        return (char) ( ( work[ 1 ] & 0xff ) << 8 | ( work[ 0 ] & 0xff ) );
    }

    public final char [] readChar(int len) throws IOException
    {
        char [] ret = new char[len];

        for (int i=0; i<len; i++)
            ret[i] = readChar();

        return ret;
    }

    /**
     * read a double. like RandomAcessFile.readDouble except little endian.
     *
     * @return the double read.
     *
     * @throws IOException if read fails.
     */
    public final double readDouble() throws IOException
    {
        return Double.longBitsToDouble( readLong() );
    }

    public final double [] readDouble(int len) throws IOException
    {
        double [] ret = new double[len];

        for (int i=0; i<len; i++)
            ret[i] = raf.readDouble();

        return ret;
    } 

    public final int [] readDoubleToInt(int len) throws IOException
    {
        int [] ret = new int[len];

        for (int i=0; i<len; i++)
            ret[i] = (int)readDouble();

        return ret;
    } 

    /**
     * read a float. like RandomAcessFile.readFloat except little endian.
     *
     * @return float read.
     *
     * @throws IOException if read fails.
     */
    public final float readFloat() throws IOException
    {
        return Float.intBitsToFloat( readInt() );
    }

    public final float [] readFloat(int len) throws IOException
    {
        float [] ret = new float[len];

        for (int i=0; i<len; i++)
            ret[i] = readFloat();

        return ret;
    }

    /**
     * Read a full array.
     *
     * @param ba the array to hold the results.
     *
     * @throws IOException if read fails.
     */
    public final void readFully( byte ba[] ) throws IOException
    {
        raf.readFully( ba, 0, ba.length );
    }

    /**
     * read an array of bytes until the count is satisfied.
     *
     * @param ba  the array to hold the results.
     * @param off offset.
     * @param len count of bytes to read.
     *
     * @throws IOException if read fails.
     */
    public final void readFully( byte ba[],
            int off,
            int len ) throws IOException
            {
        raf.readFully( ba, off, len );
            }

    /**
     * read signed little endian 32-bit int.
     *
     * @return signed int
     *
     * @throws IOException if read fails.
     * @see java.io.RandomAccessFile#readInt except little endian.
     */
    public final int readInt() throws IOException
    {
        raf.readFully( work, 0, 4 );
        return ( work[ 3 ] ) << 24
        | ( work[ 2 ] & 0xff ) << 16
        | ( work[ 1 ] & 0xff ) << 8
        | ( work[ 0 ] & 0xff );
    }

    public final int [] readInt(int len) throws IOException
    {
        int [] ret = new int[len];

        for (int i=0; i<len; i++)
            ret[i] = readInt();

        return ret;
    }

    /**
     * Read a line.
     *
     * @return line read.
     *
     * @throws IOException if read fails.
     */
    public final String readLine() throws IOException
    {
        return raf.readLine();
    }

    /**
     * Read a long, 64 bits.
     *
     * @return long read. like RandomAcessFile.readLong except little endian.
     *
     * @throws IOException if read fails.
     */
    public final long readLong() throws IOException
    {
        raf.readFully( work, 0, 8 );
        return (long) ( work[ 7 ] ) << 56
        |
        /* long cast necessary or shift done modulo 32 */
        (long) ( work[ 6 ] & 0xff ) << 48
        | (long) ( work[ 5 ] & 0xff ) << 40
        | (long) ( work[ 4 ] & 0xff ) << 32
        | (long) ( work[ 3 ] & 0xff ) << 24
        | (long) ( work[ 2 ] & 0xff ) << 16
        | (long) ( work[ 1 ] & 0xff ) << 8
        | (long) ( work[ 0 ] & 0xff );
    }

    public final long [] readLong(int len) throws IOException
    {
        long [] ret = new long[len];

        for (int i=0; i<len; i++)
            ret[i] = readLong();

        return ret;
    }

    /**
     * Read a short, 16 bits.
     *
     * @return short read. like RandomAcessFile.readShort except little endian.
     *
     * @throws IOException if read fails.
     */
    public final short readShort() throws IOException
    {
        raf.readFully( work, 0, 2 );
        return (short) ( ( work[ 1 ] & 0xff ) << 8 | ( work[ 0 ] & 0xff ) );
    }

    public final short [] readShort(int len) throws IOException
    {
        short [] ret = new short[len];

        for (int i=0; i<len; i++)
            ret[i] = readShort();

        return ret;
    }

    /**
     * Read a counted UTF-8 string.
     *
     * @return string read.
     *
     * @throws IOException if read fails.
     */
    public final String readUTF() throws IOException
    {
        return raf.readUTF();
    }

    /**
     * return an unsigned byte. Noote: returns an int, even though says Byte.
     *
     * @return the byte read.
     *
     * @throws IOException if read fails.
     */
    public final int readUnsignedByte() throws IOException
    {
        return raf.readUnsignedByte();
    }

    public final int [] readUnsignedByte(int len) throws IOException
    {
        int [] ret = new int[len];

        for (int i=0; i<len; i++)
            ret[i] = readUnsignedByte();

        return ret;
    }

    /**
     * Read an unsigned short, 16 bits. Like RandomAcessFile.readUnsignedShort except little endian. Note, returns int
     * even though it reads a short.
     *
     * @return little-endian unsigned short, as an int.
     *
     * @throws IOException if read fails.
     */
    public final int readUnsignedShort() throws IOException
    {
        raf.readFully( work, 0, 2 );
        return ( ( work[ 1 ] & 0xff ) << 8 | ( work[ 0 ] & 0xff ) );
    }

    public final int [] readUnsignedShort(int len) throws IOException
    {
        int [] ret = new int[len];

        for (int i=0; i<len; i++)
            ret[i] = readUnsignedShort();

        return ret;
    }

    /**
     * seek to a place in the file
     *
     * @param pos 0-based offset to seek to.
     *
     * @throws IOException if read fails.
     * @noinspection SameParameterValue
     */
    public final void seek( long pos ) throws IOException
    {
        raf.seek( pos );
    }

    /**
     * Skip over bytes.
     *
     * @param n number of bytes to skip over.
     *
     * @return the actual number of bytes skipped.
     *
     * @throws IOException if read fails.
     */
    public final int skipBytes( int n ) throws IOException
    {
        return raf.skipBytes( n );
    }

    /**
     * Write a byte. Only writes one byte even though says int.
     *
     * @param ib byte to write.
     *
     * @throws IOException if read fails.
     */
    public final synchronized void write( int ib ) throws IOException
    {
        raf.write( ib );
    }

    /**
     * Write an array of bytes.
     *
     * @param ba array to write.
     *
     * @throws IOException if read fails.
     * @see java.io.DataOutput#write(byte[])
     */
    public final void write( byte ba[] ) throws IOException
    {
        raf.write( ba, 0, ba.length );
    }

    /**
     * Write part of an array of bytes.
     *
     * @param ba  array to write.
     * @param off offset
     * @param len count of bytes to write.
     *
     * @throws IOException if read fails.
     * @see java.io.DataOutput#write(byte[],int,int)
     */
    public final synchronized void write( byte ba[],
            int off,
            int len ) throws IOException
            {
        raf.write( ba, off, len );
            }

    /**
     * write a boolean as one byte.
     *
     * @param v boolean to write.
     *
     * @throws IOException if read fails.
     * @see java.io.DataOutput#writeBoolean(boolean)
     */
    public final void writeBoolean( boolean v ) throws IOException
    {
        raf.writeBoolean( v );
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

    /**
     * Write a byte. Note param is an int though only a byte is written.
     *
     * @param v byte to write.
     *
     * @throws IOException if read fails.
     * @see java.io.DataOutput#writeByte(int)
     */
    public final void writeByte( int v ) throws IOException
    {
        raf.writeByte( v );
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

    /**
     * Write bytes from a String.
     *
     * @param s string source of the bytes.
     *
     * @throws IOException if read fails.
     * @see java.io.DataOutput#writeBytes(java.lang.String)
     */
    public final void writeBytes( String s ) throws IOException
    {
        raf.writeBytes( s );
    }

    /**
     * Write a char.  note param is an int though writes a char.
     *
     * @param v char to write. like RandomAcessFile.writeChar. Note the parm is an int even though this as a writeChar
     *
     * @throws IOException if read fails.
     */
    public final void writeChar( int v ) throws IOException
    {
        // same code as writeShort
        work[ 0 ] = (byte) v;
        work[ 1 ] = (byte) ( v >> 8 );
        raf.write( work, 0, 2 );
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


    /**
     * Write a string, even though method called writeChars. like RandomAcessFile.writeChars, has to flip each char.
     *
     * @param s Strinhg to write.
     *
     * @throws IOException if read fails.
     */
    public final void writeChars( String s ) throws IOException
    {
        int len = s.length();
        for ( int i = 0; i < len; i++ )
        {
            writeChar( s.charAt( i ) );
        }
    }// end writeChars

    /**
     * Write a double. Like RandomAcessFile.writeDouble.
     *
     * @param v double to write.
     *
     * @throws IOException if read fails.
     */
    public final void writeDouble( double v ) throws IOException
    {
        writeLong( Double.doubleToLongBits( v ) );
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

    /**
     * Write a float. Like RandomAcessFile.writeFloat.
     *
     * @param v float to write.
     *
     * @throws IOException if read fails.
     */
    public final void writeFloat( float v ) throws IOException
    {
        writeInt( Float.floatToIntBits( v ) );
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

    /**
     * write an int, 32-bits. Like RandomAcessFile.writeInt.
     *
     * @param v int to write.
     *
     * @throws IOException if read fails.
     */
    public final void writeInt( int v ) throws IOException
    {
        work[ 0 ] = (byte) v;
        work[ 1 ] = (byte) ( v >> 8 );
        work[ 2 ] = (byte) ( v >> 16 );
        work[ 3 ] = (byte) ( v >> 24 );
        raf.write( work, 0, 4 );
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

    /**
     * Write i long, 64 bits. Like java.io.RandomAccessFile.writeLong.
     *
     * @param v long write.
     *
     * @throws IOException if read fails.
     * @see java.io.RandomAccessFile#writeLong
     */
    public final void writeLong( long v ) throws IOException
    {
        work[ 0 ] = (byte) v;
        work[ 1 ] = (byte) ( v >> 8 );
        work[ 2 ] = (byte) ( v >> 16 );
        work[ 3 ] = (byte) ( v >> 24 );
        work[ 4 ] = (byte) ( v >> 32 );
        work[ 5 ] = (byte) ( v >> 40 );
        work[ 6 ] = (byte) ( v >> 48 );
        work[ 7 ] = (byte) ( v >> 56 );
        raf.write( work, 0, 8 );
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

    /**
     * Write an signed short even though parameter is an int. Like java.io.RandomAcessFile#writeShort. also acts as a
     * writeUnsignedShort.
     *
     * @param v signed number to write
     *
     * @throws IOException if read fails.
     */
    public final void writeShort( int v ) throws IOException
    {
        work[ 0 ] = (byte) v;
        work[ 1 ] = (byte) ( v >> 8 );
        raf.write( work, 0, 2 );
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

    /**
     * Write a counted UTF string.
     *
     * @param s String to write.
     *
     * @throws IOException if read fails.
     * @see java.io.DataOutput#writeUTF(java.lang.String)
     */
    public final void writeUTF( String s ) throws IOException
    {
        raf.writeUTF( s );
    }
}
