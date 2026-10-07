/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 OR the GNU Lesser General Public
 * License, Version 2.1 (or at your option any later version).
 *
 * You may choose which license you wish to use when copying, modifying,
 * or distributing this software.
 *
 * Licenses are available at:
 * - Apache 2.0: http://apache.org
 * - LGPL 2.1:   https://gnu.org
 *
 * SPDX-License-Identifier: Apache-2.0 OR LGPL-2.1-or-later
 *
 * Contributors:
 *     Moritz Lang - initial API and implementation
 ******************************************************************************/
package org.youscope.client;

import java.awt.BorderLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;

import org.youscope.clientinterfaces.FramePositionStorageType;
import org.youscope.clientinterfaces.StandardProperty;
import org.youscope.uielements.DynamicPanel;

class CustomizationTabAppearance extends ManageTabElement
{

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1706695732109422871L;

    private JComboBox<String> dockingField = new JComboBox<String>(new String[] { "Dock measurement control to main window.",
    "Show measurement control in own window." });
    
    private JComboBox<FramePositionStorageType> frameField = new JComboBox<FramePositionStorageType>(FramePositionStorageType.values());
    CustomizationTabAppearance()
    {
        setLayout(new BorderLayout());
        setBorder(new TitledBorder("Appearance"));
        setOpaque(false);
        
        DynamicPanel content = new DynamicPanel();
        content.add(new JLabel("Location of measurement control:"));
        content.add(dockingField);
        content.add(new JLabel("Remembering of window locations:"));
        content.add(frameField);
        
        content.addFillEmpty();
        add(content, BorderLayout.CENTER);
    }
    @Override
    public void initializeContent()
    {
        dockingField.setSelectedIndex((Boolean) PropertyProviderImpl.getInstance().getProperty(StandardProperty.PROPERTY_DOCK_MEASUREMENT_CONTROL) ? 0 : 1);
        frameField.setSelectedItem(FramePositionStorageType.getType((String) PropertyProviderImpl.getInstance().getProperty(StandardProperty.PROPERTY_POSITION_STORAGE)));
    }

    @Override
    public boolean storeContent()
    {
        PropertyProviderImpl.getInstance().setProperty(StandardProperty.PROPERTY_DOCK_MEASUREMENT_CONTROL, dockingField.getSelectedIndex() == 0);
        FramePositionStorageType storageType = (FramePositionStorageType)frameField.getSelectedItem();
        FramePositionStorage.getInstance().setStorageType(storageType);
        PropertyProviderImpl.getInstance().setProperty(StandardProperty.PROPERTY_POSITION_STORAGE, storageType.getIdentifier());
        return false;
    }

}
