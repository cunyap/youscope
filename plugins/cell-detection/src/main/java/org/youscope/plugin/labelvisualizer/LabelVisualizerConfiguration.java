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
 *     Andreas P. Cuny - New Python API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.labelvisualizer;

import org.youscope.addon.celldetection.CellVisualizationConfiguration;
import org.youscope.addon.component.ComponentMetadataAdapter;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.configuration.YSConfigAlias;
import org.youscope.common.configuration.YSConfigClassification;
import org.youscope.common.configuration.YSConfigDescription;
import org.youscope.common.configuration.YSConfigIcon;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Moritz Lang and Andreas P. Cuny
 *
 */
@XStreamAlias("label-visualizer-configuration")
@YSConfigAlias("Label Visualizer")
@YSConfigClassification("cell visualization")
@YSConfigDescription("This visualizer creates an image in which the areas the cells occupy are painted in different colors, whereas the background is black.\nIf checked, information like the cell area and fluorescence intensities is displayed next to the detected cells.")
@YSConfigIcon("icons/label-viz-icon.png")
public class LabelVisualizerConfiguration extends CellVisualizationConfiguration {

	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = -1111201671110652226L;

	/**
	 * Constructor.
	 */
	public LabelVisualizerConfiguration() {
		// do nothing.
	}

	@YSConfigAlias("display properties next to cells")
	private boolean drawCellInformation = true;

	@YSConfigAlias("draw on top of original image")
	private boolean drawIntoOrgImage = true;

	@YSConfigAlias("show cell/track id")
	private boolean showCellId = true;

	@YSConfigAlias("show area")
	private boolean showArea = true;

	@YSConfigAlias("show mean intensity")
	private boolean showMeanIntensity = true;

	/**
	 * The identifier for this configuration.
	 */
	public static final String CONFIGURATION_ID = "YouScope.LabelVisualizer";

	@Override
	public String getTypeIdentifier() {
		return CONFIGURATION_ID;
	}

	/**
	 * Set to true to draw information about each detected cell next to the cell.
	 * 
	 * @param drawCellInformation True to draw information, false otherwise.
	 */
	public void setDrawCellInformation(boolean drawCellInformation) {
		this.drawCellInformation = drawCellInformation;
	}

	/**
	 * Returns true if information about each detected cell is drawn next to the
	 * cell.
	 * 
	 * @return True if information is drawn.
	 */
	public boolean isDrawCellInformation() {
		return drawCellInformation;
	}

	/**
	 * Sets if the borders of the cells should be drawn into the original image
	 * (true), or the shape of the cell into a new image (false).
	 * 
	 * @param drawIntoOrgImage True to draw into original image.
	 */
	public void setDrawIntoOrgImage(boolean drawIntoOrgImage) {
		this.drawIntoOrgImage = drawIntoOrgImage;
	}

	/**
	 * Returns if the borders of the cells should be drawn into the original image
	 * (true), or the shape of the cell into a new image (false).
	 * 
	 * @return True to draw into original image.
	 */
	public boolean isDrawIntoOrgImage() {
		return drawIntoOrgImage;
	}

	/** @return true to draw each cell's id (track id if tracked) as text. */
	public boolean isShowCellId() {
		return showCellId;
	}

	/** @param showCellId true to draw each cell's id line. */
	public void setShowCellId(boolean showCellId) {
		this.showCellId = showCellId;
	}

	/** @return true to draw each cell's area as text. */
	public boolean isShowArea() {
		return showArea;
	}

	/** @param showArea true to draw each cell's area line. */
	public void setShowArea(boolean showArea) {
		this.showArea = showArea;
	}

	/**
	 * @return true to draw each cell's per-channel mean intensity
	 *         (from the quantifier's {@code cell.intensity.mean.chN} columns).
	 */
	public boolean isShowMeanIntensity() {
		return showMeanIntensity;
	}

	/** @param showMeanIntensity true to draw the mean-intensity line. */
	public void setShowMeanIntensity(boolean showMeanIntensity) {
		this.showMeanIntensity = showMeanIntensity;
	}

	@Override
	public void checkConfiguration() throws ConfigurationException {
		super.checkConfiguration();
		// do nothing.
	}

	@Override
	public String[] getImageSaveNames() {
		return new String[0];
	}

	@Override
	public int getNumberOfImages() {
		return 1;
	}
}
