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

import java.util.List;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.modules.profiler.heapwalk.details.jdk.image.FieldAccessor.InvalidFieldException;

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
            List<?> list = array.getValues();
            int[] result = new int[list.size()];
            for (int i = 0; i < result.length; i++) {
                try {
                    result[i] = Integer.parseInt((String) list.get(i));
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
            List<?> list = array.getValues();
            byte[] result = new byte[list.size()];
            for (int i = 0; i < result.length; i++) {
                try {
                    result[i] = Byte.parseByte((String) list.get(i));
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
            List<?> list = array.getValues();
            short[] result = new short[list.size()];
            for (int i = 0; i < result.length; i++) {
                try {
                    result[i] = Short.parseShort((String) list.get(i));
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

    public InstanceBuilder(Class<T> type) {
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
