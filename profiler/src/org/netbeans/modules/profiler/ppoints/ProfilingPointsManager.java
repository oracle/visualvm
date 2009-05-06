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

package org.netbeans.modules.profiler.ppoints;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.lib.profiler.client.ProfilingPointsProcessor;
import org.netbeans.lib.profiler.client.RuntimeProfilingPoint;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.netbeans.modules.profiler.ppoints.ui.ProfilingPointsWindow;
import org.netbeans.modules.profiler.ppoints.ui.ValidityAwarePanel;
import org.netbeans.modules.profiler.ppoints.ui.ValidityListener;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.DialogDescriptor;
import org.openide.ErrorManager;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.text.Annotatable;
import org.openide.text.Line;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;
import org.openide.filesystems.FileChangeListener;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class ProfilingPointsManager extends ProfilingPointsProcessor implements PropertyChangeListener, ProfilingStateListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class ProfilingPointsComparator implements Comparator {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean sortOrder;
        private int sortBy;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProfilingPointsComparator(int sortBy, boolean sortOrder) {
            this.sortBy = sortBy;
            this.sortOrder = sortOrder;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int compare(Object o1, Object o2) {
            ProfilingPoint pp1 = sortOrder ? (ProfilingPoint) o1 : (ProfilingPoint) o2;
            ProfilingPoint pp2 = sortOrder ? (ProfilingPoint) o2 : (ProfilingPoint) o1;

            switch (sortBy) {
                case CommonConstants.SORTING_COLUMN_DEFAULT:
                case SORT_BY_PROJECT:
                    return ProjectUtils.getInformation(pp1.getProject()).getDisplayName()
                                       .compareTo(ProjectUtils.getInformation(pp2.getProject()).getDisplayName());
                case SORT_BY_SCOPE:

                    int v1 = pp1.getFactory().getScope();
                    int v2 = pp2.getFactory().getScope();

                    return ((v1 < v2) ? (-1) : ((v1 == v2) ? 0 : 1));
                case SORT_BY_NAME:
                    return pp1.getName().compareTo(pp2.getName());
                case SORT_BY_RESULTS:
                    return pp1.getResultsText().compareTo(pp2.getResultsText());
                default:
                    throw new RuntimeException("Unsupported compare operation for " + o1 + ", " + o2); // NOI18N
            }
        }
    }

    private class CustomizerButton extends JButton implements ValidityListener {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public CustomizerButton() {
            super(OK_BUTTON_TEXT);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void validityChanged(boolean isValid) {
            setEnabled(isValid);
        }
    }

    private class CustomizerListener extends WindowAdapter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Dialog d;
        private DialogDescriptor dd;
        private Runnable updater;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public CustomizerListener(Dialog d, DialogDescriptor dd, Runnable updater) {
            super();
            this.d = d;
            this.dd = dd;
            this.updater = updater;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void windowClosed(WindowEvent e) {
            if (dd.getValue() == customizerButton) {
                updater.run();
            }

            d.removeWindowListener(this);
            d = null;
            dd = null;
            updater = null;
        }

        public void windowOpened(WindowEvent e) {
            d.requestFocus();
        }
    }

    private class RuntimeProfilingPointMapper {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final CodeProfilingPoint owner;
        private final int index;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public RuntimeProfilingPointMapper(CodeProfilingPoint owner, int index) {
            this.owner = owner;
            this.index = index;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getIndex() {
            return index;
        }

        public CodeProfilingPoint getOwner() {
            return owner;
        }
    }
    
    private class FileWatch {
        private int references = 0;
        private LocationFileListener listener;
        
        public FileWatch(LocationFileListener listener) { this.listener = listener; }
        
        public boolean hasReferences() { return references > 0; }
        public LocationFileListener getListener() { return listener; }
        
        public void increaseReferences() { references++; }
        public void decreaseReferences() { references--; }
    }
    
    private class LocationFileListener implements FileChangeListener {
        
        private File file;
        
        public LocationFileListener(File file) { this.file = file; }

        public void fileDeleted(final FileEvent fe) {
            Runnable processor = new Runnable() {
                public void run() { deleteProfilingPointsForFile(file); }
            };
            
            if (SwingUtilities.isEventDispatchThread()) {
                RequestProcessor.getDefault().post(processor);
            } else {
                processor.run();
            }
        }

        public void fileRenamed(final FileRenameEvent fe) {
            Runnable processor = new Runnable() {
                public void run() { 
                    FileObject renamedFileO = fe.getFile();
                    File renamedFile = FileUtil.toFile(renamedFileO);
                    if (renamedFile != null && renamedFile.exists() && renamedFile.isFile()) {
                        updateProfilingPointsFile(file, renamedFile);
                    } else {
                        deleteProfilingPointsForFile(file);
                    }
                }
            };
            
            if (SwingUtilities.isEventDispatchThread()) {
                RequestProcessor.getDefault().post(processor);
            } else {
                processor.run();
            }
        }
        
        public void fileFolderCreated(FileEvent fe) {}
        public void fileDataCreated(FileEvent fe) {}
        public void fileChanged(FileEvent fe) {}
        public void fileAttributeChanged(FileAttributeEvent fe) {}
        
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ANOTHER_PP_EDITED_MSG = NbBundle.getMessage(ProfilingPointsManager.class,
                                                                            "ProfilingPointsManager_AnotherPpEditedMsg"); // NOI18N
    private static final String PP_CUSTOMIZER_CAPTION = NbBundle.getMessage(ProfilingPointsManager.class,
                                                                            "ProfilingPointsManager_PpCustomizerCaption"); // NOI18N
    private static final String CANNOT_STORE_PP_MSG = NbBundle.getMessage(ProfilingPointsManager.class,
                                                                          "ProfilingPointsManager_CannotStorePpMsg"); // NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(ProfilingPointsManager.class,
                                                                     "ProfilingPointsManager_OkButtonText"); // NOI18N
                                                                                                             // -----
    public static final String PROPERTY_PROJECTS_CHANGED = "p_projects_changed"; // NOI18N
    public static final String PROPERTY_PROFILING_POINTS_CHANGED = "p_profiling_points_changed"; // NOI18N
    public static final int SORT_BY_PROJECT = 1;
    public static final int SORT_BY_SCOPE = 2;
    public static final int SORT_BY_NAME = 3;
    public static final int SORT_BY_RESULTS = 4;
    private static ProfilingPointsManager defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CustomizerButton customizerButton = new CustomizerButton();
    private List<GlobalProfilingPoint> activeGlobalProfilingPoints = new ArrayList();
    private Map<Integer, RuntimeProfilingPointMapper> activeCodeProfilingPoints = new HashMap();
    private PropertyChangeListener pcl = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(ProfilingPointFactory.AVAILABILITY_PROPERTY)) {
                refreshProfilingPointFactories();
                firePropertyChanged(PROPERTY_PROFILING_POINTS_CHANGED); // notify the profiling points list displayer about the change
            }
        }
    };

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    private Set<ProfilingPoint> dirtyProfilingPoints = Collections.synchronizedSet(new HashSet());
    private Vector<ValidityAwarePanel> customizers = new Vector();
    private Vector<Project> openedProjects = new Vector();
    private Vector<ProfilingPoint> profilingPoints = new Vector();
    private ProfilingPointFactory[] profilingPointFactories = new ProfilingPointFactory[0];
    private boolean profilingInProgress = false; // collecting data
    private boolean profilingSessionInProgress = false; // session started and not yet finished
    private int nextUniqueRPPIdentificator;
    
    private Map<File, FileWatch> profilingPointsFiles = new HashMap<File, FileWatch>();
    private boolean ignoreStoreProfilingPoints = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private ProfilingPointsManager() {
        refreshProfilingPointFactories();
        RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    processOpenedProjectsChanged(); // will subsequently invoke projectOpened on all open projects
                    NetBeansProfiler.getDefaultNB().addProfilingStateListener(ProfilingPointsManager.this);
                    OpenProjects.getDefault().addPropertyChangeListener(ProfilingPointsManager.this);
                }
            });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized ProfilingPointsManager getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new ProfilingPointsManager();
        }

        return defaultInstance;
    }

    public List<ProfilingPoint> getCompatibleProfilingPoints(Project project, ProfilingSettings profilingSettings, boolean sorted) {
        List<ProfilingPoint> projectProfilingPoints = sorted ? getSortedProfilingPoints(project, 1, false)
                                                             : getProfilingPoints(project, ProfilerIDESettings.
                                                               getInstance().getIncludeProfilingPointsDependencies(), false); // TODO: define default sorting (current sorting of Profiling Points window?)
        List<ProfilingPoint> compatibleProfilingPoints = new ArrayList();

        for (ProfilingPoint profilingPoint : projectProfilingPoints) {
            if (profilingPoint.supportsProfilingSettings(profilingSettings)) {
                compatibleProfilingPoints.add(profilingPoint);
            }
        }

        return compatibleProfilingPoints;
    }

    // Currently profiling, data are being collected
    public boolean isProfilingInProgress() {
        return profilingInProgress;
    }

    public ProfilingPointFactory[] getProfilingPointFactories() {
        return profilingPointFactories;
    }

    public List<ProfilingPoint> getProfilingPoints(Project project,
                                                   boolean inclSubprojects,
                                                   boolean inclUnavailable) {
        return getProfilingPoints(ProfilingPoint.class, project,
                                  inclSubprojects, inclUnavailable);
    }

    public <T extends ProfilingPoint> List<T> getProfilingPoints(Class<T> ppClass,
                                                                 Project project,
                                                                 boolean inclSubprojects) {
        return getProfilingPoints(ppClass, project, inclSubprojects, true);
    }

    public <T extends ProfilingPoint> List<T> getProfilingPoints(Class<T> ppClass,
                                                                 Project project,
                                                                 boolean inclSubprojects,
                                                                 boolean inclUnavailable) {
        Set<Project> projects = new HashSet();

        if (project == null) {
            projects.addAll(openedProjects);
        } else {
            projects.add(project);
            if (inclSubprojects) projects.addAll(getOpenSubprojects(project));
        }        

        ArrayList<T> filteredProfilingPoints = new ArrayList();
        Iterator<ProfilingPoint> iterator = profilingPoints.iterator();

        while (iterator.hasNext()) {
            ProfilingPoint profilingPoint = iterator.next();
            ProfilingPointFactory factory = profilingPoint.getFactory();

            // Bugfix #162132, the factory may already be unloaded
            if (factory != null) {
                if (ppClass.isInstance(profilingPoint)) {
                    if (projects.contains(profilingPoint.getProject())) {
                        if (inclUnavailable || factory.isAvailable()) {
                            filteredProfilingPoints.add((T) profilingPoint);
                        }
                    }
                }
            } else {
                // TODO: profiling points without factories should be cleaned up somehow
            }
        }

        return filteredProfilingPoints;
    }

    // Profiling session started and not yet finished
    public boolean isProfilingSessionInProgress() {
        return profilingSessionInProgress;
    }

    public List<ProfilingPoint> getSortedProfilingPoints(Project project, int sortBy, boolean sortOrder) {
        List<ProfilingPoint> sortedProfilingPoints = getProfilingPoints(project, ProfilerIDESettings.getInstance().
                                                                        getIncludeProfilingPointsDependencies(), false);
        Collections.sort(sortedProfilingPoints, new ProfilingPointsComparator(sortBy, sortOrder));

        return sortedProfilingPoints;
    }

    public void addProfilingPoint(ProfilingPoint profilingPoint) {
        addProfilingPoints(new ProfilingPoint[] { profilingPoint });
    }

    public void addProfilingPoints(ProfilingPoint[] profilingPointsArr) {
        addProfilingPoints(profilingPointsArr, false);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    // TODO: should optionally support also subprojects/project references
    public RuntimeProfilingPoint[] createCodeProfilingConfiguration(Project project, ProfilingSettings profilingSettings) {
        
        checkProfilingPoints(); // NOTE: Probably not neccessary but we need to be sure here
        
        nextUniqueRPPIdentificator = 0;

        List<RuntimeProfilingPoint> runtimeProfilingPoints = new ArrayList();
        List<ProfilingPoint> compatibleProfilingPoints = getCompatibleProfilingPoints(project, profilingSettings, false);

        for (ProfilingPoint compatibleProfilingPoint : compatibleProfilingPoints) {
            if (compatibleProfilingPoint.isEnabled() && compatibleProfilingPoint instanceof CodeProfilingPoint) {
                CodeProfilingPoint compatibleCodeProfilingPoint = (CodeProfilingPoint) compatibleProfilingPoint;
                RuntimeProfilingPoint[] rpps = compatibleCodeProfilingPoint.createRuntimeProfilingPoints();
                
                if (rpps.length == 0) ErrorManager.getDefault().log(ErrorManager.USER, "Cannot resolve RuntimeProfilingPoint(s) for " + compatibleCodeProfilingPoint.getName() + ", check location"); // NOI18N

                for (int i = 0; i < rpps.length; i++) {
                    runtimeProfilingPoints.add(rpps[i]);
                    activeCodeProfilingPoints.put(rpps[i].getId(),
                                                  new RuntimeProfilingPointMapper(compatibleCodeProfilingPoint, i)); // Note that profiled project may be closed but it's active Profiling Points are still referenced by this map => will be processed
                }
            }
        }

        return runtimeProfilingPoints.toArray(new RuntimeProfilingPoint[runtimeProfilingPoints.size()]);
    }

    // TODO: should optionally support also subprojects/project references
    public GlobalProfilingPoint[] createGlobalProfilingConfiguration(Project project, ProfilingSettings profilingSettings) {
        
        checkProfilingPoints(); // NOTE: Probably not neccessary but we need to be sure here
        
        List<ProfilingPoint> compatibleProfilingPoints = getCompatibleProfilingPoints(project, profilingSettings, false);

        for (ProfilingPoint compatibleProfilingPoint : compatibleProfilingPoints) {
            if (compatibleProfilingPoint.isEnabled() && compatibleProfilingPoint instanceof GlobalProfilingPoint) {
                activeGlobalProfilingPoints.add((GlobalProfilingPoint) compatibleProfilingPoint);
            }
        }

        return activeGlobalProfilingPoints.toArray(new GlobalProfilingPoint[activeGlobalProfilingPoints.size()]);
    }

    public synchronized int createUniqueRuntimeProfilingPointIdentificator() {
        return nextUniqueRPPIdentificator++;
    }

    public void firePropertyChanged(String property) {
        propertyChangeSupport.firePropertyChange(property, false, true);
    }

    public void ideClosing() {
        // TODO: dirty profiling points should be persisted on document save!
        storeDirtyProfilingPoints();
    }

    public void instrumentationChanged(int i, int i0) {
    }

    public void profilingPointHit(RuntimeProfilingPoint.HitEvent hitEvent) {
        RuntimeProfilingPointMapper mapper = activeCodeProfilingPoints.get(hitEvent.getId());

        if (mapper != null) {
            mapper.getOwner().hit(hitEvent, mapper.getIndex());
        } else {
            ErrorManager.getDefault().log(ErrorManager.ERROR, "Cannot resolve ProfilingPoint for event: " + hitEvent); // NOI18N
        }
    }

    public synchronized void profilingStateChanged(ProfilingStateEvent profilingStateEvent) {
        boolean wasProfilingInProgress = profilingInProgress;
        boolean wasProfilingSessionInProgres = profilingSessionInProgress;

        switch (profilingStateEvent.getNewState()) {
            case Profiler.PROFILING_INACTIVE:
            case Profiler.PROFILING_STOPPED:
                profilingInProgress = false;
                profilingSessionInProgress = false;

                break;
            case Profiler.PROFILING_STARTED:
            case Profiler.PROFILING_IN_TRANSITION:
                profilingInProgress = false;
                profilingSessionInProgress = true;

                break;
            default:
                profilingInProgress = true;
                profilingSessionInProgress = true;
        }

        if ((wasProfilingInProgress != profilingInProgress) || (wasProfilingSessionInProgres != profilingSessionInProgress)) {
            GlobalProfilingPointsProcessor.getDefault().notifyProfilingStateChanged();
            IDEUtils.runInEventDispatchThread(new Runnable() {
                    public void run() {
                        ProfilingPointsWindow.getDefault().notifyProfilingStateChanged(); // this needs to be called on EDT
                    }
                });
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof Line && Line.PROP_LINE_NUMBER.equals(evt.getPropertyName())) {
            final Line line = (Line) evt.getSource();
            RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        for (ProfilingPoint pp : profilingPoints) {
                            if (pp instanceof CodeProfilingPoint) {
                                CodeProfilingPoint cpp = (CodeProfilingPoint) pp;

                                for (CodeProfilingPoint.Annotation cppa : cpp.getAnnotations()) {
                                    if (line.equals(cppa.getAttachedAnnotatable())) {
                                        cpp.internalUpdateLocation(cppa, line.getLineNumber() + 1); // Line is 0-based, needs to be 1-based for CodeProfilingPoint.Location
                                    }
                                }

                                dirtyProfilingPoints.add(cpp);
                            }
                        }
                    }
                });
        } else if (evt.getSource() instanceof ProfilingPoint) {
            ProfilingPoint profilingPoint = (ProfilingPoint) evt.getSource();
            storeProfilingPoints(new ProfilingPoint[] { profilingPoint });

            if (isAnnotationChange(evt)) {
                deannotate((CodeProfilingPoint.Annotation[]) evt.getOldValue());
                annotate((CodeProfilingPoint) profilingPoint, (CodeProfilingPoint.Annotation[]) evt.getNewValue());
            }

            if (isLocationChange(evt)) {
                CodeProfilingPoint.Location oldLocation = (CodeProfilingPoint.Location)evt.getOldValue();
                if (oldLocation != null && !CodeProfilingPoint.Location.EMPTY.equals(oldLocation))
                    removeFileWatch(new File(oldLocation.getFile()));
                
                CodeProfilingPoint.Location newLocation = (CodeProfilingPoint.Location)evt.getNewValue();
                if (newLocation != null && !CodeProfilingPoint.Location.EMPTY.equals(newLocation))
                    addFileWatch(new File(newLocation.getFile()));
            }
                
            if (isAppearanceChange(evt)) {
                firePropertyChanged(PROPERTY_PROFILING_POINTS_CHANGED);
            }
        } else if (OpenProjects.PROPERTY_OPEN_PROJECTS.equals(evt.getPropertyName())) {
            RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        processOpenedProjectsChanged();
                    }
                });

            // --- Code for saving dirty profiling points on document save instead of IDE closing ----------------
            //    } else if (DataObject.PROP_MODIFIED.equals(evt.getPropertyName())) {
            //      System.err.println(">>> Changed " + evt.getPropertyName() + " from " + evt.getOldValue() + " to " + evt.getNewValue() + ", origin: "+ evt.getSource());
            // ---------------------------------------------------------------------------------------------------
        }
    }

    public void removeProfilingPoint(ProfilingPoint profilingPoint) {
        removeProfilingPoints(new ProfilingPoint[] { profilingPoint });
    }

    public synchronized void removeProfilingPoints(ProfilingPoint[] profilingPointsArr) {
        removeProfilingPoints(profilingPointsArr, false);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void reset() {
        // TODO: currently only last used profiling points are reset, check if all profiling points need to be reset
        List<ProfilingPoint> profilingPointsToReset = new LinkedList();

        // reset CodeProfilingPoints
        Collection<RuntimeProfilingPointMapper> mappersToReset = activeCodeProfilingPoints.values();

        for (RuntimeProfilingPointMapper mapper : mappersToReset) {
            profilingPointsToReset.add(mapper.getOwner());
        }

        activeCodeProfilingPoints.clear();

        // reset GlobalProfilingPoints
        profilingPointsToReset.addAll(activeGlobalProfilingPoints);
        activeGlobalProfilingPoints.clear();

        for (ProfilingPoint ppoint : profilingPointsToReset) {
            ppoint.reset();
        }

        profilingPointsToReset.clear();
    }

    public void threadsMonitoringChanged() {
    }

    public void timeAdjust(final int threadId, final long timeDiff0, final long timeDiff1) {
        Iterator<RuntimeProfilingPointMapper> it = activeCodeProfilingPoints.values().iterator();
        Set uniqueSet = new HashSet();

        while (it.hasNext()) {
            CodeProfilingPoint cpp = it.next().getOwner();

            if (cpp instanceof CodeProfilingPoint.Paired) {
                if (uniqueSet.add(cpp)) {
                    ((CodeProfilingPoint.Paired) cpp).timeAdjust(threadId, timeDiff0, timeDiff1);
                }
            }
        }
    }

    boolean isAnyCustomizerShowing() {
        return getShowingCustomizer() != null;
    }

    ValidityAwarePanel getShowingCustomizer() {
        Iterator<ValidityAwarePanel> iterator = customizers.iterator();

        while (iterator.hasNext()) {
            ValidityAwarePanel vaPanel = iterator.next();

            if (vaPanel.isShowing()) {
                return vaPanel;
            }
        }

        return null;
    }

    // Returns true if customizer was opened and then submitted by OK button
    void customize(final ValidityAwarePanel customizer, Runnable updater) {
        ValidityAwarePanel showingCustomizer = getShowingCustomizer();

        if (showingCustomizer != null) {
            NetBeansProfiler.getDefaultNB().displayWarningAndWait(ANOTHER_PP_EDITED_MSG);
            SwingUtilities.getWindowAncestor(showingCustomizer).requestFocus();
            showingCustomizer.requestFocusInWindow();
        } else {
            customizer.addValidityListener(customizerButton);
            customizerButton.setEnabled(customizer.areSettingsValid()); // In fact customizer should be valid but just to be sure...

            JPanel customizerContainer = new JPanel(new BorderLayout());
            JPanel customizerSpacer = new JPanel(new BorderLayout());
            customizerSpacer.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
            customizerSpacer.add(customizer, BorderLayout.CENTER);
            customizerContainer.add(customizerSpacer, BorderLayout.CENTER);
            customizerContainer.add(new JSeparator(), BorderLayout.SOUTH);

            HelpCtx helpCtx = null;

            if (customizer instanceof HelpCtx.Provider) {
                helpCtx = ((HelpCtx.Provider) customizer).getHelpCtx();
            }

            DialogDescriptor dd = new DialogDescriptor(customizerContainer, PP_CUSTOMIZER_CAPTION, false,
                                                       new Object[] { customizerButton, DialogDescriptor.CANCEL_OPTION },
                                                       customizerButton, 0, helpCtx, null);
            final Dialog d = ProfilerDialogs.createDialog(dd);
            d.addWindowListener(new CustomizerListener(d, dd, updater));
            d.setVisible(true);

            if (customizer.getInitialFocusTarget() != null) {
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            customizer.getInitialFocusTarget().requestFocusInWindow();
                        }
                    });
            }
        }
    }

    void documentOpened(Line.Set lineSet, FileObject fileObject) {
        for (ProfilingPoint profilingPoint : profilingPoints) {
            if (profilingPoint instanceof CodeProfilingPoint) {
                CodeProfilingPoint cpp = (CodeProfilingPoint) profilingPoint;

                for (CodeProfilingPoint.Annotation cppa : cpp.getAnnotations()) {
                    File annotationFile = new File(cpp.getLocation(cppa).getFile());

                    if ((annotationFile == null) || (fileObject == null)) {
                        continue; // see #98535
                    }

                    File adeptFile = FileUtil.toFile(fileObject);

                    if (adeptFile == null) {
                        continue; // see #98535
                    }

                    if (adeptFile.equals(annotationFile)) {
                        deannotateProfilingPoint(cpp);
                        annotateProfilingPoint(cpp);

                        break;
                    }
                }
            }
        }
    }

    ValidityAwarePanel safeGetCustomizer(ValidityAwarePanel customizer) {
        if (!customizers.contains(customizer)) {
            customizers.add(customizer);
        }

        return isAnyCustomizerShowing() ? null : customizer;
    }

    private boolean isAnnotationChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();

        return propertyName.equals(CodeProfilingPoint.PROPERTY_ANNOTATION);
    }
    
    private boolean isLocationChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();

        return propertyName.equals(CodeProfilingPoint.PROPERTY_LOCATION);
    }
    
    private boolean isAppearanceChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();

        return propertyName.equals(ProfilingPoint.PROPERTY_NAME) || propertyName.equals(ProfilingPoint.PROPERTY_ENABLED)
               || propertyName.equals(ProfilingPoint.PROPERTY_PROJECT) || propertyName.equals(ProfilingPoint.PROPERTY_RESULTS);
    }

    private Set<Project> getOpenSubprojects(Project project) {
        Set<Project> subprojects = new HashSet();
        ProjectUtilities.fetchSubprojects(project, subprojects);

        if (subprojects.isEmpty()) return subprojects;

        Set<Project> openSubprojects = new HashSet();
        for (Project openProject : openedProjects)
            if (subprojects.contains(openProject))
                openSubprojects.add(openProject);

        return openSubprojects;
    }
    
    // Returns only valid profiling points (currently all GlobalProfilingPoints and CodeProfilingPoints with all locations pointing to a valid java file)
    private ProfilingPoint[] getValidProfilingPoints(ProfilingPoint[] profilingPointsArr) {
        ArrayList<ProfilingPoint> validProfilingPoints = new ArrayList<ProfilingPoint>();
        for (ProfilingPoint profilingPoint : profilingPointsArr) if(profilingPoint.isValid()) validProfilingPoints.add(profilingPoint);
        return validProfilingPoints.toArray(new ProfilingPoint[validProfilingPoints.size()]);
    }
    
    // Returns only invalid profiling points (currently CodeProfilingPoints with any of the locations pointing to an invalid file)
    private ProfilingPoint[] getInvalidProfilingPoints(ProfilingPoint[] profilingPointsArr) {
        ArrayList<ProfilingPoint> invalidProfilingPoints = new ArrayList<ProfilingPoint>();
        for (ProfilingPoint profilingPoint : profilingPointsArr) if(!profilingPoint.isValid()) invalidProfilingPoints.add(profilingPoint);
        return invalidProfilingPoints.toArray(new ProfilingPoint[invalidProfilingPoints.size()]);
    }
    
    // Checks if currently loaded profiling points are valid, invalid profiling points are silently deleted
    private void checkProfilingPoints() {
        ProfilingPoint[] invalidProfilingPoints = getInvalidProfilingPoints(profilingPoints.toArray(new ProfilingPoint[profilingPoints.size()]));
        if (invalidProfilingPoints.length > 0) removeProfilingPoints(invalidProfilingPoints);
    }
    
    private void addFileWatch(File file) {
        FileObject fileo = null;
        if (file.isFile())
            fileo = FileUtil.toFileObject(FileUtil.normalizeFile(file));
        if (fileo != null) {
            FileWatch fileWatch = profilingPointsFiles.get(file);
            if (fileWatch == null) {
                LocationFileListener listener = new LocationFileListener(file);
                fileWatch = new FileWatch(listener);
                fileo.addFileChangeListener(listener);
                profilingPointsFiles.put(file, fileWatch);
            }
            fileWatch.increaseReferences();
        }
    }
    
    private void removeFileWatch(File file) {
        FileObject fileo = null;
        if (file.isFile())
            fileo = FileUtil.toFileObject(FileUtil.normalizeFile(file));
        if (fileo != null) {
            FileWatch fileWatch = profilingPointsFiles.get(file);
            if (fileWatch != null) {
                fileWatch.decreaseReferences();
                if (!fileWatch.hasReferences()) fileo.removeFileChangeListener(profilingPointsFiles.remove(file).getListener());
            }
        } else {
            profilingPointsFiles.remove(file);
        }
    }
    
    private void addProfilingPointFileWatch(CodeProfilingPoint cpp) {
        CodeProfilingPoint.Annotation[] annotations = cpp.getAnnotations();
        for (CodeProfilingPoint.Annotation annotation : annotations) {
            CodeProfilingPoint.Location location = cpp.getLocation(annotation);
            String filename = location.getFile();
            addFileWatch(new File(filename));
        }
    }
    
    private void removeProfilingPointFileWatch(CodeProfilingPoint cpp) {
        CodeProfilingPoint.Annotation[] annotations = cpp.getAnnotations();
        for (CodeProfilingPoint.Annotation annotation : annotations) {
            CodeProfilingPoint.Location location = cpp.getLocation(annotation);
            String filename = location.getFile();
            removeFileWatch(new File(filename));
        }
    }
    
    private CodeProfilingPoint[] getProfilingPointsForFile(File file) {
        List<CodeProfilingPoint> profilingPointsForFile = new ArrayList<CodeProfilingPoint>();
        
        // TODO: could be optimized to search just within the owner Project
        for (ProfilingPoint profilingPoint : profilingPoints) {
            if (profilingPoint instanceof CodeProfilingPoint) {
                CodeProfilingPoint cpp = (CodeProfilingPoint)profilingPoint;
                for (CodeProfilingPoint.Annotation annotation : cpp.getAnnotations()) {
                    CodeProfilingPoint.Location location = cpp.getLocation(annotation);
                    File ppFile = new File(location.getFile());
                    if (file.equals(ppFile)) {
                        profilingPointsForFile.add(cpp);
                        break;
                    }
                }
            }
        }
        
        return profilingPointsForFile.toArray(new CodeProfilingPoint[profilingPointsForFile.size()]);
    }
    
    private void deleteProfilingPointsForFile(File file) {
        removeProfilingPoints(getProfilingPointsForFile(file));
    }
    
    private void updateProfilingPointsFile(File oldFile, File newFile) {
        String newFilename = newFile.getAbsolutePath();
        CodeProfilingPoint[] cppa = getProfilingPointsForFile(oldFile);
        
        ignoreStoreProfilingPoints = true;
        
        for (CodeProfilingPoint cpp : cppa) {
            for (CodeProfilingPoint.Annotation annotation : cpp.getAnnotations()) {
                CodeProfilingPoint.Location location = cpp.getLocation(annotation);
                File ppFile = new File(location.getFile());
                if (oldFile.equals(ppFile)) {
                    CodeProfilingPoint.Location newLocation = new CodeProfilingPoint.Location(
                            newFilename, location.getLine(), location.getOffset());
                    cpp.setLocation(annotation, newLocation);
                }
            }
        }
        
        ignoreStoreProfilingPoints = false;
        storeProfilingPoints(cppa);
    }

    private synchronized void addProfilingPoints(ProfilingPoint[] profilingPointsArr, boolean internalChange) {
        
        profilingPointsArr = getValidProfilingPoints(profilingPointsArr);
        
        for (ProfilingPoint profilingPoint : profilingPointsArr) {
            profilingPoints.add(profilingPoint);
            profilingPoint.addPropertyChangeListener(this);

            if (profilingPoint instanceof CodeProfilingPoint) {
                CodeProfilingPoint cpp = (CodeProfilingPoint) profilingPoint;
                annotateProfilingPoint(cpp);
                addProfilingPointFileWatch(cpp);
            }
        }

        if (!internalChange) {
            storeProfilingPoints(profilingPointsArr);
            firePropertyChanged(PROPERTY_PROFILING_POINTS_CHANGED);
        }
    }

    private void annotate(final CodeProfilingPoint profilingPoint, final CodeProfilingPoint.Annotation[] annotations) {
        RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    for (CodeProfilingPoint.Annotation cppa : annotations) {
                        // --- Code for saving dirty profiling points on document save instead of IDE closing ----------------
                        //          DataObject dataObject = Utils.getDataObject(profilingPoint.getLocation(cppa));
                        //          if (dataObject != null) dataObject.addPropertyChangeListener(ProfilingPointsManager.this);
                        // ---------------------------------------------------------------------------------------------------
                        Line editorLine = Utils.getEditorLine(profilingPoint.getLocation(cppa));

                        if (editorLine != null) {
                            editorLine.addPropertyChangeListener(ProfilingPointsManager.this);
                            cppa.attach(editorLine);
                        }
                    }
                }
            });
    }

    private void annotateProfilingPoint(final CodeProfilingPoint profilingPoint) {
        annotate(profilingPoint, profilingPoint.getAnnotations());
    }

    private void deannotate(final CodeProfilingPoint.Annotation[] annotations) {
        for (CodeProfilingPoint.Annotation cppa : annotations) {
            // --- Code for saving dirty profiling points on document save instead of IDE closing ----------------
            //      DataObject dataObject = Utils.getDataObject(profilingPoint.getLocation(cppa));
            //      if (dataObject != null) dataObject.removePropertyChangeListener(ProfilingPointsManager.this);
            // ---------------------------------------------------------------------------------------------------
            Annotatable cppaa = cppa.getAttachedAnnotatable();

            if (cppaa != null) {
                cppaa.removePropertyChangeListener(this);
            }

            cppa.detach();
        }
    }

    private void deannotateProfilingPoint(final CodeProfilingPoint profilingPoint) {
        deannotate(profilingPoint.getAnnotations());
    }

    private void loadProfilingPoints(Project project) {
        for (ProfilingPointFactory factory : profilingPointFactories) {
            try {
                addProfilingPoints(factory.loadProfilingPoints(project), true);
            } catch (Exception e) {
                ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
            }
        }
    }

    private synchronized void processOpenedProjectsChanged() {
        Vector<Project> lastOpenedProjects = new Vector(openedProjects);
        refreshOpenedProjects();

        for (Project project : lastOpenedProjects) {
            if (!openedProjects.contains(project)) {
                projectClosed(project);
            }
        }

        for (Project openProject : openedProjects) {
            if (!lastOpenedProjects.contains(openProject)) {
                projectOpened(openProject);
            }
        }

        firePropertyChanged(PROPERTY_PROJECTS_CHANGED);
    }

    private void projectClosed(Project project) {
        unloadProfilingPoints(project);
    }

    private void projectOpened(Project project) {
        loadProfilingPoints(project);
    }

    private void refreshOpenedProjects() {
        openedProjects.clear();

        Project[] openProjects = ProjectUtilities.getOpenedProjects();

        for (Project openProject : openProjects) {
            openedProjects.add(openProject);
        }
    }

    private void refreshProfilingPointFactories() {
        Collection<?extends ProfilingPointFactory> factories = Lookup.getDefault().lookupAll(ProfilingPointFactory.class);
        Collection<ProfilingPointFactory> cleansedFactories = new ArrayList<ProfilingPointFactory>();

        for (ProfilingPointFactory factory : factories) {
            if (factory.isAvailable()) {
                cleansedFactories.add(factory);
            }

            factory.addPropertyChangeListener(ProfilingPointFactory.AVAILABILITY_PROPERTY,
                                              WeakListeners.propertyChange(pcl, factory));
        }

        profilingPointFactories = new ProfilingPointFactory[cleansedFactories.size()];
        cleansedFactories.toArray(profilingPointFactories);
    }

    private synchronized void removeProfilingPoints(ProfilingPoint[] profilingPointsArr, boolean internalChange) {
        for (ProfilingPoint profilingPoint : profilingPointsArr) {
            if (profilingPoint instanceof CodeProfilingPoint) {
                CodeProfilingPoint cpp = (CodeProfilingPoint) profilingPoint;
                removeProfilingPointFileWatch(cpp);
                deannotateProfilingPoint(cpp);
            }

            profilingPoint.removePropertyChangeListener(this);
            profilingPoint.hideResults();
            profilingPoint.reset();
            profilingPoints.remove(profilingPoint);
        }

        if (!internalChange) {
            storeProfilingPoints(profilingPointsArr);
            firePropertyChanged(PROPERTY_PROFILING_POINTS_CHANGED);
        }
    }

    private void storeDirtyProfilingPoints() {
        ProfilingPoint[] dirtyProfilingPointsArr = new ProfilingPoint[dirtyProfilingPoints.size()];
        dirtyProfilingPoints.toArray(dirtyProfilingPointsArr);
        storeProfilingPoints(dirtyProfilingPointsArr);
    }

    private synchronized void storeProfilingPoints(ProfilingPoint[] profilingPointsArr) {
        if (ignoreStoreProfilingPoints) return;
        
        Set<Project> projects = new HashSet();
        Set<ProfilingPointFactory> factories = new HashSet();

        for (ProfilingPoint profilingPoint : profilingPointsArr) {
            projects.add(profilingPoint.getProject());
            factories.add(profilingPoint.getFactory());
            dirtyProfilingPoints.remove(profilingPoint);
        }

        for (ProfilingPointFactory factory : factories) {
            if (factory == null) continue;

            for (Project project : projects) {
                try {
                    factory.saveProfilingPoints(project);
                } catch (IOException ex) {
                    NetBeansProfiler.getDefaultNB()
                                    .displayError(MessageFormat.format(CANNOT_STORE_PP_MSG,
                                                                       new Object[] {
                                                                           factory.getType(),
                                                                           ProjectUtils.getInformation(project).getDisplayName()
                                                                       }));
                }
            }
        }
    }

    private void unloadProfilingPoints(Project project) {
        List<ProfilingPoint> closedProfilingPoints = getProfilingPoints(project, false, true);
        List<ProfilingPoint> dirtyClosedProfilingPoints = new ArrayList();

        for (ProfilingPoint profilingPoint : closedProfilingPoints) {
            if (dirtyProfilingPoints.contains(profilingPoint)) {
                dirtyClosedProfilingPoints.add(profilingPoint);
            }
        }

        if (!dirtyClosedProfilingPoints.isEmpty()) {
            storeProfilingPoints(dirtyClosedProfilingPoints.toArray(new ProfilingPoint[0]));
        }

        for (ProfilingPoint closedProfilingPoint : closedProfilingPoints) {
            if (closedProfilingPoint instanceof CodeProfilingPoint) {
                CodeProfilingPoint cpp = (CodeProfilingPoint) closedProfilingPoint;
                removeProfilingPointFileWatch(cpp);
                deannotateProfilingPoint(cpp);
            }

            closedProfilingPoint.hideResults(); // TODO: should stay open if subproject of profiled project
            closedProfilingPoint.reset(); // TODO: should not reset if subproject of profiled project
            profilingPoints.remove(closedProfilingPoint);
        }
    }
}
