/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.spi.java;

import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class GoToSourceProvider {
    /**
     * Implementors will provide a specific functionality to open a source code
     * @param project The associated project
     * @param className The class name
     * @param methodName The method name or NULL
     * @param signature The signature or NULL
     * @param line The line number or {@linkplain Integer#MIN_VALUE}
     * @return Returns TRUE if the infrastructure was able to open the source code, FALSE otherwise
     */
    public abstract boolean openSource(Lookup.Provider project, String className, String methodName, String signature, int line);

    /**
     * Implementors will provide a specific functionality to open a source code file on a given position
     * @param srcFile The source file to be opened
     * @param offset The position to open the file at
     * @return  Returns TRUE if such file exists and the offset is valid
     */
    public abstract boolean openFile(FileObject srcFile, int offset);
}
