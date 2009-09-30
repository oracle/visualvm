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

package org.netbeans.modules.profiler.ui;

import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.UIManager;


/**
 *
 * @author Jiri Sedlacek
 */
public class HyperlinkTextArea extends HTMLTextArea {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String originalText;
    private boolean selected = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public HyperlinkTextArea() {
        setFont(UIManager.getFont("Label.font")); // NOI18N
        setForeground(Color.DARK_GRAY);
        setOpaque(false);
        setFocusable(true);
        setHighlighter(null);
        setShowPopup(false);

        addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (isSelected()) {
                        return;
                    }

                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    if (isFocusOwner()) {
                        return;
                    }

                    HyperlinkTextArea.this.decorateHighlight();
                }

                public void mouseExited(MouseEvent e) {
                    if (isSelected()) {
                        return;
                    }

                    setCursor(Cursor.getDefaultCursor());

                    if (isFocusOwner()) {
                        return;
                    }

                    HyperlinkTextArea.this.decorateNormal();
                }
            });

        addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    if (!isSelected()) {
                        HyperlinkTextArea.this.decorateHighlight();
                    }
                }

                public void focusLost(FocusEvent e) {
                    if (!isSelected()) {
                        HyperlinkTextArea.this.decorateNormal();
                    }
                }
            });
    }

    public HyperlinkTextArea(String text) {
        this();
        setText(text);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    public void updateAppearance() {
        if (isSelected() || isFocusOwner()) decorateHighlight();
        else decorateNormal();
    }

    public void setForeground(Color color) {
        String originalTextBkp = originalText;
        super.setForeground(color); // changes originalText!
        originalText = originalTextBkp;
        super.setText(originalText);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;

        if (selected) {
            decorateHighlight();
        } else {
            decorateNormal();
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setText(String value) {
        this.originalText = value;
        super.setText(value);
    }

    public void scrollRectToVisible(Rectangle aRect) {
    } // Has to be overridden to prevent automatic scrolling to active link

    protected Color getHighlightColor() {
        return Color.BLACK;
    }

    protected String getHighlightText(String originalText) {
        return "<u>" + originalText + "</u>";
    } // NOI18N

    protected Color getNormalColor() {
        return Color.DARK_GRAY;
    }

    protected String getNormalText(String originalText) {
        return originalText;
    }

    private void decorateHighlight() {
        setForeground(getHighlightColor()); // Must be before setText!
        super.setText(getHighlightText(originalText)); // NOI18N
    }

    private void decorateNormal() {
        setForeground(getNormalColor()); // Must be before setText!
        super.setText(getNormalText(originalText));
    }
}
