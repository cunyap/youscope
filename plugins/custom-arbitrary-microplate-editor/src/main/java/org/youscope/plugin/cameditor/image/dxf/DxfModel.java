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
package org.youscope.plugin.cameditor.image.dxf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A tiny, dependency-free reader for the subset of ASCII DXF we need for chip drawings:
 * CIRCLE, LINE, LWPOLYLINE, ARC and ELLIPSE, plus the {@code $INSUNITS} header variable.
 * <p>
 * DXF is a flat list of (group-code, value) line pairs. We chose a focused built-in reader over an
 * external library (e.g. Kabeja) because the available Java DXF libraries are unmaintained and
 * fragmented on Maven Central, and because a small reader keeps the plugin footprint minimal and the
 * licensing clean. It intentionally ignores blocks/inserts, text and splines. DXF is a Y-up coordinate
 * system in real drawing units; those units are preserved here and mapped to image space by
 * {@link DxfImageLoader}.
 *
 * @author CAME
 */
public final class DxfModel
{
	/** DXF $INSUNITS integer codes mapped to micrometres per drawing unit; 0 (unitless) -> unknown. */
	public static double micronsPerUnit(int insUnits)
	{
		switch(insUnits)
		{
			case 1: return 25400.0;      // inches
			case 2: return 304800.0;     // feet
			case 4: return 1000.0;       // millimetres
			case 5: return 10000.0;      // centimetres
			case 6: return 1000000.0;    // metres
			case 8: return 0.0254;       // microinches
			case 13: return 1.0;         // micrometres
			case 11: return 0.1;         // Angstroms (0.1 nm) -> 0.0001 um... kept approximate
			default: return 0.0;         // 0 = unitless / unknown
		}
	}

	public static final class Circle { public final double cx, cy, r; public Circle(double cx,double cy,double r){this.cx=cx;this.cy=cy;this.r=r;} }
	public static final class Line { public final double x1, y1, x2, y2; public Line(double x1,double y1,double x2,double y2){this.x1=x1;this.y1=y1;this.x2=x2;this.y2=y2;} }
	public static final class Arc { public final double cx, cy, r, startDeg, endDeg; public Arc(double cx,double cy,double r,double s,double e){this.cx=cx;this.cy=cy;this.r=r;this.startDeg=s;this.endDeg=e;} }
	public static final class Ellipse { public final double cx, cy, majorDx, majorDy, ratio; public Ellipse(double cx,double cy,double mdx,double mdy,double ratio){this.cx=cx;this.cy=cy;this.majorDx=mdx;this.majorDy=mdy;this.ratio=ratio;} }
	public static final class Polyline { public final double[] xs, ys; public final boolean closed; public Polyline(double[] xs,double[] ys,boolean closed){this.xs=xs;this.ys=ys;this.closed=closed;} }

	private final List<Circle> circles = new ArrayList<Circle>();
	private final List<Line> lines = new ArrayList<Line>();
	private final List<Arc> arcs = new ArrayList<Arc>();
	private final List<Ellipse> ellipses = new ArrayList<Ellipse>();
	private final List<Polyline> polylines = new ArrayList<Polyline>();
	private int insUnits = 0;

	public List<Circle> getCircles() { return circles; }
	public List<Line> getLines() { return lines; }
	public List<Arc> getArcs() { return arcs; }
	public List<Ellipse> getEllipses() { return ellipses; }
	public List<Polyline> getPolylines() { return polylines; }
	public int getInsUnits() { return insUnits; }

	public boolean isEmpty()
	{
		return circles.isEmpty() && lines.isEmpty() && arcs.isEmpty() && ellipses.isEmpty() && polylines.isEmpty();
	}

