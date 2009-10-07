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
import org.apache.tools.ant.types.Path;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import javax.swing.*;


/** Ant task to start the NetBeans profiler attach action.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class NBProfileAttachTask extends Task {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /** Explicit classpath of the profiled process. */
    private Path classpath = null;
    private String port = null;
    private boolean direct = true;
    private boolean directDefinedExplicitely = false;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setDirect(final boolean aos) {
        this.direct = aos;
        directDefinedExplicitely = true;
    }

    public void setPort(final String port) {
        this.port = port;
    }

    // properties -----------------------------------------------------------------

    /** "classpath" subelements, only one is allowed
     * @param path the classpath
     */
    public void addClasspath(final Path path) {
        if (classpath != null) {
            throw new BuildException("Only one classpath subelement is supported"); //NOI18N
        }

        classpath = path;
    }

    // main methods ---------------------------------------------------------------
    public void execute() throws BuildException {
        final Hashtable props = getProject().getProperties();
        final ProfilingSettings ps = new ProfilingSettings();

        // 1. process parameters passed via Properties
        ps.load(props);

        final AttachSettings as = new AttachSettings();
        as.load(getProject().getProperties());

        // 2. Process those passed as attributes/elements from the buildl script
        if (directDefinedExplicitely) {
            as.setDirect(Boolean.valueOf(direct).booleanValue());
        }

        if (port != null) {
            try {
                final int portNo = Integer.parseInt(port);
                as.setPort(portNo);
            } catch (NumberFormatException e) {
            } // ignore, will not be used
        }

        // 3. log used properties in verbose level
        getProject().log("Attaching to Profiled Application", Project.MSG_VERBOSE); //NOI18N
        getProject().log("  classpath: " + classpath, Project.MSG_VERBOSE); //NOI18N
        getProject().log("  attach direct: " + as.isDirect(), Project.MSG_VERBOSE); //NOI18N
        getProject().log("  remote attach: " + as.isRemote(), Project.MSG_VERBOSE); //NOI18N
        getProject().log("  remote host: " + as.getHost(), Project.MSG_VERBOSE); //NOI18N
        getProject().log("  profiler port: " + as.getPort(), Project.MSG_VERBOSE); //NOI18N

        // 4. log profiling and session settings in debug level
        getProject().log("  profiling settings: " + ps.debug(), Project.MSG_DEBUG); //NOI18N
        getProject().log("  attach settings: " + as.debug(), Project.MSG_DEBUG); //NOI18N

        // 5. determine project being profiled
        org.netbeans.api.project.Project profiledProject = null;

        String projectDir = (String) props.get("profiler.info.project.dir"); //NOI18N

        if (projectDir != null) {
            FileObject projectFO = FileUtil.toFileObject(FileUtil.normalizeFile(new File(projectDir)));

            if (projectFO != null) {
                try {
                    profiledProject = ProjectManager.getDefault().findProject(projectFO);
                } catch (IOException e) {
                    getProject().log("Could not determine project: " + e.getMessage(), Project.MSG_WARN); //NOI18N
                }
            }
        }

        // 6. invoke profiling with constructed profiling and attach settings
        ((NetBeansProfiler) Profiler.getDefault()).setProfiledProject(profiledProject, null);
        Profiler.getDefault().attachToApp(ps, as);
    }
}
