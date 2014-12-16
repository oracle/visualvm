/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
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
