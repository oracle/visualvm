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

package org.netbeans.modules.profiler.spi;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.SessionSettings;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.modules.profiler.ui.stp.DefaultSettingsConfigurator;
import org.netbeans.modules.profiler.ui.stp.SelectProfilingTask;
import org.openide.filesystems.FileObject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.swing.JComponent;
import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;


/**
 * An interface for classes that provide profiling support for a given Project type.
 * <p/>
 * The methods in this interface are called in this order:
 * <p/>
 * A. when a Project is selected as main, or Profile Project action is displayed: isProfilingSupported is called
 * to determine the enabled state
 * <p/>
 * B. when a file is selected in the explorer, the isFileObjectSupported method is called to determine if the
 * "Profile File" action is to be enabled
 * <p/>
 * C. a "Profile Main Project", "Profile Project" or "Profile File" action is invoked by the user
 * <p/>
 * 1. checkProjectIsModifiedForProfiler is called, the project type can make any adjustments it needs to the project,
 * or ask the user for more info. If it returns false as an idndication of error or user cancellation, the
 * profiling stops
 * <p/>
 * 2. checkProjectCanBeProfiled is called to give the ProjectType chance to check the project state, including the
 * selected class (if any)
 * <p/>
 * 3. setupProjectSessionSettings is called to allow the ProjectType to provide instance of SessionSettings to be used
 * for profiling of this project
 * <p/>
 * 4. getProfilerTargetName is called to determine which target to use for specific type of profiling. the
 * ProjectTypeProfiler can ask the user if the target is not determined
 * <p/>
 * 5. configurePropertiesForProfiling is called as the last thing before the Ant target is invoked, giving the
 * ProjectTypeProfiler chance to setup the properties for the Ant target (or tasks it calls) to use.
 */
public interface ProjectTypeProfiler {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final ProjectTypeProfiler DEFAULT = new ProjectTypeProfiler() {
        private final ClientUtils.SourceCodeSelection[] EMPTY_SELECTION = new ClientUtils.SourceCodeSelection[0];

        public boolean checkProjectCanBeProfiled(Project project, FileObject profiledClassFile) {
            return true;
        }

        public boolean checkProjectIsModifiedForProfiler(Project project) {
            return true;
        }

        public void configurePropertiesForProfiling(Properties props, Project project, FileObject profiledClassFile) {
        }

        public JComponent getAdditionalConfigurationComponent(Project project) {
            return null;
        }

        public SelectProfilingTask.SettingsConfigurator getSettingsConfigurator() {
            return DefaultSettingsConfigurator.SHARED_INSTANCE;
        }

        public void computeProjectPackages(Project project, boolean subprojects, String[][] storage) {
        }

        public ClientUtils.SourceCodeSelection[] getDefaultRootMethods(Project project, FileObject profiledClassFile,
                                                                       boolean profileUnderlyingFramework,
                                                                       String[][] projectPackagesDescr) {
            return EMPTY_SELECTION;
        }

        public List<SimpleFilter> getPredefinedInstrumentationFilters(Project project) {
            return Collections.emptyList();
        }

        public SimpleFilter computePredefinedInstrumentationFilter(Project project, SimpleFilter predefinedInstrFilter,
                                                                   String[][] projectPackagesDescr) {
            return null;
        }
        
        public boolean startProfilingSession(Project project, FileObject profiledClassFile, boolean isTest, Properties properties) {
            return false;
        }

        public String getProfilerTargetName(Project project, FileObject buildScript, int type, FileObject profiledClassFile) {
            return null;
        }

        public FileObject getProjectBuildScript(Project project) {
            return null;
        }

        public JavaPlatform getProjectJavaPlatform(Project project) {
            return null;
        }

        public boolean isAttachSupported(Project project) {
            // Check if project contains any Java sources
            if (ProjectUtilities.isJavaProject(project)) {
                return true;
            }

            // Check if any subproject contains any Java sources
            Set<Project> subprojects = new HashSet<Project>();
            ProjectUtilities.fetchSubprojects(project, subprojects);

            for (Project subproject : subprojects) {
                if (ProjectUtilities.isJavaProject(subproject)) {
                    return true;
                }
            }

            // Project and eventually subprojects don't contain any Java sources
            return false;
        }

