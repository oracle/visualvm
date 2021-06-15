/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.consumervisualvm.engine;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.Utilities;

/**
 *
 * @author  Jiri Rechtacek
 */
public final class RestartNotifier implements StatusLineElementProvider {

    public Component getStatusLineElement () {
        return getUpdatesVisualizer ();
    }

    private static RestartIcon flasher = null;

    private static Runnable onMouseClick = null;

    /**
     * Return an icon that is flashing when a new internal exception occurs.
     * Clicking the icon opens the regular exception dialog box. The icon
     * disappears (is hidden) after a short period of time and the exception
     * list is cleared.
     *
     * @return A flashing icon component or null if console logging is switched on.
     */
    private static Component getUpdatesVisualizer () {
        if (null == flasher) {
            ImageIcon img1 = new ImageIcon (Utilities.loadImage ("org/netbeans/modules/autoupdate/featureondemand/resources/restart.png", false)); // NOI18N
            assert img1 != null : "Icon cannot be null.";
            flasher = new RestartIcon (img1);
        }
        return flasher;
    }

    public static RestartIcon getFlasher (Runnable whatRunOnMouseClick) {
        onMouseClick = whatRunOnMouseClick;
        return flasher;
    }

    public static class RestartIcon extends FlashingIcon {
        public RestartIcon (Icon img1) {
            super (img1);
            DISAPPEAR_DELAY_MILLIS = -1;
            // don't flashing by http://ui.netbeans.org/docs/ui/AutoUpdate/AutoUpdate.html
            STOP_FLASHING_DELAY = 0;
        }

        /**
         * User clicked the flashing icon, display the exception window.
         */
        protected void onMouseClick () {
            if (onMouseClick != null) {
                onMouseClick.run ();
            }
        }

        /**
         * The flashing icon disappeared (timed-out), clear the current
         * exception list.
         */
        protected void timeout () {}
    }
    
}
