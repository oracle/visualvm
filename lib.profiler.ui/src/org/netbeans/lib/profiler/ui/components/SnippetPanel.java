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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.plaf.ComponentUI;


public class SnippetPanel extends JPanel implements MouseListener, KeyListener, FocusListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Padding extends JPanel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Padding() {
            setBackground(Color.WHITE);
            setOpaque(true);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(lineColor);
            g.drawLine(0, 0, getWidth(), 0);
        }
    }

    private static class Title extends JComponent implements Accessible {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        String name;
        private boolean collapsed;
        private boolean rollOver;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Title(String name) {
            this.name = name;
            setUI(new TitleUI());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setRollOver(boolean rollOver) {
            if (rollOver == this.rollOver) {
                return;
            }

            this.rollOver = rollOver;
            repaint();
        }

        public void collapse() {
            collapsed = true;
            repaint();
        }

        public void expand() {
            collapsed = false;
            repaint();
        }
    }

    private static class TitleUI extends ComponentUI {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        ImageIcon collapsedIcon = new ImageIcon(TitleUI.class.getResource("collapsedSnippet.png")); //NOI18N
        ImageIcon expandedIcon = new ImageIcon(TitleUI.class.getResource("expandedSnippet.png")); //NOI18N

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Dimension getPreferredSize(JComponent c) {
            FontMetrics fm = c.getGraphics().getFontMetrics(c.getFont());

            return new Dimension(20 /* 20 is hardcoded x-offset for title string in paint(Graphics g, JComponent c)*/
                                 + fm.getStringBounds(((Title) c).name, c.getGraphics()).getBounds().width, fm.getHeight() + 4);
        }

        public void installUI(JComponent c) {
            Font f = UIManager.getFont("Label.font"); //NOI18N
            c.setFont(f.deriveFont(Font.BOLD));
        }

        public void paint(Graphics g, JComponent c) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Title title = (Title) c;
            Font font = c.getFont();

            if (title.collapsed) { // use plain font if collapsed
                g.setFont(font.deriveFont(Font.PLAIN));
            } else {
                g.setFont(font);
            }

            g.setColor(lineColor);

            FontMetrics fm = g.getFontMetrics(font);

            g.drawLine(0, 0, c.getWidth(), 0);

            if (title.collapsed) { // do not draw bottom line if collapsed

                if (title.rollOver || title.isFocusOwner()) {
                    g.setColor(focusedBackgroundColor);
                } else {
                    g.setColor(backgroundColor);
                }
            }

            g.drawLine(0, 1 + fm.getHeight() + 2, c.getWidth(), 1 + fm.getHeight() + 2);

            if (title.rollOver || title.isFocusOwner()) {
                g.setColor(focusedBackgroundColor);
            } else {
                g.setColor(backgroundColor);
            }

            g.fillRect(0, 1, c.getWidth(), fm.getHeight() + 2);

            g.setColor(textColor);
            g.drawString(title.name, 20, fm.getHeight() - 1);

            int iconX = 5;
            int iconY = 5;
            ImageIcon icon = title.collapsed ? collapsedIcon : expandedIcon;

            icon.paintIcon(c, g, iconX, iconY);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Color lineColor = new Color(214, 223, 247);
    private static final Color backgroundColor = new Color(248, 248, 248);
    private static final Color focusedBackgroundColor = new Color(230, 230, 230);
    private static final Color textColor = Color.BLACK;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JComponent content;
    private String snippetName;
    private Title title;
    private boolean collapsed = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SnippetPanel(String snippetName, JComponent content) {
        this.snippetName = snippetName;
        this.content = content;
        setLayout(new BorderLayout());
        title = new Title(snippetName) {
                public AccessibleContext getAccessibleContext() {
                    return SnippetPanel.this.getAccessibleContext();
                }
            };
        title.setFocusable(true);
        title.addKeyListener(this);
        title.addMouseListener(this);
        title.addFocusListener(this);
        // transfer the tooltip from the content to the snippet panel
        title.setToolTipText(content.getToolTipText());
        content.setToolTipText(null);
        //**
        add(title, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        getAccessibleContext().setAccessibleName(snippetName);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setCollapsed(boolean collapsed) {
        if (this.collapsed == collapsed) {
            return;
        }

        this.collapsed = collapsed;

        if (collapsed) {
            title.collapse();
        } else {
            title.expand();
        }

        content.setVisible(!collapsed);
        revalidate();
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setContent(JComponent content) {
        this.content = content;
    }

    public JComponent getContent() {
        return content;
    }

    public void setSnippetName(String snippetName) {
        this.snippetName = snippetName;
    }

    public String getSnippetName() {
        return snippetName;
    }

    public void focusGained(FocusEvent e) {
        title.repaint();
    }

    public void focusLost(FocusEvent e) {
        title.repaint();
    }

    public void keyPressed(final KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_SPACE) {
            setCollapsed(!isCollapsed());
        }
    }

    public void keyReleased(final KeyEvent evt) {
    } // not used

    public void keyTyped(final KeyEvent evt) {
    } // not used

    public void mouseClicked(MouseEvent e) {
    } // not used

    public void mouseEntered(MouseEvent e) {
        title.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        title.setRollOver(true);
    }

    public void mouseExited(MouseEvent e) {
        title.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        title.setRollOver(false);
    }

    public void mousePressed(MouseEvent e) {
        setCollapsed(!collapsed);
        requestFocus();
    }

    public void mouseReleased(MouseEvent e) {
    } // not used

    public void requestFocus() {
        if (title != null) {
            title.requestFocus();
        }
    }

    /*public static void main (String args[]) {
       try {
         UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
       } catch (ClassNotFoundException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
       } catch (InstantiationException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
       } catch (IllegalAccessException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
       } catch (UnsupportedLookAndFeelException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
       }
       JFrame jf = new JFrame ();
       jf.setSize(400, 400);
       jf.getContentPane().setLayout(new GridBagLayout());
       JPanel content = new JPanel (); content.setBackground(Color.WHITE); content.setOpaque(true);
       JPanel content2 = new JPanel (); content2.setBackground(Color.WHITE); content2.setOpaque(true);
       JPanel content3 = new JPanel (); content3.setBackground(Color.WHITE); content3.setOpaque(true);
       JPanel content4 = new JPanel (); content4.setBackground(Color.WHITE); content4.setOpaque(true);
       JPanel content5 = new JPanel (); content5.setBackground(Color.WHITE); content5.setOpaque(true);
       JPanel padding = new JPanel (); padding.setBackground(Color.WHITE); padding.setOpaque(true);
       GridBagConstraints gbc = new GridBagConstraints();
       gbc.gridwidth = GridBagConstraints.REMAINDER;
       gbc.fill = GridBagConstraints.BOTH;
       gbc.weightx = 1;
       jf.getContentPane().add (new SnippetPanel ("Controls", content), gbc);
       jf.getContentPane().add (new SnippetPanel ("Status", content2), gbc);
       jf.getContentPane().add (new SnippetPanel ("View", content3), gbc);
       jf.getContentPane().add (new SnippetPanel ("Snapshots", content4), gbc);
       jf.getContentPane().add (new SnippetPanel ("Timeline", content5), gbc);
       gbc.weighty = 1;
       jf.getContentPane().add (padding, gbc);
       jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       jf.show();
       }
     */
}
