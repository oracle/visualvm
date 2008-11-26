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

package org.netbeans.modules.profiler.actions;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.profiler.ui.NBSwingWorker;
import org.netbeans.modules.profiler.ui.panels.ProgressDisplayer;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.netbeans.modules.profiler.utils.OutputParameter;
import org.netbeans.spi.project.ActionProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;


/**
 * Action sensitive to current project
 *
 * @author Ian Formanek
 */
public class ProjectSensitiveAction extends AbstractAction implements ContextAwareAction, LookupListener {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public interface ProfilerProjectActionPerformer {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * Called when the context of the action changes and the action should
         * be enabled or disabled within the new context, according to the newly
         * selected project.
         *
         * @param project the currently selected project, or null if no project is selected
         * @return true to enable the action, false to disable it
         */
        public boolean enable(Project project, Lookup context, boolean lightweightOnly);

        /**
         * Called when the user invokes the action.
         *
         * @param project the project this action was invoked for (XXX can this be null or not?)
         * @throws IllegalStateException when trying to perform the action in an illegal context
         */
        public void perform(Project project, Lookup context);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static final class ActionsUtil {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * In given lookup will find all FileObjects owned by given project
         * with given command supported.
         */
        public static FileObject[] getFilesFromLookup(final Lookup lookup, final Project project) {
            final HashSet result = new HashSet();
            final Collection dataObjects = lookup.lookup(new Lookup.Template(DataObject.class)).allInstances();

            for (Iterator it = dataObjects.iterator(); it.hasNext();) {
                final DataObject dObj = (DataObject) it.next();
                final FileObject fObj = dObj.getPrimaryFile();
                final Project p = FileOwnerQuery.getOwner(fObj);

                if ((p != null) && p.equals(project)) {
                    result.add(fObj);
                }
            }

            final FileObject[] fos = new FileObject[result.size()];
            result.toArray(fos);

            return fos;
        }

        /**
         * Finds all projects in given lookup. If the command is not null it will check
         * whether given command is enabled on all projects. If and only if all projects
         * have the command supported it will return array including the project. If there
         * is one project with the command disabled it will return empty array.
         */
        public static Project[] getProjectsFromLookup(final Lookup lookup, final String command) {
            final Set result = new HashSet();

            // First find out whether there is a project directly in the Lookup
            final Collection projects = lookup.lookup(new Lookup.Template(Project.class)).allInstances();

            for (Iterator it = projects.iterator(); it.hasNext();) {
                final Project p = (Project) it.next();
                result.add(p);
            }

            // Now try to guess the project from dataobjects
            final Collection dataObjects = lookup.lookup(new Lookup.Template(DataObject.class)).allInstances();

            for (Iterator it = dataObjects.iterator(); it.hasNext();) {
                final DataObject dObj = (DataObject) it.next();
                final FileObject fObj = dObj.getPrimaryFile();
                final Project p = FileOwnerQuery.getOwner(fObj);

                if (p != null) {
                    result.add(p);
                }
            }

            final Project[] projectsArray = new Project[result.size()];
            result.toArray(projectsArray);

            if (command != null) {
                // All projects have to have the command enabled
                for (int i = 0; i < projectsArray.length; i++) {
                    if (!commandSupported(projectsArray[i], command, lookup)) {
                        return new Project[0];
                    }
                }
            }

            return projectsArray;
        }

        /**
         * Tests whether given command is available on the project and whether
         * the action as to be enabled in current Context
         *
         * @param project Project to test
         * @param command Command for test
         * @param context Lookup representing current context or null if context
         *                does not matter.
         */
        public static boolean commandSupported(final Project project, final String command, final Lookup context) {
            //We have to look whether the command is supported by the project
            final ActionProvider ap = (ActionProvider) project.getLookup().lookup(ActionProvider.class);

            if (ap != null) {
                final List commands = Arrays.asList(ap.getSupportedActions());

                if (commands.contains(command)) {
                    if ((context == null) || ap.isActionEnabled(command, context)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Good for formating names of actions with some two parameter pattern
         * {0} nuber of objects (e.g. Projects or files ) and {1} name of one
         * or first object (e.g. Project or file) or null if the number is == 0
         */
        public static String formatName(final String namePattern, final int numberOfObjects, final String firstObjectName) {
            return MessageFormat.format(namePattern,
                                        new Object[] {
                                            new Integer(numberOfObjects), (firstObjectName == null) ? "" : firstObjectName, //NOI18N
                                        });
        }

        public static String formatProjectSensitiveName(final String namePattern, final Project[] projects) {
            // Set the action's name
            if ((projects == null) || (projects.length == 0)) {
                // No project selected
                return ActionsUtil.formatName(namePattern, 0, null);
            } else {
                // Some project selected
                return ActionsUtil.formatName(namePattern, projects.length,
                                              ProjectUtils.getInformation(projects[0]).getDisplayName());
            }
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected volatile boolean actionEnabled = false;
    private final Lookup lookup;
    private final Object initLock = new Object();
    private final Object refreshRequestLock = new Object();
    private final ProfilerProjectActionPerformer performer;
    private final String namePattern;
    private final Lookup.Result[] results;
    private final Class[] watch;

    // @GuarderBy initLock
    private volatile boolean isCalculated = false;

    // @GuardedBy refreshRequestLock
    private int refreshRequested = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Constructor for global actions. E.g. actions in main menu which
     * listen to the global context.
     */
    ProjectSensitiveAction(final ProfilerProjectActionPerformer performer, final String name, final String namePattern,
                           final Icon icon, Lookup lookup) {
        super(name);

        if (icon != null) {
            putValue(SMALL_ICON, icon);
        }

        if (lookup == null) {
            lookup = Utilities.actionsGlobalContext();
        }

        this.lookup = lookup;
        this.watch = new Class[] { Project.class, DataObject.class };
        this.results = new Lookup.Result[watch.length];

        // Needs to listen on changes in results
        for (int i = 0; i < watch.length; i++) {
            final Lookup.Result result = lookup.lookup(new Lookup.Template(watch[i]));
            // #147348 - Action instance is probobly only weakly held; we must add the strong reference to lookup listener
//            resultListeners[i] = (LookupListener) WeakListeners.create(LookupListener.class, this, result);
//            result.addLookupListener(resultListeners[i]);
            result.addLookupListener(this);
            // MUST hold on the reference to the result; otherwise it will vanish in a puff of smoke
            results[i] = result;
        }

        this.performer = performer;
        this.namePattern = namePattern;

        init();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Needs to override isEnabled in order to force refresh
     */
    public final boolean isEnabled() {
        init(); // the action must be initialized

        return super.isEnabled();
    }

    public final void actionPerformed(final ActionEvent e) {
        actionPerformed(lookup);
    }

    public static ProjectSensitiveAction projectSensitiveAction(final ProfilerProjectActionPerformer performer,
                                                                final String name, final String namePattern, final Icon icon) {
        return new ProjectSensitiveAction(performer, name, namePattern, icon, null);
    }

    // Implementation of ContextAwareAction ------------------------------------
    public Action createContextAwareInstance(final Lookup actionContext) {
        return new ProjectSensitiveAction(getPerformer(), getName(), getNamePattern(), (Icon) getValue(SMALL_ICON), actionContext);
    }

    // Implementation of LookupListener ----------------------------------------
    public final void resultChanged(final LookupEvent e) {
        isCalculated = false;
    }

    protected final void setDisplayName(final String name) {
        putValue(NAME, name);
    }

    protected final String getName() {
        return (String) getValue(NAME);
    }

    protected final String getNamePattern() {
        return namePattern;
    }

    protected final ProfilerProjectActionPerformer getPerformer() {
        return performer;
    }

    protected void actionPerformed(final Lookup context) {
        final Project[] projects = ActionsUtil.getProjectsFromLookup(context, null);

        if (projects.length == 1) {
            if (performer != null) {
                new NBSwingWorker(false) {
                        private boolean isEnabled = false;
                        private final OutputParameter<Boolean> isCancelled = new OutputParameter<Boolean>(Boolean.FALSE);
                        private final OutputParameter<ProgressDisplayer> progress = new OutputParameter<ProgressDisplayer>(null);

                        protected void doInBackground() {
                            isEnabled = performer.enable(projects[0], context, false);
                        }

                        protected void done() {
                            if (progress.isSet()) {
                                progress.getValue().close();
                            }

                            if (isEnabled && !isCancelled.getValue()) {
                                performer.perform(projects[0], context);
                            } else if (!isCancelled.getValue()) {
                                NotifyDescriptor failure = new NotifyDescriptor.Message(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/actions/Bundle")
                                                                                                                .getString("AntActions_LazyEnablementFailure"));
                                DialogDisplayer.getDefault().notifyLater(failure);
                            }
                        }

                        protected void nonResponding() {
                            progress.setValue(ProgressDisplayer.showProgress(java.util.ResourceBundle.getBundle("org/netbeans/modules/profiler/actions/Bundle")
                                                                                                     .getString("AntActions_LazyEnablementProgressMessage"),
                                                                             new ProgressDisplayer.ProgressController() {
                                    public boolean cancel() {
                                        if (progress.isSet()) {
                                            progress.getValue().close();
                                        }

                                        progress.setValue(null);
                                        isCancelled.setValue(true);

                                        return true;
                                    }
                                }));
                        }
                    }.execute();
            }
        }
    }

    protected void doRefresh(final Lookup context) {
        final Project[] projects = ActionsUtil.getProjectsFromLookup(context, null);

        setDisplayName(ActionsUtil.formatProjectSensitiveName(getNamePattern(), projects));

        if ((projects != null) && (projects.length > 0)) {
            setEnabled(getPerformer().enable(projects[0], context, true));
        } else {
            setEnabled(false);
        }
    }

    /**
     * Initializes the action
     */
    private void init() {
        if (isCalculated) {
            return;
        }

        synchronized (initLock) {
            if (isCalculated) {
                return;
            }

            refresh(ProjectSensitiveAction.this.lookup);
            isCalculated = true;
        }
    }

    private void refresh(final Lookup context) {
        synchronized (refreshRequestLock) {
            if (refreshRequested++ > 0) {
                return;
            }

            IDEUtils.runInEventDispatchThread(new Runnable() {
                    public void run() {
                        int oldReqCount = -1;
                        int currentReqCount = -1;

                        synchronized (refreshRequestLock) {
                            oldReqCount = refreshRequested;
                        }

                        doRefresh(lookup);

                        synchronized (refreshRequestLock) {
                            currentReqCount = refreshRequested;
                            refreshRequested = 0;
                        }

                        if (oldReqCount != currentReqCount) {
                            refresh(context);
                        }
                    }
                });
        }
    }
}
