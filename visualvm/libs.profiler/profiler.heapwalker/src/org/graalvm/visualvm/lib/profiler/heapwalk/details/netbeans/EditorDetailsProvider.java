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

package org.graalvm.visualvm.lib.profiler.heapwalk.details.netbeans;

import java.util.AbstractList;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public class EditorDetailsProvider  extends DetailsProvider.Basic {
    private static final String CHAR_CONTENT = "org.netbeans.modules.editor.lib2.document.CharContent"; // NOI18N

    public EditorDetailsProvider() {
        super(CHAR_CONTENT);
    }

    @Override
    public String getDetailsString(String className, Instance instance) {
        if (CHAR_CONTENT.equals(className)) {
            int gapStart = DetailsUtils.getIntFieldValue(instance, "gapStart", -1);     // NOI18N
            int gapLength = DetailsUtils.getIntFieldValue(instance, "gapLength", -1);       // NOI18N
            PrimitiveArrayInstance buffer = (PrimitiveArrayInstance)instance.getValueOfField("buffer"); // NOI18N

            if (gapStart >= 0 && gapLength >= 0 && buffer != null) {
                CharArrayWithGap array = new CharArrayWithGap(buffer, gapStart, gapLength);

                return DetailsUtils.getPrimitiveArrayString(array, 0, array.getLength(), "", "...");    // NOI18N
            }
        }
        return null;
    }


    private static class CharArrayWithGap implements PrimitiveArrayInstance {

        PrimitiveArrayInstance buffer;
        int gapStart;
        int gapLength;

        private CharArrayWithGap(PrimitiveArrayInstance buf, int start, int length) {
            buffer = buf;
            gapStart = start;
            gapLength = length;
        }

        @Override
        public int getLength() {
            return buffer.getLength() - gapLength;
        }

        @Override
        public List getValues() {
            final List origValues = buffer.getValues();

            return new AbstractList() {

                @Override
                public Object get(int index) {
                    return origValues.get(rawOffset(index));
                }

                @Override
                public int size() {
                    return getLength();
                }
            };
        }

        private int rawOffset(int index) {
            return (index < gapStart) ? index : (index + gapLength);
        }

        @Override
        public List getFieldValues() {
            throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public boolean isGCRoot() {
                throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
        }

        @Override
        public long getInstanceId() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public int getInstanceNumber() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public JavaClass getJavaClass() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public Instance getNearestGCRootPointer() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public long getReachableSize() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public List getReferences() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public long getRetainedSize() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public long getSize() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public List getStaticFieldValues() {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }

        @Override
        public Object getValueOfField(String name) {
            throw new UnsupportedOperationException("Not supported yet."); // NOI18N
        }
    }
}
