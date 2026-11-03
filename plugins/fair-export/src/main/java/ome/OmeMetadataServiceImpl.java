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
package org.youscope.plugin.fairexport.ome;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.youscope.plugin.fairexport.model.ExperimentMetadata;

import loci.formats.FormatTools;
import loci.formats.ImageReader;
import loci.formats.meta.IMetadata;
import loci.formats.out.OMETiffWriter;

/**
 * The only class that touches Bio-Formats, and therefore the only class loaded
 * child-first through
 * {@code BioFormatsClassLoader}. It wraps {@link OmeMetadataBuilder}; keeping
 * it thin means the sealed
 * JAI packages are reachable only from inside the isolated loader, never from
 * the classpath the server
 * scans at startup.
 * <p>
 * Loaded reflectively by name (see {@code OmeServiceLoader}); there must be no
 * static reference to this
 * class from anything the server loads.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
public class OmeMetadataServiceImpl implements OmeMetadataService {
	/**
	 * Constructor. Public and no-argument so it can be instantiated reflectively.
	 */
	public OmeMetadataServiceImpl() {
		// nothing
	}

	@Override
	public String buildOmeXml(ExperimentMetadata metadata, List<String> warnings) throws Exception {
		OmeMetadataBuilder builder = new OmeMetadataBuilder();
		IMetadata store = builder.build(metadata);
		if (warnings != null)
			warnings.addAll(builder.getWarnings());
		return OmeMetadataBuilder.toXML(store);
	}

	@Override
	public List<String> writeOmeTiff(ExperimentMetadata metadata, File measurementFolder, File outputFolder,
			boolean perChannel, List<String> warnings) throws Exception {
		if (perChannel)
			return writeOmeTiffPerChannel(metadata, measurementFolder, outputFolder, warnings);

		List<String> written = new ArrayList<String>();
		// Build the full metadata once; each image is one OME series, written to its
		// own file so that a
		// single unreadable image never corrupts a whole multi-series file.
		OmeMetadataBuilder builder = new OmeMetadataBuilder();
		IMetadata store = builder.build(metadata);
		if (warnings != null)
			warnings.addAll(builder.getWarnings());

		for (int imageIndex = 0; imageIndex < metadata.series.size(); imageIndex++) {
			ExperimentMetadata.ImageSeries series = metadata.series.get(imageIndex);
			try {
				String fileName = toSafeFileName(series.name) + ".ome.tiff";
				File target = new File(outputFolder, fileName);
				writeOneSeries(store, imageIndex, series, measurementFolder, target);
				written.add(fileName);
			} catch (Throwable e) {
				String message = e.getMessage();
				if (warnings != null)
					warnings.add("Could not write OME-TIFF for image \"" + series.name + "\": "
							+ (message == null ? e.toString() : message));
			}
		}
		return written;
	}

	/**
	 * Writes one single-channel OME-TIFF per channel per image/well. Needed by
	 * tools such as Argolight
	 * analysis that expect a single channel (single image or z-stack), not a
	 * multi-channel plate. Each
	 * output is a self-contained OME-TIFF built from a one-channel copy of the
	 * series.
	 */
	private List<String> writeOmeTiffPerChannel(ExperimentMetadata metadata, File measurementFolder,
			File outputFolder, List<String> warnings) throws Exception {
		List<String> written = new ArrayList<String>();
		for (ExperimentMetadata.ImageSeries series : metadata.series) {
			int channelCount = series.channels.isEmpty() ? (series.sizeC == null ? 1 : series.sizeC.intValue())
					: series.channels.size();
			for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
				String channelName = channelIndex < series.channels.size()
						&& series.channels.get(channelIndex).name != null
								? series.channels.get(channelIndex).name
								: ("channel_" + channelIndex);
				try {
					// A one-channel sub-model of this series, with only that channel's planes
					// remapped
					// to channel index 0. No fabrication: planes the channel never acquired are
					// simply
					// absent (and zero-filled by the dense writer within that single channel's
					// z-range).
					ExperimentMetadata single = singleChannelModel(metadata, series, channelIndex);
					OmeMetadataBuilder builder = new OmeMetadataBuilder();
					IMetadata store = builder.build(single);
					if (warnings != null) {
						// De-duplicate: the same structural note would otherwise repeat once per
						// channel.
						for (String warning : builder.getWarnings())
							if (!warnings.contains(warning))
								warnings.add(warning);
					}

					String fileName = toSafeFileName(series.name) + "_" + toSafeFileName(channelName) + ".ome.tiff";
					File target = new File(outputFolder, fileName);
					writeOneSeries(store, 0, single.series.get(0), measurementFolder, target);
					written.add(fileName);
				} catch (Throwable e) {
					String message = e.getMessage();
					if (warnings != null)
						warnings.add("Could not write OME-TIFF for image \"" + series.name + "\" channel \""
								+ channelName + "\": " + (message == null ? e.toString() : message));
				}
			}
		}
		return written;
	}

	/**
	 * Builds a one-image, one-channel ExperimentMetadata from a single channel of a
	 * series. Planes of
	 * that channel are copied with their channel index reset to 0; other channels
	 * are dropped.
	 */
	private ExperimentMetadata singleChannelModel(ExperimentMetadata source, ExperimentMetadata.ImageSeries series,
			int channelIndex) {
		ExperimentMetadata result = new ExperimentMetadata();
		// Copy the study-level and hardware metadata so a per-channel file is as
		// complete as the
		// combined one: operator, microscope, objective, date, software and all user
		// properties travel
		// with it. Without this the QUAREP check re-runs on an empty shell and reports
		// everything as
		// missing.
		result.name = source.name;
		result.description = source.description;
		result.acquisitionDate = source.acquisitionDate;
		result.softwareName = source.softwareName;
		result.softwareVersion = source.softwareVersion;
		result.measurementFolder = source.measurementFolder;
		result.instrument = source.instrument;
		result.objective = source.objective;
		result.detector = source.detector;
		result.properties.addAll(source.properties);

		ExperimentMetadata.ImageSeries copy = new ExperimentMetadata.ImageSeries();
		copy.name = series.name;
		copy.description = series.description;
		copy.sizeX = series.sizeX;
		copy.sizeY = series.sizeY;
		copy.sizeT = series.sizeT;
		copy.sizeC = Integer.valueOf(1);
		copy.pixelType = series.pixelType;
		copy.bigEndian = series.bigEndian;
		copy.physicalSizeXMicrons = series.physicalSizeXMicrons;
		copy.physicalSizeYMicrons = series.physicalSizeYMicrons;
		copy.physicalSizeZMicrons = series.physicalSizeZMicrons;
		copy.timeIncrementSeconds = series.timeIncrementSeconds;
		copy.temperatureCelsius = series.temperatureCelsius;

		// Keep only this channel's planes and find its z-extent so the single-channel
		// image is sized to
		// what that channel actually has (a brightfield channel becomes a single-plane
		// image, not a
		// 7-deep one with 6 empty planes).
		int maxZ = 0;
		for (ExperimentMetadata.Plane plane : series.planes) {
			if (plane.channelIndex != channelIndex)
				continue;
			maxZ = Math.max(maxZ, plane.focalPlaneIndex);
		}
		copy.sizeZ = Integer.valueOf(maxZ + 1);

		if (channelIndex < series.channels.size())
			copy.channels.add(series.channels.get(channelIndex));

		for (ExperimentMetadata.Plane plane : series.planes) {
			if (plane.channelIndex != channelIndex)
				continue;
			ExperimentMetadata.Plane planeCopy = new ExperimentMetadata.Plane();
			planeCopy.channelIndex = 0;
			planeCopy.focalPlaneIndex = plane.focalPlaneIndex;
			planeCopy.timePointIndex = plane.timePointIndex;
			planeCopy.deltaTSeconds = plane.deltaTSeconds;
			planeCopy.exposureMs = plane.exposureMs;
			planeCopy.positionXMicrons = plane.positionXMicrons;
			planeCopy.positionYMicrons = plane.positionYMicrons;
			planeCopy.positionZMicrons = plane.positionZMicrons;
			planeCopy.fileName = plane.fileName;
			copy.planes.add(planeCopy);
		}
		result.series.add(copy);
		return result;
	}

	/**
	 * Writes a single image series as an OME-TIFF, copying each plane's pixels from
	 * its source file.
	 */
	private void writeOneSeries(IMetadata store, int imageIndex, ExperimentMetadata.ImageSeries series,
			File measurementFolder, File target) throws Exception {
		if (target.exists() && !target.delete())
			throw new Exception(
					"The output file already exists and could not be replaced: " + target.getAbsolutePath());

		OMETiffWriter writer = new OMETiffWriter();
		try {
			// The writer must be told which single OME image (series) it is writing, so
			// that it embeds
			// only that image's metadata. setSeries selects it within the shared store.
			writer.setMetadataRetrieve(store);
			writer.setId(target.getAbsolutePath());
			writer.setSeries(imageIndex);

			int sizeC = series.sizeC == null ? 1 : series.sizeC.intValue();
			int sizeZ = series.sizeZ == null ? 1 : series.sizeZ.intValue();
			int sizeT = series.sizeT == null ? 1 : series.sizeT.intValue();
			int sizeX = series.sizeX == null ? 1 : series.sizeX.intValue();
			int sizeY = series.sizeY == null ? 1 : series.sizeY.intValue();
			boolean isByte = "uint8".equals(series.pixelType);
			int bytesPerPixel = isByte ? 1 : 2;

			// Index the acquired planes by their (c,z,t) so gaps can be detected. A dense
			// OME-TIFF MUST
			// contain every plane of the c*z*t block, so missing (ragged Z) planes are
			// written as zeros. The OME-XML still lists only real
			// planes.
			java.util.Map<Integer, ExperimentMetadata.Plane> byIndex = new java.util.HashMap<Integer, ExperimentMetadata.Plane>();
			for (ExperimentMetadata.Plane plane : series.planes) {
				int index = planeIndex(series, plane);
				if (index >= 0)
					byIndex.put(Integer.valueOf(index), plane);
			}

			int totalPlanes = sizeC * sizeZ * sizeT;
			byte[] zeros = null;
			for (int index = 0; index < totalPlanes; index++) {
				ExperimentMetadata.Plane plane = byIndex.get(Integer.valueOf(index));
				byte[] pixels = null;
				if (plane != null && plane.fileName != null) {
					File source = new File(measurementFolder, plane.fileName);
					if (source.isFile())
						pixels = readPlaneBytes(source);
				}
				if (pixels == null) {
					if (zeros == null)
						zeros = new byte[sizeX * sizeY * bytesPerPixel];
					pixels = zeros;
				}
				writer.saveBytes(index, pixels);
			}
		} finally {
			try {
				writer.close();
			} catch (@SuppressWarnings("unused") Exception e) {
				/* ignore */ }
		}
	}

	/**
	 * Reads the raw pixel bytes of a single-plane source image, using Bio-Formats
	 * so that the byte
	 * layout and pixel type match what the OME-TIFF writer expects.
	 */
	private byte[] readPlaneBytes(File source) throws Exception {
		ImageReader reader = new ImageReader();
		try {
			reader.setId(source.getAbsolutePath());
			byte[] pixels = reader.openBytes(0);
			// openBytes returns the pixels in the source file's byte order. Everything
			// downstream assumes
			// little-endian: the OME-TIFF store is declared little-endian
			// (OmeMetadataBuilder sets
			// PixelsBigEndian = series.bigEndian, which defaults to false) and the OME-Zarr
			// path interprets
			// its buffers as LITTLE_ENDIAN. A big-endian source (e.g. an 'MM' TIFF) would
			// otherwise be written
			// with byte-swapped 16-bit samples, destroying any quantitative measurement.
			// Normalise big-endian sources to
			// little-endian here.
			int bytesPerPixel = FormatTools.getBytesPerPixel(reader.getPixelType());
			if (bytesPerPixel > 1 && !reader.isLittleEndian())
				swapByteOrderInPlace(pixels, bytesPerPixel);
			return pixels;
		} finally {
			try {
				reader.close();
			} catch (@SuppressWarnings("unused") Exception e) {
				/* ignore */ }
		}
	}

	/**
	 * Swaps the byte order of each multi-byte sample in place, converting between
	 * big- and
	 * little-endian. Single-byte pixel types are left untouched by the caller.
	 */
	private static void swapByteOrderInPlace(byte[] data, int bytesPerPixel) {
		for (int offset = 0; offset + bytesPerPixel <= data.length; offset += bytesPerPixel)
			for (int low = 0, high = bytesPerPixel - 1; low < high; low++, high--) {
				byte temp = data[offset + low];
				data[offset + low] = data[offset + high];
				data[offset + high] = temp;
			}
	}

	/**
	 * Maps a plane's Z/C/T position to the linear plane index of the OME-TIFF, in
	 * the writer's default
	 * dimension order (XYCZT: C fastest, then Z, then T).
	 */
	private int planeIndex(ExperimentMetadata.ImageSeries series, ExperimentMetadata.Plane plane) {
		int sizeC = series.sizeC == null ? 1 : series.sizeC.intValue();
		int sizeZ = series.sizeZ == null ? 1 : series.sizeZ.intValue();
		int c = Math.max(0, plane.channelIndex);
		int z = Math.max(0, plane.focalPlaneIndex);
		int t = Math.max(0, plane.timePointIndex);
		return c + sizeC * (z + sizeZ * t);
	}

	private static String toSafeFileName(String name) {
		if (name == null || name.trim().isEmpty())
			return "image";
		return name.trim().replaceAll("[^a-zA-Z0-9._-]+", "_");
	}

	@Override
	public List<String> writeOmeZarr(ExperimentMetadata metadata, File measurementFolder, File outputFolder,
			int chunkSize, boolean sharding, List<String> warnings) throws Exception {
		List<String> written = new ArrayList<String>();
		// zarr-java is optional. If it is not on the isolated classloader, report once
		// and skip OME-Zarr
		// without touching OME-TIFF / OME-XML.
		try {
			Class.forName("dev.zarr.zarrjava.experimental.ome.v0_5.MultiscaleImage", false,
					getClass().getClassLoader());
			// cdm-core provides ucar.ma2 used by zarr-java's write API. It is published on
			// the Unidata
			// repository, not Maven Central, so a download from the wrong repo leaves an
			// invalid jar and
			// this class fails to load -- which previously surfaced only as a confusing
			// NoClassDefFoundError: ucar/ma2/InvalidRangeException during the write.
			Class.forName("ucar.ma2.InvalidRangeException", false, getClass().getClassLoader());
		} catch (@SuppressWarnings("unused") Throwable e) {
			if (warnings != null)
				warnings.add("OME-Zarr export is not available: the zarr-java backend (or its cdm-core / "
						+ "ucar.ma2 dependency) is not installed correctly. cdm-core must come from the Unidata "
						+ "repository, not Maven Central. Run the provided download script and copy all jars into "
						+ "the lib folder. OME-TIFF and OME-XML were written.");
			return written;
		}
		if (metadata.plate != null) {
			// HCS plate layout: one plate store containing all wells, each well one field
			// (image).
			try {
				String storeName = toSafeFileName(metadata.plate.name) + ".ome.zarr";
				File target = new File(outputFolder, storeName);
				deleteRecursively(target);
				writePlateZarr(metadata.plate, measurementFolder, target, chunkSize, sharding, warnings);
				written.add(storeName);
			} catch (Throwable e) {
				String message = e.getMessage();
				if (warnings != null)
					warnings.add("Could not write OME-Zarr plate \"" + metadata.plate.name + "\": "
							+ (message == null ? e.toString() : message));
			}
			return written;
		}

		// Flat layout: one independent image store per series (non-plate measurements).
		for (int imageIndex = 0; imageIndex < metadata.series.size(); imageIndex++) {
			ExperimentMetadata.ImageSeries series = metadata.series.get(imageIndex);
			try {
				String storeName = toSafeFileName(series.name) + ".ome.zarr";
				File target = new File(outputFolder, storeName);
				deleteRecursively(target);
				dev.zarr.zarrjava.store.FilesystemStore store = new dev.zarr.zarrjava.store.FilesystemStore(
						target.getAbsolutePath());
				writeImageGroup(series, measurementFolder, store.resolve(), target, chunkSize, sharding);
				written.add(storeName);
			} catch (Throwable e) {
				String message = e.getMessage();
				if (warnings != null)
					warnings.add("Could not write OME-Zarr for image \"" + series.name + "\": "
							+ (message == null ? e.toString() : message));
			}
		}
		return written;
	}

	/**
	 * Writes an HCS plate: the plate group metadata, then each acquired well as a
	 * group whose single
	 * field "0" is the well's multiscale image. Plate and well metadata are written
	 * directly per the
	 * NGFF plate/well spec (zarr-java has no plate/well create API); the field
	 * image uses the same
	 * writeImageGroup path as the flat layout.
	 */
	private void writePlateZarr(ExperimentMetadata.Plate plate, File measurementFolder, File target,
			int chunkSize, boolean sharding, List<String> warnings) throws Exception {
		dev.zarr.zarrjava.store.FilesystemStore store = new dev.zarr.zarrjava.store.FilesystemStore(
				target.getAbsolutePath());

		writePlateGroupJson(plate, target);

		for (ExperimentMetadata.PlateWell well : plate.wells) {
			// A well has one field per acquired tile (or one plain field). Write each as
			// field 0,1,2,...
			java.util.List<ExperimentMetadata.ImageSeries> fields = well.fields.isEmpty()
					? java.util.Collections.singletonList(well.image)
					: well.fields;
			int fieldCount = 0;
			for (ExperimentMetadata.ImageSeries field : fields)
				if (field != null)
					fieldCount++;
			if (fieldCount == 0)
				continue;
			try {
				writeWellGroupJson(target, well.path, fieldCount);
				int fieldIndex = 0;
				for (ExperimentMetadata.ImageSeries field : fields) {
					if (field == null)
						continue;
					String fieldPath = well.path + "/" + fieldIndex;
					dev.zarr.zarrjava.store.StoreHandle fieldHandle = store.resolve(splitPath(fieldPath));
					File fieldFolder = new File(target, fieldPath.replace('/', File.separatorChar));
					writeImageGroup(field, measurementFolder, fieldHandle, fieldFolder, chunkSize, sharding);
					fieldIndex++;
				}
			} catch (Throwable e) {
				String message = e.getMessage();
				if (warnings != null)
					warnings.add("Could not write well \"" + well.path + "\": "
							+ (message == null ? e.toString() : message));
			}
		}
	}

	/**
	 * Writes one multiscale image (channels, ragged Z, pyramid) into the given
	 * store handle. Used both
	 * for a flat image store (handle = store root) and for a plate field (handle =
	 * well/0). The NGFF
	 * multiscales/axes metadata comes from zarr-java; missing (c,z) planes are left
	 * at the fill value 0.
	 */
	private void writeImageGroup(ExperimentMetadata.ImageSeries series, File measurementFolder,
			dev.zarr.zarrjava.store.StoreHandle handle, File imageFolder, int chunkSize, boolean sharding)
			throws Exception {
		int sizeX = series.sizeX == null ? 1 : series.sizeX.intValue();
		int sizeY = series.sizeY == null ? 1 : series.sizeY.intValue();
		int sizeZ = series.sizeZ == null ? 1 : series.sizeZ.intValue();
		int sizeC = series.sizeC == null ? 1 : series.sizeC.intValue();
		int sizeT = series.sizeT == null ? 1 : series.sizeT.intValue();

		java.util.List<dev.zarr.zarrjava.experimental.ome.metadata.Axis> axes = new java.util.ArrayList<dev.zarr.zarrjava.experimental.ome.metadata.Axis>();
		axes.add(new dev.zarr.zarrjava.experimental.ome.metadata.Axis("t", "time", "second"));
		axes.add(new dev.zarr.zarrjava.experimental.ome.metadata.Axis("c", "channel", null));
		axes.add(new dev.zarr.zarrjava.experimental.ome.metadata.Axis("z", "space", "micrometer"));
		axes.add(new dev.zarr.zarrjava.experimental.ome.metadata.Axis("y", "space", "micrometer"));
		axes.add(new dev.zarr.zarrjava.experimental.ome.metadata.Axis("x", "space", "micrometer"));

		dev.zarr.zarrjava.experimental.ome.metadata.MultiscalesEntry multiscale = new dev.zarr.zarrjava.experimental.ome.metadata.MultiscalesEntry(
				axes, java.util.Collections.<dev.zarr.zarrjava.experimental.ome.metadata.Dataset>emptyList());

		dev.zarr.zarrjava.experimental.ome.v0_5.MultiscaleImage image = dev.zarr.zarrjava.experimental.ome.v0_5.MultiscaleImage
				.create(handle, multiscale);

		double scaleX = series.physicalSizeXMicrons != null ? series.physicalSizeXMicrons.doubleValue() : 1.0;
		double scaleY = series.physicalSizeYMicrons != null ? series.physicalSizeYMicrons.doubleValue() : 1.0;
		double scaleZ = series.physicalSizeZMicrons != null ? series.physicalSizeZMicrons.doubleValue() : 1.0;

		dev.zarr.zarrjava.v3.DataType dataType = "uint8".equals(series.pixelType)
				? dev.zarr.zarrjava.v3.DataType.UINT8
				: dev.zarr.zarrjava.v3.DataType.UINT16;
		boolean isByte = dataType == dev.zarr.zarrjava.v3.DataType.UINT8;

		// Pyramid: level 0 full resolution, then 2-4 levels each downsampled 2x in x
		// and y, stopping
		// when a level would be smaller than the chunk or after 4 levels. Fractal and
		// big-image viewers
		// expect a pyramid for performance.
		int maxLevels = 4;
		int minSize = 256;
		java.util.List<int[]> levelSizes = new java.util.ArrayList<int[]>();
		int levelX = sizeX;
		int levelY = sizeY;
		for (int levelIndex = 0; levelIndex < maxLevels; levelIndex++) {
			levelSizes.add(new int[] { levelX, levelY });
			if (levelX <= minSize || levelY <= minSize)
				break;
			levelX = Math.max(1, levelX / 2);
			levelY = Math.max(1, levelY / 2);
		}

		for (int levelIndex = 0; levelIndex < levelSizes.size(); levelIndex++) {
			int lx = levelSizes.get(levelIndex)[0];
			int ly = levelSizes.get(levelIndex)[1];
			double factor = Math.pow(2, levelIndex);
			java.util.List<Double> scale = java.util.Arrays.asList(
					Double.valueOf(1.0), Double.valueOf(1.0), Double.valueOf(scaleZ),
					Double.valueOf(scaleY * factor), Double.valueOf(scaleX * factor));
			int effectiveChunk = chunkSize > 0 ? chunkSize : 1024;
			final int chunkY = Math.min(ly, effectiveChunk);
			final int chunkX = Math.min(lx, effectiveChunk);
			// Sharding packs many chunks into one file (fewer files for large images). When
			// enabled, the
			// shard spans the whole plane and the inner chunk is chunkY/chunkX; otherwise a
			// plain chunk.
			final boolean useShard = sharding && (ly > chunkY || lx > chunkX);
			image.createScaleLevel(Integer.toString(levelIndex),
					dev.zarr.zarrjava.v3.Array.metadataBuilder()
							.withShape((long) sizeT, (long) sizeC, (long) sizeZ, (long) ly, (long) lx)
							.withChunkShape(1, 1, 1, useShard ? ly : chunkY, useShard ? lx : chunkX)
							.withDimensionNames("t", "c", "z", "y", "x")
							.withDataType(dataType)
							.withFillValue(0)
							.withCodecs(c -> {
								if (useShard) {
									c.withSharding(
											new int[] { 1, 1, 1, chunkY, chunkX },
											c1 -> c1.withBlosc());
								} else {
									c.withBlosc();
								}
								return c;
							})
							.build(),
					java.util.Collections.singletonList(
							dev.zarr.zarrjava.experimental.ome.metadata.transform.CoordinateTransformation
									.scale(scale)));
		}

		// Write pixels into every level. Level 0 gets the source plane; higher levels
		// get a 2x-per-level
		// downsampled copy (block mean). Only acquired (c,z,t) planes are written; the
		// rest stay fill 0.
		dev.zarr.zarrjava.v3.Array[] levels = new dev.zarr.zarrjava.v3.Array[levelSizes.size()];
		for (int levelIndex = 0; levelIndex < levelSizes.size(); levelIndex++)
			levels[levelIndex] = dev.zarr.zarrjava.v3.Array.open(handle.resolve(Integer.toString(levelIndex)));

		for (ExperimentMetadata.Plane plane : series.planes) {
			if (plane.fileName == null)
				continue;
			File source = new File(measurementFolder, plane.fileName);
			if (!source.isFile())
				continue;
			int t = Math.max(0, plane.timePointIndex);
			int c = Math.max(0, plane.channelIndex);
			int z = Math.max(0, plane.focalPlaneIndex);
			if (t >= sizeT || c >= sizeC || z >= sizeZ)
				continue;
			byte[] bytes = readPlaneBytes(source);

			int[] fullShape = new int[] { sizeY, sizeX };
			for (int levelIndex = 0; levelIndex < levels.length; levelIndex++) {
				int lx = levelSizes.get(levelIndex)[0];
				int ly = levelSizes.get(levelIndex)[1];
				byte[] levelBytes = levelIndex == 0 ? bytes
						: downsample(bytes, fullShape, new int[] { ly, lx }, isByte);
				int[] blockShape = new int[] { 1, 1, 1, ly, lx };
				ucar.ma2.Array block = toUcarBlock(levelBytes, blockShape, isByte);
				long[] offset = new long[] { t, c, z, 0L, 0L };
				levels[levelIndex].write(offset, block);
			}
		}

		// MultiscaleImage.create wrote the "multiscales" block. Navigators such as
		// napari-ome-zarr-navigator additionally require an "omero" block listing the
		// channels (their
		// names and display settings); without it they fail with
		// "NoneType object has no attribute channels". Merge an omero block into the
		// group zarr.json.
		injectOmeroChannels(series, imageFolder);
	}

	/**
	 * Adds an NGFF "omero" channels block to an image group's zarr.json, preserving
	 * the multiscales
	 * block zarr-java already wrote. Channel colours and intensity windows use
	 * sensible defaults; names
	 * come from the model.
	 */
	private void injectOmeroChannels(ExperimentMetadata.ImageSeries series, File imageFolder) throws Exception {
		File jsonFile = new File(imageFolder, "zarr.json");
		if (!jsonFile.isFile())
			return;
		String content = readUtf8(jsonFile);

		int maxValue = "uint8".equals(series.pixelType) ? 255 : 65535;
		StringBuilder channels = new StringBuilder();
		int channelCount = series.channels.isEmpty() ? (series.sizeC == null ? 1 : series.sizeC.intValue())
				: series.channels.size();
		String[] palette = new String[] { "FFFFFF", "00FF00", "FF0000", "0000FF", "FFFF00", "FF00FF", "00FFFF" };
		for (int i = 0; i < channelCount; i++) {
			String name = (i < series.channels.size() && series.channels.get(i).name != null)
					? series.channels.get(i).name
					: ("channel_" + i);
			String color = palette[i % palette.length];
			if (i > 0)
				channels.append(", ");
			channels.append("{ \"label\": ").append(jsonString(name))
					.append(", \"color\": ").append(jsonString(color))
					.append(", \"active\": true")
					.append(", \"window\": { \"min\": 0, \"max\": ").append(maxValue)
					.append(", \"start\": 0, \"end\": ").append(maxValue).append(" } }");
		}
		String omero = "\"omero\": { \"version\": \"0.5\", \"channels\": [" + channels + "] }";

		// Insert the omero entry inside attributes.ome next to multiscales. The group
		// json from
		// zarr-java contains ...\"ome\": { \"version\":\"0.5\", \"multiscales\": [...]
		// }. Insert after the
		// version so the object stays valid.
		String marker = "\"multiscales\"";
		int idx = content.indexOf(marker);
		if (idx < 0)
			return; // unexpected shape; leave as-is rather than corrupt it
		String merged = content.substring(0, idx) + omero + ", " + content.substring(idx);
		writeUtf8(jsonFile, merged);
	}

	private static String readUtf8(File file) throws Exception {
		java.io.InputStream in = new java.io.FileInputStream(file);
		try {
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) >= 0)
				out.write(buffer, 0, read);
			return new String(out.toByteArray(), java.nio.charset.Charset.forName("UTF-8"));
		} finally {
			in.close();
		}
	}

	/**
	 * Downsamples one plane from srcShape (y,x) to dstShape (y,x) by nearest-block
	 * averaging. Operates
	 * on the raw byte buffer for uint8 / little-endian uint16. Used to build
	 * pyramid levels and only averages existing pixels.
	 */
	private byte[] downsample(byte[] src, int[] srcShape, int[] dstShape, boolean isByte) {
		int srcY = srcShape[0], srcX = srcShape[1];
		int dstY = dstShape[0], dstX = dstShape[1];
		if (isByte) {
			byte[] dst = new byte[dstY * dstX];
			for (int y = 0; y < dstY; y++) {
				int sy = (int) ((long) y * srcY / dstY);
				for (int x = 0; x < dstX; x++) {
					int sx = (int) ((long) x * srcX / dstX);
					dst[y * dstX + x] = src[sy * srcX + sx];
				}
			}
			return dst;
		}
		java.nio.ByteBuffer in = java.nio.ByteBuffer.wrap(src).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		byte[] dst = new byte[dstY * dstX * 2];
		java.nio.ByteBuffer out = java.nio.ByteBuffer.wrap(dst).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		for (int y = 0; y < dstY; y++) {
			int sy = (int) ((long) y * srcY / dstY);
			for (int x = 0; x < dstX; x++) {
				int sx = (int) ((long) x * srcX / dstX);
				int index = (sy * srcX + sx) * 2;
				short value = index + 1 < src.length ? in.getShort(index) : 0;
				out.putShort((y * dstX + x) * 2, value);
			}
		}
		return dst;
	}

	/**
	 * Writes the NGFF plate group metadata (zarr.json) at the plate root.
	 * Hand-written from the plate
	 * spec because zarr-java has no plate create API.
	 */
	private void writePlateGroupJson(ExperimentMetadata.Plate plate, File plateFolder) throws Exception {
		StringBuilder rows = new StringBuilder();
		for (int i = 0; i < plate.rowNames.size(); i++) {
			if (i > 0)
				rows.append(", ");
			rows.append("{ \"name\": ").append(jsonString(plate.rowNames.get(i))).append(" }");
		}
		StringBuilder columns = new StringBuilder();
		for (int i = 0; i < plate.columnNames.size(); i++) {
			if (i > 0)
				columns.append(", ");
			columns.append("{ \"name\": ").append(jsonString(plate.columnNames.get(i))).append(" }");
		}
		StringBuilder wells = new StringBuilder();
		for (int i = 0; i < plate.wells.size(); i++) {
			ExperimentMetadata.PlateWell well = plate.wells.get(i);
			if (i > 0)
				wells.append(", ");
			wells.append("{ \"path\": ").append(jsonString(well.path))
					.append(", \"rowIndex\": ").append(well.rowIndex)
					.append(", \"columnIndex\": ").append(well.columnIndex).append(" }");
		}
		String json = "{\n"
				+ "  \"zarr_format\": 3,\n"
				+ "  \"node_type\": \"group\",\n"
				+ "  \"attributes\": {\n"
				+ "    \"ome\": {\n"
				+ "      \"version\": \"0.5\",\n"
				+ "      \"plate\": {\n"
				+ "        \"version\": \"0.5\",\n"
				+ "        \"name\": " + jsonString(plate.name) + ",\n"
				+ "        \"rows\": [" + rows + "],\n"
				+ "        \"columns\": [" + columns + "],\n"
				+ "        \"wells\": [" + wells + "],\n"
				+ "        \"field_count\": 1\n"
				+ "      }\n"
				+ "    }\n"
				+ "  }\n"
				+ "}\n";
		plateFolder.mkdirs();
		writeUtf8(new File(plateFolder, "zarr.json"), json);
	}

	/**
	 * Writes the NGFF well group metadata for a well, declaring its single field
	 * "0".
	 */
	private void writeWellGroupJson(File plateFolder, String wellPath, int fieldCount) throws Exception {
		StringBuilder images = new StringBuilder();
		int count = Math.max(1, fieldCount);
		for (int i = 0; i < count; i++) {
			if (i > 0)
				images.append(", ");
			images.append("{ \"path\": \"").append(i).append("\" }");
		}
		String json = "{\n"
				+ "  \"zarr_format\": 3,\n"
				+ "  \"node_type\": \"group\",\n"
				+ "  \"attributes\": {\n"
				+ "    \"ome\": {\n"
				+ "      \"version\": \"0.5\",\n"
				+ "      \"well\": {\n"
				+ "        \"version\": \"0.5\",\n"
				+ "        \"images\": [ " + images + " ]\n"
				+ "      }\n"
				+ "    }\n"
				+ "  }\n"
				+ "}\n";
		File wellFolder = new File(plateFolder, wellPath.replace('/', File.separatorChar));
		wellFolder.mkdirs();
		writeUtf8(new File(wellFolder, "zarr.json"), json);
	}

	private static String[] splitPath(String path) {
		return path.split("/");
	}

	private static String jsonString(String value) {
		if (value == null)
			return "null";
		StringBuilder b = new StringBuilder("\"");
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch == '"' || ch == '\\')
				b.append('\\').append(ch);
			else if (ch == '\n')
				b.append("\\n");
			else
				b.append(ch);
		}
		return b.append('"').toString();
	}

	/**
	 * Deletes a file or directory tree if it exists, so a re-export overwrites
	 * cleanly. zarr-java
	 * refuses to create an array where a zarr.json already exists, so a stale store
	 * from a previous
	 * export must be removed first.
	 */
	private static void deleteRecursively(File file) {
		if (file == null || !file.exists())
			return;
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children)
				deleteRecursively(child);
		}
		// Best effort: ignore failure, the subsequent write will report a clearer error
		// if it matters.
		file.delete();
	}

	private static void writeUtf8(File file, String content) throws Exception {
		java.io.FileOutputStream stream = new java.io.FileOutputStream(file);
		try {
			java.io.Writer writer = new java.io.OutputStreamWriter(stream, java.nio.charset.Charset.forName("UTF-8"));
			writer.write(content);
			writer.flush();
		} finally {
			stream.close();
		}
	}

	/**
	 * Wraps raw plane bytes as a ucar.ma2 array shaped exactly like the block being
	 * written
	 * (1,1,1,y,x), of the right element type. Bio-Formats returns pixels
	 * little-endian; ByteBuffer with
	 * LITTLE_ENDIAN honours that for 16-bit. The backing data length must equal the
	 * product of
	 * blockShape, or zarr-java rejects the write.
	 */
	private ucar.ma2.Array toUcarBlock(byte[] bytes, int[] blockShape, boolean isByte) {
		int count = 1;
		for (int dimension : blockShape)
			count *= dimension;
		if (isByte) {
			byte[] data = bytes;
			if (data.length != count) {
				data = new byte[count];
				System.arraycopy(bytes, 0, data, 0, Math.min(bytes.length, count));
			}
			return ucar.ma2.Array.factory(ucar.ma2.DataType.UBYTE, blockShape, data);
		}
		short[] shorts = new short[count];
		java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < shorts.length && buffer.remaining() >= 2; i++)
			shorts[i] = buffer.getShort();
		return ucar.ma2.Array.factory(ucar.ma2.DataType.USHORT, blockShape, shorts);
	}

}
