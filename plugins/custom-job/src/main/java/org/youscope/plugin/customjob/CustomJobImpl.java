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
package org.youscope.plugin.customjob;

import java.rmi.RemoteException;

import org.youscope.common.ExecutionInformation;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.job.CompositeJobAdapter;
import org.youscope.common.job.Job;
import org.youscope.common.job.JobException;
import org.youscope.common.microscope.Microscope;

/**
 * @author Moritz Lang
 */
class CustomJobImpl extends CompositeJobAdapter implements CustomJob
{

	/**
	 * Serial Version UID.
	 */
	private static final long			serialVersionUID	= -1632043889601708085L;

	public CustomJobImpl(PositionInformation positionInformation) throws RemoteException
	{
		super(positionInformation);
	}

	@Override
	protected String getDefaultName()
	{
		return "Custom job";
	}

	

	@Override
	public void runJob(ExecutionInformation executionInformation, Microscope microscope, MeasurementContext measurementContext) throws JobException, InterruptedException, RemoteException
	{
		Job[] jobs = getJobs();
		for(Job job : jobs)
		{
			job.executeJob(executionInformation, microscope, measurementContext);
			if(Thread.interrupted())
				throw new InterruptedException();
		}
	}
}
