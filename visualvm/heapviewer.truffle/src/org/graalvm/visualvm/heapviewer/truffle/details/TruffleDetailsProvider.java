/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.heapviewer.truffle.details;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class TruffleDetailsProvider extends DetailsProvider.Basic {

    private static final String DEFAULT_CALL_TARGET_MASK = "com.oracle.truffle.api.impl.DefaultCallTarget";   // NOI18N
    private static final String OPTIMIZED_CALL_TARGET_MASK = "org.graalvm.compiler.truffle.OptimizedCallTarget"; //NOI18N
    private static final String OPTIMIZED_CALL_TARGET1_MASK = "org.graalvm.compiler.truffle.runtime.OptimizedCallTarget"; // NOI18N
    private static final String ENT_OPTIMIZED_CALL_TARGET_MASK = "com.oracle.graal.truffle.OptimizedCallTarget"; // NOI18N
    private static final String LANG_INFO_MASK = "com.oracle.truffle.api.nodes.LanguageInfo"; // NOI18N
    private static final String LANG_CACHE_MASK = "com.oracle.truffle.api.vm.LanguageCache"; // NOI18N
    private static final String POLYGLOT_MASK = "com.oracle.truffle.api.vm.PolyglotLanguage";   // NOI18N
    private static final String INSTRUMENT_INFO_MASK = "com.oracle.truffle.api.InstrumentInfo"; // NOI18N
    private static final String NATIVE_ROOT_MASK = "com.oracle.truffle.nfi.LibFFIFunctionMessageResolutionForeign$ExecuteLibFFIFunctionSubNode$EXECUTERootNode"; // NOI18N

    public TruffleDetailsProvider() {
        super(DEFAULT_CALL_TARGET_MASK, OPTIMIZED_CALL_TARGET_MASK, OPTIMIZED_CALL_TARGET1_MASK,
                ENT_OPTIMIZED_CALL_TARGET_MASK, LANG_INFO_MASK, LANG_CACHE_MASK,
                POLYGLOT_MASK, INSTRUMENT_INFO_MASK, NATIVE_ROOT_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (DEFAULT_CALL_TARGET_MASK.equals(className)) {
            String rootNode = DetailsUtils.getInstanceFieldString(instance, "rootNode", heap); // NOI18N

            if (rootNode != null) {
                return rootNode;
            }
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N 
        }
        if (OPTIMIZED_CALL_TARGET_MASK.equals(className)
                || OPTIMIZED_CALL_TARGET1_MASK.equals(className)
                || ENT_OPTIMIZED_CALL_TARGET_MASK.equals(className)) {
            String rootNode = DetailsUtils.getInstanceFieldString(instance, "rootNode", heap); // NOI18N

            if (rootNode != null) {
                Object entryPoint = instance.getValueOfField("entryPoint"); // NOI18N

                if (entryPoint instanceof Long && ((Long) entryPoint).longValue() != 0) {
                    rootNode += " <opt>"; // NOI18N
                }
                if (instance.getValueOfField("sourceCallTarget") != null) { // NOI18N
                    rootNode += " <split-" + Long.toHexString(instance.getInstanceId()) + ">"; // NOI18N
                }
                return rootNode;
            } else {
                return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
            }
        }
        if (LANG_INFO_MASK.equals(className) || LANG_CACHE_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
            String version = DetailsUtils.getInstanceFieldString(instance, "version", heap); // NOI18N

            if (name != null && version != null) {
                return name + " (version " + version + ")"; // NOI18N
            }
            return name;
        }
        if (POLYGLOT_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "info", heap); // NOI18N
        }
        if (INSTRUMENT_INFO_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
            String version = DetailsUtils.getInstanceFieldString(instance, "version", heap); // NOI18N

            if (name != null && !name.isEmpty() && version != null && !version.isEmpty()) {
                return name + " (version " + version + ")"; // NOI18N
            }
            if (name == null || name.isEmpty()) {
                return DetailsUtils.getInstanceFieldString(instance, "id", heap); // NOI18N
            }
            return name;
        }
        if (NATIVE_ROOT_MASK.equals(className)) {
            return "native call"; // NOI18N
        }
        return null;
    }
}
