/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
public class HeapHistogramResponse extends Response {

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Date time;
    private String[] newNames;
    private int[] newids;
    private int[] ids;
    private long[] instances,bytes;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public HeapHistogramResponse(Date t, String[] nn, int[] nid, int[] id, long[] i, long[] b) {
        super(true, HEAP_HISTOGRAM);
        time = t;
        newNames = nn;
        newids = nid;
        ids = id;
        instances = i;
        bytes = b;
    }

    // Custom serialization support
    HeapHistogramResponse() {
        super(true, HEAP_HISTOGRAM);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Date getTime() {
        return time;
    }

    public String[] getNewNames() {
        return newNames;
    }

    public int[] getNewids() {
        return newids;
    }

    public int[] getIds() {
        return ids;
    }

    public long[] getInstances() {
        return instances;
    }

    public long[] getBytes() {
        return bytes;
    }

    void readObject(ObjectInputStream in) throws IOException {
        long t = in.readLong();
        time = new Date(t);
        int len = in.readInt();
        newNames = new String[len];
        for (int i = 0; i < len; i++) {
            newNames[i] = in.readUTF();
        }
        len = in.readInt();
        newids = new int[len];
        for (int i = 0; i < len; i++) {
            newids[i] = in.readInt();
        }
        len = in.readInt();
        ids = new int[len];
        for (int i = 0; i < len; i++) {
            ids[i] = in.readInt();
        }
        len = in.readInt();
        instances = new long[len];
        for (int i = 0; i < len; i++) {
            instances[i] = in.readLong();
        }
        len = in.readInt();
        bytes = new long[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = in.readLong();
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(time.getTime());
        out.writeInt(newNames.length);
        for (int i = 0; i < newNames.length; i++) {
            out.writeUTF(newNames[i]);
        }
        out.writeInt(newids.length);
        for (int i = 0; i < newids.length; i++) {
            out.writeInt(newids[i]);
        }
        out.writeInt(ids.length);
        for (int i = 0; i < ids.length; i++) {
            out.writeInt(ids[i]);
        }
        out.writeInt(instances.length);
        for (int i = 0; i < instances.length; i++) {
            out.writeLong(instances[i]);
        }
        out.writeInt(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            out.writeLong(bytes[i]);
        }
        newNames = null;
        newids = null;
        ids = null;
        instances = null;
        bytes = null;
    }
    
}
