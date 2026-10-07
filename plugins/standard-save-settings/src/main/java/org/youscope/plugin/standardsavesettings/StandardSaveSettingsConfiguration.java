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
package org.youscope.plugin.standardsavesettings;

import org.youscope.common.saving.MeasurementSaver;
import org.youscope.common.saving.SaveSettingsConfiguration;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Configuration of a standard folder structure to save measurements.
 * @author mlang
 *
 */
public class StandardSaveSettingsConfiguration  extends SaveSettingsConfiguration
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 113704186393312351L;

	/**
	 * Returns the type of the standard folder structure.
	 * @return Type of standard folder structure.
	 */
	public FolderStructureType getFolderStructureType() {
		return folderStructureType;
	}

	/**
	 * Sets the type of the standard folder structure.
	 * @param folderStructureType Type of the standard folder structure.
	 */
	public void setFolderStructureType(FolderStructureType folderStructureType) {
		this.folderStructureType = folderStructureType;
	}
	
	/**
	 * Type identifier of the save settings.
	 */
	public static final String TYPE_IDENTIFIER = "YouScope.StandardSaveSettings";
	
	/**
	 * The folder where the measurement (information and images made during the measurement) should be stored.
	 */
	@XStreamAlias("base-folder")
	private String					baseFolder					= "";

	/**
	 * The file type in which the images should be stored
	 */
	@XStreamAlias("image-file-type")
	private String					imageFileType			= "tif";

	@XStreamAlias("folder-structure-type")
	private FolderStructureType	folderStructureType	= FolderStructureType.ALL_IN_ONE_FOLDER;

	/**
	 * Returns the folder where all measurements should be saved.
	 * @return the folder of the measurement.
	 */
	public String getBaseFolder()
	{
		return baseFolder;
	}

	/**
	 * Sets the folder where all measurements should be saved.
	 * Be aware that an additional folder is created inside this folder when the measurement starts,
	 * which indicates the specific time when the measurement was started. This is done since one
	 * measurement can be started multiple times. To obtain the full path to this subfolder, use the
	 * respective function in {@link MeasurementSaver}.
	 * @param folder the folder of the measurement.
	 */
	public void setBaseFolder(String folder)
	{
		this.baseFolder = folder;
	}

	/**
	 * @return the imageFileType
	 */
	public String getImageFileType()
	{
		return imageFileType;
	}

	/**
	 * @param imageFileType
	 *            the imageFileType to set
	 */
	public void setImageFileType(String imageFileType)
	{
		this.imageFileType = imageFileType;
	}

	@Override
	public String getTypeIdentifier() {
		return TYPE_IDENTIFIER;
	}
}
