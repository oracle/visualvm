/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.basic;

import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.StringDecoder;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public final class StringDetailsProvider extends DetailsProvider.Basic {
    
    static final String STRING_MASK = "java.lang.String";                           // NOI18N
    static final String BUILDERS_MASK = "java.lang.AbstractStringBuilder+";         // NOI18N
    
    public StringDetailsProvider() {
        super(STRING_MASK, BUILDERS_MASK);
    }
    
    public String getDetailsString(String className, Instance instance) {
        if (STRING_MASK.equals(className)) {                                        // String
            byte coder = DetailsUtils.getByteFieldValue(instance, "coder", (byte) -1);     // NOI18N
            if (coder == -1) {
                int offset = DetailsUtils.getIntFieldValue(instance, "offset", 0);      // NOI18N
                int count = DetailsUtils.getIntFieldValue(instance, "count", -1);       // NOI18N
                return DetailsUtils.getPrimitiveArrayFieldString(instance, "value",     // NOI18N
                        offset, count, null,
                        "...");                // NOI18N
            } else {
                return getJDK9String(instance, "value", coder, null, "...");          // NOI18N
            }
        } else if (BUILDERS_MASK.equals(className)) {                               // AbstractStringBuilder+
            byte coder = DetailsUtils.getByteFieldValue(instance, "coder", (byte) -1);  // NOI18N
            if (coder == -1) {
                int count = DetailsUtils.getIntFieldValue(instance, "count", -1);       // NOI18N
                return DetailsUtils.getPrimitiveArrayFieldString(instance, "value",     // NOI18N
                        0, count, null,
                        "...");                // NOI18N
            } else {
                return getJDK9String(instance, "value", coder, null, "...");          // NOI18N
            }
        }
        return null;
    }
    
    public View getDetailsView(String className, Instance instance) {
        return new ArrayValueView(className, instance);
    }
    
    private String getJDK9String(Instance instance, String field, byte coder, String separator, String trailer) {
        Object byteArray = instance.getValueOfField(field);
        if (byteArray instanceof PrimitiveArrayInstance) {
            List<String> values = ((PrimitiveArrayInstance) byteArray).getValues();
            if (values != null) {
                Heap heap = instance.getJavaClass().getHeap();
                StringDecoder decoder = new StringDecoder(heap, coder, values);
                int valuesCount = decoder.getStringLength();
                int separatorLength = separator == null ? 0 : separator.length();
                int trailerLength = trailer == null ? 0 : trailer.length();
                int estimatedSize = Math.min(valuesCount * (1 + separatorLength), DetailsUtils.MAX_ARRAY_LENGTH + trailerLength);
                StringBuilder value = new StringBuilder(estimatedSize);
                int lastValue = valuesCount - 1;
                for (int i = 0; i <= lastValue; i++) {
                    if (value.length() >= DetailsUtils.MAX_ARRAY_LENGTH) {
                        if (trailerLength > 0) {
                            value.append(trailer);
                        }
                        break;
                    }
                    value.append(decoder.getValueAt(i));
                    if (separator != null && i < lastValue) {
                        value.append(separator);
                    }
                }
                return value.toString();
            }
        }
        return null;
    }
}
