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

import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.Token;
import jsyntaxpane.util.Configuration;
import jsyntaxpane.util.JarServiceProvider;

/**
 * ComboBox like Completion Action:
 * This will display a list of items to choose from, its can be used similar to
 * IntelliSense
 *
 * @author Ayman Al-Sairafi
 */
public class ComboCompletionAction extends TextAction implements SyntaxAction {
    final private static Set<String> CLOSING = new HashSet<String>() {
        {
            add(")");
            add("}");
            add("[");
        }
    };
    final private static String MEMBER_SEPARATOR = ".";

    Map<String, String> completions;
    ComboCompletionDialog dlg;
    private String[] items;

    public ComboCompletionAction() {
        super("COMBO_COMPLETION");
    }

    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null && target.getDocument() instanceof SyntaxDocument) {
            SyntaxDocument sDoc = (SyntaxDocument) target.getDocument();
            int dot = target.getCaretPosition();
            Token token = sDoc.getTokenAt(dot);
            String abbrev = "";
            try {
                if (token != null) {
                    abbrev = token.getText(sDoc);
                    while (CLOSING.contains(abbrev)) {
                        token = sDoc.getTokenAt(token.start - 1);
                        abbrev = token.getText(sDoc);
                    }
                    if (MEMBER_SEPARATOR.equals(abbrev)) {
                        abbrev = "[" + ActionUtils.getTokenStringAt(sDoc, token.start - 1) + "]" + abbrev;
                    } else {
                        Token prev = sDoc.getTokenAt(token.start - 1);
                        if (prev != null && MEMBER_SEPARATOR.equals(prev.getText(sDoc))) {
                            abbrev = "[" + ActionUtils.getTokenStringAt(sDoc, prev.start - 1) + "]" + MEMBER_SEPARATOR + abbrev;
                        }
                    }
                    sDoc.remove(token.start, token.length);
                    dot = token.start;
                }

                Frame frame = ActionUtils.getFrameFor(target);
                if (dlg == null) {
                    dlg = new ComboCompletionDialog(frame, true, items);
                }
                dlg.setLocationRelativeTo(frame);
                Point p = frame.getLocation();
                // Get location of Dot in rt
                Rectangle rt = target.modelToView(dot);
                Point loc = new Point(rt.x, rt.y);
                // convert the location from Text Componet coordinates to
                // Frame coordinates...
                loc = SwingUtilities.convertPoint(target, loc, frame);
                // and then to Screen coordinates
                SwingUtilities.convertPointToScreen(loc, frame);
                dlg.setLocation(loc);
                dlg.setFonts(target.getFont());
                dlg.setText(abbrev);
                dlg.setVisible(true);
                String res = dlg.getResult();
                ActionUtils.insertMagicString(target, dot, res);
            } catch (BadLocationException ex) {
                Logger.getLogger(ComboCompletionAction.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * The completions will for now reside on another properties style file
     * referenced by prefix.Completions.File
     *
     * @param config
     * @param prefix
     * @param name
     */
    public void config(Configuration config, String prefix, String name) {
        // for now we will use just one list for anything.  This can be modified
        // by having a map from TokenType to String[] or something....
        items = config.getPrefixPropertyList(prefix, name + ".Items");
    }

    public TextAction getAction(String key) {
        return this;
    }
}
