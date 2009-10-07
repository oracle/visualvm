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

package org.netbeans.lib.profiler.ui.components;

import java.awt.*;
import javax.swing.*;


/**
 * Anti-aliased JLabel.
 *
 * @author Ian Formanek
 */
public class JAntiLabel extends JLabel {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a <code>JLabel</code> instance with
     * no image and with an empty string for the title.
     * The label is centered vertically
     * in its display area.
     * The label's contents, once set, will be displayed on the leading edge
     * of the label's display area.
     */
    public JAntiLabel() {
        super();
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified text.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param text The text to be displayed by the label.
     */
    public JAntiLabel(String text) {
        super(text);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified image.
     * The label is centered vertically and horizontally
     * in its display area.
     *
     * @param image The image to be displayed by the label.
     */
    public JAntiLabel(Icon image) {
        super(image);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified
     * text and horizontal alignment.
     * The label is centered vertically in its display area.
     *
     * @param text                The text to be displayed by the label.
     * @param horizontalAlignment One of the following constants
     *                            defined in <code>SwingConstants</code>:
     *                            <code>LEFT</code>,
     *                            <code>CENTER</code>,
     *                            <code>RIGHT</code>,
     *                            <code>LEADING</code> or
     *                            <code>TRAILING</code>.
     */
    public JAntiLabel(String text, int horizontalAlignment) {
        super(text, horizontalAlignment);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified
     * image and horizontal alignment.
     * The label is centered vertically in its display area.
     *
     * @param image               The image to be displayed by the label.
     * @param horizontalAlignment One of the following constants
     *                            defined in <code>SwingConstants</code>:
     *                            <code>LEFT</code>,
     *                            <code>CENTER</code>,
     *                            <code>RIGHT</code>,
     *                            <code>LEADING</code> or
     *                            <code>TRAILING</code>.
     */
    public JAntiLabel(Icon image, int horizontalAlignment) {
        super(image, horizontalAlignment);
    }

    /**
     * Creates a <code>JLabel</code> instance with the specified
     * text, image, and horizontal alignment.
     * The label is centered vertically in its display area.
     * The text is on the trailing edge of the image.
     *
     * @param text                The text to be displayed by the label.
     * @param icon                The image to be displayed by the label.
     * @param horizontalAlignment One of the following constants
     *                            defined in <code>SwingConstants</code>:
     *                            <code>LEFT</code>,
     *                            <code>CENTER</code>,
     *                            <code>RIGHT</code>,
     *                            <code>LEADING</code> or
     *                            <code>TRAILING</code>.
     */
    public JAntiLabel(String text, Icon icon, int horizontalAlignment) {
        super(text, icon, horizontalAlignment);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void paintComponent(Graphics g) {
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        super.paintComponent(g);
    }
}
