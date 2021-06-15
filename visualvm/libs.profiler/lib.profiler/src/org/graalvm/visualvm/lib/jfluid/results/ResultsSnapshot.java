/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.results;

import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Root superclass for various types of profiling results snapshots
 */
public class ResultsSnapshot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static final Logger LOGGER = Logger.getLogger(ResultsSnapshot.class.getName());
    private static final int SNAPSHOT_VERSION = 1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    //  protected static final boolean DEBUG = System.getProperty("org.graalvm.visualvm.lib.jfluid.results.ResultsSnapshot") != null; // NOI18N // TODO [release] set to TRUE at release
    protected long beginTime;
    protected long timeTaken;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ResultsSnapshot() {
    } // for externalization

    protected ResultsSnapshot(long beginTime, long timeTaken) {
        this.beginTime = beginTime;
        this.timeTaken = timeTaken;

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long getBeginTime() {
        return beginTime;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public void readFromStream(DataInputStream in) throws IOException {
        int version = in.readInt();

        if (version != SNAPSHOT_VERSION) {
            throw new IOException("Stored version not supported: " + version); // NOI18N
        }

        beginTime = in.readLong();
        timeTaken = in.readLong();

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    public String toString() {
        return StringUtils.formatUserDate(new Date(timeTaken));
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(SNAPSHOT_VERSION);
        out.writeLong(beginTime);
        out.writeLong(timeTaken);
    }

    protected String debugLength(Object array) {
        if (array == null) {
            return "null"; // NOI18N
        } else if (array instanceof int[]) {
            return "" + ((int[]) array).length; // NOI18N
        } else if (array instanceof long[]) {
            return "" + ((long[]) array).length; // NOI18N
        } else if (array instanceof float[]) {
            return "" + ((float[]) array).length; // NOI18N
        } else if (array instanceof Object[]) {
            return "" + ((Object[]) array).length; // NOI18N
        } else {
            return "Unknown"; // NOI18N
        }
    }

    private void debugValues() {
        LOGGER.finest("beginTime: " + beginTime); // NOI18N
        LOGGER.finest("timeTaken: " + timeTaken); // NOI18N
    }

    // used by LoadedSnapshot when loading from file
    public void setProfilerSettings(ProfilerEngineSettings pes) {
    }
}
