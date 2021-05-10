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
package org.graalvm.visualvm.gotosource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.core.datasource.Storage;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SourcePathHandle {
        
    private static final String EXTRACTED_DIR = "extracted_sources";            // NOI18N
    
    private static final Logger LOGGER = Logger.getLogger(SourcesRoot.class.getName());


    private final Path path;
    private final boolean archive;
    private final Charset encoding;

    private Path regularPath;


    SourcePathHandle(Path path, boolean archive, Charset encoding) {
        this.path = path;
        this.archive = archive;
        this.encoding = encoding;
    }


    public Path getPath() {
        return path;
    }

    public Path getRegularPath() {
        if (archive) {
            if (regularPath == null) try {
                regularPath = extractArchivePath(path);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Failed extracting archived path " + path, e); // NOI18N
                regularPath = path;
            }
            return regularPath;
        } else {
            return path;
        }
    }

    public String readText() {
        try {
            return new String(Files.readAllBytes(path), encoding);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Failed resolving source text " + path.toAbsolutePath().toString(), ex); // NOI18N
            return null;
        }
    }

    public void close() {            
        if (archive) try {
            path.getFileSystem().close();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Failed closing filesystem for " + path, e); // NOI18N
        }
    }


    private static Path extractArchivePath(Path archive) throws Exception {
        Path extracted = Paths.get(Storage.getTemporaryStorageDirectoryString(), EXTRACTED_DIR, archive.toString());
        Files.createDirectories(extracted.getParent());
        Files.copy(archive, extracted, LinkOption.NOFOLLOW_LINKS, StandardCopyOption.REPLACE_EXISTING);
        return extracted;
    }

}
