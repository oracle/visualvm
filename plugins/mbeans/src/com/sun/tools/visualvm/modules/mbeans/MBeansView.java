/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.mbeans;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.DisplayArea;
import com.sun.tools.visualvm.core.ui.components.JExtendedSplitPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
class MBeansView extends DataSourceView {

    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/mbeans/ui/resources/mbeans.png"; // NOI18N
    private Application application;
    private DataViewComponent view;

    public MBeansView(Application application) {
        super("MBeans", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 50);
        this.application = application;
        view = createViewComponent();
    }

    @Override
    public DataViewComponent getView() {
        return view;
    }

    private DataViewComponent createViewComponent() {
        JComponent mbeansView = null;
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        if (jmx.getConnectionState() == JmxModel.ConnectionState.DISCONNECTED) {
            JTextArea textArea = new JTextArea("\n\nData not available in " +
                    "this tab because JMX connection to the JMX agent couldn't " +
                    "be established.");
            textArea.setEditable(false);
            mbeansView = textArea;
        } else {
            // MBeansTab
            MBeansTab mbeansTab = new MBeansTab(application);
            jmx.addPropertyChangeListener(mbeansTab);

            // MBeansTreeView
            MBeansTreeView mbeansTreeView = new MBeansTreeView(mbeansTab);

            // MBeansAttributesView
            MBeansAttributesView mbeansAttributesView = new MBeansAttributesView(mbeansTab);

            // MBeansOperationsView
            MBeansOperationsView mbeansOperationsView = new MBeansOperationsView(mbeansTab);

            // MBeansNotificationsView
            MBeansNotificationsView mbeansNotificationsView = new MBeansNotificationsView(mbeansTab);

            // MBeansMetadataView
            MBeansMetadataView mbeansMetadataView = new MBeansMetadataView(mbeansTab);

            DisplayArea mbeansDisplayArea = new DisplayArea();
            mbeansDisplayArea.setClosable(false);
            mbeansDisplayArea.addTab(new DisplayArea.Tab("Attributes", mbeansAttributesView));
            mbeansDisplayArea.addTab(new DisplayArea.Tab("Operations", mbeansOperationsView));
            mbeansDisplayArea.addTab(new DisplayArea.Tab("Notifications", mbeansNotificationsView));
            mbeansDisplayArea.addTab(new DisplayArea.Tab("Metadata", mbeansMetadataView));
            mbeansTab.setDisplayArea(mbeansDisplayArea);

            JExtendedSplitPane contentsSplitPane = new JExtendedSplitPane(JSplitPane.HORIZONTAL_SPLIT, mbeansTreeView, mbeansDisplayArea);
            tweakSplitPaneUI(contentsSplitPane);
            contentsSplitPane.setDividerLocation(0.3);
            contentsSplitPane.setResizeWeight(0);

            JPanel contentsPanel = new JPanel(new BorderLayout());
            contentsPanel.setOpaque(false);
            contentsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            contentsPanel.add(contentsSplitPane, BorderLayout.CENTER);
            
            mbeansView = contentsPanel;
        }
        return new DataViewComponent(
                new DataViewComponent.MasterView("MBeans", null, mbeansView),
                new DataViewComponent.MasterViewConfiguration(true));
    }

    private static void tweakSplitPaneUI(final JSplitPane splitPane) {
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        Dimension size = getSize();
                        g.setColor(getBackground());
                        g.fillRect(0, 0, size.width, size.height);
                    }
                };
            }
        });
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setDividerSize(6);

        final BasicSplitPaneDivider divider = ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        divider.setBackground(Color.WHITE);
        divider.setBorder(null);

        divider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                divider.setBackground(new Color(235, 235, 235));
                divider.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                divider.setBackground(Color.WHITE);
                divider.repaint();
            }
        });
    }
}
