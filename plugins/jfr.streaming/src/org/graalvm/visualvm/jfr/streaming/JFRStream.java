/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.streaming;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;
import jdk.jfr.EventSettings;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;
import jdk.jfr.Threshold;
import jdk.jfr.consumer.RecordedEvent;
import jdk.management.jfr.RemoteRecordingStream;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;

/**
 *
 * @author Tomas Hurka
 */
public class JFRStream {

    private final RemoteRecordingStream rs;

    public static JFRStream getFor(Application app) throws IOException {
        Jvm jvm = JvmFactory.getJVMFor(app);
        String ver = jvm.getJavaVersion();
        if (isJavaVersion(ver, "17") || isJavaVersion(ver, "18") 
         || isJavaVersion(ver, "19") || isJavaVersion(ver, "20")
         || isJavaVersion(ver, "21") || isJavaVersion(ver, "22")
         || isJavaVersion(ver, "23") || isJavaVersion(ver, "24")) {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
            if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
                return new JFRStream(jmxModel);
            }
        }
        return null;
    }

    private JFRStream(JmxModel jmx) throws IOException {
        rs = new RemoteRecordingStream(jmx.getMBeanServerConnection());
    }

    public void close() {
        rs.close();
    }

    public JFREventSettings enable(String eventName) {
        EventSettings s = rs.enable(eventName);
        return new JFREventSettings(eventName, s);
    }

    public void onEvent(String eventName, Consumer<RecordedEvent> action) {
        rs.onEvent(eventName, action);
    }

    public void onFlush(Runnable action) {
        rs.onFlush(action);
    }

    public void startAsync() {
        rs.startAsync();
    }

    public class JFREventSettings {

        private final String eventName;
        private final EventSettings delegate;

        private JFREventSettings(String eventName, EventSettings s) {
            this.eventName = eventName;
            delegate = s;
        }

        public JFREventSettings withStackTrace() {
            return with(StackTrace.NAME, "true");
        }

        public JFREventSettings withoutStackTrace() {
            return with(StackTrace.NAME, "false");
        }

        public JFREventSettings withoutThreshold() {
            return withThreshold(null);
        }

        public JFREventSettings withPeriod(Duration duration) {
            return with(Period.NAME, getString(duration));
        }

        public JFREventSettings withThreshold(Duration duration) {
            return with(Threshold.NAME, getString(duration));
        }

        public JFREventSettings with(String name, String value) {
            delegate.with(eventName + "#" + name, value);
            return this;
        }

        private static String getString(Duration duration) {
            if (duration == null) {
                return "0 s";
            }
            return duration.toNanos() + " ns";
        }
    }

    private static final boolean isJavaVersion(String javaVersionProperty, String releaseVersion) {
        if (javaVersionProperty.equals(releaseVersion)) {
            return true;
        }
        if (javaVersionProperty.equals(releaseVersion + "-ea")) {
            return true;
        }
        if (javaVersionProperty.equals(releaseVersion + "-internal")) {
            return true;
        }
        if (javaVersionProperty.startsWith(releaseVersion + ".")) {
            return true;
        }
        return false;
    }
}
