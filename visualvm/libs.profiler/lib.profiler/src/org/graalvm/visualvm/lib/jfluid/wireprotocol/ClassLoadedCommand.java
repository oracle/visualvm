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
 * Notification about a class load event that the server sends to the client.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class ClassLoadedCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String className;
    private byte[] classFileBytes;
    private int[] thisAndParentLoaderData;
    private boolean threadInCallGraph;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ClassLoadedCommand(String className, int[] thisAndParentLoaderData, byte[] classFileBytes, boolean threadInCallGraph) {
        super(CLASS_LOADED);
        this.className = className;
        this.thisAndParentLoaderData = thisAndParentLoaderData;
        this.classFileBytes = classFileBytes;
        this.threadInCallGraph = threadInCallGraph;
    }

    // Custom serialization support
    ClassLoadedCommand() {
        super(CLASS_LOADED);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public byte[] getClassFileBytes() {
        return classFileBytes;
    }

    public String getClassName() {
        return className;
    }

    public int[] getThisAndParentLoaderData() {
        return thisAndParentLoaderData;
    }

    public boolean getThreadInCallGraph() {
        return threadInCallGraph;
    }

    // for debugging
    public String toString() {
        return super.toString() + ", className: " + className // NOI18N
               + ", threadInCallGraph: " + threadInCallGraph // NOI18N
               + ", thisAndParentLoaderData: " // NOI18N
               + thisAndParentLoaderData[0] + ", " // NOI18N
               + thisAndParentLoaderData[1] + ", " // NOI18N
               + thisAndParentLoaderData[2] + ", classFileBytes: "
               + ((classFileBytes == null) ? "null" : ("" + classFileBytes.length)); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        className = in.readUTF();
        thisAndParentLoaderData = new int[3];

        for (int i = 0; i < 3; i++) {
            thisAndParentLoaderData[i] = in.readInt();
        }

        int len = in.readInt();

        if (len == 0) {
            classFileBytes = null;
        } else {
            classFileBytes = new byte[len];
            in.readFully(classFileBytes);
        }

        threadInCallGraph = in.readBoolean();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(className);

        for (int i = 0; i < 3; i++) {
            out.writeInt(thisAndParentLoaderData[i]);
        }

        if (classFileBytes != null) {
            out.writeInt(classFileBytes.length);
            out.write(classFileBytes);
            classFileBytes = null;
        } else {
            out.writeInt(0);
        }

        out.writeBoolean(threadInCallGraph);
    }
}
