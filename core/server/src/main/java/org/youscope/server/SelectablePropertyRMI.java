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
/**
 * 
 */
package org.youscope.server;

import java.rmi.RemoteException;

import org.youscope.addon.microscopeaccess.SelectablePropertyInternal;
import org.youscope.common.microscope.SelectableProperty;

/**
 * @author Moritz Lang
 */
class SelectablePropertyRMI extends PropertyRMI implements SelectableProperty
{

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 7230759464316017918L;

    private final SelectablePropertyInternal selectableProperty;

    /**
     * Constructor.
     * 
     * @throws RemoteException
     */
    SelectablePropertyRMI(SelectablePropertyInternal selectableProperty, int accessID)
            throws RemoteException
    {
        super(selectableProperty, accessID);
        this.selectableProperty = selectableProperty;
    }

    @Override
	public String[] getAllowedPropertyValues() throws RemoteException
    {
        return selectableProperty.getAllowedPropertyValues();
    }

}
