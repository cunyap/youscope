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
 *     Andreas P. Cuny - extend to N=100 well groups with unique colors
 ******************************************************************************/
package org.youscope.common.microplate;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;

import org.youscope.common.Well;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * A tuple consisting of a well, and a group. This follows the logic that in
 * certain conditions each well belongs to
 * a designated well groups. For example, all wells of a microplate belonging to
 * the same group might perform
 * the same imaging protocol, but wells of different groups might perform
 * different protocols.
 * 
 * @author Moritz Lang
 *
 */
@XStreamAlias("well-with-group")
public class WellWithGroup implements Serializable {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 9217362001562054051L;

	/**
	 * Each selected well belongs to a group, which could e.g. be subsets of wells
	 * having the same
	 * imaging protocol.
	 * 
	 * @author Moritz Lang
	 *
	 */
	public enum WellGroup {
		// Well groups GROUP0 .. GROUP99 (N = 100).
		// The number of constants defined here is the number of well groups. To change
		// N,
		// simply add or remove GROUPn constants below; nothing else needs to be
		// touched.
		// Colors are generated automatically (see getColor()), so there is no
		// per-constant
		// color to maintain. GROUP0 .. GROUP6 are preserved so that measurements saved
		// with
		// earlier versions (which persist the group by its enum constant name via
		// XStream)
		// still load correctly.
		GROUP0, GROUP1, GROUP2, GROUP3, GROUP4, GROUP5, GROUP6, GROUP7, GROUP8, GROUP9,
		GROUP10, GROUP11, GROUP12, GROUP13, GROUP14, GROUP15, GROUP16, GROUP17, GROUP18, GROUP19,
		GROUP20, GROUP21, GROUP22, GROUP23, GROUP24, GROUP25, GROUP26, GROUP27, GROUP28, GROUP29,
		GROUP30, GROUP31, GROUP32, GROUP33, GROUP34, GROUP35, GROUP36, GROUP37, GROUP38, GROUP39,
		GROUP40, GROUP41, GROUP42, GROUP43, GROUP44, GROUP45, GROUP46, GROUP47, GROUP48, GROUP49,
		GROUP50, GROUP51, GROUP52, GROUP53, GROUP54, GROUP55, GROUP56, GROUP57, GROUP58, GROUP59,
		GROUP60, GROUP61, GROUP62, GROUP63, GROUP64, GROUP65, GROUP66, GROUP67, GROUP68, GROUP69,
		GROUP70, GROUP71, GROUP72, GROUP73, GROUP74, GROUP75, GROUP76, GROUP77, GROUP78, GROUP79,
		GROUP80, GROUP81, GROUP82, GROUP83, GROUP84, GROUP85, GROUP86, GROUP87, GROUP88, GROUP89,
		GROUP90, GROUP91, GROUP92, GROUP93, GROUP94, GROUP95, GROUP96, GROUP97, GROUP98, GROUP99;

		/**
		 * Saturation used for all generated group colors (HSB), in the range [0, 1].
		 */
		private static final float COLOR_SATURATION = 0.75f;
		/**
		 * Brightness used for all generated group colors (HSB), in the range [0, 1].
		 */
		private static final float COLOR_BRIGHTNESS = 0.95f;
		/**
		 * Lazily generated and cached color of this group.
		 */
		private Color color = null;

		/**
		 * Returns the color associated to this well group. Colors are generated
		 * automatically such
		 * that they are spread evenly around the color wheel and stay distinguishable
		 * for any number
		 * of groups.
		 * 
		 * @return Color of the well group.
		 */
		public Color getColor() {
			if (color == null)
				color = generateColor(ordinal(), values().length);
			return color;
		}

