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

package org.netbeans.lib.profiler.wireprotocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * This command, sent by the client, contains the instrumentation parameters (settings) that
 * can be changed once instrumentation is active and profiling is going on.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class SetChangeableInstrParamsCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private boolean runGCOnGetResultsInMemoryProfiling;
    private boolean sleepTrackingEnabled;
    private boolean waitTrackingEnabled;
    private int nProfiledThreadsLimit;
    private int objAllocStackSamplingDepth;
    private int objAllocStackSamplingInterval;
    private int samplingInterval;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SetChangeableInstrParamsCommand(int nProfiledThreadsLimit, int samplingInterval, int objAllocStackSamplingInterval,
                                           int objAllocStackSamplingDepth, boolean runGCOnGetResults,
                                           boolean waitTrackingEnabled, boolean sleepTrackingEnabled) {
        super(SET_CHANGEABLE_INSTR_PARAMS);
        this.nProfiledThreadsLimit = nProfiledThreadsLimit;
        this.samplingInterval = samplingInterval;
        this.objAllocStackSamplingInterval = objAllocStackSamplingInterval;
        this.objAllocStackSamplingDepth = objAllocStackSamplingDepth;
        this.runGCOnGetResultsInMemoryProfiling = runGCOnGetResults;
        this.waitTrackingEnabled = waitTrackingEnabled;
        this.sleepTrackingEnabled = sleepTrackingEnabled;
    }

    // Custom serialization support
    SetChangeableInstrParamsCommand() {
        super(SET_CHANGEABLE_INSTR_PARAMS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getNProfiledThreadsLimit() {
        return nProfiledThreadsLimit;
    }

    public int getObjAllocStackSamplingDepth() {
        return objAllocStackSamplingDepth;
    }

    public int getObjAllocStackSamplingInterval() {
        return objAllocStackSamplingInterval;
    }

    public boolean getRunGCOnGetResultsInMemoryProfiling() {
        return runGCOnGetResultsInMemoryProfiling;
    }

    public int getSamplingInterval() {
        return samplingInterval;
    }

    public boolean getSleepTrackingEnabled() {
        return sleepTrackingEnabled;
    }

    public boolean getWaitTrackingEnabled() {
        return waitTrackingEnabled;
    }

    // For debugging
    public String toString() {
        return super.toString() + ", nProfiledThreadsLimit: " + nProfiledThreadsLimit // NOI18N
               + ", samplingInterval: " + samplingInterval // NOI18N
               + ", objAllocStackSamplingInterval: " + objAllocStackSamplingInterval // NOI18N
               + ", objAllocStackSamplingDepth: " + objAllocStackSamplingDepth // NOI18N
               + ", runGCOnGetResultsInMemoryProfiling: " + runGCOnGetResultsInMemoryProfiling // NOI18N
               + ", waitTrackingEnabled: " + waitTrackingEnabled // NOI18N
               + ", sleepTrackingEnabled: " + sleepTrackingEnabled; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        nProfiledThreadsLimit = in.readInt();
        samplingInterval = in.readInt();
        objAllocStackSamplingInterval = in.readInt();
        objAllocStackSamplingDepth = in.readInt();
        runGCOnGetResultsInMemoryProfiling = in.readBoolean();
        waitTrackingEnabled = in.readBoolean();
        sleepTrackingEnabled = in.readBoolean();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(nProfiledThreadsLimit);
        out.writeInt(samplingInterval);
        out.writeInt(objAllocStackSamplingInterval);
        out.writeInt(objAllocStackSamplingDepth);
        out.writeBoolean(runGCOnGetResultsInMemoryProfiling);
        out.writeBoolean(waitTrackingEnabled);
        out.writeBoolean(sleepTrackingEnabled);
    }
}
