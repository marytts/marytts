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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import marytts.util.MaryUtils;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author marc
 *
 */
public class ComponentDescription 
{
    private String name;
    private Locale locale;
    private String version;
    private String description;
    private URL license;
    private List<URL> locations;
    private int packageSize;
    private String packageMD5;
    private boolean isSelected = false;
    private String status = "not yet implemented";
    
    protected ComponentDescription(Element xmlDescription)
    throws NullPointerException, MalformedURLException
    {
        this.name = xmlDescription.getAttribute("name");
        this.locale = MaryUtils.string2locale(xmlDescription.getAttribute("locale"));
        this.version = xmlDescription.getAttribute("version");
        Element descriptionElement = (Element) xmlDescription.getElementsByTagName("description").item(0);
        this.description = descriptionElement.getTextContent().trim();
        Element licenseElement = (Element) xmlDescription.getElementsByTagName("license").item(0);
        this.license = new URL(licenseElement.getAttribute("xlink:href"));
        Element packageElement = (Element) xmlDescription.getElementsByTagName("package").item(0);
        packageSize = Integer.parseInt(packageElement.getAttribute("size"));
        packageMD5 = packageElement.getAttribute("md5sum");
        NodeList locationElements = packageElement.getElementsByTagName("location");
        locations = new ArrayList<URL>(locationElements.getLength());
        for (int i=0, max = locationElements.getLength(); i<max; i++) {
            Element aLocationElement = (Element) locationElements.item(i);
            locations.add(new URL(aLocationElement.getAttribute("xlink:href")));
        }
    }
    
    public String getName()
    {
        return name;
    }
    
    public Locale getLocale()
    {
        return locale;
    }
    
    public String getVersion()
    {
        return version;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public URL getLicenseURL()
    {
        return license;
    }
    
    public List<URL> getLocations()
    {
        return locations;
    }
    
    public int getPackageSize()
    {
        return packageSize;
    }
    
    public String getDisplayPackageSize()
    {
        if (packageSize >= 10*1024*1024) {
            return (packageSize/(1024*1024))+"MB";
        } else if (packageSize >= 10*1024) {
            return (packageSize/1024)+"kB";
        } else {
            return Integer.toString(packageSize);
        }
    }

    public String getPackageMD5Sum()
    {
        return packageMD5;
    }
    
    public boolean isSelected()
    {
        return isSelected;
    }
    
    public void setSelected(boolean value)
    {
        isSelected = value;
    }
    
    public String getStatus()
    {
        return status;
    }
    
    public String toString()
    {
        return name;
    }
}
