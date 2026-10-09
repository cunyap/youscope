/*******************************************************************************
 * Copyright © 2026 ETH Zürich
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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.youscope.clientinterfaces.StandardProperty;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.microscope.MicroscopeException;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.ImagePanel;
import org.youscope.uielements.LiveStreamPanel;

/**
 * @author Moritz Lang
 *
 */
class UserControlMeasurementFrame extends JPanel
{
	/**
	 * Serial Version UID.
	 */
	private static final long			serialVersionUID		= -1499077134453605763L;

	private final ImagePanel imagePanel;
	private final LiveStreamPanel.ChannelControl channelControl;

	private final UserControlMeasurementCallbackImpl callback; 
	
	private final YouScopeFrame frame;
	
	/**
	 * Constructor.
	 * @param client Interface to the client.
	 * @param server Interface to the server.
	 * @param frame 
	 * @param callback the callback to contol.
	 * @throws RemoteException
	 * @throws InterruptedException
	 * @throws MicroscopeException
	 */
	public UserControlMeasurementFrame(final YouScopeClient client, final YouScopeServer server, final YouScopeFrame frame, final UserControlMeasurementCallbackImpl callback) throws RemoteException, InterruptedException, MicroscopeException
	{
		this.callback = callback;
		this.frame = frame;
		imagePanel = new ImagePanel(client);
		imagePanel.setUserChoosesAutoAdjustContrast(true);
		channelControl = new LiveStreamPanel.ChannelControl(client, server);
		
		if((boolean) client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_USE_DEFAULT_SETTINGS))
		{
			channelControl.setExposure((double)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_EXPOSURE));
			channelControl.setImagingPeriod((int)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_PERIOD));
			channelControl.setCamera((String) client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_CAMERA));
			channelControl.setChannel((String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_CHANNEL_GROUP), 
					(String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_DEFAULT_CHANNEL));
		}
		else
		{
			channelControl.setExposure((double)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_EXPOSURE));
			channelControl.setImagingPeriod((int)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_PERIOD));
			channelControl.setCamera((String) client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CAMERA));
			channelControl.setChannel((String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CHANNEL_GROUP), 
					(String)client.getPropertyProvider().getProperty(StandardProperty.PROPERTY_STREAM_LAST_CHANNEL));
		}
	
		
		channelControl.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				callback.sendChannelSettingsChanged();
			}
		});
		imagePanel.insertControl("Channel", channelControl, 0);

		
		JButton snapImageButton = new JButton("Snap Image");
		snapImageButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				frame.startLoading();
				UserControlMeasurementFrame.this.callback.sendSnapImage();
			}
		});
		setLayout(new BorderLayout());		
		add(imagePanel, BorderLayout.CENTER);
		add(snapImageButton, BorderLayout.SOUTH);
	}

	public void newImage(ImageEvent<?> event)
	{
		if(imagePanel != null)
			imagePanel.setImage(event);
	}
	
	public void snappedImage()
	{
		frame.endLoading();
	}
	
	public String getCurrentChannel()
	{
		return channelControl.getChannel();
	}

	public String getCurrentChannelGroup() 
	{
		return channelControl.getChannelGroup();
	}

	public double getCurrentExposure() 
	{
		return channelControl.getExposure();
	}
}
