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

package com.sun.tools.visualvm.modules.memsampler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.attach.AttachModelImpl;
import com.sun.tools.visualvm.attach.HeapHistogramImpl;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.NotSupportedDisplayer;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import com.sun.tools.visualvm.tools.attach.AttachModel;
import com.sun.tools.visualvm.tools.attach.AttachModelFactory;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.management.MemoryMXBean;
import java.text.NumberFormat;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;

/**
 *
 * @author Jiri Sedlacek
 */
class MemorySamplerView extends DataSourceView implements PropertyChangeListener, DataRemovedListener<Application>, ActionListener {

    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/memsampler/memsampler.png"; // NOI18N
    private Application application;

    private final boolean histogramAvailable;

    private Timer timer;
    private Refresher refresher;

    private MasterViewSupport masterViewSupport;
    private MemoryViewSupport heapViewSupport;
    private MemoryViewSupport permgenViewSupport;

    private HeapHistogramImpl heapHistogram;

    
    public MemorySamplerView(Application application) {
        super(application, "Memory Sampler", new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 50, false); // NOI18N

        histogramAvailable = histogramAvailable(application);

        if (histogramAvailable) {
            this.application = application;

            refresher = new Refresher() {
                public final boolean checkRefresh() { return checkRefreshImpl(); }
                public final void doRefresh() { doRefreshImpl(); }
            };

            timer = new Timer(GlobalPreferences.sharedInstance().getMonitoredDataPoll() * 1000, this);

            application.notifyWhenRemoved(this);
            application.addPropertyChangeListener(Stateful.PROPERTY_STATE, WeakListeners.propertyChange(this, application));
        }
    }

    protected void added() {
        if (!histogramAvailable) return;
        timer.start();
    }

    protected void removed() {
        if (!histogramAvailable) return;
        timer.stop();
        application = null;
        heapHistogram = null;
    }

    protected DataViewComponent createComponent() {
        if (!histogramAvailable) {
            DataViewComponent.MasterView monitoringMasterView = new DataViewComponent.MasterView("Memory Sampler", null, new NotSupportedDisplayer(NotSupportedDisplayer.JVM)); // NOI18N
            DataViewComponent.MasterViewConfiguration monitoringMasterConfiguration = new DataViewComponent.MasterViewConfiguration(true);
            
            return new DataViewComponent(monitoringMasterView, monitoringMasterConfiguration);
        } else {
            masterViewSupport = new MasterViewSupport(refresher, timer, application);
            heapViewSupport = new MemoryViewSupport(refresher, "Heap samples", 10, MemoryView.MODE_HEAP);
            permgenViewSupport = new MemoryViewSupport(refresher, "PermGen samples", 20, MemoryView.MODE_PERMGEN);

            DataViewComponent.MasterView monitoringMasterView = new DataViewComponent.MasterView("Memory Sampler", null, masterViewSupport); // NOI18N
            DataViewComponent.MasterViewConfiguration monitoringMasterConfiguration = new DataViewComponent.MasterViewConfiguration(false);
            DataViewComponent dvc = new DataViewComponent(monitoringMasterView, monitoringMasterConfiguration);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("Heap", true), DataViewComponent.TOP_LEFT);    // NOI18N
            dvc.addDetailsView(heapViewSupport.getDetailsView(), DataViewComponent.TOP_LEFT);

            dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration("PermGen", true), DataViewComponent.BOTTOM_LEFT);    // NOI18N
            dvc.addDetailsView(permgenViewSupport.getDetailsView(), DataViewComponent.BOTTOM_LEFT);
            dvc.hideDetailsArea(DataViewComponent.BOTTOM_LEFT);

