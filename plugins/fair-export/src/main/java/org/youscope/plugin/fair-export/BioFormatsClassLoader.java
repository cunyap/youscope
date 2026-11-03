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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the Bio-Formats / OME jars in an isolated classloader, created only
 * when an export actually
 * runs.
 * <p>
 * This exists because of a sealing violation at server startup:
 * </p>
 * 
 * <pre>
 * SecurityException: sealing violation: can't seal package
 * com.sun.media.imageioimpl.plugins.jpeg2000: already loaded
 * </pre>
 * <p>
 * {@code ome-jai} seals the JAI codec packages. If those OME jars are named on
 * the plugin manifest's
 * {@code Class-Path}, they join the application classpath at startup, and
 * YouScope's own child-first
 * {@code MicroscopeClassLoader} can then cause the same sealed package to be
 * defined twice. The
 * JVM refuses this, aborting the server before it finishes starting.
 * </p>
 * <p>
 * The fix is to keep the OME jars <b>off</b> the manifest Class-Path entirely
 * (so the server never
 * touches them at boot) and load them here instead, once, in a dedicated loader
 * that is created lazily
 * on the first export. The loader is child-first for the OME and JAI packages
 * so their classes resolve
 * from these jars and nowhere else, and delegates everything else including
 * the YouScope API and the
 * plugin's own model classes to the parent, so types cross the boundary
 * cleanly.
 * </p>
 * <p>
 * The metadata export does not need the codec jars at all, so by default they
 * are not even on this
 * loader's path. WP8 (OME-TIFF writing) will add them here, where the isolation
 * makes the sealed JAI
 * package safe to define.
 * </p>
 * 
 * @author Andreas P. Cuny
 */
class BioFormatsClassLoader extends URLClassLoader {
	/**
	 * Jars loaded child-first, resolved from the YouScope {@code lib} folder. Only
	 * the metadata stack;
	 * codec jars are deliberately excluded until they are needed.
	 */
	private static final String[] REQUIRED_JARS = new String[] {
			"formats-api-7.3.1.jar",
			"formats-bsd-7.3.1.jar",
			"ome-common-6.0.22.jar",
			"ome-xml-6.3.6.jar",
			"joda-time-2.12.2.jar",
			"slf4j-api-2.0.9.jar",
			// Needed by OMETiffWriter. Safe here because these jars are only ever on this
			// isolated loader,
			// never on the plugin manifest Class-Path, so the sealed JAI codec package is
			// defined exactly
			// once, inside this loader, and cannot collide with the copy the rest of
			// YouScope may load.
			"ome-codecs-1.0.3.jar",
			"specification-6.3.6.jar"
	};

	/**
	 * SLF4J and its no-op provider. Loaded child-first (see below) so that
	 * Bio-Formats' logging
	 * resolves its provider entirely within this loader, finds slf4j-nop, and stays
	 * silent rather
	 * than triggering the parent's SLF4J, which has no provider on its own search
	 * path and prints the
	 * "No SLF4J providers were found" banner. Optional: if slf4j-nop is absent, the
	 * banner returns but
	 * nothing breaks.
	 */
	private static final String[] OPTIONAL_JARS = new String[] {
			"slf4j-nop-2.0.9.jar",
			// OME-Zarr backend. Optional so that their absence disables only OME-Zarr
			// export, never
			// OME-TIFF or OME-XML. zarr-java writes the NGFF metadata; blosc-java and
			// zstd-jni carry their
			// own native binaries in-jar, so no separately installed blosc.dll is needed.
			// Jackson is
			// zarr-java's JSON dependency (already present in lib for other YouScope uses,
			// but listed here
			// so the isolated loader can see it child-first alongside zarr-java).
			"zarr-java-0.1.3.jar",
			"cdm-core-5.9.1.jar",
			"blosc-java-0.3-1.21.6.jar",
			"zstd-jni-1.5.5-7.jar",
			// Newer commons + jackson than the shared lib. Placed on the isolated loader
			// (and child-first
			// below) so zarr-java sees the versions it needs without changing what the rest
			// of YouScope
			// resolves. This is why the export runs isolated: the version bumps stay
			// contained.
			"commons-compress-1.28.0.jar",
			"commons-io-2.20.0.jar",
			"commons-codec-1.19.0.jar",
			"commons-lang3-3.18.0.jar",
			"jackson-databind-2.20.0.jar",
			"jackson-core-2.20.0.jar",
			"jackson-annotations-2.20.jar",
			"jackson-datatype-jdk8-2.20.0.jar"
	};

	/**
	 * Package prefixes resolved child-first, i.e. from the jars on this loader
	 * rather than delegated to
	 * the parent. Everything else delegates to the parent so that shared types like
	 * the YouScope API, the
	 * plugin's own {@code model} and {@code ome} classes are the same class on
	 * both sides.
	 */
	private static final String[] CHILD_FIRST_PACKAGES = new String[] {
			"loci.",
			"ome.",
			"org.joda.time.",
			"org.slf4j.",
			"com.sun.media.",
			"javax.media.jai."
	};

	private static volatile BioFormatsClassLoader instance = null;

	private BioFormatsClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	/**
	 * Returns the shared instance, creating it on first use.
	 * 
	 * @return The isolated Bio-Formats classloader.
	 * @throws IllegalStateException If the required jars cannot be located in the
	 *                               YouScope lib folder.
	 */
	static BioFormatsClassLoader get() {
		BioFormatsClassLoader local = instance;
		if (local != null)
			return local;
		synchronized (BioFormatsClassLoader.class) {
			if (instance == null)
				instance = create();
			return instance;
		}
	}

