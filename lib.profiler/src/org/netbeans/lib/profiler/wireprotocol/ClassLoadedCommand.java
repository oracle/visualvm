/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.lib.profiler.wireprotocol;

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
