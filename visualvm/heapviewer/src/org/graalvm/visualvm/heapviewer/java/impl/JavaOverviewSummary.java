/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.HeapSummary;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.HTMLTextAreaSearchUtils;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.openide.util.Exceptions;
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
    "JavaOverviewSummary_OsItemString=System",
    "JavaOverviewSummary_ArchitectureItemString=Architecture:",
    "JavaOverviewSummary_JavaHomeItemString=Java Home:",
    "JavaOverviewSummary_JavaVersionItemString=Java Version:",
    "JavaOverviewSummary_JvmItemString=Java Name:",
    "JavaOverviewSummary_JavaVendorItemString=Java Vendor:",
    "JavaOverviewSummary_VmArgsSection=JVM Arguments",
    "JavaOverviewSummary_ModulesSection=Enabled Modules",
    "JavaOverviewSummary_SysPropsSection=System Properties",
    "JavaOverviewSummary_LinkShow=show",
    "JavaOverviewSummary_LinkHide=hide",
    "JavaOverviewSummary_UptimeItemString=JVM Uptime:",
    "JavaOverviewSummary_FORMAT_hms={0} hrs {1} min {2} sec",
    "JavaOverviewSummary_FORMAT_ms={0} min {1} sec",
    "JavaOverviewSummary_NotAvailable=n/a",
    "JavaOverviewSummary_NameColumn=Name",
    "JavaOverviewSummary_ValueColumn=Value",
    "JavaOverviewSummary_NoJvmArguments=<no JVM arguments>"
})
class JavaOverviewSummary extends HeapView {
    
    private final Object[][] heapData;
    private final Object[][] environmentData;
    
    private final String vmArgsData;
    private final String modulesData;
    private final String syspropsData;
    
