/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.modules.oqlsyntax;

import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.profiler.spi.*;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jaroslav Bachorik <jaroslav.bachorik@sun.com>
 */
@ServiceProvider(service=OQLEditorImpl.class)
public class OQLSyntaxEditor extends OQLEditorImpl {

    private class DocumentListenerEx implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() > 0) {
                getValidationCallback(e.getDocument()).callback(true);
            } else {
                getValidationCallback(e.getDocument()).callback(false);
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() > 0) {
                getValidationCallback(e.getDocument()).callback(true);
            } else {
                getValidationCallback(e.getDocument()).callback(false);
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() > 0) {
                getValidationCallback(e.getDocument()).callback(true);
            } else {
                getValidationCallback(e.getDocument()).callback(false);
            }
        }
    };

    @Override
    public JEditorPane getEditorPane() {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/x-oql");
        pane.getDocument().addDocumentListener(new DocumentListenerEx());
        return pane;
    }
}
