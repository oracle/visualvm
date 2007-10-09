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

import org.openide.actions.OpenAction;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.SystemAction;
import java.awt.*;
import javax.swing.*;


/**
 * Object that represents one snapshot file.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public class SnapshotDataObject extends MultiDataObject implements CookieSet.Factory, OpenCookie {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * Node representing <code>SnapshotDataObject</code>.
     */
    private static final class SnapshotNode extends DataNode {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Constructs snapshot node.
         */
        public SnapshotNode(SnapshotDataObject obj) {
            super(obj, Children.LEAF);

            int type = ResultsManager.getDefault().getSnapshotType(obj.getPrimaryFile());

            switch (type) {
                case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                    setIconBaseWithExtension(SNAPSHOT_ICON_BASE_CPU);

                    break;
                case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                    setIconBaseWithExtension(SNAPSHOT_ICON_BASE_FRAGMENT);

                    break;
                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                    setIconBaseWithExtension(SNAPSHOT_ICON_BASE_MEMORY);

                    break;
            }
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Image getIcon(int i) {
            return super.getIcon(i); //To change body of overridden methods use File | Settings | File Templates.
        }

        public Action getPreferredAction() {
            return SystemAction.get(OpenAction.class);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    /**
     * Generated serialized version UID.
     */
    static final long serialVersionUID = -6037463545669336965L;

    /**
     * Base for image resource.
     */
    private static final String SNAPSHOT_ICON_BASE_CPU = "org/netbeans/modules/profiler/resources/snapshotDataObjectCPU.png"; // NOI18N
    private static final String SNAPSHOT_ICON_BASE_MEMORY = "org/netbeans/modules/profiler/resources/snapshotDataObjectMemory.png"; // NOI18N
    private static final String SNAPSHOT_ICON_BASE_FRAGMENT = "org/netbeans/modules/profiler/resources/snapshotDataObjectFragment.png"; // NOI18N

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Constructor.
     *
     * @param pf     primary file object for this data object
     * @param loader the data loader creating it
     * @throws DataObjectExistsException if there was already a data object for it
     */
    public SnapshotDataObject(FileObject pf, MultiFileLoader loader)
                       throws DataObjectExistsException {
        super(pf, loader);

        getCookieSet().add(OpenCookie.class, this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Help context for this object.
     *
     * @return the help context
     */
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    /**
     * Implements <code>CookieSet.Factory</code> interface.
     */
    public Node.Cookie createCookie(Class clazz) {
        if (clazz.isAssignableFrom(OpenCookie.class)) {
            return this;
        } else {
            return null;
        }
    }

    public void open() {
        LoadedSnapshot[] loaded = ResultsManager.getDefault().loadSnapshots(new FileObject[] { getPrimaryFile() });
        ResultsManager.getDefault().openSnapshots(loaded);
    }

    /**
     * Create a node to represent the image. Overrides superclass method.
     *
     * @return node delegate
     */
    protected Node createNodeDelegate() {
        return new SnapshotNode(this);
    }
}
