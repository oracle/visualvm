/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.oql.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;
import org.netbeans.modules.profiler.spi.OQLEditorImpl;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Lookup;

/**
 *
 * @author Jaroslav Bachorik
 */
public class OQLEditor extends JPanel {

    final public static String VALIDITY_PROPERTY = OQLEditorImpl.VALIDITY_PROPERTY;
    volatile private boolean lexervalid = false;
    volatile private boolean parserValid = false;
    volatile private boolean oldValidity = false;
    private JEditorPane queryEditor = null;
    final private OQLEngine engine;

    final private Color disabledBgColor = UIUtils.isGTKLookAndFeel() ?
                  UIManager.getLookAndFeel().getDefaults().getColor("desktop") : // NOI18N
                  UIManager.getColor("TextField.disabledBackground"); // NOI18N

    final private Caret nullCaret = new Caret() {

        public void install(JTextComponent c) {
            //
        }

        public void deinstall(JTextComponent c) {
            //
        }

        public void paint(Graphics g) {
            //
        }

        public void addChangeListener(ChangeListener l) {
            //
        }

        public void removeChangeListener(ChangeListener l) {
            //
        }

        public boolean isVisible() {
            return false;
        }

        public void setVisible(boolean v) {
            //
        }

        public boolean isSelectionVisible() {
            return false;
        }

        public void setSelectionVisible(boolean v) {
            //
        }

        public void setMagicCaretPosition(Point p) {
            //
        }

        public Point getMagicCaretPosition() {
            return new Point(0, 0);
        }

        public void setBlinkRate(int rate) {
            //
        }

        public int getBlinkRate() {
            return 1;
        }

        public int getDot() {
            return 0;
        }

        public int getMark() {
            return 0;
        }

        public void setDot(int dot) {
            //
        }

        public void moveDot(int dot) {
            //
        }
    };

    private Color lastBgColor = null;
    private Caret lastCaret = null;

    public OQLEditor(OQLEngine engine) {
        this.engine = engine;
        init();
    }

    private void init() {
        OQLEditorImpl impl = Lookup.getDefault().lookup(OQLEditorImpl.class);
        if (impl != null) {
            queryEditor = impl.getEditorPane();
            queryEditor.getDocument().putProperty(OQLEngine.class, engine);
            queryEditor.getDocument().putProperty(OQLEditorImpl.ValidationCallback.class, new OQLEditorImpl.ValidationCallback() {

                public void callback(boolean lexingResult) {
                    lexervalid = lexingResult;
                    validateScript();
                }
            });
        } else {
            queryEditor = new JEditorPane("text/x-oql", ""); // NOI18N
            queryEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));
            lexervalid = true; // no lexer info available; assume the lexing info is valid
        }

        queryEditor.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                validateScript();
            }

            public void removeUpdate(DocumentEvent e) {
                validateScript();
            }

            public void changedUpdate(DocumentEvent e) {
                validateScript();
            }
        });

        queryEditor.setOpaque(isOpaque());
        queryEditor.setBackground(getBackground());

        setLayout(new BorderLayout());
        add(queryEditor, BorderLayout.CENTER);
    }

    public void setScript(String script) {
        queryEditor.setText(script);
    }

    public String getScript() {
        return queryEditor.getText();
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (queryEditor != null) {
            queryEditor.setBackground(bg);
        }
    }

    @Override
    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);
        if (queryEditor != null) {
            queryEditor.setOpaque(isOpaque);
        }
    }

    @Override
    public void requestFocus() {
        queryEditor.requestFocus();
    }

    final private void validateScript() {
        if (lexervalid || !parserValid) {
            // only parse the query if there are no errors from lexer
            try {
                engine.parseQuery(getScript());
                parserValid = true;
            } catch (OQLException e) {
                StatusDisplayer.getDefault().setStatusText(e.getLocalizedMessage());
                parserValid = false;
            }
        }

        firePropertyChange(VALIDITY_PROPERTY, oldValidity, lexervalid && parserValid);
        oldValidity = lexervalid && parserValid;
    }

    public void setEditable(boolean b) {
        if (queryEditor.isEditable() == b) return;
        
        queryEditor.setEditable(b);

        if (b) {
            if (lastBgColor != null) {
                queryEditor.setBackground(lastBgColor);
            }
            if (lastCaret != null) {
                queryEditor.setCaret(lastCaret);
            }
        } else {
            lastBgColor = queryEditor.getBackground();
            lastCaret = queryEditor.getCaret();
            queryEditor.setBackground(disabledBgColor);
            queryEditor.setCaret(nullCaret);
        }
    }

    public boolean isEditable() {
        return queryEditor.isEditable();
    }
}


