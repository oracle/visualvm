/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * Request from the client to the back end to initiate TA instrumentation of the given type.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Adrian Mos
 * @author Ian Formanek
 */
public class InitiateProfilingCommand extends Command {

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String[] classNames;
    private String[] profilingPointHandlers;
    private int[] profilingPointIDs;
    private String[] profilingPointInfos;
    private boolean instrSpawnedThreads;
    private boolean startProfilingPointsActive;
    private int instrType;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public InitiateProfilingCommand(int instrType, String[] classNames,
                                          int[] ppIDs, String[] ppHandlers, String[] ppInfos,
                                          boolean instrSpawnedThreads, boolean startProfilingPointsActive) {
        super(INITIATE_PROFILING);
        if ((classNames == null)) {
            classNames = new String[] { " " }; // NOI18N
        } else if (classNames[0] == null) {
            classNames[0] = " "; // NOI18N
        }

        this.instrType = instrType;
        this.classNames = classNames;
        profilingPointIDs = ppIDs;
        profilingPointHandlers = ppHandlers;
        profilingPointInfos = ppInfos;
        this.instrSpawnedThreads = instrSpawnedThreads;
        this.startProfilingPointsActive = startProfilingPointsActive;
    }

    /** Legacy support for single root instrumentation */
    public InitiateProfilingCommand(int instrType, String className, boolean instrSpawnedThreads,
                                          boolean startProfilingPointsActive) {
        this(instrType,
             className==null ? new String[]{" "} : new String[]{className},
             null,null,null,
             instrSpawnedThreads,startProfilingPointsActive);
    }


    /** This is a special method only called to setup the connection in ProfilerClient.connectToServer() - see comments there */
    public InitiateProfilingCommand(int instrType, String className) {
        this(instrType,className,false,false);
    }

    public InitiateProfilingCommand(int instrType) {
        this(instrType,null);
    }

    // Custom serialzation support
    InitiateProfilingCommand() {
        super(INITIATE_PROFILING);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean getInstrSpawnedThreads() {
        return instrSpawnedThreads;
    }

    public void setInstrType(int t) {
        instrType = t;
    }

    public int getInstrType() {
        return instrType;
    }

    public String[] getProfilingPointHandlers() {
        return profilingPointHandlers;
    }

    public int[] getProfilingPointIDs() {
        return profilingPointIDs;
    }

    public String[] getProfilingPointInfos() {
        return profilingPointInfos;
    }

    public String getRootClassName() {
        return classNames[0];
    } // Legacy support for one root

    public String[] getRootClassNames() {
        return classNames;
    }

    public boolean isStartProfilingPointsActive() {
        return startProfilingPointsActive;
    }

    // for debugging
    public String toString() {
        return super.toString() + ", instrType = " + instrType; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        instrType = in.readInt();

        int len = in.readInt();
        classNames = new String[len];

        for (int i = 0; i < len; i++) {
            classNames[i] = in.readUTF().intern(); // Interning is important, since checks are through '=='
        }

        instrSpawnedThreads = in.readBoolean();
        startProfilingPointsActive = in.readBoolean();

        try {
            profilingPointIDs = (int[]) in.readObject();
            profilingPointHandlers = (String[]) in.readObject();
            profilingPointInfos = (String[]) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(instrType);
        out.writeInt(classNames.length);

        for (int i = 0; i < classNames.length; i++) {
            out.writeUTF(classNames[i]);
        }

        out.writeBoolean(instrSpawnedThreads);
        out.writeBoolean(startProfilingPointsActive);
        out.writeObject(profilingPointIDs);
        out.writeObject(profilingPointHandlers);
        out.writeObject(profilingPointInfos);
    }
}
