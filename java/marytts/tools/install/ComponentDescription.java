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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import marytts.util.MaryUtils;

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
public class ComponentDescription extends Observable
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
            this.license = new URL(licenseElement.getAttribute("href"));
        } catch (MalformedURLException mue) {
            new Exception("Invalid license URL -- ignoring", mue).printStackTrace();
            this.license = null;
        }
        Element packageElement = (Element) xmlDescription.getElementsByTagName("package").item(0);
        packageFilename = packageElement.getAttribute("filename");
        packageSize = Integer.parseInt(packageElement.getAttribute("size"));
        packageMD5 = packageElement.getAttribute("md5sum");
        NodeList locationElements = packageElement.getElementsByTagName("location");
        locations = new ArrayList<URL>(locationElements.getLength());
        for (int i=0, max = locationElements.getLength(); i<max; i++) {
            Element aLocationElement = (Element) locationElements.item(i);
            try {
                locations.add(new URL(aLocationElement.getAttribute("href")+"/"+packageFilename));
            } catch (MalformedURLException mue) {
                new Exception("Invalid location -- ignoring", mue).printStackTrace();
            }
        }
        archiveFile = new File(System.getProperty("mary.downloadDir"), packageFilename);
        infoFile = new File(System.getProperty("mary.installedDir"), getInfoFilename());
        determineStatus();
    }
    
    private void determineStatus()
    {
        File installedDir = new File(System.getProperty("mary.installedDir"));
        File downloadDir = new File(System.getProperty("mary.downloadDir"));
        
        if (new File(installedDir, getInfoFilename()).exists()) {
            status = Status.INSTALLED;
        } else if (new File(downloadDir, packageFilename).exists()) {
            status = Status.DOWNLOADED;
        } else if (locations.size() > 0) {
            status = Status.AVAILABLE;
        } else {
            status = Status.ERROR;
        }
    }
    
    public String getInfoFilename()
    {
        return name+"-"+version+"."+getComponentTypeString();
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
    
    public String getPackageFilename()
    {
        return packageFilename;
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
        if (value != isSelected) {
            isSelected = value;
            stateChanged();
        }
    }
    
    public Status getStatus()
    {
        return status;
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
        JTextPane licensePane = new JTextPane();
        if (license != null) {
            licensePane.setPage(license);
        } else {
            licensePane.setText("Unknown license for "+getComponentTypeString()+" component '"+this.getName()+"' -- only proceed if you are certain you have the right to install this component!");
        }
        JScrollPane scroll = new JScrollPane(licensePane);
        final JOptionPane optionPane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION, null, new String[] {"Reject", "Accept"}, "Reject");
        optionPane.setPreferredSize(new Dimension(640,480));
        final JDialog dialog = new JDialog((Frame)null, "Do you accept the following license?", true);
        dialog.setContentPane(optionPane);
        optionPane.addPropertyChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        String prop = e.getPropertyName();

                        if (dialog.isVisible() 
                         && (e.getSource() == optionPane)
                         && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
                            dialog.setVisible(false);
                        }
                    }
                });
        dialog.pack();
        dialog.setVisible(true);
        
        if (!"Accept".equals(optionPane.getValue())) {
            System.out.println("License not accepted. Installation of component cannot proceed.");
            status = Status.DOWNLOADED;
            stateChanged();
            return;
        }
        System.out.println("License accepted.");
        Installer inst = new Installer();
        if (synchronous) {
            inst.run();
        } else {
            new Thread(inst).start();
        }
    }
    
    /**
     * Uninstall this voice.
     * @return true if voice was successfully uninstalled, false otherwise.
     */
    public boolean uninstall()
    {
/*        int answer = JOptionPane.showConfirmDialog(null, "Completely remove "+getComponentTypeString()+" '"+toString()+"' from the file system?", "Confirm component uninstall", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return false;
        }
        */
        try {
            String maryBase = System.getProperty("mary.base");
            System.out.println("Removing "+name+"-"+version+" from "+maryBase+"...");
            BufferedReader br = new BufferedReader(new FileReader(infoFile));
            LinkedList<String> files = new LinkedList<String>();
            String line = null;
            while ((line = br.readLine()) != null) {
                files.addFirst(line); // i.e., reverse order
            }
            for (String file: files) {
                if (file.trim().equals("")) continue; // skip empty lines
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
            return 100*downloaded/size;
        } else if (status == Status.INSTALLING) {
            return -1;
        }
        return 100;
    }
    
    private void writeComponentXML()
    throws Exception
    {
        File archiveFolder = archiveFile.getParentFile();
        String archiveFilename = archiveFile.getName();
        String compdescFilename = archiveFilename.substring(0, archiveFilename.lastIndexOf('.')) + "-component.xml";
        Document doc = createComponentXML();
        DOMImplementation implementation = DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0");
        DOMImplementationLS domImplLS = (DOMImplementationLS) implementation.getFeature("LS", "3.0");
        LSSerializer serializer = domImplLS.createLSSerializer();
        DOMConfiguration config = serializer.getDomConfig();
        config.setParameter("format-pretty-print", Boolean.TRUE);
        LSOutput output = domImplLS.createLSOutput();
        output.setEncoding("UTF-8");
        FileOutputStream fos = new FileOutputStream(compdescFilename);
        output.setByteStream(fos);
        serializer.write(doc, output);
        fos.close();
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
        licenseElt.setAttribute("href", license.toString());
        Element packageElt = (Element) desc.appendChild(doc.createElementNS(installerNamespaceURI, "package"));
        packageElt.setAttribute("size", Integer.toString(packageSize));
        packageElt.setAttribute("md5sum", packageMD5);
        for (URL l : locations) {
            Element lElt = (Element) packageElt.appendChild(doc.createElementNS(installerNamespaceURI, "location"));
            lElt.setAttribute("href", l.toString());
        }
        return doc;
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
                    String hash = MD5.asHex(MD5.getHash(archiveFile));
                    if (hash.equals(packageMD5)) {
                        System.err.println("ok!");
                        writeComponentXML();
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
            StringBuffer files = new StringBuffer();
            try {
                ZipFile zipfile = new ZipFile(archiveFile);
                Enumeration<? extends ZipEntry> entries = zipfile.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    files.append(entry.getName());
                    files.append("\n");
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
                    }
                  }
                  zipfile.close();
                  PrintWriter pw = new PrintWriter(infoFile);
                  pw.println(files);
                  pw.close();
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
