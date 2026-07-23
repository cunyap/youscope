/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/

package org.youscope.plugin.flatlaf;

import java.awt.Color;

import javax.swing.UIManager;

import org.youscope.addon.AddonException;
import org.youscope.addon.AddonMetadata;
import org.youscope.addon.AddonMetadataAdapter;
import org.youscope.addon.skin.Skin;

/**
 * Red microscopy skin using FlatLaf with a custom dark-red theme.
 * Preserves dark adaptation of the researcher's eyes.
 * Uses FlatLaf custom theme properties for the deep red color scheme.
 */
class FlatLafRedSkin implements Skin
{
    public static final String TYPE_IDENTIFIER = "YouScope.Skins.FlatRed";

    @Override
    public AddonMetadata getMetadata() { return createMetadata(); }

    static AddonMetadata createMetadata()
    {
        return new AddonMetadataAdapter(TYPE_IDENTIFIER,
            "Microscopy Red",
            new String[]{"Skins"},
            "Deep red flat theme for dark-room microscopy. Preserves night vision / dark adaptation.",
            "icons/system-monitor.png");
    }

    // Deep red palette
    private static final Color BG_DEEP     = new Color(15,  0,  0);
    private static final Color BG_PANEL    = new Color(28,  4,  4);
    private static final Color BG_CONTROL  = new Color(40,  8,  8);
    private static final Color FG_PRIMARY  = new Color(220, 60, 60);
    private static final Color FG_DIM      = new Color(140, 30, 30);
    private static final Color ACCENT      = new Color(180, 20, 20);

    @Override
    public void applySkin() throws AddonException
    {
        Thread.currentThread().setContextClassLoader(FlatLafRedSkin.class.getClassLoader());
        UIManager.getDefaults().clear();

        FlatLafLightSkin.applyQuickLogger(BG_DEEP, FG_DIM, FG_PRIMARY);
        FlatLafLightSkin.applyDesktopPane(BG_DEEP,
            "org/youscope/plugin/flatlafskin/images/background-logo.png");
        FlatLafLightSkin.applyHtmlStyles("#dc3c3c");

        try
        {
            // Use FlatDarkLaf as the base and override colors via UIManager properties
            // FlatLaf supports per-component color overrides AFTER setLookAndFeel
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
        }
        catch (Exception e)
        {
            throw new AddonException("FlatLaf not found. Ensure flatlaf-*.jar is in YouScope/lib/.", e);
        }

        // Override FlatLaf's default dark colors with our red palette
        // These UIManager keys are documented in FlatLaf's component property reference
        javax.swing.plaf.ColorUIResource bg     = new javax.swing.plaf.ColorUIResource(BG_PANEL);
        javax.swing.plaf.ColorUIResource bgDeep = new javax.swing.plaf.ColorUIResource(BG_DEEP);
        javax.swing.plaf.ColorUIResource bgCtrl = new javax.swing.plaf.ColorUIResource(BG_CONTROL);
        javax.swing.plaf.ColorUIResource fg      = new javax.swing.plaf.ColorUIResource(FG_PRIMARY);
        javax.swing.plaf.ColorUIResource fgDim   = new javax.swing.plaf.ColorUIResource(FG_DIM);
        javax.swing.plaf.ColorUIResource acc     = new javax.swing.plaf.ColorUIResource(ACCENT);

        String[] bgKeys = {
            "Panel.background", "OptionPane.background", "ScrollPane.background",
            "Viewport.background", "TabbedPane.background", "SplitPane.background",
            "ToolBar.background", "MenuBar.background", "PopupMenu.background",
            "Menu.background", "MenuItem.background", "CheckBoxMenuItem.background",
            "RadioButtonMenuItem.background", "Separator.background",
            "InternalFrame.background", "DesktopIcon.background",
            "Table.background", "TableHeader.background", "Tree.background",
            "List.background", "ComboBox.background", "Spinner.background",
            "TextField.background", "TextArea.background", "TextPane.background",
            "EditorPane.background", "PasswordField.background",
            "FormattedTextField.background", "ProgressBar.background",
            "ScrollBar.background", "Slider.background",
            "CheckBox.background", "RadioButton.background",
            "ToggleButton.background", "Button.background",
            "TitledBorder.background", "ToolTip.background",
            "Label.background", "TabbedPane.tabAreaBackground"
        };
        String[] fgKeys = {
            "Panel.foreground", "Label.foreground", "Button.foreground",
            "ToggleButton.foreground", "CheckBox.foreground", "RadioButton.foreground",
            "ComboBox.foreground", "Spinner.foreground", "TextField.foreground",
            "TextArea.foreground", "TextPane.foreground", "EditorPane.foreground",
            "EditorPane.inactiveForeground", "PasswordField.foreground",
            "FormattedTextField.foreground", "Table.foreground",
            "TableHeader.foreground", "Tree.foreground", "List.foreground",
            "Menu.foreground", "MenuItem.foreground", "MenuBar.foreground",
            "TabbedPane.foreground", "TitledBorder.titleColor",
            "ToolTip.foreground", "ProgressBar.foreground"
        };
        for (String k : bgKeys) UIManager.getDefaults().put(k, bg);
        for (String k : fgKeys) UIManager.getDefaults().put(k, fg);
        UIManager.getDefaults().put("Component.focusColor",           acc);
        UIManager.getDefaults().put("Component.linkColor",            FG_PRIMARY);
        UIManager.getDefaults().put("Button.hoverBackground",         bgCtrl);
        UIManager.getDefaults().put("Button.pressedBackground",       bgDeep);
        UIManager.getDefaults().put("ScrollBar.thumb",                acc);
        UIManager.getDefaults().put("ScrollBar.thumbHoverColor",      FG_PRIMARY);
        UIManager.getDefaults().put("TabbedPane.selectedBackground",  bgCtrl);
        UIManager.getDefaults().put("TabbedPane.underlineColor",      acc);
        UIManager.getDefaults().put("MenuItem.selectionBackground",   bgCtrl);
        UIManager.getDefaults().put("MenuItem.selectionForeground",   fg);
        UIManager.getDefaults().put("Menu.selectionBackground",       bgCtrl);
        UIManager.getDefaults().put("Menu.selectionForeground",       fg);
        UIManager.getDefaults().put("List.selectionBackground",       bgCtrl);
        UIManager.getDefaults().put("Table.selectionBackground",      bgCtrl);
        UIManager.getDefaults().put("Tree.selectionBackground",       bgCtrl);
        UIManager.getDefaults().put("TextField.selectionBackground",  acc);
        UIManager.getDefaults().put("TextField.caretForeground",      fg);
        UIManager.getDefaults().put("TextArea.caretForeground",       fg);
    }
}