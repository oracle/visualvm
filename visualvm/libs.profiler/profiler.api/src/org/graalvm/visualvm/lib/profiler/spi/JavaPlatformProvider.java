/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.spi;

import java.util.Map;

/**
 *
 * @author Tomas Hurka
 */
public abstract class JavaPlatformProvider {

    /**
     * @return  a descriptive, human-readable name of the platform
     */
    public abstract String getDisplayName();

    /** Gets the java platform system properties.
     * @return the java platform system properties
     */
    public abstract Map<String,String> getSystemProperties();

    /** Gets the java platform properties.
     * @return the java platform properties
     */
    public abstract Map<String,String> getProperties();


    /** Gets a path to java executable for specified platform.
     * Errors when obtaining the java executable will be reported to the user and null will be returned.
     *
     * @return A path to java executable or null if not found
     */
    public abstract String getPlatformJavaFile();

    /**
     * @return  a unique name of the platform
     */
    public abstract String getPlatformId();

}
