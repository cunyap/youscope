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

package org.youscope.plugin.fairexport;

import java.util.ArrayList;
import java.util.List;

import org.youscope.plugin.fairexport.model.ExperimentMetadata;

/**
 * Builds the JSON payload handed to Python's
 * {@code useq.MDASequence(**payload)} for the
 * acquisition-plan export.
 * <p>
 * This class is deliberately Bio-Formats- and Python-free: it only reads the
 * plugin's own
 * {@link ExperimentMetadata} model and emits a JSON object as a string. The
 * actual schema guarantee
 * comes from Python. {@code UseqWriter} feeds this payload to
 * {@code useq_export.py}, which constructs
 * a real {@code MDASequence} and serialises it with {@code model_dump_json()}
 * so this builder only has
 * to produce the field names {@code useq} expects and leave out anything it
 * cannot express.
 * </p>
 * <p>
 * Mapping (U3):
 * </p>
 * <ul>
 * <li>channels &rarr; {@code channels:[{config, group, exposure}]} (exposure in
 * ms, as useq expects);</li>
 * <li>time points &rarr; {@code time_plan:{interval, loops}} when a nominal
 * increment exists, else
 * {@code {loops}} only;</li>
 * <li>Z stack &rarr; {@code z_plan:{range, step}} (symmetric range around 0)
 * when a uniform step is
 * known;</li>
 * <li>plate wells / positions &rarr; {@code stage_positions:[{name,x,y,z}]}; a
 * plate additionally
 * carries a {@code grid_plan} note in {@code metadata};</li>
 * <li>everything that cannot be represented is recorded as a warning (never an
 * error) and omitted.</li>
 * </ul>
 * <p>
 * The JSON is hand-rolled (as with {@link RoCrateWriter}) to avoid a
 * JSON-library dependency; the
 * structure is small and every string is escaped.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
final class UseqPayloadBuilder {
	private final List<String> warnings = new ArrayList<String>();

	/**
	 * @return warnings collected while building (things useq cannot express); never
	 *         null.
	 */
	List<String> getWarnings() {
		return warnings;
	}

	/**
	 * Builds the payload JSON for a measurement.
	 * 
	 * @param metadata Parsed measurement.
	 * @return A JSON object string suitable for
	 *         {@code useq.MDASequence(**payload)}.
	 */
	String build(ExperimentMetadata metadata) {
		StringBuilder json = new StringBuilder();
		json.append("{\n");

		boolean needComma = false;

		// Channels
		List<ExperimentMetadata.Channel> channels = collectChannels(metadata);
		if (!channels.isEmpty()) {
			json.append("  \"channels\": [\n");
			for (int i = 0; i < channels.size(); i++) {
				ExperimentMetadata.Channel channel = channels.get(i);
				json.append("    {");
				String config = channel.name != null ? channel.name : "channel " + i;
				json.append("\"config\": ").append(quote(config));
				if (channel.channelGroup != null)
					json.append(", \"group\": ").append(quote(channel.channelGroup));
				if (channel.exposureMs != null)
					json.append(", \"exposure\": ").append(numberOrNull(channel.exposureMs));
				json.append("}");
				json.append(i < channels.size() - 1 ? ",\n" : "\n");
			}
			json.append("  ]");
			needComma = true;
		} else
			warnings.add("useq: no channels could be determined; the sequence has no channels block.");

		// Time plan
		Integer sizeT = commonSizeT(metadata);
		Double timeIncrementSeconds = commonTimeIncrement(metadata);
		if (sizeT != null && sizeT.intValue() > 1) {
			if (needComma)
				json.append(",\n");
			if (timeIncrementSeconds != null && timeIncrementSeconds.doubleValue() > 0) {
				// useq TIntervalLoops: interval is a duration (seconds), loops the number of
				// time points.
				json.append("  \"time_plan\": {\"interval\": ").append(numberOrNull(timeIncrementSeconds))
						.append(", \"loops\": ").append(sizeT.intValue()).append("}");
			} else {
				// No nominal spacing (period 0 = as fast as possible): loops only.
				json.append("  \"time_plan\": {\"interval\": 0, \"loops\": ").append(sizeT.intValue()).append("}");
				warnings.add("useq: the measurement ran without a fixed period, so the time plan carries the "
						+ "number of time points but an interval of 0 (as fast as possible).");
			}
			needComma = true;
		}

		// Z plan
		ZPlan zPlan = commonZPlan(metadata);
		if (zPlan != null) {
			if (needComma)
				json.append(",\n");
			// useq ZRangeAround: a symmetric range (total extent) with a step, centred on
			// the current Z.
			json.append("  \"z_plan\": {\"range\": ").append(numberOrNull(zPlan.range))
					.append(", \"step\": ").append(numberOrNull(zPlan.step)).append("}");
			needComma = true;
		}

		// Stage positions
		List<String> positions = collectStagePositions(metadata);
		if (!positions.isEmpty()) {
			if (needComma)
				json.append(",\n");
			json.append("  \"stage_positions\": [\n");
			for (int i = 0; i < positions.size(); i++) {
				json.append("    ").append(positions.get(i));
				json.append(i < positions.size() - 1 ? ",\n" : "\n");
			}
			json.append("  ]");
			needComma = true;
		}

		// Metadata (carries feedback/analysis config and a plate/grid note)
		StringBuilder meta = new StringBuilder();
		meta.append("{");
		boolean metaNeedComma = false;
		if (metadata != null && metadata.name != null) {
			meta.append("\"experiment\": ").append(quote(metadata.name));
			metaNeedComma = true;
		}
		if (metadata != null && metadata.plate != null) {
			if (metaNeedComma)
				meta.append(", ");
			meta.append("\"plate\": ").append(quote(metadata.plate.name == null ? "plate" : metadata.plate.name));
			meta.append(", \"grid_plan\": \"per-well stage_positions\"");
			metaNeedComma = true;
			// useq has a WellPlatePlan, but it needs the exact plate model
			// (rows/cols/spacing) which we
			// cannot always reconstruct; the wells are already listed as explicit
			// stage_positions, so we
			// note the plate rather than guessing a grid_plan geometry.
			warnings.add("useq: plate wells are exported as explicit stage_positions; the plate geometry is "
					+ "noted in metadata rather than as a useq grid_plan/WellPlatePlan (geometry not reconstructable).");
		}
		String analysis = metadata == null ? null : metadata.getProperty("Analysis pipeline");
		if (analysis != null) {
			if (metaNeedComma)
				meta.append(", ");
			meta.append("\"analysis_pipeline\": ").append(quote(analysis));
			metaNeedComma = true;
		}
		meta.append("}");
		if (metaNeedComma) {
			if (needComma)
				json.append(",\n");
			json.append("  \"metadata\": ").append(meta.toString());
			needComma = true;
		}

		json.append("\n}\n");
		return json.toString();
	}

	// Extraction helpers

	/**
	 * Channels of the first series that has any (channels are consistent across
	 * wells for one protocol),
	 * excluding derived/analysis outputs.
	 * <p>
	 * useq describes the <em>acquisition</em> sequence, so only truly acquired
	 * channels belong in
	 * {@code channels}. In a cell-detection / feedback measurement the OME image
	 * also carries computed
	 * outputs (e.g. a segmentation mask and a control image) as extra "channels";
	 * those are results of the
	 * analysis protocol, not acquired planes, and are filtered out here (with a
	 * warning). The heuristic:
	 * a channel with no channel-group and no exposure is treated as a derived
	 * output, since every acquired
	 * channel in YouScope is defined by a channel-group. The analysis protocol
	 * itself (segmenter /
	 * quantifier / tracker) is intentionally not represented in useq but it is
	 * carried in {@code metadata}
	 * instead because useq's scope is the acquisition sequence.
	 * </p>
	 */
	private List<ExperimentMetadata.Channel> collectChannels(ExperimentMetadata metadata) {
		List<ExperimentMetadata.Channel> result = new ArrayList<ExperimentMetadata.Channel>();
		if (metadata == null)
			return result;
		ExperimentMetadata.ImageSeries source = firstSeries(metadata);
		if (source == null)
			return result;
		int dropped = 0;
		for (ExperimentMetadata.Channel channel : source.channels) {
			boolean acquired = (channel.channelGroup != null && !channel.channelGroup.trim().isEmpty())
					|| channel.exposureMs != null;
			if (acquired)
				result.add(channel);
			else
				dropped++;
		}
		if (dropped > 0)
			warnings.add("useq: " + dropped + " derived/analysis channel(s) (e.g. segmentation or control "
					+ "images) were excluded from the acquisition channels; they are outputs of the analysis "
					+ "protocol, not acquired planes. The protocol itself is noted in metadata.");
		return result;
	}

	/**
	 * First image series, from the plate's first acquired well if this is a plate,
	 * else series[0].
	 */
	private static ExperimentMetadata.ImageSeries firstSeries(ExperimentMetadata metadata) {
		if (metadata == null)
			return null;
		if (!metadata.series.isEmpty())
			return metadata.series.get(0);
		if (metadata.plate != null) {
			for (ExperimentMetadata.PlateWell well : metadata.plate.wells)
				if (well.image != null)
					return well.image;
		}
		return null;
	}

	/** All image series, whether flat or under a plate. */
	private static List<ExperimentMetadata.ImageSeries> allSeries(ExperimentMetadata metadata) {
		List<ExperimentMetadata.ImageSeries> all = new ArrayList<ExperimentMetadata.ImageSeries>();
		if (metadata == null)
			return all;
		all.addAll(metadata.series);
		if (metadata.plate != null) {
			for (ExperimentMetadata.PlateWell well : metadata.plate.wells)
				if (well.image != null)
					all.add(well.image);
		}
		return all;
	}

	private Integer commonSizeT(ExperimentMetadata metadata) {
		Integer value = null;
		for (ExperimentMetadata.ImageSeries series : allSeries(metadata)) {
			if (series.sizeT == null)
				continue;
			if (value == null)
				value = series.sizeT;
			else if (!value.equals(series.sizeT)) {
				warnings.add("useq: time-point count differs between images; using " + value + " for the time plan.");
				break;
			}
		}
		return value;
	}

	private Double commonTimeIncrement(ExperimentMetadata metadata) {
		for (ExperimentMetadata.ImageSeries series : allSeries(metadata))
			if (series.timeIncrementSeconds != null)
				return series.timeIncrementSeconds;
		return null;
	}

	/**
	 * Z plan (symmetric range + uniform step) derived from a series' Z size and
	 * physical spacing.
	 */
	private ZPlan commonZPlan(ExperimentMetadata metadata) {
		for (ExperimentMetadata.ImageSeries series : allSeries(metadata)) {
			if (series.sizeZ == null || series.sizeZ.intValue() <= 1)
				continue;
			if (series.physicalSizeZMicrons == null) {
				warnings.add("useq: a Z stack is present but its step size is unknown, so no z_plan is written.");
				return null;
			}
			ZPlan plan = new ZPlan();
			plan.step = series.physicalSizeZMicrons.doubleValue();
			plan.range = plan.step * (series.sizeZ.intValue() - 1);
			return plan;
		}
		return null;
	}

	/**
	 * Stage positions from plate wells (named by well path) or from the flat
	 * series' first plane XY(Z).
	 */
	private List<String> collectStagePositions(ExperimentMetadata metadata) {
		List<String> positions = new ArrayList<String>();
		if (metadata == null)
			return positions;

		if (metadata.plate != null) {
			for (ExperimentMetadata.PlateWell well : metadata.plate.wells) {
				if (well.image == null)
					continue;
				double[] xyz = firstPlaneXYZ(well.image);
				String name = well.path != null ? well.path.replace("/", "") : ("well" + positions.size());
				positions.add(positionJson(name, xyz));
			}
			if (positions.isEmpty())
				warnings.add("useq: the plate has no acquired wells with images; no stage_positions written.");
			return positions;
		}

		for (ExperimentMetadata.ImageSeries series : metadata.series) {
			double[] xyz = firstPlaneXYZ(series);
			if (xyz == null)
				continue;
			positions.add(positionJson(series.name, xyz));
		}
		if (positions.isEmpty() && !metadata.series.isEmpty())
			warnings.add("useq: no stage coordinates were recorded, so stage_positions is omitted.");
		return positions;
	}

	/** First plane's XY(Z) in microns, or null if no plane carries a position. */
	private static double[] firstPlaneXYZ(ExperimentMetadata.ImageSeries series) {
		if (series == null)
			return null;
		for (ExperimentMetadata.Plane plane : series.planes) {
			if (plane.positionXMicrons != null && plane.positionYMicrons != null) {
				return new double[] {
						plane.positionXMicrons.doubleValue(),
						plane.positionYMicrons.doubleValue(),
						plane.positionZMicrons == null ? Double.NaN : plane.positionZMicrons.doubleValue() };
			}
		}
		return null;
	}

	private static String positionJson(String name, double[] xyz) {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		if (name != null)
			builder.append("\"name\": ").append(quote(name)).append(", ");
		if (xyz != null) {
			builder.append("\"x\": ").append(numberOrNull(Double.valueOf(xyz[0])));
			builder.append(", \"y\": ").append(numberOrNull(Double.valueOf(xyz[1])));
			if (!Double.isNaN(xyz[2]))
				builder.append(", \"z\": ").append(numberOrNull(Double.valueOf(xyz[2])));
		} else {
			// A named position with no coordinates is still valid for useq
			// (relative/current position).
			builder.append("\"x\": 0, \"y\": 0");
		}
		builder.append("}");
		return builder.toString();
	}

	/** Small value holder for a Z plan. */
	private static final class ZPlan {
		double range;
		double step;
	}

	// JSON helpers

	/**
	 * Formats a finite number without a trailing ".0" for integers; emits null for
	 * null/NaN/inf.
	 */
	private static String numberOrNull(Double value) {
		if (value == null || value.isNaN() || value.isInfinite())
			return "null";
		double d = value.doubleValue();
		if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15)
			return Long.toString((long) d);
		return Double.toString(d);
	}

	private static String quote(String value) {
		StringBuilder builder = new StringBuilder();
		builder.append('"');
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			switch (character) {
				case '"':
					builder.append("\\\"");
					break;
				case '\\':
					builder.append("\\\\");
					break;
				case '\n':
					builder.append("\\n");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\t':
					builder.append("\\t");
					break;
				default:
					if (character < 0x20)
						builder.append(String.format("\\u%04x", Integer.valueOf(character)));
					else
						builder.append(character);
			}
		}
		builder.append('"');
		return builder.toString();
	}
}
