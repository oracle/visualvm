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

package org.graalvm.visualvm.lib.jfluid.wireprotocol;

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

    private boolean remoteProfiling;
    private boolean absoluteTimerOn;
    private boolean threadCPUTimerOn;
    private int codeRegionCPUResBufSize;
    private int instrScheme;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SetUnchangeableInstrParamsCommand(boolean remote, boolean absoluteTimerOn, boolean threadCPUTimerOn, int instrScheme,
                                             int codeRegionCPUResBufSize) {
        super(SET_UNCHANGEABLE_INSTR_PARAMS);
        remoteProfiling = remote;
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

    public boolean getRemoteProfiling() {
        return remoteProfiling;
    }

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
        return super.toString() + ", remoteProfiling: " + remoteProfiling // NOI18N
               + ", absoluteTimerOn: " + absoluteTimerOn // NOI18N
               + ", threadCPUTimerOn: " + threadCPUTimerOn // NOI18N
               + ", instrScheme: " + instrScheme // NOI18N
               + ", codeRegionCPUResBufSize: " + codeRegionCPUResBufSize; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        remoteProfiling = in.readBoolean();
        absoluteTimerOn = in.readBoolean();
        threadCPUTimerOn = in.readBoolean();
        instrScheme = in.readInt();
        codeRegionCPUResBufSize = in.readInt();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(remoteProfiling);
        out.writeBoolean(absoluteTimerOn);
        out.writeBoolean(threadCPUTimerOn);
        out.writeInt(instrScheme);
        out.writeInt(codeRegionCPUResBufSize);
    }
}
