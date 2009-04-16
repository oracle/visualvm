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
import org.netbeans.modules.profiler.ppoints.ui.ResetResultsCustomizer;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Lookup;
import java.io.File;
import java.text.MessageFormat;
import java.util.Properties;
import javax.swing.Icon;
import javax.swing.ImageIcon;


/**
 *
 * @author Jiri Sedlacek
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.ppoints.ProfilingPointFactory.class)
public class ResetResultsProfilingPointFactory extends CodeProfilingPointFactory {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String PP_TYPE = NbBundle.getMessage(ResetResultsProfilingPointFactory.class,
                                                              "ResetResultsProfilingPointFactory_PpType"); // NOI18N
    private static final String PP_DESCR = NbBundle.getMessage(ResetResultsProfilingPointFactory.class,
                                                               "ResetResultsProfilingPointFactory_PpDescr"); // NOI18N
    private static final String PP_DEFAULT_NAME = NbBundle.getMessage(ResetResultsProfilingPointFactory.class,
                                                                      "ResetResultsProfilingPointFactory_PpDefaultName"); // NOI18N
                                                                                                                          // -----
    public static final Icon RESET_RESULTS_PP_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ppoints/ui/resources/resetResultsProfilingPoint.png", false); // NOI18N
    public static final String RESET_RESULTS_PP_TYPE = PP_TYPE;
    public static final String RESET_RESULTS_PP_DESCR = PP_DESCR;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static ResetResultsProfilingPointFactory getDefault() {
        return Lookup.getDefault().lookup(ResetResultsProfilingPointFactory.class);
    }

    public String getDescription() {
        return RESET_RESULTS_PP_DESCR;
    }

    public Icon getIcon() {
        return RESET_RESULTS_PP_ICON;
    }

    public int getScope() {
        return SCOPE_CODE;
    }

    public String getType() {
        return RESET_RESULTS_PP_TYPE;
    }

    public ResetResultsProfilingPoint create(Project project) {
        if (project == null) {
            project = Utils.getCurrentProject(); // project not defined, will be detected from most active Editor or Main Project will be used
        }

        CodeProfilingPoint.Location location = Utils.getCurrentLocation(CodeProfilingPoint.Location.OFFSET_START);

        if (location.equals(CodeProfilingPoint.Location.EMPTY)) {
            String filename = ""; // NOI18N
            String name = Utils.getUniqueName(getType(), "", project); // NOI18N

            return new ResetResultsProfilingPoint(name, location, project);
        } else {
            File file = FileUtil.normalizeFile(new File(location.getFile()));
            String filename = FileUtil.toFileObject(file).getName();
            String name = Utils.getUniqueName(getType(),
                                              MessageFormat.format(PP_DEFAULT_NAME,
                                                                   new Object[] { "", filename, location.getLine() }), project); // NOI18N

            return new ResetResultsProfilingPoint(name, location, project);
        }
    }

    public boolean supportsCPU() {
        return true;
    }

    public boolean supportsMemory() {
        return true;
    }

    public boolean supportsMonitor() {
        return false;
    }

    protected Class getProfilingPointsClass() {
        return ResetResultsProfilingPoint.class;
    }

    protected String getServerHandlerClassName() {
        return "org.netbeans.lib.profiler.server.ResetResultsProfilingPointHandler";
    } // NOI18N

    protected ResetResultsCustomizer createCustomizer() {
        return new ResetResultsCustomizer(getType(), getIcon());
    }

    protected ProfilingPoint loadProfilingPoint(Project project, Properties properties, int index) {
        String name = properties.getProperty(index + "_" + ProfilingPoint.PROPERTY_NAME, null); // NOI18N
        String enabledStr = properties.getProperty(index + "_" + ProfilingPoint.PROPERTY_ENABLED, null); // NOI18N
        CodeProfilingPoint.Location location = CodeProfilingPoint.Location.load(project, index, properties);

        if ((name == null) || (enabledStr == null) || (location == null)) {
            return null;
        }

        ResetResultsProfilingPoint profilingPoint = null;

        try {
            profilingPoint = new ResetResultsProfilingPoint(name, location, project);
            profilingPoint.setEnabled(Boolean.parseBoolean(enabledStr));
        } catch (Exception e) {
            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
        }

        return profilingPoint;
    }

    protected void storeProfilingPoint(ProfilingPoint profilingPoint, int index, Properties properties) {
        ResetResultsProfilingPoint resetResults = (ResetResultsProfilingPoint) profilingPoint;
        properties.put(index + "_" + ProfilingPoint.PROPERTY_NAME, resetResults.getName()); // NOI18N
        properties.put(index + "_" + ProfilingPoint.PROPERTY_ENABLED, Boolean.toString(resetResults.isEnabled())); // NOI18N
        resetResults.getLocation().store(resetResults.getProject(), index, properties);
    }
}
