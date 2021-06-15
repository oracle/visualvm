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
 * Command that is issued by back end when the option to instrument methods invoked via reflection is on,
 * and a given method is just about to be executed in this way.
 *
 * It is only used for CPU profiling, when Eager or Lazy schemes are used (RecursiveMethodInstrumentor1 or
 * RecursiveMethodInstrumentor2). In total scheme, everything is handle by ClassLoadedCommand.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class MethodLoadedCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String className;
    private String methodName;
    private String methodSignature;
    private int classLoaderId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates new MethodLoadedCommand.
     *
     * @param className name fo the class loaded
     * @param classLoaderId id of ClassLoader that loaded the class
     * @param methodName Name of method that is going to be invoked
     * @param methodSignature Signature of method that is going to be invoked
     */
    public MethodLoadedCommand(String className, int classLoaderId, String methodName, String methodSignature) {
        super(METHOD_LOADED);
        this.className = className;

        // At the client side we treat classes loaded by bootstrap and by system classloaders in the same way
        if (classLoaderId == -1) {
            classLoaderId = 0;
        }

        this.classLoaderId = classLoaderId;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
    }

    // Custom serialization support
    MethodLoadedCommand() {
        super(METHOD_LOADED);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @return id of ClassLoader that loaded the class
     */
    public int getClassLoaderId() {
        return classLoaderId;
    }

    /**
     * @return name fo the class loaded
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return Name of method that is going to be invoked
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @return Signature of method that is going to be invoked
     */
    public String getMethodSignature() {
        return methodSignature;
    }

    /**
     * @return Debug println of values
     */
    public String toString() {
        return super.toString() + ", className: " + className // NOI18N
               + ", classLoaderId: " + classLoaderId // NOI18N
               + ", methodName: " + methodName // NOI18N
               + ", methodSignature: " + methodSignature; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        className = in.readUTF();
        classLoaderId = in.readInt();
        methodName = in.readUTF();
        methodSignature = in.readUTF();
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(className);
        out.writeInt(classLoaderId);
        out.writeUTF(methodName);
        out.writeUTF(methodSignature);
    }
}
