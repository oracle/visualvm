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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public final class MathDetailsProvider extends DetailsProvider.Basic {

    private static final String BIG_INTEGRER_MASK = "java.math.BigInteger"; // NOI18N
    private static final String BIG_DECIMAL_MASK = "java.math.BigDecimal";  // NOI18N
    private static final String FD_BIG_INTEGRER_MASK = "jdk.internal.math.FDBigInteger";  // NOI18N

    public MathDetailsProvider() {
        super(BIG_INTEGRER_MASK,BIG_DECIMAL_MASK, FD_BIG_INTEGRER_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        if (BIG_INTEGRER_MASK.equals(className)) {
            BigInteger bint = getBigInteger(instance);

            if (bint != null) {
                return bint.toString();
            }
        } else if (BIG_DECIMAL_MASK.equals(className)) {
            String val = DetailsUtils.getInstanceFieldString(instance, "stringCache");   // NOI18N
            if (val == null) {
                int scale = DetailsUtils.getIntFieldValue(instance, "scale", 0);    // NOI18N
                long intCompact = DetailsUtils.getLongFieldValue(instance, "intCompact", Long.MIN_VALUE);   // NOI18N

                if (intCompact != Long.MIN_VALUE) {
                    return BigDecimal.valueOf(intCompact, scale).toString();
                } else {
                    Object bintInstace = instance.getValueOfField("intVal");    // NOI18N
                    if (bintInstace instanceof Instance) {
                        BigInteger bint = getBigInteger((Instance)bintInstace);
                        
                        if (bint != null) {
                            return new BigDecimal(bint, scale).toString();
                        }
                    }
                }
            } else {
                return val;
            }
        }
        if (FD_BIG_INTEGRER_MASK.equals(className)) {
            Integer nWords = (Integer) instance.getValueOfField("nWords");      // NOI18N
            Integer offset = (Integer) instance.getValueOfField("offset");      // NOI18N
            int[] data = DetailsUtils.getIntArray(DetailsUtils.getPrimitiveArrayFieldValues(instance, "data"));   // NOI18N
            if (nWords != null && offset != null && data != null) {
                byte[] magnitude = new byte[nWords * 4 + 1];
                for (int i = 0; i < nWords; i++) {
                    int w = data[i];
                    magnitude[magnitude.length - 4 * i - 1] = (byte) w;
                    magnitude[magnitude.length - 4 * i - 2] = (byte) (w >> 8);
                    magnitude[magnitude.length - 4 * i - 3] = (byte) (w >> 16);
                    magnitude[magnitude.length - 4 * i - 4] = (byte) (w >> 24);
                }
                return new BigInteger(magnitude).shiftLeft(offset * 32).toString();
            }
        }
        return null;
    }

    private BigInteger getBigInteger(final Instance instance) {
        int sig = DetailsUtils.getIntFieldValue(instance, "signum", Integer.MAX_VALUE);     // NOI18N
        int[] mag = DetailsUtils.getIntArray(DetailsUtils.getPrimitiveArrayFieldValues(instance, "mag"));   // NOI18N
        if (mag != null && sig != Integer.MAX_VALUE) {
            ByteBuffer buffer = ByteBuffer.allocate(mag.length * 4);
            IntBuffer intBuffer = buffer.asIntBuffer();
            intBuffer.put(mag);
            
            return new BigInteger(sig, buffer.array());
        }
        return null;
    }
    
}
