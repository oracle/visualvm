/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.jvmcap;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.snapshot.ApplicationSnapshot;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.snapshot.Snapshot;

/**
 *
 * @author Jiri Sedlacek
 */
class JvmCapabilitiesModel {
    
    private static final String PROP_PREFIX = "JvmCapabilitiesModel_";
    
    static final String SNAPSHOT_VERSION = PROP_PREFIX + "version";
    private static final String SNAPSHOT_VERSION_DIVIDER = ".";
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    public static final String PROP_IS_ATTACHABLE = PROP_PREFIX + "is_attachable";
    public static final String PROP_BASIC_INFO_SUPPORTED = PROP_PREFIX + "basic_info_supported";
    public static final String PROP_MONITORING_SUPPORTED = PROP_PREFIX + "monitoring_supported";
    public static final String PROP_CLASS_MONITORING_SUPPORTED = PROP_PREFIX + "class_monitoring_supported";
    public static final String PROP_THREAD_MONITORING_SUPPORTED = PROP_PREFIX + "thread_monitoring_supported";
    public static final String PROP_MEMORY_MONITORING_SUPPORTED = PROP_PREFIX + "memory_monitoring_supported";
    public static final String PROP_CPU_MONITORING_SUPPORTED = PROP_PREFIX + "cpu_monitoring_supported";
    public static final String PROP_COLLECTIONTIME_MONITORING_SUPPORTED = PROP_PREFIX + "collection_monitoring_supported";
    public static final String PROP_GET_SYSTEM_PROPERTIES_SUPPORTED = PROP_PREFIX + "get_system_properties_supported";
    public static final String PROP_DUMP_ON_OOM_SUPPORTED = PROP_PREFIX + "dump_on_oom_supported";
    public static final String PROP_TAKE_HEAP_DUMP_SUPPORTED = PROP_PREFIX + "take_heap_dump_supported";
    public static final String PROP_TAKE_THREAD_DUMP_SUPPORTED = PROP_PREFIX + "take_thread_dump_supported";
    
    private static final String VAL_TRUE = "yes";
    private static final String VAL_FALSE = "no";
    private static final String VAL_UNKNOWN = "&lt;unknown&gt;";
    
    private boolean initialized;
    private DataSource source;
    
    private String isAttachable;
    private String isBasicInfoSupported;
    private String isMonitoringSupported;
    private String isClassMonitoringSupported;
    private String isThreadMonitoringSupported;
    private String isMemoryMonitoringSupported;
    private String isCpuMonitoringSupported;
    private String isCollectionTimeMonitoringSupported;
    private String isGetSystemPropertiesSupported;
    private String isDumpOnOOMSupported;
    private String isTakeHeapDumpSupported;
    private String isTakeThreadDumpSupported;    
    
    static JvmCapabilitiesModel create(Application application) {
        JvmCapabilitiesModel model = new JvmCapabilitiesModel();
        model.initialized = false;
        model.source = application;
        return model;
    }
    
