/*
 * Copyright 2008 Ayman Al-Sairafi ayman.alsairafi@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License
 *       at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jsyntaxpane.components;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.actions.GotoLineDialog;
import jsyntaxpane.actions.ActionUtils;
import jsyntaxpane.util.Configuration;

/**
 * LineRuleis used to number the lines in the EdiorPane
 * @author Ayman Al-Sairafi
 */
public class LineNumbersRuler extends JComponent
        implements SyntaxComponent, PropertyChangeListener, DocumentListener {

    public static final String PROPERTY_BACKGROUND = "LineNumbers.Background";
    public static final String PROPERTY_FOREGROUND = "LineNumbers.Foreground";
    public static final String PROPERTY_LEFT_MARGIN = "LineNumbers.LeftMargin";
    public static final String PROPERTY_RIGHT_MARGIN = "LineNumbers.RightMargin";
    public static final int DEFAULT_R_MARGIN = 5;
    public static final int DEFAULT_L_MARGIN = 5;
    private JEditorPane pane;
    private String format;
    private int lineCount = -1;
    private int r_margin;
    private int l_margin;
    private int charHeight;
    private int charWidth;
    private GotoLineDialog gotoLineDialog = null;
    private MouseListener mouseListener = null;

    /**
     * The status is used to have proper propertyCHange support.  We need to know if we are INSTALLING
     * the component or DE-INSTALLING it
     */
    static enum Status {
        INSTALLING,
        DEINSTALLING
    }
    private Status status;

    public LineNumbersRuler() {
        super();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setFont(pane.getFont());
        Rectangle clip = g.getClipBounds();
        g.setColor(getBackground());
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
        g.setColor(getForeground());
        int lh = charHeight;
        int end = clip.y + clip.height + lh;
        int lineNum = clip.y / lh + 1;
        // round the start to a multiple of lh, and shift by 2 pixels to align
        // properly to the text.
        for (int y = (clip.y / lh) * lh + lh - 2; y <= end; y += lh) {
            String text = String.format(format, lineNum);
            g.drawString(text, l_margin, y);
            lineNum++;
            if (lineNum > lineCount) {
                break;
            }
        }
    }

    /**
     * Update the size of the line numbers based on the length of the document
     */
    private void updateSize() {
        int newLineCount = ActionUtils.getLineCount(pane);
        if (newLineCount == lineCount) {
            return;
        }
        lineCount = newLineCount;
        int h = lineCount * charHeight + pane.getHeight();
        int d = (int) Math.log10(lineCount) + 1;
        if (d < 1) {
            d = 1;
        }
        int w = d * charWidth + r_margin + l_margin;
        format = "%" + d + "d";
        setPreferredSize(new Dimension(w, h));
        if(getParent() != null){
            getParent().doLayout();
        }
    }

    /**
     * Get the JscrollPane that contains this EditorPane, or null if no
     * JScrollPane is the parent of this editor
     * @param editorPane
     * @return
     */
    public JScrollPane getScrollPane(JTextComponent editorPane) {
        Container p = editorPane.getParent();
        while (p != null) {
            if (p instanceof JScrollPane) {
                return (JScrollPane) p;
            }
            p = p.getParent();
        }
        return null;
    }

    public void config(Configuration config, String prefix) {
        r_margin = config.getPrefixInteger(prefix,
                PROPERTY_RIGHT_MARGIN, DEFAULT_R_MARGIN);
        l_margin = config.getPrefixInteger(prefix,
                PROPERTY_LEFT_MARGIN, DEFAULT_L_MARGIN);
        Color foreground = config.getPrefixColor(prefix,
                PROPERTY_FOREGROUND,
                Color.BLACK);
        setForeground(foreground);
        Color back = config.getPrefixColor(prefix,
                PROPERTY_BACKGROUND,
                Color.WHITE);
        setBackground(back);
    }

    public void install(JEditorPane editor) {
        this.pane = editor;
        charHeight = pane.getFontMetrics(pane.getFont()).getHeight();
        charWidth = pane.getFontMetrics(pane.getFont()).charWidth('0');
        editor.addPropertyChangeListener(this);
        JScrollPane sp = getScrollPane(pane);
        if (sp == null) {
            Logger.getLogger(this.getClass().getName()).warning(
                    "JEditorPane is not enclosed in JScrollPane, " +
                    "no LineNumbers will be displayed");
        } else {
            sp.setRowHeaderView(this);
            this.pane.getDocument().addDocumentListener(this);
            updateSize();
            gotoLineDialog = new GotoLineDialog(pane);
            mouseListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    gotoLineDialog.setVisible(true);
                }
            };
            addMouseListener(mouseListener);
        }
        status = Status.INSTALLING;
    }

    public void deinstall(JEditorPane editor) {
        removeMouseListener(mouseListener);
        status = Status.DEINSTALLING;
        JScrollPane sp = getScrollPane(editor);
        if (sp != null) {
            editor.getDocument().removeDocumentListener(this);
            sp.setRowHeaderView(null);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("document")) {
            if (evt.getOldValue() instanceof SyntaxDocument) {
                SyntaxDocument syntaxDocument = (SyntaxDocument) evt.getOldValue();
                syntaxDocument.removeDocumentListener(this);
            }
            if (evt.getNewValue() instanceof SyntaxDocument && status.equals(Status.INSTALLING)) {
                SyntaxDocument syntaxDocument = (SyntaxDocument) evt.getNewValue();
                syntaxDocument.addDocumentListener(this);
            }
        } else if (evt.getPropertyName().equals("font")) {
            charHeight = pane.getFontMetrics(pane.getFont()).getHeight();
            charWidth = pane.getFontMetrics(pane.getFont()).charWidth('0');
        }
    }

    public void insertUpdate(DocumentEvent e) {
        updateSize();
    }

    public void removeUpdate(DocumentEvent e) {
        updateSize();
    }

    public void changedUpdate(DocumentEvent e) {
        updateSize();
    }
}
