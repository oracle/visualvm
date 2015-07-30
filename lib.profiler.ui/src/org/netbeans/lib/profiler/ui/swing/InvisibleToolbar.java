
package org.netbeans.lib.profiler.ui.swing;

import java.awt.Insets;
import javax.swing.BorderFactory;

/**
 *
 * @author Jiri Sedlacek
 */
public class InvisibleToolbar extends GenericToolbar {
    
    public InvisibleToolbar() { super(); tweak(); }
    
    public InvisibleToolbar(int orientation) { super(orientation); tweak(); }
    
    public InvisibleToolbar(String name) { super(name); tweak(); }
    
    public InvisibleToolbar(String name, int orientation) { super(name, orientation); tweak(); }
    
    
    private void tweak() {
        setBorder(BorderFactory.createEmptyBorder());
        setBorderPainted(false);
        setFloatable(false);
        setRollover(true);
        setOpaque(false);
        setMargin(new Insets(0, 0, 0, 0));
        
        putClientProperty("Toolbar.noGTKBorder", Boolean.TRUE); // NOI18N
    }
    
}
