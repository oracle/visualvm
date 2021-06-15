/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.api;

import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.profiler.spi.ActionsSupportProvider;
import org.openide.util.Lookup;

/**
 * Allows to customize key bindings for profiler actions.
 *
 * @author Jiri Sedlacek
 */
public final class ActionsSupport {

    public static final KeyStroke NO_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_UNDEFINED, 0);

    private static String ACC_DELIMITER;
    public static String keyAcceleratorString(KeyStroke keyStroke) {
        if (keyStroke == null || NO_KEYSTROKE.equals(keyStroke)) return null;

        String keyText = KeyEvent.getKeyText(keyStroke.getKeyCode());

        int modifiers = keyStroke.getModifiers();
        if (modifiers == 0) return keyText;

        if (ACC_DELIMITER == null) {
            ACC_DELIMITER = UIManager.getString("MenuItem.acceleratorDelimiter"); // NOI18N
            if (ACC_DELIMITER == null) ACC_DELIMITER = "+"; // NOI18N // Note: NetBeans default, Swing uses '-' by default
        }

        return KeyEvent.getKeyModifiersText(modifiers) + ACC_DELIMITER + keyText;
    }

    public static KeyStroke registerAction(String actionKey, Action action, ActionMap actionMap, InputMap inputMap) {
        for (ActionsSupportProvider provider : Lookup.getDefault().lookupAll(ActionsSupportProvider.class)) {
            KeyStroke ks = provider.registerAction(actionKey, action, actionMap, inputMap);
            if (ks != null) return ks;
        }
        return null;
    }

}
