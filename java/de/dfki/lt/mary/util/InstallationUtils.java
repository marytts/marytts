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
package de.dfki.lt.mary.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import de.dfki.lt.mary.MaryProperties;

public class InstallationUtils
{
    
    public static void directRunInstaller(URL installerURL, String component) throws Exception
    {
        System.err.println("downloading and running installer...");
        ClassLoader aCL = Thread.currentThread().getContextClassLoader();
        URLClassLoader aUrlCL = new URLClassLoader(new URL[] {installerURL}, aCL);
        Thread.currentThread().setContextClassLoader(aUrlCL);
        File autoinstallFile = createAutoInstaller(component);
        Class installerClass = Class.forName("de.dfki.lt.izpack.AutomatedInstaller", true, aUrlCL);
        Constructor constr = installerClass.getConstructors()[0];
        constr.newInstance(new Object[] {autoinstallFile.getPath()});
        // this is: new AutomatedInstaller(autoinstallFile.getPath());
    }
    /**
     * Download an installer .jar file from the given URL and run it
     * as an external process.
     * @param installerURL the URL of the installer .jar file.
     * @param component name of the component to install
     * @return the exit code of the installer process
     * @throws IOException when a download problem occurs
     * @throws InterruptedException when we are interrupted while waiting for 
     * the process to complete.
     */
    public static int downloadAndRunInstaller(URL installerURL, String component)
    throws IOException, InterruptedException
    {
        System.err.print("Connecting...");
        //ReadableByteChannel rbc = Channels.newChannel(installerURL.openStream());
        InputStream is = installerURL.openStream();
        System.err.print(" downloading...");
        String path = installerURL.getFile();
        String filename = path.substring(path.lastIndexOf("/")+1);
        File targetFile = File.createTempFile(filename, ".jar");
        //FileChannel targetFC = new FileOutputStream(targetFile).getChannel();
        //ByteBuffer bb = ByteBuffer.allocate(8192);
        FileOutputStream fos = new FileOutputStream(targetFile);
        byte[] buf = new byte[8192];
        int nr = -1;
        //while ((nr = rbc.read(bb)) != -1) {
        //    targetFC.write(bb);
        //    bb.rewind();
        //}
        //rbc.close();
        //targetFC.close();
        while ((nr = is.read(buf)) != -1) {
            fos.write(buf, 0, nr);
        }
        System.err.print(" calling installer...");
        File autoinstallFile = createAutoInstaller(component);
        String[] cmd = new String[] {
                System.getProperty("java.home")+"/bin/java",
                "-Dlicensepanel.title=\"Installing_"+component+"\"",
                "-cp",
                targetFile.getPath(),
                "de.dfki.lt.izpack.AutomatedInstaller",
                autoinstallFile.getPath()};
        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        while ((line = errReader.readLine()) != null) {
            System.err.println(line);
        }
        process.waitFor();
        if (process.exitValue() == 0) { 
            System.err.println(" done!");
        } else {
            System.err.println(" exit code " + process.exitValue());
        }
        return process.exitValue();
        //return 0;
    }
    
    /**
     * Create a file containing the autoinstaller script for izpack to install
     * the named component in the current mary install directory.
     * @param component name of the component to install
     * @return a file object pointing to a newly created temp file on the file system
     * which contains the autoinstaller script
     * @throws IOException if the file could not be created
     */
    public static File createAutoInstaller(String component) throws IOException
    {
        String auto1 = "<AutomatedInstallation langpack=\"eng\">\n"+
            "    <com.izforge.izpack.panels.HelloPanel/>\n" +
            "    <com.izforge.izpack.panels.TargetPanel>\n" +
            "        <installpath>";
        String auto2 = "</installpath>\n" +
            "    </com.izforge.izpack.panels.TargetPanel>\n" +
            "    <com.izforge.izpack.panels.PacksPanel>\n" +
            "        <pack name=\"";
        String auto3 = "\" selected=\"true\"/>\n" +
            "    </com.izforge.izpack.panels.PacksPanel>\n" +
            "    <de.dfki.lt.izpack.LicensePanel/>\n" +
            "    <de.dfki.lt.izpack.LicensePanel/>\n" +
            "    <de.dfki.lt.izpack.LicensePanel/>\n" +
            "    <com.izforge.izpack.panels.InstallPanel/>\n" +
            "    <com.izforge.izpack.panels.ShortcutPanel/>\n" +
            "    <com.izforge.izpack.panels.XInfoPanel/>\n" +
            "    <com.izforge.izpack.panels.ProcessPanel/>\n" +
            "    <com.izforge.izpack.panels.SimpleFinishPanel/>\n" +
            "</AutomatedInstallation>";
        File f = File.createTempFile("autoinstall", ".xml");
        PrintWriter pw = new PrintWriter(new FileOutputStream(f));
        pw.println(auto1 + MaryProperties.maryBase() + auto2 + component + auto3);
        pw.close();
        return f;
    }

}
