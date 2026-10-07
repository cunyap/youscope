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
package org.youscope.uielements.scripteditor;

import java.util.EventListener;

/**
 * Interface to get informed when the undo/redo state changed. This information can be e.g. used to update undo/redo buttons, e.g. to deactivate the
 * undo button if undo is not possible.
 * @author Moritz Lang
 *
 */
public interface UndoRedoListener extends EventListener
{
	/**
	 * Fires when the undo/redo state changed (see interface description).
	 */
	public void undoRedoStateChanged();
}
