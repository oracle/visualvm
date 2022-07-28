/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle;

import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleTypeNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleObjectNode;
import org.graalvm.visualvm.heapviewer.truffle.nodes.TruffleLocalObjectNode;
import org.graalvm.visualvm.heapviewer.HeapFragment;
import org.graalvm.visualvm.heapviewer.truffle.dynamicobject.DynamicObject;
import java.awt.Image;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TruffleLanguage<O extends TruffleObject, T extends TruffleType<O>, F extends TruffleLanguageHeapFragment<O, T>> extends HeapFragment.Provider {
    
    private final Map<Heap, Reference<F>> fragments = new WeakHashMap<>();
    
    protected abstract F createFragment(Heap heap);
    
    // HeapFragment.Provider implementation, not to be used by client code
    public final List<HeapFragment> getFragments(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) {
        HeapFragment fragment = fragmentFromHeap(heap);
        return fragment == null ? null : Collections.singletonList(fragment);
    }
    
    public synchronized final F fragmentFromHeap(Heap heap) {
        Reference<F> fragmentRef = fragments.get(heap);
        if (fragmentRef == null) {
            F fragment = createFragment(heap);
            if (fragment == null) return null;
            
            fragments.put(heap, new WeakReference<>(fragment));
            return fragment;
        } else {
            return fragmentRef.get();
        }
    }
    
    
    public abstract String getID();
    
    
    private Image badgeImage;
    protected synchronized Image badgeImage() {
        if (badgeImage == null) {
            String path = getClass().getPackage().getName().replace('.', '/') + "/" + getID() + "_badge.png"; // NOI18N
            badgeImage = ImageUtilities.loadImage(path, true);
        }
        return badgeImage;
    }
    
    public Icon createLanguageIcon(Icon icon) {
        return new ImageIcon(ImageUtilities.mergeImages(ImageUtilities.icon2Image(icon), badgeImage(), 0, 0));
    }
    
    
    public abstract Class<O> getLanguageObjectClass();
    
    
    public abstract boolean isLanguageObject(Instance instance);
    
    public abstract O createObject(Instance instance);
    
    public abstract T createType(String name);
    
    
    public abstract TruffleObjectNode<O> createObjectNode(O object, String type);
    
    public abstract TruffleLocalObjectNode<O> createLocalObjectNode(O object, String type);
    
    public abstract TruffleTypeNode<O, T> createTypeNode(T type, Heap heap);
    
    
    private static final String LANGUAGE_INFO_FQN = "com.oracle.truffle.api.nodes.LanguageInfo"; // NOI18N
    private static final String LANGUAGE_CACHE_FQN = "com.oracle.truffle.api.vm.LanguageCache"; // NOI18N
    private static final String LANGUAGE_CACHE1_FQN = "com.oracle.truffle.polyglot.LanguageCache"; // NOI18N
    private static final String NAME_FIELD = "name";    // NOI18N
    
    protected static Instance getLanguageInfo(Heap heap, String languageID) {
        // check for DynamicObject
        if (!DynamicObject.hasDynamicObject(heap)) return null;
        
        // check for LanguageInfo
        JavaClass langInfoClass = heap.getJavaClassByName(LANGUAGE_INFO_FQN);
        if (!checkLangClass(langInfoClass)) {
            langInfoClass = heap.getJavaClassByName(LANGUAGE_CACHE_FQN);
            if (!checkLangClass(langInfoClass)) {
                langInfoClass = heap.getJavaClassByName(LANGUAGE_CACHE1_FQN);
                if (!checkLangClass(langInfoClass)) return null;
            }
        }
        
        // search the language
        List<Instance> langInfos = langInfoClass.getInstances();
        for (Instance langInfo : langInfos) {
            String langName = DetailsUtils.getInstanceFieldString(langInfo, "name");    // NOI18N
            if (languageID.equals(langName)) return langInfo;
        }
        
        return null;
    }
    
    private static boolean checkLangClass(JavaClass infoClass) {
        if (infoClass != null) {
            List<Field> fields = infoClass.getFields();
            for (Field field : fields)
                if (NAME_FIELD.equals(field.getName())) return true;
        }
        return false;
    }
    
}
