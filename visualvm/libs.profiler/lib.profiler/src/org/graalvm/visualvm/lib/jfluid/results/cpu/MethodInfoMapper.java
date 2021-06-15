/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.cpu;

import java.util.logging.Logger;

public abstract class MethodInfoMapper {
    final static protected Logger LOGGER = Logger.getLogger(MethodInfoMapper.class.getName());

    public static final MethodInfoMapper DEFAULT = new MethodInfoMapper() {

        @Override
        public String getInstrMethodClass(int methodId) {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return "<UNKNOWN>";
        }

        @Override
        public String getInstrMethodName(int methodId) {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return "<UNKNOWN>";
        }

        @Override
        public String getInstrMethodSignature(int methodId) {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return "<UNKNOWN>";
        }

        @Override
        public int getMaxMethodId() {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return 0;
        }

        @Override
        public int getMinMethodId() {
            LOGGER.warning("Usage of the default MethodInfoMapper implementation is discouraged");
            return 0;
        }
    };

    public abstract String getInstrMethodClass(int methodId);

    public abstract String getInstrMethodName(int methodId);

    public abstract String getInstrMethodSignature(int methodId);

    public abstract int getMinMethodId();

    public abstract int getMaxMethodId();

    public void lock(boolean mutable) {
        // default no-op
    }

    public void unlock() {
        // default no-op
    }
}
