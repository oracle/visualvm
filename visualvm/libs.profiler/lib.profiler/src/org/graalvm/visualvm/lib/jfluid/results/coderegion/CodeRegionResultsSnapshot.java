/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.results.coderegion;

import org.graalvm.visualvm.lib.jfluid.results.ResultsSnapshot;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;


/**
 * A class that holds single snapshot of Code Fragment profiling results.
 *
 * @author ian Formanek
 */
public final class CodeRegionResultsSnapshot extends ResultsSnapshot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CODE_FRAGMENT_MSG = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.coderegion.Bundle").getString("CodeRegionResultsSnapshot_CodeFragmentMsg"); // NOI18N
                                                                                                                     // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private long[] rawData;
    private long timerCountsInSecond;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CodeRegionResultsSnapshot(long beginTime, long timeTaken, long[] rawData, long timerCountsInSecond) {
        super(beginTime, timeTaken);
        this.rawData = rawData;
        this.timerCountsInSecond = timerCountsInSecond;

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    public CodeRegionResultsSnapshot() {
    } // for loading from file

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @return The number of invocations for which we remember their time.
     * @see #getTimes() - getTimes()[0] contains the total number of invocations of the tracked method/code
     */
    public int getInvocations() {
        if (rawData == null) {
            return 0;
        } else {
            return rawData.length;
        }
    }

    public long getTimerCountsInSecond() {
        return timerCountsInSecond;
    }

    /**
     * @return an array of long values. times[0] is total number of invocations, times[1]-times[times.length-1] contain
     *         the invocation times for all invocations.
     */
    public long[] getTimes() {
        return rawData;
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);
        timerCountsInSecond = in.readLong();

        int len = in.readInt();
        rawData = new long[len];

        for (int i = 0; i < len; i++) {
            rawData[i] = in.readLong();
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    public String toString() {
        return MessageFormat.format(CODE_FRAGMENT_MSG, new Object[] { super.toString() });
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);
        out.writeLong(timerCountsInSecond);
        out.writeInt(rawData.length);

        for (int i = 0; i < rawData.length; i++) {
            out.writeLong(rawData[i]);
        }
    }

    private void debugValues() {
        LOGGER.finest("rawData.length: " + debugLength(rawData)); // NOI18N
        LOGGER.finest("timerCountsInSecond: " + timerCountsInSecond); // NOI18N
    }
}
