/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLWriter;
import org.graalvm.visualvm.lib.ui.UIUtils;


/**
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class HTMLTextArea extends JEditorPane implements HyperlinkListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /** Private Writer that extracts correctly formatted string from HTMLDocument */
    private static class ExtendedHTMLWriter extends HTMLWriter {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ExtendedHTMLWriter(Writer w, HTMLDocument doc, int pos, int len) {
            super(w, doc, pos, len);
            setLineLength(Integer.MAX_VALUE);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected boolean isSupportedBreakFlowTag(AttributeSet attr) {
            Object o = attr.getAttribute(StyleConstants.NameAttribute);

            if (o instanceof HTML.Tag) {
                HTML.Tag tag = (HTML.Tag) o;

                if ((tag == HTML.Tag.HTML) || (tag == HTML.Tag.HEAD) || (tag == HTML.Tag.BODY) || (tag == HTML.Tag.HR)) {
                    return false;
                }

                return (tag).breaksFlow();
            }

            return false;
        }

        protected void emptyTag(Element elem) throws BadLocationException, IOException {
            if (isSupportedBreakFlowTag(elem.getAttributes())) {
                writeLineSeparator();
            }

            if (matchNameAttribute(elem.getAttributes(), HTML.Tag.CONTENT)) {
                text(elem);
            }
        }

        protected void endTag(Element elem) throws IOException {
            if (isSupportedBreakFlowTag(elem.getAttributes())) {
                writeLineSeparator();
            }
        }

        protected void startTag(Element elem) throws IOException, BadLocationException {
        }
    }

    // --- Private classes for copy/paste support --------------------------------
    //
    // NOTE: only vertical formatting is correctly copy/pasted,
    //       horizontal formatting (ul, li) is ignored.

    /** Private TransferHandler that copies correctly formatted string from HTMLDocument to system clipboard */
    private class HTMLTextAreaTransferHandler extends TransferHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
            try {
                int selStart = getSelectionStart();
                int selLength = getSelectionEnd() - selStart;

                StringWriter plainTextWriter = new StringWriter();

                try {
                    new ExtendedHTMLWriter(plainTextWriter, (HTMLDocument) getDocument(), selStart, selLength).write();
                } catch (Exception e) {
                }

                String plainText = NcrToUnicode.decode(plainTextWriter.toString());
                clip.setContents(new StringSelection(plainText), null);

                if (action == TransferHandler.MOVE) {
                    getDocument().remove(selStart, selLength);
                }
            } catch (BadLocationException ble) {
            }
        }
    }

    /** Class for decoding strings from NCR to Unicode */
    private static class NcrToUnicode {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static Map entities;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public static String decode(String str) {
            StringBuilder ostr = new StringBuilder();
            int i1 = 0;
            int i2 = 0;

            while (i2 < str.length()) {
                i1 = str.indexOf('&', i2); //NOI18N

                if (i1 == -1) {
                    ostr.append(str.substring(i2, str.length()));

                    break;
                }

                ostr.append(str.substring(i2, i1));
                i2 = str.indexOf(';', i1); //NOI18N

                if (i2 == -1) {
                    ostr.append(str.substring(i1, str.length()));

                    break;
                }

                String tok = str.substring(i1 + 1, i2);

                if (tok.charAt(0) == '#') { //NOI18N

                    if (tok.equals("#160")) { //NOI18N
                        ostr.append((String) getEntities().get("nbsp")); //NOI18N // Fixes Issue 92818, "&nbsp;" is resolved as "&#160;" before decoding, so redirecting back to "&nbsp;"
                    } else {
                        tok = tok.substring(1);

                        try {
                            int radix = 10;

                            if (tok.trim().charAt(0) == 'x') { //NOI18N
                                radix = 16;
                                tok = tok.substring(1, tok.length());
                            }

                            ostr.append((char) Integer.parseInt(tok, radix));
                        } catch (NumberFormatException exp) {
                            ostr.append('?'); //NOI18N
                        }
                    }
                } else {
                    tok = (String) getEntities().get(tok);

                    if (tok != null) {
                        ostr.append(tok);
                    } else {
                        ostr.append('?'); //NOI18N
                    }
                }

                i2++;
            }

            return ostr.toString();
        }

        private static synchronized Map getEntities() {
            if (entities == null) {
                entities = new HashMap();
                //Quotation mark
                entities.put("quot", "\""); //NOI18N
                                            //Ampersand

                entities.put("amp", "\u0026"); //NOI18N
                                               //Less than

                entities.put("lt", "\u003C"); //NOI18N
                                              //Greater than

                entities.put("gt", "\u003E"); //NOI18N
                                              //Nonbreaking space

                entities.put("nbsp", "\u0020"); //NOI18N // Fixes Issue 92818, "\u00A0" (&nbsp; equivalent) is resolved as incorrect character, thus mapping to standard space
                                                //Inverted exclamation point

                entities.put("iexcl", "\u00A1"); //NOI18N
                                                 //Cent sign

                entities.put("cent", "\u00A2"); //NOI18N
                                                //Pound sign

                entities.put("pound", "\u00A3"); //NOI18N
                                                 //General currency sign

                entities.put("curren", "\u00A4"); //NOI18N
                                                  //Yen sign

                entities.put("yen", "\u00A5"); //NOI18N
                                               //Broken vertical bar

                entities.put("brvbar", "\u00A6"); //NOI18N
                                                  //Section sign

                entities.put("sect", "\u00A7"); //NOI18N
                                                //Umlaut

                entities.put("uml", "\u00A8"); //NOI18N
                                               //Copyright

                entities.put("copy", "\u00A9"); //NOI18N
                                                //Feminine ordinal

                entities.put("ordf", "\u00AA"); //NOI18N
                                                //Left angle quote

                entities.put("laquo", "\u00AB"); //NOI18N
                                                 //Not sign

                entities.put("not", "\u00AC"); //NOI18N
                                               //Soft hyphen

                entities.put("shy", "\u00AD"); //NOI18N
                                               //Registered trademark

                entities.put("reg", "\u00AE"); //NOI18N
                                               //Macron accent

                entities.put("macr", "\u00AF"); //NOI18N
                                                //Degree sign

                entities.put("deg", "\u00B0"); //NOI18N
                                               //Plus or minus

                entities.put("plusmn", "\u00B1"); //NOI18N
                                                  //Superscript 2

                entities.put("sup2", "\u00B2"); //NOI18N
                                                //Superscript 3

                entities.put("sup3", "\u00B3"); //NOI18N
                                                //Acute accent

                entities.put("acute", "\u00B4"); //NOI18N
                                                 //Micro sign (Greek mu)

                entities.put("micro", "\u00B5"); //NOI18N
                                                 //Paragraph sign

                entities.put("para", "\u00B6"); //NOI18N
                                                //Middle dot

                entities.put("middot", "\u00B7"); //NOI18N
                                                  //Cedilla

                entities.put("cedil", "\u00B8"); //NOI18N
                                                 //Superscript 1

                entities.put("sup1", "\u00B9"); //NOI18N
                                                //Masculine ordinal

                entities.put("ordm", "\u00BA"); //NOI18N
                                                //Right angle quote

                entities.put("raquo", "\u00BB"); //NOI18N
                                                 //Fraction one-fourth

                entities.put("frac14", "\u00BC"); //NOI18N
                                                  //Fraction one-half

                entities.put("frac12", "\u00BD"); //NOI18N
                                                  //Fraction three-fourths

                entities.put("frac34", "\u00BE"); //NOI18N
                                                  //Inverted question mark

                entities.put("iquest", "\u00BF"); //NOI18N
                                                  //Capital A, grave accent

                entities.put("Agrave", "\u00C0"); //NOI18N
                                                  //Capital A, acute accent

                entities.put("Aacute", "\u00C1"); //NOI18N
                                                  //Capital A, circumflex accent

                entities.put("Acirc", "\u00C2"); //NOI18N
                                                 //Capital A, tilde

                entities.put("Atilde", "\u00C3"); //NOI18N
                                                  //Capital A, umlaut

                entities.put("Auml", "\u00C4"); //NOI18N
                                                //Capital A, ring

                entities.put("Aring", "\u00C5"); //NOI18N
                                                 //Capital AE ligature

                entities.put("AElig", "\u00C6"); //NOI18N
                                                 //Capital C, cedilla

                entities.put("Ccedil", "\u00C7"); //NOI18N
                                                  //Capital E, grave accent

                entities.put("Egrave", "\u00C8"); //NOI18N
                                                  //Capital E, acute accent

                entities.put("Eacute", "\u00C9"); //NOI18N
                                                  //Capital E, circumflex accent

                entities.put("Ecirc", "\u00CA"); //NOI18N
                                                 //Capital E, umlaut

                entities.put("Euml", "\u00CB"); //NOI18N
                                                //Capital I, grave accent

                entities.put("Igrave", "\u00CC"); //NOI18N
                                                  //Capital I, acute accent

                entities.put("Iacute", "\u00CD"); //NOI18N
                                                  //Capital I, circumflex accent

                entities.put("Icirc", "\u00CE"); //NOI18N
                                                 //Capital I, umlaut

                entities.put("Iuml", "\u00CF"); //NOI18N
                                                //Capital eth, Icelandic

                entities.put("ETH", "\u00D0"); //NOI18N
                                               //Capital N, tilde

                entities.put("Ntilde", "\u00D1"); //NOI18N
                                                  //Capital O, grave accent

                entities.put("Ograve", "\u00D2"); //NOI18N
                                                  //Capital O, acute accent

                entities.put("Oacute", "\u00D3"); //NOI18N
                                                  //Capital O, circumflex accent

                entities.put("Ocirc", "\u00D4"); //NOI18N
                                                 //Capital O, tilde

                entities.put("Otilde", "\u00D5"); //NOI18N
                                                  //Capital O, umlaut

                entities.put("Ouml", "\u00D6"); //NOI18N
                                                //Multiply sign

                entities.put("times", "\u00D7"); //NOI18N
                                                 //Capital O, slash

                entities.put("Oslash", "\u00D8"); //NOI18N
                                                  //Capital U, grave accent

                entities.put("Ugrave", "\u00D9"); //NOI18N
                                                  //Capital U, acute accent

                entities.put("Uacute", "\u00DA"); //NOI18N
                                                  //Capital U, circumflex accent

                entities.put("Ucirc", "\u00DB"); //NOI18N
                                                 //Capital U, umlaut

                entities.put("Uuml", "\u00DC"); //NOI18N
                                                //Capital Y, acute accent

                entities.put("Yacute", "\u00DD"); //NOI18N
                                                  //Capital thorn, Icelandic

                entities.put("THORN", "\u00DE"); //NOI18N
                                                 //Small sz ligature, German

                entities.put("szlig", "\u00DF"); //NOI18N
                                                 //Small a, grave accent

                entities.put("agrave", "\u00E0"); //NOI18N
                                                  //Small a, acute accent

                entities.put("aacute", "\u00E1"); //NOI18N
                                                  //Small a, circumflex accent

                entities.put("acirc", "\u00E2"); //NOI18N
                                                 //Small a, tilde

                entities.put("atilde", "\u00E3"); //NOI18N
                                                  //Small a, umlaut

                entities.put("auml", "\u00E4"); //NOI18N
                                                //Small a, ring

                entities.put("aring", "\u00E5"); //NOI18N
                                                 //Small ae ligature

                entities.put("aelig", "\u00E6"); //NOI18N
                                                 //Small c, cedilla

                entities.put("ccedil", "\u00E7"); //NOI18N
                                                  //Small e, grave accent

                entities.put("egrave", "\u00E8"); //NOI18N
                                                  //Small e, acute accent

                entities.put("eacute", "\u00E9"); //NOI18N
                                                  //Small e, circumflex accent

                entities.put("ecirc", "\u00EA"); //NOI18N
                                                 //Small e, umlaut

                entities.put("euml", "\u00EB"); //NOI18N
                                                //Small i, grave accent

                entities.put("igrave", "\u00EC"); //NOI18N
                                                  //Small i, acute accent

                entities.put("iacute", "\u00ED"); //NOI18N
                                                  //Small i, circumflex accent

                entities.put("icirc", "\u00EE"); //NOI18N
                                                 //Small i, umlaut

                entities.put("iuml", "\u00EF"); //NOI18N
                                                //Small eth, Icelandic

                entities.put("eth", "\u00F0"); //NOI18N
                                               //Small n, tilde

                entities.put("ntilde", "\u00F1"); //NOI18N
                                                  //Small o, grave accent

                entities.put("ograve", "\u00F2"); //NOI18N
                                                  //Small o, acute accent

                entities.put("oacute", "\u00F3"); //NOI18N
                                                  //Small o, circumflex accent

                entities.put("ocirc", "\u00F4"); //NOI18N
                                                 //Small o, tilde

                entities.put("otilde", "\u00F5"); //NOI18N
                                                  //Small o, umlaut

                entities.put("ouml", "\u00F6"); //NOI18N
                                                //Division sign

                entities.put("divide", "\u00F7"); //NOI18N
                                                  //Small o, slash

                entities.put("oslash", "\u00F8"); //NOI18N
                                                  //Small u, grave accent

                entities.put("ugrave", "\u00F9"); //NOI18N
                                                  //Small u, acute accent

                entities.put("uacute", "\u00FA"); //NOI18N
                                                  //Small u, circumflex accent

                entities.put("ucirc", "\u00FB"); //NOI18N
                                                 //Small u, umlaut

                entities.put("uuml", "\u00FC"); //NOI18N
                                                //Small y, acute accent

                entities.put("yacute", "\u00FD"); //NOI18N
                                                  //Small thorn, Icelandic

                entities.put("thorn", "\u00FE"); //NOI18N
                                                 //Small y, umlaut

                entities.put("yuml", "\u00FF"); //NOI18N
            }

            return entities;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.components.Bundle"); // NOI18N
    private static final String CUT_STRING = messages.getString("HTMLTextArea_CutString"); // NOI18N
    private static final String COPY_STRING = messages.getString("HTMLTextArea_CopyString"); // NOI18N
    private static final String PASTE_STRING = messages.getString("HTMLTextArea_PasteString"); // NOI18N
    private static final String DELETE_STRING = messages.getString("HTMLTextArea_DeleteString"); // NOI18N
    private static final String SELECT_ALL_STRING = messages.getString("HTMLTextArea_SelectAllString"); // NOI18N
                                                                                                        // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private URL activeLink;

    private boolean showPopup = true;

    // --- Lazy setting text ---------------------------------------------------
    private String pendingText;
    private int pendingDot = -1;
    
    private String currentText;
    private boolean forceSetText;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public HTMLTextArea() {
        setContentType("text/html"); // NOI18N
        setEditable(false);
        setOpaque(true);
        setAutoscrolls(true);
        addHyperlinkListener(this);
        setTransferHandler(new HTMLTextAreaTransferHandler());
        setFont(UIManager.getFont("Label.font")); //NOI18N
        setBackground(UIUtils.getProfilerResultsBackground());
        
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE)
                    invokeSelectedLink();
            }
        });

        // Bugfix #185777, update text only if visible
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing() && pendingText != null) setText(pendingText);
                    }
                }
        });
    }

    public HTMLTextArea(String text) {
        this();
        setText(text);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    public EditorKit getEditorKitForContentType(String type) {
        // Always assumes "text/html" as this is a HTML displayer
        return new HTMLEditorKit();
    }
    
    public void setOpaque(boolean o) {
        super.setOpaque(o);
        if (UIUtils.isNimbusLookAndFeel() && !o)
            setBackground(new Color(0, 0, 0, 0));
    }

    public void setForeground(Color color) {
        Color foreground = getForeground();
        if (foreground != null && foreground.equals(color)) return;

        super.setForeground(color);
        forceSetText = true;
        setText(getText());
    }

    public void setShowPopup(boolean showPopup) {
        this.showPopup = showPopup;
    }

    public boolean getShowPopup() {
        return showPopup;
    }
    
    public void setCaretPosition(int position) {
        if (pendingText == null) super.setCaretPosition(position);
        else pendingDot = position;
    }

    public void setText(String value) {
        if (value == null) return;

        if (!isShowing() && !forceSetText) {

            pendingText = value;

        } else {

            if (!forceSetText && value.equals(currentText)) return;

            currentText = value;
            pendingText = null;

            Font font = getFont();
            Color textColor = getForeground();
            value = value.replaceAll("\\n\\r|\\r\\n|\\n|\\r", "<br>"); //NOI18N
            value = value.replace("<code>", "<code style=\"font-size: " + font.getSize() + "pt;\">"); //NOI18N

            String colorText = "rgb(" + textColor.getRed() + "," + textColor.getGreen() + "," + textColor.getBlue() + ")"; //NOI18N
            String newText = "<html><body text=\"" + colorText + "\" style=\"font-size: " + font.getSize() + //NOI18N
                             "pt; font-family: " + font.getName() + ";\">" + value + "</body></html>"; //NOI18N

            setDocument(getEditorKit().createDefaultDocument()); // Workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5042872
            super.setText(newText);

            if (pendingDot != -1) {
                try { setCaretPosition(pendingDot); }
                catch (IllegalArgumentException ex) {} // expected
                pendingDot = -1;
            }
        }

        forceSetText = false;
    }

    public String getText() {
        return pendingText != null ? pendingText : currentText;
    }

    public Dimension getMinimumSize() {
        if (pendingText != null) {
            forceSetText = true;
            setText(pendingText);
        }
        return super.getMinimumSize();
    }

    public Dimension getPreferredSize() {
        if (pendingText != null) {
            forceSetText = true;
            setText(pendingText);
        }
        return super.getPreferredSize();
    }

    public Dimension getMaximumSize() {
        if (pendingText != null) {
            forceSetText = true;
            setText(pendingText);
        }
        return super.getMaximumSize();
    }

    public void deleteSelection() {
        try {
            getDocument().remove(getSelectionStart(), getSelectionEnd() - getSelectionStart());
        } catch (Exception ex) {}
    }
    
    private void invokeSelectedLink() {
        for (Action action : getEditorKit().getActions()) {
            if ("activate-link-action".equals(action.getValue(Action.NAME))) {  // NOI18N
                action.actionPerformed(new ActionEvent(this, 0, null));
                return;
            }
        }
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (!isEnabled()) {
            return;
        }

        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            activeLink = e.getURL();
            showURL(activeLink, e.getInputEvent());
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            activeLink = e.getURL();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            activeLink = null;
            setCursor(Cursor.getDefaultCursor());
        }
    }
    
    public URL getActiveLink() {
        return activeLink;
    }
    
    protected void processMouseEvent(MouseEvent e) {
        if (e.isPopupTrigger()) showPopupMenu(e);
        super.processMouseEvent(e);
    }
    
    protected void processKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_CONTEXT_MENU ||
           (code == KeyEvent.VK_F10 && e.getModifiers() == InputEvent.SHIFT_MASK)) {
            e.consume();
            showPopupMenu(null);
        }
        
        super.processKeyEvent(e);
    }
    
    private void showPopupMenu(MouseEvent e) {
        if (isEnabled() && isFocusable() && showPopup) {
            JPopupMenu popup = new JPopupMenu();
            populatePopup(popup);

            if (popup.getComponentCount() > 0) {

                if (!hasFocus()) requestFocus(); // required for Select All functionality
                
                int x, y;
                if (e != null) {
                    x = e.getX();
                    y = e.getY();
                } else {
                    Rectangle vis = getVisibleRect();
                    x = vis.x + vis.width / 2;
                    y = vis.y + vis.height / 2;
                    
                    try {
                        Rectangle pos = modelToView(getCaretPosition());
                        if (pos != null) {
                            pos.width = Math.max(pos.width, 1); // must have nonzero width for the intersects() to work
                            if (vis.intersects(pos)) {
                                x = pos.x + pos.width;
                                y = pos.y + pos.height;
                            }
                        }
                    } catch (BadLocationException ex) {}
                }

                popup.show(this, x, y);
            }
        }
    }

    protected void populatePopup(JPopupMenu popup) {
        popup.add(createCutMenuItem());
        popup.add(createCopyMenuItem());
        popup.add(createPasteMenuItem());
        popup.add(createDeleteMenuItem());
        popup.addSeparator();
        popup.add(createSelectAllMenuItem());
        
        Action find = getActionMap().get(HTMLTextAreaSearchUtils.FIND_ACTION_KEY); 
        if (find != null) {
            popup.addSeparator();
            popup.add(new JMenuItem(find));
        }
    }
    
    protected JMenuItem createCutMenuItem() {
        return new JMenuItem(CUT_STRING) {
            { setEnabled(isEditable() && getSelectedText() != null); }
            protected void fireActionPerformed(ActionEvent e) { cut(); }
        };
    }
    
    protected JMenuItem createCopyMenuItem() {
        return new JMenuItem(COPY_STRING) {
            { setEnabled(getSelectedText() != null); }
            protected void fireActionPerformed(ActionEvent e) { copy(); }
        };
    }
    
    protected JMenuItem createPasteMenuItem() {
        return new JMenuItem(PASTE_STRING) {
            {
                if (isEditable()) {
                    try {
                        Transferable clipboardContent = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
                        setEnabled(clipboardContent != null && clipboardContent.isDataFlavorSupported(DataFlavor.stringFlavor));
                    } catch (Exception e) {
                        setEnabled(false);
                    }
                } else {
                    setEnabled(false);
                }
            }
            protected void fireActionPerformed(ActionEvent e) { paste(); }
        };
    }
    
    protected JMenuItem createDeleteMenuItem() {
        return new JMenuItem(DELETE_STRING) {
            {
                if (isEditable()) {
                    setEnabled(getSelectedText() != null);
                } else {
                    setVisible(false);
                }
            }
            protected void fireActionPerformed(ActionEvent e) { deleteSelection(); }
        };
    }
    
    protected JMenuItem createSelectAllMenuItem() {
        return new JMenuItem(SELECT_ALL_STRING) {
            protected void fireActionPerformed(ActionEvent e) { selectAll(); }
        };
    }
    
    public void paste() {
        try {
            replaceSelection(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this)
                                    .getTransferData(DataFlavor.stringFlavor).toString());
        } catch (Exception ex) {}
    }
    
    protected void showURL(URL url, InputEvent e) {
        showURL(url);
    }

    protected void showURL(URL url) {
        // override to react to URL clicks
    }
    
}
