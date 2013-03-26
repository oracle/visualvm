/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.details.jdk.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.jdk.image.FieldAccessor.InvalidFieldException;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import static org.netbeans.modules.profiler.heapwalk.details.jdk.image.ImageBuilder.LOGGER;
import static org.netbeans.modules.profiler.heapwalk.details.jdk.image.ImageBuilder.BUILDERS;

/**
 *
 * @author Jan Taus
 */
@NbBundle.Messages({
    "ImageDetailProvider_ImageDescr={0}x{1}", // NOI18N
    "ImageDetailProvider_ImageDescrColors=size={0}x{1}, {2} colors", // NOI18N
    "ImageDetailProvider_NotSupported=Unsupported image", // NOI18N
    "ImageDetailProvider_Zoom=Zoom: 1/{0}", // NOI18N
    "ImageDetailProvider_Dimension=Dimension: {0}x{1}", // NOI18N
    "ImageDetailProvider_Action_Show=Open in window", // NOI18N
    "ImageDetailProvider_Action_Export=Export image", // NOI18N
    "ImageDetailProvider_Toolbar=Export toolbar" // NOI18N
})
@ServiceProvider(service = DetailsProvider.class)
public class ImageDetailProvider extends DetailsProvider.Basic {

    private static final int CHECKER_SIZE = 8;
    private static final int PREVIEW_BORDER = 4;
    private static final Color CHECKER_BG = Color.LIGHT_GRAY;
    private static final Color CHECKER_FG = Color.DARK_GRAY;

    private static void drawChecker(Graphics g, int x, int y, int width, int height) {
        g.setColor(CHECKER_BG);
        g.fillRect(x, y, width, height);
        g.setColor(CHECKER_FG);
        for (int i = 0; i < width; i += CHECKER_SIZE) {
            for (int j = 0; j < height; j += CHECKER_SIZE) {
                if ((i / CHECKER_SIZE + j / CHECKER_SIZE) % 2 == 0) {
                    g.fillRect(x + i, y + j, Math.min(CHECKER_SIZE, width - i), Math.min(CHECKER_SIZE, height - j));
                }
            }
        }
    }

    public ImageDetailProvider() {
        super(ImageBuilder.BUILDERS.getMasks(Image.class, String.class));
    }

    @Override
    public String getDetailsString(String className, Instance instance, Heap heap) {
        try {
            InstanceBuilder<? extends String> builder = BUILDERS.getBuilder(instance, String.class);
            if (builder == null) {
                LOGGER.log(Level.FINE, "Unable to get String builder for %s", className); //NOI18N
            } else {
                return builder.convert(new FieldAccessor(heap, BUILDERS), instance);
            }
        } catch (InvalidFieldException ex) {
            LOGGER.log(Level.FINE, "Unable to get text for instance", ex.getMessage()); //NOI18N
        }
        return null;
    }

    @Override
    public View getDetailsView(String className, Instance instance, Heap heap) {
        return new ImageView(instance, heap);
    }

    private static class ImageView extends DetailsProvider.View implements Scrollable {

        private final String instanceName;
        private final int instanceNumber;
        private Image instanceImage = null;

        private JLabel paintLabel;


        public ImageView(Instance instance, Heap heap) {
            super(instance, heap);
            this.instanceName = instance.getJavaClass().getName();
            this.instanceNumber = instance.getInstanceNumber();
            addMouseListener(new MouseHandler());
        }

