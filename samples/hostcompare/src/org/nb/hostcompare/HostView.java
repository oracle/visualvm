
package org.nb.hostcompare;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.host.Host;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import org.openide.util.Utilities;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;

public class HostView extends DataSourceView {

    private DataViewComponent dvc;
    private static final String IMAGE_PATH = "org/graalvm/visualvm/coredump/resources/coredump.png"; // NOI18N

    private Host host;
    private Jvm jvm;

    public HostView(Host host) {
        super(host, "Host", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false);
        this.host = host;
    }

    @Override
    protected DataViewComponent createComponent() {

        //Data area for master view:
        JEditorPane generalDataArea = new JEditorPane();
        generalDataArea.setText("Below you see the system properties of" +
                " all running apps!");
        generalDataArea.setBorder(BorderFactory.createEmptyBorder(14, 8, 14, 8));

        //Master view:
        DataViewComponent.MasterView masterView =
                new DataViewComponent.MasterView("All System Properties",
                null, generalDataArea);

        //Configuration of master view:
        DataViewComponent.MasterViewConfiguration masterConfiguration =
                new DataViewComponent.MasterViewConfiguration(false);

        //Add the master view and configuration view to the component:
        dvc = new DataViewComponent(masterView, masterConfiguration);

        //Get all the applications deployed to the host:
        Set apps = host.getRepository().getDataSources(Application.class);

        //Get the iterator:
        Iterator it = apps.iterator();

        //Set count to zero:
        int count = 0;

        //Iterate through our applications:
        while (it.hasNext()) {

            //Increase the count:
            count = count + 1;

            //Now we have our application:
            Application app = (Application) it.next();

            //Get the process id:
            String pid = count + ": " + (String.valueOf(app.getPid()));

            //Get the system properties:
            Properties jvmProperties = null;
            jvm = JvmFactory.getJVMFor(app);
            if (jvm.isGetSystemPropertiesSupported()) {
                jvmProperties = jvm.getSystemProperties();
            }

            //Extrapolate the name from the type:
            ApplicationType appType = ApplicationTypeFactory.getApplicationTypeFor(app);
            String appName = appType.getName();

            //Put the first application top left:
            if (count == 1) {

                dvc.addDetailsView(new SystemPropertiesViewSupport(jvmProperties).getDetailsView(app, appName), DataViewComponent.TOP_LEFT);

//            //Put the second application top right:
            } else if (count == 2) {
                dvc.addDetailsView(new SystemPropertiesViewSupport(jvmProperties).getDetailsView(app, appName), DataViewComponent.TOP_RIGHT);

//
//            //Put the third application bottom left:    
            } else if (count == 3) {
                dvc.addDetailsView(new SystemPropertiesViewSupport(jvmProperties).getDetailsView(app, appName), DataViewComponent.BOTTOM_LEFT);

            //Put the fourth application bottom right:        
            } else if (count == 4) {
                dvc.addDetailsView(new SystemPropertiesViewSupport(jvmProperties).getDetailsView(app, appName), DataViewComponent.BOTTOM_RIGHT);

            //Put all other applications bottom right, 
            //which creates tabs within the bottom right tab    
            } else {
                dvc.addDetailsView(new SystemPropertiesViewSupport(jvmProperties).getDetailsView(app, appName), DataViewComponent.BOTTOM_RIGHT);
            }

        }

        return dvc;

    }
}
