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
package org.youscope.plugin.nemesys;

import java.util.Arrays;

/**
 * A simplified "script engine" to allow easy Nemesys timing table processing.
 * @author Moritz Lang
 *
 */
class TimingScriptEngine
{
	private final SyringeTableRow[] timings;
	TimingScriptEngine(String script) throws NemesysException
	{
		timings = TimingProcessor.getTimings(script);
		Arrays.sort(timings);
	}
	SyringeTableRow getActiveSettings(long time)
	{
		SyringeTableRow activeTiming = null;
		for(int i=0; i<timings.length; i++)
		{
			if(timings[i].time <= time)
				activeTiming = timings[i];
			else
				break;
		}
		return activeTiming;
	}
}
