/*******************************************************************************
 * Copyright (c) 2026 Andreas P. Cuny.
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
package org.youscope.plugin.cameditor.model;

import org.youscope.plugin.custommicroplates.WellShape;

/**
 * A single, mutable well while it is being authored in the editor. Geometry is stored in image/scene
 * pixels (the editing unit); micrometre coordinates are derived at export via {@link ScaleModel}.
 * <p>
 * {@code wellX} (column, 0-based) and {@code wellY} (row, 0-based) follow YouScope's standard
 * convention: rows are shown as letters (A, B, ...), columns as numbers (1, 2, ...), so (0,0) is A1.
 * A value of -1 means "not yet assigned".
 *
 * @author Andreas P. Cuny
 */
public final class EditableWell
{
	private double xPx;
	private double yPx;
	private double widthPx;
	private double heightPx;
	private WellShape shape = WellShape.RECTANGLE;
	private boolean active = true;
	private int wellX = -1;
	private int wellY = -1;

	public EditableWell(double xPx, double yPx, double widthPx, double heightPx)
	{
		this.xPx = xPx;
		this.yPx = yPx;
		this.widthPx = Math.max(1.0, widthPx);
		this.heightPx = Math.max(1.0, heightPx);
	}

	public double getXPx() { return xPx; }
	public double getYPx() { return yPx; }
	public double getWidthPx() { return widthPx; }
	public double getHeightPx() { return heightPx; }
	public double getCenterXPx() { return xPx + widthPx / 2.0; }
	public double getCenterYPx() { return yPx + heightPx / 2.0; }

	public void setXPx(double v) { this.xPx = v; }
	public void setYPx(double v) { this.yPx = v; }
	public void setWidthPx(double v) { this.widthPx = Math.max(1.0, v); }
	public void setHeightPx(double v) { this.heightPx = Math.max(1.0, v); }

	public void setBoundsPx(double x, double y, double w, double h)
	{
		this.xPx = x;
		this.yPx = y;
		this.widthPx = Math.max(1.0, w);
		this.heightPx = Math.max(1.0, h);
	}

	public WellShape getShape() { return shape == null ? WellShape.RECTANGLE : shape; }
	public void setShape(WellShape shape) { this.shape = shape == null ? WellShape.RECTANGLE : shape; }

	public boolean isActive() { return active; }
	public void setActive(boolean active) { this.active = active; }

	public int getWellX() { return wellX; }
	public int getWellY() { return wellY; }
	public void setWellX(int wellX) { this.wellX = wellX; }
	public void setWellY(int wellY) { this.wellY = wellY; }

	/** True if a logical identifier (row/column) has been assigned. */
	public boolean hasIdentifier() { return wellX >= 0 && wellY >= 0; }

	public boolean containsPx(double px, double py)
	{
		return px >= xPx && px <= xPx + widthPx && py >= yPx && py <= yPx + heightPx;
	}
}
