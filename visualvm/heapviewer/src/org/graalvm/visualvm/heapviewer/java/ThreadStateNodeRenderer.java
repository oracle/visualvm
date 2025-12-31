/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.java;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerRenderer;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.threads.ThreadStateIcon;

/**
 *
 * @author Tomas Hurka
 */
public class ThreadStateNodeRenderer extends LabelRenderer implements HeapViewerRenderer {

    private static final Icon ICON = Icons.getIcon(ProfilerIcons.THREAD);

    public ThreadStateNodeRenderer() {
        setIcon(ICON);
        setFont(getFont().deriveFont(Font.BOLD));
    }


    public void setValue(Object value, int row) {
        ThreadStateNode node = (ThreadStateNode)value;
        setText(node.getName());
        setIcon(getIcon(node.getState()));
    }

    public String getShortName() {
        return getText();
    }

    private static final int THREAD_ICON_SIZE = 9;
    private static final Map<Thread.State, Icon> STATE_ICONS_CACHE = new HashMap<>();

    private static Icon getIcon(Thread.State state) {
        Icon icon = STATE_ICONS_CACHE.get(state);

        if (icon == null) {
            int pState;
            switch (state) {
                case RUNNABLE:
                    pState = CommonConstants.THREAD_STATUS_RUNNING;
                    break;
                case BLOCKED:
                    pState = CommonConstants.THREAD_STATUS_MONITOR;
                    break;
                case WAITING:
                    pState = CommonConstants.THREAD_STATUS_WAIT;
                    break;
                case TIMED_WAITING:
                    pState = CommonConstants.THREAD_STATUS_SLEEPING;
                    break;
                default:
                    pState = CommonConstants.THREAD_STATUS_UNKNOWN;
                    break;
            }
            icon = new ThreadStateIcon(pState, THREAD_ICON_SIZE, THREAD_ICON_SIZE);
            STATE_ICONS_CACHE.put(state, icon);
        }
        return icon;
    }


}
