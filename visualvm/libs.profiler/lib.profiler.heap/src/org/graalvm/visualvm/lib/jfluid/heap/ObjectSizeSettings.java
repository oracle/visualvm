/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tomas Hurka
 *
 * This class encapsulates different settings, which controls size of object.
 */
class ObjectSizeSettings {

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    static final int OBJECT_ALIGNMENT = 8; // must be power of 2
    static final int ARRAY_OVERHEAD = 4; // difference between size of java.lang.Object and java.lang.Object[0]

    static final Logger LOG = Logger.getLogger(ObjectSizeSettings.class.getName());

    private final HprofHeap hprofHeap;
    private int minimumInstanceSize;
    private int oopSize;

    ObjectSizeSettings(HprofHeap heap) {
        hprofHeap = heap;
    }

    int getMinimumInstanceSize() {
        if (minimumInstanceSize == 0) {
            minimumInstanceSize = hprofHeap.dumpBuffer.getIDSize() + getOopSize();
            LOG.log(Level.CONFIG, "MinimumInstanceSize computed as {0}", minimumInstanceSize); // NOI18N
        }
        return minimumInstanceSize;
    }

    int getElementSize(byte type) {
        if (type == HprofHeap.OBJECT) {
            return getOopSize();
        }
        return hprofHeap.getValueSize(type);
    }

    void setMinimumInstanceSize(int objectSize) {
        minimumInstanceSize = objectSize;
        LOG.log(Level.CONFIG, "MinimumInstanceSize set to {0}", minimumInstanceSize);  // NOI18N
    }

    int getOopSize() {
        if (oopSize == 0) {
            int idSize = hprofHeap.dumpBuffer.getIDSize();
            LOG.log(Level.CONFIG, "OopSize uses idSize {0}", idSize);           // NOI18N
            if (idSize == 8) {  // can be compressed oops
                if (hprofHeap.getClassDumpSegment().newSize) {
                    oopSize = guessNewOopSize(idSize);
                } else {
                    oopSize = guessOopSize(idSize);
                }
            } else {
                LOG.log(Level.CONFIG, "OopSize set to idSize {0}", idSize);     // NOI18N
                oopSize = idSize;
            }
        }
        return oopSize;
    }

    private int guessNewOopSize(int idSize) {
        int size = getMinimumInstanceSize() - idSize;
        LOG.log(Level.CONFIG, "OopSize computed as {0}", size);  // NOI18N
        if (size == 4 || size == 8) {
            return size;
        }
        size = (size < 8) ? 4 : 8;
        LOG.log(Level.CONFIG, "OopSize set to {0}", size);  // NOI18N
        return size;
    }

    private int guessOopSize(int idSize) {
        // detect compressed oops
        int size = getFieldsDiff("java.lang.Class$Atomic", "annotationDataOffset", "annotationTypeOffset"); // NOI18N
        if (size != 0) return size;

        size = getFieldsDiff("java.util.concurrent.FutureTask", "runnerOffset", "waitersOffset"); // NOI18N
        if (size != 0) return size;

        Properties sysProp = hprofHeap.getSystemProperties();
        // See test/hotspot/jtreg/runtime/FieldLayout/FieldDensityTest.java
        if (sysProp.getProperty("java.vm.compressedOopsMode") != null) {        // NOI18N
            // compressed oops
            LOG.log(Level.CONFIG, "OopSize guessed (A2) as {0}", 4);            // NOI18N
            return 4;
        }
        LOG.log(Level.CONFIG, "OopSize defaults to {0}", idSize);               // NOI18N
        return idSize;
    }

    private int getFieldsDiff(String javaClass, String field1Name, String field2Name) {
        JavaClass jcls = hprofHeap.getJavaClassByName(javaClass);
        if (jcls != null) {
            Object field1Offset = jcls.getValueOfStaticField(field1Name);
            Object field2Offset = jcls.getValueOfStaticField(field2Name);
            if (field1Offset instanceof Long && field2Offset instanceof Long) {
                int guessedSize = (int) ((Long) field2Offset - (Long) field1Offset);
                if (guessedSize == 4 || guessedSize == 8) {
                    LOG.log(Level.CONFIG, "OopSize guessed ({0}) as {1}", new Object[]{javaClass, guessedSize});        // NOI18N
                    return guessedSize;
                }
            }
        }
        return 0;
    }
}
