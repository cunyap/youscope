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
/**
 * 
 */
package org.youscope.plugin.cellprofilerquantifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The canonical CellProfiler feature names this quantifier requests, per module
 * family. The Java side owns this list (rather than discovering it from
 * CellProfiler at run time) so the table columns declared at configuration time
 * are exactly those produced at run time, independent of the installed
 * CellProfiler version: the backend returns a value for each requested feature
 * (NaN if that CellProfiler version did not produce it).
 *
 * Feature names are the standard CellProfiler measurement names, without the
 * {@code cell.cpro.} display prefix the backend adds. Per-channel features get the
 * channel suffix appended by CellProfiler as {@code _<channel>}.
 */
final class CellProfilerFeatures
{
	/** Display prefix for CellProfiler feature columns ("cpro"). Must match the prefix the backend adds. */
	static final String PREFIX = "cell.cpro.";

	private CellProfilerFeatures()
	{
	}

	/** MeasureObjectSizeShape: Object-only, no channel. Stable across versions. */
	static final String[] SHAPE = new String[]
	{
		"AreaShape_Area", "AreaShape_Perimeter", "AreaShape_FormFactor",
		"AreaShape_Eccentricity", "AreaShape_Solidity", "AreaShape_Extent",
		"AreaShape_Compactness", "AreaShape_MajorAxisLength",
		"AreaShape_MinorAxisLength", "AreaShape_Orientation",
		"AreaShape_MaximumRadius", "AreaShape_MeanRadius",
		"AreaShape_EquivalentDiameter", "AreaShape_EulerNumber"
	};

	/** MeasureObjectIntensity: One set per channel (suffix appended). */
	static final String[] INTENSITY = new String[]
	{
		"Intensity_IntegratedIntensity", "Intensity_MeanIntensity",
		"Intensity_StdIntensity", "Intensity_MinIntensity",
		"Intensity_MaxIntensity", "Intensity_MedianIntensity",
		"Intensity_MADIntensity", "Intensity_LowerQuartileIntensity",
		"Intensity_UpperQuartileIntensity", "Intensity_IntegratedIntensityEdge",
		"Intensity_MeanIntensityEdge", "Intensity_MassDisplacement"
	};

	/**
	 * MeasureTexture: per channel, per scale, per direction. CellProfiler emits
	 * these as {@code Texture_<feature>_<channel>_<scale>_<angle>}. The exact set
	 * is version- and scale-dependent, so texture is requested at a single default
	 * scale (3) and the common Haralick features; any name a given CellProfiler
	 * version does not emit simply comes back NaN.
	 */
	static final String[] TEXTURE = new String[]
	{
		"Texture_AngularSecondMoment", "Texture_Contrast", "Texture_Correlation",
		"Texture_Variance", "Texture_InverseDifferenceMoment", "Texture_SumAverage",
		"Texture_SumVariance", "Texture_SumEntropy", "Texture_Entropy"
	};

	static final int DEFAULT_TEXTURE_SCALE = 3;

	/**
	 * Builds the full ordered feature-name list for the enabled modules and channels
	 * (backend names, no prefix). Per-channel features get {@code _<suffix>}
	 * appended; texture also appends {@code _<scale>_00} (0-degree direction) as
	 * the representative direction.
	 */
	static List<String> requestedFeatures(boolean shape, boolean intensity, boolean texture,
			String[] channelSuffixes)
	{
		List<String> out = new ArrayList<String>();
		if(shape)
		{
			for(String f : SHAPE)
				out.add(f);
		}
		if(intensity && channelSuffixes != null)
		{
			for(String ch : channelSuffixes)
				for(String f : INTENSITY)
					out.add(f + "_" + ch);
		}
		if(texture && channelSuffixes != null)
		{
			// Request texture without a scale/angle suffix; the backend binds each
			// to CellProfiler's actual suffixed feature (e.g. _3_256), which is
			// version-dependent. The table column stays the clean, predictable
			// cell.cpro.Texture_<feature>_<channel>.
			for(String ch : channelSuffixes)
				for(String f : TEXTURE)
					out.add(f + "_" + ch);
		}
		return out;
	}

	/** The backend module kinds corresponding to the enabled families. */
	static List<String> moduleKinds(boolean shape, boolean intensity, boolean texture)
	{
		List<String> kinds = new ArrayList<String>();
		if(shape)
			kinds.add("size_shape");
		if(intensity)
			kinds.add("intensity");
		if(texture)
			kinds.add("texture");
		return kinds;
	}
}
