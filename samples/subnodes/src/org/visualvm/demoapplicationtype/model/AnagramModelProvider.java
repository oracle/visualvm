package org.visualvm.demoapplicationtype.model;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.core.datasource.DataSourceProvider;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.visualvm.demoapplicationtype.applicationtype.AnagramApplicationType;
import java.util.Set;

public class AnagramModelProvider implements DataChangeListener<Application>, DataRemovedListener<Application> {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final AnagramModelProvider INSTANCE = new AnagramModelProvider();
    
    private final DataRemovedListener<Application> removalListener = new DataRemovedListener<Application>() {

                public void dataRemoved(Application app) {
                    processFinishedApplication(app);
                }
            };
            
    public AnagramModelProvider() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public void dataChanged(DataChangeEvent<Application> event) {
        if (event.getAdded().isEmpty() && event.getRemoved().isEmpty()) {
            // Initial event to deliver DataSources already created by the provider before registering to it as a listener
            // NOTE: already existing hosts are treated as new for this provider
            Set<Application> newApplications = event.getCurrent();

            for (Application app : newApplications) {
                processNewApplication(app);
            }
        } else {
            // Real delta event
            Set<Application> newApplications = event.getAdded();

            for (Application app : newApplications) {
                processNewApplication(app);
            }
        }
    }

    public static void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(INSTANCE, Application.class);
    }

    public static void shutdown() {
        DataSourceRepository.sharedInstance().removeDataChangeListener(INSTANCE);
    }
    
    public void dataRemoved(Application application) {
        processFinishedApplication(application);
    }

    private void processFinishedApplication(Application app) {
        // TODO: remove listener!!!
        Set<AnagramModel> roots = app.getRepository().getDataSources(AnagramModel.class);
        app.getRepository().removeDataSources(roots);
    }

    private void processNewApplication(final Application app) {
        if (ApplicationTypeFactory.getApplicationTypeFor(app) instanceof AnagramApplicationType) {
            AnagramModel am = new AnagramModel(app);
            app.getRepository().addDataSource(am);
            app.notifyWhenRemoved(removalListener);
        }
    }

}
