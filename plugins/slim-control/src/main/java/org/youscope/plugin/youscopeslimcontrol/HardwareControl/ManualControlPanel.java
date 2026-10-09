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
package org.youscope.plugin.youscopeslimcontrol.HardwareControl;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;


/**
 * This class represents the panel for the Manual Control.
 * @author Elisa Garulli
 *
 */
public class ManualControlPanel extends JPanel {

	Font fontTitle = new Font(null, 1, 12);

	/**
	 * Constructor.
	 * Adds all the elements created by the classes Wheel-N-Panel to a JPanel.
	 * @param slimController an instance of SLIMController which allows to control the loaded device.
	 */
	public ManualControlPanel(SLIMController slimController) {
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(BorderFactory.createTitledBorder(null, "Optical Elements Manual Setup", 0, 0, fontTitle));
		add(new WheelOnePanel(slimController));
		add(new WheelTwoPanel(slimController));
		add(new WheelThreePanel(slimController));
	}

}
