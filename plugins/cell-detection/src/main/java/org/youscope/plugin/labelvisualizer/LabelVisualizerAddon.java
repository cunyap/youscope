/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - New Python API and implementation
 ******************************************************************************/
/**
 * 
 */
package org.youscope.plugin.labelvisualizer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.youscope.addon.celldetection.CellDetectionResult;
import org.youscope.addon.celldetection.CellDetectionTableColumns;
import org.youscope.addon.celldetection.CellVisualizationAddon;
import org.youscope.addon.celldetection.CellVisualizationException;
import org.youscope.common.PositionInformation;
import org.youscope.common.configuration.ConfigurationException;
import org.youscope.common.image.ImageEvent;
import org.youscope.common.resource.ResourceAdapter;
import org.youscope.common.table.ColumnDefinition;
import org.youscope.common.table.ColumnView;
import org.youscope.common.table.TableDefinition;
import org.youscope.common.table.TableException;
import org.youscope.common.util.ImageConvertException;
import org.youscope.common.util.ImageTools;

/**
 * Visualizes the detected cells by painting the areas of each cell in a
 * different color. Can also draw information about the cell (area,
 * fluorescence) next to the cell.
 * 
 * @author Moritz Lang and Andreas P. Cuny
 *
 */
class LabelVisualizerAddon extends ResourceAdapter<LabelVisualizerConfiguration> implements CellVisualizationAddon {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -2086877835027192450L;
	private static final int TEXT_PIXEL_DIST_HORIZONTAL = 12;
	private static final int TEXT_PIXEL_DIST_VERTICAL = 0;
	private static final float FONT_SIZE = 11f;

	/**
	 * Array of color values (RGB) in which the cells get colored.
	 */
	private final int[][] CELL_COLORS = {
			{ 255, 0, 0 },
			{ 0, 255, 0 },
			{ 0, 0, 255 },
			{ 255, 255, 0 },
			{ 255, 0, 255 },
			{ 0, 255, 255 },
			{ 255, 255, 255 }
	};

	LabelVisualizerAddon(PositionInformation positionInformation, LabelVisualizerConfiguration configuration)
			throws ConfigurationException, RemoteException {
		super(positionInformation, configuration, LabelVisualizerConfiguration.CONFIGURATION_ID,
				LabelVisualizerConfiguration.class, "Label Visualizer");
	}

	@Override
	public ImageEvent<?> visualizeCells(ImageEvent<?> orgImage, CellDetectionResult detectionResult)
			throws CellVisualizationException, RemoteException {
		if (!isInitialized())
			throw new CellVisualizationException("Addon not yet initialized.");
		if (orgImage == null)
			throw new CellVisualizationException("Original microscope image is null.");
		if (detectionResult == null)
			throw new CellVisualizationException("Cell detection result is null.");
		if (detectionResult.getLabelImage() == null)
			throw new CellVisualizationException("Cell detection label image is null.");

		ImageEvent<?> labelImage;
		if (getConfiguration().isDrawIntoOrgImage()) {
			labelImage = drawBorderImage(orgImage, detectionResult.getLabelImage());
		} else {
			labelImage = convertLabelImage(detectionResult.getLabelImage());
		}

		if (getConfiguration().isDrawCellInformation()) {
			BufferedImage image;
			try {
				image = ImageTools.getMicroscopeImage(labelImage);
			} catch (ImageConvertException e) {
				throw new CellVisualizationException("Could not create image object out of cell label data.", e);
			}
			image = paintInformationIntoImage(image, detectionResult, getConfiguration());

			try {
				return ImageTools.toYouScopeImage(image);
			} catch (ImageConvertException e) {
				throw new CellVisualizationException("Could not convert detection image to YouScope image format.", e);
			}
		}
		return labelImage;

	}

