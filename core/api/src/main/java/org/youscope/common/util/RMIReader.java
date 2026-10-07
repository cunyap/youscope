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
package org.youscope.common.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author Moritz Lang
 * 
 */
public class RMIReader extends Reader implements Serializable
{
	/**
	 * Serial Version UID.
	 */
	private static final long			serialVersionUID	= 8904838002669657159L;

	private RMIReaderRemoteInterface	remoteReader;

	/**
	 * Constructor.
	 * Constructs a mapper for a reader which supports RMI.
	 * Simply give him the original reader as an input, e.g. a FileReader to read from a file.
	 * @param reader The reader which should support RMI.
	 * @throws RemoteException
	 */
	public RMIReader(Reader reader) throws RemoteException
	{
		remoteReader = new RMIReaderRemoteImpl(reader);
	}

	@Override
	public void close() throws IOException
	{
		remoteReader.close();
	}

	@Override
	public int read(char[] arg0, int arg1, int arg2) throws IOException
	{
		return remoteReader.read(arg0, arg1, arg2);
	}

	private interface RMIReaderRemoteInterface extends Remote
	{
		public void close() throws IOException, RemoteException;

		public int read(char[] arg0, int arg1, int arg2) throws IOException, RemoteException;
	}

	private class RMIReaderRemoteImpl extends UnicastRemoteObject implements RMIReaderRemoteInterface
	{
		/**
		 * Serial Version UID.
		 */
		private static final long	serialVersionUID	= 788342367755947230L;

		private final Reader		reader;

		/**
		 * @throws RemoteException
		 */
		protected RMIReaderRemoteImpl(Reader reader) throws RemoteException
		{
			super();
			this.reader = reader;
		}

		@Override
		public void close() throws IOException, RemoteException
		{
			reader.close();
		}

		@Override
		public int read(char[] arg0, int arg1, int arg2) throws IOException, RemoteException
		{
			return reader.read(arg0, arg1, arg2);
		}

	}
}
