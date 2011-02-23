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

/**
 * This action performs SmartIndentation each time VK_ENTER is pressed
 * SmartIndentation is inserting the same amount of spaces as
 * the line above.  May not be too smart, but good enough.
 */
public class SmartIndent extends TextAction {

    public SmartIndent() {
        super("SMART_INDENT");
    }

    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null) {
            String line = ActionUtils.getLine(target);
            target.replaceSelection("\n" + ActionUtils.getIndent(line));
        }
    }
}
