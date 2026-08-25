/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.trackvisualizer;

import org.youscope.addon.component.ComponentAddonFactoryAdapter;

/**
 * Factory for the Track Visualizer. Registered via Java's service loader so it
 * auto-populates the cell-visualization dropdown.
 *
 * @author YouScope
 */
public class TrackVisualizerAddonFactory extends ComponentAddonFactoryAdapter {
	/** Constructor. */
	public TrackVisualizerAddonFactory() {
		super(TrackVisualizerConfiguration.CONFIGURATION_ID, TrackVisualizerConfiguration.class,
				TrackVisualizerAddon.class);
	}
}
