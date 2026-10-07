/*******************************************************************************
 * Copyright (c) 2019 - 2020 Elisa Garulli.
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
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;

import org.youscope.clientinterfaces.YouScopeClient;

/**
 * This class represents a panel containing a loading bar.
 * @author Elisa Garulli
 *
 */
public class LoadingBar extends BasePopUpPanel {
	JProgressBar progressBar;
	JPanel backgroundPanel = new JPanel();

	/**
	 * Constructor. Sets the characteristics of the loading bar panel.
	 * @param client
	 * @param title
	 */
	public LoadingBar(YouScopeClient client, String title) {
		super(client);
		backgroundPanel.setLayout(new OverlayLayout(backgroundPanel));
		setLook(title);
		setLoadingBar();

		super.add(backgroundPanel);
	}
	/**
	 * Sets the outlook of the loading bar panel.
	 * @param title
	 */
	public void setLook(String title) {
		setSize(100,50);
		setTitle(title);
		JLabel label = new JLabel(new ImageIcon("Icons/progressBarIcon3.png"));
		backgroundPanel.add(label, LEFT_ALIGNMENT);
	}
	/**
	 * Creates a new loading bar.
	 */
	private void setLoadingBar() {
		progressBar = new JProgressBar(0,100);
		backgroundPanel.add(progressBar);
	}
	/**
	 * Sets the loading value on the loading bar.
	 * @param n
	 */
	public void setValue(int n) {
		progressBar.setValue(n);
	}

	/**
	 * Sets the color of the loading bar.
	 * @param color
	 */
	public void setForegroundBar(Color color) {
		progressBar.setStringPainted(true);
		progressBar.setForeground(color);
		progressBar.setString("");

	}
}
