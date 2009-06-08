/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.modules.oqlsyntax;

import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.profiler.spi.*;

/**
 *
 * @author Jaroslav Bachorik <jaroslav.bachorik@sun.com>
 */
public class OQLSyntaxEditor extends OQLEditorImpl {

    private class DocumentListenerEx implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() > 0) {
                pcs.firePropertyChange(VALIDITY_PROPERTY, false, true);
            } else {
                pcs.firePropertyChange(VALIDITY_PROPERTY, true, false);
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() > 0) {
                pcs.firePropertyChange(VALIDITY_PROPERTY, false, true);
            } else {
                pcs.firePropertyChange(VALIDITY_PROPERTY, true, false);
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            System.out.println("fok");
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
