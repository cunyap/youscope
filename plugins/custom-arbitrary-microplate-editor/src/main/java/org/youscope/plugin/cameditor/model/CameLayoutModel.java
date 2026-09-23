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
package org.youscope.plugin.cameditor.model;

import java.util.ArrayList;
import java.util.List;

import org.youscope.common.Well;
import org.youscope.common.microplate.WellLayout;
import org.youscope.plugin.custommicroplates.CustomArbitraryMicroplateDefinition;
import org.youscope.plugin.custommicroplates.WellShape;
import org.youscope.plugin.cameditor.detect.DetectedWell;
import org.youscope.plugin.cameditor.detect.WellIdentifierAssigner;

/**
 * The editable layout being authored: a plate name, a default well shape, and a list of
 * {@link EditableWell}s in image/scene pixel coordinates. Converts to and from the shared
 * {@link CustomArbitraryMicroplateDefinition} using a {@link ScaleModel} (µm/px applied exactly once).
 *
 * @author Andreas P. Cuny
 */
public final class CameLayoutModel
{
	private String name = "unnamed";
	private WellShape defaultShape = WellShape.RECTANGLE;
	private final List<EditableWell> wells = new ArrayList<EditableWell>();
	private final WellIdentifierAssigner assigner = new WellIdentifierAssigner();

	public String getName() { return name; }
	public void setName(String name) { this.name = name == null ? "unnamed" : name; }

	public WellShape getDefaultShape() { return defaultShape == null ? WellShape.RECTANGLE : defaultShape; }
	public void setDefaultShape(WellShape shape) { this.defaultShape = shape == null ? WellShape.RECTANGLE : shape; }

	public List<EditableWell> getWells() { return wells; }

	public void clear() { wells.clear(); }

	public EditableWell addWell(double xPx, double yPx, double wPx, double hPx)
	{
		EditableWell well = new EditableWell(xPx, yPx, wPx, hPx);
		well.setShape(defaultShape);
		wells.add(well);
		return well;
	}

	public void removeWell(EditableWell well)
	{
		wells.remove(well);
	}

	/**
	 * Replaces the current wells with the given detections (pixel space) and (re-)assigns A1-style
	 * identifiers.
	 * @param detections detected wells in pixel coordinates.
	 * @param append if {@code true}, add to existing wells; otherwise replace them.
	 */
	public void setFromDetections(List<DetectedWell> detections, boolean append)
	{
		if(!append)
			wells.clear();
		if(detections != null)
		{
			for(DetectedWell d : detections)
			{
				EditableWell well = new EditableWell(d.x, d.y, d.width, d.height);
				well.setShape(defaultShape);
				wells.add(well);
			}
		}
		reassignIdentifiers();
	}

	/** Re-derives all row/column identifiers from the current geometry. */
	public void reassignIdentifiers()
	{
		assigner.assign(wells);
	}

	// Conversion to the shared YouScope definition

	/**
	 * Builds a {@link CustomArbitraryMicroplateDefinition} in micrometres from the current pixel-space
	 * wells, applying the given scale exactly once. Wells without a valid identifier are labelled on the
	 * fly. Zero-sized wells are skipped (YouScope requires strictly positive extents).
	 * @param scale µm/px scale.
	 * @return the definition ready to be saved.
	 */
	public CustomArbitraryMicroplateDefinition toDefinition(ScaleModel scale)
	{
		reassignIdentifiers();
		CustomArbitraryMicroplateDefinition def = new CustomArbitraryMicroplateDefinition();
		def.setCustomMicroplateName(name);
		def.setDefaultWellShape(defaultShape);

		List<WellLayout> layouts = new ArrayList<WellLayout>();
		java.util.LinkedHashSet<Well> inactive = new java.util.LinkedHashSet<Well>();
		for(EditableWell w : wells)
		{
			double x = scale.pxToUm(w.getXPx());
			double y = scale.pxToUm(w.getYPx());
			double width = scale.pxToUm(w.getWidthPx());
			double height = scale.pxToUm(w.getHeightPx());
			if(width <= 0 || height <= 0)
				continue;
			Well well = new Well(Math.max(0, w.getWellY()), Math.max(0, w.getWellX()));
			layouts.add(new WellLayout(x, y, width, height, well));
			if(!w.isActive())
				inactive.add(well);
		}
		def.setWellLayouts(layouts);
		def.setInactiveWells(inactive);

		// per-well shape overrides (only those differing from the default)
		for(EditableWell w : wells)
		{
			if(w.getShape() != defaultShape)
			{
				def.setWellShape(new Well(Math.max(0, w.getWellY()), Math.max(0, w.getWellX())), w.getShape());
			}
		}
		return def;
	}

	/**
	 * Populates this model from an existing definition (micrometres), converting to pixels with the
	 * given scale so it can be edited over an image (or, with scale = 1, edited directly in µm).
	 * @param def the definition to import.
	 * @param scale µm/px scale.
	 */
	public void fromDefinition(CustomArbitraryMicroplateDefinition def, ScaleModel scale)
	{
		wells.clear();
		if(def == null)
			return;
		setName(def.getCustomMicroplateName());
		setDefaultShape(def.getDefaultWellShape());
		for(WellLayout layout : def.getWellLayouts())
		{
			Well well = layout.getWell();
			EditableWell editable = new EditableWell(
					scale.umToPx(layout.getX()),
					scale.umToPx(layout.getY()),
					scale.umToPx(layout.getWidth()),
					scale.umToPx(layout.getHeight()));
			editable.setWellX(well.getWellX());
			editable.setWellY(well.getWellY());
			editable.setShape(def.getWellShape(well));
			editable.setActive(!def.isWellInactive(well));
			wells.add(editable);
		}
	}
}
