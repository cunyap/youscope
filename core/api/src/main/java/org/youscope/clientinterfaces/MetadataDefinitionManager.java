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
package org.youscope.clientinterfaces;

import java.util.Collection;

/**
 * Interface through which an addon can query the settings with respect to measurement metadata, e.g. which metadata properties can have which (default) values.
 * 
 * @author Moritz Lang
 */
public interface MetadataDefinitionManager extends Iterable<MetadataDefinition>
{
	/**
	 * Returns true if user is allowed to define custom properties not defined in {@link #getMetadataDefinitions()}.
	 * @return True if custom properties are allowed.
	 */
	boolean isAllowCustomMetadata();
	/**
	 * Returns all defined properties.
	 * @return defined properties.
	 */
	Collection<MetadataDefinition> getMetadataDefinitions();
	
	/**
	 * Returns all defined properties which are mandatory to be included in a measurement.
	 * Corresponds to all elements returned by {@link #getMetadataDefinitions()} which return {@link MetadataDefinition.Type#MANDATORY} for {@link MetadataDefinition#getType()}.
	 * @return List of mandatory properties. 
	 */
	Collection<MetadataDefinition> getMandatoryMetadataDefinitions();
	
	/**
	 * Returns all defined properties which are mandatory to be included in a measurement, or should be by default included.
	 * Corresponds to all elements returned by {@link #getMetadataDefinitions()} which return {@link MetadataDefinition.Type#MANDATORY} or {@link MetadataDefinition.Type#DEFAULT} for {@link MetadataDefinition#getType()}.
	 * @return List of mandatory properties. 
	 */
	Collection<MetadataDefinition> getDefaultMetadataDefinitions();
	
	/**
	 * Returns the property with the given name, or null if no property with the name is defined.
	 * @param name Name of the property.
	 * @return Property with the name, or null.
	 */
	MetadataDefinition getMetadataDefinition(String name);
	
	/**
	 * Returns the number of defined properties.
	 * @return Number of defined properties.
	 */
	int getNumMetadataDefinitions();
	
	/**
	 * Adds the property to the list of defined properties. If a property with the same name is already present, it overwrites this property.
	 * @param property property to add.
	 * @throws YouScopeClientException 
	 */
	void setMetadataDefinition(MetadataDefinition property) throws YouScopeClientException;
	
	/**
	 * Deletes the property with the given name. Does nothing if no property with the given name is defined.
	 * @param name Name of the property to be deleted.
	 * @return true if property was deleted, false if property with the given name was not defined.
	 * @throws YouScopeClientException 
	 */
	boolean deleteMetadataDefinition(String name) throws YouScopeClientException;
}
