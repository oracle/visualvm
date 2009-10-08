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
 * Response containing instrumentation- and profiling-related statistics - most of the data that is presented if one
 * invokes Profile | Get internal statistics command in the tool.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class InternalStatsResponse extends Response {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public double averageHotswappingTime;
    public double clientDataProcTime;
    public double clientInstrTime;
    public double maxHotswappingTime;
    public double methodEntryExitCallTime0;
    public double methodEntryExitCallTime1;
    public double methodEntryExitCallTime2;
    public double minHotswappingTime;
    public double totalHotswappingTime;
    public int nClassLoads;
    public int nEmptyInstrMethodGroupResponses;
    public int nFirstMethodInvocations;
    public int nNonEmptyInstrMethodGroupResponses;
    public int nSingleMethodInstrMethodGroupResponses;

    // Fields made public as an exception, to avoid too many accessors
    public int nTotalInstrMethods;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * We don't use a normal constructor with parameters here, since there are too many parameters to pass.
     * Instead we use public data fields.
     */
    public InternalStatsResponse() {
        super(true, INTERNAL_STATS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // For debugging
    public String toString() {
        return "InternalStatsResponse, " + super.toString(); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        nTotalInstrMethods = in.readInt();
        nClassLoads = in.readInt();
        nFirstMethodInvocations = in.readInt();
        nNonEmptyInstrMethodGroupResponses = in.readInt();
        nEmptyInstrMethodGroupResponses = in.readInt();
        nSingleMethodInstrMethodGroupResponses = in.readInt();
        clientInstrTime = in.readDouble();
        clientDataProcTime = in.readDouble();
        totalHotswappingTime = in.readDouble();
        averageHotswappingTime = in.readDouble();
        minHotswappingTime = in.readDouble();
        maxHotswappingTime = in.readDouble();
        methodEntryExitCallTime0 = in.readDouble();
        methodEntryExitCallTime1 = in.readDouble();
        methodEntryExitCallTime2 = in.readDouble();
    }

    // Custom serialization support
    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(nTotalInstrMethods);
        out.writeInt(nClassLoads);
        out.writeInt(nFirstMethodInvocations);
        out.writeInt(nNonEmptyInstrMethodGroupResponses);
        out.writeInt(nEmptyInstrMethodGroupResponses);
        out.writeInt(nSingleMethodInstrMethodGroupResponses);
        out.writeDouble(clientInstrTime);
        out.writeDouble(clientDataProcTime);
        out.writeDouble(totalHotswappingTime);
        out.writeDouble(averageHotswappingTime);
        out.writeDouble(minHotswappingTime);
        out.writeDouble(maxHotswappingTime);
        out.writeDouble(methodEntryExitCallTime0);
        out.writeDouble(methodEntryExitCallTime1);
        out.writeDouble(methodEntryExitCallTime2);
    }
}
