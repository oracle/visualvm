package org.visualvm.demoapplicationtype.application;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;
import org.visualvm.demoapplicationtype.model.AnagramModel;

public class AnagramMbeansModule extends AnagramApplication {

    private static final Image NODE_ICON = Utilities.loadImage("org/visualvm/demoapplicationtype/resources/application.png",
            true);
    private final Descriptor descriptor;

    public AnagramMbeansModule(String name, String objName, AnagramModel gfRoot) {
        super(name, objName, gfRoot);
        descriptor = new Descriptor();
    }

    @Override
    public void generateContents() {
    }

    @Override
    public DataSourceDescriptor getDescriptor() {
        return descriptor;
    }
    
    private class Descriptor extends DataSourceDescriptor<DataSource> {
        public Descriptor() {
            super(AnagramMbeansModule.this);
        }
        
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
    }
}
