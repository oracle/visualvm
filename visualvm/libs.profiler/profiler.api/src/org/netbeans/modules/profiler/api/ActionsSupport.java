/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api;

import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import org.netbeans.modules.profiler.spi.ActionsSupportProvider;
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
