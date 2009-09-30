/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.SessionSettings;
import org.netbeans.lib.profiler.global.CalibrationDataFileIO;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.netbeans.modules.profiler.ProfilerModule;
import org.netbeans.modules.profiler.actions.JavaPlatformSelector;
import org.netbeans.modules.profiler.actions.ProfilingSupport;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.netbeans.modules.profiler.utils.ProjectUtilities;


/**
 * Ant task to start the NetBeans profiler profile action.
 * <p/>
 * Will put the profiler into listening mode, placing the port number into the "profiler.port" property.
 * The target app then should be started through the profiler agent passing it this port number.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class NBProfileDirectTask extends Task {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * Enumerated attribute with the values "asis", "add" and "remove".
     */
    public static class YesNoAuto extends EnumeratedAttribute {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String[] getValues() {
            return new String[] { "yes", "true", "no", "false", "auto" }; //NOI18N
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CALIBRATION_FAILED_MESSAGE = NbBundle.getMessage(ProfilerModule.class,
                                                                                 "ProfilerModule_CalibrationFailedMessage"); //NOI18N
                                                                                                                             // -----
    private static final int INTERACTIVE_AUTO = 0;
    private static final int INTERACTIVE_YES = 1;
    private static final int INTERACTIVE_NO = 2;
    private static final String DEFAULT_AGENT_JVMARGS_PROPERTY = "profiler.info.jvmargs.agent"; // NOI18N
    private static final String DEFAULT_JVM_PROPERTY = "profiler.info.jvm"; // NOI18N
    private static final String EXTRA_JVM_ARGS = "profiler.info.jvmargs"; // NOI18N
    private static final String EXTRA_RUN_ARGS = "run.args.extra"; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /**
     * Explicit classpath of the profiled process.
     */
    private Path classpath = null;
    private Path rootsPath = null;
    private String jvmArgsPrefix = ""; // NOI18N
    private String jvmArgsProperty = DEFAULT_AGENT_JVMARGS_PROPERTY;
    private String jvmProperty = DEFAULT_JVM_PROPERTY;
    private String mainClass = null;
    private int interactive = INTERACTIVE_AUTO;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setInteractive(YesNoAuto arg) {
        String value = arg.getValue();

        if (value.equals("auto")) { //NOI18N
            interactive = INTERACTIVE_AUTO;
        } else if (value.equals("yes") || value.equals("true")) { // NOI18N
            interactive = INTERACTIVE_YES;
        } else if (value.equals("no") || value.equals("false")) { // NOI18N
            interactive = INTERACTIVE_NO;
        }
    }

    public void setJvmArgsPrefix(String value) {
        jvmArgsPrefix = value;
    }

    public void setJvmArgsProperty(String value) {
        jvmArgsProperty = value;
    }

    public void setJvmProperty(String value) {
        jvmProperty = value;
    }

    // -- Properties -------------------------------------------------------------------------------------------------------
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    /**
     * "classpath" subelements, only one is allowed
     *
     * @param path the classpath
     */
    public void addClasspath(final Path path) {
        if (classpath != null) {
            throw new BuildException("Only one classpath subelement is supported"); //NOI18N
        }

        classpath = path;
    }

    /**
     * "classpath" subelements, only one is allowed
     *
     * @param path the classpath
     */
    public void addRootspath(final Path path) {
        if (rootsPath != null) {
            throw new BuildException("Only one classpath subelement is supported"); //NOI18N
        }

        rootsPath = path;
    }

    // -- Main methods -----------------------------------------------------------------------------------------------------
    public void execute() throws BuildException {
        // Settings are created this way:
        //   1. project context (stored as properties)
        //   2. profiling settings (configuration)
        //         - possibly override some of the settings from 1. if ps.getOverrideGlobalSettings() is set
        //   3. explicitely override anything and everything in the build script
        final Hashtable props = getProject().getProperties();
        ProfilingSettings ps = new ProfilingSettings();
        final SessionSettings ss = new SessionSettings();
        String projectDir = (String) props.get("profiler.info.project.dir"); //NOI18N
        String singleFile = (String) props.get("profiler.info.single.file"); //NOI18N

        boolean initializedInteractively = false;

        if ((props.get(DEFAULT_AGENT_JVMARGS_PROPERTY) == null) || (interactive == INTERACTIVE_YES)) {
            if (interactive != INTERACTIVE_NO) {
                projectDir = initializeInteractively(ps, ss);
                initializedInteractively = true;
            }
        }

        if (!initializedInteractively) {
            if (props.get(DEFAULT_AGENT_JVMARGS_PROPERTY) == null) {
                throw new BuildException("Missing context for nbprofiledirect task.\n" //NOI18N
                                         + "Please set the \"interactive\" attribute to \"true\" or set the required properties." //NOI18N
                                         );
            }

            if (!DEFAULT_AGENT_JVMARGS_PROPERTY.equals(jvmArgsProperty) || !"".equals(jvmArgsPrefix)) { //NOI18N

                String args = " " + (String) props.get(EXTRA_JVM_ARGS); // get the extra JVM args
                                                                        // reformat the string to the form suitable for starting the NB platform

                args = args.replaceAll("\\s+(\\-)", " -J$1"); // NOI18N

                String origArgs = getProject().getProperty(EXTRA_RUN_ARGS);
                origArgs = (origArgs != null) ? (" " + origArgs + " ") : ""; // NOI18N

                getProject().setProperty("run.args.extra", origArgs + args); // merge the profiler extra JVM args with the platform extra JVM args

                String usedAgentJvmArgs = jvmArgsPrefix + props.get(DEFAULT_AGENT_JVMARGS_PROPERTY);
                getProject().setProperty(jvmArgsProperty, usedAgentJvmArgs);
                getProject().log("Profiler agent JVM arguments: " + usedAgentJvmArgs, Project.MSG_VERBOSE); //NOI18N
                getProject().log("Profiler agent JVM arguments stored in property " + jvmArgsProperty, Project.MSG_INFO); //NOI18N
                getProject().log("Extra JVM arguments: " + getProject().getProperty("run.args.extra")); // NOI18N
            }

            // 2. process parameters passed via Properties
            ps.load(props);
            ss.load(props);

            // get correct working directory available only at runtime (not from ProjectTypeProfiler!)
            String projectWorkDir = (String) props.get("work.dir"); // NOI18N
            ss.setWorkingDir(((projectWorkDir != null) && !"".equals(projectWorkDir.trim())) ? projectWorkDir
                                                                                             : System.getProperty("user.dir")); // NOI18N
        }

        // Correctly setup working directory
        String profilerInfoDir = (String) props.get("profiler.info.dir"); // NOI18N

        if (!initializedInteractively || (profilerInfoDir == null /* means that WD wasn't set in interactive setup */)
                || !"".equals(profilerInfoDir.trim())) { // NOI18N

            String workingDirectory = ss.getWorkingDir();

            if (ps.getOverrideGlobalSettings()) {
                String overridenWorkingDirectory = ps.getWorkingDir();

                if ((overridenWorkingDirectory != null) && !"".equals(overridenWorkingDirectory.trim())) {
                    workingDirectory = overridenWorkingDirectory; // NOI18N
                }
            }

            getProject().setProperty("profiler.info.dir", workingDirectory); // NOI18N
        }

        if (classpath != null) {
            ss.setMainClassPath(classpath.toString());
        }

        if (ps.getProfilingType() == ProfilingSettings.PROFILE_CPU_ENTIRE) {
            getProject().log("Roots path: " + rootsPath, Project.MSG_VERBOSE); //NOI18N

            if (rootsPath != null) {
                String[] paths = rootsPath.list();
                ArrayList al = new ArrayList();

                for (int i = 0; i < paths.length; i++) {
                    addPackagesList(al, paths[i]);
                }

                ClientUtils.SourceCodeSelection[] ret = new ClientUtils.SourceCodeSelection[al.size()];

                for (int i = 0; i < al.size(); i++) {
                    if ("".equals(al.get(i))) { //NOI18N
                        ret[i] = new ClientUtils.SourceCodeSelection("", "", ""); //NOI18N
                    } else {
                        ret[i] = new ClientUtils.SourceCodeSelection(((String) al.get(i)) + ".", "", ""); //NOI18N
                    }
                }

                ps.setInstrumentationRootMethods(ret);
            }
        }

        // 3. log used properties in verbose level
        getProject().log("Starting Profiled Application", Project.MSG_VERBOSE); //NOI18N
        getProject().log("  mainClass: " + ss.getMainClass(), Project.MSG_VERBOSE); //NOI18N
        getProject().log("  classpath: " + ss.getMainClassPath(), Project.MSG_VERBOSE); //NOI18N
        getProject().log("  arguments: " + ss.getMainArgs(), Project.MSG_VERBOSE); //NOI18N

        if (ps.getOverrideGlobalSettings()) {
            getProject().log("  jvm arguments: " + ps.getJVMArgs(), Project.MSG_VERBOSE); //NOI18N
        } else {
            getProject().log("  jvm arguments: " + ss.getJVMArgs(), Project.MSG_VERBOSE); //NOI18N
        }

        if (ps.getOverrideGlobalSettings()) {
            getProject().log("  working dir: " + ps.getWorkingDir(), Project.MSG_VERBOSE); //NOI18N
        } else {
            getProject().log("  working dir: " + ss.getWorkingDir(), Project.MSG_VERBOSE); //NOI18N
        }

        // 4. log profiling and session settings in debug level
        getProject().log("  profiling settings: " + ps.debug(), Project.MSG_DEBUG); //NOI18N
        getProject().log("  session settings: " + ss.debug(), Project.MSG_DEBUG); //NOI18N

        // 5. determine project being profiled
        org.netbeans.api.project.Project profiledProject = null;
        FileObject singleFO = null;

        if (projectDir != null) {
            String errorMessage = null;
            FileObject projectFO = FileUtil.toFileObject(FileUtil.normalizeFile(new File(projectDir)));

            if (projectFO != null) {
                try {
                    profiledProject = ProjectManager.getDefault().findProject(projectFO);
                } catch (IOException e) {
                    errorMessage = "IOException: " + e.getMessage(); //NOI18N
                }
            } else {
                errorMessage = "Could not find project directory: " + projectDir; //NOI18N
            }

            if (errorMessage != null) {
                getProject().log("Could not determine project: " + errorMessage, Project.MSG_INFO); //NOI18N
                getProject().log("Using global (no project) attach context", Project.MSG_INFO); //NOI18N
            }

            if (singleFile != null) {
                singleFO = FileUtil.toFileObject(FileUtil.normalizeFile(new File(singleFile)));
            }
        } else {
            getProject().log("You can use property profiler.info.project.dir to specify project that is being profiled.", //NOI18N
                             Project.MSG_VERBOSE);
        }

        final org.netbeans.api.project.Project projectToUse = profiledProject;

        if (!CalibrationDataFileIO.validateCalibrationInput(ss.getJavaVersionString(), ss.getJavaExecutable())
                || !Profiler.getDefault()
                                .runCalibration(true, ss.getJavaExecutable(), ss.getJavaVersionString(),
                                                    ss.getSystemArchitecture())) {
            Profiler.getDefault().displayError(CALIBRATION_FAILED_MESSAGE);
            throw new BuildException(CALIBRATION_FAILED_MESSAGE); // failed, cannot proceed
        }

        // 6. invoke profiling with constructed profiling and session settings
        final ProfilingSettings ps1 = ps;
        final FileObject singleFO1 = singleFO;
        //    SwingUtilities.invokeLater(
        //        new Runnable() {
        //          public void run() {
        NetBeansProfiler.getDefaultNB().setProfiledProject(projectToUse, singleFO1);
        Profiler.getDefault().connectToStartedApp(ps1, ss);

        //          }
        //        }
        //    );
    }

    private void addPackagesForArchive(ArrayList list, String s, File f) {
        getProject().log("Add root packages for archive: " + f.getName(), Project.MSG_VERBOSE); //NOI18N

        try {
            JarFile jf = new JarFile(f);

            for (Enumeration e = jf.entries(); e.hasMoreElements();) {
                JarEntry je = (JarEntry) e.nextElement();
                getProject().log("Checking jar entry: " + je.getName(), Project.MSG_VERBOSE); //NOI18N

                if (!je.isDirectory() && je.getName().endsWith(".class")) { //NOI18N

                    String name = je.getName();
                    int idx = name.lastIndexOf('/'); //NOI18N
                    String packageName = (idx == -1) ? name : name.substring(0, idx);
                    packageName = packageName.replace('/', '.'); //NOI18N

                    if (!list.contains(packageName)) {
                        getProject().log("Adding package: " + packageName, Project.MSG_VERBOSE); //NOI18N
                        list.add(packageName);
                    }
                }
            }
        } catch (IOException e) {
            getProject().log("Failed to scan packages for archive: " + f.getName()); //NOI18N
        }
    }

    private void addPackagesForDirectory(ArrayList packages, String prefix, File f) {
        if (!f.isDirectory()) { // not a folder

            return;
        }

        getProject().log("Add root packages for directory: " + f.getName(), Project.MSG_VERBOSE); //NOI18N

        File[] children = f.listFiles();

        // 1. check if there are java sdources in this folder and if so, add to the list of packages
        if (!packages.contains(prefix)) { // already in there, skip this

            for (int i = 0; i < children.length; i++) {
                File child = children[i];

                if (child.getName().endsWith(".class")) { //NOI18N
                    getProject().log("Addding package: " + prefix, Project.MSG_VERBOSE); //NOI18N
                    packages.add(prefix);

                    break;
                }
            }
        }

        // 2. recurse into subfolders
        for (int i = 0; i < children.length; i++) {
            File child = children[i];

            if (child.isDirectory()) {
                if ("".equals(prefix)) { //NOI18N
                    addPackagesForDirectory(packages, child.getName(), child);
                } else {
                    addPackagesForDirectory(packages, prefix + "." + child.getName(), child); //NOI18N
                }
            }
        }
    }

    private void addPackagesList(ArrayList list, String path)
                          throws BuildException {
        File f = new File(path);

        if (!f.exists()) {
            getProject().log("Cannot find: " + path); //NOI18N

            return;
        }

        if (f.isDirectory()) {
            addPackagesForDirectory(list, "", f); //NOI18N
        } else if (f.getName().endsWith(".jar")) { //NOI18N
            addPackagesForArchive(list, "", f); //NOI18N
        }
    }

    private String initializeInteractively(ProfilingSettings ps, SessionSettings ss)
                                    throws BuildException {
        String projectDir = null;
        getProject().log("Entering interactive mode of nbprofiledirect task...", Project.MSG_VERBOSE); //NOI18N

        org.netbeans.api.project.Project p = ProjectUtilities.getProjectForBuildScript(getLocation().getFileName());

        if (p != null) {
            getProject().log("Using project: " + ProjectUtilities.getProjectName(p), Project.MSG_INFO); //NOI18N
            projectDir = FileUtil.toFile(p.getProjectDirectory()).getAbsolutePath();
        }

        ss.setPortNo(ProfilerIDESettings.getInstance().getPortNo());

        if (mainClass != null) {
            ss.setMainClass(mainClass);
        }

        JavaPlatform platform = IDEUtils.getJavaPlatformByName(ProfilerIDESettings.getInstance().getJavaPlatformForProfiling());

        if (platform == null) {
            platform = JavaPlatformSelector.getDefault().selectPlatformToUse();

            if (platform == null) {
                throw new BuildException("Cancelled..."); //NOI18N
            }
        }

        String javaFile = IDEUtils.getPlatformJavaFile(platform);

        if (javaFile == null) {
            throw new BuildException("Cannot determine Java executable for platform: " + platform.getDisplayName()); //NOI18N
        }

        String javaVersion = IDEUtils.getPlatformJDKVersion(platform);

        if (javaVersion == null) {
            throw new BuildException("Cannot determine Java version for the selected Java platform"); //NOI18N
        }

        ss.setJavaExecutable(javaFile);
        ss.setJavaVersionString(javaVersion);
        ss.setSystemArchitecture(IDEUtils.getPlatformArchitecture(platform));

        ps = ProfilingSupport.getDefault().selectTaskForProfiling(p, ss, null, false);

        if (ps == null) {
            throw new BuildException("Cancelled by the user"); //NOI18N
        }

        String usedJavaExecutable = null;
        String usedJvmArgs = null;
        String usedWorkDir = null;

        if (ps.getOverrideGlobalSettings()) {
            getProject().log("Global settings are overridden by the profiling configuration", Project.MSG_VERBOSE); //NOI18N

            if (ps.getJavaPlatformName() != null) {
                usedJavaExecutable = Profiler.getDefault().getPlatformJavaFile(ps.getJavaPlatformName());
            }

            usedJvmArgs = ps.getJVMArgs();
            usedWorkDir = ps.getWorkingDir();

            if (usedJavaExecutable != null) {
                getProject().log("Overridden Java Executable: " + usedJavaExecutable //NOI18N
                                 + ", stored in property: " + jvmProperty, Project.MSG_VERBOSE //NOI18N
                );
                getProject().setProperty(jvmProperty, usedJavaExecutable);
            }

            if (usedJvmArgs != null) {
                getProject().log("Overridden Working Directory: " + usedWorkDir, Project.MSG_VERBOSE); //NOI18N
                getProject().setProperty("profiler.info.jvmargs", usedJvmArgs); // NOI18N
            }

            if (usedWorkDir != null) {
                getProject().setProperty("profiler.info.dir", usedWorkDir); // NOI18N
            }
        }

        String usedAgentJvmArgs = jvmArgsPrefix
                                  + IDEUtils.getAntProfilerStartArgument15(ss.getPortNo(), ss.getSystemArchitecture());
        getProject().setProperty(jvmArgsProperty, usedAgentJvmArgs); // NOI18N
        getProject().log("Profiler agent JVM arguments: " + usedAgentJvmArgs, Project.MSG_VERBOSE); //NOI18N
        getProject().log("Profiler agent JVM arguments stored in property " + jvmArgsProperty, Project.MSG_INFO); //NOI18N

        return projectDir;
    }
}
