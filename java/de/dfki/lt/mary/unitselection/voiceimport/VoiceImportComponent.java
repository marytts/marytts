/**
 * Copyright 2006 DFKI GmbH.
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
package de.dfki.lt.mary.unitselection.voiceimport;

import java.util.*;

/**
 * A component in the process of importing a voice into MARY format.
 * @author Marc Schr&ouml;der, Anna Hunecke
 *
 */
public abstract class VoiceImportComponent
{
    protected SortedMap props = null;
    protected SortedMap props2Help = null;
    protected BasenameList bnl;
    
    protected abstract void setupHelp();
    /**
     * Initialise a voice import component:
     * update values of local properties;
     * setup help text for properties;
     * call to component specific intialisation
     * 
     * @param db the database layout
     * @param bnl the list of basenames
     * @param props the map from properties to values
     */
    public final void initialise(BasenameList bnl, SortedMap props){
        //setupHelp(); this is now done by DatabaseLayout
        this.props = props;
        this.bnl = bnl;
        initialiseComp();
    }
    
    /**
     * Initialise a voice import component:
     * component specific initialisation;
     * to be overwritten by subclasses
     * 
     * @param bnl the list of basenames
     */
    public void initialiseComp(){}
        
    
    /**
     * Get the map of properties2values
     * containing the default values
     * @return map of props2values
     */
    public abstract SortedMap getDefaultProps(DatabaseLayout db);
    
    /**
     * Get the value for a property
     * @param prop the property name
     * @return the value
     */
    public String getProp(String prop){
        return (String) props.get(prop);
    }
    
    /**
     * Set a property to a value
     * @param prop the property
     * @param value the value
     */
    public void setProp(String prop, String value){
        props.put(prop,value);
    }
    
    /**
     * Get the name of this component
     * @return the name
     */
    public abstract String getName();
    
    /**
     * Do the computations required by this component.
     * 
     * @return true on success, false on failure
     */
    public abstract boolean compute() throws Exception;
    
    /**
     * Provide the progress of computation, in percent, or -1 if
     * that feature is not implemented.
     * @return -1 if not implemented, or an integer between 0 and 100.
     */
    public abstract int getProgress();
    
    public String getHelpText(){
        StringBuffer helpText = new StringBuffer();
        helpText.append("<html>\n<head>\n<title>SETTINGS HELP</title>\n"
                +"</head>\n<body>\n"
                +"<h2>Settings help for component "+getName()+"</h2>\n<dl>\n");
        try{
        for (Iterator it=props2Help.keySet().iterator();it.hasNext();){
            String key = (String) it.next();
            String value = (String) props2Help.get(key);
            helpText.append("<dt><strong>"+key+"</strong></dt>\n"
                    +"<dd>"+value+"</dd>\n");
        }
        
        helpText.append("</dl>\n</body>\n</html>");
        return helpText.toString();
        } catch (NullPointerException npe){
            npe.printStackTrace();
            throw new Error("No help text for component "+getName());
        }
    }
    
    public String getHelpTextForProp(String propname){
        return (String)props2Help.get(propname);
    }
    
}
