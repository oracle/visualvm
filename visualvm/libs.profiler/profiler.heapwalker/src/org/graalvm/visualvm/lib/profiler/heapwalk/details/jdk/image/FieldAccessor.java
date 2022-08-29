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

import java.lang.reflect.Array;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 * Utilities to access fields of heap dump instances.
 *
 * @author Jan Taus
 */
public class FieldAccessor {

    private final InstanceBuilderRegistry registry;

    public FieldAccessor() {
        this(new InstanceBuilderRegistry());
    }

    FieldAccessor(InstanceBuilderRegistry registry) {
        this.registry = registry;
    }

    // Utils -----------------------------------------------------------------------------
    public static String getClassMask(Class<?> type, boolean subtypes) {
        if (subtypes) {
            return type.getName() + "+"; // NOI18N
        }
        return type.getName();
    }

    public static boolean matchClassMask(Instance instance, String mask) {
        if (mask.endsWith("+")) {
            return DetailsUtils.isSubclassOf(instance, mask.substring(0, mask.length() - 1));
        }
        return DetailsUtils.isInstanceOf(instance, mask);
    }

    public static boolean isInstanceOf(Instance instance, Class<?> type) {
        if (instance == null) {
            return false;
        }
        return instance.getJavaClass().getName().equals(type.getName());
    }

    /**
     * Exception thrown by the field accessors if the field doesn't exists or has unexpected value.
     */
    public static class InvalidFieldException extends Exception {

        public InvalidFieldException() {
            super();
        }

        public InvalidFieldException(String message, Object... args) {
            super(String.format(message, args));
        }

        public InvalidFieldException(Instance instance, String field, String message) {
            this(instance, field, "%s", message);
        }

        public InvalidFieldException(Instance instance, String field, String message, Object... args) {
            this("%s#%d->%s: %s", // NOI18N
                    instance == null ? "null" : instance.getJavaClass().getName(), // NOI18N
                    instance == null ? -1: instance.getInstanceNumber(), // NOI18N
                    field, String.format(message, args));
        }

        public InvalidFieldException(String message) {
            super(message);
        }

        @Override
        public synchronized InvalidFieldException initCause(Throwable cause) {
            super.initCause(cause);
            return this;
        }
    }

    public static <T> T notNull(T value) throws InvalidFieldException {
        return notNullCheck(value, false);
    }

    public static <T> T notNullCheck(T value, boolean allowNull) throws InvalidFieldException {
        if (!allowNull && value == null) {
            throw new InvalidFieldException("Unexpected null value"); // NOI18N
        }
        return value;
    }

