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
/**
 * 
 */
package org.youscope.common.microscope;

/**
 * Exception thrown when a microscope configuration file contained errors.
 * @author Moritz Lang
 * 
 */
public class MicroscopeConfigurationException extends Exception
{
	/**
	 * Serial Version UID.
	 */
	private static final long	serialVersionUID	= 731706667778288639L;
	private final int			lineNumber;
	private final String		line;

	/**
	 * Constructor used when line number of error is known.
	 * @param e Error which occurred.
	 * @param line The line which could not get parsed.
	 * @param lineNumber Line number where error occurred.
	 */
	public MicroscopeConfigurationException(Exception e, String line, int lineNumber)
	{
		super("Configuration file could not be parsed.\nError on line " + Integer.toString(lineNumber + 1) + ":\n" + line, e);
		this.lineNumber = lineNumber;
		this.line = line;
	}

	/**
	 * Constructor used when line number of error is unknown.
	 * @param e Error which occurred.
	 */
	public MicroscopeConfigurationException(Exception e)
	{
		super("Configuration file contained errors.", e);
		this.lineNumber = -1;
		this.line = null;
	}

	/**
	 * Returns the line number in the configuration where the error occurred, or -1 if the line is unknown.
	 * @return Line number of error (starting at 0) or -1.
	 */
	public int getLineNumber()
	{
		return lineNumber;
	}

	/**
	 * Returns the line of the configuration which could not be parsed, or null if line is unknown.
	 * @return Line which could not be parsed.
	 */
	public String getLine()
	{
		return line;
	}

}
