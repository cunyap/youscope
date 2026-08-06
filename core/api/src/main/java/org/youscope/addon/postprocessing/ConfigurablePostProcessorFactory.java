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
package org.youscope.addon.postprocessing;

import org.youscope.common.measurement.MeasurementConfiguration;

/**
 * Optional extension for an {@link AutomaticPostProcessorFactory} whose
 * automatic execution has
 * configurable options that should be chosen together with the measurement
 * protocol.
 * <p>
 * A plain automatic post-processor is just a checkbox in the measurement
 * wizard: on or off. Some
 * post-processors like the FAIR export have options (which formats, chunk
 * sizes, ...) that ought to be defined up front with the measurement and stored
 * in its configuration,
 * rather than being set in a later manual re-run. Such a factory implements
 * this interface to
 * contribute its own small configuration panel, which the wizard shows in place
 * of the bare checkbox.
 * </p>
 * <p>
 * The panel is responsible for encoding its state into the measurement
 * configuration (typically by
 * writing one or more automatic-post-processor identifiers) in
 * {@link ConfigurationPanel#saveData} and
 * restoring it in {@link ConfigurationPanel#loadData}. The core stays ignorant
 * of what the options
 * mean: it only provides the slot in the wizard and calls load/save.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public interface ConfigurablePostProcessorFactory {
	/**
	 * Returns true if the given automatic post-processor type identifier is
	 * configured through a
	 * contributed panel rather than a bare checkbox.
	 * 
	 * @param typeIdentifier Type identifier.
	 * @return True if this factory contributes a configuration panel for it.
	 */
	boolean isConfigurable(String typeIdentifier);

	/**
	 * Creates the configuration panel for the wizard. Called once when the wizard
	 * page is built.
	 * 
	 * @param typeIdentifier Type identifier the panel configures.
	 * @return A new configuration panel.
	 */
	ConfigurationPanel createConfigurationPanel(String typeIdentifier);

	/**
	 * A small panel that edits a post-processor's options and persists them into
	 * the measurement
	 * configuration. Kept minimal and free of any core dependency beyond the
	 * configuration object, so
	 * plugins can implement it without the core knowing the option semantics.
	 * 
	 * @author Andreas P. Cuny
	 */
	public interface ConfigurationPanel {
		/**
		 * @return The Swing component to embed in the wizard.
		 */
		java.awt.Component getComponent();

		/**
		 * Restores the panel state from a measurement configuration.
		 * 
		 * @param configuration Configuration to read.
		 */
		void loadData(MeasurementConfiguration configuration);

		/**
		 * Writes the panel state into a measurement configuration.
		 * 
		 * @param configuration Configuration to write.
		 */
		void saveData(MeasurementConfiguration configuration);
	}
}
