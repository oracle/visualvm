/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;

/**
 *
 * @author Tomas Hurka
 */
public class GetClassFileBytesCommand extends Command {

    private String[] classes;
    private int[] classLoaderIds;

    public GetClassFileBytesCommand(String[] classes, int[] classLoaderIds) {
        this();
        this.classes = classes;
        this.classLoaderIds = classLoaderIds;
    }

    // Custom serializaion support
    GetClassFileBytesCommand() {
        super(GET_CLASS_FILE_BYTES);
    }

    public int[] getClassLoaderIds() {
        return classLoaderIds;
    }

    public String[] getClasses() {
        return classes;
    }

    void readObject(ObjectInputStream in) throws IOException {
        int nClasses = in.readInt();

        if (nClasses == 0) {
            return;
        }

        classes = new String[nClasses];
        classLoaderIds = new int[nClasses];

        for (int i = 0; i < nClasses; i++) {
            classes[i] = in.readUTF().replace('/', '.');    // NOI18N
            classLoaderIds[i] = in.readInt();
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        if (classes == null) {
            out.writeInt(0);

            return;
        }

        int nClasses = classes.length;
        out.writeInt(nClasses);

        for (int i = 0; i < nClasses; i++) {
            out.writeUTF(classes[i]);
            out.writeInt(classLoaderIds[i]);
        }

        classes = null;
        classLoaderIds = null;
    }

    public String toString() {
        return super.toString() + " "+classes.length+" classes(): "+Arrays.toString(classes);   // NOI18N
    }
}
