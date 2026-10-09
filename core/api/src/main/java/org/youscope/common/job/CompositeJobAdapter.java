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
package org.youscope.common.job;

import java.rmi.RemoteException;
import java.util.ArrayList;

import org.youscope.common.ComponentRunningException;
import org.youscope.common.ExecutionInformation;
import org.youscope.common.MeasurementContext;
import org.youscope.common.PositionInformation;
import org.youscope.common.microscope.Microscope;

/**
 * /**
 * Convenient adapter class to implement a job which contains other jobs.
 * Similar to {@link JobAdapter}, except that additional functionality to add and remove sub-jobs is already implemented, as well as
 * the initialization and uninitialization of these sub-jobs.
 * The sub-jobs can be accessed (e.g. the function {@link Job#executeJob(ExecutionInformation, Microscope, MeasurementContext)}) via {@link #getJobs()}.
 * @author Moritz Lang
 */
public abstract class CompositeJobAdapter extends JobAdapter implements CompositeJob
{

	/**
	 * Serial Version UID.
	 */
	private static final long				serialVersionUID	= -2780029256797068512L;

	private final ArrayList<Job>	jobs				= new ArrayList<Job>();

	/**
	 * Constructor.
	 * @param positionInformation The logical position where this job is executed. Must not be null.
	 * @throws RemoteException
	 */
	public CompositeJobAdapter(PositionInformation positionInformation) throws RemoteException
	{
		super(positionInformation);
	}

	@Override
	public void initializeJob(Microscope microscope, MeasurementContext measurementContext) throws JobException, InterruptedException, RemoteException
	{
		super.initializeJob(microscope, measurementContext);

		synchronized(jobs)
		{
			for(Job job : jobs)
			{
				job.initializeJob(microscope, measurementContext);
			}
		}
	}

	@Override
	public void uninitializeJob(Microscope microscope, MeasurementContext measurementContext) throws JobException, InterruptedException, RemoteException
	{
		super.uninitializeJob(microscope, measurementContext);

		synchronized(jobs)
		{
			for(Job job : jobs)
			{
				job.uninitializeJob(microscope, measurementContext);
			}
		}
	}

	@Override
	public synchronized void addJob(Job job) throws ComponentRunningException
	{
		assertRunning();
		synchronized(jobs)
		{
			jobs.add(job);
		}
	}

	@Override
	public synchronized void removeJob(int jobIndex) throws ComponentRunningException, IndexOutOfBoundsException
	{
		assertRunning();
		synchronized(jobs)
		{
			jobs.remove(jobIndex);
		}
	}

	@Override
	public synchronized void clearJobs() throws ComponentRunningException
	{
		assertRunning();
		synchronized(jobs)
		{
			jobs.clear();
		}
	}

	@Override
	public Job[] getJobs()
	{
		synchronized(jobs)
		{
			return jobs.toArray(new Job[jobs.size()]);
		}
	}

	@Override
	public int getNumJobs()
	{
		return jobs.size();
	}

	@Override
	public Job getJob(int jobIndex) throws IndexOutOfBoundsException 
	{
		if(jobIndex < 0 || jobIndex >= getNumJobs())
			throw new IndexOutOfBoundsException("Job container contains only " + Integer.toString(getNumJobs())+" job(s).");
		return jobs.get(jobIndex);
	}

	@Override
	public void insertJob(Job job, int jobIndex) throws ComponentRunningException, IndexOutOfBoundsException 
	{
		assertRunning();
		jobs.add(jobIndex, job);
		
	}
}
