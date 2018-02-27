/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api;

import org.netbeans.modules.profiler.spi.EditorSupportProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Decouples the profiler implementation from the underlying source code
 * editor. Provides support for finding out eg. the currently edited file,
 * the offset within that file etc.
 * 
 * @author Jiri Sedlacek
 * @author Jaroslav Bachorik
 */
final public class EditorSupport {
    
    private static EditorSupportProvider getSupport() {
        EditorSupportProvider support = Lookup.getDefault().lookup(EditorSupportProvider.class);
        return support != null ? support : EditorSupportProvider.NULL;
    }
    
    /**
     * Returns true if currently focused IDE component is Java editor.
     * 
     * @return true if currently focused IDE component is Java editor, false otherwise
     */
    public static boolean currentlyInJavaEditor() {
        return getSupport().currentlyInJavaEditor();
    }
    
    /**
     * Returns editor context of the most active Java editor.
     * 
     * @return editor context of the most active Java editor or null if not available
     */
    public static EditorContext getMostActiveJavaEditorContext() {
        return getSupport().getMostActiveJavaEditorContext();
    }

    /**
     * Returns the FileObject of the most active editor document
     * @return A FileObject or null
     */
    public static FileObject getCurrentFile() {
        return getSupport().getCurrentFile();
    }
    
    /**
     * Returns the caret position within the active editor document
     * converted into line number
     * @return The line number or -1
     */
    public static int getCurrentLine() {
        return getLineForOffset(getCurrentFile(), getCurrentOffset());
    }
    
    /**
     * Returns the caret position within the active editor document
     * @return The caret offset or -1
     */
    public static int getCurrentOffset() {
        return getSupport().getCurrentOffset();
    }

    /**
     * Validates the current offset
     * @return Returns TRUE if the current offset is valid within the bounds of the current file
     */
    public static boolean isCurrentOffsetValid() {
        return isOffsetValid(getCurrentFile(), getCurrentOffset());
    }
    
    /**
     * Validates an offset within a particular file
     * @param file The file to check
     * @param offset The offset within the file
     * @return Returns TRUE if the given offset is valid
     */
    public static boolean isOffsetValid(FileObject file, int offset) {
        return getSupport().isOffsetValid(file, offset);
    }
    
    /**
     * Calculates the line number for a given offset
     * @return Returns the line number within the active editor document or -1
     */
    public static int getLineForOffset(FileObject file, int offset) {
        return getSupport().getLineForOffset(file, offset);
    }
    
    /**
     * Calculates the offset for a given line number
     * @return Returns the offset for the provided file and line number or -1
     */
    public static int getOffsetForLine(FileObject file, int line) {
        return getSupport().getOffsetForLine(file, line);
    }
    
    /**
     * Returns the tuple of start/end selection offset in the currently activated editor
     * @return Tuple [startOffset, endOffset] or [-1, -1] if there is no selection
     */
    public static int[] getSelectionOffsets() {
        return getSupport().getSelectionOffsets();
    }

    /**
     * Returns the project the currently activated document belongs to
     * @return The most active project or null
     */
    public static Lookup.Provider getCurrentProject() {
        return getSupport().getCurrentProject();
    }
}
