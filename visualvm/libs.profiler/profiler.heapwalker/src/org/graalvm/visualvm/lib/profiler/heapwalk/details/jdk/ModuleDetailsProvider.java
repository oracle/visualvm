/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
        if (MODULE_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "descriptor");   // NOI18N
            if (name == null) {
                return "unnamed module #" + instance.getInstanceNumber();   // NOI18N
            }
            return name;
        }
        if (MODULE_DESCR_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "name");   // NOI18N
            String version = DetailsUtils.getInstanceFieldString(instance, "version");   // NOI18N

            if (version == null) {
                return name;
            }
            return name + "@" + version;
        }
        if (MODULE_VERSION_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "version");   // NOI18N
        }
        if (RES_MODULE_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "mref");   // NOI18N
        }
        if (MODULE_REF_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "descriptor");   // NOI18N
            String loc = DetailsUtils.getInstanceFieldString(instance, "location");   // NOI18N
            boolean patcher = instance.getValueOfField("patcher") != null;
            String patched = patcher ? " (patched)" : "";
            
            if (loc == null) {
                return String.valueOf(name) + patched;            // NOI18N
            }
            return String.valueOf(name) + ", " + loc + patched;  // NOI18N
        }
        return null;
    }
}
