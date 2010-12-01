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

import java.awt.Dimension;
import java.awt.Frame;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import marytts.util.MaryUtils;
import marytts.util.dom.DomUtils;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.twmacinta.util.MD5;

/**
 * @author marc
 *
 */
public class ComponentDescription extends Observable implements Comparable<ComponentDescription>
{
    public enum Status {AVAILABLE, DOWNLOADING, PAUSED, VERIFYING, DOWNLOADED, INSTALLING, CANCELLED, ERROR, INSTALLED};

    public static final String installerNamespaceURI = "http://mary.dfki.de/installer";
    // Max size of download buffer.
    private static final int MAX_BUFFER_SIZE = 1024;

    private String name;
    private Locale locale;
    private String version;
    private String description;
    private URL license;
    private List<URL> locations;
    private String packageFilename;
    private int packageSize;
    private String packageMD5;
    private boolean isSelected = false;
    private Status status;
    private File archiveFile;
    private File infoFile;
    private int downloaded = 0;
    private int size = -1;
    private String installedFilesNames = null; // must be != null for installed components
    private Set<String> sharedFileNames;
    
    /**
     * An available update is non-null in the following circumstance:
     * 1. the present component (this) has status INSTALLED;
     * 2. a component is available that has the same name and type, but a higher version number.
     * Only the newest update will be considered, i.e. if there are more than one newer version available,
     * only the one with the highest version number will be remembered here.
     */
    private ComponentDescription availableUpdate = null;
    
    
    /**
     * Replace this component definition with its available update.
     * The object will stay the same but all data will be overwritten with 
     * the content of the update.
     * After this method returns, isUpdateAvailable() will return false.
     */
    public void replaceWithUpdate() {
        if (availableUpdate == null) {
            return;
        }
        this.name = availableUpdate.name;
        this.locale = availableUpdate.locale;
        this.version = availableUpdate.version;
        this.description = availableUpdate.description;
        this.license = availableUpdate.license;
        this.locations = availableUpdate.locations;
        this.packageFilename = availableUpdate.packageFilename;
        this.packageSize = availableUpdate.packageSize;
        this.packageMD5 = availableUpdate.packageMD5;
        this.isSelected = availableUpdate.isSelected;
        this.status = availableUpdate.status;
        this.archiveFile = availableUpdate.archiveFile;
        this.infoFile = availableUpdate.infoFile;
        this.downloaded = availableUpdate.downloaded;
        this.size = availableUpdate.size;
        this.installedFilesNames = availableUpdate.installedFilesNames;
        this.availableUpdate = null;
        stateChanged();
    }
    
    protected ComponentDescription(String name, String version, String packageFilename) {
        this.name = name;
        this.version = version;
        this.packageFilename = packageFilename;
    }
    
    protected ComponentDescription(Element xmlDescription)
    throws NullPointerException
    {
        this.name = xmlDescription.getAttribute("name");
        this.locale = MaryUtils.string2locale(xmlDescription.getAttribute("locale"));
        this.version = xmlDescription.getAttribute("version");
        Element descriptionElement = (Element) xmlDescription.getElementsByTagName("description").item(0);
        this.description = descriptionElement.getTextContent().trim();
        Element licenseElement = (Element) xmlDescription.getElementsByTagName("license").item(0);
        try {
            this.license = new URL(licenseElement.getAttribute("href").trim().replaceAll(" ", "%20"));
        } catch (MalformedURLException mue) {
            new Exception("Invalid license URL -- ignoring", mue).printStackTrace();
            this.license = null;
        }
        Element packageElement = (Element) xmlDescription.getElementsByTagName("package").item(0);
        packageFilename = packageElement.getAttribute("filename").trim();
        packageSize = Integer.parseInt(packageElement.getAttribute("size"));
        packageMD5 = packageElement.getAttribute("md5sum");
        NodeList locationElements = packageElement.getElementsByTagName("location");
        locations = new ArrayList<URL>(locationElements.getLength());
        for (int i=0, max = locationElements.getLength(); i<max; i++) {
            Element aLocationElement = (Element) locationElements.item(i);
            try {
                String urlString = aLocationElement.getAttribute("href").trim().replaceAll(" ", "%20");
                if (!urlString.endsWith(packageFilename)) {
                    if (!urlString.endsWith("/")) {
                        urlString += "/";
                    }
                    urlString += packageFilename;
                }
                locations.add(new URL(urlString));
            } catch (MalformedURLException mue) {
                new Exception("Invalid location -- ignoring", mue).printStackTrace();
            }
        }
        archiveFile = new File(System.getProperty("mary.downloadDir"), packageFilename);
        String infoFilename = packageFilename.substring(0, packageFilename.lastIndexOf('.')) + "-component.xml";
        infoFile = new File(System.getProperty("mary.installedDir"), infoFilename);
        determineStatus();
        NodeList filesElements = xmlDescription.getElementsByTagName("files");
        if (filesElements.getLength() > 0) {
            Element filesElement = (Element) filesElements.item(0);
            installedFilesNames = filesElement.getTextContent();
        }
    }
    
    
    private void determineStatus()
    {
        File installedDir = new File(System.getProperty("mary.installedDir", "installed"));
        File downloadDir = new File(System.getProperty("mary.downloadDir", "download"));

        if (infoFile.exists()) {
            status = Status.INSTALLED;
        } else if (archiveFile.exists()) {
            if (archiveFile.length() == packageSize) {
                status = Status.DOWNLOADED;
            } else {
                status = Status.AVAILABLE;
            }
        } else if (locations.size() > 0) {
            status = Status.AVAILABLE;
        } else {
            status = Status.ERROR;
        }
    }

    
    public String getComponentTypeString()
    {
        return "component";
    }
    
