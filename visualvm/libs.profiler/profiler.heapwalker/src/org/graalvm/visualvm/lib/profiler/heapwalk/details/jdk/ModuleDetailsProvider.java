/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public final class ModuleDetailsProvider extends DetailsProvider.Basic {

    private static final String MODULE_MASK = "java.lang.Module";        // NOI18N
    private static final String MODULE_DESCR_MASK = "java.lang.module.ModuleDescriptor";        // NOI18N
    private static final String MODULE_VERSION_MASK = "java.lang.module.ModuleDescriptor$Version";  // NOI18N
    private static final String MODULE_REF_MASK = "java.lang.module.ModuleReference+";  // NOI18N
    private static final String RES_MODULE_MASK = "java.lang.module.ResolvedModule";  // NOI18N

    public ModuleDetailsProvider() {
        super(MODULE_MASK, MODULE_DESCR_MASK, MODULE_VERSION_MASK,
                MODULE_REF_MASK, RES_MODULE_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case MODULE_MASK: {
                String name = DetailsUtils.getInstanceFieldString(instance, "descriptor");   // NOI18N
                if (name == null) {
                    return "unnamed module #" + instance.getInstanceNumber();   // NOI18N
                }
                return name;
            }
            case MODULE_DESCR_MASK: {
                String name = DetailsUtils.getInstanceFieldString(instance, "name");   // NOI18N
                String version = DetailsUtils.getInstanceFieldString(instance, "version");   // NOI18N

                if (version == null) {
                    return name;
                }
                return name + "@" + version;
            }
            case MODULE_VERSION_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "version");   // NOI18N
            case RES_MODULE_MASK:
                return DetailsUtils.getInstanceFieldString(instance, "mref");   // NOI18N
            case MODULE_REF_MASK: {
                String name = DetailsUtils.getInstanceFieldString(instance, "descriptor");   // NOI18N
                String loc = DetailsUtils.getInstanceFieldString(instance, "location");   // NOI18N
                boolean patcher = instance.getValueOfField("patcher") != null;
                String patched = patcher ? " (patched)" : "";

                if (loc == null) {
                    return String.valueOf(name) + patched;            // NOI18N
                }
                return String.valueOf(name) + ", " + loc + patched;  // NOI18N
            }
            default:
                break;
        }
        return null;
    }
}
