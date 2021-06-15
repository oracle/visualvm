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

import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.image.FieldAccessor.InvalidFieldException;

// Convertors of instance values --------------------------------------------
/**
 * Reconstruct object from the heap instance.
 * @author Jan Taus
 */
abstract class InstanceBuilder<T> {

    public static final InstanceBuilder<String> STRING_BUILDER = new InstanceBuilder<String>(String.class) {
        @Override
        public String convert(FieldAccessor accessor, Instance instance) throws InvalidFieldException {
            return accessor.toString(instance);
        }
    };
    /**
     * Builds
     * <code>int[]</code> from {@link PrimitiveArrayInstance}
     */
    public static final InstanceBuilder<int[]> INT_ARRAY_BUILDER = new InstanceBuilder<int[]>(int[].class) {
        @Override
        public int[] convert(FieldAccessor accessor, Instance instance) throws InvalidFieldException {
            if (instance == null) {
                return null;
            }
            PrimitiveArrayInstance array = FieldAccessor.castValue(instance, PrimitiveArrayInstance.class);
            List<String> list = array.getValues();
            int[] result = new int[list.size()];
            for (int i = 0; i < result.length; i++) {
                try {
                    result[i] = Integer.parseInt(list.get(i));
                } catch (NumberFormatException e) {
                    throw new InvalidFieldException("invalid format of int at index %d: %s", i, list.get(i));
                }
            }
            return result;
        }
    };
    /**
     * Builds
     * <code>byte[]</code> from {@link PrimitiveArrayInstance}
     */
    public static final InstanceBuilder<byte[]> BYTE_ARRAY_BUILDER = new InstanceBuilder<byte[]>(byte[].class) {
        @Override
        public byte[] convert(FieldAccessor accessor, Instance instance) throws InvalidFieldException {
            if (instance == null) {
                return null;
            }
            PrimitiveArrayInstance array = FieldAccessor.castValue(instance, PrimitiveArrayInstance.class);
            List<String> list = array.getValues();
            byte[] result = new byte[list.size()];
            for (int i = 0; i < result.length; i++) {
                try {
                    result[i] = Byte.parseByte(list.get(i));
                } catch (NumberFormatException e) {
                    throw new InvalidFieldException("invalid format of byte at index %d: %s", i, list.get(i));
                }
            }
            return result;
        }
    };
    /**
     * Builds
     * <code>short[]</code> from {@link PrimitiveArrayInstance}
     */
    public static final InstanceBuilder<short[]> SHORT_ARRAY_BUILDER = new InstanceBuilder<short[]>(short[].class) {
        @Override
        public short[] convert(FieldAccessor accessor, Instance instance) throws InvalidFieldException {
            if (instance == null) {
                return null;
            }
            PrimitiveArrayInstance array = FieldAccessor.castValue(instance, PrimitiveArrayInstance.class);
            List<String> list = array.getValues();
            short[] result = new short[list.size()];
            for (int i = 0; i < result.length; i++) {
                try {
                    result[i] = Short.parseShort(list.get(i));
                } catch (NumberFormatException e) {
                    throw new InvalidFieldException("invalid format of short at index %d: %s", i, list.get(i));
                }
            }
            return result;
        }
    };
    /**
     * Builder which returns original instance.
     */
    public static final InstanceBuilder<Instance> IDENTITY_BUILDER = new InstanceBuilder<Instance>(Instance.class) {
        @Override
        public Instance convert(FieldAccessor accessor, Instance instance) throws InvalidFieldException {
            return instance;
        }
    };

    /**
     * Builds object in the field of the instance.
     */
    public static class ReferringInstanceBuilder<T> extends InstanceBuilder<T> {

        private final String[] path;

        public ReferringInstanceBuilder(Class<T> type, String... path) {
            super(type);
            this.path = path;
        }

        @Override
        public T convert(FieldAccessor fa, Instance instance) throws InvalidFieldException {
            for (int i = 0; i < path.length - 1 && instance != null; i++) {
                instance = fa.getInstance(instance, path[i], false);
            }
            if (instance == null) {
                return null;
            }
            return fa.build(instance, path[path.length - 1], getType(), false);
        }
    }
    private final Class<T> type;

    InstanceBuilder(Class<T> type) {
        this.type = type;
    }

    /**
     * Return type of the created objects. Function used to access class from generic context (e.g. allocating arrays).
     */
    Class<T> getType() {
        return type;
    }

    /**
     * Reconstruct object from the instance.
     *
     * @throws InvalidFieldException if the reconstructions failed
     */
    public abstract T convert(FieldAccessor accessor, Instance instance) throws FieldAccessor.InvalidFieldException;
}
