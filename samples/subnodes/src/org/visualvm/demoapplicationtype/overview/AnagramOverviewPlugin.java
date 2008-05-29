package org.visualvm.demoapplicationtype.overview;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.ui.DataSourceViewPlugin;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent.DetailsView;
import java.awt.BorderLayout;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class AnagramOverviewPlugin extends DataSourceViewPlugin {
    
    AnagramOverviewPlugin(DataSource ds) {
        super(ds);
    }
    
    public DetailsView createView(int position) {
        
        switch (position) {
            case DataViewComponent.TOP_RIGHT:
                //Create JPanel:
                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(false);
                
                //Add label with pic:
                JLabel label = new JLabel();
                label.setIcon(new ImageIcon(getClass().getResource("/org/visualvm/demoapplicationtype/resources/visual.png")));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(label,BorderLayout.CENTER);
                
                //Add JPanel:
                DataViewComponent.DetailsView details = new DataViewComponent.DetailsView(
                        "User interface",
                        "Picture representing the user interface",
                        30,
                        panel,
                        null);
                
                return details;
        }
        return null;
    }
    
}
