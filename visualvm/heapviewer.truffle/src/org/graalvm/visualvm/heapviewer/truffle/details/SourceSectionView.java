/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.details;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.ExportAction;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.StringDecoder;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
@NbBundle.Messages({
    "SourceSectionView_Truncated=... <truncated>",                                     // NOI18N
    "SourceSectionView_Value=Value:",                                                  // NOI18N
    "SourceSectionView_All=Show All",                                                  // NOI18N
    "SourceSectionView_Save=Save to File",                                             // NOI18N
    "SourceSectionView_OutOfMemory=Out of memory - value too long."                    // NOI18N
})
public final class SourceSectionView extends DetailsProvider.View implements Scrollable, ExportAction.ExportProvider {

    private static final int MAX_PREVIEW_LENGTH = 256;
    private static final int MAX_ARRAY_ITEMS = 1000;
    private static final int MAX_CHARARRAY_ITEMS = 500000;
    private static final String TRUNCATED = Bundle.SourceSectionView_Truncated();

    private JTextPane view;
    private JButton all;

    private String caption;
    private Heap heap;
    private List<String> values;
    private byte coder = -1;
    private int offset;
    private int count;
    private int sectionOffset;
    private int sectionSize;
    private boolean chararray;
    private String instanceIdentifier;

    public SourceSectionView(String className, Instance instance, int offset, int size) {
        super(instance);
        sectionOffset = offset;
        sectionSize = size;
    }

    protected void computeView(Instance instance) {

        offset = DetailsUtils.getIntFieldValue(instance, "offset", 0);          // NOI18N
        count = DetailsUtils.getIntFieldValue(instance, "count", -1);           // NOI18N
        coder = DetailsUtils.getByteFieldValue(instance, "coder", (byte) -1);   // NOI18N
        values = DetailsUtils.getPrimitiveArrayFieldValues(instance, "value");  // NOI18N
        caption = Bundle.SourceSectionView_Value();
        heap = instance.getJavaClass().getHeap();
        instanceIdentifier=instance.getJavaClass().getName()+"#"+instance.getInstanceNumber(); // NOI18N
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

                view = new JTextPane();
                l.setLabelFor(view);
                view.setEditable(false);
                view.setText(preview);
                try { view.setCaretPosition(0); } catch (IllegalArgumentException e) {}

                JScrollPane viewScroll = new JScrollPane(view,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                add(viewScroll, BorderLayout.CENTER);

                JPanel p = new JPanel(new GridBagLayout());
                p.setOpaque(false);

                all = htmlButton(Bundle.SourceSectionView_All(), count < (chararray ? MAX_CHARARRAY_ITEMS : MAX_ARRAY_ITEMS), new Runnable() {
                    public void run() { showAll(); }
                });
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.insets = new Insets(3, 0, 0, 5);
                p.add(all, c);

                JButton save = htmlButton(Bundle.SourceSectionView_Save(), !preview.isEmpty(), new Runnable() {
                    public void run() {
                        new ExportAction(SourceSectionView.this).actionPerformed(null);
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
                
                // Likely a native method
                // TODO: handle differently?
                if (sectionSize == -1) showAll();
            }
        });
    }

    private void showAll() {
        all.setEnabled(false);
        view.setEnabled(false);
        new RequestProcessor("SourceSection Details").post(new Runnable() { // NOI18N // TODO: use a HeapWalker processor once the API is available
            public void run() {
                String _preview = null;
                try {
                    _preview = getString(false);
                } catch (OutOfMemoryError e) {
                    ProfilerDialogs.displayError(Bundle.SourceSectionView_OutOfMemory());
                    return;
                }

                final String preview = _preview;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            view.setText(preview);
                            view.setEnabled(true);
                            MutableAttributeSet attrs = new SimpleAttributeSet();
                            StyleConstants.setBackground(attrs, Color.YELLOW);
                            StyledDocument doc = view.getStyledDocument();
                            doc.setCharacterAttributes(sectionOffset, sectionSize, attrs, false);
                            view.setCaretPosition(Math.min(sectionOffset+64, preview.length()));
                        } catch (OutOfMemoryError e) {
                            ProfilerDialogs.displayError(Bundle.SourceSectionView_OutOfMemory());
                        }
                    }
                });
            }
        });
    }


    private String getString(boolean preview) {
        if (values == null) return "";                                              // NOI18N
        StringDecoder decoder = new StringDecoder(heap, coder, values);
        int valuesCount = count < 0 ? decoder.getStringLength() - offset : count;
        int estimatedSize = (int)Math.min((long)valuesCount * 2, MAX_PREVIEW_LENGTH + TRUNCATED.length());
        StringBuilder value = new StringBuilder(estimatedSize);
        int lastValue = offset + (preview ? sectionOffset + sectionSize - 1 : valuesCount - 1);
        int firstValue = offset + (preview ? sectionOffset : 0);
        for (int i = firstValue; i <= lastValue; i++) {
            value.append(decoder.getValueAt(i));
            if (preview && value.length() >= MAX_PREVIEW_LENGTH) {
                value.append(TRUNCATED);
                break;
            }
        }
        return value.toString();
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
        return false;
    }

    @Override
    public boolean hasBinaryData() {
        return false;
    }

    @Override
    public boolean hasText() {
        return true;
    }

    @Override
    public boolean isExportable() {
        return hasText() || hasBinaryData() || hasRawData();
    }

}
