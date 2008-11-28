/**
 * Copyright 2007 DFKI GmbH.
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

package marytts.client.http;

/**
 * 
 * A class to keep host and port information in a structure
 * 
 * @author Oytun T&uumlrk
 */
public class Address {
    private String host;
    private int port;
    private String fullAddress; // --> host:port
    private String httpAddress; // --> http:\\host:port
    
    public Address()
    {
        this("", "");
    }
    
    public Address(String hostIn, int portIn)
    {
        this(hostIn, String.valueOf(portIn));
    }
    
    public Address(String hostIn, String portIn)
    {
        init(hostIn, portIn);
    }
    
    public Address(String fullAddress)
    {
        String tmpAddress = fullAddress.trim();
        int index = tmpAddress.lastIndexOf(':');

        String hostIn = "";
        String portIn = "";
        if (index>0)
        {
            hostIn = tmpAddress.substring(0, index);
            
            if (index+1<tmpAddress.length())
                portIn = tmpAddress.substring(index+1);
        }
        else
            hostIn = tmpAddress;
        
        init(hostIn, portIn);
    }
    
    public void init(String hostIn, String portIn)
    {
        this.host = hostIn;
        
        if (portIn!="")
        {
            this.port = Integer.valueOf(portIn);
            this.fullAddress = this.host + ":" + portIn;  
        }
        else //No port address specified, set fullAdrress equal to host address
        {
            this.port = Integer.MIN_VALUE;
            this.fullAddress = this.host;
        }
        
        if (this.fullAddress!=null && this.fullAddress.length()>0)
            this.httpAddress = "http://" + this.fullAddress;
        else
            this.httpAddress = null;
    }
    
    public String getHost() { return host; }

    public int getPort() { return port; }
    
    public String getFullAddress() { return fullAddress; }

    public String getHttpAddress() { return httpAddress; }
}
