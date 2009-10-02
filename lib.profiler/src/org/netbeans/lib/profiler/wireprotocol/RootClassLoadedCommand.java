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
 * This command is issued by the back end to signal that the root instrumentation class has been loaded.
 * It contains the information about all classes loaded by the target JVM by that time.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class RootClassLoadedCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String eventBufferFileName;
    private int[] allLoadedClassLoaderIds;
    private String[] allLoadedClassNames;
    private byte[][] cachedClassFileBytes;
    private int[] parentLoaderIds; // An index into this table is a loader id, and the value at this index is this loader's parent loader id.
    private int classCount;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RootClassLoadedCommand(String[] allLoadedClassNames, int[] loaderIds, byte[][] cachedClassFileBytes, int classCount,
                                  int[] parentLoaderIds, String eventBufferFileName) {
        super(ROOT_CLASS_LOADED);
        this.allLoadedClassNames = allLoadedClassNames;
        this.allLoadedClassLoaderIds = loaderIds;
        this.cachedClassFileBytes = cachedClassFileBytes;
        this.classCount = classCount;
        this.parentLoaderIds = parentLoaderIds;
        this.eventBufferFileName = eventBufferFileName;
    }

    // Custom serialization support
    RootClassLoadedCommand() {
        super(ROOT_CLASS_LOADED);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int[] getAllLoadedClassLoaderIds() {
        return allLoadedClassLoaderIds;
    }

    public String[] getAllLoadedClassNames() {
        return allLoadedClassNames;
    }

    public byte[][] getCachedClassFileBytes() {
        byte[][] res = cachedClassFileBytes;
        cachedClassFileBytes = null; // Save memory

        return res;
    }

    public String getEventBufferFileName() {
        return eventBufferFileName;
    }

    public int[] getParentLoaderIds() {
        // Return a copy, just in case, since this instance of parentLoaderIds is reused when this command is received
        int[] newParentLoaderIds = new int[parentLoaderIds.length];
        System.arraycopy(parentLoaderIds, 0, newParentLoaderIds, 0, parentLoaderIds.length);

        return newParentLoaderIds;
    }

    // for debugging
    public String toString() {
        return super.toString() + ", eventBufferFileName: " + eventBufferFileName; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        int len = in.readInt();
        allLoadedClassNames = new String[len];

        for (int i = 0; i < len; i++) {
            allLoadedClassNames[i] = in.readUTF();
        }

        allLoadedClassLoaderIds = new int[len];

        for (int i = 0; i < len; i++) {
            allLoadedClassLoaderIds[i] = in.readInt();
        }

        len = in.readInt();

        if (len == 0) {
            cachedClassFileBytes = null;
        } else {
            cachedClassFileBytes = new byte[len][];

            for (int i = 0; i < len; i++) {
                int bytesLen = in.readInt();

                if (bytesLen == 0) {
                    continue;
                } else {
                    cachedClassFileBytes[i] = new byte[bytesLen];
                    in.readFully(cachedClassFileBytes[i]);
                }
            }
        }

        len = in.readInt();
        parentLoaderIds = new int[len];

        for (int i = 0; i < len; i++) {
            parentLoaderIds[i] = in.readInt();
        }

        eventBufferFileName = in.readUTF();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(classCount);

        for (int i = 0; i < classCount; i++) {
            out.writeUTF(allLoadedClassNames[i]);
        }

        for (int i = 0; i < classCount; i++) {
            int loaderId = allLoadedClassLoaderIds[i];

            // At the client side we treat classes loaded by the bootstrap and by the system classloaders in the same way
            if (loaderId == -1) {
                loaderId = 0;
            }

            out.writeInt(loaderId);
        }

        if (cachedClassFileBytes == null) {
            out.writeInt(0);
        } else {
            out.writeInt(classCount);

            for (int i = 0; i < classCount; i++) {
                if (cachedClassFileBytes[i] == null) {
                    out.writeInt(0);
                } else {
                    out.writeInt(cachedClassFileBytes[i].length);
                    out.write(cachedClassFileBytes[i]);
                }
            }
        }

        out.writeInt(parentLoaderIds.length);

        for (int i = 0; i < parentLoaderIds.length; i++) {
            out.writeInt(parentLoaderIds[i]);
        }

        out.writeUTF(eventBufferFileName);
        // Free memory
        allLoadedClassNames = null;
        allLoadedClassLoaderIds = null;
        cachedClassFileBytes = null;
        parentLoaderIds = null;
        eventBufferFileName = null;
    }
}
