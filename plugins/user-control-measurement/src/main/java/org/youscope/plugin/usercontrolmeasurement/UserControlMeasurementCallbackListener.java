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
package org.youscope.plugin.usercontrolmeasurement;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.EventListener;

/**
 * A listener which informs the measurement if in the visual callback the user either changed the channel settings or pressed "snap image".
 * @author Moritz Lang
 */
public interface UserControlMeasurementCallbackListener extends Remote, EventListener
{
	/**
	 * Invoked by the callback if the channel to image in was changed.
	 * @throws RemoteException
	 */
	void channelSettingsChanged() throws RemoteException;
	
	/**
	 * Invoked by the callback if the measurement should save the currently displayed image.
	 * @throws RemoteException
	 */
	void snapImage() throws RemoteException;
	
	/**
	 * Called when the user closed the callback UI. The measurement should quit then.
	 * @throws RemoteException
	 */
	void callbackClosed() throws RemoteException;
}
