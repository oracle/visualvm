/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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
