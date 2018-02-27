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

import java.util.Properties;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.api.ProfilerStorage;
import org.netbeans.modules.profiler.v2.impl.WeakProcessor;
import org.openide.ErrorManager;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SessionStorage {
    
    private static final String SETTINGS_FILENAME = "settings"; // NOI18N
    
    private static final WeakProcessor PROCESSOR = new WeakProcessor("Profiler Storage Processor"); // NOI18N
    
    private boolean dirty;
    private Properties properties;
    private final Lookup.Provider project;
    
    
    SessionStorage(Lookup.Provider project) {
        this.project = project;
    }
    
    
    public synchronized void storeFlag(String flag, String value) {
        if (properties == null) loadProperties();
        
        boolean _dirty;
        if (value != null) _dirty = !value.equals(properties.put(flag, value));
        else _dirty = properties.remove(flag) != null;
        
        dirty |= _dirty;
    }
    
    public synchronized String readFlag(String flag, String defaultValue) {
        if (properties == null) loadProperties();
        
        return properties.getProperty(flag, defaultValue);
    }
    
    
    synchronized void persist(boolean immediately) {
        if (dirty) {
            if (immediately) {
                synchronized(PROCESSOR) { saveProperties(properties); }
            } else {
                final Properties _properties = new Properties();
                for (String key : properties.stringPropertyNames())
                    _properties.setProperty(key, properties.getProperty(key));
                PROCESSOR.post(new Runnable() {
                    public void run() { synchronized(PROCESSOR) { saveProperties(_properties); } }
                });
            }
            dirty = false;
        }
    }
    
    
    private void loadProperties() {
        properties = new Properties();

        assert !SwingUtilities.isEventDispatchThread();
        try {
            ProfilerStorage.loadProjectProperties(properties, project, SETTINGS_FILENAME);
        } catch (Exception e) {
            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveProperties(Properties _properties) {
        assert !SwingUtilities.isEventDispatchThread();
        try {
            ProfilerStorage.saveProjectProperties(_properties, project, SETTINGS_FILENAME);
        } catch (Exception e) {
            ErrorManager.getDefault().log(ErrorManager.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }
    
}
