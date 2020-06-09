/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sources.viewer.internal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.plaf.TextUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

/**
 *
 * @author Jiri Sedlacek
 */
final class InternalSourceViewerComponent extends JPanel implements PropertyChangeListener {
    
    private InternalSourceAppearance appearance;
    
    private final SourceArea sourceArea;
    private final LineNumbers lineNumbers;
    private final JViewport lineNumbersViewport;
    private final JPanel lineNumbersPanel;
    
    
    public InternalSourceViewerComponent(String text, int offset, InternalSourceAppearance appearance) {
        super(new BorderLayout());
        
        sourceArea = new SourceArea();
        
        this.appearance = appearance;
        appearance.addListener(this);
        propertyChange(null);
        
        sourceArea.setText(text);
        sourceArea.setOffset(offset);
        
        lineNumbersPanel = new JPanel(new BorderLayout());
        lineNumbersPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 10));
        lineNumbers = new LineNumbers(sourceArea);
        lineNumbersPanel.add(lineNumbers, BorderLayout.EAST);
        
        lineNumbersViewport = new JViewport();
        lineNumbersViewport.setView(lineNumbersPanel);
        lineNumbersViewport.setPreferredSize(lineNumbersPanel.getPreferredSize());
        
        JScrollPane scrollPane = new JScrollPane(sourceArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.setRowHeader(lineNumbersViewport);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        sourceArea.setFont(appearance.getFont());
        if (lineNumbers != null) {
            lineNumbers.updateAppearance(sourceArea);
            lineNumbersViewport.setPreferredSize(lineNumbersPanel.getPreferredSize());
            validate();
            repaint();
        }
    }
    
    
    void setOffset(int offset) {
        sourceArea.setOffset(offset);
    }
    
    
    void cleanup() {
        appearance.removeListener(this);
        appearance = null;
    }
    
    
    Component defaultFocusOwner() {
        return sourceArea;
    }
    
    
    private static class SourceArea extends JTextArea implements CaretListener {
        
        private int pendingOffset = -1;
        
        SourceArea() {
            super();
        
            setEditable(false);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
            
            setHighlighter(new LineHighlighter());
            addCaretListener(this);
            
            MouseAdapter adapter = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    setHighlight(SourceArea.this.getCaret().getDot());
                }
                public void mouseDragged(MouseEvent e) {
                    setHighlight(SourceArea.this.getCaret().getDot());
                }
            };
            
            addMouseListener(adapter);
            addMouseMotionListener(adapter);
        }
        
        protected int getRowHeight() {
            return super.getRowHeight();
        }
        
        void setOffset(int offset) {
            setCaretPosition(offset);
            scrollToOffset(offset);
        }
        
        private void scrollToOffset(int offset) {
            if (isValid()) {
                try {
                    Rectangle offsetRect = modelToView(offset);
                    if (offsetRect != null) {
                        int rowHeight = getRowHeight();
                        int currentHeight = getVisibleRect().height;
                        int margin = (currentHeight - rowHeight) / 2;
                        offsetRect.y -= margin;
                        offsetRect.height += (margin * 2);
                        scrollRectToVisible(offsetRect);
                    }
                } catch (BadLocationException ex) {}
            } else {
                pendingOffset = offset;
            }
        }
        
        public void validate() {
            super.validate();
            if (pendingOffset != -1) {
                scrollToOffset(pendingOffset);
                pendingOffset = -1;
            }
        }

        
        @Override
        public void caretUpdate(CaretEvent e) {
            setHighlight(e.getDot());
        }
        
        
        public void setHighlight(int dot) {
            getHighlighter().removeAllHighlights();
            int currentLine = getLineFromOffset(this, dot);
            int startPos = getLineStartOffsetForLine(this, currentLine);
            int endOffset = getLineEndOffsetForLine(this, currentLine);

            try {
                getHighlighter().addHighlight(startPos, endOffset, new DefaultHighlighter.DefaultHighlightPainter(new Color(233, 239, 248)));
//                getHighlighter().addHighlight(startPos, endOffset, new CustomHighlightPainter());           
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            repaint();
        }

        public int getLineFromOffset(JTextComponent component, int offset) {
            return component.getDocument().getDefaultRootElement().getElementIndex(offset);
        }

        public int getLineStartOffsetForLine(JTextComponent component, int line) {
            return component.getDocument().getDefaultRootElement().getElement(line).getStartOffset();
        }

        public int getLineEndOffsetForLine(JTextComponent component, int line) {
            return component.getDocument().getDefaultRootElement().getElement(line).getEndOffset();
        }
        
        static class LineHighlighter extends DefaultHighlighter {
            private JTextComponent component;

            @Override
            public final void install(final JTextComponent c) {
                super.install(c);
                this.component = c;
            }

            @Override
            public final void deinstall(final JTextComponent c) {
                super.deinstall(c);
                this.component = null;
            }

            @Override
            public final void paint(final Graphics g) {
                final Highlighter.Highlight[] highlights = getHighlights();
                final int len = highlights.length;
                for (int i = 0; i < len; i++) {
                    Highlighter.Highlight info = highlights[i];
                    if (info.getClass().getName().contains("LayeredHighlightInfo")) { // NOI18N
                        
                        final Rectangle a = this.component.getBounds();
                        final Insets insets = this.component.getInsets();
                        a.x = insets.left;
                        a.y = insets.top;
                        
                        a.height -= insets.top + insets.bottom;
                        final Highlighter.HighlightPainter p = info.getPainter();
                        p.paint(g, info.getStartOffset(), info.getEndOffset(), a, this.component);
                        
                        Rectangle alloc = a;
                        try {
                            TextUI mapper = this.component.getUI();
                            Rectangle p0 = mapper.modelToView(this.component, this.component.getSelectionStart());
                            Rectangle p1 = mapper.modelToView(this.component, this.component.getSelectionEnd());
                            
                            g.setColor(this.component.getSelectionColor());
                            if (p0.y == p1.y) {
                                Rectangle r = p0.union(p1);
                                g.fillRect(r.x, r.y, r.width, r.height);
                            } else {
                                int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
                                g.fillRect(p0.x, p0.y, p0ToMarginWidth, p0.height);
                                if ((p0.y + p0.height) != p1.y) g.fillRect(alloc.x, p0.y + p0.height, alloc.width, p1.y - (p0.y + p0.height));
                                g.fillRect(alloc.x, p1.y, (p1.x - alloc.x), p1.height);
                            }
                        } catch (BadLocationException e) {
                        }
                    }
                }
            }

            @Override
            public void removeAllHighlights() {
                if (component != null) component.repaint(0, 0, component.getWidth(), component.getHeight());
                super.removeAllHighlights();
            }
        }
        
//        static final class CustomHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
//        
//            CustomHighlightPainter() {
//                super(Color.ORANGE);
////                super(new Color(233, 239, 248));
//            }
//
//            public void paint(Graphics g, int offs0, int offs1,
//                                    Shape bounds, JTextComponent c) {
//
//                int selStart = c.getSelectionStart();
//                int selEnd = c.getSelectionEnd();
//
//                // No selection or selection fully outside of the highlight
//                if (selEnd - selStart == 0 || offs0 >= selEnd || offs1 <= selStart) super.paint(g, offs0, offs1, bounds, c);
//
//                // Selection fully covers the highlight
//                if (offs0 >= selStart && offs1 <= selEnd) return;
//
//                // Selection partially covers the highlight
//                if (offs0 < selStart || offs1 > selEnd) {
//                    // Selection ends inside of the highlight
//                    if (offs0 >= selStart) super.paint(g, selEnd, offs1, bounds, c);
//                    // Selection starts inside of the highlight
//                    else if (offs1 <= selEnd) super.paint(g, offs0, selStart, bounds, c);
//
//                    // Selection fully inside of the highlight
//                    super.paint(g, offs0, selStart, bounds, c);
//                    super.paint(g, selEnd, offs1, bounds, c);
//                }
//                
//                
//                Rectangle alloc = bounds.getBounds();
//                try {
//                    // --- determine locations ---
//                    TextUI mapper = c.getUI();
//                    Rectangle p0 = mapper.modelToView(c, offs0);
//                    Rectangle p1 = mapper.modelToView(c, offs1);
//
//                    // --- render ---
//                    Color color = getColor();
//
//                    if (color == null) {
//                        g.setColor(c.getSelectionColor());
//                    }
//                    else {
//                        g.setColor(color);
//                    }
//                    if (p0.y == p1.y) {
//                        // same line, render a rectangle
//                        Rectangle r = p0.union(p1);
//                        g.fillRect(r.x, r.y, r.width, r.height);
//                    } else {
//                        // different lines
//                        int p0ToMarginWidth = alloc.x + alloc.width - p0.x;
//                        g.fillRect(p0.x, p0.y, p0ToMarginWidth, p0.height);
//                        if ((p0.y + p0.height) != p1.y) {
//                            g.fillRect(alloc.x, p0.y + p0.height, alloc.width,
//                                       p1.y - (p0.y + p0.height));
//                        }
//                        g.fillRect(alloc.x, p1.y, (p1.x - alloc.x), p1.height);
//                    }
//                } catch (BadLocationException e) {
//                    // can't render
//                }
//            }
//
//        }
        
    }
    
    private static class LineNumbers extends JTable {
        
        private int currentLine;
        
        LineNumbers(final SourceArea sourceArea) {
            super(createModel(sourceArea));
            
            setShowGrid(false);
            setShowHorizontalLines(false);
            setShowVerticalLines(false);
            setOpaque(false);
            setFocusable(false);
            setCellSelectionEnabled(false);
            setRowSelectionAllowed(false);
            setColumnSelectionAllowed(false);
            setIntercellSpacing(new Dimension(0, 0));
            setBackground(new JPanel().getBackground());
            setBorder(BorderFactory.createEmptyBorder());
            
            updateAppearance(sourceArea);
            
            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value,
                          boolean isSelected, boolean hasFocus, int row, int column) {
                    Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    comp.setEnabled(row == currentLine);
                    return comp;
                }
            };
            renderer.setHorizontalAlignment(JLabel.TRAILING);
            renderer.setEnabled(false);
            setDefaultRenderer(Number.class, renderer);
            
            currentLine = sourceArea.getLineFromOffset(sourceArea, sourceArea.getCaret().getDot());
            sourceArea.addCaretListener(new CaretListener() {
                @Override
                public void caretUpdate(CaretEvent e) {
                    currentLine = sourceArea.getLineFromOffset(sourceArea, e.getDot());
                    repaint();
                }
            });
            MouseAdapter adapter = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    currentLine = sourceArea.getLineFromOffset(sourceArea, sourceArea.getCaret().getDot());
                    repaint();
                }
                public void mouseDragged(MouseEvent e) {
                    currentLine = sourceArea.getLineFromOffset(sourceArea, sourceArea.getCaret().getDot());
                    repaint();
                }
            };
            
            sourceArea.addMouseListener(adapter);
            sourceArea.addMouseMotionListener(adapter);
        }
        
        void updateAppearance(SourceArea sourceArea) {
            setRowHeight(sourceArea.getRowHeight());
            setFont(sourceArea.getFont());
            
            DefaultTableCellRenderer renderer = (DefaultTableCellRenderer)getDefaultRenderer(Number.class);
            renderer.setFont(sourceArea.getFont());
            renderer.setText(Integer.toString(sourceArea.getLineCount()));
            Dimension dim = sourceArea.getPreferredSize();
            dim.width = renderer.getPreferredSize().width;
            setPreferredSize(dim);
        }
        
        private static TableModel createModel(SourceArea sourceArea) {
            final int rowCount = sourceArea.getLineCount();
            return new AbstractTableModel() {
                @Override public int getRowCount() { return rowCount; }
                @Override public int getColumnCount() { return 1; }
                @Override public Class<?> getColumnClass(int columnIndex) { return Number.class; }
                @Override public Object getValueAt(int rowIndex, int columnIndex) { return rowIndex + 1; }
            };
        }
        
    }
    
}
