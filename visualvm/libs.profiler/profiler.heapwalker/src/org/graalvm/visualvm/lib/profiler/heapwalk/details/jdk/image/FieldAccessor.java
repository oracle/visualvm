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

import java.lang.reflect.Array;
import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 * Utilities to access fields of heap dump instances.
 *
 * @author Jan Taus
 */
public class FieldAccessor {

    private final Heap heap;
    private final InstanceBuilderRegistry registry;

    public FieldAccessor(Heap heap) {
        this(heap, new InstanceBuilderRegistry());
    }

    public FieldAccessor(Heap heap, InstanceBuilderRegistry registry) {
        this.heap = heap;
        this.registry = registry;
    }

    public Heap getHeap() {
        return heap;
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
     * @see #get(org.netbeans.lib.profiler.heap.Instance, java.lang.String, java.lang.Class, boolean)
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

    public <T> T build(Instance instance, String field, InstanceBuilder<T> builder, boolean allowNull)
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
    public <T> T[] buildArray(Instance instance, String field, InstanceBuilder<T> builder,
            boolean allowNull, boolean allowNullValues) throws InvalidFieldException {
        return buildArray(instance, field, builder.getType(), builder, allowNull, allowNullValues);
    }

    private <T> T[] buildArray(Instance instance, String field, Class<T> type, InstanceBuilder<? extends T> builder,
            boolean allowNull, boolean allowNullValues) throws InvalidFieldException {

        ObjectArrayInstance array = get(instance, field, ObjectArrayInstance.class, allowNull);
        if (array == null) {
            return null;
        }
        List<?> list = array.getValues();
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
        return DetailsSupport.getDetailsString(instance, heap);
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
