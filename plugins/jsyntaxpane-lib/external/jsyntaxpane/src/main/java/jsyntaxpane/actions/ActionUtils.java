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
package jsyntaxpane.actions;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.Token;

/**
 * Various utility methods to work on JEditorPane and its SyntaxDocument
 * for use by Actions
 *
 * @author Ayman Al-Sairafi
 */
public class ActionUtils {

    /**
     * Perform Smart Indentation:  pos must be on a line: this method will
     * use the previous lines indentation (number of spaces before any non-space
     * character or end of line) and return that as the prefix
     * @param line the line of text
     * @return
     */
    public static String getIndent(String line) {
        if (line == null || line.length() == 0) {
            return "";
        }
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return line.substring(0, i);
    }

    /**
     * Return the lines that span the selection (split as an array of Strings)
     * if there is no selection then current line is returned.
     * 
     * Note that the strings returned will not contain the terminating line feeds
     * 
     * The text component will then have the full lines set as selection
     * @param target
     * @return String[] of lines spanning selection / or Dot
     */
    public static String[] getSelectedLines(JTextComponent target) {
        String[] lines = null;
        try {
            PlainDocument pDoc = (PlainDocument) target.getDocument();
            int start = pDoc.getParagraphElement(target.getSelectionStart()).getStartOffset();
            int end;
            if (target.getSelectionStart() == target.getSelectionEnd()) {
                end = pDoc.getParagraphElement(target.getSelectionEnd()).getEndOffset();
            } else {
                // if more than one line is selected, we need to subtract one from the end
                // so that we do not select the line with the caret and no selection in it
                end = pDoc.getParagraphElement(target.getSelectionEnd() - 1).getEndOffset();
            }
            target.select(start, end);
            lines = pDoc.getText(start, end - start).split("\n");
            target.select(start, end);
        } catch (BadLocationException ex) {
            Logger.getLogger(ActionUtils.class.getName()).log(Level.SEVERE, null, ex);
            lines = EMPTY_STRING_ARRAY;
        }
        return lines;
    }

    /**
     * Return the line of text at the TextComponent's current position
     * @param target
     * @return
     */
    public static String getLine(JTextComponent target) {
        return getLineAt(target, target.getCaretPosition());
    }

