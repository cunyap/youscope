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
package org.youscope.plugin.fairexport.ome;

import java.util.List;

import org.youscope.plugin.fairexport.model.ExperimentMetadata;

/**
 * The single point of contact with Bio-Formats.
 * <p>
 * Every reference to a {@code loci.*} or {@code ome.*} type lives behind this
 * interface. Nothing that
 * the server touches at startup such as the factories, the engine, the dialog -
 * mentions an OME class, which
 * is what keeps the sealed JAI codec packages out of the boot classpath and
 * avoids the sealing
 * violation. The implementation is loaded reflectively through
 * {@code BioFormatsClassLoader} only when
 * an export runs.
 * </p>
 * <p>
 * The method signatures deliberately use only JDK types and the plugin's own
 * model, so this interface
 * loads with the plugin while its implementation loads in isolation, and values
 * still cross the
 * boundary.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public interface OmeMetadataService {
	/**
	 * Fully qualified name of the implementation, loaded reflectively so that no
	 * static reference to it
	 * exists in the plugin classes the server loads at startup.
	 */
	String IMPLEMENTATION_CLASS = "org.youscope.plugin.fairexport.ome.OmeMetadataServiceImpl";

	/**
	 * Builds the OME metadata for a measurement and returns it serialised as
	 * OME-XML.
	 * <p>
	 * Serialising here, rather than returning an {@code IMetadata}, keeps the OME
	 * types from crossing
	 * the classloader boundary at all: the caller receives a plain string.
	 * </p>
	 * 
	 * @param metadata Parsed measurement.
	 * @param warnings List to which any metadata gaps found while building are
	 *                 added.
	 * @return The OME-XML document.
	 * @throws Exception If the metadata cannot be built or serialised.
	 */
	String buildOmeXml(ExperimentMetadata metadata, List<String> warnings) throws Exception;

	/**
	 * Writes one OME-TIFF per image series into the output folder, with the OME
	 * metadata embedded.
	 * <p>
	 * Reads the source images YouScope saved and rewrites them as OME-TIFF, so the
	 * pixels and the
	 * standard metadata travel together in a single community-standard file. All
	 * Bio-Formats reader and
	 * writer contact happens inside the isolated classloader, so the sealed JAI
	 * codec packages stay
	 * contained.
	 * </p>
	 * 
	 * @param metadata          Parsed measurement.
	 * @param measurementFolder Folder containing the source images (paths in the
	 *                          model are relative to it).
	 * @param outputFolder      Folder the OME-TIFF files are written into.
	 * @param warnings          List to which any problems are added; a per-image
	 *                          failure is recorded here rather
	 *                          than aborting the whole export.
	 * @return The names of the files written.
	 * @throws Exception Only for a failure that prevents writing anything at all.
	 */
	java.util.List<String> writeOmeTiff(ExperimentMetadata metadata, java.io.File measurementFolder,
			java.io.File outputFolder, boolean perChannel, List<String> warnings) throws Exception;

	/**
	 * Writes one OME-Zarr (NGFF) store per image series into the output folder.
	 * <p>
	 * OME-Zarr is the chunked, cloud-friendly counterpart to OME-TIFF. The same
	 * pixels and OME metadata,
	 * written as a Zarr hierarchy and uses the same isolated Bio-Formats stack as
	 * {@code writeOmeTiff}.
	 * </p>
	 * 
	 * @param metadata          Parsed measurement.
	 * @param measurementFolder Folder containing the source images.
	 * @param outputFolder      Folder the OME-Zarr stores are written into.
	 * @param warnings          List to which per-image problems are added.
	 * @return The names of the stores written.
	 * @throws Exception Only for a failure that prevents writing anything at all.
	 */
	java.util.List<String> writeOmeZarr(ExperimentMetadata metadata, java.io.File measurementFolder,
			java.io.File outputFolder, int chunkSize, boolean sharding, List<String> warnings) throws Exception;
}
