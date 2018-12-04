/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.java.impl;

import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.swing.LinkButton;
import org.graalvm.visualvm.heapviewer.swing.Splitter;
import org.graalvm.visualvm.heapviewer.ui.HeapView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.SummaryView;
import org.graalvm.visualvm.uisupport.SeparatorLine;
import org.graalvm.visualvm.uisupport.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.HeapSummary;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NormalBoldGrayRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaOverviewSummary_Name=Overview",
    "JavaOverviewSummary_Description=Overview",
    "JavaOverviewSummary_HeapSection=Heap",
    "JavaOverviewSummary_EnvironmentSection=Environment",
    "JavaOverviewSummary_NotAvailableMsg=&lt;not available&gt;",
    "JavaOverviewSummary_SystemPropertiesString=System Properties:",
    "JavaOverviewSummary_SummaryString=Basic Info:",
    "JavaOverviewSummary_EnvironmentString=Environment:",
    "JavaOverviewSummary_SizeItemString=Size:",
    "JavaOverviewSummary_ClassesItemString=Classes:",
    "JavaOverviewSummary_InstancesItemString=Instances:",
    "JavaOverviewSummary_ClassloadersItemString=Classloaders:",
    "JavaOverviewSummary_GcRootsItemString=GC Roots:",
    "JavaOverviewSummary_FinalizersItemString=Objects Pending for Finalization:",
//    "JavaOverviewSummary_OOMELabelString=<b>Heap dumped on OutOfMemoryError exception</b>",
//    "JavaOverviewSummary_OOMEItemString=<b>Thread Causing OutOfMemoryError Exception: </b>{0}",
    "JavaOverviewSummary_OsItemString=System",
    "JavaOverviewSummary_ArchitectureItemString=Architecture:",
    "JavaOverviewSummary_JavaHomeItemString=Java Home:",
    "JavaOverviewSummary_JavaVersionItemString=Java Version:",
    "JavaOverviewSummary_JvmItemString=Java Name:",
    "JavaOverviewSummary_JavaVendorItemString=Java Vendor:",
    "JavaOverviewSummary_VmArgsSection=JVM Arguments",
    "JavaOverviewSummary_SysPropsSection=System Properties",
    "JavaOverviewSummary_LinkShow=show",
    "JavaOverviewSummary_LinkHide=hide",
    "JavaOverviewSummary_UptimeItemString=JVM Uptime:",
    "JavaOverviewSummary_FORMAT_hms={0} hrs {1} min {2} sec",
    "JavaOverviewSummary_FORMAT_ms={0} min {1} sec",
    "JavaOverviewSummary_NotAvailable=n/a",
    "JavaOverviewSummary_NameColumn=Name",
    "JavaOverviewSummary_ValueColumn=Value"
})
class JavaOverviewSummary extends HeapView {
    
    private final Object[][] heapData;
    private final Object[][] environmentData;
    private final Object[][] vmArgsData;
    private final Object[][] syspropsData;
    
    private JComponent component;
    
    
    private JavaOverviewSummary(HeapContext context) {
        super(Bundle.JavaOverviewSummary_Name(), Bundle.JavaOverviewSummary_Description());
        
        Heap heap = context.getFragment().getHeap();        
        Properties sysprops = heap.getSystemProperties();
        
        heapData = computeHeapData(heap);
        environmentData = computeEnvironmentData(heap, sysprops);
        vmArgsData = computeVMArgs(heap);
        syspropsData = computeSyspropsData(sysprops);
    }
    

