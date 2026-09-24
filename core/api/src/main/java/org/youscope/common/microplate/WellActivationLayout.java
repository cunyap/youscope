/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Andreas P. Cuny - initial API and implementation
 ******************************************************************************/
package org.youscope.common.microplate;

import java.util.Set;

import org.youscope.common.Well;

/**
 * Optional capability which a {@link MicroplateLayout} may additionally
 * implement to declare that
 * some of its wells are <i>structurally inactive</i> by default (e.g. pillars,
 * alignment marks, or a
 * registration-only well such as A1). Inactive wells are still part of the
 * layout (they are shown and
 * can optionally be enabled for testing/maintenance), but user interfaces which
 * let the user choose
 * which wells to image should, by default, present them as disabled so that
 * they are not accidentally
 * imaged.
 * <p>
 * This interface is purely additive and kept Java-7 compatible (no
 * default/static methods): layouts
 * which do not implement it behave exactly as before, and a layout implementing
 * it with an empty set
 * is indistinguishable from one that does not.
 *
 * @author Andreas P. Cuny
 */
public interface WellActivationLayout {
	/**
	 * Returns the set of wells which are inactive (not to be imaged) by default.
	 * Should never return
	 * {@code null}; an empty set means all wells are active.
	 * 
	 * @return the default-inactive wells.
	 */
	public Set<Well> getInactiveWells();
}
