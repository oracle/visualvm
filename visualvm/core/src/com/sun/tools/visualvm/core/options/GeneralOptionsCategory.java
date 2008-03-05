/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.visualvm.core.options;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.spi.options.OptionsCategory;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

public final class GeneralOptionsCategory extends OptionsCategory {

    public Icon getIcon() {
        return new ImageIcon(Utilities.loadImage("com/sun/tools/visualvm/core/ui/resources/options.png"));
    }

    public String getCategoryName() {
        return NbBundle.getMessage(GeneralOptionsCategory.class, "OptionsCategory_Name_Core");
    }

    public String getTitle() {
        return NbBundle.getMessage(GeneralOptionsCategory.class, "OptionsCategory_Title_Core");
    }

    public OptionsPanelController create() {
        return new GeneralOptionsPanelController();
    }
}
