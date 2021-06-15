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
 * Response to the client's request to dump the current rough profiling data contents into the shared-memory file.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class DumpResultsResponse extends Response {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private long dumpAbsTimeStamp;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DumpResultsResponse(boolean yes, long absTimeStamp) {
        super(yes, DUMP_RESULTS);
        this.dumpAbsTimeStamp = absTimeStamp;
    }

    // Custom serialization support
    DumpResultsResponse() {
        super(true, DUMP_RESULTS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long getDumpAbsTimeStamp() {
        return dumpAbsTimeStamp;
    }

    // For debugging
    public String toString() {
        return "DumpResultsResponse, timeStamp: " + dumpAbsTimeStamp + ", " + super.toString(); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        dumpAbsTimeStamp = in.readLong();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(dumpAbsTimeStamp);
    }
}
