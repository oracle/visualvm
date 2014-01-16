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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.util.Configuration;

/**
 * This Action duplicates the current line, or all the highlighted lines.
 * @author Ayman Al-Sairafi
 */
public class DuplicateLinesAction extends TextAction implements SyntaxAction {

    public DuplicateLinesAction() {
        super("DUPLICATE_LINES");
    }

    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null) {
            try {
                SyntaxDocument sDoc = (SyntaxDocument) target.getDocument();
                int st = sDoc.getLineStartOffset(target.getSelectionStart());
                int en = sDoc.getLineEndOffset(target.getSelectionEnd());
                String dupLines = sDoc.getText(st, en-st);
                sDoc.insertString(st, dupLines, null);
            } catch (BadLocationException ex) {
                Logger.getLogger(DuplicateLinesAction.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void config(Configuration config, String prefix, String name) {
    }

    public TextAction getAction(String key) {
        return this;
    }
}
