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

package org.netbeans.modules.profiler.ppoints.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;


/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ValidityAwarePanel extends JPanel implements Scrollable {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Vector<ValidityListener> listeners = new Vector();
    private boolean isValid;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getInitialFocusTarget() {
        return null;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    public Dimension getPreferredSize() {
        if (getParent() instanceof JViewport) {
            return getMinimumSize();
        } else {
            return super.getPreferredSize();
        }
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 50;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        Container parent = getParent();

        if ((parent == null) || !(parent instanceof JViewport)) {
            return false;
        }

        return getMinimumSize().width < ((JViewport) parent).getWidth();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10;
    }

    public void addValidityListener(ValidityListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public boolean areSettingsValid() {
        return isValid;
    }

    public void removeValidityListener(ValidityListener listener) {
        listeners.remove(listener);
    }

    protected void fireValidityChanged(boolean isValid) {
        this.isValid = isValid;

        for (ValidityListener listener : listeners) {
            listener.validityChanged(isValid);
        }
    }
}
