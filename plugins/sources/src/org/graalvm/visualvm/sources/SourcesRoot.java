/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sources;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.graalvm.visualvm.sources.impl.SourceRoots;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SourcesRoot {
    
    private static final String ENCODING_PREFIX = "[encoding=";                 // NOI18N
    private static final String ENCODING_SUFFIX = "]";                          // NOI18N
    
    
    private static final Logger LOGGER = Logger.getLogger(SourcesRoot.class.getName());
    
    
    private final String rootPath;
    
    
    private SourcesRoot(String rootPath) {
        this.rootPath = rootPath;
    }
    
    
    private SourcePathHandle getSourceHandle(String resourcePath) {
        Object[] resourceWithEncoding = resolveEncoding(resourcePath);
        String resource = (String)resourceWithEncoding[0];
        Charset encoding = (Charset)resourceWithEncoding[1];
        
        Path root = Paths.get(rootPath);
        
        try {
            if (Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return getHandleInDirectory(root, resource, encoding);
            else if (Files.isRegularFile(root, LinkOption.NOFOLLOW_LINKS)) return getHandleInArchive(root, resource, encoding);
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "Failed resolving source file " + resource + " in " + root, t); // NOI18N
        }
        
        return null;
    }
    
    private static SourcePathHandle getHandleInDirectory(Path directory, String sourcePath, Charset encoding) throws Throwable {
        Path sourceFile = directory.resolve(sourcePath);
        return isFile(sourceFile) ? new SourcePathHandle(sourceFile, false, encoding) : null;
    }
    
    private static SourcePathHandle getHandleInArchive(Path archive, String sourcePath, Charset encoding) throws Throwable {
        FileSystem archiveFileSystem = FileSystems.newFileSystem(archive, null);
        Path sourceFile = archiveFileSystem.getPath(sourcePath);
        return isFile(sourceFile) ? new SourcePathHandle(sourceFile, true, encoding) : null;
    }
    
    
    public boolean equals(Object o) { return o instanceof SourcesRoot ? rootPath.equals(((SourcesRoot)o).rootPath) : false; }
    
    public int hashCode() { return rootPath.hashCode(); }
    
    public String toString() { return rootPath; }
    
    
    public static SourcePathHandle getPathHandle(String resourcePath) {
        for (String string : SourceRoots.getRoots()) {
            SourcesRoot root = new SourcesRoot(string);
            SourcePathHandle handle = root.getSourceHandle(resourcePath);
            if (handle != null) return handle;
        }
        
        return null;
    }
    
    
    private static boolean isFile(Path path) {
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }
    
    private static Object[] resolveEncoding(String resourcePath) {
        int idx = resourcePath.endsWith(ENCODING_SUFFIX) ? resourcePath.indexOf(ENCODING_PREFIX) : -1;
        if (idx == -1) return new Object[] { resourcePath, StandardCharsets.UTF_8 };
        
        String resource = resourcePath.substring(0, idx);
        String charset = resourcePath.substring(idx + ENCODING_PREFIX.length(), resourcePath.length() - ENCODING_SUFFIX.length());
        
        Charset encoding;
        try { encoding = Charset.forName(charset); }
        catch (Exception e) { encoding = StandardCharsets.UTF_8; }
        
        return new Object[] { resource, encoding };
    }
    
}
