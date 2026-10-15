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

package org.youscope.plugin.argolight;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;

import javax.swing.Icon;

/**
 * Draws small distinctive vector glyphs for the different Argolight test
 * patterns. Everything is rendered with Java2D, so no image assets need to be
 * bundled and the icons scale crisply to any marker size.
 *
 * @author Andreas P. Cuny
 */
final class ArgolightPatternIcons {
	private ArgolightPatternIcons() {
		// utility class.
	}

	/**
	 * Paints the glyph for the given icon type, scaled to fit within the box.
	 * 
	 * @param g2         graphics context.
	 * @param iconType   glyph key.
	 * @param bx         left of the box.
	 * @param by         top of the box.
	 * @param boxSize    box edge length in pixels.
	 * @param color      foreground color.
	 * @param iconScale  glyph size relative to the box (1.0 = fill box; C1/C3 use
	 *                   <1).
	 * @param iconDetail glyph-specific detail (e.g. number of stair steps).
	 */
	static void paintGlyph(Graphics2D g2, String iconType, int bx, int by, int boxSize, Color color,
			double iconScale, double iconDetail) {
		Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		Stroke oldStroke = g2.getStroke();
		Color oldColor = g2.getColor();
		Shape oldClip = g2.getClip();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(color);

		if (iconScale <= 0)
			iconScale = 1.0;
		int size = (int) Math.round(boxSize * Math.min(1.0, iconScale));
		int x = bx + (boxSize - size) / 2;
		int y = by + (boxSize - size) / 2;
		// Keep every glyph inside the marker box.
		g2.clipRect(bx, by, boxSize, boxSize);

		float lw = Math.max(1f, size / 14f);
		g2.setStroke(new BasicStroke(lw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		int cx = x + size / 2;
		int cy = y + size / 2;
		int r = (int) (size * 0.40);
		String key = iconType == null ? "generic" : iconType;

		switch (key) {
			case "cross":
				g2.drawLine(cx - r, cy, cx + r, cy);
				g2.drawLine(cx, cy - r, cx, cy + r);
				break;
			case "logo":
				// Capital "A" (Argolight logo).
				int aTop = y + (int) (size * 0.16);
				int aBot = y + (int) (size * 0.84);
				int aLeft = x + (int) (size * 0.24);
				int aRight = x + (int) (size * 0.76);
				g2.drawLine(aLeft, aBot, cx, aTop); // left leg
				g2.drawLine(aRight, aBot, cx, aTop); // right leg
				int barY = y + (int) (size * 0.60);
				g2.drawLine(x + (int) (size * 0.36), barY, x + (int) (size * 0.64), barY); // crossbar
				break;
			case "target":
				g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
				g2.drawOval(cx - r / 2, cy - r / 2, r, r);
				g2.fillOval(cx - Math.max(2, (int) lw), cy - Math.max(2, (int) lw), Math.max(4, (int) (2 * lw)),
						Math.max(4, (int) (2 * lw)));
				break;
			case "ringgrid":
				paintRingGrid(g2, x, y, size, iconDetail > 0 ? (int) iconDetail : 3);
				break;
			case "rings":
				paintRingGrid(g2, x, y, size, 3);
				break;
			case "intensity4x4":
				paintIntensitySquares(g2, x, y, size, color, 4);
				break;
			case "grid4x4":
				drawGrid(g2, x, y, size, 4, 4);
				break;
			case "intensity2col":
				paintTwoColumnGradient(g2, x, y, size, color);
				break;
			case "grid2x16":
				drawGrid(g2, x, y, size, 8, 2);
				break;
			case "linesH":
				paintGraduatedLines(g2, x, y, size, 0);
				break;
			case "linesV":
				paintGraduatedLines(g2, x, y, size, 90);
				break;
			case "linesP45":
				paintGraduatedLines(g2, x, y, size, 45);
				break;
			case "linesM45":
				paintGraduatedLines(g2, x, y, size, -45);
				break;
			case "crossmatrix":
				int gn = iconDetail > 0 ? (int) iconDetail : 3;
				g2.drawRect(x + size / 12, y + size / 12, size - size / 6, size - size / 6);
				for (int gx = 0; gx < gn; gx++) {
					for (int gy = 0; gy < gn; gy++) {
						int mcx = x + size / 6 + (int) ((gx + 0.5) * (size - size / 3) / gn);
						int mcy = y + size / 6 + (int) ((gy + 0.5) * (size - size / 3) / gn);
						int mr = Math.max(1, (size - size / 3) / (gn * 3));
						g2.drawLine(mcx - mr, mcy, mcx + mr, mcy);
						g2.drawLine(mcx, mcy - mr, mcx, mcy + mr);
					}
				}
				break;
			case "meridians":
				g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
				g2.drawOval(cx - r / 3, cy - r, 2 * r / 3, 2 * r);
				g2.drawOval(cx - 2 * r / 3, cy - r, 4 * r / 3, 2 * r);
				g2.drawLine(cx, cy - r, cx, cy + r);
				break;
			case "stairs":
				paintStairs(g2, x, y, size, iconDetail > 0 ? (int) iconDetail : 4);
				break;
			case "background":
				paintBackground(g2, x, y, size);
				break;
			default:
				g2.drawOval(cx - r, cy - r, 2 * r, 2 * r);
				g2.drawLine(cx - r / 2, cy - r / 2, cx + r / 2, cy + r / 2);
				break;
		}

		g2.setClip(oldClip);
		g2.setStroke(oldStroke);
		g2.setColor(oldColor);
		if (oldAA != null)
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
	}

	/** Backwards-compatible entry point (scale 1.0, no detail). */
	static void paintGlyph(Graphics2D g2, String iconType, int x, int y, int size, Color color) {
		paintGlyph(g2, iconType, x, y, size, color, 1.0, 0);
	}

	/** A4: circles arranged on a regular square grid. */
	private static void paintRingGrid(Graphics2D g2, int x, int y, int size, int n) {
		int m = size / 10;
		int cell = (size - 2 * m) / n;
		int d = (int) (cell * 0.7);
		for (int gx = 0; gx < n; gx++) {
			for (int gy = 0; gy < n; gy++) {
				int cxk = x + m + gx * cell + (cell - d) / 2;
				int cyk = y + m + gy * cell + (cell - d) / 2;
				g2.drawOval(cxk, cyk, d, d);
			}
		}
	}

	/**
	 * B1: 4x4 filled squares, intensity highest at top-left decreasing
	 * left-to-right, top-to-bottom.
	 */
	private static void paintIntensitySquares(Graphics2D g2, int x, int y, int size, Color base, int n) {
		int m = size / 12;
		int cell = (size - 2 * m) / n;
		int total = n * n;
		for (int row = 0; row < n; row++) {
			for (int col = 0; col < n; col++) {
				int index = row * n + col; // 0 = top-left = highest
				float f = 1f - (index / (float) (total - 1)); // 1..0
				int alpha = (int) (40 + 215 * f);
				g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
				g2.fillRect(x + m + col * cell, y + m + row * cell, cell - 1, cell - 1);
				g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 90));
				g2.drawRect(x + m + col * cell, y + m + row * cell, cell - 1, cell - 1);
			}
		}
		g2.setColor(base);
	}

