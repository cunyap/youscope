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
/**
 * 
 */
package org.youscope.common.table;

/**
 * Interface each measurement component configuration should implement if its corresponding component produces table data.
 * By implementing this interface one accepts the contract that each component created as an artifact of this configuration also implements {@link TableProducer}, returning the same information as returned here.
 * 
 * @author Moritz Lang
 * 
 */
public interface TableProducerConfiguration
{	
	/**
	 * Returns the definition of the table layout of the tables produced by this producer, e.g. the number and types of its columns.
	 * This function should return the same TableDefinition as is returned by a measurement component created by this configuration, 
	 * see {@link TableProducer#getProducedTableDefinition()}.
	 * 
	 * @return Information about of the produced tables.
	 */
	public TableDefinition getProducedTableDefinition();
}
