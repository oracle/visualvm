/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.api.project.ProjectStorage;
import org.netbeans.modules.profiler.v2.impl.WeakProcessor;
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
final class SessionStorage {
    
    private static final String SETTINGS_FILENAME = "settings"; // NOI18N
    private static final String SETTINGS_FILEEXT = "xml"; // NOI18N
    
    private static final WeakProcessor PROCESSOR = new WeakProcessor("Profiler Storage Processor"); // NOI18N
    
    private Properties properties;
    private final Lookup.Provider project;
    
    
    SessionStorage(Lookup.Provider project) {
        this.project = project;
    }
    
    
    synchronized void storeFlag(String flag, String value) {
        if (properties == null) loadProperties();
        
        if (value != null) properties.put(flag, value);
        else properties.remove(flag);
    }
    
    synchronized String readFlag(String flag, String defaultValue) {
        if (properties == null) loadProperties();
        
        return properties.getProperty(flag, defaultValue);
    }
    
    
    synchronized void persist() {
        if (properties != null) {
            final Properties _properties = new Properties(properties);
            PROCESSOR.post(new Runnable() {
                public void run() { saveProperties(_properties); }
            });
        }
    }
    
    
    private void loadProperties() {
        properties = new Properties();

        assert !SwingUtilities.isEventDispatchThread();
        try {
            FileObject settingsStorage = ProjectStorage.getSettingsFolder(project, false);
            if (settingsStorage != null) {
                FileSystem fs = settingsStorage.getFileSystem();
                fs.runAtomicAction(new FileSystem.AtomicAction() {
                    public void run() throws IOException {
                        FileObject _settingsStorage = ProjectStorage.getSettingsFolder(project, true);
                        FileObject __settingsStorage = _settingsStorage.getFileObject(SETTINGS_FILENAME,
                                                                                      SETTINGS_FILEEXT);

                        if (__settingsStorage != null) {
                            InputStream is = __settingsStorage.getInputStream();
                            BufferedInputStream bis = new BufferedInputStream(is);
                            properties.loadFromXML(bis);
                            bis.close();
                        }
                    }
                });
            }
        } catch (Exception e) {
            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveProperties(Properties _properties) {
        assert !SwingUtilities.isEventDispatchThread();
        try {
            FileObject _settingsStorage = ProjectStorage.getSettingsFolder(project, true);
            FileObject __settingsStorage = _settingsStorage.getFileObject(SETTINGS_FILENAME,
                                                                          SETTINGS_FILEEXT);
            if (__settingsStorage == null) __settingsStorage = _settingsStorage.createData(SETTINGS_FILENAME,
                                                                                           SETTINGS_FILEEXT);

            if (__settingsStorage != null) {
                FileLock lock = null;

                try {
                    lock = __settingsStorage.lock();
                    final OutputStream os = __settingsStorage.getOutputStream(lock);
                    final BufferedOutputStream bos = new BufferedOutputStream(os);
                    _properties.storeToXML(os, null);
                    bos.close();
                } finally {
                    if (lock != null) lock.releaseLock();
                }
            }
        } catch (Exception e) {
            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }
    
}
