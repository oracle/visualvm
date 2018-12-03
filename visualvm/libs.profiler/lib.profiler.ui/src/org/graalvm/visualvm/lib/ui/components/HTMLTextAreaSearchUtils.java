/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import org.graalvm.visualvm.lib.profiler.api.ActionsSupport;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.swing.EditableHistoryCombo;
import org.graalvm.visualvm.lib.ui.swing.InvisibleToolbar;

/**
 *
 * @author Jiri Sedlacek
 */
public final class HTMLTextAreaSearchUtils {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.components.Bundle"); // NOI18N
    private static final String MATCHES_PATTERN = messages.getString("HTMLTextAreaSearchUtils_MatchesPattern"); // NOI18N
    private static final String NO_MATCHES = messages.getString("HTMLTextAreaSearchUtils_NoMatches"); // NOI18N
    private static final String MATCHES_TOOLTIP = messages.getString("HTMLTextAreaSearchUtils_MatchesTooltip"); // NOI18N
    private static final String SIDEBAR_CAPTION = messages.getString("HTMLTextAreaSearchUtils_SidebarCaption"); // NOI18N
    private static final String BTN_PREVIOUS = messages.getString("HTMLTextAreaSearchUtils_BtnPrevious"); // NOI18N
    private static final String BTN_PREVIOUS_TOOLTIP = messages.getString("HTMLTextAreaSearchUtils_BtnPreviousTooltip"); // NOI18N
    private static final String BTN_NEXT = messages.getString("HTMLTextAreaSearchUtils_BtnNext"); // NOI18N
    private static final String BTN_NEXT_TOOLTIP = messages.getString("HTMLTextAreaSearchUtils_BtnNextTooltip"); // NOI18N
    private static final String BTN_MATCH_CASE_TOOLTIP = messages.getString("HTMLTextAreaSearchUtils_BtnMatchCaseTooltip"); // NOI18N
    private static final String BTN_CLOSE_TOOLTIP = messages.getString("HTMLTextAreaSearchUtils_BtnCloseTooltip"); // NOI18N
    // -----
    
    public static final String FIND_ACTION_KEY = "find-action-key"; // NOI18N
    public static final String FIND_NEXT_ACTION_KEY = "find-next-action-key"; // NOI18N
    public static final String FIND_PREV_ACTION_KEY = "find-prev-action-key"; // NOI18N
    public static final String FIND_SEL_ACTION_KEY = "find-sel-action-key"; // NOI18N
    
    private static final String FIND_CLOSE_ACTION_KEY = "find-close-action-key"; // NOI18N
    
    private static final String PROP_LAST_FIND_TEXT = "last-find-text"; // NOI18N
    private static final String PROP_LAST_FIND_MATCH_CASE = "last-find-match-case"; // NOI18N
    private static final String PROP_SEARCH_PANEL = "search-panel"; // NOI18N
    private static final String PROP_SEARCH_RESULT = "search-result"; // NOI18N
    
    
    public static boolean findString(HTMLTextArea area, String text) {
        return findString(area, text, true, true);
    }
    
    public static boolean findString(HTMLTextArea area, String text, boolean matchCase, boolean next) {
        DocumentSearchResult result = findStringImpl(area, text, matchCase);
        if (result == null) return false;
        
        int offset = next ? area.getSelectionEnd() : area.getSelectionStart();
        int closest = next ? result.getNextIndex(offset) : result.getPreviousIndex(offset);
        
        if (closest < 0) {
            area.select(area.getSelectionStart(), area.getSelectionStart());
            return false;
        } else {
            offset = result.getOffset(closest);
            area.select(offset, offset + text.length());
            return true;
        }
    }
    
    
    public static void textChanged(HTMLTextArea area) {
        DocumentSearchResult result = DocumentSearchResult.reset(area);
        SearchPanel search = (SearchPanel)area.getClientProperty(PROP_SEARCH_PANEL);
        if (search.isVisible()) search.clearResultsFeedback(result);
    }
    
    
    private static DocumentSearchResult findStringImpl(HTMLTextArea area, String text, boolean matchCase) {
        area.putClientProperty(PROP_LAST_FIND_TEXT, text);
        area.putClientProperty(PROP_LAST_FIND_MATCH_CASE, Boolean.toString(matchCase));
        
        return DocumentSearchResult.get(text, matchCase, area);
    }
    
    
    private static class DocumentSearchResult {
        
        private final String search;
        private final boolean matchCase;
        
        private final List<Integer> result;
        private final List<Object> highlights;
        
        
        static DocumentSearchResult current(JTextComponent component) {
            return (DocumentSearchResult)component.getClientProperty(PROP_SEARCH_RESULT);
        }
        
