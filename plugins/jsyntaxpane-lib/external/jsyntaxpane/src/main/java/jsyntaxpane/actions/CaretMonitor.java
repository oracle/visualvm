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

import javax.swing.JLabel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import jsyntaxpane.SyntaxDocument;

/**
 * This class can be used to display the caret location in friendly manner for
 * an EditorPane.
 *
 * FIXME: Add configurable position text using String.format and arrays of locations
 *
 * @author Ayman Al-Sairafi
 */
public class CaretMonitor implements CaretListener {

    private JLabel label;
    private JTextComponent text;

    public CaretMonitor(JTextComponent text, JLabel label) {
        this.label = label;
        this.text = text;
        text.addCaretListener(this);
    }

    public void caretUpdate(CaretEvent evt) {
        if (text.getDocument() instanceof SyntaxDocument) {
            try {
                if (text.getSelectionStart() == text.getSelectionEnd()) {
                    int pos = evt.getDot();
                    String loc = String.format("%d:%d (%d)",
                            ActionUtils.getLineNumber(text, pos) + 1,
                            ActionUtils.getColumnNumber(text, pos) + 1,
                            pos);
                    label.setText(loc);
                } else {
                    int start = text.getSelectionStart();
                    int end = text.getSelectionEnd();
                    String loc = String.format("%d:%d - %d:%d (%d)",
                            ActionUtils.getLineNumber(text, start) + 1,
                            ActionUtils.getColumnNumber(text, start) + 1,
                            ActionUtils.getLineNumber(text, end) + 1,
                            ActionUtils.getColumnNumber(text, end) + 1,
                            (end - start));
                    label.setText(loc);
                }
            } catch (BadLocationException ex) {
                label.setText("Ex: " + ex.getMessage());
            }
        } else {
            label.setText("Position");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        text.removeCaretListener(this);
        super.finalize();
    }
}
