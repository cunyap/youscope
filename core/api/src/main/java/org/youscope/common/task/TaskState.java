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
package org.youscope.common.task;

/**
 * The state of a task.
 * @author Moritz Lang
 *
 */
public enum TaskState 
{
	/**
	 * The task is created and ready to be initialized.
	 */
	READY,
	/**
	 * The task is currently initializing.
	 */
	INITIALIZING,
	/**
	 * The task is initialized and can be started.
	 */
	INITIALIZED,
	/**
	 * The task is currently running.
	 */
	RUNNING,
	/**
	 * The task is finished/stopped and can be uninitialized.
	 */
	STOPPED,
	/**
	 * The task is currently uninitializing.
	 */
	UNINITIALIZING,
	/**
	 * The task is uninitialized and can be initialized again. Functionally the same as {@link #READY}.
	 */
	UNINITIALIZED,
	/**
	 * The task is paused, and can be resumed again.
	 */
	PAUSED,
	/**
	 * During the execution of the task an error occurred. The task stopped and cannot be restarted, but must be constructed completely again.
	 */
	ERROR;
	
	
	@Override
	public String toString() 
	{
	   return super.name().toLowerCase();
	}
	
	/**
	 * Returns true if the task is in an error state.
	 * Currently, this is true if the state is {@link #ERROR}.
	 * @return True if task is in error state.
	 */
	public boolean isError()
	{
		return this == TaskState.ERROR;
	}
	/**
	 * Returns true if the task is currently executed.
	 * Currently, this is true if the state is either {@link #RUNNING}.
	 * @return True if measurement is currently executed.
	 */
	public boolean isRunning()
	{
		return this == TaskState.RUNNING;
	}
	
	/**
	 * Returns true if the current state allows for safe editing of the task.
	 * Currently, this is true if this state is either {@link #READY} or {@link #UNINITIALIZED}.
	 * @return True if safe editing is possible.
	 */
	public boolean isEditable()
	{
		return this == TaskState.READY || this == TaskState.UNINITIALIZED;
	}
}
