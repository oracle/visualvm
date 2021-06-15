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
 * Command that is issued by back end when the instance is allocated via reflection
 * and a classId is needed for given class identified by class name and classloader id.
 *
 * It is only used for Memory profiling
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class GetClassIdCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String className;
    private int classLoaderId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates new MethodLoadedCommand.
     *
     * @param className name of the class loaded
     * @param classLoaderId id of ClassLoader that loaded the class
     */
    public GetClassIdCommand(String className, int classLoaderId) {
        this();
        this.className = className;

        // At the client side we treat classes loaded by bootstrap and by system classloaders in the same way
        if (classLoaderId == -1) {
            classLoaderId = 0;
        }

        this.classLoaderId = classLoaderId;
    }

    // Custom serialization support
    GetClassIdCommand() {
        super(GET_CLASSID);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @return id of ClassLoader that loaded the class
     */
    public int getClassLoaderId() {
        return classLoaderId;
    }

    /**
     * @return name of the class loaded
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return Debug println of values
     */
    public String toString() {
        return super.toString() + ", className: " + className // NOI18N
               + ", classLoaderId: " + classLoaderId; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        className = in.readUTF();
        classLoaderId = in.readInt();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(className);
        out.writeInt(classLoaderId);
    }
}
