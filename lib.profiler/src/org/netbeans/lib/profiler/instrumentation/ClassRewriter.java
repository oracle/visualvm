/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.instrumentation;

import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.utils.MiscUtils;
import java.io.IOException;


/**
 * This class contains functionality to rewrite a given complete class file, replacing given
 * methodinfos and appending the constant pool.
 *
 * @author  Misha Dmitriev
 */
public class ClassRewriter {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static byte[] rewriteClassFile(DynamicClassInfo clazz, byte[][] replacementMethodInfos, int nAddedCPEntries,
                                          byte[] addedCPContents) {
        // Now assemble a new class file. First get original class file bytes.
        byte[] origBytes = null;

        try {
            origBytes = clazz.getClassFileBytes();
        } catch (IOException ex) {
            // Shouldn't happen, so a message just in case
            MiscUtils.internalError("ClassRewriter: can't get original class file bytes for class " + clazz.getName()
                                    + "\nIOException message = " + ex.getMessage()); // NOI18N
        }

        // Compute new class file length.
        int newLen = origBytes.length;

        // First add new constant pool size
        newLen += addedCPContents.length;

        // Now add differences between new and old method sizes
        int nMethods = clazz.getMethodNames().length;

        for (int i = 0; i < nMethods; i++) {
            if (replacementMethodInfos[i] != null) {
                newLen += (replacementMethodInfos[i].length - clazz.getOrigMethodInfoLength(i));
            }
        }

        byte[] res = new byte[newLen];

        // Copy over unchanged contents from old class file, copy/add changed contents, and adjust some counts
        int destPos = 0;
        // Copy preamble and original constant pool unchanged
        System.arraycopy(origBytes, 0, res, destPos, clazz.getOrigIntermediateDataStartOfs());
        destPos += clazz.getOrigIntermediateDataStartOfs();
        // Copy our new constant pool extension
        System.arraycopy(addedCPContents, 0, res, destPos, addedCPContents.length);
        destPos += addedCPContents.length;

        // Adjust the cpool count
        int newCPCount = clazz.getOrigCPoolCount() + nAddedCPEntries;
        int pos = clazz.getOrigCPoolStartOfs();
        res[pos] = (byte) ((newCPCount >> 8) & 255);
        res[pos + 1] = (byte) (newCPCount & 255);

        // Copy intermediate data and fields unchanged
        int count = clazz.getOrigMethodsStartOfs() - clazz.getOrigIntermediateDataStartOfs();
        System.arraycopy(origBytes, clazz.getOrigIntermediateDataStartOfs(), res, destPos, count);
        destPos += count;

        // Now copy all new methodInfos. First write the method count
        res[destPos] = (byte) ((nMethods >> 8) & 255);
        res[destPos + 1] = (byte) (nMethods & 255);
        destPos += 2;

        // Write methodInfos
        for (int i = 0; i < nMethods; i++) {
            if (replacementMethodInfos[i] != null) {
                System.arraycopy(replacementMethodInfos[i], 0, res, destPos, replacementMethodInfos[i].length);
                destPos += replacementMethodInfos[i].length;
            } else {
                byte[] origMethodInfo = clazz.getOrigMethodInfo(i);
                System.arraycopy(origMethodInfo, 0, res, destPos, origMethodInfo.length);
                destPos += origMethodInfo.length;
            }
        }

        // Copy what remains - class attributes
        count = origBytes.length - clazz.getOrigAttrsStartOfs();
        System.arraycopy(origBytes, clazz.getOrigAttrsStartOfs(), res, destPos, count);

        // For debugging
        //if (clazz.getName().equals("profilertestapp/Main")) {
        //  saveClassFileToDisk(clazz, res);
        //}
        return res;
    }

    public static void saveToDisk(String name, byte[] classBytes) {
        if (Platform.getJDKVersionNumber() == Platform.JDK_CVM) {
            // No room on device to do this
            return;
        }
        name = name.replace('/', '_'); // NOI18N

        try {
            System.err.print("*** Gonna save bytecode " + name + " to disk... "); // NOI18N

            java.io.OutputStream out = new java.io.FileOutputStream(new java.io.File(name + ".class")); // NOI18N
            out.write(classBytes);
            out.close();
            System.err.println("done"); // NOI18N
        } catch (Exception ex) {
            System.err.println("*** In RecursiveMethodInstrumentor.saveClassFileToDisk caught ex = " + ex); // NOI18N
        }
    }

    private static void saveClassFileToDisk(DynamicClassInfo clazz, byte[] replacementClassFile) {
        saveToDisk(clazz.getName(), replacementClassFile);
    }
}
