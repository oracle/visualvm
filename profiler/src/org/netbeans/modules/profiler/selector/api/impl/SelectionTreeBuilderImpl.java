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

package org.netbeans.modules.profiler.selector.api.impl;

import static org.netbeans.api.java.source.ClassIndex.*;
import org.netbeans.api.project.Project;
import org.netbeans.modules.profiler.selector.api.SelectionTreeBuilder;
import org.netbeans.modules.profiler.selector.api.SelectorChildren;
import org.netbeans.modules.profiler.selector.api.SelectorNode;
import org.netbeans.modules.profiler.selector.api.nodes.ContainerNode;
import org.netbeans.modules.profiler.selector.api.nodes.ProjectNode;
import org.netbeans.modules.profiler.selector.api.nodes.ProjectPackages;
import org.netbeans.modules.profiler.ui.Utils;
import org.openide.util.NbBundle;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author Jaroslav Bachorik
 */
public class SelectionTreeBuilderImpl implements SelectionTreeBuilder {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class Children extends SelectorChildren<ProjectNode> {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final boolean includeSubprojects;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Children(boolean includeSubprojects) {
            this.includeSubprojects = includeSubprojects;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected List<SelectorNode> prepareChildren(ProjectNode parent) {
            List<SelectorNode> nodes = new ArrayList<SelectorNode>(2);
            nodes.add(new SourcesNode(includeSubprojects, parent));
            nodes.add(new LibrariesNode(includeSubprojects, parent));

            return nodes;
        }
    }

    private class LibrariesNode extends ContainerNode {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final boolean includeSubprojects;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public LibrariesNode(final boolean includeSubprojects, final ContainerNode parent) {
            super(LIBRARIES_STRING, Utils.LIBRARIES_ICON, parent);
            this.includeSubprojects = includeSubprojects;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected SelectorChildren getChildren() {
            return new ProjectPackages(ProjectPackages.PackageType.Libraries, includeSubprojects);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Nodes & Children">
    private class SourcesNode extends ContainerNode {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final boolean includeSubprojects;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public SourcesNode(final boolean includeSubprojects, final ContainerNode parent) {
            super(SOURCES_STRING, Utils.PACKAGE_ICON, parent);
            this.includeSubprojects = includeSubprojects;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected SelectorChildren getChildren() {
            return new ProjectPackages(ProjectPackages.PackageType.Source, includeSubprojects);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SOURCES_STRING = NbBundle.getMessage(SelectionTreeBuilderImpl.class,
                                                                     "SelectionTreeBuilderImpl_SourcesString"); // NOI18N
    private static final String LIBRARIES_STRING = NbBundle.getMessage(SelectionTreeBuilderImpl.class,
                                                                       "SelectionTreeBuilderImpl_LibrariesString"); // NOI18N
                                                                                                                    // -----

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isDefault() {
        return true;
    }

    public String getDisplayName() {
        return NbBundle.getMessage(this.getClass(), "BuilderDisplayName"); // NOI18N
    }

    public String getID() {
        return "PACKAGE"; // NOI18N
    }

    public boolean isPreferred(Project project) {
        return false;
    }

    // </editor-fold>
    public List<SelectorNode> buildSelectionTree(Project project, final boolean includeSubprojects) {
        List<SelectorNode> roots = new ArrayList<SelectorNode>();

        ProjectNode projectRoot = new ProjectNode(project, includeSubprojects) {
            protected SelectorChildren getChildren() {
                return new Children(includeSubprojects);
            }
        };

        roots.add(projectRoot);

        return roots;
    }

    public boolean supports(Project project) {
        return true;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
