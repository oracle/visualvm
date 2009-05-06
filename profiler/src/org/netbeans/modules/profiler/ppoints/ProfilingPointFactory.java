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
import org.netbeans.modules.profiler.ppoints.ui.ValidityAwarePanel;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.swing.Icon;


/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilingPointFactory {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String AVAILABILITY_PROPERTY = ProfilingPointFactory.class.getName() + "#AVAILABILITY"; // NOI18N
    private static final String PROFILING_POINT_STORAGE_EXT = "pp"; // NOI18N
    public static final int SCOPE_CODE = 1; // Scope of the Profiling Point: Code (see CodeProfilingPoint)
    public static final int SCOPE_GLOBAL = 2; // Scope of the Profiling Point: Global (see GlobalProfilingPoint)
    public static final Icon SCOPE_CODE_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ppoints/ui/resources/codeProfilingPoint.png", false); // NOI18N
    public static final Icon SCOPE_GLOBAL_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/ppoints/ui/resources/globalProfilingPoint.png", false); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private ValidityAwarePanel customizer = null;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public final void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    public boolean isAvailable() {
        return true;
    } // subclasses will override this method if they eg. depend on external modules

    public abstract String getDescription();

    public abstract Icon getIcon();

    // Defines scope of the Profiling Point: Code or Global (see CodeProfilingPoint or GlobalProfilingPoint)
    public abstract int getScope();

    public abstract String getType();

    public ProfilingPoint create() {
        return create(null);
    }

    public abstract ProfilingPoint create(Project project);

    public abstract boolean supportsCPU();

    public abstract boolean supportsMemory();

    // Support for each profiling type
    public abstract boolean supportsMonitor();

    public Icon getScopeIcon() {
        switch (getScope()) {
            case SCOPE_CODE:
                return SCOPE_CODE_ICON;
            case SCOPE_GLOBAL:
                return SCOPE_GLOBAL_ICON;
            default:
                return null;
        }
    }

    protected abstract Class getProfilingPointsClass();

    protected abstract ValidityAwarePanel createCustomizer(); // Creates an instance of ValidityAwarePanel (called once)

    protected abstract ProfilingPoint loadProfilingPoint(Project project, Properties properties, int index);

    protected abstract void storeProfilingPoint(ProfilingPoint profilingPoint, int index, Properties properties);

    protected void firePropertyChange(PropertyChangeEvent event) {
        pcs.firePropertyChange(event);
    }

    ValidityAwarePanel getCustomizer() {
        if (customizer == null) {
            customizer = createCustomizer(); // Created new customizer (shared instance)
        }

        ValidityAwarePanel safeCustomizer = ProfilingPointsManager.getDefault().safeGetCustomizer(customizer); // Check if any customizer is already showing

        return safeCustomizer;
    }

    ProfilingPoint[] loadProfilingPoints(Project project)
                                  throws IOException, InvalidPropertiesFormatException {
        List<ProfilingPoint> profilingPoints = new LinkedList();
        Properties properties = new Properties();
        final FileObject profilingPointsStorage = getProfilingPointsStorage(project);

        if (profilingPointsStorage != null) {
            final InputStream is = profilingPointsStorage.getInputStream();
            final BufferedInputStream bis = new BufferedInputStream(is);
            properties.loadFromXML(bis);
            bis.close();

            int index = 0;

            while (properties.getProperty(index + "_" + ProfilingPoint.PROPERTY_NAME) != null) {
                ProfilingPoint profilingPoint = loadProfilingPoint(project, properties, index);

                if (profilingPoint != null) {
                    profilingPoints.add(profilingPoint);
                } else {
                    ErrorManager.getDefault()
                                .log(ErrorManager.ERROR,
                                     "Invalid " + getType() + " Profiling Point format at index " + index + " in project "
                                     + ProjectUtils.getInformation(project).getDisplayName()); // NOI18N
                }

                index++;
            }
        }

        ProfilingPoint[] profilingPointsArr = new ProfilingPoint[profilingPoints.size()];
        profilingPoints.toArray(profilingPointsArr);

        return profilingPointsArr;
    }

    void saveProfilingPoints(Project project) throws IOException {
        saveProfilingPoints((ProfilingPoint[]) ProfilingPointsManager.getDefault()
                                                                     .getProfilingPoints(getProfilingPointsClass(), project, false)
                                                                     .toArray(new ProfilingPoint[0]), project);
    }

    private FileObject getProfilingPointsStorage(Project project)
                                          throws IOException {
        FileObject projectSettingsFolder = IDEUtils.getProjectSettingsFolder(project, false);

        if (projectSettingsFolder == null) {
            return null;
        }

        String profilingPointClassNameFull = getProfilingPointsClass().getName();
        String profilingPointClassName = profilingPointClassNameFull.substring(profilingPointClassNameFull.lastIndexOf(".") + 1); // NOI18N
        FileObject profilingPointsStorage = projectSettingsFolder.getFileObject(profilingPointClassName,
                                                                                PROFILING_POINT_STORAGE_EXT);

        return profilingPointsStorage;
    }

    private FileObject createProfilingPointsStorage(Project project)
                                             throws IOException {
        FileObject projectSettingsFolder = IDEUtils.getProjectSettingsFolder(project, true);
        String profilingPointClassNameFull = getProfilingPointsClass().getName();
        String profilingPointClassName = profilingPointClassNameFull.substring(profilingPointClassNameFull.lastIndexOf(".") + 1); // NOI18N
        FileObject profilingPointsStorage = projectSettingsFolder.createData(profilingPointClassName, PROFILING_POINT_STORAGE_EXT);

        return profilingPointsStorage;
    }

    private void deleteProfilingPointsStorage(Project project)
                                       throws IOException {
        FileObject profilingPointsStorage = getProfilingPointsStorage(project);

        if (profilingPointsStorage != null) {
            FileLock lock = null;

            try {
                lock = profilingPointsStorage.lock();
                profilingPointsStorage.delete(lock);
            } catch (Exception e) {
            } finally {
                if (lock != null) {
                    lock.releaseLock();
                }
            }
        }
    }

    private void saveProfilingPoints(ProfilingPoint[] profilingPoints, Project project)
                              throws IOException {
        if (profilingPoints.length > 0) {
            FileObject profilingPointsStorage = getProfilingPointsStorage(project);

            if (profilingPointsStorage == null) {
                profilingPointsStorage = createProfilingPointsStorage(project);
            }

            if (profilingPointsStorage != null) {
                Properties properties = new Properties();

                for (int i = 0; i < profilingPoints.length; i++) {
                    storeProfilingPoint(profilingPoints[i], i, properties);
                }

                storeSettings(profilingPointsStorage, properties);
            }
        } else {
            deleteProfilingPointsStorage(project);
        }
    }

    private void storeSettings(final FileObject storage, final Properties properties)
                        throws IOException {
        FileLock lock = null;

        try {
            lock = storage.lock();

            final OutputStream os = storage.getOutputStream(lock);
            final BufferedOutputStream bos = new BufferedOutputStream(os);
            properties.storeToXML(os, null);
            bos.close();
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
    }
}
