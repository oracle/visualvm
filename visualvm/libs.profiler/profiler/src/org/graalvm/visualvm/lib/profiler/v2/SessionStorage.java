/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.profiler.v2;

import java.util.Properties;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.profiler.api.ProfilerStorage;
import org.graalvm.visualvm.lib.profiler.v2.impl.WeakProcessor;
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
