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
package org.youscope.plugin.measurementviewer;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

/**
 * @author Moritz Lang
 *
 */
class LoadMeasurementGlassPane extends JPanel
{
	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= 2626130411854364971L;
	private final JLabel		messageLabel;
	private JProgressBar		waitbar				= null;
	private final int			messageWidth;
	private final int			messageHeight;

	LoadMeasurementGlassPane()
	{
		super(null);
		setOpaque(true);
		messageLabel = new JLabel("<html><center><b>Loading measurement data...</b><br />This may take several seconds.</center></html>", SwingConstants.CENTER);
		messageWidth = messageLabel.getPreferredSize().width;
		messageHeight = messageLabel.getPreferredSize().height;
		messageLabel.setSize(messageWidth, messageHeight);
		add(messageLabel);
		waitbar = new JProgressBar();
		waitbar.setSize(messageWidth, 20);
		add(waitbar);
	}

	public void setLoading(boolean loading)
	{
		waitbar.setIndeterminate(loading);
	}

	@Override
	public void paintComponent(Graphics grp)
	{
		grp.setColor(new Color(1.0F, 1.0F, 1.0F, 0.7F));
		grp.fillRect(0, 0, getWidth(), getHeight());

		int width = getWidth();
		int height = getHeight();

		messageLabel.setLocation((width - messageWidth) / 2, height / 2 - messageHeight - 2);
		waitbar.setLocation((width - messageWidth) / 2, height / 2 + 2);
		
		super.paintComponent(grp);
	}
}
