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
import java.io.Serializable;
import java.io.Writer;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author Moritz Lang
 * 
 */
public class RMIWriter extends Writer implements Serializable
{
	/**
	 * Serial Version UID.
	 */
	private static final long			serialVersionUID	= -3031284382382204370L;

	private RMIWriterRemoteInterface	remoteWriter;

	/**
	 * Constructor.
	 * Constructs a mapper for a writer which supports RMI.
	 * Simply give him the original writer as an input, e.g. a FileWriter to write to a file.
	 * @param writer The writer which should support RMI.
	 * @throws RemoteException
	 */
	public RMIWriter(Writer writer) throws RemoteException
	{
		remoteWriter = new RMIWriterRemoteImpl(writer);
	}

	@Override
	public void close() throws IOException
	{
		remoteWriter.close();
	}

	@Override
	public void flush() throws IOException
	{
		remoteWriter.flush();
	}

	@Override
	public void write(char[] arg0, int arg1, int arg2) throws IOException
	{
		remoteWriter.write(arg0, arg1, arg2);
	}

	/**
	 * The remote interface for the writer.
	 * @author Moritz Lang
	 * 
	 */
	private interface RMIWriterRemoteInterface extends Remote
	{

		public void close() throws IOException, RemoteException;

		public void flush() throws IOException, RemoteException;

		public void write(char[] arg0, int arg1, int arg2) throws IOException, RemoteException;
	}

	/**
	 * The remote writer. This is actually where the RMI comes in...
	 * @author Moritz Lang
	 * 
	 */
	private class RMIWriterRemoteImpl extends UnicastRemoteObject implements RMIWriterRemoteInterface
	{

		/**
		 * Serial Version UID.
		 */
		private static final long	serialVersionUID	= 2119701496933517850L;
		private final Writer		writer;

		/**
		 * @throws RemoteException
		 */
		protected RMIWriterRemoteImpl(Writer writer) throws RemoteException
		{
			super();
			this.writer = writer;
		}

		@Override
		public void close() throws IOException, RemoteException
		{
			writer.close();
		}

		@Override
		public void flush() throws IOException, RemoteException
		{
			writer.flush();
		}

		@Override
		public void write(char[] arg0, int arg1, int arg2) throws IOException, RemoteException
		{
			writer.write(arg0, arg1, arg2);
		}

	}
}
