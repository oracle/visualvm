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

package com.sun.tools.visualvm.modules.appui.welcome;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author S. Aubrecht
 */
public abstract class LinkButton extends JButton
        implements Constants, MouseListener, ActionListener, FocusListener {

    private boolean underline = false;

    public LinkButton( String label ) {
        super( label );
        setForeground( Utils.getColor(LINK_COLOR) );
        setFont( BUTTON_FONT );
        setBorder( new EmptyBorder(1, 1, 1, 1) );
        setCursor( Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) );
        setHorizontalAlignment( JLabel.LEFT );
        addMouseListener(this);
        setFocusable( true );

        setMargin( new Insets(0, 0, 0, 0) );
        setBorderPainted( false );
        setFocusPainted( false );
        setRolloverEnabled( true );
        setContentAreaFilled( false );

        addActionListener( this );
        addFocusListener( this );
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        if( isEnabled() ) {
            underline = true;
            setForeground( Utils.getColor(LINK_IN_FOCUS_COLOR) );
            repaint();
            onMouseEntered( e );
            setForeground( Utils.getColor( MOUSE_OVER_LINK_COLOR  )  );
        }
    }

    public void mouseExited(MouseEvent e) {
        if( isEnabled() ) {
            underline = false;
            setForeground( Utils.getColor(isVisited() ? VISITED_LINK_COLOR : LINK_COLOR) );
            repaint();
            onMouseExited( e );
        }
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Utils.prepareGraphics( g );
        super.paintComponent(g2);

        Dimension size = getSize();
        if( hasFocus() && isEnabled() ) {
            g2.setStroke( LINK_IN_FOCUS_STROKE );
            g2.setColor( Utils.getColor(LINK_IN_FOCUS_COLOR) );
            g2.drawRect( 0, 0, size.width - 1, size.height - 1 );
        }
    }
    
    public void focusLost(FocusEvent e) {
    }

    public void focusGained(FocusEvent e) {
        Rectangle rect = getBounds();
        rect.grow( 0, FONT_SIZE );
        scrollRectToVisible( rect );
    }

    protected void onMouseExited(MouseEvent e) {
    }

    protected void onMouseEntered(MouseEvent e) {
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if( underline && isEnabled() ) {
            Font f = getFont();
            FontMetrics fm = getFontMetrics(f);
            int iconWidth = 0;
            if( null != getIcon() ) {
                iconWidth = getIcon().getIconWidth()+getIconTextGap();
            }
            int x1 = iconWidth;
            int y1 = fm.getHeight();
            int x2 = fm.stringWidth(getText()) + iconWidth;
            if( getText().length() > 0 )
                g.drawLine(x1, y1, x2, y1);
        }
    }
    
    protected boolean isVisited() {
        return false;
    }
}
