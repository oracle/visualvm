/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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


/**
 *
 * @author Jaroslav Bachorik
 */
public enum TargetPlatformEnum {
    //~ Enum constants -----------------------------------------------------------------------------------------------------------

    JDK5(IntegrationUtils.PLATFORM_JAVA_50),
    JDK6(IntegrationUtils.PLATFORM_JAVA_60),
    JDK7(IntegrationUtils.PLATFORM_JAVA_70),
    JDK8(IntegrationUtils.PLATFORM_JAVA_80),
    JDK9(IntegrationUtils.PLATFORM_JAVA_90),
    JDK10(IntegrationUtils.PLATFORM_JAVA_100),
    JDK11(IntegrationUtils.PLATFORM_JAVA_110),
    JDK12(IntegrationUtils.PLATFORM_JAVA_120),
    JDK13(IntegrationUtils.PLATFORM_JAVA_130),
    JDK14(IntegrationUtils.PLATFORM_JAVA_140),
    JDK15(IntegrationUtils.PLATFORM_JAVA_150),
    JDK16(IntegrationUtils.PLATFORM_JAVA_160),
    JDK17(IntegrationUtils.PLATFORM_JAVA_170),
    JDK18(IntegrationUtils.PLATFORM_JAVA_180),
    JDK19(IntegrationUtils.PLATFORM_JAVA_190),
    JDK20(IntegrationUtils.PLATFORM_JAVA_200),
    JDK21(IntegrationUtils.PLATFORM_JAVA_210),
    JDK22(IntegrationUtils.PLATFORM_JAVA_220),
    JDK23(IntegrationUtils.PLATFORM_JAVA_230),
    JDK24(IntegrationUtils.PLATFORM_JAVA_240),
    JDK25(IntegrationUtils.PLATFORM_JAVA_250),
    JDK26(IntegrationUtils.PLATFORM_JAVA_260),
    JDK_CVM(IntegrationUtils.PLATFORM_JAVA_CVM);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final String jvmName;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    TargetPlatformEnum(String jvmName) {
        this.jvmName = jvmName;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return jvmName;
    }
}
