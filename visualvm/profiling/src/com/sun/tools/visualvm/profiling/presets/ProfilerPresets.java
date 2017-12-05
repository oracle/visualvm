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

package com.sun.tools.visualvm.profiling.presets;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerPresets {

    private static final String JAR_SUFFIX = ".jar";  // NOI18N    
    private static final String OPTIONS_HANDLE = "ProfilerOptions"; // NOI18N
    private static final String PROP_PRESET_HEADER = "prof_preset_header"; // NOI18N

    private static ProfilerPresets INSTANCE;
    
    private Preferences prefs;

    private List<ProfilerPreset> presets;
    private ProfilerPreset presetToSelect;
    private ProfilerPreset presetToCreate;

    private final Set<WeakReference<PresetSelector>> selectors;
    
    static final String DEFINE_CLASSES = NbBundle.getMessage(ProfilerPresets.class, "HINT_Define_roots"); // NOI18N


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
                Map<DefaultComboBoxModel, Boolean> models = new HashMap();
                Iterator<WeakReference<PresetSelector>> selectorsI =
                        selectors.iterator();
                
                while (selectorsI.hasNext()) {
                    WeakReference<PresetSelector> selectorR = selectorsI.next();
                    PresetSelector selector = selectorR.get();
                    if (selector == null) {
                        selectorsI.remove();
                    } else if (SwingUtilities.getRoot(selector) == null) {
                        selectorsI.remove();
                    } else {
                        DefaultComboBoxModel model = selector.getModel();
                        boolean savingCustom = selector.checkSavingCustom();
                        if (!Boolean.TRUE.equals(models.get(model)))
                            models.put(model, savingCustom);
                    }
                }
                
                for (DefaultComboBoxModel model : models.keySet()) {
                    ProfilerPreset selectedPreset = (ProfilerPreset)model.getSelectedItem();
                    ProfilerPreset defaultPreset = (ProfilerPreset)model.getElementAt(0);
                    ProfilerPreset customPreset = model.getSize() < 2 ? null :
                                                  (ProfilerPreset)model.getElementAt(1);
                    if (customPreset != null && !PresetSelector.isCustomPreset(customPreset))
                        customPreset = null;
                    
                    boolean savingCustom = models.get(model);
                    
//                    internalChange = true;

                    model.removeAllElements();
                    model.addElement(defaultPreset);
                    if (!savingCustom && customPreset != null) model.addElement(customPreset);
                    for (ProfilerPreset preset : presets) model.addElement(preset);
                    
                    ProfilerPreset toSelect = savingCustom && selected != null ? selected :
                                              selectedPreset;
                    if (model.getIndexOf(toSelect) >= 0) model.setSelectedItem(toSelect);
                    else model.setSelectedItem(model.getElementAt(0));

//                    internalChange = false;
                }
            }
        });
    }
    
    public PresetSelector createSelector(DefaultComboBoxModel selectorsModel,
                                         List<PresetSelector> allSelectors,
                                         Runnable presetSync) {
        return createSelector(null, selectorsModel, allSelectors, presetSync);
    }
    
    public PresetSelector createSelector(Application application,
                                         DefaultComboBoxModel selectorsModel,
                                         List<PresetSelector> allSelectors,
                                         Runnable presetSync) {
        
        
        
        if (selectorsModel.getSize() == 0) {
            for (ProfilerPreset preset : getPresets(application))
                selectorsModel.addElement(preset);
            
            String mainClass = null;
        
            if (application != null) {
                mainClass = getMainClass(application);
                if (mainClass == null || mainClass.isEmpty()) mainClass =
                        ApplicationTypeFactory.getApplicationTypeFor(application).getName();
                if (mainClass == null || mainClass.isEmpty()) mainClass =
                        DataSourceDescriptorFactory.getDescriptor(application).getName();
            }
            
            ProfilerPreset defaultPreset = (ProfilerPreset)selectorsModel.getElementAt(0);
            defaultPreset.setSelector(mainClass);

            ProfilerPreset toSelect = defaultPreset;
            if (mainClass != null && !mainClass.isEmpty()) {
                String mainClassL = mainClass.toLowerCase();
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
            
            selectorsModel.setSelectedItem(toSelect);
        }
        
        PresetSelector selector = new PresetSelector(selectorsModel, allSelectors,
                                                     presetSync);
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
        defaultPreset.setMemoryFilterP(getDefaultMemoryFilterP(application));
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
            if (jvm.isGetSystemPropertiesSupported()) {
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
        }
        return mainClass;
    }
    
    private static String getDefaultFiltersS() {
        return Utilities.isMac() ?
            "java.*, javax.*,\nsun.*, sunw.*, com.sun.*,\ncom.apple.*, apple.awt.*, apple.laf.*" : // NOI18N
            "java.*, javax.*,\nsun.*, sunw.*, com.sun.*"; // NOI18N
    }

    private static String getDefaultRootsP(Application application) {
        if (application == null) return DEFINE_CLASSES;
        String mainClass = getMainClass(application);
        if ("".equals(mainClass)) return DEFINE_CLASSES; // unknown main class
        int dotIndex = mainClass.lastIndexOf("."); // NOI18N
        if (dotIndex == -1) return mainClass;  // default package
        else return mainClass.substring(0, dotIndex + 1) + "**"; // NOI18N
    }

    private static String getDefaultFiltersP(String defaultRoots) {
        if (defaultRoots.isEmpty())
            return !Utilities.isMac() ? "sun.**, sunw.**, com.sun.**" : // NOI18N
                "sun.**, sunw.**, com.sun.**,\ncom.apple.**, apple.awt.**, apple.laf.**"; // NOI18N
        else
            return !Utilities.isMac() ? "java.**, javax.**,\nsun.**, sunw.**, com.sun.**" : // NOI18N
                "java.**, javax.**,\nsun.**, sunw.**, com.sun.**,\ncom.apple.**, apple.awt.**, apple.laf.**"; // NOI18N
    }
    
    private static String getDefaultMemoryFilterP(Application application) {
        return DEFINE_CLASSES; // NOI18N
    }


    private List<ProfilerPreset> doLoadPresets() {
        Preferences p = prefs();
        List<ProfilerPreset> loadedPresets = new ArrayList();
        
        int i = 0;
        String prefix = i + "_"; // NOI18N
        while (p.get(prefix + PROP_PRESET_HEADER, null) != null) {
            loadedPresets.add(new ProfilerPreset(p, prefix));
            prefix = ++i + "_"; // NOI18N
        }
        
        return loadedPresets;
    }

    private void doSavePresets(PresetsModel toSave) {
        Preferences p = prefs();
        try { p.clear(); } catch (Exception e) {}
        int count = toSave.size();
        for (int i = 0; i < count; i++) {
            String prefix = i + "_"; // NOI18N
            p.put(prefix + PROP_PRESET_HEADER, ""); // NOI18N
            ProfilerPreset preset = (ProfilerPreset)toSave.get(i);
            preset.toPreferences(p, prefix); // NOI18N
        }
    }
    
    private synchronized Preferences prefs() {
        if (prefs == null) prefs = NbPreferences.forModule(ProfilerPresets.class);
        return prefs;
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
