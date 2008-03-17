/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package com.sun.tools.visualvm.modules.appui.welcome;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author S. Aubrecht
 */
public class StartPageContent extends JPanel implements Constants {

    public StartPageContent() {
        super( new GridBagLayout() );
        
        setBackground( Utils.getColor( COLOR_SCREEN_BACKGROUND ) );
        setMinimumSize( new Dimension(START_PAGE_MIN_WIDTH,100) );
        
        add( new JLabel(), new GridBagConstraints(0,0,1,1,0.0,1.0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0) );
        add( createMainPanel(), new GridBagConstraints(0,1,1,1,0.0,0.0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(10,10,10,10), 0,0) );
        add( new JLabel(), new GridBagConstraints(0,2,1,1,0.0,1.0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0) );
        add( new ShowNextTime(), new GridBagConstraints(0,3,1,1,0.0,0.0,
                GridBagConstraints.SOUTH,GridBagConstraints.NONE, new Insets(10,10,20,10), 0,0) );
    }
    
    private JComponent createMainPanel() {
        JPanel res = new JPanel();
        res.setOpaque(false);
        res.setLayout(new BorderLayout());
        
        res.add(new CaptionPanel(), BorderLayout.NORTH);
        res.add(new ContentsPanel(), BorderLayout.CENTER);
        res.add(new FooterPanel(), BorderLayout.SOUTH);
        
        int preferredHeight = res.getPreferredSize().height;
        res.setMinimumSize(new Dimension(462, preferredHeight));
        res.setPreferredSize(new Dimension(462, preferredHeight));
        
        return res;
    }
}
