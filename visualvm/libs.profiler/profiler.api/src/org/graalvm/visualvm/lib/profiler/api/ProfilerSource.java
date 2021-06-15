/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.api;

import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * IDE agnostic source file wrapper<br/>
 * A simple {@linkplain Lookup.Provider} derived from the appropriate {@linkplain MimeLookup} registrations.
 * @author Jaroslav Bachorik
 */
abstract public class ProfilerSource implements Lookup.Provider {
    private FileObject file;

    protected ProfilerSource(FileObject file) {
        this.file = file;
    }

    /**
     * The wrapped file
     * @return Returns the {@linkplain FileObject|} representing a particular {@linkplain ProfilerSource}
     */
    final public FileObject getFile() {
        return file;
    }

    @Override
    final public Lookup getLookup() {
        return MimeLookup.getLookup(file.getMIMEType());
    }

    /**
     * Indicates whether a source can be run by the IDE or not
     * @return Returns <b>TRUE</b> if the source can be run by the IDE (eg. main class, test etc.), <b>FALSE</b> otherwise
     */
    abstract public boolean isRunnable();
}
