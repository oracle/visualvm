package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.openide.util.NbBundle;

class ProfilingResultsSupport extends JPanel {

    public ProfilingResultsSupport() {
        super();
        initComponents();
    }

    public DataViewComponent.DetailsView getDetailsView() {
        return new DataViewComponent.DetailsView(NbBundle.getMessage(ApplicationProfilerView.class, "MSG_Profiling_results"), null, 10, this, null);
    }

    public void setProfilingResultsDisplay(JComponent profilingResultsDisplay) {
        removeAll();
        if (profilingResultsDisplay != null) {
            add(profilingResultsDisplay);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(Color.WHITE);
    }
}