    static JvmCapabilitiesModel create(Snapshot snapshot) {
        JvmCapabilitiesModel model = new JvmCapabilitiesModel();
        model.initialized = false;
        model.source = snapshot;
        return model;
    }
    
    
    synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        if (source instanceof Application) initialize((Application)source);
        else if (source instanceof ApplicationSnapshot) initialize((Snapshot)source);
        else throw new IllegalStateException("Cannot initialize model for " + source);
    }
    
    void save(Snapshot snapshot) {
        initialize();
        Storage storage = snapshot.getStorage();
        
        storage.setCustomProperty(SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);
        storage.setCustomProperty(PROP_IS_ATTACHABLE, isAttachable);
        storage.setCustomProperty(PROP_BASIC_INFO_SUPPORTED, isBasicInfoSupported);
        storage.setCustomProperty(PROP_MONITORING_SUPPORTED, isMonitoringSupported);
        storage.setCustomProperty(PROP_CLASS_MONITORING_SUPPORTED, isClassMonitoringSupported);
        storage.setCustomProperty(PROP_THREAD_MONITORING_SUPPORTED, isThreadMonitoringSupported);
        storage.setCustomProperty(PROP_MEMORY_MONITORING_SUPPORTED, isMemoryMonitoringSupported);
        storage.setCustomProperty(PROP_CPU_MONITORING_SUPPORTED, isCpuMonitoringSupported);
        storage.setCustomProperty(PROP_COLLECTIONTIME_MONITORING_SUPPORTED, isCollectionTimeMonitoringSupported);
        storage.setCustomProperty(PROP_GET_SYSTEM_PROPERTIES_SUPPORTED, isGetSystemPropertiesSupported);
        storage.setCustomProperty(PROP_DUMP_ON_OOM_SUPPORTED, isDumpOnOOMSupported);
        storage.setCustomProperty(PROP_TAKE_HEAP_DUMP_SUPPORTED, isTakeHeapDumpSupported);
        storage.setCustomProperty(PROP_TAKE_THREAD_DUMP_SUPPORTED, isTakeThreadDumpSupported);
    }
    
    
    String isAttachable() { return isAttachable; }
    String isBasicInfoSupported() { return isBasicInfoSupported; }
    String isMonitoringSupported() { return isMonitoringSupported; }
    String isClassMonitoringSupported() { return isClassMonitoringSupported; }
    String isThreadMonitoringSupported() { return isThreadMonitoringSupported; }
    String isMemoryMonitoringSupported() { return isMemoryMonitoringSupported; }
    String isCpuMonitoringSupported() { return isCpuMonitoringSupported; }
    String isCollectionTimeMonitoringSupported() { return isCollectionTimeMonitoringSupported; }
    String isGetSystemPropertiesSupported() { return isGetSystemPropertiesSupported; }
    String isDumpOnOOMSupported() { return isDumpOnOOMSupported; }
    String isTakeHeapDumpSupported() { return isTakeHeapDumpSupported; }
    String isTakeThreadDumpSupported() { return isTakeThreadDumpSupported; }
    
    
    private void initialize(Application application) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        
        isAttachable = getValue(jvm.isAttachable());
        isBasicInfoSupported = getValue(jvm.isBasicInfoSupported());
        isMonitoringSupported = getValue(jvm.isMonitoringSupported());
        isClassMonitoringSupported = getValue(jvm.isClassMonitoringSupported());
        isThreadMonitoringSupported = getValue(jvm.isThreadMonitoringSupported());
        isMemoryMonitoringSupported = getValue(jvm.isMemoryMonitoringSupported());
        isCpuMonitoringSupported = getValue(jvm.isCpuMonitoringSupported());
        isCollectionTimeMonitoringSupported = getValue(jvm.isCollectionTimeSupported());
        isGetSystemPropertiesSupported = getValue(jvm.isGetSystemPropertiesSupported());
        isDumpOnOOMSupported = getValue(jvm.isDumpOnOOMEnabledSupported());
        isTakeHeapDumpSupported = getValue(jvm.isTakeHeapDumpSupported());
        isTakeThreadDumpSupported = getValue(jvm.isTakeThreadDumpSupported());
    }
    
    private void initialize(Snapshot snapshot) {
        Storage storage = snapshot.getStorage();
        
        isAttachable = getValue(storage, PROP_IS_ATTACHABLE);
        isBasicInfoSupported = getValue(storage, PROP_BASIC_INFO_SUPPORTED);
        isMonitoringSupported = getValue(storage, PROP_MONITORING_SUPPORTED);
        isClassMonitoringSupported = getValue(storage, PROP_CLASS_MONITORING_SUPPORTED);
        isThreadMonitoringSupported = getValue(storage, PROP_THREAD_MONITORING_SUPPORTED);
        isMemoryMonitoringSupported = getValue(storage, PROP_MEMORY_MONITORING_SUPPORTED);
        isCpuMonitoringSupported = getValue(storage, PROP_CPU_MONITORING_SUPPORTED);
        isCollectionTimeMonitoringSupported = getValue(storage, PROP_COLLECTIONTIME_MONITORING_SUPPORTED);
        isGetSystemPropertiesSupported = getValue(storage, PROP_GET_SYSTEM_PROPERTIES_SUPPORTED);
        isDumpOnOOMSupported = getValue(storage, PROP_DUMP_ON_OOM_SUPPORTED);
        isTakeHeapDumpSupported = getValue(storage, PROP_TAKE_HEAP_DUMP_SUPPORTED);
        isTakeThreadDumpSupported = getValue(storage, PROP_TAKE_THREAD_DUMP_SUPPORTED);
    }
    
    private static String getValue(boolean boolValue) {
        return boolValue ? VAL_TRUE : VAL_FALSE;
    }
    
    private static String getValue(Storage storage, String property) {
        String val = storage.getCustomProperty(property);
        return val != null ? val : VAL_UNKNOWN;
    }
    
    
    private JvmCapabilitiesModel() {
    }

}
