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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.profiler.heapwalk.oql.OQLEngine;
import org.netbeans.modules.profiler.heapwalk.oql.OQLException;
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

    public OQLEditor(OQLEngine engine) {
        this.engine = engine;
        init();
    }

    private void init() {
        OQLEditorImpl impl = Lookup.getDefault().lookup(OQLEditorImpl.class);
        if (impl != null) {
            queryEditor = impl.getEditorPane();
            impl.addPropertyChangeListener(VALIDITY_PROPERTY, new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent evt) {
                    // capturing the lexer state
                    lexervalid = (Boolean) evt.getNewValue();
                }
            });
//            queryEditor.getDocument().putProperty(OQLEngine.class, engine); // commented out; not necessary now when there is no code-completion
        } else {
            queryEditor = new JEditorPane("text/x-oql", ""); // NOI18N
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
}
