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
