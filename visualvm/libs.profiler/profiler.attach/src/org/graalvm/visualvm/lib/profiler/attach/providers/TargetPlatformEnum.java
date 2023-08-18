/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.attach.providers;

import org.graalvm.visualvm.lib.common.integration.IntegrationUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *
 * @author Jaroslav Bachorik
 */
public class TargetPlatformEnum {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String[] jvmNames = new String[] {
                                                 IntegrationUtils.PLATFORM_JAVA_50, IntegrationUtils.PLATFORM_JAVA_60,
                                                 IntegrationUtils.PLATFORM_JAVA_70, IntegrationUtils.PLATFORM_JAVA_80,
                                                 IntegrationUtils.PLATFORM_JAVA_90, IntegrationUtils.PLATFORM_JAVA_100,
                                                 IntegrationUtils.PLATFORM_JAVA_110,IntegrationUtils.PLATFORM_JAVA_120,
                                                 IntegrationUtils.PLATFORM_JAVA_130,IntegrationUtils.PLATFORM_JAVA_140,
                                                 IntegrationUtils.PLATFORM_JAVA_150,IntegrationUtils.PLATFORM_JAVA_160,
                                                 IntegrationUtils.PLATFORM_JAVA_170,IntegrationUtils.PLATFORM_JAVA_180,
                                                 IntegrationUtils.PLATFORM_JAVA_190,IntegrationUtils.PLATFORM_JAVA_200,
                                                 IntegrationUtils.PLATFORM_JAVA_210,IntegrationUtils.PLATFORM_JAVA_CVM,
                                             };
    public static final TargetPlatformEnum JDK5 = new TargetPlatformEnum(0);
    public static final TargetPlatformEnum JDK6 = new TargetPlatformEnum(1);
    public static final TargetPlatformEnum JDK7 = new TargetPlatformEnum(2);
    public static final TargetPlatformEnum JDK8 = new TargetPlatformEnum(3);
    public static final TargetPlatformEnum JDK9 = new TargetPlatformEnum(4);
    public static final TargetPlatformEnum JDK10 = new TargetPlatformEnum(5);
    public static final TargetPlatformEnum JDK11 = new TargetPlatformEnum(6);
    public static final TargetPlatformEnum JDK12 = new TargetPlatformEnum(7);
    public static final TargetPlatformEnum JDK13 = new TargetPlatformEnum(8);
    public static final TargetPlatformEnum JDK14 = new TargetPlatformEnum(9);
    public static final TargetPlatformEnum JDK15 = new TargetPlatformEnum(10);
    public static final TargetPlatformEnum JDK16 = new TargetPlatformEnum(11);
    public static final TargetPlatformEnum JDK17 = new TargetPlatformEnum(12);
    public static final TargetPlatformEnum JDK18 = new TargetPlatformEnum(13);
    public static final TargetPlatformEnum JDK19 = new TargetPlatformEnum(14);
    public static final TargetPlatformEnum JDK20 = new TargetPlatformEnum(15);
    public static final TargetPlatformEnum JDK21 = new TargetPlatformEnum(16);
    public static final TargetPlatformEnum JDK_CVM = new TargetPlatformEnum(17);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int jvmIndex = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private TargetPlatformEnum(int index) {
        this.jvmIndex = index;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean equals(Object obj) {
        if ((obj == null) || !(obj instanceof TargetPlatformEnum)) {
            return false;
        }

        return ((TargetPlatformEnum) obj).jvmIndex == this.jvmIndex;
    }

    @Override
    public int hashCode() {
        return jvmIndex;
    }

    public static Iterator<TargetPlatformEnum> iterator() {
        List<TargetPlatformEnum> jvmList = new ArrayList<>(18);
        jvmList.add(JDK5);
        jvmList.add(JDK6);
        jvmList.add(JDK7);
        jvmList.add(JDK8);
        jvmList.add(JDK9);
        jvmList.add(JDK10);
        jvmList.add(JDK11);
        jvmList.add(JDK12);
        jvmList.add(JDK13);
        jvmList.add(JDK14);
        jvmList.add(JDK15);
        jvmList.add(JDK16);
        jvmList.add(JDK17);
        jvmList.add(JDK18);
        jvmList.add(JDK19);
        jvmList.add(JDK20);
        jvmList.add(JDK21);
        jvmList.add(JDK_CVM);

        return jvmList.listIterator();
    }

    public String toString() {
        return jvmNames[this.jvmIndex];
    }
}
