package com.twmacinta.io;

import java.io.*;

/**
 * Copyright (c) 2001, 2002 by Pensamos Digital, Inc., All Rights Reserved.<p>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * <p>
 * This OutputStream discards all data written to it.
 *
 * @author Tim Macinta (twm@alum.mit.edu)
 **/

public class NullOutputStream extends OutputStream {

  private boolean closed = false;

  public NullOutputStream() {
  }

  public void close() {
    this.closed = true;
  }

  public void flush() throws IOException {
    if (this.closed) _throwClosed();
  }

  private void _throwClosed() throws IOException {
    throw new IOException("This OutputStream has been closed");
  }

  public void write(byte[] b) throws IOException {
    if (this.closed) _throwClosed();
  }

  public void write(byte[] b, int offset, int len) throws IOException {
    if (this.closed) _throwClosed();
  }

  public void write(int b) throws IOException {
    if (this.closed) _throwClosed();
  }

}
