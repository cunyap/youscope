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
package org.youscope.plugin.cameditor.image.dxf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.youscope.plugin.cameditor.detect.DetectedWell;
import org.youscope.plugin.cameditor.image.LoadedImage;

/**
 * Renders a DXF drawing to a raster backdrop and, crucially, extracts wells directly from the vector
 * geometry so no computer-vision step is required: each CIRCLE / ELLIPSE / closed rectangular
 * LWPOLYLINE becomes a well. Because DXF carries real drawing units, the physical scale (\u00b5m/px) can be
 * suggested automatically from the {@code $INSUNITS} header.
 * <p>
 * DXF is Y-up; the raster is Y-down. All coordinates are mapped as
 * {@code px = (x - minX) * pixelsPerUnit}, {@code py = (maxY - y) * pixelsPerUnit}, so extracted wells
 * live in the same pixel space as the returned backdrop image.
 *
 * @author Andreas P. Cuny
 */
public final class DxfImageLoader
{
	/** Result of importing a DXF drawing. */
	public static final class Result
	{
		public final LoadedImage image;
		public final List<DetectedWell> wells;
		public Result(LoadedImage image, List<DetectedWell> wells) { this.image = image; this.wells = wells; }
	}

	private double pixelsPerUnit = 4.0;
	private int maxDimensionPixels = 6000;

	public void setPixelsPerUnit(double pixelsPerUnit)
	{
		if(pixelsPerUnit > 0)
			this.pixelsPerUnit = pixelsPerUnit;
	}

	public Result load(File file) throws IOException
	{
		DxfModel model = DxfModel.parse(file);
		double[] b = model.bounds();
		double minX = b[0], minY = b[1], maxX = b[2], maxY = b[3];
		double spanX = Math.max(1e-6, maxX - minX);
		double spanY = Math.max(1e-6, maxY - minY);

		double ppu = pixelsPerUnit;
		// clamp resolution so huge drawings don't blow up memory
		double marginUnits = 0.02 * Math.max(spanX, spanY);
		int width = (int) Math.ceil((spanX + 2 * marginUnits) * ppu);
		int height = (int) Math.ceil((spanY + 2 * marginUnits) * ppu);
		if(width > maxDimensionPixels || height > maxDimensionPixels)
		{
			double factor = (double) maxDimensionPixels / Math.max(width, height);
			ppu *= factor;
			width = (int) Math.ceil((spanX + 2 * marginUnits) * ppu);
			height = (int) Math.ceil((spanY + 2 * marginUnits) * ppu);
		}
		width = Math.max(1, width);
		height = Math.max(1, height);
		final double fppu = ppu;
		final double ox = minX - marginUnits;
		final double oyTop = maxY + marginUnits; // y flips around this

		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.BLACK);
			g.setStroke(new BasicStroke(1.2f));

			for(DxfModel.Line l : model.getLines())
				g.draw(new Line2D.Double(tx(l.x1, ox, fppu), ty(l.y1, oyTop, fppu), tx(l.x2, ox, fppu), ty(l.y2, oyTop, fppu)));
			for(DxfModel.Circle c : model.getCircles())
			{
				double px = tx(c.cx - c.r, ox, fppu);
				double py = ty(c.cy + c.r, oyTop, fppu);
				double d = 2 * c.r * fppu;
				g.draw(new Ellipse2D.Double(px, py, d, d));
			}
			for(DxfModel.Ellipse e : model.getEllipses())
			{
				double major = Math.hypot(e.majorDx, e.majorDy);
				double minor = major * e.ratio;
				double px = tx(e.cx - major, ox, fppu);
				double py = ty(e.cy + minor, oyTop, fppu);
				g.draw(new Ellipse2D.Double(px, py, 2 * major * fppu, 2 * minor * fppu));
			}
			for(DxfModel.Arc a : model.getArcs())
			{
				java.awt.geom.Arc2D.Double arc = new java.awt.geom.Arc2D.Double();
				double px = tx(a.cx - a.r, ox, fppu);
				double py = ty(a.cy + a.r, oyTop, fppu);
				// DXF angles are counter-clockwise from +x; Java2D arc angles are counter-clockwise too,
				// but our y is flipped, so negate the angles.
				double start = -a.startDeg;
				double extent = -((a.endDeg - a.startDeg + 360) % 360);
				arc.setArc(px, py, 2 * a.r * fppu, 2 * a.r * fppu, start, extent, java.awt.geom.Arc2D.OPEN);
				g.draw(arc);
			}
			for(DxfModel.Polyline p : model.getPolylines())
			{
				GeneralPath path = new GeneralPath();
				path.moveTo(tx(p.xs[0], ox, fppu), ty(p.ys[0], oyTop, fppu));
				for(int i = 1; i < p.xs.length; i++)
					path.lineTo(tx(p.xs[i], ox, fppu), ty(p.ys[i], oyTop, fppu));
				if(p.closed)
					path.closePath();
				g.draw(path);
			}
		}
		finally
		{
			g.dispose();
		}

		// Extract wells directly from vector entities
		List<DetectedWell> wells = new ArrayList<DetectedWell>();
		for(DxfModel.Circle c : model.getCircles())
		{
			double x = tx(c.cx - c.r, ox, fppu);
			double y = ty(c.cy + c.r, oyTop, fppu);
			double d = 2 * c.r * fppu;
			wells.add(new DetectedWell(x, y, d, d, x + d / 2, y + d / 2, (int) Math.round(Math.PI * (d / 2) * (d / 2))));
		}
		for(DxfModel.Ellipse e : model.getEllipses())
		{
			double major = Math.hypot(e.majorDx, e.majorDy);
			double minor = major * e.ratio;
			double x = tx(e.cx - major, ox, fppu);
			double y = ty(e.cy + minor, oyTop, fppu);
			double w = 2 * major * fppu, h = 2 * minor * fppu;
			wells.add(new DetectedWell(x, y, w, h, x + w / 2, y + h / 2, (int) Math.round(Math.PI * (w / 2) * (h / 2))));
		}
		for(DxfModel.Polyline p : model.getPolylines())
		{
			if(!p.closed)
				continue;
			// Treat any closed polyline as a rectangular well via its bounding box
			double pminx = Double.POSITIVE_INFINITY, pminy = Double.POSITIVE_INFINITY, pmaxx = Double.NEGATIVE_INFINITY, pmaxy = Double.NEGATIVE_INFINITY;
			for(int i = 0; i < p.xs.length; i++)
			{
				double sx = tx(p.xs[i], ox, fppu);
				double sy = ty(p.ys[i], oyTop, fppu);
				pminx = Math.min(pminx, sx); pmaxx = Math.max(pmaxx, sx);
				pminy = Math.min(pminy, sy); pmaxy = Math.max(pmaxy, sy);
			}
			double w = pmaxx - pminx, h = pmaxy - pminy;
			if(w > 0 && h > 0)
				wells.add(new DetectedWell(pminx, pminy, w, h, pminx + w / 2, pminy + h / 2, (int) Math.round(w * h)));
		}

		double micronsPerUnit = DxfModel.micronsPerUnit(model.getInsUnits());
		double knownMpp = micronsPerUnit > 0 ? micronsPerUnit / fppu : -1;
		LoadedImage loaded = new LoadedImage(image, knownMpp, file.getName());
		return new Result(loaded, wells);
	}

	private static double tx(double x, double ox, double ppu) { return (x - ox) * ppu; }
	private static double ty(double y, double oyTop, double ppu) { return (oyTop - y) * ppu; }
}
