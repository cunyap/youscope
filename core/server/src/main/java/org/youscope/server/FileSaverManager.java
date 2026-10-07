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
package org.youscope.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Central class to save files produced during a measurement to the hard disk.
 * @author Moritz Lang
 * 
 */
class FileSaverManager
{
	private static final int	KEEP_THREADS_ALIVE_SECONDS			= 60;
	public static final int		SAVER_POOL_SIZE						= 10;
	private static final int	SAVER_QUEUE_MAX_CAPACITY			= 10 * SAVER_POOL_SIZE;

	private static final int	WAIT_FOR_EXECUTION_PING_TIMEOUT_MS	= 200;

	private static class SavePoolExecutor extends ThreadPoolExecutor
	{
		SavePoolExecutor()
		{
			super(0, SAVER_POOL_SIZE, KEEP_THREADS_ALIVE_SECONDS, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(SAVER_QUEUE_MAX_CAPACITY), new ThreadPoolExecutor.CallerRunsPolicy());
		}
	}

	private static final SavePoolExecutor	saverPool	= new SavePoolExecutor();

	/**
	 * Queues the given runnable for execution by a worker thread.
	 * If already too many other runnables are queued, the runnable is executed by the calling thread,
	 * thus implementing a mechanism to delay the appearance of new tasks if they cannot be executed fast enough.
	 * @param runnable Runnable to be queued for execution by one of the worker threads, or which should be executed by the calling thread if queue is full.
	 */
	public static void execute(Runnable runnable)
	{
		saverPool.execute(runnable);
	}

	/**
	 * Waits until all tasks in the queue are executed.
	 * @throws InterruptedException thrown if thread was interrupted.
	 */
	public static void waitForExecutions() throws InterruptedException
	{
		while(saverPool.getActiveCount() > 0)
		{
			Thread.sleep(WAIT_FOR_EXECUTION_PING_TIMEOUT_MS);
		}
	}
}
