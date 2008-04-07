package com.sun.tools.visualvm.application.snapshot;

import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.core.snapshot.SnapshotDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

public class ApplicationSnapshotDescriptor extends SnapshotDescriptor<ApplicationSnapshot> {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/application.png", true);
    private static final Image NODE_BADGE = Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/snapshotBadge.png", true);

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

        return icon != null ? Utilities.mergeImages(icon, NODE_BADGE, 0, 0) : null;
    }
}
