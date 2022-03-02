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
package org.graalvm.visualvm.jfr.streaming.network;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jdk.jfr.consumer.RecordedEvent;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.jfr.streaming.JFRStream;

/**
 *
 * @author Tomas Hurka
 */
final class NetworkModel {

    private static final String JFR_NETWORK_UTILIZATION = "jdk.NetworkUtilization"; // NOI18N

    private boolean initialized;
    private Host source;
    private JFRStream jfr;
    private boolean live;

    private final List<ChangeListener> listeners;
    private int chartCache = -1;
    private long timestamp = -1;
    private long readRate = -1;
    private long writeRate = -1;

    static NetworkModel create(Host host, JFRStream rs) {
        return new NetworkModel(host, rs);
    }

    DataSource getSource() {
        return source;
    }

    boolean isLive() {
        return live;
    }

    long getTimestamp() {
        return timestamp;
    }

    int getChartCache() {
        return chartCache;
    }

    long getReadRate() {
        return readRate;
    }

    long getWriteRate() {
        return writeRate;
    }

    synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        initialize(source);
    }

    synchronized void cleanup() {
        listeners.clear();
        if (!initialized) {
            return;
        }
        jfr.close();
        jfr = null;
    }

    void addChangeListener(ChangeListener listener) {
        if (live) {
            listeners.add(listener);
        }
    }

    void removeChangeListener(ChangeListener listener) {
        if (live) {
            listeners.remove(listener);
        }
    }

    private void initialize(Host host) {
        GlobalPreferences preferences = GlobalPreferences.sharedInstance();
        final int monitoredDataPoll = preferences.getMonitoredDataPoll();
        chartCache = 60 * preferences.getMonitoredDataCache() / monitoredDataPoll;

        jfr.enable(JFR_NETWORK_UTILIZATION).withPeriod(Duration.ofSeconds(monitoredDataPoll));
        jfr.onEvent(JFR_NETWORK_UTILIZATION, this::networkEvent);
        jfr.onFlush(this::jfrFlush);
        jfr.startAsync();
    }

    private void jfrFlush() {
        SwingUtilities.invokeLater(() -> {
            fireChange();
            timestamp = -1;
        });
    }

    private void networkEvent(final RecordedEvent ev) {
        final Long[] values = getData(ev);

        if (values != null) {
            SwingUtilities.invokeLater(() -> {
                long time = ev.getStartTime().toEpochMilli();
                if (time == timestamp) {
                    readRate += values[0];
                    writeRate += values[1];
                } else {
                    fireChange();
                    readRate = values[0];
                    writeRate = values[1];
                    timestamp = time;
                }
            });
        }
    }

    private Long[] getData(RecordedEvent ev) {
        if (live) {
            String iface = ev.getString("networkInterface");        // NOI18N
            long rRate = ev.getLong("readRate");        // NOI18N
            long wRate = ev.getLong("writeRate");       // NOI18N
            return new Long[]{rRate, wRate};
        }
        return null;
    }

    private void fireChange() {
        if (timestamp == -1) return;
        final List<ChangeListener> list = new ArrayList<>();
        synchronized (listeners) {
            list.addAll(listeners);
        }
        for (ChangeListener l : list) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    private NetworkModel() {
        initialized = false;
        listeners = Collections.synchronizedList(new ArrayList<>());
    }

    private NetworkModel(Host src, JFRStream rs) {
        this();
        source = src;
        jfr = rs;
        live = true;
    }
}