    @Override
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    @Override
    public ProfilerToolbar getToolbar() {
        return null;
    }
    
    
    private void init() {
        ResultsSnippet heapSnippet = new ResultsSnippet(Bundle.JavaOverviewSummary_HeapSection(), heapData, 0);
        ResultsSnippet environmentSnippet = new ResultsSnippet(Bundle.JavaOverviewSummary_EnvironmentSection(), environmentData, 1);
        Splitter overviewRow = new Splitter(Splitter.HORIZONTAL_SPLIT, heapSnippet, environmentSnippet);
        
        VMArgsSnippet vmArgsSnippet = vmArgsData == null ? null : new VMArgsSnippet(vmArgsData);
        SyspropsSnippet syspropsSnippet = new SyspropsSnippet(syspropsData);
        
        component = new JPanel(new VerticalLayout(false)) {
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.width = 0;
                return dim;
            }

            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width = 100;
                return dim;
            }
        };
        component.setOpaque(false);
        
        component.add(overviewRow);
        if (vmArgsSnippet != null) component.add(vmArgsSnippet);
        component.add(syspropsSnippet);
    }
    
    
    private static Object[][] computeHeapData(Heap heap) {
        NumberFormat numberFormat = (NumberFormat)NumberFormat.getInstance().clone();
        numberFormat.setMaximumFractionDigits(1);
        
        HeapSummary hsummary = heap.getSummary();
        
        int nclassloaders;
        JavaClass cl = heap.getJavaClassByName("java.lang.ClassLoader"); // NOI18N
        if (cl != null) {
            nclassloaders = cl.getInstancesCount();
            Collection<JavaClass> jcs = cl.getSubClasses();
            for (JavaClass jc : jcs) nclassloaders += jc.getInstancesCount();
        } else {
            nclassloaders = -1;
        }
        
        int gcroots = heap.getGCRoots().size();
        if (gcroots < 1) gcroots = -1;
        
        return new Object[][] { { Bundle.JavaOverviewSummary_SizeItemString(), format(hsummary.getTotalLiveBytes(), numberFormat, " B") },
                                { Bundle.JavaOverviewSummary_ClassesItemString(), format(heap.getAllClasses().size(), numberFormat, null) },
                                { Bundle.JavaOverviewSummary_InstancesItemString(), format(hsummary.getTotalLiveInstances(), numberFormat, null) },
                                { Bundle.JavaOverviewSummary_ClassloadersItemString(), format(nclassloaders, numberFormat, null) },
                                { Bundle.JavaOverviewSummary_GcRootsItemString(), format(gcroots, numberFormat, null) },
                                { Bundle.JavaOverviewSummary_FinalizersItemString(), format(computeFinalizers(heap), numberFormat, null) }};
    }
    
    private static Object[][] computeEnvironmentData(Heap heap, Properties sysprops) {
        HeapSummary hsummary = heap.getSummary();
        
        long startupTime = computeStartupTime(heap);
        String uptime = startupTime == -1 ? format(startupTime, null, null) :
                                            getTime(hsummary.getTime() - startupTime);
        
        if (sysprops == null) {
            return new Object[][] { { Bundle.JavaOverviewSummary_OsItemString(), Bundle.JavaOverviewSummary_NotAvailable() },
                                    { Bundle.JavaOverviewSummary_ArchitectureItemString(), Bundle.JavaOverviewSummary_NotAvailable() },
                                    { Bundle.JavaOverviewSummary_JavaHomeItemString(), Bundle.JavaOverviewSummary_NotAvailable() },
                                    { Bundle.JavaOverviewSummary_JavaVersionItemString(), Bundle.JavaOverviewSummary_NotAvailable() },
                                    { Bundle.JavaOverviewSummary_JvmItemString(), Bundle.JavaOverviewSummary_NotAvailable() },
                                    { Bundle.JavaOverviewSummary_JavaVendorItemString(), Bundle.JavaOverviewSummary_NotAvailable() },
                                    { Bundle.JavaOverviewSummary_UptimeItemString(), uptime }};
        } else {
            String os = sysprops.getProperty("os.name", Bundle.JavaOverviewSummary_NotAvailable()); // NOI18N
            String os_ver = sysprops.getProperty("os.version", ""); // NOI18N
            if (!os_ver.isEmpty()) os += " (" + os_ver + ")"; // NOI18N
            String os_patch = sysprops.getProperty("sun.os.patch.level", ""); // NOI18N
            if (!os_patch.isEmpty() && !"unknown".equals(os_patch)) os += " " + os_patch; // NOI18N

            String arch = sysprops.getProperty("os.arch", Bundle.JavaOverviewSummary_NotAvailable()); // NOI18N
            arch += " " + sysprops.getProperty("sun.arch.data.model", "?") + "bit"; // NOI18N

            String home = sysprops.getProperty("java.home", Bundle.JavaOverviewSummary_NotAvailable()); // NOI18N

            String version = sysprops.getProperty("java.version", Bundle.JavaOverviewSummary_NotAvailable()); // NOI18N
            if ("0".equals(version)) version = Bundle.JavaOverviewSummary_NotAvailable(); // NOI18N

            String name = sysprops.getProperty("java.vm.name", Bundle.JavaOverviewSummary_NotAvailable()); // NOI18N
            String name_ver = sysprops.getProperty("java.vm.version", ""); // NOI18N
            String name_info = sysprops.getProperty("java.vm.info", ""); // NOI18N
            if (name_ver.isEmpty() || name_info.isEmpty()) {
                if (name_ver.isEmpty()) name += " (" + name_info + ")"; // NOI18N
                else name += " (" + name_ver + ")"; // NOI18N
            } else {
                name += " (" + name_ver + ", " + name_info + ")"; // NOI18N
            }

            String vendor = sysprops.getProperty("java.vendor", Bundle.JavaOverviewSummary_NotAvailable()); // NOI18N

            return new Object[][] { { Bundle.JavaOverviewSummary_OsItemString(), os },
                                    { Bundle.JavaOverviewSummary_ArchitectureItemString(), arch },
                                    { Bundle.JavaOverviewSummary_JavaHomeItemString(), home },
                                    { Bundle.JavaOverviewSummary_JavaVersionItemString(), version },
                                    { Bundle.JavaOverviewSummary_JvmItemString(), name },
                                    { Bundle.JavaOverviewSummary_JavaVendorItemString(), vendor },
                                    { Bundle.JavaOverviewSummary_UptimeItemString(), uptime }};
        }
        
    }
    
    private static Object[][] computeSyspropsData(Properties sysprops) {
        if (sysprops == null) return null;
//        if (sysprops == null) return new Object[][] { { "System properties not available", "" }};
        
        Set<Map.Entry<Object, Object>> entries = new TreeSet(new Comparator<Map.Entry<Object, Object>>() {
            @Override
            public int compare(Map.Entry<Object, Object> e1, Map.Entry<Object, Object> e2) {
                return e1.getKey().toString().compareTo(e2.getKey().toString());
            }
        });
        entries.addAll(sysprops.entrySet());
        
        int idx = 0;
        Object[][] data = new Object[entries.size()][2];
        for (Map.Entry<Object, Object> entry : entries) {
            Object key = entry.getKey();
            data[idx][0] = key;
            
            Object value = entry.getValue();
            if ("line.separator".equals(key) && value != null) {  // NOI18N
                value = value.toString().replace("\n", "\\n"); // NOI18N
                value = value.toString().replace("\r", "\\r"); // NOI18N
            }
            data[idx][1] = value;
            
            idx++;
        }
        
        return data;
    }
    
    
    private static long computeFinalizers(Heap heap) {
        JavaClass finalizerClass = heap.getJavaClassByName("java.lang.ref.Finalizer"); // NOI18N
        if (finalizerClass != null) {
            Instance queue = (Instance)finalizerClass.getValueOfStaticField("queue"); // NOI18N
            if (queue != null) {
                Long len = (Long)queue.getValueOfField("queueLength"); // NOI18N
                if (len != null) return len.longValue();
            }
        }
        return -1;
    }
    
    private static long computeStartupTime(Heap heap) {
        JavaClass jmxFactoryClass = heap.getJavaClassByName("sun.management.ManagementFactoryHelper"); // NOI18N
        if (jmxFactoryClass == null) {
            jmxFactoryClass = heap.getJavaClassByName("sun.management.ManagementFactory"); // NOI18N
        }
        if (jmxFactoryClass != null) {
            Instance runtimeImpl = (Instance)jmxFactoryClass.getValueOfStaticField("runtimeMBean"); // NOI18N
            if (runtimeImpl != null) {
                Long len = (Long)runtimeImpl.getValueOfField("vmStartupTime"); // NOI18N
                if (len != null) return len.longValue();
            }
        }
        return -1;
    }
    
    private Object[][] computeVMArgs(Heap heap) {
        List<String> vmArgsList = new ArrayList();
        JavaClass vmManagementClass = heap.getJavaClassByName("sun.management.VMManagementImpl"); // NOI18N

        if (vmManagementClass != null) {
            if (vmManagementClass.getInstancesCount()>0) {
                Instance vmManagement = (Instance) vmManagementClass.getInstancesIterator().next();
                Object vma = vmManagement.getValueOfField("vmArgs"); // NOI18N

                if (vma instanceof Instance) {
                    Instance vmargs = (Instance) vma;
                    Object list = vmargs.getValueOfField("list"); // NOI18N
                    Object arr;
                    Object size = null;

                    if (list instanceof Instance) {
                        arr = ((Instance)list).getValueOfField("a"); // NOI18N
                    } else {
                        size = vmargs.getValueOfField("size"); // NOI18N
                        arr = vmargs.getValueOfField("elementData"); // NOI18N
                    }
                    if (arr instanceof ObjectArrayInstance) {
                        ObjectArrayInstance vmArgsArr = (ObjectArrayInstance) arr;
                        int length = vmArgsArr.getLength();
                        List<Instance> elements = vmArgsArr.getValues();

                        if (size instanceof Integer) {
                            length = ((Integer)size).intValue();
                        }
                        for (int i = 0; i < length; i++) {
                            Instance arg = elements.get(i);

                            vmArgsList.add(DetailsSupport.getDetailsString(arg, heap));
                        }
                        
                        if (vmArgsList.isEmpty()) vmArgsList.add("<no JVM arguments>");
                        
                        Object[][] data = new Object[vmArgsList.size()][2];
                        for (int i = 0; i < data.length; i++)
                            data[i][0] = vmArgsList.get(i);
                        return data;
                    }
                }
            }
        }
        return null;
    }

    private static String getTime(long millis) {
        // Hours
        long hours = millis / 3600000;
        String sHours = (hours == 0 ? "" : "" + hours); // NOI18N
        millis = millis % 3600000;

        // Minutes
        long minutes = millis / 60000;
        String sMinutes = (((hours > 0) && (minutes < 10)) ? "0" + minutes : "" + minutes); // NOI18N
        millis = millis % 60000;

        // Seconds
        long seconds = millis / 1000;
        String sSeconds = ((seconds < 10) ? "0" + seconds : "" + seconds); // NOI18N

        if (sHours.length() == 0) {
            return Bundle.JavaOverviewSummary_FORMAT_ms(sMinutes, sSeconds);
        } else {
            return Bundle.JavaOverviewSummary_FORMAT_hms(sHours, sMinutes, sSeconds);
        }
    }
    
    private static String format(Number number, NumberFormat format, String suffix) {
        return number.longValue() == -1 ? Bundle.JavaOverviewSummary_NotAvailable() :
                                          format.format(number) + (suffix == null ? "" : suffix); // NOI18N
    }
    
    
    private static class ResultsSnippet extends JPanel {
        
        ResultsSnippet(String text, Object[][] data, int fillerColumn) {
            super(new BorderLayout(0, 6));
            
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
            
            add(new SectionSeparator(text), BorderLayout.NORTH);
            
            TableModel model = new DefaultTableModel(data, new Object[] { Bundle.JavaOverviewSummary_NameColumn(),
                                                                          Bundle.JavaOverviewSummary_ValueColumn() }) {
                public boolean isCellEditable(int row, int column) { return false; }
            };
            SummaryView.SimpleTable table = new SummaryView.SimpleTable(model, fillerColumn);
            table.setFocusable(false);
            LabelRenderer r1 = new LabelRenderer();
            r1.setFont(r1.getFont().deriveFont(Font.BOLD));
            table.setColumnRenderer(0, r1, fillerColumn != 0);
            LabelRenderer r2 = new LabelRenderer();
            r2.setHorizontalAlignment(LabelRenderer.RIGHT);
            table.setColumnRenderer(1, r2, fillerColumn != 1);
            add(table, BorderLayout.CENTER);
        }
        
        public Dimension getMinimumSize() {
            Dimension dim = super.getMinimumSize();
            dim.width = 0;
            return dim;
        }
        
        public Dimension getPreferredSize() {
            Dimension dim = super.getPreferredSize();
            dim.width = 100;
            return dim;
        }
        
    }
    
    private static class VMArgsSnippet extends JPanel {
        
        VMArgsSnippet(final Object[][] data) {
            super(new GridBagLayout());
            
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 5, 15, 5));
            
            JLabel caption = new JLabel(Bundle.JavaOverviewSummary_VmArgsSection());
            caption.setFont(caption.getFont().deriveFont(Font.BOLD));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weighty = 1d;
            add(caption, c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 0, 0);
            add(new JLabel("["), c); // NOI18N
            
            if (data != null) {
                TableModel model = new DefaultTableModel(data, new Object[] { Bundle.JavaOverviewSummary_NameColumn() }) {
                    public boolean isCellEditable(int row, int column) { return false; }
                };
                final SummaryView.SimpleTable vmArgs = new SummaryView.SimpleTable(model, 0);
                vmArgs.setFocusable(false);
                NormalBoldGrayRenderer r1 = new NormalBoldGrayRenderer() {
                    public void setValue(Object value, int row) {
                        if (value == null) {
                            setNormalValue(""); // NOI18N
                            setBoldValue(""); // NOI18N
                        } else {
                            String s = value.toString();
                            int i = s.indexOf('=');
                            if (i > 0) {
                                setNormalValue(s.substring(i));
                                setBoldValue(s.substring(0, i));
                            } else {
                                setNormalValue(""); // NOI18N
                                setBoldValue(s);
                            }
                        }
                    }
                    
                    private ProfilerRenderer[] valueRenderers;
                    protected ProfilerRenderer[] valueRenderers() {
                        if (valueRenderers == null) {
                            valueRenderers = super.valueRenderers();
                            if (valueRenderers != null) {
                                LabelRenderer bold = (LabelRenderer)valueRenderers[1];
                                bold.setMargin(3, 3, 3, 0);
                                LabelRenderer normal = (LabelRenderer)valueRenderers[0];
                                normal.setMargin(3, 0, 3, 3);
                                valueRenderers = new ProfilerRenderer[] { bold, normal };
                            }
                        }
                        return valueRenderers;
                    }
                };
                vmArgs.setColumnRenderer(0, r1, true);

                LinkButton lb = new LinkButton() {
                    {
                        clicked(); // sets link text, hides properties table
                    }
                    @Override
                    protected void clicked() {
                        if (vmArgs.isVisible()) {
                            setText(Bundle.JavaOverviewSummary_LinkShow());
                            vmArgs.setVisible(false);
                        } else {
                            setText(Bundle.JavaOverviewSummary_LinkHide());
                            vmArgs.setVisible(true);
                        }
                    }
                };
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = 0;
                c.insets = new Insets(0, 0, 0, 0);
                add(lb, c);
                
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1d;
                c.weighty = 1d;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.fill = GridBagConstraints.BOTH;
                c.insets = new Insets(6, 0, 0, 0);
                add(vmArgs, c);
            } else {
                JLabel nal = new JLabel(Bundle.JavaOverviewSummary_NotAvailable());
                nal.setBorder(new LinkButton().getBorder());
                c = new GridBagConstraints();
                c.gridx = 2;
                c.insets = new Insets(0, 0, 0, 0);
                add(nal, c);
            }
            
            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 0, 0);
            add(new JLabel("]"), c); // NOI18N

            c = new GridBagConstraints();
            c.gridx = 4;
            c.gridy = 0;
            c.weightx = 1d;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(1, 4, 0, 0);
            add(new SeparatorLine(), c);
        }
        
        public Dimension getMinimumSize() {
            Dimension dim = super.getMinimumSize();
            dim.width = 0;
            return dim;
        }
        
        public Dimension getPreferredSize() {
            Dimension dim = super.getPreferredSize();
            dim.width = 100;
            return dim;
        }
        
    }
    
    private static class SyspropsSnippet extends JPanel {
        
        SyspropsSnippet(final Object[][] data) {
            super(new GridBagLayout());
            
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            
            JLabel caption = new JLabel(Bundle.JavaOverviewSummary_SysPropsSection());
            caption.setFont(caption.getFont().deriveFont(Font.BOLD));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weighty = 1d;
            add(caption, c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 0, 0);
            add(new JLabel("["), c); // NOI18N
            
            if (data != null) {
                TableModel model = new DefaultTableModel(data, new Object[] { Bundle.JavaOverviewSummary_NameColumn(),
                                                                              Bundle.JavaOverviewSummary_ValueColumn() }) {
                    public boolean isCellEditable(int row, int column) { return false; }
                };
                final SummaryView.SimpleTable properties = new SummaryView.SimpleTable(model, 1);
                properties.setFocusable(false);
                LabelRenderer r1 = new LabelRenderer();
                r1.setFont(r1.getFont().deriveFont(Font.BOLD));
                properties.setColumnRenderer(0, r1, true);
                LabelRenderer r2 = new LabelRenderer();
                properties.setColumnRenderer(1, r2);

                LinkButton lb = new LinkButton() {
                    {
                        clicked(); // sets link text, hides properties table
                    }
                    @Override
                    protected void clicked() {
                        if (properties.isVisible()) {
                            setText(Bundle.JavaOverviewSummary_LinkShow());
                            properties.setVisible(false);
                        } else {
                            setText(Bundle.JavaOverviewSummary_LinkHide());
                            properties.setVisible(true);
                        }
                    }
                };
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = 0;
                c.insets = new Insets(0, 0, 0, 0);
                add(lb, c);
                
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1d;
                c.weighty = 1d;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.fill = GridBagConstraints.BOTH;
                c.insets = new Insets(6, 0, 0, 0);
                add(properties, c);
            } else {
                JLabel nal = new JLabel(Bundle.JavaOverviewSummary_NotAvailable());
                nal.setBorder(new LinkButton().getBorder());
                c = new GridBagConstraints();
                c.gridx = 2;
                c.insets = new Insets(0, 0, 0, 0);
                add(nal, c);
            }
            
            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 0, 0);
            add(new JLabel("]"), c); // NOI18N

            c = new GridBagConstraints();
            c.gridx = 4;
            c.gridy = 0;
            c.weightx = 1d;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(1, 4, 0, 0);
            add(new SeparatorLine(), c);
        }
        
        public Dimension getMinimumSize() {
            Dimension dim = super.getMinimumSize();
            dim.width = 0;
            return dim;
        }
        
        public Dimension getPreferredSize() {
            Dimension dim = super.getPreferredSize();
            dim.width = 100;
            return dim;
        }
        
    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 100)
    public static class Provider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (JavaHeapFragment.isJavaHeap(context)) return new JavaOverviewSummary(context);
            return null;
        }
        
    }
    
}
