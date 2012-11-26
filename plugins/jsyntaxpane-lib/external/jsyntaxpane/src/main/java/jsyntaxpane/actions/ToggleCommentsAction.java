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
import java.security.KeyStore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.util.Configuration;

/**
 * This action will toggle comments on or off on selected whole lines.
 * 
 * @author Ayman Al-Sairafi
 */
public class ToggleCommentsAction extends TextAction implements SyntaxAction {

    protected String lineCommentStart = "// ";
    protected Pattern lineCommentPattern = null;

    /**
     * creates new JIndentAction.
     * Initial Code contributed by ser... AT mail.ru
     */
    public ToggleCommentsAction() {
        super("TOGGLE");
    }

    /**
     * {@inheritDoc}
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null && target.getDocument() instanceof SyntaxDocument) {
            String[] lines = ActionUtils.getSelectedLines(target);
            StringBuffer toggled = new StringBuffer();
            for (int i = 0; i < lines.length; i++) {
                Matcher m = lineCommentPattern.matcher(lines[i]);
                if (m.find()) {
                    toggled.append(m.replaceFirst("$2"));
                } else {
                    toggled.append(lineCommentStart);
                    toggled.append(lines[i]);
                }
                toggled.append('\n');
            }
            target.replaceSelection(toggled.toString());
        }
    }

    public void config(Configuration config, String prefix, String name) {
        // we need to escape the chars
        lineCommentStart = config.getPrefixProperty(prefix,
                name + ".LineComments", "// ").replace("\"", "");
        lineCommentPattern = Pattern.compile("(^" + lineCommentStart + ")(.*)");
    }

    public TextAction getAction(String key) {
        return this;
    }
}
