/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.heapwalk.model;

import org.graalvm.visualvm.lib.jfluid.heap.*;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;


/**
 * Constants and utilities for Fields Browser
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "BrowserUtils_OutOfMemoryMsg=<html><b>Out of memory in HeapWalker</b><br><br>To avoid this error please increase the -Xmx value<br>in the etc/visualvm.conf file in VisualVM directory.</html>",
    "BrowserUtils_TruncatedMsg=...<truncated>...",
    "BrowserUtils_PathCopiedToClipboard=Path from root copied to the clipboard."
})
public class BrowserUtils {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class GroupingInfo {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public int collapseUnitSize;
        public int containersCount;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        GroupingInfo(int containersCount, int collapseUnitSize) {
            this.containersCount = containersCount;
            this.collapseUnitSize = collapseUnitSize;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final ImageIcon ICON_INSTANCE = Icons.getImageIcon(LanguageIcons.INSTANCE);
    public static final ImageIcon ICON_PRIMITIVE = Icons.getImageIcon(LanguageIcons.PRIMITIVE);
    public static final ImageIcon ICON_ARRAY = Icons.getImageIcon(LanguageIcons.ARRAY);
    public static final ImageIcon ICON_PROGRESS = Icons.getImageIcon(HeapWalkerIcons.PROGRESS);
    public static final ImageIcon ICON_STATIC = Icons.getImageIcon(HeapWalkerIcons.STATIC);
    public static final ImageIcon ICON_LOOP = Icons.getImageIcon(HeapWalkerIcons.LOOP);
    public static final ImageIcon ICON_GCROOT = Icons.getImageIcon(HeapWalkerIcons.GC_ROOT);
    private static final RequestProcessor REQUEST_PROCESSOR = new RequestProcessor("HeapWalker Processor", 5, true); // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Get item class of an array. (e.g. <code>byte[]</code> for <code>byte[][]</code>)
     * @return <code>arrayTypeName</code> without last '[]'
     * */
    public static String getArrayItemType(String arrayTypeName) {
        int arrayBracketsIdx = arrayTypeName.lastIndexOf('['); // NOI18N

        return ((arrayBracketsIdx == -1) ? arrayTypeName : arrayTypeName.substring(0, arrayBracketsIdx));
    }

    /** Get base class of an array. (e.g. <code>byte</code> for <code>byte[][]</code>)
     * @return <code>arrayTypeName</code> without any trailing '[]'
     * */
    public static String getArrayBaseType(String arrayTypeName) {
        int arrayBracketsIdx = arrayTypeName.indexOf('['); // NOI18N
        return ((arrayBracketsIdx == -1) ? arrayTypeName : arrayTypeName.substring(0, arrayBracketsIdx));
    }

    public static String getSimpleType(String fullType) {
        int simpleTypeIdx = fullType.lastIndexOf('.'); // NOI18N

        if (simpleTypeIdx == -1) {
            return fullType;
        } else {
            if (fullType.startsWith("<")) { // NOI18N

                return "<" + fullType.substring(simpleTypeIdx + 1); // NOI18N
            } else {
                return fullType.substring(simpleTypeIdx + 1);
            }
        }
    }

    public static boolean isStaticField(FieldValue fieldValue) {
        return fieldValue.getField().isStatic();
    }
    
    public static ImageIcon createGCRootIcon(ImageIcon icon) {
        return new ImageIcon(ImageUtilities.mergeImages(icon.getImage(), ICON_GCROOT.getImage(), 0, 0));
    }

    public static ImageIcon createLoopIcon(ImageIcon icon) {
        return new ImageIcon(ImageUtilities.mergeImages(icon.getImage(), ICON_LOOP.getImage(), 0, 0));
    }

    public static ImageIcon createStaticIcon(ImageIcon icon) {
        return new ImageIcon(ImageUtilities.mergeImages(icon.getImage(), ICON_STATIC.getImage(), 0, 0));
    }

    public static RequestProcessor.Task performTask(Runnable task) {
        return REQUEST_PROCESSOR.post(task);
    }
    
    public static RequestProcessor.Task performTask(Runnable task, int timeToWait) {
        return REQUEST_PROCESSOR.post(task, timeToWait);
    }
}
