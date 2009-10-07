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

package org.netbeans.lib.profiler.server.system;


/**
 * Provides methods for accessing thread stacks contents.
 *
 * @author  Misha Dmitriev
 */
public class Stacks {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Returns the number of Java frames on the stack of the current thread */
    public static native int getCurrentJavaStackDepth(Thread thread);

    /**
     * stackDepth parameter is the maximum number of stack frames that can be sampled. Returns the actual number of
     * stack frames sampled.
     */
    public static native int getCurrentStackFrameIds(Thread thread, int stackDepth, int[] stackFrameIds);

    /**
     * For the given array of jmethodIds, returns the names of the respective methods as
     * (class name, method name and method signature) triplets.
     * All this symbolic information is returned as a single packed array of bytes (with each string in UTF8 format).
     * packedArrayOffsets is filled out with offsets of all of these strings.
     *
     * @param nMethods The number of methods, length of the methodIds array
     * @param methodIds An array of jMethodIds for which we need their names
     * @param packedArrayOffsets An array that, upon return from this method, will contain the indexes into the returned
     *        array
     * @return A packed array of bytes of triplets [class name, method name, method signature], packedArrayOffsets
     *         contains indexes into this array for individual items
     */
    public static native byte[] getMethodNamesForJMethodIds(int nMethods, int[] methodIds, int[] packedArrayOffsets);

    /** Clear the above stack frame buffer permanently. */
    public static native void clearNativeStackFrameBuffer();

    /**
     * Creates the internal, C-level stack frame buffer, used for intermediate storage of data obtained using
     * getCurrentStackFrameIds. Since just a single buffer is used, getCurrentStackFrameIds is obviously not
     * multithread-safe. The code that uses this stuff has to use a single lock - so far not a problem for memory
     * profiling where we use it, since normally it collects data for just every 10th object, thus the probability
     * of contention is not very high.
     */
    public static native void createNativeStackFrameBuffer(int sizeInFrames);

    /** Should be called at earliest possible time */
    public static void initialize() {
        // Doesn't do anything in this version
    }
}
