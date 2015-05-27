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
import java.util.Arrays;

/**
 *
 * @author Tomas Hurka
 */
public class GetClassFileBytesCommand extends Command {

    private String[] classes;
    private int[] classLoaderIds;

    public GetClassFileBytesCommand(String[] classes, int[] classLoaderIds) {
        this();
        this.classes = classes;
        this.classLoaderIds = classLoaderIds;
    }

    // Custom serializaion support
    GetClassFileBytesCommand() {
        super(GET_CLASS_FILE_BYTES);
    }
    
    public int[] getClassLoaderIds() {
        return classLoaderIds;
    }

    public String[] getClasses() {
        return classes;
    }
    
    void readObject(ObjectInputStream in) throws IOException {
        int nClasses = in.readInt();

        if (nClasses == 0) {
            return;
        }

        classes = new String[nClasses];
        classLoaderIds = new int[nClasses];

        for (int i = 0; i < nClasses; i++) {
            classes[i] = in.readUTF().replace('/', '.');    // NOI18N
            classLoaderIds[i] = in.readInt();
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        if (classes == null) {
            out.writeInt(0);

            return;
        }

        int nClasses = classes.length;
        out.writeInt(nClasses);

        for (int i = 0; i < nClasses; i++) {
            out.writeUTF(classes[i]);
            out.writeInt(classLoaderIds[i]);
        }

        classes = null;
        classLoaderIds = null;
    }

    public String toString() {
        return super.toString() + " "+classes.length+" classes(): "+Arrays.toString(classes);   // NOI18N
    }
}
