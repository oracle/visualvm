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

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ElementUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.lib.profiler.marker.Marker;
import org.netbeans.lib.profiler.marker.MethodMarker;
import org.netbeans.lib.profiler.results.cpu.marking.HierarchicalMark;
import org.netbeans.lib.profiler.results.cpu.marking.Mark;
import org.netbeans.modules.profiler.spi.ProjectTypeProfiler;
import org.netbeans.modules.profiler.ui.stp.DefaultSettingsConfigurator;
import org.netbeans.modules.profiler.ui.stp.SelectProfilingTask;
import org.netbeans.spi.project.support.ant.GeneratedFilesHelper;
import org.openide.filesystems.FileObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.swing.JComponent;
import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;


/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractProjectTypeProfiler implements ProjectTypeProfiler {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static final Logger LOGGER = Logger.getLogger("org.netbeans.modules.profiler.spi.ProjectTypeProfiler");

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract String getProfilerTargetName(Project project, FileObject buildScript, int type, FileObject profiledClassFile);

    public abstract boolean isProfilingSupported(Project project);

    public JComponent getAdditionalConfigurationComponent(Project project) {
        return null;
    }

    public boolean isAttachSupported(Project project) {
        return true;
    }

    public org.netbeans.lib.profiler.client.ClientUtils.SourceCodeSelection[] getDefaultRootMethods(Project project,
                                                                                                    FileObject profiledClassFile,
                                                                                                    boolean profileUnderlyingFramework,
                                                                                                    String[][] projectPackagesDescr) {
        if (profileUnderlyingFramework) {
            // No root method should be specified, first executed method will be treated as root method
            return new ClientUtils.SourceCodeSelection[0];
        } else {
            // Profile Project or Profile Single
            if (profiledClassFile == null) {
                // Profile Project, extract root methods from the project
                return ProjectUtilities.getProjectDefaultRoots(project, projectPackagesDescr);
            } else {
                // Profile Single, provide correct root methods
                String profiledClass = SourceUtils.getToplevelClassName(profiledClassFile);

                return new ClientUtils.SourceCodeSelection[] { new ClientUtils.SourceCodeSelection(profiledClass, "<all>", "") }; // NOI18N // Covers all innerclasses incl. anonymous innerclasses
            }
        }
    }

    public boolean isFileObjectSupported(Project project, FileObject fo) {
        return true;
    }

    public abstract boolean checkProjectCanBeProfiled(Project project, FileObject profiledClassFile);

    public abstract boolean checkProjectIsModifiedForProfiler(Project project);

    public HierarchicalMark getMarkHierarchyRoot() {
        return HierarchicalMark.DEFAULT;
    }

    public Marker getMethodMarker(Project project) {
        return Marker.DEFAULT;
    }

    public List<SimpleFilter> getPredefinedInstrumentationFilters(Project project) {
        return ProjectUtilities.getProjectDefaultInstrFilters(project);
    }

    public float getProfilingOverhead(ProfilingSettings settings) {
         float o = 0.0f;

        if (org.netbeans.modules.profiler.ui.stp.Utils.isMonitorSettings(settings)) {
            //} else if (org.netbeans.modules.profiler.ui.stp.Utils.isAnalyzerSettings(settings)) {
        } else if (org.netbeans.modules.profiler.ui.stp.Utils.isCPUSettings(settings)) {
            if (settings.getProfilingType() == ProfilingSettings.PROFILE_CPU_ENTIRE) {
                o += 0.5f; // entire app
            } else if (settings.getProfilingType() == ProfilingSettings.PROFILE_CPU_PART) {
                o += 0.2f; // part of app
            }

            if (FilterUtils.NONE_FILTER.equals(settings.getSelectedInstrumentationFilter())) {
                o += 0.5f; // profile all classes
            }
        } else if (org.netbeans.modules.profiler.ui.stp.Utils.isMemorySettings(settings)) {
            if (settings.getProfilingType() == ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS) {
                o += 0.5f; // object allocations
            } else if (settings.getProfilingType() == ProfilingSettings.PROFILE_MEMORY_LIVENESS) {
                o += 0.7f; // object liveness
            }

            if (settings.getAllocStackTraceLimit() != 0) {
                o += 0.3f; // record allocation stack traces
            }
        }

        return o;
    }

    public FileObject getProjectBuildScript(Project project) {
        return project.getProjectDirectory().getFileObject(GeneratedFilesHelper.BUILD_XML_PATH);
    }

    public JavaPlatform getProjectJavaPlatform(Project project) {
        return null;
    }

    public SelectProfilingTask.SettingsConfigurator getSettingsConfigurator() {
        return DefaultSettingsConfigurator.SHARED_INSTANCE;
    }

    public SimpleFilter computePredefinedInstrumentationFilter(Project project, SimpleFilter predefinedInstrFilter,
                                                               String[][] projectPackagesDescr) {
        return ProjectUtilities.computeProjectOnlyInstrumentationFilter(project, predefinedInstrFilter, projectPackagesDescr);
    }

    public void computeProjectPackages(Project project, boolean subprojects, String[][] storage) {
        ProjectUtilities.computeProjectPackages(project, subprojects, storage);
    }

    public void configurePropertiesForProfiling(Properties props, Project project, FileObject profiledClassFile) {
    }

    public void setupProjectSessionSettings(Project project, org.netbeans.lib.profiler.common.SessionSettings ss) {
    }

    public boolean supportsSettingsOverride() {
        return false;
    }

    public boolean supportsUnintegrate(Project project) {
        return false;
    }

    public void unintegrateProfiler(Project project) {
    }

    protected void addInterfaceMarker(MethodMarker marker, String interfaceName, Mark mark, Project project) {
        addInterfaceMarker(marker, interfaceName, null, false, mark, project);
    }

    protected void addInterfaceMarker(final MethodMarker marker, final String interfaceName,
                                      final String[] methodNameRestriction, final boolean inclusive, final Mark mark,
                                      Project project) {
        final ClasspathInfo cpInfo = ProjectUtilities.getClasspathInfo(project, true);

        JavaSource js = JavaSource.create(cpInfo, new FileObject[0]);

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        doAddInterfaceMarker(marker, interfaceName, methodNameRestriction, inclusive, mark, controller);
                    }
                }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void addInterfaceMarkers(final MethodMarker marker, final String[] interfaceNames, final Mark mark,
                                       final Project project) {
        final ClasspathInfo cpInfo = ProjectUtilities.getClasspathInfo(project, true);

        JavaSource js = JavaSource.create(cpInfo, new FileObject[0]);

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        for (String interfaceName : interfaceNames) {
                            doAddInterfaceMarker(marker, interfaceName, null, false, mark, controller);
                        }
                    }
                }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected void addInterfaceMarkers(final MethodMarker marker, final String[] interfaceNames,
                                       final String[] methodNameRestriction, final boolean inclusive, final Mark mark,
                                       final Project project) {
        final ClasspathInfo cpInfo = ProjectUtilities.getClasspathInfo(project, true);

        JavaSource js = JavaSource.create(cpInfo, new FileObject[0]);

        try {
            js.runUserActionTask(new CancellableTask<CompilationController>() {
                    public void cancel() {
                    }

                    public void run(CompilationController controller)
                             throws Exception {
                        for (String interfaceName : interfaceNames) {
                            doAddInterfaceMarker(marker, interfaceName, methodNameRestriction, inclusive, mark, controller);
                        }
                    }
                }, true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void addImplementorMethods(final MethodMarker marker, final TypeElement superElement, final List<String> restrictors,
                                       final boolean inclusive, final Mark mark, final CompilationController controller) {
        // get all implementors of the superclass and add their marker methods
        final Set<ClassIndex.SearchKind> kind = new HashSet<ClassIndex.SearchKind>(Arrays.asList(new ClassIndex.SearchKind[] {
                                                                                                     ClassIndex.SearchKind.IMPLEMENTORS
                                                                                                 }));

        //    final Set<ClassIndex.SearchScope> scope = new HashSet<ClassIndex.SearchScope>(Arrays.asList(new ClassIndex.SearchScope[]{ClassIndex.SearchScope.SOURCE, ClassIndex.SearchScope.DEPENDENCIES}));
        final Set<ClassIndex.SearchScope> scope = new HashSet<ClassIndex.SearchScope>(Arrays.asList(new ClassIndex.SearchScope[] {
                                                                                                        ClassIndex.SearchScope.SOURCE
                                                                                                    }));

        Set<ElementHandle<TypeElement>> allImplementors = new HashSet<ElementHandle<TypeElement>>();
        Set<ElementHandle<TypeElement>> implementors = controller.getClasspathInfo().getClassIndex()
                                                                 .getElements(ElementHandle.create(superElement), kind, scope);

        do {
            Set<ElementHandle<TypeElement>> tmpImplementors = new HashSet<ElementHandle<TypeElement>>();
            allImplementors.addAll(implementors);

            for (ElementHandle<TypeElement> element : implementors) {
                tmpImplementors.addAll(controller.getClasspathInfo().getClassIndex().getElements(element, kind, scope));
            }

            implementors = tmpImplementors;
        } while (!implementors.isEmpty());

        for (ElementHandle<TypeElement> handle : allImplementors) {
            // resolve the implementor's type element
            TypeElement implementor = handle.resolve(controller);
            addTypeMethods(marker, implementor, restrictors, inclusive, mark, controller);
        }
    }

    private void addTypeMethods(final MethodMarker marker, final TypeElement type, final List<String> restrictors,
                                final boolean inclusive, final Mark mark, final CompilationController controller) {
        if ((marker == null) || (type == null) || (restrictors == null) || (mark == null) || (controller == null)) {
            return;
        }

        // process all methods from the implementor
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if ((method.getKind() == ElementKind.METHOD) && !method.getModifiers().contains(Modifier.ABSTRACT)) {
                if ((inclusive && restrictors.contains(method.getSimpleName().toString()))
                        || (!inclusive && !restrictors.contains(method.getSimpleName().toString()))) {
                    try {
                        marker.addMethodMark(ElementUtilities.getBinaryName(type), method.getSimpleName().toString(),
                                             SourceUtils.getVMMethodSignature(method, controller), mark);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void doAddInterfaceMarker(final MethodMarker marker, final String interfaceName,
                                      final String[] methodNameRestriction, final boolean inclusive, final Mark mark,
                                      final CompilationController controller)
                               throws IllegalArgumentException {
        // store restriction array in a list to ease the "contains" checks
        List<String> restrictors = (methodNameRestriction != null) ? Arrays.asList(methodNameRestriction) : new ArrayList();

        // resolve the type element for the interface/supertype
        TypeElement superElement = controller.getElements().getTypeElement(interfaceName);

        if (superElement == null) {
            LOGGER.fine("Couldn't resolve type: " + interfaceName);

            return;
        }

        switch (superElement.getKind()) {
            case INTERFACE: {
                addImplementorMethods(marker, superElement, restrictors, inclusive, mark, controller);

                break;
            }
            case CLASS: {
                // add all superclass methods
                addTypeMethods(marker, superElement, restrictors, inclusive, mark, controller);

                if (!superElement.getModifiers().contains(Modifier.FINAL)) {
                    addImplementorMethods(marker, superElement, restrictors, inclusive, mark, controller);
                }

                break;
            }
        }
    }
}
