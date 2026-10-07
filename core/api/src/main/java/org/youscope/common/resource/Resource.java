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
package org.youscope.common.resource;

import java.rmi.RemoteException;

import org.youscope.common.Component;
import org.youscope.common.MeasurementContext;

/**
 * A resource is some configurable object which can be used by jobs during a measurement. This interface represents
 * the base class of other interfaces which should specify the functionality which can be carried out by this resource.
 * @author Moritz Lang
 *
 */
public interface Resource extends Component
{
	/**
	 * Returns the type identifier of the resource.
	 * 
	 * @return Type identifier of the resource
	 * @throws RemoteException 
	 */
	String getTypeIdentifier() throws RemoteException;
    
    /**
	 * Initializes the resource. Must be called before calling any resource functions except
	 *  <code>getTypeIdentifier</code>, <code>getConfigurationClass</code>, <code>isInitialized</code>,
	 *  <code>addDetectionOutputWriter</code>, or <code>removeMessageListener</code>.
     * @param measurementContext The measurement context.
	 * @throws ResourceException 
     * @throws RemoteException 
	 */
	public void initialize(MeasurementContext measurementContext) throws ResourceException,RemoteException;
	
	/**
	 * Called to free all acquired resources of the addon.
	 * This function may be called more than once, or without calling initialize a priori.
	 * @param measurementContext The measurement context.
	 * @throws ResourceException 
	 * @throws RemoteException 
	 */
	public void uninitialize(MeasurementContext measurementContext) throws ResourceException,RemoteException;
	
	/**
	 * Returns true if the addon is initialized.
	 * @return True if initialized, otherwise false.
	 * @throws RemoteException 
	 */
	public boolean isInitialized() throws RemoteException;
}
