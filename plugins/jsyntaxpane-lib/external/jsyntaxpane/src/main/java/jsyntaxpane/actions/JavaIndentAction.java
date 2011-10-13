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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.TextAction;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.util.Configuration;

/**
 * This action performs Java Indentation each time VK_ENTER is pressed
 * Java Indentation is inserting the same amount of spaces as
 * the line above.
 * If the current line ends with a '{' character, then an additional virtual
 * tab is inserted.
 * If the trimmed current line ends with '}', then the line is unindented
 */
public class JavaIndentAction extends TextAction implements SyntaxAction {

    public JavaIndentAction() {
        super("JAVA_INDENT");
    }

    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null) {
            String line = ActionUtils.getLine(target);
            String prefix = ActionUtils.getIndent(line);
            Integer tabSize = (Integer) target.getDocument().getProperty(PlainDocument.tabSizeAttribute);
            if (line.trim().endsWith("{")) {
                prefix += ActionUtils.SPACES.substring(0, tabSize);
            }
            SyntaxDocument sDoc = ActionUtils.getSyntaxDocument(target);
            if (sDoc != null && line.trim().equals("}")) {
                int pos = target.getCaretPosition();
                int start = sDoc.getParagraphElement(pos).getStartOffset();
                int end = sDoc.getParagraphElement(pos).getEndOffset();
                if (end >= sDoc.getLength()) {
                    end--;
                }
                if (line.startsWith(ActionUtils.SPACES.substring(0, tabSize))) {
                    try {
                        sDoc.replace(start, end - start, line.substring(tabSize) + "\n", null);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(ActionUtils.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                    }
                } else {
                    target.replaceSelection("\n" + prefix);
                }
            } else {
                target.replaceSelection("\n" + prefix);
            }
        }
    }

    public void config(Configuration config, String prefix, String name) {
    }

    public TextAction getAction(String key) {
        return this;
    }
}
