/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.ui;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.swing.HTMLTextComponent;
import org.graalvm.visualvm.lib.ui.components.HTMLTextAreaSearchUtils;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public class HTMLView {
    
    private final String viewID;
    
    private final HeapContext context;
    private final HeapViewerActions actions;
    
    private Collection<HeapViewerNodeAction.Provider> actionProviders;
    
    private JComponent component;
    private HTMLTextComponent htmlComponent;
    
    private String currentText;
    
    private boolean pendingText = true;
    private String pendingReference;
    
    
    public HTMLView(String viewID, HeapContext context, HeapViewerActions actions) {
        this(viewID, context, actions, null);
    }
    
    public HTMLView(String viewID, HeapContext context, HeapViewerActions actions, String initialText) {
        this.viewID = viewID;
        this.context = context;
        this.actions = actions;
        this.currentText = initialText;
        
        actionProviders = new ArrayList();
        for (HeapViewerNodeAction.Provider provider : Lookup.getDefault().lookupAll(HeapViewerNodeAction.Provider.class))
            if (provider.supportsView(context, viewID)) actionProviders.add(provider);
    }
    
    
    protected String computeData(HeapContext context, String viewID) {
        return currentText;
    }
    
    protected HeapViewerNode nodeForURL(URL url, HeapContext context) {
        return null;
    }
    
    
    public void setText(String text) {
        currentText = text;
        if (htmlComponent != null) {
            htmlComponent.setText(currentText);
            HTMLTextAreaSearchUtils.textChanged(htmlComponent);
            String _text = htmlComponent.getText();
            if (_text != null && !_text.isEmpty()) try {
                htmlComponent.setCaretPosition(0);
            } catch (Exception e) {}
        }
    }
    
    public void selectReference(String reference) {
        if (htmlComponent == null || pendingText) {
            pendingReference = reference;
        } else {
            Document d = htmlComponent.getDocument();
            if (d instanceof HTMLDocument) {
                HTMLDocument doc = (HTMLDocument)d;
                HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A);
                for (; iter.isValid(); iter.next()) {
                    AttributeSet a = iter.getAttributes();
                    String nm = (String) a.getAttribute(HTML.Attribute.NAME);
                    if (Objects.equals(reference, nm)) {
                        selectReference(iter);
                        return;
                    }
                }
            }
        }
    }
    
    private void selectReference(HTMLDocument.Iterator iter) {
        htmlComponent.requestFocus();
        
        int start = iter.getStartOffset();
        htmlComponent.select(start, iter.getEndOffset());
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    Rectangle rect = htmlComponent.modelToView(start);
                    if (rect != null) {
                        rect.x -= htmlComponent.getInsets().left + component.getInsets().left;
                        rect.height = htmlComponent.getVisibleRect().height;
                        htmlComponent.scrollRectToVisible(rect);
                    }
                } catch (BadLocationException e) {}
            }
        });
    }
    
    
    public JComponent getComponent() {
        if (component == null) initUI();
        return component;
    }
    
    String getViewID() {
        return viewID;
    }
    
    HeapContext getContext() {
        return context;
    }
    
    
    private void initUI() {
        htmlComponent = new HTMLTextComponent() {
            protected void firstDisplayed() {
                super.firstDisplayed();
                
                htmlComponent.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isMiddleMouseButton(e)) {
                            URL link = htmlComponent.getActiveLink();
                            if (link != null) HTMLView.this.invokeMiddleButtonAction(link, e);
                        }
                    }
                });
                
                new SwingWorker<String, String>() {
                    protected String doInBackground() throws Exception {
                        return computeData(context, viewID);
                    }
                    protected void done() {
                        try {
                            HTMLView.this.setText(get());
                            pendingText = false;
                            if (pendingReference != null) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        selectReference(pendingReference);
                                        pendingReference = null;
                                    }
                                });
                                
                            }
                        } catch (InterruptedException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (ExecutionException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }.execute();
            }
            protected void showURL(URL url, InputEvent e) {
                HTMLView.this.invokeDefaultAction(url, e);
            }
            protected void populatePopup(JPopupMenu popup) {
                URL link = htmlComponent.getActiveLink();
                if (link != null) HTMLView.this.populatePopup(link, popup);
                
                if (popup.getComponentCount() > 0) popup.addSeparator();
                popup.add(createCopyMenuItem());
                popup.add(createSelectAllMenuItem());
                
                Action find = getActionMap().get(HTMLTextAreaSearchUtils.FIND_ACTION_KEY); 
                if (find != null) {
                    popup.addSeparator();
                    popup.add(new JMenuItem(find));
                }
            }
        };
        
        //----------------------------------------------------------------------
        // NOTE: uncomment to allow the selection to survive focusLost
        htmlComponent.setCaret(new DefaultCaret() {
            public void setSelectionVisible(boolean visible) {
               super.setSelectionVisible(true);
            }
        });
        //----------------------------------------------------------------------
        
        if (currentText != null) htmlComponent.setText(currentText);
                
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(htmlComponent.getBackground());
        container.add(htmlComponent, BorderLayout.CENTER);
        container.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        
        component = new JPanel(new BorderLayout());
        component.add(new ScrollableContainer(container), BorderLayout.CENTER);
        component.add(HTMLTextAreaSearchUtils.createSearchPanel(htmlComponent), BorderLayout.SOUTH);
    }
    
    
    private void invokeDefaultAction(URL url, InputEvent e) {
        HeapViewerNode node = nodeForURL(url, context);
        if (node == null) return;

        HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions);
        ActionEvent ae = e == null ? new ActionEvent(htmlComponent, ActionEvent.ACTION_PERFORMED, "link"): // NO18N
                                     new ActionEvent(e.getSource(), e.getID(), "link", e.getWhen(), e.getModifiers()); // NO18N
        nodeActions.performDefaultAction(ae);
    }
    
    private void invokeMiddleButtonAction(URL url, InputEvent e) {
        HeapViewerNode node = nodeForURL(url, context);
        if (node == null) return;

        HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions);
        ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), "middle button", e.getWhen(), e.getModifiers()); // NO18N
        nodeActions.performMiddleButtonAction(ae);
    }
    
    private void populatePopup(URL url, JPopupMenu popup) {
        HeapViewerNode node = nodeForURL(url, context);
        if (node == null) return;
        
        HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions);
        nodeActions.populatePopup(popup);
    }
    
}
