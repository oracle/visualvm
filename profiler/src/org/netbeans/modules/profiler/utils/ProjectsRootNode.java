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

package org.netbeans.modules.profiler.utils;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * Root node for list of open projects
 *
 * @author Tomas Hurka
 * @author Petr Hrebejk
 */
public final class ProjectsRootNode extends AbstractNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static final class Handle implements Node.Handle {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final long serialVersionUID = 78374332058L;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Handle() {
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Node getNode() {
            return new ProjectsRootNode();
        }
    }

    private static final class ProjectChildren extends Children.Keys implements ChangeListener {
        //~ Inner Classes --------------------------------------------------------------------------------------------------------

        public static final class ProjectByDisplayNameComparator implements Comparator {
            //~ Static fields/initializers ---------------------------------------------------------------------------------------

            private static final Comparator COLLATOR = Collator.getInstance();

            //~ Methods ----------------------------------------------------------------------------------------------------------

            public final int compare(final Object o1, final Object o2) {
                if (!(o1 instanceof Project)) {
                    return 1;
                }

                if (!(o2 instanceof Project)) {
                    return -1;
                }

                final Project p1 = (Project) o1;
                final Project p2 = (Project) o2;

                return COLLATOR.compare(ProjectUtils.getInformation(p1).getDisplayName(),
                                        ProjectUtils.getInformation(p2).getDisplayName());
            }
        }

        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        public static final Comparator PROJECT_BY_DISPLAYNAME = new ProjectByDisplayNameComparator();

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final java.util.Map /*<Sources,Reference<Project>>*/ sources2projects = new WeakHashMap();

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProjectChildren() {
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        // Own methods ---------------------------------------------------------
        public Collection getKeys() {
            final List projects = Arrays.asList(OpenProjects.getDefault().getOpenProjects());
            Collections.sort(projects, PROJECT_BY_DISPLAYNAME);

            return projects;
        }

        // Children.Keys impl --------------------------------------------------
        @Override
        public void addNotify() {
            setKeys(getKeys());
        }

        @Override
        public void removeNotify() {
            for (Iterator it = sources2projects.keySet().iterator(); it.hasNext();) {
                final Sources sources = (Sources) it.next();
                sources.removeChangeListener(this);
            }

            sources2projects.clear();
            setKeys(Collections.EMPTY_LIST);
        }

        // Change listener impl ------------------------------------------------
        public void stateChanged(final ChangeEvent e) {
            final WeakReference projectRef = (WeakReference) sources2projects.get(e.getSource());

            if (projectRef == null) {
                return;
            }

            final Project project = (Project) projectRef.get();

            if (project == null) {
                return;
            }

            // Fix for 50259, callers sometimes hold locks
            rp.post(new Runnable() {
                    public void run() {
                        refreshKey(project);
                    }
                });
        }

        protected Node[] createNodes(final Object key) {
            final Project project = (Project) key;
            final LogicalViewProvider lvp = (LogicalViewProvider) project.getLookup().lookup(LogicalViewProvider.class);

            Node[] nodes;

            if (lvp == null) {
                ErrorManager.getDefault()
                            .log(ErrorManager.WARNING,
                                 "Warning - project " + ProjectUtils.getInformation(project).getName()
                                 + " failed to supply LogicalViewProvider in it's lookup"); // NOI18N

                final Sources sources = ProjectUtils.getSources(project);
                sources.removeChangeListener(this);
                sources.addChangeListener(this);
                nodes = new Node[] { Node.EMPTY };
            } else {
                nodes = new Node[] { lvp.createLogicalView() };

                if (nodes[0].getLookup().lookup(Project.class) != project) {
                    // Various actions, badging, etc. are not going to work.
                    ErrorManager.getDefault()
                                .log(ErrorManager.WARNING,
                                     "Warning - project " + ProjectUtils.getInformation(project).getName()
                                     + " failed to supply itself in the lookup of the root node of its own logical view"); // NOI18N
                }
            }

            return nodes;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String OPEN_PROJECTS_NODE_NAME = NbBundle.getMessage(ProjectsRootNode.class,
                                                                              "ProjectsRootNode_OpenProjectsNodeName"); // NOI18N
    private static final String ICON_BASE = "org/netbeans/modules/profiler/utils/projectsRootNode.gif"; //NOI18N
    private static final Action[] NO_ACTIONS = new Action[0];
    private static Action[] ACTIONS;
    private static final RequestProcessor rp = new RequestProcessor();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Node.Handle handle;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ProjectsRootNode() {
        super(new ProjectChildren());
        setIconBaseWithExtension(ICON_BASE);
        handle = new Handle();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public Action[] getActions(final boolean context) {
        if (context) {
            return NO_ACTIONS;
        } else {
            if (ACTIONS == null) {
                ACTIONS = new Action[0]; // no action sprovided
            }

            return ACTIONS;
        }
    }

    @Override
    public String getDisplayName() {
        return OPEN_PROJECTS_NODE_NAME;
    }

    @Override
    public Node.Handle getHandle() {
        return handle;
    }

    @Override
    public String getName() {
        return ("OpenProjects"); // NOI18N
    }

    @Override
    public boolean canRename() {
        return false;
    }

    /**
     * Finds node for given object in the view
     *
     * @return the node or null if the node was not found
     */
    Node findNode(final Object target) {
        final ProjectChildren ch = (ProjectChildren) getChildren();

        final Node[] nodes = ch.getNodes(true);

        for (int i = 0; i < nodes.length; i++) {
            final Project p = (Project) nodes[i].getLookup().lookup(Project.class);

            if (p == null) {
                continue;
            }

            final LogicalViewProvider lvp = (LogicalViewProvider) p.getLookup().lookup(LogicalViewProvider.class);

            if (lvp != null) {
                final Node selectedNode = lvp.findPath(nodes[i], target);

                if (selectedNode != null) {
                    return selectedNode;
                }
            }
        }

        return null;
    }
}
