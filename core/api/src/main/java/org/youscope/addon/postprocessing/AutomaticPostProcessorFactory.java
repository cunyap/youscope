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

import org.youscope.addon.AddonException;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.common.saving.MeasurementFileLocations;
import org.youscope.serverinterfaces.YouScopeServer;

/**
 * Optional extension interface for a {@link PostProcessorAddonFactory} whose
 * post-processors can also
 * run without a user interface, directly after a measurement finished.
 * <p>
 * {@link PostProcessorAddonFactory} only creates a UI, which is correct for
 * post-processors that need
 * user input but leaves no way to run one unattended. Rather than adding a
 * method to that interface
 * which would break every existing implementation we use a factory that
 * declares headless capability by also
 * implementing this one. UI-only post-processors are unaffected and simply
 * never appear as automatic
 * options.
 * </p>
 * <p>
 * Implementations must observe two rules, because they run when nobody is
 * watching:
 * </p>
 * <ul>
 * <li>The measurement data is already saved and safe by the time this is
 * called. A failure here must
 * never be reported as a failure of the measurement, and must never delete or
 * modify acquired data.</li>
 * <li>The method is called on a background thread and may run for a long time,
 * but it must return.
 * Anything that could block indefinitely e.g. a dialog, a network wait without
 * timeout etc. belongs in the
 * UI variant instead.</li>
 * </ul>
 * 
 * @author Andreas P. Cuny
 */
public interface AutomaticPostProcessorFactory {
	/**
	 * Returns true if the post-processor with the given type identifier can run
	 * without a user
	 * interface. A factory may support several post-processors of which only some
	 * are automatable.
	 * 
	 * @param typeIdentifier Type identifier of the post-processor.
	 * @return True if the post-processor can be run automatically.
	 */
	boolean isSupportingAutomaticExecution(String typeIdentifier);

	/**
	 * Runs the post-processor without a user interface.
	 * <p>
	 * Called on a background thread after the measurement finished and all files
	 * were written. Errors
	 * should be reported through
	 * {@link YouScopeClient#sendError(String, Throwable)} rather than thrown,
	 * unless the post-processor could not be started at all.
	 * </p>
	 * 
	 * @param typeIdentifier           Type identifier of the post-processor to run.
	 * @param client                   YouScope client.
	 * @param server                   YouScope server.
	 * @param measurementFileLocations Locations of the files of the finished
	 *                                 measurement.
	 * @throws AddonException Thrown if the type identifier is not supported, or the
	 *                        post-processor
	 *                        could not be started.
	 */
	void runPostProcessor(String typeIdentifier, YouScopeClient client, YouScopeServer server,
			MeasurementFileLocations measurementFileLocations) throws AddonException;
}
