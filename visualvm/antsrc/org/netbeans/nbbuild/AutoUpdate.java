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

package org.netbeans.nbbuild;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Get;
import org.apache.tools.ant.types.FileSet;
import org.netbeans.nbbuild.AutoUpdateCatalogParser.ModuleItem;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public class AutoUpdate extends Task {
    private List<Modules> modules = new ArrayList<Modules>();
    private FileSet nbmSet;
    private File dir;
    private File cluster;
    private URL catalog;
    private boolean force;

    public void setUpdateCenter(URL u) {
        catalog = u;
    }

    public FileSet createNbms() {
        if (nbmSet != null) {
            throw new BuildException("Just one nbms set allowed");
        }
        nbmSet = new FileSet();
        return nbmSet;
    }

    public void setInstallDir(File dir) {
        this.dir = dir;
    }

    public void setToDir(File dir) {
        this.cluster = dir;
    }

    /** Forces rewrite even the version of a module is not newer */
    public void setForce(boolean force) {
        this.force = force;
    }

    public Modules createModules() {
        final Modules m = new Modules();
        modules.add(m);
        return m;
    }

    @Override
    public void execute() throws BuildException {
        if ((dir != null) == (cluster != null)) {
            throw new BuildException("Specify either todir or installdir");
        }
        Map<String, ModuleItem> units;
        if (catalog != null) {
            try {
                units = AutoUpdateCatalogParser.getUpdateItems(catalog, catalog, this);
            } catch (IOException ex) {
                throw new BuildException(ex.getMessage(), ex);
            }
        } else {
            if (nbmSet == null) {
                throw new BuildException("Specify updatecenter or list of NBMs");
            }
            DirectoryScanner s = nbmSet.getDirectoryScanner(getProject());
            File basedir = s.getBasedir();
            units = new HashMap<String, ModuleItem>();
            for (String incl : s.getIncludedFiles()) {
                File nbm = new File(basedir, incl);
                try {
                    URL u = new URL("jar:" + nbm.toURI() + "!/Info/info.xml");
                    Map<String, ModuleItem> map;
                    final URL url = nbm.toURI().toURL();
                    map = AutoUpdateCatalogParser.getUpdateItems(u, url, this);
                    assert map.size() == 1;
                    Map.Entry<String,ModuleItem> entry = map.entrySet().iterator().next();
                    units.put(entry.getKey(), entry.getValue().changeDistribution(url));
                } catch (IOException ex) {
                    throw new BuildException(ex);
                }
            }
        }

        Map<String,List<String>> installed;
        if (dir != null) {
            File[] arr = dir.listFiles();
            if (arr == null) {
                throw new BuildException("installdir must be existing directory: " + dir);
            }
            installed = findExistingModules(arr);
        } else {
            installed = findExistingModules(cluster);
        }


        for (ModuleItem uu : units.values()) {
            if (!matches(uu.getCodeName(), uu.targetcluster)) {
                continue;
            }
            log("found module: " + uu, Project.MSG_VERBOSE);
            List<String> info = installed.get(uu.getCodeName());
            if (info != null && !uu.isNewerThan(info.get(0))) {
                log("Version " + info.get(0) + " of " + uu.getCodeName() + " is up to date", Project.MSG_VERBOSE);
                if (!force) {
                    continue;
                }
            }
            if (info == null) {
                log(uu.getCodeName() + " is not present, downloading version " + uu.getSpecVersion(), Project.MSG_INFO);
            } else {
                log("Version " + info.get(0) + " of " + uu.getCodeName() + " needs update to " + uu.getSpecVersion(), Project.MSG_INFO);
            }

            byte[] bytes = new byte[4096];
            File tmp = null;
            boolean delete = false;
            File lastM = null;
            try {
                if (uu.getURL().getProtocol().equals("file")) {
                    try {
                        tmp = new File(uu.getURL().toURI());
                    } catch (URISyntaxException ex) {
                        tmp = null;
                    }
                    if (!tmp.exists()) {
                        tmp = null;
                    }
                }
                final String dash = uu.getCodeName().replace('.', '-');
                if (tmp == null) {
                    tmp = File.createTempFile(dash, ".nbm");
                    tmp.deleteOnExit();
                    delete = true;
                    Get get = new Get();
                    get.setProject(getProject());
                    get.setTaskName("get:" + uu.getCodeName());
                    get.setSrc(uu.getURL());
                    get.setDest(tmp);
                    get.setVerbose(true);
                    get.execute();
                }

                File whereTo = dir != null ? new File(dir, uu.targetcluster) : cluster;
                whereTo.mkdirs();
                lastM = new File(whereTo, ".lastModified");
                lastM.createNewFile();

                if (info != null) {
                    for (int i = 1; i < info.size(); i++) {
                        File oldFile = new File(whereTo, info.get(i).replace('/', File.separatorChar));
                        oldFile.delete();
                    }
                }

                File tracking = new File(new File(whereTo, "update_tracking"), dash + ".xml");
                log("Writing tracking file " + tracking, Project.MSG_VERBOSE);
                tracking.getParentFile().mkdirs();
                OutputStream config = new BufferedOutputStream(new FileOutputStream(tracking));
                config.write(("<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<module codename='" + uu.getCodeName() + "'>\n").getBytes("UTF-8"));
                config.write(("  <module_version install_time='" + System.currentTimeMillis() + "' last='true' origin='Ant'" +
                        " specification_version='" + uu.getSpecVersion() + "'>\n").getBytes("UTF-8"));

                ZipFile  zf = new ZipFile(tmp);
                Enumeration<? extends ZipEntry> en = zf.entries();
                while (en.hasMoreElements()) {
                    ZipEntry zipEntry = en.nextElement();
                    if (!zipEntry.getName().startsWith("netbeans/")) {
                        continue;
                    }
                    if (zipEntry.getName().endsWith("/")) {
                        continue;
                    }
                    String relName = zipEntry.getName().substring(9);
                    File trgt = new File(whereTo, relName.replace('/', File.separatorChar));
                    trgt.getParentFile().mkdirs();
                    log("Writing " + trgt, Project.MSG_VERBOSE);

                    InputStream is = zf.getInputStream(zipEntry);
                    OutputStream os = new FileOutputStream(trgt);
                    boolean doUnpack200 = false;
                    if(relName.endsWith(".jar.pack.gz") && zf.getEntry(zipEntry.getName().substring(0, zipEntry.getName().length() - 8))==null) {
                        doUnpack200 = true;
                    }
                    CRC32 crc = new CRC32();
                    for (;;) {
                        int len = is.read(bytes);
                        if (len == -1) {
                            break;
                        }
                        if(!doUnpack200) {
                            crc.update(bytes, 0, len);
                        }
                        os.write(bytes, 0, len);
                    }
                    is.close();
                    os.close();
                    long crcValue = crc.getValue();
                    if(doUnpack200) {
                        File dest = new File(trgt.getParentFile(), trgt.getName().substring(0, trgt.getName().length() - 8));
                        log("Unpacking " + trgt + " to " + dest, Project.MSG_VERBOSE);
                        unpack200(trgt, dest);
                        trgt.delete();
                        crcValue = getFileCRC(dest);
                        relName = relName.substring(0, relName.length() - 8);
                    }
                    config.write(("    <file crc='" + crcValue + "' name='" + relName + "'/>\n").getBytes("UTF-8"));
                }
                config.write("  </module_version>\n</module>\n".getBytes("UTF-8"));
                config.close();
            } catch (IOException ex) {
                throw new BuildException(ex);
            } finally {
                if (delete && tmp != null) {
                    tmp.delete();
                }
                if (lastM != null) {
                    lastM.setLastModified(System.currentTimeMillis());
                }
            }
        }
    }

    
    public static boolean unpack200(File src, File dest) {
        // Copy of ModuleUpdater.unpack200        
        String unpack200Executable = new File(System.getProperty("java.home"),
                "bin/unpack200" + (isWindows() ? ".exe" : "")).getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder(unpack200Executable, src.getAbsolutePath(), dest.getAbsolutePath());
        pb.directory(src.getParentFile());
        int result = 1;
        try {
            //maybe reuse start() method here?
            Process process = pb.start();
            //TODO: Need to think of unpack200/lvprcsrv.exe issues
            //https://netbeans.org/bugzilla/show_bug.cgi?id=117334
            //https://netbeans.org/bugzilla/show_bug.cgi?id=119861
            result = process.waitFor();
            process.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result == 0;
    }
    private static boolean isWindows() {
        String os = System.getProperty("os.name"); // NOI18N
        return (os != null && os.toLowerCase().startsWith("windows"));//NOI18N
    }

    private static long getFileCRC(File file) throws IOException {
        BufferedInputStream bsrc = null;
        CRC32 crc = new CRC32();
        try {
            bsrc = new BufferedInputStream( new FileInputStream( file ) );
            byte[] bytes = new byte[1024];
            int i;
            while( (i = bsrc.read(bytes)) != -1 ) {
                crc.update(bytes, 0, i );
            }
        }
        finally {
            if ( bsrc != null )
                bsrc.close();
        }
        return crc.getValue();
    }

    private boolean matches(String cnb, String targetCluster) {
        for (Modules ps : modules) {
            if (ps.clusters != null) {
                if (targetCluster == null) {
                    continue;
                }
                if (!ps.clusters.matcher(targetCluster).matches()) {
                    continue;
                }
            }

            if (ps.pattern.matcher(cnb).matches()) {
                return true;
            }
        }
        return false;
    }

    private Map<String,List<String>> findExistingModules(File... clusters) {
        Map<String,List<String>> all = new HashMap<String, List<String>>();
        for (File c : clusters) {
            File mc = new File(c, "update_tracking");
            final File[] arr = mc.listFiles();
            if (arr == null) {
                continue;
            }
            for (File m : arr) {
                try {
                    parseVersion(m, all);
                } catch (Exception ex) {
                    log("Cannot parse " + m, ex, Project.MSG_WARN);
                }
            }
        }
        return all;
    }

    private void parseVersion(final File config, final Map<String,List<String>> toAdd) throws Exception {
        class P extends DefaultHandler {
            String name;
            List<String> arr;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if ("module".equals(qName)) {
                    name = attributes.getValue("codename");
                    int slash = name.indexOf('/');
                    if (slash > 0) {
                        name = name.substring(0, slash);
                    }
                    return;
                }
                if ("module_version".equals(qName)) {
                    String version = attributes.getValue("specification_version");
                    if (name == null || version == null) {
                        throw new BuildException("Cannot find version in " + config);
                    }
                    arr = new ArrayList<String>();
                    arr.add(version);
                    toAdd.put(name, arr);
                    return;
                }
                if ("file".equals(qName)) {
                    arr.add(attributes.getValue("name"));
                }
            }

            @Override
            public InputSource resolveEntity(String string, String string1) throws IOException, SAXException {
                return new InputSource(new ByteArrayInputStream(new byte[0]));
            }
        }
        P p = new P();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        parser.parse(config, p);
    }

    public static final class Modules {
        Pattern pattern;
        Pattern clusters;

        public void setIncludes(String regExp) {
            pattern = Pattern.compile(regExp);
        }

        public void setClusters(String regExp) {
            clusters = Pattern.compile(regExp);
        }
    }
}
