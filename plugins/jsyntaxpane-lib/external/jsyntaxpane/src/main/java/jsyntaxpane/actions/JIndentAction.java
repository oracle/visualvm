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
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.util.Configuration;

public class JIndentAction extends TextAction implements SyntaxAction {

    /**
     * creates new JIndentAction.
     * Initial Code contributed by ser... AT mail.ru
     */
    public JIndentAction() {
        super("JINDENT");
    }

    /**
     * {@inheritDoc}
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null) {
            SyntaxDocument sDoc = ActionUtils.getSyntaxDocument(target);
            int pos = target.getCaretPosition();
            int start = sDoc.getParagraphElement(pos).getStartOffset();
            String line = ActionUtils.getLine(target);
            String lineToPos = line.substring(0, pos - start);
            String prefix = ActionUtils.getIndent(line);
            int tabSize = ActionUtils.getTabSize(target);
            if (lineToPos.trim().endsWith("{")) {
                prefix += ActionUtils.SPACES.substring(0, tabSize);
            } else {
                String noComment = sDoc.getUncommentedText(start, pos); // skip EOL comments

                if (noComment.trim().endsWith("{")) {
                    prefix += ActionUtils.SPACES.substring(0, tabSize);
                }
            }
            target.replaceSelection("\n" + prefix);
        }
    }


    public void config(Configuration config, String prefix, String name) {
    }

    public TextAction getAction(String key) {
        return this;
    }
}
