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
import java.util.Date;

/**
 *
 * @author Tomas Hurka
 */
public class ThreadDumpResponse extends Response {

    private boolean jdk15;
    private Date time;
    Object[] cdThreads;

    public ThreadDumpResponse(boolean j15, Date d, Object[] td) {
        super(true, THREAD_DUMP);
        jdk15 = j15;
        time = d;
        if (td == null) td = new Object[0];
        cdThreads = td;
    }

    // Custom serialization support
    ThreadDumpResponse() {
        super(true, THREAD_DUMP);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isJDK15() {
        return jdk15;
    }

    public Date getTime() {
        return time;
    }

    public Object[] getThreads() {
        return cdThreads;
    }

    void readObject(ObjectInputStream in) throws IOException {
        jdk15 = in.readBoolean();
        long t = in.readLong();
        time = new Date(t);
        int len = in.readInt();
        cdThreads = new Object[len];
        for (int i = 0; i < len; i++) {
            try {
                cdThreads[i] = in.readObject();
            } catch (ClassNotFoundException ex) {
                throw new IOException(ex);
            }
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeBoolean(jdk15);
        out.writeLong(time.getTime());
        out.writeInt(cdThreads.length);
        for (int i = 0; i < cdThreads.length; i++) {
            out.writeObject(cdThreads[i]);
        }
        time = null;
        cdThreads = null;
    }
}
