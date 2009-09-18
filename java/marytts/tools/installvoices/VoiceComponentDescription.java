/**
 * Copyright 2009 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package marytts.tools.installvoices;

import java.net.MalformedURLException;

import org.w3c.dom.Element;

/**
 * @author marc
 *
 */
public class VoiceComponentDescription extends ComponentDescription
{
    private String gender;
    private String dependsLanguage;
    private String dependsVersion;

    /**
     * @param xmlDescription
     * @throws NullPointerException
     * @throws MalformedURLException
     */
    public VoiceComponentDescription(Element xmlDescription)
    throws NullPointerException, MalformedURLException
    {
        super(xmlDescription);
        this.gender = xmlDescription.getAttribute("gender");
        Element dependsElement = (Element) xmlDescription.getElementsByTagName("depends").item(0);
        this.dependsLanguage = dependsElement.getAttribute("language");
        this.dependsVersion = dependsElement.getAttribute("version");
    }
    
    public String getGender()
    {
        return gender;
    }

    public String getDependsLanguage()
    {
        return dependsLanguage;
    }
    
    public String getDependsVersion()
    {
        return dependsVersion;
    }
}
