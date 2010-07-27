/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl.swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

/**
 * Predefined JScrollPane to be used in VisualVM, for example in details views.
 * Use UISupport.createScrollableContainer() method instead of instantiating
 * this class directly if creating scrollable container for the Options panel.
 *
 * @author Jiri Sedlacek
 */
public final class ScrollableContainer extends JScrollPane {

    /**
     * Creates new instance of ScrollableContainer.
     * 
     * @param view component to be displayed
     */
    public ScrollableContainer(JComponent view) {
        this(view, VERTICAL_SCROLLBAR_AS_NEEDED,
             HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * Creates new instance of ScrollableContainer.
     *
     * @param view component to be displayed
     * @param vsbPolicy policy flag for the vertical scrollbar
     * @param hsbPolicy policy flag for the horizontal scrollbar
     */
    public ScrollableContainer(JComponent view, int vsbPolicy, int hsbPolicy) {
        setViewportView(new ScrollableContents(view));

        setVerticalScrollBarPolicy(vsbPolicy);
        setHorizontalScrollBarPolicy(hsbPolicy);

        setBorder(BorderFactory.createEmptyBorder());
        setViewportBorder(BorderFactory.createEmptyBorder());

        getViewport().setOpaque(false);
        setOpaque(false);
    }


    // --- Scrollable container ------------------------------------------------

    private class ScrollableContents extends JPanel implements Scrollable {

        public ScrollableContents(JComponent contents) {
            super(new BorderLayout());
            setOpaque(false);
            add(contents, BorderLayout.CENTER);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect,
                                              int orientation, int direction) {
            return 20;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect,
                                               int orientation, int direction) {
            return (int)(visibleRect.height * 0.9d);
        }

        public boolean getScrollableTracksViewportWidth() {
            if (getHorizontalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_NEVER)
                return true;

            Container parent = getParent();
            if ((parent == null) || !(parent instanceof JViewport)) return false;
            return getMinimumSize().width < ((JViewport)parent).getWidth();
        }

        public boolean getScrollableTracksViewportHeight() {
            if (getVerticalScrollBarPolicy() == VERTICAL_SCROLLBAR_NEVER)
                return true;

            Container parent = getParent();
            if ((parent == null) || !(parent instanceof JViewport)) return false;
            return getMinimumSize().height < ((JViewport)parent).getHeight();
        }

    }

}