	private static BufferedImage paintInformationIntoImage(BufferedImage image, CellDetectionResult detectionResult,
			LabelVisualizerConfiguration cfg) {
		if (detectionResult.getCellTable().getNumRows() <= 0)
			return image;

		ColumnView<Double> xposColumn;
		ColumnView<Double> yposColumn;
		try {
			// find columns of x and y position.
			xposColumn = detectionResult.getCellTable()
					.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_XPOSITION_PX);
			yposColumn = detectionResult.getCellTable()
					.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_YPOSITION_PX);
		} catch (@SuppressWarnings("unused") TableException e) {
			// if x and y position is not saved, just do not display text at all (wouldn't
			// make sense).
			return image;
		}
		ColumnView<Double> areaColumn;
		try {
			areaColumn = detectionResult.getCellTable().getColumnView(CellDetectionTableColumns.TABLE_COLUMN_AREA_PX);
		} catch (@SuppressWarnings("unused") TableException e) {
			// not necessarily needed.
			areaColumn = null;
		}
		// Track id: the standard "track.index" (0-based) if present, else the online
		// tracker's "cell.track.id" (1-based). trackOneBased records which, so the
		// displayed id is correct for both.
		ColumnView<Integer> trackColumn;
		boolean trackOneBased = false;
		try {
			trackColumn = detectionResult.getCellTable()
					.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_CELL_TRACKING_ID);
		} catch (@SuppressWarnings("unused") TableException e) {
			trackColumn = null;
		}
		if (trackColumn == null) {
			try {
				@SuppressWarnings("unchecked")
				ColumnView<Integer> byName = (ColumnView<Integer>) detectionResult.getCellTable()
						.getColumnView("cell.track.id");
				trackColumn = byName;
				trackOneBased = true;
			} catch (@SuppressWarnings("unused") TableException e) {
				trackColumn = null;
			}
		}

		// Mean-intensity columns produced by the quantifier: any
		// "cell.intensity.mean.<suffix>" (suffix is the channel name, or chN if
		// unnamed), kept in table order. If none, fall back to the legacy fixed
		// "cell.fluo" column (older segmenter output).
		List<MeanCol> meanColumns = findMeanIntensityColumns(detectionResult);
		ColumnView<Double> fluorescenceColumn = null;
		if (meanColumns.isEmpty()) {
			try {
				fluorescenceColumn = detectionResult.getCellTable()
						.getColumnView(CellDetectionTableColumns.TABLE_COLUMN_FLUORESCENCE);
			} catch (@SuppressWarnings("unused") TableException e) {
				fluorescenceColumn = null;
			}
		}

		// Which lines are shown (per-field selection, to avoid crowding dense fields).
		boolean showId = cfg.isShowCellId();
		boolean showArea = cfg.isShowArea() && areaColumn != null;
		boolean showMean = cfg.isShowMeanIntensity() && (!meanColumns.isEmpty() || fluorescenceColumn != null);

		// initialize graphics
		Graphics g = image.getGraphics();
		g.setColor(Color.WHITE);
		g.setFont(g.getFont().deriveFont(FONT_SIZE));
		if (g instanceof Graphics2D) {
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
					RenderingHints.VALUE_ANTIALIAS_ON);
		}
		int fontHeight = g.getFontMetrics().getHeight();
		int fontAscent = g.getFontMetrics().getAscent();

		int numLines = (showId ? 1 : 0) + (showArea ? 1 : 0) + (showMean ? 1 : 0);
		if (numLines == 0)
			return image; // nothing selected to draw

		// draw cells
		for (int row = 0; row < xposColumn.getNumRows(); row++) {
			Double xRaw = xposColumn.getValue(row);
			Double yRaw = yposColumn.getValue(row);
			if (xRaw == null || yRaw == null)
				continue;

			double xPosCell = xRaw.doubleValue();
			double yPosCell = yRaw.doubleValue();

			int firstLineX = (int) (xPosCell + TEXT_PIXEL_DIST_HORIZONTAL);
			int firstLineY = (int) (yPosCell + TEXT_PIXEL_DIST_VERTICAL - numLines * fontHeight / 2.0 + fontAscent);
			g.drawLine((int) xPosCell, (int) yPosCell, firstLineX - 2, firstLineY - fontAscent / 2);

			if (showId) {
				Integer trackRaw = trackColumn == null ? null : trackColumn.getValue(row);
				int cellID = trackRaw != null ? trackRaw.intValue() + (trackOneBased ? 0 : 1) : row + 1;
				g.drawString("Cell " + cellID, firstLineX, firstLineY);
				firstLineY += fontHeight;
			}
			if (showArea) {
				Double areaRaw = areaColumn.getValue(row);
				double areaCell = areaRaw != null ? areaRaw.doubleValue() : -1;
				if (areaCell >= 0) {
					g.drawString("Area: " + (int) areaCell, firstLineX, firstLineY);
					firstLineY += fontHeight;
				}
			}
			if (showMean) {
				String meanText;
				if (!meanColumns.isEmpty()) {
					boolean multi = meanColumns.size() > 1;
					StringBuilder sb = new StringBuilder();
					for (int c = 0; c < meanColumns.size(); c++) {
						MeanCol mc = meanColumns.get(c);
						Double v = mc.view.getValue(row);
						if (c > 0)
							sb.append(", ");
						// Prefix with the channel label when it is informative:
						// always for several channels, and for a single named one.
						if (multi || !mc.label.startsWith("ch"))
							sb.append(mc.label).append(":");
						sb.append(v == null ? "-" : formatNum(v.doubleValue()));
					}
					meanText = sb.toString();
				} else {
					Double fluoRaw = fluorescenceColumn.getValue(row);
					meanText = fluoRaw != null ? String.format("F:%.0f%%", fluoRaw.doubleValue() * 100) : "-";
				}
				g.drawString("FluoIntMean: " + meanText, firstLineX, firstLineY);
				firstLineY += fontHeight;
			}
		}

		return image;
	}

	/** A discovered mean-intensity column: its channel label + a view on it. */
	private static final class MeanCol {
		final String label;
		final ColumnView<Double> view;

		MeanCol(String label, ColumnView<Double> view) {
			this.label = label;
			this.view = view;
		}
	}

	/**
	 * Returns the quantifier's per-channel mean-intensity columns
	 * ({@code cell.intensity.mean.<suffix>}, where suffix is the channel name or
	 * {@code chN}), in table order (i.e. channel order), or an empty list if none.
	 */
	private static List<MeanCol> findMeanIntensityColumns(CellDetectionResult detectionResult) {
		List<MeanCol> result = new ArrayList<MeanCol>();
		TableDefinition def = detectionResult.getCellTable().getTableDefinition();
		if (def == null)
			return result;
		final String prefix = "cell.intensity.mean.";
		for (ColumnDefinition<?> col : def.getColumnDefinitions()) {
			String name = col.getColumnName();
			if (name != null && name.startsWith(prefix) && name.length() > prefix.length()) {
				String label = name.substring(prefix.length());
				try {
					@SuppressWarnings("unchecked")
					ColumnView<Double> view = (ColumnView<Double>) detectionResult.getCellTable().getColumnView(name);
					result.add(new MeanCol(label, view));
				} catch (@SuppressWarnings("unused") TableException e) {
					// skip a column that cannot be viewed
				}
			}
		}
		return result;
	}

	/** Compact number formatting: integer when whole, else one decimal. */
	private static String formatNum(double v) {
		if (Math.abs(v - Math.rint(v)) < 0.05)
			return Long.toString(Math.round(v));
		return String.format("%.1f", v);
	}

	private ImageEvent<?> drawBorderImage(ImageEvent<?> orgImage, ImageEvent<?> labelImage)
			throws CellVisualizationException {
		boolean orgImageTurned = orgImage.isSwitchXY() != labelImage.isSwitchXY();

		if (orgImageTurned
				&& (orgImage.getWidth() != labelImage.getHeight() || orgImage.getHeight() != labelImage.getWidth())
				|| !orgImageTurned && (orgImage.getWidth() != labelImage.getWidth()
						|| orgImage.getHeight() != labelImage.getHeight()))
			throw new CellVisualizationException("Original image and label image must have the same dimensions. "
					+ "Original image: " + Integer.toString(orgImage.getWidth()) + " x "
					+ Integer.toString(orgImage.getHeight()) + "; "
					+ "Label image: " + Integer.toString(labelImage.getWidth()) + " x "
					+ Integer.toString(labelImage.getHeight()) + ".");

		BufferedImage orgImageBuffered;
		try {
			orgImageBuffered = ImageTools.getMicroscopeImage(orgImage);
		} catch (ImageConvertException e) {
			throw new CellVisualizationException("Could not create image object out of original image data.", e);
		}

		BufferedImage image = new BufferedImage(orgImageBuffered.getWidth(), orgImageBuffered.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		image.createGraphics().drawImage(orgImageBuffered, 0, 0, null);

		Graphics g = image.getGraphics();
		g.setColor(Color.WHITE);

		int width = image.getWidth();
		int height = image.getHeight();

		int bytesPerPixel = labelImage.getBytesPerPixel();

		// Create integer colors.
		int[] processedColors = new int[CELL_COLORS.length];
		for (int i = 0; i < processedColors.length; i++) {
			processedColors[i] = (255 << 24) | (CELL_COLORS[i][0] << 16) | (CELL_COLORS[i][1] << 8)
					| (CELL_COLORS[i][2]);
		}

		if (bytesPerPixel == 1) {
			byte[] imageData = (byte[]) labelImage.getImageData();
			for (int i = 1; i < width - 1; i++) {
				for (int j = 1; j < height - 1; j++) {
					int idx = j * width + i;
					if (imageData[idx] == 0)
						continue;

					if (imageData[idx - 1] == 0 || imageData[idx + 1] == 0 || imageData[idx + width] == 0
							|| imageData[idx - width] == 0) {
						image.setRGB(i, j, processedColors[(imageData[idx] >= 0 ? imageData[idx] : -imageData[idx])
								% processedColors.length]);
					}
				}
			}

		} else if (bytesPerPixel == 2) {
			short[] imageData = (short[]) labelImage.getImageData();
			for (int i = 1; i < width - 1; i++) {
				for (int j = 1; j < height - 1; j++) {
					int idx = j * width + i;
					if (imageData[idx] == 0)
						continue;

					if (imageData[idx - 1] == 0 || imageData[idx + 1] == 0 || imageData[idx + width] == 0
							|| imageData[idx - width] == 0) {
						image.setRGB(i, j, processedColors[(imageData[idx] >= 0 ? imageData[idx] : -imageData[idx])
								% processedColors.length]);
					}
				}
			}
		} else
			throw new CellVisualizationException(
					"Detection image can only be created for label images with 1 or 2 bytes/pixel. Current label image has "
							+ Integer.toString(bytesPerPixel) + " bytes/pixel.");

		try {
			return ImageTools.toYouScopeImage(image);
		} catch (ImageConvertException e) {
			throw new CellVisualizationException("Could not convert detection image to YouScope image format.", e);
		}
	}

	/**
	 * Converts gray level coloring of original image into cells having different
	 * RGB colors (better separatable).
	 * 
	 * @param labelImage
	 * @return
	 * @throws CellVisualizationException
	 */
	private ImageEvent<int[]> convertLabelImage(ImageEvent<?> labelImage) throws CellVisualizationException {
		int[] labelImageData = new int[labelImage.getWidth() * labelImage.getHeight()];
		int bytesPerPixel = labelImage.getBytesPerPixel();

		// Create integer colors.
		int[] processedColors = new int[CELL_COLORS.length];
		for (int i = 0; i < processedColors.length; i++) {
			processedColors[i] = (255 << 24) | (CELL_COLORS[i][0] << 16) | (CELL_COLORS[i][1] << 8)
					| (CELL_COLORS[i][2]);
		}

		if (bytesPerPixel == 1) {
			byte[] imageData = (byte[]) labelImage.getImageData();
			for (int i = 0; i < imageData.length; i++) {
				if (imageData[i] == 0)
					labelImageData[i] = 0;
				else
					labelImageData[i] = processedColors[(imageData[i] >= 0 ? imageData[i] : -imageData[i])
							% processedColors.length];
			}

		} else if (bytesPerPixel == 2) {
			short[] imageData = (short[]) labelImage.getImageData();
			for (int i = 0; i < imageData.length; i++) {
				if (imageData[i] == 0)
					labelImageData[i] = 0;
				else
					labelImageData[i] = processedColors[(imageData[i] > 0 ? imageData[i] : -imageData[i])
							% processedColors.length];
			}
		} else
			throw new CellVisualizationException(
					"Detection image can only be created for label images with 1 or 2 bytes/pixel. Current label image has "
							+ Integer.toString(bytesPerPixel) + " bytes/pixel.");

		ImageEvent<int[]> result = ImageEvent.createImage(labelImageData, labelImage.getWidth(), labelImage.getHeight(),
				8);
		result.setBands(4);
		result.setCamera("Cell-Detection-Result");
		result.setChannel(labelImage.getChannel());
		result.setChannelGroup(labelImage.getChannelGroup());
		result.setExecutionInformation(labelImage.getExecutionInformation());
		result.setCreationTime(labelImage.getCreationTime());
		result.setCreationRuntime(labelImage.getCreationRuntime());
		result.setPositionInformation(labelImage.getPositionInformation());
		result.setSwitchXY(labelImage.isSwitchXY());
		result.setTransposeX(labelImage.isTransposeX());
		result.setTransposeY(labelImage.isTransposeY());
		result.setPositionInformation(labelImage.getPositionInformation());

		return result;
	}

}
