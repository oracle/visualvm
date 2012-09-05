/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */

package org.netbeans.lib.profiler.wireprotocol;

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