	public static DxfModel parse(File file) throws IOException
	{
		DxfModel model = new DxfModel();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "ISO-8859-1"));
		try
		{
			model.parse(reader);
		}
		finally
		{
			reader.close();
		}
		return model;
	}

	private void parse(BufferedReader reader) throws IOException
	{
		String section = null;
		boolean inEntities = false;
		boolean inHeader = false;
		String pendingHeaderVar = null;

		// current entity accumulation
		String type = null;
		List<Double> xs = new ArrayList<Double>();
		List<Double> ys = new ArrayList<Double>();
		Double x2 = null, y2 = null, val40 = null, val50 = null, val51 = null;
		Integer flags70 = null;

		String codeLine;
		while((codeLine = reader.readLine()) != null)
		{
			String valueLine = reader.readLine();
			if(valueLine == null)
				break;
			int code;
			try
			{
				code = Integer.parseInt(codeLine.trim());
			}
			catch(NumberFormatException e)
			{
				continue; // tolerate stray lines
			}
			String value = valueLine.trim();
			if(code == 999)
				continue; // comment

			if(code == 0)
			{
				// finalize any entity in progress
				if(inEntities && type != null)
					finalizeEntity(type, xs, ys, x2, y2, val40, val50, val51, flags70);
				// reset accumulator
				type = value;
				xs = new ArrayList<Double>();
				ys = new ArrayList<Double>();
				x2 = null; y2 = null; val40 = null; val50 = null; val51 = null; flags70 = null;

				if("SECTION".equals(value))
				{
					section = null; // name arrives via code 2
				}
				else if("ENDSEC".equals(value))
				{
					inEntities = false;
					inHeader = false;
					section = null;
					type = null;
				}
				else if("EOF".equals(value))
				{
					break;
				}
				continue;
			}

			if(code == 2 && section == null && type != null && "SECTION".equals(type))
			{
				section = value;
				inEntities = "ENTITIES".equals(section);
				inHeader = "HEADER".equals(section);
				type = null; // Section header consumed
				continue;
			}

			if(inHeader)
			{
				if(code == 9)
					pendingHeaderVar = value;
				else if(code == 70 && "$INSUNITS".equals(pendingHeaderVar))
				{
					try { insUnits = Integer.parseInt(value); } catch(NumberFormatException ignore) { /* keep 0 */ }
					pendingHeaderVar = null;
				}
				continue;
			}

			if(!inEntities || type == null)
				continue;

			switch(code)
			{
				case 10: xs.add(parseD(value)); break;
				case 20: ys.add(parseD(value)); break;
				case 11: x2 = parseD(value); break;
				case 21: y2 = parseD(value); break;
				case 40: val40 = parseD(value); break;
				case 50: val50 = parseD(value); break;
				case 51: val51 = parseD(value); break;
				case 70: try { flags70 = Integer.valueOf((int) Math.round(Double.parseDouble(value))); } catch(NumberFormatException ignore) { } break;
				default: break;
			}
		}
		// finalize trailing entity (if a file ends without a closing code 0)
		if(inEntities && type != null)
			finalizeEntity(type, xs, ys, x2, y2, val40, val50, val51, flags70);
	}

	private void finalizeEntity(String type, List<Double> xs, List<Double> ys, Double x2, Double y2, Double val40, Double val50, Double val51, Integer flags70)
	{
		if(type == null)
			return;
		if("CIRCLE".equals(type) && !xs.isEmpty() && !ys.isEmpty() && val40 != null)
		{
			circles.add(new Circle(xs.get(0), ys.get(0), val40));
		}
		else if("LINE".equals(type) && !xs.isEmpty() && !ys.isEmpty() && x2 != null && y2 != null)
		{
			lines.add(new Line(xs.get(0), ys.get(0), x2, y2));
		}
		else if("ARC".equals(type) && !xs.isEmpty() && !ys.isEmpty() && val40 != null)
		{
			arcs.add(new Arc(xs.get(0), ys.get(0), val40, val50 == null ? 0 : val50, val51 == null ? 360 : val51));
		}
		else if("ELLIPSE".equals(type) && !xs.isEmpty() && !ys.isEmpty() && x2 != null && y2 != null)
		{
			ellipses.add(new Ellipse(xs.get(0), ys.get(0), x2, y2, val40 == null ? 1.0 : val40));
		}
		else if(("LWPOLYLINE".equals(type) || "POLYLINE".equals(type)) && !xs.isEmpty())
		{
			int n = Math.min(xs.size(), ys.size());
			if(n >= 2)
			{
				double[] px = new double[n];
				double[] py = new double[n];
				for(int i = 0; i < n; i++) { px[i] = xs.get(i); py[i] = ys.get(i); }
				boolean closed = flags70 != null && (flags70.intValue() & 1) != 0;
				polylines.add(new Polyline(px, py, closed));
			}
		}
	}

	private static Double parseD(String v)
	{
		try { return Double.valueOf(v); } catch(NumberFormatException e) { return Double.valueOf(0.0); }
	}

	// Overall bounds in DXF units

	public double[] bounds() // {minX, minY, maxX, maxY}
	{
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		for(Circle c : circles) { minX=Math.min(minX,c.cx-c.r); maxX=Math.max(maxX,c.cx+c.r); minY=Math.min(minY,c.cy-c.r); maxY=Math.max(maxY,c.cy+c.r); }
		for(Line l : lines) { minX=Math.min(minX,Math.min(l.x1,l.x2)); maxX=Math.max(maxX,Math.max(l.x1,l.x2)); minY=Math.min(minY,Math.min(l.y1,l.y2)); maxY=Math.max(maxY,Math.max(l.y1,l.y2)); }
		for(Arc a : arcs) { minX=Math.min(minX,a.cx-a.r); maxX=Math.max(maxX,a.cx+a.r); minY=Math.min(minY,a.cy-a.r); maxY=Math.max(maxY,a.cy+a.r); }
		for(Ellipse e : ellipses) { double rr=Math.hypot(e.majorDx,e.majorDy); minX=Math.min(minX,e.cx-rr); maxX=Math.max(maxX,e.cx+rr); minY=Math.min(minY,e.cy-rr); maxY=Math.max(maxY,e.cy+rr); }
		for(Polyline p : polylines) for(int i=0;i<p.xs.length;i++){ minX=Math.min(minX,p.xs[i]); maxX=Math.max(maxX,p.xs[i]); minY=Math.min(minY,p.ys[i]); maxY=Math.max(maxY,p.ys[i]); }
		if(minX == Double.POSITIVE_INFINITY)
			return new double[]{0,0,1,1};
		return new double[]{minX,minY,maxX,maxY};
	}
}
