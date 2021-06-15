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

package org.graalvm.visualvm.lib.ui.cpu.statistics;

import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.jfluid.marker.Mark;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class StatisticalModule extends JPanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int mId = -1;
    private Mark mark = Mark.DEFAULT;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    final public void setSelectedMethodId(int methodId) {
        int oldId = this.mId;
        this.mId = methodId;
        if (oldId != this.mId) {
            onMethodSelectionChange(oldId, this.mId);
        }
    }

    final protected int getSelectedMethodId() {
        return mId;
    }

    final public void setSelectedMark(Mark mark) {
        Mark oldMark = this.mark;
        this.mark = mark;

        if (!oldMark.equals(this.mark)) {
            onMarkSelectionChange(oldMark, this.mark);
        }
    }

    final protected Mark getSelectedMark() {
        return this.mark;
    }

    public abstract void refresh(RuntimeCPUCCTNode appNode);

    protected void onMarkSelectionChange(Mark oldMark, Mark newMark) {
    }

    protected void onMethodSelectionChange(int oldMethodId, int newMethodId) {
    }
}
