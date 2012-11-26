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
import java.util.WeakHashMap;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import jsyntaxpane.util.Configuration;

/**
 * This actions displays the GotoLine dialog
 */
public class GotoLineAction extends TextAction implements SyntaxAction {

    private static WeakHashMap<JTextComponent, GotoLineDialog> DIALOGS =
            new WeakHashMap<JTextComponent, GotoLineDialog>();

    public GotoLineAction() {
        super("GOTO_LINE");
    }

    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        GotoLineDialog dlg = DIALOGS.get(target);
        if(dlg == null) {
            dlg = new GotoLineDialog(target);
            DIALOGS.put(target, dlg);
        }
        dlg.setVisible(true);
    }

    public void config(Configuration config, String prefix, String name) {
    }

    public TextAction getAction(String key) {
        return this;
    }
}