		/**
		 * Generates a distinguishable color for the group with the given zero-based
		 * index, given the
		 * total number of groups. Hues are spread evenly around the color wheel; the
		 * assignment order
		 * uses a stride that is coprime to {@code count}, so that groups with
		 * consecutive IDs receive
		 * hues that are far apart and are therefore easy to tell apart. Setting the
		 * stride to 1 (see
		 * {@link #coprimeStride(int)}) would instead assign the same evenly spaced hues
		 * in plain
		 * ascending order.
		 * 
		 * @param index Zero based index of the group.
		 * @param count Total number of groups.
		 * @return Generated color for the group.
		 */
		private static Color generateColor(int index, int count) {
			if (count <= 1)
				return Color.getHSBColor(0f, COLOR_SATURATION, COLOR_BRIGHTNESS);
			int stride = coprimeStride(count);
			int slot = (int) (((long) index * stride) % count);
			float hue = (float) slot / (float) count;
			return Color.getHSBColor(hue, COLOR_SATURATION, COLOR_BRIGHTNESS);
		}

		/**
		 * Returns a hue stride close to {@code count} divided by the golden ratio that
		 * is coprime to
		 * {@code count}. Because the stride is coprime to the count, mapping index to
		 * (index * stride) mod count is a bijection over all hue slots, so no two
		 * groups are ever
		 * assigned the same color. Falls back to 1, which yields plain ascending hues.
		 * 
		 * @param count Total number of groups.
		 * @return Stride coprime to count.
		 */
		private static int coprimeStride(int count) {
			int start = Math.round(count / 1.61803398875f);
			for (int s = start; s < start + count; s++) {
				int candidate = s % count;
				if (candidate > 1 && gcd(candidate, count) == 1)
					return candidate;
			}
			return 1;
		}

		/**
		 * Greatest common divisor (Euclidean algorithm).
		 * 
		 * @param a First value.
		 * @param b Second value.
		 * @return Greatest common divisor of a and b.
		 */
		private static int gcd(int a, int b) {
			while (b != 0) {
				int t = b;
				b = a % b;
				a = t;
			}
			return a;
		}

		/**
		 * Returns the zero based ID of this group.
		 * 
		 * @return Zero based ID of group.
		 */
		public int getGroupId() {
			return ordinal();
		}

		/**
		 * Returns the name of the group, in the form "Well Group [i]", where [i] is the
		 * ONE based index of the group.
		 * 
		 * @return Canonical name of the well group.
		 */
		public String getName() {
			return "Well Group " + Integer.toString(ordinal() + 1);
		}

		/**
		 * Returns an icon for this group, which is nothing else than an area filled
		 * with the groups color.
		 * 
		 * @return Icon for group
		 */
		public Icon getIcon() {
			return new Icon() {
				private final int WIDTH = 12;
				private final int HEIGHT = 12;

				@Override
				public void paintIcon(Component c, Graphics g, int x, int y) {
					g.setColor(getColor());
					g.fillRect(x, y, WIDTH - 1, HEIGHT - 1);
					g.setColor(Color.BLACK);
					g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
				}

				@Override
				public int getIconWidth() {
					return WIDTH;
				}

				@Override
				public int getIconHeight() {
					return HEIGHT;
				}
			};
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param well  The well.
	 * @param group The group of the well.
	 */
	public WellWithGroup(Well well, WellGroup group) {
		this.well = well;
		this.group = group;
	}

	@XStreamAlias("well")
	private final Well well;
	@XStreamAlias("group")
	private final WellGroup group;

	/**
	 * Returns the well belonging to this tuple.
	 * 
	 * @return Well of tuple.
	 */
	public Well getWell() {
		return well;
	}

	/**
	 * Returns the group to which this well belongs.
	 * 
	 * @return group of the well.
	 */
	public WellGroup getGroup() {
		return group;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((well == null) ? 0 : well.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WellWithGroup other = (WellWithGroup) obj;
		if (group != other.group)
			return false;
		if (well == null) {
			if (other.well != null)
				return false;
		} else if (!well.equals(other.well))
			return false;
		return true;
	}
}
