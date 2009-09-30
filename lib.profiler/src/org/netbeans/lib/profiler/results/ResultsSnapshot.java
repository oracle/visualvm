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

package org.netbeans.lib.profiler.results;

import org.netbeans.lib.profiler.utils.StringUtils;
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

    //  protected static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.results.ResultsSnapshot")
    //                                         != null; // NOI18N // TODO [release] set tp TRUE at release
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
}
