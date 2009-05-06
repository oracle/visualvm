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
import org.netbeans.modules.profiler.ppoints.ui.StopwatchCustomizer;
import org.openide.ErrorManager;
import org.openide.filesystems.FileUtil;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import java.io.File;
import java.text.MessageFormat;
import java.util.Properties;
import javax.swing.Icon;


/**
 *
 * @author Jiri Sedlacek
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.ppoints.ProfilingPointFactory.class)
public class StopwatchProfilingPointFactory extends CodeProfilingPointFactory {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String PP_TYPE = NbBundle.getMessage(StopwatchProfilingPointFactory.class,
                                                              "StopwatchProfilingPointFactory_PpType"); // NOI18N
    private static final String PP_DESCR = NbBundle.getMessage(StopwatchProfilingPointFactory.class,
                                                               "StopwatchProfilingPointFactory_PpDescr"); // NOI18N
    private static final String PP_DEFAULT_NAME = NbBundle.getMessage(StopwatchProfilingPointFactory.class,
                                                                      "StopwatchProfilingPointFactory_PpDefaultName"); // NOI18N
                                                                                                                       // -----
    public static final Icon RESET_RESULTS_PP_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ppoints/ui/resources/stopwatchProfilingPoint.png", false);
    public static final String RESET_RESULTS_PP_TYPE = PP_TYPE;
    public static final String RESET_RESULTS_PP_DESCR = PP_DESCR;
    private static final String START_LOCATION_PREFIX = "start_"; // NOI18N
    private static final String END_LOCATION_PREFIX = "end_"; // NOI18N
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

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

    public ProfilingPoint create(Project project) {
        if (project == null) {
            project = Utils.getCurrentProject(); // project not defined, will be detected from most active Editor or Main Project will be used
        }

        CodeProfilingPoint.Location[] selectionLocations = Utils.getCurrentSelectionLocations();

        if (selectionLocations.length != 2) {
            CodeProfilingPoint.Location location = Utils.getCurrentLocation(CodeProfilingPoint.Location.OFFSET_START);

            if (location.equals(CodeProfilingPoint.Location.EMPTY)) {
                String filename = ""; // NOI18N
                String name = Utils.getUniqueName(getType(), "", project); // NOI18N

                return new StopwatchProfilingPoint(name, location, null, project, this);
            } else {
                File file = FileUtil.normalizeFile(new File(location.getFile()));
                String filename = FileUtil.toFileObject(file).getName();
                String name = Utils.getUniqueName(getType(),
                                                  MessageFormat.format(PP_DEFAULT_NAME,
                                                                       new Object[] { "", filename, location.getLine() }), project); // NOI18N

                return new StopwatchProfilingPoint(name, location, null, project, this);
            }
        } else {
            CodeProfilingPoint.Location startLocation = selectionLocations[0];
            CodeProfilingPoint.Location endLocation = selectionLocations[1];
            File file = FileUtil.normalizeFile(new File(startLocation.getFile()));
            String filename = FileUtil.toFileObject(file).getName();
            String name = Utils.getUniqueName(getType(),
                                              MessageFormat.format(PP_DEFAULT_NAME,
                                                                   new Object[] { "", filename, startLocation.getLine() }),
                                              project); // NOI18N

            return new StopwatchProfilingPoint(name, startLocation, endLocation, project, this);
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
        return StopwatchProfilingPoint.class;
    }

    protected String getServerHandlerClassName() {
        return "org.netbeans.lib.profiler.global.ProfilingPointServerHandler";
    } // NOI18N

    protected StopwatchCustomizer createCustomizer() {
        return new StopwatchCustomizer(getType(), getIcon());
    }

    protected ProfilingPoint loadProfilingPoint(Project project, Properties properties, int index) {
        String name = properties.getProperty(index + "_" + ProfilingPoint.PROPERTY_NAME, null); // NOI18N
        String enabledStr = properties.getProperty(index + "_" + ProfilingPoint.PROPERTY_ENABLED, null); // NOI18N
        CodeProfilingPoint.Location startLocation = CodeProfilingPoint.Location.load(project, index, START_LOCATION_PREFIX,
                                                                                     properties);
        CodeProfilingPoint.Location endLocation = CodeProfilingPoint.Location.load(project, index, END_LOCATION_PREFIX, properties);

        if ((name == null) || (enabledStr == null) || (startLocation == null)) {
            return null;
        }

        StopwatchProfilingPoint profilingPoint = null;

        try {
            profilingPoint = new StopwatchProfilingPoint(name, startLocation, endLocation, project, this);
            profilingPoint.setEnabled(Boolean.parseBoolean(enabledStr));
        } catch (Exception e) {
            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
        }

        return profilingPoint;
    }

    protected void storeProfilingPoint(ProfilingPoint profilingPoint, int index, Properties properties) {
        StopwatchProfilingPoint stopwatch = (StopwatchProfilingPoint) profilingPoint;
        properties.put(index + "_" + ProfilingPoint.PROPERTY_NAME, stopwatch.getName()); // NOI18N
        properties.put(index + "_" + ProfilingPoint.PROPERTY_ENABLED, Boolean.toString(stopwatch.isEnabled())); // NOI18N
        stopwatch.getStartLocation().store(stopwatch.getProject(), index, START_LOCATION_PREFIX, properties);

        if (stopwatch.usesEndLocation()) {
            stopwatch.getEndLocation().store(stopwatch.getProject(), index, END_LOCATION_PREFIX, properties);
        }
    }
}
