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
 * Code region CPU profiling results, that are sent to the client upon request.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class CodeRegionCPUResultsResponse extends Response {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private long[] results;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CodeRegionCPUResultsResponse(long[] results) {
        super(true, CODE_REGION_CPU_RESULTS);
        // Note that he first element of the array is the total number of invocations and should not be changed.
        this.results = results;
    }

    // Custom serialization support
    CodeRegionCPUResultsResponse() {
        super(true, CODE_REGION_CPU_RESULTS);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public long[] getResults() {
        return results;
    }

    // For debugging
    public String toString() {
        return "CodeRegionCPUResultsResponse, length: " + results.length + ", " + super.toString(); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        int len = in.readInt();
        results = new long[len];

        for (int i = 0; i < len; i++) {
            results[i] = in.readLong();
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(results.length);

        for (int i = 0; i < results.length; i++) {
            out.writeLong(results[i]);
        }
    }
}
