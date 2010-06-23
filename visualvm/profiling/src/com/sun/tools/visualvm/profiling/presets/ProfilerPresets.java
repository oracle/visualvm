/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.profiling.presets;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Utils;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerPresets {

    private static final String JAR_SUFFIX = ".jar";  // NOI18N

    private static final String PROFILER_STORAGE_DIRNAME = "profiler";    // NOI18N
    private static final String PRESETS_STORAGE_PREFIX = "preset-";    // NOI18N
    private static final Object profilerStorageDirectoryStringLock = new Object();
    // @GuardedBy profilerStorageDirectoryStringLock
    private static String profilerStorageDirectoryString;
    private static final Object profilerStorageDirectoryLock = new Object();
    // @GuardedBy profilerStorageDirectoryLock
    private static File profilerStorageDirectory;
    
    private static final String OPTIONS_HANDLE = "ProfilerOptions"; // NOI18N

    private static ProfilerPresets INSTANCE;

    private List<ProfilerPreset> presets;
    private ProfilerPreset presetToSelect;
    private ProfilerPreset presetToCreate;

    private final Set<WeakReference<PresetSelector>> selectors;


    public static synchronized ProfilerPresets getInstance() {
        if (INSTANCE == null) INSTANCE = new ProfilerPresets();
        return INSTANCE;
    }


    public void editPresets(ProfilerPreset preset) {
        presetToSelect = preset;
        OptionsDisplayer.getDefault().open(OPTIONS_HANDLE);
    }

    ProfilerPreset presetToSelect() {
        ProfilerPreset toSelect = presetToSelect;
        presetToSelect = null;
        return toSelect;
    }

    public void savePreset(ProfilerPreset preset) {
        presetToCreate = preset;
        OptionsDisplayer.getDefault().open(OPTIONS_HANDLE);
    }

    ProfilerPreset presetToCreate() {
        ProfilerPreset toCreate = presetToCreate;
        presetToCreate = null;
        return toCreate;
    }

    void optionsSubmitted(final ProfilerPreset selected) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Iterator<WeakReference<PresetSelector>> selectorsI =
                        selectors.iterator();
                while (selectorsI.hasNext()) {
                    WeakReference<PresetSelector> selectorR = selectorsI.next();
                    PresetSelector selector = selectorR.get();
                    if (selector == null) selectorsI.remove();
                    else if (SwingUtilities.getRoot(selector) == null) selectorsI.remove();
                    else selector.presetsChanged(selected);
                }
            }
        });
    }
    
    public PresetSelector createSelector(Application application,
                                         PresetSelector refSelector,
                                         Runnable presetSync) {
        String mainClass = getMainClass(application);
        if (mainClass == null || mainClass.isEmpty()) mainClass =
                ApplicationTypeFactory.getApplicationTypeFor(application).getName();
        if (mainClass == null || mainClass.isEmpty()) mainClass =
                DataSourceDescriptorFactory.getDescriptor(application).getName();
        
        ProfilerPreset toSelect = null;
        if (mainClass != null && !mainClass.isEmpty()) {
            String mainClassL = mainClass == null ? null : mainClass.toLowerCase();
            for (int i = 0; i < presets.size(); i++) {
                ProfilerPreset preset = presets.get(i);
                String selector = preset.getSelector();
                if (selector != null && !selector.isEmpty()) {
                    if (mainClass.equals(selector) || mainClassL.contains(selector.toLowerCase())) {
                        toSelect = preset;
                        break;
                    }
                }
            }
        }
        
        ProfilerPreset defPreset = createDefaultPreset(application);
        ProfilerPreset custPreset = null;
        PresetSelector selector = new PresetSelector(refSelector, defPreset, custPreset,
                                                     toSelect, presetSync, mainClass);
        selectors.add(new WeakReference(selector));
        return selector;
    }

    public ProfilerPreset[] getPresets(Application application) {
        ProfilerPreset[] presetsArr = new ProfilerPreset[presets.size() + 1];
        int index = 0;
        presetsArr[index++] = createDefaultPreset(application);
        for (ProfilerPreset preset : presets) presetsArr[index++] = preset;
        return presetsArr;
    }

    private void loadPresets() {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                final List<ProfilerPreset> loadedPresets = doLoadPresets();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        presets.clear();
                        presets.addAll(loadedPresets);
                    }
                });
            }
        });
    }

    void savePresets(final PresetsModel toSave) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                presets.clear();
                Enumeration en = toSave.elements();
                while (en.hasMoreElements())
                    presets.add((ProfilerPreset)en.nextElement());
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() { doSavePresets(toSave); }
                });
            }
        });
    }

    PresetsModel getPresets() {
        PresetsModel model = new PresetsModel();
        for (ProfilerPreset preset : presets) model.addElement(preset);
        return model;
    }
    

    private static ProfilerPreset createDefaultPreset(Application application) {
        ProfilerPreset defaultPreset = new ProfilerPreset(NbBundle.getMessage(
                ProfilerPresets.class, "MSG_Default"), ""); // NOI18N
        defaultPreset.setFilterS(getDefaultFiltersS());
        defaultPreset.setRootsP(getDefaultRootsP(application));
        defaultPreset.setFilterP(getDefaultFiltersP(defaultPreset.getRootsP()));
        return defaultPreset;
    }

    private static String getMainClass(Application application) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        String mainClass = jvm.getMainClass();
        if (mainClass == null || mainClass.trim().length() == 0) {
            mainClass = ""; // NOI18N
        } else if (mainClass.endsWith(JAR_SUFFIX)) {
            // application is launched with -jar and uses relative path, try to find jar
            mainClass = ""; // NOI18N
            Properties sysProp = jvm.getSystemProperties();
            if (sysProp != null) {
                String userdir = sysProp.getProperty("user.dir");     // NOI18N
                if (userdir != null) {
                    String args = jvm.getCommandLine();
                    int index = args.indexOf(JAR_SUFFIX);
                    if (index != -1) {
                        File jarFile = new File(userdir,args.substring(0,index+JAR_SUFFIX.length()));
                        if (jarFile.exists()) {
                            try {
                                JarFile jf = new JarFile(jarFile);
                                String mainClassName = jf.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                                assert mainClassName!=null;
                                mainClass = mainClassName.replace('\\', '/').replace('/', '.'); // NOI18N
                            } catch (IOException ex) {
//                                LOGGER.log(Level.INFO, "getMainClass", ex);   // NOI18N
                            }
                        }
                    }
                }
            }
        }
        return mainClass;
    }
    
    private static String getDefaultFiltersS() {
        return Utilities.isMac() ?
            "java.*, javax.*,\nsun.*, sunw.*, com.sun.*,\ncom.apple.*, apple.awt.*, apple.laf.*" : // NOI18N
            "java.*, javax.*,\nsun.*, sunw.*, com.sun.*"; // NOI18N
    }

    private static String getDefaultRootsP(Application application) {
        String mainClass = getMainClass(application);
        int dotIndex = mainClass.lastIndexOf("."); // NOI18N
        if (dotIndex == -1) return ""; // NOI18N
        else return mainClass.substring(0, dotIndex + 1) + "**"; // NOI18N
    }

    private static String getDefaultFiltersP(String defaultRoots) {
        if (defaultRoots.isEmpty())
            return !Utilities.isMac() ? "sun.*, sunw.*, com.sun.*" : // NOI18N
                "sun.*, sunw.*, com.sun.*,\ncom.apple.*, apple.awt.*, apple.laf.*"; // NOI18N
        else
            return !Utilities.isMac() ? "java.*, javax.*,\nsun.*, sunw.*, com.sun.*" : // NOI18N
                "java.*, javax.*,\nsun.*, sunw.*, com.sun.*,\ncom.apple.*, apple.awt.*, apple.laf.*"; // NOI18N
    }

    
    private static File[] getPresetFiles() {
        if (storageDirectoryExists()) {
            return getStorageDirectory().listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(PRESETS_STORAGE_PREFIX) &&
                           name.endsWith(Storage.DEFAULT_PROPERTIES_EXT);
                }
            });
        } else {
            return null;
        }
    }

    private static List<ProfilerPreset> doLoadPresets() {
        List<ProfilerPreset> loadedPresets = new ArrayList();

        File[] presetFiles = getPresetFiles();
        if (presetFiles != null) {
            Arrays.sort(presetFiles, new Comparator<File>() {
                private int prefixL = PRESETS_STORAGE_PREFIX.length();
                private int suffixL = Storage.DEFAULT_PROPERTIES_EXT.length();
                public int compare(File f1, File f2) {
                    String n1 = f1.getName().substring(prefixL);
                    n1 = n1.substring(0, n1.length() - suffixL);
                    String n2 = f2.getName().substring(prefixL);
                    n2 = n2.substring(0, n2.length() - suffixL);
                    try {
                        return Integer.valueOf(n1).compareTo(Integer.valueOf(n2));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            });

            File storageDir = getStorageDirectory();
            for (File file : presetFiles) {
                Storage storage = new Storage(storageDir, file.getName());
                loadedPresets.add(new ProfilerPreset(storage));
            }
        }
        
        return loadedPresets;
    }

    private static void doSavePresets(PresetsModel toSave) {
        File[] presetFiles = getPresetFiles();
        if (presetFiles != null)
            for (File file : presetFiles) Utils.delete(file, false);

        File storageDir = getStorageDirectory();
        int count = toSave.size();
        for (int i = 0; i < count; i++) {
            ProfilerPreset preset = (ProfilerPreset)toSave.get(i);
            String presetFile = PRESETS_STORAGE_PREFIX + i +
                                Storage.DEFAULT_PROPERTIES_EXT;
            preset.toStorage(new Storage(storageDir, presetFile));
        }
    }


    private static String getStorageDirectoryString() {
        synchronized(profilerStorageDirectoryStringLock) {
            if (profilerStorageDirectoryString == null)
                profilerStorageDirectoryString = Storage.getPersistentStorageDirectoryString() +
                        File.separator + PROFILER_STORAGE_DIRNAME;
            return profilerStorageDirectoryString;
        }
    }

    private static File getStorageDirectory() {
        synchronized(profilerStorageDirectoryLock) {
            if (profilerStorageDirectory == null) {
                String presetsStorageString = getStorageDirectoryString();
                profilerStorageDirectory = new File(presetsStorageString);
                if (profilerStorageDirectory.exists() && profilerStorageDirectory.isFile())
                    throw new IllegalStateException("Cannot create profiler storage directory " + presetsStorageString + ", file in the way");   // NOI18N
                if (profilerStorageDirectory.exists() && (!profilerStorageDirectory.canRead() || !profilerStorageDirectory.canWrite()))
                    throw new IllegalStateException("Cannot access profiler storage directory " + presetsStorageString + ", read&write permission required");    // NOI18N
                if (!Utils.prepareDirectory(profilerStorageDirectory))
                    throw new IllegalStateException("Cannot create profiler storage directory " + presetsStorageString); // NOI18N
            }
            return profilerStorageDirectory;
        }
    }

    private static boolean storageDirectoryExists() {
        return new File(getStorageDirectoryString()).isDirectory();
    }


    private ProfilerPresets() {
        presets = new ArrayList();
        selectors = new HashSet();
        
        loadPresets();
    }


    static class PresetsModel extends DefaultListModel {

        void addPreset(ProfilerPreset preset) {
            addElement(preset);
        }

        void removePreset(int preset) {
            removeElementAt(preset);
        }

        void movePresetUp(int preset) {
            Object o = elementAt(preset);
            remove(preset);
            add(preset - 1, o);
        }

        void movePresetDown(int preset) {
            Object o = elementAt(preset);
            remove(preset);
            add(preset + 1, o);
        }

        public void fireItemChanged(int itemIndex) {
            fireContentsChanged(this, itemIndex, itemIndex);
        }

    }

}
