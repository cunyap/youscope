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
/**
 * 
 */
package org.youscope.plugin.faster;

import org.youscope.addon.celldetection.CellDetectionConfiguration;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.table.TableDefinition;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration for the FastER segmenter. FastER is an external binary run
 * headless with a pre-trained model; it emits a 16-bit label image that YouScope
 * consumes directly. Only the binary and model paths (and an optional work dir) are
 * configured here.
 */
@XStreamAlias("faster")
public class FastERConfiguration extends CellDetectionConfiguration
{
	private static final long serialVersionUID = 71000048100091001L;

	/** Type identifier of this segmenter. */
	public static final String TYPE_IDENTIFIER = "YouScope.FastER";

	@XStreamAlias("faster-binary")
	private String fasterBinary = "";

	@XStreamAlias("faster-model")
	private String fasterModel = "";

	@XStreamAlias("faster-svm")
	private String fasterSvm = "";

	@XStreamAlias("work-dir")
	private String workDir = "";

	@XStreamAlias("generate-label-image")
	private boolean generateLabelImage = true;

	@Override
	public String getTypeIdentifier()
	{
		return TYPE_IDENTIFIER;
	}

	/** @return absolute path to the FastER executable (e.g. fastER.exe). */
	public String getFasterBinary()
	{
		return fasterBinary;
	}

	public void setFasterBinary(String fasterBinary)
	{
		this.fasterBinary = fasterBinary == null ? "" : fasterBinary;
	}

	/** @return absolute path to the trained FastER model file (.fastER). */
	public String getFasterModel()
	{
		return fasterModel;
	}

	public void setFasterModel(String fasterModel)
	{
		this.fasterModel = fasterModel == null ? "" : fasterModel;
	}

	/**
	 * @return absolute path to the FastER SVM sidecar (the {@code .fastER_svm} file
	 *         written next to the {@code .fastER} model on headless export). FastER
	 *         needs it co-located with the model; if left blank the backend uses the
	 *         {@code <model>_svm} sibling automatically.
	 */
	public String getFasterSvm()
	{
		return fasterSvm;
	}

	public void setFasterSvm(String fasterSvm)
	{
		this.fasterSvm = fasterSvm == null ? "" : fasterSvm;
	}

	/** @return working directory for the transient input/output TIFFs (optional). */
	public String getWorkDir()
	{
		return workDir;
	}

	public void setWorkDir(String workDir)
	{
		this.workDir = workDir == null ? "" : workDir;
	}

	@Override
	public boolean isGenerateLabelImage()
	{
		return generateLabelImage;
	}

	public void setGenerateLabelImage(boolean generateLabelImage)
	{
		this.generateLabelImage = generateLabelImage;
	}

	@Override
	public TableDefinition getProducedTableDefinition()
	{
		return FastERTable.getTableDefinition();
	}

	@Override
	public void checkConfiguration() throws ConfigurationException
	{
		if(fasterBinary == null || fasterBinary.trim().isEmpty())
			throw new ConfigurationException("FastER: the FastER binary path is not set.");
		if(fasterModel == null || fasterModel.trim().isEmpty())
			throw new ConfigurationException("FastER: the trained model (.fastER) path is not set.");
	}
}
