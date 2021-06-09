/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.gotosource.viewer.internal;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "InternalSourceAppearance_FontPlain=Plain",                                 // NOI18N
    "InternalSourceAppearance_FontBold=Bold",                                   // NOI18N
    "InternalSourceAppearance_FontItalic=Italic",                               // NOI18N
    "InternalSourceAppearance_FontBoldItalic=Bold Italic",                      // NOI18N
    "InternalSourceAppearance_FontLabel=&Font:",                                // NOI18N
    "InternalSourceAppearance_StyleLabel=S&tyle:",                              // NOI18N
    "InternalSourceAppearance_SizeLabel=S&ize:"                                 // NOI18N
})
final class InternalSourceAppearance {
    
    private static final String PROP_FONT_NAME = "prop_InternalSourceAppearance_fontName";      // NOI18N
    private static final String PROP_FONT_STYLE = "prop_InternalSourceAppearance_fontStyle";    // NOI18N
    private static final String PROP_FONT_SIZE = "prop_InternalSourceAppearance_fontSize";      // NOI18N
    
    private static final int DEFAULT_FONT_STYLE = Font.PLAIN;
    private static final int DEFAULT_FONT_SIZE = new JTextArea().getFont().getSize();
    private static final String DEFAULT_FONT_NAME = new Font(Font.MONOSPACED, DEFAULT_FONT_STYLE, DEFAULT_FONT_SIZE).getName();
    
    
    private static enum FontStyle {
        
        PLAIN(Font.PLAIN, Bundle.InternalSourceAppearance_FontPlain()),
        BOLD(Font.BOLD, Bundle.InternalSourceAppearance_FontBold()),
        ITALIC(Font.ITALIC, Bundle.InternalSourceAppearance_FontItalic()),
        BOLD_ITALIC(Font.BOLD | Font.ITALIC, Bundle.InternalSourceAppearance_FontBoldItalic());
        
        
        private final int style;
        private final String name;
        
        FontStyle(int style, String name) {
            this.style = style;
            this.name = name;
        }
        
        int getStyle() { return style; }
        String getName() { return name; }
        
        @Override public String toString() { return getName(); }
        
        static FontStyle fromStyle(int style) {
            switch (style) {
                case Font.PLAIN: return PLAIN;
                case Font.BOLD: return BOLD;
                case Font.ITALIC: return ITALIC;
                default: return BOLD_ITALIC;
            }
        }
        
    };
    
    
    private final PropertyChangeSupport changeSupport;
    
    private JPanel settingsPanel;
    
    
    InternalSourceAppearance() {
        changeSupport = new PropertyChangeSupport(this);
    }
    
    
    Font getFont() {
        Preferences preferences = NbPreferences.forModule(InternalSourceAppearance.class);
        return new Font(savedFontName(preferences), savedFontStyle(preferences), savedFontSize(preferences));
    }
    
    
    void addListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    void removeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    
    void loadSettings() {
        if (settingsPanel == null) return;
        
        Preferences settings = NbPreferences.forModule(InternalSourceAppearance.class);
        fontSelect.setSelectedItem(savedFontName(settings));
        styleSelect.setSelectedItem(FontStyle.fromStyle(savedFontStyle(settings)));
        sizeSelect.setValue(Integer.valueOf(savedFontSize(settings)));
    }
    
    void saveSettings() {
        if (settingsPanel == null || !currentSettingsDirty()) return;
        
        Preferences settings = NbPreferences.forModule(InternalSourceAppearance.class);
        settings.put(PROP_FONT_NAME, currentFontName());
        settings.putInt(PROP_FONT_STYLE, currentFontStyle());
        settings.putInt(PROP_FONT_SIZE, currentFontSize());
        
        changeSupport.firePropertyChange(new PropertyChangeEvent(this, "appearance", null, null)); // NOI18N
    }
    
