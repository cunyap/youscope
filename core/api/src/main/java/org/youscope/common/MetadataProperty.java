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
package org.youscope.common;

import java.io.Serializable;

import org.youscope.common.measurement.Measurement;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;


/**
 * A metadata property is a name-value pair, representing metadata of e.g. a {@link Measurement}. 
 * This class is immutable.
 * @author Moritz Lang
 */
@XStreamAlias("metadata-property")
public class MetadataProperty implements Serializable, Comparable<MetadataProperty>
{
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 7590465549631720974L;
	@XStreamAlias("name")
	@XStreamAsAttribute
	private final String name;
	@XStreamAlias("value")
	@XStreamAsAttribute
	private final String value;
	/**
	 * Constructor.
	 * @param name Name of metadata property
	 * @param value Value of metadata property
	 * @throws IllegalArgumentException Thrown if name or value is null, or if name is empty.
	 */
	public MetadataProperty(String name, String value) throws IllegalArgumentException
	{
		if(name == null || name.isEmpty() || value == null)
			throw new IllegalArgumentException();
		this.name = name;
		this.value = value;
	}
	/**
	 * Returns the name of this metadata property.
	 * @return Non-null, non-empty name of property.
	 */
	public String getName()
	{
		return name;
	}
	/**
	 * Returns the value of this metadata property
	 * @return Non-null value of property.
	 */
	public String getValue()
	{
		return value;
	}
	
	@Override
	public String toString()
	{
		return name+"="+value;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetadataProperty other = (MetadataProperty) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	@Override
	public int compareTo(MetadataProperty other) 
	{
		int difference = name.compareTo(other.name);
		return difference != 0 ? difference : value.compareTo(other.value);
	}
}
