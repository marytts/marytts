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

package marytts.tools.install;

import java.net.MalformedURLException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author marc
 *
 */
public class VoiceComponentDescription extends ComponentDescription
{
    private String gender;
    private String type;
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
        this.type = xmlDescription.getAttribute("type");
        Element dependsElement = (Element) xmlDescription.getElementsByTagName("depends").item(0);
        this.dependsLanguage = dependsElement.getAttribute("language");
        this.dependsVersion = dependsElement.getAttribute("version");
    }
    
    @Override
    public String getComponentTypeString()
    {
        return "voice";
    }

    
    public String getGender()
    {
        return gender;
    }
    
    public String getType()
    {
        return type;
    }

    public String getDependsLanguage()
    {
        return dependsLanguage;
    }
    
    public String getDependsVersion()
    {
        return dependsVersion;
    }
    
    @Override
    protected Document createComponentXML()
    throws ParserConfigurationException
    {
        Document doc = super.createComponentXML();
        NodeList nodes = doc.getElementsByTagName(getComponentTypeString());
        assert nodes.getLength() == 1;
        Element voiceElt = (Element) nodes.item(0);
        voiceElt.setAttribute("type", type);
        voiceElt.setAttribute("gender", gender);
        Element dependsElt = (Element) voiceElt.appendChild(doc.createElementNS(ComponentDescription.installerNamespaceURI, "depends"));
        dependsElt.setAttribute("language", dependsLanguage);
        dependsElt.setAttribute("version", dependsVersion);
        return doc;
    }
}
