/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler;

import org.netbeans.modules.profiler.utilities.Delegate;
import javax.swing.event.ChangeEvent;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.MonitoredData;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.EqualFlowLayout;
import org.netbeans.lib.profiler.ui.components.FlatToolBar;
import org.netbeans.lib.profiler.ui.components.SnippetPanel;
import org.netbeans.modules.profiler.actions.*;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.SystemAction;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.IconUIResource;
import javax.swing.plaf.basic.BasicComboBoxUI;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.common.event.SimpleProfilingStateAdapter;
import org.netbeans.modules.profiler.ProfilerControlPanel2.WhiteFilter;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileLock;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;


/**
 * The main control panel window for profiling functionality, docked into explorer by default.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "ProfilerControlPanel2_ControlsSnippetName=Controls",
    "ProfilerControlPanel2_StatusSnippetName=Status",
    "ProfilerControlPanel2_ResultsSnippetName=Profiling Results",
    "ProfilerControlPanel2_SnapshotsSnippetName=Saved Snapshots",
    "ProfilerControlPanel2_ViewSnippetName=View",
    "ProfilerControlPanel2_TelemetrySnippetName=Basic Telemetry",
    "ProfilerControlPanel2_TypeLabelString=Type:",
    "ProfilerControlPanel2_EntireAppLabelString=Entire Application",
    "ProfilerControlPanel2_ThisCompLabelString=This Computer",
    "ProfilerControlPanel2_InactiveLabelString=Inactive",
    "ProfilerControlPanel2_TelemetryButtonName=VM Telemetry",
    "ProfilerControlPanel2_TelemetryButtonToolTip=Show VM Telemetry graphs",
    "ProfilerControlPanel2_ThreadsButtonName=Threads",
    "ProfilerControlPanel2_ThreadsButtonToolTip=Show application threads timeline",
    "ProfilerControlPanel2_GlobalComboItemString=<Global>",
    "ProfilerControlPanel2_ConfirmDeleteSnapshotCaption=Confirm File Delete",
    "ProfilerControlPanel2_ConfirmDeleteSnapshotMsg=Do you really want to delete the selected snapshot(s) from the disk?\nYou cannot undo this operation.",
    "ProfilerControlPanel2_OpenButtonName=Open",
    "ProfilerControlPanel2_DeleteButtonName=Delete",
    "ProfilerControlPanel2_RenameButtonName=Rename...",
    "ProfilerControlPanel2_ExportButtonName=Save As...",
    "ProfilerControlPanel2_TakeSnapshotButtonName=Take Snapshot",
    "ProfilerControlPanel2_DumpHeapButtonName=Dump Heap",
    "ProfilerControlPanel2_TakeSnapshotButtonToolTip=Obtains a snapshot of profiling results at the current moment",
    "ProfilerControlPanel2_TakeHeapDumpButtonToolTip=Takes a heap snapshot of the application",
    "ProfilerControlPanel2_LiveResultsButtonName=Live Results",
    "ProfilerControlPanel2_LiveResultsButtonToolTip=Show live profiling results",
    "ProfilerControlPanel2_ResetResultsButtonName=Reset Collected Results",
    "ProfilerControlPanel2_ResetResultsButtonToolTip=Reset the profiler data obtained so far (if any)",
    "ProfilerControlPanel2_InstrumentedLabelString=Instrumented:",
    "ProfilerControlPanel2_FilterLabelString=Filter:",
    "ProfilerControlPanel2_ThreadsLabelString=Threads:",
    "ProfilerControlPanel2_TotalMemoryLabelString=Total Memory:",
    "ProfilerControlPanel2_UsedMemoryLabelString=Used Memory:", 
    "ProfilerControlPanel2_GcTimeLabelString=Time Spent in GC:",
    "ProfilerControlPanel2_NoLinesCodeRegionMsg={0} Lines in Code Region",
    "ProfilerControlPanel2_NoMethodsMsg={0} Methods",
    "ProfilerControlPanel2_NoClassesMsg={0} Classes",
    "ProfilerControlPanel2_NothingInstrumentedMsg=None",
    "ProfilerControlPanel2_ControlPanelToolTip=Profiler Control Panel",
    "ProfilerControlPanel2_ComboAccessName=List of open projects.",
    "ProfilerControlPanel2_ComboAccessDescr=Select project to view saved snapshots for project.",
    "ProfilerControlPanel2_ListAccessName=List of saved snapshots for selected project.",
    "ProfilerControlPanel2_OpenButtonAccessDescr=Opens selected snapshots.",
    "ProfilerControlPanel2_DeleteButtonAccessDescr=Delete selected snapshots.",
    "ProfilerControlPanel2_ExportButtonAccessDescr=Save selected snapshots as standalone files.",
    "ProfilerControlPanel2_RenameButtonAccessDescr=Renames currently selected snapshot file.",
    "ProfilerControlPanel2_ControlPanelAcessDescr=Profiler control panel",
    "ProfilerControlPanel2_SnapshotsNotDeletedMsg=<html><b>Problem deleting snapshot(s).</b><br><br>The snapshot(s) might not have been deleted<br>or have already been deleted outside of the IDE.</html>",
    "ProfilerControlPanel2_RenameSnapshotCaption=Rename Snapshot",
    "ProfilerControlPanel2_NewFileNameLbl=&New file name:",
    "ProfilerControlPanel2_RenameSnapshotFailedMsg=Failed to rename snapshot to {0}",
    "ProfilerControlPanel2_EmptyNameMsg=Snapshot name cannot be empty.",
    "MSG_Loading_Progress=Loading...",
    "LAB_ControlPanelName=Profiler",
    "#NOI18N",
    "ProfilerControlPanel2_WindowMode=explorer"
})
public final class ProfilerControlPanel2 extends ProfilerTopComponent {
    final private static Logger LOGGER = Logger.getLogger(ProfilerControlPanel2.class.getName());
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /*
     * The following code is an externalization of various listeners registered
     * in the global lookup and needing access to an enclosing instance of
     * ProfilerControlPanel2.
     * The enclosing instance will use the FQN registration to obtain the shared instance
     * of the listener implementation and inject itself as a delegate into the listener.
     */
    @ServiceProvider(service=SnapshotsListener.class)
    public static final class Listener extends Delegate<ProfilerControlPanel2> implements SnapshotsListener, ResultsListener {
        @Override
        public void resultsAvailable() {
            //    ((TakeSnapshotAction)TakeSnapshotAction.get(TakeSnapshotAction.class)).performAction();
            if (getDelegate() != null) getDelegate().resultsSnippet.resultsAvailable();
        }

        @Override
        public void resultsReset() {
            if (getDelegate() != null) getDelegate().resultsSnippet.resultsReset();
        }

        @Override
        public void snapshotLoaded(LoadedSnapshot snapshot) {
            if (getDelegate() != null) getDelegate().snapshotsSnippet.snapshotLoaded(snapshot);
        }

        @Override
        public void snapshotRemoved(LoadedSnapshot snapshot) {
            if (getDelegate() != null) getDelegate().resultsSnippet.snapshotRemoved(snapshot);
            if (getDelegate() != null) getDelegate().snapshotsSnippet.snapshotRemoved(snapshot);
        }

        @Override
        public void snapshotSaved(LoadedSnapshot snapshot) {
            if (getDelegate() != null) getDelegate().snapshotsSnippet.snapshotSaved(snapshot);
        }

        @Override
        public void snapshotTaken(LoadedSnapshot snapshot) {
            if (getDelegate() != null) getDelegate().resultsSnippet.snapshotTaken(snapshot);
        }
    }
    
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

            final JLabel instrLabel = new JLabel(Bundle.ProfilerControlPanel2_InstrumentedLabelString());
            instrLabel.setFont(instrLabel.getFont().deriveFont(Font.BOLD));

            final JLabel instrFilterLabel = new JLabel(Bundle.ProfilerControlPanel2_FilterLabelString());
            instrFilterLabel.setFont(instrFilterLabel.getFont().deriveFont(Font.BOLD));

            final JLabel threadsLabel = new JLabel(Bundle.ProfilerControlPanel2_ThreadsLabelString());
            threadsLabel.setFont(threadsLabel.getFont().deriveFont(Font.BOLD));

            final JLabel typeLabel = new JLabel(Bundle.ProfilerControlPanel2_TypeLabelString());
            typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD));

            final JLabel totalMemLabel = new JLabel(Bundle.ProfilerControlPanel2_TotalMemoryLabelString());
            totalMemLabel.setFont(totalMemLabel.getFont().deriveFont(Font.BOLD));

            final JLabel usedMemLabel = new JLabel(Bundle.ProfilerControlPanel2_UsedMemoryLabelString());
            usedMemLabel.setFont(usedMemLabel.getFont().deriveFont(Font.BOLD));

            final JLabel relTimeLabel = new JLabel(Bundle.ProfilerControlPanel2_GcTimeLabelString());
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

            final TargetAppRunner targetAppRunner = Profiler.getDefault().getTargetAppRunner();

            String instrStatusText = ""; // NOI18N

            if (state != Profiler.PROFILING_INACTIVE) {
                final int currentInstrType = targetAppRunner.getProfilingSessionStatus().currentInstrType;

                switch (currentInstrType) {
                    case CommonConstants.INSTR_CODE_REGION:
                        instrStatusText = Bundle.ProfilerControlPanel2_NoLinesCodeRegionMsg(
                                            Integer.valueOf(targetAppRunner.getProfilingSessionStatus().instrEndLine
                                                - targetAppRunner.getProfilingSessionStatus().instrStartLine));

                        break;
                    case CommonConstants.INSTR_RECURSIVE_FULL:
                    case CommonConstants.INSTR_RECURSIVE_SAMPLED:

                        int nMethods = targetAppRunner.getProfilingSessionStatus().getNInstrMethods();

                        if (nMethods > 0) {
                            nMethods--; // Because nInstrMethods is actually the array size where element 0 is always empty
                        }

                        instrStatusText = Bundle.ProfilerControlPanel2_NoMethodsMsg(Integer.valueOf(nMethods));

                        break;
                    case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
                    case CommonConstants.INSTR_OBJECT_LIVENESS:

                        final int nClasses = targetAppRunner.getProfilingSessionStatus().getNInstrClasses();
                        instrStatusText = Bundle.ProfilerControlPanel2_NoClassesMsg(Integer.valueOf(nClasses));
                        ;

                        break;
                    case CommonConstants.INSTR_NONE_SAMPLING:
                    case CommonConstants.INSTR_NONE_MEMORY_SAMPLING:
                    case CommonConstants.INSTR_NONE:
                        instrStatusText = Bundle.ProfilerControlPanel2_NothingInstrumentedMsg();

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

            JButton stopButton = new JButton(StopAction.getInstance()) {
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

            JButton resetButton = new JButton(ResetResultsAction.getInstance());
            resetButton.setText(null);
            UIUtils.fixButtonUI(resetButton);
            resetButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) resetButton
                                                                                                          .getIcon()).getImage()))));
            resetButton.setContentAreaFilled(false);
            resetButton.setMargin(new Insets(3, 3, 3, 3));
            resetButton.setRolloverEnabled(true);
            resetButton.setBorder(myRolloverBorder);
            add(resetButton);

            JButton rungcButton = new JButton(RunGCAction.getInstance());
            rungcButton.setText(null);
            UIUtils.fixButtonUI(rungcButton);
            rungcButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) rungcButton
                                                                                                          .getIcon()).getImage()))));
            rungcButton.setContentAreaFilled(false);
            rungcButton.setMargin(new Insets(3, 3, 3, 3));
            rungcButton.setRolloverEnabled(true);
            rungcButton.setBorder(myRolloverBorder);
            add(rungcButton);

            JButton modifyButton = new JButton(ModifyProfilingAction.getInstance());
            modifyButton.setText(null);
            UIUtils.fixButtonUI(modifyButton);
            modifyButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) modifyButton
                                                                                                           .getIcon()).getImage()))));
            modifyButton.setContentAreaFilled(false);
            modifyButton.setMargin(new Insets(3, 3, 3, 3));
            modifyButton.setRolloverEnabled(true);
            modifyButton.setBorder(myRolloverBorder);
            add(modifyButton);

            JButton telemetryButton = new JButton(TelemetryOverviewAction.getInstance());
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

        private static class Renderer extends DefaultListCellRenderer {
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

            if ((value != null) && value instanceof Lookup.Provider) {
                Lookup.Provider p = (Lookup.Provider) value;
                renderer.setText(ProjectUtilities.getDisplayName(p));
                renderer.setIcon(ProjectUtilities.getIcon(p));

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
        private static final Icon TAKE_SNAPSHOT_CPU_ICON = Icons.getIcon(ProfilerIcons.TAKE_SNAPSHOT_CPU_32);
        private static final Icon TAKE_SNAPSHOT_MEMORY_ICON = Icons.getIcon(ProfilerIcons.TAKE_SNAPSHOT_MEMORY_32);
        private static final Icon TAKE_SNAPSHOT_FRAGMENT_ICON = Icons.getIcon(ProfilerIcons.TAKE_SNAPSHOT_FRAGMENT_32);
        private static final Icon TAKE_HEAP_DUMP_ICON = Icons.getIcon(ProfilerIcons.TAKE_HEAP_DUMP_32);
        private static final Icon LIVE_RESULTS_CPU_ICON = Icons.getIcon(ProfilerIcons.VIEW_LIVE_RESULTS_CPU_32);
        private static final Icon LIVE_RESULTS_MEMORY_ICON = Icons.getIcon(ProfilerIcons.VIEW_LIVE_RESULTS_MEMORY_32);
        private static final Icon LIVE_RESULTS_FRAGMENT_ICON = Icons.getIcon(ProfilerIcons.VIEW_LIVE_RESULTS_FRAGMENT_32);

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JButton takeCPUSnapshotButton;
        private final JButton takeFragmentSnapshotButton;
        private final JButton takeMemorySnapshotButton;
        private final JButton takeHeapDumpButton;
        private final JPanel centerPanel;
        private JButton liveResultsButton;
        private LoadedSnapshot lastSnapshot;
        private int displayedIcon = CPU;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ResultsSnippetPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(7, 5, 8, 0));

            final Border myRolloverBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY,
                                                                                                        Color.LIGHT_GRAY),
                                                               new FlatToolBar.FlatMarginBorder());

            // Take CPU snapshot
            takeCPUSnapshotButton = new JButton(Bundle.ProfilerControlPanel2_TakeSnapshotButtonName(), TAKE_SNAPSHOT_CPU_ICON);
            UIUtils.fixButtonUI(takeCPUSnapshotButton);
            takeCPUSnapshotButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) takeCPUSnapshotButton
                                                                                                                    .getIcon())
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .getImage()))));
            takeCPUSnapshotButton.addActionListener(this);
            takeCPUSnapshotButton.setContentAreaFilled(false);
            takeCPUSnapshotButton.setMargin(new Insets(1, 2, 1, 2));
            takeCPUSnapshotButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            takeCPUSnapshotButton.setHorizontalTextPosition(SwingConstants.CENTER);
            takeCPUSnapshotButton.setRolloverEnabled(true);
            takeCPUSnapshotButton.setBorder(myRolloverBorder);
            takeCPUSnapshotButton.setToolTipText(Bundle.ProfilerControlPanel2_TakeSnapshotButtonToolTip());

            // Take Memory snapshot
            takeMemorySnapshotButton = new JButton(Bundle.ProfilerControlPanel2_TakeSnapshotButtonName(), TAKE_SNAPSHOT_MEMORY_ICON);
            UIUtils.fixButtonUI(takeMemorySnapshotButton);
            takeMemorySnapshotButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) takeMemorySnapshotButton
                                                                                                                       .getIcon())
                                                                                                                      .getImage()))));
            takeMemorySnapshotButton.addActionListener(this);
            takeMemorySnapshotButton.setContentAreaFilled(false);
            takeMemorySnapshotButton.setMargin(new Insets(1, 2, 1, 2));
            takeMemorySnapshotButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            takeMemorySnapshotButton.setHorizontalTextPosition(SwingConstants.CENTER);
            takeMemorySnapshotButton.setRolloverEnabled(true);
            takeMemorySnapshotButton.setBorder(myRolloverBorder);
            takeMemorySnapshotButton.setToolTipText(Bundle.ProfilerControlPanel2_TakeSnapshotButtonToolTip());
            
            // Take Heap Dump
            takeHeapDumpButton = new JButton(HeapDumpAction.getInstance());
            takeHeapDumpButton.setText(Bundle.ProfilerControlPanel2_DumpHeapButtonName());
            takeHeapDumpButton.setIcon(TAKE_HEAP_DUMP_ICON);
            UIUtils.fixButtonUI(takeHeapDumpButton);
            takeHeapDumpButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) takeHeapDumpButton
                                                                                                                       .getIcon())
                                                                                                                      .getImage()))));
            takeHeapDumpButton.addActionListener(this);
            takeHeapDumpButton.setContentAreaFilled(false);
            takeHeapDumpButton.setMargin(new Insets(1, 2, 1, 2));
            takeHeapDumpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            takeHeapDumpButton.setHorizontalTextPosition(SwingConstants.CENTER);
            takeHeapDumpButton.setRolloverEnabled(true);
            takeHeapDumpButton.setBorder(myRolloverBorder);
            takeHeapDumpButton.setToolTipText(Bundle.ProfilerControlPanel2_TakeHeapDumpButtonToolTip());

            // Take Fragment snapshot
            takeFragmentSnapshotButton = new JButton(Bundle.ProfilerControlPanel2_TakeSnapshotButtonName(), TAKE_SNAPSHOT_FRAGMENT_ICON);
            UIUtils.fixButtonUI(takeFragmentSnapshotButton);
            takeFragmentSnapshotButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) takeFragmentSnapshotButton
                                                                                                                         .getIcon())
                                                                                                                        .getImage()))));
            takeFragmentSnapshotButton.addActionListener(this);
            takeFragmentSnapshotButton.setContentAreaFilled(false);
            takeFragmentSnapshotButton.setMargin(new Insets(1, 2, 1, 2));
            takeFragmentSnapshotButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            takeFragmentSnapshotButton.setHorizontalTextPosition(SwingConstants.CENTER);
            takeFragmentSnapshotButton.setRolloverEnabled(true);
            takeFragmentSnapshotButton.setBorder(myRolloverBorder);
            takeFragmentSnapshotButton.setToolTipText(Bundle.ProfilerControlPanel2_TakeSnapshotButtonToolTip());

            liveResultsButton = new JButton(Bundle.ProfilerControlPanel2_LiveResultsButtonName(), LIVE_RESULTS_CPU_ICON);
            UIUtils.fixButtonUI(liveResultsButton);
            liveResultsButton.setDisabledIcon(new IconUIResource(new ImageIcon(WhiteFilter.createDisabledImage(((ImageIcon) liveResultsButton
                                                                                                                .getIcon())
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .getImage()))));
            liveResultsButton.addActionListener(this);
            liveResultsButton.setContentAreaFilled(false);
            liveResultsButton.setMargin(new Insets(1, 2, 1, 2));
            liveResultsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            liveResultsButton.setHorizontalTextPosition(SwingConstants.CENTER);
            liveResultsButton.setRolloverEnabled(true);
            liveResultsButton.setBorder(myRolloverBorder);
            liveResultsButton.setToolTipText(Bundle.ProfilerControlPanel2_LiveResultsButtonToolTip());

            displayedIcon = CPU;

            centerPanel = new JPanel();
            centerPanel.setOpaque(false);
            centerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            centerPanel.add(takeCPUSnapshotButton);
            centerPanel.add(takeHeapDumpButton);
            centerPanel.add(liveResultsButton);

            add(centerPanel, BorderLayout.CENTER);

            refreshStatus();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void actionPerformed(final ActionEvent e) {
            if ((e.getSource() == takeCPUSnapshotButton) || (e.getSource() == takeFragmentSnapshotButton)) {
                ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                        public void run() {
                            ResultsManager.getDefault().takeSnapshot();
                        }
                    });
            } else if (e.getSource() == takeMemorySnapshotButton) {
                ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                        public void run() {
                            ResultsManager.getDefault().takeSnapshot();
                        }
                    });
            } else if (e.getSource() == liveResultsButton) {
                LiveResultsWindow.getDefault().open();
                LiveResultsWindow.getDefault().requestActive();
                LiveResultsWindow.getDefault().refreshLiveResults();
            }
        }

        public void refreshStatus() {
            updateResultsButtons();
            
            int state = Profiler.getDefault().getProfilingState();
            int instr = state != Profiler.PROFILING_INACTIVE ? 
                            Profiler.getDefault().getTargetAppRunner().getProfilerClient().getCurrentInstrType() :
                            CommonConstants.INSTR_NONE;
            int newMode;

            switch (instr) {
                case CommonConstants.INSTR_CODE_REGION:
                    newMode = FRAGMENT;

                    break;
                case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
                case CommonConstants.INSTR_OBJECT_LIVENESS:
                case CommonConstants.INSTR_NONE_MEMORY_SAMPLING:
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
            int instr = state != Profiler.PROFILING_INACTIVE ? 
                            Profiler.getDefault().getTargetAppRunner().getProfilerClient().getCurrentInstrType() :
                            CommonConstants.INSTR_NONE;
            
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
                                                                         ChangeListener {
        private static class Snapshot {
            private FileObject fo;
            private String displayName;
            private Icon icon;
            private boolean isHeapDump;
            
            Snapshot(FileObject fo) {
                this.fo = fo;
                loadDetails();
            }

            public String getDisplayName() {
                return displayName;
            }

            public Icon getIcon() {
                return icon;
            }

            public FileObject getFile() {
                return fo;
            }
            
            public boolean isHeapDump() {
                return isHeapDump;
            }
            
            private void loadDetails() {
                if (fo.getExt().equalsIgnoreCase(ResultsManager.HEAPDUMP_EXTENSION)) {
                    // Heap Dump
                    this.icon = heapDumpIcon;
                    this.displayName = ResultsManager.getDefault().getHeapDumpDisplayName(fo.getName());
                    this.isHeapDump = true;
                } else {
                    int snapshotType = ResultsManager.getDefault().getSnapshotType(fo);
                    this.displayName = ResultsManager.getDefault().getSnapshotDisplayName(fo.getName(), snapshotType);
                    this.icon = getIcon(snapshotType);
                    this.isHeapDump = false;
                }
            }
            
            private static Icon getIcon(int snapshotType) {
                switch (snapshotType) {
                    case LoadedSnapshot.SNAPSHOT_TYPE_CPU:
                        return cpuIcon;
                    case LoadedSnapshot.SNAPSHOT_TYPE_CODEFRAGMENT:
                        return fragmentIcon;
                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_ALLOCATIONS:
                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_LIVENESS:
                    case LoadedSnapshot.SNAPSHOT_TYPE_MEMORY_SAMPLED:
                        return memoryIcon;
                    default:
                        return null;
                }
            }

            public boolean equals(Object o) {
                return fo.equals(((Snapshot)o).fo);
            }
            
            public int hashCode() {
                return fo.hashCode();
            }
        }
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private DefaultListModel listModel;
        private JButton deleteButton;
        private JButton exportButton;
        private JButton renameButton;
        private JButton openButton;
        private JComboBox combo;
        private JList list;
        private Lookup.Provider displayedProject;
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
            combo.getAccessibleContext().setAccessibleName(Bundle.ProfilerControlPanel2_ComboAccessName());
            combo.getAccessibleContext().setAccessibleDescription(Bundle.ProfilerControlPanel2_ComboAccessDescr());

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
            ProjectUtilities.addOpenProjectsListener(this);

            list = new JList(listModel = new DefaultListModel());
            list.getAccessibleContext().setAccessibleName(Bundle.ProfilerControlPanel2_ListAccessName());
            list.setVisibleRowCount(8);
            list.setCellRenderer(new DefaultListCellRenderer() {
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                                  boolean cellHasFocus) {
                        final JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

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

                        if (value instanceof Snapshot) {
                            Snapshot s = (Snapshot)value;
                            // FileObject
                            final FileObject fo = s.getFile();

                            if (s.isHeapDump()) {
                                Set<TopComponent> tcs = WindowManager.getDefault().getRegistry().getOpened();
                                for (TopComponent tc : tcs) {
                                    Object o = tc.getClientProperty("HeapDumpFileName"); // NOI18N
                                    if (o != null && FileUtil.toFile(fo).equals(new File(o.toString()))) {
                                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                                        break;
                                    }
                                }
                            } else {
                                LoadedSnapshot ls = ResultsManager.getDefault().findLoadedSnapshot(FileUtil.toFile(fo));
                                if (ls != null) {
                                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                                }
                            }
                            c.setText(s.getDisplayName());
                            c.setIcon(s.getIcon());
                        } else {
                            c.setText(value.toString());
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
                            if (selectedSnapshotFiles.length == 0) return;

                            if (ProfilerDialogs.displayConfirmation(
                                    Bundle.ProfilerControlPanel2_ConfirmDeleteSnapshotMsg(), 
                                    Bundle.ProfilerControlPanel2_ConfirmDeleteSnapshotCaption())) {
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
            
            openButton = new JButton(Bundle.ProfilerControlPanel2_OpenButtonName());
            UIUtils.fixButtonUI(openButton);
            openButton.setContentAreaFilled(false);
            openButton.setMargin(new Insets(3, 3, 3, 3));
            openButton.setRolloverEnabled(true);
            openButton.setBorder(buttonsBorder);
            openButton.addActionListener(this);
            openButton.getAccessibleContext().setAccessibleDescription(Bundle.ProfilerControlPanel2_OpenButtonAccessDescr());
            
            renameButton = new JButton(Bundle.ProfilerControlPanel2_RenameButtonName());
            UIUtils.fixButtonUI(renameButton);
            renameButton.setContentAreaFilled(false);
            renameButton.setMargin(new Insets(3, 3, 3, 3));
            renameButton.setRolloverEnabled(true);
            renameButton.setBorder(buttonsBorder);
            renameButton.addActionListener(this);
            renameButton.getAccessibleContext().setAccessibleDescription(Bundle.ProfilerControlPanel2_RenameButtonAccessDescr());

            deleteButton = new JButton(Bundle.ProfilerControlPanel2_DeleteButtonName());
            UIUtils.fixButtonUI(deleteButton);
            deleteButton.setContentAreaFilled(false);
            deleteButton.setMargin(new Insets(3, 3, 3, 3));
            deleteButton.setRolloverEnabled(true);
            deleteButton.setBorder(buttonsBorder);
            deleteButton.addActionListener(this);
            deleteButton.getAccessibleContext().setAccessibleDescription(Bundle.ProfilerControlPanel2_DeleteButtonAccessDescr());

            exportButton = new JButton(Bundle.ProfilerControlPanel2_ExportButtonName());
            UIUtils.fixButtonUI(exportButton);
            exportButton.setContentAreaFilled(false);
            exportButton.setMargin(new Insets(3, 3, 3, 3));
            exportButton.setRolloverEnabled(true);
            exportButton.setBorder(buttonsBorder);
            exportButton.addActionListener(this);
            exportButton.getAccessibleContext().setAccessibleDescription(Bundle.ProfilerControlPanel2_ExportButtonAccessDescr());

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setOpaque(false);
            buttonsPanel.setLayout(new GridBagLayout());

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(0, 0, 4, 0);
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            buttonsPanel.add(openButton, gbc);
            buttonsPanel.add(renameButton, gbc);
            buttonsPanel.add(deleteButton, gbc);
            buttonsPanel.add(exportButton, gbc);
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

        public void setDisplayedProject(Lookup.Provider project) {
            displayedProject = project;
            list.clearSelection(); // clear selection in the list before refreshing it
            refreshList();
            internalChange = true;

            if (project == null) {
                combo.setSelectedItem(Bundle.ProfilerControlPanel2_GlobalComboItemString());
            } else {
                combo.setSelectedItem(project);
            }

            internalChange = false;
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == openButton) {
                //ResultsManager.getDefault().openSnapshots(loadSelectedSnapshots());
                openSelectedSnapshots();
            } else if (e.getSource() == renameButton) {
                renameSelectedSnapshot();
            } else if (e.getSource() == deleteButton) {
                final FileObject[] selectedSnapshotFiles = getSelectedSnapshotFiles();

                if (ProfilerDialogs.displayConfirmation(
                        Bundle.ProfilerControlPanel2_ConfirmDeleteSnapshotMsg(), 
                        Bundle.ProfilerControlPanel2_ConfirmDeleteSnapshotCaption())) {
                    RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                deleteSnapshots(selectedSnapshotFiles);
                                refreshList();
                            }
                        });
                }
            } else if (e.getSource() == exportButton) {
                ResultsManager.getDefault().exportSnapshots(getSelectedSnapshotFiles());
            } else if (e.getSource() == combo) {
                if (!internalChange) {
                    Object o = combo.getSelectedItem();

                    if (o instanceof Lookup.Provider) {
                        setDisplayedProject((Lookup.Provider) o);
                    } else {
                        setDisplayedProject(null);
                    }
                }
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            updateCombo();
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
                ret[i] = ((Snapshot) sel[i]).getFile();
            }

            return ret;
        }

        private void deleteSnapshots(FileObject[] selectedSnapshots) {
            boolean success = true;
            for (int i = 0; i < selectedSnapshots.length; i++) {
                FileObject selectedSnapshotFile = selectedSnapshots[i];
                try {
                    DataObject.find(selectedSnapshotFile).delete();
                } catch (Throwable t) {
                    success = false;
                    t.printStackTrace();
                }
            }
            if (!success) {
                ProfilerDialogs.displayError(Bundle.ProfilerControlPanel2_SnapshotsNotDeletedMsg());
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
                                    ResultsManager.getDefault().openSnapshot(selectedSnapshotFile);
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
        
        private void renameSelectedSnapshot() {
            FileObject[] selectedSnapshotFiles = getSelectedSnapshotFiles();
            if (selectedSnapshotFiles.length == 1) {
                final FileObject fileObject = selectedSnapshotFiles[0];
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        String origName = fileObject.getName();
                        RenameSnapshotPanel panel = new RenameSnapshotPanel();
                        panel.setSnapshotName(origName);
                        DialogDescriptor dd = new DialogDescriptor(panel, Bundle.ProfilerControlPanel2_RenameSnapshotCaption(),
                                    true, new Object[] { DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION },
                                    DialogDescriptor.OK_OPTION,
                                    0, RENAME_SNAPSHOT_HELP_CTX, null);
                        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                        d.setVisible(true);
                        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
                            String newName = panel.getSnapshotName();
                            if (!origName.equals(newName)) {
                                if (newName.length() == 0) {
                                    ProfilerDialogs.displayError(Bundle.ProfilerControlPanel2_EmptyNameMsg());
                                    renameSelectedSnapshot();
                                } else {
                                    FileLock lock = null;
                                    try {
                                        lock = fileObject.lock();
                                        final LoadedSnapshot ls = ResultsManager.getDefault().findLoadedSnapshot(
                                                FileUtil.toFile(fileObject));
                                        fileObject.rename(lock, newName, fileObject.getExt());
                                        if (ls != null) ls.setFile(FileUtil.toFile(fileObject));
                                        ProfilerControlPanel2.getDefault().refreshSnapshotsList();
                                    } catch (IOException e) {
                                        ProfilerLogger.warning("Failed to rename snapshot " // NOI18N
                                                + fileObject + " to " + newName + ": " + e.getMessage()); // NOI18N
                                        ProfilerDialogs.displayError(Bundle.ProfilerControlPanel2_RenameSnapshotFailedMsg(newName));
                                        renameSelectedSnapshot();
                                    } finally {
                                        if (lock != null) lock.releaseLock();
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }

        private static final RequestProcessor updater = new RequestProcessor("Snapshots Updater"); // NOI18N
        private static final Object updaterSync = new Object();
        private boolean updating = false;
        private boolean dirty = false;
        
        private void refreshList() {
            synchronized (updaterSync) {
                if (updating) {
                    dirty = true;
                    return;
                }
            }
            
            SwingUtilities.invokeLater(new Runnable() { // Read current selection in EDT
                public void run() {
                    final java.util.List selection = Arrays.asList(list.getSelectedValues());
                    
                    updater.post(new Runnable() { // Access snapshots on filesystem in worker thread
                        public void run()  {
                            synchronized (updaterSync) {
                                dirty = false;
                                updating = true;
                            }
                            
                            final java.util.List<Snapshot> snapshots = new ArrayList<Snapshot>();
                            try {
                                for (FileObject fo : ResultsManager.getDefault().
                                        listSavedSnapshots(displayedProject, null))
                                    snapshots.add(new Snapshot(fo));
                                for (FileObject fo : ResultsManager.getDefault().
                                        listSavedHeapdumps(displayedProject, null))
                                    snapshots.add(new Snapshot(fo));
                            } catch (Throwable t) {
                                LOGGER.log(Level.WARNING, null, t);
                            }
                            
                            SwingUtilities.invokeLater(new Runnable() { // Update snapshots in EDT
                                public void run() {
                                    listModel.clear();
                                    for (int i = 0; i < snapshots.size(); i++) {
                                        Snapshot s = snapshots.get(i);
                                        listModel.addElement(s);
                                        if (selection.contains(s))
                                            list.addSelectionInterval(i, i);
                                    }
                                    
                                    boolean refreshAgain = false;
                                    synchronized (updaterSync) {
                                        updating = false;
                                        if (dirty) refreshAgain = true;
                                    }
                                    
                                    if (refreshAgain) refreshList();
                                }
                            });
                        }
                    });
                }
            });
        }

        private void updateButtons() {
            int[] selected = list.getSelectedIndices();
            openButton.setEnabled(selected.length > 0);
            renameButton.setEnabled(selected.length == 1);
            deleteButton.setEnabled(selected.length > 0);
            exportButton.setEnabled(selected.length > 0);
        }

        private void updateCombo() {
            Lookup.Provider[] projects = ProjectUtilities.getSortedProjects(ProjectUtilities.getOpenedProjects());
            java.util.List items = new ArrayList(projects.length + 1);
            items.addAll(Arrays.asList(projects));

            items.add(0, Bundle.ProfilerControlPanel2_GlobalComboItemString());

            DefaultComboBoxModel comboModel = (DefaultComboBoxModel) combo.getModel();
            comboModel.removeAllElements();

            for (Object item : items) {
                comboModel.addElement(item);
            }

            if ((displayedProject != null) && (comboModel.getIndexOf(displayedProject) != -1)) {
                internalChange = true;
                combo.setSelectedItem(displayedProject);
                internalChange = false;
            } else {
                Lookup.Provider mainProject = ProjectUtilities.getMainProject();
                setDisplayedProject(mainProject);
            }
        }
    }

    @NbBundle.Messages({
        "ProfilerControlPanel2_ModeLabelString=Mode:",
        "ProfilerControlPanel2_ConfigLabelString=Configuration:",
        "ProfilerControlPanel2_OnLabelString=On:",
        "ProfilerControlPanel2_StatusLabelString=Status:",
        "ProfilerControlPanel2_NoConfigurationString=None",
        "ProfilerControlPanel2_AttachLabelString=Attach",
        "ProfilerControlPanel2_ProfileLabelString=Profile",
        "ProfilerControlPanel2_CodeFragmentLabelString=Code Fragment",
        "ProfilerControlPanel2_CpuSamplingLabelString=CPU sampling",
        "ProfilerControlPanel2_CpuLabelString=CPU instrumentation",       
        "ProfilerControlPanel2_MemorySamplingLabelString=Memory sampling",
        "ProfilerControlPanel2_MemoryLabelString=Memory instrumentation",
        "ProfilerControlPanel2_MonitorLabelString=Monitor",
        "ProfilerControlPanel2_RunningLabelString=Running",
        "ProfilerControlPanel2_StartedLabelString=Started",
        "ProfilerControlPanel2_PausedLabelString=Paused",
        "ProfilerControlPanel2_StoppedLabelString=Stopped",
        "ProfilerControlPanel2_StartingLabelString=Starting"
    })
    private static final class StatusPanel extends CPPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JLabel modeValueLabel;
        private final JLabel onLabel;
        private final JLabel onValueLabel;
        private final JLabel profileValueLabel;
        private final JLabel statusValueLabel;
        private final JLabel typeValueLabel;
        private int profilingType;
        private String host = null;
        private int count = 0;
        private int mode = -1;
        private int state = -1;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        StatusPanel() {
            setBorder(BorderFactory.createEmptyBorder(8, 3, 9, 3));
            setLayout(new GridBagLayout());

            final JLabel modeLabel = new JLabel(Bundle.ProfilerControlPanel2_ModeLabelString());
            modeLabel.setFont(modeLabel.getFont().deriveFont(Font.BOLD));

            final JLabel typeLabel = new JLabel(Bundle.ProfilerControlPanel2_TypeLabelString());
            typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD));

            final JLabel profileLabel = new JLabel(Bundle.ProfilerControlPanel2_ConfigLabelString());
            profileLabel.setFont(profileLabel.getFont().deriveFont(Font.BOLD));

            onLabel = new JLabel(Bundle.ProfilerControlPanel2_OnLabelString());
            onLabel.setFont(onLabel.getFont().deriveFont(Font.BOLD));

            final JLabel statusLabel = new JLabel(Bundle.ProfilerControlPanel2_StatusLabelString());
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));

            modeValueLabel = new JLabel(Bundle.ProfilerControlPanel2_NoConfigurationString());
            typeValueLabel = new JLabel(Bundle.ProfilerControlPanel2_NoConfigurationString());
            profileValueLabel = new JLabel(Bundle.ProfilerControlPanel2_NoConfigurationString());
            onValueLabel = new JLabel(Bundle.ProfilerControlPanel2_NoConfigurationString());
            statusValueLabel = new JLabel(Bundle.ProfilerControlPanel2_InactiveLabelString());

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
                    modeValueLabel.setText(Bundle.ProfilerControlPanel2_AttachLabelString());
                } else {
                    modeValueLabel.setText(Bundle.ProfilerControlPanel2_ProfileLabelString());
                }
            }

            final ProfilingSettings ps = Profiler.getDefault().getLastProfilingSettings();

            if ((ps != null) && (profilingType != ps.getProfilingType())) {
                switch (ps.getProfilingType()) {
                    case ProfilingSettings.PROFILE_CPU_STOPWATCH:
                        typeValueLabel.setText(Bundle.ProfilerControlPanel2_CodeFragmentLabelString());

                        break;
                    case ProfilingSettings.PROFILE_CPU_SAMPLING:
                        typeValueLabel.setText(Bundle.ProfilerControlPanel2_CpuSamplingLabelString());

                        break;
                        
                    case ProfilingSettings.PROFILE_CPU_ENTIRE:
                    case ProfilingSettings.PROFILE_CPU_PART:
                        typeValueLabel.setText(Bundle.ProfilerControlPanel2_CpuLabelString());

                        break;
                    case ProfilingSettings.PROFILE_MEMORY_SAMPLING:
                        typeValueLabel.setText(Bundle.ProfilerControlPanel2_MemorySamplingLabelString());

                        break;
                    case ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS:
                    case ProfilingSettings.PROFILE_MEMORY_LIVENESS:
                        typeValueLabel.setText(Bundle.ProfilerControlPanel2_MemoryLabelString());

                        break;
                    case ProfilingSettings.PROFILE_MONITOR:
                        typeValueLabel.setText(Bundle.ProfilerControlPanel2_MonitorLabelString());

                        break;
                }

                profileValueLabel.setText(ps.getSettingsName());
            }

            String newHost = Profiler.getDefault().getTargetAppRunner().getProfilerEngineSettings().getRemoteHost();

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
                        statusValueLabel.setText(Bundle.ProfilerControlPanel2_InactiveLabelString());

                        break;
                    case Profiler.PROFILING_STARTED:
                        statusValueLabel.setText(Bundle.ProfilerControlPanel2_StartedLabelString());
                        count = 1;

                        break;
                    case Profiler.PROFILING_PAUSED:
                        statusValueLabel.setText(Bundle.ProfilerControlPanel2_PausedLabelString());

                        break;
                    case Profiler.PROFILING_RUNNING:
                        statusValueLabel.setText(Bundle.ProfilerControlPanel2_RunningLabelString());

                        break;
                    case Profiler.PROFILING_STOPPED:
                        statusValueLabel.setText(Bundle.ProfilerControlPanel2_StoppedLabelString());

                        break;
                }
            } else {
                // state stays the same: started -> show progress by adding dots
                if (state == Profiler.PROFILING_STARTED) {
                    StringBuilder text = new StringBuilder(Bundle.ProfilerControlPanel2_StartingLabelString());

                    for (int i = 0; i < count; i++) {
                        text.append('.'); // NOI18N
                    }

                    statusValueLabel.setText(text.toString());
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
            setLayout(new EqualFlowLayout(FlowLayout.LEFT, 0, 0));
            setBorder(BorderFactory.createEmptyBorder(7, 6, 8, 6));

            final Border myRolloverBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY,
                                                                                                        Color.LIGHT_GRAY),
                                                               new FlatToolBar.FlatMarginBorder());

            vmTelemetryButton = new JButton(Bundle.ProfilerControlPanel2_TelemetryButtonName(), Icons.getIcon(ProfilerIcons.VIEW_TELEMETRY_32));
            UIUtils.fixButtonUI(vmTelemetryButton);
            vmTelemetryButton.addActionListener(this);
            vmTelemetryButton.setContentAreaFilled(false);
            vmTelemetryButton.setMargin(new Insets(1, 1, 1, 1));
            vmTelemetryButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            vmTelemetryButton.setHorizontalTextPosition(SwingConstants.CENTER);
            vmTelemetryButton.setRolloverEnabled(true);
            vmTelemetryButton.setBorder(myRolloverBorder);
            vmTelemetryButton.setToolTipText(Bundle.ProfilerControlPanel2_TelemetryButtonToolTip());

            threadsButton = new JButton(Bundle.ProfilerControlPanel2_ThreadsButtonName(), Icons.getIcon(ProfilerIcons.VIEW_THREADS_32));
            UIUtils.fixButtonUI(threadsButton);
            threadsButton.addActionListener(this);
            threadsButton.setContentAreaFilled(false);
            threadsButton.setMargin(new Insets(1, 1, 1, 1));
            threadsButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            threadsButton.setHorizontalTextPosition(SwingConstants.CENTER);
            threadsButton.setRolloverEnabled(true);
            threadsButton.setBorder(myRolloverBorder);
            threadsButton.setToolTipText(Bundle.ProfilerControlPanel2_ThreadsButtonToolTip());

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

    private static final class RenameSnapshotPanel extends JPanel
    {
        //~ Instance fields ----------------------------------------------------------------------------------------------------

        private JTextField textField;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        RenameSnapshotPanel() {
            initComponents();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        String getSnapshotName() {
            return textField.getText().trim();
        }

        void setSnapshotName(final String text) {
            textField.setText(text);
            textField.selectAll();
        }

        private void initComponents() {
            GridBagConstraints gridBagConstraints;
            
            JLabel textLabel = new JLabel();
            Mnemonics.setLocalizedText(textLabel, Bundle.ProfilerControlPanel2_NewFileNameLbl());
            textLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            textField = new JTextField();
            textLabel.setLabelFor(textField);
            textField.setPreferredSize(new Dimension(350, textField.getPreferredSize().height));
            textField.requestFocus();            
            textField.setAlignmentX(Component.LEFT_ALIGNMENT);

            setLayout(new GridBagLayout());
            
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(15, 10, 5, 10);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            add(textLabel, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 15, 10);
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            add(textField, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;            
            add(new JPanel(), gridBagConstraints);
            
            getAccessibleContext().setAccessibleDescription(
                    NbBundle.getMessage(NotifyDescriptor.class, "ACSD_InputPanel") // NOI18N
                    );
            textField.getAccessibleContext().setAccessibleDescription(
                    NbBundle.getMessage(NotifyDescriptor.class, "ACSD_InputField") // NOI18N
                    );
        }
    };

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final String HELP_CTX_KEY = "ProfilerControlPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static final String RENAME_SNAPSHOT_HELP_CTX_KEY = "ProfilerControlPanel.RenameSnapshot.HelpCtx"; // NOI18N
    private static final HelpCtx RENAME_SNAPSHOT_HELP_CTX = new HelpCtx(RENAME_SNAPSHOT_HELP_CTX_KEY);
    private static ProfilerControlPanel2 defaultInstance;
    private static final Image windowIcon = Icons.getImage(ProfilerIcons.WINDOW_CONTROL_PANEL);
    private static final Icon cpuIcon = Icons.getIcon(ProfilerIcons.CPU);
    private static final Icon fragmentIcon = Icons.getIcon(ProfilerIcons.FRAGMENT);
    private static final Icon memoryIcon = Icons.getIcon(ProfilerIcons.MEMORY);
    private static final Icon heapDumpIcon = Icons.getIcon(ProfilerIcons.HEAP_DUMP);
    private static final Icon emptyIcon = Icons.getIcon(GeneralIcons.EMPTY);
    private static final String ID = "profiler_cp"; // NOI18N // for winsys persistence
    private static final Integer EXTERNALIZABLE_VERSION_WITH_SNAPSHOTS = Integer.valueOf(3);
    
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
    private boolean initialized = false;
    private Listener listener;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ProfilerControlPanel2() {
        setName(Bundle.LAB_ControlPanelName());
        setIcon(windowIcon);
        getAccessibleContext().setAccessibleDescription(Bundle.ProfilerControlPanel2_ControlPanelAcessDescr());
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

        scrollPanel.add(spControls = new SnippetPanel(Bundle.ProfilerControlPanel2_ControlsSnippetName(), controlsSnippet), gbc);
        scrollPanel.add(spStatus = new SnippetPanel(Bundle.ProfilerControlPanel2_StatusSnippetName(), statusSnippet), gbc);
        scrollPanel.add(spResults = new SnippetPanel(Bundle.ProfilerControlPanel2_ResultsSnippetName(), resultsSnippet), gbc);
        scrollPanel.add(spSnapshots = new SnippetPanel(Bundle.ProfilerControlPanel2_SnapshotsSnippetName(), snapshotsSnippet), gbc);
        scrollPanel.add(spView = new SnippetPanel(Bundle.ProfilerControlPanel2_ViewSnippetName(), viewSnippet), gbc);
        scrollPanel.add(spBasicTelemetry = new SnippetPanel(Bundle.ProfilerControlPanel2_TelemetrySnippetName(), basicTelemetrySnippet), gbc);
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
        
        Profiler.getDefault().addProfilingStateListener(new SimpleProfilingStateAdapter() {

            @Override
            protected void update() {
                updateStatus();
            }

        });

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
        
        listener = Lookup.getDefault().lookup(Listener.class);
        listener.setDelegate(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized ProfilerControlPanel2 getDefault() {
        while (defaultInstance == null) {
            Runnable resolver = new Runnable() {
                public void run() {
                    defaultInstance = (ProfilerControlPanel2) WindowManager.getDefault().findTopComponent(ID);
                    if (defaultInstance == null) defaultInstance = new ProfilerControlPanel2();
                }
            };

            if (SwingUtilities.isEventDispatchThread()) {
                resolver.run();
                break;
            } else {
                try {
                    SwingUtilities.invokeAndWait(resolver);
                } catch (InterruptedException e) {
                    ProfilerLogger.info("InterruptedException in ProfilerControlPanel2.getDefault() [will retry]: " + e.getMessage()); // NOI18N
                } catch (Throwable t) {
                    ProfilerLogger.severe("Throwable in ProfilerControlPanel2.getDefault(): " + t.getMessage()); // NOI18N
                    break;
                }
            }
        }

        return defaultInstance;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public void setProfiledProject(Lookup.Provider project) {
        snapshotsSnippet.setDisplayedProject(project);
    }

    public String getToolTipText() {
        return Bundle.ProfilerControlPanel2_ControlPanelToolTip();
    }

    public static synchronized void closeIfOpened() {
        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (defaultInstance != null && defaultInstance.isOpened()) defaultInstance.close();
            }
        });
    }

    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }
    
    protected Component defaultFocusOwner() {
        return spControls;
    }

    public boolean needsDocking() {
        return WindowManager.getDefault().findMode(this) == null;
    }

    public void open() {
        if (needsDocking()) { // needs docking

            Mode mode = WindowManager.getDefault().findMode(Bundle.ProfilerControlPanel2_WindowMode());

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

    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);

        Integer version;

        try {
            version = (Integer) in.readObject();
        } catch (Exception e) {
            ProfilerLogger.severe("Error while deserializing Profiler CP2: " + e.getMessage()); // NOI18N
            LOGGER.log(Level.WARNING, null, e);

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

    public void updateStatus() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusSnippet.refreshStatus();
                resultsSnippet.refreshStatus();
                basicTelemetrySnippet.refreshStatus();
            }
        });
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
