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
package org.netbeans.modules.profiler.v2.ui;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import org.netbeans.lib.profiler.ui.components.JExtendedSpinner;
import org.netbeans.lib.profiler.ui.swing.PopupButton;

/**
 *
 * @author Jiri Sedlacek
 */
public class SettingsPanel extends JPanel {
    
    public SettingsPanel() {
        super(null);
        
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        setOpaque(false);
        
        add(Box.createVerticalStrut(defaultHeight()));
    }
    
    
    public void removeAll() {
        super.removeAll();
        add(Box.createVerticalStrut(defaultHeight()));
    }
    
    
    private static int DEFAULT_HEIGHT = -1;
    
    private static int defaultHeight() {
        if (DEFAULT_HEIGHT == -1) {
            JPanel ref = new JPanel(null);
            ref.setLayout(new BoxLayout(ref, BoxLayout.LINE_AXIS));
            ref.setOpaque(false);
            
            ref.add(new JLabel("XXX")); // NOI18N
            
            ref.add(new JButton("XXX")); // NOI18N
            ref.add(new PopupButton("XXX")); // NOI18N
            
            ref.add(new JCheckBox("XXX")); // NOI18N
            ref.add(new JRadioButton("XXX")); // NOI18N
            
            ref.add(new JTextField("XXX")); // NOI18N
            
            ref.add(new JExtendedSpinner(new SpinnerNumberModel(1, 1, 655535, 1)));
            
            Component separator = Box.createHorizontalStrut(1);
            Dimension d = separator.getMaximumSize(); d.height = 20;
            separator.setMaximumSize(d);
            ref.add(separator);
            
            DEFAULT_HEIGHT = ref.getPreferredSize().height;
        }
        return DEFAULT_HEIGHT;
    }
    
}
