/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.visualvm.demoapplicationtype;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.ui.DataSourceViewPlugin;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import javax.swing.JPanel;

/**
 *
 * @author geertjan
 */
public class AnagramOverview extends DataSourceViewPlugin {

    AnagramOverview(Application application) {
        super(application);
    }

    public DataViewComponent.DetailsView createView(int location) {
        switch (location) {
            case DataViewComponent.TOP_RIGHT:
                JPanel panel = new JPanel();
                return new DataViewComponent.DetailsView("User Interface", null, 30,
                        new ScrollableContainer(panel), null);
            default:
                return null;
        }
    }
}