        static DocumentSearchResult get(String search, boolean matchCase, JTextComponent component) {
            DocumentSearchResult result = current(component);
            if (result != null) {
                if (result.compatibleSearch(search, matchCase)) return result;
                else result.clearHighlightedResults(component.getHighlighter());
            }
                
            try {
                Document document = component.getDocument();
                result = new DocumentSearchResult(search, matchCase, document.getText(0, document.getLength()));
                component.putClientProperty(PROP_SEARCH_RESULT, result);
                return result;
            } catch (BadLocationException ex) {
                return null;
            }
        }
        
        static DocumentSearchResult reset(JTextComponent component) {
            DocumentSearchResult result = DocumentSearchResult.current(component);
            
            if (result != null) {
                result.clearHighlightedResults(component.getHighlighter());
                component.putClientProperty(PROP_SEARCH_RESULT, null);
            }
            
            return result;
        }
        
        
        private DocumentSearchResult(String search, boolean matchCase, String text) {
            if (!matchCase) {
                search = search.toLowerCase(Locale.ENGLISH);
                text = text.toLowerCase(Locale.ENGLISH);
            }
            
            this.search = search;
            this.matchCase = matchCase;
            
            this.result = new ArrayList();
            this.highlights = new ArrayList();
            
            int searchLength = search.length();
            int offset = text.indexOf(search, 0);
            
            while (offset >= 0) {
                result.add(offset);
                offset = text.indexOf(search, offset + searchLength);
            }
        }
        
        
        private boolean compatibleSearch(String string, boolean match) {
            if (matchCase != match) return false;
            return search.equals(matchCase ? string : string.toLowerCase(Locale.ENGLISH));
        }
        
        
        void highlightResults(Highlighter hl) {
            clearHighlightedResults(hl);
            
            for (int offset : result) {
                try { highlights.add(hl.addHighlight(offset, offset + search.length(), new CustomHighlightPainter())); }
                catch (BadLocationException ex) {}
            }
        }
        
        void clearHighlightedResults(Highlighter hl) {
            for (Object highlight : highlights) hl.removeHighlight(highlight);
            
            highlights.clear();
        }
        
        int getResultsCount() {
            return result.size();
        }
        
        int getOffset(int index) {
            return result.get(index);
        }
        
        int getNextIndex(int offset) {
            if (result.isEmpty()) return -1;            
            
            for (int index = 0; index < result.size(); index++)
                if (result.get(index) > offset) return index;
            
            return 0;
        }
        
        int getPreviousIndex(int offset) {
            if (result.isEmpty()) return -1;            
            
            for (int i = result.size() - 1; i >= 0; i--)
                if (result.get(i) < offset) return i;
            
            return result.size() - 1;
        }
        
    }
    
    
//    // NOTE: must not be direct subclass of DefaultHighlightPainter to not overlap selection
//    private static final class ResultsHighlightPainter implements Highlighter.HighlightPainter {
//        
//        private static final Highlighter.HighlightPainter IMPL = new DefaultHighlightPainter(Color.ORANGE);
//
//        @Override
//        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
//            IMPL.paint(g, p0, p1, bounds, c);
//        }
//        
//    }
    
    
    private static final class CustomHighlightPainter extends DefaultHighlightPainter {
        
        CustomHighlightPainter() {
            super(Color.ORANGE);
        }
        
        public Shape paintLayer(Graphics g, int offs0, int offs1,
                                Shape bounds, JTextComponent c, View view) {
            
            int selStart = c.getSelectionStart();
            int selEnd = c.getSelectionEnd();
            
            // No selection or selection fully outside of the highlight
            if (selEnd - selStart == 0 || offs0 >= selEnd || offs1 <= selStart) return super.paintLayer(g, offs0, offs1, bounds, c, view);
            
            // Selection fully covers the highlight
            if (offs0 >= selStart && offs1 <= selEnd) return bounds;
            
            // Selection partially covers the highlight
            if (offs0 < selStart || offs1 > selEnd) {
                // Selection ends inside of the highlight
                if (offs0 >= selStart) return super.paintLayer(g, selEnd, offs1, bounds, c, view);
                // Selection starts inside of the highlight
                else if (offs1 <= selEnd) return super.paintLayer(g, offs0, selStart, bounds, c, view);
                
                // Selection fully inside of the highlight
                super.paintLayer(g, offs0, selStart, bounds, c, view);
                super.paintLayer(g, selEnd, offs1, bounds, c, view);
            }
            
            return bounds;
        }
        
    }
    
    
    public static JComponent createSearchPanel(final HTMLTextArea area) {
        SearchPanel sp = new SearchPanel(area) {
            public void setVisible(boolean visible) {
                super.setVisible(visible);
                if (!visible) area.requestFocusInWindow();
            }
        };
        area.putClientProperty(PROP_SEARCH_PANEL, sp);
        enableSearchActions(area);
        
        // Make sure selection is visible even if the text area is not focused
        Caret caret = area.getCaret();
        if (caret instanceof DefaultCaret) {
            area.setCaret(new DefaultCaret() {
                public void setSelectionVisible(boolean visible) {
                   super.setSelectionVisible(true);
                }
            });
        }
        
        return sp;
    }
    
    
    private static class SearchPanel extends JPanel {
        
