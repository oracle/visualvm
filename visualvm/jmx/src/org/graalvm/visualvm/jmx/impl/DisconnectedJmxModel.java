/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jmx.impl;

import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.tools.jmx.JmxModel;

/**
 *
 * @author Tomas Hurka
 */
class DisconnectedJmxModel extends JmxModel {

    private final JMXServiceURL url;

    DisconnectedJmxModel(JmxApplication app) {
        url = app.getJMXServiceURL();
    }

    @Override
    public ConnectionState getConnectionState() {
        return ConnectionState.DISCONNECTED;
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection() {
        return null;
    }

    @Override
    public JMXServiceURL getJMXServiceURL() {
        return url;
    }

    @Override
    public Properties getSystemProperties() {
        return null;
    }

    @Override
    public boolean isTakeHeapDumpSupported() {
        return false;
    }

    @Override
    public boolean takeHeapDump(String fileName) {
        return false;
    }

    @Override
    public boolean isTakeThreadDumpSupported() {
        return false;
    }

    @Override
    public String takeThreadDump() {
        return null;
    }

    @Override
    public String takeThreadDump(long[] threadIds) {
        return null;
    }

    @Override
    public HeapHistogram takeHeapHistogram() {
        return null;
    }

    @Override
    public String getFlagValue(String name) {
        return null;
    }

    @Override
    public void setFlagValue(String name, String value) {
    }

    @Override
    public String getCommandLine() {
        return null;
    }

    @Override
    public String executeJCmd(String command, Map<String, Object> pars) {
        return "";
    }
}
