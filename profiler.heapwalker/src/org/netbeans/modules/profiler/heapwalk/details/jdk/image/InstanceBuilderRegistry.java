/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.details.jdk.image;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.lib.profiler.heap.Instance;

/**
 * Support for automatic selection of {@link InstanceBuilder} for an instance.
 *
 * @author Jan Taus
 */
class InstanceBuilderRegistry {

    private static class RegisteredBuilder {

        private final String mask;
        private final InstanceBuilder<?> builder;

        public RegisteredBuilder(String mask, InstanceBuilder<?> builder) {
            this.mask = mask;
            this.builder = builder;
        }
    }
    private final List<RegisteredBuilder> builders;

    public InstanceBuilderRegistry() {
        this.builders = new ArrayList<RegisteredBuilder>();
    }

    public void register(String mask, InstanceBuilder<?> builder) {
        builders.add(new RegisteredBuilder(mask, builder));
    }

    public void register(Class<?> type, boolean subtypes, InstanceBuilder<?> builder) {
        register(FieldAccessor.getClassMask(type, subtypes), builder);
    }

    /**
     * Returns builder which creates object of given
     * <code>type</code>. First registered builder matching given type and registered for given instance is returned. No
     * <em>best match</em> is performed.
     *
     * @return builder or <code>null</code>.
     */
    public <T> InstanceBuilder<? extends T> getBuilder(Instance instance, Class<T> type) {
        for (RegisteredBuilder builder : builders) {
            if (FieldAccessor.matchClassMask(instance, builder.mask)) {
                if (type.isAssignableFrom(builder.builder.getType())) {
                    return (InstanceBuilder<? extends T>) builder.builder;
                }
            }
        }
        return null;
    }

    public String[] getMasks(Class<?>... types) {
        Set<String> masks = new HashSet<String>();
        for (RegisteredBuilder builder : builders) {
            for (Class<?> type : types) {
                if (type.isAssignableFrom(builder.builder.getType())) {
                    masks.add(builder.mask);
                }
            }
        }
        return masks.toArray(new String[0]);
    }
}
