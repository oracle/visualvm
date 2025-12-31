/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.oql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLEngine;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLException;
import org.graalvm.visualvm.lib.profiler.oql.spi.OQLEditorImpl;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.NoCaret;
import org.graalvm.visualvm.uisupport.UISupport;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Jaroslav Bachorik
 * @author Jiri Sedlacek
 */
public class OQLEditorComponent extends JPanel {

    private static final String VALIDITY_PROPERTY = OQLEditorImpl.VALIDITY_PROPERTY;
    
    private volatile boolean lexervalid;
    private volatile boolean parserValid;
    private volatile boolean oldValidity;
    
    
    private final OQLEngine engine;
    
    private JEditorPane queryEditor;
    private JScrollBar verticalScroller;

    private Color lastBgColor;
    private Caret lastCaret;
    
    private boolean changed;

    
    public OQLEditorComponent(OQLEngine engine) {
        this.engine = engine;
        init();
    }
    
    
    public void setScript(String script) {
        queryEditor.setText(script);
        clearChanged();
        try { queryEditor.setCaretPosition(0); } catch (IllegalArgumentException e) {}
        scrollRectToVisible(new Rectangle());
    }

    public String getScript() {
        return queryEditor.getText();
    }
    
    
    protected void validityChanged(boolean valid) {}
    
    
    final void clearChanged() {
        changed = false;
    }
    
