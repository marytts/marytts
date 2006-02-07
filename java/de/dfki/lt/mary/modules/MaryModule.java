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
package de.dfki.lt.mary.modules;

// DOM classes
import de.dfki.lt.mary.MaryData;
import de.dfki.lt.mary.MaryDataType;

/**
 * A generic interface for Mary Modules.
 * This interface defines the communication of the Mary.java main program
 * with the individual modules.
 * <p>
 * The main program calls
 * <ul>
 * <li> <code>startup()</code> once after module instantiation, </li>
 * <li> <code>process()</code> many times, from different threads,
 *      possibly at the same time, during the lifetime of the server </li>
 * <li> <code>shutdown()</code> once, at the end of the program. </li>
 * </ul>
 * @author Marc Schr&ouml;der
 */
public interface MaryModule
{
    public final int MODULE_OFFLINE = 0;
    public final int MODULE_RUNNING = 1;
    
    /** This module's name, as free text, for example "Tokeniser" */
    public String name();

    /** The type of input data needed by this module. */
    public MaryDataType inputType();

    /** The type of output data produced by this module. */
    public MaryDataType outputType();

    /**
     * Allow the module to start up, performing whatever is necessary
     * to become operational. After successful completion, getState()
     * should return MODULE_RUNNING.
     */
    public void startup() throws Exception;

    /**
     * Inform about the state of this module. 
     * @return an int identifying the state of this module, either MODULE_OFFLINE or MODULE_RUNNING.
     */
    public int getState();
    

    /**
     * Perform a power-on self test by processing some example input data.
     * @throws Error if the module does not work properly.
     */
    public void powerOnSelfTest() throws Error;
 
    /**
     * Allow the module to shut down cleanly. After this has successfully completed,
     * getState() should return MODULE_OFFLINE.
     */
    public void shutdown();


    /**
     * Perform this module's processing on abstract "MaryData" input
     * <code>d</code>.
     * Classes implementing this interface need to make the
     * <code>process()</code>
     * method thread-safe, because in server-mode,
     * it will be called from different
     * threads at the same time.
     * <p>
     * The result is returned encapsulated in a MaryData object of type
     * <code>outputType()</code>.
     * <p>
     * This method should never return <code> null </code>; in case of a
     * failure, an exception should be thrown.
     */
    public MaryData process(MaryData d) throws Exception;
}
