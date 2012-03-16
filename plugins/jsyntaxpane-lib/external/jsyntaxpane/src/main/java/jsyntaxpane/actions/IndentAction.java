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
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.TextAction;
import jsyntaxpane.util.Configuration;

/**
 * IndentAction is used to replace Tabs with spaces.  If there is selected
 * text, then the lines spanning the selection will be shifted
 * right by one tab-width space  character
 */
public class IndentAction extends DefaultEditorKit.InsertTabAction 
        implements SyntaxAction {

    public IndentAction() {
        super();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null) {
            String selected = target.getSelectedText();
            if (selected == null) {
                PlainDocument pDoc = (PlainDocument) target.getDocument();
                Integer tabStop = (Integer) pDoc.getProperty(PlainDocument.tabSizeAttribute);
                int lineStart = pDoc.getParagraphElement(target.getCaretPosition()).getStartOffset();
                int column = target.getCaretPosition() - lineStart;
                int needed = tabStop - (column % tabStop);
                target.replaceSelection(ActionUtils.SPACES.substring(0, needed));
            } else {
                String[] lines = ActionUtils.getSelectedLines(target);
                int start = target.getSelectionStart();
                StringBuilder sb = new StringBuilder();
                for (String line : lines) {
                    sb.append('\t');
                    sb.append(line);
                    sb.append('\n');
                }
                target.replaceSelection(sb.toString());
                target.select(start, start + sb.length());
            }
        }
    }

    public void config(Configuration config, String prefix, String name) {
    }

    public TextAction getAction(String key) {
        return this;
    }
}