	private static BioFormatsClassLoader create() {
		File libFolder = findLibFolder();
		if (libFolder == null)
			throw new IllegalStateException(
					"Could not locate the YouScope lib folder to load the Bio-Formats jars from.");

		List<URL> urls = new ArrayList<URL>();
		List<String> missing = new ArrayList<String>();
		for (String jarName : REQUIRED_JARS) {
			File jar = new File(libFolder, jarName);
			if (!jar.isFile()) {
				missing.add(jarName);
				continue;
			}
			try {
				urls.add(jar.toURI().toURL());
			} catch (@SuppressWarnings("unused") MalformedURLException e) {
				missing.add(jarName);
			}
		}
		for (String jarName : OPTIONAL_JARS) {
			File jar = new File(libFolder, jarName);
			if (jar.isFile()) {
				try {
					urls.add(jar.toURI().toURL());
				} catch (@SuppressWarnings("unused") MalformedURLException e) {
					// optional; ignore
				}
			}
		}

		if (!missing.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			builder.append("The Bio-Formats jars needed for the FAIR export are missing from ");
			builder.append(libFolder.getAbsolutePath()).append(": ");
			for (int i = 0; i < missing.size(); i++) {
				if (i > 0)
					builder.append(", ");
				builder.append(missing.get(i));
			}
			throw new IllegalStateException(builder.toString());
		}

		// The service implementation lives inside the plugin jar. For the child loader
		// to define it
		// (child-first), that jar must be on the child's own URL list too, otherwise
		// findClass cannot
		// see its bytecode Add the plugin's code source.
		try {
			URL pluginLocation = BioFormatsClassLoader.class.getProtectionDomain().getCodeSource().getLocation();
			if (pluginLocation != null)
				urls.add(pluginLocation);
		} catch (@SuppressWarnings("unused") Exception e) {
			// If this fails the impl simply loads parent-first; the isolation is weaker but
			// startup is
			// already safe because nothing OME is on the manifest Class-Path.
		}

		return new BioFormatsClassLoader(urls.toArray(new URL[urls.size()]),
				BioFormatsClassLoader.class.getClassLoader());
	}

	/**
	 * Locates the {@code lib} folder of the running YouScope installation, by
	 * walking up from the jar
	 * this class was loaded from. The plugin sits in {@code plugins/fair-export/},
	 * and the shared jars
	 * are in {@code lib/}, so the search looks for a sibling {@code lib} directory
	 * of an ancestor.
	 */
	private static File findLibFolder() {
		try {
			URL location = BioFormatsClassLoader.class.getProtectionDomain().getCodeSource().getLocation();
			File current = new File(location.toURI());
			// Walk up a bounded number of levels looking for a lib folder that contains the
			// jars.
			for (int i = 0; i < 6 && current != null; i++) {
				File lib = new File(current, "lib");
				if (isLibFolder(lib))
					return lib;
				File sibling = new File(current.getParentFile(), "lib");
				if (isLibFolder(sibling))
					return sibling;
				current = current.getParentFile();
			}
		} catch (@SuppressWarnings("unused") Exception e) {
			// fall through to the property-based fallback
		}

		// Fallback: an explicit system property, useful for tests and unusual layouts.
		String configured = System.getProperty("youscope.lib.dir");
		if (configured != null) {
			File lib = new File(configured);
			if (isLibFolder(lib))
				return lib;
		}
		return null;
	}

	private static boolean isLibFolder(File candidate) {
		return candidate != null && candidate.isDirectory()
				&& new File(candidate, REQUIRED_JARS[0]).isFile();
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> loaded = findLoadedClass(name);
			if (loaded == null) {
				if (isChildFirst(name)) {
					try {
						loaded = findClass(name);
					} catch (@SuppressWarnings("unused") ClassNotFoundException e) {
						loaded = super.loadClass(name, false);
					}
				} else {
					loaded = super.loadClass(name, false);
				}
			}
			if (resolve)
				resolveClass(loaded);
			return loaded;
		}
	}

	/**
	 * The OME builder package is loaded child-first, because those classes
	 * reference {@code loci.ome.*} types
	 * that must resolve from the isolated jars. The single exception is the
	 * {@link OmeMetadataService}
	 * interface itself, which must stay parent-loaded so that the plugin and the
	 * isolated implementation
	 * share exactly one copy of it and values can cross the boundary.
	 */
	private static final String SHARED_INTERFACE = "org.youscope.plugin.fairexport.ome.OmeMetadataService";
	private static final String OME_BUILDER_PACKAGE = "org.youscope.plugin.fairexport.ome.";
	private static final String MODEL_PACKAGE = "org.youscope.plugin.fairexport.model.";

	private static boolean isChildFirst(String name) {
		// The shared interface and the model types must be the parent's classes: they
		// appear in the
		// interface signatures, so both sides have to agree on them.
		if (name.equals(SHARED_INTERFACE) || name.startsWith(MODEL_PACKAGE))
			return false;
		// Everything else in the ome package (builder, crosswalk, impl) is child-first.
		if (name.startsWith(OME_BUILDER_PACKAGE))
			return true;
		for (String prefix : CHILD_FIRST_PACKAGES) {
			if (name.startsWith(prefix))
				return true;
		}
		return false;
	}
}
