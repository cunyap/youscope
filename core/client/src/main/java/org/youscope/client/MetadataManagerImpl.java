/*******************************************************************************
 * Copyright (c) 2017 Moritz Lang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Moritz Lang - initial API and implementation
 *     Andreas P. Cuny - update API supporting FAIR metadata
 ******************************************************************************/
package org.youscope.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.youscope.clientinterfaces.MetadataCategoryProvider;
import org.youscope.clientinterfaces.MetadataDefinition;
import org.youscope.clientinterfaces.MetadataDefinitionManager;
import org.youscope.clientinterfaces.YouScopeClientException;
import org.youscope.common.util.TextTools;

import com.thoughtworks.xstream.XStream;

class MetadataManagerImpl extends HashMap<String, MetadataDefinition>
		implements MetadataDefinitionManager, MetadataCategoryProvider {
	/**
	 * Serial Version UID.
	 */
	private static final long serialVersionUID = 64314818791363585L;
	private static final String PROPERTY_ALLOW_CUSTOM_PROPERTIES = "Youscope.Client.AllowCustomProperties";
	private static final String METADATA_FOLDER_NAME = "configuration" + File.separator + "metadata";
	/**
	 * Maximal folder nesting depth which is scanned for metadata definitions.
	 * Guards against symlink loops.
	 */
	private static final int MAX_FOLDER_DEPTH = 8;

	/**
	 * Maps the name of a metadata definition to its category, i.e. the sub-folder
	 * of
	 * {@value #METADATA_FOLDER_NAME} it was loaded from.
	 */
	private final HashMap<String, String> definitionCategories = new HashMap<String, String>();
	/**
	 * Maps the name of a metadata definition to the file it was loaded from, such
	 * that modifications are
	 * written back to the original location instead of the metadata root folder.
	 */
	private final HashMap<String, File> definitionFiles = new HashMap<String, File>();

	private static MetadataManagerImpl instance = null;

	private MetadataManagerImpl() {
		// do nothing.
	}

	public synchronized static MetadataManagerImpl getInstance() {
		if (instance == null) {
			instance = new MetadataManagerImpl();
			if (!instance.loadMetadataDefinitions()) {
				// means folder does not exist, that is, this is the first time we get invoked.
				// Generate some new definitions, and save them (folder gets created while doing
				// so...)
				for (DefaultDefinition defaultDefinition : generateDefaults()) {
					try {
						instance.setMetadataDefinition(defaultDefinition.definition, defaultDefinition.category);
					} catch (@SuppressWarnings("unused") YouScopeClientException e) {
						// do nothing. The user can well live without defaults...
					}
				}

			}
		}
		return instance;
	}

	/**
	 * A metadata definition together with the category (sub-folder) it should be
	 * stored in.
	 */
	private static class DefaultDefinition {
		final MetadataDefinition definition;
		final String category;

		DefaultDefinition(MetadataDefinition definition, String category) {
			this.definition = definition;
			this.category = category;
		}
	}

	private static Collection<DefaultDefinition> generateDefaults() {
		ArrayList<DefaultDefinition> result = new ArrayList<DefaultDefinition>();
		result.add(new DefaultDefinition(
				new MetadataDefinition("Temperature", MetadataDefinition.Type.DEFAULT, true, 0, 100, null, "C"),
				"imaging"));
		result.add(new DefaultDefinition(new MetadataDefinition("User", MetadataDefinition.Type.OPTIONAL, true),
				"provenance"));
		result.add(new DefaultDefinition(
				new MetadataDefinition("Species", MetadataDefinition.Type.DEFAULT, true, getModelOrganisms()),
				"biological"));
		result.add(new DefaultDefinition(new MetadataDefinition("Strain", MetadataDefinition.Type.DEFAULT, true),
				"biological"));
		return result;
	}

	private static String[] getModelOrganisms() {
		String[] result = new String[] {
				"Escherichia coli",
				"Dictyostelium discoideum",
				"Saccharomyces cerevisiae",
				"Schizosaccharomyces pombe",
				"Chlamydomonas reinhardtii",
				"Tetrahymena thermophila",
				"Emiliania huxleyi",
				"Caenorhabditis elegans",
				"Drosophila melanogaster",
				"Arabidopsis thaliana",
				"Physcomitrella patens",
				"Danio rerio",
				"Fundulus heteroclitus",
				"Nothobranchius furzeri",
				"Oryzias latipes",
				"Anolis carolinensis",
				"Mus musculus",
				"Xenopus laevis"
		};
		Arrays.sort(result);
		return result;
	}

	@Override
	public boolean isAllowCustomMetadata() {
		return PropertyProviderImpl.getInstance().getProperty(PROPERTY_ALLOW_CUSTOM_PROPERTIES, true);
	}

	@Override
	public Iterator<MetadataDefinition> iterator() {
		return getMetadataDefinitions().iterator();
	}

	@Override
	public Collection<MetadataDefinition> getMetadataDefinitions() {
		ArrayList<MetadataDefinition> result = new ArrayList<MetadataDefinition>(values());
		Collections.sort(result);
		return result;
	}

	@Override
	public Collection<MetadataDefinition> getMandatoryMetadataDefinitions() {
		ArrayList<MetadataDefinition> result = new ArrayList<MetadataDefinition>(values());
		for (Iterator<MetadataDefinition> iter = result.iterator(); iter.hasNext();) {
			if (iter.next().getType() != MetadataDefinition.Type.MANDATORY)
				iter.remove();
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public Collection<MetadataDefinition> getDefaultMetadataDefinitions() {
		ArrayList<MetadataDefinition> result = new ArrayList<MetadataDefinition>(values());
		for (Iterator<MetadataDefinition> iter = result.iterator(); iter.hasNext();) {
			MetadataDefinition.Type type = iter.next().getType();
			if (type != MetadataDefinition.Type.MANDATORY && type != MetadataDefinition.Type.DEFAULT)
				iter.remove();
		}
		Collections.sort(result);
		return result;
	}

	@Override
	public MetadataDefinition getMetadataDefinition(String name) {
		return get(name);
	}

	@Override
	public Collection<String> getMetadataCategories() {
		TreeSet<String> result = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		for (String category : definitionCategories.values()) {
			if (category != null && !category.isEmpty())
				result.add(category);
		}
		return new ArrayList<String>(result);
	}

	@Override
	public String getMetadataCategory(String definitionName) {
		if (definitionName == null)
			return UNCATEGORIZED;
		String category = definitionCategories.get(definitionName);
		return category == null ? UNCATEGORIZED : category;
	}

	@Override
	public Collection<MetadataDefinition> getMetadataDefinitions(String category) {
		String normalized = normalizeCategory(category);
		ArrayList<MetadataDefinition> result = new ArrayList<MetadataDefinition>();
		for (MetadataDefinition definition : values()) {
			if (getMetadataCategory(definition.getName()).equalsIgnoreCase(normalized))
				result.add(definition);
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Brings a category into its canonical form: forward slashes, no
	 * leading/trailing separators, no
	 * path traversal. Invalid categories are mapped to
	 * {@link MetadataCategoryProvider#UNCATEGORIZED}.
	 */
	static String normalizeCategory(String category) {
		if (category == null)
			return UNCATEGORIZED;
		String result = category.replace('\\', '/').trim();
		while (result.startsWith("/"))
			result = result.substring(1);
		while (result.endsWith("/"))
			result = result.substring(0, result.length() - 1);
		if (result.isEmpty())
			return UNCATEGORIZED;
		for (String segment : result.split("/")) {
			if (segment.trim().isEmpty() || ".".equals(segment) || "..".equals(segment))
				return UNCATEGORIZED;
		}
		return result;
	}

	private static File getCategoryFolder(String category) {
		File rootFolder = new File(METADATA_FOLDER_NAME);
		String normalized = normalizeCategory(category);
		if (normalized.isEmpty())
			return rootFolder;
		return new File(rootFolder, normalized.replace('/', File.separatorChar));
	}

	/**
	 * Determines the category of a definition file from its position relative to
	 * the metadata root folder.
	 */
	private static String getCategoryOfFile(File file, File rootFolder) {
		File parent = file.getParentFile();
		if (parent == null)
			return UNCATEGORIZED;
		String rootPath = rootFolder.getAbsolutePath();
		String parentPath = parent.getAbsolutePath();
		if (parentPath.length() <= rootPath.length() || !parentPath.startsWith(rootPath))
			return UNCATEGORIZED;
		String relative = parentPath.substring(rootPath.length());
		return normalizeCategory(relative);
	}

	private static XStream getSerializerInstance() {
		XStream xstream = new XStream();
		xstream.aliasSystemAttribute("type", "class");
		// Process the annotations of the classes needed to know.
		xstream.processAnnotations(new Class<?>[] { MetadataDefinition.class, MetadataDefinition.Type.class });
		return xstream;
	}

	/**
	 * Loads all metadata definitions from {@value #METADATA_FOLDER_NAME} <b>and all
	 * of its sub-folders</b>.
	 * The sub-folder a definition is stored in becomes its category.
	 * 
	 * @return True if the metadata folder exists, false if it has to be created.
	 */
	private boolean loadMetadataDefinitions() {
		File rootFolder = new File(METADATA_FOLDER_NAME);
		if (!rootFolder.exists() || !rootFolder.isDirectory())
			return false;
		loadMetadataDefinitions(rootFolder, rootFolder, getSerializerInstance(), 0);
		return true;
	}

	private void loadMetadataDefinitions(File folder, File rootFolder, XStream xstream, int depth) {
		File[] children = folder.listFiles();
		if (children == null) {
			ClientSystem.err.println("Could not list content of metadata folder " + folder.getAbsolutePath() + ".",
					null);
			return;
		}
		// Sort to make loading deterministic, e.g. when the same definition name exists
		// in two folders.
		Arrays.sort(children);

		for (File child : children) {
			if (!child.isFile())
				continue;
			if (!child.getName().toLowerCase().endsWith(".xml"))
				continue;
			loadMetadataDefinition(child, rootFolder, xstream);
		}
		if (depth >= MAX_FOLDER_DEPTH)
			return;
		for (File child : children) {
			if (!child.isDirectory() || child.isHidden() || child.getName().startsWith("."))
				continue;
			loadMetadataDefinitions(child, rootFolder, xstream, depth + 1);
		}
	}

	private void loadMetadataDefinition(File file, File rootFolder, XStream xstream) {
		try (FileInputStream fis = new FileInputStream(file);) {
			MetadataDefinition definition = (MetadataDefinition) xstream.fromXML(fis);
			if (definition == null || definition.getName() == null || definition.getName().isEmpty()) {
				ClientSystem.err.println("Metadata definition file " + file.getAbsolutePath()
						+ " does not define a valid property name. File ignored.", null);
				return;
			}
			String name = definition.getName();
			File previousFile = definitionFiles.get(name);
			if (previousFile != null) {
				ClientSystem.err.println(
						"Metadata definition \"" + name + "\" is defined both in " + previousFile.getAbsolutePath()
								+ " and in " + file.getAbsolutePath() + ". The latter is ignored.",
						null);
				return;
			}
			put(name, definition);
			definitionFiles.put(name, file);
			definitionCategories.put(name, getCategoryOfFile(file, rootFolder));
		} catch (Throwable e) {
			ClientSystem.err
					.println("Could not load measurement metadata definition file " + file.getAbsolutePath() + ".", e);
		}
	}

	@Override
	public void setMetadataDefinition(MetadataDefinition property) throws YouScopeClientException {
		// Keep the definition in the folder it currently lives in.
		setMetadataDefinition(property, getMetadataCategory(property == null ? null : property.getName()));
	}

	/**
	 * Saves a metadata definition into the sub-folder corresponding to the given
	 * category, creating the
	 * folder if necessary. If the definition already exists in a different
	 * category, its old file is removed.
	 * 
	 * @param property Definition to save.
	 * @param category Category (sub-folder) to save the definition in, or
	 *                 {@link MetadataCategoryProvider#UNCATEGORIZED}.
	 * @throws YouScopeClientException Thrown if the definition could not be saved.
	 */
	public void setMetadataDefinition(MetadataDefinition property, String category) throws YouScopeClientException {
		if (property == null || property.getName() == null || property.getName().isEmpty())
			throw new YouScopeClientException("Metadata definition or its name is null or empty.");
		String normalizedCategory = normalizeCategory(category);
		File folder = getCategoryFolder(normalizedCategory);
		if (!folder.exists() && !folder.mkdirs())
			throw new YouScopeClientException("Could not create metadata folder " + folder.getAbsolutePath() + ".");

		File oldFile = definitionFiles.get(property.getName());
		File file = new File(folder, TextTools.convertToFileName(property.getName()) + ".xml");

		XStream xstream = getSerializerInstance();
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(new String("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n").getBytes());
			fos.flush();

			xstream.toXML(property, fos);
		} catch (IOException e) {
			throw new YouScopeClientException("Could not save measurement metadata configuration " + property.getName()
					+ " to file " + file.getAbsolutePath() + ".", e);
		}

		// The definition moved to another category: get rid of the stale file,
		// otherwise it would be
		// loaded again on the next start and produce a duplicate.
		if (oldFile != null && !oldFile.getAbsolutePath().equals(file.getAbsolutePath()) && oldFile.exists()) {
			if (!oldFile.delete())
				ClientSystem.err.println(
						"Could not delete outdated metadata definition file " + oldFile.getAbsolutePath() + ".", null);
		}

		put(property.getName(), property);
		definitionFiles.put(property.getName(), file);
		definitionCategories.put(property.getName(), normalizedCategory);
	}

	@Override
	public boolean deleteMetadataDefinition(String name) {
		if (remove(name) == null)
			return false;
		definitionCategories.remove(name);
		File file = definitionFiles.remove(name);
		if (file == null) {
			// Definition was never loaded from disk; fall back to the legacy location.
			file = new File(METADATA_FOLDER_NAME, TextTools.convertToFileName(name) + ".xml");
		}
		if (file.exists() && !file.delete())
			ClientSystem.err.println("Could not delete metadata definition file " + file.getAbsolutePath() + ".", null);
		return true;
	}

	@Override
	public int getNumMetadataDefinitions() {
		return size();
	}
}
