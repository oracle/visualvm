/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.image;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;

/**
 * Support for automatic selection of {@link InstanceBuilder} for an instance.
 *
 * @author Jan Taus
 */
class InstanceBuilderRegistry {

    private static class RegisteredBuilder {

        private final String mask;
        private final InstanceBuilder<?> builder;

        RegisteredBuilder(String mask, InstanceBuilder<?> builder) {
            this.mask = mask;
            this.builder = builder;
        }
    }
    private final List<RegisteredBuilder> builders;

    InstanceBuilderRegistry() {
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