    public String getName()
    {
        return name;
    }
    
    public Locale getLocale()
    {
        return locale;
    }
    
    public void setLocale(Locale aLocale) {
        this.locale = aLocale;
    }
    
    public String getVersion()
    {
        return version;
    }
    
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription) {
        this.description = aDescription;
    }
    

    public URL getLicenseURL()
    {
        return license;
    }

    public void setLicenseURL(URL aLicense) {
        this.license = aLicense;
    }

    public List<URL> getLocations()
    {
        return locations;
    }
    
    
    public void addLocation(URL aLocation) {
        if (this.locations == null) {
            this.locations = new ArrayList<URL>();
        }
        locations.add(aLocation);
    }
    

    public String getPackageFilename()
    {
        return packageFilename;
    }
    
    public int getPackageSize()
    {
        return packageSize;
    }
    
    public void setPackageSize(int aSize) {
        this.packageSize = aSize;
    }

    public String getDisplayPackageSize()
    {
        return MaryUtils.toHumanReadableSize(packageSize);
    }

    public String getPackageMD5Sum()
    {
        return packageMD5;
    }
    
    public void setPackageMD5Sum(String aMD5) {
        this.packageMD5 = aMD5;
    }

    public boolean isSelected()
    {
        return isSelected;
    }
    
    public void setSelected(boolean value)
    {
        if (value != isSelected) {
            isSelected = value;
            stateChanged();
        }
    }
    
    public Status getStatus()
    {
        return status;
    }
    
    public LinkedList<String> getInstalledFileNames() {
        LinkedList<String> files = new LinkedList<String>();
        StringTokenizer st = new StringTokenizer(installedFilesNames, ",");
        while (st.hasMoreTokens()) {
            String next = st.nextToken().trim();
            if (!"".equals(next)) {
                files.addFirst(next); // i.e., reverse order
            }
        }
        return files;
    }
    
    public void setSharedFiles(Set<String> fileList) {
        sharedFileNames = fileList;
    }
    
    public String toString()
    {
        return name;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ComponentDescription)) {
            return false;
        }
        ComponentDescription o = (ComponentDescription) obj;
        return name.equals(o.name) && locale.equals(o.locale) && version.equals(o.version);
        
    }

    
    // Pause this download.
    public void pause() {
        status = Status.PAUSED;
        stateChanged();
    }
    
    // Resume this download.
    public void resume(boolean synchronous) {
        status = Status.DOWNLOADING;
        stateChanged();
        download(synchronous);
    }
    
    // Cancel this download.
    public void cancel() {
        status = Status.CANCELLED;
        stateChanged();
    }
    
    // Mark this download as having an error.
    private void error() {
        status = Status.ERROR;
        stateChanged();
    }
    
    // Start or resume downloading.
    public void download(boolean synchronous) {
        Downloader d = new Downloader();
        if (synchronous) {
            d.run();
        } else {
            new Thread(d).start();
        }
    }



    // Notify observers that this download's status has changed.
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
    
    
    /**
     * Install this component, if the user accepts the license.
     */
    public void install(boolean synchronous) throws Exception
    {
        status = Status.INSTALLING;
        stateChanged();
        Installer inst = new Installer();
        if (synchronous) {
            inst.run();
        } else {
            new Thread(inst).start();
        }
    }
    
    /**
     * Uninstall this component.
     * @return true if component was successfully uninstalled, false otherwise.
     */
    public boolean uninstall() {
        if (status != Status.INSTALLED) {
            throw new IllegalStateException("Can only uninstall installed components, but status is "+status.toString());
        }
        assert installedFilesNames != null; // when we have an installed component, we must also have this information
/*        int answer = JOptionPane.showConfirmDialog(null, "Completely remove "+getComponentTypeString()+" '"+toString()+"' from the file system?", "Confirm component uninstall", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return false;
        }
        */
        try {
            String maryBase = System.getProperty("mary.base");
            System.out.println("Removing "+name+"-"+version+" from "+maryBase+"...");
            LinkedList<String> files = getInstalledFileNames();
            for (String file: files) {
                if (file.trim().equals("")) continue; // skip empty lines
                if (sharedFileNames != null && sharedFileNames.contains(file)) {
                    System.out.println("Keeping shared file: " + file);
                    continue; // don't uninstall shared files!
                }
                File f = new File(maryBase+"/"+file);
                if (f.isDirectory()) {
                    String[] kids = f.list();
                    if (kids.length == 0) {
                        System.err.println("Removing empty directory: "+file);
                        f.delete();
                    } else {
                        System.err.println("Cannot delete non-empty directory: "+file);
                    }
                } else if (f.exists()){ // not a directory
                    System.err.println("Removing file: "+file);
                    f.delete();
                } else { // else, file doesn't exist
                    System.err.println("File doesn't exist -- cannot delete: "+file);
                }
            }
            infoFile.delete();
        } catch (Exception e) {
            System.err.println("Cannot uninstall:");
            e.printStackTrace();
            return false;
        }
        determineStatus();
        return true;
    }
    
    public int getProgress()
    {
        if (status == Status.DOWNLOADING) {
            return (int) (100L*downloaded/size);
        } else if (status == Status.INSTALLING) {
            return -1;
        }
        return 100;
    }
    
    private void writeDownloadedComponentXML()
    throws Exception
    {
        File archiveFolder = archiveFile.getParentFile();
        String archiveFilename = archiveFile.getName();
        String compdescFilename = archiveFilename.substring(0, archiveFilename.lastIndexOf('.')) + "-component.xml";
        File compdescFile = new File(archiveFolder, compdescFilename);
        Document doc = createComponentXML();

        DomUtils.document2File(doc, compdescFile);
    }

    /**
     * Write a component xml file to the installed/ folder, containing the given list of installed files.
     * @param installedFilesList
     * @throws Exception
     */
    private void writeInstalledComponentXML()
    throws Exception
    {
        assert installedFilesNames != null;
        File installedFolder = infoFile.getParentFile();
        String archiveFilename = archiveFile.getName();
        String compdescFilename = archiveFilename.substring(0, archiveFilename.lastIndexOf('.')) + "-component.xml";
        File compdescFile = new File(installedFolder, compdescFilename);
        Document doc = createComponentXML();
        DomUtils.document2File(doc, compdescFile);
    }


    protected Document createComponentXML()
    throws ParserConfigurationException 
    {
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        fact.setNamespaceAware(true);
        Document doc = fact.newDocumentBuilder().newDocument();
        Element root = (Element) doc.appendChild(doc.createElementNS(installerNamespaceURI, "marytts-install"));
        Element desc = (Element) root.appendChild(doc.createElementNS(installerNamespaceURI, getComponentTypeString()));
        desc.setAttribute("locale", MaryUtils.locale2xmllang(locale));
        desc.setAttribute("name", name);
        desc.setAttribute("version", version);
        Element descriptionElt = (Element) desc.appendChild(doc.createElementNS(installerNamespaceURI, "description"));
        descriptionElt.setTextContent(description);
        Element licenseElt = (Element) desc.appendChild(doc.createElementNS(installerNamespaceURI, "license"));
        if (license != null) {
            licenseElt.setAttribute("href", license.toString());
        }
        Element packageElt = (Element) desc.appendChild(doc.createElementNS(installerNamespaceURI, "package"));
        packageElt.setAttribute("size", Integer.toString(packageSize));
        packageElt.setAttribute("md5sum", packageMD5);
        packageElt.setAttribute("filename", packageFilename);
        for (URL l : locations) {
            // Serialize the location without the filename:
            String urlString = l.toString();
            if (urlString.endsWith(packageFilename)) {
                urlString = urlString.substring(0, urlString.length()-packageFilename.length());
            }
            Element lElt = (Element) packageElt.appendChild(doc.createElementNS(installerNamespaceURI, "location"));
            lElt.setAttribute("href", urlString);
        }
        if (installedFilesNames != null) {
            Element filesElement = (Element) desc.appendChild(doc.createElementNS(installerNamespaceURI, "files"));
            filesElement.setTextContent(installedFilesNames);
        }
        return doc;
    }
    
    /**
     * Inform whether an update is available for this component.
     * @return
     */
    public boolean isUpdateAvailable() {
        return availableUpdate != null;
    }
    
    /**
     * If this component has an available update, get that update. 
     * @return null if no update is available.
     */
    public ComponentDescription getAvailableUpdate() {
        return availableUpdate;
    }
    
    /**
     * Set the given component description as the available update of this component.
     * If we already have an available update, then aDesc is only retained as the available update
     * if it has a higher version number than the previously set update.
     * @param aDesc an available update, or null to erase any previously set available updates.
     * @throws IllegalStateException if aDesc is not null and our status is not "INSTALLED".
     * @throws IllegalArgumentException if aDesc is not null and our name is not the same as aDesc's name, or if our version number
     * is not smaller than aDesc's version number.
     */
    public void setAvailableUpdate(ComponentDescription aDesc) {
        if (aDesc == null) {
            this.availableUpdate = null;
            stateChanged();
            return;
        }
        if (this.status != Status.INSTALLED) {
            throw new IllegalStateException("Can only set an available update if status is installed, but status is "+status.toString());
        }
        if (!aDesc.getName().equals(name)) {
            throw new IllegalArgumentException("Only a component with the same name can be an update of this component; but this has name "
                    +name+", and argument has name "+aDesc.getName());
        }
        if (!(version.compareTo(aDesc.getVersion()) < 0)) {
            throw new IllegalArgumentException("Version "+aDesc.getVersion()+" is not higher than installed version "+version);
        }
        if (availableUpdate != null) { // already have an available update: we will replace it with aDesc only if aDesc has a higher version number
            if (!(aDesc.getVersion().compareTo(availableUpdate.getVersion()) > 0)) {
                return;
            }
        }
        this.availableUpdate = aDesc;
        stateChanged();
    }

    /**
     * This is an update of other if and only if the following is true:
     * <ol>
     * <li>Both components have the same type (as identified by the class) and name;</li>
     * <li>other has status INSTALLED;</li>
     * <li>our version number is higher than other's version number.</li>
     * </ol>
     * @param other
     * @return
     */
    public boolean isUpdateOf(ComponentDescription other) {
        if (other == null
            || !this.getClass().equals(other.getClass())
            || !name.equals(other.getName())
            || other.getStatus() != Status.INSTALLED
            || !(version.compareTo(other.getVersion()) > 0)) {
            return false;
        }
        return true;
    }
    
    
    /**
     * Define a natural ordering for component descriptions. Languages first, in alphabetic order, then voices, in alphabetic order.
     */
    public int compareTo(ComponentDescription o) {
        int myPos = 0;
        int oPos = 0;
        if (this instanceof LanguageComponentDescription) {
            myPos = 5;
        } else if (this instanceof VoiceComponentDescription) {
            myPos = 10;
        }
        if (o instanceof LanguageComponentDescription) {
            oPos = 5;
        } else if (o instanceof VoiceComponentDescription) {
            oPos = 10;
        }

        if (oPos - myPos != 0) {
            return (oPos - myPos);
        }
        
        // Same type, sort by name
        return name.compareTo(o.name);
    }

    
    
    public static final void copyInputStream(InputStream in, OutputStream out)
    throws IOException
    {
      byte[] buffer = new byte[1024];
      int len;

      while((len = in.read(buffer)) >= 0)
        out.write(buffer, 0, len);

      in.close();
      out.close();
    }


    class Downloader implements Runnable
    {
        public void run()
        {
            status = Status.DOWNLOADING;
            stateChanged();
            
            RandomAccessFile file = null;
            InputStream stream = null;
            
            HttpURLConnection connection = null;
            Exception connectException = null;
            URL connectedURL = null;
            for (URL u : locations) {
                try {
                    // Open connection to URL.
                    connection = (HttpURLConnection) u.openConnection();
                    // Specify what portion of file to download.
                    connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
                    // Connect to server.
                    connection.connect();
                    // Make sure response code is in the 200 range.
                    if (connection.getResponseCode() / 100 != 2) {
                        continue; // try next location
                    }
                    // Check for valid content length.
                    int contentLength = connection.getContentLength();
                    if (contentLength < 1) {
                        continue; // try next location
                    }
                    
                    /* Set the size for this download if it
                       hasn't been already set. */
                    if (size == -1) {
                        size = contentLength;
                        stateChanged();
                    }
                    connectException = null;
                    connectedURL = u;
                    break; // current location seems OK, leave loop
                } catch (Exception exc) {
                    connectException = exc;
                }
            }

            if (connectException != null) {
                connectException.printStackTrace();
                error();
            } else if (connection == null) {
                error();
            }
            
            System.err.println("Connected to "+connectedURL+", downloading "+(downloaded > 0 ? "from byte "+downloaded : ""));
            try {
                // Open file and seek to the end of it.
                file = new RandomAccessFile(archiveFile, "rw");
                file.seek(downloaded);
                
                stream = connection.getInputStream();
                byte[] buffer = new byte[MAX_BUFFER_SIZE];
                while (status == Status.DOWNLOADING) {
                    /* target number of bytes to download depends on how much of the
                       file is left to download. */
                    int len = Math.min(buffer.length, size-downloaded);
                    // Read from server into buffer.
                    int read = stream.read(buffer, 0, len);
                    if (read == -1)
                        break;
                    
                    // Write buffer to file.
                    file.write(buffer, 0, read);
                    downloaded += read;
                    stateChanged();
                }
                    
                /* Change status to complete if this point was
                   reached because downloading has finished. */
                if (status == Status.DOWNLOADING) {
                    System.err.println("Download of "+packageFilename+" has finished.");
                    System.err.print("Computing checksum...");
                    status = Status.VERIFYING;
                    stateChanged();
                    String hash = MD5.asHex(MD5.getHash(archiveFile));
                    if (hash.equals(packageMD5)) {
                        System.err.println("ok!");
                        writeDownloadedComponentXML();
                        status = Status.DOWNLOADED;
                    } else {
                        System.err.println("failed!");
                        System.out.println("MD5 according to component description: "+packageMD5);
                        System.out.println("MD5 computed:   "+hash);
                        status = Status.ERROR;
                        downloaded = 0;
                    }
                    stateChanged();
                }
            } catch (Exception e) {
                e.printStackTrace();
                error();
            } finally {
                // Close file.
                if (file != null) {
                    try {
                        file.close();
                    } catch (Exception e) {}
                }
                
                // Close connection to server.
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {}
                }
            }

        }
        
    }
    
    class Installer implements Runnable
    {
        public void run() {
            String maryBase = System.getProperty("mary.base");
            System.out.println("Installing "+name+"-"+version+" in "+maryBase+"...");
            StringBuilder files = new StringBuilder();
            try {
                ZipFile zipfile = new ZipFile(archiveFile);
                Enumeration<? extends ZipEntry> entries = zipfile.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (files.length() > 0) {
                        files.append(", ");
                    }
                    files.append(entry.getName());
                    if(entry.isDirectory()) {
                      System.err.println("Extracting directory: " + entry.getName());
                      (new File(maryBase+"/"+entry.getName())).mkdir();
                    } else {
                        File newFile = new File(maryBase+"/"+entry.getName());
                        if (!newFile.getParentFile().isDirectory()) {
                            System.err.println("Creating directory tree: "+newFile.getParentFile().getAbsolutePath());
                            newFile.getParentFile().mkdirs();
                        }
                        System.err.println("Extracting file: " + entry.getName());
                        copyInputStream(zipfile.getInputStream(entry),
                           new BufferedOutputStream(new FileOutputStream(newFile)));
                        // TODO: ugly hack: try to set executable bit on files in bin/
                        if (entry.getName().startsWith("bin/")) {
                            try {
                                Runtime.getRuntime().exec(new String[] {"chmod", "a+x", newFile.getAbsolutePath()});
                            } catch (Exception e) {
                                // ignore, this will of course not work on windows
                                e.printStackTrace();
                            }
                        }
                    }
                  }
                  zipfile.close();
                  installedFilesNames = files.toString();
                  writeInstalledComponentXML();
            } catch (Exception e) {
                System.err.println("... installation failed:");
                e.printStackTrace();
                status = Status.ERROR;
                stateChanged();
            }
            System.err.println("...done");
            status = Status.INSTALLED;
            stateChanged();
        }
        


    }

    
}
