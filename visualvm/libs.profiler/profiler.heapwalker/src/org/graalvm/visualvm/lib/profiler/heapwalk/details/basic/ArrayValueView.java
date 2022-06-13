/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.basic;

import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.ExportAction;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.StringDecoder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import static org.graalvm.visualvm.lib.profiler.heapwalk.details.basic.ArrayValueView.Type.*;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ArrayValueView_Truncated=... <truncated>",                                     // NOI18N
    "ArrayValueView_Value=Value:",                                                  // NOI18N
    "ArrayValueView_Items=Array items:",                                            // NOI18N
    "ArrayValueView_All=Show All",                                                  // NOI18N
    "ArrayValueView_Save=Save to File",                                             // NOI18N
    "ArrayValueView_OutOfMemory=Out of memory - value too long."                    // NOI18N
})
final class ArrayValueView extends DetailsProvider.View implements Scrollable, ExportAction.ExportProvider {
    
    private static final int MAX_PREVIEW_LENGTH = 256;
    private static final int MAX_ARRAY_ITEMS = 1000;
    private static final int MAX_CHARARRAY_ITEMS = 500000;
    private static final String TRUNCATED = Bundle.ArrayValueView_Truncated();
    enum Type {STRING, STRING_BUILDER, PRIMITIVE_ARRAY};
    
    private final String className;
    
    private JTextArea view;
    private JButton all;
    
    private String caption;
    private Heap heap;
    private List<String> values;
    private byte coder = -1;
    private String separator;
    private int offset;
    private int count;
    private boolean truncated;
    private boolean chararray;
    private boolean bytearray;
    private String instanceIdentifier;
    private Type type;
    
    protected ArrayValueView(String className, Instance instance) {
        super(instance);
        this.className = className;
    }

