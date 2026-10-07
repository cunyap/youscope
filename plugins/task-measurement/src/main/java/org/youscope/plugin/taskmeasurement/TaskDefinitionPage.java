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
package org.youscope.plugin.taskmeasurement;

import java.awt.BorderLayout;

import javax.swing.border.TitledBorder;

import org.youscope.addon.measurement.MeasurementAddonUIPage;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;
import org.youscope.serverinterfaces.YouScopeServer;
import org.youscope.uielements.TasksDefinitionPanel;

class TaskDefinitionPage extends MeasurementAddonUIPage<TaskMeasurementConfiguration>
{
	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= -8833466993051293407L;
	
	private TasksDefinitionPanel tasksDefinitionPanel;
	private final YouScopeClient client; 
	private final YouScopeServer server; 
	TaskDefinitionPage(YouScopeClient client, YouScopeServer server)
	{
		this.client = client;
		this.server = server;
	}
	
	@Override
	public void loadData(TaskMeasurementConfiguration configuration)
	{
		tasksDefinitionPanel.setTasks(configuration.getTasks());
	}

	@Override
	public boolean saveData(TaskMeasurementConfiguration configuration)
	{
		configuration.setTasks(tasksDefinitionPanel.getTasks());
		return true;
	}

	@Override
	public void setToDefault(TaskMeasurementConfiguration configuration)
	{
		// Do nothing.
	}

	@Override
	public String getPageName()
	{
		return "Task Definition";
	}

	@Override
	public void createUI(YouScopeFrame frame)
	{
		tasksDefinitionPanel = new TasksDefinitionPanel(client, server, frame);
		setLayout(new BorderLayout());
		add(tasksDefinitionPanel, BorderLayout.CENTER);

		setBorder(new TitledBorder("Definition of Measurement Tasks"));
	}
}