    public static <T> T castValue(Object value, Class<T> type) throws InvalidFieldException {
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new InvalidFieldException("Value is %s but %s is expected", // NOI18N
                    value.getClass().getName(),
                    type.getName());
        }
        return type.cast(value);
    }

    // Checked field accessor   --------------------------------------------
    /**
     * Return value of the field casted to the specific object (primitive type object or instance subclass).
     *
     * @param allowNull if false then null is never returned and call fails with exception
     */
    public <T> T get(Instance instance, String field, Class<T> type, boolean allowNull) throws InvalidFieldException {
        Object value = instance.getValueOfField(field);
        if (value == null) {
            if (allowNull) {
                return null;
            }
            throw new InvalidFieldException(instance, field, "Empty or missing field");  // NOI18N
        }
        try {
            return castValue(value, type);
        } catch (InvalidFieldException ex) {
            throw new InvalidFieldException(instance, field, ex.getMessage()).initCause(ex);
        }
    }
    // Instance accessors  --------------------------------------------

    /**
     * Return value of the field casted as {@link Instance}.
     *
     * @see #get(org.graalvm.visualvm.lib.jfluid.heap.Instance, java.lang.String, java.lang.Class, boolean)
     */
    public Instance getInstance(Instance instance, String field, boolean allowNull)
            throws InvalidFieldException {
        return get(instance, field, Instance.class, allowNull);
    }

    /**
     * Return value of the field casted as {@link Instance}. Check if the instance java class correspond to the supplied
     * class. Simplifies call to {@link #getInstance(Instance, String, String) getInstance(instance, field, className)
     * } if the class object is available.
     *
     * @param type source of class name used in Instance type checking
     * @param subclasses match also subclasses of the <code>type<code>
     * @return never null
     */
    public Instance getInstance(Instance instance, String field, Class<?> type, boolean subclasses) throws InvalidFieldException {
        return getInstance(instance, field, getClassMask(type, subclasses));
    }

    /**
     * Return value of the field casted as {@link Instance}. Check the java class name of the returned instance.
     *
     * @param clsName name of the expected class, allow subclasses if the name end with '+'
     * @return never null
     */
    public Instance getInstance(Instance instance, String field, String clsName) throws InvalidFieldException {
        Instance value = getInstance(instance, field, false);
        if (clsName.endsWith("+")) {
            clsName = clsName.substring(0, clsName.length() - 1);
            if (!DetailsUtils.isSubclassOf(value, clsName)) {
                throw new InvalidFieldException(instance, field, "Instance is %s but subclass of %s is expected", //NOI18N
                        value.getJavaClass().getName(),
                        clsName);
            }
        } else {
            if (!DetailsUtils.isInstanceOf(value, clsName)) {
                throw new InvalidFieldException(instance, field, "Instance is %s but %s is expected", //NOI18N
                        value.getJavaClass().getName(),
                        clsName);
            }
        }
        return value;
    }

    <T> T build(Instance instance, String field, InstanceBuilder<T> builder, boolean allowNull)
            throws InvalidFieldException {
        return builder.convert(this, get(instance, field, Instance.class, allowNull));
    }

    /**
     * Builds using registry
     */
    public <T> T build(Instance instance, String field, Class<T> type, boolean allowNull)
            throws InvalidFieldException {
        Instance value = get(instance, field, Instance.class, allowNull);
        if (value == null) {
            return null;
        }
        InstanceBuilder<? extends T> builder = registry.getBuilder(value, type);
        if (builder == null) {
            if (allowNull) {
                return null;
            }
            throw new InvalidFieldException(instance, field, "No builder for %s returning %s registered", //NOI18N
                    value.getJavaClass().getName(), type.getName());
        }
        return builder.convert(this, value);
    }

    /**
     * Convert filed of the instance to the array of represented objects. Use builder registry to get builder for each
     * item in the array.
     *
     * @param instance parent instance of the array
     * @param field name of the field containing the array
     * @param type base type of returned array
     * @param allowNull never return null, throw exception instead
     * @param allowNullValues returned array can contain nulls
     */
    public <T> T[] buildArray(Instance instance, String field, Class<T> type,
            boolean allowNull, boolean allowNullValues) throws InvalidFieldException {
        return buildArray(instance, field, type, null, allowNull, allowNullValues);
    }

    /**
     * Convert filed of the instance to the array of represented objects. Use given builder to convert the array items.
     *
     * @param instance parent instance of the array
     * @param field name of the field containing the array
     * @param builder builder used to convert items in the array
     * @param allowNull never return null, throw exception instead
     * @param allowNullValues returned array can contain nulls
     */
    <T> T[] buildArray(Instance instance, String field, InstanceBuilder<T> builder,
            boolean allowNull, boolean allowNullValues) throws InvalidFieldException {
        return buildArray(instance, field, builder.getType(), builder, allowNull, allowNullValues);
    }

    private <T> T[] buildArray(Instance instance, String field, Class<T> type, InstanceBuilder<? extends T> builder,
            boolean allowNull, boolean allowNullValues) throws InvalidFieldException {

        ObjectArrayInstance array = get(instance, field, ObjectArrayInstance.class, allowNull);
        if (array == null) {
            return null;
        }
        List<Instance> list = array.getValues();
        T[] result = (T[]) Array.newInstance(builder.getType(), list.size());
        for (int i = 0; i < result.length; i++) {
            try {
                InstanceBuilder<? extends T> itemBuilder = builder == null ? registry.getBuilder(instance, type) : builder;
                result[i] = itemBuilder.convert(this, castValue(notNullCheck(list.get(i), allowNullValues), Instance.class));
            } catch (InvalidFieldException ex) {
                throw new InvalidFieldException(instance, field, "Invalid value at index %d: %s", i, ex.getMessage()).initCause(ex);
            }
        }
        return result;
    }

    // Builder functions -----------------------------------------------------------------
    public String toString(Instance instance) {
        if (instance == null) {
            return null;
        }
        return DetailsSupport.getDetailsString(instance);
    }

    // Predefined type accessors ---------------------------------------------------------
    public int getInt(Instance instance, String field) throws InvalidFieldException {
        return get(instance, field, Number.class, false).intValue();
    }

    public boolean getBoolean(Instance instance, String field) throws InvalidFieldException {
        return get(instance, field, Boolean.class, false).booleanValue();
    }

    public String getString(Instance instance, String field, boolean allowNull) throws InvalidFieldException {
        return build(instance, field, InstanceBuilder.STRING_BUILDER, allowNull);
    }

    public int[] getIntArray(Instance instance, String field, boolean allowNull) throws InvalidFieldException {
        return build(instance, field, InstanceBuilder.INT_ARRAY_BUILDER, allowNull);
    }

    public byte[] getByteArray(Instance instance, String field, boolean allowNull) throws InvalidFieldException {
        return build(instance, field, InstanceBuilder.BYTE_ARRAY_BUILDER, allowNull);
    }

    public int[][] getIntArray2(Instance instance, String field, boolean allowNull) throws InvalidFieldException {
        return buildArray(instance, field, InstanceBuilder.INT_ARRAY_BUILDER, allowNull, false);
    }

    public byte[][] getByteArray2(Instance instance, String field, boolean allowNull) throws InvalidFieldException {
        return buildArray(instance, field, InstanceBuilder.BYTE_ARRAY_BUILDER, allowNull, false);
    }

    public short[][] getShortArray2(Instance instance, String field, boolean allowNull) throws InvalidFieldException {
        return buildArray(instance, field, InstanceBuilder.SHORT_ARRAY_BUILDER, allowNull, false);
    }
}
