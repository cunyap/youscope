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
package org.youscope.addon.component.generic;

import java.awt.Component;
import java.awt.event.ActionListener;

import org.youscope.common.MessageListener;

/**
 * Interface all editors for certain types of properties of a configuration implement.
 * @author Moritz Lang
 *
 */
public interface PropertyEditor 
{
	/**
	 * Returns the UI element of the editor.
	 * @return UI element of the editor.
	 */
	Component getEditor();
	/**
	 * When called, editor commits all current edits.
	 * @throws GenericException
	 */
	void commitEdits() throws GenericException;
	
	/**
	 * Adds a listener to get notified about errors and similar.
	 * @param listener message listener to add.
	 */
	void addMessageListener(MessageListener listener);

	/**
	 * Removes a previously added listener
	 * @param listener message listener to remove.
	 */
	void removeMessageListener(MessageListener listener);
	
	/**
	 * Adds a listener to be notified when the value of the property edited changed.
	 * @param listener action listener.
	 */
	void addActionListener(ActionListener listener);
	
	/**
	 * Removes a previously added listener.
	 * @param listener listener to remove.
	 */
	void removeActionListener(ActionListener listener);
	
	/**
	 * Returns true if this editor can fill up extra space. Return false, if this would lead to an ugly UI.
	 * @return TRUE if editor can fill up extra space.
	 */
	boolean isFillSpace();
	
	/**
	 * Get the property types supported by this editor.
	 * @return Supported property types.
	 */
	Class<?>[] getSupportedTypes();
}
