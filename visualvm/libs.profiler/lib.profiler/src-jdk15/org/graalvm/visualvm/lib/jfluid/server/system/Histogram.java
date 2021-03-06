/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.server.system;

import java.io.InputStream;

/**
 *
 * @author Tomas Hurka
 */
public class Histogram {
    private static Boolean initialized;
    private static boolean runningOnJdk9;

    public static boolean isAvailable() {
        if (initialized != null) {
            return initialized.booleanValue();
        }
        return false;
    }

    public static boolean initialize(boolean jdk9) {
        runningOnJdk9 = jdk9;
        if (runningOnJdk9) {
            initialized = Boolean.valueOf(Histogram19.initialize());
        } else {
            initialized = Boolean.valueOf(Histogram18.initialize());
        }
        return initialized.booleanValue();
    }

    public static InputStream getRawHistogram() {
        if (runningOnJdk9) {
            return Histogram19.getRawHistogram();
        }
        return Histogram18.getRawHistogram();
    }
}
