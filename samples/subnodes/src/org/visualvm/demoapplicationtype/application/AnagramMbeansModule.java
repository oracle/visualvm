package org.visualvm.demoapplicationtype.application;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;
import org.visualvm.demoapplicationtype.model.AnagramModel;

public class AnagramMbeansModule extends AnagramApplication {

    private static final Image NODE_ICON = Utilities.loadImage("org/visualvm/demoapplicationtype/resources/application.png",
            true);
    private DataSourceDescriptor descriptor = new DataSourceDescriptor() {

        @Override
        public Image getIcon() {
            return NODE_ICON;
        }

        @Override
        public String getName() {
            return AnagramMbeansModule.this.getName();
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public int getAutoExpansionPolicy() {
            return DataSourceDescriptor.EXPAND_NEVER;
        }
    };

    public AnagramMbeansModule(String name, String objName, AnagramModel gfRoot) {
        super(name, objName, gfRoot);
    }

    @Override
    public void generateContents() {
    }

    @Override
    public DataSourceDescriptor getDescriptor() {
        return descriptor;
    }
}
