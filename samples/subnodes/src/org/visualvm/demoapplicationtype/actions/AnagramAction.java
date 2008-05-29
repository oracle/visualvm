package org.visualvm.demoapplicationtype.actions;


import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import org.visualvm.demoapplicationtype.application.AnagramApplication;
import org.visualvm.demoapplicationtype.applicationtype.AnagramApplicationType;

class AnagramAction extends SingleDataSourceAction<Application> {

    private static AnagramAction instance;
    
    public static synchronized AnagramAction instance() {
        if (instance == null) 
            instance = new AnagramAction();
        return instance;
    }
    
    private AnagramAction() {
        super(Application.class);
        putValue(NAME, "Anagram");
        putValue(SHORT_DESCRIPTION, "Anagram PID");
    }

    protected void actionPerformed(Application app, ActionEvent e) {
        JOptionPane.showMessageDialog(null, "PID: " + app.getPid());
    }

    protected boolean isEnabled(Application app) {
        return ApplicationTypeFactory.getApplicationTypeFor(app) instanceof AnagramApplicationType;
    }
}
