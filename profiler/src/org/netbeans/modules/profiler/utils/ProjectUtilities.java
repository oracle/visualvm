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

import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.project.*;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.lib.profiler.common.filters.SimpleFilter;
import org.netbeans.modules.profiler.heapwalk.HeapDumpWatch;
import org.netbeans.modules.profiler.spi.ProjectTypeProfiler;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.SubprojectProvider;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.awt.MouseUtils;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.w3c.dom.Element;
import java.awt.Dialog;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;


/**
 * Utilities for interaction with the NetBeans IDE, specifically related to Projects
 *
 * @author Ian Formanek
 * @deprecated 
 */
@Deprecated
public final class ProjectUtilities {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(ProjectUtilities.class.getName());
    public static final String PROFILER_NAME_SPACE = "http://www.netbeans.org/ns/profiler/1"; // NOI18N

    // -----
    // I18N String constants
    private static final String UNKNOWN_PROJECT_STRING = NbBundle.getMessage(ProjectUtilities.class,
                                                                             "ProjectUtilities_UnknownProjectString"); // NOI18N
    private static final String FAILED_CREATE_CLASSES_DIR_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                    "ProjectUtilities_FailedCreateClassesDirMsg"); // NOI18N
    private static final String FAILED_GENERATE_APPLET_FILE_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                      "ProjectUtilities_FailedGenerateAppletFileMsg"); // NOI18N
    private static final String FAILED_COPY_APPLET_FILE_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                  "ProjectUtilities_FailedCopyAppletFileMsg"); // NOI18N
    private static final String FAILED_CREATE_OUTPUT_FOLDER_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                      "ProjectUtilities_FailedCreateOutputFolderMsg"); // NOI18N
    private static final String PROFILE_PROJECT_CLASSES_STRING = NbBundle.getMessage(ProjectUtilities.class,
                                                                                     "ProjectUtilities_ProfileProjectClassesString"); // NOI18N
    private static final String PROFILE_PROJECT_SUBPROJECT_CLASSES_STRING = NbBundle.getMessage(ProjectUtilities.class,
                                                                                                "ProjectUtilities_ProfileProjectSubprojectClassesString"); // NOI18N
    private static final String PROFILER_WILL_BE_UNINTEGRATED_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                        "ProjectUtilities_ProfilerWillBeUnintegratedMsg"); // NOI18N
    private static final String PROFILER_ISNT_INTEGRATED_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                   "ProjectUtilities_ProfilerIsntIntegratedMsg"); // NOI18N
    private static final String RENAMING_BUILD_FAILED_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                "ProjectUtilities_RenamingBuildFailedMsg"); // NOI18N
    private static final String REMOVING_BUILD_FAILED_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                "ProjectUtilities_RemovingBuildFailedMsg"); // NOI18N
    private static final String REMOVING_DATA_FAILED_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                               "ProjectUtilities_RemovingDataFailedMsg"); // NOI18N
    private static final String UNINTEGRATION_ERRORS_OCCURED_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                       "ProjectUtilities_UnintegrationErrorsOccuredMsg"); // NOI18N
    private static final String UNINTEGRATION_SUCCESSFUL_MSG = NbBundle.getMessage(ProjectUtilities.class,
                                                                                   "ProjectUtilities_UnintegrationSuccessfulMsg"); // NOI18N
                                                                                                                                   // -----
    public static final SimpleFilter FILTER_PROJECT_ONLY = new SimpleFilter(PROFILE_PROJECT_CLASSES_STRING,
                                                                            SimpleFilter.SIMPLE_FILTER_INCLUSIVE,
                                                                            "{$project.classes.only}"); // NOI18N
    public static final SimpleFilter FILTER_PROJECT_SUBPROJECTS_ONLY = new SimpleFilter(PROFILE_PROJECT_SUBPROJECT_CLASSES_STRING,
                                                                                        SimpleFilter.SIMPLE_FILTER_INCLUSIVE,
                                                                                        "{$project.subprojects.classes.only}"); // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Provides list of all ProjectTypeProfiler SPI implementations currently installed.
     *
     * @return Collection<ProjectTypeProfiler> of all ProjectTypeProfilers currently installed
     */
    public static Collection<?extends ProjectTypeProfiler> getAllProjectTypeProfilers() {
        final Lookup lookup = Lookup.getDefault();
        final Lookup.Template<ProjectTypeProfiler> template = new Lookup.Template<ProjectTypeProfiler>(ProjectTypeProfiler.class);
        final Lookup.Result<ProjectTypeProfiler> result = lookup.lookup(template);

        if (result == null) {
            return new ArrayList<ProjectTypeProfiler>();
        }

        return result.allInstances();
    }

    public static ClientUtils.SourceCodeSelection[] getClassConstructors(FileObject classFile) {
        return SourceUtils.getClassConstructors(classFile);

        //    JavaClass fileClass = MDRUtils.getToplevelClass(classFile);
        //    if (fileClass == null) return new ClientUtils.SourceCodeSelection[0];
        //
        //    Constructor[] constructors = MDRUtils.getConstructors(fileClass);
        //    ClientUtils.SourceCodeSelection[] classConstructors = new ClientUtils.SourceCodeSelection[constructors.length];
        //    for (int i = 0; i < constructors.length; i++) {
        //      classConstructors[i] = new ClientUtils.SourceCodeSelection(MDRUtils.getVMClassName(fileClass), "<init>", MDRUtils.getSignature(constructors[i])); // NOI18N
        //    }
        //
        //    return classConstructors;
        //    return new ClientUtils.SourceCodeSelection[0];
    }

    public static ClasspathInfo getClasspathInfo(final Project project) {
        return getClasspathInfo(project, true);
    }

    public static ClasspathInfo getClasspathInfo(final Project project, final boolean includeSubprojects) {
        return getClasspathInfo(project, includeSubprojects, true, true);
    }

    public static ClasspathInfo getClasspathInfo(final Project project, final boolean includeSubprojects,
                                                 final boolean includeSources, final boolean includeLibraries) {
        FileObject[] sourceRoots = getSourceRoots(project, includeSubprojects);
        Set<FileObject> srcRootSet = new HashSet<FileObject>(sourceRoots.length);
        java.util.List<FileObject> compileRootList = new ArrayList<FileObject>(sourceRoots.length);
        java.util.List<URL> urlList = new ArrayList<URL>();

        srcRootSet.addAll(Arrays.asList(sourceRoots));

        if (((sourceRoots == null) || (sourceRoots.length == 0)) && !includeSubprojects) {
            sourceRoots = getSourceRoots(project, true);
        }

        final ClassPath cpEmpty = ClassPathSupport.createClassPath(new FileObject[0]);

        if (sourceRoots.length == 0) {
            return null; // fail early
        }

        ClassPath cpSource = ClassPathSupport.createClassPath(sourceRoots);

        // cleaning up compile classpatth; we need to get rid off all project's class file references in the classpath
        ClassPath cpCompile = ClassPath.getClassPath(sourceRoots[0], ClassPath.COMPILE);

        for (ClassPath.Entry entry : cpCompile.entries()) {
            SourceForBinaryQuery.Result rslt = SourceForBinaryQuery.findSourceRoots(entry.getURL());
            FileObject[] roots = rslt.getRoots();

            if ((roots == null) || (roots.length == 0)) {
                urlList.add(entry.getURL());
            }
        }

        cpCompile = ClassPathSupport.createClassPath(urlList.toArray(new URL[urlList.size()]));

        return ClasspathInfo.create(includeLibraries ? ClassPath.getClassPath(sourceRoots[0], ClassPath.BOOT) : cpEmpty,
                                    includeLibraries ? cpCompile : cpEmpty, includeSources ? cpSource : cpEmpty);
    }

    public static String getDefaultPackageClassNames(Project project) {
        Collection<String> classNames = SourceUtils.getDefaultPackageClassNames(project);
        StringBuffer classNamesBuf = new StringBuffer();

        for (String className : classNames) {
            classNamesBuf.append(className).append(" "); //NOI18N
        }

        return classNamesBuf.toString();
    }

    // Returns true if the project contains any Java sources (does not check subprojects!)
    public static boolean isJavaProject(Project project) {
        if (project == null) return false;
        
        Sources sources = ProjectUtils.getSources(project);
        SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);

        return sourceGroups.length > 0;
    }

    /**
     * @return The current main project or null if no project is main.
     */
    public static Project getMainProject() {
        return OpenProjects.getDefault().getMainProject();
    }

    public static Project[] getOpenedProjects() {
        return OpenProjects.getDefault().getOpenProjects();
    }

    public static Project[] getOpenedProjectsForAttach() {
        Project[] projects = getOpenedProjects();
        ArrayList<Project> projectsArray = new ArrayList(projects.length);

        for (int i = 0; i < projects.length; i++) {
            if (ProjectUtilities.isProjectTypeSupportedForAttach(projects[i])) {
                projectsArray.add(projects[i]);
            }
        }

        return projectsArray.toArray(new Project[projectsArray.size()]);
    }

    public static FileObject getOrCreateBuildFolder(Project project, String buildDirProp) {
        FileObject buildDir = FileUtil.toFileObject(PropertyUtils.resolveFile(FileUtil.toFile(project.getProjectDirectory()),
                                                                              buildDirProp));

        if (buildDir == null) {
            try {
                // TODO: if buildDirProp is absolute, relativize via PropertyUtils
                buildDir = FileUtil.createFolder(project.getProjectDirectory(), buildDirProp);
            } catch (IOException e) {
                ErrorManager.getDefault()
                            .annotate(e, MessageFormat.format(FAILED_CREATE_OUTPUT_FOLDER_MSG, new Object[] { e.getMessage() }));
                ErrorManager.getDefault().notify(ErrorManager.ERROR, e);

                return null;
            }
        }

        return buildDir;
    }

    public static boolean isProfilerIntegrated(Project project) {
        Element e = ProjectUtils.getAuxiliaryConfiguration(project)
                           .getConfigurationFragment("data", ProjectUtilities.PROFILER_NAME_SPACE, false); // NOI18N

        return e != null;

        // TODO: Should check for obsolete versions (currently commented below)

        //    if (e != null) {
        //      String storedVersion = e.getAttribute("version"); // NOI18N
        //      if (storedVersion.equals("0.9.1")) return true; // NOI18N
        //    }
        //
        //    return false;
    }

    public static float getProfilingOverhead(ProfilingSettings settings) {
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

    public static String getProjectBuildScript(final Project project) {
        final FileObject buildFile = findBuildFile(project);
        if (buildFile == null) {
            return null;
        }

        RandomAccessFile file = null;
        byte[] data = null;

        try {
            file = new RandomAccessFile(FileUtil.toFile(buildFile), "r");
            data = new byte[(int) buildFile.getSize()];
            file.readFully(data);
        } catch (FileNotFoundException e2) {
            ProfilerLogger.log(e2);

            return null;
        } catch (IOException e2) {
            ProfilerLogger.log(e2);

            return null;
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (IOException e2) {
                    ProfilerLogger.log(e2);
                }
            }
        }

        try {
            return new String(data, "UTF-8" //NOI18N
            ); // According to Issue 65557, build.xml uses UTF-8, not default encoding!
        } catch (UnsupportedEncodingException ex) {
            ErrorManager.getDefault().notify(ErrorManager.ERROR, ex);

            return null;
        }
    }

    public static FileObject findBuildFile(final Project project) {
        FileObject buildFile = project.getProjectDirectory().getFileObject("build.xml"); //NOI18N
        if (buildFile == null) {
            Properties props = org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities.getProjectProperties(project);
            String buildFileName = props.getProperty("buildfile"); // NOI18N
            if (buildFileName != null) {
                buildFile = project.getProjectDirectory().getFileObject(buildFileName);
            }
        }
        return buildFile;
    }

    public static java.util.List<SimpleFilter> getProjectDefaultInstrFilters(Project project) {
        java.util.List<SimpleFilter> v = new ArrayList<SimpleFilter>();

        if (ProjectUtils.getSources(project).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA).length > 0) {
            v.add(FILTER_PROJECT_ONLY);
        }

        if (hasSubprojects(project)) {
            v.add(FILTER_PROJECT_SUBPROJECTS_ONLY);
        }

        return v;
    }

    public static ClientUtils.SourceCodeSelection[] getProjectDefaultRoots(Project project, String[][] projectPackagesDescr) {
        computeProjectPackages(project, true, projectPackagesDescr);

        ClientUtils.SourceCodeSelection[] ret = new ClientUtils.SourceCodeSelection[projectPackagesDescr[1].length];

        for (int i = 0; i < projectPackagesDescr[1].length; i++) {
            if ("".equals(projectPackagesDescr[1][i])) { //NOI18N
                ret[i] = new ClientUtils.SourceCodeSelection("", "", ""); //NOI18N
            } else {
                ret[i] = new ClientUtils.SourceCodeSelection(projectPackagesDescr[1][i] + ".", "", ""); //NOI18N
            }
        }

        return ret;
    }

    public static Project getProjectForBuildScript(String fileName) {
        FileObject projectFO = FileUtil.toFileObject(new File(fileName));

        while (projectFO != null) {
            try {
                if (projectFO.isFolder()) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Trying: " + projectFO); //NOI18N
                    }

                    Project p = ProjectManager.getDefault().findProject(projectFO);

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Got: " + ((p != null) ? getProjectName(p) : null)); //NOI18N
                    }

                    if (p != null) {
                        return p;
                    }
                }

                projectFO = projectFO.getParent();
            } catch (IOException e) {
                ProfilerLogger.severe("Got: IOException : " + e.getMessage()); //NOI18N
            }
        }

        return null;
    }

    public static Icon getProjectIcon(Project project) {
        ProjectInformation info = project.getLookup().lookup(ProjectInformation.class);

        if (info == null) {
            return new ImageIcon();
        } else {
            return info.getIcon();
        }
    }

    public static String getProjectName(Project project) {
        ProjectInformation info = project.getLookup().lookup(ProjectInformation.class);

        if (info == null) {
            return UNKNOWN_PROJECT_STRING;
        } else {
            return info.getDisplayName();
        }
    }

    /**
     * Provides a list of all fully qualified packages within the project that contain some sources.
     *
     * @param project The project
     * @return a list of fully qualified String names of packages within the provided project that contain sources
     */
    public static String[] getProjectPackages(final Project project) {
        ArrayList<String> packages = new ArrayList<String>();

        for (FileObject root : getSourceRoots(project, true)) {
            addSubpackages(packages, "", root); //NOI18N
        }

        return packages.toArray(new String[0]);
    }

    /**
     * Checks if ProjectTypeProfiler capable of profiling the provided project exists and if so, returns it.
     *
     * @param project The project
     * @return ProjectTypeProfiler capable of profiling the project or null if none of the installed PTPs supports it
     */
    public static ProjectTypeProfiler getProjectTypeProfiler(final Project project) {
        if (project == null) {
            return ProjectTypeProfiler.DEFAULT; // global attach
        }

        final Collection c = getAllProjectTypeProfilers();

        for (Iterator i = c.iterator(); i.hasNext();) {
            final ProjectTypeProfiler ptp = (ProjectTypeProfiler) i.next();

            if (ptp.isProfilingSupported(project)) {
                return ptp; // project type profiler for provided project
            }
        }

        return ProjectTypeProfiler.DEFAULT; // unsupported project
    }

    /**
     * @return true if there is a ProjectTypeProfilers capable of profiling the provided project using the Profile (Main) Project action, false otherwise.
     */
    public static boolean isProjectTypeSupported(final Project project) {
        ProjectTypeProfiler ptp = getProjectTypeProfiler(project);

        if (ptp.isProfilingSupported(project)) {
            return true;
        }

        return hasAction(project, "profile"); //NOI18N
    }

    /**
     * @return true if the project can be used for profiling using the Attach Profiler action (== Java project), false otherwise.
     */
    public static boolean isProjectTypeSupportedForAttach(Project project) {
        return getProjectTypeProfiler(project).isAttachSupported(project);
    }

    /**
     * Checks which of the provided source roots contains the given file.
     *
     * @param roots A list of source roots
     * @param file A FileObject to look for
     * @return The source roots that contains the specified file or null if none of them contain it.
     */
    public static FileObject getRootOf(final FileObject[] roots, final FileObject file) {
        FileObject srcDir = null;

        for (int i = 0; i < roots.length; i++) {
            if (FileUtil.isParentOf(roots[i], file)) {
                srcDir = roots[i];

                break;
            }
        }

        return srcDir;
    }

    public static Project[] getSortedProjects(Project[] projects) {
        ArrayList projectsArray = new ArrayList(projects.length);

        for (int i = 0; i < projects.length; i++) {
            projectsArray.add(projects[i]);
        }

        try {
            Collections.sort(projectsArray,
                             new Comparator() {
                    public int compare(Object o1, Object o2) {
                        Project p1 = (Project) o1;
                        Project p2 = (Project) o2;

                        return ProjectUtils.getInformation(p1).getDisplayName().toLowerCase()
                                           .compareTo(ProjectUtils.getInformation(p2).getDisplayName().toLowerCase());
                    }
                });
        } catch (Exception e) {
            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage()); // just in case ProjectUtils doesn't provide expected information
        }

        ;

        projectsArray.toArray(projects);

        return projects;
    }

    /**
     * Provides a list of source roots for the given project.
     *
     * @param project The project
     * @return an array of FileObjects that are the source roots for this project
     */
    public static FileObject[] getSourceRoots(final Project project) {
        return getSourceRoots(project, true);
    }

    /**
     * Provides a list of source roots for the given project.
     *
     * @param project The project
     * @param traverse Include subprojects
     * @return an array of FileObjects that are the source roots for this project
     */
    public static FileObject[] getSourceRoots(final Project project, final boolean traverse) {
        Set<FileObject> set = new HashSet<FileObject>();
        Set<Project> projects = new HashSet<Project>();

        projects.add(project);
        getSourceRoots(project, traverse, projects, set);

        return set.toArray(new FileObject[set.size()]);
    }

    public static boolean backupBuildScript(final Project project) {
        final FileObject buildFile = findBuildFile(project);
        final FileObject buildBackupFile = project.getProjectDirectory().getFileObject("build-before-profiler.xml"); //NOI18N

        if (buildFile != null && buildBackupFile != null) {
            try {
                buildBackupFile.delete();
            } catch (IOException e) {
                e.printStackTrace(System.err);

                return false;

                // cannot delete already existing backup
            }
        }

        try {
            buildFile.copy(project.getProjectDirectory(), "build-before-profiler", "xml"); //NOI18N
        } catch (IOException e1) {
            ProfilerLogger.log(e1);

            return false;
        }

        return true;
    }

    public static SimpleFilter computeProjectOnlyInstrumentationFilter(Project project, SimpleFilter predefinedInstrFilter,
                                                                       String[][] projectPackagesDescr) {
        // TODO: projectPackagesDescr[1] should only contain packages from subprojects, currently contains also toplevel project packages
        if (FILTER_PROJECT_ONLY.equals(predefinedInstrFilter)) {
            computeProjectPackages(project, false, projectPackagesDescr);

            StringBuffer projectPackages = new StringBuffer();

            for (int i = 0; i < projectPackagesDescr[0].length; i++) {
                projectPackages.append("".equals(projectPackagesDescr[0][i]) ? getDefaultPackageClassNames(project)
                                                                             : (projectPackagesDescr[0][i] + ". ")); //NOI18N
            }

            return new SimpleFilter(PROFILE_PROJECT_CLASSES_STRING, SimpleFilter.SIMPLE_FILTER_INCLUSIVE,
                                    projectPackages.toString().trim());
        } else if (FILTER_PROJECT_SUBPROJECTS_ONLY.equals(predefinedInstrFilter)) {
            computeProjectPackages(project, true, projectPackagesDescr);

            StringBuffer projectPackages = new StringBuffer();

            for (int i = 0; i < projectPackagesDescr[1].length; i++) {
                projectPackages.append("".equals(projectPackagesDescr[1][i]) ? getDefaultPackageClassNames(project)
                                                                             : (projectPackagesDescr[1][i] + ". ")); //NOI18N // TODO: default packages need to be processed also for subprojects!!!
            }

            return new SimpleFilter(PROFILE_PROJECT_SUBPROJECT_CLASSES_STRING, SimpleFilter.SIMPLE_FILTER_INCLUSIVE,
                                    projectPackages.toString().trim());
        }

        return null;
    }

    public static void computeProjectPackages(final Project project, boolean subprojects, String[][] storage) {
        if ((storage == null) || (storage.length != 2)) {
            throw new IllegalArgumentException("Storage must be a non-null String[2][] array"); // NOI18N
        }

        if (storage[0] == null) {
            ArrayList<String> packages1 = new ArrayList<String>();

            for (FileObject root : getSourceRoots(project, false)) {
                addSubpackages(packages1, "", root); //NOI18N
            }

            storage[0] = packages1.toArray(new String[0]);
        }

        if (subprojects && (storage[1] == null)) {
            FileObject[] srcRoots2 = getSourceRoots(project, true); // TODO: should be computed based on already known srcRoots1
            ArrayList<String> packages2 = new ArrayList<String>();

            for (FileObject root : srcRoots2) {
                addSubpackages(packages2, "", root); //NOI18N
            }

            storage[1] = packages2.toArray(new String[0]);
        }
    }

    public static URL copyAppletHTML(Project project, PropertyEvaluator props, FileObject profiledClassFile, String value) {
        try {
            String buildDirProp = props.getProperty("build.dir"); //NOI18N

            FileObject buildFolder = getOrCreateBuildFolder(project, buildDirProp);

            FileObject htmlFile = null;
            htmlFile = profiledClassFile.getParent().getFileObject(profiledClassFile.getName(), "html"); //NOI18N

            if (htmlFile == null) {
                htmlFile = profiledClassFile.getParent().getFileObject(profiledClassFile.getName(), "HTML"); //NOI18N
            }

            if (htmlFile == null) {
                return null;
            }

            FileObject existingFile = buildFolder.getFileObject(htmlFile.getName(), htmlFile.getExt());

            if (existingFile != null) {
                existingFile.delete();
            }

            htmlFile.copy(buildFolder, profiledClassFile.getName(), value).getURL();

            if (htmlFile != null) {
                return htmlFile.getURL();
            }
        } catch (IOException e) {
            ErrorManager.getDefault()
                        .annotate(e, MessageFormat.format(FAILED_COPY_APPLET_FILE_MSG, new Object[] { e.getMessage() }));
            ErrorManager.getDefault().notify(ErrorManager.ERROR, e);

            return null;
        }

        return null;
    }

    public static void fetchSubprojects(final Project project, final Set<Project> projects) {
        // process possible subprojects
        SubprojectProvider spp = project.getLookup().lookup(SubprojectProvider.class);

        if (spp != null) {
            for (Iterator it = spp.getSubprojects().iterator(); it.hasNext();) {
                Project p = (Project) it.next();

                if (p != null) // NOTE: workaround for Issue 121157 for NetBeans 6.0 FCS branch, will be removed in trunk!!!
                    if (projects.add(p)) {
                        fetchSubprojects(p, projects);
                    }
            }
        }
    }

    /**
     * Will find
     * Copied from JUnit module implementation in 4.1 and modified
     */
    public static FileObject findTestForFile(final FileObject selectedFO) {
        if ((selectedFO == null) || !selectedFO.getExt().equalsIgnoreCase("java")) {
            return null; // NOI18N
        }

        ClassPath cp = ClassPath.getClassPath(selectedFO, ClassPath.SOURCE);

        if (cp == null) {
            return null;
        }

        FileObject packageRoot = cp.findOwnerRoot(selectedFO);

        if (packageRoot == null) {
            return null; // not a file in the source dirs - e.g. generated class in web app
        }

        URL[] testRoots = UnitTestForSourceQuery.findUnitTests(packageRoot);
        FileObject fileToOpen = null;

        for (int j = 0; j < testRoots.length; j++) {
            fileToOpen = findUnitTestInTestRoot(cp, selectedFO, testRoots[j]);

            if (fileToOpen != null) {
                return fileToOpen;
            }
        }

        return null;
    }

    public static URL generateAppletHTML(Project project, PropertyEvaluator props, FileObject profiledClassFile) {
        String buildDirProp = props.getProperty("build.dir"); //NOI18N
        String classesDirProp = props.getProperty("build.classes.dir"); //NOI18N
        String activePlatformName = props.getProperty("platform.active"); //NOI18N

        FileObject buildFolder = getOrCreateBuildFolder(project, buildDirProp);
        FileObject classesDir = FileUtil.toFileObject(PropertyUtils.resolveFile(FileUtil.toFile(project.getProjectDirectory()),
                                                                                classesDirProp));

        if (classesDir == null) {
            try {
                classesDir = FileUtil.createFolder(project.getProjectDirectory(), classesDirProp);
            } catch (IOException e) {
                ErrorManager.getDefault()
                            .annotate(e, MessageFormat.format(FAILED_CREATE_CLASSES_DIR_MSG, new Object[] { e.getMessage() }));
                ErrorManager.getDefault().notify(ErrorManager.ERROR, e);

                return null;
            }
        }

        try {
            return AppletSupport.generateHtmlFileURL(profiledClassFile, buildFolder, classesDir, activePlatformName);
        } catch (FileStateInvalidException e) {
            ErrorManager.getDefault()
                        .annotate(e, MessageFormat.format(FAILED_GENERATE_APPLET_FILE_MSG, new Object[] { e.getMessage() }));
            ErrorManager.getDefault().notify(ErrorManager.ERROR, e);

            return null;
        }
    }

    public static boolean hasAction(Project project, String actionName) {
        ActionProvider ap = project.getLookup().lookup(ActionProvider.class);

        if (ap == null) {
            return false; // return false if no ActionProvider available
        }

        String[] actions = ap.getSupportedActions();

        for (int i = 0; i < actions.length; i++) {
            if ((actions[i] != null) && actionName.equals(actions[i])) {
                return true;
            }
        }

        return false;
    }

    public static void invokeAction(Project project, String s) {
        ActionProvider ap = project.getLookup().lookup(ActionProvider.class);

        if (ap == null) {
            return; // fail early
        }

        ap.invokeAction(s, Lookup.getDefault());
    }

    /**
     * Runs an target in Ant script with properties context.
     *
     * @param buildScript The build script to run the target from
     * @param target The name of target to run
     * @param props The properties context to run the task in
     * @return ExecutorTask to track the running Ant process
     */
    public static ExecutorTask runTarget(final FileObject buildScript, final String target, final Properties props) {
        try {
            String oomeenabled = props.getProperty(HeapDumpWatch.OOME_PROTECTION_ENABLED_KEY);

            if ((oomeenabled != null) && oomeenabled.equals("yes")) { // NOI18N
                HeapDumpWatch.getDefault().monitor(props.getProperty(HeapDumpWatch.OOME_PROTECTION_DUMPPATH_KEY));
            }

            return ActionUtils.runTarget(buildScript, new String[] { target }, props);
        } catch (IOException e) {
            Profiler.getDefault().notifyException(Profiler.EXCEPTION, e);
        }

        return null;
    }

    /**
     * Asks user for name of main class
     *
     * @param project     the project in question
     * @param mainClass   current main class
     * @param projectName the name of project
     * @param messageType type of dialog -1 when the main class is not set, -2 when the main class in not valid
     * @return true if user selected main class
     */
    public static String selectMainClass(Project project, String mainClass, String projectName, int messageType) {
        boolean canceled;
        final JButton okButton = new JButton(NbBundle.getMessage(MainClassWarning.class, "LBL_MainClassWarning_ChooseMainClass_OK") //NOI18N
        ); // NOI18N
        okButton.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(MainClassWarning.class,
                                                                                     "AD_MainClassWarning_ChooseMainClass_OK") //NOI18N
        ); // NOI18N

        // main class goes wrong => warning
        String message;

        switch (messageType) {
            case -1:
                message = MessageFormat.format(NbBundle.getMessage(MainClassWarning.class, "LBL_MainClassNotFound"),
                                               new Object[] {  // NOI18N
                    projectName });

                break;
            case -2:
                message = MessageFormat.format(NbBundle.getMessage(MainClassWarning.class, "LBL_MainClassWrong"),
                                               new Object[] {  // NOI18N
                    mainClass, projectName });

                break;
            default:
                throw new IllegalArgumentException();
        }

        final MainClassWarning panel = new MainClassWarning(message, getSourceRoots(project));
        Object[] options = new Object[] { okButton, DialogDescriptor.CANCEL_OPTION };

        panel.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (e.getSource() instanceof MouseEvent && MouseUtils.isDoubleClick(((MouseEvent) e.getSource()))) {
                        // click button and the finish dialog with selected class
                        okButton.doClick();
                    } else {
                        okButton.setEnabled(panel.getSelectedMainClass() != null);
                    }
                }
            });

        okButton.setEnabled(false);

        DialogDescriptor desc = new DialogDescriptor(panel,
                                                     NbBundle.getMessage(MainClassWarning.class, "CTL_MainClassWarning_Title",
                                                                         ProjectUtils.getInformation(project).getDisplayName() //NOI18N
        ), // NOI18N
                                                     true, options, options[0], DialogDescriptor.BOTTOM_ALIGN, null, null);
        desc.setMessageType(DialogDescriptor.INFORMATION_MESSAGE);

        Dialog dlg = DialogDisplayer.getDefault().createDialog(desc);
        dlg.setVisible(true);

        if (desc.getValue() != options[0]) {
            canceled = true;
        } else {
            mainClass = panel.getSelectedMainClass();
            canceled = false;
        }

        dlg.dispose();

        if (canceled) {
            return null;
        } else {
            return mainClass;
        }
    }

    public static void unintegrateProfiler(Project project) {
        String projectName = ProjectUtils.getInformation(project).getDisplayName();

        if (isProfilerIntegrated(project)) {
            if (ProfilerDialogs.notify(new NotifyDescriptor.Confirmation(MessageFormat.format(PROFILER_WILL_BE_UNINTEGRATED_MSG,
                                                                                                  new Object[] { projectName }),
                                                                             NotifyDescriptor.YES_NO_OPTION)) != NotifyDescriptor.YES_OPTION) {
                return; // cancelled by the user
            }
        } else {
            if (ProfilerDialogs.notify(new NotifyDescriptor.Confirmation(MessageFormat.format(PROFILER_ISNT_INTEGRATED_MSG,
                                                                                                  new Object[] { projectName }),
                                                                             NotifyDescriptor.YES_NO_OPTION)) != NotifyDescriptor.YES_OPTION) {
                return; // cancelled by the user
            }
        }

        boolean failed = false;
        StringBuilder exceptionsReport = new StringBuilder();

        // Move build-before-profiler.xml back to build.xml
        FileLock buildBackupFileLock = null;
        FileLock buildBackup2FileLock = null;

        try {
            final FileObject buildFile = findBuildFile(project); //NOI18N
            final FileObject buildBackupFile = project.getProjectDirectory().getFileObject("build-before-profiler.xml"); //NOI18N

            if (buildFile != null && (buildBackupFile != null && buildBackupFile.isValid())) {
                try {
                    buildBackupFileLock = buildBackupFile.lock();

                    if ((buildFile != null) && buildFile.isValid()) {
                        buildFile.delete();
                    }

                    buildBackupFile.rename(buildBackupFileLock, "build", "xml"); //NOI18N
                } catch (Exception e) {
                    failed = true;
                    exceptionsReport.append(MessageFormat.format(RENAMING_BUILD_FAILED_MSG, new Object[] { e.getMessage() }));
                    ProfilerLogger.log(e);
                }
            }
        } finally {
            if (buildBackupFileLock != null) {
                buildBackupFileLock.releaseLock();
            }
        }

        // Remove profiler-build-impl.xml
        FileLock buildImplFileLock = null;

        try {
            final FileObject buildImplFile = project.getProjectDirectory().getFileObject("nbproject")
                                                    .getFileObject("profiler-build-impl.xml"); //NOI18N

            try {
                if ((buildImplFile != null) && buildImplFile.isValid()) {
                    buildImplFile.delete();
                }
            } catch (Exception e) {
                failed = true;
                exceptionsReport.append(MessageFormat.format(REMOVING_BUILD_FAILED_MSG, new Object[] { e.getMessage() }));
                ProfilerLogger.log(e);
            }
        } finally {
            if (buildImplFileLock != null) {
                buildImplFileLock.releaseLock();
            }
        }

        // Remove data element from private/private.xml
        ProjectUtils.getAuxiliaryConfiguration(project).removeConfigurationFragment("data", PROFILER_NAME_SPACE, false); // NOI18N

        try {
            ProjectManager.getDefault().saveProject(project);
        } catch (Exception e) {
            failed = true;
            exceptionsReport.append(MessageFormat.format(REMOVING_DATA_FAILED_MSG, new Object[] { e.getMessage() }));
            ProfilerLogger.log(e);
        }

        if (failed) {
            Profiler.getDefault()
                    .displayError(MessageFormat.format(UNINTEGRATION_ERRORS_OCCURED_MSG,
                                                       new Object[] { exceptionsReport.toString() }));
        } else {
            Profiler.getDefault().displayInfo(MessageFormat.format(UNINTEGRATION_SUCCESSFUL_MSG, new Object[] { projectName }));
        }
    }

    private static void getSourceRoots(final Project project, final boolean traverse, Set<Project> projects, Set<FileObject> roots) {
        final Sources sources = ProjectUtils.getSources(project);

        for (SourceGroup sg : sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA)) {
            roots.add(sg.getRootFolder());
        }

        if (traverse) {
            // process possible subprojects
            SubprojectProvider spp = project.getLookup().lookup(SubprojectProvider.class);

            if (spp != null) {
                for (Project p : spp.getSubprojects()) {
                    if (projects.add(p)) {
                        getSourceRoots(p, traverse, projects, roots);
                    }
                }
            }
        }
    }

    /**
     * Copied from JUnit module implementation in 4.1 and modified
     */
    private static String getTestName(ClassPath cp, FileObject selectedFO) {
        String resource = cp.getResourceName(selectedFO, '/', false); //NOI18N
        String testName = null;

        if (selectedFO.isFolder()) {
            //find Suite for package
            testName = convertPackage2SuiteName(resource);
        } else {
            // find Test for class
            testName = convertClass2TestName(resource);
        }

        return testName;
    }

    // --- private part ----------------------------------------------------------------------------------------------------
    private static void addSubpackages(ArrayList<String> packages, String prefix, FileObject packageFO) {
        if (!packageFO.isFolder()) { // not a folder

            return;
        }

        FileObject[] children = packageFO.getChildren();

        // 1. check if there are java sources in this folder and if so, add to the list of packages
        if (!packages.contains(prefix)) { // already in there, skip this

            for (int i = 0; i < children.length; i++) {
                FileObject child = children[i];

                if (child.getExt().equals("java")) { //NOI18N
                    packages.add(prefix);

                    break;
                }
            }
        }

        // 2. recurse into subfolders
        for (int i = 0; i < children.length; i++) {
            FileObject child = children[i];

            if (child.isFolder()) {
                if ("".equals(prefix)) { //NOI18N
                    addSubpackages(packages, child.getName(), child);
                } else {
                    addSubpackages(packages, prefix + "." + child.getName(), child); //NOI18N
                }
            }
        }
    }

    /**
     * Copied from JUnit module implementation in 4.1 and modified
     * Hardcoded test name prefix/suffix.
     */
    private static String convertClass2TestName(String classFileName) {
        if ((classFileName == null) || "".equals(classFileName)) {
            return ""; //NOI18N
        }

        int index = classFileName.lastIndexOf('/'); //NOI18N
        String pkg = (index > -1) ? classFileName.substring(0, index) : ""; // NOI18N
        String clazz = (index > -1) ? classFileName.substring(index + 1) : classFileName;
        clazz = clazz.substring(0, 1).toUpperCase() + clazz.substring(1);

        if (pkg.length() > 0) {
            pkg += "/"; // NOI18N
        }

        return pkg + clazz + "Test"; // NOI18N
    }

    /**
     * Copied from JUnit module implementation in 4.1 and modified
     * Hardcoded test name prefix/suffix.
     */
    private static String convertPackage2SuiteName(String packageFileName) {
        if ((packageFileName == null) || "".equals(packageFileName)) {
            return ""; //NOI18N
        }

        int index = packageFileName.lastIndexOf('/'); //NOI18N
        String pkg = (index > -1) ? packageFileName.substring(index + 1) : packageFileName;
        pkg = pkg.substring(0, 1).toUpperCase() + pkg.substring(1);

        return packageFileName + "/" + pkg + "Test"; // NOI18N
    }

    /**
     * Copied from JUnit module implementation in 4.1 and modified
     */
    private static FileObject findUnitTestInTestRoot(ClassPath cp, FileObject selectedFO, URL testRoot) {
        ClassPath testClassPath = null;

        if (testRoot == null) { //no tests, use sources instead
            testClassPath = cp;
        } else {
            try {
                java.util.List<PathResourceImplementation> cpItems = new ArrayList<PathResourceImplementation>();
                cpItems.add(ClassPathSupport.createResource(testRoot));
                testClassPath = ClassPathSupport.createClassPath(cpItems);
            } catch (IllegalArgumentException ex) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                testClassPath = cp;
            }
        }

        String testName = getTestName(cp, selectedFO);

        return testClassPath.findResource(testName + ".java"); // NOI18N
    }

    private static boolean hasSubprojects(Project project) {
        SubprojectProvider spp = project.getLookup().lookup(SubprojectProvider.class);

        if (spp == null) {
            return false;
        }

        return spp.getSubprojects().size() > 0;
    }
}
