/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.results.coderegion;

import org.netbeans.lib.profiler.results.ResultsSnapshot;
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
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.coderegion.Bundle"); // NOI18N
    private static final String CODE_FRAGMENT_MSG = messages.getString("CodeRegionResultsSnapshot_CodeFragmentMsg"); // NOI18N
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
