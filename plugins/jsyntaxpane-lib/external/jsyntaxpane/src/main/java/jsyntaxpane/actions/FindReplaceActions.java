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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import jsyntaxpane.SyntaxDocument;
import jsyntaxpane.util.Configuration;

/**
 * Finder class.  This class contains the general Find, Find Next,
 * Find Previous, and the Find Marker Actions.
 * 
 * Note that all Actions are subclasses of this class because all actions
 * require the find text to be shared among them.  This is the best approach
 * to have all Action classes share this same data.
 *
 * @author Ayman Al-Sairafi
 */
public class FindReplaceActions implements SyntaxAction {

    private Pattern pattern = null;
    private boolean wrap = true;
    private final FindDialogAction findDialogAction = new FindDialogAction();
    private final FindNextAction findNextAction = new FindNextAction();
    private ReplaceDialog dlg;

    public FindReplaceActions() {
    }

    public TextAction getFindDialogAction() {
        return findDialogAction;
    }

    public TextAction getFindNextAction() {
        return findNextAction;
    }

    public void config(Configuration config, String prefix, String name) {
    }

    public TextAction getAction(String key) {
        if(key.equals("FIND") ) {
        return findDialogAction;
        } else if(key.equals("REPLACE")) {
            return findDialogAction;
        } else if(key.equals("FIND_NEXT")) {
            return findNextAction;
        } else {
            throw new IllegalArgumentException("Bad Action: " + key);
        }
    }

    /**
     * This class displays the Find Dialog.  The dialog will use the pattern
     * and will update it once it is closed.
     */
    class FindDialogAction extends TextAction {

        public FindDialogAction() {
            super("FIND_ACTION");
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getTextComponent(e);
            if (target != null) {
                showDialog(target);
            }
        }
    }

    /**
     * This class performs a Find Next operation by using the current pattern
     */
    class FindNextAction extends TextAction {

        public FindNextAction() {
            super("FIND_NEXT");
        }

        public void actionPerformed(ActionEvent e) {
            // if we did not start searching, return now
            if (pattern == null) {
                return;
            }
            JTextComponent target = getTextComponent(e);
            doFindNext(target);
        }
    }

    /**
     * Display an OptionPane dialog that the search string is not found
     */
    public void msgNotFound() {
        JOptionPane.showMessageDialog(null,
                "Search String " + pattern + " not found",
                "Find", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Show the dialog
     * @param targetFrame
     * @param sDoc
     * @param target
     */
    private void showDialog(JTextComponent target) {
        if (dlg == null) {
            dlg = new ReplaceDialog(target, FindReplaceActions.this);
        }
        dlg.setVisible(true);
    }

    /**
     * Perform a FindNext operation on the given text component.  Position
     * the caret at the start of the next found pattern
     * @param target
     */
    public void doFindNext(JTextComponent target) {
        if (target == null || pattern == null) {
            return;
        }
        SyntaxDocument sDoc = ActionUtils.getSyntaxDocument(target);
        if (sDoc == null) {
            return;
        }
        int start = target.getCaretPosition() + 1;
        // we must advance the position by one, otherwise we will find
        // the same text again
        if (start >= sDoc.getLength()) {
            start = 0;
        }
        Matcher matcher = sDoc.getMatcher(pattern, start);
        if (matcher != null && matcher.find()) {
            // since we used an offset in the matcher, the matcher location
            // MUST be offset by that location
            target.select(matcher.start() + start, matcher.end() + start);
        } else {
            if (isWrap()) {
                matcher = sDoc.getMatcher(pattern);
                if (matcher != null && matcher.find()) {
                    target.select(matcher.start(), matcher.end());
                } else {
                    msgNotFound();
                }
            } else {
                msgNotFound();
            }
        }
    }

    /**
     * Perform a replace all operation on the given component.
     * Note that this create a new duplicate String big as the entire
     * document and then assign it to the target text component
     * @param target
     * @param replacement
     */
    public void replaceAll(JTextComponent target, String replacement) {
        SyntaxDocument sDoc = ActionUtils.getSyntaxDocument(target);
        if (pattern == null || sDoc == null) {
            return;
        }
        Matcher matcher = sDoc.getMatcher(pattern);
        String newText = matcher.replaceAll(replacement);
        target.setText(newText);
    }

    // - Getters and setters -------------------------------------------------
    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }
}