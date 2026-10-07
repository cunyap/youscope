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
package org.youscope.common.microscope;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Allows access to the current microscope channel settings.
 * 
 * @author Moritz Lang
 */
public interface ChannelManager extends Remote
{
	/**
	 * Returns a list of all available config groups (e.g. the channel group).
	 * 
	 * @return List of all config groups.
	 * @throws RemoteException
	 */
	String[] getChannelGroupIDs() throws RemoteException;

	/**
	 * Returns all channels in the given channel group.
	 * 
	 * @param channelGroupID The ID of channel group for which the IDs of the contained channels should be returned..
	 * 
	 * @return Channels.
	 * @throws SettingException
	 * @throws RemoteException
	 */
	Channel[] getChannels(String channelGroupID) throws SettingException, RemoteException;

	/**
	 * Returns all channels.
	 * 
	 * @param channelGroupID The ID of channel group for which the IDs of the contained channels should be returned..
	 * 
	 * @return Channels.
	 * @throws RemoteException
	 */
	Channel[] getChannels() throws RemoteException;

	/**
	 * Returns the channel with the given ID and channel group ID.
	 * Returns null if either the channel group or the channel id is null.
	 * @param channelGroupID The channel group where the channel is defined.
	 * @param channelID The ID of the channel.
	 * @return The channel.
	 * @throws SettingException Thrown if the channel is not defined.
	 * @throws RemoteException
	 */
	Channel getChannel(String channelGroupID, String channelID) throws SettingException, RemoteException;

	/**
	 * Adds the channel in the given config group.
	 * If the channel and/or the config group do not exist, they are created. If the channel does already exist, it is returned.
	 * @param channelGroupID The config group where the channel should be created.
	 * @param channelID The name of the channel.
	 * @return The newly added channel.
	 * @throws MicroscopeLockedException
	 * @throws SettingException
	 * @throws RemoteException
	 */
	Channel addChannel(String channelGroupID, String channelID) throws MicroscopeLockedException, SettingException, RemoteException;

	/**
	 * Removes the channel in the given channel group. If either the channel or the channel group do not exist, a setting exception is thrown.
	 * @param channelGroupID The group in which the channel is defined.
	 * @param channelID The ID of the channel.
	 * @throws MicroscopeLockedException
	 * @throws SettingException
	 * @throws RemoteException
	 */
	void removeChannel(String channelGroupID, String channelID) throws MicroscopeLockedException, SettingException, RemoteException;
}
