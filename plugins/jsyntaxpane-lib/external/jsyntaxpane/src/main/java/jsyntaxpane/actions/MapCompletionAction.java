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
import java.util.Map;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.Token;
import jsyntaxpane.util.Configuration;
import jsyntaxpane.util.JarServiceProvider;

/**
 * Completion Actions:
 * All completions are based on a simple String to String Map.
 */
public class MapCompletionAction extends TextAction implements SyntaxAction {

    Map<String, String> completions;

    public MapCompletionAction() {
        super("MAP_COMPLETION");
    }

    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null && target.getDocument() instanceof SyntaxDocument) {
            SyntaxDocument sDoc = (SyntaxDocument) target.getDocument();
            int dot = target.getCaretPosition();
            Token token = sDoc.getTokenAt(dot);
            if (token != null) {
                String abbriv = ActionUtils.getTokenStringAt(sDoc, dot);
                if (completions.containsKey(abbriv)) {
                    String completed = completions.get(abbriv);
                    if (completed.indexOf('|') >= 0) {
                        int ofst = completed.length() - completed.indexOf('|') - 1;
                        sDoc.replaceToken(token, completed.replace("|", ""));
                        target.setCaretPosition(target.getCaretPosition() - ofst);
                    } else {
                        sDoc.replaceToken(token, completed);
                    }
                }
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
        String completionsFile = config.getPrefixProperty(prefix, "Completions.File", "NONE");
        if(completionsFile != null) {
            completions = JarServiceProvider.readStringsMap(completionsFile);
        }
    }

    public TextAction getAction(String key) {
        return this;
    }
}
