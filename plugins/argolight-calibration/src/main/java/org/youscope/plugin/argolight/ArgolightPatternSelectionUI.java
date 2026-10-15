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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import org.youscope.common.Well;
import org.youscope.common.microplate.WellWithGroup;

/**
 * Interactive Argolight pattern map. Every pattern is drawn at its true
 * relative
 * position (datasheet +Y-up preserved), with a distinctive vector glyph and its
 * name as a hover tooltip. Clicking a pattern toggles selection; selected
 * patterns are filled green. This replaces the generic well-selection widget so
 * users immediately recognise which position is which.
 *
 * @author Andreas P. Cuny
 */
class ArgolightPatternSelectionUI extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final Color SELECTED_FILL = new Color(76, 175, 80);
	private static final Color SELECTED_BORDER = new Color(27, 94, 32);
	private static final Color UNSELECTED_FILL = new Color(245, 245, 245);
	private static final Color UNSELECTED_BORDER = new Color(120, 120, 120);
	private static final Color GLYPH_ON = Color.WHITE;
	private static final Color GLYPH_OFF = new Color(60, 60, 60);
	private static final Color LABEL_COLOR = new Color(40, 40, 40);
	private static final int MARKER_MIN = 26;
	private static final int MARKER_MAX = 64;
	private static final int MARGIN = 44;

	private ArgolightPlateDefinition definition;
	private final HashSet<Well> selectedWells = new HashSet<>();
	private final ArrayList<ActionListener> selectionListeners = new ArrayList<>(1);

	/**
	 * Per-paint cache of pattern -> on-screen marker rectangle, for hit-testing.
	 */
	private final HashMap<ArgolightPattern, int[]> markerBounds = new HashMap<>();

	ArgolightPatternSelectionUI(ArgolightPlateDefinition definition) {
		this.definition = definition;
		setBackground(Color.WHITE);
		setPreferredSize(new Dimension(560, 520));
		setToolTipText("");

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				ArgolightPattern p = patternAt(e.getX(), e.getY());
				if (p == null)
					return;
				Well well = p.getWell();
				if (selectedWells.contains(well))
					selectedWells.remove(well);
				else
					selectedWells.add(well);
				repaint();
				fireSelectionChanged();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				ArgolightPattern p = patternAt(e.getX(), e.getY());
				String tip = p == null ? null
						: p.getName() + "  (" + Well.getWellName(p.getWellRow(), p.getWellCol()) + ", "
								+ formatUm(p.getRelativeXUm()) + ";" + formatUm(p.getRelativeYUm()) + " um"
								+ (p.isZStack() ? ", z-stack dz=" + p.getZStepUm() + " um" : "") + ")";
				setToolTipText(tip);
			}
		});
	}

	private static String formatUm(double v) {
		if (v == Math.rint(v))
			return Integer.toString((int) v);
		return Double.toString(v);
	}

	/**
	 * Replaces the plate definition and clears any selection referring to
	 * patterns that no longer exist.
	 * 
	 * @param definition new definition (non-null).
	 */
	void setDefinition(ArgolightPlateDefinition definition) {
		this.definition = definition;
		HashSet<Well> valid = new HashSet<>();
		for (ArgolightPattern p : definition.getPatterns()) {
			if (selectedWells.contains(p.getWell()))
				valid.add(p.getWell());
		}
		selectedWells.clear();
		selectedWells.addAll(valid);
		repaint();
		fireSelectionChanged();
	}

	/** @return the current plate definition. */
	ArgolightPlateDefinition getDefinition() {
		return definition;
	}

	/** Selects all patterns. */
	void selectAll() {
		selectedWells.clear();
		for (ArgolightPattern p : definition.getPatterns())
			selectedWells.add(p.getWell());
		repaint();
		fireSelectionChanged();
	}

	/** Clears the selection. */
	void selectNone() {
		selectedWells.clear();
		repaint();
		fireSelectionChanged();
	}

	/** @return selected wells. */
	Set<Well> getSelectedWells() {
		return new HashSet<>(selectedWells);
	}

	/**
	 * Selected wells together with the group assigned to their pattern in the
	 * definition (GROUP0..GROUPn).
	 * 
	 * @return selected wells with groups.
	 */
	Set<WellWithGroup> getSelectedWellWithGroups() {
		LinkedHashSet<WellWithGroup> result = new LinkedHashSet<>();
		for (ArgolightPattern p : definition.getPatterns()) {
			if (selectedWells.contains(p.getWell()))
				result.add(new WellWithGroup(p.getWell(), groupForId(p.getGroupId())));
		}
		return result;
	}

	/**
	 * Sets the selection from a collection of wells (groups ignored; the group of
	 * each pattern is fixed by the definition).
	 * 
	 * @param wells wells to select.
	 */
	void setSelectedWells(Collection<Well> wells) {
		selectedWells.clear();
		if (wells != null) {
			for (ArgolightPattern p : definition.getPatterns()) {
				if (wells.contains(p.getWell()))
					selectedWells.add(p.getWell());
			}
		}
		repaint();
		fireSelectionChanged();
	}

	/**
	 * Sets the selection from wells-with-groups (only the wells are used).
	 * 
	 * @param wells wells-with-groups to select.
	 */
	void setSelectedWellWithGroups(Collection<WellWithGroup> wells) {
		HashSet<Well> plain = new HashSet<>();
		if (wells != null) {
			for (WellWithGroup wg : wells)
				plain.add(wg.getWell());
		}
		setSelectedWells(plain);
	}

	static WellWithGroup.WellGroup groupForId(int id) {
		WellWithGroup.WellGroup[] all = WellWithGroup.WellGroup.values();
		if (id < 0)
			id = 0;
		if (id >= all.length)
			id = all.length - 1;
		return all[id];
	}

	/**
	 * @param listener listener notified whenever the selection changes.
	 */
	void addSelectionChangedListener(ActionListener listener) {
		selectionListeners.add(listener);
	}

	/**
	 * @param listener listener to remove.
	 */
	void removeSelectionChangedListener(ActionListener listener) {
		selectionListeners.remove(listener);
	}

	private void fireSelectionChanged() {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "selection-changed");
		for (ActionListener l : new ArrayList<>(selectionListeners))
			l.actionPerformed(e);
	}

	private void drawBadge(Graphics2D g2, String text, int bx, int by, int marker, boolean selected) {
		int d = Math.max(8, marker / 4);
		g2.setColor(selected ? new Color(197, 225, 165) : new Color(120, 120, 120));
		g2.fillOval(bx, by, d, d);
		g2.setColor(selected ? SELECTED_BORDER : Color.WHITE);
		Font f = getFont().deriveFont(Font.BOLD, Math.max(7f, d * 0.7f));
		Font old = g2.getFont();
		g2.setFont(f);
		int tw = g2.getFontMetrics().stringWidth(text);
		g2.drawString(text, bx + (d - tw) / 2, by + d - Math.max(2, d / 5));
		g2.setFont(old);
	}

	private ArgolightPattern patternAt(int px, int py) {
		for (Map.Entry<ArgolightPattern, int[]> entry : markerBounds.entrySet()) {
			int[] b = entry.getValue();
			if (px >= b[0] && px <= b[0] + b[2] && py >= b[1] && py <= b[1] + b[3])
				return entry.getKey();
		}
		return null;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		markerBounds.clear();
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		java.util.List<ArgolightPattern> patterns = definition.getPatterns();
		if (patterns.isEmpty()) {
			g2.setColor(LABEL_COLOR);
			g2.drawString("No patterns defined.", 20, 20);
			g2.dispose();
			return;
		}

		// Data bounding box in relative um (screen space: x right, y down).
		double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		for (ArgolightPattern p : patterns) {
			double sx = p.getRelativeXUm();
			double sy = -p.getRelativeYUm();
			minX = Math.min(minX, sx);
			maxX = Math.max(maxX, sx);
			minY = Math.min(minY, sy);
			maxY = Math.max(maxY, sy);
		}
		double dataW = Math.max(1.0, maxX - minX);
		double dataH = Math.max(1.0, maxY - minY);

		int availW = getWidth() - 2 * MARGIN;
		int availH = getHeight() - 2 * MARGIN;
		double scale = Math.min(availW / dataW, availH / dataH);
		double drawW = dataW * scale;
		double drawH = dataH * scale;
		double offX = MARGIN + (availW - drawW) / 2.0 - minX * scale;
		double offY = MARGIN + (availH - drawH) / 2.0 - minY * scale;

		int baseMarker = (int) Math.max(MARKER_MIN, Math.min(MARKER_MAX, scale * 200));

		// Reference axes through the central cross (relative 0,0).
		int originX = (int) Math.round(offX + 0 * scale);
		int originY = (int) Math.round(offY + 0 * scale);
		g2.setColor(new Color(225, 225, 225));
		g2.drawLine(MARGIN / 2, originY, getWidth() - MARGIN / 2, originY);
		g2.drawLine(originX, MARGIN / 2, originX, getHeight() - MARGIN / 2);

		Font labelFont = getFont().deriveFont(Font.PLAIN, Math.max(9f, baseMarker / 4f));
		g2.setFont(labelFont);

		for (ArgolightPattern p : patterns) {
			double sx = p.getRelativeXUm();
			double sy = -p.getRelativeYUm();
			int cx = (int) Math.round(offX + sx * scale);
			int cy = (int) Math.round(offY + sy * scale);
			int marker = (int) Math.max(MARKER_MIN * 0.6, baseMarker * p.getMarkerScale());
			int x = cx - marker / 2;
			int y = cy - marker / 2;
			boolean selected = selectedWells.contains(p.getWell());

			g2.setColor(selected ? SELECTED_FILL : UNSELECTED_FILL);
			g2.fillRoundRect(x, y, marker, marker, marker / 4, marker / 4);
			g2.setColor(selected ? SELECTED_BORDER : UNSELECTED_BORDER);
			g2.drawRoundRect(x, y, marker, marker, marker / 4, marker / 4);

			ArgolightPatternIcons.paintGlyph(g2, p.getIconType(), x, y, marker,
					selected ? GLYPH_ON : GLYPH_OFF, 0.66 * p.getIconScale(), p.getIconDetail());

			// z-stack / scan badges.
			int badgeY = y;
			if (p.isZStack()) {
				drawBadge(g2, "z", x + marker - marker / 4, badgeY, marker, selected);
			}
			if (p.isScan()) {
				drawBadge(g2, "\u25a6", x, badgeY, marker, selected); // squared square = tiling
			}

			// Label (well id) under the marker.
			String label = Well.getWellName(p.getWellRow(), p.getWellCol());
			int lw = g2.getFontMetrics().stringWidth(label);
			g2.setColor(LABEL_COLOR);
			g2.drawString(label, cx - lw / 2, y + marker + g2.getFontMetrics().getAscent());

			markerBounds.put(p, new int[] { x, y, marker, marker });
		}

		// Legend for the origin.
		g2.setColor(new Color(150, 150, 150));
		g2.setFont(getFont().deriveFont(Font.ITALIC, 10f));
		g2.drawString("(0;0) central cross \u2014 +X right, +Y up", MARGIN / 2, getHeight() - 8);

		g2.dispose();
	}
}
