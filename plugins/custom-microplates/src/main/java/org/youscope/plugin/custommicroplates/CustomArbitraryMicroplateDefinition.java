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
 *     Andreas P. Cuny - made public; added inactive wells (WellActivationLayout) and display-only well shapes (for CAME).
 ******************************************************************************/
package org.youscope.plugin.custommicroplates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.youscope.common.Well;
import org.youscope.common.configuration.Configuration;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.microplate.MicroplateLayout;
import org.youscope.common.microplate.WellActivationLayout;
import org.youscope.common.microplate.WellLayout;

/**
 * Definition of a custom, arbitrarily-shaped microplate.
 * <p>
 * <b>Custom Arbitrary Microplate Editor (CAME) changes (backward
 * compatible):</b>
 * <ul>
 * <li>The class was made {@code public} so that the separate CAME
 * plugin can read/write the same definition type.</li>
 * <li>It now implements {@link WellActivationLayout} and carries an optional
 * set of
 * <i>inactive</i> wells (pillars / structural / registration-only positions)
 * that are excluded
 * from imaging by default.</li>
 * <li>It carries an optional, display-only per-well {@link WellShape} (default
 * {@link WellShape#RECTANGLE}).
 * Shapes never influence acquisition.</li>
 * </ul>
 * The previous {@code @XStreamAlias} annotations were intentionally removed:
 * they were never actually
 * applied (ConfigurationManagement only processes annotations of registered
 * <i>configuration</i> classes,
 * not of this definition class), so all existing {@code .csb} files use
 * XStream's default element names
 * ({@code <wellLayouts>}, {@code <customMicroplateName>}, the fully-qualified
 * root element, etc.).
 * Keeping the default naming guarantees that all existing files keep loading,
 * and the new optional
 * fields simply default to empty when absent.
 *
 * @author Moritz Lang and Andreas P. Cuny
 */
public class CustomArbitraryMicroplateDefinition implements Serializable, Cloneable, MicroplateLayout,
		CustomMicroplateDefinition, Configuration, WellActivationLayout {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -5358378772356674069L;

	private final ArrayList<WellLayout> wellLayouts = new ArrayList<WellLayout>();
	private String customMicroplateName = "unnamed";

	/**
	 * Wells that are inactive (not imaged) by default. May be null in freshly
	 * deserialized legacy objects.
	 */
	private LinkedHashSet<Well> inactiveWells = new LinkedHashSet<Well>();

	/**
	 * Plate-level default shape; per-well overrides live in
	 * {@link #wellShapeOverrides}.
	 */
	private WellShape defaultWellShape = WellShape.RECTANGLE;

	/**
	 * Sparse per-well shape overrides (only wells whose shape differs from
	 * {@link #defaultWellShape}).
	 */
	private LinkedHashMap<Well, WellShape> wellShapeOverrides = new LinkedHashMap<Well, WellShape>();

	@Override
	protected Object clone() {
		// Build an independent copy. We do not use super.clone() because the collection
		// fields would
		// then be shared with the original (they are container instances).
		// WellLayout/Well elements are
		// immutable, so copying the container references is a sufficient deep copy. The
		// method keeps the
		// original's signature (protected, no checked exception) so existing callers
		// keep compiling.
		CustomArbitraryMicroplateDefinition copy = new CustomArbitraryMicroplateDefinition();
		copy.setWellLayouts(this.getWellLayouts());
		copy.setCustomMicroplateName(this.customMicroplateName);
		copy.setInactiveWells(this.getInactiveWells());
		copy.setDefaultWellShape(this.getDefaultWellShape());
		copy.setWellShapeOverrides(this.getWellShapeOverrides());
		return copy;
	}

	public List<WellLayout> getWellLayouts() {
		return new ArrayList<WellLayout>(wellLayouts);
	}

	public void setWellLayouts(List<WellLayout> wellLayouts) {
		this.wellLayouts.clear();
		if (wellLayouts != null)
			this.wellLayouts.addAll(wellLayouts);
	}

	// WellActivationLayout / inactive wells

	@Override
	public Set<Well> getInactiveWells() {
		if (inactiveWells == null)
			inactiveWells = new LinkedHashSet<Well>();
		return new LinkedHashSet<Well>(inactiveWells);
	}

	public void setInactiveWells(Set<Well> wells) {
		this.inactiveWells = new LinkedHashSet<Well>();
		if (wells != null)
			this.inactiveWells.addAll(wells);
	}

	public boolean isWellInactive(Well well) {
		return well != null && inactiveWells != null && inactiveWells.contains(well);
	}

	public void setWellInactive(Well well, boolean inactive) {
		if (well == null)
			return;
		if (inactiveWells == null)
			inactiveWells = new LinkedHashSet<Well>();
		if (inactive)
			inactiveWells.add(well);
		else
			inactiveWells.remove(well);
	}

	// Display-only shapes

	public WellShape getDefaultWellShape() {
		return defaultWellShape == null ? WellShape.RECTANGLE : defaultWellShape;
	}

	public void setDefaultWellShape(WellShape shape) {
		this.defaultWellShape = shape == null ? WellShape.RECTANGLE : shape;
	}

	public Map<Well, WellShape> getWellShapeOverrides() {
		if (wellShapeOverrides == null)
			wellShapeOverrides = new LinkedHashMap<Well, WellShape>();
		return new LinkedHashMap<Well, WellShape>(wellShapeOverrides);
	}

	public void setWellShapeOverrides(Map<Well, WellShape> overrides) {
		this.wellShapeOverrides = new LinkedHashMap<Well, WellShape>();
		if (overrides != null)
			this.wellShapeOverrides.putAll(overrides);
	}

	/**
	 * Returns the display shape for the given well: the per-well override if
	 * present, otherwise the
	 * plate default. Never {@code null}.
	 * 
	 * @param well well identifier.
	 * @return effective display shape.
	 */
	public WellShape getWellShape(Well well) {
		if (well != null && wellShapeOverrides != null) {
			WellShape override = wellShapeOverrides.get(well);
			if (override != null)
				return override;
		}
		return getDefaultWellShape();
	}

	public void setWellShape(Well well, WellShape shape) {
		if (well == null)
			return;
		if (wellShapeOverrides == null)
			wellShapeOverrides = new LinkedHashMap<Well, WellShape>();
		if (shape == null || shape == getDefaultWellShape())
			wellShapeOverrides.remove(well);
		else
			wellShapeOverrides.put(well, shape);
	}

	// CustomMicroplateDefinition

	@Override
	public void setCustomMicroplateName(String customMicroplateName) {
		this.customMicroplateName = customMicroplateName;
	}

	@Override
	public String getCustomMicroplateName() {
		return customMicroplateName;
	}

	// MicroplateLayout

	@Override
	public Iterator<WellLayout> iterator() {
		return new Iterator<WellLayout>() {
			int currentPos = 0;

			@Override
			public boolean hasNext() {
				return currentPos < wellLayouts.size();
			}

			@Override
			public WellLayout next() {
				return wellLayouts.get(currentPos++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int getNumWells() {
		return wellLayouts.size();
	}

	@Override
	public WellLayout getWell(int index) {
		if (index < 0 || index >= wellLayouts.size())
			return null;
		return wellLayouts.get(index);
	}

	@Override
	public WellLayout getWell(Well well) {
		for (WellLayout wellLayout : wellLayouts) {
			if (wellLayout.getWell().equals(well))
				return wellLayout;
		}
		return null;
	}

	@Override
	public String getTypeIdentifier() {
		return CustomMicroplateManager.getCustomMicroplateTypeIdentifier(customMicroplateName);
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		// do nothing.
	}
}
