/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.graalvm.visualvm.lib.jfluid.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.graalvm.visualvm.lib.jfluid.global.Platform;


/**
 * Miscellaneous utilities for class names/path management, file management, and printing/logging.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class MiscUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NOT_DIRECTORY_MSG;
    private static final String NOT_FILE_MSG;
    private static final String FILE_NOT_READABLE_MSG;
    private static final String FILE_NOT_EXIST_MSG;
    private static final String VM_VERSION_MSG;
    private static final String VM_UNKNOWN_MSG;
    private static final String VM_INCOMPATIBLE_MSG;
    
    static {
        ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.utils.Bundle"); // NOI18N
        NOT_DIRECTORY_MSG = messages.getString("MiscUtils_NotDirectoryMsg"); // NOI18N
        NOT_FILE_MSG = messages.getString("MiscUtils_NotFileMsg"); // NOI18N
        FILE_NOT_READABLE_MSG = messages.getString("MiscUtils_FileNotReadableMsg"); // NOI18N
        FILE_NOT_EXIST_MSG = messages.getString("MiscUtils_FileNotExistMsg"); // NOI18N
        VM_VERSION_MSG = messages.getString("MiscUtils_VmVersionMsg"); // NOI18N
        VM_UNKNOWN_MSG = messages.getString("MiscUtils_VmUnknownMsg"); // NOI18N
        VM_INCOMPATIBLE_MSG = messages.getString("MiscUtils_VmIncompatibleMsg"); // NOI18N
    }

    // ------------------------------------------------------------------------------------------------
    //    Printing/logging management
    // ------------------------------------------------------------------------------------------------
    private static boolean verbosePrint = false;
    private static boolean printInfo = true;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String getAbsoluteFilePath(String fileName, String baseDir) {
        boolean local = false;

        // If the name is in the local form, convert it into an absolute form
        if (fileName.startsWith(".")) { // NOI18N
            local = true;
        } else {
            if (Platform.isWindows()) {
                if (!((fileName.charAt(0) == '\\') || (fileName.charAt(0) == '/') // NOI18N
                        || ((fileName.length() > 1) && (fileName.charAt(1) == ':')))) { // NOI18N
                    local = true;
                }
            } else {
                if (!((fileName.charAt(0) == '/') || (fileName.charAt(0) == '~'))) { // NOI18N
                    local = true;
                }
            }
        }

        if (local) {
            fileName = baseDir + "/" + fileName; // NOI18N
        }

        return fileName;
    }

    public static void getAllClassesInDir(String dirName, String packageName, boolean removeClassExt, Collection<String> res) {
        File dir = new File(dirName);
        String[] fileNames = dir.list();

        if (fileNames == null) {
            return;
        }

        for (int i = 0; i < fileNames.length; i++) {
            if (fileNames[i].endsWith(".class")) { // NOI18N

                String className = packageName
                                   + (removeClassExt ? fileNames[i].substring(0, fileNames[i].length() - 6) : fileNames[i]);
                res.add(className.intern());
            } else {
                String subDirName = dirName + File.separator + fileNames[i];
                File subDir = new File(subDirName);

                if (subDir.exists() && subDir.isDirectory()) {
                    String subPackage = packageName + fileNames[i] + "/"; // NOI18N
                    getAllClassesInDir(subDirName, subPackage, removeClassExt, res);
                }
            }
        }
    }

    public static void getAllClassesInJar(String jarName, boolean removeClassExt, Collection res) {
        ZipFile zip = null;

        try {
            zip = new ZipFile(jarName);
            for (Enumeration entries = zip.entries(); entries.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String className = entry.getName();

                if (className.endsWith(".class")) { // NOI18N

                    if (removeClassExt) {
                        className = className.substring(0, className.length() - 6);
                    }

                    res.add(className.intern());
                }
            }
            zip.close();
        } catch (Exception ex) {
            System.err.println("Warning: could not open archive " + jarName); // NOI18N

            return;
        }
    }

    // ------------------------------------------------------------------------------------------------
    //    File management
    // ------------------------------------------------------------------------------------------------
    public static String getCanonicalPath(File file) {
        try {
            if (!file.exists()) {
                return null;
            }

            return file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    // -----

    // ------------------------------------------------------------------------------------------------
    //   Class name and class path management
    // ------------------------------------------------------------------------------------------------

    /** Determine the outermost class name for the given source file, based on the available source path
     * @param sourceFileName the name of the source file
     * @param mainSourcePath the main source path to search within
     * @param secondarySourcePath the secondary source path to search within
     */
    public static String getClassNameForSource(String sourceFileName, String mainSourcePath, String secondarySourcePath,
                                               String workingDir) {
        if ((sourceFileName.charAt(1) == ':') && !Character.isLowerCase(sourceFileName.charAt(0))) { // NOI18N
            sourceFileName = sourceFileName.substring(0, 1).toLowerCase() + sourceFileName.substring(1);
        }

        for (int i = 0; i < 2; i++) {
            String sourcePath = ((i == 0) ? mainSourcePath : secondarySourcePath);

            if (sourcePath == null) {
                continue;
            }

            List<String> paths = getPathComponents(sourcePath, true, workingDir);

            for (int j = 0; j < paths.size(); j++) {
                String path = (String) paths.get(j);

                if ((path.charAt(1) == ':') && !Character.isLowerCase(path.charAt(0))) { // NOI18N
                    path = path.substring(0, 1).toLowerCase() + path.substring(1);
                }

                if (!path.endsWith(File.separator)) {
                    path += File.separator;
                }

                if (sourceFileName.startsWith(path)) {
                    String className = sourceFileName.substring(path.length(), sourceFileName.length() - 5);

                    return className.replace(File.separatorChar, '.'); // NOI18N
                }
            }
        }

        return null;
    }

    public static String getFirstPathComponent(String path) {
        int pos = path.indexOf(File.pathSeparatorChar);

        if (pos == -1) {
            return path;
        } else {
            return path.substring(0, pos);
        }
    }

    // ------------------------------------------------------------------------------------------------
    //    JDK version determination for a given executable file
    // ------------------------------------------------------------------------------------------------
    public static String getJDKVersionForJVMExeFile(File exeFile)
                                             throws IOException {
        String[] cmdLine = new String[] { exeFile.getAbsolutePath(), "-version" }; // NOI18N
        Process javaProcess = Runtime.getRuntime().exec(cmdLine);

        //javaProcess.waitFor(); // this should probably be here
        InputStream bis = javaProcess.getErrorStream();

        int maxLen = 500;
        int availBytes;
        int readBytes;
        int ofs = 0;
        byte[] bytes = new byte[maxLen];

        do {
            availBytes = bis.available();

            if (availBytes == 0) {
                availBytes = 1;
            }

            if ((ofs + availBytes) >= maxLen) {
                availBytes = maxLen - ofs;
            }

            readBytes = bis.read(bytes, ofs, availBytes);

            if (readBytes != -1) {
                ofs += readBytes;
            }
        } while ((readBytes != -1) && (ofs < maxLen));

        bis.close();

        String outString = new String(bytes, 0, ofs);
        String printOutString = "\n" + VM_VERSION_MSG + "\n" + outString; // NOI18N

        // The string should start with something like 'java version "1.5.0"' (note the quotes). Let's remove the stuff before the quote
        int pos = outString.indexOf('\"'); // NOI18N

        if (pos == -1) {
            throw new IOException(VM_UNKNOWN_MSG + printOutString);
        }

        outString = outString.substring(pos + 1);

        return Platform.getJDKVersionString(outString);
    }

    /** For a string representing a class path, remove all entries that don't correspond to existing files, and return the remaining ones. */
    public static String getLiveClassPathSubset(String path, String workingDir) {
        List<String> liveComponents = getPathComponents(path, true, workingDir);
        StringBuilder buf = new StringBuilder(liveComponents.size() * 10);

        if (liveComponents.size() > 0) {
            buf.append((String) liveComponents.get(0));

            for (int i = 1; i < liveComponents.size(); i++) {
                buf.append(File.pathSeparator);
                buf.append((String) liveComponents.get(i));
            }
        }

        return buf.toString();
    }

    /**
     * Returns the components of the compound path, such as CLASSPATH. If doCheck is true,
     * checks if each of the components really exists, i.e. is an existing directory or file,
     * and returns only existing components. workingDir is needed in case the passed path has
     * a local form.
     */
    public static List<String> getPathComponents(String path, boolean doCheck, String workingDir) {
        ArrayList<String> list = new ArrayList<>();

        if (path != null) {
            StringTokenizer tok = new StringTokenizer(path, File.pathSeparator);

            while (tok.hasMoreTokens()) {
                String name = tok.nextToken();
                boolean addedToList = false;
                
                if ((name == null) || (name.length() == 0)) {
                    continue; // Essentially sanity check, but who knows?
                }

                if (doCheck) {
                    name = getAbsoluteFilePath(name, workingDir);
                    name = getCanonicalPath(new File(name)); // clean up the name into a canonical path
                    if (name != null && !list.contains(name)) {
                        list.add(name);
                        addedToList = true;
                    }
                } else {
                    list.add(name);
                    addedToList = true;
                }
                if (addedToList) {
                    try {
                        getClassPathFromManifest(name,list);
                    } catch (URISyntaxException ex) {
                        System.out.println("Error processing "+name);   // NOI18N
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        System.out.println("Error processing "+name);   // NOI18N
                        ex.printStackTrace();
                    }
                }
            }
        }
        return list;
    }

    private static void getClassPathFromManifest(String jarPath,List pathList) throws IOException, URISyntaxException {
        if (jarPath.toLowerCase(Locale.ENGLISH).endsWith(".jar")) {   // NOI18N
            File pathFile = new File(jarPath);
            JarFile jarFile = new JarFile(pathFile);
            Manifest manifest = jarFile.getManifest();
            
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();

                if (attrs != null) {
                    String jarCp = attrs.getValue(Attributes.Name.CLASS_PATH);

                    if (jarCp != null) {
                        File parent = pathFile.getParentFile();
                        StringTokenizer tokens = new StringTokenizer(jarCp);
                        
                        while(tokens.hasMoreTokens()) {
                            URI fileUri = new URI(tokens.nextToken());
                            File cpFile;
                            String cpName;
                            
                            if (!fileUri.isAbsolute()) {
                                cpFile = new File(parent,fileUri.getPath());
                            } else {
                                cpFile = new File(fileUri);
                            }
                            cpName = getCanonicalPath(cpFile);
                            if (cpName != null && !pathList.contains(cpName)) {
                                pathList.add(cpName);
                                getClassPathFromManifest(cpName,pathList);
                            }
                        }
                    }
                }
            }
            jarFile.close();
        }
    }
    
    
    private static boolean addToList(File file,List path) {
        String pathName = getCanonicalPath(file);
        
        if (pathName != null && !path.contains(pathName)) {
            path.add(pathName);
            return true;
        }
        return false;
    }
    
    public static void setSilent(boolean silent) {
        printInfo = !silent;
    }

    public static boolean isSlashedJavaCoreClassName(String name) {
        return (name.startsWith("java/") || name.startsWith("sun/") || name.startsWith("javax/")); // NOI18N
    }

    public static boolean isSupportedJVM(Map jdkProperties) {
        String jdkVersionString = (String) jdkProperties.get("java.version"); // NOI18N
        String vmNameString = (String) jdkProperties.get("java.vm.name"); // NOI18N

        if (jdkVersionString == null || vmNameString == null) { // probably not a platform for JDK
            return false;
        }

        if (isSupportedJDK(jdkVersionString)) {
            return true;
        }
        // CVM is recognized via java.vm.name system property
        return isSupportedJDK(vmNameString);
    }

    // This method is used for checking running JVM if supported.
    // jvmVersionString should be enough to decide that
    public static boolean isSupportedRunningJVMVersion(String jdkVersionString) {
        return isSupportedJDK(jdkVersionString);
    }

    public static void setVerbosePrint() {
        verbosePrint = true;
    }

    public static File checkDirForName(String name) throws IOException {
        File file = new File(name);

        return checkFile(file, true);
    }

    public static File checkFile(File file, boolean isDir)
                          throws IOException {
        if (file.exists()) {
            if (isDir) {
                if (!file.isDirectory()) {
                    throw new IOException(MessageFormat.format(NOT_DIRECTORY_MSG, new Object[] { file }));
                }
            } else {
                if (!file.isFile()) {
                    throw new IOException(MessageFormat.format(NOT_FILE_MSG, new Object[] { file }));
                }
            }

            if (!file.canRead()) {
                throw new IOException(MessageFormat.format(FILE_NOT_READABLE_MSG, new Object[] { file }));
            }

            return file;
        } else {
            throw new IOException(FILE_NOT_EXIST_MSG);
        }
    }

    public static File checkFileForName(String name) throws IOException {
        File file = new File(name);

        return checkFile(file, false);
    }

    /** Checks if given directory is already listed on path */
    public static boolean containsDirectoryOnPath(String directory, String path) {
        String normalizedDirectory = new File(directory).getAbsolutePath().toLowerCase(Locale.ENGLISH);
        String normalizedPath = new File(path).getAbsolutePath().toLowerCase(Locale.ENGLISH);
        List<String> pathComponents = getPathComponents(normalizedPath, false, null);

        for (int i = 0; i < pathComponents.size(); i++) {
            if (normalizedDirectory.equals(pathComponents.get(i))) {
                return true;
            }
        }

        return false;
    }

    public static void deleteHeapTempFiles() {
        if (Platform.isWindows()) { // this is workaround for JDK bug #6359560

            try {
                File tempDir = new File(System.getProperty("java.io.tmpdir")); // NOI18N
                DirectoryStream<Path> files = Files.newDirectoryStream(tempDir.toPath());

                try {
                    for (Path p : files) {
                        String fname = p.toFile().getName();

                        if (fname.startsWith("NBProfiler") && (fname.endsWith(".map") || fname.endsWith(".ref") || fname.endsWith(".gc"))) { // NOI18N
                            Files.delete(p);
                        }
                    }
                } finally {
                    files.close();
                }
            } catch (IOException ex) {
                System.err.println("deleteHeapTempFiles failed");   // NOI18N
                ex.printStackTrace();
            }
        }
    }

    public static boolean fileForNameOk(String name) {
        try {
            checkFileForName(name);

            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean inSamePackage(String className1, String className2) {
        int ldi1 = className1.lastIndexOf('.'); // NOI18N
        int ldi2 = className2.lastIndexOf('.'); // NOI18N

        if (ldi1 != ldi2) {
            return false;
        }

        if (ldi1 == -1) {
            return true;
        }

        return (className1.substring(0, ldi1).equals(className2.substring(0, ldi2)));
    }

    /**
     * Method to handle internal error condition.
     *
     * @param message The message describing the error
     */
    public static void internalError(String message) {
        throw new InternalError(message);
    }

    public static void printErrorMessage(String message) {
        System.err.println("*** Profiler error (" + getDate() + "): " + message); // NOI18N
    }

    public static void printInfoMessage(String message) {
        if (printInfo) {
            System.err.println("*** Profiler message (" + getDate() + "): " + message); // NOI18N
        }
    }

    public static void printVerboseInfoMessage(String message) {
        if (verbosePrint) {
            System.err.println("Profiler Engine: " + message); // NOI18N
        }
    }

    public static void printVerboseInfoMessage(String[] elements) {
        if (!verbosePrint) {
            return;
        }

        int i;

        for (i = 0; i < (elements.length - 1); i++) {
            System.err.print(elements[i]);
            System.err.print(" "); // NOI18N
        }

        System.err.println(elements[i]);
    }

    public static void printWarningMessage(String message) {
        System.err.println("*** Profiler warning (" + getDate() + "): " + message); // NOI18N
    }

    public static byte[] readFileIntoBuffer(FileOrZipEntry fileOrZip)
                                     throws IOException {
        if (fileOrZip.isFile()) {
            checkFile(fileOrZip.getFile(), false);
        }

        InputStream in = fileOrZip.getInputStream();
        int len = (int) fileOrZip.getLength();
        byte[] buf = new byte[len];
        int readBytes;
        int ofs = 0;
        int remBytes = len;

        do {
            readBytes = in.read(buf, ofs, remBytes);
            ofs += readBytes;
            remBytes -= readBytes;
        } while (ofs < len);

        in.close();

        return buf;
    }

    private static String getDate() {
        return (new Date()).toString();
    }

    private static boolean isSupportedJDK(String jdkVersionString) {
        int jdkVersionNumber = Platform.getJDKVersionNumber(jdkVersionString);
        if (jdkVersionNumber == Platform.JDK_15) {
            if (jdkVersionString.equals("1.5.0") || jdkVersionString.startsWith("1.5.0_01") ||  // NOI18N
                jdkVersionString.startsWith("1.5.0_02") || jdkVersionString.startsWith("1.5.0_03")) { // NOI18N
                return false;
            } else {
                return true;
            }
        }
        return jdkVersionNumber != Platform.JDK_UNSUPPORTED;
    }
}
