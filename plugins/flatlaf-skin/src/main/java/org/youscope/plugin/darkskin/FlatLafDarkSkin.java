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
 * Modern flat dark skin using FlatLaf Darcula theme.
 * Replaces the JTattoo HiFi dark skin. HiDPI-aware, crisp rendering.
 */
class FlatLafDarkSkin implements Skin
{
    public static final String TYPE_IDENTIFIER = "YouScope.Skins.FlatDark";

    @Override
    public AddonMetadata getMetadata() { return createMetadata(); }

    static AddonMetadata createMetadata()
    {
        return new AddonMetadataAdapter(TYPE_IDENTIFIER,
            "Modern Dark",
            new String[]{"Skins"},
            "Clean flat dark theme (FlatLaf Darcula). Good for general use in darker environments.",
            "icons/system-monitor.png");
    }

    @Override
    public void applySkin() throws AddonException
    {
        Thread.currentThread().setContextClassLoader(FlatLafDarkSkin.class.getClassLoader());
        UIManager.getDefaults().clear();

        FlatLafLightSkin.applyQuickLogger(
            new Color(43, 43, 43),
            new Color(0.55f, 0.55f, 0.55f),
            new Color(0.9f, 0.9f, 0.9f));
        FlatLafLightSkin.applyDesktopPane(
            new Color(30, 30, 30),
            "org/youscope/plugin/flatlafskin/images/background-logo.png");
        FlatLafLightSkin.applyHtmlStyles("#ffffff");

        try
        {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarculaLaf");
        }
        catch (Exception e)
        {
            try { UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf"); }
            catch (Exception e2) { throw new AddonException("Could not set Look and Feel.", e2); }
        }
    }
}