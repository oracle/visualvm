/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.modules.appui.actions;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import org.graalvm.visualvm.lib.profiler.spi.ActionsSupportProvider;
import org.graalvm.visualvm.lib.ui.swing.FilterUtils;
import org.graalvm.visualvm.lib.ui.swing.SearchUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 * Definition of VisualVM shortcuts for UI actions.
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=ActionsSupportProvider.class, position=10)
public final class VisualVMActionsSupportProvider extends ActionsSupportProvider {
    
    public KeyStroke registerAction(String actionKey, Action action, ActionMap actionMap, InputMap inputMap) {
        KeyStroke ks = null;

        if (FilterUtils.FILTER_ACTION_KEY.equals(actionKey)) {
            ks = KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        } else if (SearchUtils.FIND_ACTION_KEY.equals(actionKey)) {
            ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        } else if (SearchUtils.FIND_NEXT_ACTION_KEY.equals(actionKey)) {
            ks = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        } else if (SearchUtils.FIND_PREV_ACTION_KEY.equals(actionKey)) {
            ks = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_MASK);
        } else if (SearchUtils.FIND_SEL_ACTION_KEY.equals(actionKey)) {
            ks = KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.CTRL_MASK);
        }

        if (ks != null) {
            actionMap.put(actionKey, action);
            inputMap.put(ks, actionKey);
        }

        return ks;
    }
    
}