    private JComponent component;
    
    
    private JavaOverviewSummary(HeapContext context) {
        super(Bundle.JavaOverviewSummary_Name(), Bundle.JavaOverviewSummary_Description());
        
        Heap heap = context.getFragment().getHeap();        
        Properties sysprops = heap.getSystemProperties();
        
        heapData = computeHeapData(heap);
        environmentData = computeEnvironmentData(heap, sysprops);
        vmArgsData = computeVMArgs(heap);
        modulesData = computeModules(heap);
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
        
        Snippet modulesSnippet = modulesData == null ? null : new Snippet(Bundle.JavaOverviewSummary_ModulesSection(), modulesData);
        Snippet vmArgsSnippet = vmArgsData == null ? null : new Snippet(Bundle.JavaOverviewSummary_VmArgsSection(), vmArgsData);
        Snippet syspropsSnippet = new Snippet(Bundle.JavaOverviewSummary_SysPropsSection(), syspropsData);
        syspropsSnippet.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        
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
        if (modulesSnippet != null) component.add(modulesSnippet);
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

            String name_ver = sysprops.getProperty("java.vm.version", ""); // NOI18N
            String version = sysprops.getProperty("java.version", Bundle.JavaOverviewSummary_NotAvailable()); // NOI18N
            if ("0".equals(version)) version = Bundle.JavaOverviewSummary_NotAvailable(); // NOI18N
            else {
                String relDate = sysprops.getProperty("java.version.date", "");         // NOI18N
                if (!relDate.isEmpty()) version += " " + relDate;
                if (name_ver.contains("LTS")) version += " LTS";           // NOI18N
            }

            String name = sysprops.getProperty("java.vm.name", Bundle.JavaOverviewSummary_NotAvailable()); // NOI18N
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
    
    private static String computeSyspropsData(Properties sysprops) {
        if (sysprops == null) return null;
//        if (sysprops == null) return new Object[][] { { "System properties not available", "" }};
        
        Set<Map.Entry<Object, Object>> entries = new TreeSet(new Comparator<Map.Entry<Object, Object>>() {
            @Override
            public int compare(Map.Entry<Object, Object> e1, Map.Entry<Object, Object> e2) {
                return e1.getKey().toString().compareTo(e2.getKey().toString());
            }
        });
        entries.addAll(sysprops.entrySet());
        
        boolean oddRow = false;
        Color oddRowBackground = UIUtils.getDarker(
                                 UIUtils.getProfilerResultsBackground());
        String oddRowBackgroundString =
               "rgb(" + oddRowBackground.getRed() + "," + //NOI18N
                        oddRowBackground.getGreen() + "," + //NOI18N
                        oddRowBackground.getBlue() + ")"; //NOI18N
        
        StringBuilder sb = new StringBuilder("<table border='0' cellpadding='2' cellspacing='0' width='100%'>"); // NOI18N
        
        for (Map.Entry<Object, Object> entry : entries) {
            sb.append(oddRow ?
                "<tr><td style='background-color: " + // NOI18N
                oddRowBackgroundString + ";'>" : "<tr><td>"); // NOI18N
            oddRow = !oddRow;
            
            String key = entry.getKey().toString();
            String val = entry.getValue() == null ? null : entry.getValue().toString();
            
            if (val != null) {
                if ("line.separator".equals(key)) {  // NOI18N
                    val = val.replace("\n", "\\n"); // NOI18N
                    val = val.replace("\r", "\\r"); // NOI18N
                }
                
                sb.append("<b>"); // NOI18N
                sb.append(key);
                sb.append("</b>=");   // NOI18N
                sb.append(val);
            } else {
                sb.append("<b>"); // NOI18N
                sb.append(key);
                sb.append("</b>");   // NOI18N
            }
            
            sb.append("</td></tr>"); // NOI18N
        }
        
        sb.append("</table>"); // NOI18N
        
        return expandInvalidXMLChars(sb);
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
    
    private String computeModules(Heap heap) {
        JavaClass resolvedModulesClass = heap.getJavaClassByName("java.lang.module.ResolvedModule"); // NOI18N
        if (resolvedModulesClass != null) {
            SortedSet<String> resolvedModules = new TreeSet(String.CASE_INSENSITIVE_ORDER);
            List<Instance> modules = resolvedModulesClass.getInstances();

            for (Instance module : modules) {
                resolvedModules.add(DetailsSupport.getDetailsString(module));
            }
            if (resolvedModules.isEmpty()) return null;
            return formatModules(resolvedModules);
        }
        return null;
    }

    private String computeVMArgs(Heap heap) {
        List<String> vmArgsList = new ArrayList();
        JavaClass vmManagementClass = heap.getJavaClassByName("sun.management.VMManagementImpl"); // NOI18N

        if (vmManagementClass != null) {
            if (vmManagementClass.getInstancesCount()>0) {
                Instance vmManagement = vmManagementClass.getInstancesIterator().next();
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

                            vmArgsList.add(DetailsSupport.getDetailsString(arg));
                        }
                        
                        return vmArgsList.isEmpty() ? Bundle.JavaOverviewSummary_NoJvmArguments() :
                                                      formatVMArgs(vmArgsList);
                    }
                }
            }
        }
        return null;
    }

    private String formatModules(Collection<String> data) {
        boolean oddRow = false;
        Color oddRowBackground = UIUtils.getDarker(
                                 UIUtils.getProfilerResultsBackground());
        String oddRowBackgroundString =
               "rgb(" + oddRowBackground.getRed() + "," + //NOI18N
                        oddRowBackground.getGreen() + "," + //NOI18N
                        oddRowBackground.getBlue() + ")"; //NOI18N
        StringBuilder sb = new StringBuilder("<table border='0' cellpadding='2' cellspacing='0' width='100%'>"); // NOI18N

        for (String string : data) {
            sb.append(oddRow ?
                "<tr><td style='background-color: " + // NOI18N
                oddRowBackgroundString + ";'>" : "<tr><td>"); // NOI18N
            oddRow = !oddRow;
            sb.append(string.replace(" ", "&nbsp;"));   // NOI18N
            sb.append("</td></tr>"); // NOI18N
        }
        sb.append("</table>"); // NOI18N
        return expandInvalidXMLChars(sb);
    }
    
    private final String formatVMArgs(List<String> data) {
        boolean oddRow = false;
        Color oddRowBackground = UIUtils.getDarker(
                                 UIUtils.getProfilerResultsBackground());
        String oddRowBackgroundString =
               "rgb(" + oddRowBackground.getRed() + "," + //NOI18N
                        oddRowBackground.getGreen() + "," + //NOI18N
                        oddRowBackground.getBlue() + ")"; //NOI18N
        
        StringBuilder sb = new StringBuilder("<table border='0' cellpadding='2' cellspacing='0' width='100%'>"); // NOI18N
        
        for (String string : data) {
            sb.append(oddRow ?
                "<tr><td style='background-color: " + // NOI18N
                oddRowBackgroundString + ";'>" : "<tr><td>"); // NOI18N
            oddRow = !oddRow;
            
            String key = string;
            
            int equals = key.indexOf('='); // NOI18N
            if (equals > 0) {
                key = string.substring(0, equals);
                String val = string.substring(equals + 1); // ??
                
                sb.append("<b>"); // NOI18N
                sb.append(key);
                sb.append("</b>=");   // NOI18N
                sb.append(val);
            } else {
                sb.append("<b>"); // NOI18N
                sb.append(key);
                sb.append("</b>");   // NOI18N
            }
            
            sb.append("</td></tr>"); // NOI18N
        }
        
        sb.append("</table>"); // NOI18N
        
        return expandInvalidXMLChars(sb);
    }
    
    private static String expandInvalidXMLChars(CharSequence chars) {
        StringBuilder text = new StringBuilder(chars.length());
        char ch;
        
        for (int i = 0; i < chars.length(); i++) {
            ch = chars.charAt(i);
            text.append(isValidXMLChar(ch) ? ch :
                    "&lt;0x" + Integer.toHexString(0x10000 | ch).substring(1).toUpperCase() + "&gt;"); // NOI18N
        }
        
        return text.toString();
    }
    
    private static boolean isValidXMLChar(char ch) {
        return (ch == 0x9 || ch == 0xA || ch == 0xD ||
              ((ch >= 0x20) && (ch <= 0xD7FF)) ||
              ((ch >= 0xE000) && (ch <= 0xFFFD)) ||
              ((ch >= 0x10000) && (ch <= 0x10FFFF)));
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

        if (sHours.isEmpty()) {
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

    private static class Snippet extends JPanel {
        
        Snippet(String label, final String data) {
            super(new GridBagLayout());
            
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 5, 15, 5));
            
            JLabel caption = new JLabel(label);
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
                HTMLTextArea htmlArea = new HTMLTextArea(data);
                
                final JPanel view = new JPanel(new BorderLayout());
                view.setOpaque(false);
                view.add(new HorizontalScroller(htmlArea), BorderLayout.CENTER);
                view.add(HTMLTextAreaSearchUtils.createSearchPanel(htmlArea), BorderLayout.SOUTH);

                LinkButton lb = new LinkButton() {
                    {
                        clicked(); // sets link text, hides properties table
                    }
                    @Override
                    protected void clicked() {
                        if (view.isVisible()) {
                            setText(Bundle.JavaOverviewSummary_LinkShow());
                            view.setVisible(false);
                        } else {
                            setText(Bundle.JavaOverviewSummary_LinkHide());
                            view.setVisible(true);
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
                add(view, c);
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
    
    private static class HorizontalScroller extends JScrollPane {
        
        HorizontalScroller(JComponent view) {
            super(view, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_AS_NEEDED);

            setBorder(BorderFactory.createEmptyBorder());
            setViewportBorder(BorderFactory.createEmptyBorder());

            getViewport().setOpaque(false);
            setOpaque(false);
            
            super.addMouseWheelListener(new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (e.getModifiers() == MouseWheelEvent.SHIFT_MASK) {
                        scroll(getHorizontalScrollBar(), e);
                    } else {
                        getParent().dispatchEvent(e);
                    }
                }
                
            });
        }
        
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }
        
        public Dimension getPreferredSize() {
            Dimension size = getViewport().getView().getPreferredSize();
            if (getHorizontalScrollBar().isVisible()) size.height += getHorizontalScrollBar().getPreferredSize().height;
            return size;
        }
        
        public void addMouseWheelListener(MouseWheelListener l) {}
        
        private static void scroll(JScrollBar scroller, MouseWheelEvent event) {
            if (event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                int unitsToScroll = event.getUnitsToScroll();
                if (unitsToScroll != 0) {
                    int direction = unitsToScroll < 0 ? -1 : 1;
                    int increment = scroller.getUnitIncrement(direction);
    //                int amount = event.getScrollAmount();
                    int amount = 1;
                    int oldValue = scroller.getValue();
                    int newValue = oldValue + increment * amount * direction;
                    if (oldValue != newValue) scroller.setValue(newValue);
                }
                event.consume();
            }
        }
        
        protected JViewport createViewport() {
            return new JViewport() {
                public void scrollRectToVisible(Rectangle aRect) {
                    if (getView() instanceof JTextComponent) {
                        try {
                            JTextComponent tc = (JTextComponent)getView();
                            
                            Caret caret = tc.getCaret();
                            Rectangle selStart = tc.modelToView(Math.min(caret.getDot(), caret.getMark()));
                            Rectangle selEnd = tc.modelToView(Math.max(caret.getDot(), caret.getMark()));
                            
                            int x = Math.min(selStart.x, selEnd.x);
                            int xx = Math.max(selStart.x + selStart.width, selEnd.x + selEnd.width);
                            int y = Math.min(selStart.y, selEnd.y);
                            int yy = Math.max(selStart.y + selStart.height, selEnd.y + selEnd.height);
                            Rectangle r = new Rectangle(x, y, xx - x, yy - y);
                            
                            super.scrollRectToVisible(SwingUtilities.convertRectangle(tc, r, this));
                        } catch (BadLocationException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    } else {
                        super.scrollRectToVisible(aRect);
                    }
                    
                    aRect = SwingUtilities.convertRectangle(this, aRect, getParent());
                    ((JComponent)getParent()).scrollRectToVisible(aRect);
                }
            };
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