	/** B2: two vertical intensity columns running in opposite directions. */
	private static void paintTwoColumnGradient(Graphics2D g2, int x, int y, int size, Color base) {
		int m = size / 12;
		int steps = 8;
		int colW = (size - 3 * m) / 2;
		int h = size - 2 * m;
		int cellH = h / steps;
		for (int i = 0; i < steps; i++) {
			float fLeft = (i + 1) / (float) steps; // left: top low -> bottom high
			g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (40 + 215 * fLeft)));
			g2.fillRect(x + m, y + m + i * cellH, colW, cellH - 1);
			float fRight = 1f - (i / (float) steps); // right: top high -> bottom low
			g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int) (40 + 215 * fRight)));
			g2.fillRect(x + 2 * m + colW, y + m + i * cellH, colW, cellH - 1);
		}
		g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 120));
		g2.drawRect(x + m, y + m, colW, h);
		g2.drawRect(x + 2 * m + colW, y + m, colW, h);
		g2.setColor(base);
	}

	/**
	 * D1-D4: staircase; overall size already scaled via iconScale, nSteps via
	 * iconDetail.
	 */
	private static void paintStairs(Graphics2D g2, int x, int y, int size, int nSteps) {
		if (nSteps < 1)
			nSteps = 1;
		int stepW = size / nSteps;
		int stepH = size / nSteps;
		int px = x;
		int py = y + size;
		for (int k = 0; k < nSteps; k++) {
			int nx = px + stepW;
			int ny = py - stepH;
			g2.drawLine(px, py, nx, py); // tread
			g2.drawLine(nx, py, nx, ny); // riser
			px = nx;
			py = ny;
		}
	}

	/**
	 * Draws a set of parallel, gradually more widely spaced lines, rotated by
	 * {@code angleDeg} about the box centre. All the line glyphs (H, V, +45, -45)
	 * use this one drawer, so e.g. the +45 glyph is exactly the -45 glyph rotated
	 * 90 degrees.
	 */
	private static void paintGraduatedLines(Graphics2D g2, int x, int y, int size, double angleDeg) {
		int cx = x + size / 2;
		int cy = y + size / 2;
		java.awt.geom.AffineTransform old = g2.getTransform();
		g2.rotate(Math.toRadians(angleDeg), cx, cy);
		int half = size / 2;
		int span = (int) (size * 0.42); // horizontal half-length of each line
		// Gradually increasing gaps from the top downward.
		double[] frac = { -0.90, -0.55, -0.25, -0.02, 0.24, 0.55, 0.92 };
		for (double f : frac) {
			int yy = cy + (int) (f * half);
			g2.drawLine(cx - span, yy, cx + span, yy);
		}
		g2.setTransform(old);
	}

	private static void drawGrid(Graphics2D g2, int x, int y, int size, int cols, int rows) {
		int m = size / 6;
		int w = size - 2 * m;
		int h = size - 2 * m;
		for (int c = 0; c <= cols; c++)
			g2.drawLine(x + m + c * w / cols, y + m, x + m + c * w / cols, y + m + h);
		for (int rr = 0; rr <= rows; rr++)
			g2.drawLine(x + m, y + m + rr * h / rows, x + m + w, y + m + rr * h / rows);
	}

	/**
	 * Background region: an empty field-of-view glyph.
	 *
	 * Draws four corner brackets around the field and a clear "BG" label
	 * in the centre to immediately identify the region as a background area.
	 */
	private static void paintBackground(Graphics2D g2, int x, int y, int size) {
		int m = Math.max(2, size / 8);
		int left = x + m;
		int top = y + m;
		int right = x + size - m;
		int bottom = y + size - m;

		int corner = Math.max(3, size / 5);

		// Four corner brackets.
		g2.drawLine(left, top, left + corner, top);
		g2.drawLine(left, top, left, top + corner);

		g2.drawLine(right, top, right - corner, top);
		g2.drawLine(right, top, right, top + corner);

		g2.drawLine(left, bottom, left + corner, bottom);
		g2.drawLine(left, bottom, left, bottom - corner);

		g2.drawLine(right, bottom, right - corner, bottom);
		g2.drawLine(right, bottom, right, bottom - corner);

		// Clear "BG" label in the centre.
		String label = "BG";

		java.awt.Font oldFont = g2.getFont();

		int fontSize = Math.max(8, (int) (size * 0.34));
		java.awt.Font font = oldFont.deriveFont(
				java.awt.Font.BOLD,
				(float) fontSize);

		g2.setFont(font);

		java.awt.FontMetrics fm = g2.getFontMetrics(font);

		int textWidth = fm.stringWidth(label);
		int textHeight = fm.getAscent();

		int cx = x + size / 2;
		int cy = y + size / 2;

		int textX = cx - textWidth / 2;
		int textY = cy + textHeight / 2;

		g2.drawString(label, textX, textY);

		g2.setFont(oldFont);
	}

	/**
	 * Builds a Swing {@link Icon} rendering the given glyph, e.g. for legends.
	 * 
	 * @param iconType glyph key.
	 * @param size     icon edge length.
	 * @param color    glyph color.
	 * @return a Swing icon.
	 */
	static Icon createIcon(final String iconType, final int size, final Color color) {
		return new Icon() {
			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				paintGlyph((Graphics2D) g, iconType, x, y, size, color, 1.0, 0);
			}

			@Override
			public int getIconWidth() {
				return size;
			}

			@Override
			public int getIconHeight() {
				return size;
			}
		};
	}
}
