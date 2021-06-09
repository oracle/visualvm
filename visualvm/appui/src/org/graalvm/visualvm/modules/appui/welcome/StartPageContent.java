/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.modules.appui.welcome;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.uisupport.UISupport;

/**
 *
 * @author S. Aubrecht
 */
public class StartPageContent extends JPanel implements Constants {

    public StartPageContent() {
        super( new GridBagLayout() );
        
        setBackground( !UISupport.isDarkResultsBackground() ?
                       Utils.getColor( COLOR_SCREEN_BACKGROUND ) :
                       Utils.getColor( COLOR_SCREEN_BACKGROUND_DARK ) );
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
        
        return res;
    }
}
