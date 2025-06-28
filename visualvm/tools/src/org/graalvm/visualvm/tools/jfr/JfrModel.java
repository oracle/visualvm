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
package org.graalvm.visualvm.tools.jfr;

import java.util.List;
import org.graalvm.visualvm.core.model.Model;

/**
 *
 * @author Tomas Hurka
 */
public abstract class JfrModel extends Model {

    /**
     * Checks running JFR recording(s) of target Application.
     *
     * @return returns List of recording id-s. If no recordings are in progress,
     * empty List is returned.
     */
    public abstract List<Long> jfrCheck();

    /**
     * Takes JFR dump of target Application. The JFR snapshot is written to the
     * <tt>fileName</tt> file.
     *
     * @param recording id of recording obtained using {@link #jfrCheck()}
     * @param fileName path to file, where JFR snapshot will be written
     * @return returns <CODE>null</CODE> if operation was successful.
     */
    public abstract String takeJfrDump(long recording, String fileName);

    /**
     * Starts a new JFR recording.
     *
     * @param name optional name that can be used to identify recording.
     * @param settings names of settings files to use, i.e. "default" or
     * "default.jfc".
     * @param delay optional delay recording start with (s)econds, (m)inutes,
     * (h)ours, or (d)ays, e.g. 5h.
     * @param duration optional duration of recording in (s)econds, (m)inutes,
     * (h)ours, or (d)ays, e.g. 300s.
     * @param disk if recording should be persisted to disk
     * @param path file path where recording data should be written
     * @param maxAge optional maximum time to keep recorded data (on disk) in
     * (s)econds, (m)inutes, (h)ours, or (d)ays, e.g. 60m, or <code>0</code> if
     * no limit should be set.
     * @param maxSize optional maximum amount of bytes to keep (on disk) in
     * (k)B, (M)B or (G)B, e.g. 500M, or <code>0</code> if no limit should be
     * set.
     * @param dumpOnExit if recording should dump on exit
     *
     * @return true if recording was successfully started.
     */
    public abstract boolean startJfrRecording(String name, String[] settings,
            String delay, String duration, Boolean disk, String path,
            String maxAge, String maxSize, Boolean dumpOnExit);

    /**
     * Stops JFR recording.
     *
     * @return true if recording was successfully stopped.
     */
    public abstract boolean stopJfrRecording();
}
