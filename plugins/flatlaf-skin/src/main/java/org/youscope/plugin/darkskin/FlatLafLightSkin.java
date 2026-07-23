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
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.youscope.addon.AddonException;
import org.youscope.addon.AddonMetadata;
import org.youscope.addon.AddonMetadataAdapter;
import org.youscope.addon.skin.Skin;
import org.youscope.uielements.ImageLoadingTools;
import org.youscope.uielements.QuickLogger;
import org.youscope.uielements.plaf.BasicQuickLoggerUI;
import org.youscope.uielements.plaf.ImageDesktopPaneUI;

/**
 * Modern flat light skin using FlatLaf.
 * Replaces the JTattoo-based system skin with a crisp, HiDPI-aware flat look.
 */
class FlatLafLightSkin implements Skin
{
    public static final String TYPE_IDENTIFIER = "YouScope.Skins.FlatLight";

    @Override
    public AddonMetadata getMetadata() { return createMetadata(); }

    static AddonMetadata createMetadata()
    {
        return new AddonMetadataAdapter(TYPE_IDENTIFIER,
            "Modern Light",
            new String[]{"Skins"},
            "Clean flat light theme (FlatLaf). HiDPI-aware, works on Windows, Linux and macOS.",
            "icons/application-blog.png");
    }

    @Override
    public void applySkin() throws AddonException
    {
        Thread.currentThread().setContextClassLoader(FlatLafLightSkin.class.getClassLoader());
        UIManager.getDefaults().clear();

        applyQuickLogger(new Color(248, 248, 248), new Color(0.4f, 0.4f, 0.4f), Color.BLACK);
        applyDesktopPane(new Color(220, 220, 225), null);
        applyHtmlStyles("#000000");

        try
        {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        }
        catch (Exception e)
        {
            // Fallback to system LAF if FlatLaf JAR is missing
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e2) { throw new AddonException("Could not set Look and Feel.", e2); }
        }
    }

    static void applyQuickLogger(Color bg, Color dateFg, Color msgFg)
    {
        UIManager.getDefaults().put(QuickLogger.UI_CLASS_ID, BasicQuickLoggerUI.class.getName());
        UIManager.getDefaults().put(BasicQuickLoggerUI.PROPERTY_BACKGROUND, new ColorUIResource(bg));
        UIManager.getDefaults().put(BasicQuickLoggerUI.PROPERTY_DATE_FOREGROUND, new ColorUIResource(dateFg));
        UIManager.getDefaults().put(BasicQuickLoggerUI.PROPERTY_MESSAGE_FOREGROUND, new ColorUIResource(msgFg));
    }

    static void applyDesktopPane(Color bg, String iconPath)
    {
        UIManager.getDefaults().put("DesktopPaneUI", ImageDesktopPaneUI.class.getName());
        UIManager.getDefaults().put(ImageDesktopPaneUI.PROPERTY_BACKGROUND_COLOR, new ColorUIResource(bg));
        // Load logo from the given resource path. If the path is null or the
        // resource is not found, ImageDesktopPaneUI shows just the solid color.
        // To use the existing dark-skin logo, pass:
        //   "org/youscope/plugin/darkskin/images/background-logo.png"
        // but ensure youscope-dark-skin.jar is on the classloader path.
        // Default: no logo (solid background color only).
        if (iconPath != null)
        {
            javax.swing.Icon icon = ImageLoadingTools.getResourceIcon(iconPath, "Logo");
            if (icon != null)
                UIManager.getDefaults().put(ImageDesktopPaneUI.PROPERTY_BACKGROUND_ICON, icon);
        }
    }

    static void applyHtmlStyles(String color)
    {
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet ss = kit.getStyleSheet();
        String f = "font-family:sans-serif;font-size:12pt;margin-top:0px;margin-bottom:4px";
        ss.addRule("p  {color:" + color + ";" + f + "}");
        ss.addRule("a  {color:" + color + ";" + f + "}");
        ss.addRule("li {color:" + color + ";" + f + "}");
        ss.addRule("h1 {color:" + color + ";font-weight:bold;" + f.replace("12pt", "14pt") + "}");
        ss.addRule("h2 {color:" + color + ";font-weight:bold;" + f + "}");
        UIManager.getDefaults().put("EditorPane.foreground",
            new ColorUIResource(color.equals("#ffffff") ? Color.WHITE : Color.BLACK));
        UIManager.getDefaults().put("EditorPane.inactiveForeground",
            new ColorUIResource(color.equals("#ffffff") ? Color.WHITE : Color.BLACK));
    }
}