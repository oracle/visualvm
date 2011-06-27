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
package org.netbeans.modules.profiler.spi;

import org.netbeans.modules.profiler.api.EditorContext;
import org.netbeans.modules.profiler.api.EditorSupport;
import org.netbeans.modules.profiler.api.ProfilerProject;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * An SPI interface for {@linkplain EditorSupport} functionality
 * @author Jaroslav Bachorik
 */
public interface EditorSupportProvider {
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
    boolean currentlyInJavaEditor();
    /**
     * Returns editor context of the most active Java editor.
     * 
     * @return editor context of the most active Java editor or null if not available
     */
    EditorContext getMostActiveJavaEditorContext();
    /**
     * Returns the FileObject of the most active editor document
     * @return A FileObject or null
     */
    FileObject getCurrentFile();
    /**
     * Returns the caret position within the active editor document
     * @return The caret offset or -1
     */
    int getCurrentOffset();
    /**
     * Validates an offset within a particular file
     * @param file The file to check
     * @param offset The offset within the file
     * @return Returns TRUE if the given offset is valid
     */
    boolean isOffsetValid(FileObject file, int offset);
    /**
     * Calculates the line number for a given offset
     * @return Returns the line number within the active editor document or -1
     */
    int getLineForOffset(FileObject file, int offset);
    /**
     * Returns the project the currently activated document belongs to
     * @return The most active project or null
     */
    Lookup.Provider getCurrentProject();
    /**
     * Returns the tuple of start/end selection offset in the currently activated editor
     * @return Tuple [startOffset, endOffset] or [-1, -1] if there is no selection
     */
    int[] getSelectionOffsets();
}
