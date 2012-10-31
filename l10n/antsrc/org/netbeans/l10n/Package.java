/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.netbeans.l10n;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import org.netbeans.nbbuild.XMLUtil;
import org.netbeans.nbbuild.AutoUpdate;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.taskdefs.SignJar;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.ZipFileSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Package extends Task {

    private Path classpath;
    private boolean pError = false;
    HashMap<String, Vector<String>> nbms = new HashMap<String, Vector<String>>();
    File srcDir = null;
    private String jarSignerMaxMemory = "96m";

    /** Set the location of <samp>jhall.jar</samp> or <samp>jsearch.jar</samp> (JavaHelp tools library). */
    public Path createClasspath() {
        // JavaHelp release notes say jhtools.jar is enough, but class NoClassDefFoundError
        // on javax.help.search.IndexBuilder when I tried it...
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    public void setSrc(File f) {
        srcDir = f;
    }
    File distDir = null;

    public void setDist(File d) {
        distDir = d;
    }
    File nbmsDistDir = null;

    public void setNbmsDist(File d) {
        nbmsDistDir = d;
    }
    String locales;

    /** Set a comma-separated list of locales. */
    public void setLocales(String l) {
        locales = l;
    }
    File nbmsLocation = null;

    public void setNBMs(File f) {
        nbmsLocation = f;
    }
    public File keystore;
    public String storepass,  alias;

    /** Path to the keystore (private key). */
    public void setKeystore(File f) {
        keystore = f;
    }

    /** Password for the keystore.
     * If a question mark (<samp>?</samp>), the NBM will not be signed
     * and a warning will be printed.
     */
    public void setStorepass(String s) {
        storepass = s;
    }

    /** Alias for the private key. */
    public void setAlias(String s) {
        alias = s;
    }

    public void execute() throws BuildException {

        // Create all localized jars
        StringTokenizer tokenizer = new StringTokenizer(locales.trim(), ", ");
        while (tokenizer.hasMoreTokens()) {
            String loc = tokenizer.nextToken();
            processLocale(loc);
        }

        // Deal with NBMs creation
        DirectoryScanner ds = new DirectoryScanner();
        ds.setBasedir(nbmsLocation);
        ds.setIncludes(new String[]{"**/*.nbm"});
        ds.scan();
        Mkdir mkdir = (Mkdir) getProject().createTask("mkdir");
        Copy copy = (Copy) getProject().createTask("copy");
        File tmpDir = new File("tmp");
        tmpDir.mkdir();
        for (String nbm : ds.getIncludedFiles()) {
            String nbmName = nbm.substring(nbm.lastIndexOf(File.separator) + 1, nbm.lastIndexOf("."));
            Vector<String> nbmFiles = nbms.get(nbmName);
            if (nbmFiles == null) {
                log("There is no localization content for NBM: " + nbmName);
                continue;
            }
            String cluster = nbm.substring(0, nbm.indexOf(File.separator));
            File destNbmDir = new File(nbmsDistDir, cluster);
            File destNbmFile = new File(destNbmDir, nbmName + ".nbm");
            if (!destNbmDir.isDirectory()) {
                mkdir.setDir(destNbmDir);
                mkdir.execute();
            }
            copy.setFile(new File(nbmsLocation, nbm));
            copy.setTodir(destNbmDir);
            copy.execute();

            Jar jar = (Jar) getProject().createTask("jar");
            jar.setUpdate(true);
            jar.setDestFile(destNbmFile);
            ZipFileSet zfs = new ZipFileSet();
            zfs.setDir(new File(distDir, cluster));
            zfs.setPrefix("netbeans");
            zfs.appendIncludes(nbmFiles.toArray(new String[]{""}));
            jar.addFileset(zfs);

            //Process InfoXMLs
            tokenizer = new StringTokenizer(locales.trim(), ", ");
            while (tokenizer.hasMoreTokens()) {
                String loc = tokenizer.nextToken();
                try {
                    File jarF = new File(distDir, cluster + File.separator + "modules" + File.separator + "locale" + File.separator + nbmName + "_" + loc + ".jar");
                    if (!jarF.isFile()) {
                        log("No " + loc + " localization for " + nbmName);
                        continue;
                    }
                    //Find localized bundle
                    JarFile jarFile = new JarFile(new File(distDir, cluster + File.separator + "modules" + File.separator + "locale" + File.separator + nbmName + "_" + loc + ".jar"));
                    Properties p = new Properties();
                    ZipEntry bundleentry = jarFile.getEntry(nbmName.replace('-', '/') + File.separator + "Bundle_" + loc + ".properties");
                    if (bundleentry == null) {
                        //Read it from the NBM and module's jar manifest
                        JarFile nbmFile = new JarFile(destNbmFile);
                        String jarEntryName = "netbeans/modules/" + nbmName + ".jar";
                        ZipEntry ze = nbmFile.getEntry(jarEntryName);
                        InputStream is;
                        if(ze == null) {
                            //NBM is packed with pack200
                            ze = nbmFile.getEntry(jarEntryName + ".pack.gz");
                            if(ze!=null) {
                                File packedJar = File.createTempFile(nbmName, ".jar.pack.gz", tmpDir);
                                File unpackedJar = File.createTempFile(nbmName, ".jar", tmpDir);
                                unpackedJar.deleteOnExit();
                                packedJar.deleteOnExit();
                                InputStream fis = nbmFile.getInputStream(ze);
                                BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(packedJar));
                                byte [] buffer = new byte [4096];
                                int read = 0;
                                while ((read = fis.read(buffer)) != -1) {
                                    bof.write(buffer, 0, read);
                                }
                                bof.close();
                                fis.close();
                                AutoUpdate.unpack200(packedJar, unpackedJar);
                                is = new FileInputStream(unpackedJar);
                            } else {
                                throw new BuildException("Cannot find neither " +
                                        jarEntryName + ".pack.gz nor " +
                                        jarEntryName + " entry in " + nbmFile.getName());
                            }
                        } else {
                            is = nbmFile.getInputStream(ze);
                        }
                        
                        File tmpJar = File.createTempFile("module", ".jar", tmpDir);
                        BufferedOutputStream bof = new BufferedOutputStream(new FileOutputStream(tmpJar));
                        int ch = 0;
                        while ((ch = is.read()) != -1) {
                            bof.write(ch);
                        }
                        bof.close();
                        is.close();
                        JarFile moduleJar = new JarFile(tmpJar);
                        String bundlename = moduleJar.getManifest().getMainAttributes().getValue("OpenIDE-Module-Localizing-Bundle");
                        String bfname = bundlename.substring(0, bundlename.lastIndexOf('.'));
                        String bfext = bundlename.substring(bundlename.lastIndexOf('.'));
                        bundlename = bfname + "_" + loc + bfext;
                        bundleentry = jarFile.getEntry(bundlename);
                        moduleJar.close();
                        tmpJar.delete();
                    }
                    if (bundleentry != null) {
                        InputStream is = jarFile.getInputStream(bundleentry);
                        try {
                            p.load(is);
                        } finally {
                            is.close();
                        }
                        // Open the original info XML
                        JarFile nbmFile = new JarFile(destNbmFile);
                        Document doc = XMLUtil.parse(new InputSource(nbmFile.getInputStream(nbmFile.getEntry("Info/info.xml"))), false, false, new ErrorCatcher(), null);
                        Element manifest = (Element) doc.getElementsByTagName("manifest").item(0);

                        // Now pick up attributes from the bundle and put them to the info.xml
                        for (String attr : new String[]{"OpenIDE-Module-Name", "OpenIDE-Module-Display-Category", "OpenIDE-Module-Short-Description", "OpenIDE-Module-Long-Description"}) {
                            String value = p.getProperty(attr);
                            if (value != null) {
                                manifest.setAttribute(attr, value);
                            }
                        }
                        File infofile = new File(tmpDir, "info_" + loc + ".xml");
                        OutputStream infoStream = new FileOutputStream(infofile);
                        XMLUtil.write(doc, infoStream);
                        infoStream.close();
                        zfs = new ZipFileSet();
                        zfs.setDir(tmpDir);
                        zfs.setPrefix("Info/locale");
                        zfs.appendIncludes(new String[]{"info_" + loc + ".xml"});
                        jar.addFileset(zfs);
                    } else {
                        log("Can't find localizing bundle for " + nbmName);
                    }
                } catch (IOException ex) {
                    log("Problems with reading localization bundles for " + loc + ", NBM: " + nbmName, ex, Project.MSG_WARN);

                } catch (SAXException saxe) {
                    log("Problem with creating localized info.xml for " + loc + ", NBM: " + nbmName, saxe, Project.MSG_WARN);
                }

            }
            jar.execute();

            if (keystore != null && storepass != null && alias != null) {
                if (!keystore.isFile()) {
                    continue;
                }
                SignJar signjar = (SignJar) getProject().createTask("signjar");
                try { // Signatures changed in various Ant versions.

                    try {
                        SignJar.class.getMethod("setKeystore", File.class).invoke(signjar, keystore);
                    } catch (NoSuchMethodException x) {
                        SignJar.class.getMethod("setKeystore", String.class).invoke(signjar, keystore.getAbsolutePath());
                    }
                    try {
                        SignJar.class.getMethod("setJar", File.class).invoke(signjar, destNbmFile);
                    } catch (NoSuchMethodException x) {
                        SignJar.class.getMethod("setJar", String.class).invoke(signjar, destNbmFile.getAbsolutePath());
                    }
                } catch (BuildException x) {
                    throw x;
                } catch (Exception x) {
                    throw new BuildException(x);
                }
                signjar.setStorepass(storepass);
                signjar.setAlias(alias);
                signjar.setLocation(getLocation());
                signjar.setMaxmemory(this.jarSignerMaxMemory);
                signjar.init();
                signjar.execute();
            }
        }
        Delete delete = (Delete) getProject().createTask("delete");
        delete.setDir(tmpDir);
        delete.execute();
    }

    void processLocale(String locale) throws BuildException {
        DirectoryScanner ds = new DirectoryScanner();
        File baseSrcDir = new File(srcDir, locale);
        if (!baseSrcDir.exists()) {
            log("No files for locale: " + locale);
            return;
        }
        ds.setBasedir(baseSrcDir);
        String[] includes = new String[]{"*/*/*", "*/*/ext/*", "*/*/ext/locale/*", "*/*/netbeans/*/*", "*/*/netbeans/*/locale/*", "*/*/netbeans/*/nblib/*", "*/*/netbeans/*/extra/*", "*/*/docs/*", "*/*/locale/*", "*/*/netbeans/config/*/*"};
        String[] excludes = new String[]{"other/**", "*/*/netbeans", "*/*/netbeans/*", "*/*/netbeans/*/locale", "*/*/netbeans/*/nblib", "*/*/netbeans/*/extra", "*/*/docs", "*/*/ext", "*/*/ext/locale", "*/*/locale", "*/*/netbeans/config/*", "**/CVS/*", "**/CVS", "**/.svn/*", "**/.svn"};
        ds.setIncludes(includes);
        ds.setExcludes(excludes);
        ds.scan();
        Jar jar = (Jar) getProject().createTask("jar");
        Mkdir mkdir = (Mkdir) getProject().createTask("mkdir");
        Task locJH = getProject().createTask("locjhindexer");
        for (String dir : ds.getIncludedDirectories()) {
            String name = dir.substring(dir.lastIndexOf(File.separator) + 1);
            name = name.replaceAll("^vw-rh", "visualweb-ravehelp-rave_nbpack");
            name = name.replaceAll("^vw-", "visualweb-");
            String nbm = dir.substring(dir.indexOf(File.separator) + 1);
            nbm = nbm.substring(0, nbm.indexOf(File.separator));
            String cluster = dir.substring(0, dir.indexOf(File.separator));
            String subPath = dir.substring((cluster + File.separator + nbm + File.separator).length() - 1, dir.lastIndexOf(File.separator));
            if (!subPath.startsWith(File.separator + "netbeans")) {
                subPath = File.separator + "modules" + subPath;
                if (!name.startsWith("org-") && !name.startsWith("com-") && !(subPath.endsWith(File.separator + "ext") || subPath.endsWith(File.separator + "ext" + File.separator + "locale"))) {
                    name = "org-netbeans-modules-" + name;
                } else {
                    // Handle exception from ext/
                    if (name.startsWith("web-httpmonitor") || name.startsWith("deployment-deviceanywhere")) {
                        name = "org-netbeans-modules-" + name;
                    }
                }
            } else {
                subPath = subPath.substring((File.separator + "netbeans").length());
                //Handle exceptions form nblib
                if (name.startsWith("j2ee-ant") || name.startsWith("deployment-deviceanywhere") || name.startsWith("mobility-project") || name.startsWith("java-j2seproject-copylibstask")) {
                    name = "org-netbeans-modules-" + name;
                }
            }
            nbm = nbm.replaceAll("^vw-rh", "visualweb-ravehelp-rave_nbpack");
            nbm = nbm.replaceAll("^vw-", "visualweb-");
            if (!nbm.startsWith("org-") && !nbm.startsWith("com-")) {
                nbm = "org-netbeans-modules-" + nbm;
            }
            cluster = cluster.replaceAll("^vw", "visualweb");
            if (subPath.matches(".*/docs$")) {
                ds.setBasedir(new File(baseSrcDir, dir));
                ds.setIncludes(new String[]{"**/*.hs"});
                ds.setExcludes(new String[]{"**/CVS/*"});
                ds.setExcludes(new String[]{"**/.svn/*"});
                ds.scan();
                if (ds.getIncludedFilesCount() != 1) {
                    throw new BuildException("Can't find .hs file for " + name + " module.");
                }
                File hsFile = new File(new File(baseSrcDir, dir), ds.getIncludedFiles()[0]);
                File baseJHDir = hsFile.getParentFile();

                try {
                    System.out.println("Basedir: " + baseJHDir.getAbsolutePath());
                    locJH.getClass().getMethod("setBasedir", File.class).invoke(locJH, baseJHDir);
                    locJH.getClass().getMethod("setLocales", String.class).invoke(locJH, locale);
                    locJH.getClass().getMethod("setDbdir", String.class).invoke(locJH, "JavaHelpSearch");
                    ((Path)locJH.getClass().getMethod("createClasspath").invoke(locJH)).add(classpath);

                } catch (Exception ex) {
                    throw new BuildException("Can't run locJHInxeder", ex);
                }
                locJH.execute();
            }
            if (!subPath.endsWith("locale")) {
                subPath += File.separator + "locale";
            }
            String jarFileName = name + "_" + locale + ".jar";
            File distJarDir = new File(distDir.getAbsolutePath(), cluster + subPath);
            mkdir.setDir(distJarDir);
            mkdir.execute();
            jar.setBasedir(new File(baseSrcDir, dir));
            jar.setDestFile(new File(distJarDir, jarFileName));
            jar.execute();
            Vector<String> nbmFiles = nbms.get(nbm);
            if (nbmFiles == null) {
                nbmFiles = new Vector<String>();
                nbms.put(nbm, nbmFiles);
            }
            nbmFiles.add(subPath.substring(1) + File.separator + jarFileName);
        }
        ds.setBasedir(baseSrcDir);
        ds.setIncludes(includes);
        ds.setExcludes(excludes);
        ds.scan();
        Copy copy = (Copy) getProject().createTask("copy");
        for (String file : ds.getIncludedFiles()) {
            String name = file.substring(file.lastIndexOf(File.separator) + 1);
            name = name.replaceAll("^vw-rh", "visualweb-ravehelp-rave_nbpack");
            name = name.replaceAll("^vw-", "visualweb-");
            String nbm = file.substring(file.indexOf(File.separator) + 1);
            nbm = nbm.substring(0, nbm.indexOf(File.separator));
            String cluster = file.substring(0, file.indexOf(File.separator));
            String subPath = file.substring((cluster + File.separator + nbm + File.separator).length() - 1, file.lastIndexOf(File.separator));
            if (!subPath.startsWith(File.separator + "netbeans")) {
                subPath = File.separator + "modules" + subPath;
            } else {
                subPath = subPath.substring((File.separator + "netbeans").length());
            }
            nbm = nbm.replaceAll("^vw-rh", "visualweb-ravehelp-rave_nbpack");
            nbm = nbm.replaceAll("^vw-", "visualweb-");
            if (!nbm.startsWith("org") || !nbm.startsWith("com")) {
                nbm = "org-netbeans-modules-" + nbm;
            }
            cluster = cluster.replaceAll("^vw", "visualweb");
            File distFileDir = new File(distDir.getAbsolutePath(), cluster + subPath);
            mkdir.setDir(distFileDir);
            mkdir.execute();
            copy.setFile(new File(baseSrcDir, file));
            copy.setTodir(distFileDir);
            copy.execute();
            Vector<String> nbmFiles = nbms.get(nbm);
            if (nbmFiles == null) {
                nbmFiles = new Vector<String>();
                nbms.put(nbm, nbmFiles);
            }
            nbmFiles.add(subPath.substring(1) + File.separator + file);
        }
    }

    class ErrorCatcher implements ErrorHandler {

        public void error(SAXParseException e) {
            // normally a validity error
            pError = true;
        }

        public void warning(SAXParseException e) {
            //parseFailed = true;
        }

        public void fatalError(SAXParseException e) {
            pError = true;
        }
    }
}
