/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Properties;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.modules.profiler.api.GlobalStorage;
import org.netbeans.modules.profiler.spi.project.ProjectStorageProvider;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.spi.project.ProjectStorageProvider.class)
public final class ProjectStorageProviderImpl extends ProjectStorageProvider {
    
    private static final String ERROR_SAVING_ATTACH_SETTINGS_MESSAGE = NbBundle.getMessage(ProjectStorageProviderImpl.class,
                                                                                           "ProjectStorageProviderImpl_ErrorSavingAttachSettingsMessage"); //NOI18N
    
    private static final String ATTACH_SETTINGS_FILENAME = "attach"; //NOI18N
    private static final String SETTINGS_FOR_ATTR = "settingsFor"; //NOI18N

    
    @Override
    public AttachSettings loadAttachSettings(Provider project) throws IOException {
        FileObject folder = getSettingsFolder(project, false);

        if (folder == null) {
            return null;
        }

        FileObject attachSettingsFile = folder.getFileObject(ATTACH_SETTINGS_FILENAME, "xml"); //NOI18N

        if (attachSettingsFile == null) {
            return null;
        }

        final InputStream fis = attachSettingsFile.getInputStream();
        final BufferedInputStream bis = new BufferedInputStream(fis);

        try {
            final Properties props = new Properties();
            props.loadFromXML(bis);

            AttachSettings as = new AttachSettings();
            as.load(props);

            return as;
        } finally {
            bis.close();
        }
    }

    @Override
    public void saveAttachSettings(Provider project, AttachSettings settings) {
        FileLock lock = null;

        try {
            final FileObject folder = getSettingsFolder(project, true);
            FileObject fo = folder.getFileObject(ATTACH_SETTINGS_FILENAME, "xml"); //NOI18N

            if (fo == null) {
                fo = folder.createData(ATTACH_SETTINGS_FILENAME, "xml"); //NOI18N
            }

            lock = fo.lock();

            final BufferedOutputStream bos = new BufferedOutputStream(fo.getOutputStream(lock));
            final Properties globalProps = new Properties();
            try {
                settings.store(globalProps);
                globalProps.storeToXML(bos, ""); //NOI18N
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
        } catch (Exception e) {
            ProfilerLogger.log(e);
            ProfilerDialogs.notify(new NotifyDescriptor.Message(MessageFormat.format(ERROR_SAVING_ATTACH_SETTINGS_MESSAGE,
                                                                                     new Object[] { e.getMessage() }),
                                                                NotifyDescriptor.ERROR_MESSAGE));
        } finally {
            if (lock != null) {
                lock.releaseLock();
            }
        }
    }

    @Override
    public synchronized FileObject getSettingsFolder(Provider project, boolean create) throws IOException {
        if (project == null) { // global folder for attach
            return GlobalStorage.getSettingsFolder(create);
        } else {
            // resolve 'nbproject'
            Project p = (Project)project;
            FileObject nbproject = p.getProjectDirectory().getFileObject("nbproject"); // NOI18N
            FileObject d;
            if (nbproject != null) {
                // For compatibility, continue to use nbproject/private/profiler for Ant-based projects.
                d = create ? FileUtil.createFolder(nbproject, "private/profiler") : nbproject.getFileObject("private/profiler"); // NOI18N
            } else {
                // Maven projects, autoprojects, etc.
                d = ProjectUtils.getCacheDirectory(p, IDEUtils.class);
            }
            if (d != null) {
                d.setAttribute(SETTINGS_FOR_ATTR, p.getProjectDirectory().getURL()); // NOI18N
            }
            return d;
        }
    }
    
    @Override
    public Lookup.Provider getProjectFromSettingsFolder(FileObject settingsFolder) {
        Object o = settingsFolder.getAttribute(SETTINGS_FOR_ATTR);
        if (o instanceof URL) {
            FileObject d = URLMapper.findFileObject((URL) o);
            if (d != null && d.isFolder()) {
                try {
                    return ProjectManager.getDefault().findProject(d);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        Project p = FileOwnerQuery.getOwner(settingsFolder);
        try {
            if (p != null && getSettingsFolder(p, false) == settingsFolder) {
                return p;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
    
}