            return dvc;
        }
    }

    public void dataRemoved(Application app) {
        application = null;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        dataRemoved(application);
    }

    public void actionPerformed(ActionEvent e) {
        refresher.refresh();
    }


    private static boolean histogramAvailable(Application application) {
        try {
            AttachModel aModel = AttachModelFactory.getAttachFor(application);
            if (!(aModel instanceof AttachModelImpl)) return false;
            return ((AttachModelImpl)aModel).takeHeapHistogram() != null;
        } catch (Exception e) {
            return false;
        }
    }


    private boolean checkRefreshImpl() {
        if (!timer.isRunning()) return false;
        if (application == null) {
            removed();
            return false;
        } else {
            return masterViewSupport.isShowing() ||
                   heapViewSupport.isShowing() ||
                   permgenViewSupport.isShowing();
        }
    }

    private void doRefreshImpl() {
        if (!timer.isRunning()) return;
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                try {
                    AttachModel aModel = AttachModelFactory.getAttachFor(application);
                    heapHistogram = ((AttachModelImpl)aModel).takeHeapHistogram();

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
//                            masterViewSupport.refresh(heapHistogram);
                            heapViewSupport.refresh(heapHistogram);
                            permgenViewSupport.refresh(heapHistogram);
                        }
                    });
                } catch (Exception e) {
                    timer.stop();
                }
            }
        });
    }


    // --- General data --------------------------------------------------------

    private static class MasterViewSupport extends JPanel {

        private Timer timer;

        private MemoryMXBean memoryMXBean;
        private JButton gcButton;
        private JButton heapDumpButton;


        public MasterViewSupport(final Refresher refresher, Timer timer, Application application) {
            this.timer = timer;
            
            initComponents(application);

            initActions(application);

            addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        if (isShowing()) refresher.refresh();
                    }
                }
            });
        }


        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("Memory Sampler", null, this);  // NOI18N
        }

        private void initComponents(final Application application) {
            setLayout(new BorderLayout());
            setOpaque(false);

            JPanel refreshRateContainer = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
            refreshRateContainer.setOpaque(false);
            refreshRateContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 15, 5));

            refreshRateContainer.add(new JLabel("<html><b>Refresh rate:</b></html>"));

            Integer[] refreshRates = new Integer[] { 100, 200, 500, 1000, 2000, 5000, 10000 };
            final JComboBox combo = new JComboBox(refreshRates);
            combo.setEditable(false);
            combo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timer.setDelay((Integer)combo.getSelectedItem());
                }
            });
            combo.setSelectedItem(timer.getDelay());
            combo.setRenderer(new ComboRenderer(combo));
            refreshRateContainer.add(combo);

            refreshRateContainer.add(new JLabel("msec."));


            // --- Copy-pasted from com.sun.tools.visualvm.application.views.monitor.ApplicationMonitorView
            gcButton = new JButton(new AbstractAction("Perform GC") {    // NOI18N
                public void actionPerformed(ActionEvent e) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            try { memoryMXBean.gc(); } catch (Exception e) {
                                Exceptions.printStackTrace(e);
                            }
                        };
                    });
                }
            });
            gcButton.setEnabled(false);

            heapDumpButton = new JButton(new AbstractAction("Heap Dump") {   // NOI18N
                public void actionPerformed(ActionEvent e) {
                    HeapDumpSupport.getInstance().takeHeapDump(application, (e.getModifiers() & InputEvent.CTRL_MASK) == 0);
                }
            });
            heapDumpButton.setEnabled(false);

            JPanel buttonsArea = new JPanel(new BorderLayout());
            buttonsArea.setOpaque(false);
            JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
            buttonsContainer.setOpaque(false);
            buttonsContainer.setBorder(BorderFactory.createEmptyBorder(9, 5, 20, 10));
            buttonsContainer.add(gcButton, BorderLayout.WEST);
            buttonsContainer.add(heapDumpButton, BorderLayout.EAST);
            buttonsArea.add(buttonsContainer, BorderLayout.NORTH);

            add(buttonsArea, BorderLayout.EAST);
            // -----------------------------------------------------------------


            add(refreshRateContainer, BorderLayout.WEST);
        }

        private void initActions(final Application application) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    Jvm jvm = JvmFactory.getJVMFor(application);
                    final boolean heapDumpSupported = jvm != null && jvm.isTakeHeapDumpSupported();

                    JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
                    if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                        JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                        if (mxbeans != null) memoryMXBean = mxbeans.getMemoryMXBean();
                    }
                    final boolean invokeGcSupported = memoryMXBean != null;

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            gcButton.setEnabled(invokeGcSupported);
                            heapDumpButton.setEnabled(heapDumpSupported);
                        }
                    });
                }
            });
        }

    }

    private static class ComboRenderer implements ListCellRenderer {
        
        private ListCellRenderer renderer;

        ComboRenderer(JComboBox combo) {
            renderer = combo.getRenderer();
            if (renderer instanceof JLabel)
                ((JLabel)renderer).setHorizontalAlignment(JLabel.TRAILING);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            return renderer.getListCellRendererComponent(list, NumberFormat.getInstance().format(value), index, isSelected, cellHasFocus);
        }

    }


    // --- Memory view ---------------------------------------------------------

    private static class MemoryViewSupport extends JComponent {

        private MemoryView memoryView;

        private String caption;
        private int position;


        public MemoryViewSupport(Refresher refresher, String caption, int position, int mode) {
            this.caption = caption;
            this.position = position;
            initComponents(refresher, mode);
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(caption, null, position, this, null);
        }


        public void refresh(HeapHistogramImpl histogram) {
            if (histogram == null) return;
            memoryView.refresh(histogram);
        }


        private void initComponents(final Refresher refresher, int mode) {
            setLayout(new BorderLayout());
            memoryView = new MemoryView(refresher, mode);
            add(memoryView, BorderLayout.CENTER);
        }

    }


    // --- Asynchronous refresh of the model -----------------------------------

    public static abstract class Refresher {
        private static final long REFRESH_THRESHOLD = 100;
        private long lastRefresh;

        public final void refresh() {
            if (checkRefresh()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRefresh >= REFRESH_THRESHOLD) {
                    lastRefresh = currentTime;
                    doRefresh();
                }
            }
        }

        protected boolean checkRefresh() { return true; }
        protected void doRefresh()  {}
    }


}
