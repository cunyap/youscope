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
package org.youscope.clientinterfaces;

import java.util.Collection;

/**
 * Optional extension interface for {@link MetadataDefinitionManager}
 * implementations which organize
 * their metadata definitions into categories.
 *
 * <p>
 * Categories are derived from the folder structure below
 * {@code configuration/metadata}. A definition
 * stored in {@code configuration/metadata/biological/Species.xml} belongs to
 * the category
 * {@code biological}; a definition stored directly in
 * {@code configuration/metadata} belongs to the
 * category {@link #UNCATEGORIZED}. Nested folders are represented as slash
 * separated paths, e.g.
 * {@code sample-preparation/staining}.
 * </p>
 *
 * <p>
 * This interface is deliberately kept separate from
 * {@link MetadataDefinitionManager} so that existing
 * implementations of that interface continue to compile. UI code should test
 * for it using
 * {@code instanceof} and fall back to a flat presentation if it is not
 * implemented.
 * </p>
 *
 * @author Andreas P. Cuny
 */
public interface MetadataCategoryProvider {
	/**
	 * Category of all metadata definitions which are not stored in a sub-folder.
	 */
	public static final String UNCATEGORIZED = "";

	/**
	 * Returns all categories for which at least one metadata definition is known,
	 * sorted alphabetically.
	 * {@link #UNCATEGORIZED} is not part of the returned collection.
	 * 
	 * @return All known categories.
	 */
	public Collection<String> getMetadataCategories();

	/**
	 * Returns the category of the metadata definition with the given name, or
	 * {@link #UNCATEGORIZED}
	 * if the definition is not categorized or unknown.
	 * 
	 * @param definitionName Name of the metadata definition.
	 * @return Category of the definition. Never null.
	 */
	public String getMetadataCategory(String definitionName);

	/**
	 * Returns all metadata definitions belonging to the given category.
	 * 
	 * @param category Category to filter for. Pass {@link #UNCATEGORIZED} for
	 *                 uncategorized definitions.
	 * @return All definitions of the given category, sorted.
	 */
	public Collection<MetadataDefinition> getMetadataDefinitions(String category);
}
