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
import org.netbeans.lib.profiler.ui.UIUtils;


public class SnippetPanel extends JPanel implements MouseListener, KeyListener, FocusListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Padding extends JPanel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Padding() {
            setBackground(UIUtils.getProfilerResultsBackground());
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

        private final int TITLE_X_OFFSET = 5;
        private final int TITLE_Y_OFFSET = 2;

        private final ImageIcon collapsedIcon = new ImageIcon(TitleUI.class.getResource("collapsedSnippet.png")); //NOI18N
        private final ImageIcon expandedIcon = new ImageIcon(TitleUI.class.getResource("expandedSnippet.png")); //NOI18N
        private final JLabel plainPainter = new JLabel();
        private final JLabel boldPainter = new JLabel();
        private final Font plainFont = plainPainter.getFont().deriveFont(Font.PLAIN);
        private final Font boldFont = boldPainter.getFont().deriveFont(Font.BOLD);
        private Dimension preferredSize;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Dimension getPreferredSize(JComponent c) {
            return preferredSize;
        }

        public void installUI(JComponent c) {
            plainPainter.setText(((Title)c).name);
            plainPainter.setIcon(collapsedIcon);
            plainPainter.setFont(plainFont);
            plainPainter.setIconTextGap(5);
            boldPainter.setText(((Title)c).name);
            boldPainter.setIcon(expandedIcon);
            boldPainter.setFont(boldFont);
            boldPainter.setIconTextGap(5);

            plainPainter.setSize(plainPainter.getPreferredSize());
            Dimension titlePreferredSize = boldPainter.getPreferredSize();
            boldPainter.setSize(titlePreferredSize);
            preferredSize = new Dimension(TITLE_X_OFFSET + titlePreferredSize.width,
                                          titlePreferredSize.height + TITLE_Y_OFFSET * 2);
        }

        public void paint(Graphics g, JComponent c) {

            Title title = (Title)c;

            g.setColor(lineColor);
            g.drawLine(0, 0, c.getWidth(), 0);

            if (title.collapsed) { // do not draw bottom line if collapsed

                if (title.rollOver || title.isFocusOwner()) {
                    g.setColor(focusedBackgroundColor);
                } else {
                    g.setColor(backgroundColor);
                }
            }

            g.drawLine(0, 1 + plainPainter.getHeight() + TITLE_Y_OFFSET,
                       c.getWidth(), 1 + plainPainter.getHeight() + TITLE_Y_OFFSET);

            if (title.rollOver || title.isFocusOwner()) {
                g.setColor(focusedBackgroundColor);
            } else {
                g.setColor(backgroundColor);
            }

            g.fillRect(0, 1, c.getWidth(), plainPainter.getHeight() + TITLE_Y_OFFSET);

            g.translate(TITLE_X_OFFSET, TITLE_Y_OFFSET);
            if (title.collapsed) {
                plainPainter.paint(g);
            } else {
                boldPainter.paint(g);
            }
            g.translate(-TITLE_X_OFFSET, -TITLE_Y_OFFSET);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static Color lineColor;
    private static Color backgroundColor;
    private static Color focusedBackgroundColor;
    
    static { initColors(); }

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
    
    private static void initColors() {
        Color systemBackgroundColor = UIUtils.getProfilerResultsBackground();
        
        int backgroundRed = systemBackgroundColor.getRed(); 
        int backgroundGreen = systemBackgroundColor.getGreen();
        int backgroundBlue = systemBackgroundColor.getBlue();
        boolean inverseColors = backgroundRed < 41 || backgroundGreen < 32 || backgroundBlue < 25;

        if (inverseColors) {
            lineColor = UIUtils.getSafeColor(backgroundRed + 41, backgroundGreen + 32, backgroundBlue + 8);
            backgroundColor = UIUtils.getSafeColor(backgroundRed + 7, backgroundGreen + 7, backgroundBlue + 7);
            focusedBackgroundColor = UIUtils.getSafeColor(backgroundRed + 25, backgroundGreen + 25, backgroundBlue + 25);
        } else {
            lineColor = UIUtils.getSafeColor(backgroundRed - 41 /*214*/, backgroundGreen - 32 /*223*/, backgroundBlue - 8 /*247*/);
            backgroundColor = UIUtils.getSafeColor(backgroundRed - 7 /*248*/, backgroundGreen - 7 /*248*/, backgroundBlue - 7 /*248*/);
            focusedBackgroundColor = UIUtils.getSafeColor(backgroundRed - 25 /*230*/, backgroundGreen - 25 /*230*/, backgroundBlue - 25 /*230*/);
        }
    }

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
