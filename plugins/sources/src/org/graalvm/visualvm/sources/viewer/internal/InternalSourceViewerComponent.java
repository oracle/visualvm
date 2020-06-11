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
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
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
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import org.openide.util.Exceptions;

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
    
    
    public InternalSourceViewerComponent(String text, int offset, int endOffset, InternalSourceAppearance appearance) {
        super(new BorderLayout());
        
        sourceArea = new SourceArea();
        
        this.appearance = appearance;
        appearance.addListener(this);
        propertyChange(null);
        
        sourceArea.setText(text);
        setOffset(offset, endOffset);
        
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
    
    
    void setOffset(final int offset, final int endOffset) {
        sourceArea.setOffset(offset);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { sourceArea.select(offset, endOffset); }
        });
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
        
        private Object rowHighlight;
        private final LineHighlightPainter highlightPainter;
        
        
        SourceArea() {
            super();
        
            setEditable(false);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
            
            highlightPainter = new LineHighlightPainter();
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
        
        public void requestFocus() {
            super.requestFocus();
            setHighlight(getCaretPosition());
        }

        
        @Override
        public void caretUpdate(CaretEvent e) {
            setHighlight(e.getDot());
        }
        
        
        public void setHighlight(final int dot) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (rowHighlight != null) getHighlighter().removeHighlight(rowHighlight);
            
                    int currentLine = getLineFromOffset(SourceArea.this, dot);
                    int startOffset = getLineStartOffsetForLine(SourceArea.this, currentLine);
                    int endOffset = getLineEndOffsetForLine(SourceArea.this, currentLine);
                    
                    try {
                        rowHighlight = getHighlighter().addHighlight(startOffset, endOffset, highlightPainter);
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }

                    repaint();
                }
            });
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
        
        
        static final class LineHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        
            LineHighlightPainter() {
                super(new Color(233, 239, 248));
            }
            
            public Shape paintLayer(Graphics g, int offs0, int offs1,
                                Shape bounds, JTextComponent c, View view) {
                try {
                    Rectangle r = c.modelToView(offs0);
                    r.x = 0;
                    r.width = c.getWidth();
                    
                    g.setColor(getColor());
                    ((Graphics2D)g).fill(r);
                    
                    return r;
                } catch (BadLocationException ex) {
                    return null;
                }
            }

        }
        
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
