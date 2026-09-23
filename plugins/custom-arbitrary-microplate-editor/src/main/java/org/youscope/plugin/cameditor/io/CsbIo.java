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
package org.youscope.plugin.cameditor.io;

import java.io.File;
import java.io.IOException;

import org.youscope.addon.ConfigurationManagement;
import org.youscope.plugin.custommicroplates.CustomArbitraryMicroplateDefinition;
import org.youscope.plugin.custommicroplates.CustomMicroplateManager;

/**
 * Reads and writes arbitrary microplate {@code .csb} definitions for the editor.
 * <p>
 * Saving to the microplate library goes through the (now public) {@link CustomMicroplateManager} so
 * the plate immediately appears as a selectable microplate type. Loading an arbitrary user-picked file
 * reuses {@link CustomMicroplateManager#getCustomArbitraryMicroplate(File, String)}, which first tries
 * the normal serializer and then falls back to a naming-tolerant DOM reader, so legacy files always
 * load.
 *
 * @author Andreas P. Cuny
 */
public final class CsbIo
{
	private CsbIo() { }

	/**
	 * Saves the definition into the YouScope microplate library ({@code configuration/microplates}),
	 * where it becomes selectable as a microplate type.
	 * @param definition definition to save (its name determines the file name and type id).
	 * @throws IOException on failure.
	 */
	public static void saveToLibrary(CustomArbitraryMicroplateDefinition definition) throws IOException
	{
		try
		{
			CustomMicroplateManager.saveCustomMicroplate(definition);
		}
		catch(Exception e)
		{
			throw new IOException("Could not save microplate to library: " + e.getMessage(), e);
		}
	}

	/**
	 * Saves the definition to an explicit {@code .csb} file (e.g. for sharing / export).
	 * @param definition definition to save.
	 * @param file destination file.
	 * @throws IOException on failure.
	 */
	public static void saveToFile(CustomArbitraryMicroplateDefinition definition, File file) throws IOException
	{
		ConfigurationManagement.saveConfiguration(file.toString(), definition);
	}

	/**
	 * Loads an arbitrary microplate definition from any {@code .csb} file, tolerating legacy naming.
	 * @param file file to load.
	 * @return the loaded definition.
	 * @throws IOException on failure.
	 */
	public static CustomArbitraryMicroplateDefinition loadFromFile(File file) throws IOException
	{
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		if(dot > 0)
			name = name.substring(0, dot);
		try
		{
			return CustomMicroplateManager.getCustomArbitraryMicroplate(file, name);
		}
		catch(Exception e)
		{
			throw new IOException("Could not load \"" + file.getName() + "\": " + e.getMessage(), e);
		}
	}
}
