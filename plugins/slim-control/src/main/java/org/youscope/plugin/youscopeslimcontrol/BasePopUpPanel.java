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
 *     Elisa Garulli - initial API and implementation
 *     Andreas P. Cuny - migration to the current YouScope API
 ******************************************************************************/
package org.youscope.plugin.youscopeslimcontrol;

import java.awt.Dimension;
import javax.swing.JPanel;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
/**
 * This class represents a basic, empty, pop-up panel.
 * @author Elisa Garulli
 *
 */
public class BasePopUpPanel extends JPanel
{
	private static final long serialVersionUID = -7327196260576315134L;
	protected YouScopeFrame frame = null;
	private final YouScopeClient client;
	private String title;

	/**
	 * Constructor.
	 * @param client YouScope client.
	 */
	public BasePopUpPanel(YouScopeClient client) {
		setOpaque(true);
		this.client = client;
    }

	/**
	 * Shows this panel in a frame. The frame is not made visible and has a default title.
	 * @return The frame containing this panel.
	 */
	public YouScopeFrame toFrame()
	{
		if(frame != null)
			return frame;
		frame = client.createFrame();
		frame.setTitle(title);
		frame.setResizable(true);
		frame.setClosable(true);
		frame.setMaximizable(true);
		frame.setMargins(0,0,0,0);
        frame.setContentPane(this);
        frame.pack();
		return frame;
	}

	/**
	 * Sets the title of the frame. Has no effect when using this panel directly.
	 * @param title Title of the frame.
	 */
	public void setTitle(String title)
	{
		this.title = title;
		if(frame != null)
			frame.setTitle(title);
	}

	public void close() {
		if(frame != null) {
			frame.close();
		}
	}

	public void setSize(Dimension size) {
		if(frame != null) {
			frame.setSize(size);
		}
	}
}