        @Override
        protected void computeView(Instance instance, Heap heap) {
            FieldAccessor fa = new FieldAccessor(heap, BUILDERS);
            Image image = null;
            JLabel label = null;
            try {
                image = ImageBuilder.buildImageInternal(instance, heap);
            } catch (InvalidFieldException ex) {
                LOGGER.log(Level.FINE, "Unable to get text for instance", ex.getMessage());
                label = new JLabel(Bundle.ImageDetailProvider_NotSupported(), JLabel.CENTER);
                label.setEnabled(false);
            }

            final JComponent component = label;
            final Image im = image;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    removeAll();
                    if (component != null) {
                        add(component, BorderLayout.CENTER);
                    }
                    revalidate();
                    doLayout();
                    repaint();
                    instanceImage = im;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (instanceImage == null) {
                return;
            }

            int lineHeight = g.getFontMetrics().getHeight();
            int lineAscent = g.getFontMetrics().getAscent();

            int viewWidth = getWidth() - 2 * PREVIEW_BORDER;
            int viewHeight = getHeight() - 3 * PREVIEW_BORDER - 2 * lineHeight;
            if (viewWidth < 1 || viewHeight < 1) {
                return;
            }

            int imgWidth = instanceImage.getWidth(null);
            int imgHeight = instanceImage.getHeight(null);
            if (imgWidth < 1 || imgHeight < 1) {
                return;
            }

            int width = imgWidth;
            int height = imgHeight;
            int scale = 1;
            int scaleX = (int) Math.ceil((float) imgWidth / viewWidth);
            int scaleY = (int) Math.ceil((float) imgHeight / viewHeight);
            if (scaleX > 1 || scaleY > 1) {
                scale = Math.max(scaleX, scaleY);
                width = (int) ((float) imgWidth / scale);
                height = (int) ((float) imgHeight / scale);
            }
            int x = PREVIEW_BORDER + (viewWidth - width) / 2;
            int y = PREVIEW_BORDER + (viewHeight - height) / 2;

            drawChecker(g, x, y, width, height);
            g.drawImage(instanceImage, x, y, x + width, y + height, 0, 0, imgWidth, imgHeight, null);

            g.setColor(getForeground());
            int nextY = getHeight() - drawText(g, PREVIEW_BORDER, getHeight(), Bundle.ImageDetailProvider_Dimension(imgWidth, imgHeight));
            if (scale != 1) {
                drawText(g, PREVIEW_BORDER, nextY, Bundle.ImageDetailProvider_Zoom(scale));
            }
        }

        private int drawText(Graphics g, int x, int y, String text) {
            if(paintLabel == null) {
                paintLabel = new JLabel();
            }
            paintLabel.setFont(g.getFont());
            paintLabel.setText(text);
            paintLabel.setSize(paintLabel.getPreferredSize());
            g.translate(x, y - paintLabel.getHeight());
            paintLabel.paint(g);
            g.translate(-x, paintLabel.getHeight() - y);
            return paintLabel.getHeight();
        }

        private class MouseHandler extends MouseAdapter {

            public void mousePressed(final MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    openNewWindow();
                }
            }
        }

        private void showPopup(MouseEvent e) {
            if (instanceImage == null) {
                return;
            }
            JMenuItem showItem = new JMenuItem(Bundle.ImageDetailProvider_Action_Show()) {
                protected void fireActionPerformed(ActionEvent e) {
                    openNewWindow();
                }
            };
            showItem.setFont(showItem.getFont().deriveFont(Font.BOLD));
            JPopupMenu popup = new JPopupMenu();
            popup.add(showItem);
            popup.add(new ImageExportAction(instanceImage));
            popup.show(e.getComponent(), e.getX(), e.getY());
        }

        private void openNewWindow() {
            if (instanceImage == null) {
                return;
            }
            ImageTopComponent itc = new ImageTopComponent(instanceImage, instanceName, instanceNumber);
            itc.open();
            itc.requestActive();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return null;
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 50;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return true;
        }
    }

    private static class ImageTopComponent extends ProfilerTopComponent {

        private static final String HELP_CTX_KEY = "HeapWalker.ImagePreview.HelpCtx"; // NOI18N
        private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

        ImageTopComponent(Image image, String className, int instanceNumber) {
            setName(BrowserUtils.getSimpleType(className) + "#" + instanceNumber);
            setToolTipText("Preview of " + className + "#" + instanceNumber);
            setLayout(new BorderLayout());

            int width = image.getWidth(null);
            int height = image.getHeight(null);
            BufferedImage displayedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics g = displayedImage.createGraphics();
            drawChecker(g, 0, 0, width, height);
            g.drawImage(image, 0, 0, null);

            JComponent c = new JScrollPane(new JLabel(new ImageIcon(displayedImage)));
            add(c, BorderLayout.CENTER);


            JToolBar toolBar = new JToolBar();
            toolBar.putClientProperty("JToolBar.isRollover", Boolean.TRUE); //NOI18N
            toolBar.setFloatable(false);
            toolBar.setName(Bundle.ImageDetailProvider_Toolbar());

            //JButton button = new JButton();
            //button.setText("");
            toolBar.add(new ImageExportAction(image));
            add(toolBar, BorderLayout.NORTH);
        }

        @Override
        public int getPersistenceType() {
            return TopComponent.PERSISTENCE_NEVER;
        }

        @Override
        protected String preferredID() {
            return this.getClass().getName();
        }

        @Override
        public HelpCtx getHelpCtx() {
            return HELP_CTX;
        }
    }
}
