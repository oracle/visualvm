/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;

/**
 *
 * @author Tomas Hurka
 */
class JFRStream {

    private final RemoteRecordingStream rs;

    JFRStream(Application app) throws IOException {
        JmxModel jmx = JmxModelFactory.getJmxModelFor(app);
        rs = new RemoteRecordingStream(jmx.getMBeanServerConnection());
    }

    void close() {
        rs.close();
    }

    JFREventSettings enable(String eventName) {
        EventSettings s = rs.enable(eventName);
        return new JFREventSettings(eventName, s);
    }

    void onEvent(String eventName, Consumer<RecordedEvent> action) {
        rs.onEvent(eventName, action);
    }

    void startAsync() {
        rs.startAsync();
    }

    class JFREventSettings {

        private final String eventName;
        private final EventSettings delegate;

        private JFREventSettings(String eventName, EventSettings s) {
            this.eventName = eventName;
            delegate = s;
        }

        JFREventSettings withStackTrace() {
            return with(StackTrace.NAME, "true");
        }

        JFREventSettings withoutStackTrace() {
            return with(StackTrace.NAME, "false");
        }

        JFREventSettings withoutThreshold() {
            return withThreshold(null);
        }

        JFREventSettings withPeriod(Duration duration) {
            return with(Period.NAME, getString(duration));
        }

        JFREventSettings withThreshold(Duration duration) {
            return with(Threshold.NAME, getString(duration));
        }

        JFREventSettings with(String name, String value) {
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
}
