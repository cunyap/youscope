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
package org.youscope.server;

import org.youscope.common.job.Job;

/**
 * An element in the job execution queue.
 * @author Moritz Lang
 * 
 */
class JobExecutionQueueElement
{
	public final long			evaluationNumber;
	public final Job	job;

	public JobExecutionQueueElement(Job job, long evaluationNumber)
	{
		this.job = job;
		this.evaluationNumber = evaluationNumber;
	}
}
