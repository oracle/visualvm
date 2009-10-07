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

package org.netbeans.modules.profiler.ui.stp;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 *
 * @author Jiri Sedlacek
 */
public class DefaultSettingsPanel extends JPanel implements Scrollable {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class SettingsChangeListener implements ActionListener, ChangeListener /*, DocumentListener*/ {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(ActionEvent e) {
            fireSettingsChanged();
        }

        public void stateChanged(ChangeEvent e) {
            fireSettingsChanged();
        }

        //    public void insertUpdate(DocumentEvent e) { fireSettingsChanged(); }
        //    public void removeUpdate(DocumentEvent e) { fireSettingsChanged(); }
        //    public void changedUpdate(DocumentEvent e) { fireSettingsChanged(); }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private SettingsChangeListener settingsChangeListener;
    private Vector<ChangeListener> changeListeners;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DefaultSettingsPanel() {
        setOpaque(true);
        setBackground(SelectProfilingTask.BACKGROUND_COLOR);

        changeListeners = new Vector();
        settingsChangeListener = new SettingsChangeListener();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        // Scroll almost one screen
        Container parent = getParent();

        if ((parent == null) || !(parent instanceof JViewport)) {
            return 50;
        }

        return (int) (((JViewport) parent).getHeight() * 0.95f);
    }

    public boolean getScrollableTracksViewportHeight() {
        // Allow dynamic vertical enlarging of the panel but request the vertical scrollbar when needed
        Container parent = getParent();

        if ((parent == null) || !(parent instanceof JViewport)) {
            return false;
        }

        return getPreferredSize().height < ((JViewport) parent).getHeight();
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    public void addChangeListener(ChangeListener listener) {
        if (!changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    SettingsChangeListener getSettingsChangeListener() {
        return settingsChangeListener;
    }

    private void fireSettingsChanged() {
        for (ChangeListener listener : changeListeners) {
            listener.stateChanged(new ChangeEvent(this));
        }
    }
}
