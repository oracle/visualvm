/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hellovisualvm;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import org.openide.util.Utilities;

/**
 *
 * @author geertjan
 */
public class HelloWorldView extends DataSourceView {

    private DataViewComponent dvc;
    private Application application;
    private DataViewComponent view;
    //Make sure there is an image at this location in your project:
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/core/ui/resources/coredump.png"; // NOI18N

    public HelloWorldView(Application application) {
        super("Hello World", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60);
        this.application = application;
        view = createViewComponent();
    }

    @Override
    public DataViewComponent getView() {
        return view;
    }

    private DataViewComponent createViewComponent() {

        //Data area for master view:
        JEditorPane generalDataArea = new JEditorPane();
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

        //Panel, which we'll reuse in all four of our detail views for this sample:
        JPanel panel = new JPanel();

        //Master view:
        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView
                ("Hello World Overview", null, generalDataArea);

        //Configuration of master view:
        DataViewComponent.MasterViewConfiguration masterConfiguration = 
                new DataViewComponent.MasterViewConfiguration(false);

        //Add the master view and configuration view to the component:
        dvc = new DataViewComponent(masterView, masterConfiguration);

        //Add configuration details to the component, which are the show/hide checkboxes at the top:
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Hello World Details 1", true), DataViewComponent.TOP_LEFT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Hello World Details 2", true), DataViewComponent.TOP_RIGHT);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Hello World Details 3 & 4", true), DataViewComponent.BOTTOM_RIGHT);

        //Add detail views to the component:
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Hello World Details 1", null, panel, null), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Hello World Details 2", null, panel, null), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Hello World Details 3", null, panel, null), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(new DataViewComponent.DetailsView(
                "Hello World Details 4", null, panel, null), DataViewComponent.BOTTOM_RIGHT);

        return dvc;

    }
    
}