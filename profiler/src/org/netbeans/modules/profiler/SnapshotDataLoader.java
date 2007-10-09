/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler;

import org.openide.actions.*;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.UniFileLoader;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;


/**
 * Data loader which recognizes profiler snapshot files.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public class SnapshotDataLoader extends UniFileLoader {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String DISPLAY_NAME = NbBundle.getMessage(SnapshotDataLoader.class, "SnapshotDataLoader_DisplayName"); // NOI18N
                                                                                                                                // -----

    /**
     * Generated serial version UID.
     */
    static final long serialVersionUID = -8188437526246245429L;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates new image loader.
     */
    public SnapshotDataLoader() {
        // Set the representation class.
        super("org.netbeans.modules.profiler.SnapshotDataObject"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Create the image data object.
     *
     * @param primaryFile the primary file (e.g. <code>*.gif</code>)
     * @return the data object for this file
     * @throws DataObjectExistsException if the primary file already has a data object
     * @throws java.io.IOException       should not be thrown
     */
    protected MultiDataObject createMultiObject(FileObject primaryFile)
                                         throws DataObjectExistsException, java.io.IOException {
        return new SnapshotDataObject(primaryFile, this);
    }

    /**
     * Gets default system actions. Overrides superclass method.
     */
    protected SystemAction[] defaultActions() {
        return new SystemAction[] {
                   SystemAction.get(OpenAction.class), SystemAction.get(FileSystemAction.class), null,
                   SystemAction.get(CutAction.class), SystemAction.get(CopyAction.class), SystemAction.get(PasteAction.class),
                   null, SystemAction.get(DeleteAction.class), SystemAction.get(RenameAction.class), null,
                   SystemAction.get(ToolsAction.class), SystemAction.get(PropertiesAction.class)
               };
    }

    /**
     * Gets default display name. Overrides superclass method.
     */
    protected String defaultDisplayName() {
        return DISPLAY_NAME;
    }

    protected FileObject findPrimaryFile(FileObject fo) {
        if (fo.isFolder()) {
            return null;
        } else {
            String ext = fo.getExt();

            if (ext.equals(ResultsManager.SNAPSHOT_EXTENSION)) {
                return fo;
            } else {
                return null;
            }
        }
    }
}
