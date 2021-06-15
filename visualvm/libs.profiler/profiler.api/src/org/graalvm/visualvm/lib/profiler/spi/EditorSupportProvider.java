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
package org.graalvm.visualvm.lib.profiler.spi;

import org.graalvm.visualvm.lib.profiler.api.EditorContext;
import org.graalvm.visualvm.lib.profiler.api.EditorSupport;
import org.graalvm.visualvm.lib.profiler.api.ProfilerProject;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * An SPI interface for {@linkplain EditorSupport} functionality
 * @author Jaroslav Bachorik
 */
public abstract class EditorSupportProvider {
    public static EditorSupportProvider NULL = new EditorSupportProvider() {

        @Override
        public boolean currentlyInJavaEditor() {
            return false;
        }

        @Override
        public EditorContext getMostActiveJavaEditorContext() {
            return null;
        }

        @Override
        public FileObject getCurrentFile() {
            return null;
        }

        @Override
        public int getCurrentOffset() {
            return -1;
        }

        @Override
        public boolean isOffsetValid(FileObject file, int offset) {
            return false;
        }

        @Override
        public int getLineForOffset(FileObject file, int offset) {
            return -1;
        }

        @Override
        public int getOffsetForLine(FileObject file, int line) {
            return -1;
        }

        @Override
        public ProfilerProject getCurrentProject() {
            return null;
        }

        @Override
        public int[] getSelectionOffsets() {
            return new int[]{-1, -1};
        }
    };

    /**
     * Returns true if currently focused IDE component is Java editor.
     *
     * @return true if currently focused IDE component is Java editor, false otherwise
     */
    public abstract boolean currentlyInJavaEditor();
    /**
     * Returns editor context of the most active Java editor.
     *
     * @return editor context of the most active Java editor or null if not available
     */
    public abstract EditorContext getMostActiveJavaEditorContext();
    /**
     * Returns the FileObject of the most active editor document
     * @return A FileObject or null
     */
    public abstract FileObject getCurrentFile();
    /**
     * Returns the caret position within the active editor document
     * @return The caret offset or -1
     */
    public abstract int getCurrentOffset();
    /**
     * Validates an offset within a particular file
     * @param file The file to check
     * @param offset The offset within the file
     * @return Returns TRUE if the given offset is valid
     */
    public abstract boolean isOffsetValid(FileObject file, int offset);
    /**
     * Calculates the line number for a given offset
     * @return Returns the line number within the active editor document or -1
     */
    public abstract int getLineForOffset(FileObject file, int offset);
    /**
     * Calculates the offset for a given line number
     * @return Returns the offset for the provided file and line number or -1
     */
    public abstract int getOffsetForLine(FileObject file, int line);
    /**
     * Returns the project the currently activated document belongs to
     * @return The most active project or null
     */
    public abstract Lookup.Provider getCurrentProject();
    /**
     * Returns the tuple of start/end selection offset in the currently activated editor
     * @return Tuple [startOffset, endOffset] or [-1, -1] if there is no selection
     */
    public abstract int[] getSelectionOffsets();
}