        public boolean isFileObjectSupported(Project project, FileObject fo) {
            return false;
        }

        public boolean isProfilingSupported(Project project) {
            return false;
        }

        public void setupProjectSessionSettings(Project project, SessionSettings ss) {
        }

        public boolean supportsSettingsOverride() {
            return false;
        }

        public boolean supportsUnintegrate(Project project) {
            return false;
        }

        public void unintegrateProfiler(Project project) {
        }

        public float getProfilingOverhead(ProfilingSettings settings) {
            // Simply copy-pasted from org.netbeans.modules.profiler.AbstractProjectTypeProfiler
            // to fix #156000.

            // TODO: this code should be implemented in one place, PTPUtilities?

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
    };


    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /**
     * A constant indicating "Profile Project" action
     */
    int TARGET_PROFILE = 1;

    /**
     * A constant indicating "Profile File" action
     */
    int TARGET_PROFILE_SINGLE = 2;

    /**
     * A constant indicating "Profile Test" action
     */
    int TARGET_PROFILE_TEST = 3;

    /**
     * A constant indicating "Profile Single Test" action
     */
    int TARGET_PROFILE_TEST_SINGLE = 4;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // Returns JComponent that will be displayed in left bottom corner of STP and used for setting additional configuration
    // related to the concrete project type & project. Currently displayed only for Profile Project, not for Attach Profiler
    JComponent getAdditionalConfigurationComponent(Project project);

    /**
     * The project type implementation returns true for projects it can profile using attach and false for those it can
     * not. In most cases this will be based on the project type.
     *
     * @param project the project in question
     * @return true if this ProjectTypeProfiler can profile this project using attach, false otherwise
     */
    boolean isAttachSupported(Project project);

    /**
     * Gives the project type profiler ability to influence the root methods used for CPU Entire application profiling
     *
     * @param project           The project that is going to be profiled or null in case of global attach
     * @param profiledClassFile null in case project's main class is used, or a specific file in case of profile-single
     * @param profileUnderlyingFramework    true/false determining if root methods will be automatically determined
     *                                      from the project (false) or first/other method will be selected as a root
     *                                      method (true). This can be useful if AppletViewer / AppServer / IDE(Platform)
     *                                      code should be profiled as well before executing any method of the project.
     * @return SourceCodeSelection with the root methods
       //   * @see #getEntireCPUProfilingCheckBoxText
     */
    ClientUtils.SourceCodeSelection[] getDefaultRootMethods(Project project, FileObject profiledClassFile,
                                                            boolean profileUnderlyingFramework, String[][] projectPackagesDescr);

    /**
     * Called to check if specified FileObject can be profiled (And thus Profile File action enabled).
     * For J2SE, this will check if the class has a main method or is an applet.
     *
     * @param project The project to be profiled
     * @param fo The FileObject to check
     * @return true if the specified FileObject can be profiled, false otherwise.
     */
    boolean isFileObjectSupported(Project project, FileObject fo);

    // Returns list of instrumentation filters defined by the ProjectTypeProfiler (related to concrete project type)
    // These filters are typically just placeholders useds for Instr. filter combo, real filter is computed in computePredefinedInstrumentationFilter
    List<SimpleFilter> getPredefinedInstrumentationFilters(Project project);

    /**
     * Called by the IDE to determine which target to call in the project's build script to perform the specified
     * profiling type
     *
     * @param project           The project to be profiled
     * @param buildScript       The project's build script
     * @param type              The type of profiling being performed
     * @param profiledClassFile null in case project's main class is used, or a specific file in case of profile-single
     * @return a name of the target to use, or null if there is no target to call and profiling should not procees
     */
    String getProfilerTargetName(Project project, FileObject buildScript, int type, FileObject profiledClassFile);

    float getProfilingOverhead(ProfilingSettings settings);

    /**
     * The project type implementation returns true for projects it can profile and false for those it can not.
     * In most cases this will be based on the project type.
     *
     * @param project the project in question
     * @return true if this ProjectTypeProfiler can profile this project, false otherwise
     */
    boolean isProfilingSupported(Project project);

    /**
     * Called by the IDE to obtain the build script for the project
     *
     * @param project The project for which we are looking up the build script
     * @return the FileObject of the build script or null if not found
     */
    FileObject getProjectBuildScript(Project project);

