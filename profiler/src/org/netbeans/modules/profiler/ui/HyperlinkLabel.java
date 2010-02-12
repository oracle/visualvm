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

import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URL;


/**
 *
 * @author Jiri Sedlacek
 */
public class HyperlinkLabel extends HTMLLabel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Runnable actionPerformer;
    private String focusedText;
    private String normalText;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public HyperlinkLabel(String normalText, String focusedText, Runnable actionPerformer) {
        super();
        setFocusable(true);
        setHighlighter(null);

        setText(normalText, focusedText);
        this.actionPerformer = actionPerformer;

        addFocusListener(new FocusListener() {
                public void focusGained(FocusEvent e) {
                    HyperlinkLabel.this.setText(HyperlinkLabel.this.focusedText);
                }

                public void focusLost(FocusEvent e) {
                    HyperlinkLabel.this.setText(org.netbeans.modules.profiler.ui.HyperlinkLabel.this.normalText);
                }
            });

        addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        HyperlinkLabel.this.actionPerformer.run();
                    }
                }
            });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setText(String normalText, String focusedText) {
        this.normalText = normalText;
        this.focusedText = focusedText;
        setText(isFocusOwner() ? focusedText : normalText);
    }

    public void setText(String value) {
        Font font = getFont();
        value = value.replaceAll("\\n\\r|\\r\\n|\\n|\\r", "<br>"); //NOI18N
        value = value.replace("<code>", "<code style=\"font-size: " + font.getSize() + "pt;\">"); //NOI18N
        super.setText("<html><body style=\"font-size: " + font.getSize() + "pt; font-family: " + font.getName() + ";\">" + value
                      + "</body></html>"); //NOI18N
    }

    protected void showURL(URL url) {
        actionPerformer.run();
    }
}
