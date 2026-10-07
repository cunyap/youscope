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
package org.youscope.uielements;

import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;

/**
 * @author Moritz Lang
 *
 */
public class DoubleTextField extends JFormattedTextField
{
	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= -4462391850446518462L;
	private final NumberFormatter numberFormatter;
	/**
	 * Default constructor.
	 */
	public DoubleTextField()
	{
		super(new NumberFormatter(StandardFormats.getDoubleFormat()));
		this.numberFormatter = (NumberFormatter)getFormatter();
	}
	/**
	 * Constructor which sets the initial value.
	 * @param value Initial value.
	 */
	public DoubleTextField(double value)
	{
		this();
		this.setValue(value);
	}
	@Override
	public Double getValue()
	{
		Object value = super.getValue();
		if(value == null)
			return 0.0;
		return ((Number)value).doubleValue();
	}
	/**
	 * Sets the minimal allowed value.
	 * @param value
	 */
	public void setMinimalValue(double value)
	{
		numberFormatter.setMinimum(value);
		setFormatter(numberFormatter);
	}
	/**
	 * Sets the maximal allowed value.
	 * @param value
	 */
	public void setMaximalValue(double value)
	{
		numberFormatter.setMaximum(value);
		setFormatter(numberFormatter);
	}
}