    protected void computeView(Instance instance) {
        JavaClass javaClass = instance.getJavaClass();
        String clsName = javaClass.getName();
        heap = javaClass.getHeap();
        if (StringDetailsProvider.STRING_MASK.equals(className)) {                  // String
            separator = "";                                                         // NOI18N
            offset = DetailsUtils.getIntFieldValue(instance, "offset", 0);          // NOI18N
            count = DetailsUtils.getIntFieldValue(instance, "count", -1);           // NOI18N
            coder = DetailsUtils.getByteFieldValue(instance, "coder", (byte) -1);   // NOI18N
            values = DetailsUtils.getPrimitiveArrayFieldValues(instance, "value");  // NOI18N
            caption = Bundle.ArrayValueView_Value();
            type = STRING;
        } else if (StringDetailsProvider.BUILDERS_MASK.equals(className)) {         // AbstractStringBuilder+
            separator = "";                                                         // NOI18N
            offset = 0;
            count = DetailsUtils.getIntFieldValue(instance, "count", -1);           // NOI18N
            coder = DetailsUtils.getByteFieldValue(instance, "coder", (byte) -1);   // NOI18N
            values = DetailsUtils.getPrimitiveArrayFieldValues(instance, "value");  // NOI18N
            caption = Bundle.ArrayValueView_Value();
            type = STRING_BUILDER;
        } else if (instance instanceof PrimitiveArrayInstance) {                    // Primitive array
            chararray = "char[]".equals(clsName);                       // NOI18N
            bytearray = "byte[]".equals(clsName);                       // NOI18N
            separator = chararray ? "" : ", ";                                      // NOI18N
            offset = 0;
            values = DetailsUtils.getPrimitiveArrayValues(instance);
            count = values == null ? 0 : values.size();
            caption = Bundle.ArrayValueView_Items();
            type = PRIMITIVE_ARRAY;
        }
        instanceIdentifier=clsName+"#"+instance.getInstanceNumber(); // NOI18N
        final String preview = getString(true);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                setBackground(UIUtils.getProfilerResultsBackground());
                setOpaque(true);
                
                removeAll();
                
                JLabel l = new JLabel(caption, JLabel.LEADING);
                l.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
                add(l, BorderLayout.NORTH);
                
                view = new JTextArea();
                if (bytearray) {
                    Font defaultFont = view.getFont();
                    view.setFont(new Font(Font.MONOSPACED, Font.PLAIN, defaultFont.getSize()));
                }
                l.setLabelFor(view);
                view.setEditable(false);
                view.setLineWrap(true);
                view.setWrapStyleWord(true);
                view.setText(preview);
                try { view.setCaretPosition(0); } catch (IllegalArgumentException e) {}
                
                JScrollPane viewScroll = new JScrollPane(view,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                add(viewScroll, BorderLayout.CENTER);
                
                JPanel p = new JPanel(new GridBagLayout());
                p.setOpaque(false);
                
                all = htmlButton(Bundle.ArrayValueView_All(), truncated && count < (chararray ? MAX_CHARARRAY_ITEMS : MAX_ARRAY_ITEMS), new Runnable() {
                    public void run() { showAll(); }
                });
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.insets = new Insets(3, 0, 0, 5);
                p.add(all, c);
                
                JButton save = htmlButton(Bundle.ArrayValueView_Save(), !preview.isEmpty(), new Runnable() {
                    public void run() {
                        new ExportAction(ArrayValueView.this).actionPerformed(null);
                    }
                });
                c = new GridBagConstraints();
                c.gridx = 1;
                c.insets = new Insets(3, 0, 0, 0);
                p.add(save, c);
                
                JPanel f = new JPanel(null);
                f.setOpaque(false);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.weightx = 1;
                c.fill = GridBagConstraints.HORIZONTAL;
                p.add(f, c);
                
                add(p, BorderLayout.SOUTH);
                
                revalidate();
                doLayout();
                repaint();
            }
        });
    }
    
    private void showAll() {
        all.setEnabled(false);
        view.setEnabled(false);
        BrowserUtils.performTask(new Runnable() {
            public void run() {
                String _preview = null;
                try {
                    _preview = getString(false);
                } catch (OutOfMemoryError e) {
                    ProfilerDialogs.displayError(Bundle.ArrayValueView_OutOfMemory());
                    return;
                }
                
                final String preview = _preview;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            view.setText(preview);
                            try { view.setCaretPosition(0); } catch (IllegalArgumentException e) {}
                            view.setEnabled(true);
                        } catch (OutOfMemoryError e) {
                            ProfilerDialogs.displayError(Bundle.ArrayValueView_OutOfMemory());
                        }
                    }
                });
            }
        });
    }
    
    
    private String getString(boolean preview) {
        if (values == null) return "";                                              // NOI18N
        if (bytearray) return getHexDump(preview);
        StringDecoder decoder = new StringDecoder(heap, coder, values);
        int valuesCount = count < 0 ? decoder.getStringLength() - offset : count;            
        int separatorLength = separator == null ? 0 : separator.length();
        int estimatedSize = (int)Math.min((long)valuesCount * (2 + separatorLength), MAX_PREVIEW_LENGTH + TRUNCATED.length());
        StringBuilder value = new StringBuilder(estimatedSize);
        int lastValue = offset + valuesCount - 1;
        for (int i = offset; i <= lastValue; i++) {
            value.append(decoder.getValueAt(i));
            if (preview && value.length() >= MAX_PREVIEW_LENGTH) {
                value.append(TRUNCATED);
                truncated = true;
                break;
            }
            if (separator != null && i < lastValue) value.append(separator);
        }
        return value.toString();
    }
    
    private static final int LINE_LEN = 0x10;

    private String getHexDump(boolean preview) {
        StringBuilder value = new StringBuilder();
        StringBuilder chars = new StringBuilder();
        int lastValue = count - 1;
        for (int i = 0; i <= lastValue; i++) {
            if (i%LINE_LEN == 0) {
                if (i != 0) {
                    value.append(getPrintableChars(chars));
                    value.append("\n");
                    chars = new StringBuilder();
                }
                if (preview && i >= MAX_PREVIEW_LENGTH) {
                    truncated = true;
                    break;
                }
                value.append(String.format("%04X  ", i));
            }
            byte val = Byte.parseByte(values.get(i));
            value.append(String.format("%02X ", val));
            chars.append((char)val);
        }
        if (chars.length() > 0) {
            char[] spaces = new char[(LINE_LEN-chars.length())*3];
            Arrays.fill(spaces, ' ');
            value.append(spaces);
            value.append(getPrintableChars(chars));
        }
        return value.toString();
    }

    private static final Pattern REGEXP = Pattern.compile("\\P{Print}");

    private String getPrintableChars(StringBuilder chars) {
        StringBuilder val = new StringBuilder();
        val.append("   |");
        val.append(REGEXP.matcher(chars.toString()).replaceAll("."));
        val.append("|");
        return val.toString();
    }

    public Dimension getPreferredScrollableViewportSize() {
        return null;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        // Scroll almost one screen
        Container parent = getParent();
        if ((parent == null) || !(parent instanceof JViewport)) return 50;
        return (int)(((JViewport)parent).getHeight() * 0.95f);
    }

    public boolean getScrollableTracksViewportHeight() {
        // Allow dynamic vertical enlarging of the panel but request the vertical scrollbar when needed
        Container parent = getParent();
        if ((parent == null) || !(parent instanceof JViewport)) return false;
        return getMinimumSize().height < ((JViewport)parent).getHeight();
    }

    public boolean getScrollableTracksViewportWidth() {
        // Allow dynamic horizontal enlarging of the panel but request the vertical scrollbar when needed
        Container parent = getParent();
        if ((parent == null) || !(parent instanceof JViewport)) return false;
        return getMinimumSize().width < ((JViewport)parent).getWidth();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }
    
    
    private static JButton htmlButton(final String text, final boolean enabled, final Runnable handler) {
        JButton b = new JButton() {
            public void setEnabled(boolean b) {
                setText(!b ? text : "<html><nobr><a href='#'>" + text + "</a></nobr></html>"); // NOI18N
                setCursor(!b ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                super.setEnabled(b);
            }
        };
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setEnabled(enabled);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { handler.run(); }
        });
        return b;
    }

    @Override
    public void exportData(int exportedFileType, ExportDataDumper eDD) {
        String comma = ","; // NOI18N
        if (values != null) {
            StringDecoder decoder = new StringDecoder(heap, coder, values);
            int valuesCount = count < 0 ? decoder.getStringLength() - offset : count;            
            int lastValue = offset + valuesCount - 1;
            for (int i = offset; i <= lastValue; i++) {
                String value = decoder.getValueAt(i);
                
                switch (exportedFileType) {
                    case ExportAction.MODE_CSV:
                        eDD.dumpData(value);
                        eDD.dumpData(comma);
                        break;
                    case ExportAction.MODE_TXT:
                        eDD.dumpData(value);
                        break;
                    case ExportAction.MODE_BIN:
                        byte b = Byte.valueOf(value);
                        eDD.dumpByte(b);
                        break;
                    default:
                        throw new IllegalArgumentException(); //Illegal export type
                }
            }
        }
        eDD.close();
    }

    @Override
    public String getViewName() {
        return instanceIdentifier;
    }

    @Override
    public boolean hasRawData() {
        return type.equals(PRIMITIVE_ARRAY);
    }

    @Override
    public boolean hasBinaryData() {
        return bytearray;
    }

    @Override
    public boolean hasText() {
        switch (type) {
            case STRING:
            case STRING_BUILDER:
                return true;
            case PRIMITIVE_ARRAY:
                return chararray;
        }
        throw new IllegalArgumentException(type.toString());
    }

    @Override
    public boolean isExportable() {
        return hasText() || hasBinaryData() || hasRawData();
    }
    
}