    boolean currentSettingsDirty() {
        if (settingsPanel == null) return false;
        
        Preferences settings = NbPreferences.forModule(InternalSourceAppearance.class);
        if (!currentFontName().equals(savedFontName(settings))) return true;
        if (currentFontStyle() != savedFontStyle(settings)) return true;
        if (currentFontSize() != savedFontSize(settings)) return true;
        
        return false;
    }
    
    
    private String savedFontName(Preferences preferences) {
        return preferences.get(PROP_FONT_NAME, DEFAULT_FONT_NAME).trim();
    }
    
    private String currentFontName() {
        return fontSelect.getEditor().getItem().toString().trim();
    }
    
    private int savedFontStyle(Preferences preferences) {
        return preferences.getInt(PROP_FONT_STYLE, DEFAULT_FONT_STYLE);
    }
    
    private int currentFontStyle() {
        return ((FontStyle)styleSelect.getSelectedItem()).getStyle();
    }
    
    private int savedFontSize(Preferences preferences) {
        return preferences.getInt(PROP_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    
    private int currentFontSize() {
        try {
            return Integer.parseInt(((JSpinner.DefaultEditor)sizeSelect.getEditor()).getTextField().getText().trim());
        } catch (Exception e) {
            return ((Integer)sizeSelect.getValue()).intValue();
        }
    }
    
    
    JComponent getSettingsComponent() {
        if (settingsPanel == null) {
            settingsPanel = new JPanel(null);
            settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.LINE_AXIS));
            settingsPanel.setOpaque(false);
            
            int tab = 15;
            int gap = 5;
            
            JLabel fontCaption = new JLabel();
            Mnemonics.setLocalizedText(fontCaption, Bundle.InternalSourceAppearance_FontLabel());
            settingsPanel.add(fontCaption);
            
            settingsPanel.add(Box.createHorizontalStrut(gap));
            
            fontSelect = new JComboBox(getAvailableFonts(false));
            fontSelect.setSelectedItem(new Font(Font.MONOSPACED, Font.PLAIN, 12).getName());
            Dimension dim = fontSelect.getMinimumSize();
            dim.width = 20;
            fontSelect.setMinimumSize(dim);
            fontSelect.setMaximumSize(fontSelect.getPreferredSize());
            fontSelect.setEditable(true);
            fontCaption.setLabelFor(fontSelect);
            settingsPanel.add(fontSelect);
            
            settingsPanel.add(Box.createHorizontalStrut(tab));
            
            JLabel styleCaption = new JLabel();
            Mnemonics.setLocalizedText(styleCaption, Bundle.InternalSourceAppearance_StyleLabel());
            settingsPanel.add(styleCaption);
            
            settingsPanel.add(Box.createHorizontalStrut(gap));
            
            styleSelect = new JComboBox(FontStyle.values());
            styleSelect.setPreferredSize(styleSelect.getMinimumSize());
            styleSelect.setMaximumSize(styleSelect.getMinimumSize());
            styleCaption.setLabelFor(styleSelect);
            settingsPanel.add(styleSelect);
            
            settingsPanel.add(Box.createHorizontalStrut(tab));
            
            JLabel sizeCaption = new JLabel();
            Mnemonics.setLocalizedText(sizeCaption, Bundle.InternalSourceAppearance_SizeLabel());
            settingsPanel.add(sizeCaption);
            
            settingsPanel.add(Box.createHorizontalStrut(gap));
            
            sizeSelect = new JSpinner(new SpinnerNumberModel(sizeCaption.getFont().getSize(), 1, 99, 1));
            try {
                JFormattedTextField editor = ((JSpinner.DefaultEditor)sizeSelect.getEditor()).getTextField();
                ((DefaultFormatter)editor.getFormatter()).setAllowsInvalid(false);
            } catch (Exception e) {}
            sizeSelect.setPreferredSize(sizeSelect.getMinimumSize());
            sizeSelect.setMaximumSize(sizeSelect.getMinimumSize());
            sizeCaption.setLabelFor(sizeSelect);
            settingsPanel.add(sizeSelect);
            
            loadSettings();
        }
        
        return settingsPanel;
    }
    
    
    private static String[] getAvailableFonts(boolean monospaced) {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    }
    
    
    private JComboBox<String> fontSelect;
    private JComboBox<FontStyle> styleSelect;
    private JSpinner sizeSelect;
    
}