    /**
     * Return the line of text at the given position.  The returned value may
     * be null.  It will not contain the trailing new-line character.
     * @param target the text component
     * @param pos char position
     * @return
     */
    public static String getLineAt(JTextComponent target, int pos) {
        String line = null;
        Document doc = target.getDocument();
        if (doc instanceof PlainDocument) {
            PlainDocument pDoc = (PlainDocument) doc;
            int start = pDoc.getParagraphElement(pos).getStartOffset();
            int end = pDoc.getParagraphElement(pos).getEndOffset();
            try {
                line = doc.getText(start, end - start);
                if (line != null && line.endsWith("\n")) {
                    line = line.substring(0, line.length() - 1);
                }
            } catch (BadLocationException ex) {
                Logger.getLogger(ActionUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return line;
    }

    /**
     * Returns the Frame that contains this component or null if the component
     * is not within a Window or the containing window is not a frame
     * @param comp
     * @return
     */
    public static Frame getFrameFor(Component comp) {
        Window w = SwingUtilities.getWindowAncestor(comp);
        if (w != null && w instanceof Frame) {
            Frame frame = (Frame) w;
            return frame;
        }
        return null;
    }

    /**
     * Returns the the Token at pos as a String
     * @param doc
     * @param pos
     * @return
     */
    public static String getTokenStringAt(
            SyntaxDocument doc, int pos) {
        String word = "";
        Token t = doc.getTokenAt(pos);
        if (t != null) {
            try {
                word = doc.getText(t.start, t.length);
            } catch (BadLocationException ex) {
                Logger.getLogger(ActionUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return word;
    }

    /**
     * A helper function that will return the SyntaxDocument attached to the
     * given text component.  Return null if the document is not a 
     * SyntaxDocument, or if the text component is null
     * @param component
     * @return
     */
    public static SyntaxDocument getSyntaxDocument(JTextComponent component) {
        if (component == null) {
            return null;
        }
        Document doc = component.getDocument();
        if (doc instanceof SyntaxDocument) {
            return (SyntaxDocument) doc;
        } else {
            return null;
        }
    }

    /**
     * Gets the Line Number at the give position of the editor component.
     * The first line number is ZERO
     * @param editor
     * @param pos
     * @return line number
     * @throws javax.swing.text.BadLocationException
     */
    public static int getLineNumber(JTextComponent editor, int pos)
            throws BadLocationException {
        if (getSyntaxDocument(editor) != null) {
            SyntaxDocument sdoc = getSyntaxDocument(editor);
            return sdoc.getLineNumberAt(pos);
        } else {
            Document doc = editor.getDocument();
            return doc.getDefaultRootElement().getElementIndex(pos);
        }
    }

    /**
     * Gets the column number at given position of editor.  The first column is
     * ZERO
     * @param editor
     * @param pos
     * @return the 0 based column number
     * @throws javax.swing.text.BadLocationException
     */
    public static int getColumnNumber(JTextComponent editor, int pos)
            throws BadLocationException {
        Rectangle r = editor.modelToView(pos);
        int start = editor.viewToModel(new Point(0, r.y));
        int column = pos - start;
        return column;
    }

    /**
     * Get the closest position within the document of the component that
     * has given line and column.  
     * @param editor
     * @param line
     * @param column
     * @return the closest positon for the text component at given line and
     * column
     */
    public static int getDocumentPosition(JTextComponent editor, int line,
            int column) {
        int lineHeight = editor.getFontMetrics(editor.getFont()).getHeight();
        int charWidth = editor.getFontMetrics(editor.getFont()).charWidth('m');
        int y = line * lineHeight;
        int x = column * charWidth;
        Point pt = new Point(x, y);
        int pos = editor.viewToModel(pt);
        return pos;
    }

    public static int getLineCount(JTextComponent pane) {
        SyntaxDocument sdoc = getSyntaxDocument(pane);
        if (sdoc != null) {
            return sdoc.getLineCount();
        }
        int count = 0;
        try {
            int p = pane.getDocument().getLength() - 1;
            if (p > 0) {
                count = getLineNumber(pane, p);
            }
        } catch (BadLocationException ex) {
            Logger.getLogger(ActionUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return count;
    }

    /**
     * Insert the given item into the combo box, and set it as first selected
     * item.  If the item already exists, it is removed, so there are no
     * duplicates.
     * @param combo
     * @param item
     */
    public static void insertIntoCombo(JComboBox combo, Object item) {
        MutableComboBoxModel model = (MutableComboBoxModel) combo.getModel();
        if (model.getSize() == 0) {
            model.insertElementAt(item, 0);
            return;
        }

        Object o = model.getElementAt(0);
        if (o.equals(item)) {
            return;
        }
        model.removeElement(item);
        model.insertElementAt(item, 0);
        combo.setSelectedIndex(0);
    }

    /**
     * Repeat the string source repeat times.
     * If repeats == 0 then empty String is returned
     * if source is null, then empty string is returned
     * @param source
     * @param repeat
     * @return source String repeated repeat times.
     */
    public static String repeatString(String source, int repeat) {
        if (repeat < 0) {
            throw new IllegalArgumentException("Cannot repeat " + repeat + " times.");
        }
        if (repeat == 0 || source == null || source.length() == 0) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < repeat; i++) {
            buffer.append(source);
        }
        return buffer.toString();
    }

    /**
     * Checks if the given string is null, empty or contains whitespace only
     * @param string
     * @return true if string is null, empty or contains whitespace only, false
     * otherwise.
     */
    public static boolean isEmptyOrBlanks(String string) {
        if (string == null || string.length() == 0) {
            return true;
        }
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (!Character.isWhitespace(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the TabStop property for the given text component, or 0 if not
     * used
     * @param text
     * @return
     */
    public static int getTabSize(JTextComponent text) {
        Integer tabs = (Integer) text.getDocument().getProperty(PlainDocument.tabSizeAttribute);
        return (null == tabs) ? 0 : tabs.intValue();
    }

    /**
     * Insert the given String into the textcomponent.  If the string contains
     * the | vertical BAr char, then it will not be inserted, and the cursor will
     * be set to its location.
     * If there are TWO vertical bars, then the text between them will be selected
     * <b>FIXME: add following feature
     * If the String is multi-line, then it will be indented with the same
     * indentattion as the line with pos.</b>
     * @param target
     * @param dot
     * @param toInsert
     * @throws javax.swing.text.BadLocationException
     */
    public static void insertMagicString(JTextComponent target, int dot, String toInsert)
            throws BadLocationException {
        Document doc = target.getDocument();
        if (toInsert.indexOf('|') >= 0) {
            int ofst = toInsert.indexOf('|');
            int ofst2 = toInsert.indexOf('|', ofst + 1);
            toInsert = toInsert.replace("|", "");
            doc.insertString(dot, toInsert, null);
            dot = target.getCaretPosition();
            final int strLength = toInsert.length();
            if (ofst2 > 0) {
                // note that we already removed the first |, so end offset is now
                // one less than what it was.
                target.select(dot + ofst - strLength, dot + ofst2 - strLength - 1);
            } else {
                target.setCaretPosition(dot + ofst -strLength);
            }
        } else {
            doc.insertString(dot, toInsert, null);
        }
    }
    // This is used internally to avoid NPE if we have no Strings
    static String[] EMPTY_STRING_ARRAY = new String[0];
    // This is used to quickly create Strings of at most 16 spaces (using substring)
    static String SPACES = "                ";
}
