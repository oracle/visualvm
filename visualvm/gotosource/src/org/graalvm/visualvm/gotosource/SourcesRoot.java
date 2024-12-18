/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.gotosource;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.graalvm.visualvm.gotosource.impl.SourceRoots;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SourcesRoot {
    
    // Special mask for selecting possibly modularized (e.g. JDK) sources using a single source root
    public static final String MODULES_SUBPATH  = "*modules*";                  // NOI18N
    
    private static final String SUBPATHS_PREFIX = "[subpaths=";                  // NOI18N
    private static final String SUBPATHS_SUFFIX = "]";                           // NOI18N
    
    private static final String ENCODING_PREFIX = "[encoding=";                  // NOI18N
    private static final String ENCODING_SUFFIX = "]";                           // NOI18N
    
    
    private static final Logger LOGGER = Logger.getLogger(SourcesRoot.class.getName());
    
    
    private final String rootPath;
    private final String[] subPaths;
    private final Charset encoding;
    
    
    private SourcesRoot(String rootPath) {
        Object[] resolved = resolve(rootPath);
        this.rootPath = (String)resolved[0];
        this.subPaths = (String[])resolved[1];
        this.encoding = (Charset)resolved[2];
    }
    
    
    private SourcePathHandle getSourceHandle(String resourcePath) {
        Path root = Paths.get(rootPath);
        
        try {
            if (Files.isDirectory(root)) return getHandleInDirectory(root, resourcePath, subPaths, encoding);
            else if (Files.isRegularFile(root)) return getHandleInArchive(root, resourcePath, subPaths, encoding);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Failed resolving source file " + resourcePath + " in " + root, t); // NOI18N
        }
        
        return null;
    }
    
    private static SourcePathHandle getHandleInDirectory(Path directory, String sourcePath, String[] subPaths, Charset encoding) throws Throwable {
        if (subPaths == null) {
            Path sourceFile = directory.resolve(sourcePath);
            return isFile(sourceFile) ? new SourcePathHandle(sourceFile, false, encoding) : null;
        } else {
            if (subPaths.length == 1 && MODULES_SUBPATH.equals(subPaths[0])) {              
                List<Path> subfolders = Files.walk(directory, 1).filter(Files::isDirectory).collect(Collectors.toList());
                for (Path subfolder : subfolders) {
                    Path sourceFile = subfolder.resolve(sourcePath);
                    if (isFile(sourceFile)) return new SourcePathHandle(sourceFile, false, encoding);
                }
            } else for (String subPath : subPaths) {
                Path sourceFile = directory.resolve(subPath + "/" + sourcePath); // NOI18N
                if (isFile(sourceFile)) return new SourcePathHandle(sourceFile, false, encoding);
            }
            return null;
        }
    }
    
    private static SourcePathHandle getHandleInArchive(Path archive, String sourcePath, String[] subPaths, Charset encoding) throws Throwable {
        FileSystem archiveFileSystem = FileSystems.newFileSystem(archive, (ClassLoader)null);
        if (subPaths == null) {
            Path sourceFile = archiveFileSystem.getPath(sourcePath);
            return isFile(sourceFile) ? new SourcePathHandle(sourceFile, true, encoding) : null;
        } else {
            if (subPaths.length == 1 && MODULES_SUBPATH.equals(subPaths[0])) {              
                Path path = archiveFileSystem.getRootDirectories().iterator().next();
                List<Path> subfolders = Files.walk(path, 1).filter(Files::isDirectory).collect(Collectors.toList());
                for (Path subfolder : subfolders) {
                    Path sourceFile = subfolder.resolve(sourcePath);
                    if (isFile(sourceFile)) return new SourcePathHandle(sourceFile, true, encoding);
                }
            } else for (String subPath : subPaths) {
                Path sourceFile = archiveFileSystem.getPath(subPath, sourcePath);
                if (isFile(sourceFile)) return new SourcePathHandle(sourceFile, true, encoding);
            }
            return null;
        }
    }
    
    
    public boolean equals(Object o) { return o instanceof SourcesRoot ? rootPath.equals(((SourcesRoot)o).rootPath) : false; }
    
    public int hashCode() { return rootPath.hashCode(); }
    
    public String toString() { return rootPath; }
    
    
    public static SourcePathHandle getPathHandle(String resourcePath) {
        for (String rootPath : SourceRoots.getRoots()) {
            SourcesRoot root = new SourcesRoot(rootPath);
            SourcePathHandle handle = root.getSourceHandle(resourcePath);
            if (handle != null) return handle;
        }
        
        return null;
    }
    
    
    public static String createString(String rootPath, String[] subPaths, String encoding) {
        if ((subPaths == null || subPaths.length == 0) && encoding == null) return rootPath;
        
        StringBuilder sb = new StringBuilder();
        
        if (subPaths != null && subPaths.length > 0) {
            normalizeSubpaths(subPaths);
            
            for (String subPath : subPaths) {
                if (sb.length() > 0) sb.append(":");                            // NOI18N
                sb.append(subPath);
            }
            
            sb.insert(0, SUBPATHS_PREFIX);
            sb.append(SUBPATHS_SUFFIX);
        }
        
        if (StandardCharsets.UTF_8.name().equals(encoding)) encoding = null;
        if (encoding != null) sb.append(ENCODING_PREFIX).append(encoding).append(ENCODING_SUFFIX);
        
        sb.insert(0, rootPath);
        
        return sb.toString();
    }
    
    
    private static boolean isFile(Path path) {
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }
    
    
    private static Object[] resolve(String root) {
        int idx = root.indexOf('[');                                            // NOI18N
        String[] subpaths = null;
        Charset encoding = StandardCharsets.UTF_8;
        
        if (idx != -1) {
            String params = root.substring(idx);
            root = root.substring(0, idx);
            
            String[] paramsArr = params.split("\\]\\[");                        // NOI18N
            for (String paramS : paramsArr) {
                if (!paramS.startsWith("[")) paramS = "[" + paramS;             // NOI18N
                paramS = paramS.replace("]", "");                               // NOI18N
                
                if (paramS.startsWith(SUBPATHS_PREFIX)) {
                    paramS = paramS.substring(SUBPATHS_PREFIX.length());
                    subpaths = subpaths(paramS);
                } else if (paramS.startsWith(ENCODING_PREFIX)) {
                    paramS = paramS.substring(ENCODING_PREFIX.length());
                    encoding = charset(paramS);
                }
            }
        }
        
        return new Object[] { root, subpaths, encoding };
    }
    
    private static String[] subpaths(String subpaths) {
        if (subpaths.isEmpty()) return null;
        
        String[] paths = subpaths.split(":");                                   // NOI18N
        normalizeSubpaths(paths);
        
        return paths;
    }
    
    private static void normalizeSubpaths(String[] subpaths) {
        for (int i = 0; i < subpaths.length; i++) {
            String path = subpaths[i];
            
            if (!"/".equals(File.separator)) path = path.replace(File.separator, "/"); // NOI18N
            if (path.startsWith("/")) path = path.substring(1);                 // NOI18N
            if (path.endsWith("/")) path = path.substring(0, path.length() - 1); // NOI18N
            
            subpaths[i] = path;
        }
    }
    
    private static Charset charset(String charset) {
        try { return Charset.forName(charset); }
        catch (Exception e) { return StandardCharsets.UTF_8; }
    }
    
}
