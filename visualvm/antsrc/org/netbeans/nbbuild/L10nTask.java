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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.selectors.SelectorUtils;

/**
 * This task was created to create L10N kits.
 * The xml to call this task might look like:
 * <l10nTask nbmsdir="nbms" tmpdir="tmp" patternsFile="l10n.patterns" 
 *           kitFile="build/l10n.zip"/>
 *
 *
 * Resulting kitFile will contain the files according the patterns 
 * from patternsFile
 *
 * @author Michal Zlamal
 */
public class L10nTask extends Task {

    File nbmsDir = null;
    File tmpDir = null;
    File patternsFile = null;
    File kitFile = null;

    public void execute() throws BuildException {
        LineNumberReader lnr = null;
        try {
            if (nbmsDir == null) {
                throw new BuildException("Required variable not set.  Set 'nbmsdir' in the calling build script file");
            }
            if (!nbmsDir.exists() || !nbmsDir.isDirectory()) {
                throw new BuildException("'nbmsdir' has to exist and be directory where are all NBMs stored");
            }
            if (patternsFile == null) {
                throw new BuildException("Required variable not set.  Set 'patternsFile' in the calling build script file");
            }
            if (!patternsFile.exists() || !patternsFile.isFile()) {
                throw new BuildException("'patternsFile' has to exist and be file with patterns what should be included in the kit");
            }
            if (kitFile == null) {
                throw new BuildException("Required variable not set.  Set 'kitFile' in the calling build script file");
            }
            
            lnr = new LineNumberReader(new FileReader(patternsFile));
            String line = null;
            Map<String, Set<String>> includes = new HashMap<String, Set<String>>();
            Map<String, Set<String>> excludes = new HashMap<String, Set<String>>();
            Set<String> excludeFiles = new HashSet<String>();

            //Read all the patterns from patternsFile
            while ((line = lnr.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                if (line.startsWith("#")) continue;
                if (!line.startsWith("exclude ")) {  //Include pattern

                    String[] p = line.split(":");
                    if (p.length != 2) {
                        if (line.endsWith(":")) {
                            includes.put(line.substring(0, line.length() - 1), null);
                            continue;
                        } else {
                            throw new BuildException("Wrong pattern '" + line + "' found in pattern file: " + patternsFile.getAbsolutePath());
                        }
                    }
                    Set<String> files = includes.get(p[0]);
                    if (files == null) {
                        files = new HashSet<String>();
                        includes.put(p[0], files);
                    }
                    files.add(p[1]);
                } else {        //Exlude pattern

                    line = line.substring("exclude ".length());
                    String[] p = line.split(":");
                    if (p.length != 2) {
                        if (line.endsWith(":")) {
                            excludes.put(line.substring(0, line.length() - 1), null);
                            excludeFiles.add(line.substring(0, line.length() - 1));
                            continue;
                        } else {
                            throw new BuildException("Wrong pattern '" + line + "' found in pattern file: " + patternsFile.getAbsolutePath());
                        }
                    }
                    Set<String> files = excludes.get(p[0]);
                    if (files == null) {
                        files = new HashSet<String>();
                        excludes.put(p[0], files);
                    }
                    files.add(p[1]);
                }
            }
            lnr.close();

            //Unzip all the NBMs
            DirectoryScanner ds = new DirectoryScanner();
            ds.setBasedir(nbmsDir);
            ds.setIncludes(new String[]{"**/*.nbm"});
            ds.scan();
            String[] nbms = ds.getIncludedFiles();
            Expand unzip = (Expand) getProject().createTask("unzip");
            for (String nbm : nbms) {
                File nbmFile = new File(nbmsDir, nbm);
                File nbmDir = new File(tmpDir, nbm);
                nbmDir.mkdirs();
                unzip.setSrc(nbmFile);
                unzip.setDest(nbmDir);
                unzip.execute();

                DirectoryScanner packGzDs = new DirectoryScanner();
                final String suffix = ".jar.pack.gz";
                packGzDs.setBasedir(nbmDir);
                packGzDs.setIncludes(new String[]{"**/*" + suffix});
                packGzDs.scan();
                for(String packedJar : packGzDs.getIncludedFiles()) {
                    File packedJarFile = new File(nbmDir, packedJar);
                    File unpackedJarFile = new File(nbmDir, packedJar.substring(0, packedJar.length() - suffix.length()) + ".jar");
                    log("Unpacking " + packedJar + " to " + unpackedJarFile, Project.MSG_VERBOSE);
                    AutoUpdate.unpack200(packedJarFile, unpackedJarFile);
                    packedJarFile.delete();
                }
            }
            ds.setBasedir(tmpDir);
            String[] includesKeys = includes.keySet().toArray(new String[]{""});
            String[] excludesKeys = excludes.keySet().toArray(new String[]{""});
            if (includesKeys[0] != null) {
                ds.setIncludes(includesKeys);
            }
            if (excludeFiles.size() > 0) {
                ds.setExcludes(excludeFiles.toArray(new String[]{""}));
            }
            
            //Go though all the found files maching the first part of the pattern
            ds.scan();
            
            if (kitFile.exists()) {
                kitFile.delete();
            }
            
            Zip zip = (Zip) getProject().createTask("zip");
            zip.setDestFile(kitFile);
            for (String filePath : ds.getIncludedFiles()) {
                String file = filePath.replace("\\", "/");
                ZipFileSet zipFileSet = new ZipFileSet();
                boolean matching = false;
                for (String include : includesKeys) {
                    if (SelectorUtils.matchPath(include, file)) {
                        Set<String> incPattern = includes.get(include);
                        if (incPattern != null) {
                            matching = true;
                            zipFileSet.appendIncludes(incPattern.toArray(new String[]{""}));
                        } else {
                            FileSet fileSet = new FileSet();
                            fileSet.setDir(tmpDir);
                            fileSet.setIncludes(file);
                            zip.addFileset(fileSet);
                        }
                    }
                }
                if (matching) {
                    for (String exclude : excludesKeys) {
                        if (SelectorUtils.matchPath(exclude, file)) {
                            Set<String> excPattern = excludes.get(exclude);
                            if (excPattern != null) {
                                zipFileSet.appendExcludes(excPattern.toArray(new String[]{""}));
                            }
                        }
                    }
                    
                    File oneFile = new File(tmpDir, file);
                    zipFileSet.setSrc(oneFile);
                    file = file.replaceAll("^visualweb","vw");
                    file = file.replaceAll("visualweb-","vw-");
                    file = file.replaceAll("ravehelp-rave_nbpack","rh");
                    file = file.replaceAll("org-netbeans-modules-", "");
                    file = file.replaceAll("/netbeans/modules/", "/");
                    file = file.replaceAll("\\.nbm/", "/");
                    file = file.replaceAll("\\.jar", "");
                    zipFileSet.setPrefix(file);
                    zip.addZipfileset(zipFileSet);
                }
            }
            zip.execute();
            Delete delete = (Delete) getProject().createTask("delete");
            delete.setDir(tmpDir);
            delete.execute();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(L10nTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(L10nTask.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (lnr!=null) {
                    lnr.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(L10nTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void setNbmsdir(File nbmsDir) {
        this.nbmsDir = nbmsDir;
    }

    public void setTmpdir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    public void setPatternsFile(File patternsFile) {
        this.patternsFile = patternsFile;
    }
    public void setKitFile(File kitFile) {
        this.kitFile = kitFile;
    }    
}
