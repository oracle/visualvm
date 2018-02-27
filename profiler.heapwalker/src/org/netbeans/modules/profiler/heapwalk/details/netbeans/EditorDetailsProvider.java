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

package org.netbeans.modules.profiler.heapwalk.details.netbeans;

import java.util.AbstractList;
import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
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
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (CHAR_CONTENT.equals(className)) {
            int gapStart = DetailsUtils.getIntFieldValue(instance, "gapStart", -1);     // NOI18N
            int gapLength = DetailsUtils.getIntFieldValue(instance, "gapLength", -1);       // NOI18N
            PrimitiveArrayInstance buffer = (PrimitiveArrayInstance)instance.getValueOfField("buffer"); // NOI18N

            if (gapLength >= 0 && gapLength >= 0 && buffer != null) {
                CharArrayWithGap array = new CharArrayWithGap(buffer, gapStart, gapLength);

                return DetailsUtils.getPrimitiveArrayString(array, 0, array.getLength(), "", "...");    // NOI18N
            }
        }
        return null;
    }


    private class CharArrayWithGap implements PrimitiveArrayInstance {

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
