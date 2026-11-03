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
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/

package org.youscope.plugin.fairexport;

import java.lang.reflect.Constructor;

import org.youscope.plugin.fairexport.ome.OmeMetadataService;

/**
 * Loads {@link OmeMetadataService} through the isolated
 * {@link BioFormatsClassLoader}.
 * <p>
 * The implementation class is loaded by name, not referenced statically, so
 * that the plugin classes
 * the server scans at startup contain no link to any OME type. The returned
 * proxy implements the
 * plugin-side {@link OmeMetadataService} interface, which is loaded by the
 * plugin's own loader, so the
 * value crosses the boundary as an ordinary interface reference.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
final class OmeServiceLoader {
	private OmeServiceLoader() {
		// utility
	}

	/**
	 * Instantiates the OME service inside the isolated classloader.
	 * 
	 * @return A ready service.
	 * @throws Exception If the Bio-Formats jars are missing or the implementation
	 *                   cannot be constructed.
	 */
	static OmeMetadataService load() throws Exception {
		// The interface must be visible to the child loader as the SAME class the
		// plugin uses, so the
		// child loader delegates org.youscope.* to the parent (see
		// BioFormatsClassLoader). Loading the
		// implementation through the child loader then resolves its loci.*/ome.*
		// references from the
		// isolated jars while the interface it implements is the parent's.
		BioFormatsClassLoader loader = BioFormatsClassLoader.get();
		Class<?> implementation = Class.forName(OmeMetadataService.IMPLEMENTATION_CLASS, true, loader);
		Constructor<?> constructor = implementation.getDeclaredConstructor();
		constructor.setAccessible(true);
		Object instance = constructor.newInstance();
		if (!(instance instanceof OmeMetadataService)) {
			throw new IllegalStateException("The OME metadata service implementation was loaded, but through the wrong "
					+ "classloader, so it does not share the OmeMetadataService interface. This indicates the isolated "
					+ "classloader is delegating org.youscope.* incorrectly.");
		}
		return (OmeMetadataService) instance;
	}
}
