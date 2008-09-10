package com.sun.tools.visualvm.application.snapshot;

import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.snapshot.SnapshotDescriptor;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import java.awt.Image;
import org.openide.util.Utilities;

/**
 * DataSourceDescriptor for ApplicationSnapshot.
 *
 * @author Jiri Sedlacek
 */
public class ApplicationSnapshotDescriptor extends SnapshotDescriptor<ApplicationSnapshot> {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/application.png", true);    // NOI18N

    
    /**
     * Creates new instance of ApplicationSnapshotDescriptor.
     * 
     * @param snapshot ApplicationSnapshot for the descriptor.
     */
    public ApplicationSnapshotDescriptor(ApplicationSnapshot snapshot) {
        super(snapshot, resolveIcon(snapshot));
    }

    private static Image resolveIcon(ApplicationSnapshot snapshot) {
        Image icon = NODE_ICON;
        String persistedIconString = snapshot.getStorage().getCustomProperty(PROPERTY_ICON);
        if (persistedIconString != null) {
            Image persistedIcon = Utils.stringToImage(persistedIconString);
            if (persistedIcon != null) {
                icon = persistedIcon;
            }
        }

        return icon != null ? SnapshotsSupport.getInstance().createSnapshotIcon(icon) : null;
    }
}