        private final HTMLTextArea area;
        
        private final EditableHistoryCombo combo;
        private final JTextComponent textC;
        
        private final JToggleButton matchCase;
        private final JToggleButton highlightResults;
        
        private final JLabel feedback;
        
        
        SearchPanel(final HTMLTextArea area) {
            super(new BorderLayout());
            setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("controlShadow"))); // NOI18N       
            
            this.area = area;
            
            feedback = new JLabel();
            feedback.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            
            JToolBar toolbar = new InvisibleToolbar();
            if (UIUtils.isWindowsModernLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2));
            else if (!UIUtils.isNimbusLookAndFeel() && !UIUtils.isAquaLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));

            toolbar.add(Box.createHorizontalStrut(6));
            toolbar.add(new JLabel(SIDEBAR_CAPTION));
            toolbar.add(Box.createHorizontalStrut(3));
            add(toolbar, BorderLayout.CENTER);
            
            combo = new EditableHistoryCombo();        
            textC = combo.getTextComponent();

            JPanel comboContainer = new JPanel(new BorderLayout());
            comboContainer.add(combo, BorderLayout.CENTER);
            comboContainer.setMinimumSize(combo.getMinimumSize());
            comboContainer.setPreferredSize(combo.getPreferredSize());
            comboContainer.setMaximumSize(combo.getMaximumSize());

            toolbar.add(comboContainer);
            
            toolbar.add(Box.createHorizontalStrut(5));
        
            KeyStroke escKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            KeyStroke prevKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK);
            KeyStroke nextKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

            matchCase = new JToggleButton(Icons.getIcon(GeneralIcons.MATCH_CASE)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            clearResultsFeedback(DocumentSearchResult.current(area));
                        }
                    });
                }
            };
            matchCase.setToolTipText(BTN_MATCH_CASE_TOOLTIP);
            // NOTE: added below
            
            highlightResults = new JToggleButton(Icons.getIcon(GeneralIcons.HIGHLIGHT_RESULTS), true) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            DocumentSearchResult result = DocumentSearchResult.current(area);
                            if (result != null) {
                                Highlighter hl = area.getHighlighter();
                                result.clearHighlightedResults(hl);
                                
                                if (isSelected() && !feedback.getText().isEmpty() && result.getResultsCount() > 0)
                                    result.highlightResults(hl);
                            }
                        }
                    });
                }
            };
            highlightResults.setToolTipText(MATCHES_TOOLTIP);
            // NOTE: added below

            final JButton prev = new JButton(BTN_PREVIOUS, Icons.getIcon(GeneralIcons.FIND_PREVIOUS)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            String search = getSearchString();
                            if (search.isEmpty()) return;
                            
                            DocumentSearchResult result = findStringImpl(area, search, matchCase.isSelected());
                            int results = result.getResultsCount();
                            
                            if (results > 0) {
                                int index = result.getPreviousIndex(area.getSelectionStart());
                                setResultsFeedback(index, results, result);
                                combo.addItem(search);
                                
                                int offset = result.getOffset(index);
                                area.select(offset, offset + search.length());
                            } else {
                                feedback.setText(NO_MATCHES);
                                area.select(area.getSelectionStart(), area.getSelectionStart());
                            }
                        }
                    });
                }
            };
            prev.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            prev.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
            String prevAccelerator = ActionsSupport.keyAcceleratorString(prevKey);
            prev.setToolTipText(MessageFormat.format(BTN_PREVIOUS_TOOLTIP, prevAccelerator));
            prev.setEnabled(false);
            toolbar.add(prev);

            if (!UIUtils.isAquaLookAndFeel()) toolbar.add(Box.createHorizontalStrut(2));

            final JButton next = new JButton(BTN_NEXT, Icons.getIcon(GeneralIcons.FIND_NEXT)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            String search = getSearchString();
                            if (search.isEmpty()) return;
                            
                            DocumentSearchResult result = findStringImpl(area, search, matchCase.isSelected());
                            int results = result.getResultsCount();
                            
                            if (results > 0) {
                                int index = result.getNextIndex(area.getSelectionEnd());
                                setResultsFeedback(index, results, result);
                                combo.addItem(search);
                                
                                int offset = result.getOffset(index);
                                area.select(offset, offset + search.length());
                            } else {
                                feedback.setText(NO_MATCHES);
                                area.select(area.getSelectionStart(), area.getSelectionStart());
                            }
                        }
                    });
                }
            };
            next.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
            next.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
            String nextAccelerator = ActionsSupport.keyAcceleratorString(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            next.setToolTipText(MessageFormat.format(BTN_NEXT_TOOLTIP, nextAccelerator));
            next.setEnabled(false);
            toolbar.add(next);

            toolbar.add(Box.createHorizontalStrut(2));

            toolbar.addSeparator();

            toolbar.add(Box.createHorizontalStrut(1));

            toolbar.add(matchCase);
            toolbar.add(highlightResults);

//            if (options != null) for (Component option : options) toolbar.add(option);

            toolbar.add(Box.createHorizontalStrut(2));

            combo.setOnTextChangeHandler(new Runnable() {
                public void run() {
                    boolean enable = !combo.getText().trim().isEmpty();
                    prev.setEnabled(enable);
                    next.setEnabled(enable);
                }
            });
            
            final Runnable hider = new Runnable() { public void run() { setVisible(false); } };
            JButton closeButton = CloseButton.create(hider);
            String escAccelerator = ActionsSupport.keyAcceleratorString(escKey);
            closeButton.setToolTipText(MessageFormat.format(BTN_CLOSE_TOOLTIP, escAccelerator));
            
            String HIDE = "hide-action"; // NOI18N
            InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            Action hiderAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) { hider.run(); }
            };
            getActionMap().put(HIDE, hiderAction);
            inputMap.put(escKey, HIDE);
            
            if (textC != null) {
//                inputMap = textC.getInputMap();
//                ActionMap actionMap = textC.getActionMap();
                ActionMap actionMap = getActionMap();

                String NEXT = "search-next-action"; // NOI18N
                Action nextAction = new AbstractAction() {
                    public void actionPerformed(final ActionEvent e) {
                        if (combo.isPopupVisible()) combo.hidePopup();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() { if (next.isEnabled()) next.doClick(); }
                        });
                    }
                };
                actionMap.put(NEXT, nextAction);
                inputMap.put(nextKey, NEXT);
                if (textC != null) {
                    textC.getActionMap().put(NEXT, nextAction);
                    textC.getInputMap().put(nextKey, NEXT);
                }

                KeyStroke nextKey2 = ActionsSupport.registerAction(FIND_NEXT_ACTION_KEY, nextAction, actionMap, inputMap);
                String nextAccelerator2 = ActionsSupport.keyAcceleratorString(nextKey2);
                if (nextAccelerator2 != null) next.setToolTipText(MessageFormat.format(BTN_NEXT_TOOLTIP,
                                                             nextAccelerator + ", " + nextAccelerator2)); // NOI18N

                String PREV = "search-prev-action"; // NOI18N
                Action prevAction = new AbstractAction() {
                    public void actionPerformed(final ActionEvent e) {
                        if (combo.isPopupVisible()) combo.hidePopup();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() { if (next.isEnabled()) prev.doClick(); }
                        });
                    }
                };
                actionMap.put(PREV, prevAction);
                inputMap.put(prevKey, PREV);
                if (textC != null) {
                    textC.getActionMap().put(PREV, prevAction);
                    textC.getInputMap().put(prevKey, PREV);
                }

                KeyStroke prevKey2 = ActionsSupport.registerAction(FIND_PREV_ACTION_KEY, prevAction, actionMap, inputMap);
                String prevAccelerator2 = ActionsSupport.keyAcceleratorString(prevKey2);
                if (prevAccelerator2 != null) prev.setToolTipText(MessageFormat.format(BTN_PREVIOUS_TOOLTIP,
                                                             prevAccelerator + ", " + prevAccelerator2)); // NOI18N
            }
            
            JPanel rightPanel = new JPanel(new BorderLayout());            
            rightPanel.add(feedback, BorderLayout.WEST);
            rightPanel.add(closeButton, BorderLayout.EAST);
            
            add(rightPanel, BorderLayout.EAST);
            
            setVisible(false);
        }
        
        private String getSearchString() {
            String search = combo.getText();
            return search == null ? "" : search.trim(); // NOI18N
        }
        
        private void setResultsFeedback(int index, int count, DocumentSearchResult result) {
            NumberFormat format = NumberFormat.getInstance();
            feedback.setText(MessageFormat.format(MATCHES_PATTERN, format.format(index + 1), format.format(count)));
            
            if (result != null) {
                if (highlightResults.isSelected()) result.highlightResults(area.getHighlighter());
                else result.clearHighlightedResults(area.getHighlighter());
            }
        }
        
        void clearResultsFeedback(DocumentSearchResult result) {
            feedback.setText(""); // NOI18N
            if (result != null) result.clearHighlightedResults(area.getHighlighter());
        }
        
        
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            
            clearResultsFeedback(DocumentSearchResult.current(area));
            
            if (visible) {
                String search = (String)area.getClientProperty(PROP_LAST_FIND_TEXT);
                textC.setText(search == null ? "" : search); // NOI18N
                
                boolean match = Boolean.parseBoolean((String)area.getClientProperty(PROP_LAST_FIND_MATCH_CASE));
                matchCase.setSelected(match);
                
                requestFocusInWindow();
            } else {
                if (getSearchString().isEmpty()) area.putClientProperty(PROP_LAST_FIND_TEXT, null);
                area.putClientProperty(PROP_LAST_FIND_MATCH_CASE, Boolean.toString(matchCase.isSelected()));
            }
        }
        
        public boolean requestFocusInWindow() {
            if (textC != null) {
                textC.selectAll();
                return textC.requestFocusInWindow();
            }
            return super.requestFocusInWindow();
        }
        
    }
    
    
    public static void enableSearchActions(final HTMLTextArea area) {
        ActionMap actionMapArea = area.getActionMap();
        InputMap inputMapArea = area.getInputMap();
        
        Action findAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String selected = area.getSelectedText();
                        if (selected != null) area.putClientProperty(PROP_LAST_FIND_TEXT, selected);
                        
                        ((SearchPanel)area.getClientProperty(PROP_SEARCH_PANEL)).setVisible(true);
                    }
                });
            }
        };
        ActionsSupport.registerAction(FIND_ACTION_KEY, findAction, actionMapArea, inputMapArea);
        
        Action nextAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String search = (String)area.getClientProperty(PROP_LAST_FIND_TEXT);
                        
                        if (search != null) {
                            boolean matchCase = Boolean.parseBoolean((String)area.getClientProperty(PROP_LAST_FIND_MATCH_CASE));
                            findString(area, search, matchCase, true);
                        } else {
                            ((SearchPanel)area.getClientProperty(PROP_SEARCH_PANEL)).setVisible(true);
                        }
                    }
                });
            }
        };
        ActionsSupport.registerAction(FIND_NEXT_ACTION_KEY, nextAction, actionMapArea, inputMapArea);
        
        Action prevAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String search = (String)area.getClientProperty(PROP_LAST_FIND_TEXT);
                        
                        if (search != null) {
                            boolean matchCase = Boolean.parseBoolean((String)area.getClientProperty(PROP_LAST_FIND_MATCH_CASE));
                            findString(area, search, matchCase, false);
                        } else {
                            ((SearchPanel)area.getClientProperty(PROP_SEARCH_PANEL)).setVisible(true);
                        }
                    }
                });
            }
        };
        ActionsSupport.registerAction(FIND_PREV_ACTION_KEY, prevAction, actionMapArea, inputMapArea);
        
        Action selAction = new AbstractAction() {
            public void actionPerformed(final ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        String selected = area.getSelectedText();
                        area.putClientProperty(PROP_LAST_FIND_TEXT, selected);
                        
                        if (selected != null) {
                            boolean matchCase = Boolean.parseBoolean((String)area.getClientProperty(PROP_LAST_FIND_MATCH_CASE));
                            findString(area, selected, matchCase, true);
                        } else {
                            ((SearchPanel)area.getClientProperty(PROP_SEARCH_PANEL)).setVisible(true);
                        }
                    }
                });
            }
        };
        ActionsSupport.registerAction(FIND_SEL_ACTION_KEY, selAction, actionMapArea, inputMapArea);
        
        
        
        Action closeAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ((SearchPanel)area.getClientProperty(PROP_SEARCH_PANEL)).setVisible(false);
                    }
                });
            }
        };
        actionMapArea.put(FIND_CLOSE_ACTION_KEY, closeAction);
        inputMapArea.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), FIND_CLOSE_ACTION_KEY);
    }
    
}
