/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class FilteringToolbar extends InvisibleToolbar {
        
    private final List<Component> hiddenComponents = new ArrayList();
    private final AbstractButton filterButton;

    public FilteringToolbar(String name) {
        if (!UIUtils.isNimbusLookAndFeel())
            setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        filterButton = new JToggleButton(Icons.getIcon(GeneralIcons.FILTER)) {
            protected void fireActionPerformed(ActionEvent e) {
                if (isSelected()) showFilter(); else hideFilter();
            }
        };
        filterButton.setToolTipText(name);
        add(filterButton);
    }


    protected abstract void filterChanged(String filter);


    private void showFilter() {
        filterButton.setSelected(true);

        final JTextField f = new JTextField();
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { changed(); }
            public void removeUpdate(DocumentEvent e)  { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { filterChanged(f.getText().trim()); }
        });
        f.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { if (esc(e)) hideFilter(); }
            public void keyReleased(KeyEvent e) { esc(e); }
            private boolean esc(KeyEvent e) {
                boolean esc = e.getKeyCode() == KeyEvent.VK_ESCAPE;
                if (esc) e.consume();
                return esc;
            }
        });

        for (int i = 1; i < getComponentCount(); i++)
            hiddenComponents.add(getComponent(i));

        for (Component c : hiddenComponents) remove(c);

        add(Box.createHorizontalStrut(3));
        add(f);
        f.requestFocusInWindow();

        invalidate();
        revalidate();
        doLayout();
        repaint();
    }

    private void hideFilter() {
        filterChanged(null);

        remove(2);
        remove(1);
        for (Component c : hiddenComponents) add(c);

        filterButton.setSelected(false);
        filterButton.requestFocusInWindow();

        invalidate();
        revalidate();
        doLayout();
        repaint();
    }

}
