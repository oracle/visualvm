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

package org.netbeans.modules.profiler;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.EqualFlowLayout;
import org.netbeans.lib.profiler.ui.components.FlatToolBar;
import org.netbeans.lib.profiler.ui.components.SnippetPanel;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.actions.*;
import org.netbeans.modules.profiler.heapwalk.HeapWalkerManager;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.SharedClassObject;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.basic.BasicComboBoxUI;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;


/**
 * The main control panel window for profiling functionality, docked into explorer by default.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class ProfilerControlPanel2 extends TopComponent implements ProfilingStateListener, SnapshotsListener,
                                                                         ResultsListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static final class VerticalLayout implements LayoutManager {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void addLayoutComponent(final String name, final Component comp) {
        }

        public void layoutContainer(final Container parent) {
            final Insets insets = parent.getInsets();
            final int posX = insets.left;
            int posY = insets.top;
            final int width = parent.getWidth() - insets.left - insets.right;

            final Component[] comps = parent.getComponents();

            for (int i = 0; i < comps.length; i++) {
                final Component comp = comps[i];

                if (comp.isVisible()) {
                    int height = comp.getPreferredSize().height;

                    if (i == (comps.length - 1)) // last component
                     {
                        if ((posY + height) < (parent.getHeight() - insets.bottom)) {
                            height = parent.getHeight() - insets.bottom - posY;
                        }
                    }

                    comp.setBounds(posX, posY, width, height);
                    posY += height;
                }
            }
        }

        public Dimension minimumLayoutSize(final Container parent) {
            final Dimension d = new Dimension(parent.getInsets().left + parent.getInsets().right,
                                              parent.getInsets().top + parent.getInsets().bottom);
            int maxWidth = 0;
            int height = 0;
            final Component[] comps = parent.getComponents();

            for (int i = 0; i < comps.length; i++) {
                final Component comp = comps[i];

                if (comp.isVisible()) {
                    final Dimension size = comp.getMinimumSize();
                    maxWidth = Math.max(maxWidth, size.width);
                    height += size.height;
                }
            }

            d.width += maxWidth;
            d.height += height;

            return d;
        }

        public Dimension preferredLayoutSize(final Container parent) {
            final Dimension d = new Dimension(parent.getInsets().left + parent.getInsets().right,
                                              parent.getInsets().top + parent.getInsets().bottom);
            int maxWidth = 0;
            int height = 0;
            final Component[] comps = parent.getComponents();

            for (int i = 0; i < comps.length; i++) {
                final Component comp = comps[i];

                if (comp.isVisible()) {
                    final Dimension size = comp.getPreferredSize();
                    maxWidth = Math.max(maxWidth, size.width);
                    height += size.height;
                }
            }

            d.width += maxWidth;
            d.height += height;

            return d;
        }

        public void removeLayoutComponent(final Component comp) {
        }
    }

    public static final class WhiteFilter extends RGBImageFilter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final float[] hsv = new float[3];

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Constructs a GrayFilter object that filters a color image to a
         * grayscale image. Used by buttons to create disabled ("grayed out")
         * button images.
         */
        public WhiteFilter() {
            // canFilterIndexColorModel indicates whether or not it is acceptable
            // to apply the color filtering of the filterRGB method to the color
            // table entries of an IndexColorModel object in lieu of pixel by pixel
            // filtering.
            canFilterIndexColorModel = true;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * Creates a disabled image
         */
        public static Image createDisabledImage(final Image i) {
            final WhiteFilter filter = new WhiteFilter();
            final ImageProducer prod = new FilteredImageSource(i.getSource(), filter);

            return Toolkit.getDefaultToolkit().createImage(prod);
        }

        /**
         * Overrides <code>RGBImageFilter.filterRGB</code>.
         */
        public int filterRGB(final int x, final int y, final int rgb) {
            int transparency = (rgb >> 24) & 0xFF;

            if (transparency <= 1) {
                return rgb; // do not alter fully transparent pixels (those would end up being black)
            }

            transparency /= 2; // set transparency to 50% of original
            Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, (rgb >> 0) & 0xFF, hsv);
            hsv[1] = 0;

            return Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) + (transparency << 24);
        }
    }

    private static final class BasicTelemetryPanel extends CPPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JLabel instrFilterValueLabel;
        private final JLabel instrValueLabel;
        private final JLabel relTimeValueLabel;
        private final JLabel threadsValueLabel;
        private final JLabel totalMemValueLabel;
        private final JLabel typeValueLabel;
        private final JLabel usedMemValueLabel;
        private final NumberFormat intFormat;
        private final NumberFormat percentFormat;
        private String savedInstrFilterText = ""; // NOI18N
        private String savedInstrText = ""; // NOI18N
        private boolean inactive = true;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        BasicTelemetryPanel() {
            percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMinimumFractionDigits(1);
            percentFormat.setMaximumIntegerDigits(2);

            intFormat = NumberFormat.getIntegerInstance();
            intFormat.setGroupingUsed(true);

            setBorder(BorderFactory.createEmptyBorder(8, 3, 9, 3));
            setLayout(new GridBagLayout());

            final JLabel instrLabel = new JLabel(INSTRUMENTED_LABEL_STRING);
            instrLabel.setFont(instrLabel.getFont().deriveFont(Font.BOLD));

            final JLabel instrFilterLabel = new JLabel(FILTER_LABEL_STRING);
            instrFilterLabel.setFont(instrFilterLabel.getFont().deriveFont(Font.BOLD));

            final JLabel threadsLabel = new JLabel(THREADS_LABEL_STRING);
            threadsLabel.setFont(threadsLabel.getFont().deriveFont(Font.BOLD));

            final JLabel typeLabel = new JLabel(TYPE_LABEL_STRING);
            typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD));

            final JLabel totalMemLabel = new JLabel(TOTAL_MEMORY_LABEL_STRING);
            totalMemLabel.setFont(totalMemLabel.getFont().deriveFont(Font.BOLD));

            final JLabel usedMemLabel = new JLabel(USED_MEMORY_LABEL_STRING);
            usedMemLabel.setFont(usedMemLabel.getFont().deriveFont(Font.BOLD));

            final JLabel relTimeLabel = new JLabel(GC_TIME_LABEL_STRING);
            relTimeLabel.setFont(relTimeLabel.getFont().deriveFont(Font.BOLD));

            instrValueLabel = new JLabel(savedInstrText);
            instrFilterValueLabel = new JLabel(""); // NOI18N
            threadsValueLabel = new JLabel(""); // NOI18N
            typeValueLabel = new JLabel(""); // NOI18N
            totalMemValueLabel = new JLabel(""); // NOI18N
            usedMemValueLabel = new JLabel(""); // NOI18N
            relTimeValueLabel = new JLabel(""); // NOI18N

            final GridBagConstraints labelGbc = new GridBagConstraints();
            labelGbc.anchor = GridBagConstraints.WEST;
            labelGbc.insets = new Insets(0, 10, 0, 10);

            final GridBagConstraints valueGbc = new GridBagConstraints();
            valueGbc.anchor = GridBagConstraints.WEST;
            valueGbc.gridwidth = GridBagConstraints.REMAINDER;
            valueGbc.insets = new Insets(0, 0, 0, 0);
            valueGbc.fill = GridBagConstraints.HORIZONTAL;
            valueGbc.weightx = 1;

            add(instrLabel, labelGbc);
            add(instrValueLabel, valueGbc);
            add(instrFilterLabel, labelGbc);
            add(instrFilterValueLabel, valueGbc);
            add(threadsLabel, labelGbc);
            add(threadsValueLabel, valueGbc);
            add(totalMemLabel, labelGbc);
            add(totalMemValueLabel, valueGbc);
            add(usedMemLabel, labelGbc);
            add(usedMemValueLabel, valueGbc);
            add(relTimeLabel, labelGbc);
            add(relTimeValueLabel, valueGbc);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        void refreshStatus() {
            final int state = Profiler.getDefault().getProfilingState();

            final TargetAppRunner targetAppRunner = TargetAppRunner.getDefault();

            String instrStatusText = ""; // NOI18N

            if (state != Profiler.PROFILING_INACTIVE) {
                final int currentInstrType = targetAppRunner.getProfilingSessionStatus().currentInstrType;

                switch (currentInstrType) {
                    case CommonConstants.INSTR_CODE_REGION:
                        instrStatusText = MessageFormat.format(NO_LINES_CODE_REGION_MSG,
                                                               new Object[] {
                                                                   new Integer(targetAppRunner.getProfilingSessionStatus().instrEndLine
                                                                               - targetAppRunner.getProfilingSessionStatus().instrStartLine)
                                                               });

                        break;
                    case CommonConstants.INSTR_RECURSIVE_FULL:
                    case CommonConstants.INSTR_RECURSIVE_SAMPLED:

                        int nMethods = targetAppRunner.getProfilingSessionStatus().getNInstrMethods();

                        if (nMethods > 0) {
                            nMethods--; // Because nInstrMethods is actually the array size where element 0 is always empty
                        }

                        instrStatusText = MessageFormat.format(NO_METHODS_MSG, new Object[] { new Integer(nMethods) });

                        break;
                    case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
                    case CommonConstants.INSTR_OBJECT_LIVENESS:

                        final int nClasses = targetAppRunner.getProfilingSessionStatus().getNInstrClasses();
                        instrStatusText = MessageFormat.format(NO_CLASSES_MSG, new Object[] { new Integer(nClasses) });
                        ;

                        break;
                    case CommonConstants.INSTR_NONE:
                        instrStatusText = NOTHING_INSTRUMENTED_MSG;

                        break;
                }
            }

            if ((savedInstrText == null) || !savedInstrText.equals(instrStatusText)) {
                savedInstrText = instrStatusText;
                instrValueLabel.setText(savedInstrText);
            }

            String filterText = ""; // NOI18N

            switch (state) {
                case Profiler.PROFILING_INACTIVE:

                    if (inactive) {
                        return;
                    }

                    inactive = true;
                    threadsValueLabel.setText(""); // NOI18N
                    totalMemValueLabel.setText(""); // NOI18N
                    usedMemValueLabel.setText(""); // NOI18N
                    relTimeValueLabel.setText(""); // NOI18N
                    typeValueLabel.setText(""); // NOI18N

                    break;
                default:
                    inactive = false;

                    final MonitoredData data = Profiler.getDefault().getVMTelemetryManager().getLastData();

                    if (data != null) {
                        threadsValueLabel.setText("" + intFormat.format(data.getNUserThreads() + data.getNSystemThreads())); // NOI18N
                        totalMemValueLabel.setText("" + intFormat.format(data.getTotalMemory()) + " B"); // NOI18N
                        usedMemValueLabel.setText("" + intFormat.format(data.getTotalMemory() - data.getFreeMemory()) + " B"); // NOI18N
                        relTimeValueLabel.setText("" + percentFormat.format(data.getRelativeGCTimeInPerMil() / 1000f) // NOI18N
                        ); // (the percents are multiplied by 10 to preserve single decimal digit
                        typeValueLabel.setText(""); // NOI18N
                        filterText = Profiler.getDefault().getLastProfilingSettings().getSelectedInstrumentationFilter().toString();
                    } else {
                        threadsValueLabel.setText(""); // NOI18N
                        totalMemValueLabel.setText(""); // NOI18N
                        usedMemValueLabel.setText(""); // NOI18N
                        relTimeValueLabel.setText(""); // NOI18N
                        typeValueLabel.setText(""); // NOI18N
                    }

                    break;
            }

            if ((savedInstrFilterText == null) || !savedInstrFilterText.equals(filterText)) {
                savedInstrFilterText = filterText;
                instrFilterValueLabel.setText(savedInstrFilterText);
            }
        }
    }

    private static class CPPanel extends JPanel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        CPPanel() {
            setOpaque(true);
            setBackground(CP_BACKGROUND_COLOR);
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        }
    }

    private static final class ControlsPanel extends CPPanel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ControlsPanel() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            final Border myRolloverBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY,
                                                                                                        Color.LIGHT_GRAY),
                                                               new FlatToolBar.FlatMarginBorder());

            JButton rerunButton = new JButton(SystemAction.get(RerunAction.class));
            rerunButton.setText(null);
            UIUtils.fixButtonUI(rerunButton);
            rerunButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) rerunButton
                                                                                                          .getIcon()).getImage()))));
            rerunButton.setContentAreaFilled(false);
            rerunButton.setMargin(new Insets(3, 3, 3, 3));
            rerunButton.setRolloverEnabled(true);
            rerunButton.setBorder(myRolloverBorder);
            add(rerunButton);

            JButton stopButton = new JButton(new StopAction()) {
                public void setText(String text) {}
                public void setIcon(Icon icon) {
                    super.setIcon(icon);
                    setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon)getIcon()).getImage()))));
                }
            };
            UIUtils.fixButtonUI(stopButton);
            stopButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) stopButton
                                                                                                          .getIcon()).getImage()))));
            stopButton.setContentAreaFilled(false);
            stopButton.setMargin(new Insets(3, 3, 3, 3));
            stopButton.setRolloverEnabled(true);
            stopButton.setBorder(myRolloverBorder);
            add(stopButton);

            JButton resetButton = new JButton(new ResetResultsAction());
            resetButton.setText(null);
            UIUtils.fixButtonUI(resetButton);
            resetButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) resetButton
                                                                                                          .getIcon()).getImage()))));
            resetButton.setContentAreaFilled(false);
            resetButton.setMargin(new Insets(3, 3, 3, 3));
            resetButton.setRolloverEnabled(true);
            resetButton.setBorder(myRolloverBorder);
            add(resetButton);

            JButton rungcButton = new JButton(SystemAction.get(RunGCAction.class));
            rungcButton.setText(null);
            UIUtils.fixButtonUI(rungcButton);
            rungcButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) rungcButton
                                                                                                          .getIcon()).getImage()))));
            rungcButton.setContentAreaFilled(false);
            rungcButton.setMargin(new Insets(3, 3, 3, 3));
            rungcButton.setRolloverEnabled(true);
            rungcButton.setBorder(myRolloverBorder);
            add(rungcButton);

            JButton modifyButton = new JButton(SystemAction.get(ModifyProfilingAction.class));
            modifyButton.setText(null);
            UIUtils.fixButtonUI(modifyButton);
            modifyButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) modifyButton
                                                                                                           .getIcon()).getImage()))));
            modifyButton.setContentAreaFilled(false);
            modifyButton.setMargin(new Insets(3, 3, 3, 3));
            modifyButton.setRolloverEnabled(true);
            modifyButton.setBorder(myRolloverBorder);
            add(modifyButton);

            JButton telemetryButton = new JButton(new TelemetryOverviewAction());
            telemetryButton.setText(null);
            UIUtils.fixButtonUI(telemetryButton);
            telemetryButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) telemetryButton
                                                                                                              .getIcon()).getImage()))));
            telemetryButton.setContentAreaFilled(false);
            telemetryButton.setMargin(new Insets(3, 3, 3, 3));
            telemetryButton.setRolloverEnabled(true);
            telemetryButton.setBorder(myRolloverBorder);
            add(telemetryButton);
        }
    }

    private static final class ProjectNameRenderer extends DefaultListCellRenderer {
        //~ Inner Classes --------------------------------------------------------------------------------------------------------

        private class Renderer extends DefaultListCellRenderer {
            //~ Methods ----------------------------------------------------------------------------------------------------------

            public void setFont(Font font) {
            }

            public void setFontEx(Font font) {
                super.setFont(font);
            }
        }

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Renderer renderer = new Renderer();
        private boolean firstFontSet = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel rendererOrig = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            renderer.setComponentOrientation(rendererOrig.getComponentOrientation());
            renderer.setFontEx(rendererOrig.getFont());
            renderer.setOpaque(rendererOrig.isOpaque());
            renderer.setForeground(rendererOrig.getForeground());
            renderer.setBackground(rendererOrig.getBackground());
            renderer.setEnabled(rendererOrig.isEnabled());
            renderer.setBorder(rendererOrig.getBorder());

            if ((value != null) && value instanceof Project) {
                ProjectInformation pi = ProjectUtils.getInformation((Project) value);
                renderer.setText(pi.getDisplayName());
                renderer.setIcon(pi.getIcon());

                if (ProjectUtilities.getMainProject() == value) {
                    renderer.setFontEx(renderer.getFont().deriveFont(Font.BOLD)); // bold for main project
                } else {
                    renderer.setFontEx(renderer.getFont().deriveFont(Font.PLAIN));
                }
            } else {
                renderer.setText(rendererOrig.getText());
                renderer.setIcon(emptyIcon);
            }

            return renderer;
        }
    }

    private static final class ResultsSnippetPanel extends CPPanel implements ActionListener {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final int CPU = 1;
        private static final int MEMORY = 2;
        private static final int FRAGMENT = 3;
        private static final ImageIcon TAKE_SNAPSHOT_CPU_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/takeSnapshotCPU.png", false);
        private static final ImageIcon TAKE_SNAPSHOT_MEMORY_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/takeSnapshotMem.png", false);
        private static final ImageIcon TAKE_SNAPSHOT_FRAGMENT_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/takeSnapshotFragment.png", false);
        private static final ImageIcon LIVE_RESULTS_CPU_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/liveResultsCPUView.png", false);
        private static final ImageIcon LIVE_RESULTS_MEMORY_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/liveResultsMemView.png", false);
        private static final ImageIcon LIVE_RESULTS_FRAGMENT_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/liveResultsFragmentView.png", false);

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JButton resetResultsButton;
        private final JButton takeCPUSnapshotButton;
        private final JButton takeFragmentSnapshotButton;
        private final JButton takeMemorySnapshotButton;
        private final JPanel centerPanel;
        private JButton liveResultsButton;
        private LoadedSnapshot lastSnapshot;
        private int displayedIcon = CPU;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ResultsSnippetPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(7, 10, 12, 10));

            final Border myRolloverBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY,
                                                                                                        Color.LIGHT_GRAY),
                                                               new FlatToolBar.FlatMarginBorder());

            // Take CPU snapshot
            takeCPUSnapshotButton = new JButton(TAKE_SNAPSHOT_BUTTON_NAME, TAKE_SNAPSHOT_CPU_ICON);
            UIUtils.fixButtonUI(takeCPUSnapshotButton);
            takeCPUSnapshotButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) takeCPUSnapshotButton
                                                                                                                    .getIcon())
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .getImage()))));
            takeCPUSnapshotButton.addActionListener(this);
            takeCPUSnapshotButton.setContentAreaFilled(false);
            takeCPUSnapshotButton.setMargin(new Insets(3, 3, 3, 3));
            takeCPUSnapshotButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            takeCPUSnapshotButton.setHorizontalTextPosition(SwingConstants.CENTER);
            takeCPUSnapshotButton.setRolloverEnabled(true);
            takeCPUSnapshotButton.setBorder(myRolloverBorder);
            takeCPUSnapshotButton.setToolTipText(TAKE_SNAPSHOT_BUTTON_TOOLTIP);

            // Take Memory snapshot
            takeMemorySnapshotButton = new org.netbeans.lib.profiler.ui.components.PopupButton(new String[] {
                                                                                                   TAKE_SNAPSHOT_BUTTON_NAME,
                                                                                                   DUMP_HEAP_BUTTON_NAME
                                                                                               },
                                                                                               new ImageIcon[] {
                                                                                                   TAKE_SNAPSHOT_MEMORY_ICON
                                                                                               });
            UIUtils.fixButtonUI(takeMemorySnapshotButton);
            takeMemorySnapshotButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) takeMemorySnapshotButton
                                                                                                                       .getIcon())
                                                                                                                      .getImage()))));
            takeMemorySnapshotButton.addActionListener(this);
            takeMemorySnapshotButton.setContentAreaFilled(false);
            takeMemorySnapshotButton.setMargin(new Insets(3, 3, 3, 3));
            takeMemorySnapshotButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            takeMemorySnapshotButton.setHorizontalTextPosition(SwingConstants.CENTER);
            takeMemorySnapshotButton.setRolloverEnabled(true);
            takeMemorySnapshotButton.setBorder(myRolloverBorder);
            takeMemorySnapshotButton.setToolTipText(TAKE_SNAPSHOT_BUTTON_TOOLTIP);

            // Take Fragment snapshot
            takeFragmentSnapshotButton = new JButton(TAKE_SNAPSHOT_BUTTON_NAME, TAKE_SNAPSHOT_FRAGMENT_ICON);
            UIUtils.fixButtonUI(takeFragmentSnapshotButton);
            takeFragmentSnapshotButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) takeFragmentSnapshotButton
                                                                                                                         .getIcon())
                                                                                                                        .getImage()))));
            takeFragmentSnapshotButton.addActionListener(this);
            takeFragmentSnapshotButton.setContentAreaFilled(false);
            takeFragmentSnapshotButton.setMargin(new Insets(3, 3, 3, 3));
            takeFragmentSnapshotButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            takeFragmentSnapshotButton.setHorizontalTextPosition(SwingConstants.CENTER);
            takeFragmentSnapshotButton.setRolloverEnabled(true);
            takeFragmentSnapshotButton.setBorder(myRolloverBorder);
            takeFragmentSnapshotButton.setToolTipText(TAKE_SNAPSHOT_BUTTON_TOOLTIP);

            liveResultsButton = new JButton(LIVE_RESULTS_BUTTON_NAME, LIVE_RESULTS_CPU_ICON);
            UIUtils.fixButtonUI(liveResultsButton);
            liveResultsButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) liveResultsButton
                                                                                                                .getIcon())
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .getImage()))));
            liveResultsButton.addActionListener(this);
            liveResultsButton.setContentAreaFilled(false);
            liveResultsButton.setMargin(new Insets(3, 3, 3, 3));
            liveResultsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            liveResultsButton.setHorizontalTextPosition(SwingConstants.CENTER);
            liveResultsButton.setRolloverEnabled(true);
            liveResultsButton.setBorder(myRolloverBorder);
            liveResultsButton.setToolTipText(LIVE_RESULTS_BUTTON_TOOLTIP);

            displayedIcon = CPU;

            resetResultsButton = new JButton(new ResetResultsAction()) {
                public void setIcon(Icon icon) {
                    super.setIcon(icon);
                    setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon)getIcon()).getImage()))));
                }
            };
            UIUtils.fixButtonUI(resetResultsButton);
            resetResultsButton.setText(RESET_RESULTS_BUTTON_NAME);
            resetResultsButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) resetResultsButton
                                                                                                          .getIcon()).getImage()))));
            
            resetResultsButton.setMargin(new Insets(3, 8, 3, 8));
            resetResultsButton.setContentAreaFilled(false);
            resetResultsButton.setVerticalTextPosition(SwingConstants.CENTER);
            resetResultsButton.setHorizontalTextPosition(SwingConstants.RIGHT);
            resetResultsButton.setRolloverEnabled(true);
            resetResultsButton.setBorder(myRolloverBorder);
            resetResultsButton.setToolTipText(RESET_RESULTS_BUTTON_TOOLTIP);

            final JPanel southPanel = new JPanel();
            centerPanel = new JPanel();
            southPanel.setOpaque(false);
            southPanel.setLayout(new BorderLayout());
            southPanel.add(resetResultsButton, BorderLayout.WEST);
            centerPanel.setOpaque(false);
            centerPanel.setLayout(new EqualFlowLayout(EqualFlowLayout.LEFT));
            centerPanel.add(takeCPUSnapshotButton);
            centerPanel.add(liveResultsButton);

            add(centerPanel, BorderLayout.CENTER);
            add(southPanel, BorderLayout.SOUTH);

            refreshStatus();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(final ActionEvent e) {
            if ((e.getSource() == takeCPUSnapshotButton) || (e.getSource() == takeFragmentSnapshotButton)) {
                IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                        public void run() {
                            ResultsManager.getDefault().takeSnapshot();
                        }
                    });
            } else if (e.getSource() == takeMemorySnapshotButton) {
                if (TAKE_SNAPSHOT_BUTTON_NAME.equals(e.getActionCommand())) {
                    IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                            public void run() {
                                ResultsManager.getDefault().takeSnapshot();
                            }
                        });
                } else {
                    IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                            public void run() {
                                SharedClassObject.findObject(HeapDumpAction.class, true).dumpToProject();
                            }
                        });
                }
            } else if (e.getSource() == liveResultsButton) {
                LiveResultsWindow.getDefault().open();
                LiveResultsWindow.getDefault().requestActive();
                LiveResultsWindow.getDefault().refreshLiveResults();
            }
        }

        public void refreshStatus() {
            updateResultsButtons();
            
            int instr = Profiler.getDefault().getTargetAppRunner().getProfilerClient().getCurrentInstrType();
            int newMode;

            switch (instr) {
                case CommonConstants.INSTR_CODE_REGION:
                    newMode = FRAGMENT;

                    break;
                case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
                case CommonConstants.INSTR_OBJECT_LIVENESS:
                    newMode = MEMORY;

                    break;
                case CommonConstants.INSTR_RECURSIVE_FULL:
                case CommonConstants.INSTR_RECURSIVE_SAMPLED:default:
                    newMode = CPU;

                    break;
            }

            if (displayedIcon != newMode) {
                displayedIcon = newMode;
                centerPanel.remove(centerPanel.getComponent(0));

                if (newMode == CPU) {
                    //          takeSnapshotButton.setIcon(TAKE_SNAPSHOT_CPU_ICON);
                    centerPanel.add(takeCPUSnapshotButton, 0);
                    liveResultsButton.setIcon(LIVE_RESULTS_CPU_ICON);
                } else if (newMode == MEMORY) {
                    //          takeSnapshotButton.setIcon(TAKE_SNAPSHOT_MEMORY_ICON);
                    centerPanel.add(takeMemorySnapshotButton, 0);
                    liveResultsButton.setIcon(LIVE_RESULTS_MEMORY_ICON);
                } else {
                    //          takeSnapshotButton.setIcon(TAKE_SNAPSHOT_FRAGMENT_ICON);
                    centerPanel.add(takeFragmentSnapshotButton, 0);
                    liveResultsButton.setIcon(LIVE_RESULTS_FRAGMENT_ICON);
                }

                //        takeSnapshotButton.setDisabledIcon(
                //            new IconUIResource(
                //                new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) takeSnapshotButton.getIcon()).getImage()))
                //            )
                //        );
                liveResultsButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) liveResultsButton
                                                                                                                    .getIcon())
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          .getImage()))));
            }
        }

        public void resultsAvailable() {
            updateResultsButtons();
        }

        public void resultsReset() {
            updateResultsButtons();
        }
        
        private void updateResultsButtons() {
            int state = Profiler.getDefault().getProfilingState();
            int instr = Profiler.getDefault().getTargetAppRunner().getProfilerClient().getCurrentInstrType();
            boolean enabled = ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING));
            enabled = enabled && (instr != CommonConstants.INSTR_NONE); // no live results & snapshots for monitoring
            
            liveResultsButton.setEnabled(enabled);
            
            boolean dataAvailable = ResultsManager.getDefault().resultsAvailable();
            takeCPUSnapshotButton.setEnabled(enabled && dataAvailable);
            takeMemorySnapshotButton.setEnabled(enabled && dataAvailable);
            takeFragmentSnapshotButton.setEnabled(enabled && dataAvailable);
        }

        public void snapshotRemoved(LoadedSnapshot snapshot) {
            if (lastSnapshot == snapshot) {
                lastSnapshot = null;
            }
        }

        public void snapshotTaken(LoadedSnapshot ls) {
            lastSnapshot = ls;

            if (ProfilerIDESettings.getInstance().getAutoOpenSnapshot()) {
                int sortingColumn = LiveResultsWindow.hasDefault() ? LiveResultsWindow.getDefault().getSortingColumn()
                                                                   : CommonConstants.SORTING_COLUMN_DEFAULT;
                boolean sortingOrder = LiveResultsWindow.hasDefault() ? LiveResultsWindow.getDefault().getSortingOrder() : false;
                ResultsManager.getDefault().openSnapshot(ls, sortingColumn, sortingOrder);

                //ResultsManager.getDefault().openSnapshot(ls);
            }

            if (ProfilerIDESettings.getInstance().getAutoSaveSnapshot()) {
                ResultsManager.getDefault().saveSnapshot(ls);
            }
        }
    }

    private static final class SnapshotsPanel extends CPPanel implements ListSelectionListener, ActionListener,
                                                                         PropertyChangeListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private DefaultListModel listModel;
        private JButton deleteButton;
        private JButton exportButton;
        private JButton loadButton;
        private JButton openButton;
        private JComboBox combo;
        private JList list;
        private Project displayedProject;
        private boolean internalChange = false; // for combo box selection

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        SnapshotsPanel() {
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            setLayout(new BorderLayout(6, 6));

            final Border buttonsBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY,
                                                                                                     Color.LIGHT_GRAY, Color.GRAY),
                                                            new FlatToolBar.FlatMarginBorder());

            final Border comboBorder = new LineBorder(Color.LIGHT_GRAY) {
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    Color oldColor = g.getColor();
                    g.setColor(lineColor);
                    g.drawLine(x, y, x, height - 1);
                    g.setColor(oldColor);
                }
            };

            combo = new JComboBox(new DefaultComboBoxModel());
            combo.setRenderer(new ProjectNameRenderer());
            combo.getAccessibleContext().setAccessibleName(COMBO_ACCESS_NAME);
            combo.getAccessibleContext().setAccessibleDescription(COMBO_ACCESS_DESCR);

            ComboBoxUI ui = combo.getUI();

            if (ui instanceof BasicComboBoxUI) {
                BasicComboBoxUI bui = (BasicComboBoxUI) ui;
                JButton b = null;

                try {
                    Field f = BasicComboBoxUI.class.getDeclaredField("arrowButton"); // NOI18N
                    f.setAccessible(true);
                    b = (JButton) f.get(bui);
                } catch (NoSuchFieldException e) {
                    // ignore, not a big deal, probably custom L&F we cannot do anything about
                } catch (IllegalAccessException e) {
                    // ignore, not a big deal, probably custom L&F we cannot do anything about
                }

                if (b != null) {
                    b.setBackground(CP_BACKGROUND_COLOR);
                    b.setBorder(BorderFactory.createCompoundBorder(comboBorder,
                                                                   BorderFactory.createEmptyBorder(0, b.getIconTextGap(), 0,
                                                                                                   b.getIconTextGap())));
                }
            }

            combo.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            combo.setBackground(CP_BACKGROUND_COLOR);

            combo.addActionListener(this);
            OpenProjects.getDefault().addPropertyChangeListener(this);

            list = new JList(listModel = new DefaultListModel());
            list.getAccessibleContext().setAccessibleName(LIST_ACCESS_NAME);
            list.setVisibleRowCount(8);
            list.setCellRenderer(new DefaultListCellRenderer() {
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                                  boolean cellHasFocus) {
                        JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                        // Colors
                        if (isSelected && list.isEnabled()) {
                            setForeground(list.isFocusOwner() ? list.getSelectionForeground()
                                                              : UIUtils.getUnfocusedSelectionForeground());
                            setBackground(list.isFocusOwner() ? list.getSelectionBackground()
                                                              : UIUtils.getUnfocusedSelectionBackground());
                        } else if (!list.isEnabled()) {
                            setForeground(UIManager.getColor("TextField.inactiveForeground")); // NOI18N
                            setBackground(UIManager.getColor("TextField.inactiveBackground")); // NOI18N
                        } else {
                            setForeground(list.getForeground());
                            setBackground(list.getBackground());
                        }

                        // FileObject
                        FileObject fo = (FileObject) value;

                        if (fo.getExt().equalsIgnoreCase(ResultsManager.HEAPDUMP_EXTENSION)) {
                            // Heap Dump
                            c.setIcon(memoryIcon);

                            String fileName = fo.getName();

                            if (HeapWalkerManager.getDefault().isHeapWalkerOpened(FileUtil.toFile(fo))) {
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                            }

                            if (fileName.startsWith("heapdump-")) { // NOI18N

                                String time = fileName.substring("heapdump-".length(), fileName.length()); // NOI18N

                                try {
                                    long timeStamp = Long.parseLong(time);
                                    c.setText(MessageFormat.format(HEAP_SNAPSHOT_DISPLAYNAME,
                                                                   new Object[] { StringUtils.formatUserDate(new Date(timeStamp)) }));
                                } catch (NumberFormatException e) {
                                    // file name is probably customized
                                    c.setText(MessageFormat.format(HEAP_SNAPSHOT_DISPLAYNAME, new Object[] { fileName }));
                                }
                            } else {
                                c.setText(MessageFormat.format(HEAP_SNAPSHOT_DISPLAYNAME, new Object[] { fileName }));
                            }
                        } else {
                            // Profiler snapshot
                            LoadedSnapshot ls = ResultsManager.getDefault().findLoadedSnapshot(FileUtil.toFile(fo));

                            if (ls != null) {
                                ResultsSnapshot rs = ls.getSnapshot();
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                                c.setText(StringUtils.formatUserDate(new Date(rs.getTimeTaken())));

                                switch (ls.getType()) {
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                                        c.setIcon(cpuIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                                        c.setIcon(fragmentIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                                        c.setIcon(memoryIcon);

                                        break;
                                }
                            } else {
                                String fileName = fo.getName();

                                if (fileName.startsWith("snapshot-")) { // NOI18N

                                    String time = fileName.substring("snapshot-".length(), fileName.length()); // NOI18N

                                    try {
                                        long timeStamp = Long.parseLong(time);
                                        c.setText(StringUtils.formatUserDate(new Date(timeStamp)));
                                    } catch (NumberFormatException e) {
                                        // file name is probably customized
                                        c.setText(fileName);
                                    }
                                } else {
                                    c.setText(fileName);
                                }

                                int type = ResultsManager.getDefault().getSnapshotType(fo);

                                switch (type) {
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                                        c.setIcon(cpuIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                                        c.setIcon(fragmentIcon);

                                        break;
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                                        c.setIcon(memoryIcon);

                                        break;
                                }
                            }
                        }

                        return c;
                    }
                });

            list.addListSelectionListener(this);
            list.addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            //ResultsManager.getDefault().openSnapshots(loadSelectedSnapshots());
                            openSelectedSnapshots();
                        } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                            final FileObject[] selectedSnapshotFiles = getSelectedSnapshotFiles();

                            if (ProfilerDialogs.notify(new NotifyDescriptor.Confirmation( //"Do you really want to delete selected snapshot"
                                                                                              //+ ((selectedSnapshotFiles.length > 1) ? "s" : "")
                                                                                              //+ " from disk?\nYou will not be able to undo this operation.",
                                CONFIRM_DELETE_SNAPSHOT_MSG, CONFIRM_DELETE_SNAPSHOT_CAPTION, NotifyDescriptor.YES_NO_OPTION)) == NotifyDescriptor.YES_OPTION) {
                                RequestProcessor.getDefault().post(new Runnable() {
                                        public void run() {
                                            deleteSnapshots(selectedSnapshotFiles);
                                            refreshList();
                                        }
                                    });
                            }
                        }
                    }
                });

            list.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                            //ResultsManager.getDefault().openSnapshots(loadSelectedSnapshots());
                            openSelectedSnapshots();
                        }
                    }
                });

            add(combo, BorderLayout.NORTH);

            JScrollPane listScroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            listScroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            listScroll.setPreferredSize(new Dimension(1, listScroll.getPreferredSize().height));
            add(listScroll, BorderLayout.CENTER);

            openButton = new JButton(OPEN_BUTTON_NAME);
            UIUtils.fixButtonUI(openButton);
            openButton.setContentAreaFilled(false);
            openButton.setMargin(new Insets(3, 3, 3, 3));
            openButton.setRolloverEnabled(true);
            openButton.setBorder(buttonsBorder);
            openButton.addActionListener(this);
            openButton.getAccessibleContext().setAccessibleDescription(OPEN_BUTTON_ACCESS_DESCR);

            deleteButton = new JButton(DELETE_BUTTON_NAME);
            UIUtils.fixButtonUI(deleteButton);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setMargin(new Insets(3, 3, 3, 3));
            deleteButton.setRolloverEnabled(true);
            deleteButton.setBorder(buttonsBorder);
            deleteButton.addActionListener(this);
            deleteButton.getAccessibleContext().setAccessibleDescription(DELETE_BUTTON_ACCESS_DESCR);

            loadButton = new JButton(LOAD_BUTTON_NAME);
            UIUtils.fixButtonUI(loadButton);
            loadButton.setContentAreaFilled(false);
            loadButton.setMargin(new Insets(3, 3, 3, 3));
            loadButton.setRolloverEnabled(true);
            loadButton.setBorder(buttonsBorder);
            loadButton.addActionListener(this);
            loadButton.getAccessibleContext().setAccessibleDescription(LOAD_BUTTON_ACCESS_DESCR);

            exportButton = new JButton(EXPORT_BUTTON_NAME);
            UIUtils.fixButtonUI(exportButton);
            exportButton.setContentAreaFilled(false);
            exportButton.setMargin(new Insets(3, 3, 3, 3));
            exportButton.setRolloverEnabled(true);
            exportButton.setBorder(buttonsBorder);
            exportButton.addActionListener(this);
            exportButton.getAccessibleContext().setAccessibleDescription(EXPORT_BUTTON_ACCESS_DESCR);

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(0, 0, 4, 0);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            buttonsPanel.add(openButton, gbc);
            buttonsPanel.add(deleteButton, gbc);
            buttonsPanel.add(exportButton, gbc);
            buttonsPanel.add(loadButton, gbc);
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;

            JPanel padding = new JPanel();
            padding.setOpaque(false);
            buttonsPanel.add(padding, gbc);

            add(buttonsPanel, BorderLayout.EAST);

            updateButtons();
            updateCombo();
            setDisplayedProject(ProjectUtilities.getMainProject());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setDisplayedProject(Project project) {
            displayedProject = project;
            list.clearSelection(); // clear selection in the list before refreshing it
            refreshList();
            internalChange = true;

            if (project == null) {
                combo.setSelectedItem(GLOBAL_COMBO_ITEM_STRING);
            } else {
                combo.setSelectedItem(project);
            }

            internalChange = false;
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == openButton) {
                //ResultsManager.getDefault().openSnapshots(loadSelectedSnapshots());
                openSelectedSnapshots();
            } else if (e.getSource() == deleteButton) {
                final FileObject[] selectedSnapshotFiles = getSelectedSnapshotFiles();

                if (ProfilerDialogs.notify(new NotifyDescriptor.Confirmation( //"Do you really want to delete selected snapshot"
                                                                                  //+ ((selectedSnapshotFiles.length > 1) ? "s" : "")
                                                                                  //+ " from disk?\nYou will not be able to undo this operation.",
                    CONFIRM_DELETE_SNAPSHOT_MSG, CONFIRM_DELETE_SNAPSHOT_CAPTION, NotifyDescriptor.YES_NO_OPTION)) == NotifyDescriptor.YES_OPTION) {
                    RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                deleteSnapshots(selectedSnapshotFiles);
                                refreshList();
                            }
                        });
                }
            } else if (e.getSource() == loadButton) {
                new LoadSnapshotAction().loadSnapshotOrHeapdump();
            } else if (e.getSource() == exportButton) {
                ResultsManager.getDefault().exportSnapshots(getSelectedSnapshotFiles());
            } else if (e.getSource() == combo) {
                if (!internalChange) {
                    Object o = combo.getSelectedItem();

                    if (o instanceof Project) {
                        setDisplayedProject((Project) o);
                    } else {
                        setDisplayedProject(null);
                    }
                }
            }
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if ((evt.getPropertyName() != null) && evt.getPropertyName().equals(OpenProjects.PROPERTY_OPEN_PROJECTS)) {
                updateCombo();
            }
        }

        public void snapshotLoaded(LoadedSnapshot snapshot) {
            refreshList();
        }

        public void snapshotRemoved(LoadedSnapshot snapshot) {
            SnapshotResultsWindow.closeWindow(snapshot);
            refreshList();
        }

        public void snapshotSaved(LoadedSnapshot ls) {
            // reflect name change in opened tab name (if any)
            if (SnapshotResultsWindow.hasSnapshotWindow(ls)) {
                SnapshotResultsWindow.get(ls).updateTitle();
            }

            refreshList();

            // make sure the new item is visible in the list
            FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(ls.getFile()));

            if (fo != null) {
                int idx = listModel.indexOf(fo);

                if (idx != -1) {
                    list.ensureIndexIsVisible(idx);
                }
            }
        }

        public void valueChanged(ListSelectionEvent e) {
            updateButtons();
        }

        private FileObject[] getSelectedSnapshotFiles() {
            Object[] sel = list.getSelectedValues();
            FileObject[] ret = new FileObject[sel.length];

            for (int i = 0; i < sel.length; i++) {
                ret[i] = (FileObject) sel[i];
            }

            return ret;
        }

        private void deleteSnapshots(FileObject[] selectedSnapshots) {
            for (int i = 0; i < selectedSnapshots.length; i++) {
                FileObject selectedSnapshotFile = selectedSnapshots[i];
                String selectedSnapshotFileExt = selectedSnapshotFile.getExt();

                if (selectedSnapshotFileExt.equalsIgnoreCase(ResultsManager.SNAPSHOT_EXTENSION)) {
                    ResultsManager.getDefault().deleteSnapshot(selectedSnapshots[i]);
                } else if (selectedSnapshotFileExt.equalsIgnoreCase(ResultsManager.HEAPDUMP_EXTENSION)) {
                    HeapWalkerManager.getDefault().deleteHeapDump(FileUtil.toFile(selectedSnapshotFile));
                }
            }
        }

        private LoadedSnapshot[] loadSelectedSnapshots() {
            FileObject[] sel = getSelectedSnapshotFiles();

            return ResultsManager.getDefault().loadSnapshots(sel);
        }

        private void openSelectedSnapshots() {
            FileObject[] selectedSnapshotFiles = getSelectedSnapshotFiles();

            for (int i = 0; i < selectedSnapshotFiles.length; i++) {
                final FileObject selectedSnapshotFile = selectedSnapshotFiles[i];
                String selectedSnapshotFileExt = selectedSnapshotFile.getExt();
                SnapshotResultsWindow srw = null;

                if (selectedSnapshotFileExt.equalsIgnoreCase(ResultsManager.SNAPSHOT_EXTENSION)) {
                    LoadedSnapshot ls = ResultsManager.getDefault().loadSnapshot(selectedSnapshotFile);

                    if (ls != null) {
                        srw = SnapshotResultsWindow.get(ls);
                        srw.open();
                    }
                } else if (selectedSnapshotFileExt.equalsIgnoreCase(ResultsManager.HEAPDUMP_EXTENSION)) {
                    RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                try {
                                    HeapWalkerManager.getDefault().openHeapWalker(FileUtil.toFile(selectedSnapshotFile));
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                }

                if (srw != null) {
                    srw.requestActive();
                }
            }
        }

        private void refreshList() {
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        int selIdx = list.getSelectedIndex();
                        FileObject[] snapshotsOnDisk = ResultsManager.getDefault().listSavedSnapshots(displayedProject);
                        listModel.removeAllElements();

                        for (int i = 0; i < snapshotsOnDisk.length; i++) {
                            listModel.addElement(snapshotsOnDisk[i]);
                        }

                        FileObject[] heapdumpsOnDisk = ResultsManager.getDefault().listSavedHeapdumps(displayedProject);

                        for (int i = 0; i < heapdumpsOnDisk.length; i++) {
                            listModel.addElement(heapdumpsOnDisk[i]);
                        }

                        if (selIdx != -1) { // keep selected index, if there was selection previously and there are remaining items

                            if (selIdx >= listModel.size()) {
                                selIdx = listModel.size() - 1;
                            }

                            if (selIdx != -1) {
                                list.setSelectedIndex(selIdx);
                            }
                        }
                    }
                });
        }

        private void updateButtons() {
            openButton.setEnabled(list.getSelectedIndices().length > 0);
            deleteButton.setEnabled(list.getSelectedIndices().length > 0);
            exportButton.setEnabled(list.getSelectedIndices().length > 0);
        }

        private void updateCombo() {
            Project[] projects = OpenProjects.getDefault().getOpenProjects();
            Vector items = new Vector(projects.length + 1);

            for (int i = 0; i < projects.length; i++) {
                items.add(projects[i]);
            }

            try {
                Collections.sort(items,
                                 new Comparator() {
                        public int compare(Object o1, Object o2) {
                            Project p1 = (Project) o1;
                            Project p2 = (Project) o2;

                            return ProjectUtils.getInformation(p1).getDisplayName().toLowerCase()
                                               .compareTo(ProjectUtils.getInformation(p2).getDisplayName().toLowerCase());
                        }
                    });
            } catch (Exception e) {
                ErrorManager.getDefault().notify(ErrorManager.ERROR, e); // just in case ProjectUtils doesn't provide expected information
            }

            ;

            items.add(0, GLOBAL_COMBO_ITEM_STRING);

            DefaultComboBoxModel comboModel = (DefaultComboBoxModel) combo.getModel();
            comboModel.removeAllElements();

            for (int i = 0; i < items.size(); i++) {
                comboModel.addElement(items.get(i));
            }

            if ((displayedProject != null) && (comboModel.getIndexOf(displayedProject) != -1)) {
                internalChange = true;
                combo.setSelectedItem(displayedProject);
                internalChange = false;
            } else {
                Project mainProject = ProjectUtilities.getMainProject();
                setDisplayedProject(mainProject);
            }
        }
    }

    private static final class StatusPanel extends CPPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JLabel modeValueLabel;
        private final JLabel onLabel;
        private final JLabel onValueLabel;
        private final JLabel profileValueLabel;
        private final JLabel statusValueLabel;
        private final JLabel typeValueLabel;
        private String configuration = null;
        private String host = null;
        private int count = 0;
        private int mode = -1;
        private int state = -1;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        StatusPanel() {
            setBorder(BorderFactory.createEmptyBorder(8, 3, 9, 3));
            setLayout(new GridBagLayout());

            final JLabel modeLabel = new JLabel(MODE_LABEL_STRING);
            modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));

            final JLabel typeLabel = new JLabel(TYPE_LABEL_STRING);
            typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD));

            final JLabel profileLabel = new JLabel(CONFIG_LABEL_STRING);
            profileLabel.setFont(profileLabel.getFont().deriveFont(Font.BOLD));

            onLabel = new JLabel(ON_LABEL_STRING);
            onLabel.setFont(onLabel.getFont().deriveFont(Font.BOLD));

            final JLabel statusLabel = new JLabel(STATUS_LABEL_STRING);
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));

            modeValueLabel = new JLabel(NO_CONFIGURATION_STRING);
            typeValueLabel = new JLabel(NO_CONFIGURATION_STRING);
            profileValueLabel = new JLabel(NO_CONFIGURATION_STRING);
            onValueLabel = new JLabel(NO_CONFIGURATION_STRING);
            statusValueLabel = new JLabel(INACTIVE_LABEL_STRING);

            final GridBagConstraints labelGbc = new GridBagConstraints();
            labelGbc.anchor = GridBagConstraints.WEST;
            labelGbc.insets = new Insets(0, 10, 0, 10);

            final GridBagConstraints valueGbc = new GridBagConstraints();
            valueGbc.anchor = GridBagConstraints.WEST;
            valueGbc.gridwidth = GridBagConstraints.REMAINDER;
            valueGbc.insets = new Insets(0, 0, 0, 0);
            valueGbc.fill = GridBagConstraints.HORIZONTAL;
            valueGbc.weightx = 1;

            add(typeLabel, labelGbc);
            add(typeValueLabel, valueGbc);
            add(profileLabel, labelGbc);
            add(profileValueLabel, valueGbc);
            add(onLabel, labelGbc);
            add(onValueLabel, valueGbc);
            add(statusLabel, labelGbc);
            add(statusValueLabel, valueGbc);

            onLabel.setVisible(false);
            onValueLabel.setVisible(false);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        void refreshStatus() {
            if (mode != Profiler.getDefault().getProfilingMode()) {
                mode = Profiler.getDefault().getProfilingMode();

                if (mode == Profiler.MODE_ATTACH) {
                    modeValueLabel.setText(ATTACH_LABEL_STRING);
                } else {
                    modeValueLabel.setText(PROFILE_LABEL_STRING);
                }
            }

            final ProfilingSettings ps = Profiler.getDefault().getLastProfilingSettings();

            if ((ps != null) && ((configuration == null) || !configuration.equals(ps.getSettingsName()))) {
                switch (ps.getProfilingType()) {
                    case ProfilingSettings.PROFILE_CPU_STOPWATCH:
                        typeValueLabel.setText(CODE_FRAGMENT_LABEL_STRING);

                        break;
                    case ProfilingSettings.PROFILE_CPU_ENTIRE:
                    case ProfilingSettings.PROFILE_CPU_PART:
                        typeValueLabel.setText(CPU_LABEL_STRING);

                        break;
                    case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                    case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                        typeValueLabel.setText(MEMORY_LABEL_STRING);

                        break;
                    case ProfilingSettings.PROFILE_MONITOR:
                        typeValueLabel.setText(MONITOR_LABEL_STRING);

                        break;
                }

                configuration = ps.getSettingsName();
                profileValueLabel.setText(configuration);
            }

            String newHost = TargetAppRunner.getDefault().getProfilerEngineSettings().getRemoteHost();

            if (newHost == null) {
                newHost = ""; // NOI18N
            }

            if ((host == null) || !host.equals(newHost)) {
                host = newHost;

                if ("".equals(host)) { // NOI18N
                    onValueLabel.setText(""); // NOI18N
                    onLabel.setVisible(false);
                    onValueLabel.setVisible(false);
                } else {
                    onValueLabel.setText(host);
                    onLabel.setVisible(true);
                    onValueLabel.setVisible(true);
                }
            }

            final int newState = Profiler.getDefault().getProfilingState();

            if (state != newState) {
                state = newState;

                switch (state) {
                    case Profiler.PROFILING_INACTIVE:
                        statusValueLabel.setText(INACTIVE_LABEL_STRING);

                        break;
                    case Profiler.PROFILING_STARTED:
                        statusValueLabel.setText(STARTED_LABEL_STRING);
                        count = 1;

                        break;
                    case Profiler.PROFILING_PAUSED:
                        statusValueLabel.setText(PAUSED_LABEL_STRING);

                        break;
                    case Profiler.PROFILING_RUNNING:
                        statusValueLabel.setText(RUNNING_LABEL_STRING);

                        break;
                    case Profiler.PROFILING_STOPPED:
                        statusValueLabel.setText(STOPPED_LABEL_STRING);

                        break;
                }
            } else {
                // state stays the same: started -> show progress by adding dots
                if (state == Profiler.PROFILING_STARTED) {
                    String text = STARTING_LABEL_STRING;

                    for (int i = 0; i < count; i++) {
                        text += "."; // NOI18N
                    }

                    statusValueLabel.setText(text);
                    count++;

                    if (count == 5) {
                        count = 0;
                    }
                }
            }
        }
    }

    private static final class ViewPanel extends CPPanel implements ActionListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JButton threadsButton;
        private final JButton vmTelemetryButton;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ViewPanel() {
            setLayout(new EqualFlowLayout(FlowLayout.LEFT));
            setBorder(BorderFactory.createEmptyBorder(7, 10, 12, 10));

            final Border myRolloverBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY,
                                                                                                        Color.LIGHT_GRAY),
                                                               new FlatToolBar.FlatMarginBorder());

            vmTelemetryButton = new JButton(TELEMETRY_BUTTON_NAME, ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/vmTelemetryView.png", false));
            UIUtils.fixButtonUI(vmTelemetryButton);
            vmTelemetryButton.addActionListener(this);
            vmTelemetryButton.setContentAreaFilled(false);
            vmTelemetryButton.setMargin(new Insets(3, 3, 3, 3));
            vmTelemetryButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            vmTelemetryButton.setHorizontalTextPosition(SwingConstants.CENTER);
            vmTelemetryButton.setRolloverEnabled(true);
            vmTelemetryButton.setBorder(myRolloverBorder);
            vmTelemetryButton.setToolTipText(TELEMETRY_BUTTON_TOOLTIP);

            threadsButton = new JButton(THREADS_BUTTON_NAME, ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/threadsView.png", false));
            UIUtils.fixButtonUI(threadsButton);
            threadsButton.addActionListener(this);
            threadsButton.setContentAreaFilled(false);
            threadsButton.setMargin(new Insets(3, 3, 3, 3));
            threadsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            threadsButton.setHorizontalTextPosition(SwingConstants.CENTER);
            threadsButton.setRolloverEnabled(true);
            threadsButton.setBorder(myRolloverBorder);
            threadsButton.setToolTipText(THREADS_BUTTON_TOOLTIP);

            add(vmTelemetryButton);
            add(threadsButton);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(final ActionEvent e) {
            if (e.getSource() == vmTelemetryButton) {
                new ShowTelemetryViewAction().actionPerformed(null);
            } else if (e.getSource() == threadsButton) {
                ThreadsWindow.getDefault().showThreads();
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CONTROL_PANEL_NAME = NbBundle.getMessage(ProfilerControlPanel2.class, "LAB_ControlPanelName");
    private static final String CONTROLS_SNIPPET_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                            "ProfilerControlPanel2_ControlsSnippetName"); // NOI18N
    private static final String STATUS_SNIPPET_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                          "ProfilerControlPanel2_StatusSnippetName"); // NOI18N
    private static final String RESULTS_SNIPPET_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                           "ProfilerControlPanel2_ResultsSnippetName"); // NOI18N
    private static final String SNAPSHOTS_SNIPPET_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                             "ProfilerControlPanel2_SnapshotsSnippetName"); // NOI18N
    private static final String VIEW_SNIPPET_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                        "ProfilerControlPanel2_ViewSnippetName"); // NOI18N
    private static final String TELEMETRY_SNIPPET_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                             "ProfilerControlPanel2_TelemetrySnippetName"); // NOI18N
    private static final String MODE_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                        "ProfilerControlPanel2_ModeLabelString"); // NOI18N
    private static final String TYPE_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                        "ProfilerControlPanel2_TypeLabelString"); // NOI18N
    private static final String CONFIG_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                          "ProfilerControlPanel2_ConfigLabelString"); // NOI18N
    private static final String ON_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                      "ProfilerControlPanel2_OnLabelString"); // NOI18N
    private static final String STATUS_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                          "ProfilerControlPanel2_StatusLabelString"); // NOI18N
    private static final String PROFILE_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                           "ProfilerControlPanel2_ProfileLabelString"); // NOI18N
    private static final String CPU_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                       "ProfilerControlPanel2_CpuLabelString"); // NOI18N
    private static final String ENTIRE_APP_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                              "ProfilerControlPanel2_EntireAppLabelString"); // NOI18N
    private static final String THIS_COMP_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                             "ProfilerControlPanel2_ThisCompLabelString"); // NOI18N
    private static final String RUNNING_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                           "ProfilerControlPanel2_RunningLabelString"); // NOI18N
    private static final String ATTACH_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                          "ProfilerControlPanel2_AttachLabelString"); // NOI18N
    private static final String CODE_FRAGMENT_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                 "ProfilerControlPanel2_CodeFragmentLabelString"); // NOI18N
    private static final String MEMORY_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                          "ProfilerControlPanel2_MemoryLabelString"); // NOI18N
    private static final String MONITOR_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                           "ProfilerControlPanel2_MonitorLabelString"); // NOI18N
    private static final String INACTIVE_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                            "ProfilerControlPanel2_InactiveLabelString"); // NOI18N
    private static final String STARTED_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                           "ProfilerControlPanel2_StartedLabelString"); // NOI18N
    private static final String PAUSED_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                          "ProfilerControlPanel2_PausedLabelString"); // NOI18N
    private static final String STOPPED_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                           "ProfilerControlPanel2_StoppedLabelString"); // NOI18N
    private static final String STARTING_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                            "ProfilerControlPanel2_StartingLabelString"); // NOI18N
    private static final String TELEMETRY_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                            "ProfilerControlPanel2_TelemetryButtonName"); // NOI18N
    private static final String TELEMETRY_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                               "ProfilerControlPanel2_TelemetryButtonToolTip"); // NOI18N
    private static final String THREADS_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                          "ProfilerControlPanel2_ThreadsButtonName"); // NOI18N
    private static final String THREADS_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                             "ProfilerControlPanel2_ThreadsButtonToolTip"); // NOI18N
    private static final String GLOBAL_COMBO_ITEM_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                               "ProfilerControlPanel2_GlobalComboItemString"); // NOI18N
    private static final String CONFIRM_DELETE_SNAPSHOT_CAPTION = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                      "ProfilerControlPanel2_ConfirmDeleteSnapshotCaption"); // NOI18N
    private static final String CONFIRM_DELETE_SNAPSHOT_MSG = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                  "ProfilerControlPanel2_ConfirmDeleteSnapshotMsg"); // NOI18N
    private static final String OPEN_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                       "ProfilerControlPanel2_OpenButtonName"); // NOI18N
    private static final String DELETE_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                         "ProfilerControlPanel2_DeleteButtonName"); // NOI18N
    private static final String LOAD_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                       "ProfilerControlPanel2_LoadButtonName"); // NOI18N
    private static final String EXPORT_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                         "ProfilerControlPanel2_ExportButtonName"); // NOI18N
    private static final String TAKE_SNAPSHOT_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                "ProfilerControlPanel2_TakeSnapshotButtonName"); // NOI18N
    private static final String DUMP_HEAP_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                            "ProfilerControlPanel2_DumpHeapButtonName"); // NOI18N
    private static final String TAKE_SNAPSHOT_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                   "ProfilerControlPanel2_TakeSnapshotButtonToolTip"); // NOI18N
    private static final String LIVE_RESULTS_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                               "ProfilerControlPanel2_LiveResultsButtonName"); // NOI18N
    private static final String LIVE_RESULTS_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                  "ProfilerControlPanel2_LiveResultsButtonToolTip"); // NOI18N
    private static final String RESET_RESULTS_BUTTON_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                "ProfilerControlPanel2_ResetResultsButtonName"); // NOI18N
    private static final String RESET_RESULTS_BUTTON_TOOLTIP = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                   "ProfilerControlPanel2_ResetResultsButtonToolTip"); // NOI18N
    private static final String INSTRUMENTED_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                "ProfilerControlPanel2_InstrumentedLabelString"); // NOI18N
    private static final String FILTER_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                          "ProfilerControlPanel2_FilterLabelString"); // NOI18N
    private static final String THREADS_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                           "ProfilerControlPanel2_ThreadsLabelString"); // NOI18N
    private static final String TOTAL_MEMORY_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                "ProfilerControlPanel2_TotalMemoryLabelString"); // NOI18N
    private static final String USED_MEMORY_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                               "ProfilerControlPanel2_UsedMemoryLabelString"); // NOI18N
    private static final String GC_TIME_LABEL_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                           "ProfilerControlPanel2_GcTimeLabelString"); // NOI18N
    private static final String NO_LINES_CODE_REGION_MSG = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                               "ProfilerControlPanel2_NoLinesCodeRegionMsg"); // NOI18N
    private static final String NO_METHODS_MSG = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                     "ProfilerControlPanel2_NoMethodsMsg"); // NOI18N
    private static final String NO_CLASSES_MSG = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                     "ProfilerControlPanel2_NoClassesMsg"); // NOI18N
    private static final String NOTHING_INSTRUMENTED_MSG = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                               "ProfilerControlPanel2_NothingInstrumentedMsg"); // NOI18N
    private static final String CONTROL_PANEL_TOOLTIP = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                            "ProfilerControlPanel2_ControlPanelToolTip"); // NOI18N
    private static final String COMBO_ACCESS_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                        "ProfilerControlPanel2_ComboAccessName"); // NOI18N
    private static final String COMBO_ACCESS_DESCR = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                         "ProfilerControlPanel2_ComboAccessDescr"); // NOI18N
    private static final String LIST_ACCESS_NAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                       "ProfilerControlPanel2_ListAccessName"); // NOI18N
    private static final String OPEN_BUTTON_ACCESS_DESCR = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                               "ProfilerControlPanel2_OpenButtonAccessDescr"); // NOI18N
    private static final String DELETE_BUTTON_ACCESS_DESCR = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                 "ProfilerControlPanel2_DeleteButtonAccessDescr"); // NOI18N
    private static final String EXPORT_BUTTON_ACCESS_DESCR = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                 "ProfilerControlPanel2_ExportButtonAccessDescr"); // NOI18N
    private static final String LOAD_BUTTON_ACCESS_DESCR = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                               "ProfilerControlPanel2_LoadButtonAccessDescr"); // NOI18N
    private static final String NO_CONFIGURATION_STRING = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                              "ProfilerControlPanel2_NoConfigurationString"); // NOI18N
    private static final String CONTROL_PANEL_ACCESS_DESCR = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                 "ProfilerControlPanel2_ControlPanelAcessDescr"); // NOI18N
    private static final String HEAP_SNAPSHOT_DISPLAYNAME = NbBundle.getMessage(ProfilerControlPanel2.class,
                                                                                "ProfilerControlPanel2_HeapSnapshotDisplayName"); // NOI18N
                                                                                                                                  // -----
    private static final String HELP_CTX_KEY = "ProfilerControlPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static ProfilerControlPanel2 defaultInstance;
    private static final Image windowIcon = ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/controlPanelWindow.gif"); // NOI18N
    private static final ImageIcon cpuIcon = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/cpuSmall.png", false); // NOI18N
    private static final ImageIcon fragmentIcon = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/fragmentSmall.png", false); // NOI18N
    private static final ImageIcon memoryIcon = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/memorySmall.png", false); // NOI18N
    private static final ImageIcon emptyIcon = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/empty16.gif", false); // NOI18N
    private static final String ID = "profiler_cp"; // NOI18N // for winsys persistence
    private static final Integer EXTERNALIZABLE_VERSION_WITH_SNAPSHOTS = new Integer(3);
    
    private static final Color CP_BACKGROUND_COLOR = UIUtils.getProfilerResultsBackground();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final BasicTelemetryPanel basicTelemetrySnippet;
    private final JScrollPane scrollPane;
    private final ResultsSnippetPanel resultsSnippet;
    private final SnapshotsPanel snapshotsSnippet;
    private final SnippetPanel spBasicTelemetry;
    private final SnippetPanel spControls;
    private final SnippetPanel spResults;
    private final SnippetPanel spSnapshots;
    private final SnippetPanel spStatus;
    private final SnippetPanel spView;
    private final StatusPanel statusSnippet;
    private Component lastFocusOwner;
    private boolean initialized = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ProfilerControlPanel2() {
        setName(CONTROL_PANEL_NAME); // NOI18N
        setIcon(windowIcon);
        getAccessibleContext().setAccessibleDescription(CONTROL_PANEL_ACCESS_DESCR);
        setLayout(new BorderLayout());

        ControlsPanel controlsSnippet = new ControlsPanel();
        statusSnippet = new StatusPanel();

        ViewPanel viewSnippet = new ViewPanel();
        snapshotsSnippet = new SnapshotsPanel();
        resultsSnippet = new ResultsSnippetPanel();
        basicTelemetrySnippet = new BasicTelemetryPanel();

        final SnippetPanel.Padding padding = new SnippetPanel.Padding();

        //final JPanel scrollPanel = new CPMainPanel();
        final JPanel scrollPanel = new JPanel();
        scrollPanel.setLayout(new VerticalLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;

        scrollPanel.add(spControls = new SnippetPanel(CONTROLS_SNIPPET_NAME, controlsSnippet), gbc);
        scrollPanel.add(spStatus = new SnippetPanel(STATUS_SNIPPET_NAME, statusSnippet), gbc);
        scrollPanel.add(spResults = new SnippetPanel(RESULTS_SNIPPET_NAME, resultsSnippet), gbc);
        scrollPanel.add(spSnapshots = new SnippetPanel(SNAPSHOTS_SNIPPET_NAME, snapshotsSnippet), gbc);
        scrollPanel.add(spView = new SnippetPanel(VIEW_SNIPPET_NAME, viewSnippet), gbc);
        scrollPanel.add(spBasicTelemetry = new SnippetPanel(TELEMETRY_SNIPPET_NAME, basicTelemetrySnippet), gbc);
        gbc.weighty = 1;
        scrollPanel.add(padding, gbc);

        scrollPane = new JScrollPane(scrollPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        spControls.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, CP_BACKGROUND_COLOR));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CP_BACKGROUND_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(50);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(50);
        add(scrollPane, BorderLayout.CENTER);
        
        Profiler.getDefault().addProfilingStateListener(this);
        ResultsManager.getDefault().addSnapshotsListener(this);
        ResultsManager.getDefault().addResultsListener(this);

        addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    scrollPane.getVerticalScrollBar()
                              .setBlockIncrement((int) (scrollPane.getVerticalScrollBar().getModel().getExtent() * 0.95f));
                    scrollPane.getHorizontalScrollBar()
                              .setBlockIncrement((int) (scrollPane.getHorizontalScrollBar().getModel().getExtent() * 0.95f));
                }
            });

        setFocusable(true);
        setRequestFocusEnabled(true);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized ProfilerControlPanel2 getDefault() {
        if (defaultInstance == null) {
            IDEUtils.runInEventDispatchThreadAndWait(new Runnable() {
                public void run() {
                    defaultInstance = (ProfilerControlPanel2) WindowManager.getDefault().findTopComponent(ID);
                    if (defaultInstance == null) defaultInstance = new ProfilerControlPanel2();
                }
            });
        }

        return defaultInstance;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public void setProfiledProject(Project project) {
        snapshotsSnippet.setDisplayedProject(project);
    }

    public String getToolTipText() {
        return CONTROL_PANEL_TOOLTIP;
    }

    public static synchronized void closeIfOpened() {
        IDEUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (defaultInstance != null && defaultInstance.isOpened()) defaultInstance.close();
            }
        });
    }

    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    public void componentActivated() {
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        } else if (spControls != null) {
            spControls.requestFocus();
        }
    }

    public void componentDeactivated() {
        lastFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    public void instrumentationChanged(final int oldInstrType, final int currentInstrType) {
        updateStatus();
    }

    public boolean needsDocking() {
        return WindowManager.getDefault().findMode(this) == null;
    }

    public void open() {
        if (needsDocking()) { // needs docking

            Mode mode = WindowManager.getDefault().findMode("explorer"); // NOI18N

            if (mode != null) {
                mode.dockInto(this);
            }
        }

        super.open();
    }

    public void paint(final Graphics g) {
        super.paint(g);

        // HACK: Ugly ugly ugly hack for problem with horizontal position of the ScrollPane after deserialization
        // I have not found any other way to make this work
        if (!initialized) {
            initialized = true;
            scrollPane.getViewport().setViewPosition(new Point(0, 0));
        }
    }

    public void profilingStateChanged(final ProfilingStateEvent e) {
        updateStatus();
    }

    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        Integer version;

        try {
            version = (Integer) in.readObject();
        } catch (Exception e) {
            ProfilerLogger.severe("Error while deserializing Profiler CP2: " + e.getMessage()); // NOI18N
            NetBeansProfiler.profilerErrorManager.notify(ErrorManager.INFORMATIONAL, e);

            return; // unsupported storage format
        }

        if (version.compareTo(EXTERNALIZABLE_VERSION_WITH_SNAPSHOTS) > 0) {
            return; // future version, we cannot read farther
        }

        spControls.setCollapsed(((Boolean) in.readObject()).booleanValue());
        spStatus.setCollapsed(((Boolean) in.readObject()).booleanValue());
        spResults.setCollapsed(((Boolean) in.readObject()).booleanValue());
        spView.setCollapsed(((Boolean) in.readObject()).booleanValue());
        spBasicTelemetry.setCollapsed(((Boolean) in.readObject()).booleanValue());

        if (version.equals(EXTERNALIZABLE_VERSION_WITH_SNAPSHOTS)) {
            spSnapshots.setCollapsed(((Boolean) in.readObject()).booleanValue());
        }
    }

    public void refreshSnapshotsList() {
        if (snapshotsSnippet != null) {
            snapshotsSnippet.refreshList();
        }
    }

    public void resultsAvailable() {
        //    ((TakeSnapshotAction)TakeSnapshotAction.get(TakeSnapshotAction.class)).performAction();
        resultsSnippet.resultsAvailable();
    }

    public void resultsReset() {
        resultsSnippet.resultsReset();
    }

    public void snapshotLoaded(LoadedSnapshot snapshot) {
        snapshotsSnippet.snapshotLoaded(snapshot);
    }

    public void snapshotRemoved(LoadedSnapshot snapshot) {
        resultsSnippet.snapshotRemoved(snapshot);
        snapshotsSnippet.snapshotRemoved(snapshot);
    }

    public void snapshotSaved(LoadedSnapshot snapshot) {
        snapshotsSnippet.snapshotSaved(snapshot);
    }

    public void snapshotTaken(LoadedSnapshot snapshot) {
        resultsSnippet.snapshotTaken(snapshot);
    }

    public void threadsMonitoringChanged() {
        updateStatus();
    }

    public void updateStatus() {
        statusSnippet.refreshStatus();
        resultsSnippet.refreshStatus();
        basicTelemetrySnippet.refreshStatus();
    }

    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(EXTERNALIZABLE_VERSION_WITH_SNAPSHOTS);
        out.writeObject(Boolean.valueOf(spControls.isCollapsed()));
        out.writeObject(Boolean.valueOf(spStatus.isCollapsed()));
        out.writeObject(Boolean.valueOf(spResults.isCollapsed()));
        out.writeObject(Boolean.valueOf(spView.isCollapsed()));
        //    out.writeObject(Boolean.FALSE/*spTimeline.isCollapsed()*/); // skip timeline boolean
        //    out.writeObject(Boolean.FALSE/*spInstrumentation.isCollapsed()*/); // skip instrumentation boolean
        out.writeObject(Boolean.valueOf(spBasicTelemetry.isCollapsed()));
        out.writeObject(Boolean.valueOf(spSnapshots.isCollapsed()));
    }

    protected String preferredID() {
        return ID;
    }

    //  private static class CPMainPanel extends JPanel implements Scrollable {
    //
    //    public Dimension getPreferredScrollableViewportSize() {
    //      return getPreferredSize();
    //    }
    //
    //    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    //      return 10;
    //    }
    //
    //    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    //      return 50;
    //    }
    //
    //    public boolean getScrollableTracksViewportWidth() {
    //      return true;
    //    }
    //
    //    public boolean getScrollableTracksViewportHeight() {
    //      return false;
    //    }
    //  }
}
