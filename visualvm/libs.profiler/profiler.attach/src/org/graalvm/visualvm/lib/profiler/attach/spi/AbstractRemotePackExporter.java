/*
 * Copyright (c) 2011, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.attach.spi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.graalvm.visualvm.lib.common.integration.IntegrationUtils;
import org.graalvm.visualvm.lib.profiler.attach.providers.TargetPlatformEnum;

/**
 *
 * @author Jaroslav Bachorik
 */
abstract public class AbstractRemotePackExporter {
    private static final Map<String, String> scriptMapper = new HashMap<String, String>() {
        {
            put(IntegrationUtils.PLATFORM_LINUX_AMD64_OS, "linuxamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_LINUX_OS, "linux"); //NOI18N
            put(IntegrationUtils.PLATFORM_LINUX_ARM_OS, "linuxarm"); //NOI18N
            put(IntegrationUtils.PLATFORM_LINUX_ARM_VFP_HFLT_OS, "linuxarmvfphflt"); //NOI18N
            put(IntegrationUtils.PLATFORM_LINUX_ARM_AARCH64_OS, "linuxaarch64"); //NOI18N
            put(IntegrationUtils.PLATFORM_MAC_OS, "mac"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_AMD64_OS, "solamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_INTEL_OS, "solx86"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_SPARC_OS, "solsparc"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_SPARC64_OS, "solsparcv9"); //NOI18N
            put(IntegrationUtils.PLATFORM_WINDOWS_AMD64_OS, "winamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_WINDOWS_OS, "win"); //NOI18N
        }
    };
    private static final Map<String, String> jdkMapper = new HashMap<String, String>() {
        {
            // NOTE: 15 is used to only generate Ant task name which always ends with '-15'
            put(TargetPlatformEnum.JDK5.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK6.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK7.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK8.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK9.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK10.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK11.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK12.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK13.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK14.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK15.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK16.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK17.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK18.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK19.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK20.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK21.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK22.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK_CVM.toString(), "cvm"); //NOI18N
        }
    };
    
    final protected String getPlatformShort(String hostOS) {
        return scriptMapper.get(hostOS);
    }
    
    final protected String getJVMShort(String jvm) {
        return jdkMapper.get(jvm);
    }
    
    abstract public String export(String exportPath, String hostOS, String jvm) throws IOException;
    abstract public String getRemotePackPath(String exportPath, String hostOS);
}