    /**
     * Provide JavaPlatform selected in the project, or null if the project type does not have a Java selector.
     *
     * @param project The project that is going to be profiled
     * @return JavaPlatform selected in the project for execution or null if not applicable
     */
    JavaPlatform getProjectJavaPlatform(Project project);

    // Customizer for profiling settings, DefaultSettingsConfigurator.SHARED_INSTANCE is typically perfect fit
    SelectProfilingTask.SettingsConfigurator getSettingsConfigurator();

    /**
     * This method will be called before the profiling starts to check if the project is in a correct state.
     * If not, the ProjectTypeProfiler implementation should notify the user and return false.
     *
     * @param project           The project to be profiled
     * @param profiledClassFile A FileObject of a class to be profiled (in case of profiling specific class) or
     *                          null if profiling project's main class
     * @return true if the project and selected class are in a state that supports profiling, false otherwise
     */
    boolean checkProjectCanBeProfiled(Project project, FileObject profiledClassFile);

    /**
     * This method allows the ProjectTypeProfiler to prepare the project for profiling. This typically involves creating
     * a profiler-build-impl.xml build script in the nbbuild directory of the project and importing it from the user's
     * build.xml script. This method is called each time before any of the ant targets related to profiling are invoked.
     *
     * @param project The project to be modified
     * @return true if the modification was performed succesfully and the profiling should continue, false otherwise
     */
    boolean checkProjectIsModifiedForProfiler(Project project);

    // Computes real value of predefined instrumentation filter for given placeholder filter
    SimpleFilter computePredefinedInstrumentationFilter(Project project, SimpleFilter predefinedInstrFilter,
                                                        String[][] projectPackagesDescr);

    // Computes all packages present in project ([0]) and subprojects ([1]) sources, used for computing default root methods and predefined instrumentation filters
    void computeProjectPackages(Project project, boolean subprojects, String[][] storage);

    /**
     * Called right before the Ant target (profile or profile-single) is called to perform the profiling.
     * This gives the ProfilerType the ability to setup any properties for the target (and tasks it calls) to use,
     * such as "profile.class" property for the nbprofile task, "javac.includes" for compile-single, etc.
     *
     * @param props             The properties to be used by Ant to run the target
     * @param project           The project that is going to be profiled
     * @param profiledClassFile null in case project's main class is used, or a specific file in case of profile-single
     */
    void configurePropertiesForProfiling(Properties props, Project project, FileObject profiledClassFile);

    /**
     * Called before profiling starts to give the project support the ability to setup SessionSettings context
     * from the Project
     *
     * @param project The project to be profiled
     * @param ss      The SessionSettings representing the project context
     */
    void setupProjectSessionSettings(Project project, SessionSettings ss);

    /**
     * The ProjectTypeProfiler should return true if it is capable of changing the working directory, Java platform and
     * VM arguments of the profiled application. False indicates that it cannot be changed, and the overriding should be
     * disabled in the GUI.
     *
     * @return true if the ProjectTypeProfiler can force the project to use customized working dir, Java platform and
     *              JVM arguments.
     *
     * @see org.netbeans.lib.profiler.common.ProfilingSettings#getOverrideGlobalSettings()
     * @see org.netbeans.lib.profiler.common.ProfilingSettings#getWorkingDir()
     * @see org.netbeans.lib.profiler.common.ProfilingSettings#getJavaPlatformName()
     * @see org.netbeans.lib.profiler.common.ProfilingSettings#getJVMArgs()
     */
    boolean supportsSettingsOverride();

    boolean supportsUnintegrate(Project project);
    
    /**
     * Returns true when the ProjectTypeProfiler itself is able to start the profiling session and doesn't rely on the underlying (Ant-based) framework.
     * Currently used for Maven support.
     * 
     * @param project Project to be profiled
     * @param profiledClassFile File to be profiled or null if profiling entire Project
     * @param isTest True if profiledClassFile is test
     * @param properties Properties containing settings for the upcoming profiling session
     * @return true when the ProjectTypeProfiler is able to start the profiling session.
     */
    public boolean startProfilingSession(Project project, FileObject profiledClassFile, boolean isTest, Properties properties);

    void unintegrateProfiler(Project project);
}