    public final boolean isChanged() {
        return changed;
    }

    
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        if (verticalScroller != null) updateScroller(verticalScroller);
    }
    
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (queryEditor != null) queryEditor.setBackground(bg);
    }

    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);
        if (queryEditor != null) queryEditor.setOpaque(isOpaque);
    }

    public void requestFocus() {
        if (queryEditor != null) queryEditor.requestFocus();
    }    

    public void setEditable(boolean b) {
        if (queryEditor.isEditable() == b) return;
        
        queryEditor.setEditable(b);

        if (b) {
            if (lastBgColor != null) queryEditor.setBackground(lastBgColor);
            if (lastCaret != null) queryEditor.setCaret(lastCaret);
        } else {
            lastBgColor = queryEditor.getBackground();
            lastCaret = queryEditor.getCaret();
            
            Color disabledBackground = UIUtils.isGTKLookAndFeel() ?
                  UIManager.getLookAndFeel().getDefaults().getColor("desktop") : // NOI18N
                  UIManager.getColor("TextField.disabledBackground"); // NOI18N
            queryEditor.setBackground(disabledBackground);
            queryEditor.setCaret(new NoCaret());
        }
    }

    public boolean isEditable() {
        return queryEditor.isEditable();
    }
    
    public void clearScrollBorders() {
        if (getComponentCount() > 0) {
            Component c = getComponent(0);
            if (c instanceof JScrollPane) {
                ((JScrollPane)c).setBorder(BorderFactory.createEmptyBorder());
                ((JScrollPane)c).setViewportBorder(BorderFactory.createEmptyBorder());
            }
        }
    }
    
    
    private void init() {
        setOpaque(true);
        setBackground(UIUtils.getProfilerResultsBackground());
        
        OQLEditorImpl impl = Lookup.getDefault().lookup(OQLEditorImpl.class);
        if (impl != null) {
            queryEditor = impl.getEditorPane();
            queryEditor.getDocument().putProperty(OQLEngine.class, engine);
            queryEditor.getDocument().putProperty(OQLEditorImpl.ValidationCallback.class, new OQLEditorImpl.ValidationCallback() {
                public void callback(boolean lexingResult) {
                    lexervalid = lexingResult;
                    validateScript();
                }
            });
            
            queryEditor.getCaret().addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    try {
                        Rectangle edit = queryEditor == null ? null :
                                         queryEditor.getUI().modelToView(
                                         queryEditor, queryEditor.getCaretPosition());
                        if (edit != null) queryEditor.scrollRectToVisible(edit);
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            });
            
            Element root = queryEditor.getDocument().getDefaultRootElement();
            String family = StyleConstants.getFontFamily(root.getAttributes());
            queryEditor.setFont(new Font(family, Font.PLAIN, new JEditorPane().getFont().getSize()));
        } else {
            final DocumentListener listener = new DocumentListener() {
                public void insertUpdate(DocumentEvent e)  { validateScript(); }
                public void removeUpdate(DocumentEvent e)  { validateScript(); }
                public void changedUpdate(DocumentEvent e) { validateScript(); }
            };
            final DocumentListener editHandler = new DocumentListener() {
                public void insertUpdate(DocumentEvent e)  { handleEdit(); }
                public void removeUpdate(DocumentEvent e)  { handleEdit(); }
                public void changedUpdate(DocumentEvent e) { handleEdit(); }
                private void handleEdit() { changed = true; }
            };
            
            queryEditor = new JEditorPane() {
                protected EditorKit createDefaultEditorKit() {
                    return new OQLEditorKit();
                }
                public void setText(String text) {
                    Document doc = getDocument();
                    if (doc != null) {
                        doc.removeDocumentListener(listener);
                        doc.removeDocumentListener(editHandler);
                    }
                    setDocument(getEditorKit().createDefaultDocument());
                    doc = getDocument();
                    if (doc != null) doc.addDocumentListener(listener);
                    super.setText(text);
                    if (doc != null) doc.addDocumentListener(editHandler);
                }
            };
            
            queryEditor.setFont(Font.decode(Font.MONOSPACED + " " + queryEditor.getFont().getSize())); // NOI18N
            lexervalid = true; // no lexer info available; assume the lexing info is valid
        }

        // IMPORTANT: setText() is required to register document listener/validity check!
        queryEditor.setText("");
        
        queryEditor.setOpaque(isOpaque());
        queryEditor.setBackground(getBackground());
        
        // Do not display NB TopComponent switcher, let the focus subsystem transfer the focus out of the editor
        queryEditor.putClientProperty("nb.ctrltab.popupswitcher.disable", Boolean.TRUE); // NOI18N
        
        int rowSize = queryEditor.getFontMetrics(queryEditor.getFont()).getHeight();
        
        final JScrollPane editorScroll = new JScrollPane(queryEditor,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        editorScroll.getHorizontalScrollBar().setUnitIncrement(rowSize);
        verticalScroller = editorScroll.getVerticalScrollBar();
        verticalScroller.setUnitIncrement(rowSize);
        verticalScroller.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) { updateScroller(verticalScroller); }
        });
        
        JTextField tf = new JTextField(" 999 "); // NOI18N
        tf.setBorder(BorderFactory.createEmptyBorder());
        tf.setMargin(new Insets(0, 0, 0, 0));
        tf.setFont(queryEditor.getFont());
        final int w = tf.getPreferredSize().width;

        final JEditorPane rows = new JEditorPane() {
            {
                setEditorKit(queryEditor.getEditorKit());
                setFont(queryEditor.getFont());
            }
            public Dimension getPreferredSize() {
                Dimension dim = new Dimension(w, 0);
                int refHeight = queryEditor.getPreferredSize().height;
                int viewHeight = editorScroll.getViewport().getExtentSize().height;
                dim.height = Math.max(refHeight, viewHeight);
                return dim;
            }
            public void setBackground(Color c) {
                super.setBackground(!UISupport.isDarkResultsBackground() ? new Color(245, 245, 245) : new Color(55, 55, 55));
            }
        };

        rows.setCaret(new FollowingCaret(queryEditor));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 1000; i++) {
            if (i < 1000) sb.append(" "); // NOI18N
            if (i < 100) sb.append(" "); // NOI18N
            if (i < 10) sb.append(" "); // NOI18N
            sb.append(Integer.toString(i) + " \n"); // NOI18N
        }
        rows.setText(sb.toString());
        rows.setEditable(false);
        rows.setEnabled(false);

        Insets margin = queryEditor.getMargin();
        if (margin == null) margin = new Insets(0, 0, 0, 0);
        rows.setMargin(new Insets(margin.top, 0, margin.bottom, 0));

        Border border = queryEditor.getBorder();
        if (border != null) {
            margin = border.getBorderInsets(queryEditor);
            if (margin == null) margin = new Insets(0, 0, 0, 0);
            rows.setBorder(BorderFactory.createEmptyBorder(margin.top, -1, margin.bottom, 0));
        }
        
        editorScroll.setRowHeaderView(rows);
        
        setLayout(new BorderLayout());
        add(editorScroll, BorderLayout.CENTER);
    }
    
    private void updateScroller(JScrollBar scroll) {
        scroll.setEnabled(isEnabled() && scroll.getVisibleAmount() < scroll.getMaximum());
    }
    
    private void validateScript() {
        if (engine == null) return;
        
        if (lexervalid || !parserValid) {
            // only parse the query if there are no errors from lexer
            try {
                engine.parseQuery(getScript());
                parserValid = true;
            } catch (OQLException e) {
                StatusDisplayer.getDefault().setStatusText(e.getLocalizedMessage());
                parserValid = false;
            }
        }

        firePropertyChange(VALIDITY_PROPERTY, oldValidity, lexervalid && parserValid);
        oldValidity = lexervalid && parserValid;
        
        validityChanged(oldValidity);
    }
    
    
    private static class OQLEditorKit extends DefaultEditorKit {
        
        private static final ViewFactory FACTORY = new ViewFactory() {
            public View create(Element elem) { return new PlainView(elem); }
        };
        
        public String getContentType() {
            return "text/x-oql"; // NOI18N
        }
        
        public ViewFactory getViewFactory() {
            return FACTORY;
        }
        
    }
    
    private static class FollowingCaret implements Caret {
                
        private final List<ChangeListener> listeners = new ArrayList<>();
        private int dot;

        FollowingCaret(final JTextComponent tc) {
            tc.getCaret().addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    setDot(followedPosition(tc));
                }
            });
            setDot(followedPosition(tc));
        }

        private static int followedPosition(JTextComponent tc) {
            Element root = tc.getDocument().getDefaultRootElement();
            return root.getElementIndex(tc.getCaretPosition()) * 6;
        }

        public void install(JTextComponent c) {}
        public void deinstall(JTextComponent c) {}
        public void paint(Graphics g) {}
        public void addChangeListener(ChangeListener l) { listeners.add(l); }
        public void removeChangeListener(ChangeListener l) { listeners.remove(l); }
        public boolean isVisible() { return false; }
        public void setVisible(boolean v) {}
        public boolean isSelectionVisible() { return false; }
        public void setSelectionVisible(boolean v) {}
        public void setMagicCaretPosition(Point p) {}
        public Point getMagicCaretPosition() { return new Point(0, 0); }
        public void setBlinkRate(int rate) {}
        public int getBlinkRate() { return 1; }
        public int getDot() { return dot; }
        public int getMark() { return dot; }
        public void moveDot(int dot) {}
        
        public void setDot(int dot) {
            this.dot = dot;
            ChangeEvent e = new ChangeEvent(this);
            for (ChangeListener l : listeners) l.stateChanged(e);
        }
        

    }
    
}
