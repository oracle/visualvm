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
 * This command, sent by the client, contains the instrumentation parameters (settings),
 * that cannot be modified once instrumentation is active and profiling is going on.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class SetUnchangeableInstrParamsCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private boolean absoluteTimerOn;
    private boolean threadCPUTimerOn;
    private int codeRegionCPUResBufSize;
    private int instrScheme;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SetUnchangeableInstrParamsCommand(boolean absoluteTimerOn, boolean threadCPUTimerOn, int instrScheme,
                                             int codeRegionCPUResBufSize) {
        super(SET_UNCHANGEABLE_INSTR_PARAMS);
        this.absoluteTimerOn = absoluteTimerOn;
        this.threadCPUTimerOn = threadCPUTimerOn;
        this.instrScheme = instrScheme;
        this.codeRegionCPUResBufSize = codeRegionCPUResBufSize;
    }

    // Custom serialization support
    SetUnchangeableInstrParamsCommand() {
        super(SET_UNCHANGEABLE_INSTR_PARAMS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean getAbsoluteTimerOn() {
        return absoluteTimerOn;
    }

    public int getCodeRegionCPUResBufSize() {
        return codeRegionCPUResBufSize;
    }

    public int getInstrScheme() {
        return instrScheme;
    }

    public boolean getThreadCPUTimerOn() {
        return threadCPUTimerOn;
    }

    // For debugging
    public String toString() {
        return super.toString() + ", absoluteTimerOn: " + absoluteTimerOn // NOI18N
               + ", threadCPUTimerOn: " + threadCPUTimerOn // NOI18N
               + ", instrScheme: " + instrScheme // NOI18N
               + ", codeRegionCPUResBufSize: " + codeRegionCPUResBufSize; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        absoluteTimerOn = in.readBoolean();
        threadCPUTimerOn = in.readBoolean();
        instrScheme = in.readInt();
        codeRegionCPUResBufSize = in.readInt();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(absoluteTimerOn);
        out.writeBoolean(threadCPUTimerOn);
        out.writeInt(instrScheme);
        out.writeInt(codeRegionCPUResBufSize);
    }
}
